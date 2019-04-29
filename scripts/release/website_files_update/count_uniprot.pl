#!/usr/local/bin/perl
use strict;
use warnings;

# The collection of UniProt identifiers does not distinguish
# between canonical and isoform representations, so isoforms
# are not counted.

use lib "/usr/local/gkbdev/modules";

use GKB::ClipsAdaptor;
use GKB::Config;
use GKB::DBAdaptor;
use GKB::Utils_esther;
use GKB::Utils;

use autodie;
use Data::Dumper;
use Getopt::Long;
use Readonly;
use Term::ReadKey;

my ($help);
GetOptions(
    'help' => \$help
);

if ($help) {
    print usage_instructions();
    exit;
}

Readonly my $default_recent_db = 'slice_current';
Readonly my $default_previous_db = 'slice_previous'
my $recent_db = prompt("Enter recent slice database name (leave blank for default of $default_recent_db):") || $default_recent_db;
my $recent_version = prompt("Enter Reactome version of recent database:");
die "Reactome version must be numeric" if !$recent_version || $recent_version !~ /^\d+$/;
my $previous_db = prompt("Enter previous slice database name (leave blank for default of $default_previous_db):") || $default_previous_db;

print "recent db is $recent_db\n";
my $dba_recent = get_dba($recent_db);
my $uniprot_recent = get_human_uniprot_instances($dba_recent);

print "previous db is $previous_db\n";
my $dba_old = get_dba($previous_db);
my $uniprot_old = get_human_uniprot_instances($dba_old);

my (%recent_uniprot_ids, %old_uniprot_ids);

foreach my $uniprot_instance (@{$uniprot_recent}){
    $recent_uniprot_ids{$uniprot_instance->identifier->[0]} = 1;
}
print 'new slice total = ' . (scalar keys %recent_uniprot_ids) ."\n";

foreach my $uniprot_instance (@{$uniprot_old}){
    $old_uniprot_ids{$uniprot_instance->identifier->[0]} = 1;
}
print 'old slice total = ' . (scalar keys %old_uniprot_ids) . "\n";

my %seen = ();
my @new_uniprot_ids;
foreach my $recent_uniprot_id (keys %recent_uniprot_ids) {
    unless ($old_uniprot_ids{$recent_uniprot_id}) {
        push (@new_uniprot_ids, $recent_uniprot_id) unless $seen{$recent_uniprot_id}++;
    }
}
print 'new reference gene products in the recent release: ' . scalar @new_uniprot_ids."\n";

print "Second step for curators to reference gene products mapping starts now\n\n";
open (my $curator_new_uniprot_fh, '>', "curator_newUniProt$recent_version.txt");

my $new_uniprot_instances = $dba_recent->fetch_instance(
    -CLASS => 'ReferenceGeneProduct',
    -QUERY =>
        [
            {
            -ATTRIBUTE => 'identifier',
            -VALUE => \@new_uniprot_ids
            }
        ]
);

print $curator_new_uniprot_fh "Curator\tnew_rgp_count\n\n";
print "Curator\tnew_rgp_count\n\n";

my %curator2new_uniprot;

my %seen_uniprot_ids = ();
foreach my $new_uniprot_instance (@{$new_uniprot_instances}) {
    my $new_uniprot_referrers = $dba_recent->fetch_referer_by_instance($new_uniprot_instance);
    if (@{$new_uniprot_referrers}) {
        my @representative_ewas = (); # Each EWAS uniquely represents a UniProt reference gene product

        foreach my $ewas (grep {$_->is_a('EntityWithAccessionedSequence')} @{$new_uniprot_referrers}) {
            push (@representative_ewas, $ewas) unless $seen_uniprot_ids{$ewas->referenceEntity->[0]->identifier->[0]}++;
        }

        foreach my $ewas (@representative_ewas) {
            my $curator;
            if ($ewas->created->[0] && $ewas->created->[0]->author->[0]) {
                if ($ewas->created->[0]->author->[0]->_displayName->[0]) {
                    $curator = $ewas->created->[0]->author->[0]->_displayName->[0];
                }
            }
            $curator ||= 'Unknown';
            push @{$curator2new_uniprot{$curator}}, $new_uniprot_instance;
        }
    }
}

foreach my $curator (keys %curator2new_uniprot){
    my $output = $curator . "\t" . scalar @{$curator2new_uniprot{$curator}} . "\t" . join("\t",map($_->identifier->[0],@{$curator2new_uniprot{$curator}}));
    print $curator_new_uniprot_fh "$output\n";
    print "$output\n";
}
print $curator_new_uniprot_fh "Total new reference gene products: ". @new_uniprot_ids."\n";
print "Total new reference gene products: ".@new_uniprot_ids."\n";

# Ask user for information
sub prompt {
    my $question = shift;
    print $question;
    my $pass = shift;
    ReadMode 'noecho' if $pass; # Don't show keystrokes if it is a password
    my $return = ReadLine 0;
    chomp $return;

    ReadMode 'normal';
    print "\n" if $pass;
    return $return;
}

sub get_dba {
    my $db = shift;

    return GKB::DBAdaptor->new(
        -dbname => $db,
        -user   => $GKB::Config::GK_DB_USER,
        -host   => $GKB::Config::GK_DB_HOST, # host where mysqld is running
        -pass   => $GKB::Config::GK_DB_PASS,
        -port   => '3306'
    );
}

sub get_human_uniprot_instances {
    my $dba = shift;
    my $protein_class = &GKB::Utils::get_reference_protein_class($dba);

    return get_instances($dba, $protein_class, '48887');
}

sub get_instances {
    my $dba = shift;
    my $class = shift;
    my $species = shift;

    return $dba->fetch_instance(
        -CLASS => $class,
        -QUERY =>
        [
            {
                -ATTRIBUTE => 'species',
                -VALUE => [$species]
            }
        ]
    );
}

sub usage_instructions {
    return <<END;

This script counts and reports the number of new human UniProt reference gene
products between an old and new test slice as well as a break down by curator
reporting the number of new human UniProt reference gene products.

Usage: perl $0

Output: curator_newUniProtXX.txt where XX is the reactome version number

END
}