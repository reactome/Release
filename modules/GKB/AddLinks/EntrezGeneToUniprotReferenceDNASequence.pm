=head1 NAME

GKB::AddLinks::EntrezGeneToUniprotReferenceDNASequence

=head1 SYNOPSIS

Adds Entrez gene entries to the referenceGene slots of ReferenceGeneProduct
instances with UniProt or ENSP identifiers.

=head1 DESCRIPTION

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

package GKB::AddLinks::EntrezGeneToUniprotReferenceDNASequence;

use GKB::Config;
use GKB::AddLinks::IdentifierMappedReferenceDNASequenceToReferencePeptideSequence;
use strict;
use vars qw(@ISA $AUTOLOAD %ok_field);

@ISA = qw(GKB::AddLinks::IdentifierMappedReferenceDNASequenceToReferencePeptideSequence);

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

# New code uses ENSEMBL Mart to do the job - much faster.
sub buildPart {
	my ($self) = @_;
	
	$self->class_name("EntrezGeneToUniprotReferenceDNASequence");
	
	print STDERR "\n\n" . $self->class_name . ".buildPart: entered\n";
	
	$self->insert_xrefs('referenceGene', 'Entrez Gene', 1);
}

# This subroutine extracts uniprot - entrez gene pairs from the gene2accession file from NCBI.
# Gets the file via ftp:   ftp ftp.ncbi.nih.gov  gene/DATA/gene2accession.gz.
# Then uses the UniProt IDs in the existing Reactome database to find corresponding
# EntrezGene IDs, and adds those EntrezGene entries to Reactome.
#
# This mostly only works for human, because that's the only species where you
# can guarantee UniProt IDs for proteins.  It used to work for mouse too, with
# the old OrthoMCL, but the latest OrthoMCL delivers ENSMU rather than UniProt
# IDs, and this script doesn't know how to handle ENSMU.
#
# Original code lifted from the script addEntrezLinks.pl, probably from Esther.
sub old_buildPart {
	my ($self) = @_;
	
	print STDERR "\n\nEntrezGeneToUniprotReferenceDNASequence.buildPart: entered\n";
	
	$self->timer->start($self->timer_message);
	my $dba = $self->builder_params->refresh_dba();

	my $reference_peptide_sequences = $self->fetch_reference_peptide_sequences(0);
	my $reference_database = $self->builder_params->reference_database->get_entrez_gene_reference_database();
	
	my @attributes = ['referenceGene', 'otherIdentifier'];
	$self->set_instance_edit_note("@attributes inserted by EntrezGeneToUniprotReferenceDNASequence");
	
	# Create a hash, mapping protein IDs onto arrays of
	# ReferencePeptideSequence instances.
	my $reference_peptide_sequence;
	my $identifier;
	my %reference_peptide_sequence_hash = ();
	foreach $reference_peptide_sequence (@{$reference_peptide_sequences}) {
	    $reference_peptide_sequence->inflate();
	    $identifier = $reference_peptide_sequence->Identifier->[0];
	    if (!(defined $identifier)) {
	    	print STDERR "EntrezGeneToUniprotReferenceDNASequence.run: WARNING - UniProt instance with DB_ID=" .  $reference_peptide_sequence->db_id() . " doesn't have an identifier.\n";
	    	next;
	    }
	    
		# Remove entrez gene identifiers to make sure the mapping is up-to-date, keep others
		# this isn't really necessary as long as the script is run on the slice only
		# But a good thing to have if you need to run the script a second time.
	    foreach my $attribute (@attributes) {
	        $self->remove_typed_instances_from_attribute($reference_peptide_sequence, $attribute, $reference_database);
	    }
	    
	    # Assume that this ReferencePeptideSequence is going to get
	    # changed, even if we don't know that for sure.
	    # TODO: it would be good to keep track of which RPSs *really*
	    # get changed, and only set the modified slot for those.
	    $reference_peptide_sequence->add_attribute_value('modified', $self->instance_edit);
	    $dba->update_attribute($reference_peptide_sequence, 'modified');

		push(@{$reference_peptide_sequence_hash{$identifier}}, $reference_peptide_sequence);
	}

	my $tmp_dir = $self->get_tmp_dir();
	my $compressed_filename = "$tmp_dir/gene2accession.gz";
	
	print STDERR "EntrezGeneToUniprotReferenceDNASequence.buildPart: compressed_filename$compressed_filename\n";
	print STDERR "EntrezGeneToUniprotReferenceDNASequence.buildPart: int(-M compressed_filename)=" . int(-M $compressed_filename) . "\n";
	
	if (!(-e $compressed_filename) || int(-M $compressed_filename) > 14) {
		# Get file from Entrez mapping genes to proteins, if there
		# is no pre-existing file less than two weeks old.
		unlink($compressed_filename);
		my $cmd = "wget -P $tmp_dir --passive-ftp ftp://ftp.ncbi.nih.gov/gene/DATA/gene2accession.gz";
		if (system($cmd) != 0) {
			print STDERR "EntrezGeneToUniprotReferenceDNASequence.buildPart: $cmd failed: $!";
			return;
		}
	}
	
	print STDERR "EntrezGeneToUniprotReferenceDNASequence.buildPart: reading from ID map file\n";
	
	if (!(-e $compressed_filename)) {
		print STDERR "EntrezGeneToUniprotReferenceDNASequence.buildPart: WARNING - $compressed_filename does not exist, maybe there was a problem downloading it from the NCBI?\n";
		return;
	}
	if (!open (IN, "gunzip -c $compressed_filename|")) {
		print STDERR "EntrezGeneToUniprotReferenceDNASequence.buildPart: WARNING - could not unzip $compressed_filename, aborting!!\n";
		return;
	}
	# Loop over the entire contents of the Entrez dump file and check
	# each protein ID to see if it corresponds to anything known to
	# Reactome.
	my $line_num = 0;
	while (<IN>) {
	    chomp;
	    
	    if ($_ =~ /^#/) {
	    	# Skip comment lines
	    	next;
	    }
	    my ($tax, $gene, $c, $d, $e, $prot) = split(/\t/, $_);
	    if (!(defined $prot)) {
	    	# Skip dodgy lines, but warn user
	    	print STDERR "EntrezGeneToUniprotReferenceDNASequence.run: WARNING - current line from gene2accession seems to have no prot field\n";
	    	print STDERR "EntrezGeneToUniprotReferenceDNASequence.run: line=" . $_ . "\n";
	    	print STDERR "EntrezGeneToUniprotReferenceDNASequence.run: skipping to next line\n";
	    	next;
	    }
	    chomp($prot);
	    $prot =~ s/\.\d+$//;
	    if ($prot eq '-') {
	    	# Lines that don't have an associated protein ID
	    	# are no use for mapping purposes, so ignore.
	    	next;
	    }
	    
	    my $reference_peptide_sequence_list = $reference_peptide_sequence_hash{$prot};
	    if (defined $reference_peptide_sequence_list) {
	    	foreach my $reference_peptide_sequence (@{$reference_peptide_sequence_list}) {
	    		print STDERR "EntrezGeneToUniprotReferenceDNASequence.run: inserting gene into hash, line_num=$line_num, prot=|$prot|, gene=$gene\n";
				my $rds = $self->builder_params->miscellaneous->get_reference_dna_sequence($reference_peptide_sequence->Species, $reference_database, $gene);
				$self->check_for_identical_instances($rds);
				foreach my $attribute (@attributes) {
				    $reference_peptide_sequence->add_attribute_value($attribute, $rds);
				}
				
				$self->increment_insertion_stats_hash($reference_peptide_sequence->db_id);
	    	}
		}
		
	    $line_num++;
	}
	close(IN);
	
	# Make sure the newly inserted genes also get put into the database.
	foreach $reference_peptide_sequence (@{$reference_peptide_sequences}) {
		foreach my $attribute (@attributes) {
		    $dba->update_attribute($reference_peptide_sequence, $attribute);
		}
	}
	
	$self->print_insertion_stats_hash();
	
	$self->timer->stop($self->timer_message);
	$self->timer->print();
}

1;

