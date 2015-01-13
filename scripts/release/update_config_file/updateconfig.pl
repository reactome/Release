#!/usr/bin/perl
use strict;
use warnings;

use autodie;
use Getopt::Long;

# Author: Joel Weiser (joel.weiser@oicr.on.ca)
# Created: February 2014
# Purpose: Update the Reactome GKB configuration file to reference a new test_reactome_XX database and "last release" date
#	   This script can modify the configuration file on the local server and remotely (useful for updating the fallback
#	   and live server copies from the automation pipeline on the release server)

our ($opt_host, $opt_configpath, $opt_version, $opt_lastrelease); 

&GetOptions("host:s", "configpath:s", "version:i", "lastrelease:i");

unless ($opt_host &&
	$opt_configpath &&
	$opt_version && $opt_version =~ /^\d+$/ &&
	$opt_lastrelease && $opt_lastrelease =~ /^\d{8}$/) {
	print "Usage: perl $0 -host CONFIG_FILE_SERVER -configpath CONFIG_DIR_PATH -version RELEASE_VERSION -lastrelease LAST_RELEASE_DATE\n";
	print "Example: perl $0 -host reactomerelease.oicr.on.ca -configpath /usr/local/gkbdev/modules/GKB -version 48 -lastrelease 20131204\n";
	exit 1;
}

# Hostname of this server
my $hostname = `hostname -f`;
chomp $hostname;

my $ssh = ($opt_host eq "localhost" || $opt_host eq $hostname) ? 0 : 1;

my $secrets = "perl -pi -e \"s/GK_DB_NAME\\s*=\\s*'\\S+';/GK_DB_NAME  = 'test_reactome_$opt_version';/\" $opt_configpath/Secrets.pm"; # change the database name to test_reactome_xx
my $config = "perl -pi -e \"s/LAST_RELEASE_DATE = \\d{8}/LAST_RELEASE_DATE = $opt_lastrelease/\" $opt_configpath/Config.pm"; # update the last release date
if ($ssh) {
	$config =~ s/"/\\"/g;

	`ssh $opt_host "$config"`;
} else {
	`$secrets`;
	`$config`;
}
