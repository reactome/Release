#!/usr/local/bin/perl  -w
use strict;

use lib "/usr/local/gkb/modules";

use GKB::Secrets;
use Data::Dumper;
use Getopt::Long;

our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db,$opt_source);

(@ARGV) || die "Usage: -user db_user -host db_host -pass db_pass -port db_port -db db_name -source mysql_dump_file\n";

&GetOptions("user:s", "host:s", "pass:s", "port:i", "db:s", "source:s");

if (!(defined $opt_db)) {
	print STDERR "You must specify a Reactome database to restore, e.g. test_slice_22\n";
	exit(1);
}
if (!(defined $opt_source)) {
	print STDERR "You must specify a source mysql dump file to restore Reactome database $opt_db\n";
	exit(1);
}


$opt_user ||= $GK_DB_USER;
$opt_pass ||= $GK_DB_PASS;
$opt_host ||= $GK_DB_HOST;

if (-e $opt_source && $opt_source =~ /\.dump$/) {
	print "Backing up database $opt_db if exists\n";
	`mysqldump -u $opt_user -p$opt_pass -h $opt_host $opt_db > $opt_db.backup.dump`;
	my $error = system("mysql -u $opt_user -p$opt_pass -h $opt_host -e 'drop database if exists $opt_db; create database $opt_db; use $opt_db; source $opt_source'");
	print "Database $opt_db successfully restored\n" unless $error;
} else {
	die "$opt_source is not the correct format -- a .dump file is required\n";
}