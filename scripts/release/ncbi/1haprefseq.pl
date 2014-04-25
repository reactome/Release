#!/usr/local/bin/perl  -w
#this script is used to generate Uniprot to Refseq id mapping for Hapmap

# Author: Unknown
# Last modified: Wed April 27th, 2011
# Edited by: Joel Weiser (joel.weiser@oicr.on.ca)

use lib '/usr/local/gkbdev/modules';
use lib "$ENV{HOME}/bioperl-1.0";
#use lib "$ENV{HOME}/GKB/modules";
use GKB::ClipsAdaptor;
use GKB::DBAdaptor;
use Data::Dumper;
use Getopt::Long;
use strict;
use GKB::Utils;

my $num;
unless ($ARGV[0]) {
   $num = <STDIN>;
   chomp $num;
} else {
   $num = $ARGV[0];
}


#my $num ='30';
my $output1 = "archive/haprefseq_errorfile.txt"; 
my $output2 = 'archive/hapmap_reactome'.$num."\.txt";

open (OUTPUT, ">$output1"); # Error file
open (OUTPUT2, ">$output2"); # Output file

# Database connection info 
my $db = "test_reactome_$num"; 
my ($user,$host,$pass) = ('authortool','localhost','***REMOVED***');

#my ($db,$user,$host,$pass) = ('gk_central','authortool','brie8','***REMOVED***');

my $dba = GKB::DBAdaptor->new
    (
     -dbname => $db,
     -user   => $user,
     -host   => $host, # host where mysqld is running
     -pass   => $pass,
     -port   =>  3306
         );
        

#print OUTPUT2 "\nUniProt ID"."\t"."RefSeq id"."\t"."Pathways in Reactome\n\n"; 

my $prot_db = $dba->fetch_instance_by_attribute('ReferenceDatabase', [['name', ['UniProt']]]);

my $protein_class = &GKB::Utils::get_reference_protein_class($dba);

my $uni = $dba->fetch_instance($protein_class,[['species',['48887']]]); # Obtains all reference gene products in humans from reactome


#my $uni = $dba->fetch_instance_by_db_id('65919');

print @{$uni}."\n"; # Prints count of rgp to make sure they were obtained

 
# Instructions to obtain all events for an rgp (executed inside the foreach loop below) 
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

my @array;

my $count;

my %test;

# Each reference gene product is processed
foreach my $sdi(@{$uni}){
   @array = get_refseq ($sdi); # Obtains all hapmap ids for the rgp
   my $str; # Will hold the pathway name

   my $ar = $sdi->follow_class_attributes(%instructions); # Obtains all events for the rgp
    
   if (@{$ar}){ # Executes if events for the rgp were found
      my @pathways = grep {@{$_->reverse_attribute_value('frontPageItem')}} @{$ar}; # Stores only the events that are pathways  
      
      foreach my $pathway (@pathways) { # Loops through each pathway in the rpg   	 		
	 $str = $pathway->Name->[0]; # The pathway name is stored
	 $str = fix_name ($str); # The pathway name is standardized
	 # the hapmap ids are associated with the pathway
	 foreach my $ref (@array){ 
            push @{$test{$ref}}, $str;
         }
      } 	      
   }else{ # Error msg and error count increased if no events found for the rgp
      print OUTPUT  "http://www.reactome.org/cgi-bin/link?SOURCE=UniProt&ID=",$sdi->Identifier->[0]."\n".$sdi->extended_displayName . " participates in Event(s) but no top Pathway can be found, i.e. there seem to be a pathway which contains or is an instance of itself.\n";
      $count++;
   }
}

print STDERR  "total no.of errors:$count\n";
print STDERR "check haprefseq_errorfile.txt for the list of errors\n";		
  
# loops through the refseq ids and prints them to output with associated pathways
foreach my $key (keys %test){

   my %seen; 
   my @ary; 
 
   print OUTPUT2 $key."\t"; # Prints the refseq id
   
   # appends each pathway to the current refseq id unless it has already been appended
   foreach (@{$test{$key}}){
	push (@ary,$_) unless $seen{$_}++;
   }
   
   print OUTPUT2 join ("\t", @ary); # Joins and prints the pathways to the output on the same line as the associated refseq	
   print OUTPUT2 "\n";
}


#*************************************#
#*************************************#

sub get_refseq{
   my @transcripts; # Will hold the refseq ids
   my ($sd) = shift; # Holds the reference gene product for which hapmap refseq ids must be obtained
   my $other = $sd->otherIdentifier; # Obtains all other identifiers for the reference gene product
   
   # Obtains the hapmap refseq ids from the list of other identifiers
   foreach(@{$other}) {
      push @transcripts, grep /^NM_/,($_);
   }
   
   return @transcripts; # Returns the refseq ids
}

# Looks for key words and standardizes the pathway name
sub fix_name{

   my $str = shift @_;

   if ($str=~/sugars/){
       $str= "Metabolism of sugars";
   }

   if ($str=~/amino acids/){
         $str= "Metabolism of nitrogenous molecules";
   }

   if ($str =~/Cycle, Mitotic/){
      $str = "Cell Cycle (Mitotic)";
   }

   if ($str =~/energy metabolism/){
        $str = "Energy Metabolism";
   }
	
   if ($str =~/Lipid and lipoprotein metabolism/){
      $str = "Lipid Metabolism";
   }

   if ($str =~/vitamins and cofactors/){
      $str = "Vitamin Metabolism";
   }

   if ($str =~/Pyruvate metabolism/){
      $str = "TCA cycle";
   }	 
   return $str;
}
