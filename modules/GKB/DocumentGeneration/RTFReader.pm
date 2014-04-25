package GKB::DocumentGeneration::RTFReader;

=head1 NAME

GKB::RTFReader

=head1 SYNOPSIS

Takes info from plain text and presents it in a text-generation-friendly way.

=head1 DESCRIPTION

Gets assorted book-related information from a text URL or string
and presents it in a form
that is friendly for text generation, especially book generation.

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

use LWP::UserAgent;
use RTF::Tokenizer;
use GKB::Config;
use GKB::DocumentGeneration::TextUnit;
use GKB::DocumentGeneration::Reader;
use Storable qw(nstore retrieve);
use Data::Dumper;

@ISA = qw(GKB::DocumentGeneration::Reader);

# List the object variables here, so that they can be checked
for my $attr
    (qw(
    tokenizer
    is_header
    header_level
    is_paragraph
	) ) { $ok_field{$attr}++; }

sub AUTOLOAD {
    my $self = shift;
    my $attr = $AUTOLOAD;
    $attr =~ s/.*:://;
    return unless $attr =~ /[^A-Z]/;  # skip DESTROY and all-cap methods
#    $self->throw("invalid attribute method: ->$attr()") unless $ok_field{$attr};
    $self->{$attr} = shift if @_;
    return $self->{$attr};
}

# Create a new instance of this class
sub new {
    my($pkg, @args) = @_;
    my $self = bless {}, $pkg;
    
    # It's a drag that we have to do this again here
    my $depth_offset = 0;
    $self->depth_offset($depth_offset);
    
    $self->emit_flag(1);
	
    return $self;
}

# Given a URL as an argument, fetch the HTML
sub init_from_file {
    my ($self, $file) = @_;

    my $tokenizer = RTF::Tokenizer->new(file => $file, sloppy => 1);
    $self->tokenizer($tokenizer);
    $self->is_header(0);
    $self->is_paragraph(0);
}

# Get the next text unit from the input stream.
# The depth and depth_limit arguments are ignored.
# If include_images_flag is 0, then images will
# not be returned.
sub get_next_text_unit {
    my ($self, $depth, $depth_limit) = @_;
    
    my $text_unit = $self->read_tokens();

    return $text_unit;
}

sub read_tokens {
    my ($self) = @_;
    
    my $tokenizer = $self->tokenizer;

    my ($token_type, $argument, $parameter) = $tokenizer->get_token();

    print STDERR "RTFReader.get_next_text_unit: token_type=$token_type, argument=$argument, parameter=$parameter\n";

    my $text_unit = GKB::DocumentGeneration::TextUnit->new();
    $text_unit->set_type("empty");
    if ($token_type eq "control" && $argument eq "pard") {
        $self->is_paragraph(1);
        $text_unit = $self->read_tokens();
    } elsif ($token_type eq "control" && $argument eq "s" && $self->is_paragraph) {
        $self->is_header(1);
        $self->header_level($parameter);
        $text_unit = $self->read_tokens();
    } elsif ($token_type eq "text") {
        my $is_header = $self->is_header;
        my $is_paragraph = $self->is_paragraph;
        if ($is_header) {
            print STDERR "RTFReader.get_next_text_unit: we have a header, header_level=" . $self->header_level . "\n";
            $text_unit->set_type("section_header");
            $text_unit->set_depth($self->header_level + 1);
            $text_unit->set_contents($argument);
            $self->is_header(0);
            $self->is_paragraph(0);
        } elsif ($is_paragraph) {
            print STDERR "RTFReader.get_next_text_unit: we have a body_text_paragraph\n";
            $text_unit->set_type("body_text_paragraph");
            $text_unit->set_contents($argument);
        } else {
            print STDERR "RTFReader.get_next_text_unit: we have an unknown, continuing to parse\n";
            # Unknown text token type - carry on parsing
            $text_unit = $self->read_tokens();
        }
    } elsif ($token_type eq "eof") {
        $text_unit->set_type("eof");
    } else {
        $text_unit = $self->read_tokens();
    }

    return $text_unit;
}

1;
