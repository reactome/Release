#!/usr/local/bin/perl  -w

# Given a database name and the name of an external resource, this script will
# insert links from Reactome to the external resource, such
# as UniProt.  These will appear as hyperlinks on
# displayed web pages.

use Getopt::Long;
use strict;
use GKB::AddLinks::Director;
use GKB::AddLinks::BuilderParams;
use GKB::IdentifierMapper::ENSEMBLMart;
use GKB::IdentifierMapper::PICR;

our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db,$opt_debug,$opt_edb,$opt_db_ids,$opt_res);

# Parse commandline
my $usage = "Usage: $0 -user db_user -host db_host -pass db_pass -port db_port -db db_name -edb ENSEMBL_db -db_ids 'id1,id2,..' -res resource\n";
&GetOptions("user:s", "host:s", "pass:s", "port:i", "db=s", "debug", "test", "db_ids:s", "res:s");
$opt_db || die $usage;
$opt_res || die $usage;

my $identifier_mapper = GKB::IdentifierMapper::ENSEMBLMart->new();
$identifier_mapper->set_ensembl_mart_params($opt_edb);
#my $identifier_mapper = GKB::IdentifierMapper::PICR->new();

my $builder_params = GKB::AddLinks::BuilderParams->new();
$builder_params->set_db_params($opt_db, $opt_host, $opt_port, $opt_user, $opt_pass);
$builder_params->set_identifier_mapper($identifier_mapper);
$builder_params->set_db_ids($opt_db_ids);

my $director = GKB::AddLinks::Director->new();
$director->set_builder_params($builder_params);
$director->add_builder($opt_res);
$director->construct();
