package GKB::DocumentGeneration::GenerateTextPDF;

=head1 NAME

GKB::DocumentGeneration::GenerateTextPDF

=head1 SYNOPSIS

Plugin for producing PDF

=head1 DESCRIPTION

A Perl utility module for the Reactome book package, implementing
GenerateText.  This class provides the specialized methods
needed for producing PDF.

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

use PDF::API2;
use PDF::API2::Util;
use PDF::API2::Page;

use constant mm => 25.4/72;
use constant in =>    1/72;
use constant pt =>    1;

use GKB::Config;
use GKB::DocumentGeneration::GenerateText;
use Log::Log4perl qw/get_logger/;
Log::Log4perl->init(\$LOG_CONF);

@ISA = qw(GKB::DocumentGeneration::GenerateText);

my $output_file_name;
my $max_recursion_depth = 20; # to prevent runaway recursions

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
}

# Needed by subclasses to gain access to class variables defined in
# this base class.
sub get_ok_field {
	my ($pkg) = @_;
	
	%ok_field = $pkg->SUPER::get_ok_field();
	$ok_field{"pdf"}++;
	$ok_field{"sheet_size"}++;
	$ok_field{"paragraph_size"}++;
	$ok_field{"margins"}++;
	$ok_field{"page"}++;
	$ok_field{"pg_num"}++;
	$ok_field{"pnum_mode"}++;
	$ok_field{"position"}++;
	$ok_field{"image_file_names"}++;
	$ok_field{"header"}++;
	$ok_field{"hdr_position"}++;
	$ok_field{"numbering_of_header"}++;
	$ok_field{"toc_pg_nm"}++;

	return %ok_field;
}

# Get the stored current page, if there is one, otherwise create a fresh one.
sub get_current_page {
    my ($self) = @_;

    my $page = $self->page;

    unless ($page) {
	my $pdf = $self->pdf;
	my @sheet_size = $self->get_sheet_size();

	$page = $pdf->page;
	$page->mediabox($sheet_size[0], $sheet_size[1]);
	$self->page($page);
    }

    return $page;
}

# Open stream to RTF file
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

	$output_file_name = $filename;
	
    # Add an appropriate extension, if it doesn't already exist
    unless ($output_file_name =~ /\.pdf$/i) {
		$output_file_name .= ".pdf";
    }

    my $pdf = PDF::API2->new(-file => $output_file_name);
    $self->pdf($pdf);
}

# Close stream to PDF file
sub close {
    my ($self) = @_;

    my $pdf = $self->pdf;

    # Now at last we can generate the table of contents
    $self->insert_pdf_toc_page();

    $pdf->save;
    $pdf->end;

    # Delete the image files, which we no longer need
    my $image_file_names = $self->image_file_names;
    if ($image_file_names) {
	foreach my $image_file_name (@{$image_file_names}) {
	    unlink($image_file_name);
	}
    }

    # ...if the user wants to dump to a stream, cat the contents
    # of the temporary file
    my $output_file_stream = $self->get_output_file_stream();
    if ($output_file_stream) {
		if (CORE::open(TEMPFILE, "<$output_file_name")) {
		    while (<TEMPFILE>) {
				print $output_file_stream $_;
		    }
		    CORE::close(TEMPFILE);
		}
	
		# Get rid of temporary o/p file
		unlink($output_file_name);
    }
}

# Document meta-data
sub generate_prolog {
    my ($self, $author, $company, $title, $subject) = @_;

    my $pdf = $self->pdf;

    my $depth_limit = $self->get_depth_limit();
    my @sheet_size = $self->get_sheet_size();
    my @margins = $self->get_margins();

    if ($author && $title && $subject) {
	$pdf->info(
		   'Author' =>  $author,
		   'Title' =>   $title ,
		   'Subject' => $subject
		   );
    }

    $self->sheet_size(\@sheet_size);
    $self->margins(\@margins);

    my $paragraph_width = $sheet_size[0] - $margins[0] - $margins[2];
    my $paragraph_depth = $sheet_size[1] - $margins[1] - $margins[3];
    my @paragraph_size = ($paragraph_width, $paragraph_depth);
    $self->paragraph_size(\@paragraph_size);

    $self->pg_num(1);
    $self->pnum_mode(0);
    $self->toc_pg_nm(-1); # No TOC by default

    my @hdr_position = ($margins[0], $sheet_size[1] - ($margins[3]/2));
    $self->hdr_position(\@hdr_position);

    $self->position($self->top_left_corner());

    $self->header(undef);

    # Kick off the array relating section headers to page numbers
    # Each element of the array corresponds to one line in the
    # TOC.  Each element is in turn an array, with 3 terms:
    #
    # 1. Page number
    # 2. Depth in hierarchy
    # 3. Section title, including section number
    my @numbering_of_header = ();
    $self->numbering_of_header(\@numbering_of_header);
}

sub generate_page_numbering {
    my ($self, $starting_page_number) = @_;

    my $reader = $self->get_reader();
    my $title = $reader->get_title();
    my $subject = $reader->get_subject();

    my $header = "$title: $subject";
    $self->header($header);
}

# Emits a page containing the table of contents
# Arguments:
#
# toc_depth - cutoff depth for the TOC, below which no headers will be
#             output. -1 means go right to bottom of hierarchy.
#
# This method overwrites the superclass, because the TOC will not be
# generated until the end.
sub generate_toc_page {
    my ($self, $toc_depth) = @_;

    # Note page number where TOC should be inserted
    $self->toc_pg_nm($self->pg_num);
}

# Prints out the raw table of contents
sub generate_toc {
    my ($self, $toc_depth) = @_;

    my $logger = get_logger(__PACKAGE__);

    $logger->warn("GenerateTextPDF.generate_toc: WARNING - this subroutine should never be used during PDF generation!!");
}

# Get the Helvatica Latin font.  If it has already been got before,
# use a cached version.
#
# This turned out to be the trick that stopped PDF creation from
# doing core dumps and issuing "out of memory" errors.  Ours is
# not to question why.  It also reduces the size of the PDF file by
# a factor of 5 or more.
my %font_helvetica_latin_hash = ();
sub get_font_helvetica_latin {
    my ($self, $font_name) = @_;
    
    if (!(defined $font_name)) {
    	$font_name = "Helvetica";
    }

    if (!(defined $font_helvetica_latin_hash{$font_name})) {
		my $pdf = $self->pdf;
		$font_helvetica_latin_hash{$font_name} = $pdf->corefont($font_name, -encoding => 'latin1');
    }

    return $font_helvetica_latin_hash{$font_name};
}

# This is called AFTER all the rest of the text has been generated, allowing
# TOC information to be collected first.  It uses toc_pg_nm to decide where
# to insert the TOC, so make sure this variable has been set before you
# call this subroutine!
sub insert_pdf_toc_page {
    my ($self) = @_;

    my $toc_pg_nm = $self->toc_pg_nm;

    # A negative TOC page number is a flag saying "don't insert a
    # table of contents".
    if ($toc_pg_nm < 0) {
	return;
    }

    # Run the TOC generator on a "dummy" text block first, to
    # find out how many pages are needed by the TOC.  The "dummy"
    # text block does not generate any PDF.
    my $pdf = $self->pdf;
    my $dummy_text_block = PDF::API2::Content->new();
    my $font = $self->get_font_helvetica_latin();
    $dummy_text_block->font($font, 8);
    my $new_page_count = $self->generate_pdf_toc_page($dummy_text_block, 0);

    # Reset the page index, so that the TOC gets inserted near the
    # front of the document
    $self->pg_num($toc_pg_nm);
    $self->pnum_mode(1);

    # Now insert the TOC for real
    $self->generate_pdf_toc_page(undef, $new_page_count+1);

    # Finally, add page numbering and page headers to those pages that
    # need them.
    $self->generate_page_top_headers();
}

# Emits a TOC page at the current page.
# Arguments:
#
# text_block - the text block to write to.
sub generate_pdf_toc_page {
    my ($self, $text_block, $pg_num_increment) = @_;

    my %formatting;
    %formatting = ('text_block' => $text_block);

    my $new_page_count = $self->generate_header(0, "Table of Contents", \%formatting);

    # Rotate contents of TOC array 1 element to the right, because
    # the TOC page will be at the end, whereas it needs to be at
    # the beginning.  Only do this if we are emiting to a live PDF
    # stream.
    unless ($text_block) {
	my @numbering_of_header = @{$self->numbering_of_header};
	my $toc_numbering_of_header_element = pop @numbering_of_header;

	$toc_numbering_of_header_element->[0] -= $pg_num_increment; # will be added back later!
	unshift @numbering_of_header, $toc_numbering_of_header_element;

	$self->numbering_of_header(\@numbering_of_header);
    }

    my $pdf = $self->pdf;

    my $position;
    my $new_position;
    my $pg_num;
    my $depth;
    my $text;
    my $left_indent;

    my $dummy_text_block = PDF::API2::Content->new();
    my $font = $self->get_font_helvetica_latin();
    $dummy_text_block->font($font, 8);
    my $space_width = $dummy_text_block->advancewidth('   ');

    # Emit the TOC, line by line, dealing with indentation and positioning
    # of page numbers.
    foreach my $numbering_of_header_element (@{$self->numbering_of_header}) {
	$pg_num = $numbering_of_header_element->[0];

	$depth = $numbering_of_header_element->[1];
	$text = $numbering_of_header_element->[2];

	# Print text, i.e. header title and section number.  Left indent
	# gives visual cue to depth in hierarchy, right indent leaves a
	# bit of space to print the page number.
	$position = $self->position;
	$left_indent = $depth * $space_width;

	%formatting = ('text_block' => $text_block, 'left_indent' => $left_indent, 'right_indent' => 3/mm, 'no_separation' => 1);
	$new_page_count += $self->generate_body_text_paragraph($text, \%formatting);
	$new_position = $self->position;

	# Print page number where section starts.  Note that position
	# must be reset, so that the number is placed on the same
	# line as the text.  Also note the increment: this bumps up
	# the reported page number so that the additional pages taken
	# up by the TOC itself is taken into account.
	$self->position($position);
	%formatting = ('text_block' => $text_block, 'justify' => "right", 'no_page_break' => 1);
	$self->generate_body_text_paragraph($pg_num + $pg_num_increment, \%formatting);
	$self->position($new_position);
    }

    return $new_page_count;
}

# Inserts page top headers into all pages from toc_pg_nm onwards
sub generate_page_top_headers {
    my ($self) = @_;

    my $pdf = $self->pdf;
    my $page;

    for (my $i=$self->toc_pg_nm+1; $i<$pdf->pages; $i++) {
	$page = $pdf->openpage($i);
	$self->page($page);
	$self->pg_num($i);
	$self->generate_page_top_header();
    }
}

# Inserts a page top header into the current page
sub generate_page_top_header {
    my ($self) = @_;

    my $position = $self->hdr_position;
    my $font_size = 12;
    my %formatting = ('italic' =>  "true", 'font_size' => $font_size, 'foreground' => "blue");

    my $header = $self->header;
    if ($header) {
	my %header_formatting = %formatting;
	$header_formatting{'justify'} = "center";
	$self->position($position);
	$self->generate_body_text_paragraph($header, \%header_formatting);
    }

    my %page_number_formatting = %formatting;
    $page_number_formatting{'justify'} = "right";
    $self->position($position);
    $self->generate_body_text_paragraph($self->pg_num, \%page_number_formatting);
}

# Emit a paragraph containing the supplied image file
sub generate_image {
    my ($self, $image, $filename) = @_;
    
    my $logger = get_logger(__PACKAGE__);

    if (!(defined $image)) {
        $logger->warn("GenerateTextPDF.generate_image: WARNING - no image!!");
	return;
    }

    # Dump image into temporary file
    my $image_file_name = GKB::FileUtils->print_image($image, undef, 1);

    if (!(defined $image_file_name)) {
        $logger->warn("GenerateTextPDF.generate_image: WARNING - no image file name!!");
        return;
    }

	my $pdf = $self->pdf;
	my $position = $self->position;
	my $page = $self->get_current_page();
	my $margins = $self->margins;
	my $paragraph_size = $self->paragraph_size;
	
	# Get unscaled size of image
	my $width;
	my $height;
	($width, $height) = $image->getBounds();
	
	# Find a scaling for the image, so that it ia i) visible and
	# ii) not so big that it doesn't fit onto the page.
	#
	# The fudge factor is there because otherwise the images have a
	# tendency to cover some of the subsequent text, especially if
	# a new page has to be thrown in order to fit the image onto
	# a side.  I havn't been able to work out why this is, so until
	# I do, the fudge factor will live on.
	my $scale = 0.78 * $self->find_scale_to_fit_image_to_page($width, $height);
	
	# Work out where the text cursor will be once the imge has been
	# inserted
	my @new_position = ($position->[0] + (($paragraph_size->[0] - ($scale * $width)) / 2), $position->[1] - ($scale * $height));
	
	# Check to see if image flops over the bottom of the page
	if ($new_position[1] < $margins->[1]) {
		$self->generate_page_break();
		$page = $self->page;
		$position = $self->position;
		@new_position = ($position->[0] + (($paragraph_size->[0] - ($scale * $width)) / 2), $position->[1] - ($scale * $height));
	}
	
	if ($new_position[1] < $margins->[1]) {
	    $logger->warn("GenerateTextPDF.generate_image: WARNING - image is still flopping over edge of page!!!");
	}
	
	my $pdf_ok = 0;
	eval {
		# Insert image into PDF
		my $graphic = $page->gfx;
		
		# TODO: specify more image types!
		my $image_file=undef;
		if ($image_file_name =~ /\.png$/i) {
			$image_file=$pdf->image_png($image_file_name);			
		} elsif ($image_file_name =~ /\.jpg/i || $image_file_name =~ /\.jpeg/i) {
			$image_file=$pdf->image_jpeg($image_file_name);
		} else {
			$logger->warn("GenerateTextPDF.generate_image: WARNING - unknown image type for image_file_name=$image_file_name");
		}
	
		if (defined $image_file) {
			$graphic->image($image_file, $new_position[0], $new_position[1], $scale);
			$pdf_ok = 1;
		}
	}; # eval
	
	if ($pdf_ok == 0) {
	    $logger->warn("GenerateTextPDF.generate_image: WARNING - problem occured while trying to insert image into PDF");
	}
	
	
	# Save file name for later deletion - the file needs
	# to stay in existence until after the PDF document
	# has been saved.
	my $image_file_names = $self->image_file_names;
	unless ($image_file_names) {
		my @new_array = ();
		$image_file_names = \@new_array;
	}
	push @{$image_file_names}, $image_file_name;
	$self->image_file_names($image_file_names);
	
	# Update position
	$new_position[0] = $position->[0];
	$self->position(\@new_position);
	$self->generate_vertical_space(1);
}

# Emit a paragraph containing the supplied vector graphics file.
# Returns 1 if it succeeds, 0 otherwise.
sub generate_vector_graphics_from_file {
    my ($self, $filename) = @_;

    my $logger = get_logger(__PACKAGE__);

    unless ($filename) {
#        print STDERR "generate_vector_graphics_from_file: WARNING - no image!!\n";
	return 0;
    }

    # Open the file containing the vector graphics to be imported
    my $vg_pdf = PDF::API2->open($filename);
    unless ($vg_pdf) {
	$logger->warn("GenerateTextPDF.generate_vector_graphics_from_file: WARNING - could not open file to read PDF!!");
	return 0;
    }

    my $pdf = $self->pdf;

    # Extract the first page from the import file - it is assumed
    # that the file will only have one page anyway.
    my $form = $pdf->importPageIntoForm($vg_pdf, 1);
    unless ($form) {
	$logger->warn("GenerateTextPDF.generate_vector_graphics_from_file: WARNING - form is null!!");
	return 0;
    }

    my $position = $self->position;
    my $page = $self->get_current_page();
    my $margins = $self->margins;
    my $paragraph_size = $self->paragraph_size;

    my @bbox = $form->bbox();

    my $llx = $bbox[0];
    my $lly = $bbox[1];
    my $urx = $bbox[2];
    my $ury = $bbox[3];

#    print STDERR "generate_vector_graphics_from_file: llx=$llx, lly=$lly, urx=$urx, ury=$ury\n";

    # Get unscaled size of image
    my $width = $urx - $llx;
    my $height = $ury - $lly;

    # Find a scaling for the image, so that it ia i) visible and
    # ii) not so big that it doesn't fit onto the page.
    my $scale = $self->find_scale_to_fit_image_to_page($width, $height);

    # Work out where the text cursor will be once the imge has been
    # inserted
    my @new_position = ($position->[0] + (($paragraph_size->[0] - ($scale * $width)) / 2), $position->[1] - ($scale * $height));
    
    # Check to see if image flops over the bottom of the page
    if ($new_position[1] < $margins->[1]) {
	$self->generate_page_break();
	$page = $self->page;
	$position = $self->position;
	@new_position = ($position->[0] + (($paragraph_size->[0] - ($scale * $width)) / 2), $position->[1] - ($scale * $height));
    }

    if ($new_position[1] < $margins->[1]) {
	$logger->warn("GenerateTextPDF.generate_vector_graphics_from_file: WARNING - image is still flopping over edge of page!!!");
    }

    my $gfx = $page->gfx;
    $gfx->formimage($form, $new_position[0] - ($scale * $llx), $new_position[1] - ($scale * $lly), $scale);

    # Update position
    $new_position[0] = $position->[0];

    $self->position(\@new_position);

    $self->generate_vertical_space(1);

    return 1;
}

sub read_pdf_core {
    my ($self, $filename) = @_;

    my $content = GKB::FileUtils->read_file($filename);

    unless ($content) {
	return $content;
    }

    my $core = "";
    my @lines = split("\n", $content);
    my $read_flag = 0;
    my $preread_flag1 = 0;
    my $preread_flag2 = 0;
    foreach my $line (@lines) {
	if ($line =~ /EOF/) {
	    last;
	} elsif ($read_flag) {
	    $core .= $line . "\n";
	} elsif ($line =~ /EndComments/) {
	    $preread_flag1 = 1;
	} elsif ($preread_flag1 && $line =~ /endstream/) {
	    $preread_flag2 = 1;
	} elsif ($preread_flag2 && $line =~ /endobj/) {
	    $read_flag = 1;
	}
    }

    return @lines;#$core;
}

# Find image scaling so that it fits within a page, but also so that it
# isn't so tiny that you can't see it.
# Arguments:
#
# width
# height
#
# Returns scaling value
sub find_scale_to_fit_image_to_page {
    my ($self, $width, $height) = @_;
    
    my $logger = get_logger(__PACKAGE__);

    my $paragraph_size = $self->paragraph_size;
    my $max_width = $paragraph_size->[0];

    my $reactome_scale = $max_width/$width;

    # Don't enlarge images too drastically, otherwise you get
    # horrible pixel blurring effects
    if ($width <= (1.05 * $max_width) && $width >= $max_width/2) {
	$reactome_scale = 1.0;
    } elsif ($reactome_scale>1.5) {
	$reactome_scale = 1.5;
    }

    if ($reactome_scale * $height > $paragraph_size->[1]) {
	$logger->warn("GenerateTextPDF.scale_image_to_fit_page: WARNING - new image height=" . $reactome_scale * $height . " is bigger than paragraph height=" . $paragraph_size->[1]);
    }

    return $reactome_scale;
}

sub generate_page_break {
    my ($self) = @_;
    my %additional_formatting = ();
    if (scalar(@_) > 1) {
		my $ref_formatting =  $_[1];
		%additional_formatting = %{$ref_formatting};
    }

    # An explicitly defined text block is assumed to be a dummy, and
    # should therefore not be allowed to cause a change to the PDF
    # o/p stream
    unless ($additional_formatting{"text_block"}) {
		my $pdf = $self->pdf;
		my $sheet_size = $self->sheet_size;
	
		my $pg_num = $self->pg_num;
		my $pnum_mode = $self->pnum_mode;
	
		# Bump up the page count
		$pg_num ++;
		$self->pg_num($pg_num);
	
		my $page;
		if ($pnum_mode) {
		    # gets new page based on current page number
		    $page = $pdf->page($pg_num);
		} else {
		    $self->finish_page(); # write out original page
		    $page = $pdf->page; 
		}
	
		$page->mediabox($sheet_size->[0], $sheet_size->[1]);
	
		$self->page($page);
    }

    $self->position($self->top_left_corner());
}

# Return coordinates of tiop left corner of page
# Returns an array (x,y)
sub top_left_corner {
    my ($self) = @_;

    my $sheet_size = $self->sheet_size;
    my $margins = $self->margins;

    my @position = ($margins->[0], $sheet_size->[1] - $margins->[3]);

    return(\@position);
}

# Emits a block of text.
# Arguments:
#
# text - string to be emited
# formatting - reference to a hash containing format info
#
# Returns the total number of new pages that were needed to hold the
# entire text (will generally be 0).
sub generate_paragraph {
    my ($self, $text, $formatting, $recursion_depth) = @_;

    my $logger = get_logger(__PACKAGE__);
    
    if (!(defined $recursion_depth)) {
        $recursion_depth  = 0;
    }
    $recursion_depth++;

    $text = $self->interpret_markup($text);

    my $pdf = $self->pdf;
    my $page = $self->get_current_page();
    my $sheet_size = $self->sheet_size;
    my $margins = $self->margins;
    my $position = $self->position;

    # Translates from the Reactome formatting hash to a list of
    # PDF formatting directives
    my $font_size = 6;
    my $justify = 'left';
    my $font_name = "Helvetica";
    my $bold = "";
    my $italic = "";
    my $underline = "";
    my $center = 0;
    my $left_indent = 0;
    my $right_indent = 0;
    my $foreground_color = "black";
    my $no_separation = 0;
    my $text_block;
    my $bind_flag = 0;
    my $no_page_break = 0;
    foreach my $format_key (keys(%{$formatting})) {
	if ($format_key eq "font") {
	    # Set the font
	    $font_name = $formatting->{$format_key};
	} elsif ($format_key eq "font_size") {
	    # Set the font size
	    $font_size = $formatting->{$format_key};
	} elsif ($format_key eq "bold") {
	    # Set boldface
	    $bold = 1;
	} elsif ($format_key eq "italic") {
	    # Set italic
	    $italic = 1;
	} elsif ($format_key eq "underline") {
	    # Set underline
	    $underline = 1;
	} elsif ($format_key eq "left_indent") {
	    # Set left indent
	    $left_indent = $formatting->{$format_key};
	} elsif ($format_key eq "right_indent") {
	    # Set right indent
	    $right_indent = $formatting->{$format_key};
	} elsif ($format_key eq "first_line_indent") {
	    # Set first line extra left indent
	    $logger->warn("GenerateTextPDF.generate_paragraph: WARNING - do not know how to set first line extra indent");
	} elsif ($format_key eq "justify") {
	    # Set the justification
	    $justify = $formatting->{$format_key};
	} elsif ($format_key eq "bind_next_para") {
	    # Bind to next paragraph (don't allow page break)
	    $bind_flag = 1;
	} elsif ($format_key eq "no_page_break") {
	    # Bind to next paragraph (don't allow page break)
	    $no_page_break = 1;
	} elsif ($format_key eq "foreground") {
	    # Set foreground color
	    $foreground_color = $formatting->{$format_key};
	} elsif ($format_key eq "no_separation") {
	    # Determine if paragraph should be vertically separated from next paragraph
	    $no_separation = $formatting->{$format_key};
	} elsif ($format_key eq "voodoo") {
	    # Set mysterious command that magically makes things work
	    # ...Voodoo has no effect on PDF, it's only superstition, isn't it?
	} elsif ($format_key eq "text_block") {
	    # Sets text block - only understood by PDF
	    $text_block = $formatting->{$format_key};
	} else {
	    $logger->warn("GenerateTextPDF.generate_paragraph: WARNING - format_key=$format_key not recognised, skipping!");
	    next;
	}
    }

    # Build font style into font name (this is the PDF way)
    if ($bold) {
	$font_name .= "-Bold";
    }
    if ($italic && !$bold) {
	if ($font_name =~ /^Helvetica/) {
	    $font_name .= "-Oblique";
	} else {
	    $font_name .= "-Italic";
	}
    }

    # Underlining is actually done during paragraph generation.  Here,
    # we just work out the distance of the underline below the text,
    # will be applied later.
    my $underline_distance = (-1);
    if ($underline) {
	$underline_distance = 0.1 * $font_size;
	if ($underline_distance < 1.0) {
	    $underline_distance = 1;
	}
    }

    my $font = $self->get_font_helvetica_latin($font_name);

    # $lead is the height of 1 line of text, including inter-line
    # separation.
    my $lead = 1.1 * $font_size;
    if ($lead == $font_size) {
	$lead = $font_size + 2;
    }

    # Get a new text block from the current page and set some of it's
    # parameters
    my $imported_text_block_flag = 0;
    if ($text_block) {
		$imported_text_block_flag = 1;
    } else {
		$text_block = $page->text;
    }
    $text_block->fillcolor($foreground_color);
    $text_block->font($font, $font_size);

    # Some of the parameters needed by generate_text_block are
    # optional - build up the options that are needed for a nice
    # paragraph.
    my %options = (-align => $justify, -lead  => $lead);
    if ($underline_distance > 0) {
		$options{"-underline"} = $underline_distance;
    }

    # Determine width and depth needed by generate_text_block, taking
    # indents into account.
    my $paragraph_size = $self->paragraph_size;
    my $width = $paragraph_size->[0] - ($left_indent + $right_indent);
    my $depth = $paragraph_size->[1];

    # This is a weak attempt to simulate paragraph binding.  As long
    # as the current paragraph is relatively short, it will force
    # generate_text_block to throw a new page if it is too close to
    # the bottom of the page, so that it looks as though it is "bound"
    # to the paragraph that follows.  This helps to make section headings
    # behave in a more sensible way.
    my $page_bottom_clearance = 0;
    if ($bind_flag) {
	$page_bottom_clearance = 10* $lead;
    }

    # Now emit the text, at long last!
    my $ypos;          # y-position of last line printed
    my $overflow_text; # unprinted text
    ($ypos, $overflow_text) = $self->generate_text_block(
					    $text_block,
					    $text,
					    $position->[0] + $left_indent,
					    $position->[1],
					    $width,
					    $depth,
					    $page_bottom_clearance,
					    %options
					    );

    # Free up a bit of mem
    $text = undef;
#    foreach my $block_key (%{$text_block}) {
#	if ($block_key && $text_block->{$block_key}) {
##	    delete($text_block->{$block_key});
#	    undef $text_block->{$block_key};
#	}
#    }
#    $text_block = undef;

    # Create a new paragraph on a new page if there is any overflow text, otherwise,
    # add a bit of vertical space and store the new position.
    my $new_page_count = 0;
    if ($overflow_text && $recursion_depth < $max_recursion_depth) {
		# Start new paragraph on a fresh page, if:
		#
		# 1) the output goes to a live PDF stream, and
		# 2) the $no_page_break has not been set
		if ($imported_text_block_flag || $no_page_break) {
		    $self->position($self->top_left_corner());
		} else {
		    $self->generate_page_break();
		}
	
		# Now go do that paragraph!
		$new_page_count = $self->generate_paragraph($overflow_text, $formatting, $recursion_depth) + 1;
    } else {
		# Calculate a new position, based on the actual final y position
		# returned by generate_text_block
		my @new_position = ($position->[0], $ypos);
	
		# Advance y position by a newline, acts as paragraph separator
		my $y = $new_position[1] - $lead;
		if ($y > 0 && !$no_separation) {
		    $new_position[1] = $y;
		}
	
		# Stash new position
		$self->position(\@new_position);
    }

    $self->set_first_page_flag(0);

    return $new_page_count;
}

# This subroutine adapted from http://www.printaform.com.au/clients/pdfapi2/
# I use this instead of PDF::API2::Content->paragraph because it deals
# with line width better.
#
# Places the supplied string into the PDF::API2::Content::Text object,
# with appropriate formatting.
# Arguments:
#
# text_object - PDF::API2::Content::Text object
# text - text to be printed (assumed single paragraph)
# x - x posn top left corner
# y - y posn top left corner
# width
# height
# page_bottom_clearance - min allowed distance between bottom pf para and lower page margin
# arg - a hash of optional arguments
sub generate_text_block {
    my ($self, $text_block, $text, $x, $y, $width, $height, $page_bottom_clearance, %arg) = @_;

    my $margins = $self->margins;

    # calculate width of all words
    my %word_width_hash = ();
    my $space_width = $text_block->advancewidth(' ');
    my @words = split(" ", $text);
    foreach (@words) {
        next if exists $word_width_hash{$_};
        $word_width_hash{$_} = $text_block->advancewidth($_);
    }

    # "lead" is the distance from one line to the next.  This ought
    # to be a bit bigger than the point size of the text, so that
    # there is a bit of space between lines.
    my $lead;
    if (exists $arg{'-lead'}) {
	$lead = $arg{'-lead'};
    } else {
	$lead = 10/pt; # dumb default value
    }

    my $xpos;
    my $ypos = $y; # set position of first line
    my $first_line = 1;
    my $wordspace;
    my $align;
    my $endw = 0;

    # while we can add another line
    while ( $ypos >= $y - $height + $lead && $ypos > $margins->[1] + $page_bottom_clearance) {
        unless (@words) {
	    last;
        }
        
        $xpos = $x;

        # while there's room on the line, add another word
        my @line = ();

        my $line_width =0;
        if ($first_line && exists $arg{'-hang'}) {
            my $hang_width = $text_block->advancewidth($arg{'-hang'});

            $text_block->translate( $xpos, $ypos );
            $text_block->text( $arg{'-hang'} );

            $xpos         += $hang_width;
            $line_width   += $hang_width;
        }
        elsif ($first_line && exists $arg{'-flindent'}) {
            $xpos += $arg{'-flindent'};
            $line_width += $arg{'-flindent'};
        }
        elsif (exists $arg{'-indent'}) {
            $xpos += $arg{'-indent'};
            $line_width += $arg{'-indent'};
        }
                
                         
        while ( @words and $line_width + (scalar(@line) * $space_width) + $word_width_hash{$words[0]} < $width ) {
            $line_width += $word_width_hash{ $words[0] };
            push(@line, shift(@words));
        }
        
        # calculate the space width
        if ($arg{'-align'} eq 'fulljustify' or ($arg{'-align'} eq 'justify' and @words)) {
            if (scalar(@line) == 1) {
                @line = split(//,$line[0]);
            }
            $wordspace = ($width - $line_width) / (scalar(@line) - 1);
            $align='justify';
        } else {
            $align=($arg{'-align'} eq 'justify') ? 'left' : $arg{'-align'};
            $wordspace = $space_width;
        }
        $line_width += $wordspace * (scalar(@line) - 1);
        
        
        if ($align eq 'justify') {
            foreach my $word (@line) {
                $text_block->translate( $xpos, $ypos );
                $text_block->text( $word );
                $xpos += ($word_width_hash{$word} + $wordspace) if (@line);
            }
            $endw = $width;
        } else {
    
            # calculate the left hand position of the line
            if ($align eq 'right') {
                $xpos += $width - $line_width;
            } elsif ($align eq 'center') {
                $xpos += ($width/2) - ($line_width / 2);
            }
    
            # render the line
            $text_block->translate( $xpos, $ypos );
            $endw = $text_block->text( join(' ', @line) );
        }        
        $ypos -= $lead;
        $first_line = 0;
    }

    # Return:
    # 1. New y position (i.e. bottom of paragraph)
    # 2. Any text that couldn't be printed on the current page
    return ($ypos, join(" ", @words))
}

sub generate_bullet_text {
    my ($self, $text) = @_;

    my $original_position = $self->position;
    my $original_page = $self->page;

    # Do the text
    my %formatting = ('left_indent' =>  3/mm);
    $self->generate_body_text_paragraph($text, \%formatting);

    # Put a blob on the page (do this last, just in case a new page
    # has been thrown).
    my $pdf = $self->pdf;
    my $page = $self->get_current_page();

    my $font = $self->get_font_helvetica_latin();

    my $position = $original_position;
    if ($page != $original_page) {
	# New page got thrown
	$position = $self->top_left_corner()
	
    }

    my $text_block = $page->text;

    $text_block->font($font, 8);
    $text_block->translate($position->[0], $position->[1]);

    $text_block->text("*");
}

# TODO: this behaves exactly like generate_bullet_text, needs to
# be modified so that it inserts numbers instead!
sub generate_numbered_text {
    my ($self, $text) = @_;

    my $original_position = $self->position;
    my $original_page = $self->page;

    # Do the text
    my %formatting = ('left_indent' =>  3/mm);
    $self->generate_body_text_paragraph($text, \%formatting);

    # Put a blob on the page (do this last, just in case a new page
    # has been thrown).
    my $pdf = $self->pdf;
    my $page = $self->get_current_page();

    my $font = $self->get_font_helvetica_latin();

    my $position = $original_position;
    if ($page != $original_page) {
	# New page got thrown
	$position = $self->top_left_corner()
	
    }

    my $text_block = $page->text;

    $text_block->font($font, 8);
    $text_block->translate($position->[0], $position->[1]);

    $text_block->text("*");
}

# Treat the supplied text as a header.  Font size is adjusted
# according to depth in event hierarchy, the deeper, the smaller.
# Arguments:
#
# depth - depth in event hierarchy
# text - string, containnig header (including section number, if required)
#
# This method overwrites the superclass method, so that it can
# compile a list of headers from which the TOC will be generated.
sub generate_header {
    my ($self, $depth, $text) = @_;
    my %additional_formatting = ();
    if (scalar(@_) > 3) {
		my $ref_formatting =  $_[3];
		%additional_formatting = %{$ref_formatting};
    }

    my $new_page_count = $self->SUPER::generate_header($depth, $text, \%additional_formatting);

    # Store section header and page number information in order to
    # create the TOC later...but only do it if we are emiting to
    # a live PDF stream.
    unless ($additional_formatting{"text_block"}) {
		my $numbering_of_header = $self->numbering_of_header;
		my $numbering_of_header_element;
		$numbering_of_header_element->[0] = $self->pg_num;
		$numbering_of_header_element->[1] = $depth;
		$numbering_of_header_element->[2] = $text;
		push @{$numbering_of_header}, $numbering_of_header_element;
		$self->numbering_of_header($numbering_of_header);
    }

    return $new_page_count;
}

# Can be  used to flush buffers and stuff.
#
# Returns nothing
sub update {
    my ($self) = @_;

    my $pdf = $self->pdf;

    $pdf->update();
}

# Can be  used to flush buffers and stuff, after the current
# page has been filled.
#
# Returns nothing
sub finish_page {
    my ($self) = @_;

    my $pdf = $self->pdf;
    my $page = $self->page;

    if ($pdf && $page) {
#		$pdf->finishobjects($page);
		$self->destroy_page($page);
    }
}

# Fills the supplied page object with undefs.
#
# Returns nothing
sub destroy_page {
    my ($self, $page) = @_;

    if (!$page) {
		return;
    }

    $page->{' stream'}=undef;
    $page->{' poststream'}=undef;
    $page->{' font'}=undef;
    $page->{' fontset'}=undef;
    $page->{' fontsize'}=undef;
    $page->{' charspace'}=undef;
    $page->{' hspace'}=undef;
    $page->{' wordspace'}=undef;
    $page->{' lead'}=undef;
    $page->{' rise'}=undef;
    $page->{' render'}=undef;
    $page->{' matrix'}=undef;
    $page->{' textmatrix'}=undef;
    $page->{' textlinematrix'}=undef;
    $page->{' fillcolor'}=undef;
    $page->{' strokecolor'}=undef;
    $page->{' translate'}=undef;
    $page->{' scale'}=undef;
    $page->{' skew'}=undef;
    $page->{' rotate'}=undef;
    $page->{' apiistext'}=undef;
}

1;
