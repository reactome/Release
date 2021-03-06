#!/usr/bin/perl
use strict;
use warnings;

use feature qw/say/;
use List::MoreUtils qw/any/;

use lib '/usr/local/gkb/modules';

use GKB::CommonUtils;
use GKB::Utils_esther;

my $dba = get_dba('gk_central_20190101', 'reactomecurator.oicr.on.ca');
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

    if ($instance->is_a('PhysicalEntity')) {
	my @catalyst_activities = @{$instance->reverse_attribute_value('physicalEntity')};
	foreach my $catalyst_activity (@catalyst_activities) {
	    say get_name_and_id($catalyst_activity);

	    my $catalyst_activity_display_name = $catalyst_activity->displayName;
	    $catalyst_activity->namedInstance;

	    if ($catalyst_activity_display_name ne $catalyst_activity->displayName) {
	        $catalyst_activity->modified(@{$instance->modified});
	        $catalyst_activity->add_attribute_value('modified', $instance_edit);
	        $dba->update_attribute($catalyst_activity, '_displayName');
	        $dba->update_attribute($catalyst_activity, 'modified');
	    }

	    say get_name_and_id($catalyst_activity);
	}
    }

    say get_name_and_id($instance) . ': ' . join("\t", map{ $_->displayName} @{$instance->compartment});
}
