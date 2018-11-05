#!/usr/bin/perl
use strict;
use warnings;
use feature qw/state/;

use lib '/usr/local/gkb/modules';

use autodie qw/:all/;
use Carp;
use DBI;
use Getopt::Long;
use Try::Tiny;

use GKB::Config;
use GKB::DBAdaptor;

use Log::Log4perl qw/get_logger/;
Log::Log4perl->init(\$LOG_CONF);
my $logger = get_logger(__PACKAGE__);

my ($source_db, $source_host, $simplified_db, $overwrite, $help);
GetOptions(
    'source_db=s' => \$source_db,
    'source_host=s' => \$source_host,
    'simplified_db=s' => \$simplified_db,
    'overwrite' => \$overwrite,
    'help' => \$help
);

if ($help) {
    print usage_instructions();
    exit;
}

$source_db 	||= $GKB::Config::GK_DB_NAME;
$source_host	||= $GKB::Config::GK_DB_HOST;
$simplified_db 	||= $source_db . "_dn";

create_simplified_database($simplified_db, $overwrite);
load_simplified_database_schema($simplified_db);

my $simplified_db_args = "-source_db $source_db -source_host $source_host -simplified_db $simplified_db";
foreach my $selection ('pathways', 'reaction_like_events', 'pathway_links', 'physical_entities', 'physical_entities_hierarchy',
    'RLE_to_PE','external_identifiers') {
    system("perl simplified_db.pl $simplified_db_args -$selection");
}

sub create_simplified_database {
    my $simplified_database = shift;
    my $overwrite = shift;
    
    my $logger = get_logger(__PACKAGE__);
    
    my $dbh = get_simplified_database_handle();
    
    try {
        $logger->info("Creating database $simplified_database");
        $dbh->do("create database $simplified_database");
    } catch {
        unless (/database exists/ && $overwrite) {
            my $error = /database exists/ ?
            "$simplified_database exists.  Use the -overwrite flag if you wish to replace it." :
            $_;
	    
            $logger->error_die($error);
        }
	
        $dbh->do("drop database $simplified_database");
        create_simplified_database($simplified_database);
    };
}

sub load_simplified_database_schema {
    my $simplified_database = shift;
    
    my $schema_file = "simplified.sql";
    croak "$schema_file doesn't exist" unless (-e $schema_file);
    
    system("mysql -u $GKB::Config::GK_DB_USER -p$GKB::Config::GK_DB_PASS $simplified_database < $schema_file");
    my $dbh = get_simplified_database_handle();
    $dbh->do("use $simplified_database");
}

sub get_simplified_database_handle {
    state $dbh = DBI->connect("DBI:mysql:host=$GKB::Config::GK_DB_HOST;port=$GKB::Config::GK_DB_PORT",
			      $GKB::Config::GK_DB_USER,
			      $GKB::Config::GK_DB_PASS,
			      {RaiseError => 1,
			       PrintError => 0}
			      );
    
    return $dbh;
}

sub usage_instructions {
    return <<END;
    perl $0 [options]
    
    Options:
    
    -source_db	[db_name]	Source database used to populate the simplified database (default is $GKB::Config::GK_DB_NAME)
    -source_host [db_host]	Host of source database (default is $GKB::Config::GK_DB_HOST)
    -simplified_db [db_name]	Name of database to be created (default is $GKB::Config::GK_DB_NAME\_dn)
    -overwrite			Overwrite simplified database if it exists
    -help			Display these instructions
END
}
