package GKB::EnsEMBLUtils;

use strict;
use warnings;

use constant GKB_MODULES => '/usr/local/gkb/modules';

use parent 'Exporter';

our @EXPORT_OK = qw/get_version get_ensembl_genome_version install_ensembl_api ensembl_api_installed/;
our %EXPORT_TAGS = (all => [@EXPORT_OK]);

sub get_version {
    my $url = 'http://rest.ensembl.org/info/software?';
    
    my $release_json = `wget -q --header='Content-type:application/json' '$url' -O -`;
    
    my ($release) = $release_json =~ /"release":(\d+)/;
    
    return $release - 1;
}

sub get_ensembl_genome_version {
    my $url = 'http://rest.ensemblgenomes.org/info/eg_version?';
    
    my $version_json = `wget -q --header='Content-type:application/json' '$url' -O -`;
    
    my ($version) = $version_json =~ /"version":"(\d+)"/;
    
    return $version;
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