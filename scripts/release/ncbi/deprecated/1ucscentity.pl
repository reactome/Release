#!/usr/local/bin/perl  -w
use strict;

use lib '/usr/local/gkb/modules';

use GKB::ClipsAdaptor;
use GKB::DBAdaptor;

use autodie;
use Data::Dumper;
use Getopt::Long;

our ( $opt_user, $opt_host, $opt_pass, $opt_port, $opt_db);
(@ARGV)  || die "Usage: $0 -user db_user -host db_host -pass db_pass -port db_port -db db_name\n";
&GetOptions( "user:s", "host:s", "pass:s", "port:i", "db:s");
$opt_db  || die "Need database name (-db).\n";

my ($num) = $opt_db =~ /(\d+)/;

my $entity_base_url = "http://www.reactome.org/content/query?q=UniProt:";
my $event_base_url = "http://www.reactome.org/PathwayBrowser/#";

my $archive = 'archive';

my $dba = GKB::DBAdaptor->new(
     -dbname => $opt_db,
     -user   => $opt_user,
     -host   => $opt_host, # host where mysqld is running
     -pass   => $opt_pass,
     -port   =>  3306
);

my @accepted_rgp_ids;
my $rejected_rgp_count;

my $protein_class = &GKB::Utils::get_reference_protein_class($dba);
# gets Entrez Gene
my $gene_db = $dba->fetch_instance_by_attribute('ReferenceDatabase', [['name', ['UniProt']]]) ||  die("No ReferenceDatabase with name 'UniProt'.\n");
my $uniprot_rgps = $dba->fetch_instance($protein_class, [['referenceDatabase',[$gene_db->[0]->db_id]]]);

open (my $entities_fh, ">", "$archive/ucsc_entity$num");
print $entities_fh "URL for entity_identifier: $entity_base_url\n\n";
print $entities_fh "Reactome Entity\n\n";

foreach my $uniprot_rgp (@{$uniprot_rgps}){
   my $sp = $uniprot_rgp->species->[0]->_displayName->[0];
   if ($sp =~ /Homo|Rattus|Mus/){
      push (@accepted_rgp_ids,$uniprot_rgp->Identifier->[0]);      
      print $entities_fh $uniprot_rgp->identifier->[0]."\t". get_stable_identifier($uniprot_rgp)."\n";
   }else{
      $rejected_rgp_count++;
   }
} 

#To get a list of proteins, print this array to another output file
print "Total number of proteins:". scalar @accepted_rgp_ids."\n";
print "Second step starts now\n";

my $error_count;
my $entity_w_event_count;

open (my $error_fh, ">", "$archive/ucsc.err");
open (my $events_fh, ">", "$archive/ucsc_events$num"); 

print $events_fh "URL for events: $event_base_url\n\n";
print $events_fh "Reactome Entity\tEvent ST_ID\tEvent_name\n\n";

foreach my $uniprot_rgp (@{get_rgp_instances_by_id(@accepted_rgp_ids)}) {
   unless ($uniprot_rgp->Identifier->[0]) {
      print $error_fh $uniprot_rgp->extended_displayName . " has no identifier!\n";
      next;
   }

   my @reactions_and_pathways = get_reactions_and_pathways($uniprot_rgp);
   unless (@reactions_and_pathways) {
      print $error_fh "$entity_base_url",$uniprot_rgp->Identifier->[0]."\n".$uniprot_rgp->extended_displayName . " participates in Event(s) but no top Pathway can be found, i.e. there seem to be a pathway which contains or is an instance of itself.\n";
      $error_count++;
      next;
   }
   
   foreach my $event (@reactions_and_pathways){
      print $events_fh
	       $uniprot_rgp->Identifier->[0]. "\t" .
	       get_stable_identifier($event). "\t" .
	       fix_name($event->name->[0]) ."\n";
   }
   $entity_w_event_count++;
}
print "Number of entities with events is  ".$entity_w_event_count."\n";
print "Number of not those entities is: ".$rejected_rgp_count."\n";

close($entities_fh);
close($events_fh);
close($error_fh);

sub get_rgp_instances_by_id {
   my @ids = @_;
   
   return $dba->fetch_instance(-CLASS => $protein_class,
			      -QUERY => [{-ATTRIBUTE => 'identifier',
                              -VALUE => \@ids                                         
                              }]);
}

sub get_reactions_and_pathways {
   my $instance = shift;
   
   my %instructions = (-INSTRUCTIONS =>
		    {
			$protein_class => {'reverse_attributes' => [qw(referenceEntity referenceSequence)]},
			'PhysicalEntity' => {'reverse_attributes' => [qw(hasComponent hasMember hasCandidate input output physicalEntity)]},
			'CatalystActivity' => {'reverse_attributes' =>[qw(catalystActivity regulator)]},
			'Event' => {'reverse_attributes' =>[qw(hasComponent hasMember hasEvent)]}
		    },
		    -OUT_CLASSES => ['Event']
   );

   my $events = $instance->follow_class_attributes(%instructions);
   if (@{$events}) {
      @{$events} = grep {! @{$_->reverse_attribute_value('hasInstance')}} grep {! @{$_->reverse_attribute_value('hasComponent')}} @{$events};
   }
   
   my @pathways = grep {$_->is_a('Pathway')} @{$events};
   my @reactions = grep {$_-> is_a('Reaction')} @{$events};
   
   return (@reactions, @pathways);
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

sub get_stable_identifier {
   my $instance = shift;
   
   return '' unless $instance && $instance->stableIdentifier->[0];
   
   return $instance->stableIdentifier->[0]->identifier->[0];
}
