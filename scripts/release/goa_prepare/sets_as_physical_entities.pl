#!/usr/bin/perl
use strict;
use warnings;

# This script will produce a report of catalyst activities whose physical entity or active unit(s)
# is a non EWAS set (i.e. a set whose members are not exclusively EWAS instances)

use lib "/usr/local/gkb/modules";
use GKB::CommonUtils;

use autodie;
use Getopt::Long;
# Functions which check a list to see if any/notall values return true for a specified condition
use List::MoreUtils qw/any notall/;

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

my @catalyst_activities_with_sets = grep { physical_entity_is_a_non_EWAS_set($_) || active_unit_is_a_non_EWAS_set($_) } @{$dba->fetch_instance(-CLASS => 'CatalystActivity')};
print $output join("\t", qw/Species Catalyst_Activity_ID Catalyst_Activity_Name Set_ID Set_Name Members/) . "\n";
foreach my $catalyst_activity (@catalyst_activities_with_sets) {
    my @sets;
    push @sets, $catalyst_activity->physicalEntity->[0] if physical_entity_is_a_non_EWAS_set($catalyst_activity);
    push @sets, grep { is_a_non_EWAS_set($_) } @{$catalyst_activity->activeUnit} if active_unit_is_a_non_EWAS_set($catalyst_activity);
    
    foreach my $set (@sets) {
        my $species_name = $set->species->[0] ? $set->species->[0]->displayName : 'N/A';
        print $output join("\t", ($species_name, $catalyst_activity->db_id, $catalyst_activity->displayName, $set->db_id, $set->displayName, join('|', map { get_name_and_id($_) } @{$set->hasMember}))) . "\n";
    }
}

sub physical_entity_is_a_non_EWAS_set {
    my $catalyst_activity = shift;
    
    return is_a_non_EWAS_set($catalyst_activity->physicalEntity->[0]);
}

sub active_unit_is_a_non_EWAS_set {
    my $catalyst_activity = shift;
    
    return any { is_a_non_EWAS_set($_) } @{$catalyst_activity->activeUnit};
}

sub is_a_non_EWAS_set {
    my $instance = shift;
    
    return $instance &&
           $instance->is_a('EntitySet') &&
           (notall { $_->is_a('EntityWithAccessionedSequence') } @{$instance->hasMember});
}