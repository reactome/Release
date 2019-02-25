#!/usr/local/bin/perl
use strict;
use warnings;

use lib "/usr/local/reactomes/Reactome/development/GKB/modules";

use GKB::Config;
use GKB::Config_Species;
use GKB::EnsEMBLMartUtils qw/get_identifiers get_species_mart_name get_wget_query_for_identifier/;

use Data::Dumper;
use Getopt::Long;
use Try::Tiny;

use Log::Log4perl qw/get_logger/;
Log::Log4perl->init(\$LOG_CONF);
my $logger = get_logger(__PACKAGE__);

our($species_abbr, $help);

&GetOptions(
    "sp=s" => \$species_abbr,
    "help" => \$help
);

if ($help) {
    print usage_instructions();
    exit;
}

foreach my $species_abbreviation (get_species_to_query($species_abbr)) {
    my $species_mart_name = get_species_mart_name($species_abbreviation);

    foreach my $identifier (get_identifiers($species_mart_name)) {
        next if $identifier =~ /chembl|clone_based|dbass|description|ottg|ottt|ottp|shares_cds|merops|mirbase|reactome/;

        my $wget_query = get_wget_query_for_identifier($species_abbreviation, $identifier);
        system "$wget_query > output/$species_mart_name\_$identifier";
    }
}

# Returns all configured species abbreviations by default
# or the single species abbreviation if it exists in the
# @species array imported from Config_Species.pm
sub get_species_to_query {
    my $selected_species = shift;

    my @species_to_query;
    if ($selected_species) {
        if (any { $_ eq $selected_species } @species) {
            @species_to_query = ($selected_species);
        } else {
            $logger->logconfess("EnsEMBL mart information is unknown for $selected_species");
        }
    } else {
        @species_to_query = @species;
    }

    return @species_to_query;
}

sub usage_instructions {
    return <<END;

This script will attempt to retreive tab-delimited files from EnsEMBL Mart for various
external identifiers.  The output of the files will have four columns:

EnsEMBL Gene ID\tEnsEMBL Transcript ID\tEnsEMBL Peptide ID\tExternal Identifier ID

By default, this script will query for the set of available identifiers for each species
specified in the Reactome Config_Species.pm perl module.  A single species can also be
queried by providing its abbreviation (e.g. hsap for Homo sapiens) with the flag -sp.

Usage: $0 [-sp 'species abbreviation']
END
}