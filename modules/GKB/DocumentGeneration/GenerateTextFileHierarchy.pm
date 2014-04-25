package GKB::DocumentGeneration::GenerateTextFileHierarchy;

=head1 NAME

GKB::DocumentGeneration::GenerateTextFileHierarchy

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
	$ok_field{"filename"}++;
	$ok_field{"current_filehandle"}++;
	$ok_field{"section_heading_path"}++;
	$ok_field{"current_section_heading_depth"}++;

	return %ok_field;
}

# Open stream to file
sub open_stream {
    my ($self, $stream) = @_;

    print STDERR "GenerateTextFileHierarchy.open_stream: WARNING - output to stream is not available for this text generator\n";
}

# Open stream to named output file.
sub open_filename {
    my ($self, $filename) = @_;

    print STDERR "GenerateTextFileHierarchy.open: opening filename=$filename\n";

    $self->filename($filename);

    # Create directory for hierarchy
    my $directory = $filename;
    $directory =~ s/\.txt$//;
    if (!(-e $directory)) {
        print STDERR "GenerateTextFileHierarchy.open_filename: making directory=$directory\n";

        mkdir($directory);
    }

    print STDERR "GenerateTextFileHierarchy.open: done\n";
}

# Close file handle
sub close {
    my ($self) = @_;

    print STDERR "GenerateTextFileHierarchy.close: closing filehandle\n";

    my $current_filehandle = $self->current_filehandle;
    if (defined $current_filehandle) {
        close($current_filehandle);
    }

    my $directory = $self->filename;
    $directory =~ s/\.txt$//;
    print STDERR "GenerateTextFileHierarchy.close: directory=$directory\n";
    $self->clean_up($directory);

    print STDERR "GenerateTextFileHierarchy.close: done\n";
}

# Clean up empty files and directories
sub clean_up {
    my ($self, $directory) = @_;

    print STDERR "GenerateTextFileHierarchy.clean_up: entered, directory=$directory\n";

    if (opendir (DIR, $directory)) {
        my $file_counter = 0;
        print STDERR "GenerateTextFileHierarchy.clean_up: about to do a readdir, directory=$directory\n";
        my @files = readdir(DIR);
        closedir(DIR);
#        while (my $file = readdir(DIR)) {
        foreach my $file (@files) {
            print STDERR "GenerateTextFileHierarchy.clean_up: file=$file\n";
            if ($file eq "." || $file eq "..") {
                next;
            }
            $file_counter++;
            my $path = $directory . "/" . $file;
            if (-f $path) {
                # Regular file - delete if it has zero size.
                my $filesize = -s $path;
                if ($filesize == 0) {
                    unlink($path);
                }
            } else {
                # Directory - recurse down and do a clean up at the
                # next level of the hierarchy.
                $self->clean_up($path);
            }
        }

        # Get rid of empty directories.
        if ($file_counter == 0) {
            rmdir($directory);
        }
    } else {
        print STDERR "GenerateTextFileHierarchy.clean_up: WARNING - could not open directory=$directory\n";
    }

    print STDERR "GenerateTextFileHierarchy.clean_up: done\n";
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
    
    print STDERR "GenerateTextFileHierarchy.generate_image: ignoring image filename=$filename\n";
}

# Dummy - doesn't actually do anything
sub generate_page_break {
    my ($self) = @_;

}

# Treat the supplied text as a header.  Font size is adjusted
# according to depth in event hierarchy, the deeper, the smaller.
# Arguments:
#
# depth - depth in event hierarchy
# text - string, containnig header (including section number, if required)
# formatting (optional) - formatting directives for the output text
sub generate_header {
    my ($self, $depth, $text) = @_;

    my $directory_name = $self->convert_header_to_directory_name($text);
    my $current_section_heading_depth = $self->current_section_heading_depth;
    my $section_heading_path = $self->section_heading_path;
    if ($depth <= $current_section_heading_depth) {
        for (my $i=$depth; $i<=$current_section_heading_depth; $i++) {
            pop(@{$section_heading_path});
        }
    }
    push(@{$section_heading_path}, $directory_name);
    $self->section_heading_path($section_heading_path);

    my $output_file_name = $self->get_output_file_name();
    $output_file_name =~ s/\.txt$//;
    my $file_path = $output_file_name . "/" . join("/", @{$section_heading_path});
    my $file_name = $file_path . ".txt";

    print STDERR "GenerateTextFileHierarchy.generate_header: about to mkdir file_path=$file_path\n";

    mkdir($file_path);

    print STDERR "GenerateTextFileHierarchy.generate_header: depth=$depth, current_section_heading_depth=$current_section_heading_depth, text=$text\n";

    my $current_filehandle = $self->current_filehandle;
    if (defined $current_filehandle) {
        CORE::close($current_filehandle);
    }
    if (open($current_filehandle, ">$file_name")) {
        $self->current_filehandle($current_filehandle);
    } else {
    	print STDERR "GenerateTextFileHierarchy.generate_header: cannot create file handle for: $file_name\n";
        $self->current_filehandle(undef);
    }

    $self->current_section_heading_depth($depth);

#    return $self->SUPER::generate_header($depth, $text);
    return 0;
}

sub convert_header_to_directory_name {
    my ($self, $text) = @_;

    my $directory_name = $text;
    $directory_name =~ s/[^a-zA-Z0-9]+/_/g;

    return $directory_name;
}

# Emits a block of text.
# Arguments:
#
# text - string to be emited
# formatting - reference to a hash containing format info
sub generate_paragraph {
    my ($self, $text, $formatting) = @_;

    $text = $self->interpret_markup($text);

	my $filehandle = $self->current_filehandle;

	if (!(defined $filehandle)) {
	    print STDERR "GenerateTextFileHierarchy.generate_paragraph: WARNING - filehandle is undef for text=$text, Im outta here!\n";
	    return;
	}
	
	# Ignore formatting
		
	# Don't generate the paragraph straight into the
	# output file.  Instead, generate it into a string
	# and then process that string to find embedded
	# markup tags, and dump them to file.
    my $interpreted_text = $self->interpret_markup($text);
    print STDERR "GenerateTextFileHierarchy.generate_paragraph: interpreted_text=$interpreted_text\n";
    print $filehandle $interpreted_text;
    print STDERR "interpreted_text=$interpreted_text\n";

	# Ignore formatting

    print $filehandle "\n";

    $self->set_first_page_flag(0);
}

sub generate_bullit_text {
    my ($self, $text) = @_;

	my $filehandle = $self->current_filehandle;
	
    print $filehandle "* $text\n";
}

sub generate_numbered_text {
    my ($self, $text, $number) = @_;

	my $filehandle = $self->current_filehandle;
	
    print $filehandle "$number. $text\n";
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
			if ($lines3_counter > 0) {
			    $lines3[$lines3_counter-1] =~ s/>$//;
			}
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
	
	print STDERR "GenerateTextFileHierarchy.interpret_markup: new_text=$new_text\n";
	
	return $new_text;
}

1;
