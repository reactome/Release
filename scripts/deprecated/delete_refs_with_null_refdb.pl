#!/usr/local/bin/perl

use strict;
use lib "/usr/local/gkb/modules";
use GKB::Instance;
use GKB::DBAdaptor;
use GKB::Utils;
use Data::Dumper;
use Getopt::Long;


# This script should delete all ReferenceDNASequences (and ancestor records) where
# the referenceDatabase is null.

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
my $outfile = "DeleteRefsWithNULLRefDB" . $1;

open(FILE, ">$outfile") or die "$0: could not open file $outfile";

my $ar = $dba->fetch_instance(-CLASS => 'ReferenceDNASequence'); # Obtains a reference to the array of all Reactome events

# Each Ref DNA Seq in Reactome is processed
foreach my $ref (@{$ar}) {
    print "$0 checking: ref->db_id=" . $ref->db_id. "\n";
    DeleteRefs($ref);
}

close(FILE); # The output file has all entries now and is closed

print "$0 has finished its job\n";


sub DeleteRefs
{
    my $ref = shift;
    if (! $ref->referenceDatabase->[0])
    {
        print("deleting: ".$ref->db_id." \n");
        $dba->delete_by_db_id($ref->db_id);
    }
}
