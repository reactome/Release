#!/usr/local/bin/perl -w
use strict;
use lib "/usr/local/gkb/modules";

use GKB::DBAdaptor;
use Getopt::Long;
use GKB::EFO;
use Data::Dumper;


# This script reports the DOID for Reactome Diseases along with
# the mapping to the corresponding EFO term


our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db,$opt_debug,$opt_type);

&GetOptions("user:s", "host:s", "pass:s", "port:i", "db:s", "debug");

$opt_db && $opt_user && $opt_pass 
    || die "Usage: $0 -user db_user [-host db_host] -pass db_pass [-port db_port] -db db_name [-debug]\n";

$opt_db || die "Need database name (-db).\n";    

my $dba = GKB::DBAdaptor->new
    (
     -user   => $opt_user,
     -host   => $opt_host,
     -pass   => $opt_pass,
     -port   => $opt_port,
     -dbname => $opt_db,
     -DEBUG => $opt_debug
     );

my $mapper = GKB::EFO->new($dba);
my $efo = $mapper->fetch_diseases();

open OUT, "| sort -n |";
my %efo;
for my $efo_map (keys %$efo) {
    my @instances = @{$efo->{$efo_map}};
    for my $i (@instances) {
	my ($identifier)   = @{$i->identifier};
	print OUT join("\t",$identifier,$efo_map), "\n";
    }
}
close OUT;
