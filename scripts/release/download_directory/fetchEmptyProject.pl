#!/usr/local/bin/perl

# Make sure you don't have "competing" libraries...
# for use @CSHL
use lib "/usr/local/gkb/modules";
# for use @HOME
use lib "$ENV{HOME}/bioperl-1.0";
use lib "$ENV{HOME}/GKB/modules";

use GKB::DBAdaptor;
use GKB::Utils;
use Data::Dumper;
use Getopt::Long;
use strict;

our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db,$opt_outputdir,$opt_debug);

(@ARGV) || die "Usage: $0 basename -user db_user -host db_host -pass db_pass -port db_port -db db_name -outputdir output_directory -debug\n";

&GetOptions("user:s", "host:s", "pass:s", "port:i", "db:s", "outputdir:s", "debug");

$opt_db || die "Need database name (-db).\n";
$opt_outputdir ||= '.';

my $basename = $ARGV[0] || die "Need basename.\n";

my $dba = GKB::DBAdaptor->new
    (
     -user   => $opt_user,
     -host   => $opt_host,
     -pass   => $opt_pass,
     -port   => $opt_port,
     -dbname => $opt_db,
     -DEBUG => $opt_debug
     );

open (OUT, "> $opt_outputdir/$basename.pprj") || die "Could not create $basename.pprj: $!\n";
(my $pprj_fc = $dba->ontology->pprj_file_content) =~ s/\w+\.pins/$basename\.pins/gms;
$pprj_fc =~ s/\w+\.pont/$basename\.pont/gms;
print OUT $pprj_fc, "\n";
close OUT;

open (OUT, "> $opt_outputdir/$basename.pont") || die "Could not create $basename.pont: $!\n";
print OUT $dba->ontology->pont_file_content, "\n";
close OUT;

open (OUT, "> $opt_outputdir/$basename.pins") || die "Could not create $basename.pins: $!\n";
print OUT $dba->ontology->pins_file_stub, "\n";
close OUT;
