package GKB::EnsEMBLMartUtils;

use strict;
use warnings;

use Carp;
use Capture::Tiny ':all';
use Cwd 'abs_path';
use File::Basename 'dirname';
use Time::Out qw/timeout/;
use Tie::IxHash;
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

our @EXPORT_OK = qw/
get_species_results
get_species_results_with_attribute_info
query_for_species_results
get_mart_info_for_species
get_uniprot_attributes
get_default_uniprot_attributes
get_wget_query
get_xml_query
get_wget_query_for_identifier
get_species_mart_name
get_uniprot_attribute_tags
check_for_attribute_error
get_registry
get_query
get_query_runner
update_registry_file
get_identifiers
/;
push @EXPORT_OK, @GKB::EnsEMBLUtils::EXPORT_OK;

our %EXPORT_TAGS = (all => [@EXPORT_OK],
                   query => [qw/get_query get_query_runner/]);

sub get_species_results {
    my $species = shift;
    return get_species_results_with_attribute_info($species)->{'results'};
}

sub get_species_results_with_attribute_info {
    my $species = shift;

    my $logger = get_logger(__PACKAGE__);

    my $species_results_with_attribute_info;
    my $results_complete;
    my $query_attempts = 0;
    until (($species_results_with_attribute_info && $results_complete) || $query_attempts == 3) {
        $query_attempts += 1;

        my $five_minutes = 5 * 60;
        timeout $five_minutes => sub {
            $species_results_with_attribute_info = query_for_species_results($species);
        };

        $results_complete = $species_results_with_attribute_info->{'results'} =~ /\[success\]$/;

        $logger->info("Query attempt $query_attempts for species $species");
        $results_complete ?
            $logger->info("Results obtained successfully") :
            $logger->warn("Problem obtaining results - got " . $species_results_with_attribute_info->{'results'});
    }

    return $species_results_with_attribute_info;
}

sub query_for_species_results {
    my $species = shift;
    my $uniprot_attribute_info = shift // {};

    my $logger = get_logger(__PACKAGE__);

    my $mart_info = get_mart_info_for_species($species);
    my $mart_url = $mart_info->{'url'};
    my $mart_dataset = $mart_info->{'dataset'};
    my $mart_virtual_schema = $mart_info->{'virtual_schema'};

    my $attribute_with_error = $uniprot_attribute_info->{'error'};

    my $uniprot_attributes = get_uniprot_attributes(
        $uniprot_attribute_info->{'attributes'},
        $attribute_with_error
    );
    my $cached_attribute_errors = $uniprot_attribute_info->{'cached_attribute_errors'} // {};

    my $species_results = capture_stdout {
        system(get_wget_query($mart_info, $uniprot_attributes));
    };

    $attribute_with_error = check_for_attribute_error($species_results);
    if ($attribute_with_error &&
        !$cached_attribute_errors->{$mart_url}{$mart_dataset}{$mart_virtual_schema}{$attribute_with_error}) {

        $cached_attribute_errors->{$mart_url}{$mart_dataset}{$mart_virtual_schema}{$attribute_with_error}++;
        return query_for_species_results(
            $species,
            {
                'error' => $attribute_with_error,
                'attributes' => $uniprot_attributes,
                'cached_attribute_errors' => $cached_attribute_errors
            }
        );
    }

    if ($species_results =~ /ERROR/) {
        $logger->warn("Problem obtaining results - got $species_results");
    }

    return {
        'uniprot_attributes' => $uniprot_attributes,
        'results' => $species_results
    };
}

sub get_mart_info_for_species {
    my $species = shift;

    my $species_dataset = $species_info{$species}->{'mart_group'};
    my $species_virtual_schema = $species_info{$species}->{'mart_virtual_schema'} || 'default';
    my $species_mart_url  = $species_info{$species}->{'mart_url'} || 'http://www.ensembl.org/biomart/martservice';

    return {
        'dataset' => $species_dataset,
        'virtual_schema' => $species_virtual_schema,
        'url' => $species_mart_url,
    };
}

sub get_uniprot_attributes {
    my $uniprot_attributes = shift // [];
    my $attribute_with_error = shift;

    my $default_uniprot_attributes = get_default_uniprot_attributes();

    # Set uniprot attributes to default attributes if no values present
    if (!@{$uniprot_attributes}) {
        $uniprot_attributes = [keys %{$default_uniprot_attributes}];
    }

    my @uniprot_attributes = grep { defined } map {
        # Use default if no error with attribute or alternative otherwise
        $attribute_with_error && ($_ eq $attribute_with_error) ?
            $default_uniprot_attributes->{$_} :
            $_;
    } @{$uniprot_attributes};

    return \@uniprot_attributes;
}

sub get_default_uniprot_attributes {
    my %default_uniprot_attributes;
    tie %default_uniprot_attributes, 'Tie::IxHash';
    %default_uniprot_attributes = (
        'uniprot_swissprot_accession' => 'uniprotswissprot',
        'uniprotsptrembl' => undef
    );
    return \%default_uniprot_attributes;
}

sub get_wget_query {
    my $mart_info = shift;
    my $uniprot_attributes = shift;

    return "wget -q -O - '" . $mart_info->{'url'} . "?query=" . get_xml_query($mart_info, $uniprot_attributes) . "'";
}

sub get_xml_query {
    my ($mart_info, $uniprot_attributes) = @_;

    my $dataset = $mart_info->{'dataset'};
    my $virtual_schema = $mart_info->{'virtual_schema'};

    $dataset // confess "No dataset defined\n";
    $virtual_schema // confess "No virtual schema defined\n";

    # Format tags for here document (indentation and one line each)
    my $attribute_tags = join "\n", map { "\t\t$_" } (
        '<Attribute name = "ensembl_gene_id" />',
        get_uniprot_attribute_tags(@{$uniprot_attributes}),
        '<Attribute name = "ensembl_peptide_id" />'
    );

    return <<XML;
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE Query>
<Query  virtualSchemaName = "$virtual_schema" formatter = "TSV" header = "0" uniqueRows = "0" count = "" completionStamp = "1">
    <Dataset name = "$dataset" interface = "default" >
$attribute_tags
    </Dataset>
</Query>
XML
}

sub get_wget_query_for_identifier {
    my $species_abbreviation = shift;
    my $identifier = shift;

    return "wget -q -O - '" . get_mart_info_for_species($species_abbreviation)->{'url'} . "?query=" .
        get_xml_query_for_identifier($species_abbreviation, $identifier) . "'";
}

sub get_xml_query_for_identifier {
    my $species_abbreviation = shift;
    my $identifier = shift;

    my $mart_info = get_mart_info_for_species($species_abbreviation);

    my $dataset = $mart_info->{'dataset'};
    my $virtual_schema = $mart_info->{'virtual_schema'};

    $dataset // confess "No dataset defined\n";
    $virtual_schema // confess "No virtual schema defined\n";

    return <<XML;
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE Query>
<Query  virtualSchemaName = "$virtual_schema" formatter = "TSV" header = "0" uniqueRows = "0" count = "" completionStamp = "1">
    <Dataset name = "$dataset" interface = "default" >
        <Attribute name = "ensembl_gene_id" />
        <Attribute name = "ensembl_transcript_id" />
        <Attribute name = "ensembl_peptide_id" />
        <Attribute name = "$identifier" />
    </Dataset>
</Query>
XML
}

sub get_species_mart_name {
    my $species_abbreviation = shift;

    my $logger = get_logger(__PACKAGE__);

    my $species_name = $species_info{$species_abbreviation}->{'name'}->[0];
    if ($species_name =~ /^(\w)\w+ (\w+)$/) {
        return lc("$1$2");
    } else {
        $logger->error("Can't form species abbreviation for mart from $species_name\n");
    }
}

sub get_uniprot_attribute_tags {
    my @uniprot_attributes = @_;

    return map { "<Attribute name = \"$_\" />" } grep { defined } @uniprot_attributes;
}

sub check_for_attribute_error {
    my $species_results = shift;

    # Capture and return attribute that caused error
    if ($species_results =~ /Attribute (?<attribute>\w+) NOT FOUND/) {
        return $+{attribute};
    }

    return;
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

    if ($update) {
        `echo '$contents' > $registry_file`;
        `rm -rf *[Cc]ached*/`;
    }

    return $update;
}

sub get_identifiers {
    my $species = shift;
    my $ensembl_url = 'http://www.ensembl.org/biomart/martservice?' .
        'type=listAttributes&mart=ENSEMBL_MART_ENSEMBL' .
        '&virtualSchema=default' .
        '&dataset='.$species.'_gene_ensembl' .
        '&interface=default' .
        '&attributePage=feature_page' .
        '&attributeGroup=external' .
        '&attributeCollection=';

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
