package GKB::DTreeMaker::EntityHierarchy;

use strict;
use vars qw(@ISA);
use Carp;
use GKB::DTreeMaker::AbstractHierarchy;
@ISA = qw(GKB::DTreeMaker::AbstractHierarchy);

#use Data::Dumper;

sub create_tree {
    my ($self,$instance) = @_;
    $self->_set_js_dtree_var_name($instance);
    return $self->controls($instance) . $self->create_tree_wo_controls($instance);
}

sub create_tree_wo_controls {
    my ($self,$instance) = @_;
    $instance->is_a('PhysicalEntity') || $instance->is_a('ReferenceEntity') || confess("Need PhysicalEntity or ReferenceEntity, got " . $instance->extended_displayName);
    my $ar = $instance->follow_class_attributes(-INSTRUCTIONS => {'PhysicalEntity' => {'reverse_attributes' => [qw(hasComponent hasMember hasCandidate repeatedUnit)]}, 'ReferenceEntity' => {'reverse_attributes' => [qw(referenceEntity)]}}, -OUT_CLASSES => [qw(PhysicalEntity)]);
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
	    ! $_->reverse_attribute_value('hasMember')->[0] &&
	    ! $_->reverse_attribute_value('repeatedUnit')->[0] &&
	    ! $_->reverse_attribute_value('hasCandidate')->[0]
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
    . qq{$d.setHierarchyTypeVisibility();\n};
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
    my ($self,$node,$parent_id,$typeIcon,$coefficient,$is_on_other_cell) = @_;
    if ($self->{'looptracker'}->{"$node"}++) {
	confess($node->extended_displayName . " refers to itself in event hierarchy.\n");
    }
    $typeIcon ||= "null";
    my $icon = $self->_get_node_icon($node); # TMP HACK
    my $node_id = $self->next_node_id;
    my $db_id = $node->db_id;
    my $lbl = $self->_get_label($node,$is_on_other_cell);
    if ($coefficient and $coefficient > 1) {
	$lbl = "$coefficient x $lbl";
    }
    my $url = $self->urlmaker->urlify($node);
    my $d = $self->js_dtree_var_name;
#    my $out = qq{$d.add($node_id,$parent_id,$db_id,'$lbl',"$url",null,null,$icon,$icon,null,$typeIcon);\n};
    my $out = qq{$d.add($node_id,$parent_id,$db_id,'$lbl',null,null,null,$icon,$icon,null,$typeIcon);\n};
    if ($node->is_a('EntitySet')) {
	foreach my $i (@{$node->HasMember}) {
	    $out .= $self->_add_node($i,$node_id,qq('<SPAN CLASS="attributename" STYLE="background-color:red;">m</SPAN>'));
	}
    }
    if ($node->is_a('CandidateSet')) {
	foreach my $i (@{$node->HasCandidate}) {
	    $out .= $self->_add_node($i,$node_id,qq('<SPAN CLASS="attributename" STYLE="background-color:orange;">m</SPAN>'));
	}
    }
    if ($node->is_a('Complex')) {
        my %h;
        map {$h{$_->db_id}++} @{$node->HasComponent};
        my %h2;
        if ($node->is_valid_attribute('entityOnOtherCell')) {
            map {$h2{$_->db_id}++} @{$node->EntityOnOtherCell};
        }
        foreach my $i (@{$node->HasComponent}) {
#the following conditions make sure the correct number of instances of a given id get assigned the "normal" compartment display name vs the "other cell" compartment display name - each condition should be entered only once at the most
	    if ($h{$i->db_id} && ($h{$i->db_id} > $h2{$i->db_id})) { #if at all, this condition is fulfilled first for any given id, as h>=h2 => "normal" compartment display name appears first
                $out .= $self->_add_node($i,$node_id,qq('<SPAN CLASS="attributename" STYLE="background-color:navy;">c</SPAN>'),($h{$i->db_id} - $h2{$i->db_id}));
                $h{$i->db_id} = $h2{$i->db_id}; #now the elsif condition is fulfilled (if h2 exists)
	    } elsif ($h2{$i->db_id} && ($h2{$i->db_id} == $h{$i->db_id})) { #all (remaining) instances of the given id should get the "other cell compartment" display name
                $out .= $self->_add_node($i,$node_id,qq('<SPAN CLASS="attributename" STYLE="background-color:navy;">c</SPAN>'),$h2{$i->db_id},1);
                delete $h{$i->db_id}; #can't enter if condition any more for the given id
	    }
        }
    }
    if ($node->is_a('Polymer')) {
	foreach my $i (@{$node->RepeatedUnit}) {
	    $out .= $self->_add_node($i,$node_id,qq('<SPAN CLASS="attributename" STYLE="background-color:blue;">u</SPAN>'));
	}
    }
    delete $self->{'looptracker'}->{"$node"};
    return $out;
}

sub controls {
    my ($self,$instance) = @_;
    my $d = $self->js_dtree_var_name;
    my $out = qq{
<div class="controls">
<a href="javascript: $d.closeAll();};
    if ($instance->is_a('ReferenceEntity')) {
	foreach my $i (@{$instance->reverse_attribute_value('referenceEntity')}) {
	    $out .= sprintf qq{$d.openToDbId(%i,true);}, $i->db_id;
	}
    } else {
	$out .= sprintf qq{$d.openToDbId(%i,true);}, $instance->db_id;
    }
    $out .= qq{">open to selected entity</a> | 
<a href="javascript: $d.openAll();">open all</a> | 
<a href="javascript: $d.closeAll();">close all</a> | 
<a href="javascript: $d.toggleTypeIcon()">show/hide hierarchy types</a>
</div>
};
    $out .=  $self->hierarchy_type_key;
    return $out;
}

sub downward_controls {
    my ($self,$instance) = @_;
    my $d = $self->js_dtree_var_name;
    return qq{
<div class="controls">
<a href="javascript: $d.openAll();">open all</a>
<a href="javascript: $d.closeAll();">close all</a> 
<a href="javascript: $d.toggleTypeIcon()">show/hide hierarchy types</a>
</div>
} . $self->hierarchy_type_key;
}

sub _get_node_icon {
    my ($self,$node) = @_;
    my $path = get_node_icon($node);
    if ($path) {
	return '"' . $path . '"';
    } else {
	return "null";
    }
}

sub get_node_icon {
    my $node = shift;
    if ($node->is_a('Pathway')) {
	return '/icons/dtree/Pathway.gif';
    } elsif ($node->is_a('Reaction')) {
	return  '/icons/dtree/Reaction.gif';
    } elsif ($node->is_a('ConceptualEvent')) {
	return  '/icons/dtree/ConceptualEvent.gif';
    } elsif ($node->is_a('EquivalentEventSet')) {
	return  '/icons/dtree/EquivalentEventSet.gif';
    } else {
	return undef;
    }
}

sub _get_cached_label {
    my ($self,$node) = @_;
    my $lbl;
    unless ($lbl = $node->get_cached_value('label')) {
	my $tmp = $node->displayName;
	$tmp =~ s/'/`/g;
	$tmp =~ s/"/&quot;/g;
	$node->displayName($tmp);
	$node->prettyfy(-URLMAKER => $self->urlmaker, -WEBUTILS => $self->webutils, -SUBCLASSIFY => 1);
	$lbl = $node->hyperlinked_displayName;
	$lbl =~ s/'/&quot;/gms;
	$node->set_cached_value('label',$lbl);
    }
    return $lbl;
}

sub _get_label {
    my ($self,$node,$is_on_other_cell) = @_;
    my $lbl;
    my $tmp = $node->displayName;
    $tmp =~ s/'/`/g;
    $tmp =~ s/"/&quot;/g;
    $node->displayName($tmp);
    $node->prettyfy(-URLMAKER => $self->urlmaker, -WEBUTILS => $self->webutils, -SUBCLASSIFY => 1);
    $lbl = $node->hyperlinked_displayName($is_on_other_cell);
    $lbl =~ s/'/&quot;/gms;
    return $lbl;
}

sub hierarchy_type_key {
    my $self = shift;
    return sprintf 
qq(<DIV CLASS="hierarchyType_%s"><DIV STYLE="font-size:8pt;padding-left:4px">
<SPAN CLASS="attributename" STYLE="background-color:red;">m</SPAN> - known member,
<SPAN CLASS="attributename" STYLE="background-color:orange;">m</SPAN> - possible member,
<SPAN CLASS="attributename" STYLE="background-color:navy;">c</SPAN> - component
</DIV></DIV>), $self->js_dtree_var_name;
}

1;
