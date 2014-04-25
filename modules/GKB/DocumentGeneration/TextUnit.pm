package GKB::DocumentGeneration::TextUnit;

=head1 NAME

GKB::DocumentGeneration::TextUnit

=head1 SYNOPSIS

Unit of textual or image information to be emitted to a document

=head1 DESCRIPTION

Stores a unit of text or image and provides information on the type of stored information, so that it can be emited correctly.

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

# Make a note of local variable names
for my $attr
    (qw(
	depth
	number
	url
	contents
	type
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

# Create a new instance of this class.  You may optionally
# supply arguments type and content (in that order).
sub new {
    my($pkg, @args) = @_;
    my $self = bless {}, $pkg;

    my ($type, $contents) = @args;

	if (defined $type) {
	    print STDERR "TextUnit->new: type=$type\n";

    	$self->type($type);
	}
	if (defined $contents) {
	    print STDERR "TextUnit->new: contents=$contents\n";

    	$self->contents($contents);
	}
    
    my $depth = 0;
    $self->depth($depth);

	return $self;
}

# Gets the contents (text or whatever) contained in this object.
sub get_contents {
    my ($self) = @_;

    return $self->contents;
}

# Sets the contents (text or whatever) contained in this object.
sub set_contents {
    my ($self, $contents) = @_;

    $self->contents($contents);
}

# Gets the type of the text unit.
sub get_type {
    my ($self) = @_;

    return $self->type;
}

# Sets the type (e.g. "section_header", "body_text_paragraph", "body_text", "bullit_text", "image", "eof", etc.)
sub set_type {
    my ($self, $type) = @_;

    $self->type($type);
}

# Gets the depth in the section heading hierarchy, or the
# degree of indentation of a bullit point.  Starts at 0.
sub get_depth {
    my ($self) = @_;

    return $self->depth;
}

# Sets the depth in the section heading hierarchy, or the
# degree of indentation of a bullit point.  Starts at 0.
sub set_depth {
    my ($self, $depth) = @_;

    $self->depth($depth);
}

# Gets the number in the bullit list
sub get_number {
    my ($self) = @_;

    return $self->number;
}

# Sets the number in the bullit list
sub set_number {
    my ($self, $number) = @_;

    $self->number($number);
}

# Gets a URL
sub get_url {
    my ($self) = @_;

    return $self->url;
}

# Sets a URL
sub set_url {
    my ($self, $url) = @_;

    $self->url($url);
}

# Returns 1 if the supplied type corresponds to the type of this
# object, 0 otherwise.
sub isa {
    my ($self, $type) = @_;

    if (defined $type && defined $self->type && $type eq $self->type) {
	return 1;
    }

    return 0;
}

# Null the contents of this object.  Use this method
# to free memory once you are certain that the object
# is no longer needed.
sub destroy {
    my ($self) = @_;

    $self->set_contents(undef);
    $self->set_type(undef);
    $self->set_depth(undef);
}

1;

