package GKB::SearchUtils::SearchResults;

=head1 NAME

GKB::SearchUtils::SearchResults

=head1 SYNOPSIS

Base class for sourcing search results

=head1 DESCRIPTION

This class dishes out results as arrays of WWW::SearchResult objects.
The most important method is get_search_results, which is able to
get all results or just a subset of them, if you require.

This is intended to be a base class, so it has limited funtionality.
You should subclass from this to do the things you want.  You may
well need to have some kind of persistence mechanism, for example,
or you might have some clever mechanism that does a SELECT in an
SQL database and only retrieves exactly the results required.

=head1 SEE ALSO

WWW::SearchResult

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

@ISA = qw(Bio::Root::Root);

for my $attr
    (qw(
        search_results
        type_counts
	) ) { $ok_field{$attr}++; }

%INSTANCE_CLASS_NAME_MAP = 
    (
     'ReferencePeptideSequence' => 'Protein',
     'ReferenceGeneProduct' => 'Protein',
     'ReferenceIsoform' => 'Protein',
     'ReferenceDNASequence' => 'Gene',
     'ReferenceRNASequence' => 'RNA',
     'ReferenceMolecule' => 'Compound',
     'EntityWithAccessionedSequence' => 'Protein',
     'ReactionlikeEvent' => 'Reaction',
     'BlackBoxEvent' => 'Reaction',
     'LiteratureReference' => 'Literature',
     'NegativeRegulation' => 'Inhibition',
     'PositiveRegulation' => 'Activation',
     'Requirement' => 'Activation',
    );
     
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
    my($pkg, $instances, $db_name) = @_;
    my $self = bless {}, $pkg;
    
    my $search_results = [];
    if (defined $instances) {
    	$search_results = $self->convert_instances_to_query_results($instances, $db_name);
    }
    $self->search_results($search_results);
    
    my %type_counts = ();
    $self->type_counts(\%type_counts);
    
    return $self;
}

sub set_search_results {
    my($self, $search_results) = @_;
    
    $self->search_results($search_results);
}

# Returns a reference to an array of SearchResult objects.
# With no arguments, all known SearchResult objects are
# returned.  You can control things more precisely with
# the following options:
#
# offset			Offset from the beginning of the results
#					list to start with.
# range				The number of results, starting at offset,
#					to be returned.
# padding_flag		If set to 1, causes results before offset
#					and after range to be filled with undefs;
#					by default, no results are inserted before
#					offset or after range.
sub get_search_results {
    my($self, $offset, $range, $padding_flag) = @_;
    
    my $search_results = $self->search_results();
    my @new_search_results = ();
    if (defined $offset && defined $search_results) {
    	my $search_result;
    	for (my $i=0; $i<scalar(@{$search_results}); $i++) {
    		if ($i<$offset) {
    			if ($padding_flag) {
    				$new_search_results[$i] = undef;
    			}
    			next;
    		}
    		if (!(defined $range) || (($offset - $i)<$range)) {
    			if ($padding_flag) {
    				$new_search_results[$i] = $search_results->[$i];
    			} else {
    				$new_search_results[$offset - $i] = $search_results->[$i];
    			}
    		} else {
     			if ($padding_flag) {
    				$new_search_results[$i] = undef;
    			} else {
    				last;
     			}
    		}
    	}
    	$search_results = \@new_search_results;
    }
    
    return $search_results;
}

sub get_result {
	my($self, $result_num) = @_;
	
    my $search_results = $self->search_results();
	if (defined $search_results) {
		return $search_results->[$result_num];
	}
	
	return undef;
}

sub results_count {
	my($self, $result_num) = @_;
	
    my $search_results = $self->search_results();
	if (defined $search_results) {
		return scalar(@{$search_results});
	}
	
	return 0;
}

# Calculates counts for Pathways, Reactions, Proteins and Others from
# scratch.
sub calculate_type_counts {
	my($self) = @_;
	
	my $type_counts = $self->type_counts;
	$type_counts->{"Pathways"} = 0;
	$type_counts->{"Reactions"} = 0;
	$type_counts->{"Proteins"} = 0;
	$type_counts->{"Others"} = 0;
    my $search_results = $self->search_results();
    if (defined $search_results) {
    	my $search_result;
    	my $search_result_type;
    	foreach $search_result (@{$search_results}) {
    		$search_result_type = $self->get_search_result_type($search_result);
    		$type_counts->{$search_result_type}++;
    	}
    }
}

sub get_type_counts {
	my($self) = @_;
	
	return $self->type_counts;
}

sub set_type_counts {
	my($self, $type_counts) = @_;
	
	$self->type_counts($type_counts);
}

# Takes the internally stored search results, filters according to the
# supplied hash of types, and returns a new GKB::SearchUtils::SearchResults
# object containing the filtered results set.  The original type counts
# are passed over to the new SearchResults object.
sub filter_by_type {
	my ($self, $type_selectors) = @_;
	
    my $search_results = $self->search_results();
	my $search_result;
	my @filtered_search_result_array = ();
	my $type;
	foreach $search_result (@{$search_results}) {
		$type = $self->get_search_result_type($search_result);
		if ($type_selectors->{$type}) {
			push(@filtered_search_result_array, $search_result);
			next;
		}
	}
	
	my $filtered_search_results = GKB::SearchUtils::SearchResults->new();
	$filtered_search_results->set_search_results(\@filtered_search_result_array);
	$filtered_search_results->set_type_counts($self->get_type_counts());
	
	return $filtered_search_results;
}

sub get_search_result_type {
	my ($self, $search_result) = @_;

    my $title = $search_result->title();
    my $type = undef;
    if (defined $title && !($title eq '')) {
    	if ($title =~ /^([A-Za-z]+):/) {
    		$type = $1;
    		
    		if ($type eq 'Pathway' || $type eq 'Reaction' || $type eq 'Protein') {
    			return $type . "s";
    		} else {
    			return "Others";
    		}
    	}
    }
    
    return $type;
}

# Take a reference to an array of Reactome instances as an argument,
# and return a reference to an array of corresponding WWW::SearchResult
# objects.  If gk_db_name is not defined, it will be set to the default
# "gk_current" in order to construct result URLs.
sub convert_instances_to_query_results {
	my($self, $instances, $gk_db_name) = @_;
	
	if (!(defined $gk_db_name)) {
		$gk_db_name = "gk_current";
	}
	
	my @query_result_array = ();
	if (defined $instances) {
		my $instance;
		foreach $instance (@{$instances}) {
			push(@query_result_array, $self->convert_instance_to_query_result($instance, $gk_db_name));
		}
	}
	
	return \@query_result_array;
}

# Given a single Reactome instance as an argument, extract the
# relevant attributes and put this information into a WWW::SearchResult
# object, and return this object.
sub convert_instance_to_query_result {
	my($self, $instance, $gk_db_name) = @_;
	
	if (defined $gk_db_name) {
		$gk_db_name = "DB=$gk_db_name&";
	} else {
		$gk_db_name = "";
	}
	
	my $url_base = "/cgi-bin/eventbrowser?$gk_db_name" . "ID=";
	my $url = $url_base . $instance->db_id();
	
	my $instance_class = $INSTANCE_CLASS_NAME_MAP{$instance->class()};
	if (!(defined $instance_class)) {
		$instance_class = $instance->class();
	}
	my $title = "$instance_class: " . $instance->_displayName->[0];
	if ($instance->is_valid_attribute("species") && defined $instance->species && defined $instance->species->[0]) {
		$title .= " (" . $instance->species->[0]->_displayName->[0] . ")";
	}
	
	my $description = $instance->get_description();
	
	my $change_date = undef;
	if (defined $instance->modified && scalar(@{$instance->modified})>0) {
		$change_date = $instance->modified->[scalar(@{$instance->modified}) - 1]->dateTime->[0];
	} elsif (defined $instance->created && scalar(@{$instance->created})>0) {
		$change_date = $instance->created->[0]->dateTime->[0];
	} elsif (defined $instance->_timestamp && scalar(@{$instance->_timestamp})>0) {
		$change_date = $instance->_timestamp->[0];
	}
	
	my $search_result = WWW::SearchResult->new();
	$search_result->url($url);
	$search_result->title($title);
	$search_result->description($description);
	$search_result->change_date($change_date);
	
	return $search_result;
}

1;

