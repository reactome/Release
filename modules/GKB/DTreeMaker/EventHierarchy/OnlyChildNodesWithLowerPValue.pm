package GKB::DTreeMaker::EventHierarchy::OnlyChildNodesWithLowerPValue;

=head
This module is intended for use from SkyPainter. It differs from the parent class in that it only includes
those nodes in the tree which have a lower cached p-value than their given "parent".
=cut

use strict;
use vars qw(@ISA);
use Carp;
use GKB::DTreeMaker::EventHierarchy;
@ISA = qw(GKB::DTreeMaker::EventHierarchy);

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
    my $extraStr = $self->_p_value_and_counts_string($node);
    my $out = qq{$d.add($node_id,$parent_id,$db_id,'$lbl',"$url",null,null,$icon,$icon,null,$typeIcon,'$extraStr');\n};
    if ($node->get_cached_value('matching_submitted_identifiers')) {
	$out .= $self->_add_participant_nodes_coloured_by_value($node,$node_id);
    }
    my $p_val = $node->get_cached_value('p_value');
    if ($node->is_valid_attribute('hasMember')) {
	foreach my $i (grep {$_->get_cached_value('p_value') < $p_val} grep {$_->get_cached_value('is_on_path')} @{$self->_sort_by_cached_p_value($node->HasMember)}) {
	    $out .= $self->_add_node_on_path($i,$node_id,qq('<SPAN CLASS="attributename" STYLE="background-color:red;">m</SPAN>'));
	}
    }
    if ($node->is_a('ConceptualEvent')) {
	foreach my $i (grep {$_->get_cached_value('p_value') < $p_val} grep {$_->get_cached_value('is_on_path')} @{$self->_sort_by_cached_p_value($node->HasSpecialisedForm)}) {
	    $out .= $self->_add_node_on_path($i,$node_id,qq('<SPAN CLASS="attributename" STYLE="background-color:green;">s</SPAN>'));
	}
    }
    if ($node->is_valid_attribute('hasComponent')) {
	foreach my $i (grep {$_->get_cached_value('p_value') < $p_val} grep {$_->get_cached_value('is_on_path')} @{$self->_sort_by_cached_p_value($node->HasComponent)}) {
	    $out .= $self->_add_node_on_path($i,$node_id,qq('<SPAN CLASS="attributename" STYLE="background-color:navy;">c</SPAN>'));
	}
    }
    if ($node->is_valid_attribute('hasEvent')) {
	foreach my $i (grep {$_->get_cached_value('p_value') < $p_val} grep {$_->get_cached_value('is_on_path')} @{$self->_sort_by_cached_p_value($node->HasEvent)}) {
	    $out .= $self->_add_node_on_path($i,$node_id,qq('<SPAN CLASS="attributename" STYLE="background-color:navy;">c</SPAN>'));
	}
    }
    delete $self->{'looptracker'}->{"$node"};
    return $out;
}

1;
