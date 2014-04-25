package GKB::SearchUtils;

=head1 NAME

GKB::SearchUtils

=head1 SYNOPSIS

Utilities to help with displaying search results and other
search-related tasks.

=head1 DESCRIPTION

This is a very mixed bag of methods, all of which have the
common thread that they are related to searching in some
way, particularly, in dealing with search results.

If you specify a valid DBAdaptor object as the argument
for the constructor (i.e. the new method), then you can get
some speedup for your searches.

=head1 SEE ALSO

=head1 AUTHOR

David Croft E<lt>croft@ebi.ac.ukE<gt>

Copyright (c) 2007 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

use strict;
use vars qw(@ISA $AUTOLOAD %ok_field %INSTANCE_CLASS_NAME_MAP);
use Bio::Root::Root;
use Carp;
use WWW::SearchResult;
use Storable;
use GKB::Config;
use GKB::SearchUtils::SearchResults;
use GKB::SearchUtils::ResultsFilter;
use GKB::SearchUtils::ExplanatoryInstances;
use GKB::SearchUtils::ResultsRanker;
use GKB::SearchUtils::ResultRendererHighlighter;
use GKB::SearchUtils::ResultsPaginatorReactome;
use GKB::Utils::Timer;
use GKB::SearchUtils::ExplanatoryInstances;
use GKB::FileUtils;

@ISA = qw(Bio::Root::Root);

for my $attr
    (qw(
    	timer
    	dba
	) ) { $ok_field{$attr}++;}

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
    my($pkg, $dba) = @_;
    my $self = bless {}, $pkg;
   	
   	$self->timer(GKB::Utils::Timer->new());
   	
   	$self->dba($dba);

    return $self;
}

# Takes a list of query results (instances), adds and subtracts
# a few instances to make them easier to understand, sorts them
# to put important ones at the beginning, then paginates them
# and prints the appropriate HTML to create the current page
# in the list of results that a user wants to view.
#
# If cached results for this query already exist, they will be
# used directly, rather than doing that possibly time consuming
# analysis.
sub paginate_query_results {
	my($self, $cgi, $ar, $query, $operator, $species_db_id) = @_;
	
	# Get query terms
	my @terms = $self->extract_search_terms($query, $operator);
	
	my $result_type_selector = GKB::SearchUtils::ResultTypeSelector->new($cgi);
	my $search_results = undef;
	if ($result_type_selector->any_type_selectors_set($cgi)) {
		# Get list of results from cache, if available.
		my $type_selectors = $result_type_selector->get_type_selectors($cgi);
		my $store_filename = $self->create_search_results_filename($cgi->param("DB"), $species_db_id, $query, $operator, $type_selectors);
		if ( -e $store_filename ) {
			my $storable_string = GKB::FileUtils->read_file($store_filename);
			$search_results = Storable::thaw($storable_string);
		}
		
		# If the query has been run before, but none of the types have
		# been deselected, create and cache a filtered results set.
		if (!(defined $search_results)) {
			my $unfiltered_store_filename = $self->create_search_results_filename($cgi->param("DB"), $species_db_id, $query, $operator);
			if ( -e $unfiltered_store_filename ) {
				my $storable_string = GKB::FileUtils->read_file($unfiltered_store_filename);
				$search_results = Storable::thaw($storable_string);
				if (defined $search_results) {
					$search_results = $search_results->filter_by_type($type_selectors);
				}
			}
		}
		
		# If this query has never been run before, run it and cache
		# the results
		if (!(defined $search_results)) {
			# Skip over the neat stuff if the user queried for a stable
			# ID.  This is Esther's idea, ask her if you want an
			# explanation for it.  An alternative would have been to keep
			# the explanatory instances, but promote the instance that
			# matches the stable ID to the top of the list.
			if (!($query =~ /^\s*REACT_[0-9\.]+\s*$/)) {
				$ar = GKB::SearchUtils::ExplanatoryInstances->add_explanatory_instances($ar, $species_db_id);
				$ar = GKB::SearchUtils::ResultsFilter->filter($ar);
			}
			$ar = GKB::SearchUtils::ResultsRanker->rank($ar, \@terms);
			
			$search_results = GKB::SearchUtils::SearchResults->new($ar, $cgi->param("DB"));
			$search_results->calculate_type_counts();
			
			my $storable_string = Storable::freeze($search_results);
			GKB::FileUtils->write_file($store_filename, $storable_string);
		}
	} else {
		# If the user has (perhaps foolishly) deselected all of the type
		# selectors, don't show any results.
		$search_results = GKB::SearchUtils::SearchResults->new();
	}
	
	my $render_result = GKB::SearchUtils::ResultRendererHighlighter->new();
	$render_result->set_terms(\@terms);
	
	my $results_paginator = GKB::SearchUtils::ResultsPaginatorReactome->new($cgi, $query, $search_results, $render_result, $result_type_selector);

	if ($result_type_selector->any_type_selectors_changed($cgi)) {
		if ($search_results->results_count()<1) {
			$results_paginator->set_no_results_html("<DIV STYLE=\"font-size:9pt;font-weight:bold;text-align:center;color:red;padding-top:10px;\">You have deselected too many result types.<BR>Select one or more of the boxes below: \"Pathways\", \"Reactions\", \"Proteins\" or \"Others\", then click \"Show\".</DIV>");
		}
		$results_paginator->set_reset_page_flag(1);
	} else {
		if ($search_results->results_count()<1) {
			$results_paginator->result_type_selector->visible(0);
		}
	}
	
	$results_paginator->print();
}

# Given a string containing search terms, break it up into
# an array containing the individual terms, and return the
# array.
sub extract_search_terms {
	my($self, $query, $operator) = @_;
	
	my @terms = ();
	
	# Don't break up the query if it is to be matched as a complete
	# phrase.
	if (defined $operator && $operator eq 'PHRASE') {
		$query =~ /^"([^"]+)"$/;
		my $unquoted_query = $1;
		@terms = ($unquoted_query);
	} else {
		@terms = split(/[^a-zA-Z0-9]+/, $query);
	}
	
	return @terms;
}

# Creates a filename for storing a list of search results.  The
# method requires two arguments:
#
# species_db_id		DB_ID for the species over which the search was done (can be undef)
# query				Query string for the search
sub create_search_results_filename {
	my($self, $db_name, $species_db_id, $query, $operator, $type_selectors) = @_;
	
	if (!(defined $species_db_id)) {
		$species_db_id = "UNDEFINED";
	}
	
	my @terms = $self->extract_search_terms($query, $operator);
	my @new_terms = ();
	my $term;
	foreach $term (@terms) {
		# Replace non-alphanumeric characters with a double-underscore.
		# A bit dangerous if queries contain non-alphanumeric characters.
		$term =~ s/^[^a-zA-Z0-9]+//g;
		$term =~ s/[^a-zA-Z0-9]+$//g;
		$term =~ s/[^a-zA-Z0-9]+/__/g;
		push(@new_terms, $term);
	}
	my $search_results_filename = "$GK_TMP_IMG_DIR/query_store_" . $db_name . "_" . $species_db_id . "_" . join('_', @new_terms);
	
	if (defined $type_selectors && keys(%{$type_selectors})>0 && keys(%{$type_selectors})<4) {
		$search_results_filename .= "_" . join('_', keys(%{$type_selectors}));
	}
	
	return $search_results_filename;
}

sub is_search_already_performed {
	my($self, $db_name, $species_db_id, $query, $operator) = @_;
	
	my $store_filename = $self->create_search_results_filename($db_name, $species_db_id, $query, $operator);
	
	if ( -e $store_filename ) {
		return 1;
	} else {
		return 0;
	}
}

1;

