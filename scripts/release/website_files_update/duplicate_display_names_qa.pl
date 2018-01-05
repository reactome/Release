#!/usr/local/bin/perl  -w
use strict;

use lib '/usr/local/gkb/modules';

use GKB::CommonUtils;
use GKB::Config;
use GKB::DBAdaptor;

use autodie;
use Carp;
use Getopt::Long;


my ($host, $db, $help);

&GetOptions(
    "host:s" => \$host,
    "db:s" => \$db,
    "help" => \$help
);

if ($help) {
    print usage_instructions();
    exit;
}

$db ||= $GK_DB_NAME;
$host ||= $GK_DB_HOST;

my %seen; # Prevents duplication in the file
open(my $output, ">", "duplicate_display_names_$db.txt");

my @events = @{get_dba($db, $host)->fetch_instance(-CLASS => 'Event')};

my %display_name_to_events;

foreach my $event (@events) {
    push @{$display_name_to_events{$event->displayName}}, $event;
}

my (@for_manual_check, @different_species);
foreach my $display_name (keys %display_name_to_events) {
    my @events = @{$display_name_to_events{$display_name}};
    next unless scalar @events > 1;
    
    my @event_records = map { join("\t", ($_->db_id, get_event_modifier($_))) } @events;
    my $display_name_record = join("\t", ($display_name, @event_records)) . "\n";
    
    if (scalar @events > 2 || !different_species($events[0], $events[1])) {
        push @for_manual_check, $display_name_record;
    } else {
        push @different_species, $display_name_record;
    }
}

print $output "Duplicated display names:\n\n";
print $output "$_" foreach @for_manual_check;
print $output "\n\n";
print $output "Duplicated display names with different species:\n\n";
print $output "$_" foreach @different_species;

close $output;

sub different_species {
    my $event1 = shift;
    my $event2 = shift;
    
    return different_values($event1->species, $event2->species, {'instance_type_attribute' => 1});
}

sub get_species_names {
    my $instance = shift;
    
    return join(', ', map {$_->displayName} @{$instance->species});
}

sub usage_instructions {
    return <<END;
    
    This script checks all events (pathways and reaction like events) in the database and
    reports duplicated display names in a text file with the name duplicated_display_names_<db>.txt.
    
    Usage: perl $0 [options]
    
    Options:
    
    -db [db_name]   Source database (default is $GKB::Config::GK_DB_NAME)
    -host [db_host] Host of source database (default is $GKB::Config::GK_DB_HOST)
    -help           Display these instructions
END
}