=head1 NAME

GKB::AddLinks::IdentifierMappedReferenceDNASequenceToReferencePeptideSequence

=head1 SYNOPSIS

Adds gene entries to the referenceGene slots of ReferenceGeneProduct
instances with UniProt or ENSP identifiers.

=head1 DESCRIPTION

This is a base class; you should call the method "insert_xrefs" from within "buildPart"
in your implementation of this class.  This takes the arguments "attribute" and "output_db",
which allow you to decide where you want to put the links and which database you want to
link to.

=head1 SEE ALSO

GKB::DBAdaptor

=head1 AUTHOR

David Croft E<lt>croft@ebi.ac.ukE<gt>

Copyright (c) 2010 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

package GKB::AddLinks::IdentifierMappedReferenceDNASequenceToReferencePeptideSequence;

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

# Insert cross references into ReferenceGeneProduct instances in database.  Uses an
# ID mapper to find corresponding IDs from the database being linked to.  Arguments:
#
# attribute				The attribute into which the xrefs are inserted, e.g. 'referenceGene'
# output_db				The database being linked to, e.g. 'Wormbase'
# overwrite_old_xrefs	A flag - 0 if you want to keep pre-existing cross references of this type.
sub insert_xrefs {
	my ($self, $attribute, $output_db, $overwrite_old_xrefs) = @_;
	
	$self->timer->start($self->timer_message);
	my $dba = $self->builder_params->refresh_dba();

	my $limiting_species = $self->builder_params->get_species_name();

	my $hapmap_reference_database = $self->builder_params->reference_database->get_hapmap_reference_database();

	# Fetch RefrencePetideSequences to be "annotated".
	my $reference_peptide_sequences = $self->fetch_reference_peptide_sequences(1);
	
	# Load the values of an attribute to be updated. Not necessary for the 1st time though.
	$dba->load_class_attribute_values_of_multiple_instances($self->get_reference_protein_class(),$attribute,$reference_peptide_sequences);
	$dba->load_class_attribute_values_of_multiple_instances($self->get_reference_protein_class(),'identifier',$reference_peptide_sequences);
	$dba->load_class_attribute_values_of_multiple_instances($self->get_reference_protein_class(),'variantIdentifier',$reference_peptide_sequences);
	
	my %species_hit_count_hash = ();
	$self->set_instance_edit_note("${attribute}s inserted by " . $self->class_name);
	
	# Create a hash of ReferenceGeneProduct instances, keyed by
	# species and identifier or variant identifier (where available)
	my $accs = $self->create_reference_peptide_sequence_hash_variant_ids($reference_peptide_sequences);
	
	$self->builder_params->identifier_mapper->refresh();
	
	# Loop over all of the species discovered and do a mass-conversion of
	# all identifiers for each.
	my %need_to_update = ();
	my @input_ids;
	my $output_id_hash;
	my $refdb;
	my $species;
	my $output_id_hash_defined_flag;
	my %id_hash = ();
	my $reference_database;
	my $value;
	foreach $species (keys(%{$accs})) {
		if (defined $limiting_species && !($species eq $limiting_species)) {
			next;
		}
		
		print STDERR $self->class_name . ".insert_xrefs: dealing with species: $species\n";
		
		# Define the reference database inside the species iterator loop,
		# because in some cases, such as ENSEMBL, different databases are
		# used, depending on species·
		$reference_database = $self->builder_params->reference_database->get_reference_database($output_db, $species);
		if (!(defined $reference_database)) {
			next;
		}
		
		# Get rid of old xrefs of this type, if necessary.
		if ($overwrite_old_xrefs) {
			$self->remove_typed_instances_from_reference_peptide_sequence_hash($accs, $attribute, $reference_database, $species);
		}
		
		@input_ids = keys(%{$accs->{$species}});
		$output_id_hash_defined_flag = 0;
		$output_id_hash = $self->builder_params->identifier_mapper->convert_list('UniProt', \@input_ids, $output_db, $species);
		if (defined $output_id_hash) {
			$output_id_hash_defined_flag = 1;
			foreach my $acc (keys(%{$output_id_hash})) {
				foreach my $i (@{$accs->{$species}->{uc($acc)}}) {
					if (!$overwrite_old_xrefs && $self->exists_typed_instances_from_attribute($i, $attribute, $reference_database)) {
						# Don't insert any genes if there are some there
						# already.
						next;
					}
					foreach my $id (@{$output_id_hash->{$acc}}) {
				    	# Generally speaking, there will only be one
				    	# ReferencePeptideSequence instance associated
				    	# with a given accession, so this loop will only
				    	# be entered once.
				    	if ($attribute eq 'referenceGene') {
				    		$value = $self->builder_params->miscellaneous->get_reference_dna_sequence($i->species, $reference_database, $id);
				    	} elsif ($attribute eq 'referenceTranscript') {
				    		if ($species eq "Homo sapiens") {
				    			$value = $self->builder_params->miscellaneous->get_reference_rna_sequence($reference_database, $hapmap_reference_database, $id);
				    		}
				    	} else {
				    		$value = $self->builder_params->database_identifier->get_database_identifier($reference_database, $id);
				    	}
				    	if ((defined $attribute) && (defined $value)) {
						my $add_success = 0;
						eval {
							$add_success = $i->add_attribute_value_if_necessary($attribute, $value);
						};
						if ($add_success) {
#							print STDERR $self->class_name . ".insert_xrefs: inserting $id from UniProt for $acc\n";
							$self->increment_insertion_stats_hash($i->db_id);
							$id_hash{$id}++;
				    		} else {
				    			print STDERR "IdentifierMappedReferenceDNASequenceToReferencePeptideSequence.insert_xrefs: WARNING - add_attribute_value_if_necessary failed\n";
				    		}
				    	} else {
				    		if (!(defined $attribute) && (defined $value)) {
				    			print STDERR "IdentifierMappedReferenceDNASequenceToReferencePeptideSequence.insert_xrefs: WARNING - attribute undef, value=$value\n";
				    		} elsif ((defined $attribute) && !(defined $value)) {
				    			print STDERR "IdentifierMappedReferenceDNASequenceToReferencePeptideSequence.insert_xrefs: WARNING - attribute=$attribute=, value undef\n";
				    		} else {
				    			print STDERR "IdentifierMappedReferenceDNASequenceToReferencePeptideSequence.insert_xrefs: WARNING - both attribute and value undef\n";
				    		}
				    	}
				    }
				}
			}
		}
		$output_id_hash = $self->builder_params->identifier_mapper->convert_list('ENSP', \@input_ids, $output_db, $species);
		if (defined $output_id_hash) {
			$output_id_hash_defined_flag = 1;
			foreach my $acc (keys(%{$output_id_hash})) {
				foreach my $i (@{$accs->{$species}->{uc($acc)}}) {
					if (!$overwrite_old_xrefs && $self->exists_typed_instances_from_attribute($i, $attribute, $reference_database)) {
						# Don't insert any genes if there are some there
						# already.
						next;
					}
					foreach my $id (@{$output_id_hash->{$acc}}) {
				    	# Generally speaking, there will only be one
				    	# ReferencePeptideSequence instance associated
				    	# with a given accession, so this loop will only
				    	# be entered once.
				    	if ($attribute eq 'referenceGene') {
				    		$value = $self->builder_params->miscellaneous->get_reference_dna_sequence($i->species, $reference_database, $id);
				    	} elsif ($attribute eq 'referenceTranscript') {
				    		if ($species eq "Homo sapiens") {
				    			$value = $self->builder_params->miscellaneous->get_reference_rna_sequence($reference_database, $hapmap_reference_database, $id);
				    		}
				    	} else {
				    		$value = $self->builder_params->database_identifier->get_database_identifier($reference_database, $id);
				    	}
						if ($i->add_attribute_value_if_necessary($attribute, $value)) {
							print STDERR $self->class_name . ".insert_xrefs: inserting $id from ENSP for $acc\n";
							$self->increment_insertion_stats_hash($i->db_id);
							$id_hash{$id}++;
				    	}
				    }
				}
			}
		}
		if (!$output_id_hash_defined_flag) {
			if (!(defined $self->termination_status)) {
				$self->termination_status("no xrefs for species: ");
			}
			$self->termination_status($self->termination_status . "$species, ");
			next;
		}
		
		# Update to database.
		while (my ($acc,$ar) = each %{$accs->{$species}}) {
		    foreach my $i (@{$ar}) {
				print STDERR $self->class_name . ".insert_xrefs: for protein $acc adding following to database: \t", join(',',map {$_->displayName} @{$i->$attribute}), "\n";
#				if (scalar(@{$i->$attribute})>0 && $i->$attribute->[0]) {
				if ($self->insertion_stats_hash->{$i->db_id}) {
				    $i->add_attribute_value('modified', $self->instance_edit);
				    $dba->update_attribute($i, 'modified');
				    $dba->update_attribute($i, $attribute);
				    $species_hit_count_hash{$species}++;
				}
		    }
		}
	}
	
	# Summary diagnostics
	print STDERR $self->class_name . ".insert_xrefs: proteins for which references have been found, per species:\n";
	foreach $species (sort(keys(%species_hit_count_hash))) {
		print STDERR "\t$species: " . $species_hit_count_hash{$species} . "\n";
	}
	
	$self->print_insertion_stats_hash();
	
	print STDERR "\n";
	print STDERR $self->class_name . ".insert_xrefs: number of unique IDs inserted: " . scalar(keys(%id_hash)) . "\n";
	print STDERR "\n";

	$self->timer->stop($self->timer_message);
	$self->timer->print();
}

1;

