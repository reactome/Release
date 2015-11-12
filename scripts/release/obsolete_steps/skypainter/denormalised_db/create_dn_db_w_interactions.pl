#!/usr/local/bin/perl -w

use lib "$ENV{HOME}/GKB/modules";
use lib "$ENV{HOME}/bioperl-1.0";
use lib "$ENV{HOME}/my_perl_stuff";

use GKB::ClipsAdaptor;
use GKB::Instance;
use GKB::DBAdaptor;
use GKB::Config;
use Getopt::Long;
use strict;

$GKB::Config::NO_SCHEMA_VALIDITY_CHECK = undef;

our($opt_debug,
    $opt_from_db,$opt_from_user,$opt_from_pass,$opt_from_host,$opt_from_port,
    $opt_to_db,$opt_to_user,$opt_to_pass,$opt_to_host,$opt_to_port
    );

&GetOptions("debug",
	    "from_db:s",
	    "from_user:s",
	    "from_pass:s",
	    "from_host:s",
	    "from_port:i",
	    "to_db:s",
	    "to_user:s",
	    "to_pass:s",
	    "to_host:s",
	    "to_port:i",
);

($opt_to_db && $opt_from_db) ||die "Usage: $0 -from_db db1 -to_db db2\n";

my ($dba_from,$dba_to,%participant_cache,%species_cache);

$dba_from = GKB::DBAdaptor->new
    (
     -user   => $opt_from_user,
     -host   => $opt_from_host,
     -pass   => $opt_from_pass,
     -port   => $opt_from_port,
     -dbname => $opt_from_db,
     -DEBUG => $opt_debug
     );

$dba_to = GKB::DBAdaptor->new
    (
     -user   => $opt_to_user,
     -host   => $opt_to_host,
     -pass   => $opt_to_pass,
     -port   => $opt_to_port,
     -dbname => $opt_to_db,
     -DEBUG => $opt_debug
     );
my $to_cache = $dba_to->instance_cache;

# This is a bit of a hack in order to avoid DB_ID collision between instances which get their DB_IDs from
# the source db and truely new instances.
my ($sth,$res) = $dba_from->execute('SELECT MAX(DB_ID) FROM ' . $dba_from->ontology->root_class);
my $dummy_db_id = $sth->fetchall_arrayref->[0]->[0] + 1;
$dba_to->execute('INSERT INTO ' . $dba_to->ontology->root_class . " SET DB_ID=$dummy_db_id,_displayName='dummy'");

#my $events = $dba_from->fetch_all_class_instances_as_shells('Event');
my $events = $dba_from->fetch_instance_by_attribute('Event',[['_class',['Reaction','Pathway','Interaction']]]);

my %ins1 = (-INSTRUCTIONS =>
	   {
	       'Pathway' => {'attributes' => [qw(hasComponent)]
				 , 'reverse_attributes' => [qw(regulatedEntity)]
	                    },
	       'Regulation' => {'attributes' => [qw(regulator)]},
	       'Reaction' => {'attributes' => [qw(input output catalystActivity hasMember)]
				  , 'reverse_attributes' => [qw(regulatedEntity)]
	                     },
	       'Interaction' => {'attributes' => [qw(interactor)]},
	       'CatalystActivity' => {'attributes' => [qw(physicalEntity)]},
	       'Complex' => {'attributes' => [qw(hasComponent)]},
	       'Polymer' => {'attributes' => [qw(repeatedUnit)]},
	       'SimpleEntity' => {'attributes' => [qw(referenceEntity)]},
	       'EntityWithAccessionedSequence' => {'attributes' => [qw(referenceEntity)]},
	       'OpenSet' => {'attributes' => [qw(referenceEntity)]},	       
	       'EntitySet' => {'attributes' => [qw(hasMember)]},
	   },
	    -OUT_CLASSES => [qw(ReferenceSequence ReferenceMolecule)]
	   );

# These instruction are for adding GO terms (and their parents) to the reaction.
# They DO NOT add GO terms of pathway COMPONENTS to the pathway.
my %ins2 = (-INSTRUCTIONS =>
	   {
# Pathway should not be here - this would result in a reaction having identifiers from all other 
# reaction in the same pathway.	       
#	       'Regulation' => {'attributes' => [qw(regulator)]},
	       'Reaction' => {'attributes' => [qw(input output catalystActivity hasMember)]},
# Not sure if 'reverse_attributes' => [qw(hasComponent)] is appropriate here
	       'Event' => {'attributes' => [qw(goBiologicalProcess)], 'reverse_attributes' => [qw(hasComponent)]},
	       'CatalystActivity' => {'attributes' => [qw(physicalEntity activity)]},
#	       'Complex' => {'attributes' => [qw(hasComponent)]},
# NO, we shouldn't follow Complex.hasComponent to get to relevant GO terms since they should be attached to the complex
	       'EntitySet' => {'attributes' => [qw(hasMember)]},
	       'PhysicalEntity' => {'attributes' => [qw(compartment goCellularComponent)]},
	       'GO_MolecularFunction' => {'attributes' => [qw(instanceOf componentOf)]},
	       'GO_CellularComponent' => {'attributes' => [qw(instanceOf componentOf)]},
	       'GO_BiologicalProcess' => {'attributes' => [qw(instanceOf componentOf)]}
	   },
	    -OUT_CLASSES => [qw(GO_MolecularFunction GO_CellularComponent GO_BiologicalProcess)]
	   );

my %ins4 = (-INSTRUCTIONS =>
	   {
	       'Pathway' => {'attributes' => [qw(hasComponent)]},
#	       'Regulation' => {'attributes' => [qw(regulator)]},
	       'Reaction' => {'attributes' => [qw(input output catalystActivity hasMember)]},
	       'CatalystActivity' => {'attributes' => [qw(physicalEntity)]},
	       'Complex' => {'attributes' => [qw(hasComponent)]},
	       'Polymer' => {'attributes' => [qw(repeatedUnit)]},
	       'EntitySet' => {'attributes' => [qw(hasMember)]},
	       'DatabaseObject' => {'attributes' => [qw(stableIdentifier)]},
	   },
	    -OUT_CLASSES => [qw(StableIdentifier)]
	   );

foreach my $r (@{$events}) {
    print $r->extended_displayName, "\n";
    my $new_i = GKB::Instance->new(-ONTOLOGY => $dba_to->ontology,
				   -CLASS => $r->class,
				   '_displayName' => $r->displayName);
    $new_i->db_id($r->db_id);
    $new_i->inflated(1);
    my $ar = $r->follow_class_attributes(%ins1);
    foreach my $i (@{$ar}) {
	my $p = get_Participant($i);
	$new_i->add_attribute_value_if_necessary('participant',$p);
	$new_i->add_attribute_value_if_necessary('indirectIdentifier', @{$p->Identifier});
	$p->is_a('Gene') && $new_i->add_attribute_value_if_necessary('species',@{$p->Species});
    }
    $new_i->GeneCount(scalar(grep {$_->is_a('Gene')} @{$new_i->Participant}));
    $new_i->CompoundCount(scalar(grep {$_->is_a('Compound')} @{$new_i->Participant}));
    $ar = $r->follow_class_attributes(%ins2);
    foreach my $i (@{$ar}) {
	$new_i->add_attribute_value_if_necessary('indirectIdentifier', 'GO:' . $i->Accession->[0]);
	if ($i->is_valid_attribute('ecNumber') && $i->EcNumber->[0]) {
	    $new_i->add_attribute_value_if_necessary('indirectIdentifier', @{$i->EcNumber});
	}
    }
#    Skip this as we don't have stable ids in this db at the moment
#    $ar = $r->follow_class_attributes(%ins4);
##    printf "Got %i StableIdentifier instances\n",  scalar(@{$ar});
#    foreach my $i (@{$ar}) {
#	$new_i->add_attribute_value_if_necessary('indirectIdentifier', @{$i->Identifier}, $i->displayName);
#    }
    # Add this reaction's db_id
    $new_i->add_attribute_value('indirectIdentifier',$r->db_id);
    print join(',', @{$new_i->IndirectIdentifier}), "\n";
    if ($new_i->is_valid_attribute('stableIdentifier')) {
	if (my $stid_i = $r->StableIdentifier->[0]) {
	    $new_i->StableIdentifier($stid_i->Identifier->[0]);
	    $new_i->StableIdentifierDotVersion($stid_i->displayName);
	}
    }
    $dba_to->store($new_i,1);
    $to_cache->store($new_i->db_id,$new_i);
}

# Fill with direct components
print "Filling direct components.\n";
foreach my $p (@{$events}) {
    next unless ($p->is_a('Pathway'));
    if ($p->HasComponent->[0]) {
	my $new_i = $to_cache->fetch($p->db_id) || die("No instance in dba_to cache for ". $p->extended_displayName);
	$new_i->HasDirectComponent
	    (map{$to_cache->fetch($_) || die("No instance in dba_to cache for BD_ID $_")} map {$_->db_id} @{$p->HasComponent});
	$dba_to->update_attribute($new_i,'hasDirectComponent');
    }
}

my %ins3 = (-INSTRUCTIONS =>
	   {
	       'Pathway' => {'attributes' => [qw(hasDirectComponent)]}
	   },
#	    -OUT_CLASSES => ['Event']
	   );

# Loop again. Now we can fill the other attributes
print "Filling other attributes.\n";
foreach my $p (@{$events}) {
    next unless ($p->is_a('Pathway'));
    if ($p->HasComponent->[0]) {
	print $p->extended_displayName, "\n";
	my $new_i = $to_cache->fetch($p->db_id) || die("No instance in dba_to cache for ". $p->extended_displayName);
	my $all = $new_i->follow_class_attributes(%ins3);
	@{$all} = grep {$_ != $new_i} @{$all};
#	print "All components:\t", scalar(@{$all}), "\n";
	$new_i->HasEveryComponent(@{$all});
	$dba_to->update_attribute($new_i,'hasEveryComponent');
	my @leaves = grep {! $_->is_valid_attribute('hasDirectComponent') || ! $_->HasDirectComponent->[0]} @{$all};
#	print "Leaf components:\t", scalar(@leaves), "\n";
	$new_i->HasLeafComponent(@leaves);
	$dba_to->update_attribute($new_i,'hasLeafComponent');
    }    
}

# Need to update couple of participant attributes
foreach my $p (values %participant_cache) {
    $dba_to->update_attribute($p,'identifier');
    $dba_to->update_attribute($p,'db_id_in_main_db');
}

print STDERR "$0: no fatal errors.\n";

sub get_Participant {
    my $re = shift;
    return ($re->is_a('ReferenceSequence')) ? get_Participant_Gene($re) : get_Participant_Compound($re);
}

sub get_Participant_Gene {
    my $re = shift;
    my $participant;
    
    my $key = (($re->ReferenceDatabase->[0]) ? $re->ReferenceDatabase->[0]->Name->[0] . ':' : '') . $re->Identifier->[0];
    unless ($participant = $participant_cache{$key}) {
	$participant = GKB::Instance->new(-ONTOLOGY => $dba_to->ontology,
					  -CLASS => 'Gene',
#					  '_displayName' => $re->displayName
					  '_displayName' => $key
	    );
	$participant->inflated(1);
	$participant_cache{$key} = $participant;
	foreach (@{$re->Species}) {
	    $participant->add_attribute_value('species',get_Species($_));
	}
    }
    if ($re->Identifier->[0] =~ /^\d$/) {
	$participant->add_attribute_value_if_necessary('identifier',$key);
    } else {
	$participant->add_attribute_value_if_necessary('identifier',$re->Identifier->[0]);
    }
    $participant->add_attribute_value_if_necessary('identifier',@{$re->OtherIdentifier},$re->db_id);
    $participant->add_attribute_value_if_necessary('db_id_in_main_db',$re->db_id);
    return $participant;
}

sub get_Participant_Compound {
    my $re = shift;
    my $participant;
    my $key = $re->db_id;
    unless ($participant = $participant_cache{$key}) {
	$participant = GKB::Instance->new(-ONTOLOGY => $dba_to->ontology,
					  -CLASS => 'Compound',
					  '_displayName' => $re->displayName);
	$participant->inflated(1);
	$participant_cache{$key} = $participant;
    }
    foreach my $i ($re, @{$re->CrossReference}) {
	if (my $id = get_nonnumeric_identifier($i)) {
	    $participant->add_attribute_value_if_necessary('identifier',$id);
	}
    }
    $participant->add_attribute_value_if_necessary('identifier',$re->db_id);
    $participant->add_attribute_value_if_necessary('db_id_in_main_db',$re->db_id);
    return $participant;
}

sub get_nonnumeric_identifier {
    my $i = shift;
    if ($i->ReferenceDatabase->[0] && $i->Identifier->[0]) {
	my $id = $i->Identifier->[0];
	if ($id =~ /^\d+$/) {
	    return $i->ReferenceDatabase->[0]->displayName . ':' . $id;
	} else {
	    return $id;
	}
    }
    return;
}

sub get_Species {
    my $sp = shift;
    my  $new_sp;
    unless ($new_sp = $to_cache->fetch($sp->db_id)) {
	$new_sp = GKB::Instance->new(-ONTOLOGY => $dba_to->ontology,
				     -CLASS => 'Species',
				     '_displayName' => $sp->displayName,
	    );
	$new_sp->db_id($sp->db_id);
	$sp->CrossReference->[0] && $sp->CrossReference->[0]->Identifier->[0] && $new_sp->TaxonomyID($sp->CrossReference->[0]->Identifier->[0]);
	$new_sp->inflated(1);
	$to_cache->store($new_sp->db_id,$new_sp);
	$dba_to->store($new_sp,1);
    }
    return $new_sp;
}
