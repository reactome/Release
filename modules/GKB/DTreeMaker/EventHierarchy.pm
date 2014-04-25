package GKB::DTreeMaker::EventHierarchy;

use strict;
use vars qw(@ISA);
use Carp;
use GKB::DTreeMaker::AbstractHierarchy;
use GKB::Utils;
use GKB::Config qw($ELECTRONICALLY_INFERRED_REACTION_COLOR $MANUALLY_INFERRED_REACTION_COLOR $CONFIRMED_REACTION_COLOR);
@ISA = qw(GKB::DTreeMaker::AbstractHierarchy);

#use Data::Dumper;

sub _set_js_dtree_var_name {
    my ($self,$instance) = @_;
    $instance || confess("Need Event.");
    $self->SUPER::_set_js_dtree_var_name($instance,@_);
#    # This is a rather horrible hack to get rid of the hierarchy type button in case of simplified Event model
#    if ($instance->ontology->is_valid_class('EquivalentEventSet')) {
#	$self->use_hierarchy_type(1);
#    }
}

sub create_tree {
    my ($self,$instance) = @_;
    $instance || confess("Need Event.");
    $instance->is_a('Event') || confess("Need Event, got " . $instance->extended_displayName);
    my $ar = $instance->follow_class_attributes(-INSTRUCTIONS => {'Event' => {'reverse_attributes' => [qw(hasComponent hasMember hasSpecialisedForm hasEvent)]}});
    $self->_set_js_dtree_var_name($instance);
    my $d = $self->js_dtree_var_name;
    my $db_id = $instance->db_id;
    @{$ar} = grep {
	    ! $_->reverse_attribute_value('hasComponent')->[0] &&
	    ! $_->reverse_attribute_value('hasMember')->[0] &&
	    ! $_->reverse_attribute_value('hasSpecialisedForm')->[0] &&
	    ! $_->reverse_attribute_value('hasEvent')->[0]
    } @{$ar};
    @{$ar} = sort {$a->displayName cmp $b->displayName} @{$ar};
    unless(@{$ar}) {
	confess("No top level Event! Seems that there is a cycle in the event hierarchy.\n");
    }    
    my $out = ''
    . $self->controls($db_id)
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
    . qq{$d.openToDbId($db_id,true);\n}
#    . qq{$d.selectNode($db_id);\n}
    . qq{</script>\n}
     ;
    return $out;
}

sub create_tree_wo_controls {
    my ($self,$instance) = @_;
    $instance || confess("Need Event.");
    $instance->is_a('Event') || confess("Need Event, got " . $instance->extended_displayName);
    my $ar = $instance->follow_class_attributes(-INSTRUCTIONS => {'Event' => {'reverse_attributes' => [qw(hasComponent hasMember hasSpecialisedForm hasEvent)]}});
    $self->_set_js_dtree_var_name($instance);
    my $d = $self->js_dtree_var_name;
    my $db_id = $instance->db_id;
    @{$ar} = grep {
	    ! $_->reverse_attribute_value('hasComponent')->[0] &&
	    ! $_->reverse_attribute_value('hasMember')->[0] &&
	    ! $_->reverse_attribute_value('hasSpecialisedForm')->[0] &&
	    ! $_->reverse_attribute_value('hasEvent')->[0]
    } @{$ar};
    @{$ar} = sort {$a->displayName cmp $b->displayName} @{$ar};
    unless(@{$ar}) {
	confess("No top level Event! Seems that there is a cycle in the event hierarchy.\n");	
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
    . qq{$d.openToDbId($db_id,true);\n}
#    . qq{$d.selectNode($db_id);\n}
    . qq{</script>\n}
     ;
    return $out;
}

sub create_pruned_tree {
    my ($self,$instances) = @_;
    my $start = time();
    @{$instances} || confess("Need instances.");
    $self->_sort_by_cached_p_value($instances);
    $self->_set_js_dtree_var_name($instances->[0]);
    my $d = $self->js_dtree_var_name;
    my (%h,@roots);
    foreach my $instance (@{$instances}) {
	my $ar = $instance->follow_class_attributes
	    (-INSTRUCTIONS => {'Event' => {'reverse_attributes' => [qw(hasComponent hasMember hasSpecialisedForm hasEvent)]}});
	push @roots, grep {!$h{$_->db_id}++} grep {
	    ! $_->reverse_attribute_value('hasComponent')->[0] &&
	    ! $_->reverse_attribute_value('hasMember')->[0] &&
	    ! $_->reverse_attribute_value('hasSpecialisedForm')->[0] &&
	    ! $_->reverse_attribute_value('hasEvent')->[0]
	} @{$ar};
	map {$_->set_cached_value('is_on_path',1)} @{$ar};
    }
#    @roots = sort {$a->displayName cmp $b->displayName} @roots;
    unless(@roots) {
	confess("No top level Event! Seems that there is a cycle in the event hierarchy.\n");	
    }
    my $out = ''
    . $self->controls2
    . qq{<script type="text/javascript">\n}
    . qq{var $d = new dTree('$d');\n}
    . qq{$d.add(0,-1,0,'');\n}
    ;
    $self->reset_next_node_id;
    foreach my $t (@roots) {
	$out .= $self->_add_node_on_path($t, 0);
    }
    $out .= qq{document.write($d);\n}
    . qq{$d.setHierarchyTypeVisibility();\n}
    . qq{</script>\n}
     ;
#    printf qq(<PRE>%s\t%s</PRE>\n), (caller(0))[3], (time() - $start);
    return $out;
}

sub create_pruned_tree_for_root {
    my ($self,$instances,$root) = @_;
    @{$instances} || confess("Need instances.");
    $self->_set_js_dtree_var_name($instances->[0]);
    my $d = $self->js_dtree_var_name;
    map {$_->set_cached_value('is_on_path',1)} @{$instances};
    my $out = ''
    . $self->controls2
    . qq{<script type="text/javascript">\n}
    . qq{var $d = new dTree('$d');\n}
    . qq{$d.add(0,-1,0,'');\n}
    ;
    $self->reset_next_node_id;
    $out .= $self->_add_node_on_path($root, 0);
    $out .= qq{document.write($d);\n}
    . qq{$d.setHierarchyTypeVisibility();\n}
    . qq{</script>\n}
     ;
    return $out;
}

sub _add_node {
    my ($self,$node,$parent_id,$typeIcon) = @_;
    if ($self->{'looptracker'}->{"$node"}++) {
	confess($node->extended_displayName . " refers to itself in event hierarchy.\n");
    }
    $typeIcon ||= "null";
#    my $icon = ($node->is_a('Pathway') || $node->is_a('ConceptualEvent')) ? '"/icons/dtree/Pathway.gif"' : '"/icons/dtree/Reaction.gif"';
    my $icon = $self->_get_node_icon($node); # TMP HACK
    my $node_id = $self->next_node_id;
    my $db_id = $node->db_id;
    my $lbl = $self->_get_label($node);
    my $url = $self->urlmaker->urlify($node);
    my $d = $self->js_dtree_var_name;
    my $out = qq{$d.add($node_id,$parent_id,$db_id,'$lbl',"$url",null,null,$icon,$icon,null,$typeIcon);\n};
#    if ($node->is_a('EquivalentEventSet') || $node->is_a('Reaction')) {
    if ($node->is_valid_attribute('hasMember')) {
	foreach my $i (@{$node->HasMember}) {
	    $out .= $self->_add_node($i,$node_id,qq('<SPAN CLASS="attributename" STYLE="background-color:red;">m</SPAN>'));
	}
    }
    if ($node->is_a('ConceptualEvent')) {
	foreach my $i (@{$node->HasSpecialisedForm}) {
	    $out .= $self->_add_node($i,$node_id,qq('<SPAN CLASS="attributename" STYLE="background-color:green;">s</SPAN>'));
	}
    }
    if ($node->is_valid_attribute('hasComponent')) {
	foreach my $i (@{$node->HasComponent}) {
	    $out .= $self->_add_node($i,$node_id,qq('<SPAN CLASS="attributename" STYLE="background-color:navy;">c</SPAN>'));
	}
    }
    if ($node->is_valid_attribute('hasEvent')) {
	foreach my $i (@{$node->HasEvent}) {
	    $out .= $self->_add_node($i,$node_id,qq('<SPAN CLASS="attributename" STYLE="background-color:navy;">c</SPAN>'));
	}
    }
    delete $self->{'looptracker'}->{"$node"};
    return $out;
}

sub _add_node_on_path {
    my ($self,$node,$parent_id,$typeIcon) = @_;
    if ($self->{'looptracker'}->{"$node"}++) {
	confess($node->extended_displayName . " refers to itself in event hierarchy.\n");
    }
    $typeIcon ||= "null";
    my $icon = $self->_get_node_icon($node);
    my $node_id = $self->next_node_id;
    my $db_id = $node->db_id;
    my $lbl = $self->_get_coloured_label($node);
    my $url = $self->urlmaker->urlify($node);
    my $d = $self->js_dtree_var_name;
#    my $matchingIds = matching_identifiers_as_extra_string($node);
#    my $extraStr = sprintf "%1.1e", $node->get_cached_value('p_value');
    my $extraStr = $self->_p_value_and_counts_string($node);
    my $out = qq{$d.add($node_id,$parent_id,$db_id,'$lbl',"$url",null,null,$icon,$icon,null,$typeIcon,'$extraStr');\n};
    if ($node->get_cached_value('matching_submitted_identifiers')) {
	$out .= $self->_add_participant_nodes_coloured_by_value($node,$node_id);
    }
#    if ($node->is_a('EquivalentEventSet') || $node->is_a('Reaction')) {
    if ($node->is_valid_attribute('hasMember')) {
	foreach my $i (grep {$_->get_cached_value('is_on_path')} @{$self->_sort_by_cached_p_value($node->HasMember)}) {
	    $out .= $self->_add_node_on_path($i,$node_id,qq('<SPAN CLASS="attributename" STYLE="background-color:red;">m</SPAN>'));
	}
    }
    if ($node->is_a('ConceptualEvent')) {
	foreach my $i (grep {$_->get_cached_value('is_on_path')} @{$self->_sort_by_cached_p_value($node->HasSpecialisedForm)}) {
	    $out .= $self->_add_node_on_path($i,$node_id,qq('<SPAN CLASS="attributename" STYLE="background-color:green;">s</SPAN>'));
	}
    }
    if ($node->is_valid_attribute('hasComponent')) {
	foreach my $i (grep {$_->get_cached_value('is_on_path')} @{$self->_sort_by_cached_p_value($node->HasComponent)}) {
	    $out .= $self->_add_node_on_path($i,$node_id,qq('<SPAN CLASS="attributename" STYLE="background-color:navy;">c</SPAN>'));
	}
    }
    if ($node->is_valid_attribute('hasEvent')) {
	foreach my $i (grep {$_->get_cached_value('is_on_path')} @{$self->_sort_by_cached_p_value($node->HasEvent)}) {
	    $out .= $self->_add_node_on_path($i,$node_id,qq('<SPAN CLASS="attributename" STYLE="background-color:navy;">c</SPAN>'));
	}
    }
    delete $self->{'looptracker'}->{"$node"};
    return $out;
}

sub _p_value_and_counts_string {
    my ($self,$node) = @_;
    my $participant_ar = $node->get_cached_value('matching_submitted_identifiers');
    my ($up, $down) = (0,0);
    map {$_->[2] && $_->[2]->[0] && (($_->[2]->[0] > 0) ? $up++ : $down++)} @{$participant_ar}; #if the submitted identifier list came with values, a count is done as to how many identifiers mapped to the given event are positive, and how many are negative (only the first value column is considered)
    my $lbl = sprintf "%1.1e, %i/%i", $node->get_cached_value('p_value'), scalar(@{$participant_ar}), $node->get_cached_value('geneCount');
    if ($up || $down) {
	$lbl .= sprintf qq/ (%i up, %i down)/, $up, $down;
    }
    return $lbl;
}

sub _add_participant_nodes {
    my ($self,$node,$parent_id) = @_;
    my $typeIcon = "null";
    my $icon = "null";
    my $db_id = "null";
    my $d = $self->js_dtree_var_name;
    my $node_id = $self->next_node_id;
    my $participant_ar = $node->get_cached_value('matching_submitted_identifiers');
    my $lbl = sprintf "%i/%i matching identifiers", scalar(@{$participant_ar}), $node->get_cached_value('geneCount');
    my $out = qq{$d.add($node_id,$parent_id,$db_id,'<SPAN CLASS="small">$lbl</SPAN>',null,null,null,$icon,$icon,null,$typeIcon);\n};
    $parent_id = $node_id;
    my $urlmaker = $self->urlmaker;
    foreach my $ar (@{$participant_ar}) {
	$node_id = $self->next_node_id;
	my $id = $ar->[0];
#	my $extraStr = _participant_links($urlmaker,$ar->[1]) . ' ' . join(', ',map {sprintf "%1.1e", $_} @{$ar->[2]});
	my $extraStr = _participant_links($urlmaker,$ar->[1]) . ' ' . 
	    join(', ',map {(/[a-zA-Z]/) ?  sprintf "%s", $_ : sprintf "%1.1e", $_} @{$ar->[2]});
	$out .= qq{$d.add($node_id,$parent_id,$db_id,'<SPAN CLASS="small">$id</SPAN>',null,null,null,$icon,$icon,null,$typeIcon,'<SPAN CLASS="small">$extraStr</SPAN>');\n};
    }
    return $out;
}

sub _add_participant_nodes_coloured_by_value1 {
    my ($self,$node,$parent_id) = @_;
    my $typeIcon = "null";
    my $icon = "null";
    my $db_id = "null";
    my $d = $self->js_dtree_var_name;
    my $node_id = $self->next_node_id;
    my $participant_ar = $node->get_cached_value('matching_submitted_identifiers');
    my ($up, $down) = (0,0);
    map {$_->[2] && $_->[2]->[0] && (($_->[2]->[0] > 0) ? $up++ : $down++)} @{$participant_ar};
    my $lbl = sprintf "%i/%i matching identifiers", scalar(@{$participant_ar}), $node->get_cached_value('geneCount');
    if ($up || $down) {
	$lbl .= sprintf qq/ (%i up, %i down)/, $up, $down;
    }
    my $out = qq{$d.add($node_id,$parent_id,$db_id,'<SPAN CLASS="small">$lbl</SPAN>',null,null,null,$icon,$icon,null,$typeIcon);\n};
    $parent_id = $node_id;
    my $urlmaker = $self->urlmaker;
    foreach my $ar (@{$participant_ar}) {
	$node_id = $self->next_node_id;
	my $id = $ar->[0];
	my $cls = ($ar->[2] && $ar->[2]->[0]) ? (($ar->[2]->[0] > 0) ? 'up' : 'down') : 'no';
	my $extraStr = _participant_links($urlmaker,$ar->[1]);
	$out .= qq{$d.add($node_id,$parent_id,$db_id,'<SPAN CLASS="small">$id</SPAN>',null,null,null,$icon,$icon,null,$typeIcon,'<SPAN CLASS="$cls">$extraStr</SPAN>');\n};
    }
    return $out;
}

sub _add_participant_nodes_coloured_by_value {
    my ($self,$node,$parent_id) = @_;
    my $typeIcon = "null";
    my $icon = "null";
    my $db_id = "null";
    my $d = $self->js_dtree_var_name;
    my $node_id = $self->next_node_id;
    my $participant_ar = $node->get_cached_value('matching_submitted_identifiers'); #this list of identifiers is sorted by the value of the first value column, if available
    my $lbl = sprintf "Matching identifiers";
    my $out = qq{$d.add($node_id,$parent_id,$db_id,'<SPAN CLASS="small">$lbl</SPAN>',null,null,null,$icon,$icon,null,$typeIcon);\n};
    $parent_id = $node_id;
    my $urlmaker = $self->urlmaker;
    foreach my $ar (@{$participant_ar}) {
	$node_id = $self->next_node_id;
	my $id = $ar->[0];
	my $cls = ($ar->[2] && $ar->[2]->[0]) ? (($ar->[2]->[0] > 0) ? 'up' : 'down') : 'no'; #up/down refers to positive/negative values in the first value column
	my $extraStr = _participant_links($urlmaker,$ar->[1]);
	$out .= qq{$d.add($node_id,$parent_id,$db_id,'<SPAN CLASS="small">$id</SPAN>',null,null,null,$icon,$icon,null,$typeIcon,'<SPAN CLASS="$cls">$extraStr</SPAN>');\n};
    }
    return $out;
}

sub _participant_links {
    my ($urlmaker,$ar) = @_;
    my %names;
    map {$names{$_->displayName}++} @{$ar};
    return sprintf qq( <A HREF="%s">%s</A>) , $urlmaker->urlify_db_ids(map {@{$_->Db_id_in_main_db}} @{$ar}), join(', ', keys %names);
#    my $out = '';
#    foreach my $p (@{$ar}) {
#	$out .= sprintf qq( <A HREF="%s">%s</A>) , $urlmaker->urlify_db_ids(@{$p->Db_id_in_main_db}), $p->displayName;
#    }
#    return $out;
}

sub controls {
    my ($self,$db_id) = @_;
    my $d = $self->js_dtree_var_name;
    return qq{
<div class="controls">
<a href="javascript: $d.closeAll(); $d.openToDbId($db_id,true);">open to selected event</a> 
<a href="javascript: $d.openAll();">open all</a>
<a href="javascript: $d.closeAll();">close all</a>} .
($self->use_hierarchy_type
? qq{\n<a href="javascript: $d.toggleTypeIcon()">show/hide hierarchy types</a>}
: '') .
qq{
</div>
};
}

sub controls2 {
    my $self = shift;
    my $d = $self->js_dtree_var_name;
    return qq{
<div class="controls">
<a href="javascript: $d.openAll();">open all</a> | 
<a href="javascript: $d.closeAll();">close all</a>} .
($self->use_hierarchy_type
? qq{ |\n<a href="javascript: $d.toggleTypeIcon()">show/hide hierarchy types</a>}
: '') .
qq{
</div>
};
}

sub boxed_controls {
    my ($self,$i) = @_;
    $self->js_dtree_var_name || $self->_set_js_dtree_var_name($i);
    my $d = $self->js_dtree_var_name;
    my $db_id = $i->db_id;
    return qq{
<div class="controls">
<table><tr>
<td onclick="javascript: $d.closeAll(); $d.openToDbId($db_id,true);">open to selected event</td>
<td onclick="javascript: $d.openAll();">open all</td> 
<td onclick="javascript: $d.closeAll();">close all</td>} .
($self->use_hierarchy_type 
? qq{<td onclick="javascript: $d.toggleTypeIcon()">show/hide hierarchy types</td>}
: '') .
qq{
</tr></table>
</div>
};
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
    } elsif ($node->is_a('BlackBoxEvent')) {
	return  '/icons/dtree/BlackBoxEvent.gif';
    } elsif ($node->is_a('Polymerisation')) {
	return  '/icons/dtree/Polymerization.gif';
    } elsif ($node->is_a('Depolymerisation')) {
	return  '/icons/dtree/Depolymerization.gif';
    } else {
	return undef;
    }
}

sub _get_label {
    my ($self,$node) = @_;
    return get_label($node);
}

sub get_label {
    my $node = shift;
    my $lbl = GKB::Utils::escape($node->displayName);

    # Remove newlines from label, otherwise auto-generated Javascript will rebel
    $lbl =~ s/\n+/ /g;
    $lbl =~ s/ +$//g;
    $lbl =~ s/^ +//g;

    my $rgb;
    if ($node->EvidenceType->[0]) {
	$rgb = $ELECTRONICALLY_INFERRED_REACTION_COLOR;
    } elsif ($node->InferredFrom->[0]) {
	$rgb = $MANUALLY_INFERRED_REACTION_COLOR;
    } else {
	$rgb = $CONFIRMED_REACTION_COLOR;
    }
    return qq{<SPAN STYLE="color:rgb(} . join(',', @{$rgb}) . qq{);">$lbl</SPAN>};
}

sub _get_coloured_label {
    my ($self,$node) = @_;
    my $lbl = GKB::Utils::escape($node->displayName);
    my $rgb = $node->get_cached_value('rgb') || [255,255,255];
    my $font_rgb = (($rgb->[0] == 0) && ($rgb->[1] == 0)) ? [255,255,255] : [0,0,0];
    if ($rgb && @{$rgb}) {
#	return qq{<SPAN STYLE="background-color:rgb(} . join(',', @{$rgb}) . qq{);">$lbl</SPAN>};
	return qq{<SPAN STYLE="background-color:rgb(} . join(',', @{$rgb}) . qq{);color:rgb(} . join(',', @{$font_rgb}) . qq{);">$lbl</SPAN>};
    } else {
	return $lbl;
    }
}

sub matching_identifiers_as_extra_string {
    my $node = shift;
    my $ar = $node->get_cached_value('matching_submitted_identifiers');
    return join(',', @{$ar});
}

sub _sort_by_cached_p_value {
    my ($self,$ar) = @_;
    @{$ar} = sort {(defined $a->get_cached_value('p_value') ? $a->get_cached_value('p_value') : 1) <=> 
    (defined $b->get_cached_value('p_value') ? $b->get_cached_value('p_value') : 1)} @{$ar};
    return $ar;
}


1;
