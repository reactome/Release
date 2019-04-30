#!/usr/local/bin/perl
use strict;
use warnings;

use lib "/usr/local/gkbdev/modules";

use GKB::CommonUtils;
use GKB::Config;

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

Readonly my $default_recent_db => 'slice_current';
Readonly my $default_previous_db => 'slice_previous';
my $recent_db = prompt("Enter recent slice database name (leave blank for default of $default_recent_db):") || $default_recent_db;
my $previous_db = prompt("Enter previous slice database name (leave blank for default of $default_previous_db):") || $default_previous_db;

print "recent db is $recent_db\n";
my $dba_recent = get_dba($recent_db, $GKB::Config::GK_DB_HOST);

print "previous db is $previous_db\n";
my $dba_old = get_dba($previous_db, $GKB::Config::GK_DB_HOST);

my @recent_compartments = get_compartments($dba_recent);
my @old_compartments = get_compartments($dba_old);
my @new_compartments = get_new_instances(\@old_compartments, \@recent_compartments);

(my $outfile = $0) =~ s/\.pl/\.txt/;
open(my $out, '>', $outfile);
print $out "Compartments in $recent_db\n\n";
print_compartments($out, @recent_compartments);

print $out "New compartments in $recent_db compared to $previous_db\n\n";
print_compartments($out, @new_compartments);
close $out;

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

sub get_compartments {
    my $dba = shift;

    my $events = $dba->fetch_instance(-CLASS => 'Event');
    my $entities = $dba->fetch_instance(-CLASS => 'PhysicalEntity');

    my @compartments = map {@{$_->compartment}} (@$events, @$entities);
    my @included_location = map {@{$_->includedLocation}} (@$events, @$entities);

    return get_unique_instances(@compartments, @included_location);
}

sub get_new_instances {
    my $old_instances = shift;
    my $recent_instances = shift;

    my %db_id_to_instance;
    $db_id_to_instance{$_->db_id} = $_ foreach @$old_instances;

    my @new_instances;
    foreach my $recent_instance (@$recent_instances) {
        push @new_instances, $recent_instance unless (exists $db_id_to_instance{$recent_instance->db_id});
    }
    return @new_instances;
}

sub print_compartments {
    my $file_handle = shift;
    my @compartments = @_;
    foreach my $compartment (sort {$a->displayName cmp $b->displayName} @compartments) {
        print $file_handle join("\t",
            $compartment->db_id,
            $compartment->displayName,
            $compartment->accession->[0]) . "\n";
    }
    print $file_handle "\n";
}

sub usage_instructions {
    return <<END;

    This script prompts for two slice database names.  The compartments
    for each slice are obtained.  The list of compartments (db id, display name,
    and accession) for the more recent slice database and the newer compartments
    (in the recent slice but not in the older slice) are printed to an output file
    with the same name as the script but with a .txt extension.

    Usage: perl $0 [options]

    -help   Print these instructions

END
}