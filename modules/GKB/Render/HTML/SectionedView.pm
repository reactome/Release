package GKB::Render::HTML::SectionedView;

use strict;
use Carp qw(cluck confess);
use CGI::Util 'rearrange';
use GKB::PrettyInstance;
use GKB::Config;
use GKB::DTreeMaker::EventHierarchy;
use GKB::ReactionMap;
use GKB::Graphics::ReactomeReaction;
use GKB::Dissect;
use GKB::Utils::HTML;

sub new {
    my($pkg, @args) = @_;
    my $self = bless {}, $pkg;
    my ($debug,$wu) = rearrange
	([qw(
	     DEBUG
	     WEBUTILS
	     )], @args);
    $wu && $self->webutils($wu);
    return $self;
}

sub webutils {
    my $self = shift;
    if (@_) {
	$self->{'webutils'} = shift;
    }
    return $self->{'webutils'};
}

sub _check_instance {
    my ($self,$instance) = @_;
    unless ($instance->isa("GKB::PrettyInstance")) {
	$instance = GKB::PrettyInstance->new(-INSTANCE => $instance,
					     -URLMAKER => $self->webutils->urlmaker,
					     -SUBCLASSIFY => 1,
					     -WEBUTILS => $self->webutils,
					    );
    }
}

sub render {
    my ($self,$instance) = @_;
    $self->_check_instance($instance);
    return
	$instance->page_title .
	$self->reactionmap_section($instance) .
	$self->eventhierarchy_section($instance) .
	$self->diagram_section($instance) .
	$self->details_section($instance) .
	$self->switches_section($instance);
}

sub reactionmap_section {
    my ($self,$instance) = @_;
    if (!$SHOW_REACTIONMAP_IN_EVENTBROWSER) {
    	return '';
    }
    return '' unless ($instance->can('create_image'));
    my $div_id = 'reactionmap_' . $instance->db_id;
    return
	qq(<DIV CLASS="section">) .
	GKB::Utils::HTML::section_visibility_toggle_button($div_id) .
	qq(<SPAN CLASS="sectionname">Reactionmap</SPAN>) .
	qq(<DIV ID="$div_id">\n) .
	$self->_reactionmap_html($instance) .
	qq(</DIV><!-- $div_id -->\n) .
	GKB::Utils::HTML::section_visibility_js($div_id,1) .
	qq(</DIV><!-- section -->\n);	
}

sub eventhierarchy_section {
    my ($self,$instance) = @_;
    if (!$SHOW_REACTIONMAP_IN_EVENTBROWSER) {
    	return '';
    }
    return '' unless ($instance->is_a('Event'));
    my $div_id = 'eventhierarchy_' . $instance->db_id;
    my $off_by_default = 0;
    return
	qq(<DIV CLASS="section">) .
	GKB::Utils::HTML::section_visibility_toggle_button($div_id) .
	qq(<SPAN CLASS="sectionname">Event hierarchy</SPAN>) .
	qq(<DIV ID="$div_id" CLASS="eventhierarchy">\n) .
	qq(<script language="javascript" src="/javascript/dtree/dtree.js"></script>\n) .
	qq(<link href="/javascript/dtree/dtree.css" rel="stylesheet" type="text/css">\n) .
	$self->eventhierarchy($instance) . 
	qq(</DIV><!-- $div_id -->\n) .
	GKB::Utils::HTML::section_visibility_js($div_id,$off_by_default) .
	qq(</DIV><!-- section -->\n);
}

sub eventhierarchy {
    my ($self,$instance,$use_hierarchy_type) = @_;
    return '' unless ($instance->is_a('Event'));
    my $eh = GKB::DTreeMaker::EventHierarchy->new(
	-WEBUTILS => $self->webutils, 
	-USE_HIERARCHY_TYPE => $use_hierarchy_type
	);
    return $eh->create_tree($instance);
}

sub diagram_section1 {
    my ($self,$instance) = @_;
    return '' unless ($instance->is_a('Reaction'));
    my $db_id = $instance->db_id;
    my $div_id = 'diagram_' . $db_id;
    return
	qq(<DIV CLASS="section">) .
	GKB::Utils::HTML::section_visibility_toggle_button($div_id) .
	qq(<SPAN CLASS="sectionname">Diagram</SPAN>) .
	qq(<DIV ID="$div_id" CLASS="diagram" ALIGN="center">\n) .
	qq(<IMG SRC="/cgi-bin/image4reaction?DB=) . $self->webutils->cgi->param('DB') . qq(&ID=$db_id">) .
	qq(</DIV><!-- $div_id -->\n) .
	GKB::Utils::HTML::section_visibility_js($div_id) .
	qq(</DIV><!-- section -->\n);
}

sub diagram_section {
    my ($self,$instance) = @_;
    if ($instance->is_a('Reaction') || $instance->is_a('ReactionlikeEvent')) {
	return $self->reaction_diagram_section($instance);
    } elsif ($instance->is_a('Pathway')) {
	return $self->pathway_diagram_section($instance);
    } elsif ($instance->is_a('Complex') || ($instance->is_a('EntitySet') && $instance->HasMember->[0])) {
	return $self->entity_diagram_section($instance);
    }
    return '';
}

sub reaction_diagram_section {
    my ($self,$instance) = @_;
    return '' unless ($instance->is_a('Reaction') || $instance->is_a('ReactionlikeEvent'));
    return '' unless ($instance->Input->[0] || $instance->Output->[0] || $instance->CatalystActivity->[0]);
    my $db_id = $instance->db_id;
    my $div_id = 'diagram_' . $db_id;
    my $gr = GKB::Graphics::ReactomeReaction->new($instance);
    my ($fh,$path) = $self->webutils->get_tmp_img_file;
    print $fh $gr->draw->png;
    close $fh;
    my $usemap = $gr->create_usemap($self->webutils->urlmaker);
    my $mapname = 'dmap_' . $instance->db_id;
    return
	qq(<DIV CLASS="section">) .
	GKB::Utils::HTML::section_visibility_toggle_button($div_id) .
	qq(<SPAN CLASS="sectionname">Diagram</SPAN>) .
	qq(<DIV ID="$div_id" CLASS="diagram" ALIGN="center">\n) .
	qq(<MAP NAME="$mapname">$usemap</MAP>) .
	qq(<IMG SRC="$path" USEMAP="#$mapname" BORDER="0">) .
	qq(</DIV><!-- $div_id -->\n) .
	GKB::Utils::HTML::section_visibility_js($div_id) .
	qq(</DIV><!-- section -->\n);
}

sub pathway_diagram_section {
    my ($self,$instance) = @_;
    return '';
    return '' unless ($instance->is_a('Pathway'));    
    return '' unless ($instance->HasComponent->[0]);
    my $db_id = $instance->db_id;
    my $div_id = 'diagram_' . $db_id;
    my $db = $self->webutils->cgi->param('DB');
    return
#	qq(
#<script type="text/javascript" language="JavaScript" src="/javascript/svg/svgcheck.js"></script>
#<script type="text/vbscript" language="VBScript" src="/javascript/svg/svgcheck.vbs"></script>
#<script type="text/javascript" language="JavaScript">checkAndGetSVGViewer();</script>\n) .
	qq(<DIV CLASS="section">) .
	GKB::Utils::HTML::section_visibility_toggle_button($div_id) .
	qq(<SPAN CLASS="sectionname">Diagram</SPAN>) .
	qq(<DIV ID="$div_id" CLASS="diagram" ALIGN="center">\n) .
	qq(<embed src="/cgi-bin/svgexporter?ID=$ {db_id}&DB=$ {db}" style="width:0px;height:0px;" type="image/svg+xml">\n) .
	qq(</DIV><!-- $div_id -->\n) .
	GKB::Utils::HTML::section_visibility_js($div_id) .
	qq(</DIV><!-- section -->\n) .
	qq(
<script language="javascript">
function setEmbedSize() {
  var a = document.embeds;   
  var w,h;
  if (window.innerWidth) {
    // non-IE
    w = window.innerWidth - 20;
    h = window.innerHeight - 200;
  } else {
    // IE
    var w = document.body.clientWidth - 20;
    var h = document.body.clientHeight - 200;
  }
  for (var i = 0; i < a.length; i++) {
    a[i].style.width = w + "px";
    a[i].style.height = h + "px";
//    alert(a[i].style.width + " <-> " + a[i].style.height + " w = " + w + " h = " + h);
  }
}  

window.onresize = setEmbedSize;
window.onload = setEmbedSize;
</script>\n);
}

sub entity_diagram_section {
    my ($self,$instance) = @_;
    my $db_id = $instance->db_id;
    my $div_id = 'diagram_' . $db_id;
    # Having to pass DB_ID to GKB::Dissect and having it to re-fetch the instance is not optimal
    my $gr = GKB::Dissect->new([$instance->db_id],$instance->dba);
    my ($gd,$usemap) = $gr->plot_One_Entity;
    my ($fh,$path) = $self->webutils->get_tmp_img_file;
    print $fh $gd->png;
    close $fh;
    my $mapname = 'dmap_' . $instance->db_id;
    my $keydiv_id = 'diagramkey_' . $db_id;
    return
	qq(<DIV CLASS="section">) .
	GKB::Utils::HTML::section_visibility_toggle_button($div_id) .
	qq(<SPAN CLASS="sectionname">Diagram</SPAN>) .
	qq(<DIV ID="$div_id" CLASS="diagram" ALIGN="center">\n) .
	qq(<MAP NAME="$mapname">$usemap</MAP>) .
	qq(<IMG SRC="$path" USEMAP="#$mapname" BORDER="0">) .
	qq(<BR />\n) .
	qq(<DIV ALIGN="left" style="padding-left:3px;">) .
	GKB::Utils::HTML::section_visibility_toggle_button($keydiv_id) .
	qq(<SPAN CLASS="sectionname">Diagram key</SPAN>) .
	qq(<DIV ID="$keydiv_id" ALIGN="center">\n) .
	qq(<IMG SRC="/icons/dissect_key.png" BORDER="0">) .
	qq(</DIV><!-- $keydiv_id -->\n) .
	GKB::Utils::HTML::section_visibility_js($keydiv_id,1) .
	qq(</DIV>) .

	qq(</DIV><!-- $div_id -->\n) .
	GKB::Utils::HTML::section_visibility_js($div_id) .
	qq(</DIV><!-- section -->\n);
}

sub details_section {
    my ($self,$instance) = @_;
    my $div_id = 'details_' . $instance->db_id;
    return
	qq(<DIV CLASS="section">) .
	GKB::Utils::HTML::section_visibility_toggle_button($div_id) .
	qq(<SPAN CLASS="sectionname">Details</SPAN>) .
	qq(<DIV ID="$div_id">\n) .
#	qq(<TABLE cellspacing="0" WIDTH="$HTML_PAGE_WIDTH" BORDER="0" CLASS=") . $instance->class . qq(">\n) .
#	$instance->html_table_rows .
#	qq(</TABLE>\n) .
	$self->details($instance) .
	qq(</DIV><!-- $div_id -->\n) .
	GKB::Utils::HTML::section_visibility_js($div_id) .
	qq(</DIV><!-- section -->\n);
}

sub details {
    my ($self,$instance) = @_;
    return
	qq(<TABLE cellspacing="0" WIDTH="$HTML_PAGE_WIDTH" BORDER="0" CLASS=") . $instance->class . qq(">\n) .
	$instance->html_table_rows .
	qq(</TABLE>\n);
}

# Split the table into muliple tables if there's a row spanning multiple columns. Otherwise IE7 will not use intended
# column width if the text in the spanning table cell is "too long".
sub few_details {
    my ($self,$instance) = @_;
    my $hstr = qq(<TABLE cellspacing="0" WIDTH="$HTML_PAGE_WIDTH" BORDER="0" CLASS=") . $instance->class . qq(">\n);
    my $out = $hstr . $instance->few_details . qq(</TABLE>\n);
    $out =~ s/(<tr><t[dh] (?:class="\w+" )?colspan.+?<\/t[dh]><\/tr>)/<\/TABLE>$ {hstr}${1}<\/TABLE>$ {hstr}/gmsi;
    return $out;
}

sub switches_section {
    my ($self,$instance) = @_;
    
    return
	qq(<DIV CLASS="section">) .
	qq(<DIV CLASS="switches">\n) .
	qq(<TABLE WIDTH="100%">\n) .
	$instance->_view_switch_html .
	qq(</TABLE>\n) .
	qq(</DIV><!-- switches -->\n) .
    qq(</DIV><!-- section -->\n) .
    $instance->_view_stable_link;
}

sub section_visibility_toggle_button {
    return GKB::Utils::HTML::section_visibility_toggle_button(@_);
    my ($div_id) = @_;
    my $img_id = 'toggle_' . $div_id;
    return qq(<A ONMOUSEOVER="ddrivetip('Show/hide this section','#DCDCDC')" ONMOUSEOUT="hideddrivetip()" ONCLICK="javascript:changeSectionVisibility('$div_id');"><IMG ID="$img_id" SRC="/icons/plus-box.gif"></A>);
}

sub section_visibility_js {
    cluck("Deprecated method ". (caller(0))[3] . ". Use the method from GKB::Utils::HTML.\n");
    return GKB::Utils::HTML::section_visibility_js(@_);
}

sub _reactionmap_html {
    my ($self,$instance) = @_;
    my $db_id = $instance->db_id;
    my $mapwidth = $REACTIONMAP_WIDTH + 3;
    my $cgi = $self->webutils->cgi;
    my $form_name = 'reactionmapform_' . $db_id;
    my $rm = $instance->create_image || return '';
    my $start = time();
    my $basename = rand() * 1000 . $$;
    my $name = $basename . '.png';
  
    #store PNG-image
    open(OUT, ">$GK_TMP_IMG_DIR/$name") || confess("Can't create '$GK_TMP_IMG_DIR/$name': $!");
    binmode OUT;
    print OUT $rm->image->png;
    close OUT;
    
    my $um_path = "$GK_TMP_IMG_DIR/$basename.map";
    open(OUT, ">$um_path")  || confess("Can't create '$um_path': $!");
    print OUT $rm->usemap;
    close OUT;

    my $x_mag = $rm->x_magnification * $rm->zoom;
    my $y_mag = $rm->y_magnification * $rm->zoom;
    my $x_offset = $rm->x_offset;
    my $y_offset = $rm->y_offset;
    $cgi->param("X_MAG", $x_mag);
    $cgi->param("Y_MAG", $y_mag);
    $cgi->param("X_OFFSET", $x_offset);
    $cgi->param("Y_OFFSET", $y_offset);
    $cgi->param("FOCUS", $db_id);
    $rm->highlites_as_hidden_param;
    my ($width,$height) = $rm->image->getBounds();
#    print qq(<PRE>), (caller(0))[3],"\t",  time() - $start, qq(</PRE>\n);
    my $key_str = '';
    if (my $tmp = $instance->reactionmap_key) {	
	$key_str = qq(<TR><TD COLSPAN="11" ALIGN="center">$tmp</TD></TR>\n);
    }
    my $reaction_map = $self->_reactionmap_js($instance);
    if (!(defined $reaction_map)) {
	$reaction_map = '';
    }
    return
	$reaction_map .
	qq(<TABLE cellspacing="0" ALIGN="center" CLASS="reactionmap" WIDTH="$mapwidth">) .
	$cgi->start_form(-method =>'POST',-name =>$form_name,-action => '/cgi-bin/eventbrowser') .
	$cgi->hidden(-name => 'DB',-value => $cgi->param('DB')) .
	qq(<INPUT TYPE="hidden" NAME="X_MAG" VALUE=\") . $cgi->param("X_MAG") . qq(\" />) .
	qq(<INPUT TYPE="hidden" NAME="Y_MAG" VALUE=\") . $cgi->param("Y_MAG") . qq(\" />) .
	qq(<INPUT TYPE="hidden" NAME="X_OFFSET" VALUE=\") . $cgi->param("X_OFFSET") . qq(\" />) .
	qq(<INPUT TYPE="hidden" NAME="Y_OFFSET" VALUE=\") . $cgi->param("Y_OFFSET") . qq(\" />) .
	qq(<INPUT TYPE="hidden" NAME="ID" VALUE="" />) .
	qq(<INPUT TYPE="hidden" NAME="REACTIONMAP_x" VALUE="" />) .
	qq(<INPUT TYPE="hidden" NAME="REACTIONMAP_y" VALUE="" />) .
        qq(<INPUT TYPE="hidden" NAME="reactionmap" VALUE="" />) .
        qq(<INPUT TYPE="hidden" NAME="eventhierarchy" VALUE="" />) .
        qq(<INPUT TYPE="hidden" NAME="description" VALUE="" />) .
	qq(<INPUT TYPE="hidden" NAME="FOCUS" VALUE=\") . $db_id . qq(\" />) .
	qq(<INPUT TYPE="hidden" NAME="FOCUS_SPECIES" VALUE=\") . $cgi->param('FOCUS_SPECIES') . qq(\" />) .
	$rm->highlites_as_hidden_param .
	qq(<TR CLASS="reactionmap">) .
	qq(<TD NOWRAP CLASS="reactionmap">Click on the map:</TD>) .
	qq(<TD CLASS="reactionmap" ALIGN="right"><INPUT CLASS="reactionmap" TYPE="radio" NAME="ZOOM" VALUE="1" CHECKED /></TD>) .
	qq(<TD CLASS="reactionmap" ALIGN="left">shifts focus</TD>) .
	qq(<TD CLASS="reactionmap" ALIGN="right"><INPUT CLASS="reactionmap" TYPE="radio" NAME="ZOOM" VALUE="2" /></TD>) .
	qq(<TD CLASS="reactionmap" ALIGN="left">zooms in</TD>) .
	qq(<TD CLASS="reactionmap" ALIGN="right"><INPUT CLASS="reactionmap" TYPE="radio" NAME="ZOOM" VALUE="0.5" /></TD>) .
	qq(<TD CLASS="reactionmap" ALIGN="left">zooms out</TD>) .
	qq(<TD CLASS="reactionmap"><INPUT CLASS="reactionmap" TYPE="image" NAME="MOVE_L" VALUE="-0.25" SRC="/icons/left.png" ALT="move left" HEIGHT="10" WIDTH="10" /></TD>) .
	qq(<TD CLASS="reactionmap"><INPUT CLASS="reactionmap" TYPE="image" NAME="MOVE_R" VALUE="0.25" SRC="/icons/right.png" ALT="move right" HEIGHT="10" WIDTH="10" /></TD>) .
	qq(<TD CLASS="reactionmap"><INPUT CLASS="reactionmap" TYPE="image" NAME="MOVE_U" VALUE="-0.25" SRC="/icons/up.png" ALT="move up" HEIGHT="10" WIDTH="10" /></TD>) .
	qq(<TD CLASS="reactionmap"><INPUT CLASS="reactionmap" TYPE="image" NAME="MOVE_D" VALUE="0.25" SRC="/icons/down.png" ALT="move down" HEIGHT="10" WIDTH="10" /></TD>) . 
	qq(</TR>) .
	qq(<TR><TD COLSPAN="11" ALIGN="center">) .
#	qq(<MAP NAME=img_map>) . $rm->usemap . qq(</MAP>) .
#	qq(<img USEMAP=\#img_map BORDER="0" SRC="/img-tmp/$name">) .
	qq(<IMG ID="img_$basename" BORDER="0" SRC="/img-tmp/$name">) .
	qq(</TD></TR>) .
        $cgi->end_form .
	$key_str .
	qq(</TABLE>\n) .
	qq(<script language="JavaScript">loadUseMapForImgId('img_$basename');</script>);
}

sub _reactionmap_w_svg_links_html {
    my ($self,$instance) = @_;
    my $db_id = $instance->db_id;
    my $mapwidth = $REACTIONMAP_WIDTH + 3;
    my $cgi = $self->webutils->cgi;
    my $form_name = 'reactionmapform_' . $db_id;
    my $rm = $instance->create_image || return '';
    #svg-format
    my $format = "1";
    my $start = time();
    my $basename = rand() * 1000 . $$;
    my $name = $basename . '.png';
    my $svgname = $basename . '.svg';
  
    #store PNG-image
    open(OUT, ">$GK_TMP_IMG_DIR/$name") || confess("Can't create '$GK_TMP_IMG_DIR/$name': $!");
    binmode OUT;
    print OUT $rm->image->png;
    close OUT;
    
    #store SVG-image
    open(OUT, ">$GK_TMP_IMG_DIR/$svgname") || confess("Can't create '$GK_TMP_IMG_DIR/$svgname': $!");
    binmode OUT;
    close OUT;

    my $x_mag = $rm->x_magnification * $rm->zoom;
    my $y_mag = $rm->y_magnification * $rm->zoom;
    my $x_offset = $rm->x_offset;
    my $y_offset = $rm->y_offset;
    $cgi->param("X_MAG", $x_mag);
    $cgi->param("Y_MAG", $y_mag);
    $cgi->param("X_OFFSET", $x_offset);
    $cgi->param("Y_OFFSET", $y_offset);
    $cgi->param("FOCUS", $db_id);
    $rm->highlites_as_hidden_param;
    my ($width,$height) = $rm->image->getBounds();
#    print qq(<PRE>), (caller(0))[3],"\t",  time() - $start, qq(</PRE>\n);
    my $key_str = '';
    if (my $tmp = $instance->reactionmap_key) {	
	$key_str = qq(<TR><TD COLSPAN="16" ALIGN="center">$tmp</TD></TR>\n);
    }
    return
	$self->_reactionmap_js($instance) .
	qq(<TABLE cellspacing="0" ALIGN="center" CLASS="reactionmap" WIDTH="$mapwidth">) .
#	qq(<TABLE cellspacing="0" ALIGN="center" CLASS="reactionmap" WIDTH="100%">) .
	$cgi->start_form(-method =>'POST',-name =>$form_name,-action => '/cgi-bin/eventbrowser') .
	$cgi->hidden(-name => 'DB',-value => $cgi->param('DB')) .
	qq(<INPUT TYPE="hidden" NAME="X_MAG" VALUE=\") . $cgi->param("X_MAG") . qq(\" />) .
	qq(<INPUT TYPE="hidden" NAME="Y_MAG" VALUE=\") . $cgi->param("Y_MAG") . qq(\" />) .
	qq(<INPUT TYPE="hidden" NAME="X_OFFSET" VALUE=\") . $cgi->param("X_OFFSET") . qq(\" />) .
	qq(<INPUT TYPE="hidden" NAME="Y_OFFSET" VALUE=\") . $cgi->param("Y_OFFSET") . qq(\" />) .
	qq(<INPUT TYPE="hidden" NAME="ID" VALUE="" />) .
	qq(<INPUT TYPE="hidden" NAME="REACTIONMAP_x" VALUE="" />) .
	qq(<INPUT TYPE="hidden" NAME="REACTIONMAP_y" VALUE="" />) .
        qq(<INPUT TYPE="hidden" NAME="reactionmap" VALUE="" />) .
        qq(<INPUT TYPE="hidden" NAME="eventhierarchy" VALUE="" />) .
        qq(<INPUT TYPE="hidden" NAME="description" VALUE="" />) .
	qq(<INPUT TYPE="hidden" NAME="FOCUS" VALUE=\") . $db_id . qq(\" />) .
	qq(<INPUT TYPE="hidden" NAME="FOCUS_SPECIES" VALUE=\") . $cgi->param('FOCUS_SPECIES') . qq(\" />) .
	$rm->highlites_as_hidden_param .
	qq(<TR CLASS="reactionmap">) .
	qq(<TD NOWRAP CLASS="reactionmap">Click on the map:</TD>) .
	qq(<TD CLASS="reactionmap" ALIGN="right"><INPUT CLASS="reactionmap" TYPE="radio" NAME="ZOOM" VALUE="1" CHECKED /></TD>) .
	qq(<TD CLASS="reactionmap" ALIGN="left">shifts focus</TD>) .
	qq(<TD CLASS="reactionmap" ALIGN="right"><INPUT CLASS="reactionmap" TYPE="radio" NAME="ZOOM" VALUE="2" /></TD>) .
	qq(<TD CLASS="reactionmap" ALIGN="left">zooms in</TD>) .
	qq(<TD CLASS="reactionmap" ALIGN="right"><INPUT CLASS="reactionmap" TYPE="radio" NAME="ZOOM" VALUE="0.5" /></TD>) .
	qq(<TD CLASS="reactionmap" ALIGN="left">zooms out</TD>) .
	qq(<TD CLASS="reactionmap"><INPUT CLASS="reactionmap" TYPE="image" NAME="MOVE_L" VALUE="-0.25" SRC="/icons/left.png" ALT="move left" HEIGHT="10" WIDTH="10" /></TD>) .
	qq(<TD CLASS="reactionmap"><INPUT CLASS="reactionmap" TYPE="image" NAME="MOVE_R" VALUE="0.25" SRC="/icons/right.png" ALT="move right" HEIGHT="10" WIDTH="10" /></TD>) .
	qq(<TD CLASS="reactionmap"><INPUT CLASS="reactionmap" TYPE="image" NAME="MOVE_U" VALUE="-0.25" SRC="/icons/up.png" ALT="move up" HEIGHT="10" WIDTH="10" /></TD>) .
	qq(<TD CLASS="reactionmap"><INPUT CLASS="reactionmap" TYPE="image" NAME="MOVE_D" VALUE="0.25" SRC="/icons/down.png" ALT="move down" HEIGHT="10" WIDTH="10" /></TD>) . 
	#pdf-part
	qq(<TD CLASS="reactionmap" ALIGN="right">) . $self->_call_pdf($svgname) . 
	qq(<A onMouseover="ddrivetip('View rectionmap in SVG format.','#DCDCDC', 250)" onMouseout="hideddrivetip()" HREF="/img-tmp/$svgname" TYPE="image/svg+xml" TARGET="_blank">[SVG]</A></TD>) .
	qq(</TR>) .
	qq(<TR><TD COLSPAN="12" ALIGN="center">) .
	qq(<MAP NAME=img_map>) . $rm->usemap . qq(</MAP>) .
	qq(<img USEMAP=\#img_map BORDER="0" SRC="/img-tmp/$name">) .
	qq(</TD></TR>) .
        $cgi->end_form .
	$key_str .
	qq(</TABLE>\n);
}

sub _call_pdf{
    my ($self,$svgname)=@_;
    my $reactionmap_pdfurl = "reactionmap_pdfexporter";
    $reactionmap_pdfurl .= '?SVG=' . $svgname;
    
    my $tmp= qq(<A onMouseover="ddrivetip('View/download reactionmap in PDF format.','#DCDCDC', 250)" onMouseout="hideddrivetip()" HREF="$reactionmap_pdfurl" CLASS="reactionmap">[PDF]</A>);
    
    return $tmp;
}


sub _reactionmap_js {
    my ($self,$instance) = @_;
    my $db = $self->webutils->cgi->param('DB');
    return <<__END__;
<script type="text/javascript" src="/javascript/yui/build/yahoo/yahoo-min.js"></script>
<script type="text/javascript" src="/javascript/yui/build/dom/dom-min.js"></script>
<script type="text/javascript" src="/javascript/yui/build/event/event-min.js"></script>
<script type="text/javascript" src="/javascript/yui/build/connection/connection-min.js"></script>

<script language="JavaScript">

var previousNodeClass;
var currentInstanceId;
var db = "$db";
var dtree;

function handleClick2(form_id,id,e) {
    var x;
    var y;
//    var tmp;
    if (!e) var e = window.event;
    if (e.pageX || e.pageY) {
	x = e.pageX - (window.innerWidth - $REACTIONMAP_WIDTH) / 2 + 10;
	y = e.pageY - 100;
//        tmp = "pageXY";
    }
    else if (e.clientX || e.clientY) {
	x = e.clientX + document.body.scrollLeft - (document.body.offsetWidth - $REACTIONMAP_WIDTH) / 2 + 10;
	y = e.clientY + document.body.scrollTop - 100;
//        tmp = "clientXY";
    }
    //Have to use eval to keep IE on OS X happy.
    eval("document.reactionmapform_" + form_id + ".REACTIONMAP_x.value = " + x);
    eval("document.reactionmapform_" + form_id + ".REACTIONMAP_y.value = " + y);
    if (id) {
	eval("document.reactionmapform_" + form_id + ".ID.value = " + id);
    }
//    window.alert(tmp + ":" + x + "," + y);
    eval("document.reactionmapform_" + form_id + ".submit()");
    return false;
}

function handleMouseOver(instance_id,tip_label,tip_bg,tip_width,focus_instance_id) {
    ddrivetip(tip_label,tip_bg,tip_width);
    currentInstanceId = instance_id;
    // have top check if there is eventhierarchy (with dtree).
    if (document.getElementById('eventhierarchy_' + focus_instance_id)) {
        dtree = eval('d_' + focus_instance_id);
        if (dtree) {
            previousNodeClass = dtree.changeNodeClass(instance_id,'nodeSel');
        }
    }
}

function handleMouseOut() {
    hideddrivetip();
    if (dtree) {
        dtree.changeNodeClass(currentInstanceId,previousNodeClass);
        previousNodeClass = null;
	dtree = null;
    }
}

function loadUseMapForImgId (imgId) {
	var img = YAHOO.util.Dom.get(imgId);
	var mapsrc = img.src.replace(/png/, "map");
	var callback = {
		scope: this,
		customevents:{
			onSuccess: function(eventType, args) {
				var mapEl = document.createElement('map');
				var mapName = imgId + "_map"
				mapEl.setAttribute("name", mapName);
				img.parentNode.appendChild(mapEl);
				mapEl.innerHTML = args[0].responseText;
				img.useMap = "#" + mapName;
				}, 
			onFailure: function(eventType, args) {
				//alert("Failed to load " + mapsrc + ": " + args[0].statusText);
			}
		}
	};
	YAHOO.util.Connect.asyncRequest('GET', mapsrc, callback);
}
</script>
__END__
}

sub collapsable_section {
    carp("Deprecated method ". (caller(0))[3] . ". Use the method from GKB::Utils::HTML.\n");
    return GKB::Utils::HTML::collapsable_section(@_);
}

sub collapsable_section_start {
    carp("Deprecated method ". (caller(0))[3] . ". Use the method from GKB::Utils::HTML.\n");
    return GKB::Utils::HTML::collapsable_section_start(@_);
}

sub collapsable_section_end {
    carp("Deprecated method ". (caller(0))[3] . ". Use the method from GKB::Utils::HTML.\n");
    return GKB::Utils::HTML::collapsable_section_end(@_);
}

1;
