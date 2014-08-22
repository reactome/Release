#!/usr/local/bin/perl -w
use strict;

use lib "/usr/local/reactomes/Reactome/development/GKB/modules";
use lib '/usr/local/reactomes/Reactome/development/GKB/BioMart/biomart-perl/lib';

use DBI;
use GKB::DBAdaptor;
use GKB::Utils;
use Data::Dumper;
use Getopt::Long;

use BioMart::Initializer;
use BioMart::Query;
use BioMart::QueryRunner;

our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db,$opt_debug,$opt_sp);

(@ARGV) || die "Usage: $0 -sp 'species name'
-user db_user -host db_host -pass db_pass -port db_port -db db_name\n";

&GetOptions("user:s", "host:s", "pass:s", "port:i", "db:s", "debug", "sp=s");

$opt_db || die "Need database name (-db).\n";    

# Get connection to reactome db
my $dba = GKB::DBAdaptor->new
    (
     -user   => $opt_user,
     -host   => $opt_host,
     -pass   => $opt_pass,
     -port   => $opt_port,
     -dbname => $opt_db,
#     -DEBUG => $opt_debug
     );

# Fetch the species
$opt_sp ||= 'Homo sapiens';
my $sp = $dba->fetch_instance_by_attribute('Species',[['name',[$opt_sp]]])->[0]
    || die "No species '$opt_sp' found.\n";
my $sp_mart_name;
if ($sp->displayName =~ /^(\w)\w+ (\w+)$/) {
    $sp_mart_name = lc("$1$2");
} else {
    die "Can't form species abbreviation for mart from '" . $sp->displayName . "'.\n";
}

my $registry_file = 'registry.xml';
update_registry_file($registry_file);
my $initializer = BioMart::Initializer->new('registryFile'=>$registry_file,'action'=>'cached');
my $registry = $initializer->getRegistry;

foreach my $identifier (get_identifiers($sp_mart_name)) {
    next if $identifier =~ /chembl|clone_based|dbass|description|ottg|ottt|shares_cds|merops|mirbase/;
    
    my $query = BioMart::Query->new('registry'=>$registry,'virtualSchemaName'=>'default');

    $query->setDataset($sp_mart_name . "_gene_ensembl");

    $query->addAttribute("ensembl_gene_id");
    $query->addAttribute("ensembl_transcript_id");
    $query->addAttribute("ensembl_peptide_id");
    $query->addAttribute($identifier);

    $query->formatter("TSV");

    my $query_runner = BioMart::QueryRunner->new();
    $query_runner->execute($query);
    open(my $fh, '>', "output/$sp_mart_name\_$identifier");
    $query_runner->printResults($fh);
    close $fh;
}

if ($sp_mart_name eq 'hsapiens') {
    `curl --data-urlencode query\@affy_huex_query.xml http://www.ensembl.org/biomart/martservice/results -o output/hsapiens_affy_huex_1_0_st_v2`;
}

sub update_registry_file {
    my $registry_file = shift;
    
    require 'ensembl.lib';
    my $version = get_ensembl_version();
    return unless $version =~ /^\d+$/;
    
    my $contents = `cat $registry_file`;
    chomp $contents;
    $contents =~ s/(ensembl_mart_)(\d+)/$1$version/;    
    
    my $update = $version != $2;
    `echo '$contents' > $registry_file` if $update;
    `rm -r *[Cc]ached*/` if $update;
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


