#!/usr/local/bin/perl  -w

# Stops the Reactome Mart server.

#use lib "$ENV{HOME}/bioperl-1.0";
#use lib "$ENV{HOME}/GKB/modules";
use lib "../modules";
use strict;
use GKB::BioMart::Server;

my $server = GKB::BioMart::Server->new();
$server->stop();

print "Done\n";
