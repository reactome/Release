package GKB::Graphics::SimpleDraw::ThreePartArrow;

use strict;
use CGI::Util 'rearrange';
use GD::Polyline;
use Carp 'croak';

use constant EXPERIMENTAL_SPLINE_STUFF=>0;
use constant FUDGE_DISPLACEMENT=>0.15;

sub new {
  my $class        = shift;
  my ($gd,$color,$dashed) = rearrange([qw(GD COLOR DASHED)],@_);

  return bless {gd    => $gd,
		color => $color || [0,0,0],
		dashed => $dashed,
	       },ref $class || $class;
}

sub color {
  my $self  = shift;
  my $d     = $self->{color};
  $self->{color} = shift if @_;
  $d;
}

sub dashed { shift->{dashed} }
sub gd     { shift->{gd} }

# THOUGHT: consider using GD::Polyline here for splined curve
sub draw {
  my $self = shift;
  my ($x1,$y1,$x2,$y2) = @_;
  if (abs($x2-$x1) > abs($y2-$y1)) { # width greater than height
    $self->draw_horizontal(@_);
  } else {
    $self->draw_vertical(@_);
  }
}

sub draw_horizontal {
  my $self             = shift;
  my ($x1,$y1,$x2,$y2) = @_;
  my $gd               = $self->gd;
  my $operation        = $self->dashed ? 'dashedLine' : 'line';

  # the 10 and 40 below are addends of ReactomeReaction::H_SPACING;
  my $midpoint;
#  if (($x1 + $x2) < $gd->width) {
#      $midpoint = int($x1 + 10 + abs($y2-$y1)/$gd->height * 40);
#  } else {
#      $midpoint = int($x2 - 10 - abs($y2-$y1)/$gd->height * 40);
#  }
  if ($y1 < $y2) {
      # downward
      if (($y1 + $y2) < $gd->height) {
	  # above midline
	  $midpoint = int($x1 + 10 + abs($y2-$y1)/$gd->height * 40);
      } else {
	  # below midline
	  $midpoint = int($x2 - 10 - abs($y2-$y1)/$gd->height * 40);
      }
  } else {
      # upward
      if (($y1 + $y2) < $gd->height) {
	  # above midline
	  $midpoint = int($x2 - 10 - abs($y2-$y1)/$gd->height * 40);
      } else {
	  # below midline
	  $midpoint = int($x1 + 10 + abs($y2-$y1)/$gd->height * 40);
      }
  }

  my $color    = $gd->colorResolve(@{$self->{color}});

  if (EXPERIMENTAL_SPLINE_STUFF) {
    my $spline = GD::Polyline->new();
    $spline->addPt($x1,$y1);
    $spline->addPt($midpoint,$y1);
    $spline->addPt($midpoint,$y2);
    $spline->addPt($x2,$y2);
    $gd->polydraw($spline->toSpline,$color);
  } else {

    $gd->$operation($x1,$y1,$midpoint,$y1,$color);
    $gd->$operation($midpoint,$y1,$midpoint,$y2,$color);
    $gd->$operation($midpoint,$y2,$x2,$y2,$color);

  }

  #arrowhead
  if ($x1 < $x2) {
    $gd->line($x2,$y2,$x2-3,$y2-3,$color);
    $gd->line($x2,$y2,$x2-3,$y2+3,$color);
  } else {
    $gd->line($x2,$y2,$x2+3,$y2-3,$color);
    $gd->line($x2,$y2,$x2+3,$y2+3,$color);
  }
}

sub draw_vertical {
  my $self             = shift;
  my ($x1,$y1,$x2,$y2) = @_;
  my $gd               = $self->gd;
  my $midpoint = int ($y1+$y2)/2;
  my $color    = $gd->colorResolve(@{$self->{color}});
  my $operation        = $self->dashed ? 'dashedLine' : 'line';

  $gd->$operation($x1,$y1,$x1,$midpoint,$color);
  $gd->$operation($x1,$midpoint,$x2,$midpoint,$color);
  $gd->$operation($x2,$midpoint,$x2,$y2,$color);
  # now the arrowhead
  if ($y1 < $y2) {
    $gd->line($x2,$y2,$x2-3,$y2-3,$color);
    $gd->line($x2,$y2,$x2+3,$y2-3,$color);
  } elsif ($y2 < $y1) {
    $gd->line($x2,$y2,$x2-3,$y2+3,$color);
    $gd->line($x2,$y2,$x2+3,$y2+3,$color);
  }
}

1;
