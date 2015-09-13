package GKB::EnsEMBLUtils;

use strict;
use warnings;

use feature qw/state/;

use Carp qw/cluck/;
use Try::Tiny;
use LWP::UserAgent;

use constant GKB_MODULES => '/usr/local/gkb/modules';

use parent 'Exporter';

our @EXPORT_OK = qw/get_version get_ensembl_genome_version install_ensembl_api ensembl_api_installed/;
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
		my $url = 'http://rest.ensemblgenomes.org/info/eg_version?';
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