package GKB::DocumentGeneration::TextReader;

=head1 NAME

GKB::TextReader

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
use GKB::Config;
use GKB::DocumentGeneration::TextUnit;
use GKB::DocumentGeneration::Reader;
use Storable qw(nstore retrieve);
use Data::Dumper;

@ISA = qw(GKB::DocumentGeneration::Reader);

# List the object variables here, so that they can be checked
for my $attr
    (qw(
    html_root
    lines
    line_count
    line_num
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
    
    # It's a drag that we have to do this again here
    my $depth_offset = 0;
    $self->depth_offset($depth_offset);
    
    my @lines = ();
    $self->lines(\@lines);
    
    my $line_count = 0;
    $self->line_count($line_count);
    
    my $line_num = 0;
    $self->line_num($line_num);
    
    my @text_units = ();
    $self->text_units(\@text_units);
    
    $self->emit_flag(1);
	
    return $self;
}

# Given a URL as an argument, fetch the HTML
sub init_from_url {
    my ($self, $url) = @_;
    
    # Try to extract a root URL from the URL supplied.
	my $html_root = $url;
	$html_root =~ s/[^\/]+$//;
	$self->html_root($html_root);

	# Retrieve the HTML from the URL
    my $content = $self->fetch_content_from_url($url);
    
    $self->init_from_string($content);
}

sub init_from_string {
    my ($self, $content) = @_;
    
    $self->content($content);
    
    # Break text into lines
    my @lines = split(/\n/, $content);
    my $line_count = scalar(@lines);
    my $line_num = 0;
    
    print STDERR "TextReader.init_from_string: line_count=$line_count\n";
    
    $self->lines(\@lines);
    $self->line_count($line_count);    
    $self->line_num($line_num);    
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
	    		$content = $response->content;
	    	} else {
	    		print STDERR "TextReader.fetch_content_from_url: GET request failed for url=$url\n";
	    	}
	    } else {
	    	print STDERR "TextReader.fetch_content_from_url: no response!\n";
	    }
    } else {
    	print STDERR "TextReader.fetch_content_from_url: you need to supply a URL!\n";
    }
    
    return $content;
}

# Get the next text unit from the input stream.
# The depth and depth_limit arguments are ignored.
# If include_images_flag is 0, then images will
# not be returned.
sub get_next_text_unit {
    my ($self, $depth, $depth_limit) = @_;
    
    my @text_units = @{$self->text_units};
    if (scalar(@text_units)<1) {
    	@text_units = $self->get_next_text_units();
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
    
#    print STDERR "TextReader.get_next_text_unit: text_units=\n" . Dumper(@text_units) . "\n";
    
    my $text_unit;
	if (scalar(@text_units)<1) {
		# We didn't find anything, guess that's the end of
		# the road for us.
		$text_unit = GKB::DocumentGeneration::TextUnit->new();
		$text_unit->set_type("eof");
	} else {
    	$text_unit = shift(@text_units);
	}
    
#    print STDERR "TextReader.get_next_text_unit: text_unit=\n" . Dumper($text_unit) . "\n";
    
    $self->text_units(\@text_units);
    
    if (!(defined $text_unit)) {
    	print STDERR "TextReader.get_next_text_unit: oh grot, test_unit is undef!!!\n";
    }
    
    print STDERR "TextReader.get_next_text_unit: text_unit type=" . $text_unit->get_type() . "\n";
    
    return $text_unit;
}

# Get the next text unit from the input stream.
# The depth and depth_limit arguments are ignored.
# If include_images_flag is 0, then images will
# not be returned.
sub get_next_text_units {
    my ($self) = @_;
    
    my $text;
	print STDERR "\n\nTextReader.get_next_text_unit: lets go\n";
	
	# Try to parse various different kinds of text unit
	# from the lines, until nothing more can be extracted.
	my @text_units = $self->get_header_text_units();
	if (scalar(@text_units)<1) {
		@text_units = $self->get_bullit_text_units();
		if (scalar(@text_units)<1) {
			@text_units = $self->get_numbered_text_units();
			if (scalar(@text_units)<1) {
				@text_units = $self->get_diagram_text_units();
				if (scalar(@text_units)<1) {
					@text_units = $self->get_paragraph_text_units();
				}
			}
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

sub get_header_text_units {
    my ($self) = @_;
    
	print STDERR "TextReader.get_header_text_units: entered\n";

    my @text_units = ();
	my $line_num = $self->line_num;
	if (($line_num + 1)<$self->line_count) {
		my $last_line = $self->lines->[$line_num ];
		my $line = $self->lines->[$line_num+1];
		
		# Are we dealing with a header?
		if (defined $last_line && $line =~ /[\-_=]+/ && $line =~ /^[\-_= ]+$/) {
			my $header_line = $last_line;
			$header_line =~ s/ //g;
			my $undeline_line = $line;
			$undeline_line =~ s/ //g;
			# It could be that everything, including spaces
			# is underlined, or perhaps only the words are
			# underlined.
			if (length($last_line) == length($line) || length($header_line) == length($undeline_line)) {
				my $text_unit = GKB::DocumentGeneration::TextUnit->new();
				my $stop_header = $self->get_stop_header();
				if (defined $stop_header && $last_line =~ /$stop_header/) {
					# Special mechanism that allows generation
					# of text units to be halted when a given
					# header has been encountered.
					$text_unit->set_type("eof");
					push(@text_units, $text_unit);
				} else {
					my $start_header = $self->get_start_header();
					if (defined $start_header && $last_line =~ /$start_header/) {
						# Special mechanism that allows generation
						# of text units to be started when a given
						# header has been encountered.
						@text_units = ();
						$self->emit_flag(1);
					}
					$text_unit->set_type("section_header");
					$text_unit->set_depth($self->get_depth_offset());
					$text_unit->set_contents($last_line);
					push(@text_units, $text_unit);
				}
				
				$self->line_num($self->line_num + 2);
			}
		}
	}
	
    return @text_units;
}

sub get_bullit_text_units {
    my ($self) = @_;
    
	print STDERR "TextReader.get_bullit_text_units: entered\n";

    my @text_units = ();
	my $line_num;
	my $line;
	my $text;
	for ($line_num=$self->line_num; $line_num<scalar(@{$self->lines}); $line_num++) {
		print STDERR "TextReader.get_bullit_text_units: OUTER line_num=$line_num\n";

		$line = $self->lines->[$line_num];
			
		print STDERR "TextReader.get_bullit_text_units: OUTER line=$line\n";
		
		# Ig-gnaw this line if it is blank
		if ($line =~ /^\s*$/) {
			next;
		}

		if ($line =~ /^[\*\-]\s*/) {
			$text = $line;
			$text =~ s/^[\*\-]\s*//;
			$line_num++;
			for (; $line_num<scalar(@{$self->lines}); $line_num++) {
				print STDERR "TextReader.get_bullit_text_units: INNER line_num=$line_num\n";

				$line = $self->lines->[$line_num];
					
				print STDERR "TextReader.get_bullit_text_units: INNER line=$line\n";

				if ($line =~ /^[\*\-]\s*/) {
					# We just hit the next bullit line -
					# bomb out without incrementing line_num.
#					$line_num--;
					last;
				}
				if ($line =~ /^\s*$/) {
					# Blank line - terminate bullit line -
					# bomb out without incrementing line_num.
#					$line_num--;
					last;
				}
				
				$text .= " $line";
			}
			
			print STDERR "TextReader.get_bullit_text_units: text=$text\n";

			my $text_unit = GKB::DocumentGeneration::TextUnit->new();
			$text_unit->set_type("bullit_text");
			$text_unit->set_contents($text);
			push(@text_units, $text_unit);
		} else {
			last;
		}
	}
	
	if (scalar(@text_units)>1) {
		$self->line_num($line_num + 1);
	} else {
		# Don't consider a single line with a number in front
		# of it to be part of a numbered list.
		@text_units = ();
	}
	
    return @text_units;
}

sub get_numbered_text_units {
    my ($self) = @_;
    
	print STDERR "TextReader.get_numbered_text_units: entered\n";

    my @text_units = ();
	my $line_num;
	my $line;
	my $text;
	my $number;
	for ($line_num=$self->line_num; $line_num<scalar(@{$self->lines}); $line_num++) {
		print STDERR "TextReader.get_numbered_text_units: OUTER line_num=$line_num\n";

		$line = $self->lines->[$line_num];
			
		print STDERR "TextReader.get_numbered_text_units: OUTER line=$line\n";

		# Ig-gnaw this line if it is blank
		if ($line =~ /^\s*$/) {
			next;
		}

		if ($line =~ /^([0-9]{1,2})\.{0,1}\s+/) {
			$number = $1;

			print STDERR "TextReader.get_numbered_text_units: number=$number\n";

			$text = $line;
			$text =~ s/^[0-9]{1,2}\.{0,1}\s+//;
			$line_num++;
			for (; $line_num<scalar(@{$self->lines}); $line_num++) {
				print STDERR "TextReader.get_numbered_text_units: INNER line_num=$line_num\n";

				$line = $self->lines->[$line_num];
					
				if ($line =~ /^[0-9]{1,2}\.{0,1}\s+/) {
					# We just hit the next numbered line -
					# bomb out without incrementing line_num.
					last;
				}
				if ($line =~ /^\s*$/) {
					# Blank line - terminate numbered line -
					# bomb out without incrementing line_num.
					last;
				}
				
				$text .= " $line";
			}
			
			print STDERR "TextReader.get_numbered_text_units: text=$text\n";

			my $text_unit = GKB::DocumentGeneration::TextUnit->new();
			$text_unit->set_type("numbered_text");
			$text_unit->set_number($number);
			$text_unit->set_contents($text);
			push(@text_units, $text_unit);
		} else {
			last;
		}
	}
	
	if (scalar(@text_units)>1) {
		$self->line_num($line_num + 1);
	} else {
		# Don't consider a single line with a number in front
		# of it to be part of a numbered list.
		@text_units = ();
	}
	
    return @text_units;
}

sub get_diagram_text_units {
    my ($self) = @_;
    
	print STDERR "TextReader.get_diagram_text_units: entered\n";

    my @text_units = ();
	my $line_num;
	my $line;
	my $i;
	my $lookahead_line_count = 2;
	for ($line_num=$self->line_num; $line_num<scalar(@{$self->lines}); $line_num++) {
		print STDERR "TextReader.get_diagram_text_units: OUTER line_num=$line_num\n";

		$line = $self->lines->[$line_num];
			
		print STDERR "TextReader.get_diagram_text_units: OUTER line=$line\n";

		if ($line =~ /\|/ || $line =~ /--/) { # "strong" criterion for diagram line
			my $text_unit = GKB::DocumentGeneration::TextUnit->new();
			$text_unit->set_type("body_text_paragraph");
			$text_unit->set_contents($line);
			push(@text_units, $text_unit);
			
			$line_num++;
			for ($i=0; $line_num<scalar(@{$self->lines}) && $i<$lookahead_line_count; $line_num++,$i++) {
				print STDERR "TextReader.get_diagram_text_units: INNER line_num=$line_num\n";

				$line = $self->lines->[$line_num];
					
				print STDERR "TextReader.get_diagram_text_units: INNER line=$line\n";

				if ($line =~ /\|/ || $line =~ /--/) {
					# We just hit the next "strong" line -
					# reset without incrementing line_num.
					$line_num--;
					last;
				}
				if (!($line =~ /[\-|]/)) {
					# Non-diagram line - terminate diagram.
					$line_num--;
					last;
				}
				
				my $text_unit2 = GKB::DocumentGeneration::TextUnit->new();
				$text_unit2->set_type("body_text_paragraph");
				$text_unit2->set_contents($line);
				push(@text_units, $text_unit2);
			}
		} else {
			last;
		}
	}
	
	if (scalar(@text_units)>1) {
		my $text_unit2 = GKB::DocumentGeneration::TextUnit->new();
		$text_unit2->set_type("vertical_space");
		$text_unit2->set_contents(1);
		push(@text_units, $text_unit2);
		
		$self->line_num($line_num + 1);
	} else {
		# Don't consider a single line to be part of a diagram.
		@text_units = ();
	}
	
    return @text_units;
}

sub get_paragraph_text_units {
    my ($self) = @_;
    
 	print STDERR "TextReader.get_paragraph_text_units: entered\n";

    my @text_units = ();
	my $line_num = undef;
	my $line;
	my $text = '';
	for ($line_num=$self->line_num; $line_num<scalar(@{$self->lines}); $line_num++) {
		print STDERR "TextReader.get_paragraph_text_units: line_num=$line_num\n";
		
		$line = $self->lines->[$line_num];
			
		print STDERR "TextReader.get_paragraph_text_units: line=$line\n";
		
		if (!($text eq '')) {
			$text .= ' ';
		}
		$text .= $line;
		
		if ($line =~ /^\s*$/ || $line =~ /:$/) {
			# Blank line - terminate paragraph -
			# bomb out, incrementing line_num.
			$line_num++;
			last;
		}
	}
	
	$text = $self->clean_text($text);
			
 	print STDERR "TextReader.get_paragraph_text_units: line_num=$line_num, self->line_num=|" . $self->line_num . "|\n";
 	print STDERR "TextReader.get_paragraph_text_units: text=|$text|\n";

	if (defined $line_num && $self->line_num < $line_num) {
		my $text_unit1 = GKB::DocumentGeneration::TextUnit->new();
		if ($text eq '') {
			print STDERR "TextReader.get_paragraph_text_units: inserting empty, because only blank lines were found\n";
			
			$text_unit1->set_type("empty");
			push(@text_units, $text_unit1);
		} else {
			print STDERR "TextReader.get_paragraph_text_units: ahhhh, a REAL paragraph!!\n";
			
			$text_unit1->set_type("body_text_paragraph");
			$text_unit1->set_contents($text);
			push(@text_units, $text_unit1);
			
			my $text_unit2 = GKB::DocumentGeneration::TextUnit->new();
			$text_unit2->set_type("vertical_space");
			$text_unit2->set_contents(1);
			push(@text_units, $text_unit2);
		}
		
		$self->line_num($line_num);
	}
	
    return @text_units;
}

sub clean_text {
    my ($self, $text) = @_;
    
    my $new_text = '';
    
    # Get rid of newlines
    my @lines = split(/\n/, $text);
    foreach my $line (@lines) {
    	if (!($new_text eq '')) {
    		$new_text .= ' ';
    	}
    	$new_text .= $line;
    }
    
    $new_text =~ s/^\s+//;
    $new_text =~ s/\s+$//;
    $new_text =~ s/\s+/ /g;
    
    return $new_text;
}

# Make the current chapter number visible to the whole world
sub get_chapter_num {
    my ($self) = @_;
    
    return $self->chapter_num;
}

1;
