#!/usr/local/bin/perl -w

#This script should be run over a release database as it requires stable identifiers to be present
#This script produces a tab delimited file for submission to goa - including Reactome annotations for cellular components, molecular function and biological process.

#NOTE: after running this script, run goa_submission_stats.pl to produce stats

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

(@ARGV) || die "Usage: $0 -user db_user -host db_host -pass db_pass -port db_port -db db_name -date date(YYYYMMDD) -debug\n";

&GetOptions("user:s", "host:s", "pass:s", "port:i", "db:s", "date:i", "debug");

$opt_db || die "Need database name (-db).\n";
#$opt_date || die "Need date (-date).\n";  #need to revisit this, at present some instances don't have InstanceEdits attached, this should be fixed

my $dba= GKB::DBAdaptor->new
    (
     -user   => $opt_user || '',
     -host   => $opt_host,
     -pass   => $opt_pass,
     -port   => $opt_port,
     -dbname => $opt_db,
     -DEBUG => $opt_debug
     );

$opt_db =~ /(\d+)$/;
my $outfile = "Reactome2GoV" . $1;

open(FILE, ">$outfile") or die "$0: could not open file $outfile";

my $ar = $dba->fetch_instance(-CLASS => 'Event'); # Obtains a reference to the array of all Reactome events

# Each event in Reactome is processed
foreach my $ev (@{$ar}) {
    print "$0: ev->db_id=" . $ev->db_id() . "\n";
    Reactome2GO($ev);
}

close(FILE); # The output file has all entries now and is closed

print "$0 has finished its job\n";



sub Reactome2GO {
	my $event = shift;
	my @go;
	push @go, $event->GoBiologicalProcess->[0];
	
	foreach my $cat (@{$event->CatalystActivity}) {
		push @go, $cat->Activity->[0];	
	}
	
	return unless $event->stableIdentifier->[0];
	my $eventid = $event->stableIdentifier->[0]->identifier->[0];
	
	my $eventdisplayname = $event->_displayName->[0];
	$eventdisplayname =~ s/\s+/ /g;
	$eventdisplayname = trim($eventdisplayname);
	
	my $eventspecies = $event->species->[0]->name->[0];
	
	foreach my $go (@go) {
		next unless $go;
		my $goid;
		my $goname;
		if ($go->accession) {
			$goid = $go->accession->[0];
		}
		if ($go->_displayName) {
			$goname = $go->_displayName->[0];
		}
	
		print FILE "Reactome:$eventid $eventdisplayname, $eventspecies > GO:$goname ; GO:$goid \n";
	}
}

# Trims white space from beginning and end of string
sub trim {
	my $name = shift;
	$name =~ s/^ +//;
	$name =~ s/ +$//;
	return $name;
}
