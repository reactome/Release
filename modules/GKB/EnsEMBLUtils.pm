package GKB::EnsEMBLUtils;

use strict;
use warnings;

use feature qw/state/;

use Carp qw/cluck confess/;
use HTTP::Tiny;
use JSON;
use List::MoreUtils qw/any/;
use Time::HiRes qw/time usleep/;
use Try::Tiny;
use LWP::UserAgent;

use constant GKB_MODULES => '/usr/local/gkb/modules';

use parent 'Exporter';

our @EXPORT_OK = qw/get_version get_ensembl_genome_version on_EnsEMBL_primary_assembly install_ensembl_api ensembl_api_installed/;
our %EXPORT_TAGS = (all => [@EXPORT_OK]);


my $release;
sub get_version {
    my $attempts = 0;
    until ($attempts == 10 || $release) {
        my $url = 'http://rest.ensembl.org/info/software?';
        my $release_json = `wget -q --header='Content-type:application/json' '$url' -O -`;
        ($release) = $release_json =~ /"release":(\d+)/;

        $attempts++;
        sleep 60;
    }

    if ($release) {
        my $previous_release = $release - 1;
        my $pan_homology_previous_release = 'ftp://ftp.ensemblgenomes.org/pub/pan_ensembl/current/mysql/ensembl_compara_pan_homology_' .
                                            get_ensembl_genome_version() . '_' . $previous_release;
        my $ua = LWP::UserAgent->new;
        state $pan_homology_previous_release_accessible = $ua->head($pan_homology_previous_release)->is_success;
        return $previous_release if $pan_homology_previous_release_accessible;
        return $release;
    } else {
        cluck "Unable to get EnsEMBL version\n";
    }
}


my $version;
sub get_ensembl_genome_version {
    my $attempts = 0;
    until ($attempts == 10 || $version) {
        my $url = 'http://rest.ensembl.org/info/eg_version?';
        my $version_json = `wget -q --header='Content-type:application/json' '$url' -O -`;
        ($version) = $version_json =~ /"version":"(\d+)"/;

        $attempts++;
        sleep 60;
    }

    if ($version) {
        return $version;
    } else {
        cluck "Unable to get EnsEMBL Genomes version\n";
    }
}

my $time_last_invoked; # Time just after response from EnsEMBL
sub on_EnsEMBL_primary_assembly {
    my $gene_id = shift;

    my $http = HTTP::Tiny->new(verify_SSL => 0);

    my $server = 'https://rest.ensembl.org';
    my $query = "/lookup/id/$gene_id?content-type=application/json";

    if ($time_last_invoked && (time() - $time_last_invoked < 0.5)) {
        usleep 500;
    }
    my $response = $http->get($server.$query);
    $time_last_invoked = time();

    if (!$response->{success}) {
        cluck("Response from " . $server . $query . " failed: " . $response->{status} . ' - ' . $response->{reason} . "\n");
        return;
    } elsif (!(length $response->{content})) {
        cluck("Response from " . $server . $query . " returned empty content\n");
        return;
    }

    my $response_content = decode_json($response->{content});
    my $seq_region_name = $response_content->{'seq_region_name'};
    return unless $seq_region_name;

    my @primary_assembly_regions = qw/1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 X Y MT/;
    return any {$_ eq $seq_region_name} @primary_assembly_regions;
}

sub install_ensembl_api {
    my ($self, $version) = @_;

    chdir GKB_MODULES;
    `perl install_ensembl_api.pl $version`;
    chdir $self->directory;
}

sub ensembl_api_installed {
    my $ensembl_api_dir = GKB_MODULES . "/ensembl_api/";
    my @subdirectories = qw/bioperl-live ensembl ensembl-compara/;

    foreach my $subdirectory (map {$ensembl_api_dir . $_} @subdirectories) {
        return 0 unless (-d $subdirectory);
    }

    return 1;
}

1;
