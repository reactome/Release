#!/usr/local/bin/perl  -w
use strict;

use lib "/usr/local/gkb/modules";
use GKB::Config;

use Data::Dumper;
use Getopt::Long;

my ($user, $pass, $db, $host, $help);

GetOptions(
    "help" => \$help
);

if ($help) {
    print usage_instructions();
    exit;
}

GetOptions(
    "user:s",
    "host:s",
    "pass:s",
    "db=s"
);

$user ||= $GKB::Config::GK_CURATOR_DB_USER;
$pass ||= $GKB::Config::GK_CURATOR_DB_PASS;
$db ||= 'gk_central';
$host ||= 'curator.reactome.org';

`java -jar $GK_ROOT_DIR/java/authorTool/uncurated_proteins.jar $host $db $user $pass`;

sub usage_instructions {
    return <<END;

This script invokes the jar file of the same name
(in the $GK_ROOT_DIR/java/authorTool directory)
to produce a list of UniProt accessions that
haven't been used in Reactome.

The output file is named UnUsedUniProtsYYYYMMDD.txt
using the current date.

Usage: perl $0 [options]
    
-user db_user (default: $GKB::Config::GK_CURATOR_DB_USER)
-host db_host (default: curator.reactome.org)
-pass db_pass (default: password for $GKB::Config::GK_CURATOR_DB_USER user)
-db db_name (default: gk_central)

END
}
