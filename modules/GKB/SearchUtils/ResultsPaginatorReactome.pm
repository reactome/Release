package GKB::SearchUtils::ResultsPaginatorReactome;

=head1 NAME

GKB::SearchUtils::ResultsPaginatorReactome

=head1 SYNOPSIS

Class for rendering pages of search results

=head1 DESCRIPTION

This class inherits from Paginator.  It implements a Reactome-specific
paginator.

=head1 SEE ALSO

GKB::SearchUtils::Paginator
GKB::SearchUtils::HTMLPaginatorReactome

=head1 AUTHOR

David Croft E<lt>croft@ebi.ac.ukE<gt>

Copyright (c) 2007 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

use strict;
use vars qw(@ISA $AUTOLOAD %ok_field);
use Bio::Root::Root;
use Carp;
use GKB::Config;
use GKB::SearchUtils::ResultsPaginator;
use GKB::SearchUtils::ResultTypeSelector;

@ISA = qw(GKB::SearchUtils::ResultsPaginator);

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
    my($pkg, $cgi, $query, $results, $render_result, $result_type_selector) = @_;
    my $self = bless {}, $pkg;
   	
	%ok_field = $self->SUPER::get_ok_field();
	
	$self->set_cgi($cgi);
    $self->set_query($query);
    $self->set_results($results);
    $self->set_render_result($render_result);
    
    # Add extra parameters to ok_field, needed in this subclass
	$ok_field{"items_per_page"}++;
    $ok_field{"page_count"}++;
    $ok_field{"pages"}++;
    $ok_field{"no_results_html"}++;
    $ok_field{"reset_page_flag"}++;
    $ok_field{"result_type_selector"}++;

	$self->set_items_per_page(10);
	$self->no_results_html("<A HREF=\"" . $PROJECT_HELP_URL . "?subject=Can you help with a Reactome query\%3F&body=Hi\%2C\%0D\%0A\%0D\%0AI tried to run the query\%3A\%0D\%0A\%0D\%0A$query\%0D\%0A\%0D\%0A...but it produced no results\%2C can you help\%3F\"><B>BUT:</B> if you let us know what you want to find, maybe we can help! (click here)</A>");
	$self->reset_page_flag(0);

    $result_type_selector->set_type_counts($results->get_type_counts);
	$self->result_type_selector($result_type_selector);

    return $self;
}

sub set_no_results_html {
    my($self, $no_results_html) = @_;
    
    $self->no_results_html($no_results_html);
}

sub set_reset_page_flag {
    my($self, $reset_page_flag) = @_;
    
    $self->reset_page_flag($reset_page_flag);
}

sub set_items_per_page {
    my($self, $items_per_page) = @_;
    
	$self->items_per_page($items_per_page);

	# Process results so that they are split across pages
	my $results = $self->get_results();
	my @search_results = @{$results->get_search_results()};
	
	my $page_count = 0;
	my @pages = ();
	while (my @slice = splice(@search_results, 0, $self->items_per_page)) {
	    push(@pages, \@slice);
	    $page_count++;
	}
	$self->page_count($page_count);
	$self->pages(\@pages);
}

sub print {
    my($self) = @_;
    
    my $results = $self->results();
    if (defined $results) {
    	my $cgi = $self->cgi();
    	my $page = $cgi->param('page') || 1;
    	if ($self->reset_page_flag) {
    		$page = 1;
    		$cgi->param('page', '1');
    	}
    	
    	# Allows user to take control of the number of results per page,
    	# E.g. when all results should be on a single page.
    	my $page_size = $cgi->param('PAGE_SIZE');
    	if (defined $page_size) {
    		if ($page_size eq 'MAX') {
    			$page_size = $self->get_result_count();
    		}
    		$self->set_items_per_page($page_size);
    	}
    	
    	# Get the search results for the current page
    	my @items = $self->get_items_in_page($page);
    	
    	# Print the page
    	my $item;
    	my $render_result = $self->render_result();
    	my $div = qq(<div style="text-align: center; padding-bottom:1em; padding-top:1em; font-size:x-large; font-weight:bold;">\n);
    	my $undiv = qq(</div>\n);
		print $div;
		print $self->generate_page_header($page);
		print $undiv;
		foreach $item (@items) {
			$render_result->print($item);
			print "<br>\n";
		}
		print $div;
		print $self->generate_page_navigator($page);
		print $undiv;
    }
}

# Gets the root part of the URL needed to retrieve a given page
sub _get_href_link {
	my ($self) = @_;
	
	my $href_link = $ENV{REQUEST_URI};
    if ($href_link =~ /&page\=\d+&/) {
        $href_link =~ s/&page\=\d+&/&/g;
		$href_link .= '&page=';
    } elsif ($href_link =~ /\?page\=\d+&/) {
        $href_link =~ s/\?page\=\d+&//g;
		$href_link .= '&page=';
    } elsif ($href_link =~ /&page\=\d+$/) {
        $href_link =~ s/&page\=\d+$//g;
		$href_link .= '&page=';
    } elsif ($href_link =~ /\?page\=\d+$/) {
        $href_link =~ s/\?page\=\d+$//g;
		$href_link .= '?page=';
    } elsif ($href_link =~ /\?/) {
        $href_link .= '&page=';
    } else {
        $href_link .= '?page=';
    }
    
    # Remove JustShown from the URL because this is used when deciding
    # how to deal with changes in the selected type.
    $href_link =~ s/JustShown=[a-zA-Z0-9_]+//g;
    $href_link =~ s/&&+/&/g;
    $href_link =~ s/\?&/?/;
    
    return $href_link;
}

sub get_items_in_page {
    my ($self, $page) = @_;

	if (!(defined $page) || $page<1 || $page>$self->get_page_count()) {
		return ();
	}
    
    return @{$self->pages->[$page - 1]};
}

sub get_item_count_in_page {
    my ($self, $page) = @_;

	my @items_in_page = $self->get_items_in_page($page);
    return scalar(@items_in_page);
}

sub get_page_count {
    my ($self) = @_;

    return $self->page_count;
}

sub get_result_count {
    my ($self) = @_;

	my $page_count = $self->get_page_count();
    return $self->items_per_page * ($page_count - 1) + $self->get_item_count_in_page($page_count);
}

sub get_first_item_num_in_page {
    my ($self, $page) = @_;

	if (!(defined $page) || $page<1) {
		return undef;
	}
    
    return undef unless $self->get_item_count_in_page($page);
    return ($self->items_per_page * ($page - 1)) + 1;
}

sub get_last_item_num_in_page {
    my ($self, $page) = @_;

	if (!(defined $page) || $page<1) {
		return undef;
	}
    
    return undef unless $self->get_item_count_in_page($page);
    return ($self->items_per_page * ($page - 1)) + scalar $self->get_item_count_in_page($page);
}

sub generate_page_header {
    my ($self, $page) = @_;

	if (!(defined $page) || $page<1) {
		return "[Page number error]";
	}
    
    my $item_count_this_page = $self->get_item_count_in_page($page);
    my $html;
    my $page_count = $self->get_page_count();
    if ($page_count > 1) {
        $html = "Results " . $self->get_first_item_num_in_page($page) . " to " . $self->get_last_item_num_in_page($page) . " of " . $self->get_result_count();
    } elsif ($item_count_this_page == 1) {
        $html = "The only result";
    } elsif ($item_count_this_page == 0) {
    	my $query = $self->query;
    	if (!(defined $query)) {
    		$query = '';
    	}
     	$query =~ s/[^A-Za-z0-9 \-_"]+/ /g; # get rid of most non-alphanumerics
    	$query =~ s/"/&#34;/g; # HTML esc code for quote
    	
        $html = "No results<P />" . $self->no_results_html;
    } else { # Case one page multiple items
        $html = "All $item_count_this_page results";
    }
    
    $html .= $self->result_type_selector->generate_type_selector();
    
    return $html;
}

sub generate_page_navigator {
    my ($self, $page) = @_;
    
	if (!(defined $page) || $page<1) {
		return "[Page number error]";
	}
       
    my $href_link = $self->_get_href_link();
    my $page_count = $self->get_page_count();
    my $previous_html = "";
    if ($page>1) {
    	my $previous_page = $page - 1;
        $previous_html = qq(<a href="$href_link$previous_page">< Previous </a>);
    }
    my $page_number_html = "";
    if ($page_count>1) {
	    my $start_page = $page - 5;
	    if ($start_page<1) {
	    	$start_page = 1;
	    }
	    my $end_page = $start_page + 9;
	    if ($end_page>$page_count) {
	    	$end_page = $page_count;
	    }
	    for (my $i=$start_page; $i<=$end_page; $i++) {
	    	if ($i==$page) {
	    		$page_number_html .= qq(<font color="red">$i</font>);
	    	} else {
	    		$page_number_html .= qq(<a href="$href_link$i">$i</a>);
	    	}
	    	$page_number_html .= ' ';
	    }
    }
    my $next_html = "";
    if ($page<$page_count) {
    	my $next_page = $page + 1;
        $next_html = qq(<a href="$href_link$next_page">Next ></a>);
    }
    
    $href_link =~ s/PAGE_SIZE\=\d+//g;
    $href_link =~ s/PAGE_SIZE\=MAX//g;
    $href_link =~ s/&&+/&/g;
    $href_link =~ s/\?&/?/;
    my $link_url = $href_link . "1&PAGE_SIZE=";
    my $show_all_results = '';
    if ($page_count>1) {
    	$link_url .= 'MAX';
    	$show_all_results = qq(<a href="$link_url">Show all results</a>);
    } elsif ($page_count==1 && $self->get_result_count()>10) {
    	$link_url .= '10';
    	$show_all_results = qq(<a href="$link_url">Paginate results</a>);
    }
    
    my $table_html = qq(<TABLE cellspacing="0" ALIGN="center">);
    $table_html .= qq(<COLGROUP><COL align="left" width="65"><COL align="center" width="170"><COL align="right" width="40"></COLGROUP>);
    $table_html .= qq(<TR>);
    $table_html .= qq(<TD>$previous_html</TD>);
    $table_html .= qq(<TD>$page_number_html</TD>);
    $table_html .= qq(<TD>$next_html</TD>);
    $table_html .= qq(</TR>);
    $table_html .= qq(<TR>);
    $table_html .= qq(<TD></TD>);
    $table_html .= qq(<TD> </TD>);
    $table_html .= qq(<TD></TD>);
    $table_html .= qq(</TR>);
    $table_html .= qq(<TR>);
    $table_html .= qq(<TD></TD>);
    $table_html .= qq(<TD> </TD>);
    $table_html .= qq(<TD></TD>);
    $table_html .= qq(</TR>);
    $table_html .= qq(<TR>);
    $table_html .= qq(<TD></TD>);
    $table_html .= qq(<TD>$show_all_results</TD>);
    $table_html .= qq(<TD></TD>);
    $table_html .= qq(</TR>);
    $table_html .= qq(</TABLE>);
    
    return $table_html;
}

1;

