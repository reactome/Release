package GKB::SearchUtils::ResultRenderer;

=head1 NAME

GKB::SearchUtils::ResultRenderer

=head1 SYNOPSIS

Base class for rendering a single search result

=head1 DESCRIPTION

This class provides a public method, "print", which takes
as an argument an object of type WWW::SearchResult, and prints
HTML to STDOUT.

Additional public methods reflect the information stored
in the WWW::SearchResult object, e.g. "print_url", "print_title",
"print_description", etc.  These methods are called by "print"
in order to construct the output.

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
use vars qw(@ISA $AUTOLOAD %ok_field);
use Bio::Root::Root;
use Carp;

@ISA = qw(Bio::Root::Root);

for my $attr
    (qw(
        test
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
    my($pkg, @args) = @_;
    my $self = bless {}, $pkg;
    return $self;
}

sub print {
    my($self, $www_search_result) = @_;
    
    print "<div style=\"padding-left:6em;\">\n";
    if (defined $www_search_result) {
	    $self->print_title($www_search_result);
#	    print "<br>\n"; # comment if title uses div
	    if ($self->print_description($www_search_result)) {
		    print "<br>\n";
#		    $self->print_url($www_search_result);
#		    print "<br>\n";
	    }
	    $self->print_change_date($www_search_result);
    } else {
    	print "WARNING - undefined search result\n";
    }
    print "</div>\n";
}

sub print_title {
    my($self, $www_search_result) = @_;
    
    my $title = $self->create_title($www_search_result);
    
    if (defined $title && !($title eq '')) {
    	print $title;
    }
}

sub create_title {
    my($self, $www_search_result) = @_;
    
    my $title = $www_search_result->title();
    
    if (defined $title && !($title eq '')) {
    	$title = $self->strip_html($title);
    	my $class;
    	my $rendered_class;
    	my $url = $www_search_result->url();
    	if ($title =~ /^([A-Za-z]+):/) {
    		$class = $1;
    		$rendered_class = "<b>$class<\/b>";
    		if ($class eq "Activation") {
    			$rendered_class = $self->create_image_icon("/icons/search/Activation.jpg") . $rendered_class;
    		} elsif ($class eq "Complex") {
    			$rendered_class = $self->create_image_icon("/icons/search/Complex.gif") . $rendered_class;
    		} elsif ($class eq "Compound") {
    			$rendered_class = $self->create_image_icon("/icons/search/Compound.jpg") . $rendered_class;
    		} elsif ($class eq "Gene") {
    			$rendered_class = $self->create_image_icon("/icons/search/Gene.jpg") . $rendered_class;
    		} elsif ($class eq "Inhibition") {
    			$rendered_class = $self->create_image_icon("/icons/search/Inhibition.jpg") . $rendered_class;
    		} elsif ($class eq "Literature") {
    			$rendered_class = $self->create_image_icon("/icons/search/Literature.jpg") . $rendered_class;
    		} elsif ($class eq "Pathway") {
    			$rendered_class = $self->create_image_icon("/icons/search/Pathway.gif") . $rendered_class;
    		} elsif ($class eq "Protein") {
    			$rendered_class = $self->create_image_icon("/icons/search/Protein.jpg") . $rendered_class;
    		} elsif ($class eq "Reaction") {
    			$rendered_class = $self->create_image_icon("/icons/search/Reaction.gif") . $rendered_class;
    		} elsif ($class eq "RNA") {
    			$rendered_class = $self->create_image_icon("/icons/search/RNA.jpg") . $rendered_class;
    		}
    		$title =~ s/^$class/$rendered_class/;
    	}
    	if (defined $url && !($url eq '')) {
    		$title =  "<a href=\"$url\">$title</a>\n";
    	} else {
    		$title =  "$title\n";
    	}
    }
    
    return $title;
}

sub create_image_icon {
    my($self, $image_url) = @_;
    
    return "<img SRC=\"$image_url\" width=\"14\" height=\"13\" /> "
}

# Must return 1 if successful, 0 otherwise (e.g. if no description
# text available).
sub print_description {
    my($self, $www_search_result) = @_;
    
    my $description = $www_search_result->description();
    if (defined $description && !($description eq '')) {
    	$description = $self->strip_html($description);
    	print "$description\n";
    	return 1;
    }
    
    return 0;
}

sub print_url {
    my($self, $www_search_result) = @_;
    
    my $url = $www_search_result->url();
    if (defined $url && !($url eq '')) {
    	print "<a href=\"$url\">$url</a>\n";
    }
}

sub print_change_date {
    my($self, $www_search_result) = @_;
    
    my $change_date = $www_search_result->change_date();
    if (defined $change_date && !($change_date eq '')) {
    	print "<I>Last changed: $change_date</I>\n";
    }
}

# Strip out all HTML tags.
sub strip_html {
    my($self, $text) = @_;
    
    $text =~ s/\<\/{0,1}[a-zA-Z0-9]+\>/ /g;
    
    return($text);
}

# Strip out HTML tags that are used in creating paragraphs,
# such as <br>, <p>, etc.
sub strip_html_para {
    my($self, $text) = @_;
    
    $text =~ s/\<br\>/ /g;
    $text =~ s/\<p\>/ /g;
    $text =~ s/\<\/p\>/ /g;
    
    return($text);
}

1;

