#!/usr/local/bin/perl  -w
use strict;

use lib "/usr/local/gkb/modules";
use GKB::Config;

use Data::Dumper;
use Getopt::Long;

if ($ARGV[0] && $ARGV[0] =~ /-h(elp)?$/) {
    print <<END;
Usage: $0 -user db_user -host db_host -pass db_pass -db db_name

END
    exit;
}

our($opt_user,$opt_host,$opt_pass,$opt_db);
&GetOptions("user:s", "host:s", "pass:s", "db=s");

$opt_user ||= $GKB::Config::GK_DB_USER;
$opt_pass ||= $GKB::Config::GK_DB_PASS;
$opt_db ||= 'gk_central';
$opt_host = 'reactomecurator.oicr.on.ca';

`java -jar ../../../java/authorTool/uncurated_proteins.jar $opt_host $opt_db $opt_user $opt_pass`;
