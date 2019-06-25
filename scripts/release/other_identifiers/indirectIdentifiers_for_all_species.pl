#!/usr/bin/perl
use strict;
use warnings;

use Data::Dumper;
use File::Path qw/make_path/;
use Getopt::Long;
Getopt::Long::Configure('pass_through');

use GKB::Config;
use GKB::Config_Species;

my ($user, $pass, $host, $port, $db, $retrieve, $insert);
GetOptions(
    'user:s' => \$user,
    'pass:s' => \$pass,
    'host:s' => \$host,
    'port:i' => \$port,
    'db:s' => \$db,
    'retrieve' => \$retrieve,
    'insert' => \$insert
);

if ($insert && !$db) {
    die "Need database name (-db).\n";
}

print "My species:\n", Dumper(\@species);

foreach my $sp (@species) {
    if ($retrieve) {
        make_path('output', { mode => oct(775) });
        print "Retrieving other identifiers for $sp\n";
        system("perl retrieve_indirectIdentifiers_from_mart.pl -sp $sp") == 0
            or die "Could not retrieve other identifiers for $sp";
    }

    if ($insert) {
        print "Inserting other identifiers for $sp\n";
        system("perl indirectIdentifiers_from_mart.pl -sp $sp -user $user -pass $pass -host $host -port $port -db $db") == 0
            or die "Could not insert other identifiers for $sp";
    }
}