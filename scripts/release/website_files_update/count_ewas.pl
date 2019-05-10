#!/usr/local/bin/perl
use strict;
use warnings;

use lib "/usr/local/gkbdev/modules";

use GKB::Config;
use GKB::DBAdaptor;
use GKB::Utils;

use autodie;
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

my $recent_db = prompt("Enter recent slice database name:");
while (!$recent_db) {
    print "No value entered for recent slice database name.\n";
    $recent_db = prompt("Enter recent slice database name:");
}
my $recent_version = get_dba($recent_db)->fetch_instance(-CLASS => '_Release')->[0]->releaseNumber->[0];
if ($recent_version !~ /^\d+$/) {
    die "$recent_version is not numeric\n";
}

my $previous_db = prompt("Enter previous slice database name:");
while (!$previous_db) {
    print "No value entered for previous slice database name.\n";
    $previous_db = prompt("Enter previous slice database name:");
}

print "recent db is $recent_db\n";
my $dba_recent = get_dba($recent_db);
my $ewas_recent = get_human_ewas_instances($dba_recent);

print "previous db is $previous_db\n";
my $dba_old = get_dba($previous_db);
my $ewas_old = get_human_ewas_instances($dba_old);

my %recent_ewas_names;
my %old_ewas_names;

foreach my $ewas_instance (@{$ewas_recent}){
    push @{$recent_ewas_names{$ewas_instance->displayName}},$ewas_instance;
}
print 'new slice total = ' . (scalar keys %recent_ewas_names) ."\n";

foreach my $ewas_instance (@{$ewas_old}){
    push @{$old_ewas_names{$ewas_instance->displayName}},$ewas_instance;
}
print 'old slice total = ' . (scalar keys %old_ewas_names) . "\n";

my %seen = ();
my @new_ewas;
foreach my $recent_ewas_name (keys %recent_ewas_names) {
    unless ($old_ewas_names{$recent_ewas_name}) {
        foreach my $ewas_instance (@{$recent_ewas_names{$recent_ewas_name}}) {
            push (@new_ewas, $ewas_instance) unless $seen{$ewas_instance->db_id}++;
        }
    }
}

print "Second step for curators to EWASs mapping starts now\n\n";

open my $curator_new_ewas_fh, '>', "curator_newEWAS$recent_version.txt";
print $curator_new_ewas_fh "Curator\tnew_ewas_count\n\n";
print "Curator\tnew_ewas_count\n\n";

my %curator2new_ewas;
foreach my $ewas (@new_ewas) {
    my $ewas_description = $ewas->db_id . ":" . $ewas->displayName;
    my $curator;
    if ($ewas->created->[0] && $ewas->created->[0]->author->[0]) {
        if ($ewas->created->[0]->author->[0]->_displayName->[0]) {
            $curator = $ewas->created->[0]->author->[0]->_displayName->[0];
        }
    }
    $curator ||= 'Unknown';

    push @{$curator2new_ewas{$curator}}, $ewas_description;
}

foreach my $curator (keys %curator2new_ewas){
    my $output = $curator . "\t" . scalar @{$curator2new_ewas{$curator}} . "\t" . join("\t",@{$curator2new_ewas{$curator}});
    print $curator_new_ewas_fh "$output\n";
    print "$output\n";
}

print $curator_new_ewas_fh "Total new EWASs: ". @new_ewas."\n";
print "Total new EWASs: ".@new_ewas."\n";

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

sub get_human_ewas_instances {
    my $dba = shift;

    return get_instances($dba, 'EntityWithAccessionedSequence', '48887');
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

This script counts and reports the number of new EWASs between an old
and new test slice as well as a break down by curator reporting the number
of new EWASs as well as the EWAS names.

Usage: perl $0

Output: curator_newEWASXX.txt where XX is reactome release version
END
}
