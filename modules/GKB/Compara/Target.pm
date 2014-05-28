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
use GKB::Compara::BioMart;
use GKB::Compara::Utils;
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
    
    my %mapping = ();
        
    my %seen = ();
    my %swiss = ();
    my %trembl = ();
    my %ensp = ();
	
    my $biomart = GKB::Compara::BioMart->new();
    my $out = $biomart->get_mapping_table_from_mart($species_key);
    
    #parsing biomart output
    if (!(defined $out)) {
        $logger->error("out is undef for $species_key!\n");
        return %mapping;
    }


    my @rows = split /\n/, $out;

    foreach my $row (@rows) {
        my @items = split /\t/, $row;
        my $ensg = $items[0];
	$seen{$ensg}++;
		
	#sort into swissprot, trembl and ensp identifiers
	$items[1] && push @{$swiss{$ensg}}, 'SWISS:'.$items[1];
        $items[2] && push @{$trembl{$ensg}}, 'TREMBL:'.$items[2];
        $items[3] && push @{$ensp{$ensg}}, 'ENSP:'.$items[3];
    }
	
    #apply a hierarchy for the target protein ids: take swissprot if available, if not then trembl, if not then ENSP into the mapping hash

    $logger->info("swiss size=" . scalar(keys(%swiss)) . "\n");
    $logger->info("trembl size=" . scalar(keys(%trembl)) . "\n");
    $logger->info("ensp size=" . scalar(keys(%ensp)) . "\n");

    my $utils = GKB::Compara::Utils->new();
    foreach my $ensg (keys %seen) {
	if ($swiss{$ensg}->[0]) {
	    $mapping{$ensg} = $utils->uniquify($swiss{$ensg});
	} elsif ($trembl{$ensg}->[0]) {
	    $mapping{$ensg} = $utils->uniquify($trembl{$ensg});
	} else {
	    $mapping{$ensg} = $utils->uniquify($ensp{$ensg});
	}
    }

    $logger->info("mapping size=" . scalar(keys(%mapping)) . "\n");

    #print target mapping
    open(my $target, '>', "$species_key\_gene_protein_mapping.txt") || $logger->error_die($!);
    foreach my $gene (sort keys %mapping) {
	print $target $gene, "\t", join (' ', @{$mapping{$gene}}), "\n";
    }
    close($target);
    
    return %mapping;
}

1;
