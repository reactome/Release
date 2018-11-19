#!/usr/local/bin/perl
use strict;
use warnings;

use Getopt::Long;
use autodie;
use List::MoreUtils qw/any/;

use lib '/usr/local/gkb/modules';
use GKB::CommonUtils;

my @infectious_species = get_infectious_species();
foreach my $reaction_like_event (@{get_dba('gk_central', 'reactomecurator.oicr.on.ca')->fetch_instance(-CLASS => 'ReactionlikeEvent')}) {
    next unless species_attribute_contains($reaction_like_event, \@infectious_species) && has_multiple_species($reaction_like_event);

    print get_name_and_id($reaction_like_event) . "\n";
}

sub species_attribute_contains {
    my $instance = shift;
    my $species_names = shift;
    
    my @instance_species_names = map {$_->displayName} @{$instance->species};
    return any {
        my $instance_species_name = $_;
        return any { $instance_species_name =~ /$_/i} @{$species_names};
    } @instance_species_names;
}

sub get_infectious_species {
    return (
        'bacteria',
        'virus',
        'Bacillus',
        'Candida albicans',
        'Chlamydia',
        'Clostridium',
        'Corynephage beta',
        'Crithidia fasciculata',
        'Cryptococcus neoformans',
        'Entamoeba histolytica',
        'Escherichia coli',        
        'H1N1 subtype',
        'Listeria monocytogenes',
        'Mycobacterium tuberculosis',
        'Neisseria gonorrhoeae',
        'Neisseria meningitidis',
        'Plasmodium falciparum',
        'Salmonella',
        'Sarcocystidae',
        'Sedoreovirinae',
        'Shigella dysenteriae',
        'Staphylococcus aureus',
        'Streptomyces hygroscorpius',
        'Toxoplasma gondii'
    );
}