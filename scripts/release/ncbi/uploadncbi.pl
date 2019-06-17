#!/usr/bin/perl
use strict;
use warnings;

use autodie;
use File::Spec;
use Getopt::Long;
use List::MoreUtils qw/any/;
use Net::FTP;

my ($ftppass, $version, $file_directory);
GetOptions(
    "ftppass:s" => \$ftppass,
    "version:i" => \$version,
    "file_dir:s" => \$file_directory 
);
$ftppass || die "NCBI ftp server password must be provided with -ftppass flag\n";
$version || die "Reactome version must be provided with -version flag\n";
$file_directory = 'archive';

my $ftp = Net::FTP->new("ftp-private.ncbi.nih.gov", Debug => 0, Passive => 1);
$ftp->login("reactome", $ftppass) || die "NCBI login failed\n";
$ftp->cwd("holdings") || die "Unable to change to the holdings directory\n";

foreach my $file_to_upload (map { File::Spec->catfile($file_directory, $_) } get_ncbi_file_names($file_directory, $version)) {
    print "Uploading file $file_to_upload to ncbi ftp server\n";
    if ($ftp->put($file_to_upload)) {
        print "$file_to_upload successfully uploaded to ncbi ftp server\n";
    }
}

my $previous_version = $version - 1;
my @files_on_ncbi_server = $ftp->ls();
foreach my $file_to_delete (get_ncbi_file_names($file_directory, $previous_version)) {
    if (any { $_ eq $file_to_delete } @files_on_ncbi_server) {
        print "Deleting file $file_to_delete from ncbi ftp server\n";
        if ($ftp->delete($file_to_delete)) {
            print "$file_to_delete successfully deleted from ncbi ftp server\n";
        }
    }
}

print "\nThe following files are in the NCBI holdings directory:\n";
print "$_\n" foreach $ftp->ls();
$ftp->quit();

sub get_ncbi_file_names {
    my $file_directory = shift;
    my $version = shift;
    
    opendir(my $dh, $file_directory);
    my @ncbi_files = grep {
        ($_ =~ get_gene_file_pattern($version)) ||
        ($_ =~ get_protein_file_pattern($version))
    } readdir($dh);
    closedir($dh);
    
    return @ncbi_files;
}

sub get_gene_file_pattern {
    my $version = shift;
    
    return qr/gene_reactome$version(-\d+)?\.xml$/;
}

sub get_protein_file_pattern {
    my $version = shift;
    
    return qr/protein_reactome$version\.ft$/;
}
