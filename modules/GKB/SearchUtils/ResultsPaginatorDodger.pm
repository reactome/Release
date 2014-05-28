package GKB::SearchUtils::ResultsPaginatorDodger;

=head1 NAME

GKB::SearchUtils::ResultsPaginatorDodger

=head1 SYNOPSIS

Class for rendering pages of search results

=head1 DESCRIPTION

This class inherits from Paginator.  It uses HTML::Paginator
to split results into pages.

The curious name of this class derives from the author of the
HTML::Paginator class.

=head1 SEE ALSO

GKB::SearchUtils::Paginator
HTML::Paginator

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
#use HTML::Paginator;
use GKB::SearchUtils::HTMLPaginatorReactome;
use GKB::SearchUtils::ResultsPaginator;

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
    my($pkg, @args) = @_;
    my $self = bless {}, $pkg;
   	
	%ok_field = $self->SUPER::get_ok_field();
	
    if (defined $args[0]) {
    	$self->set_results($args[0]);
    }
    if (defined $args[1]) {
    	$self->set_render_result($args[1]);
    }
	$ok_field{"cgi"}++;

    return $self;
}

sub print {
    my($self) = @_;
    
    my $results = $self->results();
    if (defined $results) {
    	my $cgi = $self->cgi();
    	my $page = $cgi->param('page') || 1;
    	my $html_paginator = HTML::Paginator->new(10, @{$results->get_search_results()});
#    	my $html_paginator = GKB::SearchUtils::HTMLPaginatorReactome->new(10, @{$results->get_search_results()});
    	
    	$html_paginator->Name_Item('Result');
    	my @items = $html_paginator->Contents($page);
    	my $item;
    	my $render_result = $self->render_result();
    	my $div = qq(<div style="text-align: center; padding-bottom:1em; padding-top:1em; font-size:larger; font-weight:bold;">\n);
    	my $undiv = qq(</div>\n);
		print $div;
		print $html_paginator->Page_Header_HTML($page);
		print $undiv;
		foreach $item (@items) {
			$render_result->print($item);
			print "<br>\n";
		}
		print $div;
		print $html_paginator->Page_Nav_HTML($page);
		print $undiv;


#    	# Test out Data::Paginate
#    	my $total_entries = 20;
#    	my $pgr = Data::Paginate->new({ total_entries => $total_entries });
#    	print scalar $pgr->get_navi_html();


    }
}

1;

