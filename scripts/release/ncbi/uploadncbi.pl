#!/usr/bin/perl
use strict;
use warnings;
use v5.10;
use Net::FTP;

my $ftp = Net::FTP->new("ftp-private.ncbi.nih.gov", Debug => 0, Passive => 0);
my ($ftppass, $version) = @ARGV;
my $previous_version = $version - 1;

$ftp->login("reactome", $ftppass) or die "NCBI login failed";
$ftp->cwd("holdings");

$ftp->put("archive/gene_reactome$version.xml");
$ftp->delete("gene_reactome$previous_version.xml");
$ftp->put("archive/protein_reactome$version.ft");
$ftp->delete("protein_reactome$previous_version.ft");

# OMIM no longer processed by NCBI (October 8, 2015 -- Joel Weiser)
#$ftp->put("archive/omim_reactome$version.ft");
#$ftp->delete("omim_reactome$previous_version.ft");

say 'The following files are in the NCBI holdings directory';
say foreach $ftp->ls();
$ftp->quit();
