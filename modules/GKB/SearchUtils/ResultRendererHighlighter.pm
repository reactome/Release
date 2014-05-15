package GKB::SearchUtils::ResultRendererHighlighter;

=head1 NAME

GKB::SearchUtils::ResultRendererHighlighter

=head1 SYNOPSIS

Renders a single search result with highlighting.

=head1 DESCRIPTION

Inherits from the "Result" class, provides additional highlighting
functionality.  You need to provide the terms for highlighting
as a reference to an array to the method "set_terms", e.g.

my @terms = ("citric", "acid");
set_terms(\@terms);

These terma are stored inside the "ResultHighlighter" object, so
that you can call the print method of this object multiple times
with different reaults, and the same terms will get highllighted.

=head1 SEE ALSO

GKB::SearchUtils::ResultRenderer
Search::Tools::HiLiter

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
use GKB::SearchUtils::ResultRenderer;
use Search::Tools::HiLiter;
#use Search::Tools::RegExp;

@ISA = qw(GKB::SearchUtils::ResultRenderer);

for my $attr
    (qw(
        terms
        hiliter
        hl
	) ) { $ok_field{$attr}++; }

# These constants control the behaviour of description
# printing.
my $INITIAL_STRING_LENGTH = 100;
my $INDEXED_STRING_LENGTH = 300;
my $INDEXED_TERM_OFFSET = 50;

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
    return $self;
}

sub set_terms {
    my($self, $terms) = @_;
    
    # If we have terms containing spaces, break them up, to
    # make things easier for the highlighter.
    my @all_terms = ();
    my @subterms;
    my $subterm;
    my $term;
    foreach $term (@{$terms}) {
    	@subterms = split(' ', $term);
    	foreach $subterm (@subterms) {
    		if (!($subterm eq '')) {
    			push(@all_terms, $subterm);
    		}
    	}
    }
    $self->terms(\@all_terms);

# RegExp has now been deprecated
#    my $re = Search::Tools::RegExp->new;
#    my $rekw = $re->build(\@all_terms);
# Search::Tools->parser is the new way
# TODO: only the first search term gets highlighted
    my $qparser    = Search::Tools->parser();
    my $rekw      = $qparser->parse(@all_terms);
	my $hiliter = Search::Tools::HiLiter->new('query' => $rekw );
	$self->hiliter($hiliter);
}
    
# Must return 1 if successful, 0 otherwise (e.g. if no description
# text available).
sub print_description {
    my($self, $www_search_result) = @_;
    
    my $terms = $self->terms();
    # If there are no search terms available, don't bother to try highlighting
    if (!defined $terms || scalar(@{$terms})<1) {
    	return $self->SUPER::print_description($www_search_result);
    }
    
    # If the description is essentially the same as the title, don't
    # print it.
    my $description = $www_search_result->description();
    my $title = $www_search_result->title();
    $description =~ s/[ ;\.\n]+$//;
    $title =~ s/[ ;\.\n]+$//;
    if ($title eq $description) {
    	return 0;
    }
    my $regexp_description = $description;
    $regexp_description =~ s/[^A-Za-z0-9_\- ]/./g;
    if ($title =~ /$regexp_description +\([^)]+\)$/) {
    	return 0;
    }

    if (defined $description && !($description eq '')) {
    	if (($INITIAL_STRING_LENGTH + $INDEXED_STRING_LENGTH + 5) < length($description)) {
			my $min_term_index = length($description) + 1;
			my $term_index;
			my $term;
			# Find position of first term in description
			foreach $term (@{$terms}) {
				$term_index = index($description, $term);
				if ($term_index<$min_term_index) {
					$min_term_index = $term_index;
				}
			}
			my $term_start_index = $min_term_index - $INDEXED_TERM_OFFSET;
			if ($term_start_index < $INITIAL_STRING_LENGTH) {
				$description = substr($description, 0, $INITIAL_STRING_LENGTH + $INDEXED_STRING_LENGTH + 5);
			} else {
				my $initial_string = substr($description, 0, $INITIAL_STRING_LENGTH);
				my $term_string = substr($description, $term_start_index, $INDEXED_STRING_LENGTH);
				$description = "$initial_string ... $term_string ...";
			}
    	}
    	$description = $self->strip_html($description);
    	$description = $self->pad_terms($description);
		$description = $self->hiliter->light($description);
    	$description = $self->unpad_terms($description);
    	print "$description\n";
    	
    	return 1;
    }
    
    return 0;
}

sub print_title {
    my($self, $www_search_result) = @_;
    
    my $terms = $self->terms();
    if (!defined $terms || scalar(@{$terms})<1) {
    	$self->SUPER::print_title($www_search_result);
    	return;
    }
    
    my $title = $self->SUPER::create_title($www_search_result);
    if (defined $title && !($title eq '')) {
    	$title =~ s/^(.+: )//;
    	my $type = $1;
    	$title = $self->pad_terms($title);
		$title = $self->hiliter->light($title);
    	$title = $self->unpad_terms($title);
    	print "<div style=\"font-size:11pt;\">$type$title</div>\n";
    }
}

# Adds some padding around the terms found in the supplied description,
# to fool the highlighter into highlighting all instances of the
# terms, even if they are parts of bigger words.
sub pad_terms {
    my($self, $description) = @_;
    
    my $terms = $self->terms;
    my $term;
    foreach $term (@{$terms}) {
    	$description =~ s/($term)([^ ])/$1 ___$2/g;
    	$description =~ s/([^ ])($term)/$1___ $2/g;
    }

    return $description;
}

# Removes padding added by pad_terms
sub unpad_terms {
    my($self, $description) = @_;
    
    $description =~ s/ ___([^ ])/$1/g;
    $description =~ s/([^ ])___ /$1/g;

    return $description;
}

1;

