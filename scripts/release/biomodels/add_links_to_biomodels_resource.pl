#!/usr/bin/perl
use strict;
use warnings;

# Given a database name this script will insert links from Reactome to the the BioModels resource.
# These will appear as hyperlinks on displayed web pages.

use English qw/-no_match_vars/;
use Getopt::Long;

use lib "/usr/local/gkb/modules";

use GKB::AddLinks::Director;
use GKB::AddLinks::BuilderParams;

my ($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db);

# Parse commandline
my $usage = "Usage: $PROGRAM_NAME -user db_user -host db_host -pass db_pass -port db_port -db db_name\n";
GetOptions('user:s', 'host:s', 'pass:s', 'port:i', 'db=s');
$opt_db || die $usage;

my $builder_params = GKB::AddLinks::BuilderParams->new();
$builder_params->set_db_params($opt_db, $opt_host, $opt_port, $opt_user, $opt_pass);

my $director = GKB::AddLinks::Director->new();
$director->set_builder_params($builder_params);
$director->add_builder('BioModelsEventToDatabaseIdentifier');
$director->construct();

exit 0;
