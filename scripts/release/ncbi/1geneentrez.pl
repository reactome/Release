#!/usr/local/bin/perl -w
use strict;

#this script is meant for the new format given by NCBI

use lib '/usr/local/gkbdev/modules';

use GKB::ClipsAdaptor;
use GKB::DBAdaptor;
use GKB::Utils;

use autodie;
use Data::Dumper;
use Getopt::Long;

our ($opt_user, $opt_host, $opt_pass, $opt_port, $opt_db);
(@ARGV) || die "Usage: $0 -user db_user -host db_host -pass db_pass -port db_port -db db_name\n";
&GetOptions( "user:s", "host:s", "pass:s", "port:i", "db:s");
$opt_db || die "Need database name (-db).\n";

my ($num) = $opt_db =~ /(\d+)/;
chomp $num;	

my $entity_base_url = "http://www.reactome.org/content/query?q=UniProt:";
my $event_base_url = "http://www.reactome.org/PathwayBrowser/#";

my $dba = GKB::DBAdaptor->new(
   -dbname => $opt_db,
   -user   => $opt_user,
   -host   => $opt_host, # host where mysqld is running
   -pass   => $opt_pass
);
# gets Entrez Gene reference db
my $gene_db = $dba->fetch_instance_by_attribute('ReferenceDatabase', [['name', ['NCBI Gene']]]) ||
   die("No ReferenceDatabase with name 'Gene'.\n");

#gets all Entrez Gene instances
my $ap = $dba->fetch_instance('ReferenceDNASequence',[['referenceDatabase',[$gene_db->[0]->db_id]]]);

my $protein_class = &GKB::Utils::get_reference_protein_class($dba);
my %uniprot2entrez_gene = ();

open(my $proteins, ">", "archive/proteins_version$num");  # this is just to compare with prot_gene file from 1proteinentrez.pl
print $proteins "UniProt ID"."\t"."Gene id"."\n\n";
foreach my $entrez_gene_instance (@{$ap}){
   #get only RefPepSeq with Entrez Gene as refGene
   my $rgps = $dba->fetch_instance($protein_class,[['referenceGene',[$entrez_gene_instance->db_id]]]);

   foreach my $rgp (@{$rgps}){	
      my $uniprot_id = $rgp->Identifier->[0];
      my $entrez_gene_id =  $entrez_gene_instance->Identifier->[0];
      
      print $proteins "$uniprot_id\t$entrez_gene_id\n";
      
      push @{$uniprot2entrez_gene{$uniprot_id}}, $entrez_gene_id;
   }
}
close($proteins);
print "Total number of proteins: ". (keys %uniprot2entrez_gene) ."\n";


open(my $gene_xml, ">", "archive/gene_reactome$num.xml");
print $gene_xml "<?xml version=\"1.0\"?>\n<!DOCTYPE LinkSet PUBLIC \"-//NLM//DTD LinkOut 1.0//EN\"\n";
print $gene_xml "\"http://www.ncbi.nlm.nih.gov/entrez/linkout/doc/LinkOut.dtd\"\n\[\n";
print $gene_xml "\t<!ENTITY entity.base.url\n\"$entity_base_url\">\n\t<!ENTITY event.base.url\n";
print $gene_xml "\"$event_base_url\">\n\]>\n";

my $error_file = "geneentrez_$num.err";
open (my $error, ">", "archive/$error_file");

my $rgps = $dba->fetch_instance(
   -CLASS => $protein_class,
   -QUERY => [{-ATTRIBUTE => 'identifier',
               -VALUE => [keys %uniprot2entrez_gene]
	    }]
);

my $error_count;
my $link_id = 1;
print $gene_xml "<LinkSet>";
foreach my $rgp (@{$rgps}) {
   my $rgp_identifier = $rgp->identifier->[0]; 
   unless ($rgp_identifier) {
      print $error $rgp->extended_displayName . " has no identifier!\n";
      next;
   }

   my $events = get_events($rgp);
   unless (@{$events}){
      print $error "$entity_base_url",$rgp_identifier."\n".$rgp->extended_displayName . " participates in Event(s) but no top Pathway can be found, i.e. there seem to be a pathway which contains or is an instance of itself.\n";
      $error_count++;
      next;
   }

   my @entrez_genes = @{$uniprot2entrez_gene{$rgp_identifier}};
   foreach my $entrez_gene (@entrez_genes) {
      print $gene_xml get_link_xml($link_id, $entrez_gene, "&entity.base.url;", $rgp_identifier, "Reactome Entity:$rgp_identifier");
      $link_id++;

      my @pathways = grep {@{$_->reverse_attribute_value('frontPageItem')}} @{$events}; 
      foreach my $pathway (@pathways){
	 print $gene_xml get_link_xml($link_id, $entrez_gene, "&event.base.url;", $pathway->stableIdentifier->[0]->identifier->[0], "Reactome Event:".fix_name($pathway->Name->[0]));
	 $link_id++;
      }
   }
}
print $gene_xml "</LinkSet>\n";
print $error "total no.of errors: $error_count\n";

close($gene_xml);
close($error);

sub get_events {
   my $rgp = shift; 
   
   return $rgp->follow_class_attributes(
      -INSTRUCTIONS =>
      {
	 $protein_class => {'reverse_attributes' => [qw(referenceEntity referenceSequence)]},
	 'PhysicalEntity' => {'reverse_attributes' => [qw(hasComponent hasMember hasCandidate input output physicalEntity inferredFrom)]},
	 'CatalystActivity' => {'reverse_attributes' =>[qw(catalystActivity regulator)]},
	 'Event' => {'reverse_attributes' =>[qw(hasComponent hasEvent hasMember)]}
      },
      -OUT_CLASSES => ['Event']
   );   
}

sub get_link_xml {
   my $link_id = shift;
   my $entrez_gene = shift;
   my $base = shift;
   my $rule = shift;
   my $url_name = shift;
   
return <<XML;

   <Link>
      <LinkId>$link_id</LinkId>
      <ProviderId>4914</ProviderId>
      <ObjectSelector>
	 <Database>Gene</Database>
	 <ObjectList>
	    <ObjId>$entrez_gene</ObjId>
	 </ObjectList>
      </ObjectSelector>
      <ObjectUrl>
	 <Base>$base</Base>
	 <Rule>$rule</Rule>
	 <UrlName>$url_name</UrlName>
      </ObjectUrl>
   </Link>
XML
}

sub fix_name{
   my ($str)= @_;

   if ($str=~/amino acids/){
      $str= "Metabolism of nitrogenous molecules";
   }

   if ($str =~/Cycle, Mitotic/){
      $str = "Cell Cycle (Mitotic)";
   }
   
   if ($str =~/L13a-mediated translational/){
      $str = "L13a-mediated translation";
   }

   if ($str =~/Abortive initiation after/){
      $str = "Abortive initiation";
   }

   if ($str =~/Formation of the Cleavage and Polyadenylation/){
      $str = "Cleavage and Polyadenylation";
   }

   if ($str =~/energy metabolism/){
      $str = "Energy Metabolism";
   }	

   if ($str=~/sugars/){
      $str= "Metabolism of sugars";
   }

   return $str;
}
