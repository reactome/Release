#!/usr/local/bin/perl -w
use strict;

#This script should be run over a release database

# Created by: Joel Weiser (joel.weiser@oicr.on.ca)
# Created on: April 6th, 2015

use lib "/usr/local/gkb/modules";

use GKB::Instance;
use GKB::DBAdaptor;
use GKB::Utils;

use autodie;
use Data::Dumper;
use Getopt::Long;

# Database connection
our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db, $opt_date, $opt_debug);

(@ARGV) || die "Usage: $0 -user db_user -host db_host -pass db_pass -port db_port -db db_name -debug";

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
		
my $outfile = "events_2_NCBIGenes.txt"; # Output file for website
		
open(my $file, ">", $outfile);
			
print $file "Pathway db id\tPathway name\tNCBI Gene id\tSpecies\n";
			
my $ar = $dba->fetch_instance(-CLASS => 'Pathway'); # Obtains a reference to the array of all Reactome events
# Every pathway/reaction in reactome is processed
foreach my $cls (@{$ar}) {
	my $entities = find_ewas($cls);
	
	# Each top-level pathway for the current pathway is processed
	my $name = $cls->Name->[0]; # Obtains the top-level pathway name
	#my $stableid = $cls->stableIdentifier->[0]->identifier->[0]; # Obtains the top-level pathway stable id
	my $db_id = $cls->db_id;
	my $species = $cls->species->[0]->name->[0];
	
	# Each entity for the current pathway is processed and linked with the current top-level pathway
	ENTITY:foreach my $entity (@{$entities}) {
		my $references = $entity->referenceEntity;
		next unless $entity->referenceEntity->[0];
		
		my @gene_ids;
		foreach my $reference (@{$references}){			
			if ($reference->referenceGene) {
				foreach (@{$reference->referenceGene}) {
					if ($_->_displayName->[0] =~ /^NCBI Gene/i) {
						push @gene_ids, $_->identifier->[0];
					}
				}
			}
			foreach my $identifier (@{$reference->otherIdentifier}, @{$reference->identifier}) {
				push @gene_ids, $1 if $identifier =~ /^EntrezGene:(\d+)/;
			}
			
			foreach my $gene_id (@gene_ids) {
				my $row = "$db_id\t$name\t$gene_id\t$species\n"; # Annotation assembled here
				next if $seen{$row}++; # Duplicates weeded out
				print $file $row; # Unique annotation added to file output
			}
		}
	}
}	
close $file;

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