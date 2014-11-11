#!/usr/local/bin/perl  -w

#This script should be run over a release database as it requires stable identifiers to be present

# Created by: Joel Weiser (joel.weiser@oicr.on.ca)
# Created on: April 24, 2014
# Purpose:  This script runs through each pathway in reactome and prints a mapping of stable id
#	    to summation text

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
		
my $class = "pathway";

my $outfile = "$class\_2_summation.txt"; # Output file for website
		
# If creation of a filehandle is unsuccessful, the following error message
# prints and the program terminates.
if (!open(FILE, ">$outfile")) {
	print STDERR "$0: could not open file $outfile\n";
	exit(1);
}
			
my $ar = $dba->fetch_instance(-CLASS => $class); # Obtains a reference to the array of all Reactome pathways
my %processes;

# Every pathway in reactome is processed
foreach my $cls (@{$ar}) {
	my $human;
	foreach my $species (@{$cls->species}) {
		$human = 1 if $species->displayName =~ /Homo sapiens/;
	}
	next unless $human;
	
	# Each top-level pathway for the current pathway is processed
	my $name = $cls->Name->[0]; # Obtains the top-level pathway name
	my $stableid = $cls->stableIdentifier->[0]->identifier->[0]; # Obtains the top-level pathway stable id
	
	# Each summation for the current pathway is 
	foreach my $summation_instance (@{$cls->summation}) {
		my $summation = $summation_instance->text->[0];
		$summation =~ s/\s+/ /g;
		
		my $row = "$stableid\t$name\t$summation\n"; # Annotation assembled here
		next if $seen{$row}++; # Duplicates weeded out
		print FILE $row; # Unique annotation added to file output	
	}	
	
}	
close FILE;
