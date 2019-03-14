#!/usr/local/bin/perl -w

use strict;

use lib '/usr/local/gkbdev/modules';

use GKB::Instance;
use GKB::DBAdaptor;
use GKB::Config_Species;
use Data::Dumper;
use Getopt::Long;

## Database connection and command line parsing:
our ($opt_user, $opt_host,  $opt_pass, $opt_port, $opt_db, $opt_debug);
(@ARGV)  || die "Usage: $0 -user db_user -host db_host -pass db_pass -port db_port -db db_name -debug\n";
&GetOptions( "user:s", "host:s", "pass:s", "port:i", "db:s", "debug" );
$opt_db  || die "Need database name (-db).\n";

my $dba = GKB::DBAdaptor->new(
    -user => $opt_user || '',
    -host => $opt_host,
    -pass   => $opt_pass,
    -port   => $opt_port,
    -dbname => $opt_db,
    -DEBUG  => $opt_debug
);

foreach my $species (@species) {
    my $name = $species_info{$species}->{'name'}->[0];
    my $sp_inst = $dba->fetch_instance_by_attribute( 'Species', [ [ '_displayName', [$name] ] ] );

    foreach my $inst (@$sp_inst) {
	$inst->inflate();
    }
    
    if ($sp_inst->[1]) {
	if ($sp_inst->[0]->superTaxon->[0]) {
	    $dba->merge_instances($sp_inst->[0], $sp_inst->[1]);
	} else {
	    $dba->merge_instances($sp_inst->[1], $sp_inst->[0]);
	}
    }
}

 
  