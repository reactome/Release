package GKB::DocumentGeneration::HTMLReader;

=head1 NAME

GKB::HTMLReader

=head1 SYNOPSIS

Takes info from HTML and presents it in a text-generation-friendly way.

=head1 DESCRIPTION

Gets assorted book-related information from a URL
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

use HTML::PullParser ();
use LWP::UserAgent;
use GKB::Config;
use GKB::DocumentGeneration::TextUnit;
use GKB::DocumentGeneration::TextReader;
use Storable qw(nstore retrieve);
use Data::Dumper;

@ISA = qw(GKB::DocumentGeneration::TextReader);

# List the object variables here, so that they can be checked
for my $attr
    (qw(
    html_root
    parser
    report_tags
    report_tags_hash
    text_units
    chapter_num
    emit_flag
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
    
    my @report_tags = ('a', 'title', 'script', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'h7', 'p', 'img', 'l1', 'br', 'b', 'i', 'li', 'ul', 'ol', 'div');
#    my @report_tags = ('title', 'h1', 'h2', 'h3', 'p', 'img', 'l1', 'br', 'b', 'i');
    $self->report_tags(\@report_tags);
    my %report_tags_hash = ();
    foreach my $report_tag (@report_tags) {
    	$report_tags_hash{$report_tag} = $report_tag;
    }
    $self->report_tags_hash(\%report_tags_hash);
    
    # It's a drag that we have to do this again here
    my $depth_offset = 0;
    $self->depth_offset($depth_offset);
    
    my @text_units = ();
    $self->text_units(\@text_units);
    
    $self->emit_flag(1);
	
    return $self;
}

# Given a URL as an argument, fetch the HTML
# and start the parser.
sub init {
    my ($self, $url) = @_;
    
    # Try to extract a root URL from the URL supplied.
	my $html_root = $url;
	$html_root =~ s/[^\/]+$//;
	$self->html_root($html_root);
	
	# Retrieve the HTML from the URL
    my $content = $self->fetch_content_from_url($url);
    
#    # Process content to remove weird combinations of HTML
#    # tags that get inserted by the wiki
#    my @lines = split(/\n/, $content);
#    my $line;
#    $content = '';
#    foreach $line (@lines) {
#    	$line =~ s/^<\/p><p>//;
#    	
#    	if (($content eq ' ')) {
#    		$content .= "\n";
#    	}
#    	$content .= $line;
#    }
    
	# Initialize pull-parser
	my $parser = HTML::PullParser->new(doc => $content,
				      start => 'tag, attr',
				      end   => 'tag',
				      text  => '@{text}',
				      report_tags => $self->report_tags,
				     );

    $self->parser($parser);
}

# Given a URL as an argument, fetch the HTML
sub fetch_content_from_url {
    my ($self, $url) = @_;
    my $content = undef;
    
    if (defined $url) {
	    my $ua = LWP::UserAgent->new();
	
	    my $response = $ua->get($url);
	    if(defined $response) {
	    	if ($response->is_success) {
	    		print STDERR "HTMLReader.fetch_content_from_url: Ah-ha, we have SUCCESS!!!\n";

	    		$content = $response->content;
	    		
#	    		print STDERR "HTMLReader.fetch_content_from_url: content=$content\n";

	    	} else {
	    		print STDERR "HTMLReader.fetch_content_from_url: GET request failed for url=$url\n";
	    	}
	    } else {
	    	print STDERR "HTMLReader.fetch_content_from_url: no response!\n";
	    }
    } else {
    	print STDERR "HTMLReader.fetch_content_from_url: you need to supply a URL!\n";
    }
    
    return $content;
}

# Get the next text unit from the input stream.
# The depth and depth_limit arguments are ignored.
# If include_images_flag is 0, then images will
# not be returned.
sub get_next_text_unit {
    my ($self, $depth, $depth_limit,$include_images_flag) = @_;
    
    my @text_units = @{$self->text_units};
    if (scalar(@text_units)<1) {
    	@text_units = $self->get_next_text_units($include_images_flag);
    	my $initial_header = $self->get_initial_header();
    	if (defined $initial_header) {
    		# Add on an initial header, if necessary
    		my $text_unit = GKB::DocumentGeneration::TextUnit->new();
			$text_unit->set_type("section_header");
			$text_unit->set_depth($self->get_depth_offset() + 1);
			$text_unit->set_contents($initial_header);
			unshift(@text_units, $text_unit);
			$self->initial_header(undef);
    	}
    	my $chapter_header = $self->get_chapter_header();
    	if (defined $chapter_header) {
    		# Add on an initial header, if necessary
    		my $text_unit = GKB::DocumentGeneration::TextUnit->new();
			$text_unit->set_type("section_header");
			$text_unit->set_depth($self->get_depth_offset());
			$text_unit->set_contents($chapter_header);
			unshift(@text_units, $text_unit);
			$self->chapter_header(undef);
			$self->add_depth_offset(1);
    	}
    }
    
#    print STDERR "HTMLReader.get_next_text_unit: text_units=\n" . Dumper(@text_units) . "\n";
    
    my $text_unit;
	if (scalar(@text_units)<1) {
		# We didn't find anything, guess that's the end of
		# the road for us.
		$text_unit = GKB::DocumentGeneration::TextUnit->new();
		$text_unit->set_type("eof");
	} else {
    	$text_unit = shift(@text_units);
	}
    
#    print STDERR "HTMLReader.get_next_text_unit: text_unit=\n" . Dumper($text_unit) . "\n";
    
    $self->text_units(\@text_units);
    
    if (!(defined $text_unit)) {
    	print STDERR "HTMLReader.get_next_text_unit: oh grot, test_unit is undef!!!\n";
    }
    
    print STDERR "HTMLReader.get_next_text_unit: text_unit type=" . $text_unit->get_type() . "\n";
    
    return $text_unit;
}

# Get the next text unit from the input stream.
# The depth and depth_limit arguments are ignored.
# If include_images_flag is 0, then images will
# not be returned.
sub get_next_text_units {
    my ($self, $include_images_flag) = @_;
    
    my @text_units = ();
    
    my $parser = $self->parser;
    my $token;
    my @tokens;
    my $tag;
    my $text;
    my $start_header;
    my $stop_header;
    my $depth;
	print STDERR "\n\nHTMLReader.get_next_text_units: get tokens\n";
	$token = $parser->get_token;
	if (!(defined $token)) {
		print STDERR "HTMLReader.get_next_text_units: no token, I guess thats all folks!\n";
		return @text_units;
	}
	
	if (ref $token) {
		$tag = $token->[0];
			
		print STDERR "HTMLReader.get_next_text_units: tag=$tag\n";
			
		if ($tag eq 'p' || $tag eq 'br') {
			print STDERR "HTMLReader.get_next_text_units: hmpf, a paragraph\n";
				
			my @paragraph_text_units = $self->get_paragraph_text_units();
			@text_units = (@text_units, @paragraph_text_units);
		} elsif ($tag eq 'l1') {
			print STDERR "HTMLReader.get_next_text_units: hee hee, L1 bullits!\n";
				
			my @bullit_text_units = $self->get_l1_bullit_text_units($tag);
			@text_units = (@text_units, @bullit_text_units);
		} elsif ($tag eq 'ol') {
			print STDERR "HTMLReader.get_next_text_units: woo, ol bullits!\n";
				
			my @bullit_text_units = $self->get_ol_numbered_text_units(0);
			@text_units = (@text_units, @bullit_text_units);
		} elsif ($tag eq 'ul') {
			print STDERR "HTMLReader.get_next_text_units: guffaw, ul bullits!\n";
				
			my @bullit_text_units = $self->get_ul_bullit_text_units(0);
			@text_units = (@text_units, @bullit_text_units);
		} elsif ($tag eq 'img') {
			print STDERR "HTMLReader.get_next_text_units: heh, an image\n";
				
			my @image_text_units = $self->get_image_text_units($token);
			@text_units = (@text_units, @image_text_units);
		} elsif ($tag eq 'a') {
			print STDERR "HTMLReader.get_next_text_units: hups, looks like a link\n";
				
			$text = $self->get_a_text($token);
			my $text_unit = $self->create_paragraph_text_unit($text);
			push(@text_units, $text_unit);
		} elsif ($tag =~ /h([0-9]+)/ || $tag eq 'title' || $tag eq 'script') { # sandwich tags
			print STDERR "HTMLReader.get_next_text_units: yum, a token sandwich!\n";
				
			# Assume that we have a <tag>...</tag> pair
			$text = $self->get_token_sandwich($tag);
				
			print STDERR "HTMLReader.get_next_text_unit: sandwich text = $text\n";
				
			# Decide what to do depending on the tag
			if ($tag =~ /h([0-9]+)/) {
				# Section heading
				$depth = $1;
				$depth--;

				print STDERR "HTMLReader.get_next_text_units: ho, a section heading\n";
				
				my $text_unit = GKB::DocumentGeneration::TextUnit->new();
				$stop_header = $self->get_stop_header();
				if (defined $stop_header && $text =~ /$stop_header/) {
					# Special mechanism that allows generation
					# of text units to be halted when a given
					# header has been encountered.
					$text_unit->set_type("eof");
					push(@text_units, $text_unit);
					return @text_units;
				}
				$start_header = $self->get_start_header();
				if (defined $start_header && $text =~ /$start_header/) {
					# Special mechanism that allows generation
					# of text units to be started when a given
					# header has been encountered.
					@text_units = ();
					$self->emit_flag(1);
				}
				$text_unit->set_type("section_header");
				$text_unit->set_depth($depth + $self->get_depth_offset());
				$text_unit->set_contents($text);
				push(@text_units, $text_unit);
			} elsif ($tag eq 'title') {
				print STDERR "HTMLReader.get_next_text_units: hah, a title\n";
				
				# I don't know what to do here, so don't
				# emit anything
				my $text_unit = GKB::DocumentGeneration::TextUnit->new();
				$text_unit->set_type("empty");
				push(@text_units, $text_unit);
			} elsif ($tag eq 'script') {
				print STDERR "HTMLReader.get_next_text_units: coo, javascript\n";
				
				# Ignore javascript
				my $text_unit = GKB::DocumentGeneration::TextUnit->new();
				$text_unit->set_type("empty");
				push(@text_units, $text_unit);
			} else {
				print STDERR "HTMLReader.get_next_text_units: tag $tag is not suitable for token sandwiches, ignoring\n";
			}
		} elsif ($tag eq 'div' || $tag eq '/div') {
			# Dump divs in the dustbin
			my $text_unit = GKB::DocumentGeneration::TextUnit->new();
			$text_unit->set_type("empty");
			push(@text_units, $text_unit);
		} else {
			print STDERR "HTMLReader.get_next_text_units: unknown tag=$tag, ignoring\n";
			my $text_unit = GKB::DocumentGeneration::TextUnit->new();
			$text_unit->set_type("empty");
			push(@text_units, $text_unit);
		}
	} else {
		print STDERR "HTMLReader.get_next_text_units: unenclosed text, emit a paragraph\n";
		print STDERR "HTMLReader.get_next_text_units: token=|$token|\n";
		
		$text = $self->clean_text($token);
			
		# We found some text, not enclosed in a paragraph
		# or ennifink, treat it as a regular paragraph.
		# Dont bovvah if token is empty.
		if (!($text =~ /^\s*$/)) {
			print STDERR "HTMLReader.get_next_text_units: lets emit a paragraph then\n";
			
			$parser->unget_token($token);
			my @paragraph_text_units = $self->get_paragraph_text_units();
			@text_units = (@text_units, @paragraph_text_units);
		} else {
			my $text_unit = GKB::DocumentGeneration::TextUnit->new();
			$text_unit->set_type("empty");
			push(@text_units, $text_unit);
		}
	}
	
	# Forget about everything we just parsed if the emit_flag
	# isn't set.  This flag allows us to delay the emitting
	# of text units until we have reacted a preselected point
	# in the source document.
	if (!($self->emit_flag)) {
		my $text_unit = GKB::DocumentGeneration::TextUnit->new();
		$text_unit->set_type("empty");
		@text_units = ($text_unit);
	}
	
    return @text_units;
}

# Get information associated with <a>...</a> tags (links).
# Assumes that the <a> tag has already been encountered;
# the token corresponding to this tag must be supplied as
# an argument
sub get_a_text {
    my ($self, $token) = @_;
    
	my $tag = $token->[0];
	 
	# Assume that we have a <tag>...</tag> pair
	my $text = $self->get_token_sandwich($tag);
	
	# Certain text, such as "click here" doesn't really
	# make sense when in, say, a PDF document, so this
	# will be translated into a reference to the concealed
	# URL.
	if ($text eq "click" ||
		$text eq "click here") {
		my $href = $token->[1]->{"href"};
		$text = "please visit the following URL:\n\n$href\n";
	}
				
    return $text;
}

# See if we can pull one or more bullit points from the HTML.
# Assume we hav an L1 tag.
# Return an array of text units containing the information.
sub get_l1_bullit_text_units {
    my ($self, $tag) = @_;
    
    my @text_units = ();
    my $parser = $self->parser;
    my $token;
    my $text = '';

	# A type of listing - map on to bullit points
	while (defined($token = $parser->get_token)) {
		if (ref $token) {
			if ($token->[0] eq "br" || $token->[0] eq "/l1") {
				my $text_unit = GKB::DocumentGeneration::TextUnit->new();
				$text_unit->set_type("bullit_text");
				$text = $self->clean_text($text);
				# If we have only parsed white space, don't bother
				# to emit anything.
				if ($text =~ /^\s*$/) {
					$text_unit->set_type("empty");
				}
				$text_unit->set_contents($text);
				push(@text_units, $text_unit);
				$text = '';
				if ($token->[0] eq "/l1") {
					last;
				}
			}
		} else {
			$text .= $token;
		}
	}
	
	return @text_units;
}

# See if we can pull one or more numbered points from the HTML.
# Assume we hav a ol tag.
# Return an array of text units containing the information.
sub get_ol_numbered_text_units {
    my ($self, $indentation) = @_;
    
    my @text_units = ();
    my $parser = $self->parser;
    my $token;
    my $text = '';
	my $first_li_flag = 0;
	my $number = 1;
	# A type of listing - map on to numbered points
	while (defined($token = $parser->get_token)) {
		if (ref $token) {
			if ($token->[0] eq "li") {
				print STDERR "HTMLReader.get_ol_numbered_text_units: we have a li\n";
				
				if (!($text eq '')) {
					print STDERR "HTMLReader.get_ol_numbered_text_units: noting text as numbered point\n";
				
					$number++;
					
					my $text_unit = GKB::DocumentGeneration::TextUnit->new();
					$text_unit->set_type("numbered_text");
					$text = $self->clean_text($text);
					# If we have only parsed white space, don't bother
					# to emit anything.
					if ($text =~ /^\s*$/) {
						$text_unit->set_type("empty");
					} else {
						$text_unit->set_contents($text);
						$text_unit->set_depth($indentation);
						$text_unit->set_number($number);
					}
					push(@text_units, $text_unit);
				}
				$first_li_flag = 1;
				$text = '';
			} elsif ($token->[0] eq "/li" || $token->[0] eq "/ol") {
				print STDERR "HTMLReader.get_ol_numbered_text_units: we have a /li or /ol\n";
				
				my $text_unit = GKB::DocumentGeneration::TextUnit->new();
				$text_unit->set_type("numbered_text");
				$text = $self->clean_text($text);
				# If we have only parsed white space, don't bother
				# to emit anything.
				if ($text =~ /^\s*$/) {
					$text_unit->set_type("empty");
				} else {
					$text_unit->set_contents($text);
					$text_unit->set_depth($indentation);
					$text_unit->set_number($number);
				}
				push(@text_units, $text_unit);
				$text = '';
				$number++;
				if ($token->[0] eq "/ol") {
					print STDERR "HTMLReader.get_ol_numbered_text_units: well it was a /ol, so bomb out!\n";
				
					last;
				}
			} else {
				print STDERR "HTMLReader.get_ol_numbered_text_units: unknown token: " . $token->[0] . ", ignoring\n";
			}
		} else {
			if ($first_li_flag) {
				$text .= $token;
			}
		}
	}
	
	return @text_units;
}

# See if we can pull one or more bullit points from the HTML.
# Assume we hav a ul tag.
# Return an array of text units containing the information.
sub get_ul_bullit_text_units {
    my ($self, $indentation) = @_;
    
    print STDERR "HTMLReader.get_ul_bullit_text_units: entered, indentation=$indentation\n";
    
    my @text_units = ();
    my $parser = $self->parser;
    my $token;
    my $text = '';
	my $first_li_flag = 0;
	# A type of listing - map on to bullit points
	while (defined($token = $parser->get_token)) {
		if (ref $token) {
			if ($token->[0] eq "ol") {
				print STDERR "HTMLReader.get_ul_bullit_text_units: gosh, we have a nested ol!!!\n";
				
				my @ol_text_units = $self->get_ol_numbered_text_units($indentation + 1);
				@text_units = (@text_units, @ol_text_units);
			} elsif ($token->[0] eq "ul") {
				print STDERR "HTMLReader.get_ul_bullit_text_units: gosh, we have a nested ul!!!\n";
				
				my @ul_text_units = $self->get_ul_bullit_text_units($indentation + 1);
				@text_units = (@text_units, @ul_text_units);
			} elsif ($token->[0] eq "li") {
				print STDERR "HTMLReader.get_ul_bullit_text_units: we have a li\n";
				
				if (!($text eq '')) {
					print STDERR "HTMLReader.get_ul_bullit_text_units: noting text as bullit point\n";
				
					my $text_unit = GKB::DocumentGeneration::TextUnit->new();
					$text_unit->set_type("bullit_text");
					$text = $self->clean_text($text);
					# If we have only parsed white space, don't bother
					# to emit anything.
					if ($text =~ /^\s*$/) {
						$text_unit->set_type("empty");
					} else {
						print STDERR "HTMLReader.get_ul_bullit_text_units: text=$text\n";
				
						$text_unit->set_contents($text);
						$text_unit->set_depth($indentation);
					}
					push(@text_units, $text_unit);
				}
				$first_li_flag = 1;
				$text = '';
			} elsif ($token->[0] eq "/li" || $token->[0] eq "/ul") {
				print STDERR "HTMLReader.get_ul_bullit_text_units: we have a /li or /ul\n";
				
				my $text_unit = GKB::DocumentGeneration::TextUnit->new();
				$text_unit->set_type("bullit_text");
				$text = $self->clean_text($text);
				# If we have only parsed white space, don't bother
				# to emit anything.
				if ($text =~ /^\s*$/) {
					$text_unit->set_type("empty");
				} else {
					print STDERR "HTMLReader.get_ul_bullit_text_units: text=$text\n";
					
					$text_unit->set_contents($text);
					$text_unit->set_depth($indentation);
				}
				push(@text_units, $text_unit);
				$text = '';
				if ($token->[0] eq "/ul") {
					print STDERR "HTMLReader.get_ul_bullit_text_units: well it was a /ul, so bomb out!\n";
				
					last;
				}
			} else {
				print STDERR "HTMLReader.get_ul_bullit_text_units: unknown token: " . $token->[0] . ", ignoring\n";
			}
		} else {
			if ($first_li_flag) {
				$text .= $token;
			}
		}
	}
	
	return @text_units;
}

# If you think you are at the start of a paragraph, run this
# method to slurp up all the text until the end of it, and
# return an array of text units containing the information.
sub get_paragraph_text_units {
    my ($self) = @_;
    
    my $parser = $self->parser;
    my $token;
    my $tag;
    my $text = '';
    my $new_text;
    my $paragraph_nesting = 0;
    my $src;
	while (defined($token = $parser->get_token)) {
		if (ref $token) {
			$tag = $token->[0];
						
			print STDERR "HTMLReader.get_paragraph_text_units: in-paragraph tag=$tag\n";
						
			if ($tag eq "p") {
				print STDERR "HTMLReader.get_paragraph_text_units: ho, we have a nested paragraph!!!\n";
							
				$paragraph_nesting++;
			} elsif ($tag eq "/p") {
				print STDERR "HTMLReader.get_paragraph_text_units: end of a paragraph\n";
				
				if ($paragraph_nesting>0) {
					$paragraph_nesting--;
					
					# Lets's honor the end-of-paragraph with a
					# newline, har-di-har-har!
					$text .= "\n";
				} else {
					last;
				}
			} elsif ($tag eq "b" || $tag eq "i") {
				print STDERR "HTMLReader.get_paragraph_text_units: bold or italic\n";
							
				# Deal with embedded text in bold and
				# italics.
				$text .= "<$tag>" . $self->get_token_sandwich($tag) . "</$tag>";
			} elsif ($tag eq "img") {
				print STDERR "HTMLReader.get_paragraph_text_units: image\n";
							
				# Deal with embedded images.
				$src = $self->get_image_from_src($token->[1]->{'src'});
				$text .= $self->get_image_markup($src, $token->[1]->{'width'}, $token->[1]->{'height'});
			} elsif ($tag eq 'a') {
				print STDERR "HTMLReader.get_paragraph_text_units: hups, looks like a link\n";
					
				$text .= $self->get_a_text($token);
			} else {
				print STDERR "HTMLReader.get_paragraph_text_units: unknown tag $tag, paragraph aborted\n";
							
				$parser->unget_token($token);
				last;
			}
		} else {
			print STDERR "HTMLReader.get_paragraph_text_units: token=$token\n";
			
			if (!($text eq '')) {
				# Put the newlines back in - these help
				# TextReader subroutines to do their job.
				$text .= "\n";
			}
			$new_text = $token;
			$new_text =~ s/^[\.\s]+//; # Remove leading full stops and spaces from lines
			$text .= $new_text;
		}
	}
	
	# Clean up
	$text =~ s/^\s+//;
	$text =~ s/\s+$//;
	
	print STDERR "HTMLReader.get_paragraph_text_units: creating paragraph, text=\n$text\n";
	
	my @text_units = ();
	my @next_text_units;
	my $reader = GKB::DocumentGeneration::TextReader->new();
	$reader->init_from_string($text);
	my $next_text_units_size;
	
	# Use TextReader subroutines to pull non-HTML formatting
	# from the supplied text
	while (1) {
		@next_text_units = $reader->get_next_text_units();
		$next_text_units_size = scalar(@next_text_units);
		
		# remove last text unit if it is eof!!!
		if ($next_text_units_size>0 && ($next_text_units[$next_text_units_size-1]->get_type() eq "eof")) {
			pop(@next_text_units);
		}
		
		if (scalar(@next_text_units)<1) {
			last;
		}
		
		@text_units = (@text_units, @next_text_units);
	}
	if (scalar(@text_units)<1) {
		my $text_unit2 = GKB::DocumentGeneration::TextUnit->new();
		$text_unit2->set_type("empty");
		$text_unit2->set_contents(1);
		push(@text_units, $text_unit2);
	}
	
	print STDERR "HTMLReader.get_paragraph_text_units: TextReader scalar(text_units)=" . scalar(@text_units) . "\n";

	return @text_units;
}

# Given an image token, will download the image to a local
# file and return appropriate text units for adding this
# image to the document.
sub get_image_text_units {
    my ($self, $token) = @_;
    
	my $tag = $token->[0];
	my %attr = %{$token->[1]};
	my $src = $attr{'src'};
	
	my $image_path = $self->get_image_from_src($src);
						
	my @text_units = ();
	my $text_unit1 = GKB::DocumentGeneration::TextUnit->new();
	$text_unit1->set_type("image_file_name");
	# The second element of "contents" is set to 1 to force
	# the deletion of this file once it has been used.
	my @contents = ($image_path, 1);
	$text_unit1->set_contents(\@contents);
	push(@text_units, $text_unit1);
	
	my $text_unit2 = GKB::DocumentGeneration::TextUnit->new();
	$text_unit2->set_type("vertical_space");
	$text_unit2->set_contents(1);
	push(@text_units, $text_unit2);
	
	return @text_units;
}

sub get_image_from_src {
    my ($self, $src) = @_;
    
	my $image_url = $self->get_image_url_from_src($src);
	my $image_path = $self->get_image_path_from_src($src);
	my $content =  $self->fetch_content_from_url($image_url);
	if (open(IMAGE, ">$image_path")) {
		print IMAGE $content;
		close(IMAGE);
	}
	
	# The conversion will produce a converted image with
	# a new filename.
	$image_path = $self->convert_image_to_jpeg($image_path);
	
	return $image_path;
}

sub get_image_file_name_from_src {
    my ($self, $src) = @_;
    
	print STDERR "HTMLReader.get_image_file_name_from_src: src=$src\n";

    my $image_file_name = $src;
	$image_file_name =~ /^(.+)\/[^\/]+$/;
	my $stem = $1;
	if (defined $stem && !($stem eq '')) {
		my $regexp_stem = $stem;
		$regexp_stem =~ s/\//\\\//g;
		print STDERR "HTMLReader.get_image_file_name_from_src: stem=$stem, regexp_stem=|$regexp_stem|\n";
		$image_file_name =~ s/^$regexp_stem//;
	}
	$image_file_name =~ s/^\/+//;
	
	return $image_file_name;
}

sub get_image_path_from_src {
    my ($self, $src) = @_;
    
	print STDERR "HTMLReader.get_image_path_from_src: src=$src\n";

    my $image_file_name = $self->get_image_file_name_from_src($src);
	my $image_path = $GK_TMP_IMG_DIR . "/" . rand() * 1000 . $image_file_name;
	return $image_path;
}

sub get_image_url_from_src {
    my ($self, $src) = @_;
    
    my $image_file_name = $self->get_image_file_name_from_src($src);
	my $image_url = $self->html_root . $image_file_name;
	return $image_url;
}

# Convert the supplied image to JPEG and return the name
# of the JPEG image file thus created.  The original
# image file will be deleted.
sub convert_image_to_jpeg {
    my ($self, $image_file_name) = @_;
    
    if ($image_file_name =~ /\.jpe{0,1}g$/i) {
    	return $image_file_name;
	}
    
    my $new_image_file_name = $image_file_name;
    $new_image_file_name =~ s/\.[^\.]+$/.png/;
    my $command = "convert $image_file_name $new_image_file_name";
    system($command);
    unlink($image_file_name);
    
    return $new_image_file_name;
}

# Given the text, create a text unit.  Does some clean
# up on the text as well.  If the text is only whitespace
# or something boring like that, returns a text unit
# of type "empty".
sub create_paragraph_text_unit {
    my ($self, $text) = @_;

	$text = $self->clean_text($text);
		
	my $text_unit = GKB::DocumentGeneration::TextUnit->new();
	$text_unit->set_type("body_text_paragraph");

	# If we have only parsed white space, don't bother
	# to emit anything.
	if ($text =~ /^\s*$/) {
		print STDERR "HTMLReader.get_next_text_unit: token is boring white space\n";
			
		$text_unit->set_type("empty");
	} else {
		$text_unit->set_contents($text);
	}
	
	return $text_unit;
}

# Plow through the HTML until a </tag> is encountered, accumulating
# all the text encountered on the way.  Other tags will be ignored.
sub get_token_sandwich {
    my ($self, $tag) = @_;
    
    my $filling = '';
    my $parser = $self->parser;
    my $token;
	while (defined($token = $parser->get_token)) {
		if (ref $token) {
			if ($token->[0] eq "/$tag") {
				last;
			}
		} else {
			$filling .= $token;
		}
	}
	
	# Clean up
	$filling =~ s/^\s+//;
	$filling =~ s/\s+$//;
	
	return $filling;
}

# Make the current chapter number visible to the whole world
sub get_chapter_num {
    my ($self) = @_;
    
    return $self->chapter_num;
}

1;
