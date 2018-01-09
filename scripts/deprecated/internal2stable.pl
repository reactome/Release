#!/usr/local/bin/perl  -w

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

my $dba = GKB::DBAdaptor->new
    (
     -user   => $opt_user || '',
     -host   => $opt_host,
     -pass   => $opt_pass,
     -port   => $opt_port,
     -dbname => $opt_db,
     -DEBUG => $opt_debug
     );


# If creation of a filehandle is unsuccessful, the following error message
# prints and the program terminates.
my $outfile = "/usr/local/gkbdev/scripts/release/internal2stable.txt"; # Output file for submission to GO

# If creation of a filehandle is unsuccessful, the following error message
# prints and the program terminates.
if (!open(FILE, ">$outfile")) {
	print STDERR "$0: could not open file $outfile\n";
	exit(1);
}


my $ar = $dba->fetch_instance(-CLASS => 'Event'); # Obtains a reference to the array of all Reactome events

# Each event in Reactome is processed
foreach my $ev (@{$ar}) {
    print FILE $ev->db_id . "\t" . $ev->StableIdentifier->[0]->Identifier->[0] . "\n";
}