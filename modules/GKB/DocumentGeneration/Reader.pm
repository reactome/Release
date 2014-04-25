package GKB::DocumentGeneration::Reader;

=head1 NAME

GKB::DocumentGeneration::Reader

=head1 SYNOPSIS

Takes some kind of input info and presents it in a text-generation-friendly way.

=head1 DESCRIPTION

Gets assorted book-related information and presents it in a form
that is friendly for document generation, especially book generation.
The "output" from this clas comes in the form of a stream of TextUnit
objects, which encapsulate things like section headers, paragraphs,
etc.

The input could come, for example, from a database, or from a
website, or from a document.

This is intended to be the superclass for any Reader class that you implement.
It supplies a whole bunch of get_* methods that classes inheriting from
GenerateText might use.  There's only one method you really *must* implement
yourself, and that's get_next_text_unit.  This method pulls a TextUnit
object from whatever you are using as an input stream.  The idea is, you
generate a sequence of TextUnit objects representing the parts of the
book, which are then passed on to a GenerateText object to be emited to
an appropriate output stream, e.g. a PDF file.

=head1 SEE ALSO

GKB::DocumentGeneration::GenerateText

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

@ISA = qw(Bio::Root::Root);

my $title;

# List the object variables here, so that they can be checked
for my $attr
    (qw(
    chapter_header
    initial_header
    start_header
    stop_header
    depth_offset
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

# Create a new instance of this class
sub new {
    my($pkg, @args) = @_;
    my $self = bless {}, $pkg;
    
    print STDERR "Reader.new: setting initial depth_offset\n";
    
    my $depth_offset = 0;
    $self->depth_offset($depth_offset);
    
    return $self;
}

# Returns the author(s) of this publication
# Generally, you will need to overwrite this in a subclass, this version
# of the method just returns an empty string.
sub get_author {
    my ($self) = @_;

    return "";
}

# Returns the company or institution primarily responsible for the publication
# Generally, you will need to overwrite this in a subclass, this version
# of the method just returns an empty string.
sub get_company {
    my ($self) = @_;

    return "";
}

# Returns the publication's title
# Generally, you will need to overwrite this in a subclass, this version
# of the method just returns an empty string.
sub get_title {
    my ($self) = @_;

    return $title;
}

# Sets the publication's title
# Generally, you will need to overwrite this in a subclass.
sub set_title {
    my ($self, $external_title) = @_;

    $title = $external_title;
}

# Returns a slightly more detailed description of the publication's contents
# Generally, you will need to overwrite this in a subclass, this version
# of the method just returns an empty string.
sub get_subject {
    my ($self) = @_;

    return "";
}

# Returns the ISDN for the publication
# Generally, you will need to overwrite this in a subclass, this version
# of the method just returns an empty string.
sub get_isdn {
    my ($self) = @_;

    return "";
}

# Returns the copyright terms for the publication
# Generally, you will need to overwrite this in a subclass, this version
# of the method just returns an empty string.
sub get_copyright_conditions {
    my ($self) = @_;

    return "";
}

# Returnsthe preface text.
# Generally, you will need to overwrite this in a subclass, this version
# of the method just returns an empty string.
sub get_preface {
    my ($self) = @_;

    return "";
}

# Get a page of kowtowing acknowledgements
# Generally, you will need to overwrite this in a subclass, this version
# of the method just returns an empty string.
sub get_acknowledgements {
    my ($self) = @_;

    return "";
}

# Get the next text unit from the input stream.
# It would be nice if this worked in its own thread.
# It would be nice if this didn't have to take any arguments.
sub get_next_text_unit {
    my ($self, $depth, $depth_limit,$include_images_flag) = @_;

    print STDERR "get_next_text_unit: this method must be defined in the subclass\n";
    exit;
}

# Invent new section number.  If previous section number was null/empty,
# assume this is a root instance.
sub get_new_section {
    my ($self, $section, $instance_num) = @_;

    my $new_section = $section . "." . $instance_num;

    return $new_section;
}

# Sets a chapter header for the document.  This will be the
# top-level header for that part of the document being dealt
# with by the current Reader object.
sub get_chapter_header {
    my ($self) = @_;

	return $self->chapter_header;
}

# Gets a chapter header for the document.  This will be the
# top-level header for that part of the document being dealt
# with by the current Reader object.
sub set_chapter_header {
    my ($self, $header) = @_;

	if (defined $header && !($header eq '')) {
	    $self->chapter_header($header);
	}
}

# Sets an initial header for the document.  This is used
# when the document starts with text then contains subsequent
# headers.
sub get_initial_header {
    my ($self) = @_;

	return $self->initial_header;
}

# Sets an initial header for the document.  This is used
# when the document starts with text then contains subsequent
# headers.
sub set_initial_header {
    my ($self, $header) = @_;

	if (defined $header && !($header eq '')) {
	    $self->initial_header($header);
	}
}

# Gets the starting point in the document.  Text units will
# be generated from this point on.  May be the full text of
# the header, but Perl regular expressions are also allowed.
sub get_start_header {
    my ($self) = @_;

	return $self->start_header;
}

# Sets the starting point in the document.  Text units will
# be generated from this point on.  May be the full text of
# the header, but Perl regular expressions are also allowed.
sub set_start_header {
    my ($self, $header) = @_;

	if (defined $header && !($header eq '')) {
	    $self->start_header($header);
	    $self->emit_flag(0);
	}
}

# Gets the ending point in the document.  Text units will
# be generated until this point.  May be the full text of
# the header, but Perl regular expressions are also allowed.
sub get_stop_header {
    my ($self) = @_;

	return $self->stop_header;
}

# Sets the ending point in the document.  Text units will
# be generated until this point.  May be the full text of
# the header, but Perl regular expressions are also allowed.
sub set_stop_header {
    my ($self, $header) = @_;

 	if (defined $header && !($header eq '')) {
    	$self->stop_header($header);
 	}
}

# Gets the depth offset - this is added to the depth settings
# of all headers extracted from the source document.
sub get_depth_offset {
    my ($self) = @_;

	return $self->depth_offset;
}

# Sets the depth offset - this is added to the depth settings
# of all headers extracted from the source document.
sub set_depth_offset {
    my ($self, $depth_offset) = @_;

 	if (defined $depth_offset) {
    	$self->depth_offset($depth_offset);
 	}
}

# Adds to the depth offset - this is added to the depth settings
# of all headers extracted from the source document.
sub add_depth_offset {
    my ($self, $depth_offset) = @_;

 	if (defined $depth_offset) {
 		$self->set_depth_offset($self->set_depth_offset + $depth_offset);
 	}
}

# Gets the in-paragraph markup for starting boldface text.
sub get_bold_start_markup {
    my ($self) = @_;

	my $markup = "<b>";
 	
 	return $markup;
}

# Gets the in-paragraph markup for ending boldface text.
sub get_bold_end_markup {
    my ($self) = @_;

	my $markup = "</b>";
 	
 	return $markup;
}

# Gets the in-paragraph markup for starting italic text.
sub get_italic_start_markup {
    my ($self) = @_;

	my $markup = "<i>";
 	
 	return $markup;
}

# Gets the in-paragraph markup for ending italic text.
sub get_italic_end_markup {
    my ($self) = @_;

	my $markup = "</i>";
 	
 	return $markup;
}

# Gets the in-paragraph markup for an image.
sub get_image_markup {
    my ($self, $src, $width, $height) = @_;

	my $markup = '';
 	if (!(defined $src)) {
    	print STDERR "get_next_text_unit: image source file not defined!\n";
 	} else {
 		$markup = "<img src=\"$src\"";
 		if (defined $width) {
 			$markup .= " width=\"$width\"";
 		}
 		if (defined $height) {
 			$markup .= " height=\"$height\"";
 		}
 		$markup .= "/>";
 	}
 	
 	return $markup;
}

1;
