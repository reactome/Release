=head1 NAME

GKB::AddLinks::IntActDatabaseIdentifierToComplexOrReactionlikeEvent

=head1 SYNOPSIS

=head1 DESCRIPTION


Original code lifted from the script add_links_to_IntAct.pl, from Imre and David.

=head1 SEE ALSO

GKB::DBAdaptor

=head1 AUTHOR

David Croft E<lt>croft@ebi.ac.ukE<gt>

Copyright (c) 2008 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

package GKB::AddLinks::IntActDatabaseIdentifierToComplexOrReactionlikeEvent;

use GKB::Config;
use GKB::AddLinks::Builder;
use GKB::InteractionGenerator;
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
   	
    $self->generate_interactions(0);
   	
    return $self;
}

# Needed by subclasses to gain access to object variables defined in
# this class.
sub get_ok_field {
	my ($pkg) = @_;
	
	%ok_field = $pkg->SUPER::get_ok_field();
	$ok_field{"generate_interactions"}++;

	return %ok_field;
}

sub clear_variables {
    my ($self) = @_;
    
	$self->SUPER::clear_variables();
	
	$self->generate_interactions(undef);
}

# Set to 1 if you want to add automatically created links from Reactome
# to IntAct; by default or when set to 0, only complexes curated by\
# IntAct are used to generate links.
sub set_generate_interactions {
	my ($self, $generate_interactions) = @_;
	
	$self->generate_interactions($generate_interactions);
}

sub buildPart {
	my ($self) = @_;
	
	print STDERR "\n\nIntActDatabaseIdentifierToComplexOrReactionlikeEvent.buildPart: entered\n";
	
	$self->timer->start($self->timer_message);
	my $dba = $self->builder_params->refresh_dba();
	$self->generate_interactions(1);
	
	my $attribute = 'interactionIdentifier';
	$self->set_instance_edit_note("{$attribute}s inserted by IntActDatabaseIdentifierToComplexOrReactionlikeEvent");

	# BEGIN Imre's original code
	$dba->matching_instance_handler(new GKB::MatchingInstanceHandler::Simpler);
	$self->tweak_datamodel_if_necessary($attribute);
	my $intact_reactome_pair;
	foreach my $intact_reactome_pair (@{$self->get_intact_reactome_pairs()}) {
	    my ($intact_acc,$db_id) = @{$intact_reactome_pair};
	    
	    if (!(defined $db_id)) {
			print STDERR "IntActDatabaseIdentifierToComplexOrReactionlikeEvent.buildPart: WARNING - $intact_acc has no matching DB_ID\n";
	    	next;
	    }
	    
	    my $instance = undef;
	    my $stable_identifier = undef;
	    if ($db_id =~ /(REACT_[0-9]+)/) {
	    	# First check to see if ID is actually a stable
	    	# ID and use that to retrieve instance if so
	    	$stable_identifier = $1;
	    	my $instances = $dba->fetch_instance_by_remote_attribute('DatabaseObject',[['stableIdentifier.identifier','=',[$stable_identifier]]]);
	    	$instance = $instances->[0];
	    }
	    if (!$instance) {
	    	# Maybe ID really was a DB_ID - let's see:
	    	$instance = $dba->fetch_instance_by_db_id($db_id)->[0];
	    }
	    if (!$instance) {
			print STDERR "IntActDatabaseIdentifierToComplexOrReactionlikeEvent.buildPart: WARNING - $intact_acc\t$db_id\tno instance with matching DB_ID\n";
			next;
	    }
	    if (!($instance->is_a('Complex'))) {
			print STDERR "IntActDatabaseIdentifierToComplexOrReactionlikeEvent.buildPart: WARNING - $intact_acc\t$db_id\tnot a Complex\t", $instance->extended_displayName, "\n";
			next;
	    }
	    my $di = $self->builder_params->database_identifier->get_intact_database_identifier($intact_acc);
	    $instance->add_attribute_value($attribute, $di);
	    $dba->update_attribute($instance, $attribute);
		$instance->add_attribute_value('modified', $self->instance_edit);
		$dba->update_attribute($instance, 'modified');
	}
	# END Imre's original code
	
	if ($self->generate_interactions) {
	
		print STDERR "IntActDatabaseIdentifierToComplexOrReactionlikeEvent.buildPart: about to generate interactions\n";
		
		# If the -generate_interactions flag was given, then try to match interactions
		# generated on the basis of Reactome complexes and reactions with IntAct
		# interactions, and embed the IntAct IDs as links into the Reactome database.
		my $interaction_generator = new GKB::InteractionGenerator();
		$interaction_generator->set_dba($dba);
		$interaction_generator->add_intact_ids_flag(1);
	
		my $reference_peptide_sequences = $self->fetch_reference_peptide_sequences(0);
		
		unless (@{$reference_peptide_sequences}) {
		    print STDERR "IntActDatabaseIdentifierToComplexOrReactionlikeEvent.buildPart: WARNING - no " . $self->get_reference_protein_class() . " instances - is the species name correct?\n";
		    return;
		}

		my $idx;
		my $rs_count = @$reference_peptide_sequences;
		$interaction_generator->{rs_count} = $rs_count;
		for my $refpep (@$reference_peptide_sequences) {
		    # This will get IntAct IDs for interactions, but only for
		    # those arising from reactions or complexes involving 3 or
		    # fewer proteins.
		    my $interactions_hash = $interaction_generator->find_interactors_for_ReferenceSequences([$refpep], 3, undef, ++$idx);
		    next unless $interactions_hash;
		    
		    # Only insert cross-references for reactions or complexes involving 3 or
		    # fewer proteins.
		    # Note that insert_intact_xrefs deals with removing old IntAct
		    # cross references wherever it finds them, be it on Events or
		    # complexes.
		    $interaction_generator->insert_intact_xrefs($interactions_hash, 3);
		
		    print STDERR "IntActDatabaseIdentifierToComplexOrReactionlikeEvent.buildPart: done inserting xrefs\n";

#		    $interactions_hash = undef; # memory hog, force garbage collection
#		    print STDERR "IntActDatabaseIdentifierToComplexOrReactionlikeEvent.buildPart: garbage should now be collected\n";
		}
	}
	
	print STDERR "IntActDatabaseIdentifierToComplexOrReactionlikeEvent.buildPart: time to do some timing\n";

	$self->timer->stop($self->timer_message);
	$self->timer->print();
}

sub get_page {
	my ($self, $url) = @_;
    
	print STDERR "IntActDatabaseIdentifierToComplexOrReactionlikeEvent.buildPart: url=$url\n";
	
    my $ua = LWP::UserAgent->new();
    my $response = $ua->get($url);
    if($response->is_success) {
        return $response->content;
    }
    
	print STDERR "IntActDatabaseIdentifierToComplexOrReactionlikeEvent.buildPart: Failed to GET $url\n";
}

sub get_intact_reactome_pairs {
	my ($self) = @_;
	
    $ENV{FTP_PASSIVE} = 1;
    my $url = 'ftp://ftp.ebi.ac.uk/pub/databases/intact/current/various/reactome.dat';
    my $page = $self->get_page($url);
    my @lines = split(/\n/, $page);
    my @out;
    foreach my $line (@lines) {
		my ($intact_acc,$db_id) = split(/\s+/,$line);
		push @out, [$intact_acc,$db_id];
    }
    return \@out;
}

# This is a horrible hack till we get it prperly sorted
sub tweak_datamodel_if_necessary {
	my ($self, $attribute) = @_;
	
	my $dba = $self->builder_params->get_dba();
    if (!($dba->ontology->is_valid_class_attribute('Complex',$attribute))) {
		# Add the attribute
		$dba->ontology->_create_multivalue_attribute('Complex',$attribute,'db_instance_type');
		$dba->ontology->class_attribute_allowed_classes('Complex',$attribute,'DatabaseIdentifier');
		$dba->ontology->initiate;
		# Create the table
		$dba->create_multivalue_attribute_table('Complex',$attribute);
		# Store the new schema
		$dba->store_schema;
    }
}

1;

