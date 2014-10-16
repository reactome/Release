#!/usr/bin/perl
use strict;
use warnings;

# Purpose: Reroute apache web requests on a specified reactome server to either port 80 or 8000 by modifying httpd.conf.
# Created: March 25, 2014
# Author: Joel Weiser (joel.weiser@oicr.on.ca)

use autodie;

my $config_file = shift || die "$0: File path to the apache configuration file is required as the first argument to this script\n" . usage_instructions();
my $remote_server = shift || '';
my $fallback = shift;

redirect_requests_in_configuration_file($config_file, $remote_server, $fallback);

    
# Rewrite apache configuration file with requests rerouted to new port
sub redirect_requests_in_configuration_file {
    my $config_file = shift;
    my $remote_server = shift;
    my $fallback = shift;    
    
    my @config_file_contents = get_configuration_file_contents($config_file, $remote_server);
    
    my $new_config_file_contents = '';
    
    foreach (@config_file_contents) {
        if (/(Redirect \/ http:\/\/reactomerelease.oicr.on.ca\/)/) {
           if ($fallback) {
               s/^#//;
           } else {
               s/(.+)/#$1/;    
           }        
        }
        $new_config_file_contents .= $_ . "\n";
    }
    
    if ($remote_server) {
	`ssh $remote_server 'cat $new_config_file_contents > $config_file'`;
    } else {
        `cat $new_config_file_contents > $config_file`;
    }
}

sub get_configuration_file_contents {
    my $config_file = shift;
    my $remote_server = shift;
    
    my $contents = $remote_server ? `ssh $remote_server 'cat $config_file'` : `cat $config_file'`;
    
    return split /\n/, $contents ;
}

sub usage_instructions {
    return <<END;
    
    Usage:
        perl $0 "configuration file path" ["server - defaults to localhost"] ["redirect to fallback - 1 is true, 0 is false - defaults to 0"]
    Example:
        perl $0 /usr/local/gkb/website/html/conf/httpd.conf reactome.oicr.on.ca 1
    
END
}