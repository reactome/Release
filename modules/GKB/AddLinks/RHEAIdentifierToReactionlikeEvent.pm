=head1 NAME

GKB::AddLinks::RHEAIdentifierToReactionlikeEvent

=head1 SYNOPSIS

=head1 DESCRIPTION

Uses RHEA's web services to find the RHEA reactions that have correspondences in Reactome, and insert links.

=head1 SEE ALSO

GKB::DBAdaptor

=head1 AUTHOR

David Croft E<lt>croft@ebi.ac.ukE<gt>

Copyright (c) 2012 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

package GKB::AddLinks::RHEAIdentifierToReactionlikeEvent;

use GKB::Config;
use GKB::AddLinks::Builder;
use strict;
use vars qw(@ISA $AUTOLOAD %ok_field);
use LWP::UserAgent;

@ISA = qw(GKB::AddLinks::Builder);

sub AUTOLOAD {
    my $self = shift;
    my $attr = $AUTOLOAD;
    $attr =~ s/.*:://;
    return unless $attr =~ /[^A-Z]/;  # skip DESTROY and all-cap methods
    $self->throw("invalid attribute method: ->$attr()") unless $ok_field{$attr};
    $self->{$attr} = shift if @_;
    return $self->{$attr};
}  

sub new {
    my($pkg) = @_;

   	# Get class variables from superclass and define any new ones
   	# specific to this class.
	$pkg->get_ok_field();

   	my $self = $pkg->SUPER::new();
   	
    return $self;
}

# Needed by subclasses to gain access to object variables defined in
# this class.
sub get_ok_field {
	my ($pkg) = @_;
	
	%ok_field = $pkg->SUPER::get_ok_field();

	return %ok_field;
}

sub clear_variables {
    my ($self) = @_;
    
	$self->SUPER::clear_variables();
}

sub buildPart {
	my ($self) = @_;
	
	print STDERR "\n\nRHEAIdentifierToReactionlikeEvent.buildPart: entered\n";
	
	$self->timer->start($self->timer_message);
	my $dba = $self->builder_params->refresh_dba();
	
	print STDERR "\n\nRHEAIdentifierToReactionlikeEvent.buildPart: do a set_instance_edit_note\n";

	my $attribute = 'crossReference';
	$self->set_instance_edit_note("{$attribute}s inserted by RHEAIdentifierToReactionlikeEvent");

	print STDERR "\n\nRHEAIdentifierToReactionlikeEvent.buildPart: do a matching_instance_handler\n";

	$dba->matching_instance_handler(new GKB::MatchingInstanceHandler::Simpler);

	print STDERR "\n\nRHEAIdentifierToReactionlikeEvent.buildPart: do a get_all_rhea_ids_with_links_to_reactome\n";

	my $rhea_ids = $self->get_all_rhea_ids_with_links_to_reactome();
	my $rhea_cross_reference_count = 0;
	foreach my $rhea_id (@{$rhea_ids}) {
	    print STDERR "\n\nRHEAIdentifierToReactionlikeEvent.buildPart: do a rhea_id=$rhea_id\n";

	    my $reactome_id = $self->get_reactome_id_from_rhea_id($rhea_id);

	    print STDERR "\n\nRHEAIdentifierToReactionlikeEvent.buildPart: do a reactome_id=$reactome_id\n";
	    
	    if (!(defined $reactome_id) || $reactome_id eq "") {
			print STDERR "RHEAIdentifierToReactionlikeEvent.buildPart: WARNING - $rhea_id has no matching Reactome ID\n";
	    	next;
	    }
	    
	    my $instance = undef;
	    my $stable_identifier = undef;
	    if ($reactome_id =~ /(REACT_[0-9]+)/) {
	    	# First check to see if ID is actually a stable
	    	# ID and use that to retrieve instance if so
	    	$stable_identifier = $1;
	    	my $instances = $dba->fetch_instance_by_remote_attribute('DatabaseObject',[['stableIdentifier.identifier','=',[$stable_identifier]]]);
	    	$instance = $instances->[0];
	    }
	    if (!$instance) {
	    	# Maybe ID really was a DB_ID - let's see:
	    	$instance = $dba->fetch_instance_by_db_id($reactome_id)->[0];
	    }
	    if (!$instance) {
			print STDERR "RHEAIdentifierToReactionlikeEvent.buildPart: WARNING - $rhea_id\t$reactome_id\tno instance with matching DB_ID\n";
			next;
	    }
	    if (!($instance->is_a('ReactionlikeEvent'))) {
			print STDERR "RHEAIdentifierToReactionlikeEvent.buildPart: WARNING - $rhea_id\t$reactome_id\tnot a ReactionlikeEvent\t", $instance->extended_displayName, "\n";
			next;
	    }
	    
		print STDERR "RHEAIdentifierToReactionlikeEvent.buildPart: rhea_id=$rhea_id, reactome_id=$reactome_id, reaction=", $instance->extended_displayName, "\n";

	    my $database_identifier = $self->builder_params->database_identifier->get_rhea_database_identifier($rhea_id);
	    $instance->add_attribute_value($attribute, $database_identifier);
	    $dba->update_attribute($instance, $attribute);
		$instance->add_attribute_value('modified', $self->instance_edit);
		$dba->update_attribute($instance, 'modified');
		
		$rhea_cross_reference_count++;
	}
	
	print STDERR "RHEAIdentifierToReactionlikeEvent.buildPart: rhea_cross_reference_count=$rhea_cross_reference_count\n";

	$self->timer->stop($self->timer_message);
	$self->timer->print();

	print STDERR "\n\nRHEAIdentifierToReactionlikeEvent.buildPart: done\n";
}

sub get_page {
	my ($self, $url) = @_;
    
	print STDERR "RHEAIdentifierToReactionlikeEvent.get_page: url=$url\n";
	
    my $ua = LWP::UserAgent->new();
    my $response = $ua->get($url);
    if (defined $response && $response->is_success) {
        return $response->content;
    }
    
	print STDERR "RHEAIdentifierToReactionlikeEvent.get_page: WARNING - Failed to GET $url\n";
	
	return "";
}

sub get_all_rhea_ids_with_links_to_reactome {
	my ($self) = @_;
	
#    $ENV{FTP_PASSIVE} = 1;
    my $url = 'http://www.ebi.ac.uk/rhea/rest/1.0/ws/reaction?q=REACT_*';
    my $page = $self->get_page($url);
    
	print STDERR "RHEAIdentifierToReactionlikeEvent.get_all_rhea_ids_with_links_to_reactome: page=$page\n";
	
    my @lines = split(/\n/, $page);
    my @rhea_ids = ();
    my %seen = ();
    foreach my $line (@lines) {
		if ($line =~ /reaction\/cmlreact\/([0-9]+)/ || $line =~ /reaction\/biopax2\/([0-9]+)/) {
			my $rhea_id = $1;
			if (!$seen{$rhea_id}) {
			    print STDERR "RHEAIdentifierToReactionlikeEvent.get_all_rhea_ids_with_links_to_reactome: adding rhea_id=$rhea_id\n";
			    push @rhea_ids, $rhea_id;
			    $seen{$rhea_id} = 1;
			}
		}
    }
    return \@rhea_ids;
}

sub get_reactome_id_from_rhea_id {
	my ($self, $rhea_id) = @_;
	
    my $url = "http://www.ebi.ac.uk/rhea/rest/1.0/ws/reaction/cmlreact/$rhea_id";
    my $page = $self->get_page($url);
    my @lines = split(/\n/, $page);
    my $reactome_id = "";
    foreach my $line (@lines) {
		if ($line =~ /(REACT_[0-9]+)/) {
			$reactome_id = $1;
			last;
		}
    }
    return $reactome_id;
}

1;

