=head1 NAME

GKB::AddLinks::DOCKBlasterToUniprotDatabaseIdentifier

=head1 SYNOPSIS

=head1 DESCRIPTION

Takes existing PDB DatabaseIdentifiers associated with ReferenceGeneProducts
and clones them into DatabaseIdentifiers for DOCK Blaster.  This means you
need to run the script for inserting PDB links before you run this one.

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

package GKB::AddLinks::DOCKBlasterToUniprotDatabaseIdentifier;

use LWP::UserAgent;
use GKB::Config;
use GKB::AddLinks::Builder;
use GKB::HTMLUtils;
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
   	
   	$self->allowed_pdb_hash(undef);
   	
    return $self;
}

# Needed by subclasses to gain access to class variables defined in
# this base class.
sub get_ok_field {
	my ($pkg) = @_;
	
	%ok_field = $pkg->SUPER::get_ok_field();
	$ok_field{"allowed_pdb_hash"}++;

	return %ok_field;
}

sub clear_variables {
    my ($self) = @_;
    
	$self->SUPER::clear_variables();
	
	$self->allowed_pdb_hash(undef);
}

sub buildPart {
	my ($self) = @_;
	
	print STDERR "\n\nDOCKBlasterToUniprotDatabaseIdentifier.buildPart: entered\n";
	
	$self->timer->start($self->timer_message);
	my $dba = $self->builder_params->refresh_dba();

	my $reference_peptide_sequences = $self->fetch_reference_peptide_sequences(0);
	my $pdb_gene_reference_database = $self->builder_params->reference_database->get_pdb_reference_database();
	my $dockblaster_gene_reference_database = $self->builder_params->reference_database->get_dockblaster_reference_database();
	
	print STDERR "DOCKBlasterToUniprotDatabaseIdentifier.buildPart: reference_peptide_sequence count=" . scalar(@{$reference_peptide_sequences}) . "\n";

	my $attribute = 'crossReference';
	$self->set_instance_edit_note("${attribute}s inserted by DOCKBlasterToUniprotDatabaseIdentifier");
	
	$self->create_allowed_pdb_hash();
	
	my $reference_peptide_sequence;
	my $inserted_flag;
	foreach $reference_peptide_sequence (@{$reference_peptide_sequences}) {
	    $reference_peptide_sequence->inflate();
	    
		print STDERR "DOCKBlasterToUniprotDatabaseIdentifier.buildPart: dealing with " . $reference_peptide_sequence->_displayName->[0] . "\n";

		# Remove dockblaster gene identifiers to make sure the mapping is up-to-date, keep others
		# this isn't really necessary as long as the script is run on the slice only
		# But a good thing to have if you need to run the script a second time.
	    $self->remove_typed_instances_from_attribute($reference_peptide_sequence, $attribute, $dockblaster_gene_reference_database);

		my $reference_genes = $reference_peptide_sequence->$attribute;
		if (!(defined $reference_genes) || scalar(@{$reference_genes}) == 0) {
#			print STDERR "DOCKBlasterToUniprotDatabaseIdentifier.buildPart: WARNING - no $attribute instances for " . $reference_peptide_sequence->_displayName->[0] . "\n";
			next;
		}
		$inserted_flag = 0;
		foreach my $reference_gene (@{$reference_genes}) {
#			print STDERR "DOCKBlasterToUniprotDatabaseIdentifier.buildPart: reference database: " . $reference_gene->referenceDatabase->[0]->name->[0] . "\n";
			if ($reference_gene->referenceDatabase->[0]->db_id() == $pdb_gene_reference_database->db_id()) {
				my $pdb_gene_id = $reference_gene->identifier->[0];
				if (!($self->is_allowed_pdb_id($pdb_gene_id))) {
					next;
				}
				print STDERR "DOCKBlasterToUniprotDatabaseIdentifier.buildPart: inserting gene, pdb_gene_id=$pdb_gene_id\n";
				my $rds = $self->builder_params->database_identifier->get_dockblaster_database_identifier($pdb_gene_id);
				$self->check_for_identical_instances($rds);
				$reference_peptide_sequence->add_attribute_value($attribute, $rds);
				$inserted_flag = 1;
			}
		}
		if (!$inserted_flag) {
#			print STDERR "DOCKBlasterToUniprotDatabaseIdentifier.buildPart: WARNING - no pdb genes for " . $reference_peptide_sequence->_displayName->[0] . "\n";
			next;
		}
		$reference_peptide_sequence->add_attribute_value('modified', $self->instance_edit);
		$dba->update_attribute($reference_peptide_sequence, 'modified');
		
		# Make sure the newly inserted genes also get put into the database.
		$dba->update_attribute($reference_peptide_sequence, $attribute);
							
		$self->increment_insertion_stats_hash($reference_peptide_sequence->db_id);
	}
	
	$self->print_insertion_stats_hash();
	
	$self->timer->stop($self->timer_message);
	$self->timer->print();
}

sub is_allowed_pdb_id {
    my ($self, $pdb_id) = @_;
    
    if (defined $self->allowed_pdb_hash) {
    	if (!(defined $self->allowed_pdb_hash->{lc($pdb_id)})) {
    		return 0;
    	}
    }
    
    return 1;
}
 
sub create_allowed_pdb_hash {
    my ($self) = @_;
    
    $self->allowed_pdb_hash(undef);

    my $content = $self->fetch_content_from_url("http://data.docking.org/2010/knd/knd/forMatt.txt");
    if (!(defined $content)) {
    	print STDERR "DOCKBlasterToUniprotDatabaseIdentifier.create_allowed_pdb_hash: WARNING - content undefined, aborting\n";
    	return;
    }
    if ($content eq '') {
    	print STDERR "DOCKBlasterToUniprotDatabaseIdentifier.create_allowed_pdb_hash: WARNING - content empty, aborting\n";
    	return;
    }
    
    my @lines = split(/\n+/, $content);
    
    my %pdb_hash = ();
    foreach my $line (@lines) {
    	my @cols = split(/\t/, $line);
    	my $pdb_id = lc($cols[2]);
    	$pdb_id =~ s/^[^a-z0-9]+//;
    	$pdb_id =~ s/[^a-z0-9]+$//;
    	
    	$pdb_hash{$pdb_id} = $pdb_id;
    }
    
    if (scalar(keys(%pdb_hash)) > 0) {
    	$self->allowed_pdb_hash(\%pdb_hash);
    }
}

sub fetch_content_from_url {
    my ($self, $url) = @_;
    my $content = undef;
    
    if (defined $url) {
	    my $ua = LWP::UserAgent->new();
	
	    my $response = $ua->get($url);
	    if(defined $response) {
	    	if ($response->is_success) {
#	    		print STDERR "DOCKBlasterToUniprotDatabaseIdentifier.fetch_content_from_url: Ah-ha, we have SUCCESS!!!\n";

	    		$content = $response->content;
	    		
#	    		print STDERR "DOCKBlasterToUniprotDatabaseIdentifier.fetch_content_from_url: content=$content\n";

	    	} else {
	    		print STDERR "DOCKBlasterToUniprotDatabaseIdentifier.fetch_content_from_url: WARNING - GET request failed for url=$url\n";
	    	}
	    } else {
	    	print STDERR "DOCKBlasterToUniprotDatabaseIdentifier.fetch_content_from_url: WARNING - no response!\n";
	    }
    } else {
    	print STDERR "DOCKBlasterToUniprotDatabaseIdentifier.fetch_content_from_url: WARNING - you need to supply a URL!\n";
    }
    
    return $content;
}

1;

