#!/usr/local/bin/perl -w

use lib "/usr/local/gkb/modules";
# for use @HOME
use lib "/tmp/libs/bioperl-1.0";
use lib "/tmp/libs/GKB/modules";
use lib "/tmp/libs/my_perl_stuff";

use strict;
use GKB::DBAdaptor;
use Data::Dumper;
use Getopt::Long;

#Accepting connection parameters

@ARGV || die "Usage: $0 -sp 'species name' -user db_user -host db_host -pass db_pass -port db_port -db db_name";

our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db,$opt_debug,$opt_sp);

&GetOptions("user:s",
	    "host:s",
	    "pass:s",
	    "port:i",
	    "db=s",
	    "sp:s"
	    );

$opt_db || die "Need database name (-db).\n";
$opt_sp ||= 'Homo sapiens';

my $dba = GKB::DBAdaptor->new
    (
     -dbname => $opt_db,
     -user   => $opt_user,
     -host   => $opt_host,
     -pass   => $opt_pass,
     -port   => $opt_port,
     );
###

#Setting SBML tags

my $endtag='/>';

my $cmp_s='<listOfCompartments>';
my $cmp_e='</listOfCompartments>';
my $cname='<compartment name=';

my $spec_s='<listOfSpecies>';
my $spec_e='</listOfSpecies>';
my $sname='<species name=';

my $rnlist_s='<listOfReactions>';
my $rnlist_e='</listOfReactions>';

my $react_s='<reaction name=';
my $react_e='</reaction>';

my $sref='<speciesReference species=';
my $mref='<modifierSpeciesReference species=';

my $rtlist_s='<listOfReactants>';
my $rtlist_e='</listOfReactants>';

my $pdlist_s='<listOfProducts>';
my $pdlist_e='</listOfProducts>';

my $mlist_s='<listOfModifiers>';
my $mlist_e='</listOfModifiers>';

###

#Defining hashes & variables handling export data

my %compartments;
my %species; #{sp dbid} {name} {compartment}
my %reactions; #{rct dbid} {name} {Input}{sp db_id} {Output}{sp db_id} {Modifier}{sp db_id};

#my $PE = ''; #SBML prefixes
#my $ID = '';
#my $EV = '';
#my $CMP= '';
# taken from the "sbml_export" CGI:
my $PE = 's';
my $ID = '';
my $EV = 'r';
my $CMP= 'c';

my $event;
my $instance;
my $entity;
my $activity;
my $rctent;
my $reactions;
my $nospacename = '';

###

# Start loop to fetch all reactions
my $reactions_ar = $dba->fetch_instance_by_remote_attribute('ReactionlikeEvent',[['species.name','=',[$opt_sp]]]);
@{$reactions_ar} || die("Couldn't fetch species/organism with name '$opt_sp'");
foreach my $r (@{$reactions_ar}) {
    # Skip Generics with instances
    #next if ($r->is_a('GenericReaction') && $r->HasInstance->[0]);
    &dissect_event($r);
}

###

#Start printing out SBML

print "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n<sbml xmlns=\"http://www.sbml.org/sbml/level2\" level=\"2\" version=\"1\" xmlns:html=\"http://www.w3.org/1999/xhtml\">\n<model name=\"REACTOME\">\n";

print "$cmp_s\n";

foreach $entity (sort(keys(%compartments))){
    print "$cname\"$CMP$entity\" id=\"$CMP".$compartments{$entity}."\"$endtag\n";
}#foreach

print "$cmp_e\n";
print "$spec_s\n";

foreach $entity (sort(keys(%species))){

  #clean up entity names to conform to SBML requirements
  &clean_up(\$species{$entity}{"name"});
  print "$sname\"$PE".$species{$entity}{"name"}."\" compartment=\"$CMP".$species{$entity}{"compartment"}."\" id=\"$PE".$species{$entity}{"name"}."\" $endtag\n";

}#foreach

print "$spec_e\n";
print "$rnlist_s\n";

foreach $entity (sort(keys(%reactions))){

  &clean_up(\$reactions{$entity}{"name"});
  print "$react_s\"$EV".$reactions{$entity}{"name"}."\" id=\"$ID".$entity."\">\n";

  if(keys(%{$reactions{$entity}{"Input"}})){
    print "$rtlist_s\n";
    foreach $rctent (sort(keys(%{$reactions{$entity}{"Input"}}))){
      &clean_up(\$reactions{$entity}{"Input"}{$rctent});
      print $sref."\"$PE".$species{$rctent}{"name"}."\" $endtag\n";
    }#foreach
    print "$rtlist_e\n";
  }#if

  if(keys(%{$reactions{$entity}{"Output"}})){
    print "$pdlist_s\n";
    foreach $rctent (sort(keys(%{$reactions{$entity}{"Output"}}))){
      &clean_up(\$reactions{$entity}{"Output"}{$rctent});
      print $sref."\"$PE".$species{$rctent}{"name"}."\" $endtag\n";
    }#foreach
    print "$pdlist_e\n";
  }#if

  if(keys(%{$reactions{$entity}{"Modifier"}})){
    print "$mlist_s\n";
    foreach $rctent (sort(keys(%{$reactions{$entity}{"Modifier"}}))){
      &clean_up(\$reactions{$entity}{"Modifier"}{$rctent});
      print $mref."\"$PE".$species{$rctent}{"name"}."\" $endtag\n";
    }#foreach
    print "$mlist_e\n";
  }#if

  print "$react_e\n"
}#foreach

print "$rnlist_e\n";
print "</model>\n</sbml>";

$dba->db_handle->disconnect;

#END

#################################

#Subroutine code

sub dissect_event{

    my $instance = shift;
    (ref($instance) && ($instance =~ /GKB::Instance/) && $instance->isa("GKB::Instance")) || die "Need GKB::Instance, got '$instance'";
    my $event;
    my $tmp_name = '';
    my $valid_reaction = 0;

    my $r_id = &ID($instance);
    my $s_id;

    #For an SBML reaction to be valid it must have at least one proper input or output entity (which they call Species, BTW). A species without compartmental information is not valid. The two foreach loops below check that the reaction has at least one valid entity as input or output.

    foreach $entity (@{$instance->input}){
	if(defined($entity->attribute_value('compartment')->[0])){
	    $valid_reaction = 1;
	}#if
    }#foreach

    foreach $entity (@{$instance->output}){
        if(defined($entity->attribute_value('compartment')->[0])){
            $valid_reaction = 1;
        }#if
    }#foreach

    #If the reaction passes muster, then add the details of reaction, input, output and catalyst entities to the hashes.

    if($valid_reaction == 1){

      $reactions{$r_id}{"name"}=$instance->attribute_value('name')->[0];

      foreach $entity (@{$instance->input}){
	if(defined($entity->attribute_value('compartment')->[0])){

	  $tmp_name=$entity->attribute_value('compartment')->[0]->attribute_value('name')->[0];
	  &clean_up(\$tmp_name);
	  $compartments{$tmp_name}=&ID($entity->attribute_value('compartment')->[0]);
	  $s_id=&ID($entity);
	  $species{$s_id}{"db_id"}=$entity->db_id;
	  $species{$s_id}{"name"}=$s_id."_".$entity->attribute_value('name')->[0];
	  $species{$s_id}{"compartment"}=$tmp_name;
	  $reactions{$r_id}{"Input"}{$s_id}=$s_id;
	}#if
      }#foreach
      #print "Output\n";
      foreach $entity (@{$instance->output}){
	if(defined($entity->attribute_value('compartment')->[0])){

	  $tmp_name=$entity->attribute_value('compartment')->[0]->attribute_value('name')->[0];
	  &clean_up(\$tmp_name);
	  $compartments{$tmp_name}=&ID($entity->attribute_value('compartment')->[0]);
          $s_id=&ID($entity);

	  $species{$s_id}{"db_id"}=$s_id;
	  $species{$s_id}{"name"}=$s_id."_".$entity->attribute_value('name')->[0];
	  $species{$s_id}{"compartment"}=$tmp_name;
	  $reactions{$r_id}{"Output"}{$s_id}=$s_id;
	}#if
      }#foreach
      #print "Catalyst\n";
      foreach $activity (@{$instance->attribute_value('catalystActivity')}){
	if(defined($activity->attribute_value('physicalEntity')->[0])){
	  $entity=$activity->attribute_value('physicalEntity')->[0];
	  if(defined($entity->attribute_value('compartment')->[0])){

	    $tmp_name=$entity->attribute_value('compartment')->[0]->attribute_value('name')->[0];
	    &clean_up(\$tmp_name);
	    $compartments{$tmp_name}=&ID($entity->attribute_value('compartment')->[0]);
	    $s_id=&ID($entity);

	    $species{$s_id}{"db_id"}=$s_id;
	    $species{$s_id}{"name"}=$s_id."_".$entity->attribute_value('name')->[0];
	    $species{$s_id}{"compartment"}=$tmp_name;
	    $reactions{$r_id}{"Modifier"}{$s_id}=$s_id;
	  }#if
	}#if
      }#foreach
    } # if($valid_reaction == 1){

}#sub dissect_event

sub clean_up{

  #clean up reaction names to conform to SBML requirements

  my $string = $_[0];
  $$string=~s/[^a-zA-Z0-9]/\_/g;
  $$string=~s/\_+/\_/g;

}#sub clean_up{

sub ID{

    my $i = $_[0];
    my $id;

    if ($i->is_valid_attribute('stableIdentifier') && $i->StableIdentifier->[0]) {
        #use stable identifier
        $id = $i->StableIdentifier->[0]->displayName;
    } else {
        #useDB_ID
        $id = $i->db_id;
    }

    return $id
    }#sub ID{

