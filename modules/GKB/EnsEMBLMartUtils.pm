package GKB::EnsEMBLMartUtils;

use strict;
use warnings;

use Carp;
use Capture::Tiny ':all';
use Cwd 'abs_path';
use File::Basename 'dirname';
use Time::Out qw/timeout/;
use Try::Tiny;

use lib '/usr/local/gkb/modules/';
use lib '/usr/local/gkb/BioMart/biomart-perl/lib';
use BioMart::Initializer;
use BioMart::Query;
use BioMart::QueryRunner;

use GKB::Config;
use GKB::Config_Species;
use GKB::EnsEMBLUtils qw/:all/;
use parent 'GKB::EnsEMBLUtils';

use Log::Log4perl qw/get_logger/;
Log::Log4perl->init(\$LOG_CONF);

our @EXPORT_OK = qw/get_species_results get_query get_query_runner get_registry update_registry_file get_identifiers/;
push @EXPORT_OK, @GKB::EnsEMBLUtils::EXPORT_OK;

our %EXPORT_TAGS = (all => [@EXPORT_OK],
                   query => [qw/get_query get_query_runner/]);

sub get_species_results {
    my $species = shift;
    
    my $logger = get_logger(__PACKAGE__);
    
    my $species_dataset = $species_info{$species}->{'mart_group'};
    my $species_virtual_schema = $species_info{$species}->{'mart_virtual_schema'} || 'default';
    my $species_mart_url  = $species_info{$species}->{'mart_url'} || 'http://www.ensembl.org/biomart/martservice';
    
    my $species_results;
    my $results_complete;
    my $query_attempts = 0;
    until (($species_results && $results_complete) || $query_attempts == 3) {
        $query_attempts += 1;
        
        my $five_minutes = 5 * 60;
        timeout $five_minutes => sub {
            $species_results = capture_stdout {
                system(get_wget_query($species_mart_url, $species_dataset, $species_virtual_schema));
            };
            
            if ($species_results =~ /ERROR/) {
                $species_results = capture_stdout {
                    system(get_wget_query($species_mart_url, $species_dataset, $species_virtual_schema, 'uniprot_swissprot_accession'));
                };
            }
        };

        $results_complete = $species_results =~ /\[success\]$/;
                            
        $logger->info("Query attempt $query_attempts for species $species");
        $results_complete ? $logger->info("Results obtained successfully") : $logger->warn("Problem obtaining results - got $species_results");
    }
    
    return $species_results;
}

sub get_wget_query {
    my $mart_url = shift;
    my $mart_dataset = shift;
    my $mart_virtual_schema = shift;
    my $swissprot_attribute_name = shift;
    
    return "wget -q -O - '$mart_url?query=" . get_xml_query($mart_dataset, $mart_virtual_schema, $swissprot_attribute_name) . "'";
}

sub get_xml_query {
    my ($dataset, $virtual_schema, $swissprot_attribute_name) = @_;
    
    $dataset // confess "No dataset defined\n";
    $virtual_schema // confess "No virtual schema defined\n";
    $swissprot_attribute_name //= "uniprotswissprot";
    
    return <<XML;
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE Query>
<Query  virtualSchemaName = "$virtual_schema" formatter = "TSV" header = "0" uniqueRows = "0" count = "" completionStamp = "1">	
	<Dataset name = "$dataset" interface = "default" >
		<Attribute name = "ensembl_gene_id" />
		<Attribute name = "$swissprot_attribute_name" />
		<Attribute name = "uniprotsptrembl" />
        <Attribute name = "ensembl_peptide_id" />
	</Dataset>
</Query>
XML
}
                   
sub get_registry {
    my $action = shift // 'cached';
    my $registry_file = shift;
    
    if (!$registry_file) {
        $registry_file = get_registry_file_path();
        update_registry_file($registry_file);
    }
    
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
    
    my $ensembl_version = get_version();
    return unless $ensembl_version =~ /^\d+$/;
    
    my $ensembl_genome_version = get_ensembl_genome_version();
    return unless $ensembl_genome_version =~ /^\d+$/;
    
    my $contents = get_registry_xml_contents($registry_file);
    chomp $contents;
    $contents =~ s/(ensembl_mart_)(\d+)/$1$ensembl_version/;
    my $update = ($ensembl_version != $2);
    $contents =~ s/(plants_mart_)(\d+)/$1$ensembl_genome_version/;
    $update ||= ($ensembl_genome_version != $2);
    $contents =~ s/(protists_mart_)(\d+)/$1$ensembl_genome_version/;
    $update ||= ($ensembl_genome_version != $2);
    $contents =~ s/(fungi_mart_)(\d+)/$1$ensembl_genome_version/;
    $update ||= ($ensembl_genome_version != $2);
    
    `echo '$contents' > $registry_file` if $update;
    `rm -rf *[Cc]ached*/` if $update;
    
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
    
    return @identifiers, "interpro", "smart", "pfam", "prints", "go_id", "goslim_goa_accession";
}

sub get_registry_xml_contents {
    my $registry_file = shift // get_registry_file_path();
    
    open(my $registry_file_handle, '<', $registry_file);
    my $contents = join("", <$registry_file_handle>);
    close $registry_file_handle;
    
    return $contents;
}

sub get_registry_file_path {
    return dirname(abs_path(__FILE__)) . '/registry.xml';
}

1;
