package GKB::HTMLUtils;

=head1 NAME

GKB::HTMLUtils

=head1 SYNOPSIS

Extract information from HTML

=head1 DESCRIPTION

A Perl utility module for the Reactome book package, providing specialized
methods for extracting information from HTML text.

=head1 SEE ALSO


=head1 AUTHOR

David Croft E<lt>croft@ebi.ac.ukE<gt>

Copyright (c) 2005 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

use vars qw(@ISA $AUTOLOAD %ok_field);
use strict;
use Bio::Root::Root;

@ISA = qw(Bio::Root::Root);

sub AUTOLOAD {
    my $self = shift;
    my $attr = $AUTOLOAD;
    $attr =~ s/.*:://;
    return unless $attr =~ /[^A-Z]/;  # skip DESTROY and all-cap methods
    $self->throw("invalid attribute method: ->$attr()") unless $ok_field{$attr};
    $self->{$attr} = shift if @_;
    return $self->{$attr};
}  

# Create a new instance of this class
sub new {
    my($pkg, @args) = @_;
    my $self = bless {}, $pkg;
}


# Find all paragraphs in the supplied HTMl document with more than
# min_size characters.
# Arguments:
#
# content - the web page ASCII content
# min_size - minimum alowed paragraph size (# of characters)
# include_breaks - if set to 1, then <br> will also be treated as a paragraph sparator
#
# Returns a list of paragraphs found, filtered free of HTML tags..
sub extract_paragraphs_from_html {
    my ($self, $content, $min_size, $include_breaks) = @_;

    my @paragraph_list = ();

    if (!(defined $content) || $content eq "") {
#    print STDERR "HTMLUtils.extract_paragraphs_from_web_page: WARNING - empty web page content!!\n";
    return @paragraph_list;
    }

    # Use the HTML <p> and </p> tags to split text into paragraphs
    my @paragraph_arr1 = split(/\<\/*p[ \/]*\>/i, $content);

    # That's it if we are not considering <br> as paragraph separators
    unless ($include_breaks) {
    for (my $j=0; $j<scalar(@paragraph_arr1); $j++) {
        # Remove HTML tags
        $paragraph_arr1[$j] = remove_html_tags($self, $paragraph_arr1[$j]);
        # Remove starting and trailing white space
        $paragraph_arr1[$j] = remove_white_padding($self, $paragraph_arr1[$j]);
    }

    return @paragraph_arr1;
    }

    # <br> are paragraph separators, so now do some more decomposition
    my @paragraph_arr2;
    my $paragraph;
    for (my $i=0; $i<scalar(@paragraph_arr1); $i++) {
    $paragraph = $paragraph_arr1[$i];
    @paragraph_arr2 = split(/\<\/*br[ \/]*\>/i, $paragraph);

    for (my $j=0; $j<scalar(@paragraph_arr2); $j++) {
        $paragraph = $paragraph_arr2[$j];

        # Remove HTML tags
        $paragraph = remove_html_tags($self, $paragraph);
        # Remove starting and trailing white space
        $paragraph = remove_white_padding($self, $paragraph);

        # Ignore short paragraphs
        if (length($paragraph) <= $min_size) {
#        print STDERR "extract_paragraphs_from_web_page: paragraph too short(is " . length($paragraph) . ", should be $min_size), rejecting\n";
        next;
        }

        # Ignore empty paragraphs
        unless ($paragraph) {
        print STDERR "HTMLUtils.extract_paragraphs_from_web_page: WARNING - something went wrong while trying to parse web page, $paragraph_arr2[0]=", $paragraph_arr2[0], "\n";
        next;
        }

        push @paragraph_list, $paragraph;
    }
    }

    return @paragraph_list;
}

# Given start and stop points within $content, extract all text between
# them.
# Arguments:
#
# content - the web page ASCII content
# start_string - start point - no regular expressions allowed!
# end_string - end point - no regular expressions allowed!
#
# Returns the chunk of text found.
sub extract_chunk_from_html {
    my ($self, $content, $start_string, $end_string) = @_;

    my $start_chunk = $content;

    if (!(defined $content) || $content eq '') {
#        print STDERR "HTMLUtils.extract_chunk_from_html: WARNING - empty web page content!!\n";
        return $start_chunk;
    }

    if (defined $start_string && !($start_string eq '')) {
        my @line_arr1 = split($start_string, $content);
    
        unless ($line_arr1[1]) {
            print STDERR "HTMLUtils.extract_chunk_from_html: WARNING - could not find $start_string in web page content!!\n";
            return $start_chunk;
        }

        $start_chunk = $line_arr1[1];
    }

    my $end_chunk = $start_chunk;
    if (defined $end_string && !($end_string eq '')) {
        my @line_arr2 = split($end_string, $start_chunk);
        $end_chunk = $line_arr2[0];
    }

    # Remove starting and trailing white space
    $end_chunk = remove_white_padding($self, $end_chunk);

    unless ($end_chunk) {
        print STDERR "HTMLUtils.extract_chunk_from_html: WARNING - something went wrong while trying to parse web page\n";
        return $start_chunk;
    }

    return $end_chunk;
}


# Given start and stop points within $content, extract all lines between
# them.
# Arguments:
#
# content - the web page ASCII content
# start_string - start point - no regular expressions allowed!
# end_string - end point - no regular expressions allowed!
#
# Returns a list of lines found.
sub extract_lines_from_html {
    my ($self, $content, $start_string, $end_string) = @_;

    my @line_list = ();

    if (!(defined $content) || $content eq '') {
#        print STDERR "HTMLUtils.extract_lines_from_web_page: WARNING - empty web page content!!\n";
        return @line_list;
    }

    my $end_chunk = extract_chunk_from_html($self, $content, $start_string, $end_string);

    unless ($end_chunk) {
        print STDERR "HTMLUtils.extract_lines_from_web_page: WARNING - something went wrong while trying to parse web page\n";
        return @line_list;
    }

    @line_list = split(/\n+/, $end_chunk);
    my $line_count = scalar(@line_list);

    # Clean up a bit
    for (my $i=0; $i<$line_count; $i++) {
        # remove leading and trailing spaces
        $line_list[$i] = remove_white_padding($self, $line_list[$i]);
    }

    return @line_list;
}

# Removes all HTML tags from the supplid text
# Arguments:
#
# text - the input string
#
# Returns input string, free of nasty HTML tags
sub remove_html_tags {
    my ($self, $text) = @_;

    $text =~ s/\<[^\>]*\>//g;

    return $text;
}

# Removes starting and trailing white space from the supplid text
# Arguments:
#
# text - the input string
#
# Returns input string, free of nasty white padding
sub remove_white_padding {
    my ($self, $text) = @_;

    $text =~ s/^[\s\n]+//;
    $text =~ s/[\s\n]+$//;
    $text =~ s/\n+$//;

    return $text;
}

1;
