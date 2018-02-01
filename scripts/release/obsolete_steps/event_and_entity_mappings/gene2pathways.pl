#!/usr/local/bin/perl  -w

#This script should be run over a release database as it requires stable identifiers to be present

# Created by: Joel Weiser (joel.weiser@oicr.on.ca)
# Created on: July 4, 2011
# Last modified: July 5, 2011
# Purpose:  This script runs through each pathway in reactome and for each simple entity links it to
#	    its top-level pathway.

# Make sure you don't have "competing" libraries...
# for use @CSHL
use lib "/usr/local/gkb/modules";
# for use @HOME
use lib "$ENV{HOME}/bioperl-1.0";
use lib "$ENV{HOME}/GKB/modules";
use GKB::Instance;
use GKB::DBAdaptor;
use GKB::Utils;
use Data::Dumper;
use Getopt::Long;
use strict;

my $type;
do {
	print "Choose:\n";
	print "Ensembl (1)\n";
	print "Entrez-Gene (2)\n";
	print "Kegg Gene (3)\n";
	$type = <STDIN>;
} while($type != 1 && $type != 2 && $type != 3);
	
my $outfile;
my $outfile2;
my $outfile3;

if ($type == 1) {
	$type = "Ensembl";
} elsif ($type == 2) {
	$type = "Entrez-Gene";
} elsif ($type == 3) {
	$type = "Kegg Gene";
}

$outfile = "release/$type"."2pathways.stdid.txt"; # Output file for website
$outfile2 = "release/$type"."2pathways.txt";
$outfile3 = "release/curated_and_inferred_$type"."2pathways.txt";

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



my %seen; # Prevents duplication in the file

# If creation of a filehandle is unsuccessful, the following error message
# prints and the program terminates.
if (!open(FILE, ">$outfile")) {
	print STDERR "$0: could not open file $outfile\n";
	exit(1);
}

if(!open(FILE2, ">$outfile2")) {
	print STDERR "$0: could not open file $outfile2\n";
}

if(!open(FILE3, ">$outfile3")) {
	print STDERR "$0: could not open file $outfile3\n";
}

my $ar = $dba->fetch_instance(-CLASS => 'Pathway'); # Obtains a reference to the array of all Reactome pathways
my %processes;

# Every pathway in reactome is processed
foreach my $pathway (@{$ar}) {
	my $toppaths = get_top_level_events($pathway); # Obtains the top-level pathway for the current pathway
	my $rps_ar = find_rps($pathway); # Obtains all simple entities associated with the current pathway

	# Each top-level pathway for the current pathway is processed
	foreach my $top (@{$toppaths}) {
		my $human;
		if ($top->species->[0]->name->[0] eq "Homo sapiens") {
			$human = 1;
		} else {
			$human = 0;
		}
		my $name = $top->Name->[0]; # Obtains the top-level pathway name
		my $stableid = $top->stableIdentifier->[0]->identifier->[0]; # Obtains the top-level pathway stable id
		my $url = "http://www.reactome.org/cgi-bin/eventbrowser_st_id?ST_ID=$stableid"; # Obtains the url to the top-level pathway
		
		# Each simple entity for the current pathway is processed and linked with the current top-level pathway
		foreach my $entity (@{$rps_ar}) {
			my @refgenes = @{$entity->referenceGene}; # The simple entity must have a reference entity (i.e. to ChEBI)
			next unless @refgenes;
			my $identifier;
			my $displayname;
			foreach my $reference (@refgenes) {
				$identifier = $reference->identifier->[0];
				$displayname = $reference->_displayName->[0];
				next unless $identifier; # Entity skipped if there is no ChEBI id			
				last if $identifier =~ /ENSG\d+/ && $type eq "Ensembl";
				last if $displayname =~ /Entrez Gene/ && $type eq "Entrez-Gene";
				last if $displayname =~ /KEGG Gene/ && $type eq "Kegg Gene";
			}
			next if $identifier !~ /ENSG\d+/ && $type eq "Ensembl";
			next if $displayname !~ /Entrez Gene/ && $type eq "Entrez-Gene";
			next if $displayname !~ /KEGG Gene/ && $type eq "Kegg Gene";
			my $row = "$identifier\t$stableid\t$name\t$url\n"; # Annotation assembled here
			next if $seen{$row}++; # Duplicates weeded out
			push @{$processes{$identifier."\t".$displayname}}, $name.$human; 
			print FILE $row if $human; # Unique annotation added to file output
		}	
	}
}

foreach (keys %processes) {
	my $nonhuman = "";
	my ($identifier, $displayname) = split /\t/, $_;
	next if $identifier !~ /ENSG\d+/ && $type eq "Ensembl";
	next if $displayname !~ /Entrez Gene/ && $type eq "Entrez-Gene";
	next if $displayname !~ /KEGG Gene/ && $type eq "Kegg Gene";
	my $num = scalar @{$processes{$_}};
	my $row = "$identifier\t$type:$identifier\t";
	if ($num > 1) {
		$row .= "[$num processes]:";
	} 
	foreach my $name (@{$processes{$_}}) {
		$name =~ s/(\d)$//;
		my $human = $1;
		unless ($human) {
			$nonhuman .= "$name;";
		} else {
			$row .= "$name;";
		}
	}
	$nonhuman =~ s/;$//;
	$row =~ s/;$//;
	
	my $url = "http://www.reactome.org/cgi-bin/link?SOURCE=$type&ID=$identifier";
	$row .= "\t$url\n"; # Annotation assembled here
	next if $seen{$row}++; # Duplicates weeded out
	print FILE2 $row; # Unique annotation added to file output
	$row =~ s/($url)/\t$nonhuman$1/;
	print FILE3 $row;
}

sub find_rps {
    my $protein_class = &GKB::Utils::get_reference_protein_class($dba);
    my ($ev) = @_;
#this ignores candidates in CandidateSets - may need to revisit
    my $rps_ar = $ev->follow_class_attributes(-INSTRUCTIONS =>
					      {'Pathway' => {'attributes' => [qw(hasEvent)]},
					       'ReactionlikeEvent' => {'attributes' => [qw(input output catalystActivity)]},
					       'CatalystActivity' => {'attributes' => [qw(physicalEntity)]},
					       'Complex' => {'attributes' => [qw(hasComponent)]},
					       'EntitySet' => {'attributes' => [qw(hasMember)]},
					       'Polymer' => {'attributes' => [qw(repeatedUnit)]},
					       'EntityWithAccessionedSequence' => {'attributes' => [qw(referenceEntity)]}},
					      -OUT_CLASSES => [($protein_class)]);
    return $rps_ar;
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