=head1 NAME

GKB::AddLinks::OrphanetToUniprotReferenceDNASequence

=head1 SYNOPSIS

=head1 DESCRIPTION

This class extracts uniprot - Orphanet gene pairs from the genes_diseases_external_references.xml
file, obtained from the Orphanet website.
Then uses the UniProt IDs in the existing Reactome database to find corresponding
Orphanet IDs, and adds those Orphanet entries to Reactome.

=head1 SEE ALSO

GKB::DBAdaptor

=head1 AUTHOR

David Croft E<lt>croft@ebi.ac.ukE<gt>

Copyright (c) 2013 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

package GKB::AddLinks::OrphanetToUniprotReferenceDNASequence;

use GKB::Config;
use GKB::AddLinks::Builder;
use Data::Dumper;
use IO::Uncompress::Gunzip qw(gunzip $GunzipError);
use strict;
use vars qw(@ISA $AUTOLOAD %ok_field);
require XML::Simple;

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
	
	print STDERR "\n\nOrphanetToUniprotReferenceDNASequence.buildPart: entered\n";
	$self->class_name("OrphanetToUniprotReferenceDNASequence");
	
	$self->timer->start($self->timer_message);
	my $dba = $self->builder_params->refresh_dba();

	# Get file from Orphanet mapping genes to proteins
	my $mapping_file = $self->get_mapping_file();
	
	if (!(defined $mapping_file)) {
		print STDERR "OrphanetToUniprotReferenceDNASequence.buildPart: WARNING - mapping file is undef\n";
    	$self->termination_status("mapping file is undef");
    	return;		
	}
	print STDERR "OrphanetToUniprotReferenceDNASequence.buildPart: mapping_file=$mapping_file\n";
	if (!(-e $mapping_file)) {
		print STDERR "OrphanetToUniprotReferenceDNASequence.buildPart: WARNING - missing mapping file\n";
    	$self->termination_status("missing mapping file");
    	return;		
	}
	if (-s $mapping_file == 0) {
		print STDERR "OrphanetToUniprotReferenceDNASequence.buildPart: WARNING - mapping file has zero length\n";
    	$self->termination_status("mapping file has zero length");
    	return;		
	}
	
	# Create a hash mapping UniProt IDs (keys) onto Orphanet IDs (values)
	# TODO: it might be more efficient to create a hash of the UniProt
	# IDs known to Reactome and then loop over the Orphanet IDs instead.
	my %gene;
	my $uniprot_id;
	my $i;
        my $xml_parser = XML::Simple->new();
        my $mapping_hash = $xml_parser->XMLin($mapping_file);
        my $orphanet_gene_hash = $mapping_hash->{"GeneList"}->{"Gene"};
	foreach my $orphanet_gene_id (sort(keys(%{$orphanet_gene_hash}))) {
	    my $orphanet_gene = $orphanet_gene_hash->{$orphanet_gene_id};
	    my $external_reference_list = $orphanet_gene->{"ExternalReferenceList"};
	    my $external_reference = $external_reference_list->{"ExternalReference"};
	    foreach my $external_reference_id (sort(keys(%{$external_reference}))) {
	        if ($external_reference_id eq "Reference" || $external_reference_id eq "Source") {
	            next;
	        }
	        my $external_reference_object = $external_reference->{$external_reference_id};
	        if (!(scalar($external_reference_object) =~ /HASH/)) {
	            next;
	        }
	        my $external_reference_source = $external_reference_object->{"Source"};
	        if ($external_reference_source eq "Reactome") {
	            my $uniprot_id = $external_reference_object->{"Reference"}; # Assume UniProt ID
	    	    push(@{$gene{$uniprot_id}}, $orphanet_gene_id);
	    	    print STDERR "OrphanetToUniprotReferenceDNASequence.buildPart: external_reference_id=$external_reference_id, uniprot_id=$uniprot_id\n";
	            last;
	        }
	    }
	}

	my $attribute = 'referenceGene';
	$self->set_instance_edit_note("${attribute}s inserted by OrphanetToUniprotReferenceDNASequence");
	
	# Retrieve all UniProt entries from Reactome
	my $reference_peptide_sequences = $self->fetch_reference_peptide_sequences(1);

	my $gene_db = $self->builder_params->reference_database->get_orphanet_reference_database();
	my $reference_peptide_sequence;
	my $identifier;
	foreach $reference_peptide_sequence (@{$reference_peptide_sequences}) {
	    $reference_peptide_sequence->inflate;
	    $identifier = $reference_peptide_sequence->Identifier->[0];
	    my $gene_ids_ref = $gene{$identifier};
	    if (!(defined $gene_ids_ref)) {
	        next;
	    }
	    my @gene_ids = @{$gene_ids_ref};

		print STDERR "OrphanetToUniprotReferenceDNASequence.buildPart: gene ID count: " . scalar(@gene_ids) . "\n";
		print STDERR "OrphanetToUniprotReferenceDNASequence.buildPart: reference_peptide_sequence->Identifier=$identifier; gene_ids=[";

		# Remove Orphanet gene identifiers to make sure the mapping is up-to-date, keep others
		# this isn't really necessary as long as the script is run on the slice only
		# But a good thing to have if you need to run the script a second time.
	    $self->remove_typed_instances_from_attribute($reference_peptide_sequence, $attribute, $gene_db);
	    
		#create ReferenceDNASequence for Orphanet gene id
	    foreach my $gene_id (@{$gene{$identifier}}) {
			print STDERR "$gene_id,";
			my $rds = $self->builder_params->miscellaneous->get_reference_dna_sequence($reference_peptide_sequence->Species, $gene_db, $gene_id);
			$self->check_for_identical_instances($rds);
			$reference_peptide_sequence->add_attribute_value($attribute, $rds);
			$self->increment_insertion_stats_hash($reference_peptide_sequence->db_id);
	    }
		print STDERR "]\n";

	    $reference_peptide_sequence->add_attribute_value('modified', $self->instance_edit);
	    $dba->update_attribute($reference_peptide_sequence, $attribute);
	}
	
	$self->print_insertion_stats_hash();
	
	$self->timer->stop($self->timer_message);
	$self->timer->print();
}

# Get file from data provider website mapping genes to proteins
sub get_mapping_file {
    my ($self) = @_;

	my $cmd;
	my $tmp_dir = $self->get_tmp_dir();
	my $filename = "genes_diseases_external_references.xml";
	my $path = "$tmp_dir/$filename";
	my $old_path = "$path.old";
	
	# If the file is outdated, move it to a backup location - we
	# might need it again later.
	if ((-e $path) && int(-M $path) > 14) {
		print STDERR "OrphanetToUniprotReferenceDNASequence.buildPart: $path is older than 14 days\n";
		if ((-s $path > 0)) {
		    $cmd = "mv $path $old_path";
		    if (system($cmd) != 0) {
			print STDERR "OrphanetToUniprotReferenceDNASequence.buildPart: WARNING: $cmd failed\n";
		    }
		}
	}
	
	if (!(-e $path) || (-s $path == 0)) {
		# Get file from Orphanet mapping genes to proteins, if there
		# is no pre-existing file less than two weeks old.
		$cmd = "wget -O $path --user=Reactome --password='$GKB::Config::ORPHAN_PWD'  http://www.orphadata.org/data/BDD_partners/$filename";
		print STDERR "OrphanetToUniprotReferenceDNASequence.buildPart: cmd=$cmd\n";
		if (system($cmd) != 0) {
			print STDERR "OrphanetToUniprotReferenceDNASequence.buildPart: WARNING: $cmd failed\n";
			if ((-e $old_path)) {
				my $cmd = "cp $path.old $path";
				if (system($cmd) != 0) {
					print STDERR "OrphanetToUniprotReferenceDNASequence.buildPart: WARNING: $cmd failed\n";
				}
			} else {
				# If there is no pre-existing file on the disk, then
				# give up completely.
				return undef;
			}
		}
		$cmd = "touch $path";
		if (system($cmd) != 0) {
			print STDERR "OrphanetToUniprotReferenceDNASequence.buildPart: WARNING: $cmd failed\n";
		}
	}
	
	# Remove the backup, since we now have a valid file.
	if (-e $path) {
		$cmd = "rm -f $old_path";
		if (system($cmd) != 0) {
			print STDERR "OrphanetToUniprotReferenceDNASequence.buildPart: WARNING: $cmd failed\n";
		}
	}
	
	return $path;
}

1;

