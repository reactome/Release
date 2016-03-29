#!/usr/local/bin/perl
use strict;
use warnings;
use feature qw/state/;

#This script should be run over a release database as it requires stable identifiers to be present
#This script produces a tab delimited file for submission to goa - including Reactome annotations for cellular components, molecular function and biological process.

#NOTE: after running this script, run goa_submission_stats.pl to produce stats

use lib "/usr/local/gkb/modules";

use GKB::Config;
use GKB::Instance;
use GKB::DBAdaptor;
use GKB::Utils;

use autodie;
use Carp;
use Data::Dumper;
use File::Slurp;
use Getopt::Long;

# Database connection
our($user, $host, $pass, $port, $db, $date, $debug, $help);
GetOptions("help" => \$help);

if ($help) {
    print "Usage: $0 -user db_user -host db_host -pass db_pass -port db_port -db db_name -date date(YYYYMMDD) -debug\n";
    exit;
}    

GetOptions("user:s" => \$user, "host:s" => \$host, "pass:s" => \$pass, "port:i" => \$port, "db:s" => \$db, "date:i" => \$date, "debug" => \$debug);
my $parameters = {'user' => $user, 'host' => $host, 'pass' => $pass, 'port' => $port, 'db' => $db, 'date' => $date, 'debug' => $debug};

#$opt_date || die "Need date (-date).\n";  #need to revisit this, at present some instances don't have InstanceEdits attached, this should be fixed

my @rows; 			# Holds assertions to be printed
my %date; 			# Holds dates of assertions so they are not lost when the assertion is manipulated
my (@manual, @electronic); 	# Holds events that are manually/electronically inferred
my (%source, %elec_source); 	# Holds the events that infer the manual/experimental events

# Each event in Reactome is processed
foreach my $reaction_like_event (@{get_dba($parameters)->fetch_instance(-CLASS => 'ReactionlikeEvent')}) {
    #next unless $reaction_like_event->_doRelease->[0] && $reaction_like_event->_doRelease->[0] eq 'TRUE';
    
    #collect electronically inferred events to be handled further down
    if ($reaction_like_event->EvidenceType->[0] && ($reaction_like_event->EvidenceType->[0]->Name->[0] eq 'inferred by electronic annotation')) { #the plan is to run this script before the electronic inference is run, so this is here just in case it is run afterwards...
		foreach(@{$reaction_like_event->InferredFrom}) {
			push @{$elec_source{$reaction_like_event->db_id}}, $_;
		}
		push @electronic, $reaction_like_event;
		next;
    }
    
    #collect manually inferred events to be handled further down
    if ($reaction_like_event->InferredFrom->[0]) {  # Executes if the event is inferred
		foreach (@{$reaction_like_event->InferredFrom}) { # Executes for each place the event was inferred from
	    	push @{$source{$reaction_like_event->db_id}}, $_; # Stores the sources of inference with the event's id as the key
		}
		push @manual, $reaction_like_event; # Stores the events to be handled later
		next;
    }

    push @rows, process_cellular_compartments($reaction_like_event);
    push @rows, process_molecular_functions($reaction_like_event);
	push @rows, process_biological_processes($reaction_like_event);
} # End of event loop

open(my $output, ">", "gene_association.reactome");
print $output "!gaf-version: 2.1\n"; # Header line for GO

# Each output line is processed and written to the output file
foreach my $row (@rows) {
    if (replace_EXP_evidence_code_with_NAS($row)) {
		my $date = $date{$row};
		$row =~ s/EXP/NAS/; # Replace NAS code with EXP code
		$date{$row} = $date; #otherwise date is lost
    }
    
    print $output join("\t", $row, $date{$row}, 'Reactome') . "\n";  #the date has to be added after the check, otherwise duplicate entries are created simply due to different modification dates
}
close($output);

print "goa_submission.pl has finished its job\n";

sub get_dba {
    my $parameters = shift;
    
    return GKB::DBAdaptor->new(
        -user   => $parameters->{'user'} || $GKB::Config::GK_DB_USER,
        -host   => $parameters->{'host'} || $GKB::Config::GK_DB_HOST,
        -pass   => $parameters->{'pass'} || $GKB::Config::GK_DB_PASS,
        -port   => $parameters->{'port'} || $GKB::Config::GK_DB_PORT,
        -dbname => $parameters->{'db'} || $GKB::Config::GK_DB_NAME,
        -DEBUG => $parameters->{'debug'}
    );
}

sub replace_EXP_evidence_code_with_NAS {
    my $row = shift;
    
    foreach my $replacement_line (get_EXP_NAS_list()) {
        my ($uni, $go, $pmid) = split/\s+/, $replacement_line; # Obtain the uniprot id, go accession, and pubmed id for the current replacement line
        return 1 if $row =~ /$uni/ && $row =~ /$go/ && $row =~ /$pmid/;
    }
    return 0;
}

# We had feedback about a number of our submissions, stating that the literature reference provided didn't contain
# direct evidence for the GO assignment we have made. For these cases, the EXP evidence code needs to be changed to
# NAS, according to the following list:
sub get_EXP_NAS_list {
    state $EXP_NAS_list = [read_file("EXP_NAS_list.txt")];
    return @{$EXP_NAS_list};
}

sub get_row {
	return join("\t", @_);
}

sub process_cellular_compartments {
	my $event = shift;
	my $letter = 'C';
    
	my %seen;
    my @compartment_rows;
	foreach my $row (process_proteins({'event' => $event, 'letter' => $letter})) {
        push @compartment_rows, $row unless $seen{$row}++;
    }
    
    return @compartment_rows;
}

sub process_molecular_functions {
	my $event = shift;
    my $letter = 'F';
	
    my %seen;
    my @molecular_function_rows;
	CAT:foreach my $cat (@{$event->CatalystActivity}) { # Obtains the entities with catalyst activity in the current event						 	           
	  	#Note: This logic checks complexes and intentionally excludes heteromeric Complexes and Complexes with EntitySets as components,
        # as it wouldn't be known which of the components actually is the active one - deal with them separately?

		foreach my $row (process_proteins({'event' => $event, 'catalyst_activity' => $cat, 'letter' => $letter})) {
            push @molecular_function_rows, $row unless $seen{$row}++;
        }
	} # End of catalyst block
    return @molecular_function_rows;
}

sub process_biological_processes {
	my $event = shift;
    my $letter = 'P';
	    
	my %seen;
    my @biological_process_rows;	
	foreach my $row (process_proteins({'event' => $event, 'letter' => $letter})) {
		push @biological_process_rows, $row unless $seen{$row}++;
	}
	
    return @biological_process_rows;
}

sub process_proteins {
    my $parameters = shift;
    
    # Potential proteins
    my $proteins = $parameters->{'catalyst_activity'} ?
        [get_proteins_from_catalyst_activity($parameters->{'catalyst_activity'})] :
        find_proteins($parameters->{'event'});
    
    my %rows;
	foreach my $protein (@{$proteins}) {
		process_protein($protein, $parameters, \%rows);
	}
    
    return keys %rows;
}

sub process_protein {
	my $protein = shift;
    my $parameters = shift;
	my $rows = shift || {};
    
    # The 'return unless' statements here skip processing the protein unless
	# it meets the conditions specified
	return unless $protein->is_a('EntityWithAccessionedSequence');
	return unless $protein->ReferenceEntity->[0]; # Must have a reference entity
    return unless $protein->ReferenceEntity->[0]->ReferenceDatabase->[0];
    return unless ($protein->ReferenceEntity->[0]->ReferenceDatabase->[0]->Name->[0] eq 'UniProt'); # Must be referencing UniProt
	return unless $protein->Species->[0]; # Must associate with a species
	return unless $protein->Species->[0]->CrossReference->[0]; # Must have access to the species name 
    
    foreach my $row (get_rows_from_protein($protein, $parameters)) {
        return if exists $rows->{$row};
        get_date(get_instance_with_accession($protein, $parameters), $row);
        $rows->{$row} = undef;
    }
}

sub get_rows_from_protein {
    my $protein = shift;
    my $parameters = shift;
    
    my $event = $parameters->{'event'};
    return unless $event;
    my $ontology_letter = $parameters->{'letter'};
    return unless $ontology_letter;
    my $catalyst_activity = $parameters->{'catalyst_activity'};
	
    my $DB = 'UniProtKB'; # Information on proteins is from UnitProtKB
	my $uni = $protein->ReferenceEntity->[0]->Identifier->[0]; # Obtains UniProt ID
	my $object_symbol = assign_object_symbol($protein->ReferenceEntity->[0]); # Obtains alternate UniProt ID

	my $go_accessions = get_annotation_dispatch_table()->{$ontology_letter}->{'GO_accession'}->(get_instance_with_accession($protein, $parameters));
    return unless $go_accessions;
	
    my $object_type = 'protein'; # The entries always concern proteins
                                # (note: should check with GO to determine
                                # if they look at rRNA/tRNA genes)
    		
    my $taxon = get_taxon($protein); # Species ID (by taxon number) obtained
    return unless $taxon;
    return if $taxon ~~ get_microbial_species_to_exclude();
	
    my $reference_record = get_prot_reference($catalyst_activity);
    my @rows;
    foreach my $go (@{$go_accessions}) {
        my $accession = $go->{'accession'};
        next unless $accession;
        
        my $event_with_accession = $go->{'event'} || $event;
        
        my $evidence_code = get_annotation_dispatch_table()->{$ontology_letter}->{'evidence_code'}->($reference_record);
        
        push @rows, get_row($DB, $uni, $object_symbol, '', "GO:$accession", $reference_record->[1] || get_reaction_id($event_with_accession), $evidence_code, '', $ontology_letter, '', '', $object_type, "taxon:$taxon");
    }
    return @rows;
}

sub get_instance_with_accession {
    my $protein = shift;
    my $parameters = shift;
    
    my $ontology_letter = $parameters->{'letter'};
    
    return $ontology_letter eq 'F' ? $parameters->{'catalyst_activity'} :
           $ontology_letter eq 'P' ? $parameters->{'event'} :
           $ontology_letter eq 'C' ? $protein :
           undef;
}

sub get_reaction_id {
	my $event = shift;
	
	return unless $event->StableIdentifier->[0];
	return "REACTOME:" . $event->StableIdentifier->[0]->Identifier->[0];
}

sub get_proteins_from_catalyst_activity {
    my $catalyst_activity = shift;
    
    my @errors = check_catalyst_activity($catalyst_activity);
    if (@errors) {
        print(join("\n", @errors) . "\n");
        return;
    }          
    
    return get_proteins_from_physical_entity($catalyst_activity->activeUnit->[0]) if $catalyst_activity->activeUnit->[0];
    return get_proteins_from_physical_entity($catalyst_activity->physicalEntity->[0]);
}

sub check_catalyst_activity {
    my $catalyst_activity = shift;
    
    my @errors;
    
    my $catalyst_id = $catalyst_activity->displayName . ' (' . $catalyst_activity->db_id . ')';
    my $physical_entity = $catalyst_activity->physicalEntity->[0];
    my @active_units = @{$catalyst_activity->activeUnit};
    
    push @errors, "No physical entity: $catalyst_id" if (!$physical_entity);
    push @errors, "No compartment for physical entity: $catalyst_id" unless $physical_entity && $physical_entity->compartment->[0];
    push @errors, "No active unit and physical entity is a complex with multiple, distinct proteins: $catalyst_id"
        if (scalar @active_units == 0) && (scalar @{find_rps($catalyst_activity)} > 1) && $physical_entity->is_a('Complex');
    push @errors, "Active unit is a complex: $catalyst_id" if (scalar @active_units == 1) && $active_units[0]->is_a('Complex');
    push @errors, "Multiple active units: $catalyst_id" if scalar @active_units > 1;
    
    return @errors;
}

sub get_proteins_from_physical_entity {
	my $physical_entity = shift;
	
    return unless $physical_entity;
        
	my @proteins = ();
    if ($physical_entity->is_a('Complex') || $physical_entity->is_a('EntitySet')) {
        foreach my $sub_element (@{$physical_entity->hasMember}, @{$physical_entity->hasComponent}) {
            my @proteins_from_complex_or_set = get_proteins_from_physical_entity($sub_element);
            push @proteins, @proteins_from_complex_or_set if @proteins_from_complex_or_set;
        }        
    } elsif ($physical_entity->is_a('EntityWithAccessionedSequence')) { # If the entity is a protein
		push @proteins, $physical_entity;
	}
	
	return @proteins;
}
	
sub get_taxon {
	my $protein = shift;
	
    return unless $protein;
	return unless $protein->Species->[0];
	return unless $protein->Species->[0]->CrossReference->[0];
	
	return $protein->Species->[0]->CrossReference->[0]->Identifier->[0];
}
	
sub get_annotation_dispatch_table {
	return {
		'C' => {
			'GO_accession' => sub {
				my $protein = shift;
				
                return unless $protein;
                                
				if (get_taxon($protein) ~~ get_species_with_alternate_go_compartment()) {
					return unless ($protein->GO_CellularComponent->[0]);
					return [
                        {
                            'accession' => $protein->GO_CellularComponent->[0]->Accession->[0],
                            'event' => undef
                        }
                    ];
				}
				
				return unless $protein->Compartment->[0]; # Must have a cellular compartment
				return [
                    {
                        'accession' => $protein->Compartment->[0]->Accession->[0],
                        'event' => undef
                    }
                ];
			},
			'evidence_code' => sub {
				return 'TAS';
			}
		},
		'F' => {
			'GO_accession' => sub {
				my $cat = shift;
				
                return unless $cat;
				return unless $cat->Activity->[0]; # Skip unless it has molecular function
				return [
                    {
                        'accession' => $cat->Activity->[0]->Accession->[0],
                        'event' => undef
                    }
                ];
			},
			'evidence_code' => sub {
				my $reference = shift;
                
                return $reference->[0];
			}
		},
        'P' => {
            'GO_accession' => sub {
                my $event = shift;
                return unless $event;
                
                my @biological_process_accessions;
                get_biological_process_accessions($event, \@biological_process_accessions);
                return \@biological_process_accessions;
            },
            'evidence_code' => sub {
                return 'TAS';
            }
        }
	};
}

sub get_biological_process_accessions {
    my $event = shift;
    my $biological_process_accessions = shift;
    my $recursion_depth = shift // 0;
    
    return if $recursion_depth > 2;
    
    if ($event->GoBiologicalProcess->[0]) {    
        push @{$biological_process_accessions}, {
            'accession' => $event->GoBiologicalProcess->[0]->Accession->[0],
            'event' => $event
        }
    } else {
        $recursion_depth++;
        foreach my $parent (@{$event->reverse_attribute_value('hasEvent')}) {
            get_biological_process_accessions($parent, $biological_process_accessions, $recursion_depth);
        }
    }
}

# Removes white space
sub trim {
	my $name = shift;
	$name =~ s/^ +//;
	$name =~ s/ +$//;
	return $name;
}

sub get_prot_reference {
	my $prot = shift;
    
    return unless $prot;

    my @pubmed_identifiers = map { "PMID:" . $_->PubMedIdentifier->[0] } (@{$prot->LiteratureReference});
	return ["EXP", join('|', @pubmed_identifiers)];
}

sub check_reference {  #if a reaction has only one lit-ref, the code used is EXP plus the pubmed id. If more than one or no litrefs are attached, the code is TAS and the Reactome stable id is given as reference
    my ($ev) = @_;
    if ($ev->LiteratureReference->[1] || !$ev->LiteratureReference->[0]) {
		return "TAS", "REACTOME:".$ev->StableIdentifier->[0]->Identifier->[0];
		return "TAS", get_stable_identifier($ev);
    } else {
		my $pubmed = $ev->LiteratureReference->[0]->PubMedIdentifier->[0];
		$pubmed || return "TAS", "REACTOME:".$ev->StableIdentifier->[0]->Identifier->[0]; #this is probably a book reference that doesn't have a pubmed id - use TAS evidence code for now
		return "EXP", "PMID:".$pubmed;
    }
}

=head
Obs - only needed for the first time
sub get_stable_identifier {
    my ($ev) = @_;
    my $ev2 = $dbc->fetch_instance_by_db_id($ev->db_id, $ev->class)->[0];
    return $ev2->StableIdentifier->[0]->Identifier->[0].".".$ev2->StableIdentifier->[0]->IdentifierVersion->[0];
}
=cut

sub find_proteins {
    my ($rxn) = @_;
    return $rxn->follow_class_attributes(
		-INSTRUCTIONS => {
			'Pathway' => {'attributes' => [qw(hasEvent)]},
			'ReactionlikeEvent' => {'attributes' => [qw(input output catalystActivity)]},
			'Reaction' => {'attributes' => [qw(input output catalystActivity)]},
			'CatalystActivity' => {'attributes' => [qw(physicalEntity)]},
			'Complex' => {'attributes' => [qw(hasComponent)]},
			'EntitySet' => {'attributes' => [qw(hasMember)]},
			'Polymer' => {'attributes' => [qw(repeatedUnit)]}
		},
        -OUT_CLASSES => [qw(EntityWithAccessionedSequence)]
	);
}

sub find_complexes {
    my ($rxn) = @_;
    return $rxn->follow_class_attributes(
		-INSTRUCTIONS => {
			'Pathway' => {'attributes' => [qw(hasEvent)]},
			'ReactionlikeEvent' => {'attributes' => [qw(input output catalystActivity)]},
			'Reaction' => {'attributes' => [qw(input output catalystActivity)]},
			'CatalystActivity' => {'attributes' => [qw(physicalEntity)]},
			'EntitySet' => {'attributes' => [qw(hasMember)]},
			'Polymer' => {'attributes' => [qw(repeatedUnit)]}},
		-OUT_CLASSES => [qw(Complex)]
	);		
}

sub find_rps {
    my ($ev) = @_;
#this ignores candidates in CandidateSets - may need to revisit
    return $ev->follow_class_attributes(
		-INSTRUCTIONS => {
			'Pathway' => {'attributes' => [qw(hasEvent)]},
			'ReactionlikeEvent' => {'attributes' => [qw(input output catalystActivity)]},
			'CatalystActivity' => {'attributes' => [qw(physicalEntity)]},
			'Complex' => {'attributes' => [qw(hasComponent)]},
			'EntitySet' => {'attributes' => [qw(hasMember)]},
			'Polymer' => {'attributes' => [qw(repeatedUnit)]},
			'EntityWithAccessionedSequence' => {'attributes' => [qw(referenceEntity)]}
		},
		-OUT_CLASSES => [qw(ReferenceGeneProduct)]
	);
}

sub find_gee {
    my ($ev) = @_;
    my $gee_ar = $ev->follow_class_attributes(
		-INSTRUCTIONS => {
			'Pathway' => {'attributes' => [qw(hasEvent)]},
            'ReactionlikeEvent' => {'attributes' => [qw(input output catalystActivity)]},
            'CatalystActivity' => {'attributes' => [qw(physicalEntity)]},
            'Complex' => {'attributes' => [qw(hasComponent)]},
            'EntitySet' => {'attributes' => [qw(hasMember)]},
			'CandidateSet' => {'attributes' => [qw(hasCandidate)]},
			'Polymer' => {'attributes' => [qw(repeatedUnit)]}
		},
        -OUT_CLASSES => [qw(GenomeEncodedEntity)]
	);
	#need to restrict to those instances that actually are GEEs, not subclasses of GEE
    my @tmp;
    foreach my $gee (@{$gee_ar}) {
		if ($gee->class eq 'GenomeEncodedEntity') {
		    push @tmp, $gee;
		}
    }
    return \@tmp;
}

sub assign_object_symbol {
    my ($rps) = @_;
    
	if ($rps->SecondaryIdentifier->[0]) {
		return $rps->SecondaryIdentifier->[0];
    } elsif ($rps->GeneName->[0]) {
		return $rps->GeneName->[0];
    } else {
		return $rps->Identifier->[0];
    }
}

sub get_date {
    my ($i, $row) = @_;
    my $instance_date;
    
    eval {
        if ($i->Modified->[0]) {
        	$instance_date = $i->Modified->[-1]->DateTime->[0];
        } elsif ($i->Created->[0]) {
        	$instance_date = $i->Created->[0]->DateTime->[0];
        } else { #keep this just in case, even though old CatalystActivities without created or modified slots have been sorted out by now
        	$instance_date = $date;
        }
    };
    if ($@) {
        confess Dumper $i;
    }
    
	$instance_date =~ s/-//g;
    $instance_date =~ s/\s.+//;
    unless ($date{$row} && ($date{$row} > $instance_date)) { #don't touch if the already assigned date is more recent than the date for this particular instance
		$date{$row} = $instance_date;
    }
}

sub get_species_with_alternate_go_compartment {
	return (11676, 211044, 1491);
}

sub get_microbial_species_to_exclude {
	return (813, 562, 491, 90371, 1280, 5811);
}