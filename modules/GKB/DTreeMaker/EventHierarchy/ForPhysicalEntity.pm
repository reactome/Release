package GKB::DTreeMaker::EventHierarchy::ForPhysicalEntity;

use strict;
use vars qw(@ISA);
use Carp;
use GKB::DTreeMaker::EventHierarchy;
@ISA = qw(GKB::DTreeMaker::EventHierarchy);

sub create_tree {
    my ($self,$entities,$focus_species) = @_;
    @{$entities} || confess("Need nodes.");
    unless ($focus_species || $entities->[0]->Species->[0]) {
	confess("Need focus_species or entities with species");
    }
    my (%leafevents);
    foreach my $entity (@{$entities}) {
	#print $entity->extended_displayName, "\n";
	my $ar = $entity->follow_class_attributes
	    (-INSTRUCTIONS => {
		'PhysicalEntity' => {'reverse_attributes' => [qw(input output physicalEntity)]},
		'CatalystActivity' => {'reverse_attributes' => [qw(catalystActivity)]}
	     },
	     -OUT_CLASSES => [qw(ReactionlikeEvent)]
	    );
	if ($focus_species) {
	    @{$ar} = grep {$_->Species->[0] == $focus_species} @{$ar};
	}
	map {$leafevents{$_->db_id} = $_} @{$ar};
    }
    %leafevents || confess("No leaf events?!\n");
    # not used yet
    $self->{leafevents} = \%leafevents;
    $self->_set_js_dtree_var_name($entities->[0]);
    my $d = $self->js_dtree_var_name;
    my (@roots,%h);
    foreach my $instance (values %leafevents) {
	my $ar = $instance->follow_class_attributes
	    (-INSTRUCTIONS => {
		'Event' => {'reverse_attributes' => [qw(hasComponent hasMember hasSpecialisedForm hasEvent)]}
	     },
	     -OUT_CLASSES => [qw(Event)]
	    );
	push @roots, grep {!$h{$_->db_id}++} grep {
	    ! $_->reverse_attribute_value('hasComponent')->[0] &&
	    ! $_->reverse_attribute_value('hasMember')->[0] &&
	    ! $_->reverse_attribute_value('hasSpecialisedForm')->[0] &&
	    ! $_->reverse_attribute_value('hasEvent')->[0]
	} @{$ar};
	map {$_->set_cached_value('is_on_path',1)} @{$ar};
    }
    @roots = sort {$a->displayName cmp $b->displayName} @roots;
    unless(@roots) {
	confess("No top level Event! Seems that there is a cycle in the event hierarchy.\n");	
    }
    if (@roots == 1) {
	*main::GKB::DTreeMaker::EventHierarchy::ForEntityNode::_add_node = *main::GKB::DTreeMaker::EventHierarchy::_add_node;
    }
    my $out = ''
    . $self->controls2
    . qq{<script type="text/javascript">\n}
    . qq{var $d = new dTree('$d');\n}
    . qq{$d.add(0,-1,0,'');\n}
    ;
    $self->reset_next_node_id;
    foreach my $t (@roots) {
	$out .= $self->_add_node($t, 0);
    }
    $out .= qq{document.write($d);\n}
    . qq{$d.setHierarchyTypeVisibility();\n}
    . qq{</script>\n}
     ;
    return $out;
}

sub _add_node {
    my ($self,$node,$parent_id,$typeIcon) = @_;
#    if ($self->{leafevents}->{$node->db_id}) {
#	_handle_leaf_event(@_);
#    }
    if ($self->{'looptracker'}->{"$node"}++) {
	confess($node->extended_displayName . " refers to itself in event hierarchy.\n");
    }
    $typeIcon ||= "null";
    my $icon = $self->_get_node_icon($node);
    my $node_id = $self->next_node_id;
    my $db_id = $node->db_id;
    my $lbl = $self->_get_label($node);
    my $url = $self->urlmaker->urlify($node);
    my $d = $self->js_dtree_var_name;
    my $out = qq{$d.add($node_id,$parent_id,$db_id,'$lbl',"$url",null,null,$icon,$icon,null,$typeIcon);\n};
    if ($node->is_valid_attribute('hasMember')) {
	foreach my $i (grep {$_->get_cached_value('is_on_path')} @{$node->HasMember}) {
	    $out .= $self->_add_node($i,$node_id,qq('<SPAN CLASS="attributename" STYLE="background-color:red;">m</SPAN>'));
	}
    }
    if ($node->is_a('ConceptualEvent')) {
	foreach my $i (grep {$_->get_cached_value('is_on_path')} @{$node->HasSpecialisedForm}) {
	    $out .= $self->_add_node($i,$node_id,qq('<SPAN CLASS="attributename" STYLE="background-color:green;">s</SPAN>'));
	}
    }
    if ($node->is_valid_attribute('hasComponent')) {
	foreach my $i (grep {$_->get_cached_value('is_on_path')} @{$node->HasComponent}) {
	    $out .= $self->_add_node($i,$node_id,qq('<SPAN CLASS="attributename" STYLE="background-color:navy;">c</SPAN>'));
	}
    }
    if ($node->is_valid_attribute('hasEvent')) {
	foreach my $i (grep {$_->get_cached_value('is_on_path')} @{$node->HasEvent}) {
	    $out .= $self->_add_node($i,$node_id,qq('<SPAN CLASS="attributename" STYLE="background-color:navy;">c</SPAN>'));
	}
    }
    delete $self->{'looptracker'}->{"$node"};
    return $out;
}

=head
# Not used yet (?). Will become handy if we want to display also the initial entities
# in the hierarchy.
sub _handle_leaf_event {
    my ($self,$node,$parent_id,$typeIcon) = @_;
    if ($self->{'looptracker'}->{"$node"}++) {
	confess($node->extended_displayName . " refers to itself in event hierarchy.\n");
    }
    $typeIcon ||= "null";
    my $icon = _get_node_icon($node);
    my $node_id = $self->next_node_id;
    my $db_id = $node->db_id;
    my $lbl = $self->_get_label($node);
    my $url = $self->urlmaker->urlify($node);
    my $d = $self->js_dtree_var_name;
    my $out = qq{$d.add($node_id,$parent_id,$db_id,'$lbl',"$url",null,null,$icon,$icon,null,$typeIcon);\n};
    delete $self->{'looptracker'}->{"$node"};
    return $out;
}
=cut

sub get_leaf_event_db_ids {
    my $self = shift;
    return keys %{$self->{leafevents}};
}

1;
