package GKB::Graphics::SimpleDraw::TwoPartArrow;

use strict;
use base 'GKB::Graphics::SimpleDraw::ThreePartArrow';

sub draw_horizontal {
  my $self             = shift;
  my ($x1,$y1,$x2,$y2) = @_;
  my $gd               = $self->gd;
  my $operation        = $self->dashed ? 'dashedLine' : 'line';

  my $color    = $gd->colorResolve(@{$self->{color}});
  $gd->$operation($x1,$y1,$x1,$y2,$color);
  $gd->$operation($x1,$y2,$x2,$y2,$color);

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
  my $color    = $gd->colorResolve(@{$self->{color}});
  my $operation        = $self->dashed ? 'dashedLine' : 'line';

  $gd->$operation($x1,$y1,$x2,$y1,$color);
  $gd->$operation($x2,$y1,$x2,$y2,$color);

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
