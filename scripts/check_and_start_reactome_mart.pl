#!/usr/local/bin/perl  -w

# Starts the Reactome Mart server.

#use lib "$ENV{HOME}/bioperl-1.0";
#use lib "$ENV{HOME}/GKB/modules";
use lib "../modules";
use strict;
use Getopt::Long;
use GKB::BioMart::Server;

our($opt_b,$opt_url);
&GetOptions("b=s","url=s");

if (!(defined $opt_url)) {
	$opt_url = "http://www.reactome.org:5555/biomart/martview";
}

my $server_running_flag = 1;
my $command = "wget -q $opt_url -O -";
my $content = "";
if (open(WGET, "$command |")) {
	while (<WGET>) {
		if ($_ =~ "404 Not Found") {
			$server_running_flag = 0;
			print STDERR "$0: WARNING - URL not found\n";
			last;
		}
		$content .= $_;
	}
	if ($content  eq "") {
		$server_running_flag = 0;
		print STDERR "$0: WARNING - URL returns no content\n";
	}
} else {
	print STDERR "$0: WARNING - problem trying to run: $command\n";
}

if ($server_running_flag) {
	print "Evrything looks OK\n"; 
	exit(0);
}

print STDERR "$0: WARNING - the BioMart server has stopped\n";

my $server = GKB::BioMart::Server->new();
if (defined $opt_b) {
	$server->set_biomart_dir($opt_b);
}
$server->environment();
$server->start();

