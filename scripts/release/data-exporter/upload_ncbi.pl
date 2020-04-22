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

my $ncbi_ftp_connection = create_ftp_connection_to_ncbi({
    user => $GK_NCBI_FTP_USER,
    password => $GK_NCBI_FTP_PASS,
    directory => 'holdings'
});

upload_files_to_ncbi($ncbi_ftp_connection, $local_file_directory, $reactome_version)
    || die "Unable to upload some or all files to the NCBI ftp server " . $ncbi_ftp_connection->host();

delete_old_files_from_ncbi($ncbi_ftp_connection, $reactome_version);

print_listing_of_reactome_files_on_ncbi($ncbi_ftp_connection);

$ncbi_ftp_connection->quit();

sub create_ftp_connection_to_ncbi {
    my $ftp_parameters = shift // {};
    my $ftp_user = $ftp_parameters->{user} // 'anonymous';
    my $ftp_password = $ftp_parameters->{password} // '';
    my $ftp_directory = $ftp_parameters->{directory} // '.';

    my $ncbi_ftp_host_name = 'ftp-private.ncbi.nih.gov';
    my $ncbi_ftp_connection = Net::FTP->new($ncbi_ftp_host_name, Debug => 0, Passive => 1);

    $ncbi_ftp_connection->login($ftp_user, $ftp_password) ||
        confess "NCBI login failed for user $ftp_user: " . $ncbi_ftp_connection->message;

    $ncbi_ftp_connection->cwd($ftp_directory) ||
        confess "Unable to change to the designated Reactome directory on the NCBI ftp server " .
                $ncbi_ftp_connection->host() . ": " . $ncbi_ftp_connection->message;

    return $ncbi_ftp_connection;
}

sub upload_files_to_ncbi {
    my $ncbi_ftp_connection = shift // confess "An FTP connection to NCBI is required";
    my $local_file_directory = shift // confess "The directory path containing the files to upload is required";
    my $reactome_version = shift // confess "A Reactome version number is required";

    my @files_to_upload = get_local_ncbi_file_names_to_upload($local_file_directory, $reactome_version);
    if (scalar @files_to_upload == 0) {
        print STDERR "No files were found in the directory '$local_file_directory' which should be uploaded to " .
                     "NCBI\n";
        return 0; # Failure as no files were found to upload
    }

    foreach my $file_to_upload (@files_to_upload) {
        print "Uploading file '$file_to_upload' to NCBI ftp server " . $ncbi_ftp_connection->host() . "...\n";

        if ($ncbi_ftp_connection->put($file_to_upload)) {
            print "Successfully uploaded '$file_to_upload' to NCBI ftp server " .
                  $ncbi_ftp_connection->host() . "\n";
        } else {
            print STDERR "There was a problem uploading '$file_to_upload' to the NCBI ftp server " .
                         $ncbi_ftp_connection->host() . ": " . $ncbi_ftp_connection->message . "\n";
            return 0; # File failed to upload - indicate not all files were uploaded successfully
        }
    }

    return 1; # Files uploaded successfully
}

sub delete_old_files_from_ncbi {
    my $ncbi_ftp_connection = shift // confess "An FTP connection to NCBI is required";
    my $reactome_version = shift // confess "A Reactome version number is required";

    my $previous_version = $reactome_version - 1;

    my @files_to_delete = get_remote_ncbi_file_names_to_delete($ncbi_ftp_connection, $previous_version);

    if (scalar @files_to_delete == 0) {
        print "No files from Reactome version $previous_version to delete on the NCBI ftp server " .
              $ncbi_ftp_connection->host() . "\n";
        return;
    }

    foreach my $file_to_delete (@files_to_delete) {
        if (any { $_ eq $file_to_delete } get_all_files_on_ncbi_server($ncbi_ftp_connection)) {
            print "Deleting file '$file_to_delete' from NCBI ftp server " .
                  $ncbi_ftp_connection->host() . "...\n";

            if ($ncbi_ftp_connection->delete($file_to_delete)) {
                print "Successfully deleted '$file_to_delete' from NCBI ftp server " .
                      $ncbi_ftp_connection->host() . "\n";
            } else {
                print STDERR "There was a problem deleting '$file_to_delete' from the NCBI ftp server " .
                             $ncbi_ftp_connection->host() . ": " . $ncbi_ftp_connection->message . "\n";
            }
        }
    }
}

sub print_listing_of_reactome_files_on_ncbi {
    my $ncbi_ftp_connection = shift // confess "An FTP connection to NCBI is required";

    print "\nThe following files are in the directory designated for Reactome on the NCBI server:\n";
    print "$_\n" foreach $ncbi_ftp_connection->dir();
}

sub get_all_files_on_ncbi_server {
    my $ncbi_ftp_connection = shift // confess "An FTP connection to NCBI is required";

    return $ncbi_ftp_connection->ls();
}

sub get_local_ncbi_file_names_to_upload {
    my $local_file_directory = shift // confess "The directory path containing the files to upload is required";
    my $reactome_version = shift // confess "A Reactome version number is required";

    opendir(my $dh, $local_file_directory);
    my @ncbi_files = map {
        File::Spec->catfile($local_file_directory, $_)
    } grep {
        ($_ =~ get_gene_file_pattern($reactome_version)) ||
        ($_ =~ get_protein_file_pattern($reactome_version))
    } readdir $dh;
    closedir $dh;

    return @ncbi_files;
}

sub get_remote_ncbi_file_names_to_delete {
    my $ncbi_ftp_connection = shift // confess "An FTP connection to NCBI is required";
    my $reactome_version = shift // confess "A Reactome version number is required";

    my @ncbi_files = grep {
        ($_ =~ get_gene_file_pattern($reactome_version)) ||
        ($_ =~ get_protein_file_pattern($reactome_version))
    } $ncbi_ftp_connection->ls();

    return @ncbi_files;
}

sub get_gene_file_pattern {
    my $reactome_version = shift // confess "Reactome version number required";

    return qr/gene_reactome$reactome_version(-\d+)?\.xml$/;
}

sub get_protein_file_pattern {
    my $reactome_version = shift // confess "Reactome version number required";

    return qr/protein_reactome$reactome_version\.ft$/;
}
