package GKB::EnsEMBLMartUtils;

use strict;
use warnings;

use Cwd 'abs_path';
use File::Basename 'dirname';

use lib "/usr/local/reactomes/Reactome/development/GKB/modules";
use lib '/usr/local/reactomes/Reactome/development/GKB/BioMart/biomart-perl/lib';

use BioMart::Initializer;
use BioMart::Query;
use BioMart::QueryRunner;

use GKB::EnsEMBLUtils qw/:all/;
use parent 'GKB::EnsEMBLUtils';

our @EXPORT_OK = qw/get_query get_query_runner get_registry update_registry_file get_identifiers/;
push @EXPORT_OK, @GKB::EnsEMBLUtils::EXPORT_OK;

our %EXPORT_TAGS = (all => [@EXPORT_OK],
                   query => [qw/get_query get_query_runner/]);

sub get_registry {
    my $action = shift // 'cached';
    my $registry_file = shift // get_registry_file_path();
    
    update_registry_file($registry_file);
    
    my $initializer = BioMart::Initializer->new('registryFile'=>$registry_file,'action'=>$action);

    return $initializer->getRegistry();
}

sub get_query {
    my $registry = shift // get_registry();

    return BioMart::Query->new('registry'=>$registry,'virtualSchemaName'=>'default');
}    

sub get_query_runner {
    return BioMart::QueryRunner->new();
}

sub update_registry_file {
    my $registry_file = shift // get_registry_file_path();
    
    my $version = get_version();
    return unless $version =~ /^\d+$/;
    
    my $contents = get_registry_xml_contents();
    chomp $contents;
    $contents =~ s/(ensembl_mart_)(\d+)/$1$version/;    
    
    my $update = $version != $2;
    `echo '$contents' > $registry_file` if $update;
    `rm -r *[Cc]ached*/` if $update;
    
    return $update;
}

sub get_identifiers {
    my $species = shift;
    my $ensembl_url = 'http://www.ensembl.org/biomart/martservice?type=listAttributes&mart=ENSEMBL_MART_ENSEMBL&virtualSchema=default&dataset='.$species.'_gene_ensembl&interface=default&attributePage=feature_page&attributeGroup=external&attributeCollection=';
    
    my @identifiers;
    
    foreach my $attribute_type ('xrefs', 'microarray') {
        my $url = $ensembl_url.$attribute_type;
        my $results = `wget -qO- '$url'`;
        push @identifiers, (split /\n/, $results);
    }
    
    return @identifiers, "interpro", "smart", "pfam", "prints";
}

sub get_registry_xml_contents {
    return <<END
<MartRegistry>
  <MartURLLocation database="ensembl_mart_78" default="1" displayName="ENSEMBL GENES (SANGER UK)" host="www.ensembl.org" includeDatasets="" martUser="" name="ENSEMBL_MART_ENSEMBL" path="/biomart/martservice" port="80" serverVirtualSchema="default" visible="1" />
</MartRegistry>    
END
}

sub get_registry_file_path {
    return dirname(abs_path(__FILE__)) . '/registry.xml';
}

1;