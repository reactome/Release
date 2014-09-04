=head1 NAME

GKB::AddLinks::PROToReferencePeptideSequence

=head1 SYNOPSIS

=head1 DESCRIPTION


Original code lifted from the script add_ucsc_links.pl, probably from Imre.

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

package GKB::AddLinks::PROToReferencePeptideSequence;

use GKB::Config;
use GKB::AddLinks::Builder;
use strict;
use vars qw(@ISA $AUTOLOAD %ok_field);

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

sub buildPart {
    my ($self) = @_;
	
    my $pkg = __PACKAGE__;
    
    print STDERR "\n\n$pkg.buildPart: entered\n";
	
    $self->timer->start($self->timer_message);
    my $dba = $self->builder_params->refresh_dba();
    $dba->matching_instance_handler(new GKB::MatchingInstanceHandler::Simpler);
	
    my $reference_peptide_sequences = $self->fetch_reference_peptide_sequences(1);
	
    my $attribute = 'crossReference';
    $self->set_instance_edit_note("${attribute}s inserted by $pkg");
	
    # Load the values of an attribute to be updated. Not necessary for the 1st time though.
    $dba->load_class_attribute_values_of_multiple_instances('DatabaseIdentifier','identifier',$reference_peptide_sequences);

    my $pro_reference_database = $self->builder_params->reference_database->get_genecards_reference_database();
    foreach my $reference_peptide_sequence (@{$reference_peptide_sequences}) {
	print STDERR "$pkg.buildPart: i->Identifier=" . $reference_peptide_sequence->Identifier->[0] . "\n";
		
	# Remove identifiers to make sure the mapping is up-to-date, keep others
	# this isn't really necessary as long as the script is run on the slice only
	# But a good thing to have if you need to run the script a second time.
	$self->remove_typed_instances_from_attribute($reference_peptide_sequence, $attribute, $pro_reference_database);
	    
	my $pro_database_identifier = $self->builder_params->database_identifier->get_pro_database_identifier($reference_peptide_sequence->Identifier->[0]);
	$reference_peptide_sequence->add_attribute_value($attribute, $pro_database_identifier);
	$dba->update_attribute($reference_peptide_sequence,$attribute);
	$reference_peptide_sequence->add_attribute_value('modified', $self->instance_edit);
	$dba->update_attribute($reference_peptide_sequence, 'modified');
	$self->increment_insertion_stats_hash($reference_peptide_sequence->db_id());
    }
	
    $self->print_insertion_stats_hash();
	
    $self->timer->stop($self->timer_message);
    $self->timer->print();
}

1;

