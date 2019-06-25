#!/usr/bin/perl
use strict;
use warnings;

use autodie;
use File::Spec;
use Getopt::Long;
use List::MoreUtils qw/any/;
use Net::FTP;

use GKB::Config;

my ($reactome_version, $file_directory);
GetOptions(
    'version:i' => \$reactome_version,
    'file_dir:s' => \$file_directory
);
$reactome_version || die "Reactome version must be provided with -version flag\n";
$file_directory = 'archive';

my $ftp = Net::FTP->new('labslink.ebi.ac.uk', Debug => 0, Passive => 1);
$ftp->login($GK_EUROPEPMC_FTP_USER, $GK_EUROPEPMC_FTP_PASS)
    || die "EuropePMC login failed for user $GK_EUROPEPMC_FTP_USER: " . $ftp->message;
$ftp->cwd($GK_EUROPEPMC_FTP_DIRECTORY)
    || die "Unable to change to the designated Reactome directory on the EuropePMC server\n";

foreach my $file_to_upload (get_local_europepmc_file_names_to_upload($file_directory, $reactome_version)) {
    print "Uploading file $file_to_upload to EuropePMC ftp server\n";

    if ($ftp->put($file_to_upload)) {
       print "$file_to_upload successfully uploaded to EuropePMC ftp server\n";
    }
}

my $previous_version = $reactome_version - 1;
my @files_on_europepmc_server = $ftp->ls();
foreach my $file_to_delete (get_remote_europepmc_file_names_to_delete($ftp, $previous_version)) {
    if (any { $_ eq $file_to_delete } @files_on_europepmc_server) {
        print "Deleting file $file_to_delete from EuropePMC ftp server\n";

        if ($ftp->delete($file_to_delete)) {
           print "$file_to_delete successfully deleted from EuropePMC ftp server\n";
        }
    }
}

print "\nThe following files are in the directory designated for Reactome on the EuropePMC server:\n";
print "$_\n" foreach $ftp->dir();
$ftp->quit();

sub get_local_europepmc_file_names_to_upload {
    my $file_directory = shift;
    my $version = shift;

    opendir(my $dh, $file_directory);
    my @europepmc_files = map {
        File::Spec->catfile($file_directory, $_)
    } grep {
        ($_ =~ get_europepmc_profile_file_pattern($version)) ||
        ($_ =~ get_europepmc_links_file_pattern($version))
    } readdir $dh;
    closedir $dh;

    return @europepmc_files;
}

sub get_remote_europepmc_file_names_to_delete {
    my $ftp = shift;
    my $version = shift;

    my @europepmc_files = grep {
        ($_ =~ get_europepmc_profile_file_pattern($version)) ||
        ($_ =~ get_europepmc_links_file_pattern($version))
    } $ftp->ls();

    return @europepmc_files;
}

sub get_europepmc_profile_file_pattern {
    my $version = shift;

    return qr/europe_pmc_profile_reactome_$version\.xml$/;
}

sub get_europepmc_links_file_pattern {
    my $version = shift;

    return qr/europe_pmc_links_reactome_$version\.xml$/;
}
