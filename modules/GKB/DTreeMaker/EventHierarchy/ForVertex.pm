package GKB::DTreeMaker::EventHierarchy::ForVertex;

use strict;
use vars qw(@ISA);
use Carp;
use GKB::DTreeMaker::EventHierarchy;
@ISA = qw(GKB::DTreeMaker::EventHierarchy);

sub create_tree {
    my ($self,$verteces) = @_;
    @{$verteces} || confess("Need nodes.");
    my (%leafevents);
    foreach my $vertex (@{$verteces}) {
	#print $vertex->extended_displayName, "\n";
	my $ar = $vertex->follow_class_attributes
	    (-INSTRUCTIONS => {
		'EntityVertex' => {'reverse_attributes' => [qw(targetVertex sourceVertex)]},
		'Edge' => {'attributes' => [qw(targetVertex sourceVertex)]},
		'ReactionVertex' => {'attributes' => [qw(representedInstance)]}
	     },
	     -OUT_CLASSES => [qw(ReactionlikeEvent)]
	    );
	map {$leafevents{$_->db_id} = $_} @{$ar};
    }
    %leafevents || confess("No leaf events?!\n");
    # not used yet
    $self->{leafevents} = \%leafevents;
    $self->_set_js_dtree_var_name($verteces->[0]);
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
	#map {$_->set_cached_value('is_on_path',1)} @{$ar};
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

sub get_leaf_event_db_ids {
    my $self = shift;
    return keys %{$self->{leafevents}};
}

1;
