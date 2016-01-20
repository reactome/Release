#!/usr/local/bin/perl  -w
use strict;

use lib "/usr/local/gkb/modules";
use GKB::Config;
use GKB::DBAdaptor;
use GKB::Utils;

use autodie;
use Array::Utils qw/:all/;
use Data::Dumper;
use File::Slurp;
use Getopt::Long;
use List::MoreUtils qw/uniq/;

my ($db, $host, $reaction_file, $help);

GetOptions(
    "help" => \$help
);

if ($help) {
    print usage_instructions();
    exit;
}

GetOptions(
    "host:s" => \$host,
    "db:s" => \$db,
    "reaction_file:s" => \$reaction_file
);

$reaction_file || die usage_instructions();

my %uniprot_id_to_reactions_map = get_uniprot_id_to_reactions_map($reaction_file);
my @uniprot_from_reactions = keys %uniprot_id_to_reactions_map;
my @uniprot_from_release_db = get_uniprot_ids_from_release_db($db, $host);

my @unreleased_uniprot_ids = array_minus(@uniprot_from_reactions, @uniprot_from_release_db);

(my $outfile = $0) =~ s/\.pl/\.txt/;
open(my $out, '>' , $outfile);
foreach my $uniprot_id (sort @unreleased_uniprot_ids) {
    my @reactions = uniq sort @{$uniprot_id_to_reactions_map{$uniprot_id}};
    print $out $uniprot_id . "\t" . join(';', @reactions) . "\n";
}
close $out;

sub get_uniprot_id_to_reactions_map {
    my $reaction_file = shift;
    
    my %uniprot_id_to_reactions_map;
    my $gk_central_dba = get_dba('gk_central', 'reactomecurator.oicr.on.ca');
    my @reaction_ids = read_file($reaction_file);
    foreach my $reaction_id (@reaction_ids) {
        chomp $reaction_id;
        my @uniprot_ids = map {$_->identifier->[0]} @{find_rps(get_reaction_instance($reaction_id, $gk_central_dba), $gk_central_dba)};
        foreach my $uniprot_id (@uniprot_ids) {
            push @{$uniprot_id_to_reactions_map{$uniprot_id}}, $reaction_id;
        }
    }
    return %uniprot_id_to_reactions_map;
}

sub get_reaction_instance {
    my $reaction_id = shift;
    my $dba = shift;
    
    return $dba->fetch_instance_by_db_id($reaction_id)->[0];
}

sub get_uniprot_ids_from_release_db {
    my $db = shift;
    my $host = shift;
    
    my @uniprot_instances = @{get_dba($db, $host)->fetch_instance_by_remote_attribute("ReferenceGeneProduct", [['referenceDatabase.name', '=', ['UniProt']]])};
    my @uniprot_ids = map {$_->identifier->[0]} @uniprot_instances;
    
    return uniq @uniprot_ids;
}

sub get_dba {
    my ($db, $host) = @_;
    
    return GKB::DBAdaptor->new(
        -dbname => $db || $GKB::Config::GK_DB_NAME,
        -user => $GKB::Config::GK_DB_USER,
        -pass => $GKB::Config::GK_DB_PASS,
        -host => $host || $GKB::Config::GK_DB_HOST
    );
}

sub find_rps {
    my $reaction = shift;
    my $dba = shift;
    
    my $protein_class = &GKB::Utils::get_reference_protein_class($dba);
    
    return $reaction->follow_class_attributes(
        -INSTRUCTIONS => {
            'ReactionlikeEvent' => {'attributes' => [qw(input output catalystActivity)]},
            'CatalystActivity' => {'attributes' => [qw(physicalEntity)]},
            'Complex' => {'attributes' => [qw(hasComponent)]},
            'EntitySet' => {'attributes' => [qw(hasMember)]},
            'Polymer' => {'attributes' => [qw(repeatedUnit)]},
            'EntityWithAccessionedSequence' => {'attributes' => [qw(referenceEntity)]}
        },
        -OUT_CLASSES => [($protein_class)]    
    );
}

sub usage_instructions {
    return <<END;

This script reports UniProt accessions present in reactions
in gk_central (provided as a list of database identifiers in
a file) where the accessions are not in a release database.

Usage: perl $0 [options]

-reaction_file file_name (REQUIRED)
-host db_host (default: $GKB::Config::GK_DB_HOST)
-db db_name (default: $GKB::Config::GK_DB_NAME)

END
}