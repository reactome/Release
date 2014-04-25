#!/usr/local/bin/perl  -w

use lib '/usr/local/gkb/modules';
use lib "$ENV{HOME}/bioperl-1.0";
#use lib "$ENV{HOME}/GKB/modules";
use GKB::ClipsAdaptor;
use GKB::DBAdaptor;
use Data::Dumper;
use Getopt::Long;
use strict;
my $count =0;
my $count1 =0;

my $num = $ARGV[0];

my $db = 'test_reactome_' . $num;
chomp $db;



my $archive = 'archive/';
my $out_put = $archive . 'ucsc_errorfile.txt';

my $out_put2 = $archive . 'ucsc_events'.$num;

my $out_put3 = $archive . 'ucsc_entity'.$num;

open (OUTPUT, ">$out_put");

open (OUTPUT2, ">$out_put2"); 

open (OUTPUT3, ">$out_put3");

print OUTPUT2  "URL for events: ". "http://www.reactome.org/cgi-bin/eventbrowser?DB=gk_current&ID=\n\n";

print OUTPUT2  "Reactome Entity\tEvent DB_ID\tEvent_name\n\n";

print OUTPUT3 "URL for entity_identifier: "."http://www.reactome.org/cgi-bin/link?SOURCE=UniProt&ID="."\n\n";
 
print OUTPUT3 "URL for entity_db_id: "."http://www.reactome.org/cgi-bin/eventbrowser?DB=gk_current&ID="."\n\n";

print OUTPUT3  "Reactome Entity\tEntity DB_ID\n\n";


my @out_classes = ('Event');



my %hash;

my $dbid;


my ($user,$host,$pass) = ('authortool','localhost','T001test');

#my ($db,$user,$host,$pass) = ('gk_central','authortool','brie8','T001test');

my $dba = GKB::DBAdaptor->new(
     -dbname => $db,
     -user   => $user,
     -host   => $host, # host where mysqld is running
     -pass   => $pass,
     -port   =>  3306
);

# gets Entrez Gene
    
my $gene_db = $dba->fetch_instance_by_attribute('ReferenceDatabase', [['name', ['UniProt']]]) ||  die("No ReferenceDatabase with name 'UniProt'.\n");
    
#print Dumper( $gene_db);

#print $gene_db->[0]->db_id."\n";


my $s =0;
my @input;
my %test;
my $species;
my $xy =0;

my $protein_class = &GKB::Utils::get_reference_protein_class($dba);

my $gp = $dba->fetch_instance($protein_class, [['referenceDatabase',[$gene_db->[0]->db_id]]]);

foreach (@{$gp}){
   
   my $sp = $gp->[$s]->species->[0]->_displayName->[0];
   
   #print $sp."\n";
   
   if ($sp =~ /Homo|Rattus|Mus/){
      push (@input,$gp->[$s]->Identifier->[0]."\n");      
      print OUTPUT3 $gp->[$s]->identifier->[0]."\t".$gp->[$s]->db_id."\n";
   }else{
      $xy++;
   }
   $s++;
} 

my $l = @input;
 
#To get a list of proteins, print this array to another output file

print "Total number of proteins:".$l."\n";
print "Second step starts now\n";

my $sdi;
my $sdis;
my $entity = "Reactome Entity:";
my $event = "Reactome Event:";
my $tag2 = "Reactome DB_ID:";

foreach (@input) { s/\s//g;}

   $sdis = $dba->fetch_instance(-CLASS => $protein_class,
				-QUERY => [{-ATTRIBUTE => 'identifier',
                                -VALUE => \@input                                          
                                }]);
#print $_."===>x"."\n";				  
				  


   my %instructions = (-INSTRUCTIONS =>
		    {
			$protein_class => {'reverse_attributes' => [qw(referenceEntity referenceSequence)]},
			'PhysicalEntity' => {'reverse_attributes' => [qw(hasComponent hasMember hasCandidate input output physicalEntity)]},
			'CatalystActivity' => {'reverse_attributes' =>[qw(catalystActivity regulator)]},
			'Event' => {'reverse_attributes' =>[qw(hasComponent hasMember hasEvent)]}
		    },
		    -OUT_CLASSES => \@out_classes
		    );

foreach $sdi (@{$sdis}) {
   unless ($sdi->Identifier->[0]) {
	print STDERR $sdi->extended_displayName . " has no identifier!\n";
	next;
    }
    
	 
   my $str;
   # print "====>".$sdi->Identifier->[0]."\n";

    my $ar = $sdi->follow_class_attributes(%instructions);

   if (@{$ar}) {
       @{$ar} = grep {! @{$_->reverse_attribute_value('hasInstance')}} grep {! @{$_->reverse_attribute_value('hasComponent')}} @{$ar};
   }
       
   if (@{$ar}){	 
	 
      my $label = 'reactions';
      my @pathways = grep {$_->is_a('Pathway')} @{$ar};
      my @reactions = grep {$_-> is_a('Reaction')} @{$ar};
	   
      if(@reactions){
   	 @{$ar} = @reactions;
		 
		#foreach(@{$ar}){
	 if (@{$ar} == 1) {
	    $str = $ar->[0]->Name->[0];
	 	
	    fix_name($str);

	    print OUTPUT2  $sdi->Identifier->[0]."\t";
	 	
	    print OUTPUT2 $ar->[0]->db_id."\t";
		 
	    print OUTPUT2  "$str\n";
	   	
	   	
		 
	 } else{
	      
            my $p = 0;
	       
	    foreach(@{$ar}){  
	    	
	       $str = $ar->[$p]->Name->[0];
		 
		 
	       fix_name($str);

	       print OUTPUT2  $sdi->Identifier->[0]."\t";
		 
	 
	       print OUTPUT2  $ar->[$p]->db_id."\t"; 
		
	       print OUTPUT2  "$str\n";
		 
	        
	       $p++; 
	       
	    }		
	 }
      }
		
	 
		
      if(@pathways){
         @{$ar} = @pathways;
         if (@{$ar} == 1) {
	    $str = $ar->[0]->Name->[0];
   	       
	    fix_name($str);
   
   
		 
	    print OUTPUT2  $sdi->Identifier->[0]."\t";
	 	
	    print OUTPUT2 $ar->[0]->db_id."\t";
	 	 
	    print OUTPUT2  "$str\n";
	 	 
	 } else{
	      
	    my $p = 0;
	        
	    foreach(@{$ar}){  
	 	   	# use join to have an array of evenly split elements
	       $str =$ar->[$p]->Name->[0];
	    		
	       fix_name($str);
	 			
	 		   	
	       print OUTPUT2  $sdi->Identifier->[0]."\t";
	 	 
	 		   	
	       print OUTPUT2  $ar->[$p]->db_id."\t"; 
	  	
	       print OUTPUT2  "$str\n";
			    
	 		    
	       $p++;		     
	    }	
	 }	    	
      }
      $count1++;
   }else{
      print OUTPUT  "http://www.reactome.org/cgi-bin/link?SOURCE=UniProt&ID=",$sdi->Identifier->[0]."\n".$sdi->extended_displayName . " participates in Event(s) but no top Pathway can be found, i.e. there seem to be a pathway which contains or is an instance of itself.\n";
      $count++;
   }
}	
print "Number of entities with events is  ".$count1."\n";
print "Number of not those entities is: ".$xy."\n";
print STDERR  "total no.of errors:$count\n";
      
sub find_top_events {
   my ($events) = @_;
   my @out;
   foreach my $e (@{$events}) {
      @{$e->reverse_attribute_value('hasComponent')} && next;
      @{$e->reverse_attribute_value('hasInstance')} && next;
      push @out, $e;
   }
   return \@out;
}	


sub fix_name{
   my ($str)= @_;
   if ($str =~ /amino acids/){
      $str = "Metabolism of nitrogenous molecules";
   }

   if ($str =~ /Cycle, Mitotic/){
      $str = "Cell Cycle (Mitotic)";
   }
   
   if ($str =~ /L13a-mediated translational/){
      $str = "L13a-mediated translation";
   }

   if ($str =~ /Abortive initiation after/){
      $str = "Abortive initiation";
   }

   if ($str =~ /Formation of the Cleavage and Polyadenylation/){
      $str = "Cleavage and Polyadenylation";
   }

   if ($str =~ /energy metabolism/){
      $str = "Energy Metabolism";
   }
 
   if ($str =~ /sugars/){
      $str = "Metabolism of sugars";
   }
 
   return $str;
}
