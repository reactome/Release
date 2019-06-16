#!/usr/local/bin/perl -w
use strict;

use lib '/usr/local/gkbdev/modules';

use GKB::ClipsAdaptor;
use GKB::DBAdaptor;
use GKB::Utils;

use autodie;
use Data::Dumper;
use Getopt::Long;

our ( $opt_user, $opt_host, $opt_pass, $opt_port, $opt_db);
(@ARGV)  || die "Usage: $0 -user db_user -host db_host -pass db_pass -port db_port -db db_name\n";
&GetOptions( "user:s", "host:s", "pass:s", "port:i", "db:s");
$opt_db  || die "Need database name (-db).\n";

my ($num) = $opt_db =~ /(\d+)/;

my $dba = GKB::DBAdaptor->new
    (
     -dbname => $opt_db,
     -user   => $opt_user,
     -host   => $opt_host, # host where mysqld is running
     -pass   => $opt_pass,
     -port   =>  3306
    );

# gets Entrez Gene
my $gene_db = $dba->fetch_instance_by_attribute('ReferenceDatabase', [['name', ['NCBI Gene']]])||
    die("No ReferenceDatabase with name 'Gene'.\n");

#gets all Entrez Gene instances
my $entrez_gene_instances = $dba->fetch_instance('ReferenceDNASequence',[['referenceDatabase',[$gene_db->[0]->db_id]]]);
print "Total number of Gene ids are:".@{$entrez_gene_instances}."\n";


my $archive = 'archive';  
open(my $protein_reactome_out, ">", "$archive/protein_reactome$num.ft");
print $protein_reactome_out get_protein_reactome_header();

#this file is needed for getting OMIM mapping
open(my $prot_gene_out, ">", "$archive/prot_gene$num");
print $prot_gene_out "UniProt ID"."\t"."Gene id\n\n";

my $protein_count;
foreach my $entrez_gene_instance (@{$entrez_gene_instances}){
    #get only RefPepSeq with Entrez Gene as refGene
    my $protein_class = &GKB::Utils::get_reference_protein_class($dba);
    my @rgps =
        grep { $_->referenceDatabase->[0]->displayName =~ /UniProt/ }
        @{$dba->fetch_instance($protein_class,[['referenceGene',[$entrez_gene_instance->db_id]]])};

    foreach my $rgp (@rgps){
        print $protein_reactome_out "query:\t".$rgp->Identifier->[0]."\t[pacc]\n";
        print $prot_gene_out $rgp->Identifier->[0]."\t".
              $entrez_gene_instance->Identifier->[0]."\n";
        $protein_count++;
    }
}

print "Total number of proteins:$protein_count\n";

print $protein_reactome_out get_protein_reactome_footer();

close($protein_reactome_out);
close($prot_gene_out);

sub get_protein_reactome_header {
    return
    get_separator() .
    "prid:     4914\n" .
    "dbase:    protein\n" .
    "stype:    meta-databases\n" .
    "!base:    http://www.reactome.org/content/query?q=UniProt:\n" .
    get_separator() .
    "linkid:   0\n";
}

sub get_protein_reactome_footer {
    return
    "base:     &base\;\n" .
    "rule:     &lo.pacc\;\n" .
    get_separator();
}

sub get_separator {
    return "-"x56 . "\n";
}

