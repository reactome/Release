#!/usr/local/bin/perl -w

use lib "/usr/local/gkbdev/modules";

use GKB::ClipsAdaptor;
use GKB::Ontology;
use GKB::DBAdaptor;
use GKB::Utils;
use Data::Dumper;
use Getopt::Long;
use strict;

our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db,$opt_debug);

(@ARGV) || die "Usage: $0 -user db_user -host db_host -pass db_pass -port db_port -db db_name -debug\n";

my @params = @ARGV;

&GetOptions("user:s", "host:s", "pass:s", "port:i", "db:s", "debug");

$opt_db || die "Need database name (-db).\n";    

print STDERR "$0: about to tweak_datamodel.pl\n";
my $cmd = qq(./tweak_datamodel.pl @params);
print "$cmd\n";
system($cmd) == 0 or die "$cmd failed.";

print STDERR "$0: about to indirectIdentifiers_for_all_species.pl\n";
$cmd = qq(./indirectIdentifiers_for_all_species.pl @params);
print "$cmd\n";
system($cmd) == 0 or die "$cmd failed.";

print STDERR "$0: about to createDatabase.pl\n";
my $skypainter_db = $opt_db . '_dn';
$cmd = qq(./createDatabase.pl model.pprj -host $opt_host -port $opt_port -pass $opt_pass -db $skypainter_db);
print "$cmd\n";
system($cmd) == 0 or die "$cmd failed.";

print STDERR "$0: about to create_denormalised_identifier_db.pl\n";
$cmd = qq(./create_denormalised_identifier_db.pl -from_host $opt_host -from_port $opt_port -from_pass $opt_pass -from_db $opt_db -to_host $opt_host -to_port $opt_port -to_pass $opt_pass -to_db $skypainter_db);
print "$cmd\n";
system($cmd) == 0 or die "$cmd failed.";

# Precompute the background p-value distributions for some species (as specified the the script).
# This way we can estimate the FDR.
# Do 1000 iterations and use range 2 .. 100, i.e. we can estimate the FDR for effective set size
# of 2 .. 100. It would ne nice to be able to use any list length but the longer the list the
# longer it takes to compute the background. Please note that even with these parameters  this step
# can take a day to complete.

print STDERR "$0: about to compute_and_store_background_p_value_distribution_4_some_species.pl\n";
#$cmd = qq(./compute_and_store_background_p_value_distribution_4_some_species.pl -host $opt_host -port $opt_port -pass $opt_pass -db $skypainter_db -start 2 -stop 100 -iter 1000);
#print "$cmd\n";
#system($cmd) == 0 or die "$cmd failed.";

print STDERR "$0 has finished its job\n";

