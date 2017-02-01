=head1 NAME

GKB::AddLinks::GeneToUniprotReferenceDNASequence

=head1 SYNOPSIS

=head1 DESCRIPTION

Base class for a number of subclasses that add new genes to a ReferenceGeneProduct,
where Entrez IDs are used.  It simply looks to see if EntrezGenes already
exist for a given ReferenceGeneProduct, and clones them into the target genes if so.
This means that there is a dependency here; you must run a script to insert
EntrezGenes first, before you run this script.

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

package GKB::AddLinks::GeneToUniprotReferenceDNASequence;
use strict;

use GKB::Config;
use GKB::AddLinks::Builder;

use Log::Log4perl qw/get_logger/;
Log::Log4perl->init(\$LOG_CONF);

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

# Needed by subclasses to gain access to class variables defined in
# this base class.
sub get_ok_field {
    my ($pkg) = @_;

    %ok_field = $pkg->SUPER::get_ok_field();
    $ok_field{"target_gene_reference_database"}++;

    return %ok_field;
}

sub clear_variables {
    my ($self) = @_;
    
    $self->SUPER::clear_variables();

    $self->target_gene_reference_database(undef);
}

sub buildPart {
    my ($self) = @_;
    
    my $logger = get_logger(__PACKAGE__);
    
    $logger->info("entered");
    
    $self->timer->start($self->timer_message);
    my $dba = $self->builder_params->refresh_dba();

    my $reference_peptide_sequences = $self->fetch_reference_peptide_sequences(0);
    my $entrez_gene_reference_database = $self->builder_params->reference_database->get_entrez_gene_reference_database();
    my $target_gene_reference_database = $self->target_gene_reference_database;
    
    $logger->info("reference_peptide_sequence count=" . scalar(@{$reference_peptide_sequences}));

    if (!(defined $target_gene_reference_database)) {
        $logger->error("target_gene_reference_database not defined, aborting!");
        return;
    }

    my $attribute = 'referenceGene';
    $self->set_instance_edit_note("${attribute}s inserted by GeneToUniprotReferenceDNASequence");
    
    my $reference_peptide_sequence;
    my $inserted_flag;
    foreach $reference_peptide_sequence (@{$reference_peptide_sequences}) {
        $reference_peptide_sequence->inflate();
        
    	$logger->info("dealing with " . $reference_peptide_sequence->_displayName->[0]);

    	# Remove target gene identifiers to make sure the mapping is up-to-date, keep others
    	# this isn't really necessary as long as the script is run on the slice only
    	# But a good thing to have if you need to run the script a second time.
        $self->remove_typed_instances_from_attribute($reference_peptide_sequence, $attribute, $target_gene_reference_database);

    	my $reference_genes = $reference_peptide_sequence->$attribute;
    	if (!(defined $reference_genes) || scalar(@{$reference_genes}) == 0) {
		    $logger->warn("no reference genes for " . $reference_peptide_sequence->_displayName->[0]);
            next;
        }
        $inserted_flag = 0;
        foreach my $reference_gene (@{$reference_genes}) {
            next unless $reference_gene && $reference_gene->referenceDatabase->[0];
            if ($reference_gene->referenceDatabase->[0]->db_id() == $entrez_gene_reference_database->db_id()) {
            	my $entrez_gene_id = $reference_gene->identifier->[0];
	        	$logger->info("inserting gene, entrez_gene_id=$entrez_gene_id");
            	my $rds = $self->builder_params->miscellaneous->get_reference_dna_sequence($reference_peptide_sequence->Species, $target_gene_reference_database, $entrez_gene_id);
            	$self->check_for_identical_instances($rds);
            	$reference_peptide_sequence->add_attribute_value($attribute, $rds);
            	$inserted_flag = 1;
            }
        }
        if (!$inserted_flag) {
	    	$logger->warn("no Entrez genes for " . $reference_peptide_sequence->_displayName->[0]);
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

1;

