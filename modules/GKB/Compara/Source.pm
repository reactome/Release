=head1 NAME

GKB::Compara::Source;

=head1 SYNOPSIS

=head1 DESCRIPTION

This is a class for creating the source hash between source species 
uniprot ids and source species genes using Ensembl gene ids.

=head1 SEE ALSO

=head1 AUTHOR

Joel Weiser E<lt>joel.weiser@oicr.on.caE<gt>

Copyright (c) 2012 European Bioinformatics Institute, Cold Spring
Harbor Laboratory, and Ontario Institute for Cancer Research.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

package GKB::Compara::Source;

use strict;

use autodie;
use Data::Dumper;
use File::Basename;
use GKB::Config;
use GKB::Config_Species;
use GKB::EnsEMBLMartUtils qw/get_species_results/;
use Log::Log4perl qw/get_logger/;

use vars qw(@ISA $AUTOLOAD %ok_field);
use Bio::Root::Root;

Log::Log4perl->init(dirname(__FILE__) . '/compara_log.conf');

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

# This subroutine returns a hash with uniprot accessions as keys
# and ensembl gene ids as values
sub prepare_source_mapping_hash { 
    my ($self, $opt_from) = @_;
    
    my %source_mapping = get_protein_to_gene_mapping($opt_from);
    	
    # Print source mapping hash content
    my $output_file = "$opt_from\_protein_gene_mapping.txt";
    open my $output_fh, '>', $output_file;
    foreach my $protein_id (sort keys %source_mapping) {
        print $output_fh $protein_id . "\t" . join(' ', sort @{$source_mapping{$protein_id}}) . "\n";
    }
    close $output_fh;
    
    return %source_mapping;
}

sub get_protein_to_gene_mapping {
    my $species = shift;
    
    my %protein_to_gene;
    my @lines = split "\n", get_species_results($species);
    foreach my $line (@lines) {
        my ($gene_id, $swissprot_id, $trembl_id) = split "\t", $line;
        next unless $gene_id && ($swissprot_id || $trembl_id);
        
        if ($swissprot_id) {
            $protein_to_gene{$swissprot_id}{$gene_id}++;
        }
        if ($trembl_id) {
            $protein_to_gene{$trembl_id}{$gene_id}++;
        }
    }
    
    my %protein_to_gene_mapping;
    foreach my $protein_id (keys %protein_to_gene) {
        my @gene_ids = keys %{$protein_to_gene{$protein_id}};
        
        $protein_to_gene_mapping{$protein_id} = \@gene_ids;
    }
    return %protein_to_gene_mapping;
}

1;