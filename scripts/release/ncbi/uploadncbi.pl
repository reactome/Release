#!/usr/bin/perl
use strict;
use warnings;
use v5.10;
use Net::FTP;

my $ftp = Net::FTP->new("ftp-private.ncbi.nih.gov", DEBUG => 0);
my ($ftppass, $version) = @ARGV;

$ftp->login("reactome", $ftppass) or die "NCBI login failed";
$ftp->cwd("holdings");
$ftp->put("archive/gene_reactome$version.xml");
$ftp->put("archive/protein_reactome$version.ft");
$ftp->put("archive/omim_reactome$version.ft");
say 'The following files are in the NCBI holdings directory';
say foreach $ftp->ls();
$ftp->quit();
