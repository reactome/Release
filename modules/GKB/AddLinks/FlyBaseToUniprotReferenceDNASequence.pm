=head1 NAME

GKB::AddLinks::FlyBaseToUniprotReferenceDNASequence

=head1 SYNOPSIS

=head1 DESCRIPTION

This class extracts uniprot - FlyBase gene pairs from the fbgn_NAseq_Uniprot_fb_*.tsv
file, obtained from the FlyBase website.
Then uses the UniProt IDs in the existing Reactome database to find corresponding
FlyBase IDs, and adds those FlyBase entries to Reactome.

=head1 SEE ALSO

GKB::DBAdaptor

=head1 AUTHOR

David Croft E<lt>croft@ebi.ac.ukE<gt>

Copyright (c) 2009 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

package GKB::AddLinks::FlyBaseToUniprotReferenceDNASequence;

use GKB::Config;
use GKB::AddLinks::Builder;
use Data::Dumper;
use IO::Uncompress::Gunzip qw(gunzip $GunzipError);
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
	
	print STDERR "\n\nFlyBaseToUniprotReferenceDNASequence.buildPart: entered\n";
	$self->class_name("FlyBaseToUniprotReferenceDNASequence");
	
	$self->timer->start($self->timer_message);
	my $dba = $self->builder_params->refresh_dba();

	# Get file from FlyBase mapping genes to proteins
	my $mapping_file = $self->get_mapping_file();
	
	if (!(defined $mapping_file)) {
		print STDERR "FlyBaseToUniprotReferenceDNASequence.buildPart: WARNING - mapping file is undef\n";
    	$self->termination_status("mapping file is undef");
    	return;		
	}
	print STDERR "FlyBaseToUniprotReferenceDNASequence.buildPart: mapping_file=$mapping_file\n";
	if (!(-e $mapping_file)) {
		print STDERR "FlyBaseToUniprotReferenceDNASequence.buildPart: WARNING - missing mapping file\n";
    	$self->termination_status("missing mapping file");
    	return;		
	}
	if (-s $mapping_file == 0) {
		print STDERR "FlyBaseToUniprotReferenceDNASequence.buildPart: WARNING - mapping file has zero length\n";
    	$self->termination_status("mapping file has zero length");
    	return;		
	}
	
	if (!open(FILE, $mapping_file)) {
		print STDERR "FlyBaseToUniprotReferenceDNASequence.buildPart: WARNING - could not open mapping file: $mapping_file\n";
    	$self->termination_status("could not open mapping file: $mapping_file");
    	return;
    }
	# Create a hash mapping UniProt IDs (keys) onto FlyBase IDs (values)
	# TODO: it might be more efficient to create a hash of the UniProt
	# IDs known to Reactome and then loop over the FlyBase IDs instead.
	my %gene;
	my $uniprot_id;
	my $i;
    my @cells;
    while (<FILE>) {
    	@cells = split(/\t/, $_);
	    if (scalar(@cells) < 5) {
	    	next;
	    }
		$uniprot_id = $cells[4];
	    if (!(defined $uniprot_id) || $uniprot_id eq '' || $uniprot_id =~ /^[ \t\n]+$/) {
		print STDERR "FlyBaseToUniprotReferenceDNASequence.buildPart: cant extract UniProt ID from line from file=" . $_ . "\n";
	    } else {
		$uniprot_id =~ s/\n+$//;
		print STDERR "FlyBaseToUniprotReferenceDNASequence.buildPart: uniprot_id=|$uniprot_id|\n";
	    	push(@{$gene{$uniprot_id}}, $cells[1]);
	    }
	}
    close(FILE);
	
	# Diagnostics
	my @keys = keys(%gene);
	my @values = values(%gene);
	print STDERR "FlyBaseToUniprotReferenceDNASequence.buildPart: keys(gene)=@keys\n";
	print STDERR "FlyBaseToUniprotReferenceDNASequence.buildPart:";
	foreach my $value (@values) {
		my @value_list = @{$value};
		print STDERR "[@value_list], ";
	}
	print STDERR "\n";
	
	my $attribute = 'referenceGene';
	$self->set_instance_edit_note("${attribute}s inserted by FlyBaseToUniprotReferenceDNASequence");
	
	# Retrieve all UniProt entries from Reactome
	my $reference_peptide_sequences = $self->fetch_reference_peptide_sequences(1);

	my $gene_db = $self->builder_params->reference_database->get_flybase_reference_database();
	my $reference_peptide_sequence;
	my $identifier;
	foreach $reference_peptide_sequence (@{$reference_peptide_sequences}) {
	    $reference_peptide_sequence->inflate;
	    $identifier = $reference_peptide_sequence->Identifier->[0];
#	    if (!(defined $identifier)) {
#	    	print STDERR "FlyBaseToUniprotReferenceDNASequence.buildPart: WARNING - UniProt instance with DB_ID=" .  $reference_peptide_sequence->db_id() . " doesn't have an identifier.\n";
#	    	next;
#	    }

		print STDERR "FlyBaseToUniprotReferenceDNASequence.buildPart: reference_peptide_sequence->Identifier=$identifier; gene_ids=[";

		# Remove FlyBase gene identifiers to make sure the mapping is up-to-date, keep others
		# this isn't really necessary as long as the script is run on the slice only
		# But a good thing to have if you need to run the script a second time.
	    $self->remove_typed_instances_from_attribute($reference_peptide_sequence, $attribute, $gene_db);
	    
		#create ReferenceDNASequence for FlyBase gene id
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
	my $filename = "fbgn_NAseq_Uniprot_fb.tsv";
	my $wildcard_filename = "fbgn_NAseq_Uniprot_fb_*.tsv";
	my $path = "$tmp_dir/$filename";
	my $mapping_file = $self->comments_free_filename($path);
	my $compressed_path = "$path.gz";
	my $old_compressed_path = "$compressed_path.old";
	if (-e $path) {
		unlink($path);
	}
	
	# If the file is outdated, move it to a backup location - we
	# might need it again later.
	if ((-e $compressed_path) && int(-M $compressed_path) > 14) {
		print STDERR "FlyBaseToUniprotReferenceDNASequence.buildPart: $compressed_path is older than 14 days\n";
		if ( -s $compressed_path > 0 ) {
			print STDERR "FlyBaseToUniprotReferenceDNASequence.buildPart: $compressed_path is not empty\n";
			$cmd = "mv $compressed_path $old_compressed_path";
			if (system($cmd) != 0) {
				print STDERR "FlyBaseToUniprotReferenceDNASequence.buildPart: WARNING: $cmd failed\n";
			}
		}
	}
	
	if (!(-e $compressed_path) || (-s $compressed_path == 0)) {
		# Get file from FlyBase mapping genes to proteins, if there
		# is no pre-existing file less than two weeks old.
#		$cmd = "wget -t 5 -P $tmp_dir http://flybase.org/static_pages/downloads/FB2008_10/genes/$filename.gz";
#		$cmd = "wget -t 5 -P $tmp_dir -O $filename.gz ftp://flybase.org/releases/current/precomputed_files/genes/$wildcard_filename.gz";
		$cmd = "wget -t 5 -O $compressed_path ftp://flybase.org/releases/current/precomputed_files/genes/$wildcard_filename.gz";
		print STDERR "FlyBaseToUniprotReferenceDNASequence.buildPart: cmd=$cmd\n";
		if (system($cmd) != 0) {
			print STDERR "FlyBaseToUniprotReferenceDNASequence.buildPart: WARNING: $cmd failed\n";
			if ((-e $old_compressed_path)) {
				print STDERR "FlyBaseToUniprotReferenceDNASequence.buildPart: using $compressed_path.old instead\n";
				my $cmd = "mv $compressed_path.old $compressed_path";
				if (system($cmd) != 0) {
					print STDERR "FlyBaseToUniprotReferenceDNASequence.buildPart: WARNING: $cmd failed\n";
				}
			} else {
				print STDERR "FlyBaseToUniprotReferenceDNASequence.buildPart: $old_compressed_path does not exist, giving up completely\n";
				# If there is no pre-existing file on the disk, then
				# give up completely.
				return undef;
			}
		}
		$cmd = "touch $compressed_path";
		if (system($cmd) != 0) {
			print STDERR "FlyBaseToUniprotReferenceDNASequence.buildPart: WARNING: $cmd failed\n";
		}
	}
	
	gunzip($compressed_path => $path);
	if (!(-e $path)) {
		print STDERR "FlyBaseToUniprotReferenceDNASequence.buildPart: WARNING: $path does not exist!\n";
		return undef;
	}
	
	$mapping_file = $self->remove_comments_from_file($path);
	
	return $mapping_file;
}

# Takes the named file and removes any initial empty or comment lines.
# The output is written to a new file with the extension .nc.
# Returns the output file name if it completes successfully,
# otherwise returns undef.
sub remove_comments_from_file {
    my ($self, $path) = @_;

	my $comment_free_path = $self->comments_free_filename($path);
    unless (open(READ_FILE, "<$path")) {
 		print STDERR "remove_comments_from_file: WARNING - problem opening file $path\n";
		$self->termination_status("problem opening file $path");
		return $comment_free_path;
    }
    unless (open(WRITE_FILE, ">$comment_free_path")) {
		print STDERR "remove_comments_from_file: WARNING - problem opening file $comment_free_path for writing\n";
		$self->termination_status("problem opening file $comment_free_path for writing, nonlethal if previous copy exists");
		return $comment_free_path;
    }

	my $line;
    while (<READ_FILE>) {
    	$line = $_;
    	if ($line =~ /^#/) {
    		# Ignore comments
    		next;
    	}
    	if ($line =~ /^\n*$/) {
    		# Ignore empty lines
    		next;
    	}
	    print WRITE_FILE $line;
    }

    close(READ_FILE);
    close(WRITE_FILE);
    
    return $comment_free_path;
}

sub comments_free_filename {
    my ($self, $file_name) = @_;

	return "$file_name.nc";
}

1;

