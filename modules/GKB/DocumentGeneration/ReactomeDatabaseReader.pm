package GKB::DocumentGeneration::ReactomeDatabaseReader;

=head1 NAME

GKB::ReactomeDatabaseReader

=head1 SYNOPSIS

Takes info from Reactome and presents it in a text-generation-friendly way.

=head1 DESCRIPTION

Gets assorted book-related information from the Reactome directory structure
or database (or in some cases even invents it) and presents it in a form
that is friendly for text generation, especially book generation.


WARNING: the following methods:

get_preface
get_acknowledgements

make the assumption that the current directory is:

 .../GKB/scripts/TheReactomeBook

Thats because they need to find the file:

 .../GKB/website/html/about.html

This will be fixed in a future version!

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

use GKB::Config;
use GKB::DBAdaptor;
use GKB::HTMLUtils;
use GKB::FileUtils;
use GKB::Utils;
use GKB::DocumentGeneration::TextUnit;
use GKB::DocumentGeneration::Reader;
use GKB::Graphics::ReactomeReaction;
use Storable qw(nstore retrieve);
use Data::Dumper;

@ISA = qw(GKB::DocumentGeneration::Reader);

my $pathways;
my $reaction_representation = 2;
my $chapter_num = 1;
my @global_text_units = ();
my $db_name;
my $db_user;
my $db_host;
my $db_pass;
my $db_port;
my $db_driver;
my $dba;
my $storable_root_dir = "/tmp";
my $DOCUMENT_STYLE_BOOK = "book";
my $DOCUMENT_STYLE_REPORT = "report";
my $DOCUMENT_STYLE_REVIEWERS_REPORT = "reviewers_report";

# List the object variables here, so that they can be checked
for my $attr
    (qw(
    stored_array_element_num
    stored_array_stem
    document_style
    instance_hash
    hyperlink_base_url
    hyperlink_db
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
    my($pkg, @args) = @_;
    my $self = bless {}, $pkg;
    
    $self->document_style($DOCUMENT_STYLE_BOOK);
    $self->hyperlink_base_url("http://www.reactome.org");
    $self->hyperlink_db("gk_current");
    
    my $instance_hash = {};
    $self->instance_hash($instance_hash);
    
    return $self;
}

sub set_document_style {
    my ($self, $document_style) = @_;
	
    $self->document_style($document_style);
}

sub set_document_style_book {
    my ($self) = @_;
	
    $self->document_style($DOCUMENT_STYLE_BOOK);
}

sub set_document_style_report {
    my ($self) = @_;
	
    $self->document_style($DOCUMENT_STYLE_REPORT);
}

sub set_document_style_reviewers_report {
    my ($self) = @_;
	
    $self->document_style($DOCUMENT_STYLE_REVIEWERS_REPORT);
}

sub is_document_style_book {
    my ($self) = @_;
	
	if ($self->document_style() eq $DOCUMENT_STYLE_BOOK) {
		return 1;
	}
	
	return 0;
}

sub is_document_style_report {
    my ($self) = @_;
	
	if ($self->document_style() eq $DOCUMENT_STYLE_REPORT) {
		return 1;
	}
	
	return 0;
}

sub is_document_style_reviewers_report {
    my ($self) = @_;
	
	if ($self->document_style() eq $DOCUMENT_STYLE_REVIEWERS_REPORT) {
		return 1;
	}
	
	return 0;
}

sub set_hyperlink_base_url {
    my ($self, $hyperlink_base_url) = @_;
	
    $self->hyperlink_base_url($hyperlink_base_url);
}

sub set_hyperlink_db {
    my ($self, $hyperlink_db) = @_;
	
    $self->hyperlink_db($hyperlink_db);
}

# Use Storable to put the elements of the supplied array
# reference into serialized files. Creates a stem to make the
# filenames hopefully unique.  Stem is stored internally.
sub store_array {
    my ($self, $array) = @_;
    
	my $user = $ENV{USER};
	if (!(defined $user)) {
		$user = "anonymous"
	}
	my $stem = $user . "_" . join("_", localtime());
	my $i;
	my $filename;
	for ($i=0;$i<scalar(@{$array}); $i++) {
		$filename = "$storable_root_dir/$stem" . "_" . $i;
		Storable::nstore($array->[$i], $filename);
	}
	
	$self->stored_array_element_num(0);
	$self->stored_array_stem($stem);
}

# Gets the next element from an array saved using "store_array".
# Returns undef if nothing more is left.  The corresponding
# file gets deleted, so you have only one chance to get this
# data!
sub retrieve_next_array_element {
    my ($self) = @_;
    
    if (!(defined $self->stored_array_stem)) {
    	return undef;
    }
    
    my $filename = "$storable_root_dir/" . $self->stored_array_stem . "_" . $self->stored_array_element_num;
    my $array_element = undef;
    if ( -e $filename ) {
    	$array_element = Storable::retrieve($filename);
		unlink($filename);
    }

	if (defined $array_element) {
		my $stored_array_element_num = $self->stored_array_element_num;
		$stored_array_element_num++;
		$self->stored_array_element_num($stored_array_element_num);
	}
	
	return $array_element;
}

sub set_db_params {
    my ($self, $name, $user, $host, $pass, $port, $driver) = @_;

    $db_name = $name;
    $db_user = $user;
    $db_host = $host;
    $db_pass = $pass;
    $db_port = $port;
    $db_driver = $driver;
}

sub open_db_connection {
    my ($self) = @_;

    # Get DBAdaptor
    $dba = GKB::DBAdaptor->new
	(
	 -dbname => $db_name,
	 -user   => $db_user,         ,
	 -host   => $db_host,
	 -pass   => $db_pass,
	 -port   => $db_port || 3306,
	 -driver => $db_driver,
	 );
}

# If you already have the dba, you can call this subroutine instead of going
# through the mill of first calling set_db_params and then open_db_connection.
sub set_dba {
    my ($self, $external_dba) = @_;

    $dba = $external_dba;
}

sub init_pathways {
    my ($self, $ids) = @_;

    my @local_pathways;
    if (@{$ids} && scalar(@{$ids})>0) {
	# $ids is a list of DB_IDs for pathways or reactions that
	# you want to use to restrict the scope of what gets printed
	# out.
	@local_pathways = GKB::Utils->get_pathways_by_id($dba, $ids);
    } else {
	@local_pathways = GKB::Utils->get_top_level_pathways($dba);
    }

    $pathways = \@local_pathways;
}

sub set_pathways {
    my ($self, $external_pathways) = @_;

    $pathways = $external_pathways;
}

# Set the reaction representation:
#
# 0 - no reactions get emitted
# 1 - emit reaction as text
# 2 - emit a graphical version of the reaction
sub set_reaction_representation {
    my ($self, $external_reaction_representation) = @_;

    $reaction_representation = $external_reaction_representation;
}

# Returns the author(s) of this publication
sub get_author {
    my ($self) = @_;

    # Derive a list of authors from the pathways
    my %author_hash = ();
    my $pathway; # Pathway object
    my $created; # InstanceEdit object
    my $author;  # Person object
    my $name;    # String
    foreach my $pathway (@{$pathways}) {
		foreach $author (@{GKB::Utils::get_authors_recursively($pathway)}) {
			$name = "";
			if ($author->initial && $author->initial->[0]) {
				$name .= $author->initial->[0];
			}
			if ($author->surname && $author->surname->[0]) {
				if ($name) {
					$name .= " ";
				}
				$name .= $author->surname->[0];
			}
			if ($name) {
				$author_hash{$name} = $name;
			}
		}
    }
    my $authors = join("; ", keys(%author_hash));

    return $authors;
}

# Returns the company or institution primarily responsible for the publication
sub get_company {
    my ($self) = @_;

    return "European Bioinformatics Institute & Cold Spring Harbor Laboratory";
}

# Returns the publication's title
sub get_title {
    my ($self) = @_;

    return "The Reactome Book";
}

# Returns a slightly more detailed description of the publication's contents
sub get_subject {
    my ($self) = @_;

    return "A textbook of biological pathways";
}

# Returns the copyright terms for the publication
sub get_copyright_conditions {
    my ($self) = @_;

    return "The contents of this book may be freely copied and distributed in any media, provided the authors, plus the European Bioinformatics Institute and the Cold Spring Harbor Laboratory, are credited.";
}

# Emits a page containing the preface text.
sub get_preface {
    my ($self) = @_;

#    # Pull the text out of the about.html file
#    my $content = $self->get_about_html_content();
#    my $text = "";
#    my @lines = GKB::HTMLUtils->extract_lines_from_html($content, 'begin about', 'end about');
#    my $line;
#    foreach $line (@lines) {
#	    $line = GKB::HTMLUtils->remove_html_tags($line);
#	    $line = GKB::HTMLUtils->remove_white_padding($line);
#	    $line .= ' ';
#    	$text .= $line;
#    }
#    
#    # Get rid of any spaces that got added at the beginning or end
#	$text = GKB::HTMLUtils->remove_white_padding($text);
#	
#	# Tidy up commas and full-stops
#    $text =~ s/ +,/,/g;
#    $text =~ s/ +\././g;
#    $text =~ s/,/, /g;
#    $text =~ s/, +/, /g;
#    $text =~ s/\./. /g;
#    $text =~ s/\. +/.  /g;
#    
#    # Remove stuff that smells of HTML comments
#    $text =~ s/^-->//;
#    $text =~ s/<!--$//;
#    
#    # Pop a newline onto the end, otherwise it doesn't get treated
#    # as a paragraph
#    $text .= "\n";
    my $text = "REACTOME is an open-source, open access, manually curated and peer-reviewed pathway database. Pathway annotations are authored by expert biologists, in collaboration with Reactome editorial staff and cross-referenced to many bioinformatics databases. These include NCBI Entrez Gene, Ensembl and UniProt databases, the UCSC and HapMap Genome Browsers, the KEGG Compound and ChEBI small molecule databases, PubMed, and Gene Ontology.\n";
    
    print STDERR "get_preface: text=|$text|\n";

    return $text;
}

# Get a page of kowtowing acknowledgements
sub get_acknowledgements {
    my ($self) = @_;

    my $text = "";

    # The grant awarding bodies
    my $acknowledgements = "The development of Reactome is supported by an EU STRP, EMI-CD grant from the European Union, the EBI Industry program and grant P41 HG003751 from the National Human Genome Research Institute at the US National Institutes of Health.";
    $text .= $acknowledgements . "\n";

    # Pull the text out of the about.html file and extract the
    # lines it contains pertaining to the SAB
    my $content = $self->get_about_html_content();
    my @lines = GKB::HTMLUtils->extract_lines_from_html($content, 'Board member', 'Alumni');
    my @sab_members = $self->get_members(\@lines);;

    # Emit a list of SAB member's names
    if (@sab_members && scalar(@sab_members)>0) {
		$text .= "We would also like to thank our Scientific Advisory Board: ";
		for (my $i=0; $i<scalar(@sab_members); $i++) {
		    if ($i>0) {
				$text .= ", ";
		    }
		    $text .= $sab_members[$i];
		}
		$text .= ".\n";
    }

#    @lines = GKB::HTMLUtils->extract_lines_from_html($content, '<TH>Cold Spring Harbor Laboratory', 'TABLE');
#    my @cshl_team = $self->get_members(\@lines);
#    @lines = GKB::HTMLUtils->extract_lines_from_html($content, '<TH>European Bioinformatics Institute', 'TABLE');
#    my @ebi_team = $self->get_members(\@lines);
#    @lines = GKB::HTMLUtils->extract_lines_from_html($content, '<TH>New York University', 'TABLE');
#    my @nyu_team = $self->get_members(\@lines);
#    @lines = GKB::HTMLUtils->extract_lines_from_html($content, '<TH>Ontario Cancer Research Institute', 'TABLE');
#    my @oicr_team = $self->get_members(\@lines);
#    @lines = GKB::HTMLUtils->extract_lines_from_html($content, '<TH>Gene Ontology Consortium', 'TABLE');
#    my @go_team = $self->get_members(\@lines);
#    
#    my @teams = ();
#    push(@teams, @cshl_team);
#    push(@teams, @ebi_team);
#    push(@teams, @nyu_team);
#    push(@teams, @oicr_team);
#    push(@teams, @go_team);
#    @teams = sort {$self->get_last_word($a) cmp $self->get_last_word($b)} @teams;
#    my $team = "";
#    my $member;
#    foreach $member (@teams) {
#    	if (!($team eq "")) {
#    		$team .= ", ";
#    	}
#    	$team .= $member;
#    }
#
#    $text .= "The Reactome project, and therefore the existence of this book, depends finally on the Reactome team members themselves: $team.\n";

    return $text;
}

# Get group members from the supplied lines of HTML.
sub get_members {
    my ($self, $lines) = @_;

    my @members = ();
    my $member;
    foreach my $line (@{$lines}) {
		# Use the HTML tags to identify which lines contain
		# member names.  Break out if the word "alumni" is
		# encountered, this means that the remaining list
		# members no longer work for us.
		if ($line =~ /\<tr\>\<td\>(.+)\<\/td\>/i) {
		    $member=$1;
	
		    if ($member =~ /alumni/i) {
				last;
		    }
		    # Remove starting and trailing white space
		    $member = GKB::HTMLUtils->remove_white_padding($member);
		    
		    # Replace tabs with spaces
		    $member =~ s/\t+/ /g;
	
		    push @members, $member;
		}
    }
    
    return @members;
}

sub get_last_word {
    my ($self, $text) = @_;

	$text =~ / ([^ ]+)$/;
	my $last_word = $1;
	
	return $last_word;
}

# If $GK_ROOT_DIR is not defined, makes the assumption that the current directory is:
#
# .../GKB/scripts/TheReactomeBook
#
# This is not very nice!!
sub get_about_html_content {
    my ($self) = @_;
    
    my $about_html = "../../website/html/about.html";
	if (defined $GK_ROOT_DIR) {
		$about_html = "$GK_ROOT_DIR/website/html/about.html";
	}
    my $content = GKB::FileUtils->read_file($about_html);
    
    return $content;
}

# Get the next text unit from the input stream.
# It would be nice if this worked in its own thread.
# It would be nice if this didn't have to take any arguments.
sub get_next_text_unit {
    my ($self, $depth, $depth_limit,$include_images_flag) = @_;
    
    # If there are still text units for the current chapter hanging around,
    # retrieve one of those.
    my $text_unit = $self->retrieve_next_array_element();
    
    # If there are no more text units for the current chapter, assume that
    # the end of the chapter has been reached and try to start a new
    # chapter.
    unless ($text_unit) {
    	# Get next top-level pathway
		my $pathway = shift @{$pathways};
		if ($pathway) {
		    my @initial_pathways = ($pathway);
		    	
		    my @text_units = $self->get_chapters_from_events_text_units($depth, $depth_limit, $chapter_num, "", \@initial_pathways, $include_images_flag);
	
		    if (@text_units && scalar(@text_units)>0) {
		    	$self->store_array(\@text_units);

		    	# Pop the first array element out of its file
		    	$text_unit = $self->retrieve_next_array_element();
		    }
	
		    $chapter_num++;
		}
    }

    # If there are no more chapters left, assume we are at the end of
    # the book and emit an EOF.
    unless ($text_unit) {
		$text_unit = GKB::DocumentGeneration::TextUnit->new();
		$text_unit->set_type("eof");
    }

    return $text_unit;
}

# Get the next text unit from the input stream.
# It would be nice if this worked in its own thread.
# It would be nice if this didn't have to take any arguments.
sub get_next_text_unit_old {
    my ($self, $depth, $depth_limit,$include_images_flag) = @_;

    my $text_unit;
    my $blobbi_text_unit = $self->retrieve_next_array_element();
    
	if (defined $blobbi_text_unit) {
    	print STDERR "ReactomeDatabaseReader.get_next_text_unit: blobbi_text_unit=$blobbi_text_unit\n";
	} else {
    	print STDERR "ReactomeDatabaseReader.get_next_text_unit: blobbi_text_unit=undef\n";
	}

    # If there are still text units for the current chapter hanging around,
    # pull off one of those.
    if (@global_text_units && scalar(@global_text_units)>0) {
		$text_unit = shift @global_text_units;
    }

    # If there are no more text units for the current chapter, assume that
    # the end of the chapter has been reached and try to start a new
    # chapter.
    unless ($text_unit) {
		my $pathway = shift @{$pathways};
		if ($pathway) {
		    my @initial_pathways = ($pathway);
		    @global_text_units = $self->get_chapters_from_events_text_units($depth, $depth_limit, $chapter_num, "", \@initial_pathways, $include_images_flag);
	
		    if (@global_text_units && scalar(@global_text_units)>0) {
		    	print STDERR "ReactomeDatabaseReader.get_next_text_unit: storing new array\n";
		    	$self->store_array(\@global_text_units);
		    	print STDERR "ReactomeDatabaseReader.get_next_text_unit: get a new blobbi\n";
		    	$blobbi_text_unit = $self->retrieve_next_array_element();
				if (defined $blobbi_text_unit) {
			    	print STDERR "ReactomeDatabaseReader.get_next_text_unit: blobbi_text_unit=$blobbi_text_unit\n";
				} else {
			    	print STDERR "ReactomeDatabaseReader.get_next_text_unit: blobbi_text_unit=undef\n";
				}
				$text_unit = shift @global_text_units;
		    }
	
		    $chapter_num++;
		}
    }

    # If there are no more chapters left, assume we are at the end of
    # the book and emit an EOF.
    unless ($text_unit) {
		$text_unit = GKB::DocumentGeneration::TextUnit->new();
		$text_unit->set_type("eof");
    }

    return $text_unit;
}

my @step_thru_increments = ();
my $step_thru_pathway;

# Get the next text unit from the input stream.
# It would be nice if this worked in its own thread.
# It would be nice if this didn't have to take any arguments.
sub get_next_text_unit_step_thru {
    my ($self, $depth, $depth_limit, $include_images_flag) = @_;

    my $text_unit;

    # If there are still text units for the current chapter hanging around,
    # pull off one of those.
    if (@global_text_units && scalar(@global_text_units)>0) {
	$text_unit = shift @global_text_units;
    } else {
	# If this is the first time we have called this subroutine or 
	# @global_text_units is empty, try to extract more text units.
	if (!$step_thru_pathway) {
	    $step_thru_pathway = shift @{$pathways};
	}

	if ($step_thru_pathway) {
	    my @initial_pathways = ($step_thru_pathway);
	    @global_text_units = $self->get_chapters_from_events_text_units($depth, $depth_limit, $chapter_num, "", \@initial_pathways, $include_images_flag, @step_thru_increments);

	    if (@global_text_units && scalar(@global_text_units)>0) {
		$text_unit = shift @global_text_units;
	    } else {
		$step_thru_pathway = shift @{$pathways};
		if ($step_thru_pathway) {
		    $chapter_num++;
		    @initial_pathways = ($step_thru_pathway);
		    @global_text_units = $self->get_chapters_from_events_text_units($depth, $depth_limit, $chapter_num, "", \@initial_pathways, $include_images_flag, @step_thru_increments);
		    if (@global_text_units && scalar(@global_text_units)>0) {
			$text_unit = shift @global_text_units;
		    }
		}
	    }
	}
    }

    # If there are no more chapters left, assume we are at the end of
    # the book and emit an EOF.
    unless ($text_unit) {
	$text_unit = GKB::DocumentGeneration::TextUnit->new();
	$text_unit->set_type("eof");
    }

    return $text_unit;
}

# Make the current chapter number visible to the whole world
sub get_chapter_num {
    return $chapter_num;
}

# Recurse down in the tree of event instances and for each event, add
# text and diagrams to the growing book.
#
# step_thru_increments allows
# you to call this subroutine multiple times and will get a different
# event each time until all events are exhausted.  If specified, then
# only those text units will be returned that are relevant to the
# current event.  Once all events have been processed, an empty list
# of text units will be returned.  This means that the subroutine
# might need to be called many times before all of the text units
# have been generated, an empty list of text units signifying completion.
# This allows you to extract text units in small bursts, rather than
# as one long list, which could be more memory efficient.
#
# Arguments:
#
# depth - current depth (how deep we are in the event hierarchy)
# depth_limit - maximum recursion depth, negative value recurses right down to bottom.
# section - the section number (e.g. 2.1.23) of the event above in the hierarchy.
# events - list of events to start recursion from
# step_thru_increments - a list showing the event currently being dealt with at every level of the recursion. Optional.
#
# Returns nothing
sub get_chapters_from_events_text_units {
    my ($self, $depth, $depth_limit, $section, $parent_event, $events, $include_images_flag, $step_thru_increments) = @_;

    my @actual_events = @{$events};
    if (scalar(@actual_events) == 2 && $events->[0]->is_a("Pathway") && $events->[1]->is_a("Pathway")) {
        print STDERR "ReactomeDatabaseReader.get_chapters_from_events_text_units: we have 2 pathways\n";

        if (scalar(@{$events->[0]->disease}) > 0 && scalar(@{$events->[1]->disease}) == 0) {
            print STDERR "ReactomeDatabaseReader.get_chapters_from_events_text_units: first pathway is diseased\n";
            @actual_events = ($events->[0]);
        }
        if (scalar(@{$events->[0]->disease}) == 0 && scalar(@{$events->[1]->disease}) > 0) {
            print STDERR "ReactomeDatabaseReader.get_chapters_from_events_text_units: second pathway is diseased\n";
            @actual_events = ($events->[1]);
        }
    }

    # List of basic text units to be emited
    my @text_units = ();
    my $text_unit;

    if ($depth_limit>=0 && $depth==$depth_limit) {
		return @text_units;
    }

    # Work out where we are in the iteration.  If step_thru_position is -1,
    # then it will be ignored by this subroutine, which will consequently
    # act like a normal recursive subroutine.
    my $step_thru_position = (-1);
    if ($step_thru_increments) {
		if (!$step_thru_increments->[$depth]) {
		    $step_thru_increments->[$depth]=0;
		}
	
		$step_thru_position = $step_thru_increments->[$depth];
    }

    my $event;
    my $new_depth = $depth+1;

# 28.11.07 Bernard would like to suppress printing of sub-event listing    
#    # Only emit the subevent list once.
#    if ($step_thru_position<1) {
#	       @text_units = (@text_units, $self->get_subevent_summary($event, $parent_event, \@actual_events, $depth, $new_depth, $depth_limit, $include_images_flag));
#    }
    
    # Print each event and then recurse down to sub-events, if there
    # are any.
    my $event_num;
    my $added_sub_pathway_flag;
    my $new_section;
    my $instance_hash = $self->instance_hash;
    my $instance_text;
    for ($event_num = 0; $event_num<scalar(@actual_events); $event_num++) {
		# Limit recursion to a single event.
		if ($step_thru_increments) {
		    if ($event_num<$step_thru_position) {
				next;
		    } elsif ($event_num>$step_thru_position){
				last;
		    }
		}
	
		$event = $actual_events[$event_num];

		# Add a title and/or a list of authors to the beginning of the document
		if ($depth==0 && !($self->is_document_style_book()) && $event->is_a("Pathway") && scalar(GKB::Utils->get_instance_sub_events($event))>0) {
			if ($self->is_document_style_report()) {
				@text_units = (@text_units, $self->get_report_title($event, $depth));
				@text_units = (@text_units, $self->get_report_authors($event));
			} elsif ($depth==0 && !$self->is_document_style_report() && $event->is_a("Pathway") && scalar(GKB::Utils->get_instance_sub_events($event))>0) {
				@text_units = (@text_units, $self->get_reviewers_report_title($event, $depth));
				@text_units = (@text_units, $self->get_reviewers_report_introduction($event, $depth));
			}
		}
		
		if ($depth == 0) {
		    $new_section = $section;
		} else {
		    $new_section = $self->get_new_section($section, $event_num+1);
		}
	
		# Don't try to emit an event that lacks content (text or image)
		if ($event->is_a("Pathway") && !( GKB::Utils->is_pathway_with_content($event, $new_depth, $depth_limit) )) {
			next;
		}
		
		# Only include reactions if the user wants them
		if (!$event->is_a("ReactionlikeEvent") || $reaction_representation) {
		    # Emit event
		    @text_units = (@text_units, $self->get_instance_text_units($event, $depth, $new_section, $instance_hash, $include_images_flag));
		}
	
		# Find all sub- pathways and reactions
		my @sub_events = GKB::Utils->get_instance_sub_events($event);
	
		# Go down one level in the recursion, glub, glub, glub
		my @new_text_units = ();
		if (@sub_events && scalar(@sub_events)>0) {
		    @new_text_units = $self->get_chapters_from_events_text_units($new_depth, $depth_limit, $new_section, $event, \@sub_events, $include_images_flag, $step_thru_increments);
		}
	
		if (@new_text_units && scalar(@new_text_units)>0) {
		    @text_units = (@text_units, @new_text_units);
		} else {
		    if ($step_thru_increments) {
			# If the next level of recursion no longer returns any
			# text units, it's an indication that it has been exhausted
			# and so we can go on to the next event.
			$step_thru_increments->[$depth]++;
		    }
		}
		
		# Add a literature list to the end of the document
		if ($depth==0 && !($self->document_style() eq $DOCUMENT_STYLE_BOOK) && $event->is_a("Pathway") && scalar(GKB::Utils->get_instance_sub_events($event))>0) {
			@text_units = (@text_units, $self->get_event_literature_summary($event, $depth));
		}
    }

    return @text_units;
}

sub get_report_title {
    my ($self, $event, $depth) = @_;

    # List of basic text units to be emited
    my @text_units = ();
    my $text_unit;

    my $instance_name = $self->get_instance_name($event);
	$text_unit = GKB::DocumentGeneration::TextUnit->new();
	$text_unit->set_type("section_title");
	$text_unit->set_contents("Report for $instance_name");
	$text_unit->set_depth($depth);
	push @text_units, $text_unit;
	
	return @text_units;
}

sub get_reviewers_report_title {
    my ($self, $event, $depth) = @_;

    # List of basic text units to be emited
    my @text_units = ();
    my $text_unit;

    my $instance_name = $self->get_instance_name($event);
	$text_unit = GKB::DocumentGeneration::TextUnit->new();
	$text_unit->set_type("section_title");
	$text_unit->set_contents("Report for Pathway Review");
	$text_unit->set_depth($depth);
	push @text_units, $text_unit;
	
	return @text_units;
}

sub get_reviewers_report_introduction {
    my ($self, $event, $depth) = @_;

    # List of basic text units to be emited
    my @text_units = ();
    my $text_unit;

    $text_unit = GKB::DocumentGeneration::TextUnit->new();
    $text_unit->set_type("section_header");
    $text_unit->set_contents("Introduction");
    $text_unit->set_depth($depth);
    push @text_units, $text_unit;

    my $instance_name = $self->get_instance_name($event);
	my $text = "This report is intended for reviewers of the pathway \"$instance_name\".  It has been automatically generated.";
	$text_unit = GKB::DocumentGeneration::TextUnit->new();
	$text_unit->set_type("body_text");
	$text_unit->set_contents($text);
	push @text_units, $text_unit;
	
	$text = "Please note that non-ASCII characters (primarily in author names) are not displayed correctly in this document. We apologize for this inconvenience.";
	$text_unit = GKB::DocumentGeneration::TextUnit->new();
	$text_unit->set_type("body_text");
	$text_unit->set_contents($text);
	push @text_units, $text_unit;
	
	$text = "You may come across overlapping/redundant information in the text summaries of the events. The reason that we include this redundancy is to accommodate users that are not reading the module through as a chapter but rather coming to the website to visit individual pages. Each summary is meant to provide some context. We realize that this is not optimal for the person that happens to be reading the module as a chapter, but we try to strike a balance. Any suggestions for improvement are welcome!";
	$text_unit = GKB::DocumentGeneration::TextUnit->new();
	$text_unit->set_type("body_text");
	$text_unit->set_contents($text);
	push @text_units, $text_unit;
	
	$text = "Our reference list is not necessarily complete. For each reaction, we aim to provide at least one primary literature reference that demonstrates the occurrence of a given reaction in humans. We unfortunately do not have the resources to identify all relevant references, but we would be happy to cite any that you feel should be included.";
	$text_unit = GKB::DocumentGeneration::TextUnit->new();
	$text_unit->set_type("body_text");
	$text_unit->set_contents($text);
	push @text_units, $text_unit;
	
	$text = "Each reaction description is followed by a reaction diagram that shows the input and output molecules for that reaction and lists the reaction that immediately precedes it.  ";
	$text_unit = GKB::DocumentGeneration::TextUnit->new();
	$text_unit->set_type("body_text");
	$text_unit->set_contents($text);
	push @text_units, $text_unit;
	
	$text = "The description of any human reaction that has been inferred from a reaction in another species is followed (in the text) by a description of the corresponding reaction in that other species. At first glance, this may appear to be a duplication, but the primary literature reference and the molecular description are provided only for the reference species. A more detailed description of the human reaction can be found on the corresponding webpage.";
	$text_unit = GKB::DocumentGeneration::TextUnit->new();
	$text_unit->set_type("body_text");
	$text_unit->set_contents($text);
	push @text_units, $text_unit;
	
	return @text_units;
}

sub get_report_authors {
    my ($self, $event) = @_;

    # List of basic text units to be emited
    my @text_units = ();
    my $text_unit;

	my $instance_authors = GKB::Utils::get_authors_recursively($event);
	$self->get_literature_refs(\@text_units, $instance_authors, "", 1);
			
	$text_unit = GKB::DocumentGeneration::TextUnit->new();
	$text_unit->set_type("vertical_space");
	$text_unit->set_contents(2);
	push @text_units, $text_unit;
	
	return @text_units;
}

sub get_subevent_summary {
    my ($self, $event, $parent_event, $events, $depth, $new_depth, $depth_limit, $include_images_flag) = @_;

    # List of basic text units to be emited
    my @text_units = ();
    my $text_unit;
		my $event_name;
		if ($depth>0) {
		    # Are all events either Pathway or Reaction, or are they mixed?
		    my $event_type = "";
		    foreach $event (@{$events}) {
				if ($event->is_a("Pathway")) {
				    if (!($event_type)) {
						$event_type = "Sub-pathways";
				    } elsif (!($event_type eq "Sub-pathways")) {
						$event_type = "Events";
				    }
				} elsif ($event->is_a("ReactionlikeEvent")) {
				    if (!($event_type)) {
						$event_type = "Reactions";
				    } elsif (!($event_type eq "Reactions")) {
						$event_type = "Events";
				    }
				} else {
				    $event_type = "Events";
				}
		    }
	
		    $text_unit = GKB::DocumentGeneration::TextUnit->new();
		    $text_unit->set_type("vertical_space");
		    $text_unit->set_contents(2);
		    push @text_units, $text_unit;
	
		    # Create a bullited list of the sub-events that will subsequently
		    # be explored in depth.
		    $text_unit = GKB::DocumentGeneration::TextUnit->new();
		    $text_unit->set_type("section_internal_header");
		    my $list_heading = "making up this pathway";
		    if ($parent_event->is_a("ConceptualEvent")) {
				$list_heading = "representative for this event";
		    }
		    $text_unit->set_contents("$event_type $list_heading");
		    push @text_units, $text_unit;
		
		    $text_unit = GKB::DocumentGeneration::TextUnit->new();
		    $text_unit->set_type("vertical_space");
		    $text_unit->set_contents(1);
		    push @text_units, $text_unit;
		
		    my $bullit_text;
		    foreach $event (@{$events}) {
				if ($event->is_a("Pathway")) {
				    $event_type = "Pathway";
				} else {
				    if ($event->is_a("ReactionlikeEvent")) {
#					    if ($reaction_representation == 0) {
#						next;
#					    }
			
						$event_type = "Reaction";
				    } else {
						$event_type = "Event";
				    }
				}
		
				$event_name = $event->attribute_value("name")->[0];
				unless ($event_name) {
				    $event_name = $event->attribute_value("_displayName")->[0];
				}
				unless ($event_name) {
				    $event_name = "UNKNOWN";
				}
			    
				$bullit_text = "  $event_type: $event_name";
				if (!$event->is_a("ReactionlikeEvent") && !( GKB::Utils->is_pathway_with_content($event, $new_depth, $depth_limit, $include_images_flag) )) {
				    $bullit_text .= " (no information currently available for this sub-pathway).";
				}
				$text_unit = GKB::DocumentGeneration::TextUnit->new();
				$text_unit->set_type("bullit_text");
				$text_unit->set_contents($bullit_text);
				push @text_units, $text_unit;
		    }
		}

    return @text_units;
}

sub get_event_literature_summary {
    my ($self, $event, $depth) = @_;

    # List of basic text units to be emited
    my @text_units = ();
    my $text_unit;

	my @instance_references = GKB::Utils->get_event_literature_references_recursive($event);
	if (scalar(@instance_references)>0) {
		$text_unit = GKB::DocumentGeneration::TextUnit->new();
		$text_unit->set_type("vertical_space");
		$text_unit->set_contents(4);
		push @text_units, $text_unit;

    	my $instance_name = $self->get_instance_name($event);
		$text_unit = GKB::DocumentGeneration::TextUnit->new();
		$text_unit->set_type("section_header");
	    $text_unit->set_contents("Full List of Literature References for Pathway \"$instance_name\"");
		$text_unit->set_depth($depth);
		push @text_units, $text_unit;
		
		foreach my $instance_reference (@instance_references) {
			$text_unit = GKB::DocumentGeneration::TextUnit->new();
			$text_unit->set_type("body_text_paragraph");
			$text_unit->set_contents($instance_reference);
			push @text_units, $text_unit;
		}
				
		$text_unit = GKB::DocumentGeneration::TextUnit->new();
		$text_unit->set_type("vertical_space");
		$text_unit->set_contents(2);
		push @text_units, $text_unit;
	}

    return @text_units;
}

sub get_instance_name {
    my ($self, $instance) = @_;

    my $instance_name = $instance->attribute_value("name")->[0];
    unless ($instance_name) {
		$instance_name = $instance->attribute_value("_displayName")->[0];
    }
    unless ($instance_name) {
		$instance_name = "UNKONOWN";
    }

	return $instance_name;
}

# Returns s list of text units for the supplied instance
sub get_instance_text_units {
    my ($self, $instance, $depth, $new_section, $instance_hash, $include_images_flag) = @_;

    my @text_units = ();
    my $text_unit;

    unless ($instance) {
		print STDERR "get_instance_text_units: WARNING - instance is null!\n";
		return @text_units;
    }

    my $instance_name = $self->get_instance_name($instance);
    my $instance_db_id = $instance->db_id();

	my $instance_authors;
	my $instance_editors;
	my $instance_reviewers;
	
	my $event_type = lc($instance->class());
	if ($instance->is_a("Event")) {
		$event_type = "event";
		if ($instance->is_a("ReactionlikeEvent")) {
			$event_type = "reaction";
		} elsif ($instance->is_a("Pathway")) {
			$event_type = "pathway";
		}
	}

    # Emit a header containing the instance name, to start the new section
    $text_unit = GKB::DocumentGeneration::TextUnit->new();
    $text_unit->set_type("section_header");
    $text_unit->set_contents($new_section . "  " . $instance_name . " (" . $instance->class . ")");
    $text_unit->set_depth($depth);
    push @text_units, $text_unit;

	if ($instance->is_a("ReactionlikeEvent")) {
	    if ($reaction_representation==1) {
			@text_units = (@text_units, $self->get_reaction_description_text_units($instance));
		} elsif ($reaction_representation==2) {
			@text_units = (@text_units, $self->get_reaction_diagram_text_units($instance));
	    }
	}

    # Emit a hyperlink to the Reactome page for the instance
#    if ($instance->is_a("Pathway")) { # Hack for Lisa
#        if (defined $self->hyperlink_base_url && defined $self->hyperlink_db) {
#            $text_unit = GKB::DocumentGeneration::TextUnit->new();
#            $text_unit->set_type("vertical_space");
#            push @text_units, $text_unit;
#            $text_unit = GKB::DocumentGeneration::TextUnit->new();
#            $text_unit->set_type("hyperlink");
#            $text_unit->set_contents("See web page for this $event_type");
##            $text_unit->set_url($self->hyperlink_base_url . "/cgi%2Dbin/eventbrowser?DB=" . $self->hyperlink_db . "&ID=$instance_db_id");
#            $text_unit->set_url($self->hyperlink_base_url . "/PathwayBrowser/#FOCUS_PATHWAY_ID=$instance_db_id");
#            push @text_units, $text_unit;
#        }
#    }

    # If the instance has already been encountered
    # somewhere else, simply emit a reference to the appropriate
    # section, rather than printing out the content.
    if ($instance_hash->{$instance_db_id}) {
		$text_unit = GKB::DocumentGeneration::TextUnit->new();
		$text_unit->set_type("body_text_paragraph");
		$text_unit->set_contents("Please refer to section " . $instance_hash->{$instance_db_id} . " for full details of \"" . $instance_name . "\"\n");
		push @text_units, $text_unit;
    } else {
		$instance_hash->{$instance_db_id} = $new_section;
		# The latest request from CSHL is not to do this recursively,
		# but I'm leaving the recursive stuff there, commented out,
		# just in case they change their minds on this for a third
		# time and decide they want them back again.  There is a
		# drawback to doing things recursively: the methods that
		# do this try to deal with both new and old data models,
		# and this gives rise to gadzillions of warning messages
		# from GKB::Ontology.
#		$instance_authors = GKB::Utils::get_authors_recursively($instance);
		$instance_authors = $instance->authored;
#		$instance_editors = GKB::Utils::get_editors_recursively($instance);
		$instance_editors = $instance->edited;
#		$instance_reviewers = GKB::Utils::get_reviewers_recursively($instance);
		$instance_reviewers = $instance->reviewed;
	
		$self->get_literature_refs(\@text_units, $instance_authors, "Authors");
		$self->get_literature_refs(\@text_units, $instance_editors, "Editors");
		$self->get_literature_refs(\@text_units, $instance_reviewers, "Reviewers");
	
		my @descriptive_text_units = $self->get_descriptive_text_units_from_instance($instance);
		push @text_units, @descriptive_text_units;
	
		# Diagram
		if ($include_images_flag) {
			@text_units = (@text_units, $self->get_instance_diagram_text_units($instance));
		}
	
		# If this is a disease reaction, then there may well be a corresponding
		# "normal" reaction.  Get this, if available.
		if ($instance->is_valid_attribute("normalReaction") && (defined $instance->normalReaction) && scalar(@{$instance->normalReaction})>0) {
			@text_units = (@text_units, $self->get_associated_normal_reaction($instance, $event_type));
		}
		
		# Literature references
		my @instance_references = GKB::Utils->get_event_literature_references($instance);
		if (scalar(@instance_references)>0) {
		    $text_unit = GKB::DocumentGeneration::TextUnit->new();
		    $text_unit->set_type("section_internal_header");
		    $text_unit->set_contents("References");
		    push @text_units, $text_unit;
	
		    foreach my $instance_reference (@instance_references) {
				$text_unit = GKB::DocumentGeneration::TextUnit->new();
				$text_unit->set_type("body_text_paragraph");
				$text_unit->set_contents($instance_reference);
				push @text_units, $text_unit;
		    }
		}
		
		# The event in another species that this one was inferred
		# from, if such a thing exists
		if ($instance->is_valid_attribute("inferredFrom") && (defined $instance->inferredFrom) && scalar(@{$instance->inferredFrom})>0) {
			@text_units = (@text_units, $self->get_instances_from_which_inferred($instance, $event_type));
		}
		
		# Regulation
		if ($instance->is_a("ReactionlikeEvent")) {
			@text_units = (@text_units, $self->get_regulation_text_units_from_reaction($instance));
			@text_units = (@text_units, $self->get_regulated_input_text_units_from_reaction($instance));
			@text_units = (@text_units, $self->get_regulated_output_text_units_from_reaction($instance));
			@text_units = (@text_units, $self->get_regulated_catalyst_text_units_from_reaction($instance));
		}	
    }

    return @text_units;
}

sub get_instances_from_which_inferred() {
    my ($self, $instance, $event_type) = @_;

    my @text_units = ();
    my $text_unit;

	my $text = "Source $event_type";
	if (scalar(@{$instance->inferredFrom})>1) {
		$text .= "s";
	}
	$text_unit = GKB::DocumentGeneration::TextUnit->new();
	$text_unit->set_type("section_internal_header");
	$text_unit->set_contents("Source $event_type");
	push @text_units, $text_unit;
	
	my $inferred_from;
	my $species;
	foreach $inferred_from (@{$instance->inferredFrom}) {
		my $inferred_from_name = $inferred_from->_displayName->[0];
		my $inferred_from_species = "UNKNOWN";
		if ((defined $inferred_from->species) && scalar(@{$inferred_from->species})>0) {
			$inferred_from_species = "";
			foreach $species (@{$inferred_from->species}) {
				if (!($inferred_from_species eq "")) {
					$inferred_from_species .= ", ";
				}
				$inferred_from_species .= $species->_displayName->[0];
			}
		}
				
		my $text = "This $event_type was inferred from the corresponding $event_type \"$inferred_from_name\" in species $inferred_from_species.";
		$text_unit = GKB::DocumentGeneration::TextUnit->new();
		$text_unit->set_type("body_text_paragraph");
		$text_unit->set_contents($text);
		push @text_units, $text_unit;
				
		my @inferred_from_descriptive_text_units = $self->get_descriptive_text_units_from_instance($inferred_from);
		push(@text_units, @inferred_from_descriptive_text_units);

		my @inferred_from_literature_references = GKB::Utils->get_event_literature_references($inferred_from);
		if (scalar(@inferred_from_literature_references)>0) {
			$text_unit = GKB::DocumentGeneration::TextUnit->new();
			$text_unit->set_type("body_text_paragraph");
			$text_unit->set_contents("The following literature references support the source $event_type:");
			push @text_units, $text_unit;
		
		    foreach my $instance_reference (@inferred_from_literature_references) {
				$text_unit = GKB::DocumentGeneration::TextUnit->new();
				$text_unit->set_type("body_text_paragraph");
				$text_unit->set_contents($instance_reference);
				push @text_units, $text_unit;
		    }
		}
				
		$text_unit = GKB::DocumentGeneration::TextUnit->new();
	    $text_unit->set_type("vertical_space");
	    $text_unit->set_contents(1);
	    push @text_units, $text_unit;
	}
	
	return @text_units;
}

sub get_associated_normal_reaction() {
    my ($self, $instance, $event_type) = @_;

    my @text_units = ();
    my $text_unit;

    $text_unit = GKB::DocumentGeneration::TextUnit->new();
    $text_unit->set_type("section_internal_header");
    $text_unit->set_contents("Normal $event_type");
    push @text_units, $text_unit;
	
    $text_unit = GKB::DocumentGeneration::TextUnit->new();
    $text_unit->set_type("body_text_paragraph");
    $text_unit->set_contents("This $event_type has the corresponding normal (healthy) $event_type:");
    push @text_units, $text_unit;
		
    my $space_text_unit = GKB::DocumentGeneration::TextUnit->new();
    $space_text_unit->set_type("vertical_space");
    $space_text_unit->set_contents(1);
    push @text_units, $space_text_unit;

    foreach my $normal_event (@{$instance->normalReaction}) {
        my $normal_event_name = $self->get_instance_name($normal_event);
        $text_unit = GKB::DocumentGeneration::TextUnit->new();
        $text_unit->set_type("body_text_paragraph");
        $text_unit->set_contents($normal_event_name);
        push @text_units, $text_unit;
    
        $space_text_unit = GKB::DocumentGeneration::TextUnit->new();
        $space_text_unit->set_type("vertical_space");
        $space_text_unit->set_contents(1);
        push @text_units, $space_text_unit;
    
        my @inferred_from_descriptive_text_units = $self->get_descriptive_text_units_from_instance($normal_event, 1);
        if (scalar(@inferred_from_descriptive_text_units)>0 && (defined $inferred_from_descriptive_text_units[0])) {
            $text_unit = GKB::DocumentGeneration::TextUnit->new();
            $text_unit->set_type("body_text_paragraph");
            $text_unit->set_contents("Summary:");
            push @text_units, $text_unit;
    
            $space_text_unit = GKB::DocumentGeneration::TextUnit->new();
            $space_text_unit->set_type("vertical_space");
            $space_text_unit->set_contents(1);
            push @text_units, $space_text_unit;
    
            push(@text_units, @inferred_from_descriptive_text_units);
        }
    }

    return @text_units;
}

sub get_regulation_text_units_from_reaction() {
    my ($self, $instance) = @_;

    my @text_units = ();
    my $text_unit;

	my @regulation_text_units = $self->get_regulation_text_units_from_instance($instance);
	if (scalar(@regulation_text_units) > 0) {
		$text_unit = GKB::DocumentGeneration::TextUnit->new();
		$text_unit->set_type("section_internal_header");
		$text_unit->set_contents("Regulators of this Reaction");
		push @text_units, $text_unit;
		
		@text_units = (@text_units, @regulation_text_units);
	}
				
    return @text_units;
}

sub get_regulated_input_text_units_from_reaction() {
    my ($self, $reaction) = @_;
    
    my @text_units = ();
    my $text_unit;
    
    my $instances = $reaction->input;
    my @instances_text_units = $self->get_regulation_text_units_from_instances($instances);
    if (scalar(@instances_text_units) > 0) {
		$text_unit = GKB::DocumentGeneration::TextUnit->new();
		$text_unit->set_type("section_internal_header");
		$text_unit->set_contents("Regulators of Reactants in this Reaction");
		push @text_units, $text_unit;
		
		@text_units = (@text_units, @instances_text_units);
    }

	return @text_units;
}

sub get_regulated_output_text_units_from_reaction() {
    my ($self, $reaction) = @_;
    
    my @text_units = ();
    my $text_unit;
    
    my $instances = $reaction->output;
    my @instances_text_units = $self->get_regulation_text_units_from_instances($instances);
    if (scalar(@instances_text_units) > 0) {
		$text_unit = GKB::DocumentGeneration::TextUnit->new();
		$text_unit->set_type("section_internal_header");
		$text_unit->set_contents("Regulators of Products in this Reaction");
		push @text_units, $text_unit;
		
		@text_units = (@text_units, @instances_text_units);
    }

	return @text_units;
}

sub get_regulated_catalyst_text_units_from_reaction() {
    my ($self, $reaction) = @_;
    
    my @text_units = ();
    my $text_unit;
    
    my $catalyst_activities = $reaction->catalystActivity;
    my @instances_text_units = ();
	foreach my $catalyst_activity (@{$catalyst_activities}) {
		my $physical_entity = $catalyst_activity->physicalEntity->[0];
		if (defined $physical_entity) {
			@instances_text_units = (@instances_text_units, $self->get_regulation_text_units_from_instance($physical_entity));
		}
	}
    if (scalar(@instances_text_units) > 0) {
		$text_unit = GKB::DocumentGeneration::TextUnit->new();
		$text_unit->set_type("section_internal_header");
		$text_unit->set_contents("Regulators of Enzymes in this Reaction");
		push @text_units, $text_unit;
		
		@text_units = (@text_units, @instances_text_units);
    }

	return @text_units;
}

sub get_regulation_text_units_from_instances() {
    my ($self, $instances) = @_;
    
    my @text_units = ();
    
	foreach my $instance (@{$instances}) {
		@text_units = (@text_units, $self->get_regulation_text_units_from_instance($instance));
	}

	return @text_units;
}

sub get_regulation_text_units_from_instance() {
    my ($self, $instance) = @_;

    my @text_units = ();
    my $text_unit;

	my $regulators = $instance->reverse_attribute_value('regulatedEntity');
	if (!(defined $regulators) || scalar(@{$regulators}) < 1) {
    	return @text_units;
	}
	
	my $text;
	foreach my $regulator (@{$regulators}) {
		$text = $regulator->_displayName->[0];
		my @regulator_literature_references = GKB::Utils->get_event_literature_references($regulator);
		if (scalar(@regulator_literature_references) > 0) {
			$text .= " (reference";
			if (scalar(@regulator_literature_references) > 1) {
				$text .= "s";
			}
			$text .= " supporting this regulation: ";
			for (my $i=0; $i<scalar(@regulator_literature_references); $i++) {
				my $regulator_literature_reference = $regulator_literature_references[$i];
				if ($i > 0) {
					$text .= ", ";
				}
				$text .= $regulator_literature_reference;
			}
			$text .= ")";
		}
					
		$text_unit = GKB::DocumentGeneration::TextUnit->new();
		$text_unit->set_type("body_text_paragraph");
		$text_unit->set_contents($text);
		push @text_units, $text_unit;
	}

    return @text_units;
}

sub get_descriptive_text_units_from_instance() {
    my ($self, $instance, $use_italics) = @_;

    my @text_units = ();
    my $text_unit;

    unless ($instance) {
		print STDERR "ReactomeDatabaseReader.get_descriptive_text_units_from_instance: WARNING - instance is null!\n";
		return @text_units;
    }

	my $text = GKB::Utils->get_pathway_text($instance);
	if ($text) {
		# Translate various exotic formats to plain ASCII
		$text =~ s/\x91/`/g; #` Taken from "demoroniser" source
		$text =~ s/\x92/'/g; #' Taken from "demoroniser" source
		$text =~ s/\x93/"/g; #" Taken from "demoroniser" source
		$text =~ s/\x94/"/g; #" Taken from "demoroniser" source
		$text =~ s/&#8216;/`/g;#` Taken from "demoroniser" source
		$text =~ s/&#8217;/'/g;#' Taken from "demoroniser" source
		$text =~ s/&#8219;/`/g;#` Taken from "demoroniser" source
		$text =~ s/&#8220;/"/g;#" Taken from "demoroniser" source
		$text =~ s/&#8221;/"/g;#" Taken from "demoroniser" source
		$text =~ s/&#8222;/"/g;#" Taken from "demoroniser" source
		$text =~ s/&#8223;/"/g;#" Taken from "demoroniser" source
	
		$text =~ s/\xE2\x80\x9C/"/g;; #" found using hexedit
		$text =~ s/\xE2\x80\x9D/"/g;; #" found using hexedit

		# Try to do something sensible with HTML tags
		$text =~ s/<br>/__LINEBREAK__/ig;
		$text =~ s/<br[^0-9a-zA-Z]/__LINEBREAK__/ig; # to cope with somebody forgetting the closing >
		$text =~ s/<\/*p>/__LINEBREAK__/ig;
		$text =~ s/<[a-zA-Z]+>/ /g;
	
#		$text_unit = GKB::DocumentGeneration::TextUnit->new();
#		$text_unit->set_type("section_internal_header");
#		$text_unit->set_contents("Description");
#		push @text_units, $text_unit;
    	my $space_text_unit = GKB::DocumentGeneration::TextUnit->new();
    	$space_text_unit->set_type("vertical_space");
    	$space_text_unit->set_contents(1);
    	push @text_units, $space_text_unit;
	
		my @text_blocks = split(/__LINEBREAK__/, $text);
		foreach my $text_block (@text_blocks) {
			$text_unit = GKB::DocumentGeneration::TextUnit->new();
			$text_unit->set_type("body_text");
			if (defined $use_italics) {
		            $text_block =~ s/<\/*i>//ig;
			    $text_unit->set_contents("<i>$text_block</i>");
			} else {
			    $text_unit->set_contents($text_block);
			}
			push @text_units, $text_unit;
		}
	}
	
	return @text_units;
}

# Generates the text units for predecessor or successor events.
# Arguments:
#
# instance_hash - references section number by Event instance
# event_type - reaction, pathway, or whatever
# flanking_events_ref - list of precursor or successor events
# flanking_name - can be either "Preceding" or "Successor"
# flanking_relation - can be either "follows on from" or "leads on to"
sub get_flanking_events {
    my ($self, $instance_hash, $event_type, $flanking_events_ref, $flanking_name, $flanking_relation) = @_;

    my @text_units = ();
    my $text_unit;

    if ($flanking_events_ref && @{$flanking_events_ref} && scalar(@{$flanking_events_ref}) > 0) {
		$text_unit = GKB::DocumentGeneration::TextUnit->new();
		$text_unit->set_type("section_internal_header");
		$text_unit->set_contents("$flanking_name Events");
		push @text_units, $text_unit;
	
		my $flanking_event_text;
		my $flanking_event_type;
		my $flanking_event_name;
		my $flanking_event;
		if (scalar(@{$flanking_events_ref}) == 1) {
		    $flanking_event = $flanking_events_ref->[0];
		    $flanking_event_type = "event";
		    if ($flanking_event->is_a("ReactionlikeEvent")) {
				$flanking_event_type = "reaction";
		    } elsif ($flanking_event->is_a("Pathway")) {
				$flanking_event_type = "pathway";
		    }
		    $flanking_event_name = "";
		    if ($flanking_event->attribute_value("name")) {
				$flanking_event_name = $flanking_event->attribute_value("name")->[0];
		    }
		    $flanking_event_text = "This $event_type $flanking_relation the $flanking_event_type: $flanking_event_name";
		    my $flanking_section = $instance_hash->{$flanking_event->db_id()};
		    if ($flanking_section) {
				$flanking_event_text .= " (section " . $instance_hash->{$flanking_event->db_id()} . ")";
		    }
		    $text_unit = GKB::DocumentGeneration::TextUnit->new();
		    $text_unit->set_type("body_text_paragraph");
		    $text_unit->set_contents($flanking_event_text);
		    push @text_units, $text_unit;
		} elsif (scalar(@{$flanking_events_ref}) > 1) {
		    $flanking_event_text = "This $event_type $flanking_relation:";
		    $text_unit = GKB::DocumentGeneration::TextUnit->new();
		    $text_unit->set_type("body_text_paragraph");
		    $text_unit->set_contents($flanking_event_text);
		    push @text_units, $text_unit;
		    
		    my $bullit_text;
		    foreach $flanking_event (@{$flanking_events_ref}) {
			$flanking_event_type = "event";
			if ($flanking_event->is_a("ReactionlikeEvent")) {
			    $flanking_event_type = "reaction";
			} elsif ($flanking_event->is_a("Pathway")) {
			    $flanking_event_type = "pathway";
			}
	
			$flanking_event_name = "";
			if ($flanking_event->attribute_value("name")) {
			    $flanking_event_name = $flanking_event->attribute_value("name")->[0];
			}
	
			$bullit_text = "the $flanking_event_type $flanking_event_name";
			my $flanking_section = $instance_hash->{$flanking_event->db_id()};
			if ($flanking_section) {
			    $bullit_text .= " (section " . $instance_hash->{$flanking_event->db_id()} . ")";
			}
			$text_unit = GKB::DocumentGeneration::TextUnit->new();
			$text_unit->set_type("bullit_text");
			$text_unit->set_contents($bullit_text);
			push @text_units, $text_unit;
		    }
		}
    }

    return @text_units;
}

# Given a list of literature reference instances, generate
# a single text unit for displaying the lot.
sub get_literature_refs {
    my ($self, $text_units, $refs, $title, $centred_flag) = @_;

    my $text_unit;
    if ($refs && scalar(@{$refs})>0) {
    	if (defined $title && !($title eq '')) {
			$text_unit = GKB::DocumentGeneration::TextUnit->new();
			$text_unit->set_type("section_internal_header");
			$text_unit->set_contents($title);
			push @{$text_units}, $text_unit;
    	}
		
		my $ref_string = "";
		foreach my $ref (@{$refs}) {
		    if ($ref_string) {
			    $ref_string .= ", ";
			}
		    $ref_string .= $ref->displayName();
		}
		if ($ref_string) {
		    $ref_string .= ".";
		}
		
		$text_unit = GKB::DocumentGeneration::TextUnit->new();
		if ($centred_flag) {
			$text_unit->set_type("centered_paragraph");
		} else {
			$text_unit->set_type("body_text_paragraph");
		}
		$text_unit->set_contents($ref_string);
		push @{$text_units}, $text_unit;
    }
}

# Emit a reaction description based on inputs and outputs
# Arguments:
#
# reaction - a reaction instance
sub get_reaction_description_text_units {
    my ($self, $reaction) = @_;

    unless ($reaction) {
	print STDERR "generate_reaction_description: yurks, reaction is null!!\n";
	return;
    }

    my $text = "";

    my $input = $reaction->input;
    if (scalar(@{$input}) == 1) {
	$text .= $input->[0]->name->[0] . " is converted to ";
    } elsif (scalar(@{$input}) > 1) {
	for (my $i=0; $i<scalar(@{$input}); $i++) {
	    if ($i>0) {
		$text .= " and ";
	    }
	    $text .= $input->[$i]->name->[0];
	}

	$text .= " react to form ";
    }

    my $output = $reaction->output;
    if (scalar(@{$output}) > 0) {
	for (my $i=0; $i<scalar(@{$output}); $i++) {
	    if ($i>0) {
		$text .= " and ";
	    }
	    $text .= $output->[$i]->name->[0];
	}
    }
    $text .= ".";

    my $catalystActivity = $reaction->catalystActivity;
    if (scalar(@{$catalystActivity}) > 0) {
	$text .= "  This reaction is catalysed by ";
	for (my $i=0; $i<scalar(@{$catalystActivity}); $i++) {
	    if ($i>0) {
		$text .= " and ";
	    }
	    $text .= $catalystActivity->[$i]->_displayName->[0];
	}
	$text .= ".";
    }

    my $text_unit = GKB::DocumentGeneration::TextUnit->new();
    $text_unit->set_type("body_text_paragraph");
    $text_unit->set_contents($text);
    my @text_units = ($text_unit);

    return @text_units;
}

# Emit a reaction diagram
# Arguments:
#
# reaction - a reaction instance
sub get_reaction_diagram_text_units {
    my ($self, $reaction) = @_;
    
    my @text_units = ();

    unless ($reaction) {
        print STDERR "ReactomeDatabaseReader.get_reaction_diagram_text_units: WARNING - reaction is undef!!\n";
        return @text_units;
    }

    my $gr = GKB::Graphics::ReactomeReaction->new($reaction);
    if (!(defined $gr)) {
        print STDERR "ReactomeDatabaseReader.get_reaction_diagram_text_units: WARNING - gr is undef!!\n";
        return @text_units;
    }

    # Get object of type GD::Image
    my $gd = $gr->draw;
    if (!(defined $gd)) {
        print STDERR "ReactomeDatabaseReader.get_reaction_diagram_text_units: WARNING - gd is undef!!\n";
        return @text_units;
    }

    # Last argument is 1 to force generation of PNG - this
    # tends to be available even in poorly configured installations
    # of GD.
    my $image_file_name = GKB::FileUtils->print_image($gd, $reaction->db_id() . "_reaction_diagram", 1);
    if (!(defined $image_file_name)) {
        print STDERR "ReactomeDatabaseReader.get_reaction_diagram_text_units: WARNING - cannot use image for reaction: " . $reaction->db_id() . ", skipping\n";
        return @text_units;
    }

    my $space_text_unit = GKB::DocumentGeneration::TextUnit->new();
    $space_text_unit->set_type("vertical_space");
    $space_text_unit->set_contents(2);
    push @text_units, $space_text_unit;
		    
    my $image_text_unit = GKB::DocumentGeneration::TextUnit->new();
#   $image_text_unit->set_type("image");
#   $image_text_unit->set_contents($gd);

    # Use a temporary file rather than the GD::Image,
    # because otherwise we will run out of memory when
    # dealing with large amounts of data (e.g. generating
    # the entire Reactome book).
    $image_text_unit->set_type("image_file_name");
    # The second element of "contents" is set to 1 to force
    # the deletion of this file once it has been used.
    my @contents = ($image_file_name, 1);

    $image_text_unit->set_contents(\@contents);
    push @text_units, $image_text_unit;

    return @text_units;
}

# Take for the image file associated with the given instance, and
# if there is one, emit a paragraph containing the image.
# This is mainly intended for pathways, but will work for any
# instance with an associated curator-drawn diagram.
sub get_instance_diagram_text_units {
    my ($self, $instance) = @_;
    
    my @text_units = ();

    unless ($instance) {
		print STDERR "get_instance_diagram_text_units: WARNING - instance is null!!\n";
		return @text_units;
    }

    my $image_file = GKB::Utils->find_image_file($instance);
#    my $vector_graphics_file_name = GKB::Utils->find_vector_graphics_file($instance);
    my $vector_graphics_file_name = undef; # this speeds things up

    if ((defined $image_file && !($image_file eq "")) || $vector_graphics_file_name) {
		my @contents = ($vector_graphics_file_name, $image_file);
		my $text_unit = GKB::DocumentGeneration::TextUnit->new();
		$text_unit->set_type("image_or_vector_graphics_file_name");
		$text_unit->set_contents(\@contents);
		push @text_units, $text_unit;
    } else {
#    	print STDERR "ReactomeDatabaseReader.get_instance_diagram_text_units: WARNING - no image or vector graphics file could be found for DB_ID=" . $instance->db_id() . "\n";
    }

    return @text_units;
}

1;
