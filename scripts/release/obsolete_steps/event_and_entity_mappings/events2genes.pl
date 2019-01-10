#!/usr/local/bin/perl  -w

#This script should be run over a release database as it requires stable identifiers to be present

# Created by: Joel Weiser (joel.weiser@oicr.on.ca)
# Created on: July 4, 2011
# Last modified: October 31, 2011
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

# Database connection
our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db, $opt_date, $opt_debug);

(@ARGV) || die "Usage: $0 class -user db_user -host db_host -pass db_pass -port db_port -db db_name -debug (Note: class can be reaction or pathway)";

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

my $class = ucfirst $ARGV[-1];

my $outfile = "../website/html/download/current/$class\_2_genes.txt"; # Output file for website

# If creation of a filehandle is unsuccessful, the following error message
# prints and the program terminates.
if (!open(FILE, ">$outfile")) {
	print STDERR "$0: could not open file $outfile\n";
	exit(1);
}

my $ar = $dba->fetch_instance(-CLASS => $class); # Obtains a reference to the array of all Reactome pathways/reactions
my %processes;

# Every pathway/reaction in reactome is processed
foreach my $cls (@{$ar}) {
	my $toppaths = get_top_level_events($cls); # Obtains the top-level pathway for the current pathway

	my $entities = find_ewas($cls);

	# Each top-level pathway for the current pathway is processed
	my $name = $cls->Name->[0]; # Obtains the top-level pathway name
	my $db_id = $cls->db_id;
	my $stableid = $cls->stableIdentifier->[0]->identifier->[0]; # Obtains the top-level pathway stable id
	my $url = "http://www.reactome.org/content/detail/$stableid"; # Obtains the url to the pathway
	next if (!$cls->reverse_attribute_value('hasEvent')->[0]) && $class eq "Reaction";
	my $pathwayid = $cls->reverse_attribute_value('hasEvent')->[0]->stableIdentifier->[0]->identifier->[0] . "\t" if $class eq "Reaction";

	# Each entity for the current pathway is processed and linked with the current top-level pathway
	ENTITY:foreach my $entity (@{$entities}) {
		my $protein_name = $entity->Name->[0];
		my $protein_db_id = $entity->referenceEntity->[0]->db_id;
		my $protein_st_id = "";
		$protein_st_id = $entity->stableIdentifier->[0]->identifier->[0] if $entity->stableIdentifier->[0];
		my $protein_species = $entity->species->[0]->name->[0];
		my $references = $entity->referenceEntity;
		next unless $entity->referenceEntity->[0];

		my $gene_id = "";
		my $gene_name = "";
		my $protein_id = "";
		foreach my $reference (@{$references}){
			my $display_name = $reference->_displayName->[0];
			$reference->description->[0] =~
/recommendedName. (.+?) (alternativeName.+)?shortName/;
			my $long_name = $1;
			$protein_name = $long_name if $long_name;
			$gene_name = $reference->geneName->[0] if $reference->geneName;
			$gene_name = $reference->name->[0] if $reference->name && !$gene_name;
			if ($reference->referenceGene) {
				foreach (@{$reference->referenceGene}) {
					if ($_->_displayName->[0] =~ /^ENSEMBL:/i) {
						$gene_id = $_->identifier->[0];
					}
				}
			}
			foreach my $identifier (@{$reference->otherIdentifier}, @{$reference->identifier}) {
				$gene_id = $identifier if $identifier =~ /^ENS[A-Z]*G\d+/ && !$gene_id;
				$protein_id = $identifier if $identifier =~ /^ENS[A-Z]*P\d+/;
				last if $gene_id && $protein_id;
			}
			$protein_id = $reference->identifier->[0] if $display_name =~ /^ENSEMBL/i && !$protein_id;
			$gene_name = $gene_id unless $gene_name;
			my $row = "$stableid\t$db_id\t$name\t$pathwayid$gene_id\t$gene_name\t$protein_id\t$protein_name\t$protein_db_id\t$protein_st_id\t$protein_species\n"; # Annotation assembled here
			next if $seen{$row}++; # Duplicates weeded out
			print FILE $row; # Unique annotation added to file output
		}
	}
}
close FILE;

sub find_ewas {
    my ($ev) = @_;
#this ignores candidates in CandidateSets - may need to revisit
    my $ewas_ar = $ev->follow_class_attributes(-INSTRUCTIONS =>
					      {'Pathway' => {'attributes' => [qw(hasEvent)]},
					       'ReactionlikeEvent' => {'attributes' => [qw(input output catalystActivity)]},
					       'CatalystActivity' => {'attributes' => [qw(physicalEntity)]},
					       'Complex' => {'attributes' => [qw(hasComponent)]},
					       'EntitySet' => {'attributes' => [qw(hasMember)]},
					       'Polymer' => {'attributes' => [qw(repeatedUnit)]}},
					       -OUT_CLASSES => [('EntityWithAccessionedSequence')]);
    return $ewas_ar;
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
