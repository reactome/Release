#!/usr/local/bin/perl -w
use strict;
use Carp;
#This script infers reactions from one species to another based on a file of homologue pairs. The implementation at present is based on the Ensembl compara orthologue mapping, but the script can easily be adapted by adding a method returning the homologue hash. The orthopair file for the Ensembl compara system can be prepared by running the script  prepare_orthopair_files.pl under GKB/scripts/compara.
#After the reactions have been inferred, the higher-level event hierarchy is also created based on the from-species.

#From and to species can be specified, the default from species is human (hsap).

#A threshold can be applied for complex inference rules. The threshold indicates that at least this percentage of proteins in a complex must have an orthologue in order for the complex to be inferred.

#It's also possible to restrict the number of paralogues in the to-species to a defined number by using -filt.

#One can also limit inference to specific Events by giving the internal id of the upstream event(s) on the command line. Inference will then be performed for these Events and all their downstream Events.


use lib "$ENV{HOME}/bioperl-1.0";
use lib "$ENV{HOME}/GKB/modules";

use GKB::DBAdaptor;
use GKB::Instance;
use GKB::Utils_esther;
use GKB::Utils;
use GKB::Config;
use GKB::Config_Species;

use autodie;
use Data::Dumper;
use Getopt::Long;
use DBI;
use List::MoreUtils qw/any/;

use Log::Log4perl qw/get_logger/;
Log::Log4perl->init(\$LOG_CONF);
my $logger = get_logger(__PACKAGE__);

$logger->info("starting\n");

@ARGV || $logger->error_die("Usage: $0 -user db_user -host db_host -pass db_pass -port db_port -db db_name -r reactome_release -from from_species_name(e.g. hsa) -sp to_species_name(e.g.dme) -filt second_taxon_filter -thr threshold_for_complex");

our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db, $opt_r, $opt_from, $opt_sp, $opt_release_date, $opt_filt, $opt_thr, $opt_debug);

&GetOptions(
    "user:s",
	"host:s",
	"pass:s",
	"port:i",
	"db=s",
	"r=i",
	"from=s",
	"sp=s",
    "release_date:s",
	"filt=i",
	"thr=i",
	"debug",
);

$opt_db || $logger->error_die("Need database name (-db).\n");
$opt_r || $logger->error_die("Need Reactome release number, e.g. -r 32\n");
$opt_sp || $logger->error_die("Need species (-sp), e.g. mmus.\n");
$opt_release_date || $logger->error_die("Need release date e.g. -release_date 2017-12-13");
$opt_from ||= 'hsap';

$logger->info("opt_db=$opt_db\n");

#connection to Reactome
my $dba = GKB::DBAdaptor->new(
    -dbname => $opt_db,
    -user   => $opt_user,         
    -host   => $opt_host,
    -pass   => $opt_pass,
    -port   => $opt_port || 3306,
    -driver => 'mysql',
    -DEBUG => $opt_debug
);

my $protein_class = &GKB::Utils::get_reference_protein_class($dba); #determines whether the database is in the pre-March09 schema with ReferencePeptideSequence, or in the new one with ReferenceGeneProduct and ReferenceIsoform

$logger->info("protein_class=$protein_class\n");

######################################################################
#These variables should be edited according to the method employed
######################################################################

my $orthopairs = "/usr/local/gkbdev/scripts/release/orthopairs";

my $file = "$orthopairs/$opt_r\/$opt_from\_$opt_sp\_mapping.txt";
$file || die "Can't find $file.\n";
my $ensg_file = "$orthopairs/$opt_r\/$opt_sp\_gene_protein_mapping.txt";
$ensg_file || die "Can't find $ensg_file.\n";
my $note = "inferred events based on ensembl compara"; #added to InstanceEdit
my $summation_text = "This event has been computationally inferred from an event that has been demonstrated in another species.<p>The inference is based on the homology mapping in Ensembl Compara. Briefly, reactions for which all involved PhysicalEntities (in input, output and catalyst) have a mapped orthologue/paralogue (for complexes at least $opt_thr\% of components must have a mapping) are inferred to the other species. High level events are also inferred for these events to allow for easier navigation.<p><a href='/electronic_inference_compara.html' target = 'NEW'>More details and caveats of the event inference in Reactome.</a> For details on the Ensembl Compara system see also: <a href='http://www.ensembl.org/info/docs/compara/homology_method.html' target='NEW'>Gene orthology/paralogy prediction method.</a>";
my $complex_text = 'This complex/polymer has been computationally inferred (based on Ensembl Compara) from a complex/polymer involved in an event that has been demonstrated in another species.';
my @evidence_names = ('inferred by electronic annotation', 'IEA');
my $instance_edit = GKB::Utils_esther::create_instance_edit($dba, 'Schmidt', 'EE', $note);
my $int_ar = $dba->fetch_instance_by_attribute('GO_CellularComponent', [['name', ['intracellular'],0]]);
my $intra = $int_ar->[0]; #will be assigned to inferred cytosolic, nuclear etc. entities in bacteria as this distinction doesn't exist
my $from_name = $species_info{$opt_from}->{'name'}->[0];
########

#get homologue assignments
my $hr = read_orthology($file); #based on Ensembl Compara
my %homologue = %{$hr};

#get ensg mapping for protein identifiers - this is used to fill the referenceGene slot in newly created ReferenceGeneProduct instances
my $hr_ensg = read_ensg_mapping($ensg_file); #based on BioMart
my %ensg = %{$hr_ensg};

#prepare relevant ReferenceDatabases (UniProt as default, Ensembl db as specified in Species_Config.pm)
my $uni_db = $dba->fetch_instance_by_attribute('ReferenceDatabase', [['name', ['UniProt'],0]])->[0];
$logger->info("UniProt ReferenceDatabase.extended_displayName=" . $uni_db->extended_displayName . "\n");

my $ens_db = create_instance('ReferenceDatabase');
$ens_db->Name('Ensembl', "ENSEMBL_$species_info{$opt_sp}->{'name'}->[0]\_PROTEIN");
$ens_db->Url($species_info{$opt_sp}->{'refdb'}->{'url'});
$ens_db->AccessUrl($species_info{$opt_sp}->{'refdb'}->{'access'});
$ens_db = check_for_identical_instances($ens_db);

$logger->info("ENSEMBL ReferenceDatabase.extended_displayName=" . $ens_db->extended_displayName . "\n");

my $ensg_db = create_instance('ReferenceDatabase');
$ensg_db->Name('ENSEMBL', "ENSEMBL_$species_info{$opt_sp}->{'name'}->[0]\_GENE");
$ensg_db->Url($species_info{$opt_sp}->{'refdb'}->{'url'});
$ensg_db->AccessUrl($species_info{$opt_sp}->{'refdb'}->{'ensg_access'});
$ensg_db = check_for_identical_instances($ensg_db);

#create alternative ReferenceDatabase (e.g. SGD for S.cerevisiae, etc)
my $alt_refdb;
if ($species_info{$opt_sp}->{'alt_refdb'}) {
    $alt_refdb = create_instance('ReferenceDatabase');
    $alt_refdb->Name($species_info{$opt_sp}->{'alt_refdb'}->{'dbname'}->[0]);
    $alt_refdb->Url($species_info{$opt_sp}->{'alt_refdb'}->{'url'});
    $alt_refdb->AccessUrl($species_info{$opt_sp}->{'alt_refdb'}->{'access'});
    $alt_refdb = check_for_identical_instances($alt_refdb);
}

$logger->info("about to prepare static instances\n");

#prepare 'static' instances
#Summation used for all newly created or modified instances during orthology inference
my $summation = create_instance('Summation');
$summation->Text($summation_text);
$summation = check_for_identical_instances($summation);

my $summation_complex = create_instance('Summation');
$summation_complex->Text($complex_text);
$summation_complex = check_for_identical_instances($summation_complex);

my $evidence_type = create_instance('EvidenceType');
$evidence_type->Name(@evidence_names);
$evidence_type = check_for_identical_instances($evidence_type);

#taxon that reactions are inferred *to*
my $taxon = create_instance('Species');
$taxon->Name(@{$species_info{$opt_sp}->{'name'}});
$taxon = check_for_identical_instances($taxon);
$logger->info("taxon=" . $taxon->extended_displayName . "\n");

#get instance for *from* Taxon
my $source_species = $dba->fetch_instance(-CLASS => 'Species',
					  -QUERY => [{-ATTRIBUTE => 'name',
						      -VALUE => [$species_info{$opt_from}->{'name'}->[0]]}
						     ]
					  )->[0];
$logger->info("dba=$dba\n");
$logger->info("opt_from=$opt_from\n");

$source_species || $logger->error_die("can't find source species instance for $species_info{$opt_from}->{'name'}->[0], aborting!\n");


##################
####code begins###
##################

open(my $report, ">>", "$opt_r\/report_ortho_inference_$opt_db\.txt");
open(my $regulator, ">>", "$opt_r\/cyclical_reactions_$opt_db\.txt");
open(my $inf, ">", "$opt_r\/inferred_$opt_sp\_$opt_thr\.txt");
open(my $eli, ">", "$opt_r\/eligible_$opt_sp\_$opt_thr\.txt");
open(my $manual, ">", "$opt_r\/skip_manual_inferred_human_events_$opt_sp");

my (%uni, %orthologous_entity, %inferred_cp, %inferred_gse, %homol_gee, %seen_rps, %inferred_event, %being_inferred, %homol_cat, %instances);
my $a =("#"x20)."\n";
my @inferrable_human_events;
my @manual_human_events;
my %manual_event_to_non_human_source;
my $count_leaves = 0;
my $count_inferred_leaves = 0;

#define reactions to be considered for inference (by default all ReactionlikeEvents)
my $reaction_ar;
if (@ARGV) { #list of event ids, for which downstream reactions should be inferred - NOTE : make sure these events are from the source species
	$reaction_ar = get_reaction_instances($dba, @ARGV);
} else { #get all ReactionlikeEvents for the target species by default
	$reaction_ar = $dba->fetch_instance(-CLASS => 'ReactionlikeEvent',
					    -QUERY => [{-ATTRIBUTE => 'species',
							-VALUE => [$source_species->db_id]}
						       ]
					    );
}
    
my $complex_ar = $dba->fetch_instance(-CLASS => 'Complex',
					  -QUERY => [{-ATTRIBUTE => 'species',
						      -VALUE => [$source_species->db_id]}
						    ]
					);

my $ewas_ar = $dba->fetch_instance(-CLASS => 'EntityWithAccessionedSequence',
				       -QUERY => [{-ATTRIBUTE => 'species',
						   -VALUE => [$source_species->db_id]}
						  ]
					);

foreach my $attribute ("input", "output", "catalystActivity") {
    $dba->load_class_attribute_values_of_multiple_instances('ReactionlikeEvent', $attribute, $reaction_ar);
}

$dba->load_class_attribute_values_of_multiple_instances('Complex', 'hasComponent', $complex_ar);
$dba->load_class_attribute_values_of_multiple_instances('Complex', 'compartment', $complex_ar);

foreach my $attribute ("compartment", "species", "referenceEntity") {
    $dba->load_class_attribute_values_of_multiple_instances('EntityWithAccessionedSequence', $attribute, $ewas_ar);
}

#start looping over all events to be considered for inference
foreach my $rxn (@{$reaction_ar}) {
    $logger->info("considering reaction DB_ID=" . $rxn->db_id);

#checks whether the event exists already in the target species - two scenarios
#are possible: the from-species event was itself inferred from the target
#species event, or the event exists in both source and target species as
#orthologousEvent, but there is evidence for both and therefore the events are
#not inferred from each other. As all events that are inferred from another and
#have a different species (as opposed to inference within a species) should
#have the orthologousEvent slot filled in for both species, it would be
#sufficient to check for orthologous events. However, it was necessary to include
#the InferredFrom attribute here as not all of these reactions have the
#'orthologousEvent' slot filled in - should maybe addressed by a 'filler script'
#(this is now available in QA_collection.pm)
    my @tmp = grep {$_->Species->[0]->db_id == $taxon->db_id && !(is_chimeric($_))} (@{$rxn->OrthologousEvent}, @{$rxn->InferredFrom});
    $logger->info("scalar(tmp)=" . scalar(@tmp) . "\n");
    if ($tmp[0]) { #the event exists in the other species, need not be inferred but should be kept for the step when the event hierarchy is created, so that it can be fit in at the appropriate position in the event hierarchy
        unless ($tmp[0]->disease->[0]) {        
            $manual_event_to_non_human_source{$rxn} = $tmp[0]; #disregards multiple events here..., the first event is taken into the hash to allow building the event structure further down 
            push @manual_human_events, $rxn;
        } else {
            $logger->info("Skipping building of hierarchy around pre-existing disease reaction " .
                          $tmp[0]->displayName . " (" . $tmp[0]->db_id . ")");
        }
        next;
    }
    $logger->info("infer event for reaction=" . $rxn->extended_displayName . "\n");
    infer_event($rxn, $opt_release_date);
}

#creating hierarchy structure as in human event
my %seen;
foreach (@inferrable_human_events){
    next if $seen{$_}++;
    create_orthologous_generic_event($_, $opt_release_date);
}

#fill pathways and blackboxevents with their components in the same order as in human
my %seen3;
foreach my $hum_pathway (@inferrable_human_events){
    next if $seen3{$hum_pathway}++;
    next unless $hum_pathway->is_valid_attribute('hasEvent'); #This should happen for both Pathways and BlackBoxEvents
    $logger->info("filling pathway name=" . $hum_pathway->extended_displayName . "\n");
    my @pathway_comps;
    foreach my $path_comp (@{$hum_pathway->HasEvent}) {
        #print STDERR $path_comp->extended_displayName, "\n";
        if ($inferred_event{$path_comp}) {
            push @pathway_comps, $inferred_event{$path_comp};
        }
    }
    if ($inferred_event{$hum_pathway}->is_valid_attribute('hasEvent')) {
    	$inferred_event{$hum_pathway}->HasEvent;
    	$inferred_event{$hum_pathway}->add_attribute_value_if_necessary('hasEvent', @pathway_comps);
    	$dba->update_attribute($inferred_event{$hum_pathway}, 'hasEvent');
    } else {
    	$logger->info($hum_pathway->extended_displayName. " and ". $inferred_event{$hum_pathway}->extended_displayName . " (likely connected via manual inference) have different classes.\n");;
    }
}
$logger->info("inferring preceding events.....\n");
infer_preceding_events(@inferrable_human_events);
#finally mark all human events with orthologous events as modified
my %seen4;
foreach my $hum_event (@inferrable_human_events){
    next if $seen4{$hum_event}++;
    GKB::Utils_esther::update_modified_if_necessary($hum_event, $instance_edit, $dba);
}
#print report
my $perc = 0;
if ($count_inferred_leaves == $count_leaves) {
	$perc = 100;
} elsif ($count_leaves != 0) {
	$perc = (100 * $count_inferred_leaves) / $count_leaves;
}
my $warning = "";
if (!(defined $count_inferred_leaves)) {
	$warning .= " count_inferred_leaves is undef;";
}
if (!(defined $count_leaves)) {
	$warning .= " count_leaves is undef;";
}
if ($count_leaves == 0) {
	$warning .= " count_leaves == 0;";
}
print $report $opt_from, " to ",$opt_sp, "\t", $count_inferred_leaves, " out of ", $count_leaves, " eligible reactions (", $perc, "%)$warning\n";

foreach my $manual_event (@manual_human_events) {
    print $manual $manual_event->displayName . ' (' . $manual_event->db_id . ') was skipped for ' . $opt_sp . "\n";

    my $non_human_event = $manual_event_to_non_human_source{$manual_event};
    
    print $manual $manual_event->displayName . ' (' . $manual_event->db_id . ') inferred from ' .
                  $non_human_event->displayName . ' (' . $non_human_event->db_id . ")\n";
}

close($report);
close($regulator);
close($inf);
close($eli);
close($manual);

$logger->info("end\n");



sub get_skip_list {
    # exclude selected pathways from inference
    my @list;
    foreach my $pwy_id (162906, 168254, 977225) {  #human-viral pathways, amyloids
        push @list, get_reaction_ids($pwy_id);  #inference is done on reaction level, therefore extract all downstream reactions and store them in @list.
    }
    
    # exclude
    open(my $skip_list_fh, '<', 'normal_event_skip_list.txt');
    while (my $reaction_db_id = <$skip_list_fh>) {
    	chomp $reaction_db_id;
    	push @list, $reaction_db_id;
    }
    close $skip_list_fh;
    
    return @list;
}

#creates and returns an instance of a given class, ready to be used and with the appropriate InstanceEdit in the 'created' slot
sub create_instance {
    my ($class) = @_;
    my $i = GKB::Instance->new(-ONTOLOGY=>$dba->ontology,
                               -CLASS=>$class);
    $i->inflated(1);
    $i->Created($instance_edit);
    return $i;
}

#retrieves all downstream reaction ids for a specified pathway id
#returns an array of ids
sub get_reaction_ids {
    my ($pwy_id) = @_;
    my @tmp;
    my $ar = $dba->fetch_instance_by_db_id($pwy_id);
    
    if ($ar->[0]) {
        my $ar2 = $ar->[0]->follow_class_attributes(-INSTRUCTIONS => 
						    {'Pathway' => {'attributes' => [qw(hasEvent)]}},
						    -OUT_CLASSES => [qw(ReactionlikeEvent)]);
        foreach my $rxn (@{$ar2}) {
            push @tmp, $rxn->db_id;
        }
    }
    return @tmp;
}

#retrieves all downstream reaction instances for a list of event ids
#returns an array ref
sub get_reaction_instances {
    my ($dba, @id) = @_;
    my @tmp;
    foreach my $id (@id) {
        my $event_ar = $dba->fetch_instance_by_db_id($id);
        my $ar = $event_ar->[0]->follow_class_attributes(-INSTRUCTIONS => 
							 {'Pathway' => {'attributes' => [qw(hasEvent)]},
							  'BlackBoxEvent' => {'attributes' => [qw(hasEvent)]}},
							  -OUT_CLASSES => [qw(ReactionlikeEvent)]);
        push @tmp, @{$ar};
    }
    return \@tmp;
}

sub is_chimeric {
    my ($rxn) = @_;
    
    return $rxn->isChimeric->[0] && $rxn->isChimeric->[0] eq 'TRUE'; 
}

sub get_species_from_reaction_like_event_entities {
	my $event = shift;
	
	my @entities = get_physical_entities_in_reaction_like_event($event);
	
	my %species;
	foreach my $entity (@entities) {
		foreach my $sp (@{$entity->species}) {
			$species{$sp->db_id} = $sp;
		}
	}
	
	return values %species;
}

sub get_physical_entities_in_reaction_like_event {
    my $reaction_like_event = shift;

    my $logger = get_logger(__PACKAGE__);

    my @physical_entities;
    push @physical_entities, @{$reaction_like_event->input};
    push @physical_entities, @{$reaction_like_event->output};
    push @physical_entities, map($_->physicalEntity->[0], @{$reaction_like_event->catalystActivity});
    
    my @regulations = @{$reaction_like_event->regulatedBy};
    my @regulators = map {@{$_->regulator}} @regulations;
    push @physical_entities, grep {$_->is_a('PhysicalEntity')} @regulators;
    push @physical_entities, map {$_->physicalEntity->[0]} grep {$_->is_a('catalystActivity')} @regulators;
    
    my %physical_entities = map {$_->db_id => $_} grep {$_} @physical_entities;
    
    # ...and all the sub-components of this PE
    for my $pe (values %physical_entities) {
		my @subs = recurse_physical_entity_components($pe);
		for my $sub (@subs) {
			$sub or next;
			#$logger->info("Adding sub component ".join(' ',$sub->class,$sub->displayName));
			$physical_entities{$sub->db_id} = $sub;
		}
    }

    return values %physical_entities;
}

# Recurse through all members/components so all descendent PEs will also
# be linked to the reaction/pathway
sub recurse_physical_entity_components {
    my $pe = shift;

    my %components = map {$_->db_id => $_} grep {$_} @{$pe->hasMember}, @{$pe->hasComponent}, @{$pe->repeatedUnit};
    keys %components || return ();
    
    for my $component (values %components) {
		next unless $component->is_a('EntitySet') || $component->is_a('Complex') || $component->is_a('Polymer');
		for my $sub_component (recurse_physical_entity_components($component)) { 
			$components{$sub_component->db_id} = $sub_component;
		}
    }

    return values %components;
}

#This method reads an orthopair file (in the format 'from_species tab to_species(=list separated by space)'), and returns a hash reference 'homologue'.
sub read_orthology {
    my ($file) = @_;
    
    my $logger = get_logger(__PACKAGE__);
    
    my %homologue;
    $logger->info("Now reading orthology mapping file: $file\n");
    if (open(my $read_orthopair, '<', $file)) {
	    while (<$read_orthopair>) {
            my %seen_to;
            my ($from, $tos) = split/\t/, $_;
            my @tos = split/\s/, $tos;
            foreach my $to (@tos) {
                $seen_to{$to}++;
            }
            push @{$homologue{$from}}, keys %seen_to;
	    }
	    close($read_orthopair);
    } else {
    	$logger->error("Could not open file: $file\n");
    }
    return \%homologue;
}

#This method reads the gene-protein mapping file for the target species (tab delimited, multiple protein ids separated by space), and returns a hash reference 'ensg'.
sub read_ensg_mapping {
    my ($file) = @_;
    
    my $logger = get_logger(__PACKAGE__);
    
    my %ensg;
    $logger->info("Now reading ensg mapping file: $file\n");
    if (open(my $read, '<', $file)) {
	    while (<$read>) {
	        my ($ensg, $protein_ids) = split/\t/, $_;
	        my @protein_ids = split/\s/, $protein_ids;
	        foreach my $id (@protein_ids) {
                $id =~ s/\w+://;  #remove database prefix
                push @{$ensg{$id}}, $ensg;
	        }
	    }
	    close($read);
    } else {
    	$logger->error("Could not open file: $file\n");
    }
    return \%ensg;
}

#Argument: EntitySet to be inferred
#returns an EntitySet instance if inference is successful, or undef if unsuccessful. If the inference is successful, but the resulting Set would have only one member, the inferred member instance is returned rather than a Set instance. 
#When the override flag is set to true and the inference is unsuccessful, a GenomeEncodedEntity instance (ghost) is returned. 
sub infer_gse {
    my ($gse, $override) = @_;
    $inferred_gse{$gse} && return $inferred_gse{$gse}; #return cached inferred set instance if it exists
    
    my $logger = get_logger(__PACKAGE__);

    my @members = @{infer_members($gse->HasMember)};
    my $inf_gse = new_inferred_instance($gse);
    $inf_gse->Name(@{$gse->Name});
    $inf_gse->HasMember(@members);
    if ($gse->is_a('OpenSet')) {
        $logger->info("Inferring open set " . $gse->displayName . ' (' . $gse->db_id . ')');
        $inf_gse->ReferenceEntity(@{$gse->ReferenceEntity});
    } else {
        my ($total, $inferred, $count) = count_distinct_proteins($gse); #disregarding candidates for counting (unless a CandidateSet has only candidates and no members - in this case all candidates have to have a homologue in order for the inferred variable to be set to 1)
        return if (!$override && $total && !$inferred); #contains protein, but none can be inferred
        $inf_gse->TotalProt($total);
        $inf_gse->InferredProt($inferred);
        $inf_gse->MaxHomologues($count);

        #handle CandidateSets
        if ($gse->is_a('CandidateSet')) {
            $logger->info("Inferring candidate set " . $gse->displayName . ' (' . $gse->db_id . ')');
            my @candidates = grep { !instance_in_list($_, \@members) } @{infer_members($gse->HasCandidate)};
            if (scalar @candidates > 0) {
                $inf_gse->HasCandidate(@candidates);
            } else {
                $logger->info("No inferred candidates");
                if ($members[0]) {
                    if (!$members[1]) {
                        $logger->info("Single member -- returning member rather than set");
                        $inf_gse = $members[0]; # return single member rather than a set
                    } else { # change to defined set if multiple members but no candidates
                        $logger->info("Multiple members -- changing candidate set to defined set");
                        my $inf_defined_set = new_inferred_instance_with_class($gse, 'DefinedSet');
                        $inf_defined_set->Name(@{$inf_gse->Name});
                        $inf_defined_set->HasMember(@members);
                        $inf_defined_set->TotalProt($total);
                        $inf_defined_set->InferredProt($inferred);
                        $inf_defined_set->MaxHomologues($count);
                    
                        $inf_gse = $inf_defined_set;
                    }
                } else {
                    $logger->info("No member -- returning undef instead of instance");
                    return unless $override;
                    
                    $logger->info("Creating ghost set due to forced override");
                    $inf_gse = create_ghost($gse);
                }
            }
        #handle DefinedSets
        } elsif ($gse->is_a('DefinedSet')) {
            $logger->info("Inferring defined set " . $gse->displayName . ' (' . $gse->db_id . ')');
            if (!$members[0]) { #no member
            	$override ? return create_ghost($gse) : return;
            } elsif (!$members[1]) { #only one member, return member itself rather than DefinedSet
            	$inf_gse = $members[0];
            }	   
        }
    }
    $inf_gse = check_for_identical_instances($inf_gse);
#set inferredFrom attribute for entities with species
    if ($inf_gse->is_valid_attribute('species') && $inf_gse->Species->[0]) { #skip assignment for species-less ES like NTP, otherwise it refers to itself
    	$inf_gse->InferredFrom;
        $inf_gse->add_attribute_value_if_necessary('inferredFrom', $gse);
        $dba->update_attribute($inf_gse, 'inferredFrom');
        $gse->InferredTo;
        $gse->add_attribute_value_if_necessary('inferredTo', $inf_gse);
        $dba->update_attribute($gse, 'inferredTo');
    }
    $override && return $inf_gse;
    $inferred_gse{$gse} = $inf_gse; #make this assignment only for bona fide inferred instances, not those created based on override
    return $inf_gse;
}


#Argument: members of an EntitySet to be inferred (arrayref of PhysicalEntity instances)
#members are given as array ref, inferred members are returned as array ref
#the flag imposes a 'none or all' inference to avoid partial inference for CandidateSets - unless all candidates can be inferred, undef is returned
sub infer_members {
    my ($ar, $flag) = @_;
    my @tmp;
    my %seen;
    foreach my $i (@{$ar}) {
        my $inf_i = orthologous_entity($i);
        return if ($flag && !$inf_i); #inference for none or all
        next unless $inf_i;
        next if $seen{$inf_i->db_id}++;
        push @tmp, $inf_i;
    }
    return \@tmp;
}

#Argument: PhysicalEntity instance
#central method to assign an orthologous entity
#calls other methods dependent on the class, and can therefore return a range of different PhysicalEntity instances, or undef
#override ensures the return of an instance even if inference is unsuccessful (ghost instances)
#returns the incoming instance itself if 'species' is not a valid attribute - except for the situation where the compartment needs to change to 'intracellular' when the target species is a bacteria
sub orthologous_entity {
    my ($i, $override) = @_;
    
    my $logger = get_logger(__PACKAGE__);
    
    if ($i->is_valid_attribute('species')) {
        unless ($orthologous_entity{$i}) {
	    my $inf_ent;
	    if (!has_species($i)) {
            $inf_ent = $i;
            $logger->info("Referring to instance " . $i->displayName . ' (' . $i->db_id . ') of class ' . $i->class . ' rather than creating an inference');
        } elsif ($i->is_a('GenomeEncodedEntity')) {
            $inf_ent = create_homol_gee($i, $override);
	    } elsif ($i->is_a('Complex') || $i->is_a('Polymer')) {
            $inf_ent = infer_complex_polymer($i, $override);
	    } elsif ($i->is_a('EntitySet')) {
            $inf_ent = $i->species->[0] ? infer_gse($i, $override) : $i;
	    } elsif ($i->is_a('SimpleEntity')) {
            # SimpleEntity was causing the script to die, so I included
            # a case for this.  My assumption was that small molecules can
            # be inferred unchanged to other species.  I don't know if
            # the assumption makes sense or if it produces correct results,
            # but it stops the script from dying. David Croft.
            # TODO: somebody needs to look into this.            
            $logger->warn($i->displayName . ' (' . $i->db_id . ') is a simple entity with a species');
            $inf_ent = $i;
	    } else {
            $logger->error_die("Unknown PhysicalEntity class: " . $i->class . ", instance name: " . $i->extended_displayName . "\n");
	    }
            $override && return $inf_ent; #should not be assigned to $orthologous_entity, otherwise this will be taken as orthologous entity even when the source species entity comes in again without override being set
            $orthologous_entity{$i} = $inf_ent;
        }
        return $orthologous_entity{$i};
    } else {
        my ($comp, $flag) = check_intracellular($i->Compartment);
        if ($flag) { #indicates compartment has changed to intracellular
            $i->inflate;
            my $clone = $i->clone;
            $clone->Created($instance_edit);
            $clone->Modified(undef);
            $clone->StableIdentifier(undef);
            $clone->Compartment(@{$comp});
            # The following call is buggy for SmallMolecule with empty ReferenceEntity which will fetch
            # identifical identity with different names. For example, two SimpleEntity instances, 200739 and 174404
            # are used as input/output in reaction 200676. However, in E. coli, the predicted reaction has the same
            # SmallMolecule instance is used as input and output, which is obviously wrong. Need to be fixed - Commented
            # by Guanming
            $clone = check_for_identical_instances($clone);
            return $clone;
        } else {
            return $i;
        }
    }
}

sub instance_in_list {
    my $instance = shift;
    my $list = shift;
    
    return any { $instance->reasonably_identical($_) } @$list;
}

sub has_species {
    my $instance = shift;
    
    if ($instance->is_a('OtherEntity')) {
        return 0;
    } elsif ($instance->is_a('Complex') || $instance->is_a('Polymer') || $instance->is_a('EntitySet')) {
        return any { has_species($_) } get_contained_instances($instance);
    } else {
        return $instance->species->[0] ? 1 : 0;
    }
}

sub get_contained_instances {
    my $instance = shift;
    if ($instance->is_a('Complex')) {
        return @{$instance->hasComponent};
    } elsif ($instance->is_a('EntitySet')) {
        return (@{$instance->hasMember}, @{$instance->hasCandidate});
    } elsif ($instance->is_a('Polymer')) {
        return @{$instance->repeatedUnit};
    } else {
        return;
    }    
}

#Argument: any instance
#returns a new instance of the same class as the incoming instance - exception is the ReferenceIsoform class, where a ReferenceGeneProduct instance needs to be returned (no isoform information available for inferred species)
#the inference target species is assigned where appropriate, and the compartment is copied over or, in the case of bacteria, replaced by 'intracellular' as appropriate
sub new_inferred_instance {
    my $i = shift;
    my $class;
    if (($protein_class eq 'ReferenceGeneProduct') && ($i->class eq 'ReferenceIsoform')) {
        $class = 'ReferenceGeneProduct';
    } else {
        $class = $i->class;
    }
    
    return new_inferred_instance_with_class($i, $class);
}

sub new_inferred_instance_with_class {
    my $i = shift;
    my $class = shift;
    
    my $inf_i = GKB::Instance->new(-ONTOLOGY=>$i->ontology, -CLASS=>$class);
    $inf_i->inflated(1);
    $inf_i->Created($instance_edit);
    if ($i->is_valid_attribute('compartment') && $i->Compartment->[0]) {
        my ($comp) = check_intracellular($i->Compartment);
        $inf_i->Compartment(@{$comp});
    }
    if ($i->is_valid_attribute('species') && $i->Species->[0]) {
        $inf_i->Species($taxon);
    }
    return $inf_i;
}

#Argument: Reaction to be inferred
#a number of tests confirms whether a reaction can be inferred (checking input, output, catalyst and requirement)
#returns an event instance if inference successful, undef if unsuccessful. However, if the incoming reaction does not contain any EntityWithAccessionedSequence, it returns 1. In this case the event is not counted as an eligible event.
sub infer_event {
    my ($event, $release_date) = @_;
    
    return if skip_event($event);
    
    my $logger = get_logger(__PACKAGE__);
    
    $inferred_event{$event} && return $inferred_event{$event};
    my $inf_e = new_inferred_instance($event);
=head #was used when event names contained e.g. species info, should be resolved now... (?)
    my $event_name = $event->Name->[0];
    print "infer_events.infer_event: event_name=$event_name\n";
    $event_name =~ s/([^\[])\[[\w\s\/,]+\]/$1/g; #remove bracketed text
    print "infer_events.infer_event: NEW event_name=$event_name\n";
    $inf_e->Name($event_name);
=cut
    $inf_e->Name(@{$event->Name});
    $inf_e->Summation($summation);
    $inf_e->EvidenceType($evidence_type);
    $inf_e->GoBiologicalProcess(@{$event->GoBiologicalProcess}); 

    my ($total, $inferred, $max) = count_distinct_proteins($event);
    $logger->info("total=$total, inferred=$inferred, max=$max\n");
    return 1 unless $total; #reactions with no EWAS at all should not be inferred
    $count_leaves++; #these are the eligible events
    binmode($eli, ":utf8");
    print $eli $event->db_id, "\t", $event->displayName, "\n";

    $being_inferred{$event} = 1;
#fill in physical entities - this is done in the respective methods called, the test variables decide as to whether the event inference can continue successfully or whether inference should be stopped as unsuccessful
#    print "infer input..........................\n";
    $logger->info("Inferring reaction inputs");
    my ($input_inference_successful) = infer_attributes($event, $inf_e, 'input');
    if (!$input_inference_successful) {
        $logger->info(get_info($event));
        $logger->info("Aborting $opt_sp event inference -- input inference unsuccessful");
        return;
    }
        
#    print "infer output..........................\n";
    $logger->info("Inferring reaction outputs");
    my ($output_inference_successful) = infer_attributes($event, $inf_e, 'output');
    if (!$output_inference_successful) {
        $logger->info(get_info($event));
        $logger->info("Aborting $opt_sp event inference -- output inference unsuccessful");
        return;
    }

#    print "infer catalystActivity...................................\n";
    $logger->info("Inferring reaction catalyst activities");
    my ($catalyst_inference_successful) = infer_catalyst($event, $inf_e);
    if (!$catalyst_inference_successful) {
        $logger->info(get_info($event));
        $logger->info("Aborting $opt_sp event inference -- catalyst inference unsuccessful");
        return;
    }
#    print "infer regulation.........................\n";
    
    my ($regulation_inference_successful, $regulation_collection) = infer_regulation($event, $release_date); #returns undef only when Regulation class is Requirement
    $logger->info("Inferring reaction regulation instances");
    if (!$regulation_inference_successful) {
        $logger->info(get_info($event));
        $logger->info("Aborting $opt_sp event inference -- regulation inference unsuccessful");
        return;
    }
    if ($inf_e->is_valid_attribute('releaseDate'))
    {
    	$inf_e->releaseDate($release_date);
    }
    $inf_e->TotalProt($total);
    $inf_e->InferredProt($inferred);
#    $inf_e->MaxHomologues($max);
    $inf_e = check_for_identical_instances($inf_e);
#fill in attributes connecting source and target species events
    $inf_e->InferredFrom;
    $inf_e->OrthologousEvent;
    if ($inf_e->is_valid_attribute('inferredFrom'))
    {
    	$inf_e->add_attribute_value_if_necessary('inferredFrom', $event);
    	$dba->update_attribute($inf_e, 'inferredFrom');	
    }
    $inf_e->add_attribute_value_if_necessary('orthologousEvent', $event);
    $dba->update_attribute($inf_e, 'orthologousEvent');

    $event->OrthologousEvent; 
    $event->add_attribute_value('orthologousEvent', $inf_e);
    $dba->update_attribute($event, 'orthologousEvent');

    unless ($opt_from eq 'hsap') {
        update_human_event($inf_e); #to make sure reactions are visible in the sky (coordinates for human events are taken as "template" for other species events)
    }
    $inferred_event{$event} = $inf_e; #keep track of human - target species event pairs
    $being_inferred{$event} = 0;
    
    if ($regulation_collection->[0]) {
    	$logger->info("Number of Regulators that this event (".$inf_e->db_id.") is regulatedBy: ".scalar(@{$regulation_collection}));
    	#$inf_e->RegulatedBy(@{$regulation_collection});
        foreach my $regulation_pair (@{$regulation_collection}) {
            my $source_regulation = $regulation_pair->{source};
            my $inferred_regulation = $regulation_pair->{inferred};
            
            $inferred_regulation = check_for_identical_instances($inferred_regulation); #this can only be done after inf_e has been stored
#            $source_regulation->inferredTo(@{$source_regulation->inferredTo});
#            $source_regulation->add_attribute_value('inferredTo', $inferred_regulation);
#            $dba->update_attribute($source_regulation, 'inferredTo');
            
            $inf_e->add_attribute_value('regulatedBy', $inferred_regulation);
    		$dba->update_attribute($inf_e,'regulatedBy');
        }
    }
    $count_inferred_leaves++; #counts successfully inferred events
    push @inferrable_human_events, $event;
    binmode($inf, ":utf8");
    print $inf $inf_e->db_id, "\t", $inf_e->displayName, "\n";

    return $inf_e;
}

#Argument: Reaction to be inferred
#checks exclusion criteria for using reaction for inference
#returns 1 if inference of reaction should be skipped
sub skip_event {
    my ($event) = @_;
    
    my $logger = get_logger(__PACKAGE__);
    
    my @list = get_skip_list();
    
    if (grep {$event->db_id == $_} @list) {
		$logger->info("skipping reaction on skip list - " . $event->db_id);
		return 1;
	}
    
	if (is_chimeric($event)) {
        $logger->info("skipping chimeric reaction - " . $event->db_id);
        return 1;
    }
	
	if ($event->Species->[1]) {
		$logger->info("skipping reaction with multiple species - " . $event->db_id); #multispecies events should not be inferred - TODO: once isChimeric attribute is consistently filled in, one may only want to exclude chimeric reactions for inference while inferring e.g. Toll receptor pathway
		return 1;
	}
	
	if ($event->relatedSpecies->[0]) {
		$logger->info("skipping reaction with related species - " . $event->db_id);
		return 1;
	}
	
    if ($event->disease->[0]) {
		$logger->info("skipping disease reaction - " . $event->db_id);
		return 1;
	}
	
	if ($event->inferredFrom->[0]) {
		$logger->info("skipping manually inferred reaction - " . $event->db_id); # manually inferred reactions should not be used for inference
		return 1;
	}
	
    return 1 if ($event->is_a('ReactionlikeEvent') && $event->reverse_attribute_value('hasMember')->[0]); #Reactions under hasMember are basically covered by the higher-level event, including them would be a duplication
	
	my @species = get_species_from_reaction_like_event_entities($event);
	if (scalar @species > 1) {
		$logger->info("skipping reaction with multiple species in entities - " . $event->db_id);
		return 1;
	}
	
	if (scalar @species == 1 && $species[0]->db_id != $source_species->db_id) {
		$logger->info("skipping reaction with only one species that differs from source species - " . $event->db_id);
		return 1;
	}
}

sub get_info {
    my $instance = shift;
    
    return $instance->displayName . ' (' . $instance->db_id . ")\n";
}

#Argument: Event instance to be inferred, inferred event instance, attribute name
#infers attribute values and attaches them to the inferred event
#returns 1 if inference successful, otherwise undef
sub infer_attributes {
    my ($event, $inf_e, $attribute) = @_;
    my @attribute_values;
    foreach my $i (@{$event->$attribute}) {
        my $orthologous_entity = orthologous_entity($i);
        return unless $orthologous_entity;
        push @attribute_values, $orthologous_entity;
    }
    $inf_e->attribute_value($attribute, @attribute_values);
    return 1;
}

#Argument: Event to be inferred, and inferred event instance
#infers catalystActivities and attaches them to the inferred event
#returns 1 if inference successful, otherwise undef
sub infer_catalyst {
    my ($event, $inf_e) = @_;

    foreach my $cat (@{$event->catalystActivity}) {
        my $inf_cat = create_inf_cat($cat);
        return unless $inf_cat;
        
        $inf_e->CatalystActivity;
        $inf_e->add_attribute_value('catalystActivity', $inf_cat);
    }
    return 1;
}

#Argument: CatalystActivity to be inferred (takes care of activeUnit as well)
#tests confirm whether a CatalystActivity can be inferred (checking physicalEntity and requirement)
#returns a CatalystActivity instance if inference successful, undef if unsuccessful.
sub create_inf_cat {
	my ($cat) = @_;
	$homol_cat{$cat} && return $homol_cat{$cat};

	my $inf_cat = new_inferred_instance($cat);
	$inf_cat->Activity(@{$cat->Activity});
	my $i = $cat->physicalEntity->[0];
	if ($i) {
		return unless orthologous_entity($i);
		$inf_cat->attribute_value('physicalEntity', orthologous_entity($i));
	}
	my @tmp;
	foreach my $au (@{$cat->ActiveUnit}) {
		next if $au->is_a('Domain');
		my $inf_au = orthologous_entity($au);
		$inf_au && push @tmp, $inf_au;
	}
	$tmp[0] && $inf_cat->ActiveUnit(@tmp);
	my ($test, $reg) = infer_regulation($cat);
	return unless $test;
	$inf_cat = check_for_identical_instances($inf_cat);
	$homol_cat{$cat} = $inf_cat;
	if ($reg->[0]) {
		foreach my $r (@{$reg}) {
			my $source_regulation = $r->{source};
			my $inferred_regulation = $r->{inferred};

			# There is no longer a relationship between Regulation and CatalystActivity,
			# so there is nothing that can replace the statement below:
#            $inferred_regulation->RegulatedEntity($inf_cat);
			#
			$inferred_regulation = check_for_identical_instances($inferred_regulation); #this can only be done after inf_cat has been stored
			#$source_regulation->inferredTo(@{$source_regulation->inferredTo});
			#$source_regulation->add_attribute_value('inferredTo', $inferred_regulation);
			#$dba->update_attribute($source_regulation, 'inferredTo');
		}
	}
	return $inf_cat;
}

#manages inference of Regulation instances attached to Events or CatalystActivities
#Arguments: Event or CatalystActivity instance to be inferred and release date
#returns undef if inference unsuccessful and the Regulation instance is of class 'Requirement', returns 1 and an array ref with the inferred Regulation instances in all other cases (the array may be empty if there is no Regulation instance attached to the incoming instance in the first place, or if the Regulation instance cannot be inferred, but is not of class 'Requirement')
sub infer_regulation {
	my ($i, $release_date) = @_;
	my @reg;

	my $reg_ar = $i->regulatedBy;
	if ($reg_ar->[0]) {
		foreach my $reg (@{$reg_ar}) {
			my $regulator = infer_regulator($reg->Regulator->[0], $release_date);
			unless ($regulator) {
				if ($reg->is_a('Requirement')) {
					return; #the event should not be inferred in this case
				} else {
					next; #no Regulation object is stored, but this doesn't stop the event being inferred
				}
			}
			my $inf_reg = new_inferred_instance($reg);
			$inf_reg->Regulator($regulator);
			# $inf_reg->add_attribute_value_if_necessary('inferredFrom', $reg);
			push @reg, {
				source => $reg,
				inferred => $inf_reg
			};
		}
	}
	return (1, \@reg);
}

#Argument: an instance allowed as regulator and release date
#returns an instance if inference is successful, or undef if unsuccessful
sub infer_regulator {
	my ($reg, $release_date) = @_;
	return unless $reg;

	my $inf_reg;
	if ($reg->is_a('PhysicalEntity')) {
		$inf_reg = orthologous_entity($reg);
	} elsif ($reg->is_a('CatalystActivity')) {
		$inf_reg = create_inf_cat($reg);
	} elsif ($reg->is_a('Event')) {
		if ($being_inferred{$reg}) {
			print $regulator $reg->db_id . "\n";
			return;
		}
		$inf_reg = infer_event($reg, $release_date);
		return if defined $inf_reg && $inf_reg == 1; #the event has no accessioned sequences and is therefore not eligible for inference
	}
	return $inf_reg;
}

#Argument: Complex or Polymer to be inferred
#applies a threshold to inference if opt_thr is given (at least opt_thr percent of protein components need to have orthologues) - once an instance has passed this threshold, downstream instances are all inferred, if necessary via ghost instances (override flag set to 1)
#returns a Complex/Polymer instance if inference successful, undef if unsuccessful
sub infer_complex_polymer {
    my ($cp, $override) = @_;
    
    my $logger = get_logger(__PACKAGE__);
    
    $inferred_cp{$cp} && return $inferred_cp{$cp};
#count components to apply thresholds
    my ($total, $inferred, $max) = count_distinct_proteins($cp);
    $logger->info("$total\t$inferred\t$max\n");
    use integer;
    my $perc;
    $total && ($perc = $inferred * 100 / $total);
    unless ($override) {
        return if ($total && !$inferred); #i.e. the entity has EWASs but none can be inferred
        if ($opt_thr && $perc) {
            return if $perc < $opt_thr;
        }
    }
    my $inf_cp = new_inferred_instance($cp);
    my $comp_name = $cp->Name->[0];
    $comp_name =~ s/([^\[])\[[\w\s\/,]+\]/$1/g; #remove bracketed text
    $inf_cp->Name($comp_name);
    $inf_cp->Summation($summation_complex);
    my @new_components;
    if ($cp->is_a('Complex')) {
        foreach my $comp (@{$cp->HasComponent}) {
            push @new_components, orthologous_entity($comp, 1);
        }
        $inf_cp->HasComponent(@new_components);
    } else {
        foreach my $comp (@{$cp->RepeatedUnit}) {
            push @new_components, orthologous_entity($comp, 1);
        }
        $inf_cp->RepeatedUnit(@new_components);
    }
    $inf_cp->TotalProt($total);
    $inf_cp->InferredProt($inferred);
    $inf_cp->MaxHomologues($max);
    $inf_cp = check_for_identical_instances($inf_cp);
    unless ($inf_cp->db_id == $cp->db_id) { #skip assignment for species-less PE like NTP, otherwise it refers to itself
        $inf_cp->InferredFrom;
        $inf_cp->add_attribute_value_if_necessary('inferredFrom', $cp);
        $dba->update_attribute($inf_cp, 'inferredFrom');
        $cp->InferredTo;
        $cp->add_attribute_value_if_necessary('inferredTo', $inf_cp);
        $dba->update_attribute($cp, 'inferredTo');
    }
    $override && return $inf_cp; #skip assignment to %inferred_cp, otherwise events may wrongly be inferred
    $inferred_cp{$cp} = $inf_cp;
    return $inf_cp;
}

#Argument: GenomeEncodedEntity to be inferred
#if there are more than one homologues for an EWAS, a DefinedSet is created
#returns an EWAS instance or a DefinedSet if inference successful, a ghost (GEE) instance if unsuccessful but $override is set, or undef if unsuccessful
sub create_homol_gee {
    my ($i, $override) = @_;
    $homol_gee{$i} && return $homol_gee{$i};
    if ($i->class eq 'GenomeEncodedEntity') { #GEEs without accession don't have orthologues by default and should not be inferred, unless $override
        return unless $override;
        return create_ghost($i);
    }
    my ($ar, $count) = infer_ewas($i); #returns list and number of homologues (if there are more than $opt_filt homologues, the list contains $opt_filt homologues, and $count = $opt_filt + 1)
    if ($count>1) { # more than one homologue -> create DS
        my $gse = GKB::Instance->new(-ONTOLOGY=>$dba->ontology,
                                     -CLASS=>'DefinedSet');
        $gse->inflated(1);
        $gse->Created($instance_edit);
        my ($comp) = check_intracellular($i->Compartment);
        $gse->Compartment(@{$comp});
        $gse->Name('Homologues of '.$i->Name->[0]);
        if (defined $opt_filt && ($count > $opt_filt)) {
            $gse->add_attribute_value('name', "more than $opt_filt homologues");
        }
        $gse->Species($taxon);
        $gse->HasMember(@{$ar});
        $gse = check_for_identical_instances($gse);
        $gse->InferredFrom;
        $gse->add_attribute_value_if_necessary('inferredFrom', $i);
        $dba->update_attribute($gse, 'inferredFrom');
        $i->InferredTo;
        $i->add_attribute_value_if_necessary('inferredTo', $gse);
        $dba->update_attribute($i, 'inferredTo');
        $homol_gee{$i} = $gse;
    } elsif ($count==1) {
        $homol_gee{$i} = $ar->[0];
    } else { #no homologue
        return unless $override;
        return create_ghost($i);
    }
    return $homol_gee{$i};
}

#Argument: GenomeEncodedEntity
#creates and returns a GEE instance as 'ghost homologue' (needed in cases of unsuccessful inference, where $override is set)
sub create_ghost {
    my ($i) = @_;
    my $ghost = GKB::Instance->new(-ONTOLOGY=>$dba->ontology,
				   -CLASS=>'GenomeEncodedEntity');
    $ghost->inflated(1);
    $ghost->Created($instance_edit);
    my ($comp) = check_intracellular($i->Compartment);
    $ghost->Compartment(@{$comp});
    $ghost->Name('Ghost homologue of '.$i->Name->[0]);
    $ghost->InferredFrom($i);
    $ghost->Species($taxon);
    $ghost = check_for_identical_instances($ghost);
    $i->InferredTo;
    $i->add_attribute_value_if_necessary('inferredTo', $ghost);
    $dba->update_attribute($i, 'inferredTo');
    return $ghost;
}

#Argument: EntityWithAccessionedSequence to be inferred to target species
#creates inferred EWAS instances, and the required ReferenceEntities
#returns array ref with homologue instances and the number of homologues (if there are more than $opt_filt homologues, the list contains $opt_filt homologues, and $count = $opt_filt + 1)
#the array is empty if there are no homologues
sub infer_ewas {
    my ($i) = @_;
    my @tmp;
    my $id = $i->referenceEntity->[0]->identifier->[0];
    my $count = 0;
    foreach my $homol (@{$homologue{$id}}) {
        unless ($homol) {die "empty homologue{$id}!\n";} #otherwise empty ewas are created, check why...
        $count++;
	last if ($opt_filt && ($count > $opt_filt));
	my ($source, $inf_id) = split/:/, $homol; #$source indicates whether the identifier comes from UniProt (SWISS or TREMBL), or from ensembl (ENSP)
#create ReferenceEntity
        my $inf_rps = $seen_rps{$inf_id};
        unless ($inf_rps) {
	    $inf_rps = new_inferred_instance($i->ReferenceEntity->[0]);
	    my $ref_db;
	    if ($source eq 'ENSP') {
		$ref_db = $ens_db;
	    } else {
		$ref_db = $uni_db;
	    }
	    $inf_rps->ReferenceDatabase($ref_db);
            $inf_rps->Identifier($inf_id);
	    my $ref_gene = create_ReferenceDNASequence($inf_id);
	    $inf_rps->ReferenceGene(@{$ref_gene});
            $inf_rps->Species($taxon);
            $inf_rps = check_for_identical_instances($inf_rps);
            $seen_rps{$inf_id} = $inf_rps;
	}
#create EWAS
        my $inf_ewas = new_inferred_instance($i);
        $inf_ewas->ReferenceEntity($inf_rps);
        $inf_ewas->Name($inf_id);
        $inf_ewas->StartCoordinate(@{$i->StartCoordinate});
        $inf_ewas->EndCoordinate(@{$i->EndCoordinate});
        if ((defined $inf_ewas->StartCoordinate->[0]  && $inf_ewas->StartCoordinate->[0] > 1) || (defined $inf_ewas->EndCoordinate->[0] && $inf_ewas->EndCoordinate->[0] > 1)) {
            $inf_ewas->Name($i->Name->[0], $inf_id);
        }
#infer modifications
        my @mod_res;
        my $flag;
        foreach my $res (@{$i->HasModifiedResidue}) {
            my $inf_res = new_inferred_instance($res);
            $inf_res->Coordinate(@{$res->Coordinate});
            $inf_res->ReferenceSequence($inf_rps);
	    $inf_res->is_valid_attribute('modification') && $inf_res->Modification(@{$res->Modification}); #currently only GroupModifiedResidue has modification
#check whether the modification is a phosphorylation, if so add 'phospho' to name
	    if (!$flag && ($res->PsiMod->[0] && $res->PsiMod->[0]->Name->[0] =~ /phospho/)) {
                $inf_ewas->Name('phospho-'.$inf_ewas->Name->[0]);
                $flag++; #to make sure 'phospho-' is added only once
            }
            if ($res->Coordinate->[0]) {
                $inf_res->_displayName($res->displayName." (in $from_name\)");
            }
	    $inf_res->is_valid_attribute('residue') && $inf_res->Residue(@{$res->Residue}); #this attribute has been removed from data model, only here for backward compatibility
	    $inf_res->PsiMod(@{$res->PsiMod});
            $inf_res = check_for_identical_instances($inf_res);
            push @mod_res, $inf_res;
        }
        $inf_ewas->HasModifiedResidue(@mod_res);
        $inf_ewas = check_for_identical_instances($inf_ewas); #in case it exists already, replace with existing one
        $inf_ewas->InferredFrom;
        $inf_ewas->add_attribute_value_if_necessary('inferredFrom', $i);
        $dba->update_attribute($inf_ewas, 'inferredFrom');
	$i->InferredTo;
        $i->add_attribute_value_if_necessary('inferredTo', $inf_ewas);
        $dba->update_attribute($i, 'inferredTo');
        push @tmp, $inf_ewas;
    }
    return \@tmp, $count;
}

#creates ReferenceDNASequence instances for the ENSG identifier mapping to the protein, and for some model organisms (for which Ensembl uses their original ids) also a direct "link" to the model organism database - to be filled into the referenceGene slot of the ReferenceGeneProduct
#Argument: a protein identifier, Returns: an arrayref for the corresponding ReferenceDNASequences
sub create_ReferenceDNASequence {
    my ($inf_id) = @_;
    my $ref_ensg = $ensg{$inf_id};
    $ref_ensg->[0] || return;
    my @tmp;
    foreach my $ensg (@{$ref_ensg}) {
#create ensg ReferenceDNASequence
	my $i = create_instance('ReferenceDNASequence');
	$i->Identifier($ensg);
	$i->ReferenceDatabase($ensg_db);
	$i->Species($taxon);
	$i = check_for_identical_instances($i);
	push @tmp, $i;
#create alternative ReferenceDNASequence
	if ($alt_refdb) {
	    my $alt_id = $ensg;
#some species need an altered identifier, while others can use the ensg identifier directly
	    if ($species_info{$opt_sp}->{'alt_refdb'}->{'alt_id'}) {
		my $regex = $species_info{$opt_sp}->{'alt_refdb'}->{'alt_id'};
		$alt_id =~ s/$regex//;
	    }
	    my $alt_i = create_instance('ReferenceDNASequence');
	    $alt_i->Identifier($alt_id);
	    $alt_i->ReferenceDatabase($alt_refdb);
	    $alt_i->Species($taxon);
	    $alt_i = check_for_identical_instances($alt_i);
	    push @tmp, $alt_i;
	}
    }
    return \@tmp;
}

#This method checks for identical instances in the db, based on the defining attributes
#If an identical instance is found, the incoming instance is replaced by the existing instance, otherwise the instance is stored as a new instance in the db
#Argument: an instance, returns an instance
sub check_for_identical_instances {
    my ($i) = @_;
    
    my $logger = get_logger(__PACKAGE__);
    
    $i->identical_instances_in_db(undef); #clear first
    $dba->fetch_identical_instances($i);
    $logger->info("Checking for identical instances for " . ($i->db_id || ('unstored instance ' . ($i->name->[0] ? $i->name->[0] : ''))));
    if ($i->identical_instances_in_db && $i->identical_instances_in_db->[0]) {
        my $count_ii = @{$i->identical_instances_in_db}; #number of elements
        if ($count_ii == 1) {
            $logger->info("Replaced with existing identical instance: " . $i->identical_instances_in_db->[0]->db_id . "\n");
            return $i->identical_instances_in_db->[0];
        } else { #interactive replacement
            $logger->info("This entity has more than one identical instances.......\n");
            #The following condition check is only needed for databases in the old data model when isoforms were included in the ReferencePeptideSequence class  - the details are slightly complicated, but that was one reason why we have changed the data model - so it's sorted now and this check is only kept for backward compatibility
    	    if (($protein_class eq 'ReferencePeptideSequence') && $i->is_a('ReferencePeptideSequence') && $i->identical_instances_in_db->[0]->VariantIdentifier->[0]) {
                $logger->info($i->identical_instances_in_db->[0]->VariantIdentifier->[0], " ***different isoforms used?***\n");
            } else {
                #temporary hack to avoid failing of script due to duplicates
        		$logger->warn("***duplicates***:\n");
        		foreach (@{$i->identical_instances_in_db}) {
        		    $logger->warn("\t" . $_->extended_displayName . "\n");
        		}
    	    }
            return $i->identical_instances_in_db->[0]; #return first element for now
        }
    } else {
        store_instance($i);
        return $i;
    }
}

sub store_instance {
    my $instance = shift;
    
    my $logger = get_logger(__PACKAGE__);
    
    my $ID = $dba->store($instance);
    $logger->info("Stored Instance: $ID");
    #push @newly_stored_instances, $instance;
}

#creates and stores the event hierarchy above a given inferred reaction, based on the hierarchy of the corresponding human events
#This method now deals with both Pathways and BlackBoxEvents (both of which can group subevents)
sub create_orthologous_generic_event {
    my ($hum_event, $release_date) = @_;
    
    my $logger = get_logger(__PACKAGE__);
    
    my $ar = $hum_event->reverse_attribute_value('hasEvent');
    if ($ar->[0]) {
        $logger->info("ID_hum_event: " . $hum_event->db_id . "\n");
        foreach my $gen_hum_event (@{$ar}) {
            unless ($inferred_event{$gen_hum_event}) { 
                my $gen_inf_event = new_inferred_instance($gen_hum_event);
                $gen_inf_event->Name(@{$gen_hum_event->Name});
                $gen_inf_event->Summation($summation);
                if ($gen_inf_event->is_valid_attribute('releaseDate'))
                {
                	$gen_inf_event->releaseDate($release_date);
                }
                $gen_inf_event->InferredFrom($gen_hum_event);
                $gen_inf_event->EvidenceType($evidence_type);
                $gen_inf_event->GoBiologicalProcess(@{$gen_hum_event->GoBiologicalProcess});
                $gen_inf_event->OrthologousEvent($gen_hum_event);
                
                if ($gen_hum_event->is_a('ReactionlikeEvent')) {
                    infer_attributes($gen_hum_event, $gen_inf_event, 'input');
                    infer_attributes($gen_hum_event, $gen_inf_event, 'output');
                    infer_catalyst($gen_hum_event, $gen_inf_event);
                }
        
                $inferred_event{$gen_hum_event} = $gen_inf_event;
                $dba->store($gen_inf_event);
                
                $gen_hum_event->OrthologousEvent;
                $gen_hum_event->add_attribute_value('orthologousEvent',$gen_inf_event);
                $dba->update_attribute($gen_hum_event, 'orthologousEvent');
                unless ($opt_from eq 'hsap') {
                    update_human_event($gen_inf_event);
                }
                push @inferrable_human_events, $gen_hum_event;
            }
            create_orthologous_generic_event($gen_hum_event, $release_date);

            $logger->info("orthologous generic event subroutine:\n");
    	    $logger->info($gen_hum_event->displayName . " => " . $inferred_event{$gen_hum_event}->displayName . "\n");
    	}
    }
}

#uses human event structure to create the same structure for the target species, connects events via the 'precedingEvent' attribute
sub infer_preceding_events {
    my (@all_human_events) = @_;
    my %seen_event;
    foreach my $hum_event (@all_human_events) {
	next if $seen_event{$hum_event}++;
	next unless $hum_event->PrecedingEvent->[0];
	$logger->info("hum_event=$hum_event\n");
	my @tmp;
	foreach my $preceding_hum_event (@{$hum_event->PrecedingEvent}) {
	    if ($inferred_event{$preceding_hum_event}) {
		push @tmp, $inferred_event{$preceding_hum_event};
	    } 
	}
	$inferred_event{$hum_event}->PrecedingEvent;

#only add the same event once:
	my %seen;
	map {$seen{$_->db_id}++} @{$inferred_event{$hum_event}->PrecedingEvent};
	@tmp = grep {! $seen{$_->db_id}++} @tmp;
	next unless $tmp[0];
	$inferred_event{$hum_event}->add_attribute_value('precedingEvent', @tmp);
	$dba->update_attribute($inferred_event{$hum_event}, 'precedingEvent');
    }
}

#slightly complicated method of counting accessions involved in an instance (could be improved...) - the problem is that for inference threshold purposes, one cannot simply count all accessions within an EntitySet, as an EntitySet really only stands for one entity at a time, so a big EntitySet would distort the numbers
#So this method looks at 'direct' RPS first, and then at the remaining EntitySets. RPSs in EntitySets only give a count of 1, while Complexes within EntitySets contribute with the number of their components (however, only the biggest Complex is included in the count). Candidates are ignored as long as there are members as well, and if there are no members, all candidates need to have orthologues in order for the CandidateSet to be counted as inferred.
 #returns the total number of proteins, the number of inferred proteins, and the maximal number of homologues for any entity involved in the count
sub count_distinct_proteins {
    my ($i) = @_;
    
    my $logger = get_logger(__PACKAGE__);
    
    my $ar = $i->follow_class_attributes(-INSTRUCTIONS => 
					 {'ReactionlikeEvent' => {'attributes' => [qw(input output catalystActivity)]},
					  'CatalystActivity' => {'attributes' => [qw(physicalEntity)]},
					  'Complex' => {'attributes' => [qw(hasComponent)]},
					  'Polymer' => {'attributes' => [qw(repeatedUnit)]},
					  'EntityWithAccessionedSequence' => {'attributes' => [qw(referenceEntity)]}},
					 -OUT_CLASSES => [($protein_class, 'EntitySet')]);

    my $max = 0;
    my $total = 0;
    my $inferred = 0;
    #map {print $_->extended_displayName, "\n"} @{$ar};
#check RPSs first
    foreach my $pe (@{$ar}) { # for now, this counts different ReferenceIsoforms (=subclass of ReferenceGeneProduct) with the same UniProt accession as separate entities - may need to be revisited
	if ($pe->is_a($protein_class)) {
	    my $id = $pe->Identifier->[0];
	    my $count = 0;
	    $homologue{$id} && ($count = scalar(@{$homologue{$id}}));
	    $logger->info("$id\t$count\n");
	    $total++;
	    ($count >  $max) && ($max = $count);
	    ($count > 0) && ($inferred += 1);
#	} elsif ($pe->is_a('ReferenceSequence')) { #count DNA and RNA as inferred for the time being
#	    $total++;
#	    $inferred++;
	}
    }
    foreach my $pe (@{$ar}) {
	if ($pe->is_a('EntitySet')) {
	    my $ar1 = $pe->follow_class_attributes(-INSTRUCTIONS =>
						   {'DefinedSet' => {'attributes' => [qw(hasMember)]},
						    'CandidateSet' => {'attributes' => [qw(hasMember)]},
						    'EntityWithAccessionedSequence' => {'attributes' => [qw(referenceEntity)]}},
						   -OUT_CLASSES => [qw(Complex Polymer ReferenceSequence)]);

#if array is empty, check whether PE is a CandidateSet with only candidates
	    if (!$ar1->[0] && $pe->is_a('CandidateSet')) {
		my ($a, $b, $c) = check_candidates($pe, $ar);
		$total += $a if defined $a;
		$inferred += $b if defined $b;
		$max = $c if (defined $c && $c > $max);
		next;
	    }
	    next unless $ar1->[0];
	#check whether instances within the set have been counted already - if the gse contains an as yet uncounted entity, the numbers are upped.
	    my $uncounted;
	    foreach my $rps (@{$ar1}) {
		next if grep {$rps->db_id == $_->db_id} @{$ar};
		$uncounted++;
	    }
	    next unless $uncounted;

	    my $flag = 0;
	    my $flag_inferred = 0;
	    foreach my $inst (@{$ar1}) {
		if ($inst->is_a('Complex') || $inst->is_a('Polymer')) {
		    my ($total, $inferred, $count) = count_distinct_proteins($inst);
		    ($total > $flag) && ($flag = $total); #keep numbers from biggest complex only
		    ($inferred > $flag_inferred) && ($flag_inferred = $inferred);
		    ($count > $max) && ($max = $count);
		} elsif ($inst->is_a($protein_class)) {
		    $flag = 1;
		    my $id = $inst->Identifier->[0];
		    my $count = 0;
		    $homologue{$id} && ($count = scalar(@{$homologue{$id}}));
		    ($count > $max) && ($max = $count);
		    ($count > 0) && ($flag_inferred = 1);
#		} elsif ($inst->is_a('ReferenceSequence')) {
#		    $flag = 1;
#		    $flag_inferred = 1;
		}
	    }
	    $total += $flag;
	    $inferred += $flag_inferred;
	}
    }
    #print STDERR "**********************  ", $total, "\t", $inferred, "\t", $max, "\n";    
    return ($total, $inferred, $max);
}

#similar counting procedure as in count_distinct_proteins - adapted for CandidateSets - if a CandidateSet has no members but only candidates, all candidates need to have orthologues in order to be counted
#Arguments: the CandidateSet instance to be assessed, and the array ref of other instances counted already in count_distinct_proteins
#returns the total number of proteins, the number of inferred proteins, and the maximal number of homologues for any entity involved in the count
sub check_candidates {
    my ($pe, $counted) = @_;
    return unless $pe->HasCandidate->[0];
    my ($a, $b, $c, $flag);
#all candidates have to have an orthologue in order to go ahead
    my $ar = $pe->follow_class_attributes(-INSTRUCTIONS =>
					  { 'CandidateSet' => {'attributes' => [qw(hasCandidate)]},
					    'EntityWithAccessionedSequence' => {'attributes' => [qw(referenceEntity)]}},
					  -OUT_CLASSES => [qw(Complex Polymer ReferenceSequence)]);
#check whether the instances within the set have been counted already - if the set contains an as yet uncounted entity, the numbers are upped.
    my $uncounted;
    foreach my $rps (@{$ar}) {
	next if grep {$rps->db_id == $_->db_id} @{$counted};
	$uncounted++;
    }
    return unless $uncounted;
    foreach my $i (@{$ar}) {
	if ($i->is_a('Complex') || $i->is_a('Polymer')) {
	    my ($ca, $cb, $cc) = count_distinct_proteins($i);
	    if ($ca && !$cb) {
		$flag++; #this will make the CandidateSet drop out
	    }
	    $a = $ca if (defined $a && $ca > $a);
	    $b = $cb if (defined $b && $cb > $b);
	    $c = $cc if (defined $c && $cc > $c);
	} elsif ($i->is_a($protein_class)) {
	    $a = 1;
	    my $id = $i->Identifier->[0];
	    my $count = 0;
	    $homologue{$id} && ($count = scalar(@{$homologue{$id}}));
	    $c = $count if (defined $c && $count > $c);
	    ($count > 0) && ($b = 1);
	    unless ($b) {$flag++};
	}
    }
    $flag && return $a, 0;
    return $a, $b, $c;
}

#!!!!! Not needed anywhere in this script
#counts and returns the total number of human UniProt RPS in the db, and the number of those that have homologues
sub count_hum_proteins {
    my $ar1 = $dba->fetch_instance(-CLASS => 'ReferenceDatabase',
				   -QUERY => [{-ATTRIBUTE => 'name',
					       -VALUE => ['UniProt']}
					      ]
				   );
    my $ar2 = $dba->fetch_instance(-CLASS => 'Species',
				   -QUERY => [{-ATTRIBUTE => 'name',
					       -VALUE => ['Homo sapiens']}
					      ]
				   );
    my $hum_prot_ar = $dba->fetch_instance(-CLASS => $protein_class,
					   -QUERY => [{-ATTRIBUTE => 'referenceDatabase',
						       -VALUE => [$ar1->[0]->db_id]},
						      {-ATTRIBUTE => 'species',
						       -VALUE => [$ar2->[0]->db_id]}
						      ]
					   ); 
    my ($count, $count_hom);
    foreach my $hum_prot (@{$hum_prot_ar}) {
	next if (($protein_class eq 'ReferenceGeneProduct') && $hum_prot->is_a('ReferenceIsoform'));
	$count++;
	my $uni = $hum_prot->Identifier->[0];
	next unless $homologue{$uni}->[0];
	$count_hom++;
    }
    return $count, $count_hom;
}

#This method is mainly for inference from a species other than human. It fills in the 'orthologousEvent' attribute on the human event in the db as well, so that reactions appear in the sky (note: this may have changed with the new frontpage handling, not sure - may not be necessary any longer).
sub update_human_event {
    my ($event) = @_;
    my $ar = $event->follow_class_attributes(
					     -INSTRUCTIONS => 
					     {'Event' => {'attributes' => [qw(orthologousEvent)]}},
					     -OUT_CLASSES => [qw(Event)]
					     );
    foreach my $ev (@{$ar}) {
	next unless $ev->Species->[0];
	if ($ev->Species->[0]->Name->[0] eq 'Homo sapiens') {

	    $ev->OrthologousEvent;
	    $ev->add_attribute_value_if_necessary('orthologousEvent', $event);
	    $dba->update_attribute($ev, 'orthologousEvent');
	    GKB::Utils_esther::update_modified_if_necessary($ev, $instance_edit, $dba);
#in case the human event was only found further down, but is not attached to the incoming event itself, this is taken care of here
	    $event->OrthologousEvent;
	    $event->add_attribute_value_if_necessary('orthologousEvent', $ev);
	    $dba->update_attribute($event, 'orthologousEvent');
	    GKB::Utils_esther::update_modified_if_necessary($event, $instance_edit, $dba);
	}
    }
}

#checks whether the compartment for an instance needs to be replaced by 'intracellular' - this is needed when the target species is a prokaryote, and the compartment is a subterm of 'intracellular' (as e.g. nucleus doesn't exist in prokaryotes).
#Argument: array ref of Compartment array
#returns: array ref of Compartment array plus a flag - if TRUE indicating that the return value is different from the incoming one
sub check_intracellular {
    my ($ar) = @_;
    return $ar unless $species_info{$opt_sp}->{'prokaryote'};
    my @tmp;
    my $flag;
    COMP:foreach my $comp (@{$ar}) {
	my $ar1 = $comp->follow_class_attributes(-INSTRUCTIONS => 
						 {'GO_CellularComponent' => {'attributes' => [qw(componentOf instanceOf)]}},
						 -OUT_CLASSES => [qw(GO_CellularComponent)]);
	foreach my $cc (@{$ar1}) {
	    next unless $cc->Name->[0] =~ /intracellular/;
	    push @tmp, $intra;
	    $flag++; #indicates return value is different from original value
	    next COMP;
	}
	push @tmp, $comp;
    }
    return (\@tmp, $flag);
}

1;
