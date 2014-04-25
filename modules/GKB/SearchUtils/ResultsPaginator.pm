package GKB::SearchUtils::ResultsPaginator;

=head1 NAME

GKB::SearchUtils::ResultsPaginator

=head1 SYNOPSIS

Base class for rendering pages of search results

=head1 DESCRIPTION

This class provides a public method, "print", which prints the
current page of search results to STDOUT in HTML format.  The
constructor takes two arguments:

* an object of type "SearchResults", which parcels out the results
  and delivers blocks of them to "Paginator" as needed.
* an object of type "Result" which knows how to render a single
  result into HTML.

Since this is the base class, only very limited functionality
is offered.  In order to get proper pagination, you should
subclass and use one of the paginator classes available from
CPAN to do the donkey work.

=head1 SEE ALSO

GKB::SearchUtils::SearchResults
GKB::SearchUtils::Result

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
use GKB::Utils;

@ISA = qw(Bio::Root::Root);

for my $attr
    (qw(
        cgi
        query
        results
        render_result
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
    my($pkg, $cgi, $query, $results, $render_result) = @_;

    my $self = bless {}, $pkg;
    
	$self->set_cgi($cgi);
    if (defined $query) {
    	$self->set_query($query);
    }
    if (defined $results) {
    	$self->set_results($results);
    }
    if (defined $render_result) {
    	$self->set_render_result($render_result);
    }
        
    return $self;
}

# Needed by subclasses
sub get_ok_field {
	return %ok_field;
}

sub set_cgi {
    my($self, $cgi) = @_;
    
    $self->cgi($cgi);
}

# Stores the query in the ResultsPaginator object, and as
# a sideffect, also logs the query.
sub set_query {
    my($self, $query) = @_;
    
    $self->query($query);
    GKB::Utils::log($query, "query");
}

sub set_results {
    my($self, $results) = @_;
    
    $self->results($results);
}

sub get_results {
    my($self) = @_;
    
    return $self->results;
}

sub set_render_result {
    my($self, $render_result) = @_;
    
    $self->render_result($render_result);
}

# Prints out the current page of results.  If you subclass,
# you will most likely want to overwrite this method.
sub print {
    my($self) = @_;
    
    my $results = $self->results();
    if (defined $results) {
    	my $result_num;
    	for ($result_num=0; $result_num<$results->results_count(); $result_num++) {
    		$self->print_result($result_num);
    		print "<br>\n";
    	}
    }
}

# Prints a single result, with the given index in the results
# list
sub print_result {
    my($self, $result_num) = @_;
    
    my $results = $self->results();
    my $render_result = $self->render_result();
    if (defined $results && defined $render_result) {
    	my $result = $results->get_result($result_num);
    	$render_result->print($result);
    }
}

1;

