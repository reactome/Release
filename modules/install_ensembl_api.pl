#!/usr/bin/perl
use strict;
use warnings;

if (@ARGV != 1 || $ARGV[0] !~ /^\d+$/) {
    print "This script requires the ensembl version number as the first (and only) argument\n";
    print "Example: $0 73\n";
    exit 0;
}

my $version = $ARGV[0];
`cvs -d :pserver:cvsuser\@cvs.sanger.ac.uk:/cvsroot/ensembl login`;
`cvs -d :pserver:cvsuser\@cvs.sanger.ac.uk:/cvsroot/ensembl checkout -r branch-ensembl-$version ensembl`;
`cvs -d :pserver:cvsuser\@cvs.sanger.ac.uk:/cvsroot/ensembl checkout -r branch-ensembl-$version ensembl-compara`;

