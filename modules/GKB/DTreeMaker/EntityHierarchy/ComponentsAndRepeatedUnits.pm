package GKB::DTreeMaker::EntityHierarchy::ComponentsAndRepeatedUnits;

# Given and Physical- or ReferenceEntity, create dynamic html composition hierarchies
# for all Complexes and Polymers containing this Entity directly, i.e. not via a set.
# hasMember attribute is followed neither when finding the top level entities and nor
# when finding the components.

use strict;
use vars qw(@ISA);
use Carp;
use GKB::DTreeMaker::EntityHierarchy;
@ISA = qw(GKB::DTreeMaker::EntityHierarchy);


sub create_tree_wo_controls {
    my ($self,$instance) = @_;
    $instance->is_a('PhysicalEntity') || $instance->is_a('ReferenceEntity') || confess("Need PhysicalEntity or ReferenceEntity, got " . $instance->extended_displayName);
    my $ar = $instance->follow_class_attributes(-INSTRUCTIONS => {'PhysicalEntity' => {'reverse_attributes' => [qw(hasComponent repeatedUnit)]}, 'ReferenceEntity' => {'reverse_attributes' => [qw(referenceEntity)]}}, -OUT_CLASSES => [qw(PhysicalEntity)]);
    if ($instance->isa('GKB::PrettyInstance')) {
	my $tmp = $instance->grep_for_instances_with_focus_species($ar);
	# Consider reporting this case somehow
	$ar = $tmp if (@{$tmp});
    }
    $self->_set_js_dtree_var_name($instance);
    my $d = $self->js_dtree_var_name;
    my $db_id = $instance->db_id;
    @{$ar} = grep {
	    ! $_->reverse_attribute_value('hasComponent')->[0] &&
	    ! $_->reverse_attribute_value('repeatedUnit')->[0]
    } @{$ar};
    @{$ar} = sort {$a->displayName cmp $b->displayName} @{$ar};
    unless(@{$ar}) {
	confess("No top level Entity! Seems that there is a cycle in the Entity hierarchy.\n");
    }    
    my $out = ''
    . qq{<script type="text/javascript">\n}
    . qq{var $d = new dTree('$d');\n}
    . qq{$d.add(0,-1,0,'');\n}
    ;
    $self->reset_next_node_id;
    foreach my $t (@{$ar}) {
	$out .= $self->_add_node($t, 0);
    }
    $out .= qq{document.write($d);\n}
    . qq{$d.setHierarchyTypeVisibility();\n}
    . qq{$d.closeAll();\n};
    if ($instance->is_a('ReferenceEntity')) {
	foreach my $i (@{$instance->reverse_attribute_value('referenceEntity')}) {
	    $out .= sprintf qq{$d.openToDbId(%i,true);\n}, $i->db_id;
	}
    } else {
	$out .= qq{$d.openToDbId($db_id,true);\n};
    }
    $out .= qq{</script>\n};
    return $out;
}

sub _add_node {
    my ($self,$node,$parent_id,$typeIcon,$coefficient) = @_;
    if ($self->{'looptracker'}->{"$node"}++) {
	confess($node->extended_displayName . " refers to itself in event hierarchy.\n");
    }
    $typeIcon ||= "null";
    my $icon = $self->_get_node_icon($node); # TMP HACK
    my $node_id = $self->next_node_id;
    my $db_id = $node->db_id;
    my $lbl = $self->_get_label($node);
    if ($coefficient and $coefficient > 1) {
	$lbl = "$coefficient x $lbl";
    }
    my $url = $self->urlmaker->urlify($node);
    my $d = $self->js_dtree_var_name;
#    my $out = qq{$d.add($node_id,$parent_id,$db_id,'$lbl',"$url",null,null,$icon,$icon,null,$typeIcon);\n};
    my $out = qq{$d.add($node_id,$parent_id,$db_id,'$lbl',null,null,null,$icon,$icon,null,$typeIcon);\n};
    if ($node->is_a('Complex')) {
	my %h;
	map {$h{$_->db_id}++} @{$node->HasComponent};
	foreach my $i (@{$node->HasComponent}) {
	    if (my $coeff = $h{$i->db_id}) {
		$out .= $self->_add_node($i,$node_id,qq('<SPAN CLASS="attributename" STYLE="background-color:navy;">c</SPAN>'),$coeff);
		delete $h{$i->db_id};
	    }
	}
    } elsif ($node->is_a('Polymer')) {
	foreach my $i (@{$node->RepeatedUnit}) {
	    $out .= $self->_add_node($i,$node_id,qq('<SPAN CLASS="attributename" STYLE="background-color:blue;">u</SPAN>'));
	}
    }
    delete $self->{'looptracker'}->{"$node"};
    return $out;
}


1;
