#!/usr/bin/perl
use strict;
use warnings;

# Purpose: Reroute apache web requests on a specified reactome server to either port 80 or 8000 by modifying httpd.conf.
# Created: March 25, 2014
# Author: Joel Weiser (joel.weiser@oicr.on.ca)

use autodie;
use Carp;

my $port = shift;
($port && $port =~ /^\d+$/) || die "$0: Port number is required as the first argument to this script\n" . usage_instructions();

my $config_file = shift || die "$0: File path to the apache configuration file is required as the second argument to this script\n" . usage_instructions();

my $remote_server = shift || '';

redirect_requests_in_configuration_file($config_file, $remote_server, $port);

    
# Rewrite apache configuration file with requests rerouted to new port
sub redirect_requests_in_configuration_file {
    my $config_file = shift;
    my $remote_server = shift;
    my $port = shift;    
    
    my @config_file_contents = get_configuration_file_contents($config_file, $remote_server);
    
    my $new_config_file_contents = '';
    
    foreach (@config_file_contents) {
        if (/(Redirect \/ http:\/\/www.reactome.org:8000\/)/) {
           if ($port eq "8000" ) {
               s/^#//;
           } else {
               s/(.+)/#$1/;    
           }        
        }
        $new_config_file_contents .= $_;
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

sub get_file_handle {
    my $mode = shift; # Reading or writing
    my $file = shift;
    my $remote_server = shift;
    
    my ($file_handle, $pid);
    if ($remote_server) {
        my $ssh = Net::OpenSSH->new($remote_server);
        $ssh->error and croak $ssh->error;
        
        if ($mode eq ">") {
            ($file_handle, $pid) = $ssh->pipe_in("cat >$file") or croak $ssh->error; # Writing
        } else {
            ($file_handle, $pid) = $ssh->pipe_out("cat $file") or croak $ssh->error; # Reading
        }
    } else {
        open($file_handle, $mode, $file);
    }
    
    return $file_handle;
}

sub usage_instructions {
    return <<END;
    
    Usage:
        perl $0 "port number - 80 or 8000" "configuration file path" ["server - defaults to localhost"]
    Example:
        perl $0 8000 /usr/local/gkb_test/website/html/conf/httpd.conf reactome.oicr.on.ca
    
END
}
