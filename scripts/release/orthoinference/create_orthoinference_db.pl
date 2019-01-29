#!/usr/local/bin/perl
use strict;
use warnings;

use lib '/usr/local/gkb/modules';
use GKB::Config;

use autodie qw/:all/;
use English qw/-no_match_vars/;
use Getopt::Long;
use Log::Log4perl qw/get_logger/;
Log::Log4perl->init(\$LOG_CONF);
my $logger = get_logger(__PACKAGE__);

@ARGV || die "Usage: $PROGRAM_NAME -source_db [source database] -target_db [database_to_create]\n";

my ($user, $pass, $host, $port, $source_db, $target_db);
GetOptions(
    'user:s' => \$user,
    'pass:s' => \$pass,
    'host:s' => \$host,
    'port:i' => \$port,
    'source_db:s' => \$source_db,
    'target_db:s' => \$target_db,
);
$source_db || die "Need source database: -source_db [database from which to copy content]\n";
$user ||= $GKB::Config::GK_DB_USER;
$pass ||= $GKB::Config::GK_DB_PASS;
$host ||= $GKB::Config::GK_DB_HOST;
$port ||= $GKB::Config::GK_DB_PORT;
$target_db ||= 'release_current';

# Create and populate target db from source db
system "mysql -u$user -p$pass -e 'drop database if exists $target_db; create database $target_db'";
system "mysqldump --opt -u$user -p$pass -h$host $source_db | mysql -u$user -p$pass -h$host $target_db";