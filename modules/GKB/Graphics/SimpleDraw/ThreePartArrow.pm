package GKB::Graphics::SimpleDraw::ThreePartArrow;

use strict;
use CGI::Util 'rearrange';
use GD::Polyline;
use GD;
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
  my ($x1,$y1,$x2,$y2,$options) = @_;
  my $gd               = $self->gd;
  my $operation        = $self->dashed ? 'dashedLine' : 'line';

  my $color = $self->{color} ? $gd->colorResolve(@{$self->{color}}) : gdAntiAliased;

  my $w = abs($x2 - $x1);

  my $join;

  if ($options->{right}) {
      my $join = $x2 - $w/2;
      $gd->$operation($x1-1,$y1,$join,$y1,$color);
      $gd->$operation($join,$y1,$x2-7,$y2,$color);
      $self->draw_arrowhead($x2,$y2,$color);
  }
  elsif ($options->{left}) {
      $join = $x1 + $w/2;
      $gd->$operation($x1+2,$y1,$join,$y2,$color);
      $gd->$operation($join,$y2,$x2+1,$y2,$color);
  }
  else { # catalyst arrow
      $join = $x2 - $w/2;
      $gd->$operation($x1,$y1,$join,$y1,$color);
      $gd->$operation($join,$y1,$x2-7,$y2,$color);
      $self->draw_arrowhead($x2,$y2,$color);
  }
}

sub draw_arrowhead {
    my $self = shift;
    my ($x,$y,$color,$up) = @_;
    my $head = GD::Polygon->new();
    
    $head->addPt($x,$y);
    $head->addPt($x-4,$y-4);
    $head->addPt($x-4,$y+4);
    $self->gd->filledPolygon($head,$color);
    $self->gd->line($x-7,$y,$x,$y,$color);
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


# use this once I figutre out how top discriminate between positive and nergative regulation
sub EXPERIMENTAL_draw_vertical {
  my $self             = shift;
  my ($x1,$y1,$x2,$y2) = @_;
  my $gd               = $self->gd;
  my $midpoint = int ($y1+$y2)/2;
  my $color    = $gd->colorResolve(@{$self->{color}});
  my $operation        = $self->dashed ? 'dashedLine' : 'line';

  my $inhibits;

  $gd->$operation($x1,$y1,$x1,$midpoint,$color);
  $gd->$operation($x1,$midpoint,$x2,$midpoint,$color);
  $gd->$operation($x2,$midpoint,$x2,$y2,$color);

  if ($inhibits && $y1 < $y2) {
      my $x = $x1 - 4;
      $gd->line($x1,$y1,$x1,$y2,$color);
      $gd->line($x,$y2,$x+8,$y2,$color);
  }
  elsif ($y1 < $y2) {
      my $white = $gd->colorResolve(255,255,255);
      $gd->line($x1,$y2-2,$x1,$y2,$white);
      $gd->arc($x1,$y2+2,9,9,0,360,$color);
      $gd->fill($x1,$y2+2,$white);
  }
  else {
      my $head = GD::Polygon->new();
      $head->addPt($x2,$y2);
      $head->addPt($x2-4,$y2+4);
      $head->addPt($x2+4,$y2+4);
      $gd->polygon($head,$color);
  }
}

1;
