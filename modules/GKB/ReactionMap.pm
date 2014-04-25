package GKB::ReactionMap;

use strict;
no strict 'subs';
use vars qw(@ISA $AUTOLOAD %ok_field $coordinate_table);
use Bio::Root::Root;
use CGI;
#use GD; tai use GD::SVG valitaan tilanteen mukaan
#use GD::SVG;
use GD::Polygon;

use GKB::NamedInstance;
use GKB::PrettyInstance;
use GKB::Config;
use GKB::Utils;

@ISA = qw(Bio::Root::Root);

for my $attr
    (qw(
	cgi
	width
	height
	x_offset
	y_offset
	x_magnification
	y_magnification
	dba
	image
	default_color
	highlite_color
	arrow_size
	arrow_lw_ratio
	line_thickness
	zoom
	default_view_created
	usemap
        use_default_image
	use_antialiased
	coordinate_array
	base_url
	origin_instance
	arrow_shaft_width
	arrowhead_width
	arrowhead_length
	format
	) ) { $ok_field{$attr}++; }


sub AUTOLOAD {
    my $self = shift;
    my $attr = $AUTOLOAD;
    $attr =~ s/.*:://;
    return unless $attr =~ /[^A-Z]/;  # skip DESTROY and all-cap methods
    $self->throw("invalid attribute method: ->$attr()") unless $ok_field{$attr};
    $self->{$attr} = shift if @_;
    $self->_create_get_set_method($attr);
    return $self->{$attr};
}  

sub _create_get_set_method {
    my ($self,$attr) = @_;
    my $str = <<__HERE__;
sub $attr {
    my \$self = shift;
    if (\@_) {
	\$self->{$attr} = shift;
    }
    return \$self->{$attr};  
}
__HERE__
    eval $str;
    $@ && $self->throw($@);
}

sub new{
    my($pkg, @args) = @_;
    my $self = bless {}, $pkg;
    my ($debug,$cgi,$width,$height,$dba,$use_antialised,$no_def_img,$origin_instance,$format) = $self->_rearrange
	([qw(
	     DEBUG
	     CGI
	     WIDTH
	     HEIGHT
	     DBA
	     USE_ANTIALIASED
	     NO_DEFAULT_IMAGE
	     ORIGIN_INSTANCE
	     FORMAT
	     )], @args);
    $self->debug($debug);
    $dba && $self->dba($dba);
    # Temporary hack
    $coordinate_table = $dba->ontology->is_valid_class('ReactionCoordinates')
	? 'ReactionCoordinates'
	: 'EventLocation';
## width and height of image
    $width ? $self->width($width) : $self->width($REACTIONMAP_WIDTH);
    $height ? $self->height($height) : $self->height(int(($self->width)/ 6));
#    $use_antialised && $self->use_antialiased($use_antialised);
#    $self->use_antialiased(1);
    $origin_instance && $self->origin_instance($origin_instance);
    $format && $self->format($format);
    if ($cgi) {
	$self->cgi($cgi);
	$self->_handle_cgi_params($cgi);
    } else {
	$self->x_offset(0);
	$self->y_offset(0);
	$self->zoom(1);
    }
    $no_def_img && $self->use_default_image(undef);
    $self->_init;
    return $self;
}

#HUOM tanne pitaa mahdollisesti laittaa svg:ta, tai sitten luoda erillinen svg-init-palikka
sub _init {
    my ($self) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my $extention ;
    my $require_attribute;
#svg-valinta taalla ei toimi!!!
    if($self->format){
	$extention = "svg";
	$require_attribute = "GD::SVG";
    }
    else{
	$extention = "png";
	$require_attribute = "GD";
    }
    eval "require $require_attribute";
    $@ && $self->throw("couldn't load module : $@");
    my $image;
    my $image2;
    # The lines labelled as '# HACK 20060613' are addressing the problem with drawing the arrow for instance 110437 as reported by Guanming on 20060612.
    # Somehow, one of the lines drawn is too long. Making the image 1 pixel higher resolves this particula issue but I have no clue why did
    # it occur in the 1st place.
    if ($self->format) {
	$image = GD::SVG::Image->new($self->width,$self->height,1);
    } else {
#	$image = GD::Image->new($self->width,$self->height,1);
	$image = GD::Image->new($self->width,$self->height+1,1); # HACK 20060613
	$image->transparent($image->colorAllocate(255,255,255));
    }
#    $image->filledRectangle(0,0,$self->width - 1,$self->height - 1,$image->colorAllocate(255,255,255));
    $image->filledRectangle(0,0,$self->width - 1,$self->height,$image->colorAllocate(255,255,255)); # HACK 20060613
    $self->use_default_image(undef);
    $self->image($image);
    unless ($self->x_magnification && $self->y_magnification) {
	my ($max_x,$max_y) = $self->find_max_xy2;
	my $x_mag = ($max_x) ? $self->width / $max_x : 1;
	my $y_mag = ($max_y) ? $self->height / $max_y : 1;
	my $mag = ($x_mag < $y_mag) ? $x_mag : $y_mag;
#	print "$max_x,$max_y,$x_mag,$y_mag,$mag\n";
	$self->x_magnification($mag);
	$self->y_magnification($mag);
    }
    $self->_install_default_attributes;
}

# overload the Bio::Root::Root method
sub debug {
    my $self = shift;
    if (@_) {
	$self->{'debug'} = shift;
    }
    return $self->{'debug'};
}

sub get_default_color {
    my ($self) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    return $self->get_rgb_color(@{$self->default_color});
}

#eiko taa olekaytossa??
=head
sub create_default_view {
    my $self = shift;
    $self->debug && print "", (caller(0))[3], "\n";
#    my $start = time();
    $self->dba || $self->throw("Need dba");
    my $color = $self->get_default_color;
    my $query = qq(SELECT el.sourceX,el.sourceY,el.targetX,el.targetY,el.locatedEvent,do._displayName FROM $coordinate_table el, DatabaseObject do WHERE do.DB_ID=el.locatedEvent);
    my ($sth,$res) = $self->dba->execute($query);
    my $aar = $sth->fetchall_arrayref;
    my $image = $self->draw_as_arrows($aar,$color);
#    print qq(<PRE>), (caller(0))[3],"\t",  time() - $start, qq(</PRE>\n);
    $self->create_usemap($aar);
    return $image;
}
=cut


sub draw_as_arrows {
    my ($self,$aar,$color) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
#    my $start = time();
    my $image = $self->image;
    my $arrow_size = $self->arrow_size;
    my $arrow_lw_ratio = $self->arrow_lw_ratio;
    my $x_offset =  $self->x_offset;
    my $x_mag = $self->zoom * $self->x_magnification;
    my $y_offset =  $self->y_offset;
    my $y_mag = $self->zoom * $self->y_magnification;
    foreach my $ar (@{$aar}) {
#	print "[", join(',',@{$ar}[0..3]), "],\n";
	my $sx = $ar->[0];
	my $sy = $ar->[1];
	my $tx = $ar->[2];
	my $ty = $ar->[3];
	my $db_id = $ar->[4];
	my $displayName = $ar->[5];
	my $dx = $tx - $sx;
	my $dy = $ty - $sy;
	my $d = int((sort {$b <=> $a} (1, sqrt($dx * $dx + $dy * $dy)))[0]);
	my $ax = - ($arrow_size * $dx / $d);
	my $ay = - ($arrow_size * $dy / $d);
#	print qq(<PRE>draw_as_arrows\t$sx,$sy,$tx,$ty</PRE>\n);
	$image->line(($sx - $x_offset) * $x_mag,
		     ($sy - $y_offset) * $y_mag,
		     ($tx - $x_offset) * $x_mag,
		     ($ty - $y_offset) * $y_mag,
		     $color);
	# Closed arrowhead
	my $poly = new GD::Polygon;
#	$poly->addPt(($tx - $x_offset) * $x_mag,($ty - $y_offset) * $y_mag);
#	$poly->addPt((($tx + $ax + $ay / $arrow_lw_ratio) - $x_offset) * $x_mag,
#		     (($ty + $ay - $ax / $arrow_lw_ratio) - $y_offset) * $y_mag);
#	$poly->addPt((($tx + $ax - $ay / $arrow_lw_ratio) - $x_offset) * $x_mag,
#		     (($ty + $ay + $ax / $arrow_lw_ratio) - $y_offset) * $y_mag);
	$poly->addPt(int(($tx - $x_offset) * $x_mag),int(($ty - $y_offset) * $y_mag));
	$poly->addPt(int((($tx + $ax + $ay / $arrow_lw_ratio) - $x_offset) * $x_mag),
		     int((($ty + $ay - $ax / $arrow_lw_ratio) - $y_offset) * $y_mag));
	$poly->addPt(int((($tx + $ax - $ay / $arrow_lw_ratio) - $x_offset) * $x_mag),
		     int((($ty + $ay + $ax / $arrow_lw_ratio) - $y_offset) * $y_mag));
	$image->filledPolygon($poly,$color);
#	$image->line(($tx - $x_offset) * $x_mag,
#		     ($ty - $y_offset) * $y_mag,
#		     (($tx + $ax + $ay / $arrow_lw_ratio) - $x_offset) * $x_mag,
#		     (($ty + $ay - $ax / $arrow_lw_ratio) - $y_offset) * $y_mag,
#		     $color);
#	$image->line(($tx - $x_offset) * $x_mag,
#		     ($ty - $y_offset) * $y_mag,
#		     (($tx + $ax - $ay / $arrow_lw_ratio) - $x_offset) * $x_mag,
#		     (($ty + $ay + $ax / $arrow_lw_ratio) - $y_offset) * $y_mag,
#		     $color);
    }
#    $self->default_view_created(1);
#    print qq(<PRE>), time() - $start, qq(</PRE>\n);
    return $image;
}


sub draw_as_lines {
    my ($self,$aar,$color) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
#    my $start = time();
    my $image = $self->image;
    my $x_offset =  $self->x_offset;
    my $x_mag = $self->zoom * $self->x_magnification;
    my $y_offset =  $self->y_offset;
    my $y_mag = $self->zoom * $self->y_magnification;
    foreach my $ar (@{$aar}) {
	my $sx = $ar->[0];
	my $sy = $ar->[1];
	my $tx = $ar->[2];
	my $ty = $ar->[3];
	$image->line(($sx - $x_offset) * $x_mag,
		     ($sy - $y_offset) * $y_mag,
		     ($tx - $x_offset) * $x_mag,
		     ($ty - $y_offset) * $y_mag,
		     $color);
    }
#    print qq(<PRE>), time() - $start, qq(</PRE>\n);
    return $image;
}

sub create_usemap {
    my ($self,$aar) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    $aar ||= $self->coordinate_array;
#    my $start = time();
    my $x_offset =  $self->x_offset;
    my $x_mag = $self->zoom * $self->x_magnification;
    my $y_offset =  $self->y_offset;
    my $y_mag = $self->zoom * $self->y_magnification;
    my $usemap = $self->usemap || '';
    my $db = $self->cgi->param('DB');
#    my $baseurl = ($self->base_url || $self->cgi->script_name) . "?DB=$db&ID=";
    my $origin_instance_db_id = "''";
    if (my $tmp = $self->origin_instance) {
	$origin_instance_db_id = $tmp->db_id;
    }
    foreach my $ar (@{$aar}) {
	my $sx = int(($ar->[0] - $x_offset) * $x_mag);
	my $sy = int(($ar->[1] - $y_offset) * $y_mag);
	my $tx = int(($ar->[2] - $x_offset) * $x_mag);
	my $ty = int(($ar->[3] - $y_offset) * $y_mag);
	my $db_id = $ar->[4];
#	(my $displayName = $ar->[5]) =~ s/[<>]//g;
	(my $displayName = $ar->[5]) =~ s/</\&lt;/g;
	$displayName =~ s/>/\&gt;/g;
	if ($displayName !~ /\s/) {
	    $displayName =~ s/-/- /g;
	}
	$displayName =~ s/\'/\`/g;
	$displayName =~ s/\"/&quot;/g;
	my $coord_str = join(',',GKB::Utils::rectangular_polygon_coordinates($sx,$sy,$tx,$ty,2));
	$usemap .= qq(<AREA SHAPE="poly" COORDS="$coord_str" ONMOUSEOVER="handleMouseOver($db_id,'$displayName','dcdcdc',250,$origin_instance_db_id);" ONMOUSEOUT='handleMouseOut();' ONCLICK="handleClick2($origin_instance_db_id,$db_id,event)">\n);
    }
    $self->usemap($usemap);
    return $usemap;
}

sub _add_background_handling_to_usemap {
    my $self = shift;
    $self->debug && print "", (caller(0))[3], "\n";
    my $usemap = $self->usemap;
    my $width = $self->width;
    my $height = $self->height;
#    $usemap .= qq(<AREA COORDS="1,1,$width,$height" ONCLICK='handleClick(0,event)'>\n);
    my $origin_instance_db_id = "''";
    if (my $tmp = $self->origin_instance) {
	$origin_instance_db_id = $tmp->db_id;
    }
    $usemap .= qq(<AREA COORDS="1,1,$width,$height" ONCLICK="handleClick2($origin_instance_db_id,0,event)">\n);
    $self->usemap($usemap);
    return $usemap;
}

sub _install_default_attributes {
    my $self = shift;
    $self->debug && print "", (caller(0))[3], "\n";
    $self->image || $self->throw("No image!?");
    $self->default_color($DEFAULT_REACTION_COLOR);
    $self->highlite_color([0,0,128]);
    $self->arrow_size(6);
    $self->arrow_lw_ratio(3);
    $self->line_thickness(1/6);
}

sub set_highlite_color {
    my ($self,$r,$g,$b) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my $color = $self->get_rgb_color($r,$g,$b);
#    $self->highlite_color($color);
    return $color;
}

sub get_rgb_color {
    my ($self,$r,$g,$b) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my $color = $self->image->colorAllocate($r,$g,$b);
#    my $color = $self->image->colorResolve($r,$g,$b);
    if ($self->use_antialiased) {
	$self->image->setAntiAliased($color);
	$color = gdAntiAliased;
    }
    return $color;
}

sub draw_as_polygon {
    my ($self,$el,$color) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my $arrow_size = $self->arrow_size;
    my $arrow_lw_ratio = $self->arrow_lw_ratio;
    my $line_thickness = $self->line_thickness;
    my $sx = $el->SourceX->[0];
    my $sy = $el->SourceY->[0];
    my $tx = $el->TargetX->[0];
    my $ty = $el->TargetY->[0];
    my $dx = $tx - $sx;
    my $dy = $ty - $sy;
    my $d = int((sort {$b <=> $a} (1, sqrt($dx * $dx + $dy * $dy)))[0]);
    my $ax = - ($arrow_size * $dx / $d);
    my $ay = - ($arrow_size * $dy / $d);
    my $poly = new GD::Polygon;
    $poly->addPt($self->real2screenX($tx),
		 $self->real2screenY($ty));
    $poly->addPt($self->real2screenX($tx + $ax + $ay / $arrow_lw_ratio),
		 $self->real2screenY($ty + $ay - $ax / $arrow_lw_ratio));
    $poly->addPt($self->real2screenX($tx + $ax + $ay * $line_thickness),
		 $self->real2screenY($ty + $ay - $ax * $line_thickness));
    $poly->addPt($self->real2screenX($sx + $ax * $line_thickness + $ay * $line_thickness),
		 $self->real2screenY($sy + $ay * $line_thickness - $ax * $line_thickness));
    $poly->addPt($self->real2screenX($sx + $ax * $line_thickness - $ay * $line_thickness),
		 $self->real2screenY($sy + $ay * $line_thickness + $ax * $line_thickness));
    $poly->addPt($self->real2screenX($tx + $ax - $ay * $line_thickness),
		 $self->real2screenY($ty + $ay + $ax * $line_thickness));
    $poly->addPt($self->real2screenX($tx + $ax - $ay / $arrow_lw_ratio),
		 $self->real2screenY($ty + $ay + $ax / $arrow_lw_ratio));
    $self->image->filledPolygon($poly,$color);
}

sub draw_as_arrow {
    my ($self,$el,$color) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my $arrow_size = $self->arrow_size;
    my $arrow_lw_ratio = $self->arrow_lw_ratio;
    my $sx = $el->SourceX->[0];
    my $sy = $el->SourceY->[0];
    my $tx = $el->TargetX->[0];
    my $ty = $el->TargetY->[0];
    my $dx = $tx - $sx;
    my $dy = $ty - $sy;
    my $d = int((sort {$b <=> $a} (1, sqrt($dx * $dx + $dy * $dy)))[0]);
    my $ax = - ($arrow_size * $dx / $d);
    my $ay = - ($arrow_size * $dy / $d);
    $self->image->line($self->real2screenX($sx),
		       $self->real2screenY($sy),
		       $self->real2screenX($tx),
		       $self->real2screenY($ty),
		       $color);
    $self->image->line($self->real2screenX($tx),
		       $self->real2screenY($ty),
		       $self->real2screenX($tx + $ax + $ay / $arrow_lw_ratio),
		       $self->real2screenY($ty + $ay - $ax / $arrow_lw_ratio),
		       $color);
    $self->image->line($self->real2screenX($tx),
		       $self->real2screenY($ty),
		       $self->real2screenX($tx + $ax - $ay / $arrow_lw_ratio),
		       $self->real2screenY($ty + $ay + $ax / $arrow_lw_ratio),
		       $color);
}

sub draw_highlites_as_lines {
    my ($self, $ar) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my $color = $self->highlite_color; # is red
    foreach (@{$ar}){
	$self->draw_as_arrow($_,$color);
    }
}

sub draw_highlites_as_polygons {
    my ($self, $ar) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my $color = $self->highlite_color; # is red
    foreach (@{$ar}){
	$self->draw_as_polygon($_,$color);
    }
}

sub find_max_xy {
    my ($self,$coords) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my ($max_x,$max_y) = (0,0);
    foreach my $i (@{$coords}) {
	$max_x = (sort{$b <=> $a} ($max_x,$i->SourceX->[0],$i->TargetX->[0]))[0];
	$max_y = (sort{$b <=> $a} ($max_y,$i->SourceY->[0],$i->TargetY->[0]))[0];
    }
    return ($max_x,$max_y);
}

sub find_max_xy2 {
    my $self = shift;
    $self->debug && print "", (caller(0))[3], "\n";
    my $query = "SELECT MAX(sourceX),MAX(sourceY),MAX(targetX),MAX(targetY) FROM $coordinate_table";
    my ($sth,$res) = $self->dba->execute($query);
    my ($sx,$sy,$tx,$ty) = @{$sth->fetchrow_arrayref()};
    my ($max_x,$max_y);
    if (defined $sx and defined $tx) {
	$max_x = (sort{$b <=> $a} ($sx,$tx))[0];
    }
    if (defined $sy and defined $ty) {
	$max_y = (sort{$b <=> $a} ($sy,$ty))[0];
    }
    return ($max_x,$max_y);
}

sub real2screenX {
    my ($self,$c) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    return int($self->zoom * (($c - $self->x_offset) * $self->x_magnification));
}

#sub real2screenX {
#    return int($_[0]->zoom * (($_[1] - $_[0]->x_offset) * $_[0]->x_magnification));
#}

sub real2screenY {
    my ($self,$c) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    return int($self->zoom * (($c - $self->y_offset) * $self->y_magnification));

}

sub _find_viewable_area {
    my $self = shift;
    $self->debug && print "", (caller(0))[3], "\n";
    return
	(
	 $self->x_offset,
	 $self->y_offset,
	 int($self->x_offset + $self->width / $self->x_magnification / $self->zoom),
	 int($self->y_offset + $self->height / $self->y_magnification / $self->zoom)
	);
}

sub _handle_cgi_params {
    my ($self,$cgi) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    $cgi ||= $self->cgi || $self->throw("Need cgi object.");
    my ($max_x,$max_y) = $self->find_max_xy2;
    my ($x_mag,$y_mag);
    unless ($x_mag = $cgi->param('X_MAG') and  $y_mag = $cgi->param('Y_MAG')) {
#	print qq(<PRE> _handle_cgi_params\t$max_x,$max_y</PRE>\n);
	my $x_mag_tmp = ($max_x) ? $self->width / $max_x : 1;
	my $y_mag_tmp = ($max_y) ? $self->height / $max_y : 1;
	my $def_mag = ($x_mag_tmp < $y_mag_tmp) ? $x_mag_tmp : $y_mag_tmp;
	$x_mag = $y_mag = $def_mag;
#	$self->use_default_image(1);
    }
    $self->x_magnification($x_mag);
    $self->y_magnification($y_mag);
    my $x_offset = $cgi->param('X_OFFSET') || 0;
    my $y_offset = $cgi->param('Y_OFFSET') || 0;
    my $zoom = $cgi->param('ZOOM') || 1;
    $self->zoom($zoom);
    my ($dx,$dy) = (0,0);
    my $tmp;
    # This is because IE on (Win at least) does not pass back the value assigned
    # to the button, just the x and y coords of the click. Silly...
    if ($tmp = $cgi->param('MOVE_L')) {
	$dx = $tmp;
    } elsif ($cgi->param('MOVE_L.x')) {
	$dx = -0.25;
    } elsif ($tmp = $cgi->param('MOVE_R')) {
	$dx = $tmp;
    } elsif ($cgi->param('MOVE_R.x')) {
	$dx = 0.25;
    }
    if ($tmp = $cgi->param('MOVE_U')) {
	$dy = $tmp;
    } elsif ($cgi->param('MOVE_U.x')) {
	$dy = -0.25;
    } elsif ($tmp = $cgi->param('MOVE_D')) {
	$dy = $tmp;
    } elsif ($cgi->param('MOVE_D.x')) {
	$dy = 0.25;
    }
    $dx *= $self->width / $zoom / $x_mag;
    $dy *= $self->height / $zoom / $y_mag;
    #Only retain the previous highlites if there's no request for changed focus.
    #Yes, the check for it is rather indirect.
    if (($cgi->param('FOCUS') and $cgi->param('ID') and $cgi->param('FOCUS') == $cgi->param('ID'))
	or $zoom != 1 or $dx or $dy) {
	if (my $str = $cgi->param('HIGHLITES')) {
	    foreach my $substr (split /\;/, $str) {
		my ($r,$g,$b,@reaction_ids) = split /,/, $substr;
		$self->set_reaction_color_by_id($r,$g,$b,\@reaction_ids);
	    }
	}
	if ($zoom != 1) {
	    my $x_mapclick = $self->cgi->param('REACTIONMAP_x');
	    my $y_mapclick = $self->cgi->param('REACTIONMAP_y');
	    if (!(defined $x_mapclick)) {
	    	$x_mapclick = 0;
	    }
	    if (!(defined $y_mapclick)) {
	    	$y_mapclick = 0;
	    }
	    $x_offset = $x_offset + $x_mapclick / $x_mag -  $x_mapclick / ($x_mag * $zoom);
	    $y_offset = $y_offset + $y_mapclick / $y_mag -  $y_mapclick / ($y_mag * $zoom);
#	    print qq(<PRE>$x_mapclick,$y_mapclick,$x_offset,$y_offset</PRE>\n);
	} else {
	    $x_offset += $dx;
	    $y_offset += $dy;
	}
    }
    if ($x_offset < 0) {
	$x_offset = 0;
    } elsif ($x_offset > $max_x) {
	$x_offset = $max_x;
    }
    if ($y_offset < 0) {
	$y_offset = 0;
    } elsif ($y_offset > $max_y) {
	$y_offset = $max_y;
    }
    $self->x_offset($x_offset);
    $self->y_offset($y_offset);
}

sub set_reaction_color_by_id {
    my ($self,$r,$g,$b,$reaction_ids) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    if (@{$reaction_ids}) {
	push @{$self->{'reaction_color'}}, [[$r,$g,$b],$reaction_ids];
    }
}

sub set_reaction_color {
    my ($self,$r,$g,$b,$reactions) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    if (@{$reactions}) {
	push @{$self->{'reaction_color'}}, [[$r,$g,$b],[map {$_->db_id} @{$reactions}]];
    }
}

sub _get_all_colored_reaction_ids {
    my $self = shift;
    $self->debug && print "", (caller(0))[3], "\n";
    my @out;
    foreach my $ar (@{$self->{'reaction_color'}}) {
	push @out, @{$ar->[1]};
    }
    return \@out;
}

sub highlites_as_hidden_param {
    my $self = shift;
    $self->debug && print "", (caller(0))[3], "\n";
    my $str = '';
    foreach my $ar (@{$self->{'reaction_color'}}) {
	$str .= ';' if ($str);
	$str .= join(",", @{$ar->[0]}, @{$ar->[1]});
    }
    return qq(<INPUT TYPE="hidden" NAME="HIGHLITES" VALUE="$str" />);
}

sub create_image {
    my ($self) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my $species = GKB::Utils::find_focus_species_or_default($self->dba,$self->cgi,$self->origin_instance);
    return $self->create_image_for_species($species);
}

sub create_image_with_all_reactions {
    my ($self) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my $start = time();
    my ($x_min,$y_min,$x_max,$y_max) = $self->_find_viewable_area;
#    print qq(<PRE>create_image\t$x_min,$y_min,$x_max,$y_max</PRE>\n);
    $self->_draw_non_highlites($x_min,$y_min,$x_max,$y_max);
    $self->_draw_highlites($x_min,$y_min,$x_max,$y_max);
    $self->_add_background_handling_to_usemap;
#    print qq(<PRE>), (caller(0))[3],"\t",  time() - $start, qq(</PRE>\n);
    return $self->image;
}

sub create_frontpage_image {
    my ($self,$species) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my $event_sp_tbl = GKB::Utils::get_Event_species_table($self->dba);
    my $sp_db_id = ($species) ? $species->db_id : GKB::Utils::find_focus_species_or_default($self->dba,$self->cgi)->[0]->db_id;
    my $query = qq(
SELECT el.sourceX,el.sourceY,el.targetX,el.targetY,el.locatedEvent,do._displayName
FROM $coordinate_table el, DatabaseObject do, $event_sp_tbl es
WHERE do.DB_ID = el.locatedEvent
AND es.DB_ID = el.locatedEvent
AND es.species = $sp_db_id 
);
    my ($sth,$res) = $self->dba->execute($query);
    my $aar = $sth->fetchall_arrayref;
    if (@{$aar}) {
		$self->coordinate_array($aar);
		$self->_draw_connecting_lines($aar);
		$self->draw_as_arrows($aar,$self->get_default_color);
    }
     	
    return $self->image;
}

sub create_image_for_species {
    my ($self,$species,$noglow) = @_;
#    print qq(<PRE>) . $self->stack_trace_dump . qq(</PRE>\n);
    $self->debug && print "", (caller(0))[3], "\n";
    $species || $self->throw("Need species instance, got '$species'.");
    my $start = time();
    my ($x_min,$y_min,$x_max,$y_max) = $self->_find_viewable_area;
#    print qq(<PRE>create_image\t$x_min,$y_min,$x_max,$y_max</PRE>\n);
    $noglow || $self->_draw_highlited_background_for_species($x_min,$y_min,$x_max,$y_max,$species);
    $self->_draw_non_highlites_for_species($x_min,$y_min,$x_max,$y_max,$species);
    $self->_draw_highlites_for_species($x_min,$y_min,$x_max,$y_max,$species);
    $self->_add_background_handling_to_usemap;
#    print qq(<PRE>), (caller(0))[3],"\t",  time() - $start, qq(</PRE>\n);
    return $self->image;
}

sub _draw_non_highlites {
    my ($self,$x_min,$y_min,$x_max,$y_max) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my $start = time();
    my $query = qq/
SELECT el.sourceX,el.sourceY,el.targetX,el.targetY,el.locatedEvent,do._displayName
FROM $coordinate_table el, DatabaseObject do
WHERE do.DB_ID=el.locatedEvent
AND ((el.sourceX > $x_min) OR (el.targetX > $x_min))
AND ((el.sourceY > $y_min) OR (el.targetY > $y_min))
AND ((el.sourceX < $x_max) OR (el.targetX < $x_max))
AND ((el.sourceY < $y_max) OR (el.targetY < $y_max))
/;
    my $colored = $self->_get_all_colored_reaction_ids;
    if (@{$colored}) {
        $query .= "\nAND el.locatedEvent NOT IN(" . join(',',@{$colored}) . ')';
#	print qq(<PRE>\n$query\n</PRE>\n);
    }
    my ($sth,$res) = $self->dba->execute($query);
    my $aar = $sth->fetchall_arrayref;
    unless($self->use_default_image) {
	$self->_draw_connecting_lines($aar,$colored);
	$self->draw_as_arrows($aar,$self->get_default_color);
    }
    $self->create_usemap($aar);
#    print qq(<PRE>), (caller(0))[3],"\t",  time() - $start, qq(</PRE>\n);
}

sub _draw_connecting_lines {
    my ($self,$aar,$colored) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my $color = $self->get_rgb_color(@{$REACTION_CONNECTOR_COLOR});
    my @reactions;
    $colored && push @reactions, @{$colored};
    map {push @reactions,$_->[4]} @{$aar};
    my $in_str = join(',', @reactions);
    my $query = 
"SELECT el2.targetX,el2.targetY,el1.sourceX,el1.sourceY,el2.locatedEvent,el1.locatedEvent
FROM $coordinate_table el1, $coordinate_table el2, Event_2_precedingEvent pe
WHERE el1.locatedEvent = pe.DB_ID
AND pe.precedingEvent = el2.locatedEvent
AND el1.locatedEvent IN ($in_str)
AND el2.locatedEvent IN ($in_str)
";
    my ($sth,$res) = $self->dba->execute($query);
    my $aar2 = $sth->fetchall_arrayref;
    $self->draw_as_lines($aar2,$color);
}

sub _draw_connecting_lines_for_species {
    my ($self,$aar,$colored,$species) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my $color = $self->get_rgb_color(220,220,220);
    my @reactions;
    $colored && push @reactions, @{$colored};
    map {push @reactions,$_->[4]} @{$aar};
    if (@reactions) {
	my $in_str = join(',', @reactions);
	my $event_sp_tbl = GKB::Utils::get_Event_species_table($self->dba);
	my $sp_db_id_str = join(',', map {$_->db_id} @{$species});
	my $query = qq/
SELECT el2.targetX,el2.targetY,el1.sourceX,el1.sourceY,el2.locatedEvent,el1.locatedEvent
FROM $coordinate_table el1, $coordinate_table el2, Event_2_precedingEvent pe, $event_sp_tbl es
WHERE el1.locatedEvent = pe.DB_ID
AND es.DB_ID = el1.locatedEvent
AND es.species IN($sp_db_id_str)
AND pe.precedingEvent = el2.locatedEvent
AND el1.locatedEvent IN ($in_str)
AND el2.locatedEvent IN ($in_str)
/;
#	print qq(<PRE>$query</PRE>\n);
	my ($sth,$res) = $self->dba->execute($query);
	my $aar2 = $sth->fetchall_arrayref;
	$self->draw_as_lines($aar2,$color);
    }
}

sub _draw_connecting_lines_for_species_via_orthology {
    my ($self,$aar,$colored) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my $color = $self->get_rgb_color(220,220,220);
    my @reactions;
    $colored && push @reactions, @{$colored};
    map {push @reactions,$_->[4]} @{$aar};
    if (@reactions) {
	my $in_str = join(',', @reactions);
	my $query = 
"SELECT el2.targetX,el2.targetY,el1.sourceX,el1.sourceY,el2.locatedEvent,el1.locatedEvent
FROM $coordinate_table el1, $coordinate_table el2, Event_2_orthologousEvent oe1, Event_2_orthologousEvent oe2, Event_2_precedingEvent pe
WHERE el1.locatedEvent = oe1.DB_ID
AND oe1.orthologousEvent IN($in_str)
AND oe1.orthologousEvent = pe.DB_ID
AND pe.precedingEvent = oe2.orthologousEvent
AND oe2.DB_ID = el2.locatedEvent
";
#	print qq(<PRE>$query</PRE>\n);
	my ($sth,$res) = $self->dba->execute($query);
	my $aar2 = $sth->fetchall_arrayref;
	$self->draw_as_lines($aar2,$color);
    }
}

sub _draw_non_highlites_for_species {
    my ($self,$x_min,$y_min,$x_max,$y_max,$species) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my $start = time();
    my $event_sp_tbl = GKB::Utils::get_Event_species_table($self->dba);
    my $sp_db_id_str = join(',', map {$_->db_id} @{$species});
    my $query = qq/
SELECT el.sourceX,el.sourceY,el.targetX,el.targetY,el.locatedEvent,do._displayName
FROM $coordinate_table el, DatabaseObject do, $event_sp_tbl es
WHERE do.DB_ID = el.locatedEvent
AND es.DB_ID = el.locatedEvent
AND es.species IN ($sp_db_id_str)
AND ((el.sourceX > $x_min) OR (el.targetX > $x_min))
AND ((el.sourceY > $y_min) OR (el.targetY > $y_min))
AND ((el.sourceX < $x_max) OR (el.targetX < $x_max))
AND ((el.sourceY < $y_max) OR (el.targetY < $y_max))
/;
    my $colored = $self->_get_all_colored_reaction_ids;
    if (@{$colored}) {
        $query .= "\nAND el.locatedEvent NOT IN(" . join(',',@{$colored}) . ')';
#	print qq(<PRE>\n$query\n</PRE>\n);
    }
    my ($sth,$res) = $self->dba->execute($query);
    my $aar = $sth->fetchall_arrayref;
    $self->_draw_connecting_lines_for_species($aar,$colored,$species);
    $self->draw_as_arrows($aar,$self->get_default_color);
    $self->create_usemap($aar);
#    print qq(<PRE>), (caller(0))[3],"\t",  time() - $start, qq(</PRE>\n);
}

sub _draw_non_highlites_for_species_via_orthology {
    my ($self,$x_min,$y_min,$x_max,$y_max,$species) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my $start = time();
    my $query;
    if ($self->dba->ontology->is_multivalue_class_attribute('Event','species')) {
	$query = qq/
SELECT el.sourceX,el.sourceY,el.targetX,el.targetY,eoe.orthologousEvent,do._displayName
FROM $coordinate_table el, DatabaseObject do, Event_2_orthologousEvent eoe, Event e, Event_2_species es
WHERE el.locatedEvent = eoe.DB_ID
AND eoe.orthologousEvent = e.DB_ID
AND e.DB_ID = do.DB_ID
AND es.DB_ID = e.DB_ID
AND es.species = / . $species->db_id . qq/
AND ((el.sourceX > $x_min) OR (el.targetX > $x_min))
AND ((el.sourceY > $y_min) OR (el.targetY > $y_min))
AND ((el.sourceX < $x_max) OR (el.targetX < $x_max))
AND ((el.sourceY < $y_max) OR (el.targetY < $y_max))
/;
    } else {
	$query = qq/
SELECT el.sourceX,el.sourceY,el.targetX,el.targetY,eoe.orthologousEvent,do._displayName
FROM $coordinate_table el, DatabaseObject do, Event_2_orthologousEvent eoe, Event e
WHERE el.locatedEvent = eoe.DB_ID
AND eoe.orthologousEvent = e.DB_ID
AND e.DB_ID = do.DB_ID
AND e.species = / . $species->db_id . qq/
AND ((el.sourceX > $x_min) OR (el.targetX > $x_min))
AND ((el.sourceY > $y_min) OR (el.targetY > $y_min))
AND ((el.sourceX < $x_max) OR (el.targetX < $x_max))
AND ((el.sourceY < $y_max) OR (el.targetY < $y_max))
/;
    }
    my $colored = $self->_get_all_colored_reaction_ids;
    if (@{$colored}) {
        $query .= "AND eoe.orthologousEvent NOT IN(" . join(',',@{$colored}) . ')';
#	print qq(<PRE>\n$query\n</PRE>\n);
    }
    my ($sth,$res) = $self->dba->execute($query);
    my $aar = $sth->fetchall_arrayref;
    $self->_draw_connecting_lines_for_species($aar,$colored);
    $self->draw_as_arrows($aar,$self->get_default_color);
    $self->create_usemap($aar);
#    print qq(<PRE>), (caller(0))[3],"\t",  time() - $start, qq(</PRE>\n);
}

sub _draw_highlites {
    my ($self,$x_min,$y_min,$x_max,$y_max) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my $start = time();
    my $basequery = qq/
SELECT el.sourceX,el.sourceY,el.targetX,el.targetY,el.locatedEvent,do._displayName
FROM $coordinate_table el, DatabaseObject do
WHERE do.DB_ID=el.locatedEvent
AND ((el.sourceX > $x_min) OR (el.targetX > $x_min))
AND ((el.sourceY > $y_min) OR (el.targetY > $y_min))
AND ((el.sourceX < $x_max) OR (el.targetX < $x_max))
AND ((el.sourceY < $y_max) OR (el.targetY < $y_max))
/;
    foreach my $ar (@{$self->{'reaction_color'}}) {
	@{$ar->[1]} || next;
	my ($r,$g,$b) = @{$ar->[0]};
	my $color = $self->get_rgb_color($r,$g,$b);
        my $query = $basequery . "AND el.locatedEvent IN(" . join(',',@{$ar->[1]}) . ')';
        my ($sth,$res) = $self->dba->execute($query);
        my $aar = $sth->fetchall_arrayref;
        $self->draw_as_arrows($aar,$color);
        $self->create_usemap($aar);
    }
#    print qq(<PRE>), (caller(0))[3],"\t",  time() - $start, qq(</PRE>\n);
}

sub draw_highlites_wo_usemap {
    my ($self,$x_min,$y_min,$x_max,$y_max) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my $start = time();
    my $basequery = qq/
SELECT el.sourceX,el.sourceY,el.targetX,el.targetY,el.locatedEvent,do._displayName
FROM $coordinate_table el, DatabaseObject do
WHERE do.DB_ID=el.locatedEvent
/;
    if (defined $x_min && defined $y_min && defined $x_max && defined $y_max) {
	$basequery .= qq/AND ((el.sourceX > $x_min) OR (el.targetX > $x_min))
AND ((el.sourceY > $y_min) OR (el.targetY > $y_min))
AND ((el.sourceX < $x_max) OR (el.targetX < $x_max))
AND ((el.sourceY < $y_max) OR (el.targetY < $y_max))
/;
    }
    foreach my $ar (@{$self->{'reaction_color'}}) {
	@{$ar->[1]} || next;
	my ($r,$g,$b) = @{$ar->[0]};
	my $color = $self->get_rgb_color($r,$g,$b);
        my $query = $basequery . "AND el.locatedEvent IN(" . join(',',@{$ar->[1]}) . ')';
        my ($sth,$res) = $self->dba->execute($query);
        my $aar = $sth->fetchall_arrayref;
        $self->draw_as_arrows($aar,$color);
    }
#    print qq(<PRE>), (caller(0))[3],"\t",  time() - $start, qq(</PRE>\n);
}

sub _draw_highlites_for_species {
    my ($self,$x_min,$y_min,$x_max,$y_max,$species) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my $start = time();
    my $event_sp_tbl = GKB::Utils::get_Event_species_table($self->dba);
    my $sp_db_id_str = join(',', map {$_->db_id} @{$species});
    my $basequery = qq/
SELECT el.sourceX,el.sourceY,el.targetX,el.targetY,el.locatedEvent,do._displayName
FROM $coordinate_table el, DatabaseObject do, $event_sp_tbl es
WHERE do.DB_ID = el.locatedEvent
AND es.DB_ID = el.locatedEvent
AND es.species IN($sp_db_id_str)
AND ((el.sourceX > $x_min) OR (el.targetX > $x_min))
AND ((el.sourceY > $y_min) OR (el.targetY > $y_min))
AND ((el.sourceX < $x_max) OR (el.targetX < $x_max))
AND ((el.sourceY < $y_max) OR (el.targetY < $y_max))
/;
    foreach my $ar (@{$self->{'reaction_color'}}) {
	@{$ar->[1]} || next;
	my ($r,$g,$b) = @{$ar->[0]};
        my $color = $self->set_highlite_color($r,$g,$b);
        my $query = $basequery . "AND el.locatedEvent IN(" . join(',',@{$ar->[1]}) . ')';
#	print qq(<PRE>\n$query\n</PRE>\n);
        my ($sth,$res) = $self->dba->execute($query);
        my $aar = $sth->fetchall_arrayref;
        $self->draw_as_arrows($aar,$color);
        $self->create_usemap($aar);
    }
#    print qq(<PRE>), (caller(0))[3],"\t",  time() - $start, qq(</PRE>\n);
}

sub _draw_highlited_background_for_species {
    my ($self,$x_min,$y_min,$x_max,$y_max,$species) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my $start = time();
    my $event_sp_tbl = GKB::Utils::get_Event_species_table($self->dba);
    my $sp_db_id_str = join(',', map {$_->db_id} @{$species});
    my $basequery = qq{
SELECT (el.sourceX + el.targetX) / 2,(el.sourceY + el.targetY) / 2, SQRT(POWER(el.sourceX - el.targetX,2) + POWER(el.sourceY - el.targetY, 2))
FROM $coordinate_table el, $event_sp_tbl es
WHERE es.DB_ID = el.locatedEvent
AND es.species IN($sp_db_id_str)
AND ((el.sourceX > $x_min) OR (el.targetX > $x_min))
AND ((el.sourceY > $y_min) OR (el.targetY > $y_min))
AND ((el.sourceX < $x_max) OR (el.targetX < $x_max))
AND ((el.sourceY < $y_max) OR (el.targetY < $y_max))
};
    foreach my $ar (@{$self->{'reaction_color'}}) {
	@{$ar->[1]} || next;
	my ($r,$g,$b) = @{$ar->[0]};
	_tweak_colors_for_background($r,$g,$b);
        my $color = $self->set_highlite_color($r,$g,$b);
        my $query = $basequery . "AND el.locatedEvent IN(" . join(',',@{$ar->[1]}) . ')';
#	print qq(<PRE>\n$query\n</PRE>\n);
        my ($sth,$res) = $self->dba->execute($query);
        my $aar = $sth->fetchall_arrayref;
        $self->draw_as_circles($aar,$color);
    }
#    print qq(<PRE>), (caller(0))[3],"\t",  time() - $start, qq(</PRE>\n);
}

sub _tweak_colors_for_background {
    foreach my $c (@_) {
	$c += (255 - $c) * 0.9;
    }
}

sub draw_as_circles {
    my ($self,$aar,$color) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
#    my $start = time();
    my $image = $self->image;
    my $x_offset =  $self->x_offset;
    my $x_mag = $self->zoom * $self->x_magnification;
    my $y_offset =  $self->y_offset;
    my $y_mag = $self->zoom * $self->y_magnification;
    foreach my $ar (@{$aar}) {
	my $x = $ar->[0];
	my $y = $ar->[1];
	my $d = $ar->[2];
	$image->filledArc(($x - $x_offset) * $x_mag,
			  ($y - $y_offset) * $y_mag,
			  3 * $d * $x_mag,
			  3 * $d * $y_mag,
			  0,
			  360,
			  $color);
    }
#    print qq(<PRE>), time() - $start, qq(</PRE>\n);
    return $image;
}

sub _draw_highlites_for_species_via_orthology {
    my ($self,$x_min,$y_min,$x_max,$y_max,$species) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my $start = time();
    my $basequery = qq/
SELECT el.sourceX,el.sourceY,el.targetX,el.targetY,eoe.orthologousEvent,do._displayName
FROM $coordinate_table el, DatabaseObject do, Event_2_orthologousEvent eoe
WHERE el.locatedEvent = eoe.DB_ID
AND eoe.orthologousEvent = do.DB_ID
AND ((el.sourceX > $x_min) OR (el.targetX > $x_min))
AND ((el.sourceY > $y_min) OR (el.targetY > $y_min))
AND ((el.sourceX < $x_max) OR (el.targetX < $x_max))
AND ((el.sourceY < $y_max) OR (el.targetY < $y_max))
/;
    foreach my $ar (@{$self->{'reaction_color'}}) {
	@{$ar->[1]} || next;
	my ($r,$g,$b) = @{$ar->[0]};
        my $color = $self->set_highlite_color($r,$g,$b);
        my $query = $basequery . "AND eoe.orthologousEvent IN(" . join(',',@{$ar->[1]}) . ')';
#	print qq(<PRE>\n$query\n</PRE>\n);
        my ($sth,$res) = $self->dba->execute($query);
        my $aar = $sth->fetchall_arrayref;
        $self->draw_as_arrows($aar,$color);
        $self->create_usemap($aar);
    }
#    print qq(<PRE>), (caller(0))[3],"\t",  time() - $start, qq(</PRE>\n);
}

sub draw_onto_existing_image_clone {
    my ($self,$rgb,$reactions) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my $existing = $self->image || $self->throw("Need existing image.");
    my $copy = $existing->clone;
    return $copy unless (@{$reactions});
    $self->image($copy);
    my $color = $self->set_highlite_color(@{$rgb});
    my $query = qq/
SELECT el.sourceX,el.sourceY,el.targetX,el.targetY,el.locatedEvent,do._displayName
FROM $coordinate_table el, DatabaseObject do
WHERE do.DB_ID=el.locatedEvent
AND el.locatedEvent IN(/ . join(',',map {$_->db_id} @{$reactions}) . ')';
    my ($sth,$res) = $self->dba->execute($query);
    my $aar = $sth->fetchall_arrayref;
    $self->draw_as_arrows($aar,$color);
    $self->image($existing);
    return $copy;
}

sub draw_onto_existing_image {
    my ($self,$rgb,$reactions,$image) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    return $image unless (@{$reactions});
    my $tmp;
    if ($image) {
	$tmp = $self->image;
	$self->image($image);
    }
    $self->image || $self->throw("Need existing image.");
    my $color = $self->set_highlite_color(@{$rgb});
    my $query = qq/
SELECT el.sourceX,el.sourceY,el.targetX,el.targetY,el.locatedEvent,do._displayName
FROM $coordinate_table el, DatabaseObject do
WHERE do.DB_ID=el.locatedEvent
AND el.locatedEvent IN(/ . join(',',map {$_->db_id} @{$reactions}) . ')';
    my ($sth,$res) = $self->dba->execute($query);
    my $aar = $sth->fetchall_arrayref;
    $self->draw_as_arrows($aar,$color);
    if ($tmp) {
	$image = $self->image;
	$self->image($tmp);
    }
    return $image;
}

sub create_skypainter_image1 {
    my ($self,$rid2color) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my $tmp = join(',', keys %{$rid2color});
    my $query = qq/
SELECT el.sourceX,el.sourceY,el.targetX,el.targetY,el.locatedEvent,do._displayName
FROM $coordinate_table el, DatabaseObject do
WHERE do.DB_ID=el.locatedEvent
AND el.locatedEvent IN($tmp)
/;
    my ($sth,$res) = $self->dba->execute($query);
    my $aar = $sth->fetchall_arrayref;
    if (@{$aar}) {
	$self->coordinate_array($aar);
	$self->_draw_connecting_lines($aar);
	$self->draw_as_arrows2($aar,$rid2color);
    }
    return $self->image;
}

sub create_skypainter_image {
    my ($self,$rid2color,$sp_db_id) = @_;
    $sp_db_id || $self->throw("Need focu_ species DB_ID.");
    $self->debug && print "", (caller(0))[3], "\n";
    my $start = time();
    my $aar = [];
    my $event_sp_tbl = GKB::Utils::get_Event_species_table($self->dba);
    unless ($aar = $self->coordinate_array) {
	my $query = qq(
SELECT el.sourceX,el.sourceY,el.targetX,el.targetY,el.locatedEvent,do._displayName
FROM $coordinate_table el, DatabaseObject do, $event_sp_tbl es
WHERE do.DB_ID=el.locatedEvent
AND es.species=$sp_db_id
AND es.DB_ID=el.locatedEvent
);
	my ($sth,$res) = $self->dba->execute($query);
	$aar = $sth->fetchall_arrayref;
	$self->coordinate_array($aar);
	$self->_draw_connecting_lines($aar);
    }
    $self->draw_as_arrows2($aar,$rid2color);
    return $self->image;
}

sub draw_as_arrows2 {
    my ($self,$aar,$rid2rgb) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
#    my $start = time();
    my $image = $self->image;
    my $arrow_size = $self->arrow_size;
    my $arrow_lw_ratio = $self->arrow_lw_ratio;
    my $x_offset =  $self->x_offset;
    my $x_mag = $self->zoom * $self->x_magnification;
    my $y_offset =  $self->y_offset;
    my $y_mag = $self->zoom * $self->y_magnification;
    foreach my $ar (@{$aar}) {
	my $sx = $ar->[0];
	my $sy = $ar->[1];
	my $tx = $ar->[2];
	my $ty = $ar->[3];
	my $db_id = $ar->[4];
	my $displayName = $ar->[5];
	my $dx = $tx - $sx;
	my $dy = $ty - $sy;
	my $d = int((sort {$b <=> $a} (1, sqrt($dx * $dx + $dy * $dy)))[0]);
	my $ax = - ($arrow_size * $dx / $d);
	my $ay = - ($arrow_size * $dy / $d);
	my $color;
	if (my $rgb = $rid2rgb->{$db_id}) {
#	    print "<PRE>@{$rgb}</PRE>\n";
	    $color = $self->get_rgb_color(@{$rgb});
	} else {
	    $color = $self->get_default_color;
	}
	$image->line(($sx - $x_offset) * $x_mag,
		     ($sy - $y_offset) * $y_mag,
		     ($tx - $x_offset) * $x_mag,
		     ($ty - $y_offset) * $y_mag,
		     $color);
#	$image->line(($tx - $x_offset) * $x_mag,
#		     ($ty - $y_offset) * $y_mag,
#		     (($tx + $ax + $ay / $arrow_lw_ratio) - $x_offset) * $x_mag,
#		     (($ty + $ay - $ax / $arrow_lw_ratio) - $y_offset) * $y_mag,
#		     $color);
#	$image->line(($tx - $x_offset) * $x_mag,
#		     ($ty - $y_offset) * $y_mag,
#		     (($tx + $ax - $ay / $arrow_lw_ratio) - $x_offset) * $x_mag,
#		     (($ty + $ay + $ax / $arrow_lw_ratio) - $y_offset) * $y_mag,
#		     $color);
	# Closed arrow heads
	my $poly = new GD::Polygon;
	$poly->addPt(($tx - $x_offset) * $x_mag,($ty - $y_offset) * $y_mag);
	$poly->addPt((($tx + $ax + $ay / $arrow_lw_ratio) - $x_offset) * $x_mag,
		     (($ty + $ay - $ax / $arrow_lw_ratio) - $y_offset) * $y_mag);
	$poly->addPt((($tx + $ax - $ay / $arrow_lw_ratio) - $x_offset) * $x_mag,
		     (($ty + $ay + $ax / $arrow_lw_ratio) - $y_offset) * $y_mag);
	$image->filledPolygon($poly,$color);

    }
#    $self->default_view_created(1);
#    print qq(<PRE>), time() - $start, qq(</PRE>\n);
    return $image;
}

sub draw_as_polygons {
    my ($self,$aar,$rid2weight) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
#    my $start = time();
    my $image = $self->image;

    my $x_offset =  $self->x_offset;
    my $x_mag = $self->zoom * $self->x_magnification;
    my $y_offset =  $self->y_offset;
    my $y_mag = $self->zoom * $self->y_magnification;

    my $def_head_length = $self->arrowhead_length || 10;

    foreach my $ar (@{$aar}) {
	my $sx = $ar->[0];
	my $sy = $ar->[1];
	my $tx = $ar->[2];
	my $ty = $ar->[3];
	my $db_id = $ar->[4];
	my $displayName = $ar->[5];

	my $dx = $tx - $sx;
	my $dy = $ty - $sy;
	my $d = int((sort {$b <=> $a} (1, sqrt($dx * $dx + $dy * $dy)))[0]);
	my $dxd = -$dx/$d;
	my $dyd = -$dy/$d;

	my $head_length = ($def_head_length < $d) ? $def_head_length : $d;
	
	my $color;
	my $half_head_width;

	if (defined (my $weight = $rid2weight->{$db_id})) {
	    $color = $self->get_rgb_color(@{$self->highlite_color});
	    my $half_shaft_width = $weight * 10;
	    $half_head_width = ($half_shaft_width > 1) ? (1.5 * $half_shaft_width) : 1.5;

	    my $poly = new GD::Polygon;

	    $poly->addPt(($tx - $x_offset) * $x_mag,
			 ($ty - $y_offset) * $y_mag);

	    $poly->addPt(($tx + $head_length * $dxd - $half_head_width * $dyd - $x_offset) * $x_mag,
			 ($ty + $head_length * $dyd + $half_head_width * $dxd - $y_offset) * $y_mag);

	    $poly->addPt(($tx + $head_length * $dxd - $half_shaft_width * $dyd - $x_offset) * $x_mag,
			 ($ty + $head_length * $dyd + $half_shaft_width * $dxd - $y_offset) * $y_mag);

	    $poly->addPt(($sx - $half_shaft_width * $dyd - $x_offset) * $x_mag,
			 ($sy + $half_shaft_width * $dxd - $y_offset) * $y_mag);

	    $poly->addPt(($sx + $half_shaft_width * $dyd - $x_offset) * $x_mag,
			 ($sy - $half_shaft_width * $dxd - $y_offset) * $y_mag);

	    $poly->addPt(($tx + $head_length * $dxd + $half_shaft_width * $dyd - $x_offset) * $x_mag,
			 ($ty + $head_length * $dyd - $half_shaft_width * $dxd - $y_offset) * $y_mag);

	    $poly->addPt(($tx + $head_length * $dxd + $half_head_width * $dyd - $x_offset) * $x_mag,
			 ($ty + $head_length * $dyd - $half_head_width * $dxd - $y_offset) * $y_mag);
    
	    $image->polygon($poly,$color);
	} else {
	    $color = $self->get_default_color;
	    $half_head_width = 1.5;

	    $image->line(($tx - $x_offset) * $x_mag,
			 ($ty - $y_offset) * $y_mag,
			 ($sx - $x_offset) * $x_mag,
			 ($sy - $y_offset) * $y_mag,
			 $color);

	    $image->line(($tx - $x_offset) * $x_mag,
			 ($ty - $y_offset) * $y_mag,
			 ($tx + $head_length * $dxd - $half_head_width * $dyd - $x_offset) * $x_mag,
			 ($ty + $head_length * $dyd + $half_head_width * $dxd - $y_offset) * $y_mag,
			 $color);

	    $image->line(($tx - $x_offset) * $x_mag,
			 ($ty - $y_offset) * $y_mag,
			 ($tx + $head_length * $dxd + $half_head_width * $dyd - $x_offset) * $x_mag,
			 ($ty + $head_length * $dyd - $half_head_width * $dxd - $y_offset) * $y_mag,
			 $color);
	}
    }
    return $image;
}

sub reset_offset_if_necessary {
    my ($self,$ar) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my $cgi = $self->cgi;
    if ($cgi->param('ZOOM') && (! defined $cgi->param('REACTIONMAP_x')) && (! defined $cgi->param('REACTIONMAP_y'))) {
	$self->reset_offset_based_on_provided_reactions($ar);
    }
}

sub reset_offset_based_on_provided_reactions {
    my ($self,$ar) = @_;
    @{$ar} || return;
    $self->debug && print "", (caller(0))[3], "\n";
    my $tmp = ('?,') x scalar(@{$ar});
    chop($tmp);
    my @bindvalues = map {$_->db_id} @{$ar};
    my $query = qq{SELECT (AVG(sourceX) + AVG(targetX))/2, (AVG(sourceY) + AVG(targetY))/2 FROM ReactionCoordinates WHERE locatedEvent IN ($tmp)};
    my ($sth,$res) = $self->dba->execute($query, @bindvalues);
    my ($x,$y) = @{$sth->fetchrow_arrayref()};
    # This is probably a rather clunky way of dealing with the reactions from other species which
    # do not have coordinates directly.
    unless (defined $x && defined $y) {
	$query = qq{SELECT (AVG(rc.sourceX) + AVG(rc.targetX))/2, (AVG(rc.sourceY) + AVG(rc.targetY))/2 FROM ReactionCoordinates rc, Event_2_orthologousEvent eoe WHERE eoe.DB_ID=rc.locatedEvent AND eoe.orthologousEvent IN ($tmp)};
	($sth,$res) = $self->dba->execute($query, @bindvalues);
	($x,$y) = @{$sth->fetchrow_arrayref()};
	
    }
    unless (defined $x && defined $y) {
	return;
    }
    my $zoom = $self->zoom;
    my $x_mag = $self->x_magnification;
    my $y_mag = $self->y_magnification;
    my $x_offset = $self->x_offset;
    my $y_offset = $self->y_offset;
    my $half_max_x = $self->width / $x_mag / 2;
    my $half_max_y = $self->height / $y_mag / 2;
#    $x_offset = $x - $half_max_x + $half_max_x - $half_max_x / $zoom;
#    $y_offset = $y - $half_max_y + $half_max_y - $half_max_y / $zoom;
    $x_offset = $x - $half_max_x / $zoom;
    $y_offset = $y - $half_max_y / $zoom;
    my ($max_x,$max_y) = $self->find_max_xy2;
    if ($x_offset < 0) {
	$x_offset = 0;
    } elsif ($x_offset > ($max_x - 2 * $half_max_x / $zoom)) {
	$x_offset = $max_x - 2 * $half_max_x / $zoom;
    }
    if ($y_offset < 0) {
	$y_offset = 0;
    } elsif ($y_offset > ($max_y - 2 * $half_max_y / $zoom)) {
	$y_offset = $max_y - 2 * $half_max_y / $zoom;
    }
    $self->x_offset($x_offset);
    $self->y_offset($y_offset);
}

sub colour_reactions_by_evidence_type {
    my ($self,$reactions,$preload_necessary_values) = @_;
    my ($confirmed,$manually_inferred,$iea) = 
	GKB::Utils::split_reactions_by_evidence_type($reactions,$self->dba,$preload_necessary_values);
    $self->set_reaction_color(@{$CONFIRMED_REACTION_COLOR},$confirmed );
    $self->set_reaction_color(@{$MANUALLY_INFERRED_REACTION_COLOR},$manually_inferred);
    $self->set_reaction_color(@{$ELECTRONICALLY_INFERRED_REACTION_COLOR},$iea);
}

sub create_overlay_image {
    my $self = shift;
    my $im = GD::Image->new($self->width,$self->height);
    my $white = $im->colorAllocate(255,255,255);
    my $fg = $im->colorAllocate(0,0,0);
    $im->transparent($white);
    my $x_offset =  $self->x_offset;
    my $x_mag = $self->zoom * $self->x_magnification;
    my $y_offset =  $self->y_offset;
    my $y_mag = $self->zoom * $self->y_magnification;
    my $query = qq(
SELECT pc.minX,pc.minY,pc.maxX,pc.maxY,pc.locatedEvent,do._displayName
FROM PathwayCoordinates pc, DatabaseObject do
WHERE do.DB_ID=pc.locatedEvent
);
#    print STDERR $query;
    my ($sth,$res) = $self->dba->execute($query);
    while (my $ar = $sth->fetchrow_arrayref) {
	my $l = ($ar->[0] - $x_offset) * $x_mag;
	my $t = ($ar->[1] - $y_offset) * $y_mag;
	my $r = ($ar->[2] - $x_offset) * $x_mag;
	my $b = ($ar->[3] - $y_offset) * $y_mag;
#	print STDERR "$l,$t,$r,$b\t", $ar->[5], "\n";
#	$im->rectangle($l,$t,$r,$b,$fg);
	if (($r-$l) < ($b-$t)) {
	    $im->stringUp(gdSmallFont,$l,$b,$ar->[5],$fg);
	} else {
	    $im->string(gdSmallFont,$l,$t,$ar->[5],$fg);
	}
    }
    return $im;
}

sub create_overlay_div {
    my $self = shift;
    my $x_offset =  $self->x_offset;
    my $x_mag = $self->zoom * $self->x_magnification;
    my $y_offset =  $self->y_offset;
    my $y_mag = $self->zoom * $self->y_magnification;
    my $out;
    my $query = qq(
SELECT pc.minX,pc.minY,pc.maxX,pc.maxY,pc.locatedEvent,do._displayName
FROM PathwayCoordinates pc, DatabaseObject do
WHERE do.DB_ID=pc.locatedEvent
);
    my ($sth,$res) = $self->dba->execute($query);
    while (my $ar = $sth->fetchrow_arrayref) {
	my $l = ($ar->[0] - $x_offset) * $x_mag;
	my $t = ($ar->[1] - $y_offset) * $y_mag;
	my $r = ($ar->[2] - $x_offset) * $x_mag;
	my $b = ($ar->[3] - $y_offset) * $y_mag;
	my $w = $r - $l;
	my $h = $b - $t;
	$out .= qq(<DIV STYLE="position:absolute;left:${l}px;top:${t}px;width:${w}px;height:${h}px;text-align:center;display:table;"><DIV STYLE="_position:absolute;_top:50%;display:table-cell;vertical-align:middle;"><DIV STYLE="_position:relative;_top:-50%;font-weight:bold;font-size:9px;color:red;">) . $ar->[5] . qq(</DIV></DIV></DIV>\n);
#	$out .= qq(<DIV STYLE="position:absolute;left:${l}px;top:${t}px;width:${w}px;height:${h}px;text-align:center;display:table;border:thin dotted black;"><DIV STYLE="_position:absolute;_top:50%;display:table-cell;vertical-align:middle;"><DIV STYLE="_position:relative;_top:-50%;font-weight:bold;font-size:9px;color:red;">) . $ar->[5] . qq(</DIV></DIV></DIV>\n);
	
    }
    return $out;
}

1;
