package GKB::Graphics::SimpleDraw::Pathway;
use strict;
use GD;
use CGI::Util 'rearrange';
use GKB::Graphics::SimpleDraw::BoxFactory;
use GKB::Graphics::SimpleDraw::ThreePartArrow;
use Carp 'croak';

use constant V_SPACING => 20;  # min vertical spacing between reactant boxes
use constant H_SPACING => 20;  # min horizontal spacing between reactant boxes
use constant REACTION_COLOR  => [255,255,0];
use constant SIBLING_COLOR   => [200,200,200];
use constant PARENT_COLOR    => [qw(222 184 135)];
use constant WEDGE_COLOR     => [255,218,185];

sub new {
  my $class = shift;
  $class = ref($class) if ref $class;

  my ($title,$preceding,$following,$parents) =
    rearrange(['TITLE','PRECEDING','FOLLOWING',['PARENT','PARENTS']],@_);
  $title ||= 'untitled reaction';
  return bless {
		title      => $title,
		preceding  => $preceding,
		following  => $following,
		parents    => $class->_force_array($parents),
		},$class;
}

sub title     { return shift->{title}       }
sub preceding { return shift->{preceding}   }
sub following { return shift->{following}   }
sub parents   { return shift->{parents}     }

sub draw {
  my $self   = shift;
  my $box_formatter = GKB::Graphics::SimpleDraw::BoxFactory->new(-font   => gdSmallFont());

  my $preceding_b   = $box_formatter->format_box($self->preceding) if $self->preceding;
  my $following_b   = $box_formatter->format_box($self->following) if $self->following;
  my @parent_b      = $box_formatter->format_boxes(@{$self->parents});
  my $title_b       = $box_formatter->format_box($self->title);

  # calculate height based on summing the parents
  my $height        = $self->_sum_height(@parent_b,$title_b);

  # calculate width based on summing the inputs, outputs and title
  my $width         = $self->_sum_width($preceding_b,$following_b,$title_b);

  # create image
  my $gd  = GD::Image->new($width+1,$height+1) or croak;
  my $three_part_arrow = GKB::Graphics::SimpleDraw::ThreePartArrow->new($gd);

  # align the siblings along the bottom
  my $max_height   = $self->_max($preceding_b && $preceding_b->height,
				 $title_b     && $title_b->height,
				 $following_b && $following_b->height);
  my $max_top      = $height - $max_height;
  my $midpoint_x   = $width/2;
  my $midpoint_y   = ($height+$max_top)/2;

  my $left = 0;
  my $center;
  for my $box ($preceding_b,$title_b,$following_b) {
    next unless $box;
    my $top = $midpoint_y-$box->height/2;
    $box->render($gd,$left,$top,$box eq $title_b ? {box=>1,fill=>REACTION_COLOR}
		                                 : {box=>0,fill=>SIBLING_COLOR});
    $three_part_arrow->draw($left + $box->width+3,
			    $top  + $box->height/2,
			    $left + $box->width+H_SPACING-3,
			    $top  + $box->height/2);
    $center = $left + $box->width/2 if $box eq $title_b;
    $left += $box->width + H_SPACING;
  }

  # draw the parents, working our way upwards
  my ($last_top,$last_width) = ($max_top,$width);
  my $bottom = $last_top - V_SPACING;
  for (my $i=$#parent_b;$i>=0;$i--) {
    my $box  = $parent_b[$i];
    my $top  = $bottom - $box->height;
    my $left = $center - $box->width/2;
    $box->render($gd,$left,$top,{box=>0,fill=>PARENT_COLOR});

    # draw the inverted V
    my $poly = GD::Polygon->new;
    $last_width *= 0.8;
    $poly->addPt($center,$bottom+3);
    $poly->addPt($center-$last_width/2,$last_top-3);
    $poly->addPt($center+$last_width/2,$last_top-3);
    $poly->addPt($center,$bottom+3);
    $gd->filledPolygon($poly,$gd->colorResolve(@{WEDGE_COLOR()}));
    ($last_top,$last_width) = ($top,$box->width);
    $bottom  = $top - V_SPACING;
  }

  return $gd;
}

sub _sum_height {
  my $self = shift;
  my @boxes = @_;
  my ($height,$count);
  foreach (@boxes) {
    next unless defined $_;
    $count++;
    $height += $_->height;
  }
  $height + $count * V_SPACING;
}


sub _sum_width {
  my $self = shift;
  my @boxes = @_;
  my ($width,$count);
  foreach (@boxes) {
    next unless defined $_;
    $count++;
    $width += $_->width;
  }
  $width + ($count-1) * H_SPACING;
}

sub _max {
  my $self = shift;
  my $max;
  for (@_) {
    next unless defined $_;
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

1;
