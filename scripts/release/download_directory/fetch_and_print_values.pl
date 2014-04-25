#!/usr/local/bin/perl  -w

use lib "$ENV{HOME}/bioperl-1.0";
use lib "$ENV{HOME}/GKB/modules";
use GKB::Utils;
use GKB::DBAdaptor;
use Getopt::Long;
use strict;

our($opt_host,$opt_db,$opt_pass,$opt_port,$opt_debug,$opt_user,$opt_class,$opt_query,@opt_output);

&GetOptions("user:s", "host:s", "pass:s", "port:i", "debug", "db=s", "class:s", "query=s", "output=s@");

$opt_db || die qq{Fetch instances according to specified class name and query parameters and print attribute values according
to output instructions. Can use remote attributes and reverse attributes for querying. However, reverse attributes
cannot be printed out.

Usage: $0 -class class_name \
-query "[['att1','operator',['list','of','values']],['att2.remote_att1','IS NULL',[]]]" \
-output att3 -output 'att4.remote_att2[0]'
-db db_name -user db_user -host db_host -pass db_pass -port db_port\n};

my $dba = GKB::DBAdaptor->new
(
	     -user   => $opt_user,
	     -host   => $opt_host,
	     -pass   => $opt_pass,
	     -port   => $opt_port,
	     -dbname => $opt_db,
	     -DEBUG => $opt_debug
);

$opt_class ||= $dba->ontology->root_class;

my $query = eval($opt_query);

my $ar = $dba->fetch_instance_by_remote_attribute($opt_class,$query);

GKB::Utils::print_values_according_to_instructions($ar,\@opt_output);

