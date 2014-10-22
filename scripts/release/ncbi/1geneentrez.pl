#!/usr/local/bin/perl  -w
#this script is meant for the new format given by NCBI

use lib '/usr/local/gkbdev/modules';
use lib "$ENV{HOME}/bioperl-1.0";
#use lib "$ENV{HOME}/GKB/modules";
use GKB::ClipsAdaptor;
use GKB::DBAdaptor;
use Data::Dumper;
use Getopt::Long;
use strict;
use GKB::Utils;

our ( $opt_user, $opt_host, $opt_pass, $opt_port, $opt_db);
(@ARGV)  || die "Usage: $0 -user db_user -host db_host -pass db_pass -port db_port -db db_name\n";
&GetOptions( "user:s", "host:s", "pass:s", "port:i", "db:s");
$opt_db  || die "Need database name (-db).\n";

my $count =0;

my $tag = "<LinkSet>";
my $tag1a = "\n\t<Link>\n\t\t<LinkId>";
my $tag1b = "</LinkId>\n\t\t<ProviderId>4914</ProviderId>\n\t\t<ObjectSelector>\n\t\t\t";
my $tag2a = "<Database>Gene</Database>\n\t\t\t<ObjectList>\n\t\t\t\t<ObjId>";
my $tag2b  = "</ObjId>\n\t\t\t</ObjectList>\n\t\t</ObjectSelector>\n\t\t<ObjectUrl>\n\t\t\t<Base>";
my $tag3 = "\;</Base>\n\t\t\t<Rule>";
my $tag4 = "</Rule>\n\t\t\t<UrlName>";
my $tag5 = "</UrlName>\n\t\t</ObjectUrl>\n\t</Link>";

my %seen =();

my $tag7 = "</LinkSet>\n"; 

my $tag6 = $tag1b.$tag2a;


#my $num='31';
my ($num) = $opt_db =~ /(\d+)/;
chomp $num;

my $output2 = 'gene_reactome'.$num.'.xml';

open (OUTPUT2, ">archive/$output2");

my $db="test_reactome_$num";

chomp $db;		
		


my $entity_base_url = "http://www.reactome.org/cgi-bin/link?SOURCE=UniProt&amp;ID=";
my $event_base_url = "http://www.reactome.org/cgi-bin/eventbrowser_st_id?ST_ID=";

my $entity_url = "&entity.base.url";
my $event_url = "&event.base.url";

my $out_put = "geneentrez_errorfile$num.txt";
open (OUTPUT, ">archive/$out_put");

#my $out_put2 = 'gene_reactome';
#open (OUTPUT2, ">$out_put2");




my %hash;

my $dbid;




#my $out_put1 = 'proteins14';
#open (OUTPUT1, ">$out_put1");

my $output3 = 'proteins_version'.$num;  # this is just to compare with prot_gene file from 1proteinentrez.pl
open (OUTPUT3, ">archive/$output3") or die "Can't write to file";

print OUTPUT3 "UniProt ID"."\t"."Gene id"."\n\n";

 
#my ($db,$user,$host,$pass) = ('gk_central','authortool','brie8','**REMOVED**');

my $dba = GKB::DBAdaptor->new
    (
     -dbname => $opt_db,
     -user   => $opt_user,
     -host   => $opt_host, # host where mysqld is running
     -pass   => $opt_pass,
     -port   =>  3306
         );

# gets Entrez Gene
    
my $gene_db = $dba->fetch_instance_by_attribute('ReferenceDatabase', [['name', ['Entrez Gene']]])||
    die("No ReferenceDatabase with name 'Gene'.\n");
    
#print Dumper( $gene_db);


#gets all Entrez Gene instances

my $ap = $dba->fetch_instance('ReferenceDNASequence',[['referenceDatabase',[$gene_db->[0]->db_id]]]);

#print "Total number of Gene ids are:".@{$ap}."\n";


my $s =0;
my @input;
my %test;


my $protein_class = &GKB::Utils::get_reference_protein_class($dba);
foreach(@{$ap}){

   #get only RefPepSeq with Entrez Gene as refGene

   my $gp = $dba->fetch_instance($protein_class,[['referenceGene',[$ap->[$s]->db_id]]]);


   if(@{$gp}==1){	#see whether the RefPepSeq has a  referenceGene at all

 

      push (@input,$gp->[0]->Identifier->[0]) unless $seen{$gp->[0]->Identifier->[0]}++;
 

      my $x = $gp->[0]->Identifier->[0];
      my $y=  $ap->[$s]->Identifier->[0];
      
 
      $test{$x} =$y;

      print OUTPUT3 $x."\t".$test{$x}."\n";

      $s++;

   }elsif(@{$gp}>1){

      my $c =0;

      my $len = @{$gp};

      #print $len."\t";

      until ($c == $len){

	 push (@input,$gp->[0]->Identifier->[0]) unless $seen{$gp->[0]->Identifier->[0]}++;

   


	 my $x = $gp->[0]->Identifier->[0];
	 my $y=  $ap->[$s]->Identifier->[0];

	 $test{$x} =$y;

	 print OUTPUT3 $x."\t".$test{$x}."\n";

	 $c++;

      }
      $s++;

   }else{
   
      $s++;

      next;

   }
}
 

my $l = @input;

print "Total number of proteins:".$l."\n";


print "Second step starts now\n";

my $sdi;
my $sdis;
my $entity = "Reactome Entity:";
my $event = "Reactome Event:";
 


print OUTPUT2 "<?xml version=\"1.0\"?>\n<!DOCTYPE LinkSet PUBLIC \"-//NLM//DTD LinkOut 1.0//EN\"\n";
print OUTPUT2 "\"http://www.ncbi.nlm.nih.gov/entrez/linkout/doc/LinkOut.dtd\"\n\[\n";

print OUTPUT2	"\t<!ENTITY entity.base.url\n\"http://www.reactome.org/cgi-bin/link?SOURCE=UniProt&amp;ID=\">\n\t<!ENTITY event.base.url\n";
print OUTPUT2 "\"http://www.reactome.org/cgi-bin/eventbrowser_st_id?ST_ID=\">\n\]>\n";


$sdis = $dba->fetch_instance(-CLASS => $protein_class,
                                  -QUERY => [{-ATTRIBUTE => 'identifier',
                                             -VALUE => \@input                                           
                                   
				              }]
                                  );
#}

#print Dumper ($sdis);

print "Third step starts now\n\n";




my $xx =1;
print OUTPUT2  $tag;



my @out_classes = ('Event');

my %instructions = (-INSTRUCTIONS =>
		    {
			$protein_class => {'reverse_attributes' => [qw(referenceEntity referenceSequence)]},
			'PhysicalEntity' => {'reverse_attributes' => [qw(hasComponent hasMember hasCandidate input output physicalEntity inferredFrom)]},
			'CatalystActivity' => {'reverse_attributes' =>[qw(catalystActivity regulator)]},
			'Event' => {'reverse_attributes' =>[qw(hasComponent hasEvent hasMember)]}
		
		    },
		    -OUT_CLASSES => \@out_classes
		    );
		    
foreach $sdi (@{$sdis}) {
   unless ($sdi->Identifier->[0]) {
	print STDERR $sdi->extended_displayName . " has no identifier!\n";
	next;
   }
    
	#print   $sdi->Identifier->[0]."\n";
	
 
	
   my $str;


   my $ar = $sdi->follow_class_attributes(%instructions);
    
       
   if (@{$ar}){	 
	 
      #my $label = 'reactions';
	   
      my @pathways = grep {@{$_->reverse_attribute_value('frontPageItem')}} @{$ar};
	   
	
      my $key = $test{$sdi->Identifier->[0]};
	 
	 
      print  OUTPUT2 $tag1a.$xx.$tag6.$key.$tag2b.$entity_url.$tag3.$sdi->Identifier->[0].$tag4.$entity.$sdi->identifier->[0].$tag5."\n";  
	 
	 
	
      $xx++;
	   	 
		
      if(@pathways){
	 @{$ar} = @pathways;
	   
	 if (@{$ar} == 1) {
	    $str = $ar->[0]->Name->[0];
	       
	    fix_name($str);
		
	    print OUTPUT2 $tag1a.$xx.$tag6.$key.$tag2b.$event_url.$tag3.$ar->[0]->stableIdentifier->[0]->identifier->[0].$tag4.$event.$str.$tag5."\n";
		
	    #print   $sdi->Identifier->[0]."\t".$str ."\n";
		 
	    $xx++; 
		
	 } else{
	      
	    #print @{$ar}."\n";
	    my $p = 0;
	       
	    foreach(@{$ar}){  
	       # use join to have an array of evenly split elements
			
	       $str = $ar->[$p]->Name->[0];
				 
	       		 
			
	   
	       fix_name($str);
				
	       print OUTPUT2 $tag1a.$xx.$tag6.$key.$tag2b.$event_url.$tag3.$ar->[$p]->stableIdentifier->[0]->identifier->[0].$tag4.$event.$str.$tag5."\n";
			
	       #print $sdi->Identifier->[0]."\t".$tag5."\n";
			
		   	
	       $xx++;
			    
	       $p++;		     
	    }	
	 }
      }
   }else{
      print OUTPUT  "http://www.reactome.org/cgi-bin/link?SOURCE=UniProt&amp;ID=",$sdi->Identifier->[0]."\n".$sdi->extended_displayName . " participates in Event(s) but no top Pathway can be found, i.e. there seem to be a pathway which contains or is an instance of itself.\n";
      $count++;
   }	
}
	
print OUTPUT2 $tag7;
	
print STDERR  "total no.of errors:$count\n";
print STDERR "check $out_put for the list of errors\n";	
    
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
