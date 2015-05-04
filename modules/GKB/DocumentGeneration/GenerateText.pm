package GKB::DocumentGeneration::GenerateText;

=head1 NAME

GKB::DocumentGeneration::GenerateText

=head1 SYNOPSIS

Base class for producing text in assorted formats

=head1 DESCRIPTION

The base class for the document generation package.  The methods in this
module allow a programmer to generate all or parts of a
book.  Being the base class, the methods are independant of the input
and output format.  If you want to produce output in a specific format,
e.g. RTF, you will need to write a subclass inheriting from this
class, and you may need to overwrite some of the methods.

You may notice if you browse through this source code that some
methods contain nothing more than an exit statement.  Sounds
nasty, and it is nasty if you do not write an overwriting method
in your subclass, because the program will simply die when it
hits that method.  This is to try to force you to implement
these methods, since these are the base-level methods that
actually emit text or formatting directives to the output stream
and they will be specific to the output format.

Before you can use this package, you will definitely need to set the
reader used to get input, e.g. by:

$generate_text->set_reader($reader)

where $reader is of type Reader.  E.g. ReactomeDatabaseReader.

Most likely you will also want to customize the output, in which case
you will also need to set the appropriate o/p parameters via a hash,
e.g.

$generate_text->set_params(\%params)

Once that's done, you could generate a book:

$generate_text->generate_book();

...or just the chapters (without title page, table of contents, etc.):

$generate_text->open()
$generate_text->generate_prolog("", "", "", "")
$generate_text->generate_chapters()
$generate_text->close()

How does GenerateText know which chapters to generate?  Well, it
doesn't.  That information is supplied via the reader, GenerateText
more or less passively takes the stream of text units given to it
by the reader and dumps them straight to it's output.  So now you
need to find out how to configure a reader...

If you would like to see some example code, take a look at:

.../GKB/scripts/TheReactomeBook/gen*.pl

=head1 SEE ALSO

GKB::GenerateTextRTF
GKB::GenerateTextPDF

=head1 AUTHOR

David Croft E<lt>croft@ebi.ac.ukE<gt>

Copyright (c) 2005 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

use GKB::Config;
use GKB::HTMLUtils;
use GKB::FileUtils;
use GKB::DocumentGeneration::TextUnit;
use GD::Image;
use constant font => "Helvetica";
use strict;
use vars qw(@ISA $AUTOLOAD %ok_field);
use Bio::Root::Root;
use Log::Log4perl qw/get_logger/;
Log::Log4perl->init(\$LOG_CONF);

@ISA = qw(Bio::Root::Root);

my $reader; # reads input and converts to book-friendly form

# Document formatting parameters (with default vals)
my $output_file_name = "TheBook"; # Silly name for o/p file
my $output_file_stream;           # Optional, supercedes name, if there
my $include_images_flag = 1;      # Include images
my $depth_limit = 2;              # Only go 2 deep in event hierarchy
my $toc_depth = (-1);             # Full table of contents
#my @sheet_size = (595, 842);      # width, depth (A4)
my @sheet_size = (612, 792);      # width, depth (letter)
my @margins = (50, 100, 50, 100); # left, bottom, right and top
my $first_page_flag = 1;
my $book_flag = 0;				  # 1 to chapterize
my $split_into_files_flag = 0;	  # 1 to split o/p by chapter
my $split_into_files_counter = 0; # The number of the current o/p file

# List the object variables here, so that they can be checked
for my $attr
    (qw(
    regular_text_font_size
    document_title_font_size
    document_subject_font_size
    document_author_font_size
    header_0_font_size
    header_1_font_size
    header_2_font_size
    header_3_font_size
    header_4_font_size
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
    my($pkg) = @_;
    
    my $self = bless {}, $pkg;
    
    $self->set_regular_text_font_size(12);

    return $self;
}

# Needed by subclasses to gain access to class variables defined in
# this class.
sub get_ok_field {
    return %ok_field;
}

# Globally sets all font sizes relative to the given "regular"
# font size (i.e. the font size for regular paragraph text).
sub set_regular_text_font_size {
    my ($self, $regular_text_font_size) = @_;
    
    $self->regular_text_font_size($regular_text_font_size);
    $self->document_title_font_size($regular_text_font_size + 12);
    $self->document_subject_font_size($regular_text_font_size +4);
    $self->document_author_font_size($regular_text_font_size);
    $self->header_0_font_size($regular_text_font_size + 8);
    $self->header_1_font_size($regular_text_font_size + 6);
    $self->header_2_font_size($regular_text_font_size + 4);
    $self->header_3_font_size($regular_text_font_size + 2);
    $self->header_4_font_size($regular_text_font_size + 1);
}

# Set reader, the object that takes input and mangles it into a
# book-friendly form.
sub set_reader {
    my ($self, $external_reader) = @_;

    $reader = $external_reader;
}

sub get_reader {
    return $reader;
}

sub get_toc_depth {
    return $toc_depth;
}

sub get_split_into_files_flag {
    return $split_into_files_flag;
}

# Set document formatting parameters.
sub set_params {
    my ($self, $params) = @_;

    if (exists($params->{"output_file_name"})) {
	$output_file_name = $params->{"output_file_name"};
    }
    if (exists($params->{"output_file_stream"})) {
	$output_file_stream = $params->{"output_file_stream"};
    }
    if (exists($params->{"include_images_flag"})) {
	$include_images_flag = $params->{"include_images_flag"};
    }
    if (exists($params->{"depth_limit"})) {
	$depth_limit = $params->{"depth_limit"};
    }
    if (exists($params->{"toc_depth"})) {
	$toc_depth = $params->{"toc_depth"};
    }
    if (exists($params->{"sheet_size"})) {
	@sheet_size = $params->{"sheet_size"};
    }
    if (exists($params->{"margins"})) {
	@margins = $params->{"margins"};
    }
    if (exists($params->{"split_flag"}) && $params->{"split_flag"}) {
	$split_into_files_flag = 1;
    }
}

sub get_output_file_name {
    my ($self) = @_;

    return($output_file_name);
}

sub set_output_file_name {
    my ($self, $new_output_file_name) = @_;

    $output_file_name = $new_output_file_name;
}

sub get_output_file_stream {
    my ($self) = @_;

    return($output_file_stream);
}

sub get_include_images_flag {
    my ($self) = @_;

    return($include_images_flag);
}

sub get_depth_limit {
    my ($self) = @_;

    return($depth_limit);
}

sub get_sheet_size {
    my ($self) = @_;

    return(@sheet_size);
}

sub get_margins {
    my ($self) = @_;

    return(@margins);
}

sub set_first_page_flag {
    my ($self, $external_first_page_flag) = @_;

    $first_page_flag = $external_first_page_flag;
}

sub set_book_flag {
    my ($self, $external_book_flag) = @_;

    $book_flag = $external_book_flag;
}

# Open stream to output file.  The name will be generated automatically;
# if you specify a qualifier, it will be incorporated into the name.
sub open {
    my ($self, $qualifier) = @_;
    
    my $logger = get_logger(__PACKAGE__);
    
    my $output_file_stream = $self->get_output_file_stream();
    if (defined $output_file_stream) {
	$self->open_stream($output_file_stream);
    } else {
	my $filename = $self->get_output_file_name();
	if ($self->get_split_into_files_flag() && $split_into_files_counter == 0) {
	    if (!(defined $qualifier)) {
		$qualifier = 0;
	    }
	    
	    # Create/use a directory to put all the split files into
	    if (-e $filename && !(-d $filename)) {
		$logger->warn("$filename exists but is not a directory");
	    } elsif (!(-e $filename) && !(mkdir $filename)) {
		$logger->warn("could not create directory $filename");
	    } else {
		$filename =~ s/\/+$//;
		$filename .= "/";
		$self->set_output_file_name($filename);
	    }
	}
	if (defined $qualifier) {
	    if ($self->get_split_into_files_flag()) {
	    	$filename .= $qualifier;
	    } else {
	    	$filename .= ".$qualifier";
	    }
	}
	
	$self->open_filename($filename);
    }
}

# Open stream to RTF file
sub open_stream {
    my ($self, $stream) = @_;

    my $logger = get_logger(__PACKAGE__);
    
    $logger->error_die("this method must be defined in the subclass");
}

# Open stream to named output file.
sub open_filename {
    my ($self, $filename) = @_;

    my $logger = get_logger(__PACKAGE__);
    
    $logger->error_die("this method must be defined in the subclass");
}

# Close stream to currently open output file
sub close {
    my ($self) = @_;
    
    my $logger = get_logger(__PACKAGE__);

    $logger->error_die("this method must be defined in the subclass");
}

# Document meta-data
sub generate_prolog {
    my ($self, $depth_limit, $author, $company, $title, $subject, $sheet_size, $margins) = @_;

    my $logger = get_logger(__PACKAGE__);
    
    $logger->error_die("this method must be defined in the subclass");
}

sub generate_page_numbering {
    my ($self, $starting_page_number) = @_;
    
    my $logger = get_logger(__PACKAGE__);

    $logger->error_die("this method must be defined in the subclass");
}

# Emits a page containing the table of contents
# Arguments:
#
# toc_depth - cutoff depth for the TOC, below which no headers will be
#             output. -1 means go right to bottom of hierarchy.
sub generate_toc_page {
    my ($self, $toc_depth) = @_;

    $self->generate_header(0, "Table of Contents");
    $self->generate_toc($toc_depth);
}

# Prints out the raw table of contents
sub generate_toc {
    my ($self, $toc_depth) = @_;
    
    my $logger = get_logger(__PACKAGE__);

    $logger->error_die("this method must be defined in the subclass");
}

# Emit the image contained in the supplied file.  If the $delete_flag
# is set to 1, then the image file will be deleted after emission;
# use this with care!
sub generate_image_from_file {
    my ($self, $image_file, $delete_flag) = @_;
    
    my $logger = get_logger(__PACKAGE__);
    
    $image_file =~ /^(.+)$/;
    my $detainted_image_file = $1;
    
    if (!(-e $detainted_image_file)) {
    	$logger->warn("detainted_image_file=$detainted_image_file does not exist!!");
    	return;
    }

    my $image = undef;
    eval {
        $image = GD::Image->new($detainted_image_file);
    };

    if (defined $image) {
    	$detainted_image_file =~ /([^\/]+)\.[a-zA-Z]*$/;
    	my $filename = $1;
    	
	$self->generate_image($image, $filename);
    } else {
	$self->generate_image_from_file_basic($detainted_image_file);
    }
    
    if ($delete_flag) {
	unlink($detainted_image_file);
    }
}

# Emit the image contained in the supplied file.  If the $delete_flag
# is set to 1, then the image file will be deleted after emission;
# use this with care!
#
# This subroutine is supposed to be a simplified version of
# generate_image_from_file. with no dependency on GD.
#
# You will need to overwrite this in the subclass.
sub generate_image_from_file_basic {
    my ($self, $image_file) = @_;
    
    my $logger = get_logger(__PACKAGE__);
    $logger->warn("could not generate image for $image_file");
}

# Emit the image contained in the supplied GD::Image argument.
# The filename is for text generators that need a nice file
# name (if you don't supply this, then any files generated
# will have names that are random numbers).
sub generate_image {
    my ($self, $image, $filename) = @_;

    my $logger = get_logger(__PACKAGE__);
    $logger->error_die("this method must be defined in the subclass");
}

# Given a file containing a diagram as vector graphics, emit
# the relevant part of the file contents.  Returns 1 if successfull,
# 0 otherwise.
sub generate_vector_graphics_from_file {
    my ($self, $image) = @_;

    return 0;
}

# Introduces vertical blank space into a page.
# Arguments:
# lines - amount of space - one line is equivalent to the height of a line of body text.
sub generate_vertical_space {
    my ($self, $lines) = @_;
    if (!(defined $lines)) {
    	$lines = 1;
    }
    
    my %additional_formatting = ();
    if (scalar(@_) > 3) {
	my $ref_formatting =  $_[3];
	%additional_formatting = %{$ref_formatting};
    }

    for (my $i=0; $i<$lines; $i++) {
	$self->generate_body_text_paragraph("", \%additional_formatting);
    }
}

sub generate_page_break {
    my ($self) = @_;

    my $logger = get_logger(__PACKAGE__);
    
    $logger->error_die("this method must be defined in the subclass");
}

# Generates an entire book for you, dont cost nothing, cant
# say better than that mate.
sub generate_book {
    my ($self) = @_;
    
    $book_flag = 1;

    # Get publication details
    my $author = $reader->get_author();
    my $company = $reader->get_company();
    my $title = $reader->get_title();
    my $subject = $reader->get_subject();
    my $isdn = $reader->get_isdn();
    my $copyright_conditions = $reader->get_copyright_conditions();
    my $acknowledgements = $reader->get_acknowledgements();
    my $preface = $reader->get_preface();
    
    $self->open();

    $self->generate_prolog($author, $company, $title, $subject);

    $self->generate_page_numbering();

    $self->generate_title_page($author, $company, $title, $subject, $copyright_conditions);

    if (!$split_into_files_flag) {
    	$self->generate_toc_page($toc_depth);
    }

    $self->generate_preface($preface);

    $self->generate_acknowledgements($acknowledgements);

    $self->generate_chapters();

    $self->close();
}

# First book page, with title, authors, etc.
sub generate_title_page {
    my ($self, $author, $company, $title, $subject, $conditions) = @_;

    my %formatting;

    if ($book_flag) {
        $self->generate_vertical_space(4);
    }
    if (defined $title && !($title eq '')) {
        $self->generate_title($title, \%formatting);
    }
    
    if ($book_flag) {
        $self->generate_vertical_space(1);
    }
    if (defined $subject && !($subject eq '')) {
        %formatting = (
	    'voodoo' =>  "true",
	    'bold' =>  "true",
    	    'italic' =>  "true",
    	    'font' =>  font,
    	    'justify' =>  "center",
    	    'font_size' =>  $self->document_subject_font_size,
    	);
	$self->generate_paragraph($subject, \%formatting);
    }
    
    if ($book_flag) {
        $self->generate_vertical_space(16);
    }
    if (defined $author && !($author eq '')) {
        %formatting = (
    	    'voodoo' =>  "true",
    	    'font' =>  font,
    	    'justify' =>  "center",
    	    'font_size' =>  $self->document_author_font_size,
    	    'left_indent' =>  50,
    	    'right_indent' =>  50,
    	);
        $self->generate_paragraph($author, \%formatting);
    }
    
    if ($book_flag) {
        my $vspace = 17;
        if (length($author) > 120) {
	    $vspace -= length($author) / 60;
	    if ($vspace < 3) {
		$vspace = 3;
	    }
	}
	$self->generate_vertical_space($vspace);
    }
    if (defined $company && !($company eq '')) {
        %formatting = (
	    'voodoo' =>  "true",
	    'bold' =>  "true",
	    'font' =>  font,
	    'justify' =>  "center",
	    'font_size' =>  $self->regular_text_font_size,
	);
	$self->generate_paragraph($company, \%formatting);
    }

    if (defined $conditions && !($conditions eq '')) {
	if ($book_flag) {
	    $self->generate_vertical_space(1);
	}
	%formatting = (
	    'font' =>  font,
	    'justify' =>  "center",
	    'font_size' =>  $self->regular_text_font_size,
	    'left_indent' =>  50,
	    'right_indent' =>  50,
	);
	$self->generate_paragraph($conditions, \%formatting);
    }
}

# Emit a page contaning copyright information
sub generate_copyright_page {
    my ($self, $isbn, $conditions) = @_;

    unless ($isbn || $conditions) {
	return;
    }

    $self->generate_page_break();

    $self->generate_vertical_space(2);
    my $text;
    my %formatting;

    if ($isbn) {
	$text = $isbn;
	%formatting = (
	    'font' =>  font,
	    'justify' =>  "center",
	    'font_size' =>  $self->regular_text_font_size,
	);
	$self->generate_paragraph($text, \%formatting);
    }

    $self->generate_vertical_space(50);

    if ($conditions) {
	$text = $conditions;
	%formatting = (
	    'font' =>  font,
	    'justify' =>  "center",
	    'font_size' =>  $self->regular_text_font_size,
	    'left_indent' =>  50,
	    'right_indent' =>  50,
	);
	$self->generate_paragraph($text, \%formatting);
    }
}

# Emits a page containing the preface text.
sub generate_preface {
    my ($self, $text) = @_;

    unless ($text) {
	return;
    }

    $self->generate_header(0, "Preface");

    $self->generate_body_text_paragraphs_from_text($text);
}

# Emits a page of kowtowing acknowledgements
sub generate_acknowledgements {
    my ($self, $text) = @_;

    unless ($text) {
	return;
    }

    $self->generate_header(0, "Acknowledgements");

    $self->generate_body_text_paragraphs_from_text($text);
}

# Chops the input text into paragraphs (using hints from HTML
# tags, if available) then emits the paragraphs.
sub generate_body_text {
    my ($self, $text) = @_;

    # Turn the text into a list of paragraph
    my @paragraphs = GKB::HTMLUtils->extract_paragraphs_from_html($text, 0, 1);
    my $paragraph;
    if (@paragraphs) {
	for (my $i=0; $i<scalar(@paragraphs); $i++) {
	    $paragraph = $paragraphs[$i] . "\n";
	    # Remove HTML tags
	    $paragraph =~ s/\<[^\>]*\>//g;
	    $self->generate_body_text_paragraph($paragraph);
	}
    }
}

# Chops the input text into paragraphs (using hints from HTML
# tags, if available) then emits the paragraphs.
sub generate_body_text_paragraphs_from_text {
    my ($self, $text) = @_;

    # Turn the text into a list of paragraphs
    my @paragraphs = split(/\n+/, $text);
    my $paragraph;
    if (@paragraphs) {
	for (my $i=0; $i<scalar(@paragraphs); $i++) {
	    $paragraph = $paragraphs[$i] . "\n";
	    $self->generate_body_text_paragraph($paragraph);
	}
    }
}

# Emits a standard paragraph, as it would appear in the body of the
# text.
# Arguments:
# text - the text you want to print
# formatting (optional) - formatting directives for the output text
sub generate_body_text_paragraph {
    my ($self, $text) = @_;
    my %formatting = ();
    if (scalar(@_) > 2) {
	my $ref_formatting =  $_[2];
	%formatting = %{$ref_formatting};
    }

    # Don't overwrite formatting passed to this subroutine
    unless (exists($formatting{'font'})) {
	$formatting{'font'} =  font;
    }
    unless (exists($formatting{'font_size'})) {
	$formatting{'font_size'} =  $self->regular_text_font_size;
    }

    return $self->generate_paragraph($text, \%formatting);
}

# Emits a standard paragraph, as it would appear in the body of the
# text, in boldface.
sub generate_bold_paragraph {
    my ($self, $text) = @_;

    my %formatting = ('bold' =>  "true");

    return $self->generate_body_text_paragraph($text, \%formatting);
}

# Emits a standard paragraph, as it would appear in the body of the
# text, underlined.
sub generate_underlined_paragraph {
    my ($self, $text) = @_;

    my %formatting = ('underline' =>  "true");

    return $self->generate_body_text_paragraph($text, \%formatting);
}

# Emits a block of text.
# Arguments:
#
# text - string to be emited
# formatting - reference to a hash containing format info
sub generate_paragraph {
    my ($self, $text, $formatting) = @_;

    my $logger = get_logger(__PACKAGE__);
    
    $logger->error_die("this method must be defined in the subclass");
}

sub generate_bullet_text {
    my ($self, $text) = @_;

    my $logger = get_logger(__PACKAGE__);

    $logger->error_die("this method must be defined in the subclass");
}

sub generate_numbered_text {
    my ($self, $text, $number) = @_;

    my $logger = get_logger(__PACKAGE__);

    $logger->error_die("this method must be defined in the subclass");
}

# You should overwrite this in your implementation.  Default behavior
# is to do nothing.
sub generate_hyperlink {
    my ($self, $text, $url) = @_;
}

# Creates the white space that precedes a header, be that simple
# vertical space, a new page, or nothing at all.
# Arguments:
#
# depth - depth in event hierarchy
# text - string, containnig header (including section number, if required)
# formatting (optional) - formatting directives for the output text
sub generate_header_initial_whitespace {
    my ($self, $depth) = @_;
    my %additional_formatting = ();
    if (scalar(@_) > 3) {
	my $ref_formatting =  $_[3];
	%additional_formatting = %{$ref_formatting};
    }

    if ($depth==0) {
	if ($book_flag && !$first_page_flag) {
	    $self->generate_page_break(\%additional_formatting);
	}
    } else {
	$self->generate_vertical_space(1, \%additional_formatting);
    }
}

# Treat the supplied text as a title, centred, large, bold.
# Arguments:
#
# text - string, containnig header (including section number, if required)
# formatting (optional) - formatting directives for the output text
sub generate_title {
    my ($self, $text) = @_;
    my %additional_formatting = ();
    if (scalar(@_) > 2) {
	my $ref_formatting =  $_[2];
	%additional_formatting = %{$ref_formatting};
    }

    my %formatting = (
	'voodoo' =>  "true",
	'bind_next_para' =>  "true",
	'bold' =>  "true",
	'font' =>  font,
	'justify' =>  "center",
	'font_size' =>  $self->document_title_font_size,
    );

    # Add any additional formatting that might have comve via an argument.
    foreach my $key (keys(%additional_formatting )) {
	$formatting{$key} = $additional_formatting{$key};
    }

    my $new_page_count = $self->generate_paragraph($text, \%formatting);

    $additional_formatting{'font_size'} = $self->regular_text_font_size + 4;
    $self->generate_vertical_space(1, \%additional_formatting);

    return $new_page_count;
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
    my %additional_formatting = ();
    if (scalar(@_) > 3) {
	my $ref_formatting =  $_[3];
	%additional_formatting = %{$ref_formatting};
    }

    $self->generate_header_initial_whitespace($depth, $text, \%additional_formatting);

    # Get standard header formatting
    my %formatting = $self->header_formatting($depth);

    # Add any additional formatting that might have comve via an argument.
    foreach my $key (keys(%additional_formatting )) {
	$formatting{$key} = $additional_formatting{$key};
    }

    my $new_page_count = $self->generate_paragraph($text, \%formatting);

    $self->generate_vertical_space(1, \%additional_formatting);

    return $new_page_count;
}

# Treat the supplied text as a section-internal header.
# Arguments:
#
# text - string, containnig header (including section number, if required)
# formatting (optional) - formatting directives for the output text
sub generate_section_internal_header {
    my ($self, $text) = @_;
    my %formatting = ();
    if (scalar(@_) > 2) {
	my $ref_formatting =  $_[2];
	%formatting = %{$ref_formatting};
    }

    $formatting{'bold'} = "true";
    $formatting{'bind_next_para'} = "true";
    $formatting{'font_size'} = $self->header_3_font_size;

    $self->generate_vertical_space(1, \%formatting);

    my $new_page_count = $self->generate_paragraph($text, \%formatting);

    #$self->generate_vertical_space(1, \%formatting);

    return $new_page_count;
}

# Generate the formatting needed for a header, based on the depth
# in the hierarchy: font size is adjusted according to depth in
# event hierarchy, the deeper, the smaller.
# Arguments:
#
# depth - depth in event hierarchy
#
# Returns a hash, containing formatting information
sub header_formatting {
    my ($self, $depth) = @_;

    my $font_size = $self->calculate_header_font_size($depth);
    my %formatting = (
	'voodoo' =>  "true",
	'bind_next_para' =>  "true",
	'bold' =>  "true",
	'font' =>  font,
	'font_size' =>  $font_size,
    );

    return %formatting;
}

# Calculate the font size needed for a header, based on the depth
# in the hierarchy: font size is adjusted according to depth in
# event hierarchy, the deeper, the smaller.
# Arguments:
#
# depth - depth in event hierarchy
#
# Returns a hash, containing formatting information
sub calculate_header_font_size {
    my ($self, $depth) = @_;

    # Make font size depth-dependent
    my $font_size = $self->header_4_font_size;
    if ($depth==0) {
        $font_size = $self->header_1_font_size;
    } elsif ($depth==1) {
        $font_size = $self->header_2_font_size;
    } elsif ($depth==2) {
        $font_size = $self->header_3_font_size;
    }

    return $font_size;
}

# Generates the bulk of the book text, chapter by chapter.
#
# Returns nothing
sub generate_chapters {
    my ($self) = @_;
    
    my $logger = get_logger(__PACKAGE__);
    
    my $text_unit;
    while (1) {
	$text_unit = $reader->get_next_text_unit(0, $self->get_depth_limit(), $self->get_include_images_flag());
	
	if (!(defined $text_unit)) {
	    $logger->warn("yikes, text_unit is undef!!");
	}

	if ($text_unit->isa("eof")) {
	    last;
	}
	
	# Use page breaks to determine where to split the document into
	# chapters, if this is required by the user.
	if ($split_into_files_flag && $text_unit->isa("section_header") && $text_unit->get_depth() == 0 && $book_flag) {
	    $self->close();
	    $split_into_files_counter++;
	    $self->open($split_into_files_counter);
	    $self->generate_prolog();
	    $self->generate_page_numbering();
	}
	
	$self->generate_text_unit($text_unit);
    }
}

# TextUnit emitter.  Depending on the type of the supplied text unit, emits the
# contents in an appropriate way.
sub generate_text_unit {
    my ($self, $text_unit) = @_;

    my $logger = get_logger(__PACKAGE__);
    
    # Decide how to emit the text unit, depending on its type
    if ($text_unit->isa("section_header")) {
	$self->generate_header($text_unit->get_depth(), $text_unit->get_contents());
    } elsif ($text_unit->isa("section_title")) {
	$self->generate_title($text_unit->get_contents());
    } elsif ($text_unit->isa("section_internal_header")) {
	$self->generate_section_internal_header($text_unit->get_contents());
    } elsif ($text_unit->isa("body_text_paragraph")) {
	$self->generate_body_text_paragraph($text_unit->get_contents());
    } elsif ($text_unit->isa("body_text_paragraph_binding")) {
	my %formatting = ('bind_next_para' =>  "true");
	$self->generate_body_text_paragraph($text_unit->get_contents(), \%formatting);
    } elsif ($text_unit->isa("centered_paragraph")) {
	my %formatting = ('justify' =>  "center");
	$self->generate_body_text_paragraph($text_unit->get_contents(), \%formatting);
    } elsif ($text_unit->isa("body_text")) {
	$self->generate_body_text($text_unit->get_contents());
    } elsif ($text_unit->isa("bullet_text")) {
	$self->generate_bullet_text($text_unit->get_contents());
    } elsif ($text_unit->isa("numbered_text")) {
	$self->generate_numbered_text($text_unit->get_contents(), $text_unit->get_number());
    } elsif ($text_unit->isa("hyperlink")) {
	$self->generate_hyperlink($text_unit->get_contents(), $text_unit->get_url());
    } elsif ($text_unit->isa("vertical_space")) {
	$self->generate_vertical_space($text_unit->get_contents());
    } elsif ($text_unit->isa("image_file_name")) {
	my $contents = $text_unit->get_contents();
	$self->generate_image_from_file($contents->[0], $contents->[1]);
    } elsif ($text_unit->isa("image")) {
	$self->generate_image($text_unit->get_contents());
    } elsif ($text_unit->isa("vector_graphics_file_name")) {
	$self->generate_vector_graphics_from_file($text_unit->get_contents());
    } elsif ($text_unit->isa("image_or_vector_graphics_file_name")) {
	# Try to generate an image from vector graphics first,
	# and if it doesn't work, resort to a regular image.
	# This is a kludge to get around the fact that I
	# know how to generate vector graphics for PDF but
	# not for RTF.  In this case, "contents" is an array
	# with two members, the first member is the name of
	# a file containing vector graphics, the second is
	# a file containing an image.
	my $contents = $text_unit->get_contents();
	unless ($self->generate_vector_graphics_from_file($contents->[0])) {
	    $self->generate_image_from_file($contents->[1], 0);
	}
    } elsif ($text_unit->isa("empty") || $text_unit->isa("eof")) {
    	# Nothing to do, yahoo!!
    	$logger->info("doing nothing, how relaxing");
    } else {
	$logger->warn("strewf mate, never erd of a " . ($text_unit->get_type() || 'UNKNOWN') . " before, cant print that, more than me jobs worf");
    }

    # Let's see if we can free up some memory
    $text_unit->destroy();
}

# Can be  used to flush buffers and stuff.  In thi root class,
# this is just a dummy.
#
# Returns nothing
sub update {
    my ($self) = @_;

}

# Can be  used to flush buffers and stuff.  In thi root class,
# this is just a dummy.
#
# Returns nothing
sub finish_page {
    my ($self, $page) = @_;
}


# Take any markup in the input text and convert it into
# something innocuous.
#
# You may wish to overwite this.
sub interpret_markup {
    my ($self, $text) = @_;
    
    my $logger = get_logger(__PACKAGE__);

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
	if ($line eq "") {
	    next;
	}
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
    my $new_text = '';
    foreach my $line3 (@lines3) {
	if ($line3 =~ /^</) {
	    # We have markup
	    if ($line3 =~ /^<img/) {
		$line3 =~ s/^<img[^>]*>//;
    		$new_text .= $line3; # Remove image markup
	    } elsif ($line3 =~ /^<b>$/) {
		# Start bold
		$new_text .= ""; # Do nothing
	    } elsif ($line3 =~ /^<\/b>$/) {
		# Stop bold
		$new_text .= ""; # Do nothing
	    } elsif ($line3 =~ /^<i>$/) {
		# Start italics
		$new_text .= ""; # Do nothing
	    } elsif ($line3 =~ /^<\/i>$/) {
		# Stop italics
		$new_text .= ""; # Do nothing
	    } elsif ($line3 =~ /^<td>$/i) {
		# Start table cell
		$new_text .= ""; # Do nothing
	    } elsif ($line3 =~ /^<\/td>$/i) {
		# Stop table cell
		$new_text .= ""; # Do nothing
	    } else {
		# unknown markup, or maybe not markup at all
		$logger->info("possible unknown markup, line3=$line3, text=$text");
		$new_text .= ""; # Do nothing
	    }
	} else {
	    # We have plain text
	    $new_text .= $line3;
	}
    }
    
    return $new_text;
}

1;
