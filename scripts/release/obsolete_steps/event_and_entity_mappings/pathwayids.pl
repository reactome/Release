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
		
my $outfile = "pathwayids.txt"; # Output file for website
		
open(my $file, ">", $outfile);

print $file "Pathway id\tSpecies\tPathway name\n";
			
my $ar = $dba->fetch_instance(-CLASS => 'Pathway'); # Obtains a reference to the array of all Reactome events

# Every pathway/reaction in reactome is processed
foreach my $cls (@{$ar}) {
	# Each top-level pathway for the current pathway is processed
	my $name = $cls->Name->[0]; # Obtains the top-level pathway name
	#my $stableid = $cls->stableIdentifier->[0]->identifier->[0]; # Obtains the top-level pathway stable id
	my $db_id = $cls->db_id;
	my $species = $cls->species->[0]->name->[0];
	
	my $line = "$db_id\t$species\t$name\n";
	next if $seen{$line}++;
	
	print $file $line;
}	
close $file;