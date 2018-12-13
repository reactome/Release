#!/usr/bin/perl
use strict;
use warnings;

use feature qw/say/;
use List::MoreUtils qw/any/;

use lib '/usr/local/gkb/modules';

use GKB::CommonUtils;
use GKB::Utils_esther;

my $dba = get_dba('gk_central.sql', '127.0.0.1');
my $instance_edit = GKB::Utils_esther::create_instance_edit(
    $dba,
    'Weiser',
    'JD',
    'extracellular space compartment fix'
);
say "Instance edit id is " . $instance_edit->db_id;
my $extracellular_space_go_instance = $dba->fetch_instance_by_db_id('982')->[0];
my $extracellular_region_go_instance = $dba->fetch_instance_by_db_id('984')->[0];

my @physical_entities = grep {
    $_->compartment->[0] && any {$_->db_id == 982} @{$_->compartment} 
} @{$dba->fetch_instance(-CLASS => 'PhysicalEntity')};

foreach my $physical_entity (@physical_entities) {
    my @compartments = @{$physical_entity->compartment};
    my $has_extracellular_region = any { $_->db_id == $extracellular_region_go_instance->db_id } @compartments;
    my @repaired_compartments = map { 
        if ($_->db_id == $extracellular_space_go_instance->db_id) {
            !$has_extracellular_region ? $extracellular_region_go_instance : undef;
        } else {
            $_;
        }
    } @compartments;

    say get_name_and_id($physical_entity) . ': ' . join("\t", map{ $_->displayName} @compartments);

    $physical_entity->compartment(@repaired_compartments);
    $physical_entity->namedInstance;
    $physical_entity->modified(@{$physical_entity->modified});
    $physical_entity->add_attribute_value('modified', $instance_edit);
    $dba->update_attribute($physical_entity, 'compartment');
    $dba->update_attribute($physical_entity, '_displayName');
    $dba->update_attribute($physical_entity, 'modified');

    say get_name_and_id($physical_entity) . ': ' . join("\t", map{ $_->displayName} @{$physical_entity->compartment});
}
