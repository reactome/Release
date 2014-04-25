package GKB::DocumentGeneration::GenerateTextTxt;

=head1 NAME

GKB::DocumentGeneration::GenerateTextTxt

=head1 SYNOPSIS

Plugin for producing Wiki pages

=head1 DESCRIPTION

A Perl utility module for the Reactome book package, implementing
GenerateText.  This class provides the specialized methods
needed for producing plain text (.txt) pages.

=head1 SEE ALSO

GKB::DocumentGeneration::GenerateText

=head1 AUTHOR

David Croft E<lt>croft@ebi.ac.ukE<gt>

Copyright (c) 2013 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

use vars qw(@ISA $AUTOLOAD %ok_field);
use strict;

use GKB::DocumentGeneration::GenerateText;

@ISA = qw(GKB::DocumentGeneration::GenerateText);

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
# Is this the right thing to do for a subclass?
sub new {
    my($pkg, @args) = @_;
    
   	# Get class variables from superclass and define any new ones
   	# specific to this class.
	$pkg->get_ok_field();

   	my $self = $pkg->SUPER::new();
    
    return $self;
}

# Needed by subclasses to gain access to class variables defined in
# this base class.
sub get_ok_field {
	my ($pkg) = @_;
	
	%ok_field = $pkg->SUPER::get_ok_field();
	$ok_field{"filehandle"}++;

	return %ok_field;
}

# Open stream to file
sub open_stream {
    my ($self, $stream) = @_;

    # If the user wants to dump to a stream, dump first to a
    # temporary file...
    if (defined $stream) {
		my $filename = rand() * 1000 . $$;
    	$self->open_filename($filename);
    }
}

# Open stream to named output file.
sub open_filename {
    my ($self, $filename) = @_;

	print STDERR "GenerateTextTxt.open: opening filename=$filename\n";

	# Open file for holding plain text
	if (!($filename =~ /\.txt$/)) {
		$filename .= ".txt";
	}
	my $filehandle = $self->filehandle;
	if (defined $filehandle) {
		#close old file, if still open
		close($filehandle);
	}
	if (open($filehandle, ">$filename")) {
		$self->filehandle($filehandle);
	} else {
		print STDERR "GenerateTextTxt.open: cannot create file handle for: $filename\n";
	}
	
	# Create directory for images
	if (!(-e $filename)) {
		mkdir($filename);
	}

	print STDERR "GenerateTextTxt.open: done\n";
}

# Close file handle
sub close {
    my ($self) = @_;

	print STDERR "GenerateTextTxt.close: closing filehandle\n";

	my $filehandle = $self->filehandle;
	if (defined $filehandle) {
		print STDERR "GenerateTextTxt.close: filehandle exists\n";

		#close old directory, if still open
		close($filehandle);
		$filehandle = undef;
		$self->filehandle($filehandle);
	}

	print STDERR "GenerateTextTxt.close: done\n";
}

# Document meta-data
sub generate_prolog {
    my ($self, $author, $company, $title, $subject) = @_;
}

# Dummy - doesn't actually do anything
sub generate_page_numbering {
    my ($self, $starting_page_number) = @_;
}

# Dummy - doesn't actually do anything
# Arguments:
#
# toc_depth - amount of nesting in TOC.  Zero or negative values nest right
#             down to the bottom of the hierarchy.
sub generate_toc {
    my ($self, $toc_depth) = @_;

}

# Emit a paragraph containing the supplied image file
sub generate_image {
    my ($self, $image, $filename) = @_;
    
    print STDERR "GenerateTextTxt.generate_image: ignoring image filename=$filename\n";
}

# Dummy - doesn't actually do anything
sub generate_page_break {
    my ($self) = @_;

}

# Emits a block of text.
# Arguments:
#
# text - string to be emited
# formatting - reference to a hash containing format info
sub generate_paragraph {
    my ($self, $text, $formatting) = @_;

    $text = $self->interpret_markup($text);

	my $filehandle = $self->filehandle;
	
	# Ignore formatting
		
	# Don't generate the paragraph straight into the
	# output file.  Instead, generate it into a string
	# and then process that string to find embedded
	# markup tags, and dump them to file.
    my $interpreted_text = $self->interpret_markup($text);
    print $filehandle $interpreted_text;
    print STDERR "interpreted_text=$interpreted_text\n";

	# Ignore formatting

    print $filehandle "\n";

    $self->set_first_page_flag(0);
}

sub generate_bullit_text {
    my ($self, $text) = @_;

	my $filehandle = $self->filehandle;
	
    print $filehandle "* $text\n";
}

sub generate_numbered_text {
    my ($self, $text, $number) = @_;

	my $filehandle = $self->filehandle;
	
    print $filehandle "$number. $text\n";
}

# Treat the supplied text as a header.
# Arguments:
#
# depth - depth in event hierarchy
# text - string, containnig header (including section number, if required)
#
sub generate_header {
    my ($self, $depth, $text) = @_;

	my $filehandle = $self->filehandle;
	
    $self->generate_header_initial_whitespace($depth, $text);

    print $filehandle "$text";

	print $filehandle "\n";
	
    $self->generate_vertical_space(1);
}

# Take any markup in the input text and convert it into
# something that will be understood by the wiki interpreter
sub interpret_markup {
    my ($self, $text) = @_;

	my $rtf_string;
	# Break up text to extract potential markup
	my $initial_backarrow_flag = 0;
	if ($text =~ /^</) {
		$initial_backarrow_flag = 1;
	}
	my $final_arrow_flag = 0;
	my @lines = split(/</, $text);
	my @lines3 = ();
	my $lines3_counter = 0;
	my $line;
	foreach $line (@lines) {
		if ($initial_backarrow_flag) {
			;
		} else {
			$initial_backarrow_flag = 1;
		}
		$final_arrow_flag = 0;
		if ($line =~ />$/) {
			$final_arrow_flag = 1;
		}
		my @lines2 = split(/>/, $line);
		foreach $line (@lines2) {
			$lines3[$lines3_counter] = "$line";
			$lines3_counter++;
		}
		if (!$final_arrow_flag) {
			$lines3[$lines3_counter-1] =~ s/>$//;
		}
	}
	
	# lines3 has lines containing either plain text or
	# markup, not mixed.  We can now loop through it
	# and substitute any wiki markup needed.
	my $src;
	my $src_filename;
	my $width;
	my $height;
	my $new_text = '';
	my $i;
	foreach $line (@lines3) {
		if ($line =~ /^</) {
			# We have markup
			if ($line =~ /^<img /) {
				;
			} elsif ($line =~ /^<b>$/) {
				# Start bold
				;
			} elsif ($line =~ /^<\/b>$/) {
				# Stop bold
				;
			} elsif ($line =~ /^<i>$/) {
				# Start italics
				;
			} elsif ($line =~ /^<\/i>$/) {
				# Stop italics
				;
			}
		} else {
			# We have plain text
			$new_text .= $line;
		}
	}
	
	print STDERR "GenerateTextTxt.interpret_markup: new_text=$new_text\n";
	
	return $new_text;
}

1;
