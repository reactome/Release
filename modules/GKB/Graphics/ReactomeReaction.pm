package GKB::Graphics::ReactomeReaction;
use strict;
use GD;
use CGI::Util 'rearrange';
use GKB::Graphics::ReactomeReaction::NodeFactory;
use GKB::Graphics::SimpleDraw::ThreePartArrow;
use GKB::Graphics::ReactomeReaction::Edge;
use GKB::Graphics::ReactomeReaction::Node;
use GKB::Utils;
use Carp 'croak';

use constant V_SPACING => 3;  # min vertical spacing between reactant boxes
use constant H_SPACING => 50; # min horizontal spacing between reactant boxes
use constant RC_SPACING => 50; # min vertical spacing between reaction and catalyst
use constant INPUT_COLOR    => [255,130,71];
use constant REACTION_COLOR => [230,230,230];
use constant OUTPUT_COLOR   => [176,196,222];
use constant CATALYST_COLOR => [255,0,0];

use constant NE_SPACING => 3; # gap between node and edge

#use constant INPUT_RENDERING_PARAMS => {box=>1,fill=>INPUT_COLOR};
#use constant OUTPUT_RENDERING_PARAMS => {box=>1,fill=>OUTPUT_COLOR};
#use constant CATALYST_RENDERING_PARAMS => {box=>1,fill=>CATALYST_COLOR};
#use constant INPUTCATALYST_RENDERING_PARAMS => {box=>1,fill=>CATALYST_COLOR};
#use constant NEIGHBOURINGEVENT_RENDERING_PARAMS => {fill=>[240,240,240]};
use constant NEIGHBOURINGEVENT_RENDERING_PARAMS => {};


sub new {
  my $class = shift;
  $class = ref($class) if ref $class;
  my $instance = shift || croak("Need a Reaction instance.");
  ref($instance) && (ref($instance) =~ /GKB::.*Instance/) && ($instance->is_a('Reaction') || $instance->is_a('ReactionlikeEvent'))
      || croak("Need a Reaction instance, got '$instance'.");
  return bless {
      instance => $instance,
  },$class;
}

sub instance { return shift->{instance} }
sub edges    { return shift->{edges} || [] } 

sub reaction_node { if (@_ > 1) { $_[0]->{reaction_node} = $_[1];} return $_[0]->{reaction_node}}
sub helper_node { if (@_ > 1) { $_[0]->{helper_node} = $_[1];} return $_[0]->{helper_node}}
sub input_nodes { if (@_ > 1) { $_[0]->{input_nodes} = $_[1];} return $_[0]->{input_nodes}}
sub output_nodes { if (@_ > 1) { $_[0]->{output_nodes} = $_[1];} return $_[0]->{output_nodes}}
sub catalyst_nodes { if (@_ > 1) { $_[0]->{catalyst_nodes} = $_[1];} return $_[0]->{catalyst_nodes}}
sub preceding_nodes { if (@_ > 1) { $_[0]->{preceding_nodes} = $_[1];} return $_[0]->{preceding_nodes}}
sub following_nodes { if (@_ > 1) { $_[0]->{following_nodes} = $_[1];} return $_[0]->{following_nodes}}

sub node_factory {
    my $self = shift;
    if (@_) {
	$self->{node_factory} = shift;
    }
    return $self->{node_factory};
}

sub create_nodes_and_edges1 {
    my ($self) = @_;
    my $r = $self->instance;
    my $node_factory = GKB::Graphics::ReactomeReaction::NodeFactory->new(-font => gdSmallFont());
    $self->node_factory($node_factory);

    # Create nodes and edges
    # This reaction
    my $reaction_node = $node_factory->create_node($r);
    # catalyst and preceding events
    my $preceding_nodes = [];
    my $catalyst_nodes = $node_factory->create_nodes([map {$_->PhysicalEntity->[0]} @{$r->CatalystActivity}]);
    foreach my $node (@{$catalyst_nodes}) {
	$self->create_edge($node,$reaction_node,'catalyst');
	my $i = $node->instance;
	foreach my $e (@{$r->PrecedingEvent}) {
	    my $p_node = $node_factory->create_node($e);
	    push @{$preceding_nodes}, $p_node;
	    if (grep {$_ == $i} @{$e->Output}) {
		$self->create_edge($p_node,$node,'output');
	    }
	}
    }
    # inputs and (remaining) preceding events
    my $input_nodes = $node_factory->create_nodes($r->Input);
    foreach my $node (@{$input_nodes}) {
	$self->create_edge($node,$reaction_node,'input');
	my $i = $node->instance;
	foreach my $e (@{$r->PrecedingEvent}) {
	    if (grep {$_ == $i} @{$e->Output}) {
		my $p_node;
		unless ($p_node = $node_factory->get_instance_node($e)->[0]) {
		    $p_node = $node_factory->create_node($e);
		    push @{$preceding_nodes}, $p_node;
		}
		$self->create_edge($p_node,$node,'output');
	    }
	}
    }
    # output and following events
    my $following_nodes = [];
    my $output_nodes = $node_factory->create_nodes($r->Output);
    foreach my $node (@{$output_nodes}) {
	$self->create_edge($reaction_node,$node,'output');
	my $i = $node->instance;
	foreach my $e (@{$r->reverse_attribute_value('precedingEvent')}) {
	    if (grep {$_ == $i} @{$e->Output}) {
		my $f_node = $node_factory->create_node($e);
		push @{$following_nodes}, $f_node;
		$self->create_edge($node,$f_node,'input');
	    }
	}
    }
    $self->reaction_node($reaction_node);
    $self->input_nodes($input_nodes);
    $self->output_nodes($output_nodes);
    $self->catalyst_nodes($catalyst_nodes);
    $self->preceding_nodes($preceding_nodes);
    $self->following_nodes($following_nodes);
}

sub create_nodes_and_edges {
    my ($self) = @_;
    my $r = $self->instance;
    my $node_factory = GKB::Graphics::ReactomeReaction::NodeFactory->new(-font => gdSmallFont());
    $self->node_factory($node_factory);

    # Create nodes and edges
    # This reaction
    my $reaction_node = $node_factory->create_node($r);
    my ($inputs,$input_catalysts,$catalysts) = GKB::Utils::group_inputs_and_catalysts($r);
    # catalyst and preceding events
    my $preceding_nodes = [];
    my $catalyst_nodes = $node_factory->create_nodes($catalysts);
    foreach my $node (@{$catalyst_nodes}) {
	$self->create_edge($node,$reaction_node,'catalyst');
	my $i = $node->instance;
	foreach my $e (@{$r->PrecedingEvent}) {
	    if (grep {$_ == $i} @{$e->Output}) {
		my $p_node = $node_factory->create_node($e);
		push @{$preceding_nodes}, $p_node;
		$self->create_edge($p_node,$node,'output');
	    }
	}
    }
    # inputcatalyst and preceding events
    my $input_nodes = $node_factory->create_nodes($input_catalysts);
    foreach my $node (@{$input_nodes}) {
	$self->create_edge($node,$reaction_node,'inputcatalyst');
	my $i = $node->instance;
	foreach my $e (@{$r->PrecedingEvent}) {
	    if (grep {$_ == $i} @{$e->Output}) {
		my $p_node;
		unless ($p_node = $node_factory->get_instance_node($e)->[0]) {
		    $p_node = $node_factory->create_node($e);
		    push @{$preceding_nodes}, $p_node;
		}
		$self->create_edge($p_node,$node,'output');
	    }
	}
    }
    # inputs and (remaining) preceding events
    my $tmp = $node_factory->create_nodes($inputs);
    foreach my $node (@{$tmp}) {
	$self->create_edge($node,$reaction_node,'input');
	my $i = $node->instance;
	foreach my $e (@{$r->PrecedingEvent}) {
	    if (grep {$_ == $i} @{$e->Output}) {
		my $p_node;
		unless ($p_node = $node_factory->get_instance_node($e)->[0]) {
		    $p_node = $node_factory->create_node($e);
		    push @{$preceding_nodes}, $p_node;
		}
		$self->create_edge($p_node,$node,'output');
	    }
	}
    }
    push @{$input_nodes}, @{$tmp};
    # remaining preceding events
    foreach my $e (@{$r->PrecedingEvent}) {
	unless (my $p_node = $node_factory->get_instance_node($e)->[0]) {
	    $p_node = $node_factory->create_node($e);
	    push @{$preceding_nodes}, $p_node;
	}
    }

    # output and following events
    my $following_nodes = [];
    my $output_nodes = $node_factory->create_nodes($r->Output);
    foreach my $node (@{$output_nodes}) {
	$self->create_edge($reaction_node,$node,'output');
	my $i = $node->instance;
	foreach my $e (@{$r->reverse_attribute_value('precedingEvent')}) {
	    if (grep {$_ == $i} (@{$e->Input}, map {$_->PhysicalEntity->[0]} @{$e->CatalystActivity})) {
		my $f_node;
		unless (($f_node = $node_factory->get_instance_node($e)->[0]) && (! grep {$_ == $f_node} @{$preceding_nodes})) {
		    $f_node = $node_factory->create_node($e);
		    push @{$following_nodes}, $f_node;
		}
		$self->create_edge($node,$f_node,'input');
	    }
	}
    }
    # remaining following events
    foreach my $e (@{$r->reverse_attribute_value('precedingEvent')}) {
	my $f_node;
	unless (($f_node = $node_factory->get_instance_node($e)->[-1]) && (! grep {$_ == $f_node} @{$preceding_nodes})) {
	    $f_node = $node_factory->create_node($e);
	    push @{$following_nodes}, $f_node;
	}
    }
    #
    foreach my $n (@{$preceding_nodes}, @{$following_nodes}) {
	$n->rendering_params(NEIGHBOURINGEVENT_RENDERING_PARAMS);
    }
    $self->reaction_node($reaction_node);
    $self->input_nodes($input_nodes);
    $self->output_nodes($output_nodes);
    $self->catalyst_nodes($catalyst_nodes);
    $self->preceding_nodes($preceding_nodes);
    $self->following_nodes($following_nodes);
}

sub create_nodes_and_edges3 {
    my ($self) = @_;
    my $r = $self->instance;
    my $node_factory = GKB::Graphics::ReactomeReaction::NodeFactory->new(-font => gdSmallFont());
    $self->node_factory($node_factory);

    # Create nodes and edges
    # This reaction
    my $reaction_node = $node_factory->create_node($r);
    my $helper_node = $node_factory->create_helper_node();
    $self->helper_node($helper_node);
    my ($inputs,$input_catalysts,$catalysts) = GKB::Utils::group_inputs_and_catalysts($r);
    # catalyst and preceding events
    my $preceding_nodes = [];
    my $catalyst_nodes = $node_factory->create_nodes($catalysts);
    foreach my $node (@{$catalyst_nodes}) {
	$self->create_edge($node,$reaction_node,'catalyst');
	my $i = $node->instance;
	foreach my $e (@{$r->PrecedingEvent}) {
	    if (grep {$_ == $i} @{$e->Output}) {
		my $p_node = $node_factory->create_node($e);
		push @{$preceding_nodes}, $p_node;
		$self->create_edge($p_node,$node,'output');
	    }
	}
    }
    # inputcatalyst and preceding events
    my $input_nodes = $node_factory->create_nodes($input_catalysts);
    foreach my $node (@{$input_nodes}) {
	$self->create_edge($node,$reaction_node,'inputcatalyst');
	my $i = $node->instance;
	foreach my $e (@{$r->PrecedingEvent}) {
	    if (grep {$_ == $i} @{$e->Output}) {
		my $p_node;
		unless ($p_node = $node_factory->get_instance_node($e)->[0]) {
		    $p_node = $node_factory->create_node($e);
		    push @{$preceding_nodes}, $p_node;
		}
		$self->create_edge($p_node,$node,'output');
	    }
	}
    }
    # inputs and (remaining) preceding events
    my $tmp = $node_factory->create_nodes($inputs);
    foreach my $node (@{$tmp}) {
	$self->create_edge($node,$reaction_node,'input');
	my $i = $node->instance;
	foreach my $e (@{$r->PrecedingEvent}) {
	    if (grep {$_ == $i} @{$e->Output}) {
		my $p_node;
		unless ($p_node = $node_factory->get_instance_node($e)->[0]) {
		    $p_node = $node_factory->create_node($e);
		    push @{$preceding_nodes}, $p_node;
		}
		$self->create_edge($p_node,$node,'output');
	    }
	}
    }
    push @{$input_nodes}, @{$tmp};
    # remaining preceding events
    foreach my $e (@{$r->PrecedingEvent}) {
	unless (my $p_node = $node_factory->get_instance_node($e)->[0]) {
	    $p_node = $node_factory->create_node($e);
	    push @{$preceding_nodes}, $p_node;
	    $self->create_edge($p_node,$helper_node,'preceding');
	}
    }

    # output and following events
    my $following_nodes = [];
    my $output_nodes = $node_factory->create_nodes($r->Output);
    foreach my $node (@{$output_nodes}) {
	$self->create_edge($reaction_node,$node,'output');
	my $i = $node->instance;
	foreach my $e (@{$r->reverse_attribute_value('precedingEvent')}) {
	    if (grep {$_ == $i} (@{$e->Input}, map {$_->PhysicalEntity} @{$e->CatalystActivity})) {
		my $f_node = $node_factory->create_node($e);
		push @{$following_nodes}, $f_node;
		$self->create_edge($node,$f_node,'input');
	    }
	}
    }
    # remaning following events
    foreach my $e (@{$r->reverse_attribute_value('precedingEvent')}) {
	unless (my $f_node = $node_factory->get_instance_node($e)->[0]) {
	    $f_node = $node_factory->create_node($e);
	    push @{$following_nodes}, $f_node;
	    $self->create_edge($helper_node,$f_node,'following');
	}
    }
    #
    $self->reaction_node($reaction_node);
    $self->input_nodes($input_nodes);
    $self->output_nodes($output_nodes);
    $self->catalyst_nodes($catalyst_nodes);
    $self->preceding_nodes($preceding_nodes);
    $self->following_nodes($following_nodes);
}

sub do_layout {
    my $self = shift;
    $self->node_factory || $self->create_nodes_and_edges;
    my $max_height = $self->_max(
	($self->reaction_node->height + 2 * ($self->_sum_height(@{$self->catalyst_nodes})) + RC_SPACING),
	$self->_sum_height(@{$self->input_nodes}),
	$self->_sum_height(@{$self->output_nodes}),
	$self->_sum_height(@{$self->preceding_nodes}),
	$self->_sum_height(@{$self->following_nodes})
    );
    my $max_catalyst_w = $self->_max_width(@{$self->catalyst_nodes});
    my $middle_col_max_w = $self->_max($self->reaction_node->width, $max_catalyst_w);
    if (!defined $middle_col_max_w) {
    	$middle_col_max_w = 0;
    }
    my $input_nodes = $self->_max(map {$_->width} @{$self->input_nodes});
    if (!defined $input_nodes) {
    	$input_nodes = 0;
    }
    my $output_nodes = $self->_max(map {$_->width} @{$self->output_nodes});
    if (!defined $output_nodes) {
    	$output_nodes = 0;
    }
    my $preceding_nodes = $self->_max(map {$_->width} @{$self->preceding_nodes});
    if (!defined $preceding_nodes) {
    	$preceding_nodes = 0;
    }
    my $following_nodes = $self->_max(map {$_->width} @{$self->following_nodes});
    if (!defined $following_nodes) {
    	$following_nodes = 0;
    }
    
    my $width = 
	$middle_col_max_w 
#	+ $self->_max(map {$_->width} @{$self->input_nodes})
#	+ $self->_max(map {$_->width} @{$self->output_nodes})
#	+ $self->_max(map {$_->width} @{$self->preceding_nodes})
#	+ $self->_max(map {$_->width} @{$self->following_nodes})
	+ $input_nodes
	+ $output_nodes
	+ $preceding_nodes
	+ $following_nodes
	+ H_SPACING *4;
    my $gd  = GD::Image->new($width+2,$max_height+2) or croak;
    my $left = 1;
    if (map {@{$_->in_edges}} @{$self->catalyst_nodes}) {
	$left = $self->_lay_out_column2($gd,$left,1,$self->preceding_nodes) + H_SPACING;
    } else {
	$left = $self->_lay_out_column($gd,$left,$self->preceding_nodes) + H_SPACING;
    }
    $left = $self->_lay_out_column($gd,$left,$self->input_nodes,) + H_SPACING;
    $self->_lay_out_column2($gd,$left+($middle_col_max_w - $max_catalyst_w)/2,1,$self->catalyst_nodes);
    $left = $self->_lay_out_column($gd,$left+($middle_col_max_w - $self->reaction_node->width)/2,[$self->reaction_node]) + H_SPACING + ($middle_col_max_w - $self->reaction_node->width)/2;
    $left = $self->_lay_out_column($gd,$left,$self->output_nodes) + H_SPACING;
    $left = $self->_lay_out_column($gd,$left,$self->following_nodes);

    my $three_part_arrow = GKB::Graphics::SimpleDraw::ThreePartArrow->new(-GD => $gd);
    my $catalyst_arrow   = GKB::Graphics::SimpleDraw::ThreePartArrow->new(-GD=>$gd,
									  -color=>CATALYST_COLOR,
#									  -dashed=>1
									 );
    foreach my $edge (@{$self->edges}) {
#	my @c = $edge->get_coordinates;
#	print STDERR "@c\n";
#	$three_part_arrow->draw_horizontal(@c);
	if ($edge->type eq 'catalyst') {
	    $catalyst_arrow->draw($edge->get_coordinates);
	} elsif ($edge->type eq 'inputcatalyst') {
	    $catalyst_arrow->draw_horizontal($edge->get_coordinates);
	} else {
	    $three_part_arrow->draw_horizontal($edge->get_coordinates);
	}
    }

    return $gd;
}

sub do_layout2 {
    my $self = shift;
    $self->node_factory || $self->create_nodes_and_edges;
    my $bounding_box_height = $self->_max(
	($self->reaction_node->height + 2 * ($self->_sum_height(@{$self->catalyst_nodes})) + RC_SPACING),
	$self->_sum_height(@{$self->input_nodes}),
	$self->_sum_height(@{$self->output_nodes})
    );

    my $max_height = $self->_max(
#	($self->reaction_node->height + 2 * ($self->_sum_height(@{$self->catalyst_nodes})) + RC_SPACING),
#	$self->_sum_height(@{$self->input_nodes}),
#	$self->_sum_height(@{$self->output_nodes}),
	$bounding_box_height,
	$self->_sum_height(@{$self->preceding_nodes}),
	$self->_sum_height(@{$self->following_nodes})
    );
    my $max_catalyst_w = $self->_max_width(@{$self->catalyst_nodes});
    my $middle_col_max_w = $self->_max($self->reaction_node->width, $max_catalyst_w);
    my $bounding_box_width = 
	$middle_col_max_w 
	+ $self->_max(map {$_->width} @{$self->input_nodes})
	+ $self->_max(map {$_->width} @{$self->output_nodes})
	+ H_SPACING *3;
    my $width = 
#	$middle_col_max_w 
#	+ $self->_max(map {$_->width} @{$self->input_nodes})
#	+ $self->_max(map {$_->width} @{$self->output_nodes})
	$bounding_box_width
	+ $self->_max(map {$_->width} @{$self->preceding_nodes})
	+ $self->_max(map {$_->width} @{$self->following_nodes})
	+ H_SPACING;
    my $bounding_box_left = $self->_max(map {$_->width} @{$self->preceding_nodes}) + H_SPACING/2;
    my $bounding_box_top = ($max_height+2 - $bounding_box_height)/2;
    my $gd  = GD::Image->new($width+2,$max_height+2) or croak;

    my $helper_node = $self->helper_node;
    $helper_node->width($bounding_box_width+2);
    $helper_node->height($bounding_box_height);
    $helper_node->render($gd, $bounding_box_left, $bounding_box_top);


    my $left = 1;
    if (map {@{$_->in_edges}} @{$self->catalyst_nodes}) {
	$left = $self->_lay_out_column2($gd,$left,1,$self->preceding_nodes) + H_SPACING;
    } else {
	$left = $self->_lay_out_column($gd,$left,$self->preceding_nodes) + H_SPACING;
    }
    $left = $self->_lay_out_column($gd,$left,$self->input_nodes,) + H_SPACING;
    $self->_lay_out_column2($gd,$left+($middle_col_max_w - $max_catalyst_w)/2,1,$self->catalyst_nodes);
    $left = $self->_lay_out_column($gd,$left+($middle_col_max_w - $self->reaction_node->width)/2,[$self->reaction_node]) + H_SPACING + ($middle_col_max_w - $self->reaction_node->width)/2;
    $left = $self->_lay_out_column($gd,$left,$self->output_nodes) + H_SPACING;
    $left = $self->_lay_out_column($gd,$left,$self->following_nodes);

    my $three_part_arrow = GKB::Graphics::SimpleDraw::ThreePartArrow->new(-GD => $gd);
    my $catalyst_arrow   = GKB::Graphics::SimpleDraw::ThreePartArrow->new(-GD=>$gd,
									  -color=>CATALYST_COLOR,
#									  -dashed=>1
									 );
    my $pf_arrow   = GKB::Graphics::SimpleDraw::ThreePartArrow->new(-GD=>$gd,
									  -color=>REACTION_COLOR,
									 );
    foreach my $edge (map {@{$_->in_edges},@{$_->out_edges}} (@{$self->input_nodes}, @{$self->output_nodes})) {
	if ($edge->type eq 'inputcatalyst') {
	    $catalyst_arrow->draw_horizontal($edge->get_coordinates);
	} else {
	    $three_part_arrow->draw_horizontal($edge->get_coordinates);
	}
    }
    foreach my $edge (map {@{$_->in_edges}} @{$self->catalyst_nodes}) {
	$three_part_arrow->draw_horizontal($edge->get_coordinates);
    }
    if ($self->catalyst_nodes->[0]) {
	$catalyst_arrow->draw($self->catalyst_nodes->[-1]->out_edges->[0]->get_coordinates);
    }
    foreach my $edge (grep {$_->type eq 'following'} @{$self->edges}) {
	$pf_arrow->draw_horizontal($edge->get_coordinates);
    }

    return $gd;
}

sub draw {
    my $self = shift;
    $self->create_nodes_and_edges;
    return $self->do_layout;
}

sub create_usemap {
    my ($self,$urlmaker) = @_;
    my $out = '';
    foreach my $node (@{$self->input_nodes},
		      @{$self->output_nodes},
		      @{$self->catalyst_nodes},
		      @{$self->preceding_nodes},
		      @{$self->following_nodes}) {
	$out .= qq(<AREA SHAPE="rect" COORDS=") . join(',', $node->coords) 
	    . qq(" HREF=") . $urlmaker->urlify($node->instance) . qq(">);
    }
    return $out;
}

sub _lay_out_column {
    my ($self,$gd,$left,$nodes,$params) = @_;
    my $top   = ($gd->height - $self->_sum_height(@{$nodes}))/2;
    return $self->_lay_out_column2($gd,$left,$top,$nodes,$params);
}

sub _lay_out_column2 {
    my ($self,$gd,$left,$top,$nodes,$params) = @_;
    my $max_width = $self->_max(map {$_->width} @{$nodes});
	if (!(defined $max_width)) {
		$max_width = 0;
	}
    my $mid_x = $left + $max_width / 2;
    foreach my $n (@{$nodes}) {
	$n->col_left($left);
	$n->col_right($left + $max_width);
	$n->render($gd, $mid_x - $n->width/2, $top,$params);
	$top += $n->height + V_SPACING;
    }
    return $left +  $max_width;
}

sub create_edge {
    my ($self, $from, $to,$type) = @_;
    my $edge = GKB::Graphics::ReactomeReaction::Edge->new(-FROM => $from, -TO => $to, -TYPE => $type);
    $self->add_edge($edge);
    return $edge;
}

sub add_edge {
    my $self = shift;
    push @{$self->{edges}}, @_;
}

sub qqq {
    my $self = shift;
    my $r = $self->instance;
    my @catal
}

sub _sum_height {
  my $self = shift;
  my @nodes = @_;
  my $height = 0;
  foreach (@nodes) {
    $height += $_->height;
  }
  $height + @nodes * V_SPACING - 1;
}


sub _sum_width {
  my $self = shift;
  my @nodes = @_;
  my $width;
  foreach (@nodes) {
    $width += $_->width;
  }
  $width;
}

sub _max_width {
    my $self = shift;
    my @nodes = @_;
    my $m_width = 0;
    foreach my $w (map {$_->width} @nodes) {
	$m_width = ($m_width > $w) ? $m_width: $w;
    }
    $m_width;
}

sub _max {
  my $self = shift;
  my $max;
  for (@_) {
    $max = $_ if !defined $max or $max < $_;
  }
  $max;
}

sub _force_array {
  my $self = shift;
  my $thingy = shift;
  return []        unless defined $thingy;
  return [$thingy] unless ref $thingy && ref $thingy eq 'ARRAY';
  return $thingy;
}

sub clean {
    my $self = shift;
    foreach my $e (@{$self->edges}) {
	$e->clean;
    }
    foreach my $k (keys %{$self}) {
	delete $self->{$k};
    }
}

1;
