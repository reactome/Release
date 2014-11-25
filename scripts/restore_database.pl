#!/usr/local/bin/perl  -w
use strict;

use lib "/usr/local/gkb/modules";

use GKB::Config;
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
	if (system("mysql -u $opt_user -p$opt_pass -h $opt_host -e 'use $opt_db' 2> /dev/null") == 0) {
		print "Backing up database $opt_db\n";
		`mysqldump -u $opt_user -p$opt_pass -h $opt_host $opt_db > $opt_db.backup.dump`;
	}
	
	print "Restoring $opt_db database\n";
	my $error = system("mysql -u $opt_user -p$opt_pass -h $opt_host -e 'drop database if exists $opt_db; create database $opt_db'");
	die "Database $opt_db could not be created\n" if $error;
	
	$error = system("cat $opt_source | mysql -u $opt_user -p$opt_pass -h $opt_host $opt_db");
	die "Database $opt_db could not be populated by $opt_source" if $error;
	
	print "Database $opt_db has been restored\n";
} else {
	die "$opt_source does not exist or is not the correct format -- a .dump file is required\n";
}