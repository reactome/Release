#!/usr/local/bin/perl -w

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
use GKB::Config_Species;
use Data::Dumper;
use Getopt::Long;
use DBI;
use strict;

print "infer_events: starting\n";

@ARGV || die "Usage: $0 -user db_user -host db_host -pass db_pass -port db_port -db db_name -r reactome_release -from from_species_name(e.g. hsa) -sp to_species_name(e.g.dme) -filt second_taxon_filter -thr threshold_for_complex";

our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db, $opt_r, $opt_from, $opt_sp, $opt_filt, $opt_thr, $opt_debug);

&GetOptions("user:s",
	    "host:s",
	    "pass:s",
	    "port:i",
	    "db=s",
	    "r=i",
	    "from=s",
	    "sp=s",
	    "filt=i",
	    "thr=i",
	    "debug",
	    );

$opt_db || die "Need database name (-db).\n";
$opt_r || die "Need Reactome release number, e.g. -r 32\n";
$opt_sp || die "Need species (-sp), e.g. mmus.\n";
$opt_from || ($opt_from = 'hsap');

print "infer_events: opt_db=$opt_db\n";

#connection to Reactome
my $dba = GKB::DBAdaptor->new
   (
     -dbname => $opt_db,
     -user   => $opt_user,         
     -host   => $opt_host,
     -pass   => $opt_pass,
     -port   => $opt_port || 3306,
     -driver => 'mysql',
     -DEBUG => $opt_debug
     );

my $protein_class = &GKB::Utils::get_reference_protein_class($dba); #determines whether the database is in the pre-March09 schema with ReferencePeptideSequence, or in the new one with ReferenceGeneProduct and ReferenceIsoform

print "infer_events: protein_class=$protein_class\n";

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
print "infer_events: UniProt ReferenceDatabase.extended_displayName=" . $uni_db->extended_displayName . "\n";

my $ens_db = create_instance('ReferenceDatabase');
$ens_db->Name('Ensembl', "ENSEMBL_$species_info{$opt_sp}->{'name'}->[0]\_PROTEIN");
$ens_db->Url($species_info{$opt_sp}->{'refdb'}->{'url'});
$ens_db->AccessUrl($species_info{$opt_sp}->{'refdb'}->{'access'});
$ens_db = check_for_identical_instances($ens_db);
print "infer_events: ENSEMBL ReferenceDatabase.extended_displayName=" . $ens_db->extended_displayName . "\n";

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

print "infer_events: about to prepare static instances\n";

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
print "infer_events: taxon=" . $taxon->extended_displayName . "\n";

#get instance for *from* Taxon
my $source_species = $dba->fetch_instance(-CLASS => 'Species',
					  -QUERY => [{-ATTRIBUTE => 'name',
						      -VALUE => [$species_info{$opt_from}->{'name'}->[0]]}
						     ]
					  )->[0];
print "infer_events.pl: dba=$dba\n";
print "infer_events.pl: opt_from=$opt_from\n";

$source_species || die ("infer_events: can't find source species instance for $species_info{$opt_from}->{'name'}->[0], aborting!\n");


##################
####code begins###
##################

open(FILE, ">>$opt_r\/report_ortho_inference_$opt_db\.txt");
open(REGULATOR, ">>$opt_r\/cyclical_reactions_$opt_db\.txt");
open(INF, ">$opt_r\/inferred_$opt_sp\_$opt_thr\.txt");
open(ELI, ">$opt_r\/eligible_$opt_sp\_$opt_thr\.txt");

my (%uni, %orthologous_entity, %inferred_cp, %inferred_gse, %homol_gee, %seen_rps, %inferred_event, %being_inferred, %homol_cat, %instances);
my $a =("#"x20)."\n";
my @all_human_events;
my $count_leaves = 0;
my $count_inferred_leaves = 0;

#exclude selected pathways from inference
my @list;
foreach my $pwy_id (162906, 168254) {  #human-viral pathways
    push @list, get_reaction_ids($pwy_id);  #inference is done on reaction level, therefore extract all downstream reactions and store them in @list.
}
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
	print "infer_events: considering reaction DB_ID=" . $rxn->db_id . "\n";

#exclude some events based on a number of criteria
    next if grep {$rxn->db_id == $_} @list; #list of ids to be excluded
    next if $rxn->Species->[1]; #multispecies events should not be inferred - TODO: once isChimeric attribute is consistently filled in, one may only want to exclude chimeric reactions for inference while inferring e.g. Toll receptor pathway    
    if (is_chimeric($rxn)) {
        print "infer_events: skipping chimeric reaction DB_ID=" . $rxn->db_id . "\n";
        next;
    }
    next if ($rxn->is_a('ReactionlikeEvent') && $rxn->reverse_attribute_value('hasMember')->[0]); #Reactions under hasMember are basically covered by the higher-level event, including them would be a duplication

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
	print "infer_events: scalar(tmp)=" . scalar(@tmp) . "\n";
    if ($tmp[0]) { #the event exists in the other species, need not be inferred but should be kept for the step when the event hierarchy is created, so that it can be fit in at the appropriate position in the event hierarchy
	$inferred_event{$rxn} = $tmp[0]; #disregards multiple events here..., the first event is taken into the hash to allow building the event structure further down
	push @all_human_events, $rxn;
	next;
    }
    print "infer_events: infer event for reaction=" . $rxn->extended_displayName . "\n";
    infer_event($rxn);
}

#creating hierarchy structure as in human event
my %seen;
foreach (@all_human_events){
    next if $seen{$_}++;
    create_orthologous_generic_event($_);
}
print "\n";
#fill pathways and blackboxevents with their components in the same order as in human
my %seen3;
foreach my $hum_pathway (@all_human_events){
    next if $seen3{$hum_pathway}++;
    next unless $hum_pathway->is_valid_attribute('hasEvent'); #This should happen for both Pathways and BlackBoxEvents
   print "infer_events: filling pathway name=" . $hum_pathway->extended_displayName . "\n";
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
    	print "WARNING - ". $hum_pathway->extended_displayName. " and ". $inferred_event{$hum_pathway}->extended_displayName . " (likely connected via manual inference) have different classes.\n";
    }
}
print "infer_events: inferring preceding events.....\n";
infer_preceding_events(@all_human_events);
#finally mark all human events with orthologous events as modified
my %seen4;
foreach my $hum_event (@all_human_events){
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
print FILE $opt_from, " to ",$opt_sp, "\t", $count_inferred_leaves, " out of ", $count_leaves, " eligible reactions (", $perc, "%)$warning\n";

close(FILE);
close(REGULATOR);
close(INF);
close(ELI);

print "infer_events: end\n";

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

#This method reads an orthopair file (in the format 'from_species tab to_species(=list separated by space)'), and returns a hash reference 'homologue'.
sub read_orthology {
    my ($file) = @_;
    my %homologue;
    print "infer_events.read_orthology: Now reading orthology mapping file: ", $file, "\n";
    if (open(READ_ORTHOPAIR, $file)) {
	    while (<READ_ORTHOPAIR>) {
		my %seen_to;
		my ($from, $tos) = split/\t/, $_;
		my @tos = split/\s/, $tos;
		foreach my $to (@tos) {
		    $seen_to{$to}++;
		}
		push @{$homologue{$from}}, keys %seen_to;
	    }
	    close(READ_ORTHOPAIR);
    } else {
    	print "Could not open file: $file\n";
    }
    return \%homologue;
}

#This method reads the gene-protein mapping file for the target species (tab delimited, multiple protein ids separated by space), and returns a hash reference 'ensg'.
sub read_ensg_mapping {
    my ($file) = @_;
    my %ensg;
    print "infer_events.read_ensg_mapping: Now reading ensg mapping file: ", $file, "\n";
    if (open(READ, $file)) {
	    while (<READ>) {
	        my ($ensg, $protein_ids) = split/\t/, $_;
	        my @protein_ids = split/\s/, $protein_ids;
	        foreach my $id (@protein_ids) {
		    $id =~ s/\w+://;  #remove database prefix
		    push @{$ensg{$id}}, $ensg;
	        }
	    }
	    close(READ);
    } else {
    	print"Could not open file: $file\n";
    }
    return \%ensg;
}

#Argument: EntitySet to be inferred
#returns an EntitySet instance if inference is successful, or undef if unsuccessful. If the inference is successful, but the resulting Set would have only one member, the inferred member instance is returned rather than a Set instance. 
#When the override flag is set to true and the inference is unsuccessful, a GenomeEncodedEntity instance (ghost) is returned. 
sub infer_gse {
    my ($gse, $override) = @_;
    $inferred_gse{$gse} && return $inferred_gse{$gse};
#infer members only to start with, no candidates
    my $ar = infer_members($gse->HasMember);
    my $inf_gse = new_inferred_instance($gse);
    $inf_gse->Name(@{$gse->Name});
    $inf_gse->HasMember(@{$ar});
    if ($gse->is_a('OpenSet')) {
	$inf_gse->ReferenceEntity(@{$gse->ReferenceEntity});
    } else {
	my ($total, $inferred, $count) = count_distinct_proteins($gse); #disregarding candidates for counting (unless a CandidateSet has only candidates and no members - in this case all candidates have to have a homologue in order for the inferred variable to be set to 1)
	return if (!$override && $total && !$inferred); #contains protein, but none can be inferred
	$inf_gse->TotalProt($total);
	$inf_gse->InferredProt($inferred);
	$inf_gse->MaxHomologues($count);
#handle CandidateSets - infer candidates where no confirmed member exists (and all candidates have a homologue - otherwise the "correct" protein may be missing)
	if ($gse->is_a('CandidateSet')) {
	    my $ar_cand;
	    if (!$gse->HasMember->[0]) { #no member exists
		$ar_cand = infer_members($gse->HasCandidate, 1); #the flag set to 1 ensures an "all or none" inference
	    }
	    $inf_gse->HasCandidate(@{$ar_cand});
	    unless ($ar_cand->[0] || $ar->[0]) { #Candidate set must have at least one member or candidate
		return unless $override;
		$inf_gse = create_ghost($gse);
	    }
#handle DefinedSets
	} elsif ($gse->is_a('DefinedSet')) {
	    if (!$ar->[0]) { #no member
		$override?return create_ghost($gse):return;
	    } elsif (!$ar->[1]) { #only one member, return member itself rather than DefinedSet
		$inf_gse = $ar->[0];
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
    if ($i->is_valid_attribute('species')) {
        unless ($orthologous_entity{$i}) {
	    my $inf_ent;
	    if ($i->is_a('GenomeEncodedEntity')) {
		$inf_ent = create_homol_gee($i, $override);
	    } elsif ($i->is_a('Complex') || $i->is_a('Polymer')) {
		$inf_ent = infer_complex_polymer($i, $override);
	    } elsif ($i->is_a('EntitySet')) {
		$inf_ent = infer_gse($i, $override);
	    } elsif ($i->is_a('SimpleEntity')) {
		# SimpleEntity was causing the script to die, so I included
		# a case for this.  My assumption was that small molecules can
		# be inferred unchanged to other species.  I don't know if
		# the assumption makes sense or if it produces correct results,
		# but it stops the script from dying. David Croft.
		# TODO: somebody needs to look into this.
		$inf_ent = $i;
	    } else {
		die "Unknown PhysicalEntity class: " . $i->class . ", instance name: " . $i->extended_displayName . "\n";
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

#Argument: any instance
#returns a new instance of the same class as the incoming instance - exception is the ReferenceIsoform class, where a ReferenceGeneProduct instance needs to be returned (no isoform information available for inferred species)
#the inference target species is assigned where appropriate, and the compartment is copied over or, in the case of bacteria, replaced by 'intracellular' as appropriate
sub new_inferred_instance {
    my ($i) = @_;
    my $class;
    if (($protein_class eq 'ReferenceGeneProduct') && ($i->class eq 'ReferenceIsoform')) {
	$class = 'ReferenceGeneProduct';
    } else {
	$class = $i->class;
    }
    my $inf_i = GKB::Instance->new(-ONTOLOGY=>$i->ontology,
				   -CLASS=>$class);
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
    my ($event) = @_;
    $inferred_event{$event} && return $inferred_event{$event};
    my $inf_e = new_inferred_instance($event);
=head #was used when event names contained e.g. species info, should be resolved now... (?)
    my $event_name = $event->Name->[0];
    print STDERR "infer_events.infer_event: event_name=$event_name\n";
    $event_name =~ s/([^\[])\[[\w\s\/,]+\]/$1/g; #remove bracketed text
    print STDERR "infer_events.infer_event: NEW event_name=$event_name\n";
    $inf_e->Name($event_name);
=cut
    $inf_e->Name(@{$event->Name});
    $inf_e->Summation($summation);
    $inf_e->EvidenceType($evidence_type);
    $inf_e->GoBiologicalProcess(@{$event->GoBiologicalProcess}); 

    my ($total, $inferred, $max) = count_distinct_proteins($event);
    print "infer_events.infer_event: total=$total, inferred=$inferred, max=$max\n";
    return 1 unless $total; #reactions with no EWAS at all should not be inferred
    $count_leaves++; #these are the eligible events
    print ELI $event->db_id, "\t", $event->displayName, "\n";

    $being_inferred{$event} = 1;
#fill in physical entities - this is done in the respective methods called, the test variables decide as to whether the event inference can continue successfully or whether inference should be stopped as unsuccessful
#    print "infer input..........................\n";
    my $test1 = infer_attributes($event, $inf_e, 'input');
    return unless $test1;
#    print "infer output..........................\n";
    my $test2 = infer_attributes($event, $inf_e, 'output');
    return unless $test2;
#    print "infer catalystActivity...................................\n";
    my $test3 = infer_catalyst($event, $inf_e);
    return unless $test3;
#    print "infer regulation.........................\n";
    my ($test4, $reg) = infer_regulation($event, $inf_e);
    return unless $test4; #returns undef only when Regulation class is Requirement

    $inf_e->TotalProt($total);
    $inf_e->InferredProt($inferred);
#    $inf_e->MaxHomologues($max);
    $inf_e = check_for_identical_instances($inf_e);
#fill in attributes connecting source and target species events
    $inf_e->InferredFrom;
    $inf_e->OrthologousEvent;
    $inf_e->add_attribute_value_if_necessary('inferredFrom', $event);
    $inf_e->add_attribute_value_if_necessary('orthologousEvent', $event);
    $dba->update_attribute($inf_e, 'inferredFrom');
    $dba->update_attribute($inf_e, 'orthologousEvent');

    $event->OrthologousEvent; 
    $event->add_attribute_value('orthologousEvent', $inf_e);
    $dba->update_attribute($event, 'orthologousEvent');

    unless ($opt_from eq 'hsap') {
	update_human_event($inf_e); #to make sure reactions are visible in the sky (coordinates for human events are taken as "template" for other species events)
    }
    $inferred_event{$event} = $inf_e; #keep track of human - target species event pairs
    $being_inferred{$event} = 0;

    if ($reg->[0]) {
	foreach my $r (@{$reg}) {
	    $r->RegulatedEntity($inf_e);
	    $r = check_for_identical_instances($r); #this can only be done at this point, after inf_e has been stored
	}
    }
    $count_inferred_leaves++; #counts successfully inferred events
    push @all_human_events, $event;
    print INF $inf_e->db_id, "\t", $inf_e->displayName, "\n";

    return $inf_e;
}

#Argument: Event instance to be inferred, inferred event instance, attribute name
#infers attribute values and attaches them to the inferred event
#returns 1 if inference successful, otherwise undef
sub infer_attributes {
    my ($event, $inf_e, $attribute) = @_;
    my @attribute_values;
    foreach my $i (@{$event->$attribute}) {
	my $test = orthologous_entity($i);
	return unless orthologous_entity($i);
	push @attribute_values, orthologous_entity($i);
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
	    $r->RegulatedEntity($inf_cat);
	    $r = check_for_identical_instances($r); #this can only be done after inf_cat has been stored
	}
    }
    return $inf_cat;
}

#manages inference of Regulation instances attached to Events or CatalystActivities
#Arguments: Event or CatalystActivity instance to be inferred
#returns undef if inference unsuccessful and the Regulation instance is of class 'Requirement', returns 1 and an array ref with the inferred Regulation instances in all other cases (the array may be empty if there is no Regulation instance attached to the incoming instance in the first place, or if the Regulation instance cannot be inferred, but is not of class 'Requirement')
sub infer_regulation {
    my ($i) = @_;
    my @reg;
    my $reg_ar = $i->reverse_attribute_value('regulatedEntity');
    if ($reg_ar->[0]) {
	foreach my $reg (@{$reg_ar}) {
	    my $regulator = infer_regulator($reg->Regulator->[0]);
	    unless ($regulator) {
		if ($reg->is_a('Requirement')) {
		    return; #the event should not be inferred in this case
		} else {
		    next; #no Regulation object is stored, but this doesn't stop the event being inferred
		}
	    }
	    my $inf_reg = new_inferred_instance($reg);
	    $inf_reg->Regulator($regulator);
	    push @reg, $inf_reg;
	}
    }
    return 1, \@reg;
}

#Argument: an instance allowed as regulator
#returns an instance if inference is successful, or undef if unsuccessful
sub infer_regulator {
    my ($reg) = @_;
    return unless $reg;
    
    my $inf_reg;
    if ($reg->is_a('PhysicalEntity')) {
	$inf_reg = orthologous_entity($reg);
    } elsif ($reg->is_a('CatalystActivity')) {
	$inf_reg = create_inf_cat($reg);
    } elsif ($reg->is_a('Event')) {
	if ($being_inferred{$reg}) {
	    print REGULATOR $reg->db_id . "\n";
	    return;
	}
	
	$inf_reg = infer_event($reg);
	return if defined $inf_reg && $inf_reg == 1; #the event has no accessioned sequences and is therefore not eligible for inference
    }
    return $inf_reg;
}

#Argument: Complex or Polymer to be inferred
#applies a threshold to inference if opt_thr is given (at least opt_thr percent of protein components need to have orthologues) - once an instance has passed this threshold, downstream instances are all inferred, if necessary via ghost instances (override flag set to 1)
#returns a Complex/Polymer instance if inference successful, undef if unsuccessful
sub infer_complex_polymer {
    my ($cp, $override) = @_;
    $inferred_cp{$cp} && return $inferred_cp{$cp};
#count components to apply thresholds
    my ($total, $inferred, $max) = count_distinct_proteins($cp);
    print $total, "\t", $inferred, "\t", $max, "\n";
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
    my $comp_name = $cp->Name->[0]." (name copied from entity in $from_name\)";
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
            $inf_ewas->Name(@{$i->Name}, "Note: the coordinates are copied over from $from_name\.", $inf_id);
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
    $i->identical_instances_in_db(undef); #clear first
    $dba->fetch_identical_instances($i);
    if ($i->identical_instances_in_db && $i->identical_instances_in_db->[0]) {
	my $count_ii = @{$i->identical_instances_in_db}; #number of elements
	if ($count_ii == 1) {
	    print STDERR "Replaced with existing identical instance: ",$i->identical_instances_in_db->[0]->db_id , ".\n";
	    return $i->identical_instances_in_db->[0];
	} else { #interactive replacement
	    print STDERR "This entity has more than one identical instances.......\n";
#The following condition check is only needed for databases in the old data model when isoforms were included in the ReferencePeptideSequence class  - the details are slightly complicated, but that was one reason why we have changed the data model - so it's sorted now and this check is only kept for backward compatibility
	    if (($protein_class eq 'ReferencePeptideSequence') &&
		$i->is_a('ReferencePeptideSequence') && $i->identical_instances_in_db->[0]->VariantIdentifier->[0]) {
		print STDERR $i->identical_instances_in_db->[0]->VariantIdentifier->[0], " ***different isoforms used?***\n";
	    } else {
#temporary hack to avoid failing of script due to duplicates
		print STDERR "***duplicates***:\n";
		foreach (@{$i->identical_instances_in_db}) {
		    print STDERR "\t", $_->extended_displayName, "\n";
		}
	    }
	    return $i->identical_instances_in_db->[0]; #return first element for now
	}
    } else {
	my $ID = $dba->store($i);
	print STDERR "Stored instance: ", $ID, "\n";
	return $i;
    }
}

#creates and stores the event hierarchy above a given inferred reaction, based on the hierarchy of the corresponding human events
#This method now deals with both Pathways and BlackBoxEvents (both of which can group subevents)
sub create_orthologous_generic_event {
    my ($hum_event) = @_;
    my $ar = $hum_event->reverse_attribute_value('hasEvent');
    if ($ar->[0]) {
	print "ID_hum_event: ", $hum_event->db_id, "\n";
	foreach my $gen_hum_event (@{$ar}) {
	    unless ($inferred_event{$gen_hum_event}) { 
		my $gen_inf_event = new_inferred_instance($gen_hum_event);
		$gen_inf_event->Name(@{$gen_hum_event->Name});
		$gen_inf_event->Summation($summation);
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
		push @all_human_events, $gen_hum_event;
	    }
	    create_orthologous_generic_event($gen_hum_event);
	    
	    print "orthologous generic event subroutine:\n",
	    $gen_hum_event->displayName, " => ", $inferred_event{$gen_hum_event}->displayName, "\n";
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
	print "hum_event=$hum_event\n";
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
	    print $id, "\t", $count, "\n";
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
