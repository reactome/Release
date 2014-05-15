package GKB::Render::HTML::FrontPage;

use strict;
use vars qw($TABLE_COL_NUM @ISA @EXPORT);
use CGI::Util qw(rearrange);
use Carp qw(cluck confess);
use GKB::Config;
use GKB::ReactionMap;

$TABLE_COL_NUM = 4;

sub new {
    my($pkg, @args) = @_;
    my $self = bless {}, $pkg;
    my ($wu) = rearrange
	([qw(
             WU
	     )], @args);
    $wu || confess("Need WeUtils object");
    $self->webutils($wu);
    return $self;
}

sub webutils {
    $_[1] and $_[0]->{'webutils'} = $_[1];
    return $_[0]->{'webutils'};
}

sub print_species_page {
    my ($self, $classic) = @_;
    
    $self->print_page_top($classic);
    $self->print_focus_species_selector();
    print qq(<DIV ID="reactionmap_and_table">\n);
    $self->create_content_for_species_if_necessary_and_print;
    print qq(</DIV><!-- reactionmap_and_table -->\n);
    print_advert_section();
    $self->print_static_content;
    $self->print_page_bottom;
}

sub print_reactionmap_form {
    my $self = shift;
    my $cgi = $self->webutils->cgi;
    print
	$cgi->start_form(-method =>'GET',-name =>"reactionmap",-action=>'/cgi-bin/eventbrowser', -style=>'display:none;') .
	$cgi->hidden(-name => 'DB',-value => $cgi->param('DB')) .
	$cgi->hidden(-name => 'ZOOM', -value => 2) .
	$cgi->hidden(-name => 'ID', -value => '') .
#	qq(<INPUT TYPE="hidden" NAME="ZOOM" VALUE="2" />) .
#	qq(<INPUT TYPE="hidden" NAME="ID" VALUE="" />) .
        $cgi->end_form;
}

sub print_page_top {
    my ($self, $classic) = @_;
    
    my $cgi = $self->webutils->cgi;
    my @meta_tags = $self->webutils->meta_tag_builder($GLOBAL_META_TAGS);
    if (defined $classic) {
    	my $classic_view_cookie;
    	if ($classic =~ /1/) {
			$classic_view_cookie = $cgi->cookie(-name => 'ClassicView', -value => '1');
    	} else {
			$classic_view_cookie = $cgi->cookie(-name => 'ClassicView', -value => '0', -expires => '-1h');
    	}
    	print $cgi->header(-charset => 'UTF-8', -cookie=>$classic_view_cookie);
    } else {
    	print $cgi->header(-charset => 'UTF-8');
    }
    print $cgi->start_html(
	-style => {-src => '/stylesheet.css'},
	-title => "$PROJECT_NAME",
#	-onLoad => image_preload_instructions()
	-head => [
			  $cgi->Link({-href => "/xml/rss.xml", -rel => "alternate", -type => "application/rss+xml"}),
			  @meta_tags
			 ]
	);
    print image_swapping_javascript();
    if ($USE_REACTOME_GWT) {
    	my $db = $cgi->param('DB');
    	my $db_string = "";
    	if (defined $db && !($db eq "")) {
    		$db_string = "&DB=$db";
    	}
		print "<DIV STYLE=\"text-align:center;padding-bottom:10px;color:red;\"><b>The reactionmap for the Sky is no longer maintained and is out of date. Click <a href=\"/cgi-bin/frontpage?CLASSIC=0$db_string\">here</a> to go to our new home page.</b></DIV>";
	}
    print $self->webutils->navigation_bar;
    $self->webutils->print_simple_query_form;
    print qq(<TABLE ALIGN="center" WIDTH="$HTML_PAGE_WIDTH" CLASS="frontpagetitle"><TR><TD CLASS="frontpagetitle">$PROJECT_TITLE</TR></TABLE>\n);
}

sub print_page_bottom {
    my $self = shift;
    my $cgi = $self->webutils->cgi;
    print $self->webutils->make_footer;
    print $cgi->end_html;
}

sub print_static_content {
	if (-e "$GK_ROOT_DIR/website/html/about.html" || -e "$GK_ROOT_DIR/website/html/news.html") {
		my $width = "46%";
		if (!(-e "$GK_ROOT_DIR/website/html/about.html") || !(-e "$GK_ROOT_DIR/website/html/news.html")) {
			$width = "100%";
		}
		print "<TABLE CLASS=\"newsandnotes\" BORDER=\"0\" CELLPADDING=\"0\" CELLSPACING=\"0\" WIDTH=\"$HTML_PAGE_WIDTH\">\n";
		print "  <TR>\n";
		if (-e "$GK_ROOT_DIR/website/html/about.html") {
			print "    <TH CLASS=\"newsandnotes\" WIDTH=\"$width\">About <B>$PROJECT_NAME</B></TH>\n";
			print "    <TH CLASS=\"spacer\" WIDTH=\"2\">&nbsp;</TH>\n";
		}
		if (-e "$GK_ROOT_DIR/website/html/news.html") {
			print "    <TH CLASS=\"newsandnotes\" WIDTH=\"$width\">News and Notes</TH>\n";
		}
		print "  </TR>\n";
		print "  <TR>\n";
	
		if (-e "$GK_ROOT_DIR/website/html/about.html") {
		    print "<TD CLASS=\"about\">\n";
		    print "<IMG SRC=\"$PROJECT_LOGO_URL\" ALIGN=\"left\" HEIGHT=\"80\" WIDTH=\"80\">\n";
		    eval {
			print_about_section();
		    }; $@ && handle_error($@);
		    print "</TD>\n";
	    	print "<TD></TD>\n";
		}

		if (-e "$GK_ROOT_DIR/website/html/news.html") {
		    print "<TD CLASS=\"newsandnotes\">\n";
		    eval {
				print_news_section($NEWS_FILE,2,0);
		    }; $@ && handle_error($@);
			print "    </TD>\n";
		}
		
		print "  </TR>\n";
		print "</TABLE>\n";
	}

	if (defined $PROJECT_FUNDING && !($PROJECT_FUNDING eq '')) {
		my $content_funding_text = '';
		if (!(defined $GK_ROOT_DIR) || -e "$GK_ROOT_DIR/website/html/content_funding.html") {
			$content_funding_text = " Information on topic-specific funding can be found <A HREF=\"/content_funding.html\">here</A>.";
		}
    	print qq(<TABLE CLASS="footer" WIDTH="$HTML_PAGE_WIDTH"><TR><TD CLASS="footer">The development of $PROJECT_NAME is supported by $PROJECT_FUNDING.$content_funding_text</TD></TR></TABLE>\n);
	}
}

sub print_reactionmap_and_table_for_species1 {
    my $self = shift;
    my $species = shift || $self->webutils->get_focus_species->[0] || confess("Need species.");
    $self->preload_events_with_necessary_attribute_values($species);
    $self->_create_db_directories_if_necessary($species);
    my $frontpageitems = get_frontpageItems_for_species($self->webutils->dba,$species);
    print $self->reactionmap_html_for_species($species,$frontpageitems);
    $self->print_table_for_species($species,$frontpageitems);
}

sub print_reactionmap_and_table_for_species {
    my $self = shift;
    my $species = shift || $self->webutils->get_focus_species->[0] || confess("Need species.");
    $self->preload_events_with_necessary_attribute_values($species);
    $self->_create_db_directories_if_necessary($species);
    my $frontpageitems = get_frontpageItems_for_species($self->webutils->dba,$species);
    my $frontpageitems2 = get_frontpageItems_for_species2($self->webutils->dba,$species);
    my $reactionmap_html = $self->reactionmap_html_for_species($species,$frontpageitems);
    
    # TODO: grepping in $reactionmap_html for "AREA SHAPE" would tell you if
    # anything is actually in the reaction map, and you could then theoretically
    # use this to decide between a static image and showing the reaction map.  But,
    # at least in the Drosophila Reactome case, the map contained the insulin
    # pathway (even though it wasn't one of the top level pathways) so I shelved the
    # idea.
    
    if (defined $SKY_REPLACEMENT_IMAGE) {
    	# If an image file has been specified, try to display it.  If
    	# an empty string was encountered, assume that nothing is to be
    	# displayed.
    	if (!($SKY_REPLACEMENT_IMAGE eq '')) {
    		my $reactionmap_replacement_image = qq(<TABLE ALIGN="center" WIDTH="100%" CLASS="reactionmap"><TR><TD ALIGN="center"><IMG HEIGHT="150" ALIGN="center" BORDER="0" SRC="$SKY_REPLACEMENT_IMAGE"></TD></TR></TABLE>);
    		print "$reactionmap_replacement_image\n";
    	}
    } else {
    	print $reactionmap_html;
    }
    
    if ($self->is_multispecies_view) {
		$self->print_multispecies_table($species,$frontpageitems2);
    } else {
		$self->print_table_for_species_missing_items_greyed($species,$frontpageitems2);
    }
}

sub create_content_for_species_if_necessary_and_print {
    my $self = shift;
    my $species = shift || $self->webutils->get_focus_species->[0] || confess("Need species.");
    $self->_create_db_directories_if_necessary($species);
    my $out_file = sprintf "$FRONTPAGE_IMG_DIR/%s%s.html", $self->cached_file_basename($species), ($self->is_multispecies_view ? '.multi' : '');
    unless (-e $out_file) {
		local *OUT2;
		open(OUT2, ">$out_file")  or die($!);
		select OUT2;
		$self->print_reactionmap_and_table_for_species($species),
		select STDOUT;
		close OUT2;
    }
    local $/ = undef;
    local *IN2;
    open(IN2, $out_file) or die($!);
    print <IN2>;
    close IN2;
}

sub print_table_for_species {
    my ($self,$species,$frontpageitems) = @_;
    my $rows = int(scalar(@{$frontpageitems}) / $TABLE_COL_NUM);
    if ((scalar(@{$frontpageitems}) % $TABLE_COL_NUM) != 0) {
	$rows++;
    }
    print qq(<DIV CLASS="section">\n<TABLE WIDTH="$HTML_PAGE_WIDTH" CLASS="frontpageitems" CELLSPACING="0">);
    for my $r (0 .. $rows - 1) {
	print qq(<TR>);
	for my $c (0 .. $TABLE_COL_NUM - 1) {
	    my $i = $TABLE_COL_NUM * $r + $c;
	    if (($i < @{$frontpageitems}) && $frontpageitems->[$i]) {
		print qq(<TD CLASS="frontpageitem2">);
		print $self->hyperlinked_frontpageItem_for_species($frontpageitems->[$i],$species);
	    } else {
		print qq(<TD CLASS="frontpageitem2">);
		print '&nbsp;';
	    }
	    print qq(</TD>);
	}
	print qq(</TR>\n);
    }
    print qq(</TABLE>\n</DIV>\n);
}

sub print_table_for_species_missing_items_greyed {
    my ($self,$species,$frontpageitems) = @_;
    my $rows = int(scalar(@{$frontpageitems}) / $TABLE_COL_NUM);
    if ((scalar(@{$frontpageitems}) % $TABLE_COL_NUM) != 0) {
	$rows++;
    }
    print qq(<DIV CLASS="section">\n<TABLE WIDTH="$HTML_PAGE_WIDTH" CLASS="frontpageitems" CELLSPACING="0">);
    for my $r (0 .. $rows - 1) {
	print qq(<TR>);
	for my $c (0 .. $TABLE_COL_NUM - 1) {
	    my $i = $TABLE_COL_NUM * $r + $c;
	    if ($i < @{$frontpageitems}) {
		if ($frontpageitems->[$i]->[0]) {
		    print qq(<TD CLASS="frontpageitem2">);
		    print $self->hyperlinked_frontpageItem_for_species($frontpageitems->[$i]->[0],$species);
		} else {
		    print qq(<TD CLASS="frontpageitem2">);
		    print qq(<SPAN CLASS="greyed" ONMOUSEOVER="ddrivetip2('Not annotated or predicted in this species(print_table_for_species_missing_items_greyed)','#DCDCDC', 250)" ONMOUSEOUT="hideddrivetip2()">) . $frontpageitems->[$i]->[1]->displayName . qq(</SPAN>);
		}
	    } else {
		print qq(<TD CLASS="frontpageitem2">);
		print '&nbsp;';
	    }
	    print qq(</TD>);
	}
	print qq(</TR>\n);
    }
    print qq(</TABLE>\n</DIV>\n);
}

sub print_multispecies_table {
    my ($self,$species,$frontpageitems) = @_;
    my $rows = int(scalar(@{$frontpageitems}) / $TABLE_COL_NUM);
    if ((scalar(@{$frontpageitems}) % $TABLE_COL_NUM) != 0) {
	$rows++;
    }
    print qq(<DIV CLASS="section">\n<TABLE WIDTH="$HTML_PAGE_WIDTH" CLASS="frontpageitems" CELLSPACING="0">);
    for my $r (0 .. $rows - 1) {
	print qq(<TR>);
	for my $c (0 .. $TABLE_COL_NUM - 1) {
	    my $i = $TABLE_COL_NUM * $r + $c;
	    if ($i < @{$frontpageitems}) {
		if ($frontpageitems->[$i]->[0]) {
		    print qq(<TD CLASS="frontpageitem2">);
		    print $self->hyperlinked_multispecies_frontpageItem($frontpageitems->[$i]->[0],$species);
		} else {
		    print qq(<TD CLASS="frontpageitem2">);
		    print qq(<SPAN CLASS="greyed" ONMOUSEOVER="ddrivetip2('Not annotated or predicted in this species(print_multispecies_table)','#DCDCDC', 250)" ONMOUSEOUT="hideddrivetip2()">) . $frontpageitems->[$i]->[1]->displayName . qq(</SPAN><BR />);
		    print join(' ', @{$self->hyperlinked_species_abbreviations_for_frontpageItem_orthologousEvents($frontpageitems->[$i]->[1], 1)});
		}
	    } else {
		print qq(<TD CLASS="frontpageitem2">);
		print '&nbsp;';
	    }
	    print qq(</TD>);
	}
	print qq(</TR>\n);
    }
    print qq(</TABLE>\n</DIV>\n);
}

sub hyperlinked_multispecies_frontpageItem {
    my ($self,$item,$species) = @_;
    my $db_id = $item->db_id;
    my $db_name = $self->webutils->dba->db_name;
    my $sp_id = $species->db_id;
    my $sp_abbr = species_abbreviation($species);
    my $sp_name = $species->displayName;
    # Only those FrontPageItems with Reactions that are located on the map
    # should trigger the change of map upon mouseover.
    my $mouseover_str = '';
    my $img_path = "$FRONTPAGE_IMG_DIR/" . $self->_image_file_name($species,$item);
    if (-e $img_path) {
        $mouseover_str = qq{ ONMOUSEOVER='change("$db_name","$sp_id","$db_id");ddrivetip("$sp_name","#DCDCDC",150);'' ONMOUSEOUT='unchange("$db_id");hideddrivetip();'};
    }
    return
	qq(<A ID="$db_id") .
	_class_str($item) .
	qq( HREF=\") .
	$self->webutils->urlmaker->urlify($item) .
	'ZOOM=2' .
	qq(\"$mouseover_str>) .
	$item->displayName . qq(<BR />$sp_abbr</A> ) .
	join(' ', @{$self->hyperlinked_species_abbreviations_for_frontpageItem_orthologousEvents($item)});
}

sub hyperlinked_species_abbreviations_for_frontpageItem_orthologousEvents {
    my ($self,$item,$include_original) = @_;
    my $orthologues = $item->follow_class_attributes
	(-INSTRUCTIONS =>
	 {'Event' => {'attributes' => [qw(orthologousEvent)], 'reverse_attributes' => [qw(orthologousEvent)]}}
	 );
    unless ($include_original) {
	@{$orthologues} = grep {$_ != $item} @{$orthologues};
    }
    @{$orthologues} = sort {$a->Species->[0]->displayName cmp $a->Species->[0]->displayName} @{$orthologues};
    my @out;
    foreach my $o (@{$orthologues}) {
	push @out, $self->hyperlinked_species_abbreviation_for_frontpageItem_orthologousEvent($o,$item);
    }
    return \@out;
}

sub hyperlinked_species_abbreviation_for_frontpageItem_orthologousEvent {
    my ($self,$item) = @_;
    my $db_id = $item->db_id;
    my $db_name = $self->webutils->dba->db_name;
    my $species = $item->Species->[0];
    unless ($species) {
	cluck(sprintf "Species not specified for %s\n", $item->extended_displayName);
	return;
    }
    my $sp_id = $species->db_id;
    my $sp_abbr = species_abbreviation($species);
    my $sp_name = $species->displayName;
    # Only those FrontPageItems with Reactions that are located on the map
    # should trigger the change of map upon mouseover.
    my $mouseover_str = '';
    my $img_path = "$FRONTPAGE_IMG_DIR/" . $self->_image_file_name($species,$item);
    if (-e $img_path) {
        $mouseover_str = qq{ ONMOUSEOVER='change("$db_name","$sp_id","$db_id");ddrivetip("$sp_name","#DCDCDC",150);'' ONMOUSEOUT='unchange("$db_id");hideddrivetip();'};
    }
    return
	qq(<A ID="$db_id") .
	_class_str($item) .
	qq( HREF=\") .
	$self->webutils->urlmaker->urlify($item) .
	'ZOOM=2' .
	qq( \"$mouseover_str>$sp_abbr</A>);
}

sub hyperlinked_frontpageItem_for_species {
    my ($self,$item,$species) = @_;
    my $db_id = $item->db_id;
    my $db_name = $self->webutils->dba->db_name;
    my $sp_id = $species->db_id;
    # Only those FrontPageItems with Reactions that are located on the map
    # should trigger the change of map upon mouseover.
    my $mouseover_str = '';
    my $img_path = "$FRONTPAGE_IMG_DIR/" . $self->_image_file_name($species,$item);
    if (-e $img_path) {
        $mouseover_str = qq{ ONMOUSEOVER='change("$db_name","$sp_id","$db_id");' ONMOUSEOUT='unchange("$db_id");'};
    }
    return
	qq(<A ID="$db_id") .
	_class_str($item) .
	qq( HREF=\") .
	$self->webutils->urlmaker->urlify($item) .
	'ZOOM=2' .
	qq(\"$mouseover_str>) .
	$item->displayName . qq(</A> );
}

sub _class_str {
    my ($event) = @_;
    if (my $et = $event->EvidenceType->[0]) {
	if ($et->Name->[-1]) {
	    return ' CLASS="' . $et->Name->[-1] . '"';
	}
    }
    return ' CLASS="curated"';
}

sub species_abbreviation {
    my ($sp) = @_;
    my $dn = $sp->displayName;
#    if ($dn =~ /^(\w)\w*\s+(\w\w)/) {
#	return $1 . $2;
#    }
    if ($dn =~ /^(\w)\w*\s+(\w)/) {
	return $1 . $2;
    } elsif ($dn =~ /^(\w)/) {
	return $1;
    }
    return $dn;
}

sub get_frontpageItems_for_species {
    my ($dba,$species) = @_;
    my @out;
    foreach my $fp (@{$dba->fetch_all_class_instances_as_shells('FrontPage')}) {
	foreach my $fpi (@{$fp->FrontPageItem}) {
	    if (grep {$_ == $species} @{$fpi->Species}) {
		push @out, $fpi;
	    } else {
		my $flag = 0;
		foreach my $oe (@{$fpi->OrthologousEvent}) {
		    if (grep {$_ == $species} @{$oe->Species}) {
			push @out, $oe;
			$flag = 1;
			last;
		    }
		}
		$flag || push @out, undef;
	    }
	}
    }
    return \@out;
}

sub get_frontpageItems_for_species2 {
    my ($dba,$species) = @_;
    my @out;
    
    foreach my $fp (@{$dba->fetch_all_class_instances_as_shells('FrontPage')}) {
		foreach my $fpi (@{$fp->FrontPageItem}) {
		    my @tmp = ($fpi);
		    if (grep {$_ == $species} @{$fpi->Species}) {
				unshift @tmp, $fpi;
		    } else {
				my $flag = 0;
				foreach my $oe (@{$fpi->OrthologousEvent}) {
				    if (grep {$_ == $species} @{$oe->Species}) {
						unshift @tmp, $oe;
						$flag = 1;
						last;
				    }
				}
				$flag || unshift @tmp, undef;
		    }
		    push @out, \@tmp;
		}
    }
    return \@out;
}

# Returns a string containing the HTML needed to construct a reactionmap.
# As a sideffect, generates the reactionmap image and stores it to disk.
sub reactionmap_html_for_species1 {
    my ($self,$species,$frontpageitems) = @_;
    my $basename = $self->cached_file_basename($species);
    my $img_name = "$basename.$DEFAULT_IMAGE_FORMAT";
    my $img_path = "$FRONTPAGE_IMG_DIR/$img_name";
    my $um_path = "$FRONTPAGE_IMG_DIR/$basename.usemap";
    my $cgi = $self->webutils->cgi;
    my $usemap;
    
    if (-e $img_path and -e $um_path) {
		local *UM;
		open(UM, "$um_path")  || confess("Can't open '$um_path': $!");
		local $/ = undef;
		$usemap = <UM>;
		close UM;
    } else {
		my $rm = GKB::ReactionMap->new(-DBA => $self->webutils->dba,-CGI => $cgi,-USE_ANTIALIASED => 1);
		local *OUT;
		open(OUT, ">$img_path") || confess("Can't create '$img_path': $!");
		binmode OUT;
		my $frontpage_image = $rm->create_frontpage_image($species);
		my $frontpage_image_png = undef;
		eval {
			# Put this in an eval, because it is susceptible to
			# inconsitent PNG library versions
			$frontpage_image_png = $frontpage_image->png;
		};
		
		if (defined $frontpage_image_png) {
			print OUT $frontpage_image_png;
		}
#		print OUT $rm->create_frontpage_image($species)->png;
		
		close OUT;
		
		$usemap = $self->create_usemap_and_hilited_pathway_images($frontpageitems,$rm,$species);
		
		local *UM;
		open(UM, ">$um_path")  || confess("Can't create '$um_path': $!");
		print UM $usemap;
		close UM;
    }
    
    my $mapwidth = $REACTIONMAP_WIDTH + 3;
    my $tmp_hack = ($HTML_PAGE_WIDTH eq '100%')
	? qq(ALIGN="center" WIDTH="$mapwidth")
	: qq(WIDTH="$HTML_PAGE_WIDTH");
    my $height = int($REACTIONMAP_WIDTH / 6);
		
    return
	qq(<TABLE $tmp_hack CLASS="reactionmap">) .
	$cgi->start_form(-method =>'GET',-name =>"reactionmap",-action=>'/cgi-bin/eventbrowser') .
	$cgi->hidden(-name => 'DB',-value => $self->webutils->cgi->param('DB')) .
	qq(<INPUT TYPE="hidden" NAME="ZOOM" VALUE="2" />) .
	qq(<INPUT TYPE="hidden" NAME="ID" VALUE="" />) .
	qq(<TR><TD>) .
	qq(<MAP NAME="img_map">) . $usemap . qq(</MAP>) .
	qq(<IMG ID="rm_image" HEIGHT="$height" WIDTH="$REACTIONMAP_WIDTH" USEMAP="\#img_map" BORDER="0" SRC="/img-fp/$img_name">) .
	qq(</TD></TR>) .
        $cgi->end_form .
        qq(<TR><TD ALIGN="center"><IMG SRC="/icons/event_reactionmap_key.png"></TD></TR>) .
	qq(</TABLE>\n);

}

sub reactionmap_html_for_species {
    my ($self,$species,$frontpageitems) = @_;
    my $basename = $self->cached_file_basename($species);
    my $img_name = "$basename.$DEFAULT_IMAGE_FORMAT";
    my $img_path = "$FRONTPAGE_IMG_DIR/$img_name";
    my $um_path = "$FRONTPAGE_IMG_DIR/$basename.usemap";
    my $cgi = $self->webutils->cgi;
    my $usemap;
    
    if (-e $img_path and -e $um_path) {
		local *UM;
		open(UM, "$um_path")  || confess("Can't open '$um_path': $!");
		local $/ = undef;
		$usemap = <UM>;
		close UM;
    } else {
		my $rm = GKB::ReactionMap->new(-DBA => $self->webutils->dba,-CGI => $cgi,-USE_ANTIALIASED => 1);
		local *OUT;
		open(OUT, ">$img_path") || confess("Can't create '$img_path': $!");
		binmode OUT;
		my $frontpage_image = $rm->create_frontpage_image($species);
		my $frontpage_image_png = undef;
		eval {
			# Put this in an eval, because it is susceptible to
			# inconsitent PNG library versions
			$frontpage_image_png = $frontpage_image->png;
		};
		
		if (defined $frontpage_image_png) {
			print OUT $frontpage_image_png;
		}
		close OUT;
		
		$usemap = $self->create_usemap_and_hilited_pathway_images($frontpageitems,$rm,$species);
		
		local *UM;
		open(UM, ">$um_path")  || confess("Can't create '$um_path': $!");
		print UM $usemap;
		close UM;
    }
    
    my $mapwidth = $REACTIONMAP_WIDTH + 3;
    my $tmp_hack = ($HTML_PAGE_WIDTH eq '100%')
	? qq(ALIGN="center" WIDTH="$mapwidth")
	: qq(WIDTH="$HTML_PAGE_WIDTH");
    my $height = int($REACTIONMAP_WIDTH / 6);
		
    return
	qq(<TABLE $tmp_hack CLASS="reactionmap">) .
	$cgi->start_form(-method =>'GET',-name =>"reactionmap",-action=>'/cgi-bin/eventbrowser') .
	$cgi->hidden(-name => 'DB',-value => $self->webutils->cgi->param('DB')) .
	qq(<INPUT TYPE="hidden" NAME="ZOOM" VALUE="2" />) .
	qq(<INPUT TYPE="hidden" NAME="ID" VALUE="" />) .
	qq(<TR><TD>) .
	qq(<MAP NAME="img_map" ID="img_map"></MAP>) .
	qq(<IMG ID="rm_image" HEIGHT="$height" WIDTH="$REACTIONMAP_WIDTH" BORDER="0" SRC="/img-fp/$img_name">) .
	qq(</TD></TR>) .
        $cgi->end_form .
        qq(<TR><TD ALIGN="center"><IMG SRC="/icons/event_reactionmap_key.png"></TD></TR>) .
	qq(</TABLE>\n) .
	qq(<script language="JavaScript">loadUseMapForImgId('rm_image');</script>);
}

sub create_usemap_and_hilited_pathway_images {
    my ($self,$frontpageitems,$rm,$species) = @_;
    unless ($rm->coordinate_array) {
	return '';
    }
    my $usemap = '';
    my %r2fpi;
    my %fpi2r;
    foreach my $frontpageitem (@{$frontpageitems}) {
	$frontpageitem || next;
	my $reactions = $frontpageitem->follow_class_attributes
	    (-INSTRUCTIONS =>
	     {
              'ConceptualEvent' => {'attributes' => [qw(hasSpecialisedForm)]},
              'EquivalentEventSet' => {'attributes' => [qw(hasMember)]},
	      'Reaction' => {'attributes' => [qw(hasMember)]},
	      'BlackBoxEvent' => {'attributes' => [qw(hasComponent)]},
	      'Pathway' => {'attributes' => [qw(hasComponent hasEvent)]}},
	     -OUT_CLASSES => ['Reaction']
	     );
	$fpi2r{$frontpageitem->db_id} = $reactions;
	foreach my $r (@{$reactions}) {
	    push @{$r2fpi{$r->db_id}}, $frontpageitem;
	}
    }
    my $x_mag = $rm->x_magnification;
    my $y_mag = $rm->y_magnification;
    my $dba = $self->webutils->dba;
    my $cgi = $self->webutils->cgi;
    my $db_name = $dba->db_name;
    my $sp_db_id = $species->db_id;
    foreach my $ar (@{$rm->coordinate_array}) {
	my ($sx,$sy,$tx,$ty,$reaction_db_id,$reaction_displayName) = @{$ar};
	$sx *= $x_mag;
	$tx *= $x_mag;
	$sy *= $y_mag;
	$ty *= $y_mag;
	my $coord_str = join(',',GKB::Utils::rectangular_polygon_coordinates($sx,$sy,$tx,$ty,2));
	$reaction_displayName =~ s/</\&lt;/g;
	$reaction_displayName =~ s/>/\&gt;/g;
	if ($reaction_displayName !~ /\s/) {
	    $reaction_displayName =~ s/-/- /g;
	}
	$reaction_displayName =~ s/\'/\`/g;
	$reaction_displayName =~ s/\"/\\\"/g;

	my $fpis =  $r2fpi{$reaction_db_id};
	if ($fpis) {
	    @{$fpis} = sort {$a->db_id <=> $b->db_id} @{$fpis};
	    my $img_name = $self->_image_file_name($species,@{$fpis});
	    my $img_path = "$FRONTPAGE_IMG_DIR/$img_name";
	    unless (-e $img_path) {
		my @reactions;
#		my %species;
		foreach my $fpi (@{$fpis}) {
		    push @reactions, @{$fpi2r{$fpi->db_id}};
#		    map {$species{$_->db_id} = $_} @{$fpi->Species};
		}
	        my $rm1 = new GKB::ReactionMap(-DBA => $dba,-CGI => $cgi,-USE_ANTIALIASED => 1);
		$rm1->colour_reactions_by_evidence_type(\@reactions);		
#  		my $image = $rm1->create_image_for_species([values %species]);
		my $image = $rm1->create_image_for_species([$species]);
		local *OUT;
		open(OUT, ">$img_path") || confess("Can't create '$img_path': $!");
		binmode OUT;
		print OUT $image->png;
		close OUT;
	    }
	    my $tmp = join(',', map {'"' . $_->db_id . '"'} @{$fpis});
	    $usemap .= qq(<AREA SHAPE="poly" COORDS="$coord_str" ONMOUSEOVER='ddrivetip("$reaction_displayName","#DCDCDC",250);change2("$db_name",$sp_db_id,new Array($tmp))' ONMOUSEOUT='hideddrivetip();unchange2(new Array($tmp))' ONCLICK='handleClick($reaction_db_id,event)'>\n);
	} else {
	    $usemap .= qq(<AREA SHAPE="poly" COORDS="$coord_str" ONMOUSEOVER='ddrivetip("$reaction_displayName","#DCDCDC",250)' ONMOUSEOUT='hideddrivetip()' ONCLICK='handleClick($reaction_db_id,event)'>\n);
	}
    }
    foreach my $frontpageitem (@{$frontpageitems}) {
	$frontpageitem || next;
	if (my $reactions = $fpi2r{$frontpageitem->db_id}) {
	    my $img_name = $self->_image_file_name($species,$frontpageitem);
	    my $img_path = "$FRONTPAGE_IMG_DIR/$img_name";
	    unless (-e $img_path) {
                my $rm1 = new GKB::ReactionMap(-DBA => $dba,-CGI => $cgi,-USE_ANTIALIASED => 1);
		$rm1->colour_reactions_by_evidence_type($reactions);
#                my $image = $rm1->create_image_for_species($frontpageitem->Species);
                my $image = $rm1->create_image_for_species([$species]);
		local *OUT;
		open(OUT, ">$img_path") || confess("Can't create '$img_path': $!");
		binmode OUT;
		print OUT $image->png;
		close OUT;
	    }
	}
    }
    return $usemap;
}

sub _image_file_name {
    return cached_file_basename(@_) . ".$DEFAULT_IMAGE_FORMAT";
}

sub cached_file_basename {
    my ($self,$species,@instances) = @_;
    @instances = ($species) unless (@instances);
    return $self->webutils->dba->db_name . '/' . $species->db_id . '/' . join('_', (map {$_->db_id} sort {$a->db_id <=> $b->db_id} @instances));
}

sub _create_db_directories_if_necessary {
    my ($self,$species) = @_;
#    my $db_name = $self->webutils->dba->db_name;
#    my $dir_name = "$FRONTPAGE_IMG_DIR/$db_name";
    my $dir_name = $self->get_db_directory_name;
    unless (-e $dir_name) {
	mkdir($dir_name) || confess("Can't create '$dir_name': $!");
    }
    $dir_name .= '/' . $species->db_id;
    unless (-e $dir_name) {
	mkdir($dir_name) || confess("Can't create '$dir_name': $!");
    }
}

sub get_db_directory_name {
    return "$FRONTPAGE_IMG_DIR/" . $_[0]->webutils->dba->db_name;
}

sub preload_events_with_necessary_attribute_values {
    my ($self,$species) = @_;
    my $dba = $self->webutils->dba;
    my $events;
    if ($species) {
	$events = $dba->fetch_instance_by_attribute('Event',[['species',[$species->db_id]]]);
    } else {
	$events = $dba->fetch_all_class_instances_as_shells('Event');
	$dba->load_class_attribute_values_of_multiple_instances('Event','orthologousEvent',$events);
    }
    $dba->load_class_attribute_values_of_multiple_instances('Event','inferredFrom',$events);
    $dba->load_class_attribute_values_of_multiple_instances('Event','evidenceType',$events);
    $dba->load_class_attribute_values_of_multiple_instances('Event','species',$events);
    $dba->load_class_attribute_values_of_multiple_instances('Pathway','hasComponent',$events);
    $dba->load_class_attribute_values_of_multiple_instances('Pathway','hasEvent',$events);
    $dba->load_class_attribute_values_of_multiple_instances('EquivalentEventSet','hasMember',$events); 
    $dba->load_class_attribute_values_of_multiple_instances('ConceptualEvent','hasSpecialisedForm',$events);
}

# Extracts the first paragraph or so from the news file and
# prints it to STDOUT.  This allows you to put a shortened
# summary of the news into a little box on the front page.
#
# path					Path to the news file
# number_of_items			Number of news items
#					to be printed
# number_of_lines_from_last_item	Limits the number of
#					lines from the last
#					item that will be printed.
sub print_news_section {
    my ($path,$number_of_items,$number_of_lines_from_last_item) = @_;
    $number_of_items ||= 9;
    my $text;
    if ($path && -T $path && -r $path) {
		local *IN;
		local $/ = undef;
		open(IN, $path) || die "Unable to read $path.";
		$text = <IN>;
		close IN;
		my @items = $text =~ /(<li .+?<\/li>)/gmsi;
		my $item_text = "";
		for (my $i=0; $i<$number_of_items; $i++) {
			if (!$number_of_lines_from_last_item || $i<$number_of_items-1) {
				$item_text .= $items[$i];
			} else {
				# Try to split at punctuation full-stops
				# Assume trailing white space, to exclude
				# things like URLs, which also contain
				# full-stops, but which shouldn't be
				# broken.
				my @lines = split(/\.[ \n]/, $items[$i]);
				if (scalar(@lines)==0) {
					next;
				}
				for (my $j=0; $j<$number_of_lines_from_last_item; $j++) {
					$item_text .= $lines[$j];
				}
				$item_text .= " ....";
				if (scalar(@lines)>1 && $number_of_items<scalar(@lines)) {
					# Since this item does not contain
					# all lines, the terminating </LI>
					# will also be missing, so put it
					# back.
					$item_text .= "</LI>";
				}
			}
		}
		$text = "<UL>\n" . $item_text
		    . qq(\n<li class="normalsize"><A HREF="/news.html">More...</A></LI>)
		    . "</UL>\n";
    }
    print $text || '&nbsp;';
}

sub print_advert_section {
    my ($path) = @_;
    $path ||= "$GK_ROOT_DIR/website/html/advert.html";
    if ($path && -T $path && -r $path) {
	local *IN;
	local $/ = undef;
	open(IN, $path) || die "Unable to read $path.";
	print <IN>;
	close(IN);
    }
}

sub print_about_section {
    my $path = "$GK_ROOT_DIR/website/html/about.html";
    my $text = '';
    if ($path && -T $path && -r $path) {
	local *IN;
	local $/ = undef;
	open(IN, $path) || die "Unable to read $path.";
	$text = <IN>;
	close IN;
	($text) = $text =~ /<!-- begin about -->(.*?)<!-- end about -->/gmsi;
    }
    print $text || '&nbsp;';
}

sub handle_error {
    print qq(<PRE CLASS="error">\n$@\n</PRE>\n);
}

sub image_swapping_javascript {
    return <<__END__;
<script type="text/javascript" src="/javascript/yui/build/yahoo/yahoo-min.js"></script>
<script type="text/javascript" src="/javascript/yui/build/dom/dom-min.js"></script>
<script type="text/javascript" src="/javascript/yui/build/event/event-min.js"></script>
<script type="text/javascript" src="/javascript/yui/build/connection/connection-min.js"></script>
<script language="JavaScript">
var previousImage;
var previousColor;
var previousBg;

var offsetxpoint=0;
var offsetypoint=0;
var ie=document.all;
var ns6=document.getElementById && !document.all;

function ietruebody(){
    return (document.compatMode && document.compatMode!="BackCompat")? document.documentElement : document.body;
}

function change(db,sp,id){
    var img_src = "/img-fp/" + db + "/" + sp + "/" + id + ".$DEFAULT_IMAGE_FORMAT";
    previousImage = document.getElementById("rm_image").src;
    document.getElementById("rm_image").src = img_src;
    previousBg = document.getElementById(id).style.backgroundColor;
    document.getElementById(id).style.backgroundColor = "#B0C4DE";
    previousColor = document.getElementById(id).style.color;
    document.getElementById(id).style.color = "#000000";
}

function unchange(id){
    document.getElementById("rm_image").src = previousImage;
    document.getElementById(id).style.backgroundColor = previousBg;
    document.getElementById(id).style.color = previousColor;
    //previousImage = null;
}

function change2(db,sp_id,idArray){
    previousImage = document.getElementById("rm_image").src;
    previousBg = document.getElementById(idArray[0]).style.backgroundColor;
    previousColor = document.getElementById(idArray[0]).style.color;
    var img_src = "/img-fp/" + db + "/" + sp_id + "/" + idArray[0];
    document.getElementById(idArray[0]).style.backgroundColor = "#B0C4DE";
    document.getElementById(idArray[0]).style.color = "#000000";
    for (var i=1; i<idArray.length; i++) {
        document.getElementById(idArray[i]).style.backgroundColor = "#B0C4DE";
        document.getElementById(idArray[i]).style.color = "#000000";
        img_src += "_" + idArray[i];
    }
    img_src += ".$DEFAULT_IMAGE_FORMAT";
    document.getElementById("rm_image").src = img_src;
}

function unchange2(idArray){
    document.getElementById("rm_image").src = previousImage;
    for (var i=0; i<idArray.length; i++) {
        document.getElementById(idArray[i]).style.backgroundColor = previousBg;
        document.getElementById(idArray[i]).style.color = previousColor;
    }
    //previousImage = null;
}

function preloadImages() {
  var d=document; if(d.images){ if(!d.MM_p) d.MM_p=new Array();
    var i,j=d.MM_p.length,a=preloadImages.arguments; for(i=0; i<a.length; i++)
    if (a[i].indexOf("#")!=0){ d.MM_p[j]=new Image; d.MM_p[j++].src=a[i];}}
}

function handleClick(id,e) {
    if (id) {
	document.reactionmap.ID.value = id;
    }
    document.reactionmap.submit();
    return false;
}

function handleFocusSpeciesChange(evt) {
    evt = (evt) ? evt : ((window.event) ? window.event : null);
    if (evt) {
        // equalize W3C/IE models to get event target reference
        var elem = (evt.target) ? evt.target : ((evt.srcElement) ? evt.srcElement : null);
        if (elem) {
	    document.cookie = "FOCUS_SPECIES=" + escape(elem.options[elem.selectedIndex].value);
        }
    }
}

function loadUseMapForImgId (imgId) {
    var img = YAHOO.util.Dom.get(imgId);
    var mapsrc = img.src.replace(/png/, "usemap");
    var callback = {
      scope: this,
      customevents:{
	onSuccess: function(eventType, args) {
	    var mapEl = YAHOO.util.Dom.get("img_map");
	    mapEl.innerHTML = args[0].responseText;
	    img.useMap = "#img_map";
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

sub print_ajax_focus_species_selector {
    my ($self,$species) = @_;
    $species ||= $self->webutils->get_focus_species->[0] || confess("Need focus species");
    my %sp;
    my $db_name = $self->webutils->dba->db_name;
#    map {$sp{sprintf "/cgi-bin/reactionmap_and_table_4_species?DB=%s&FOCUS_SPECIES=%s", $db_name, $_} = $_} map {$_->displayName} 
#    @{$self->webutils->dba->fetch_instance_by_remote_attribute('Species',[['species:Reaction','IS NOT NULL',[]]])};
    map {$sp{'/img-fp/' . $self->cached_file_basename($_) . '.html'} = $_->displayName}
    grep {$_->displayName !~ /virus/i}
    @{$self->webutils->dba->fetch_instance_by_remote_attribute('Species',[['species:Reaction.locatedEvent:ReactionCoordinates','IS NOT NULL',[]]])};
    $self->webutils->cgi->delete('FOCUS_SPECIES_ID');
    print 
	qq(<DIV STYLE="margin-bottom:10px;color:white;background-color:navy;border:thin solid #B0C4DE;text-align:center;">) .
	qq(Focus on ) . 
        $self->webutils->cgi->popup_menu
	(
	 -name => 'FOCUS_SPECIES_ID',
	 -values => [sort {$sp{$a} cmp $sp{$b}} keys %sp],
	 -labels => \%sp,
	 -default => '/img-fp/' . $self->cached_file_basename($species) . '.html',
	 -onchange => 'loadDoc(event)'
	) .
	qq(</DIV>);
}

sub print_focus_species_selector {
    my ($self,$species) = @_;
    $species ||= $self->webutils->get_focus_species->[0] || confess("Need focus species");
#    my @species_names =
#	map {$_->displayName}
#    grep {$_->displayName !~ /(virus|bos taurus)/i}
#    @{$self->webutils->dba->fetch_instance_by_remote_attribute('Species',[['species:Reaction.locatedEvent:ReactionCoordinates','IS NOT NULL',[]]])};
    my @species_names = map {$_->displayName} @{$self->webutils->dba->fetch_frontpage_species()};
    my $cgi = $self->webutils->cgi;
    
    # If $species is not in the list of species known to the frontpage,
    # we need to find a plausible alternative.
    if (scalar(@species_names)>0) {
	    my $focus_species_in_frontpage = 0;
	    foreach my $species_name (@species_names) {
	    	if ($species_name eq $species->displayName) {
	    		$focus_species_in_frontpage = 1;
	    		last;
	    	}
	    }
	    if (!$focus_species_in_frontpage) {
	    	$species = $self->webutils->dba->fetch_frontpage_species()->[0];
	    	$cgi->param('FOCUS_SPECIES', $species);
	    }
    }
    
    print 
	$cgi->start_form(-method => 'GET',-action=>'/cgi-bin/frontpage') . 
	qq(<DIV STYLE="margin-bottom:10px;color:red;background-color:navy;border:thin solid #B0C4DE;text-align:center;font-weight:bold;">) .
	qq(The data displayed is for ) . 
	$cgi->hidden(-name => 'DB', -value => $self->webutils->dba->db_name) .
        $cgi->popup_menu
	(
	 -name => 'FOCUS_SPECIES',
	 -values => [sort {$a cmp $b} @species_names],
	 -default => $species->displayName,
	 -onchange => 'handleFocusSpeciesChange(event);submit();'
	) .
	qq(. Use the menu to change the species.) .
	qq( Check ) . $cgi->checkbox(-name => 'MULTISPECIES', -label => ' ', -onclick => 'submit();') . qq(for cross-species comparison.) .
	$cgi->end_form .
	qq(</DIV>);
}

sub is_multispecies_view {
    my $self = shift;
    return $self->webutils->cgi->param('MULTISPECIES');
}

1;
