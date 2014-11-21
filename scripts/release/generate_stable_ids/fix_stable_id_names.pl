#!/usr/bin/perl -w
use strict;
use DBI;
use Data::Dumper;
use Getopt::Long;

# This script repairs stable ID names by giving them
# The correct version
# Sheldon McKay <sheldon.mckay@gmail.com>

use constant QUERY1  => 'select do.DB_ID,identifier,identifierVersion,_displayName from '.
                        'StableIdentifier s, DatabaseObject do where do.DB_ID = s.DB_ID';
use constant REPLACE => 'update DatabaseObject set _displayName = ? where DB_ID = ?';
use constant NAME    => 'select _displayName from DatabaseObject where DB_ID = ?';

my ($db,$user,$pass,$host,$port,$help,$dry_run);
GetOptions (
    "db:s"   => \$db,
    "help"   => \$help,
    "user:s" => \$user,
    "pass:s" => \$pass,
    "host:s" => \$host,
    "port:i" => \$port,
    "dry_run" => \$dry_run);
die usage() if $help || !($db && $user && $pass);

$port ||= '';
$host ||= '';

print STDERR "Dumping database before I change it\n";
undef $host if $host eq 'localhost';
my $myhost = $host ? "-h$host" : '';
my $myport = $port ? "-p$port" : '';
 
unless ($dry_run) {
    system "mysqldump --skip-add-locks -u$user -p$pass $myhost $myport $db | gzip -c > ${db}_before_st_id_fix.sql.gz"; 
}

my $dsn = "DBI:mysql:database=$db";
$dsn .= ";host=$host" if $host && $host ne 'localhost';
$dsn .= ";port=$port" if $port && $port != 3306;

my $dbh = DBI->connect($dsn, $user, $pass);

my $sth = $dbh->prepare(QUERY1);
$sth->execute;


my %report;
while (my ($db_id,$st_id,$version,$name_before) = $sth->fetchrow_array) {
    my $display_name = $st_id . ".$version";
    my $name_after = $display_name;
    $report{null}++ and $name_before = 'NULL' unless $name_before;  # this happens!
    unless ($name_before eq $display_name) {
	unless ($dry_run) {
	    my $sth = $dbh->prepare(REPLACE);
	    $sth->execute($display_name,$db_id);
	    my $name_after = get_name($db_id);
	}
	$report{renamed}++;
	print "$name_before changed to $name_after\n";
    }
    else {
	$report{ok}++;
	print "$name_before is already up to date\n";
    }
}

my $null = $report{null} || 0;
my $changed = $report{renamed} || 0;
my $ok = $report{ok} || 0;

print "I did not actually do anything but " if $dry_run;
print "$changed display names were updated\n$ok were already up-to-date\n$null were null\n";

exit if $dry_run;
print STDERR "Dumping the repaired database\n";
system "mysqldump --skip-add-locks -u$user -p$pass $myhost $myport $db | gzip -c > ${db}_after_st_id_fix.sql.gz";


sub get_name {
    my $db_id = shift;
    my $sth = $dbh->prepare(NAME);
    $sth->execute($db_id);
    my ($name) = $sth->fetchrow_array;
    return $name;
}

sub usage {
"Usage: $0
 Options:
   user    username
   pass    password
   db      database
   host    host (default localhost)
   port    port (default 3306)
"
}
