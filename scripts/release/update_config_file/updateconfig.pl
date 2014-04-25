#!/usr/bin/perl
use strict;
use warnings;

use autodie;
use Getopt::Long;

use Net::OpenSSH;

# Author: Joel Weiser (joel.weiser@oicr.on.ca)
# Last Modified: February 7th, 2014
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

my $ssh;
unless ($opt_host eq "localhost" || $opt_host eq $hostname) {
	$ssh = Net::OpenSSH->new($opt_host);
	$ssh->error and die $ssh->error;
}

# Open current configuration file (via SSH pipe for remote host)
my $current_config = "$opt_configpath/Config.pm";

my ($current, $current_pid);
if ($ssh) {
	($current, $current_pid) = $ssh->pipe_out("cat $current_config") or die $!;
} else {
	open $current, "<", $current_config;
}

# Open new configuration file for writing (via SSH pipe for remote host)
my $new_config = "$opt_configpath/ConfigNew.pm";

my ($new, $new_pid);
if ($ssh) {	
	($new, $new_pid) = $ssh->pipe_in("cat >$new_config") or die $!;
} else {
	open $new, ">", $new_config;
}
    
# Go through the current configuration line by line
while (my $line = <$current>) {
	# If the line contains it, change the database name to test_reactome_xx for the live site configuration file
   	$line =~ s/(GK_DB_NAME = 'test_reactome_)\d+/$1$opt_version/;
    
	# If the line contains it, update the last release date
	$line =~ s/(LAST_RELEASE_DATE = )\d{8}/$1$opt_lastrelease/;
        
	# Print the line to the new configuration file
	print $new $line;
}
    
# Close both files/pipes
close $current;
close $new;

my $archive = "mv $current_config $current_config.$opt_version"; # Archive the old configuration file
my $rename = "mv $new_config $current_config"; # Rename the new configuration file to config.pm

if ($ssh) {
	$ssh->system($archive) or die $ssh->error;
	$ssh->system($rename) or die $ssh->error;	
} else {
	system($archive) == 0 or die $!;;
	system($rename) == 0 or die $!;
}
