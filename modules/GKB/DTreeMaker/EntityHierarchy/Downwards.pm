package GKB::DTreeMaker::EntityHierarchy::Downwards;

use strict;
use vars qw(@ISA);
use Carp;
use GKB::DTreeMaker::EntityHierarchy;
@ISA = qw(GKB::DTreeMaker::EntityHierarchy);


sub create_tree_wo_controls {
    my ($self,$instance) = @_;
    $instance->is_a('Complex') || $instance->is_a('Polymer') || $instance->is_a('EntitySet') || confess("Need Complex, Polymer or EntitySet, got " . $instance->extended_displayName);
    $self->_set_js_dtree_var_name($instance);
    my $d = $self->js_dtree_var_name;
    my $db_id = $instance->db_id;
    my $out = ''
    . qq{<script type="text/javascript">\n}
#    . qq{var $d = new dTree('$d');\n}
    . qq{var $d = new dTree('$d');\n}
    . qq{$d.add(0,-1,0,'');\n}
    ;
    $self->reset_next_node_id;
    $out .= $self->_add_node($instance, 0);
    $out .= qq{document.write($d);\n};
    $out .= qq{$d.setHierarchyTypeVisibility();\n};
    $out .= qq{$d.openToDbId($db_id,true);\n};
    $out .= qq{</script>\n};
    return $out;
}

sub controls {
    my $self = shift;
    return $self->SUPER::downward_controls;
}

1;
