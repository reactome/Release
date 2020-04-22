#!/usr/bin/perl
use strict;
use warnings;

use autodie;
use Carp;
use File::Spec;
use Getopt::Long;
use List::MoreUtils qw/any/;
use Net::FTP;

use GKB::Config;

my ($reactome_version, $local_file_directory);
GetOptions(
    'version:i' => \$reactome_version,
    'file_dir:s' => \$local_file_directory
);
$reactome_version || die "Reactome version must be provided with -version flag\n";
$local_file_directory ||= 'archive';

my $europe_pmc_ftp_connection = create_ftp_connection_to_europe_pmc({
    user => $GK_EUROPEPMC_FTP_USER,
    password => $GK_EUROPEPMC_FTP_PASS,
    directory => $GK_EUROPEPMC_FTP_DIRECTORY
});

upload_files_to_europe_pmc($europe_pmc_ftp_connection, $local_file_directory, $reactome_version)
    || die "Unable to upload some or all files to the EuropePMC ftp server " . $europe_pmc_ftp_connection->host();

delete_old_files_from_europe_pmc($europe_pmc_ftp_connection, $reactome_version);

print_listing_of_reactome_files_on_europe_pmc($europe_pmc_ftp_connection);

$europe_pmc_ftp_connection->quit();

sub create_ftp_connection_to_europe_pmc {
    my $ftp_parameters = shift // {};
    my $ftp_user = $ftp_parameters->{user} // 'anonymous';
    my $ftp_password = $ftp_parameters->{password} // '';
    my $ftp_directory = $ftp_parameters->{directory} // '.';

    my $europe_pmc_ftp_host_name = 'labslink.ebi.ac.uk';
    my $europe_pmc_ftp_connection = Net::FTP->new($europe_pmc_ftp_host_name, Debug => 0, Passive => 1);

    $europe_pmc_ftp_connection->login($ftp_user, $ftp_password) ||
        confess "EuropePMC login failed for user $ftp_user: " . $europe_pmc_ftp_connection->message;

    $europe_pmc_ftp_connection->cwd($ftp_directory) ||
        confess "Unable to change to the designated Reactome directory on the EuropePMC ftp server " .
                $europe_pmc_ftp_connection->host() . ": " . $europe_pmc_ftp_connection->message;

    return $europe_pmc_ftp_connection;
}

sub upload_files_to_europe_pmc {
    my $europe_pmc_ftp_connection = shift // confess "An FTP connection to Europe PMC is required";
    my $local_file_directory = shift // confess "The directory path containing the files to upload is required";
    my $reactome_version = shift // confess "A Reactome version number is required";

    my @files_to_upload = get_local_europepmc_file_names_to_upload($local_file_directory, $reactome_version);
    if (scalar @files_to_upload == 0) {
        print STDERR "No files were found in the directory '$local_file_directory' which should be uploaded to " .
                     "EuropePMC\n";
        return 0; # Failure as no files were found to upload
    }

    foreach my $file_to_upload (@files_to_upload) {
        print "Uploading file '$file_to_upload' to EuropePMC ftp server " . $europe_pmc_ftp_connection->host() . "...\n";

        if ($europe_pmc_ftp_connection->put($file_to_upload)) {
            print "Successfully uploaded '$file_to_upload' to EuropePMC ftp server " .
                  $europe_pmc_ftp_connection->host() . "\n";
        } else {
            print STDERR "There was a problem uploading '$file_to_upload' to the EuropePMC ftp server " .
                         $europe_pmc_ftp_connection->host() . "\n";
            return 0; # File failed to upload - indicate not all files were uploaded successfully
        }
    }

    return 1; # Files uploaded successfully
}

sub delete_old_files_from_europe_pmc {
    my $europe_pmc_ftp_connection = shift // confess "An FTP connection to Europe PMC is required";
    my $reactome_version = shift // confess "A Reactome version number is required";

    my $previous_version = $reactome_version - 1;

    my @files_to_delete = get_remote_europepmc_file_names_to_delete($europe_pmc_ftp_connection, $previous_version);

    if (scalar @files_to_delete == 0) {
        print "No files from Reactome version $previous_version to delete on the EuropePMC ftp server " .
              $europe_pmc_ftp_connection->host() . "\n"; 
        return;
    }

    foreach my $file_to_delete (@files_to_delete) {
        if (any { $_ eq $file_to_delete } get_all_files_on_europepmc_server($europe_pmc_ftp_connection)) {
            print "Deleting file '$file_to_delete' from EuropePMC ftp server " .
                  $europe_pmc_ftp_connection->host() . "...\n";

            if ($europe_pmc_ftp_connection->delete($file_to_delete)) {
                 print "Successfully deleted '$file_to_delete' from EuropePMC ftp server " .
                       $europe_pmc_ftp_connection->host() . "\n";
            } else {
                print STDERR "There was a problem deleting '$file_to_delete' from the EuropePMC ftp server " .
                             $europe_pmc_ftp_connection->host() . "\n";
            }
        }
    }
}

sub print_listing_of_reactome_files_on_europe_pmc {
    my $europe_pmc_ftp_connection = shift // confess "An FTP connection to Europe PMC is required";

    print "\nThe following files are in the directory designated for Reactome on the EuropePMC server:\n";
    print "$_\n" foreach $europe_pmc_ftp_connection->dir();
}

sub get_local_europepmc_file_names_to_upload {
    my $local_file_directory = shift // confess "The directory path containing the files to upload is required";
    my $reactome_version = shift // confess "A Reactome version number is required";

    opendir(my $dh, $local_file_directory);
    my @europepmc_files = map {
        File::Spec->catfile($local_file_directory, $_)
    } grep {
        ($_ =~ get_europepmc_profile_file_pattern($reactome_version)) ||
        ($_ =~ get_europepmc_links_file_pattern($reactome_version))
    } readdir $dh;
    closedir $dh;

    return @europepmc_files;
}

sub get_all_files_on_europepmc_server {
    my $europe_pmc_ftp_connection = shift // confess "An FTP connection to Europe PMC is required";

    return $europe_pmc_ftp_connection->ls();
}

sub get_remote_europepmc_file_names_to_delete {
    my $europe_pmc_ftp_connection = shift // confess "An FTP connection to Europe PMC is required";
    my $reactome_version = shift // confess "A Reactome version number is required";

    my @europepmc_files = grep {
        ($_ =~ get_europepmc_profile_file_pattern($reactome_version)) ||
        ($_ =~ get_europepmc_links_file_pattern($reactome_version))
    } $europe_pmc_ftp_connection->ls();

    return @europepmc_files;
}

sub get_europepmc_profile_file_pattern {
    my $reactome_version = shift // confess "Reactome version number required";

    return qr/europe_pmc_profile_reactome_$reactome_version\.xml$/;
}

sub get_europepmc_links_file_pattern {
    my $reactome_version = shift // confess "Reactome version number required";

    return qr/europe_pmc_links_reactome_$reactome_version\.xml$/;
}
