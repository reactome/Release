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

my @instances = grep {
    any {$_->db_id == $extracellular_space_go_instance->db_id} @{$_->compartment}
} map { @{$dba->fetch_instance(-CLASS => $_)} } ('PhysicalEntity', 'Event');

foreach my $instance (@instances) {
    my @compartments = @{$instance->compartment};
    my $has_extracellular_region = any { $_->db_id == $extracellular_region_go_instance->db_id } @compartments;
    my @repaired_compartments = grep { defined } map { 
        if ($_->db_id == $extracellular_space_go_instance->db_id) {
            !$has_extracellular_region ? $extracellular_region_go_instance : undef;
        } else {
            $_;
        }
    } @compartments;

    say get_name_and_id($instance) . ': ' . join("\t", map{ $_->displayName} @compartments);

    $instance->compartment(@repaired_compartments);
    $instance->namedInstance;
    $instance->modified(@{$instance->modified});
    $instance->add_attribute_value('modified', $instance_edit);
    $dba->update_attribute($instance, 'compartment');
    $dba->update_attribute($instance, '_displayName');
    $dba->update_attribute($instance, 'modified');

    say get_name_and_id($instance) . ': ' . join("\t", map{ $_->displayName} @{$instance->compartment});
}
