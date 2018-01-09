#!/usr/local/bin/perl  -w

use lib "$ENV{HOME}/bioperl-1.0";
use lib "$ENV{HOME}/GKB/modules";
use lib "$ENV{HOME}/my_perl_stuff";
use GKB::DBAdaptor;
use Data::Dumper;
use Getopt::Long;
use strict;

@ARGV || die "Usage: $0 -user db_user -host db_host -pass db_pass -port db_port -db db_name";

our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db,$opt_debug);

&GetOptions("user:s",
	    "host:s",
	    "pass:s",
	    "port:i",
	    "db=s",
	    "debug",
	    );

$opt_db || die "Need database name (-db).\n";

my $dba = GKB::DBAdaptor->new
    (
     -dbname => $opt_db,
     -user   => $opt_user,
     -host   => $opt_host,
     -pass   => $opt_pass,
     -port   => $opt_port,
     -DEBUG => $opt_debug
     );

my $hs_db_id = $dba->fetch_instance_by_attribute('Species', [['name',['Homo sapiens']]])->[0]->db_id;

my $ar = $dba->fetch_instance_by_remote_attribute
    (
     'Event',
     [
      ['species', '=', [$hs_db_id]],
      ['inferredFrom', 'IS NOT NULL', []],
      ['inferredFrom.species', '!=', [$hs_db_id]]
      ]
     );

my %update;

# Have to load attribute values before adding to them
foreach my $e (@{$ar}) {
    $e->load_attribute_values('orthologousEvent');
    foreach my $inferredFrom (@{$e->InferredFrom}) {
	$inferredFrom->load_attribute_values('orthologousEvent');
    }
}

foreach my $e (@{$ar}) {
#    print $e->extended_displayName, "\n";
    my $update_flag;
    foreach my $inferredFrom (@{$e->InferredFrom}) {
	next if ($e->Species->[0] == $inferredFrom->Species->[0]);
	my $added1 = $e->add_attribute_value_if_necessary('orthologousEvent',$inferredFrom);
	if (@{$added1}) {
	    $update{$e->db_id} = $e;
	}
	my $added2 = $inferredFrom->add_attribute_value_if_necessary('orthologousEvent',$e);
	if (@{$added2}) {
	    $update{$inferredFrom->db_id} = $inferredFrom;
	}
    }
}

foreach my $e (values %update) {
    print "Updating ", $e->extended_displayName, "\n";
    $dba->update_attribute($e,'orthologousEvent');
}
