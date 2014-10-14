#!/usr/local/bin/perl  -w

use lib '/usr/local/gkbdev/modules';
use lib "$ENV{HOME}/bioperl-1.0";
#use lib "$ENV{HOME}/GKB/modules";
use GKB::ClipsAdaptor;
use GKB::DBAdaptor;
use Data::Dumper;
use Getopt::Long;
use strict;
use GKB::Utils;

my $count =0;

my $tag1 = "http://www.reactome.org/cgi-bin/link?SOURCE=UniProt&ID=";
my $tag ="http://www.reactome.org/cgi-bin/eventbrowser?DB=gk_current&ID=";

 


my @out_classes = ('Event');

my %instructions = (-INSTRUCTIONS =>
		    {
			'ReferencePeptideSequence' => {'reverse_attributes' => [qw(referenceEntity referenceSequence)]},
			'PhysicalEntity' => {'reverse_attributes' => [qw(hasComponent hasMember hasCandidate input output physicalEntity inferredFrom)]},
			'CatalystActivity' => {'reverse_attributes' =>[qw(catalystActivity regulator)]},
			'Event' => {'reverse_attributes' =>[qw(hasComponent hasEvent hasMember)]}
		
		    },
		    -OUT_CLASSES => \@out_classes
		    );
		    
my %hash;

my $dbid;

my $num = $ARGV[0];
chomp $num;
  
my $out_put1 = 'protein_reactome'.$num.'.ft';


open (OUTPUT1, ">archive/$out_put1");

#Eg. my $out_put1 = 'protein_reactome16.ft';

my $db = "test_reactome_$num";
chomp $db;

my $out_put3 = 'prot_gene'.$num; #this file is needed for getting OMIM mapping
#my $out_put3 = 'prot_gene16';

open (OUTPUT3, ">archive/$out_put3");

print OUTPUT3 "UniProt ID"."\t"."Gene id\n\n";


print OUTPUT1  "-" x 56,"\n";
print OUTPUT1 "prid:     4914\n";
print OUTPUT1 "dbase:    protein\n";
print OUTPUT1 "stype:    meta-databases\n";
print OUTPUT1 "!base:    $tag1"."\n";
print OUTPUT1  "-" x  56,"\n";

print OUTPUT1 "linkid:   0\n";

my ($user,$host,$pass) = ('authortool','localhost','**REMOVED**');

 
#my ($db,$user,$host,$pass) = ('test_reactome_16','authortool','brie8','**REMOVED**'); 

#my ($db,$user,$host,$pass) = ('gk_central','authortool','brie8','**REMOVED**');

my $dba = GKB::DBAdaptor->new
    (
     -dbname => $db,
     -user   => $user,
     -host   => $host, # host where mysqld is running
     -pass   => $pass,
     -port   =>  3306
    );

# gets Entrez Gene
    
my $gene_db = $dba->fetch_instance_by_attribute('ReferenceDatabase', [['name', ['Entrez Gene']]])||
    die("No ReferenceDatabase with name 'Gene'.\n");
    
#print Dumper( $gene_db);


 


#gets all Entrez Gene instances

my $ap = $dba->fetch_instance('ReferenceDNASequence',[['referenceDatabase',[$gene_db->[0]->db_id]]]);

print "Total number of Gene ids are:".@{$ap}."\n";


my $s =0;
my @input;
my %test;

foreach(@{$ap}){

    #get only RefPepSeq with Entrez Gene as refGene

    my $protein_class = &GKB::Utils::get_reference_protein_class($dba);

    my $gp = $dba->fetch_instance($protein_class,[['referenceGene',[$ap->[$s]->db_id]]]);


    if(@{$gp}>=1){	#see whether the RefPepSeq has a  referenceGene at all

	push (@input,$gp->[0]->Identifier->[0]."\n");
	print OUTPUT1 "query:\t".$gp->[0]->Identifier->[0]."\t[pacc]\n";
	print OUTPUT3 $gp->[0]->Identifier->[0]."\t".$ap->[$s]->Identifier->[0]."\n";


	my $x = $gp->[0]->Identifier->[0];
	my $y=  $ap->[$s]->Identifier->[0];

	$test{$x} =$y;

	#print $test{$x}."\n";

	$s++;
	####################################################################################

	#if there are any many to many mappings use this section after changing if clause argument
	#in the above part of the loop to ==1




    #}elsif(@{$gp}>1){
	#
	#my $c =0;
	#
	#my $len = @{$gp};
	#
	##print $len."\t";
	#
	#until ($c == $len){
	    #
	    #push (@input,$gp->[0]->Identifier->[0]."\n");
	    #print OUTPUT1 "query:\t".$gp->[0]->Identifier->[0]."\t[pacc]\n";
	    #print OUTPUT3 $gp->[$c]->variantIdentifier->[0]."\t".$ap->[$s]->Identifier->[0]."\n";
	    #
	    #
	    #my $x = $gp->[0]->Identifier->[0];
	    #my $y=  $ap->[$s]->Identifier->[0];
	    #
	    #$test{$x} =$y;
	    #
	    #$c++;
	    #
	#}
	#$s++;
	##################################################################################
    }else{
	$s++;
	next;
    }
}

my $l = @input;
print "Total number of proteins:".$l."\n";

print OUTPUT1 "base:     &base\;\n";
print OUTPUT1 "rule:     &lo.pacc\;\n";
print OUTPUT1  "-"x56,"\n";

       
print STDERR  "total no.of errors:$count\n";
print STDERR "check proteinentrez_errorfile.txt for the list of errors\n";		
  
  
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


