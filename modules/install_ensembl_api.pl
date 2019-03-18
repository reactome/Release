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
`git clone https://github.com/bioperl/bioperl-live.git`;

print "Installing BioMart Perl\n";
`git clone https://github.com/biomart/biomart-perl.git`;

my $version = $ARGV[0];
print "Installing version $version of Ensembl Core and Ensembl Compara Perl APIs\n";
`git clone https://github.com/Ensembl/ensembl.git`;
`cd ensembl;git checkout release/$version;cd ..`;
`git clone https://github.com/Ensembl/ensembl-compara.git`;
`cd ensembl-compara;git checkout release/$version;cd ..`;

