#!/usr/bin/perl
use strict;
use warnings;
use autodie;

if (@ARGV != 1 || $ARGV[0] !~ /^\d+$/) {
    print "This script requires the ensembl version number as the first (and only) argument\n";
    print "Example: perl $0 73\n";
    exit 0;
}

my $dir = 'ensembl_api';
mkdir $dir unless (-e $dir);
chdir $dir;

print "Installing BioPerl\n";
`cvs -d :pserver:cvs:cvs\@code.open-bio.org:/home/repository/bioperl login`;
`cvs -d :pserver:cvs\@code.open-bio.org:/home/repository/bioperl checkout -r bioperl-release-1-2-3 bioperl-live`;

my $version = $ARGV[0];
print "Installing version $version of Ensembl Core and Ensembl Compara Perl APIs\n";
`cvs -d :pserver:cvsuser:CVSUSER\@cvs.sanger.ac.uk:/cvsroot/ensembl login`;
`cvs -d :pserver:cvsuser\@cvs.sanger.ac.uk:/cvsroot/ensembl checkout -r branch-ensembl-$version ensembl`;
`cvs -d :pserver:cvsuser\@cvs.sanger.ac.uk:/cvsroot/ensembl checkout -r branch-ensembl-$version ensembl-compara`;

