package GKB::Graphics::ReactomeReaction::NodeFactory;

use strict;
use GKB::Graphics::ReactomeReaction::Node;
use GKB::Graphics::SimpleDraw::Box;
use CGI::Util 'rearrange';
use Carp 'croak';
use GD ();

sub new {
  my $class = shift;
  $class = ref($class) if ref $class;
  my ($font) = rearrange([qw(FONT)],@_);
  return bless {
		font         => $font || GD->gdFontSmall,
	       },$class;
}

sub width_min  { shift->{width_range}[0]  };
sub width_max  { shift->{width_range}[1]  };
sub height_min { shift->{height_range}[0] };
sub height_max { shift->{height_range}[1] };
sub font       { shift->{font}            };

sub create_nodes {
  my ($self,$ar,$rendering_params) = @_;
  my %count;
  my @tmp = grep {! $count{$_}++} @{$ar};
  my @nodes = map {$self->create_node($_,$count{$_},$rendering_params)} @tmp;
  return \@nodes;
}

sub create_node {
    my ($self,$instance,$count,$rendering_params) = @_;
    my $node;
    if ($count && ($count > 1)) {
	$node = GKB::Graphics::ReactomeReaction::Node->new
	    (
	     -INSTANCE => $instance,
	     -COEFFICIENT => "$count x ",
#	     -RENDERING_PARAMS => $self->$rendering_params,
	    );
    } else {
	$node = GKB::Graphics::ReactomeReaction::Node->new
	    (
	     -INSTANCE => $instance,
#	     -RENDERING_PARAMS => $rendering_params,
	    );
    }
    $self->add_node($instance,$node);
    $self->format_node($node);
    return $node;
}

sub create_helper_node {
    my $self = shift;
    my $node = GKB::Graphics::ReactomeReaction::Node->new();
    $node->box(GKB::Graphics::SimpleDraw::Box->new_empty());
    return $node;
}

sub add_node {
    my ($self,$instance,$node) = @_;
    push @{$self->{instance2node}->{$instance->db_id || "$instance"}}, $node;
}

# returns arrayref since there can be multiple nodes for an instance
sub get_instance_node {
    my ($self,$instance) = @_;
    return $self->{instance2node}->{$instance->db_id || "$instance"} || [];
}

sub format_node {
    my ($self,$node) = @_;
    my $label = $node->instance->displayName;
    if (!$label || $label eq "") {
	$label = "UNKNOWN";
    }
    my ($cur_width,$lines);
    my $line_count = 1;
    while (1) {
	$lines = $self->_wrap($label,$line_count++);
	$cur_width = $self->_measure_width(@$lines);
	my $cur_height = $self->font->height * @{$lines};
	croak "insolvable" if $cur_height <= 0 || $cur_width <= 0;
	last if $cur_width/$cur_height <= 4 || @$lines < $line_count-1;
    }
    $node->box(GKB::Graphics::SimpleDraw::Box->new_with_coefficient($self->font,$cur_width,$lines,$node->coefficient));
    return $node;
}

sub format_box_with_coefficient {
  my $self      = shift;
  my $caption   = shift;
  my $coefficient = shift;
  if ($coefficient == 1) {
      $coefficient = '';
  } else {
      $coefficient = "$coefficient x ";
  }
  my $line_count = 1;
  while (1) {
    my $lines = $self->_wrap($caption,$line_count++);
    my $cur_width  = $self->_measure_width(@$lines);
    my $cur_height = $self->font->height * @$lines;
    croak "insolvable" if $cur_height <= 0 || $cur_width <= 0;
    return GKB::Graphics::SimpleDraw::Box->new_with_coefficient($self->font,$cur_width,$lines,$coefficient)
      if $cur_width/$cur_height <= 4 || @$lines < $line_count-1;
  }
}

sub _wrap {
  my $self               = shift;
  my ($text,$line_count) = @_;
  my @lines = ();
  if (!$text || length($text) == 0) {
      return \@lines;
  }
  my $chars_per_line = length($text)/$line_count;
  my @words = split /(\s+|-)/,$text;
  my $line = '';
  while (@words) {
    my $w  = shift @words;
#    $line .= "$w ";
    $line .= $w;
    if (length($line) >= $chars_per_line) {
      push @lines,$line;
      $line = '';
    }
  }
  push @lines,$line if $line;
  return \@lines;
}


sub _measure_width {
  my $self       = shift;
  my @lines      = @_;
  my $font_width = $self->font->width;

  my $max_width  = 0;
  for (@lines) {
    my $width  = length($_) * $font_width;
    $max_width = $width if $width > $max_width;
  }

  $max_width;
}


1;
