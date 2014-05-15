=head1 NAME

GKB::AddLinks::EnsemblGeneToUniprotReferencePeptideSequence

=head1 SYNOPSIS

Adds ENSEMBL genes to referenceGene slots of ReferenceGeneProduct
instances with Uniprot identifiers.

=head1 DESCRIPTION

Original code lifted from the script addEnsemblGene2swallSdi.pl, probably from Esther.
Very little of this now survives....

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

package GKB::AddLinks::EnsemblGeneToUniprotReferencePeptideSequence;

use GKB::AddLinks::Builder;
use GKB::SOAPServer::PICR;
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
   	
    my %species_database = ();
   	$self->species_database(\%species_database);

    return $self;
}

# Needed by subclasses to gain access to object variables defined in
# this class.
sub get_ok_field {
	my ($pkg) = @_;
	
	%ok_field = $pkg->SUPER::get_ok_field();
	$ok_field{"species_database"}++;
	$ok_field{"mapper"}++;

	return %ok_field;
}

sub clear_variables {
    my ($self) = @_;
    
	$self->SUPER::clear_variables();
	
	$self->species_database(undef);
	$self->mapper(undef);
}

sub buildPart {
	my ($self) = @_;
	
	print STDERR "\n\nEnsemblGeneToUniprotReferencePeptideSequence.buildPart: entered\n";
	
	$self->timer->start($self->timer_message);
	my $dba = $self->builder_params->refresh_dba();
	
	my $limiting_species = $self->builder_params->get_species_name();

	# Retrieve all UniProt entries from Reactome
	my $reference_peptide_sequences = $self->fetch_reference_peptide_sequences(1);
	
	my %species_hit_count_hash = ();
	my $attribute = 'referenceGene';
	$self->set_instance_edit_note("${attribute}s inserted by EnsemblGeneToUniprotReferencePeptideSequence");
	
	# Create a hash of ReferencePeptideSequence instances, keyed by
	# species and UniProt identifier
#	my $accs = $self->create_reference_peptide_sequence_hash($reference_peptide_sequences);
	my $accs = $self->create_reference_peptide_sequence_hash_variant_ids($reference_peptide_sequences);
	
	# Loop over all of the species discovered and do a mass-conversion of
	# all identifiers for each.
	my %need_to_update = ();
	my @input_ids;
	my $output_id_hash;
	my $reference_database;
	my $species;
	foreach $species (keys(%{$accs})) {
		if (defined $limiting_species && !($species eq $limiting_species)) {
			next;
		}
		
		print STDERR "EnsemblGeneToUniprotReferencePeptideSequence.buildPart: dealing with species: $species\n";
		
	    $reference_database = $self->get_ensembldb($species);
	    if ($reference_database eq 'none') {
			print STDERR "EnsemblGeneToUniprotReferencePeptideSequence.buildPart: WARNING - $species is not an ensembl species\n";
			next;
	    }

		# Put this inside the loop, because the reference DB is species-dependent
		# for ENSEMBL!
		$self->remove_typed_instances_from_reference_peptide_sequence_hash($accs, $attribute, $reference_database, $species);

		@input_ids = keys(%{$accs->{$species}});
		$output_id_hash = $self->builder_params->identifier_mapper->convert_list('UniProt', \@input_ids, 'ENSEMBL', $species);
		if (!(defined $output_id_hash)) {
			next;
		}
		foreach my $acc (keys(%{$output_id_hash})) {
			foreach my $id (@{$output_id_hash->{$acc}}) {
			    foreach my $i (@{$accs->{$species}->{uc($acc)}}) {
			    	# Generally speaking, there will only be one
			    	# ReferencePeptideSequence instance associated
			    	# with a given accession, so this loop will only
			    	# be entered once.
					if ($i->add_attribute_value_if_necessary($attribute, $self->builder_params->miscellaneous->get_reference_dna_sequence($i->species, $reference_database, $id))) {
				    	$need_to_update{$i->db_id}++;
						$self->increment_insertion_stats_hash($i->db_id);
					}
			    }
			}
		}
	
		# Update to database.
		while (my ($acc,$ar) = each %{$accs->{$species}}) {
		    foreach my $i (@{$ar}) {
				if ($need_to_update{$i->db_id}) {
				    print STDERR "EnsemblGeneToUniprotReferencePeptideSequence.buildPart: $acc\t", join(',',map {$_->identifier->[0]} @{$i->referenceGene}), "\n";
				    $i->add_attribute_value('modified', $self->instance_edit);
				    $dba->update_attribute($i, 'modified');
				    $dba->update_attribute($i, $attribute);
				    $species_hit_count_hash{$species}++;
				}
		    }
		}
	}
	
	# Summary diagnostics
	print STDERR "RefseqReferenceDatabaseToReferencePeptideSequence.buildPart: proteins for which references have been found, per species:\n";
	foreach $species (sort(keys(%species_hit_count_hash))) {
		print STDERR "\t$species: " . $species_hit_count_hash{$species} . "\n";
	}
	
	$self->print_insertion_stats_hash();
	
	$self->timer->stop($self->timer_message);
	$self->timer->print();
}

# Returns an ENSEMBL ReferenceDatabase object for the species $spec.
# If the species is not known, return the string 'none'.
sub get_ensembldb {
	my ($self, $spec) = @_;
	
	my $species_database = $self->species_database;
	
    $species_database->{$spec} && return $species_database->{$spec};
    
    # Exclude 'strange' species names not found in ensembl anyway.
    if (!($spec =~ /^\w+\s+\w+$/)) {
		return 'none';
    }

	# Get/make ensembldb
    my $species = $spec;
    $species =~ s/(\w+) (\w+)/$1\_$2/; #needed for Ensembl URL

    my $ensg_database = $self->builder_params->reference_database->get_ensembl_reference_database($species);
    if (!(defined $ensg_database)) {
    	$ensg_database = 'none';
    }
    $species_database->{$spec} = $ensg_database;
    return $species_database->{$spec};
}

1;

