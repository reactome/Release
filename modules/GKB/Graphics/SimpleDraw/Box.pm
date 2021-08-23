package GKB::Graphics::SimpleDraw::Box;

use strict;
use Carp 'croak';
use GD;

# distance between box boundary and caption
use constant MARGIN => 5;

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
		width  => $width + MARGIN*2 + (length($coefficient)+1) * $font->width,
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

  $self->box($gd,$options,$dx,$dy,$dx+$self->width,$dy+$self->height)
      if $options->{box};

   $self->elipse($gd,$options,$dx,$dy,$dx+$self->width,$dy+$self->height)
      if $options->{elipse};
  
#  $gd->filledRectangle($dx,$dy,$dx+$self->width,$dy+$self->height,$fill)
#    if $fill;

#  $gd->rectangle($dx,$dy,$dx+$self->width,$dy+$self->height,$black)
#    if $options->{box};


  my $top = $dy + MARGIN;
  for my $line (@{$self->{lines}}) {
    my $center = ($self->width - $font->width * length($line))/2;
    $gd->string($font,$dx+$center+MARGIN,$top,$line,$fontcolor);
    $top += $font->height;
  }
  1;  # just to return a true value
}

sub cell {
    my ($self,$gd,$options,$x1,$y1,$x2,$y2) = @_;

    my $orange = $gd->colorResolve(194,128,0);

    # Outer box for cell membrane
    my $ox1 = $x1 + 2;
    my $ox2 = $x2 - 2;
    my $oy1 = $y1 + 2;
    my $oy2 = $y2 - 2;
    $self->box($gd,$options,$ox1,$oy1,$ox2,$oy2);

    # Inner box for nucleus
    my $ix1 = $x1;
    my $ix2 = $x2;
    my $iy1 = $y1 + (($y2 - $y1) / 2);
    my $iy2 = $y2;
    $self->box($gd,$options,$ix1,$iy1,$ix2,$iy2);

    my $outer_width = $ox2 - $ox1;
    my $outer_height = $oy2 - $oy1;

    # Eclipses for organelles

    ## Left organelle
    my $left_oval_width = $outer_width / 3;
    my $left_oval_height = $outer_height / 4;
    my $left_oval_x =  $ox1 + ($outer_width / 12);
    my $left_oval_y = $oy1 + ($outer_height / 8);
    my $left_oval_centre_x = $left_oval_x + ($left_oval_width / 2);
    my $left_oval_centre_y = $left_oval_y + ($left_oval_height / 2);
    $gd->ellipse($left_oval_centre_x, $left_oval_centre_y, $left_oval_width, $left_oval_height, $orange);

    ## Right organelle
    my $right_oval_width = $outer_width / 3;
    my $right_oval_height = $outer_height / 4;
    my $right_oval_x =  $ox1 + ($outer_width / 12) * 7;
    my $right_oval_y = $oy1 + ($outer_height / 8);
    my $right_oval_centre_x = $right_oval_x + ($right_oval_width / 2);
    my $right_oval_centre_y = $right_oval_y + ($right_oval_height / 2);
    $gd->ellipse($right_oval_centre_x, $right_oval_centre_y, $right_oval_width, $right_oval_height, $orange);
}

sub double_box {
    my ($self,$gd,$options,$x1,$y1,$x2,$y2) = @_;
    my $ox1 = $x1 + 2;
    my $ox2 = $x2 - 2;
    my $oy1 = $y1 + 2;
    my $oy2 = $y2 - 2;
    $self->box($gd,$options,$x1,$y1,$x2,$y2);
    $self->box($gd,$options,$ox1,$oy1,$ox2,$oy2);
}


sub box {
  my ($self,$gd,$options,$x1,$y1,$x2,$y2) = @_;

  my $black = $gd->colorResolve(0,0,0);
  my $fill  = $gd->colorResolve(@{$options->{fill}}) if $options->{fill};
  $gd->setAntiAliased($black);
  $black = gdAntiAliased;

  my $tl_corner_start = [$x1, $y1+5];
  my $tl_corner_stop  = [$x1+5, $y1];
  my $tl_arc          = [$x1+5,$y1+5,10,10,180,270,$black];
  my $tr_corner_start = [$x2-5, $y1];
  my $tr_corner_stop  = [$x2, $y1+5];
  my $tr_arc          = [$x2-5,$y1+5,10,10,270,360,$black];
  my $br_corner_start = [$x2, $y2-5];
  my $br_corner_stop  = [$x2-5, $y2];
  my $br_arc          = [$x2-5,$y2-5,10,10,0,90,$black];
  my $bl_corner_start = [$x1+5, $y2];
  my $bl_corner_stop  = [$x1, $y2-5];
  my $bl_arc          = [$x1+5,$y2-5,10,10,90,180,$black];

  $gd->line(@$tl_corner_stop,@$tr_corner_start,$black);
  $gd->line(@$tr_corner_stop,@$br_corner_start,$black);
  $gd->line(@$br_corner_stop,@$bl_corner_start,$black);
  $gd->line(@$bl_corner_stop,@$tl_corner_start,$black);  
  $gd->arc(@$tl_arc);
  $gd->arc(@$tr_arc);
  $gd->arc(@$br_arc);
  $gd->arc(@$bl_arc);

  $gd->fill($x1+5,$y1+5,$fill) if $fill;

}

sub elipse {
    my ($self,$gd,$options,$x1,$y1,$x2,$y2) = @_;
    my $black = $gd->colorResolve(0,0,0);
    my $fill  = $gd->colorResolve(@{$options->{fill}}) if $options->{fill};
    $gd->setAntiAliased($black);
    $black = gdAntiAliased;

    my $x = int(($x2 - $x1)/2 + 0.5) + $x1;
    my $y = int(($y2 - $y1)/2 + 0.5) + $y1;
    my $w = $x2 - $x1 + int(MARGIN/2+0.5);
    my $h = $y2 - $y1;
    
    $gd->arc($x,$y,$w,$h,0,360,$black);
    $gd->fill($x,$y,$fill) if $fill;
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

  $self->double_box($gd,$options,$dx1,$dy,$dx+$self->width,$dy+$self->height)
      if $options->{double_box};

  $self->box($gd,$options,$dx1,$dy,$dx+$self->width,$dy+$self->height)
      if $options->{box};

  $self->elipse($gd,$options,$dx1,$dy,$dx+$self->width,$dy+$self->height)
      if $options->{elipse};

  # draw a tiny box if this is the reaction
  if (@{$self->{lines}}==1 && $self->{lines}->[0] !~ /\S/) {
      my $x = $dx + int($self->width/2)  - 5;
      my $y = $dy + int($self->height/2) - 5;
      $gd->rectangle($x,$y,$x+10,$y+10,gdAntiAliased);
      return 1;
  }

  my $top = $dy + MARGIN;
  my $center = $dx1 + int($self->width/2);

  for my $line (@{$self->{lines}}) {
      $line .= ' ' if $options->{double_box} || $options->{box};
      my $width = $coefficient_w + ($font->width * length($line));
      my $x = $center - int($width/2) + MARGIN;
      $gd->string($font,$x,$top,$line,$fontcolor);
      $top += $font->height;
  }
  1;  # just to return a true value
}

1;
