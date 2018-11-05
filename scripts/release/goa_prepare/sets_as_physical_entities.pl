#!/usr/bin/perl
use strict;
use warnings;

use lib "/usr/local/gkb/modules";
use GKB::CommonUtils;

use autodie;
use Getopt::Long;
use List::MoreUtils qw/all any/;

# Database connection
our($host, $db, $help);
GetOptions('help' => \$help);

if ($help) {
    print "Usage: $0 -host db_host -db db_name\n";
    exit;
}
GetOptions('host:s' => \$host, 'db:s' => \$db);

my $dba = get_dba($db, $host);

(my $output_file_name = $0) =~ s/\.pl$//;
open(my $output, ">", "$output_file_name.txt");

my @catalyst_activities_with_sets = grep { physical_entity_is_an_EWAS_set($_) || active_unit_is_an_EWAS_set($_) } @{$dba->fetch_instance(-CLASS => 'CatalystActivity')};
print $output join("\t", qw/Catalyst_Activity Set Members/) . "\n";
foreach my $catalyst_activity (@catalyst_activities_with_sets) {
    my @sets;
    push @sets, $catalyst_activity->physicalEntity->[0] if physical_entity_is_an_EWAS_set($catalyst_activity);
    push @sets, grep { is_an_EWAS_set($_) } @{$catalyst_activity->activeUnit} if active_unit_is_an_EWAS_set($catalyst_activity);
    
    foreach my $set (@sets) {
        print $output join("\t", (get_name_and_id($catalyst_activity), get_name_and_id($set), join('|', map { get_name_and_id($_) } @{$set->hasMember}))) . "\n";
    }
}

sub physical_entity_is_an_EWAS_set {
    my $catalyst_activity = shift;
    
    return is_an_EWAS_set($catalyst_activity->physicalEntity->[0]);
}

sub active_unit_is_an_EWAS_set {
    my $catalyst_activity = shift;
    
    return any { is_an_EWAS_set($_) } @{$catalyst_activity->activeUnit};
}

sub is_an_EWAS_set {
    my $instance = shift;
    
    return $instance &&
           $instance->is_a('EntitySet') &&
           (all { $_->is_a('EntityWithAccessionedSequence') } @{$instance->hasMember});
}