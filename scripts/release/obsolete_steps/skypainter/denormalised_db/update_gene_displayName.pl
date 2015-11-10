#!/usr/local/bin/perl -w

use lib "$ENV{HOME}/GKB/modules";
use lib "$ENV{HOME}/bioperl-1.0";
use lib "$ENV{HOME}/my_perl_stuff";

use GKB::ClipsAdaptor;
use GKB::Instance;
use GKB::DBAdaptor;
use GKB::Config;
use Getopt::Long;
use strict;

$GKB::Config::NO_SCHEMA_VALIDITY_CHECK = undef;

our($opt_debug,
    $opt_from_db,$opt_from_user,$opt_from_pass,$opt_from_host,$opt_from_port,
    $opt_to_db,$opt_to_user,$opt_to_pass,$opt_to_host,$opt_to_port,
    $opt_sp
    );

&GetOptions("debug",
	    "from_db:s",
	    "from_user:s",
	    "from_pass:s",
	    "from_host:s",
	    "from_port:i",
	    "to_db:s",
	    "to_user:s",
	    "to_pass:s",
	    "to_host:s",
	    "to_port:i",
	    "sp:s"
);

($opt_to_db && $opt_from_db) ||die "Usage: $0 -from_db db1 -to_db db2\n";

my ($dba_from,$dba_to,%participant_cache,%species_cache);

$dba_from = GKB::DBAdaptor->new
    (
     -user   => $opt_from_user,
     -host   => $opt_from_host,
     -pass   => $opt_from_pass,
     -port   => $opt_from_port,
     -dbname => $opt_from_db,
     -DEBUG => $opt_debug
     );

$dba_to = GKB::DBAdaptor->new
    (
     -user   => $opt_to_user,
     -host   => $opt_to_host,
     -pass   => $opt_to_pass,
     -port   => $opt_to_port,
     -dbname => $opt_to_db,
     -DEBUG => $opt_debug
     );

my $ar;
if ($opt_sp) {
    $ar = $dba_to->fetch_instance_by_remote_attribute('Gene',[['species._displayName','=',[$opt_sp]]]);
} else {
    $ar = $dba_to->fetch_all_class_instances_as_shells('Gene');
}
$dba_to->load_class_attribute_values_of_multiple_instances('Gene','db_id_in_main_db',$ar);
foreach my $g (@{$ar}) {
    if(my $i = $dba_from->fetch_instance_by_attribute('ReferenceSequence',
						   [['DB_ID',$g->Db_id_in_main_db],
						    ['geneName',[],'IS NOT NULL']]
       )->[0]) {
	$g->displayName($i->GeneName->[0]);
	print $g->extended_displayName, "\n";
	$dba_to->update_attribute($g,'_displayName');
    }
}

print STDERR "$0: no fatal errors.\n";

