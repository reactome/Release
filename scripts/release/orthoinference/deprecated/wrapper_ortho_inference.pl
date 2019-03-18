#!/usr/local/bin/perl
use strict;
use warnings;

# This script wraps the various steps needed for inference from one species to another. It creates the ortho database,
# tweaks the datamodel needed for the inference, and runs the inference script, followed by two clean-up scripts.
# The standard run (for Reactome releases) only requires the reactome release version on the command line. One can also
# change the source species, restrict the run to one target species, or indicate a source database other than the
# default test_slice_reactomeversion_myisam. It's also possible to limit inference to specific Events by giving the
# internal id of the upstream event(s) on the command line. Inference will then be performed for these Events and all
# their downstream Events.

use lib '/usr/local/gkb/modules';

use GKB::Config;
use GKB::DBAdaptor;
use GKB::Instance;
use GKB::Utils_esther;
use GKB::Config_Species;

use Data::Dumper;
use DBI;
use English qw/-no_match_vars/;
use Getopt::Long;
use List::MoreUtils qw/any/;
use Readonly;

use Log::Log4perl qw/get_logger/;
Log::Log4perl->init(\$LOG_CONF);
my $logger = get_logger(__PACKAGE__);

@ARGV || die "Usage: $PROGRAM_NAME -source_db [source database] -target_db [database_to_create] " .
    '-source_species [species_to_infer_from - default hsap] -target_species [species_to_infer_to - default all] ' .
    '-release [Reactome version number] -release_date [scheduled release date as yyyy-mm-dd]';

my ($user, $pass, $host, $port, $source_db, $target_db, $source_species, $target_species, $release, $release_date, $debug);
GetOptions(
    'user:s' => \$user,
    'pass:s' => \$pass,
    'host:s' => \$host,
    'port:i' => \$port,
    'source_db:s' => \$source_db,
    'target_db:s' => \$target_db,
    'source_species:s' => \$source_species,
    'target_species:s' => \$target_species,
    'release:i' => \$release,
    'release_date:s' => \$release_date,
    'debug' => \$debug
);
$source_db || die "Need source database: -source_db [database from which to copy content]\n";
$release || die "Need release version: -release [Reactome version number]\n";
$release_date || die "Need release date: -release_date [scheduled release date as yyyy-mm-dd]\n";
$user ||= $GKB::Config::GK_DB_USER;
$pass ||= $GKB::Config::GK_DB_PASS;
$host ||= $GKB::Config::GK_DB_HOST;
$port ||= $GKB::Config::GK_DB_PORT;
$target_db ||= 'release_current';
$source_species ||= 'hsap'; # Human is defined as default source species

my %db_options = (
    '-host' => $host,
    '-port' => $port,
    '-user' => $user,
    '-pass' => $pass,
);
my $db_option_string = create_db_option_string(\%db_options);

#create database handle - needed to create the source database copy which will be used to add orthology data
my $dbc = DBI->connect(
    "DBI:mysql:database=$source_db;host=$host;port=$port",
    $user,
    $pass,
    { RaiseError => 1, AutoCommit => 1}
);
if (!$dbc) {
    $logger->error_die("Error connecting to database; $DBI::errstr\n");
}

# Create and populate target db from source db
system("mysql -u$user -p$pass -e 'drop database if exists $target_db; create database $target_db'") == 0 or die "$?";
run("mysqldump --opt -u$user -p$pass -h$host $source_db | mysql -u$user -p$pass -h$host $target_db") == 0 or die "$?";

# Some datamodel changes are required for running the script, mainly to adjust defining attributes in order to avoid
# either duplication or merging of instances in the inference procedure, plus introduction of some additional attributes
my $exit_value = run("perl tweak_datamodel.pl -db $target_db $db_option_string");
if ($exit_value != 0) {
    $logger->error_die("problem encountered during tweak_datamodel, aborting\n");
}

#run script for each species (order defined in config.pm)
my @species_to_exclude = qw/mtub/;
foreach my $current_species (@species) {
    $logger->info("wrapper_ortho_inference: considering species=$current_species\n");
    next if $current_species eq $source_species; #skip source species in species list
    next if any { $_ eq $current_species } @species_to_exclude;
    if ($target_species) {
       next unless $current_species eq $target_species;
    }
    $logger->info("wrapper_ortho_inference: running infer_events script\n");
    run(
        "perl infer_events.pl -db $target_db -r $release -from $source_species -sp $target_species " .
        " -release_date $release_date -thr 75 @ARGV $db_option_string"
    ); #run script with 75% complex threshold
}
`chgrp reactome $release/* 2> /dev/null`; # Allows all group members to read/write compara release files

$logger->info("wrapper_ortho_inference: run clean up scripts\n");
#These are two "clean-up" scripts to remove unused PhysicalEntities and to update display names
run("perl remove_unused_PE.pl -db $target_db $db_option_string");
run("perl updateDisplayName.pl -db $target_db -class PhysicalEntity $db_option_string");

$logger->info("$PROGRAM_NAME has finished its job\n");

sub create_db_option_string {
     my $db_options_hashref = shift;

     my $logger = get_logger(__PACKAGE__);

     my $db_option_string = '';
     foreach my $option_key (keys %{$db_options_hashref}) {
	  my $option_value = $db_options_hashref->{$option_key};

	  if ($option_value) {
	       $db_option_string .= " $option_key $option_value";
	  }
     }

     $logger->info("$0 db_option_string:$db_option_string\n");

     return $db_option_string;
}

sub run {
    my $cmd = shift;

    my $logger = get_logger(__PACKAGE__);

    $logger->info("Now starting: $cmd\n");
    my $exit_value = system($cmd);
    if ($exit_value != 0) {
        $logger->error("potential problem encountered during $cmd (exit value $exit_value)!\n");
    }
    return $exit_value;
}
