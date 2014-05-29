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

my $initializer = BioMart::Initializer->new('registry'=>'http://www.biomart.org/biomart/martservice?type=registry',
											'action'=>'cached');
my $registry = $initializer->getRegistry;

my $query = BioMart::Query->new('registry'=>$registry,'virtualSchemaName'=>'default');

$query->setDataset($sp_mart_name . "_gene_ensembl");

	$query->addAttribute("ensembl_gene_id");
	$query->addAttribute("ensembl_transcript_id");
	$query->addAttribute("refseq_mrna_predicted");

$query->formatter("TSV");

my $query_runner = BioMart::QueryRunner->new();
$query_runner->execute($query);
$query_runner->printResults();



