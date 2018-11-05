#!/usr/local/bin/perl  -w

# Starts the Reactome Mart server.

#use lib "$ENV{HOME}/bioperl-1.0";
#use lib "$ENV{HOME}/GKB/modules";
use lib "../modules";
use strict;
use Getopt::Long;
use GKB::BioMart::Server;

our($opt_b);
&GetOptions("b=s");

my $server = GKB::BioMart::Server->new();
$server->set_biomart_dir($opt_b);
$server->environment();
$server->start();

print "Done\n";
