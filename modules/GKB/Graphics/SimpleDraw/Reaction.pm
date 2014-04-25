package GKB::Graphics::SimpleDraw::Reaction;
use strict;
use GD;
use CGI::Util 'rearrange';
use GKB::Graphics::SimpleDraw::BoxFactory;
use GKB::Graphics::SimpleDraw::ThreePartArrow;
use GKB::Graphics::SimpleDraw::TwoPartArrow;
use Carp 'croak';

use constant V_SPACING => 3;  # min vertical spacing between reactant boxes
use constant H_SPACING => 50; # min horizontal spacing between reactant boxes
use constant WEDGE_SPACING => 30; # vert spacing between reaction and its "parent"
use constant INPUT_COLOR    => [255,130,71];
use constant REACTION_COLOR => [255,255,0];
use constant OUTPUT_COLOR   => [176,196,222];
use constant CATALYST_COLOR => [200,220,160];
use constant PARENT_COLOR    => [qw(222 184 135)];
use constant WEDGE_COLOR     => [255,218,185];
use constant EVENT_COLOR     => [150,150,150];
use constant TRIANGLE_HEIGHT => 60;
use constant ALPHA_COLORS    => [[255,0,0],[0,255,0],[0,0,255],
				 [255,255,0],[0,255,255],[255,0,255]];
use constant PATHWAY_FONT    => 'Palatino:italic';
use constant PATHWAY_FS      => 9;

sub new {
  my $class = shift;
  $class = ref($class) if ref $class;

  my ($title,$input,$output,$catalyst,$parent,$preceding,$following)
    = rearrange([qw(TITLE INPUT OUTPUT CATALYST PARENT PRECEDING FOLLOWING)],@_);
  $title ||= 'untitled reaction';
  return bless {
		title    => $title,
		parent   => $class->_force_array($parent),
		input    => $class->_force_array($input),
		output   => $class->_force_array($output),
		catalyst => $class->_force_array($catalyst),
		preceding => $class->_force_array($preceding),
		following => $class->_force_array($following),
		},$class;
}

sub title     { return shift->{title}    }
sub input     { return shift->{input}    }
sub output    { return shift->{output}   }
sub catalyst  { return shift->{catalyst} }
sub parent    { return shift->{parent} }
sub preceding { return shift->{preceding} }
sub following { return shift->{following} }

sub draw {
  my $self   = shift;
  my $box_formatter = GKB::Graphics::SimpleDraw::BoxFactory->new(-font   => gdSmallFont());

  my @input_b       = $box_formatter->format_boxes(@{$self->input});
  my $title_b       = $box_formatter->format_boxes($self->title);
  my @output_b      = $box_formatter->format_boxes(@{$self->output});
  my @catalyst_b    = $box_formatter->format_boxes(@{$self->catalyst});

  # three columns, one each for input, output and title
  # find the one that is largest - it defines the overall height
  my $max_height   = $self->_max($self->_sum_height(@input_b),
				 $self->_sum_height($title_b),
				 $self->_sum_height(@output_b));

  my $catalyst_top    = $catalyst_b[0] ? ($max_height+$title_b->height)/2+$catalyst_b[0]->height+H_SPACING : 0;
  my $catalyst_bottom = $catalyst_b[1] ? ($max_height+$title_b->height)/2+$catalyst_b[1]->height+H_SPACING : 0;
  $max_height         = $self->_max($max_height,$catalyst_top,$catalyst_bottom);

  my $max_width = $self->_max(map {$_->width} @input_b)
    + $title_b->width
      + $self->_max(map {$_->width} @output_b)
	+ H_SPACING *2;

  my $reaction_height = $max_height;

  # now we're going to offset the entire thing to make room for preceding and following events
  my @preceding_b   = $box_formatter->format_boxes(@{$self->preceding});
  my @following_b   = $box_formatter->format_boxes(@{$self->following});
  my $extra_height  = $self->_max($self->_sum_height(@preceding_b),
				  $self->_sum_height(@following_b));
  $max_height  += $extra_height;

  # this is perhaps where pathways go?
  my $reaction_top = TRIANGLE_HEIGHT;
  $max_height     += $reaction_top;

  # create image
  my $gd  = GD::Image->new($max_width+1,$max_height+1,1) or croak;
  $gd->saveAlpha(0);
  $gd->alphaBlending(1);
  my $white = $gd->colorAllocate(255,255,255);
  my $black = $gd->colorAllocate(0,0,0);
  $gd->filledRectangle(0,0,$max_width+3,$max_height+3,$white);

  my @alpha_colors = map {$gd->colorAllocateAlpha(@$_,90)} (@{ALPHA_COLORS()});

  my $three_part_arrow = GKB::Graphics::SimpleDraw::ThreePartArrow->new($gd);
  my $catalyst_arrow   = GKB::Graphics::SimpleDraw::ThreePartArrow->new(-GD=>$gd,
									-color=>CATALYST_COLOR,
									-dashed=>1
								       );
  my $event_arrow   = GKB::Graphics::SimpleDraw::TwoPartArrow->new(-GD=>$gd,
								   -color=>EVENT_COLOR,
								   -dashed=>1
								  );

  my $mid_y = TRIANGLE_HEIGHT + $reaction_height/2;
  $mid_y    = TRIANGLE_HEIGHT + $catalyst_b[0]->height + H_SPACING + $title_b->height/2 - 10 # bug somewhere
    if $catalyst_b[0] && TRIANGLE_HEIGHT + $catalyst_b[0]->height + H_SPACING + $title_b->height/2 > $mid_y;

  my $mid_x = $max_width/2;

  my $title_top   = $mid_y - $self->_sum_height($title_b)/2;

  # placeholder - draw the pathways
  $gd->useFontConfig(1);
  my $division = $max_width/(@{$self->parent}+1);

  for (my $i=0;$i<@{$self->parent};$i++) {
    my $parent = $self->parent->[$i];
    $parent    =~  s/<[^>]+?>//g;  # remove HTML
    my @args = ($black,
		PATHWAY_FONT,
		PATHWAY_FS,   # font size
		0,   # rotation
		);
    my @bounds = GD::Image->stringFT(@args,0,0,$parent);
    my $width  = $bounds[2]-$bounds[0];
    my $left   = $division*($i+1) - $width/2;
    @bounds    = $gd->stringFT(@args,$left,12,$parent);

    # draw the triangle
    my $top    = $bounds[1]+3;
    my $center = ($bounds[0]+$bounds[4])/2;
    my $bottom = $reaction_top-3;
    my $l      = $max_width*0.1;
    my $r      = $max_width*0.9;
    my $transparent_color = $alpha_colors[$i % @alpha_colors];
    my $poly = GD::Polygon->new;
    $poly->addPt($center,$top);
    $poly->addPt($l,$bottom);
    $poly->addPt($r,$bottom);
    $poly->addPt($center,$top);
    $gd->filledPolygon($poly,$transparent_color);
  }

  # layout and draw column 1
  my $right = $self->_max(map{$_->width} @input_b);
  my $title_left = $right + H_SPACING;

  my $top   = $mid_y - $self->_sum_height(@input_b)/2;
  for (my $i=0;$i<@input_b;$i++) {
    # box
    $input_b[$i]->render($gd,2+$right-$input_b[$i]->width,$top,{box=>1,fill=>INPUT_COLOR});
    # arrow
    my $x1 = $right+3;
    my $y1 = $top + $input_b[$i]->height/2;
    my $x2 = $title_left - 3;
    my $y2 = $title_top + $title_b->height/(@input_b+1) * ($i+1);
    $three_part_arrow->draw_horizontal($x1,$y1,$x2,$y2);
    $top += $input_b[$i]->height + V_SPACING;
  }

  # layout and draw column 2
  $title_b->render($gd,$title_left,$title_top,{box=>0});

  # layout and draw column 3
  my $left += $title_left + $title_b->width + H_SPACING;
  $top   = $mid_y - $self->_sum_height(@output_b)/2;
  for (my $i=0;$i<@output_b;$i++) {
    # box
    $output_b[$i]->render($gd,$left,$top,{box=>1,fill=>OUTPUT_COLOR});
    # arrow
    my $x1 = $title_left + $title_b->width + 3;
    my $y1 = $title_top  + $title_b->height/(@output_b+1) * ($i+1);
    my $x2 = $left - 3;
    my $y2 = $top + $output_b[$i]->height/2;
    $three_part_arrow->draw_horizontal($x1,$y1,$x2,$y2);
    $top += $output_b[$i]->height + V_SPACING;
  }

  # currently we only support two catalysts - one on top and one on bottom
  if (my $c = $catalyst_b[0]) {
    my $left = $mid_x     - $c->width/2;
    my $top  = $title_top - $catalyst_b[0]->height - H_SPACING/2;
    $c->render($gd,$left,$top,{box=>1,fill=>CATALYST_COLOR});
    $catalyst_arrow->draw($mid_x,$top+$c->height+3,$mid_x,$title_top-3);
  }

  # currently we only support two catalysts - one on top and one on bottom
  if (my $c = $catalyst_b[1]) {
    my $left = $mid_x     - $c->width/2;
    my $top  = $title_top + $title_b->height + H_SPACING/2;
    $c->render($gd,$left,$top,{box=>1,fill=>CATALYST_COLOR});
    $catalyst_arrow->draw($mid_x,$top-3,$mid_x,$title_top+$title_b->height+3);
  }

  # draw a box around entire thing
  $gd->rectangle(0,$reaction_top-2,$gd->width-1,$gd->height-$extra_height,$gd->colorResolve(0,0,0));

  # draw in preceding and following events
  if (@preceding_b || @following_b) {
      $right     = $mid_x - $title_b->width/3 - H_SPACING;
      my $bottom = $max_height - 2;
      my $pixels_per_arrow = $title_b->width/(@preceding_b + @following_b);
      my $touchdown        = $title_left + $pixels_per_arrow;
      
      for (my $i=0;$i<@preceding_b;$i++) {
	  my $box = $preceding_b[$i];
	  $box->render($gd,$right-$box->width,$bottom-$box->height,{box=>0});
	  $event_arrow->draw_vertical($right+3,$bottom - $box->height/2,
				      $touchdown,$max_height-$extra_height+6);
	  $top -= $box->height - V_SPACING;
	  $touchdown += $pixels_per_arrow;
      }
      
      $bottom   = $max_height - 2;
      for (my $i=0;$i<@following_b;$i++) {
	  my $box = $following_b[$i];
	  $box->render($gd,$max_width-$box->width,$bottom-$box->height,{box=>0});
	  $event_arrow->draw_horizontal($touchdown,$max_height-$extra_height+6,
					$max_width-$box->width-3,$bottom-$box->height/2,
				       );
	  $bottom -= $box->height - V_SPACING;
	  $touchdown += $pixels_per_arrow;
      }
  }
  return $gd;
}

sub _sum_height {
  my $self = shift;
  my @boxes = @_;
  my $height;
  foreach (@boxes) {
    $height += $_->height;
  }
  $height + @boxes * V_SPACING - 1;
}


sub _sum_width {
  my $self = shift;
  my @boxes = @_;
  my $width;
  foreach (@boxes) {
    $width += $_->width;
  }
  $width;
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

1;
