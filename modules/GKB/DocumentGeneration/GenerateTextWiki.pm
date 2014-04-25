package GKB::DocumentGeneration::GenerateTextWiki;

=head1 NAME

GKB::DocumentGeneration::GenerateTextWiki

=head1 SYNOPSIS

Plugin for producing Wiki pages

=head1 DESCRIPTION

A Perl utility module for the Reactome book package, implementing
GenerateText.  This class provides the specialized methods
needed for producing MediaWiki pages.

=head1 SEE ALSO

GKB::DocumentGeneration::GenerateText

=head1 AUTHOR

David Croft E<lt>croft@ebi.ac.ukE<gt>

Copyright (c) 2007 European Bioinformatics Institute and Cold Spring
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

# Open stream to named output file.
sub open_filename {
    my ($self, $filename) = @_;

	# Open file for holding wiki text
	if (!($filename =~ /\.wiki$/)) {
		$filename .= ".wiki";
	}
	my $filehandle = $self->filehandle;
	if (defined $filehandle) {
		#close old file, if still open
		close($filehandle);
	}
	if (open($filehandle, ">$filename")) {
		$self->filehandle($filehandle);
	} else {
		print STDERR "GenerateTextWiki.open: cannot create file handle for: $filename";
	}
	
	# Create directory for images
	if (!(-e $filename)) {
		mkdir($filename);
	}
}

# Close file handle
sub close {
    my ($self) = @_;

	my $filehandle = $self->filehandle;
	if (defined $filehandle) {
		#close old directory, if still open
		close($filehandle);
		$filehandle = undef;
		$self->filehandle($filehandle);
	}
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
    
    print STDERR "GenerateTextWiki.generate_image: filename=$filename\n";

    # Dump image into temporary file
    my $image_file_name = GKB::FileUtils->print_image($image, $filename);
    if (defined $image_file_name) {
        print STDERR "GenerateTextWiki.generate_image: WARNING - cant do anything with image, skipping\n";
        return;
    }
	$image_file_name =~ s/\/+$//; # remove trailing slashes
	$image_file_name =~ /([^\/]+)$/;
	my $src_filename = $1;
	
	# Move image file to an area associated with
	# the wiki text.
	rename($image_file_name, $self->get_output_file_name() . "/$src_filename");

    if ($image_file_name) {
    	my $new_text .= "[[Image:$src_filename]]";
		my $filehandle = $self->filehandle;
		print $filehandle "$new_text\n";
    }
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
	
	# Deal with formatting
	if ($formatting->{"bold"}) {
		print $filehandle "\'\'\'";
	}
	if ($formatting->{"italic"}) {
		print $filehandle "\'\'";
	}
	if ($formatting->{"underline"}) {
		print $filehandle "_";
	}
		
	# Don't generate the paragraph straight into the
	# output file.  Instead, generate it into a string
	# and then process that string to find embedded
	# markup tags, and dump them to file.
    print $filehandle $self->interpret_markup($text);

	# Deal with formatting
	if ($formatting->{"bold"}) {
		print $filehandle "\'\'\'";
	}
	if ($formatting->{"italic"}) {
		print $filehandle "\'\'";
	}
	if ($formatting->{"underline"}) {
		print $filehandle "_";
	}

    print $filehandle "\n";

    $self->set_first_page_flag(0);
}

sub generate_bullit_text {
    my ($self, $text) = @_;

	my $filehandle = $self->filehandle;
	
    print $filehandle "*$text\n";
}

sub generate_numbered_text {
    my ($self, $text, $number) = @_;

	my $filehandle = $self->filehandle;
	
    print $filehandle "#$text\n";
}

# Treat the supplied text as a header.  Font size is adjusted
# according to depth in event hierarchy, the deeper, the smaller.
# Arguments:
#
# depth - depth in event hierarchy
# text - string, containnig header (including section number, if required)
#
# This method overwrites the superclass method, in order to be able to
# add header stylesheet info.
sub generate_header {
    my ($self, $depth, $text) = @_;

	my $filehandle = $self->filehandle;
	
    $self->generate_header_initial_whitespace($depth, $text);

	my $i;
	
	# markup for header
	for ($i=0; $i<$depth + 1; $i++) {
		print $filehandle "=";
	}
	
    print $filehandle "$text";

	# markup for header
	for ($i=0; $i<$depth + 1; $i++) {
		print $filehandle "=";
	}
	
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
			$line = "<$line";
		} else {
			$initial_backarrow_flag = 1;
		}
		$final_arrow_flag = 0;
		if ($line =~ />$/) {
			$final_arrow_flag = 1;
		}
		my @lines2 = split(/>/, $line);
		foreach $line (@lines2) {
			$lines3[$lines3_counter] = "$line>";
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
				# An embedded image
				$line =~ /src="([^"]+)"/;
				$src = $1;
				
				if (!(defined $src) || $src eq '') { #'
					print STDERR "GenerateTextWiki.interpret_markup: cannot find image source file, skipping to next line\n";
					next;
				}
				if (!(-e $src)) {
					print STDERR "GenerateTextWiki.interpret_markup: we cant see the file $src, skipping to next line\n";
					next;
				}
				
				# We could use this, but we don't right now:
				$line =~ /width="([^"]+)"/; #"
				$width = $1;
				$line =~ /height="([^"]+)"/; #"
				$height = $1;

				$src =~ s/\/+$//; # remove trailing slashes
				$src =~ /([^\/]+)$/;
				$src_filename = $1;
    			$new_text .= "[[Image:$src_filename]]";

				# TODO: we need to copy the file somewhere where it will
				# be accessible to Wiki as well!
				
#				unlink($src); # don't need anymore
			} elsif ($line =~ /^<b>$/) {
				# Start bold
				$new_text .= "\'\'\'";
			} elsif ($line =~ /^<\/b>$/) {
				# Stop bold
				$new_text .= "\'\'\'";
			} elsif ($line =~ /^<i>$/) {
				# Start italics
				$new_text .= "\'\'";
			} elsif ($line =~ /^<\/i>$/) {
				# Stop italics
				$new_text .= "\'\'";
			}
		} else {
			# We have plain text
			$new_text .= $line;
		}
	}
	
	print STDERR "GenerateTextWiki.interpret_markup: new_text=$new_text\n";
	
	return $new_text;
}

1;
