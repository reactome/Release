package GKB::Graphics::SimpleDraw::PackedBoxes;
use strict;
use GD;
use CGI::Util 'rearrange';
use GKB::Graphics::SimpleDraw::Reaction;
use Carp 'croak';

use constant SPACING   => 3;
use constant ALIGNMENT => 'center';

use constant ALIGMENTS => {left   => 'vertical',
			   right  => 'vertical',
			   center => 'vertical',
			   top    => 'horizontal',
			   bottom => 'horizontal',
			   middle => 'horizontal'
			   };

# the orientation of the packed boxes is determined by the alignment setting
sub new {
    my $class = shift;
    my ($spacing,$alignment) = rearrange(['SPACING','ALIGN'],@_);
    $alignment ||= ALIGNMENT;
    $spacing   ||= SPACING;
    croak "-alignment must be one of {left,right,center,top,bottom,middle"
	unless exists ALIGNMENTS->{$alignment};

    return bless {
	spacing   => $spacing,
	alignment => $alignment,
	parts     => [],
	dx        => 0,
	dy        => 0,
    } ref($class) || $class;
}

sub spacing {
    my $self = shift;
    my $d    = $self->{spacing};
    $self->{spacing} = shift if @_;
    $d;
}

sub parts {
    my $self = shift;
    return @{$self->{parts}};
}

sub orientation {
    my $self = shift;
    return ALIGNMENTS->{$self->{alignment}};
}

sub _add_part {
    my $self = shift;
    my $box  = shift;
    my $part = {box => $box,
		dx  => $self->{dx},
		dy  => $self->{dy}};
    push @{$self->{parts}},$part;
}

sub add {
    my $self = shift;
    my $box  = shift;
    croak "usage: \$packed_boxes->add(GKB::Graphics::SimpleDraw::Box"
	unless defined $box && $box->isa('GKB::Graphics::SimpleDraw::Box');
    $self->_add_part($box);
    if ($self->orientation eq 'vertical') {
	$self->{dy} += $self->spacing + $box->height;
    } elsif ($self->orientation eq 'horizontal') {
	$self->{dx} += $self->spacing + $box->width;
    } else {
	die "programming error, orientation must be vertical or horizontal";
    }
}

sub _adjust_alignment {
    my $self = shift;
    $self->{'.adjusted'}++ && return;

    my @parts       = $self->parts;
    my $bottommost  = 0;
    my $rightmost     = 0;
    for my $part (@parts) {
	my $height  = $part->{box}->height;
	my $width   = $part->{box}->width;
	$bottommost = $height if $bottommost < $height;
	$rightmost  = $width  if $rightmost  < $width;
    }
    for my $part (@parts) {
	$self->alignment eq 'right'  && $part->{dx} = $rightmost    - $part->{box}->width;
	$self->alignment eq 'center' && $part->{dx} = ($rightmost   - $part->{box}->width)/2;
	$self->alignment eq 'bottom' && $part->{dy} = $bottommost   - $part->{box}->height;
	$self->alignment eq 'middle' && $part->{dy} = ($bottommost  - $part->{box}->height)/2;
    }
}

sub render {
    my $self = shift;
    my ($dx,$dy,$options) = @_;
    $self->_adjust_alignment;
    for my $part ($self->parts) {
	$part->{box}->render($dx+$part->{dy},$dy+$part->{dy},$options);
    }
}

sub handles {
    my $self = shift;
    my ($dx,$dy) = @_;

    $self->_adjust_alignment;
    my $orientation = $self->orientation;

    if ($orientation eq 'vertical') {
	return map {$dx + $_->{dx} + $_->{box}->width} $self->parts;
    } elsif ($orientation eq 'horizontal') {
	return map {$dy + $_->{dy} + $_->{box}->height} $self->parts;
    }
}

1;
