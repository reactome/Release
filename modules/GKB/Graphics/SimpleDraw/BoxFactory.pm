package GKB::Graphics::SimpleDraw::BoxFactory;

use strict;
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

sub format_boxes {
  my $self     = shift;
  my @captions = @_;
  my @boxes    = map {$self->format_box($_)} @captions;
  return wantarray ? @boxes : $boxes[0];
}

sub format_boxes_with_coefficient {
  my $self     = shift;
  my @captions = @_;
  my %count;
  my @tmp = grep {! $count{$_}++} @captions;
  my @boxes    = map {$self->format_box_with_coefficient($_,$count{$_})} @tmp;
  return wantarray ? @boxes : $boxes[0];
}

sub format_box {
  my $self      = shift;
  my $caption   = shift;

  my $line_count = 1;
  while (1) {
    my $lines = $self->_wrap($caption,$line_count++);
    my $cur_width  = $self->_measure_width(@$lines);
    my $cur_height = $self->font->height * @$lines;
    croak "insolvable" if $cur_height <= 0 || $cur_width <= 0;
    return GKB::Graphics::SimpleDraw::Box->new($self->font,$cur_width,$lines)
      if $cur_width/$cur_height <= 4 || @$lines < $line_count-1;
  }
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
  my $chars_per_line = length($text)/$line_count;
  my @lines;
  my @words = split /\s+/,$text;
  my $line = '';
  while (@words) {
    my $w  = shift @words;
    $line .= "$w ";
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
