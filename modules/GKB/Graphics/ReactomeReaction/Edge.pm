package GKB::Graphics::ReactomeReaction::Edge;

use strict;
use GD;
use CGI::Util 'rearrange';
use Carp;

use constant NE_SPACING => 3; # gap between node and edge

sub new {
    my $class = shift;
    $class = ref($class) if ref $class;
    my ($from,$to,$type) = rearrange([qw(FROM TO TYPE)],@_);
    ref($from) eq 'GKB::Graphics::ReactomeReaction::Node' or confess("Need Node, got '$from'.");
    ref($to) eq 'GKB::Graphics::ReactomeReaction::Node' or confess("Need Node, got '$to'.");
    my $self = bless {
	from => $from,
	to => $to,
	type => $type,
    },$class;
    $from->add_out_edge($self);
    unless ($type eq 'catalyst') {
	$to->add_in_edge($self);
    }
    return $self;
}

sub from { return shift->{from} }
sub to { return shift->{to} }
sub type { return shift->{type} }

sub get_coordinates {
    my $self = shift;
    my $from = $self->from;
    my $to = $self->to;
    if ($self->type eq 'catalyst') {
	return (
	    $from->left + $from->width/2,
	    $from->top + $from->height + NE_SPACING,
	    $to->left + $to->width/2,
	    $to->top - NE_SPACING
	);
    } elsif ($self->type eq 'following') {
	return (
	    $from->left + $from->width,
	    $from->top + $from->height/2,
	    $to->left,
	    $to->top - NE_SPACING
	);
    }
    return ($from->col_right + NE_SPACING,
	    $from->out_edge_y($self),
	    $to->col_left - NE_SPACING,
	    $to->in_edge_y($self));
    return ($from->left + $from->width + NE_SPACING,
	    $from->out_edge_y($self),
	    $to->left - NE_SPACING,
	    $to->in_edge_y($self));

}

sub clean {
    my $self = shift;
    foreach my $k (keys %{$self}) {
	delete $self->{$k};
    }
}

1;
