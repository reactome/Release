package GKB::DocumentGeneration::GenerateTextRTF;

=head1 NAME

GKB::DocumentGeneration::GenerateTextRTF

=head1 SYNOPSIS

Plugin for producing RTF

=head1 DESCRIPTION

A Perl utility module for the Reactome book package, implementing
GenerateText.  This class provides the specialized methods
needed for producing RTF.

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
use Tie::File;
use RTF::Writer;
use Data::Dumper;
use CGI::Carp qw/fatalsToBrowser/;

use GKB::Config;
use GKB::DocumentGeneration::GenerateText;
use Log::Log4perl qw/get_logger/;
Log::Log4perl->init(\$LOG_CONF);

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

sub new {
    my($pkg) = @_;

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
    $ok_field{"rtf"}++;

    return %ok_field;
}

# Open stream to RTF file
sub open_stream {
    my ($self, $stream) = @_;

    $self->rtf(RTF::Writer->new_to_filehandle($stream));
}

# Open stream to named output file.
sub open_filename {
    my ($self, $filename) = @_;

    # Add an appropriate extension, if it doesn't already exist
    unless ($filename =~ /\.rtf$/i) {
       $filename .= ".rtf";
    }

    $self->rtf(RTF::Writer->new_to_file($filename));
}

# Close stream to RTF file
sub close {
    my ($self) = @_;

    my $rtf = $self->rtf;

    my $output_file_stream = $self->get_output_file_stream();

    $rtf->print(\'}'); # Close group opened by prolog

    $rtf->close;
}

# The splitting of the RTF is done in a hackish afterthought
# kind of way because the RTF::Writer source looks too scary
# to mess with.  Basically, we take the generated o/p file,
# scan through it looking for RTF tags indicating chapter
# starts, and dump each chapter into a new file.
#
# PROBLEM!!!!!
#
# The read operation on the original RTF file stops at an arbitrary
# point, so that this subroutine isn't much use.  I tried using
# both the regular Perl open/close and the Tie::File ways of
# doing this, but got the same result.
sub split_op_file_into_smaller_files {
    my ($self) = @_;
    
    my $logger = get_logger(__PACKAGE__);
    
    $logger->info("entered");

    my $output_file_name = $self->get_output_file_name();
    # Add an appropriate extension, if it doesn't already exist
    unless ($output_file_name =~ /\.rtf$/i) {
	$output_file_name .= ".rtf";
    }
	
    # First get the stuff that needs to be appended to all
    # files that get generated.
    my $header1 = "";
    my $ok_flag = 1;
    my $line;
    CORE::open(FILE, $output_file_name);
    while (<FILE>) {
	my $line = $_;
	
#	if ($_ =~ /info/) {
#		$ok_flag = 0;
#	} elsif ($_ =~ /stylesheet/) {
#		$ok_flag = 1;
#	} elsif ($_ =~ /(.*)}}/) {
#		$header1 .= $1;
#		last;
#	}
	if ($line =~ /(.*)}}/) {
	    $header1 .= $1;
	    last;
	}
		
#	if ($ok_flag) {
	    $header1 .= $line;
#	}
    }
    CORE::close(FILE);
	
    my $tail1 = "\\par}\n\n}\n";
    
    my @tie_array;
    tie @tie_array, 'Tie::File', $output_file_name, memory => 0, mode => 0 or print STDERR "split_op_file_into_smaller_files: oh horrors, could not open $output_file_name with Tie::File!!\n";
	
    # Now break into separate files, chapter by chapter, using page
    # breaks to detect chapter boundaries
    my $split_file_counter = 0;
    my $split_file_name;
    my $split_filehandle = undef;
    my $new_file_flag = 1;
    my $skip_page_break_count = 3;
	
    foreach $line (@tie_array) {
	if ($new_file_flag || ($line =~ /^\\page$/ && $skip_page_break_count<=0)) {
	    # Found a page boundary; open new file and close previous
	    # one.
	    if (defined $split_filehandle) {
		print $split_filehandle $tail1;
		CORE::close($split_filehandle);
	    }
			
	    $split_file_counter++;
	    if ($output_file_name =~ /\.rtf$/i) {
		$split_file_name = $output_file_name;
		$split_file_name =~ s/\.rtf$/.$split_file_counter.rtf/i;
	    } else {
		$split_file_name = "$output_file_name.$split_file_counter";
	    }
	    if (!($split_file_name =~ /\.rtf$/i)) {
		$split_file_name .= ".rtf";
	    }

	    my $open_flag = CORE::open($split_filehandle, ">$split_file_name");
    
	    if ($new_file_flag) {
		print $split_filehandle $line;
	    } else {
		print $split_filehandle $header1;
	    }
    
	    $new_file_flag = 0;
	} else {
	    print $split_filehandle $line;
	}
	if ($line =~ /^\\page$/) {
	    $skip_page_break_count--;
	}
    }

    print $split_filehandle $tail1;
    CORE::close($split_filehandle);
    untie @tie_array;
}

# Document meta-data
sub generate_prolog {
    my ($self, $author, $company, $title, $subject) = @_;

    my $rtf = $self->rtf;

    # $rtf->prolog(
	# 	 'author' =>  $author,
	# 	 'company' =>  $company,
	# 	 'title' =>  $title ,
	# 	 'subject' =>    $subject,
	# 	 );

    my $rtf_timestamp = get_rtf_timestamp();

    $rtf->print(
        \'{',\'\rtf1\ansi\ansicpg1252\cocoartf1671\cocoasubrtf400',
            \'{',\'\fonttbl\f0\fswiss\fcharset0 Times New Roman;\f1\fswiss\fcharset0 Times New Roman;',\'}',
            \'{',\'\colortbl;\red255\green0\blue0;\red0\green0\blue255;',\'}',
            \'{',\'\info',
                \'{',\"\\creatim $rtf_timestamp",\'}',
                \'{',\"\\revtim $rtf_timestamp",\'}',
                \'{',\"\\title $title",\'}',
                \'{',\"\\subject $subject",\'}',
                \'{',\"\\author $author",\'}',
                \'{',\"\\company $company",\'}',
                \'{',\'\doccomm written by /usr/local/gkb/website/cgi-bin/rtfexporter [Perl RT\\\'46::Writer v1\\\'2e11]',\'}',
            \'}',
    );

    $self->generate_stylesheet($self->get_depth_limit());

    # Without this, images can't be included.  Don't ask me why, I discovered
    # it by trial and error.
#    $rtf->print(\'\sect', \'\sectd', \'');   # '
    $rtf->print(\'',  #'
		\'{',  #'
		   \'\header\pard\qr\plain\f0',  #'
		   \'\par', #'
		\'}',  #'
		);   # '
}

# Adapted from __time_to_rtf subroutine from https://metacpan.org/release/RTF-Writer/source/lib/RTF/Writer.pm
sub get_rtf_timestamp {
    my ($second, $minute, $hour, $day, $month, $year) = localtime(time);
    $year += 1900; # Adjusted since localtime(time) returns year since 1900
    $month += 1; # Adjusted since localtime(time) returns month number starting at an index of zero

  return sprintf '\yr%d\mo%d\dy%d\hr%d\min%d\sec%d', ($year, $month, $day, $hour, $minute, $second);
}

sub generate_page_numbering {
    my ($self, $starting_page_number) = @_;

    my $rtf = $self->rtf;
    
    my $reader = $self->get_reader();
    my $title = $reader->get_title();
    my $subject = $reader->get_subject();
    
	# Do this instead of number_pages to get the formatting we want
	my $param = "";
#    if (defined $starting_page_number) {
#    	$param = "\\pgnstarts$starting_page_number";
#    }
	my $param_ref = \$param;
	$rtf->print(\'',  #'
		\'{',  #'
		\'\header\pard\qr\plain\f0',  #'
		$param_ref, #'
		\'\qc',  #'
		\'\i',   #'
		\'\cf2', #'
		"$title: $subject            ",
		\'\chpgn3', #'
		\'\par', #'
		\'}',  #'
	);   # '
}

# Prints out the raw table of contents
# Arguments:
#
# toc_depth - amount of nesting in TOC.  Zero or negative values nest right
#             down to the bottom of the hierarchy.
sub generate_toc {
    my ($self, $toc_depth) = @_;

    my $rtf = $self->rtf;

    my $set_toc_depth = '\fs24\cgrid0  TOC \\\\o "1-' . $toc_depth . '"'; #'
    if ($toc_depth<=0) {
	$set_toc_depth = '\fs24\cgrid0  TOC'; #'
    }
    my $ref_set_toc_depth = \$set_toc_depth;

    $rtf->print(\'{', # '
		   \'\field\fldedit', # '
		   \'{', # '
		      \'\*\fldinst', # '
		      \'{', # '
			 $ref_set_toc_depth, # Show depth levels
			 \'}', # '
		      \'}', # '
		   \'{', # '
		      \'\fldrslt', # '
		      \'{', # '
			 \'\lang1024', # '
			 \'\par', # '
			 \'}', # '
		      \'\pard\plain \s15\widctlpar\tqr\tldot\tx8630\adjustright \fs20\cgrid', # '
		      \'{', # '
			 \'\lang1024\cgrid0 Right mouse click {\i here} and select "Update Field" in the popup menu (does not work with OpenOffice, sorry!).', # '
			 \'}', # '
		      \'}', # '
		   \'}', # '
		);
}

# Creates styles used in text.  The main purpose is actually to create
# the header styles, because these are used in generating the table of
# contents.
sub generate_stylesheet {
    my ($self, $depth_limit) = @_;

    my $rtf = $self->rtf;

    my @params;
    push @params, \''; # 'Newline
    push @params, \'{\stylesheet'; #'
    push @params, \'{\widctlpar\adjustright \fs20\cgrid \snext0 Normal;}'; #'
    
    for (my $i=1; $i<=$depth_limit; $i++) {
	my $param = '{\s' . $i . '\sb240\sa60\keepn\widctlpar\adjustright \b\f1\fs32\cgrid \sbasedon0 \snext0 heading ' . $i . ';}';
	
	push @params, \$param;
    }

    push @params, \'{\*\cs10 \additive Default Paragraph Font;}'; #'
    push @params, \'}'; #'

    $rtf->print(@params);
}

# Emit the image contained in the supplied file.  If the $delete_flag
# is set to 1, then the image file will be deleted after emission;
# use this with care!
#
# This subroutine is supposed to be a simplified version of
# generate_image_from_file. with no dependency on GD.
sub generate_image_from_file_basic {
    my ($self, $image_file) = @_;
    
    my $rtf = $self->rtf;
    $rtf->image_paragraph('filename' => $image_file);
}

# Emit a paragraph containing the supplied image file
sub generate_image {
    my ($self, $image, $filename) = @_;

    my $logger = get_logger(__PACKAGE__);

    if (!(defined $image)) {
        $logger->warn("no image!!");
	return;
    }

    # Dump image into temporary file
    my $image_file_name = GKB::FileUtils->print_image($image, undef, 1);

    if (!(defined $image_file_name)) {
        $logger->warn("cant do anything with image, skipping");
        return;
    }
    
    # Scale the image first, so that it ia i) visible and
    # ii) not so big that it doesn't fit onto the page.
    my $percent_scale = 100 * $self->find_scale_to_fit_image_to_page($image);

    my $rtf = $self->rtf;
    $rtf->image_paragraph('filename' => $image_file_name, 'scalex' => $percent_scale, 'scaley' => $percent_scale);

    unlink($image_file_name); # don't need image file anymore
}

# Scale image so that it fits within a page, but also so that it
# isn't so tiny that you can't see it.
# Arguments:
#
# image - an object of type GD::Image
sub find_scale_to_fit_image_to_page {
    my ($self, $image) = @_;

    my $max_width = 450.0;

    my ($width,$height) = $image->getBounds();

    my $reactome_scale = $max_width/$width;

    # Don't enlarge images too drastically, otherwise you get
    # horrible pixel blurring effects
    if ($width <= (1.05 * $max_width) && $width >= 300) {
	$reactome_scale = 1.0;
    } elsif ($reactome_scale>1.5) {
	$reactome_scale = 1.5;
    }

    return $reactome_scale;
}

sub generate_page_break {
    my ($self) = @_;

    my $rtf = $self->rtf;
    
    $rtf->paragraph(
		    \'\page',       # 'Font size
		    "");
}

# Emits a block of text.
# Arguments:
#
# text - string to be emited
# formatting - reference to a hash containing format info
sub generate_paragraph {
    my ($self, $text, $formatting) = @_;

    my $logger = get_logger(__PACKAGE__);

    my $rtf = $self->rtf;
    
    # Take formatting information and use it to generate RTF
    # formatting commands.
    my @params = $self->translate_formatting($formatting);

    # Don't generate the paragraph straight into the
    # output file.  Instead, generate it into a string
    # and then process that string to find embedded
    # markup tags, and turn them into RTF.
    my $paragraph_printed_ok = 0;
    eval {
        my $paragraph_rtf_string = ''; # String to hold generated RTF paragraph
        my $paragraph_rtf = RTF::Writer->new_to_string(\$paragraph_rtf_string);
        $paragraph_rtf->paragraph(@params, $text);
        my $markup_free_paragraph_rtf_string = $self->interpret_markup($paragraph_rtf_string);
	$rtf->print(\$markup_free_paragraph_rtf_string);
        $paragraph_rtf->close();
        $paragraph_printed_ok = 1;
    };
    if (!$paragraph_printed_ok) {
        if (defined $text) {
            $logger->warn("serious problem printing paragraph, params=" . Dumper(@params) . ", text=|$text|");
        } else {
            $logger->warn("serious problem printing paragraph, params=" . Dumper(@params) . ", text=undef");
        }
    }

    $self->set_first_page_flag(0);
}

# Translates from the Reactome formatting hash to a list of
# RTF formatting directives
sub translate_formatting {
    my ($self, $formatting) = @_;

    my $logger = get_logger(__PACKAGE__);

    # Take formatting information and use it to generate RTF
    # formatting commands.
    my @params;
    my $param_ref;
    foreach my $format_key (keys(%{$formatting})) {
	my $param;

	if ($format_key eq "font") {
	    # Set the font
	    if ($formatting->{$format_key} eq "Courier") {
		$param_ref = \'\f1'; #'
	    } else {
#		print STDERR "GenerateTextRTF.translate_formatting: WARNING - format value=" . $formatting->{$format_key} . " not recognised, setting to Courier!\n";
		$param_ref = \'\f1'; #'
	    }
	} elsif ($format_key eq "font_size") {
	    # Set the font size
	    my $points = $formatting->{$format_key} * 2;
	    $param = "\\fs$points";
	    $param_ref = \$param;
	} elsif ($format_key eq "bold") {
	    # Set boldface
	    $param_ref = \'\b'; #'
	} elsif ($format_key eq "italic") {
	    # Set italic
	    $param_ref = \'\i'; #'
	} elsif ($format_key eq "underline") {
	    # Set underline
	    $param_ref = \'\ul'; #'
	} elsif ($format_key eq "left_indent") {
	    # Set left indent
	    $param = "\\li" . $formatting->{$format_key} * 2;
	    $param_ref = \$param;
	} elsif ($format_key eq "right_indent") {
	    # Set right indent
	    $param = "\\ri" . $formatting->{$format_key} * 2;
	    $param_ref = \$param;
	} elsif ($format_key eq "first_line_indent") {
	    # Set first line extra left indent
	    $param = "\\fi" . $formatting->{$format_key} * 2;
	    $param_ref = \$param;
	} elsif ($format_key eq "justify") {
	    # Set the justification
	    if ($formatting->{$format_key} eq "left") {
		$param_ref = \'\ql'; #'
	    } elsif ($formatting->{$format_key} eq "right") {
		$param_ref = \'\qr'; #'
	    } elsif ($formatting->{$format_key} eq "center") {
		$param_ref = \'\qc'; #'
	    } else {
		$logger->warn("format value=" . $formatting->{$format_key} . " not recognised, skipping!");
		next;
	    }
	} elsif ($format_key eq "bind_next_para") {
	    # Bind to next paragraph (don't allow page break)
	    $param_ref = \'\keepn'; #'
	} elsif ($format_key eq "voodoo") {
	    # Set mysterious command that magically makes things work
	    $param_ref = \'\tx0\ls11'; #'
	} else {
	    $logger->warn("format_key=$format_key not recognised, skipping!");
	    next;
	}

	push @params, $param_ref;
    }

    return @params;
}

sub generate_bullet_text {
    my ($self, $text) = @_;

    my $rtf = $self->rtf;

    my $font_size = $self->regular_text_font_size * 2;
    
    $rtf->paragraph(
		    \'\f1',         # 'Font 1 (Courier)
		    \"\\fs$font_size",
		    \'\li500',      # 'Left indent
		    \'\fi-200',     # 'Negative indent for 1st line
		    \'\bullet',     # 'Emit a bullet point
		    \" $text\n");
}

sub generate_numbered_text {
    my ($self, $text, $number) = @_;

    my $rtf = $self->rtf;

    my $font_size = $self->regular_text_font_size * 2;
    
    $rtf->paragraph(
		    \'\f1',         # 'Font 1 (Courier)
		    \"\\fs$font_size",
		    \'\li200',      # 'Left indent
		    \'\fi-200',     # 'Negative indent for 1st line
		    " $number.",    #  line number
		    " $text\n");
}


sub generate_hyperlink {
    my ($self, $text, $url) = @_;

    my $rtf = $self->rtf;
    
    my $font_size = $self->regular_text_font_size * 2;
    
    $rtf->paragraph(
            $self->colortableblue(),
            \'\cf2',        # 'Font color: blue
            \'\ul',         # 'Underline
            \'\f1',         # 'Font 1 (Courier)
            \"\\fs$font_size",
            \'\li200',      # 'Left indent
            \'\fi-200',     # 'Negative indent for 1st line
            \'{',           #'
            \'\field',      #'
            \'{',           #'
            \'\*',          #'
            \'\fldinst',    #'
            \'{',           #'
            "HYPERLINK $url",
            \'}',           #'
            \'}',           #'
            \'{',           #'
            \'\fldrslt',    #'
            \'{',           #'
            "$text",
            \'}',           #'
            \'}',           #'
            \'}',           #'
            \'\ul0',        # 'Underline off
            \'\cf1',        # 'Font color: black
            "\n");
}



sub colortableblue {
    my ($self, $text, $url) = @_;

	my $colortable_string = 
		    \'{',			#'
		    \'\colortbl;',
		    \'\red0',
		    \'\green0',
		    \'\blue0;',
		    \'\red0',
		    \'\green0',
		    \'\blue255;',
                    \'\red255',
                    \'\green0',
                    \'\blue0;', 
			\'}',			#'
		;
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

    my $logger = get_logger(__PACKAGE__);

    $self->generate_header_initial_whitespace($depth, $text);

    my %formatting = $self->header_formatting($depth);

    # Take formatting information and use it to generate RTF
    # formatting commands.
    my @params = $self->translate_formatting(\%formatting);

    # Add header stylesheet information to @params
    my $header_num = $depth + 1;
    my $depth_param = "\\s$header_num";
    push @params, \$depth_param;

    # make header red if required
    my $rtf = $self->rtf;
    unless ($text =~ /<font/) {
	$rtf->paragraph(@params, $text);
    }
    else {
	my $paragraph_rtf_string = '';
	my $paragraph_rtf = RTF::Writer->new_to_string(\$paragraph_rtf_string);
	$text =~ s/<\/?font.*?>//g;
	$logger->info("TEXT $text\n");
	$paragraph_rtf->paragraph(@params, $text);
	my $marked_up_rtf_string = '{{\colortbl;\red0\green0\blue255;\red255\green0\blue0;}\cf1'. "\n$paragraph_rtf_string\n" . '}';
	$rtf->print(\$marked_up_rtf_string);
	$paragraph_rtf->close();
    }
}

# Generate the formatting needed for a header, based on the depth
# in the hierarchy.  This overwrites the definition in the
# superclass, and is tailored for RTF.
# Arguments:
#
# depth - depth in event hierarchy
#
# Returns a hash, containing formatting information
sub header_formatting {
    my ($self, $depth) = @_;

    my $font_size = $self->calculate_header_font_size($depth);
    my %formatting = (
		      'bind_next_para' =>  "true",
		      'bold' =>  "true",
		      'font_size' =>  $font_size,
		      );

    return %formatting;
}

# Take any markup in the input text and convert it into
# something that will be understood by the RTF interpreter
sub interpret_markup {
    my ($self, $text) = @_;
    
    my $logger = get_logger(__PACKAGE__);

    my $new_text = $text;

    my $rtf_red_font_string = '{\rtf1\ansi\deff0{\colortbl;\red0\green0\blue255;\red255\green0\blue0;}\cf1';
    $new_text =~ s/\<font color=red\>/$rtf_red_font_string/g;
    $new_text =~ s/\<\/font>/}/g;
    
    $new_text =~ s/\<b\>/{\\b /gi;
    $new_text =~ s/\<\/b\>/}/gi;
    $new_text =~ s/\<i\>/{\\i /gi;
    $new_text =~ s/\<\/i\>/}/gi;
    $new_text =~ s/\<sup\>/{\\super /gi;
    $new_text =~ s/\<\/sup\>/}/gi;
    $new_text =~ s/\<sub\>/{\\sub /gi;
    $new_text =~ s/\<\/sub\>/}/gi;
    
    while ($new_text =~ /^(<img.*?\>)/gi) {
	my $image_tag = $1;
	
	# An embedded image
	$image_tag =~ /src="([^"]+)"/i;
	my $src = $1;
    
	# Remove RTF-specific escape sequences
	$src =~ s/\\_/-/g;
	$src =~ s/\\'2e/./g; #'
	
	if (!(defined $src) || $src eq '') { #'
	    $logger->warn("cannot find image source file, skipping to next line");
	    next;
	}
	if (!(-e $src)) {
	    $logger->warn("we cant see the file $src, skipping to next line");
	    next;
	}
	
	$image_tag =~ /width="([^"]+)"/i; #"
	my $width = $1;
	$image_tag =~ /height="([^"]+)"/i; #"
	my $height = $1;
	
	# Don't write to the RTF output stream, because we
	# want to edit the RTF a bit first.
	my $rtf_string = '';
	my $rtf = RTF::Writer->new_to_string(\$rtf_string);
	$rtf->image_paragraph('filename' => $src);
	
	# Chuck out the first and last lines of the RTF
	# string, they contain paragraph stuff
	my @lines = split(/\n/, $rtf_string);
	$rtf_string = '';
	for (my $i=1; $i<scalar(@lines)-1; $i++) {
	    my $line = $lines[$i];
	    if (!($rtf_string eq '')) {
		$rtf_string .= "\n";
	    }
	    $rtf_string .= $line;
	}
	
	$rtf->close();
	unlink($src); # don't need anymore
	
	$new_text =~ s/$image_tag/$rtf_string/;
    }

    return $new_text;

=head
    my $rtf_string;
    my $rtf;
    
    
    # Break up text to extract potential markup
    my $initial_backarrow_flag = 0;
    if ($text =~ /^</) {
	$initial_backarrow_flag = 1;
    }
    my $final_arrow_flag = 0;
    my @lines = split(/</, $text);
    my @lines3 = ();
    my $lines3_counter = 0;
    foreach my $line (@lines) {
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
	foreach my $line2 (@lines2) {
	    $lines3[$lines3_counter] = "$line2>";
	    $lines3_counter++;
	}
	if (!$final_arrow_flag) {
	    $lines3[$lines3_counter-1] =~ s/>$//;
	}
    }
    
    # lines3 has lines containing either plain text or
    # markup, not mixed.  We can now loop through it
    # and substitute any RTF markup needed.
    my $src;
    my $width;
    my $height;
    my $new_text = '';
    my $i;
    
    foreach my $line3 (@lines3) {
	if ($line3 =~ /^</) {
	    # We have markup
	    if ($line3 =~ /^<img /i) {
		# An embedded image
		$line3 =~ /src="([^"]+)"/i;
		$src = $1;
		
		# Remove RTF-specific escape sequences
		$src =~ s/\\_/-/g;
		$src =~ s/\\'2e/./g; #'
		
		if (!(defined $src) || $src eq '') { #'
		    $logger->warn("cannot find image source file, skipping to next line");
		    next;
		}
		if (!(-e $src)) {
		    $logger->warn("we cant see the file $src, skipping to next line");
		    next;
		}
		
		$line3 =~ /width="([^"]+)"/i; #"
		$width = $1;
		$line3 =~ /height="([^"]+)"/i; #"
		$height = $1;
		
		# Don't write to the RTF output stream, because we
		# want to edit the RTF a bit first.
		$rtf_string = '';
		$rtf = RTF::Writer->new_to_string(\$rtf_string);
		$rtf->image_paragraph('filename' => $src);
		
		# Chuck out the first and last lines of the RTF
		# string, they contain paragraph stuff
		@lines = split(/\n/, $rtf_string);
		$rtf_string = '';
		for ($i=1; $i<scalar(@lines)-1; $i++) {
		    my $line = $lines[$i];
		    if (!($rtf_string eq '')) {
			$rtf_string .= "\n";
		    }
		    $rtf_string .= $line;
		}
		
		$new_text .= $rtf_string;
		$rtf->close();
		
		unlink($src); # don't need anymore
	    } elsif ($line3 =~ /^<b>$/i) {
		# Start bold
		$new_text .= '{\b' . "\n";
	    } elsif ($line3 =~ /^<\/b>$/i) {
		# Stop bold
		$new_text .= "\n" . '}' . "\n";
	    } elsif ($line3 =~ /^<i>$/i) {
		# Start italics
		$new_text .= '{\i' . "\n";
	    } elsif ($line3 =~ /^<\/i>$/i) {
		# Stop italics
		$new_text .= "\n" . '}' . "\n";
	    } elsif ($line3 =~ /^<font/i) {
		my ($color) = $line3 =~ /color=["']?([a-z]+)/i;
		if ($color && $color =~ /red|blue/i) {
		    $new_text .= '{\rtf1\ansi\deff0{\colortbl;\red0\green0\blue255;\red255\green0\blue0;}\cf1'
		}
	    } elsif ($line3 =~ /^<\/font/i) {
		$new_text .= '}';#'\cf1'."\n".'}';
	    }
	    else {
		# unknown markup, or maybe not markup at all
		$logger->info("possible unknown markup, line3=$line3");
		$new_text .= $line3;
	    }
	} else {
	    # We have plain text
	    $new_text .= $line3;
	}
    }

    return $new_text;
=cut

}

1;
