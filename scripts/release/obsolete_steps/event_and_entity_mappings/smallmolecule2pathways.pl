#!/usr/local/bin/perl  -w

#This script should be run over a release database as it requires stable identifiers to be present

# Created by: Joel Weiser (joel.weiser@@oicr.on.ca)
# Created on: July 4, 2011
# Last modified: July 5, 2011
# Purpose:  This script runs through each pathway in reactome and for each simple entity links it to
#	    its top-level pathway.

# Make sure you don't have "competing" libraries...
# for use @@CSHL
use lib "/usr/local/gkb/modules";
# for use @@HOME
use lib "$ENV{HOME}/bioperl-1.0";
use lib "$ENV{HOME}/GKB/modules";
use GKB::Instance;
use GKB::DBAdaptor;
use GKB::Utils;
use Data::Dumper;
use Getopt::Long;
use strict;


# Database connection
our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db, $opt_date, $opt_debug);

(@ARGV) || die "Usage: $0 -user db_user -host db_host -pass db_pass -port db_port -db db_name -debug\n";

&GetOptions("user:s", "host:s", "pass:s", "port:i", "db:s", "date:i", "debug");

$opt_db || die "Need database name (-db).\n";
#$opt_date || die "Need date (-date).\n";  #need to revisit this, at present some instances don't have InstanceEdits attached, this should be fixed

my $dba = GKB::DBAdaptor->new
    (
     -user   => $opt_user || '',
     -host   => $opt_host,
     -pass   => $opt_pass,
     -port   => $opt_port,
     -dbname => $opt_db,
     -DEBUG => $opt_debug
     );



my @type = ("PubChem Substance", "COMPOUND", "ChEBI");
my $directory = "/usr/local/gkbdev/website/html/download/current";

foreach my $type (@type) {
	my %seen; # Prevents duplication in the file

	my $outfile = "$directory/" . lc $type . "2pathways.stid.txt"; # Output file for website
	my $outfile2 = "$directory/" . lc $type . "2pathways.txt";
	my $outfile3 = "$directory/curated_and_inferred_". lc $type . "2pathways.txt";
	
	$outfile =~ s/ //;
	$outfile2 =~ s/ //;
	$outfile3 =~ s/ //;
	
	# If creation of a filehandle is unsuccessful, the following error message
	# prints and the program terminates.
	open(FILE, ">$outfile") or die "$0: could not open file $outfile\n";
	open(FILE2, ">$outfile2") or die "$0: could not open file $outfile2\n";
	open(FILE3, ">$outfile3") or die "$0: could not open file $outfile3\n";
	
	my $ar = $dba->fetch_instance(-CLASS => 'Pathway'); # Obtains a reference to the array of all Reactome pathways
	my %processes;
	
	# Every pathway in reactome is processed
	foreach my $pathway (@{$ar}) {
		my $toppaths = get_top_level_events($pathway); # Obtains the top-level pathway for the current pathway
		my $se_ar = find_SimpleEntities($pathway); # Obtains all simple entities associated with the current pathway
	
		# Each top-level pathway for the current pathway is processed
		foreach my $top (@{$toppaths}) {
			my $human;
			if ($top->species->[0]->name->[0] eq "Homo sapiens") {
				$human = 1;
			} else {
				$human = 0;	
			}
			
			#next unless $top->species->[0]->name->[0] eq "Homo sapiens";
			my $name = $top->Name->[0]; # Obtains the top-level pathway name
			my $stableid = $top->stableIdentifier->[0]->identifier->[0]; # Obtains the top-level pathway stable id
			my $url = "http://www.reactome.org/cgi-bin/eventbrowser_st_id?ST_ID=$stableid"; # Obtains the url to the top-level pathway
			
			# Each simple entity for the current pathway is processed and linked with the current top-level pathway
			foreach my $entity (@{$se_ar}) {
				next unless $entity->referenceEntity->[0]; # The simple entity must have a reference entity (i.e. to ChEBI)

				my $references;
				if ($type eq "ChEBI") {
					$references = $entity->referenceEntity; # Obtains the ChEBI display name
				} else {
					next unless $entity->referenceEntity->[0]->crossReference->[0]; # Obtains the ChEBI display name
					$references = $entity->referenceEntity->[0]->crossReference;
				}
				foreach (@{$references}) {
					my $display = $_->_displayName->[0];
					$display =~ /($type:[A-Za-z0-9]+)/; # Obtains only the identifier portion of the display name
					$display = $1; 
					next unless $display; # Entity skipped if there is no ChEBI id
					
					my $row = "$display\t$stableid\t$name\t$url\n"; # Annotation assembled here
					next if $seen{$row}++ || !$name; # Duplicates and no name pathways weeded out
					push @{$processes{$display}}, "$name\t$human";
					print FILE $row if $human; # Unique annotation added to file output
				}
			}	
		}
	}
	
	foreach (keys %processes) {
		my %pathway;
		my ($identifier) = $_; # Chebi Identifier
		my $num = scalar @{$processes{$_}}; # Number of pathways
		
		#Annotation assembled in $row
		$type = lcfirst $type if $type eq "ChEBI";
		my $row = "$identifier\t$type:$identifier\t";
		my $nonhuman;
		if ($num > 1) {
			$row .= "[$num processes]:";
		}
		
		my ($name, $human);
		my $humancount = 0;
		my $nonhumancount = 0;
		foreach my $variables (@{$processes{$_}}) {
			($name, $human) = split /\t/, $variables;
			next if $pathway{$name}++;
			if ($human) {
				$row .= "$name;";
				$humancount++;
			} else {
				$nonhuman .= "$name;";
				$nonhumancount++;
			}
		}
		
		my $humanrow = $row;
		my $nonhumanrow = $row;
		$humanrow =~ s/;$//;
		$nonhumanrow =~ s/;$//;

		$nonhuman =~ s/;$//;
		
		$humanrow =~ s/$num processes/$humancount processes/;
		$humanrow =~ s/\[1 processes\]://;
		
		my $url = "\thttp://www.reactome.org/cgi-bin/link?SOURCE=$type&ID=$identifier\n";
		$humanrow .= $url;
		next if $seen{$humanrow}++; # Duplicates weeded out
		print FILE2 $humanrow; # Unique annotation added to file output

		$nonhumanrow .= $nonhuman . $url;
		my $nonhumannum = $humancount + $nonhumancount;
		$nonhumanrow =~ s/$num processes/$nonhumannum processes/;
		$nonhumanrow =~ s/\[1 processes\]://;
		print FILE3 $nonhumanrow;
	}
	print "$type is finished\n";
}

sub find_SimpleEntities {
    my ($ev) = @_;
    
    #this ignores candidates in CandidateSets - may need to revisit
    my $se_ar = $ev->follow_class_attributes(-INSTRUCTIONS =>
					      {'Pathway' => {'attributes' => [qw(hasEvent)]},
					       'ReactionlikeEvent' => {'attributes' => [qw(input output catalystActivity)]},
					       'CatalystActivity' => {'attributes' => [qw(physicalEntity)]},
					       'Complex' => {'attributes' => [qw(hasComponent)]},
					       'EntitySet' => {'attributes' => [qw(hasMember)]},
					       'Polymer' => {'attributes' => [qw(repeatedUnit)]},
					       },
					      -OUT_CLASSES => [('SimpleEntity')]);
    return $se_ar;
}

sub get_top_level_events {
    my $event = shift;
    return top_events($event->follow_class_attributes(-INSTRUCTIONS => {'Event' => {'reverse_attributes' =>[qw(hasEvent)]}},
						      -OUT_CLASSES => ['Event']));
}

sub top_events {
    my ($events) = @_;
    my @out;
    foreach my $e (@{$events}) {
	@{$e->reverse_attribute_value('hasEvent')} && next; # If the event has a higher level event, it is not a top-level event and is skipped
#	@{$e->reverse_attribute_value('hasMember')} && next;
	push @out, $e; # All top-level events collected here
    }
    # Filter out reactions
    @out = grep {! $_->is_a('Reaction')} @out; 
    return \@out; # Returns top-level pathways
}
