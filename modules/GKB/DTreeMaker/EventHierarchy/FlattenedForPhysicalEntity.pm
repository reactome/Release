package GKB::DTreeMaker::EventHierarchy::FlattenedForPhysicalEntity;

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
    # Start from the members of sets if applicable
    my %instructions1 = 
	(-INSTRUCTIONS => {
	    'EntitySet' => {'attributes' => [qw(hasMember)]},
	    'CandidateSet' => {'attributes' => [qw(hasCandidate)]},
	 },
	 -OUT_CLASSES => [qw(PhysicalEntity)]
	);
    my (@tmp,%seen);
    foreach my $entity (@{$entities}) {
	my $ar = $entity->follow_class_attributes(%instructions1);
	if ($focus_species) {
	    @{$ar} = grep {$_->Species->[0] == $focus_species} @{$ar};
	}
	@{$ar} = grep {$_ != $entity} @{$ar};
	if (@{$ar}) {
	    push @tmp, grep {!$seen{$_->db_id}++} @{$ar};
	} else {
	    push @tmp, grep {!$seen{$_->db_id}++} ($entity);
	}
    }
    $entities = \@tmp;
    my (%leafevents);
    my %instructions2 = 
	(-INSTRUCTIONS => {
	    'PhysicalEntity' => {'reverse_attributes' => [qw(input output physicalEntity hasMember hasComponent hasRepeatedUnit hasCandidate)]},
	    'CatalystActivity' => {'reverse_attributes' => [qw(catalystActivity)]},
	    'ReferenceEntity' => {'reverse_attributes' => [qw(referenceEntity)]}
	 },
	 -OUT_CLASSES => [qw(ReactionlikeEvent)]
	);
    foreach my $entity (@{$entities}) {
	#print $entity->extended_displayName, "\n";
	my $ar = $entity->follow_class_attributes(%instructions2);
	if ($focus_species) {
	    @{$ar} = grep {$_->Species->[0] == $focus_species} @{$ar};
	}
	map {$leafevents{$_->db_id} = $_} @{$ar};
    }
    %leafevents || return;
    # not used yet
    $self->{leafevents} = \%leafevents;
    $self->_set_js_dtree_var_name($entities->[0]);
    my $d = $self->js_dtree_var_name;
    my (@roots,%h);
    foreach my $instance (values %leafevents) {
	printf STDERR "%s\n", $instance->extended_displayName;
	my $ar = $instance->follow_class_attributes
	    (-INSTRUCTIONS => {
		'Event' => {'reverse_attributes' => [qw(hasComponent hasMember hasSpecialisedForm hasEvent)]}
	     },
	     -OUT_CLASSES => [qw(Event)]
	    );
	@{$ar} =  grep {
	    ! $_->reverse_attribute_value('hasComponent')->[0] &&
	    ! $_->reverse_attribute_value('hasMember')->[0] &&
	    ! $_->reverse_attribute_value('hasSpecialisedForm')->[0] &&
	    ! $_->reverse_attribute_value('hasEvent')->[0]
	} @{$ar};
	foreach my $r (grep {$_ != $instance} @{$ar}) {
	    my $hr;
	    unless ($hr = $r->get_cached_value('leafreactions')) {
		$hr = {};
		$r->set_cached_value('leafreactions', $hr);
	    }
	    $hr->{$instance->db_id} = $instance;
	}
	push @roots, grep {!$h{$_->db_id}++} @{$ar};
    }
    @roots = sort {$a->displayName cmp $b->displayName} @roots;
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
    if (my $hr = $node->get_cached_value('leafreactions')) {
	foreach my $i (values %{$hr}) {
	    $out .= $self->_add_node($i,$node_id);
	}
    }
    delete $self->{'looptracker'}->{"$node"};
    return $out;
}

sub get_leaf_event_db_ids {
    my $self = shift;
    return keys %{$self->{leafevents}};
}

1;
