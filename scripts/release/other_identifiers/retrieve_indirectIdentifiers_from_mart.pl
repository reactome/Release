#!/usr/bin/perl
use strict;
use warnings;

use lib "/usr/local/reactomes/Reactome/development/GKB/modules";

use GKB::Config;
use GKB::Config_Species;
use GKB::EnsEMBLMartUtils qw/get_identifiers get_species_mart_name get_wget_query_for_identifier/;

use Capture::Tiny qw/capture/;
use Getopt::Long;
use List::MoreUtils qw/any/;
use Try::Tiny;

use Log::Log4perl qw/get_logger/;
Log::Log4perl->init(\$LOG_CONF);
my $logger = get_logger(__PACKAGE__);

our($species_code, $help);

&GetOptions(
    "sp=s" => \$species_code,
    "help" => \$help
);

if ($help) {
    print usage_instructions();
    exit;
}

foreach my $species_abbreviation (get_species_to_query($species_code)) {
    my $species_mart_name = get_species_mart_name($species_abbreviation);

    foreach my $identifier (get_identifiers($species_mart_name)) {
        next if $identifier =~ /chembl|clone_based|dbass|description|ottg|ottt|ottp|shares_cds|merops|mirbase|reactome/;

        my $output_file = "output/$species_mart_name\_$identifier";
        if (-s $output_file) {
            $logger->info("$output_file already exists - creating backup");
            system("mv -f $output_file $output_file.bak");
        }
        download_identifier_file($species_abbreviation, $identifier, $output_file);
    }
}

# Returns all configured species abbreviations by default
# or the single species abbreviation if it exists in the
# @species array imported from Config_Species.pm
sub get_species_to_query {
    my $selected_species = shift;

    my $logger = get_logger(__PACKAGE__);

    my @species_to_query;
    if ($selected_species) {
        if (any { $_ eq $selected_species } @species) {
            @species_to_query = ($selected_species);
        } else {
            $logger->logconfess("$selected_species species: EnsEMBL mart information is unknown");
        }
    } else {
        @species_to_query = @species;
    }

    return @species_to_query;
}

# Retrieves the appropriate wget query for the species and
# external identifier and then attemps to download the
# identifier file and save it to an output file.
# The function will recurse on failure to re-try
# downloading to a pre-set maximum attempt number
sub download_identifier_file {
    my $species_abbreviation = shift;
    my $identifier = shift;
    my $output_file = shift;
    my $attempt_count = shift // 1;
    my $max_attempts = 3;

    my $logger = get_logger(__PACKAGE__);

    my $wget_query = get_wget_query_for_identifier($species_abbreviation, $identifier);

    $logger->info("$identifier for species $species_abbreviation: beginning download of identifier file");
    my ($output, $error) = capture {
        system $wget_query;
    };

    my $completion_tag = qr/\[success\]\n/;
    if ($output =~ /$completion_tag/) {
        $output =~ s/$completion_tag//;

        open(my $fh, '>', $output_file);
        print {$fh} $output;
        close $fh;
    } else {
        my $error_message = "$identifier for species $species_abbreviation: download of identifier file not complete";
        if ($attempt_count < $max_attempts) {
            $logger->warn("$error_message on attempt $attempt_count - retrying");
            download_identifier_file($species_abbreviation, $identifier, $output_file, ++$attempt_count);
        } else {
            $logger->error("$error_message due to $error - aborting");
        }
    }
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