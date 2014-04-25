#!/usr/local/bin/perl -w

use lib "$ENV{HOME}/GKB/modules";
use lib "$ENV{HOME}/bioperl-1.0";
use lib "$ENV{HOME}/my_perl_stuff";

use GKB::ClipsAdaptor;
use GKB::Instance;
use GKB::DBAdaptor;
use Getopt::Long;
use strict;

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

my ($dba_from,$dba_to);

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

my $reactions = $dba_from->fetch_instance_by_remote_attribute
    ('Reaction',
     [['locatedEvent:ReactionCoordinates','IS NOT NULL',[]]]
     );


my %ins1 = (-INSTRUCTIONS =>
	   {
	       'ConceptualEvent' => {'attributes' => [qw(hasSpecialisedForm)]},
	       'EquivalentEventSet' => {'attributes' => [qw(hasMember)]},
	       'Reaction' => {'attributes' => [qw(input catalystActivity)]},
	       'CatalystActivity' => {'attributes' => [qw(physicalEntity)]},
	       'Complex' => {'attributes' => [qw(hasComponent)]},
	       'Polymer' => {'attributes' => [qw(repeatedUnit)]},
	       'SimpleEntity' => {'attributes' => [qw(referenceEntity)]},
	       'EntityWithAccessionedSequence' => {'attributes' => [qw(referenceEntity)]},
	       'OpenSet' => {'attributes' => [qw(referenceEntity)]},	       
	       'EntitySet' => {'attributes' => [qw(hasMember)]},
	       'ReferenceEntity' => {'attributes' => [qw(crossReference)]}
	   },
	    -OUT_CLASSES => [qw(ReferenceEntity DatabaseIdentifier)]
	   );

my %ins2 = (-INSTRUCTIONS =>
	   {
	       'ConceptualEvent' => {'attributes' => [qw(hasSpecialisedForm)]},
	       'EquivalentEventSet' => {'attributes' => [qw(hasMember)]},
	       'Reaction' => {'attributes' => [qw(input catalystActivity)]},
	       'Event' => {'attributes' => [qw(goBiologicalProcess)], 'reverse_attributes' => [qw(hasComponent hasMember hasSpecialisedForm)]},
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

foreach my $r (@{$reactions}) {
    print $r->extended_displayName, "\n";
    my $new_i = GKB::Instance->new(-ONTOLOGY => $dba_to->ontology,
				   -CLASS => 'Reaction',
				   '_displayName' => $r->displayName);
    $new_i->db_id($r->db_id);
    $r->Species->[0] && $new_i->species_DB_ID(map {$_->db_id} @{$r->Species});
    $new_i->inflated(1);
    my $ar = $r->follow_class_attributes(%ins1);
    foreach my $i (@{$ar}) {
	if ($i->is_valid_attribute('identifier') && $i->Identifier->[0] && $i->ReferenceDatabase->[0]) {
	    $new_i->add_attribute_value_if_necessary
		('indirectIdentifier',
		 map {(/^\d+$/) ? $i->ReferenceDatabase->[0]->Name->[0] . ':' . $_: $_} @{$i->Identifier});
	}
	if ($i->is_valid_attribute('secondaryIdentifier') && $i->SecondaryIdentifier->[0]) {
	    $new_i->add_attribute_value_if_necessary('indirectIdentifier', @{$i->SecondaryIdentifier});
	}
	if ($i->is_valid_attribute('otherIdentifier') && $i->OtherIdentifier->[0]) {
	    $new_i->add_attribute_value_if_necessary('indirectIdentifier', @{$i->OtherIdentifier});
	}
    }
    $ar = $r->follow_class_attributes(%ins2);
    foreach my $i (@{$ar}) {
	$new_i->add_attribute_value_if_necessary('indirectIdentifier', 'GO:' . $i->Accession->[0]);
	if ($i->is_valid_attribute('ecNumber') && $i->EcNumber->[0]) {
	    $new_i->add_attribute_value_if_necessary('indirectIdentifier', @{$i->EcNumber});
	}
    }
    # Add this reaction's db_id and those of the orthologues
    $new_i->add_attribute_value('indirectIdentifier',$r->db_id);
    print join(',', @{$new_i->IndirectIdentifier}), "\n";
    $dba_to->store($new_i,1);
}

print STDERR "$0: no fatal errors.\n";

