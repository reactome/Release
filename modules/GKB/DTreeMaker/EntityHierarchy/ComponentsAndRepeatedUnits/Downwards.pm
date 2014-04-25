package GKB::DTreeMaker::ComponentsAndRepeatedUnits::Downwards;

# Given a Complex or Polymer create a dynamic html tree showing the composition
# of the given entity. Set.hasMember attribute is not followed when finding the
# components.

use strict;
use vars qw(@ISA);
use Carp;
use GKB::DTreeMaker::EntityHierarchy::ComponentsAndRepeatedUnits;
@ISA = qw(GKB::DTreeMaker::EntityHierarchy::ComponentsAndRepeatedUnit);


sub create_tree_wo_controls {
    my ($self,$instance) = @_;
    $instance->is_a('Complex') || $instance->is_a('Polymer') || confess("Need Complex or Polymer, got " . $instance->extended_displayName);
    $self->_set_js_dtree_var_name($instance);
    my $d = $self->js_dtree_var_name;
    my $db_id = $instance->db_id;
    $self->reset_next_node_id;
    return
	qq{<script type="text/javascript">\n}
    . qq{var $d = new dTree('$d');\n}
    . qq{$d.add(0,-1,0,'');\n}
    . $self->_add_node($instance, 0)
    . qq{document.write($d);\n}
    . qq{$d.setHierarchyTypeVisibility();\n}
    . qq{$d.closeAll();\n}
    . qq{$d.openToDbId($db_id,true);\n}
    . qq{</script>\n};
}

sub controls {
    my $self = shift;
    return $self->SUPER::downward_controls;
}

1;
