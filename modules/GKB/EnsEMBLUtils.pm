package GKB::EnsEMBLUtils;

use strict;
use warnings;

use feature qw/state/;

use Carp;
use Try::Tiny;

use constant GKB_MODULES => '/usr/local/gkb/modules';

use parent 'Exporter';

our @EXPORT_OK = qw/get_version get_ensembl_genome_version install_ensembl_api ensembl_api_installed/;
our %EXPORT_TAGS = (all => [@EXPORT_OK]);


my $release;
sub get_version {    
    my $attempts = 0;
    until ($release || $attempts == 10) {
	my $url = 'http://rest.ensembl.org/info/software?';
	my $release_json = `wget -q --header='Content-type:application/json' '$url' -O -`;
	($release) = $release_json =~ /"release":(\d+)/;
	return $release - 1 if $release;
	
	$attempts++;
	sleep 60;
    }
    
    if ($release) {
	return $release - 1;
    } else {
	carp "Unable to get EnsEMBL version\n";
    }
}


my $version;
sub get_ensembl_genome_version {
    my $attempts = 0;
    until ($version || $attempts == 10) {    
	my $url = 'http://rest.ensemblgenomes.org/info/eg_version?';
	my $version_json = `wget -q --header='Content-type:application/json' '$url' -O -`;
	($version) = $version_json =~ /"version":"(\d+)"/; 
	return $version if $version;
	
	$attempts++;
	sleep 60;
    }
    
    if ($version) {
	return $version;
    } else {
	carp "Unable to get EnsEMBL Genomes version\n";
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