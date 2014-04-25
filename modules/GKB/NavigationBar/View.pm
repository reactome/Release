=head1 NAME

GKB::NavigationBar::View

=head1 SYNOPSIS

Models the data used to construct the navigation bar used at the top of
the Reactome web pages.

=head1 DESCRIPTION

=head1 SEE ALSO

GKB::WebUtils
GKB::NavigationBar::Model

=head1 AUTHOR

David Croft E<lt>croft@ebi.ac.ukE<gt>

Copyright (c) 2008 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

package GKB::NavigationBar::View;

use GKB::Config;
use strict;
use vars qw(@ISA $AUTOLOAD %ok_field);
use Bio::Root::Root;
use GKB::NavigationBar::Model;

@ISA = qw(Bio::Root::Root);

# List the object variables here, so that they can be checked
for my $attr
    (qw(
    model
    release_warning
    display_banner
	) ) { $ok_field{$attr}++; }

sub AUTOLOAD {
    my $self = shift;
    my $attr = $AUTOLOAD;
    $attr =~ s/.*:://;
    return unless $attr =~ /[^A-Z]/;  # skip DESTROY and all-cap methods
    $self->throw("invalid attribute method: ->$attr()") unless $ok_field{$attr};
    $self->{$attr} = shift if @_;
    return $self->{$attr};
}  

sub new {
    my($pkg, $model, $release_warning, $display_banner) = @_;
    
    my $self = bless {}, $pkg;
   	
   	$self->model($model);
   	$self->release_warning('');
   	if (defined $release_warning) {
   		$self->release_warning($release_warning);
   	}
   	if (defined $display_banner) {
   		$self->display_banner($display_banner);
   	}
   	
    return $self;
}

# Needed by subclasses to gain access to class variables defined in
# this class.
sub get_ok_field {
	return %ok_field;
}

# Generates the view.  Returns a string containing the HTML & Javascript
# needed to build the navigation bar.
sub generate {
    my $self = shift;
    
    my $out = "";
    
	# These two lines are needed so that tooltips function
	# as advertized.    
    $out .= qq(<div id="dhtmltooltip" class="dhtmltooltip"></div>\n);
    $out .= qq(<script language="javascript" src="/javascript/tooltip.js"></script>\n);    
    
    # Yahoo Javascript for pulldown menu
    $out .= qq(<script src="/javascript/yui/build/yahoo-dom-event/yahoo-dom-event.js" type="text/javascript"></script>\n);
    $out .= qq(<script src="/javascript/yui/build/container/container_core-min.js" type="text/javascript"></script>\n);
    $out .= qq(<script src="/javascript/yui/build/menu/menu.js" type="text/javascript"></script>\n);
    $out .= qq(<script src="/javascript/pulldown_menu.js" type="text/javascript"></script>\n);
    my $release_warning = $self->release_warning;
    if (defined $release_warning && !($release_warning eq '')) {
    	$out .= qq(\n$release_warning);
    }
    
    # Put in a banner
    my $display_banner = $self->display_banner;
    if (defined $display_banner && !($display_banner eq '')) {
	    $out .= "<TABLE BORDER=\"0\" cellspacing=\"0\" cellpadding=\"0\">\n";
	    $out .= "  <TR>\n";
	    $out .= "    <TD><IMG WIDTH=\"600px\" SRC=\"/ReactomeGWT/images/half_height_banner1.png\" /></TD>\n";
	    $out .= "    <TD style=\"width:100%;\"><DIV style=\"background:#4443A8;height:38px;\"></DIV></TD>\n";
	    $out .= "  </TR>\n";
		$out .= "</TABLE>\n";
    }

	# Use Javascript to detect browser type and decide
	# on that basis which navigation bar to present -
	# non-Firefox versions of Mozilla/Netscape can't
	# render the pulldown menus properly, so in this
	# case, revert to the reliable table rendering.
	my $list_navigation_bar = $self->list_navigation_bar();
	my $table_navigation_bar = $self->table_navigation_bar();
	$list_navigation_bar =~ s/"/\\"/g;
	$list_navigation_bar =~ s/\n/ /g;
	$table_navigation_bar =~ s/"/\\"/g;
	$table_navigation_bar =~ s/\n/ /g;
    $out .= qq(<script language="JavaScript" type="text/javascript">\n);
    $out .= qq(var browser=navigator.appName;\n);
    $out .= qq(var nua=navigator.userAgent;\n);
    $out .= qq(var op=(nua.indexOf('Opera')!=-1);\n);
    $out .= qq(var saf=(nua.indexOf('Safari')!=-1);\n);
    $out .= qq(var konq=(!saf && (nua.indexOf('Konqueror')!=-1) ) ? true : false;\n);
    $out .= qq(var ffx=(nua.indexOf('Firefox')!=-1 );\n);
    $out .= qq(var moz=( (!saf && !konq && !ffx) && ( nua.indexOf('Gecko')!=-1 ) ) ? true : false;\n);
    $out .= qq(var ie=(browser=="Microsoft Internet Explorer") || ((nua.indexOf('MSIE')!=-1)&&!op);\n);
    $out .= qq(if (konq || moz) {\n);
    $out .= qq(	document.write("$table_navigation_bar")\n);
    $out .= qq(} else {\n);
    $out .= qq(	document.write("$list_navigation_bar")\n);
    $out .= qq(}\n);
    $out .= qq(</script>\n);
    
	return $out;
}

# Generates the Reactome "classic" navigation bar, using an HTML table.
# All of the items come up as buttons in a single row.
sub table_navigation_bar {
    my ($self) = @_;

    my @navigation_actions = $self->model->get_buttons();

	my $out = '';
	my $tooltip;
    $out .= qq(<TABLE id="navigation_bar">\n<TR>\n);
    for (my $i = 0; $i < @navigation_actions; $i++) {
		my $td_class = 'navigation';
		my $target_str = $navigation_actions[$i]->[3] || " target=\"_top\"";
		$out .= qq(<TD CLASS="$td_class"><A );
		$tooltip = $navigation_actions[$i]->[2];
		if (defined $tooltip && length($tooltip)>0) {
			$out .= qq(onMouseover="ddrivetip('$tooltip','#DCDCDC', 250)"; );
		}
		$out .= qq(onMouseout="hideddrivetip()" HREF="$navigation_actions[$i]->[0]"$target_str>$navigation_actions[$i]->[1]</A></TD>\n);
    }
    $out .= qq(</TR>\n</TABLE>\n);
    return $out;
}

# Generates a navigation bar with pulldown menus.
sub list_navigation_bar {
    my ($self) = @_;

	my $items = $self->model->get_pulldown_menus();

    my $out = "";

    $out .= qq(<div id="navigation_bar" class="yui-skin-sam">\n);
    $out .= qq(<div id="productsandservices" class="yui-main yui-b yuimenubar yuimenubarnav yui-module yui-overlay visible">\n);
    
    $out .= $self->navigation_bar_items($items, 0);

    $out .= qq(</div><!-- id="productsandservices" -->\n);
    $out .= qq(</div><!-- class="yui-skin-sam" -->\n);

    return $out;
}

# Recursively builds up ul/li lists for list navigation bar
sub navigation_bar_items {
	my ($self, $items, $depth) = @_;
	
    my $tabs = "\t";
    for (my $i=0; $i<$depth; $i++) {
    	$tabs .= "\t";
    }
    my $div2_class = qq( class="bd");
    my $div2_style = '';
    my $ul_class = '';
	my $out = '';
    if ($depth == 0) {
#    	$div2_style = qq( style="height: auto; overflow: visible;");
#    	$ul_class = qq( class="first-of-type");
    } else {
		$out .= qq($tabs<div class="yuimenu">\n);
    }
	$out .= qq($tabs<div$div2_class$div2_style>\n);
	$out .= qq($tabs<ul$ul_class>\n);
    for (my $i = 0; $i < @{$items}; $i++) {
		$out .= $self->navigation_bar_item($items->[$i], $depth, $i);
    }
    $out .= qq($tabs</ul>\n);
    $out .= qq($tabs</div>\n);
    if ($depth != 0) {
    	$out .= qq($tabs</div>\n);
    }
    
    return $out;
}

# Creates an li menu item and then calls navigation_bar_items
# if there any submenus.
sub navigation_bar_item {
	my ($self, $item, $depth, $position) = @_;

	my $url = $item->{'url'};
	if (defined $item->{'target'}) {
		$url .= $item->{'target'};
	}
	my $title = $item->{'title'} || '';
	if (!(defined $title) || $title eq "") {
	    print STDERR "View.navigation_bar_item: WARNING - title is undefined or empty!\n";
	    return "";
	}
	my $class = '';
	my $tooltip = $item->{'tooltip'};
	my $a_attributes;
    if ($depth == 0) {
    	my $use_reactome_icon = 0;
    	if (defined $self->model) {
     		$use_reactome_icon = $self->model->use_reactome_icon;
     		if (!(defined $use_reactome_icon)) {
     			$use_reactome_icon = 0;
     		}
    	}
    	if ($position==0 && $use_reactome_icon) {
#    		$class = qq( class="yuimenubaritem first-of-type");
    	
			$a_attributes = qq( class="yuimenubaritemlabel home");
    	} else {
#    		$class = qq( class="yuimenubaritem");
    	
			$a_attributes = qq( class="yuimenubaritemlabel");
    	}
	} else {
#		$class = qq( class="yuimenuitem");
		
		$a_attributes = qq( class="yuimenuitemlabel");
	}
	my $mouseover = '';
	if (defined $tooltip) {
		$mouseover = qq(onMouseover="ddrivetip('$tooltip','#DCDCDC', 250)" onMouseout="hideddrivetip()");
	}
	my $show;
	if (defined $url) {
		if (!(defined $item->{'target'})) {
			$url .= " target=\"_top\"";
		}
		$a_attributes .= " $mouseover HREF=$url";
	}
	$show = "<a$a_attributes>$title</a>";
    my $tabs = "\t\t";
    for (my $i=0; $i<$depth; $i++) {
    	$tabs .= "\t";
    }
    
    my $out = qq($tabs<li$class>$show\n);
    
    my $subitems = $item->{'subitems'};
    if (defined $subitems) {
    	# Recursively deal with subitems
    	$out .= $self->navigation_bar_items($subitems, $depth + 1);
    }
    
    $out .= qq(</li>\n);
    
	return $out;
}

1;

