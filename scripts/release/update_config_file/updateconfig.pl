#!/usr/bin/perl
use strict;
use warnings;

use autodie;
use Getopt::Long;

# Author: Joel Weiser (joel.weiser@oicr.on.ca)
# Created: February 2014
# Purpose: Update the Reactome GKB configuration file to reference a new database and "last release" date
#          This script can modify the configuration file on the local server and remotely (useful for updating the
#          fallback and live server copies from the automation pipeline on the release server)

my ($host, $config_path, $database, $last_release); 
GetOptions(
    "host:s" => \$host,
    "config_path:s" => \$config_path,
    "database:s" => \$database,
    "last_release:i" => \$last_release
);

unless ($host && $config_path && $database && $last_release && $last_release =~ /^\d{8}$/) {
    print "Usage: perl $0 -host CONFIG_FILE_SERVER -config_path CONFIG_DIR_PATH -database DATABASE " .
        "-last_release LAST_RELEASE_DATE\n";
    print "Example: perl $0 -host reactomerelease.oicr.on.ca -config_path /usr/local/gkbdev/modules/GKB " . 
        "-database release_current -last_release 20131204\n";
    exit 1;
}

# Hostname of this server
my $hostname = `hostname -f`;
chomp $hostname;

my $ssh = ($host eq "localhost" || $host eq $hostname) ? 0 : 1;

# change the database name
my $secrets = "perl -pi -e \"s/GK_DB_NAME\\s*=\\s*'\\S+';/GK_DB_NAME  = '$database';/\" $config_path/Secrets.pm";

# update the last release date
my $config = "perl -pi -e \"s/LAST_RELEASE_DATE = \\d{8}/LAST_RELEASE_DATE = $last_release/\" $config_path/Config.pm";
if ($ssh) {
	$config =~ s/"/\\"/g;

	`ssh $host "$config"`;
} else {
	`$secrets`;
	`$config`;
}
