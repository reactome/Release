=head1 NAME

GKB::Compara::Target;

=head1 SYNOPSIS

=head1 DESCRIPTION

This is a class for creating the target hash between target species 
uniprot ids and target species genes using Ensembl gene ids.

=head1 SEE ALSO

=head1 AUTHOR

Joel Weiser E<lt>joel.weiser@oicr.on.caE<gt>

Copyright (c) 2012 European Bioinformatics Institute, Cold Spring
Harbor Laboratory, and Ontario Institute for Cancer Research.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

package GKB::Compara::Target;

use strict;

use autodie;
use Data::Dumper;
use File::Basename;
use GKB::Config;
use GKB::Config_Species;
use GKB::Compara::Utils;
use GKB::EnsEMBLMartUtils qw/get_species_results_with_attribute_info/;
use List::MoreUtils qw/any/;
use Log::Log4perl qw/get_logger/;

Log::Log4perl->init(dirname(__FILE__) . '/compara_log.conf');

use vars qw(@ISA $AUTOLOAD %ok_field);
use Bio::Root::Root;

@ISA = qw(Bio::Root::Root);

# List the object variables here, so that they can be checked
for my $attr
    (qw(
	
	) ) { $ok_field{$attr}++; }

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
    
    my $self = bless {}, $pkg;

    $self->clear_variables();

    return $self;
}

sub clear_variables {
    my ($self) = @_;

}

# Needed by subclasses to gain access to class variables defined in
# this class.
sub get_ok_field {
    return %ok_field;
}

# Parses biomart output and prepares the target species mapping hash, 
# with ensg ids as keys and swissprot OR trembl OR ensp ids as values
sub prepare_target_mapping_hash {
    my ($self, $species_key) = @_;
    
    my $logger = get_logger(__PACKAGE__);
    
    my %mapping = get_gene_to_protein_mapping($species_key);
    
    $logger->info("mapping size=" . scalar(keys(%mapping)) . "\n");

    my $output_file = "$species_key\_gene_protein_mapping.txt";
    open my $output_fh, '>', $output_file;
    foreach my $gene_id (sort keys %mapping) {
        print $output_fh $gene_id . "\t" . join(' ', @{$mapping{$gene_id}}) . "\n";
    }
    close $output_fh;
    
    return %mapping;
}

sub get_gene_to_protein_mapping {
    my $species = shift;
    
    my %gene_to_protein_identifier_type_to_protein;
    my $species_results_with_attribute_info = get_species_results_with_attribute_info($species);
    my @attributes = @{$species_results_with_attribute_info->{'uniprot_attributes'}};
    my @lines = split "\n", $species_results_with_attribute_info->{'results'};
    #print "attributes: @attributes\n";
    foreach my $line (@lines) {
        #print "$line\n";
        my ($gene_id, $swissprot_id, $trembl_id, $ensembl_protein);
        my @fields = split "\t", $line;
        $gene_id = $fields[0];
        $swissprot_id = $fields[1];
        if (any {$_ =~ /trembl/} @attributes) {
            $trembl_id = $fields[2];
            $ensembl_protein = $fields[3];
        } else {
            $ensembl_protein = $fields[2];
        }
        next unless $gene_id && ($swissprot_id || $trembl_id || $ensembl_protein);

        #print "Gene: $gene_id\tSwissprot: $swissprot_id\tTrembl: $trembl_id\tEnsEMBL Protein: $ensembl_protein\n";

        if ($swissprot_id) {
            $gene_to_protein_identifier_type_to_protein{$gene_id}{'swissprot'}{"SWISS:$swissprot_id"}++;
        }
        if ($trembl_id) {
            $gene_to_protein_identifier_type_to_protein{$gene_id}{'trembl'}{"TREMBL:$trembl_id"}++;
        }
        if ($ensembl_protein) {
            $gene_to_protein_identifier_type_to_protein{$gene_id}{'ensp'}{"ENSP:$ensembl_protein"}++;
        }        
    }
    
    my %gene_to_protein_mapping;
    foreach my $gene_id (keys %gene_to_protein_identifier_type_to_protein) {
        my @protein_ids = sort keys %{$gene_to_protein_identifier_type_to_protein{$gene_id}{'swissprot'}};
        @protein_ids = sort keys %{$gene_to_protein_identifier_type_to_protein{$gene_id}{'trembl'}} unless @protein_ids;
        @protein_ids = sort keys %{$gene_to_protein_identifier_type_to_protein{$gene_id}{'ensp'}} unless @protein_ids;
        
        $gene_to_protein_mapping{$gene_id} = \@protein_ids;
    }
    
    return %gene_to_protein_mapping;
}

1;
