#!/usr/local/bin/perl -w
use strict;

use lib "/usr/local/gkbdev/modules";

use GKB::CommonUtils;
use GKB::Config;
use GKB::DBAdaptor;
use GKB::Utils;

use autodie;
use Getopt::Long;
use Term::ReadKey;

my ($help);
GetOptions(
    'help' => \$help
);

if ($help) {
    print usage_instructions();
    exit;
}

my $recent_version = prompt('Enter recent test slice version: (e.g. 39, 39a, etc):');
my $previous_version = prompt('Enter previous test slice version: (e.g. 38):');

my $recent_db = "test_slice_$recent_version";
print 'recent db is '.$recent_db."\n";
my $dba_recent = get_dba($recent_db, $GKB::Config::GK_DB_HOST);

my $previous_db = "test_slice_$previous_version";
print 'previous db is '.$previous_db."\n";
my $dba_old = get_dba($previous_db, $GKB::Config::GK_DB_HOST);

my @recent_compartments = get_compartments($dba_recent);
my @old_compartments = get_compartments($dba_old);
my @new_compartments = get_new_instances(\@old_compartments, \@recent_compartments);

print "Compartments in $recent_db\n\n";
print_compartments(@recent_compartments)

print "New compartments in $recent_db compared to $previous_db\n\n";
print_compartments(@new_compartments);

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

sub get_new_instanaces {
    my $old_instances = shift;
    my $recent_instances = shift;
    
    my %db_id_to_instance;
    $db_id_to_instance{$_->db_id} = $_ foreach @$old_instances;
    
    my @new_instances;
    foreach my $recent_instance (@$recent_instances) {
        push @new_instance, $recent_instance unless (exists $db_id_to_instance{$recent_instance->db_id});
    }
    
    return @new_instances;
}

sub print_compartments {
    my @compartments = @_;
    
    foreach my $compartment (sort {$a->displayName cmp $b->displayName} @compartments) {
        print join("\t",
            $compartment->db_id,
            $compartment->displayName,
            $compartment->accession->[0]) . "\n";
    }
}

sub usage_instructions {
    return <<END;

Usage: perl $0

END
}