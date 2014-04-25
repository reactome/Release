=head1 NAME

GKB::AddLinks::UCSCReferenceDatabaseToReferencePeptideSequence

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

package GKB::AddLinks::UCSCReferenceDatabaseToReferencePeptideSequence;

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
	
	print STDERR "\n\nUCSCReferenceDatabaseToReferencePeptideSequence.buildPart: entered\n";
	
	$self->timer->start($self->timer_message);
	my $dba = $self->builder_params->refresh_dba();
	$dba->matching_instance_handler(new GKB::MatchingInstanceHandler::Simpler);
	
	my $reference_peptide_sequences = $self->fetch_reference_peptide_sequences(1);
	
	my $attribute = 'crossReference';
	$self->set_instance_edit_note("${attribute}s inserted by UCSCReferenceDatabaseToReferencePeptideSequence");
	
	# Load the values of an attribute to be updated. Not necessary for the 1st time though.
	$dba->load_class_attribute_values_of_multiple_instances('DatabaseIdentifier','identifier',$reference_peptide_sequences);

	my $ucsc_reference_database = $self->builder_params->reference_database->get_ucsc_reference_database();
	foreach my $reference_peptide_sequence (@{$reference_peptide_sequences}) {
		if (!($reference_peptide_sequence->species->[0]->name->[0] eq "Homo sapiens")) {
			# TODO: This mapping is currently not working properly for non-human species
			next;
		}
		
		print STDERR "UCSCReferenceDatabaseToReferencePeptideSequence.buildPart: i->Identifier=" . $reference_peptide_sequence->Identifier->[0] . "\n";
		
		# Remove UCSC gene identifiers to make sure the mapping is up-to-date, keep others
		# this isn't really necessary as long as the script is run on the slice only
		# But a good thing to have if you need to run the script a second time.
	    $self->remove_typed_instances_from_attribute($reference_peptide_sequence, $attribute, $ucsc_reference_database);
	    
	    # We are expecting a 1 to 1 mapping between UniProt and
	    # UCSC identifiers.
	    my $ucsc_database_identifier = $self->builder_params->database_identifier->get_ucsc_database_identifier($reference_peptide_sequence->Identifier->[0]);
	    $reference_peptide_sequence->add_attribute_value($attribute, $ucsc_database_identifier);
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

