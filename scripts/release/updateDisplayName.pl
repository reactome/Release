#!/usr/local/bin/perl  -w

use lib "/usr/local/gkb/modules";
use lib "$ENV{HOME}/bioperl-1.0";
use lib "$ENV{HOME}/GKB/modules";
use GKB::DBAdaptor;
use Data::Dumper;
use Getopt::Long;
use strict;

@ARGV || die "Usage: $0 -user db_user -host db_host -pass db_pass -port db_port -db db_name -class class";

our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db,$opt_debug,$opt_class);

&GetOptions("user:s",
	    "host:s",
	    "pass:s",
	    "port:i",
	    "db=s",
	    "debug",
	    "class=s",
	    );

$opt_db || die "Need database name (-db).\n";
$opt_class || die "Need class name (-class).\n";

my $dba = GKB::DBAdaptor->new
    (
     -dbname => $opt_db,
     -user   => $opt_user,
     -host   => $opt_host,
     -pass   => $opt_pass,
     -port   => $opt_port,
     -driver => 'mysql',
     -DEBUG => $opt_debug
     );

my $ar = $dba->fetch_instance(-CLASS => $opt_class);

foreach my $i (@{$ar}) {
    $i->namedInstance;
    #print $i->extended_displayName, "\n";
    $dba->update_attribute($i,'_displayName');
}

