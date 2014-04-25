package GKB::Graphics::SimpleDraw::Box;

use strict;
use Carp 'croak';

# distance between box boundary and caption
use constant MARGIN => 3;

sub new {
  my $class                = shift;
  my ($font,$width,$lines) = @_;
  $class                   = ref $class if ref $class;
  # Remove HTML from the @$lines array of label strings.
  foreach (@$lines) {
    s/<[^>]+?>//g;
  }
  return bless {
		width  => $width + MARGIN*2,
		height => @$lines * $font->height + MARGIN*2,
		lines  => $lines,
		font   => $font,
		},$class;
}

sub new_with_coefficient {
  my $class                = shift;
  my ($font,$width,$lines,$coefficient) = @_;
  unless ($coefficient) {
      return new($class,$font,$width,$lines,$coefficient);
  }
  $class                   = ref $class if ref $class;
  return bless {
		width  => $width + MARGIN*2 + length($coefficient) * $font->width,
		height => @$lines * $font->height + MARGIN*2,
		lines  => $lines,
		font   => $font,
		coefficient => $coefficient,
		},$class;
}

sub new_empty {
    my $class = shift;
    my ($width,$height) = @_;
    return bless {
    }, $class;
}

#sub width  { return shift->{width}  }
#sub height { return shift->{height} }
sub width { if (@_ > 1) { $_[0]->{width} = $_[1];} return $_[0]->{width}}
sub height { if (@_ > 1) { $_[0]->{height} = $_[1];} return $_[0]->{height}}
sub lines  { return shift->{lines}  }
sub font   { return shift->{font}   }
sub coefficient   { return shift->{coefficient}   }

sub render {
  my $self              = shift;
  my $gd                = shift;
  my ($dx,$dy,$options) = @_;

  my $white = $gd->colorResolve(255,255,255);
  my $black = $gd->colorResolve(0,0,0);
  my $fill  = $gd->colorResolve(@{$options->{fill}}) if $options->{fill};
  my $fontcolor  = $options->{fontcolor} ? $gd->colorResolve(@{$options->{fontcolor}}) : $black;
  my $font  = $self->font;

  $gd->filledRectangle($dx,$dy,$dx+$self->width,$dy+$self->height,$fill)
    if $fill;

  $gd->rectangle($dx,$dy,$dx+$self->width,$dy+$self->height,$black)
    if $options->{box};

  my $top = $dy + MARGIN;
  for my $line (@{$self->{lines}}) {
    my $center = ($self->width - $font->width * length($line))/2;
    $gd->string($font,$dx+$center+MARGIN,$top,$line,$fontcolor);
    $top += $font->height;
  }
  1;  # just to return a true value
}

sub render_with_coefficient {
  my $self              = shift;
  my $gd                = shift;
  my ($dx,$dy,$options) = @_;
  
  my $white = $gd->colorResolve(255,255,255);
  my $black = $gd->colorResolve(0,0,0);
  my $fill  = $gd->colorResolve(@{$options->{fill}}) if $options->{fill};
  my $fontcolor  = $options->{fontcolor} ? $gd->colorResolve(@{$options->{fontcolor}}) : $black;
  my $font  = $self->font;
  
  my $coefficient = $self->coefficient;
  my $coefficient_w = ($coefficient) ? length($coefficient) * $font->width : 0;
  if (!defined $coefficient) {
  	$coefficient = "";
  }

  $gd->string($font,
	      $dx,
	      $dy + ($self->height - $font->height) / 2,
	      $coefficient,
	      $black);

  my $dx1 = $dx + $coefficient_w;
  $gd->filledRectangle($dx1,$dy,$dx+$self->width,$dy+$self->height,$fill)
    if $fill;

  $gd->rectangle($dx1,$dy,$dx+$self->width,$dy+$self->height,$black)
    if $options->{box};

  my $top = $dy + MARGIN;
  for my $line (@{$self->{lines}}) {
    my $center = ($self->width - $coefficient_w - $font->width * length($line))/2;
    $gd->string($font,$dx1+$center+MARGIN,$top,$line,$fontcolor);
    $top += $font->height;
  }
  1;  # just to return a true value
}

1;
