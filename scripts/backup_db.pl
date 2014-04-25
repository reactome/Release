#!/usr/local/bin/perl -w

use strict;
use Getopt::Long;


our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db,$opt_dir);

(@ARGV) || die "Usage: $0 -user db_user -host db_host -pass db_pass -port db_port -db db_name\n";

&GetOptions("user:s", "host:s", "pass:s", "port:i", "db=s", "dir:s");

$opt_db || die "Need database name (-db).\n"; 


my ($sec, $min, $hour, $day, $month, $year) = localtime();
$year  += 1900;
$month += 1;

$opt_dir ||= '.';
my $outpath = $opt_dir . '/' . sprintf( "$ {opt_db}.%04d-%02d-%02d_%02d:%02d:%02d.sql", $year, $month, $day, $hour, $min, $sec );

my $cmd = "/usr/bin/mysqldump --opt --lock-tables --flush-logs $opt_db";
if ($opt_user) {
    $cmd .= " -u $opt_user";
}
if (defined $opt_pass) {
    $cmd .= " -p$opt_pass";
}
if ($opt_host) {
    $cmd .= " -h $opt_host";
}
if ($opt_port) {
    $cmd .= " -P $opt_port";
}
$cmd .= " | /bin/gzip -c > $outpath.gz";

my $retval = system("$cmd");
if ($retval) {
    throw("Database backup failed with return value $retval.\n");
}
