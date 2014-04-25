=head1 NAME

GKB::NavigationBar::Model

=head1 SYNOPSIS

Models the data used to construct the navigation bar used at the top of
the Reactome web pages.

=head1 DESCRIPTION

=head1 SEE ALSO

GKB::WebUtils
GKB::NavigationBar::View

=head1 AUTHOR

David Croft E<lt>croft@ebi.ac.ukE<gt>

Copyright (c) 2008 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

package GKB::NavigationBar::Model;

use GKB::Config;
use strict;
use vars qw(@ISA $AUTOLOAD %ok_field);
use Bio::Root::Root;

@ISA = qw(Bio::Root::Root);

# This array determines the order of the level 0 (top level) menus
# in the navigation bar
my @default_level_0_menu_order = (
	"Home",
	"About",
	"Content",
	"Documentation",
	"Tools",
	"Download",
	"Contact Us",
	"Announcements",
);
# This hash determines the order of the level 1 (second-level) menus
# in the navigation bar
my %default_level_1_menu_order = (
	"About" =>         ['About Reactome', 'News', 'Other Reactomes', 'Reactome Group', 'SAB Members', 'Disclaimer', 'License Agreement'],
	"Content" =>       ['Table of Contents', 'DOIs', 'Database Schema', 'Editorial Calendar', 'Statistics',],
	"Documentation" => ['User Guide', 'Data Model', 'Orthology Prediction', 'Object/Relational mapping', 'Wiki', 'Linking to Reactome', 'Citing Reactome', ],
	"Tools" =>         ['Analyse your data', 'Compare species', 'Advanced Search', 'Small molecule Search', 'BioMart: query, link', 'PathFinder', 'SkyPainter'],
	"Outreach" =>      ['About Outreach', 'Training', 'Publications', 'Citing Reactome', 'Resource Guide', 'Outreach Calendar',  'Announcements'],
);

# List the object variables here, so that they can be checked
for my $attr
    (qw(
    db_name
    old_release_flag
    items_hash
    use_reactome_icon
    launch_new_page_flag
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
    my($pkg, $db_name, $old_release_flag, $use_reactome_icon, $launch_new_page_flag) = @_;
    
    my $self = bless {}, $pkg;
    
   	$self->db_name($db_name);
   	$self->old_release_flag($old_release_flag);
   	$self->use_reactome_icon($use_reactome_icon);
   	$self->launch_new_page_flag($launch_new_page_flag);
   	$self->items_hash($self->construct_items_hash());
   	
    return $self;
}

# Needed by subclasses to gain access to class variables defined in
# this class.
sub get_ok_field {
	return %ok_field;
}

# Gets menu items in a form that is suitable for producing pull-down
# menus
sub get_pulldown_menus {
	my ($self) = @_;

	my @new_level_0_menu_titles = keys(%{$self->items_hash});
	my $new_level_0_menu_title_hash = {};
	foreach my $new_level_0_menu_title (@new_level_0_menu_titles) {
		$new_level_0_menu_title_hash->{$new_level_0_menu_title} = 1;
	}
	my $items = [];
	my $title;
	my $subtitle;
	my @new_level_1_submenu_titles;
	my $new_level_1_submenu_title_hash;
	foreach $title (@default_level_0_menu_order) {
		delete $new_level_0_menu_title_hash->{$title};
		my $item = $self->items_hash->{$title};
		if (!(defined $item)) {
			next;
		}
		
		my @new_level_1_submenu_titles = keys(%{$item->{'subitems_hash'}});
		my $new_level_1_submenu_title_hash = {};
		foreach my $new_level_1_submenu_title (@new_level_1_submenu_titles) {
			$new_level_1_submenu_title_hash->{$new_level_1_submenu_title} = 1;
		}
	
		my $subitems = [];
		foreach $subtitle (@{$default_level_1_menu_order{$title}}) {
			delete $new_level_1_submenu_title_hash->{$subtitle};
			my $subitem = $item->{'subitems_hash'}->{$subtitle};
			if (!(defined $subitem)) {
				next;
			}
			
			push(@{$subitems}, $subitem);
		}
		# Add user-defined submenus
		foreach $subtitle (sort(keys(%{$new_level_1_submenu_title_hash}))) {
			my $subitem = $item->{'subitems_hash'}->{$subtitle};
			if (!(defined $subitem)) {
				next;
			}
			
			push(@{$subitems}, $subitem);
		}
		if (scalar(@{$subitems})>0) {
			$item->{'subitems'} = $subitems;
		}
		
		push(@{$items}, $item);
	}
	
	# Add user-defined menus
	foreach $title (sort(keys(%{$new_level_0_menu_title_hash}))) {
		my $item = $self->items_hash->{$title};
		if (!(defined $item)) {
			next;
		}
		
		my $subitems = [];
		foreach $subtitle (@{$default_level_1_menu_order{$title}}) {
			my $subitem = $item->{'subitems_hash'}->{$subtitle};
			if (!(defined $subitem)) {
				next;
			}
			
			push(@{$subitems}, $subitem);
		}
		if (scalar(@{$subitems})>0) {
			$item->{'subitems'} = $subitems;
		}
		
		push(@{$items}, $item);
	}
	
	return $items;
}

# Gets menu items in a form that is suitable for producing a row
# of buttons
sub get_buttons {
	my ($self) = @_;

# This code lifted from WebUtils, to preserve tooltips, just in case they are needed in the future

#    my @navigation_actions = ();
#    if ($old_release_flag) {
#	    @navigation_actions = (
#		["/cgi-bin/frontpage$ {dbstring}", 'Home', "$PROJECT_NAME home page"],
#		['/about.html','About', "Brief description of $PROJECT_NAME"],
#		["/cgi-bin/toc$ {dbstring}",'TOC', "Table of contents"],
#		[$USER_GUIDE_URL, 'User Guide', "How to use Reactome", ' target="guide_popout"'],     
#		["/cgi-bin/classbrowser$ {dbstring}",'Schema', "$PROJECT_NAME class attributes and relationships"],
#		["/cgi-bin/extendedsearch$ {dbstring}",'Extended Search', "An extended query form to search $PROJECT_NAME"],
#		["/cgi-bin/pathfinder$ {dbstring}",'PathFinder', "A utility to find pathways in $PROJECT_NAME"],
#		["/cgi-bin/skypainter2$ {dbstring}",'SkyPainter', "A utility for highlighting reactions on reaction map based on sequence and other identifiers"],
#		["$WIKI_URL/Main_Page", 'Wiki', "Allows you to edit documentation associated with $PROJECT_NAME"],
#		[$PROJECT_HELP_URL,'Contact Us', "Ask for help with $PROJECT_NAME"],
#		['http://mail.reactome.org/mailman/listinfo/reactome-announce','Announcements', "Subscribe to Reactome announcements mailing list"],
#		     );
#	} else {
#	    @navigation_actions = (
#		["/cgi-bin/frontpage$ {dbstring}", 'Home', "$PROJECT_NAME home page"],
#		['/about.html','About', "Brief description of $PROJECT_NAME"],
#		["/cgi-bin/toc$ {dbstring}",'TOC', "Table of contents"],
#		[$USER_GUIDE_URL, 'User Guide', "How to use Reactome", ' target="guide_popout"'],     
#		['/data_model.html','Data Model', "Simplified description of the $PROJECT_NAME data model"],
#		["/cgi-bin/classbrowser$ {dbstring}",'Schema', "$PROJECT_NAME class attributes and relationships"],
#		["/cgi-bin/extendedsearch$ {dbstring}",'Extended Search', "An extended query form to search $PROJECT_NAME"],
#		["/cgi-bin/pathfinder$ {dbstring}",'PathFinder', "A utility to find pathways in $PROJECT_NAME"],
#		["/cgi-bin/skypainter2$ {dbstring}",'SkyPainter', "A utility for highlighting reactions on reaction map based on sequence and other identifiers"],
#		['/download/index.html','Download', "Download $PROJECT_NAME data and code"],
#		['/referring2GK.html','Linking', "Information on how to link to $PROJECT_NAME"],
#		['/citation.html','Citing', "How to link and cite $PROJECT_NAME and its content"],
#		['http://wiki.reactome.org/index.php/Reactome_Calendar','Editorial Calendar', "Planned future content of $PROJECT_NAME "],
#		["/cgi-bin/mart", 'Mart', "Fast bulk queries of $PROJECT_NAME and links to external databases"],
#		["$WIKI_URL/Main_Page", 'Wiki', "Allows you to edit documentation associated with $PROJECT_NAME"],
#		[$PROJECT_HELP_URL,'Contact Us', "Ask for help with $PROJECT_NAME"],
#		['http://mail.reactome.org/mailman/listinfo/reactome-announce','Announcements', "Subscribe to Reactome announcements mailing list"],
#		     );
#	}

	my $menus = $self->get_pulldown_menus();
	my @buttons = ();
	my $item;
	my $subitems;
	my $subitem;
	foreach $item (@{$menus}) {
		$subitems = $item->{'subitems'};
		if (!(defined $subitems) || scalar(@{$subitems})<1) {
			my $button = [$item->{'url'}, $item->{'title'}, $item->{'tooltip'}];
			push(@buttons, $button);
			next;
		}
		foreach $subitem (@{$subitems}) {
			my $button = [$subitem->{'url'}, $subitem->{'title'}, $subitem->{'tooltip'}];
			push(@buttons, $button);
		}
	}
	
	return @buttons;
}

# Constructs Reactome navigation bar menus.  User-defined changes to
# the menus specified in Config.pm will also be incorporated.
sub construct_items_hash {
	my ($self) = @_;

   	my $db_name = $self->db_name();
   	my $old_release_flag = $self->old_release_flag();

	my $dbstring = "";
    if (defined $db_name && !($db_name eq "")) {
		$dbstring = "?DB=$db_name";
    }
    
    my $items_hash = {
    	'Home' =>          $self->construct_home_item_hash($db_name),
    	'About' =>         $self->construct_about_item_hash(),
    	'Content' =>       $self->construct_content_item_hash($db_name),
    	'Documentation' => $self->construct_documentation_item_hash(),
    	'Tools' =>         $self->construct_tools_item_hash($db_name, $old_release_flag),
    	'Download' =>      $self->construct_download_item_hash($old_release_flag),
    	'Contact Us' =>    $self->construct_contact_us_item_hash(),
    	'Outreach' =>      $self->construct_outreach_item_hash(),
#    	'Announcements' => $self->construct_announcements_item_hash(),
    };
    
    $items_hash = $self->incorporate_user_config_into_items_hash($items_hash);
    
    if ($self->launch_new_page_flag) {
    	foreach my $item_name (keys(%{$items_hash})) {
    		if ($item_name eq 'Home') {
    			next;
    		}
    		
    		my $url = $items_hash->{$item_name}->{'url'};
    		if (defined $url) {
    			$items_hash->{$item_name}->{'target'} = ' target="guide_popout"';
    		}
    		
    		foreach my $subitem_name (keys(%{$items_hash->{$item_name}->{'subitems_hash'}})) {
    			$items_hash->{$item_name}->{'subitems_hash'}->{$subitem_name}->{'target'} = ' target="guide_popout"';
    		}
    	}
    }
    
	return $items_hash;
}

sub construct_home_item_hash {
	my ($self, $db_name) = @_;

	my $dbstring = "";
    if (defined $db_name && !($db_name eq "")) {
		$dbstring = "?DB=$db_name";
    }
    
    my $title = "Home";
    if ($self->use_reactome_icon) {
    	$title = '<img SRC="' . $PROJECT_LOGO_URL . '" BORDER="0" ALIGN="left" HEIGHT="100%" WIDTH="26">&nbsp;&nbsp;&nbsp;' . $title;
    }
    
    my $item = {'url' => "/cgi-bin/frontpage$ {dbstring}", 'title' => $title};

    return $item;
}

sub construct_about_item_hash {
	my ($self) = @_;

    my $subitems = {};
    if (!(defined $GK_ROOT_DIR) || -e "$GK_ROOT_DIR/website/html/about.html") {
    	$subitems->{'About Reactome'} = {'url' => '/about.html', 'title' => 'About Reactome'};
    }
    if (!(defined $GK_ROOT_DIR) || -e "$GK_ROOT_DIR/website/html/news.html") {
    	$subitems->{'News'} = {'url' => '/news.html', 'title' => 'News'};
    }
    $subitems->{'Other Reactomes'} = {'url' => 'http://wiki.reactome.org/index.php/Reactomes', 'title' => 'Other Reactomes'};
    $subitems->{'Disclaimer'} = {'url' => 'http://wiki.reactome.org/index.php/Reactome_Disclaimer', 'title' => 'Disclaimer'};
    $subitems->{'License Agreement'} = {'url' => 'http://wiki.reactome.org/index.php/Reactome_License_Agreement', 'title' => 'License Agreement'};
    
	my $about_url = '';
    if (!(defined $GK_ROOT_DIR) || -e "$GK_ROOT_DIR/website/html/about.html") {
    	$about_url = '/about.html';
    }
    
    my $item = {'url' => $about_url, 'title' => "About", 'subitems_hash' => $subitems};

    return $item;
}

sub construct_content_item_hash {
	my ($self, $db_name) = @_;

	my $dbstring = "";
    if (defined $db_name && !($db_name eq "")) {
		$dbstring = "?DB=$db_name";
    }
    
    my $subitems = {};
	$subitems->{'Table of Contents'} = {'url' => "/cgi-bin/toc$dbstring", 'title' => 'Table of Contents',};
	$subitems->{'DOIs'} = {'url' => "/cgi-bin/doi_toc$dbstring", 'title' => 'DOIs',};
	$subitems->{'Database Schema'} = {'url' => "/cgi-bin/classbrowser$dbstring", 'title' => 'Database Schema'};
	$subitems->{'Editorial Calendar'} = {'url' => 'http://wiki.reactome.org/index.php/Reactome_Calendar', 'title' => 'Editorial Calendar'};
    	
    if (!(defined $GK_ROOT_DIR) || -e "$GK_ROOT_DIR/website/html/stats.html") {
    	$subitems->{'Statistics'} = {'url' => '/stats.html', 'title' => 'Statistics'};
    }

    my $item = {'url' => "/cgi-bin/toc$ {dbstring}", 'title' => "Content", 'subitems_hash' => $subitems};

    return $item;
}

sub construct_documentation_item_hash {
	my ($self) = @_;

    my $subitems = {};
	$subitems->{'User Guide'} = {'url' => $USER_GUIDE_URL, 'title' => 'User Guide', 'target' => ' target="guide_popout"'}; 
	$subitems->{'Data Model'} = {'url' => '/data_model.html', 'title' => 'Data Model'};
	$subitems->{'Orthology Prediction'} = {'url' => "/electronic_inference.html", 'title' => 'Orthology Prediction'};
	$subitems->{'Object/Relational mapping'} = {'url' => "/object_relational_mapping.html", 'title' => 'Object/Relational mapping'};
	$subitems->{'Wiki'} = {'url' => "$WIKI_URL/Main_Page", 'title' => 'Wiki'};
    if ((!(defined $GK_ROOT_DIR) || -e "$GK_ROOT_DIR/website/html/referring2GK.html")) {
    	$subitems->{'Linking to Reactome'} = {'url' => '/referring2GK.html', 'title' => 'Linking to Reactome'};
    }
#    if ((!(defined $GK_ROOT_DIR) || -e "$GK_ROOT_DIR/website/html/citation.html")) {
#    	$subitems->{'Citing Reactome'} = {'url' => '/citation.html', 'title' => 'Citing Reactome'};
#    }

    my $item = {'url' => $USER_GUIDE_URL, 'title' => "Documentation", 'subitems_hash' => $subitems};

    return $item;
}

sub construct_tools_item_hash {
	my ($self, $db_name, $old_release_flag) = @_;

	my $dbstring = "";
    if (defined $db_name && !($db_name eq "")) {
		$dbstring = "?DB=$db_name";
    }
    
	my $entity_level_view_db = $GK_ENTITY_DB_NAME;
	if (!(defined $entity_level_view_db)) {
	    $entity_level_view_db = "${db_name}";
	}

    my $subitems = {};
	$subitems->{'Analyse your data'} = {'url' => "/ReactomeGWT/service/analysis/multipart/combined_analysis_set",'title' => 'Analyse your data'};
	$subitems->{'Compare species'} = {'url' => "/ReactomeGWT/service/analysis/multipart/compare_species_analysis_set",'title' => 'Compare species'};
	$subitems->{'Advanced Search'} = {'url' => "/cgi-bin/extendedsearch$dbstring",'title' => 'Advanced Search'};
	$subitems->{'Small molecule Search'} = {'url' => "/cgi-bin/small_molecule_search$dbstring",'title' => 'Small molecule Search'};
	$subitems->{'PathFinder'} = {'url' => "/cgi-bin/pathfinder$dbstring",'title' => 'PathFinder'};
	$subitems->{'SkyPainter'} = {'url' => "/cgi-bin/skypainter2$dbstring",'title' => 'SkyPainter'};
        if (!$old_release_flag) {
		$subitems->{'BioMart: query, link'} = {'url' => "/cgi-bin/mart", 'title' => 'BioMart: query, link'};
	}

    my $item = {'title' => "Tools", 'subitems_hash' => $subitems};

    return $item;
}

sub construct_download_item_hash {
	my ($self, $old_release_flag) = @_;

    my $item = undef;
    if (!$old_release_flag) {
		$item = {'url' => '/download/index.html','title' => 'Download'};
	}

    #print STDERR "construct_download_item_hash: old_release_flag=$old_release_flag, item=$item\n";

    return $item;
}

sub construct_contact_us_item_hash {
	my ($self, $db_name, $old_release_flag) = @_;

    my $item = {'url' => $PROJECT_HELP_URL,'title' => 'Contact Us'};

    return $item;
}

sub construct_announcements_item_hash {
	my ($self, $db_name, $old_release_flag) = @_;

	my $item = {'url' => 'http://mail.reactome.org/mailman/listinfo/reactome-announce', 'title' => 'Announcements', 'tooltip' => "Subscribe to Reactome announcements mailing list"};

    return $item;
}

sub construct_outreach_item_hash {
	my ($self) = @_;

    my $subitems = {};
	$subitems->{'About Outreach'} = {'url' => "http://wiki.reactome.org/index.php/Outreach",'title' => 'About Outreach'};
	$subitems->{'Training'} = {'url' => "http://wiki.reactome.org/index.php/Reactome_Training",'title' => 'Training'};
	$subitems->{'Publications'} = {'url' => "http://wiki.reactome.org/index.php/Reactome_Publications",'title' => 'Publications'};
	$subitems->{'Citing Reactome'} = {'url' => "http://wiki.reactome.org/index.php/Citing_Reactome",'title' => 'Papers Citing Reactome'};
	$subitems->{'Resource Guide'} = {'url' => "http://wiki.reactome.org/index.php/Reactome_Resource_Guide",'title' => 'Resource Guide'};
	$subitems->{'Outreach Calendar'} = {'url' => "http://wiki.reactome.org/index.php/Outreach_Calendar",'title' => 'Outreach Calendar'};
#	$subitems->{'Announcements'} = {'url' => "http://mail.reactome.org/mailman/listinfo/reactome-announce",'title' => 'Announcements'};
	$subitems->{'Announcements'} = {'url' => "https://lists.reactome.org/mailman/listinfo/reactome-announce",'title' => 'Announcements'};

    my $item = {'title' => "Outreach", 'subitems_hash' => $subitems};

    return $item;
}

# Uses the user-defined changes to the menu laid down
# in Config.pm to modify the supplied item hash.
sub incorporate_user_config_into_items_hash {
	my ($self, $items_hash) = @_;
	
	if (!(defined $NAVIGATION_BAR_MENUS)) {
		return $items_hash;
	}
	
	my $title;
	my $subtitle;
	my $subitems_hash;
	my $user_defined_subitems_hash;
	foreach $title (keys(%{$NAVIGATION_BAR_MENUS})) {
		if (defined $items_hash->{$title}) {
			if (!(defined $NAVIGATION_BAR_MENUS->{$title})) {
				delete $items_hash->{$title};
				next;
			}
			
			if (defined $NAVIGATION_BAR_MENUS->{$title}->{'url'}) {
				$items_hash->{$title}->{'url'} = $NAVIGATION_BAR_MENUS->{$title}->{'url'};
			}
			
			$user_defined_subitems_hash = $NAVIGATION_BAR_MENUS->{$title}->{'subitems_hash'};
			if (!(defined $user_defined_subitems_hash)) {
				next;
			}
			$subitems_hash = $items_hash->{$title}->{'subitems_hash'};
			if (!(defined $subitems_hash)) {
				next;
			}
			foreach $subtitle (keys(%{$user_defined_subitems_hash})) {
				if (exists $subitems_hash->{$subtitle}) {
					if (defined $user_defined_subitems_hash->{$subtitle}) {
						$subitems_hash->{$subtitle} = $user_defined_subitems_hash->{$subtitle};
					} else {
						delete $subitems_hash->{$subtitle};
					}
				} else {
					$subitems_hash->{$subtitle} = $user_defined_subitems_hash->{$subtitle};
				}
			}
			$items_hash->{$title}->{'subitems_hash'} = $subitems_hash;
		} else {
			$items_hash->{$title} = $NAVIGATION_BAR_MENUS->{$title};
		}
	}

	return $items_hash;
}

1;

