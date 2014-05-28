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
use GKB::Compara::BioMart;
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
    
    my %source_mapping = ();
    
    my $biomart = GKB::Compara::BioMart->new();
    my $out = $biomart->get_mapping_table_from_mart($opt_from);

    my @rows = split /\n/, $out;
    
    foreach my $row (@rows) {
	my @items = split /\t/, $row;
	my $ensg = $items[0];
	$items[1] && push @{$source_mapping{$items[1]}}, $ensg; #swissprot accession
	$items[2] && push @{$source_mapping{$items[2]}}, $ensg; #trembl accession
    }
	
	
    # Get rid of (ensg) duplicates
    my $utils = GKB::Compara::Utils->new();
    foreach my $key (keys %source_mapping) {
	my $ar = $utils->uniquify($source_mapping{$key});
	$source_mapping{$key} = $ar; 
    }
	
    # Print source mapping hash content
    open(my $source_fh, '>', "$opt_from\_protein_gene_mapping.txt");
    foreach my $protein_key (sort keys %source_mapping) {
	print $source_fh $protein_key, "\t", join (' ', @{$source_mapping{$protein_key}}), "\n";
    }
    close($source_fh);
    
    return %source_mapping;
}

1;