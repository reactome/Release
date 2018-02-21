#!/usr/bin/perl
use strict;
use warnings;

use lib '/usr/local/gkb/modules';
use GKB::Config;

use DBI;
use Getopt::Long;

my ($db, $host, $help);

GetOptions(
    "db:s" => \$db,
    "host:s" => \$host,
    "help" => \$help
);

if ($help) {
    print usage_instructions();
    exit;
}


$db ||= 'gk_central';
$host ||= 'reactomecurator.oicr.on.ca';
my $user = $GKB::Config::GK_DB_USER;
my $pass = $GKB::Config::GK_DB_PASS;

my $dbh = DBI->connect("DBI:mysql:database=$db;host=$host", $user, $pass, {RaiseError => 1});
my $select_table_names = "SELECT table_name FROM information_schema.tables WHERE information_schema.tables.table_schema=? AND table_name NOT LIKE '%\_2\_%'";
my $sth = $dbh->prepare($select_table_names);
$sth->execute($db);
print "Checking $db tables for inconsistency\n";
while (my @row = $sth->fetchrow_array) {
    print "$row[0]\n";
    my $sql = "SELECT * FROM DatabaseObject WHERE _class=? AND DB_ID NOT IN (SELECT DB_ID FROM $row[0])";
    my $sql_sth = $dbh->prepare($sql);
    $sql_sth->execute($row[0]);
    while (my @sql_row = $sql_sth->fetchrow_array) {
        my $record = join "\t", map { defined $_ ? $_ : '' } @sql_row;
        print "$record\n";
    }
}

sub usage_insturctions {
    return <<END;
This script checks a database for DatabaseObject records with an attribute
of any given class which are not in the table for that class.  These inconsistencies
are printed under the heading of each class.

Usage: perl $0 [options]
-host "db_host" (default: reactomecurator.oicr.on.ca)
-db "db_name" (default: gk_central)
-help

END
}

