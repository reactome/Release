#!/usr/local/bin/perl  -w

# Creates a Mart-ified version of Reactome.  Requires command line
# parameters specifying the Reactome database to use as a source.
#
# Further options:
#
# -bdb			Use to specify optional name for the BioMart database.
# -db_ids		You can use this to supply a list of DB_IDs to limit
# 				the process.
# -mmax			You can use this to limit the number of rows generated in
#				the main table.
# -interactions	Flag that inhibits generation of interactions
# -only_interactions	Flag that generates interactions only, skips everything else.
# -emn			Flag to suppress expansion of multiple attribute names.

use lib "$ENV{HOME}/bioperl-1.0";
use lib "$ENV{HOME}/GKB/modules";
use GKB::Config;
use GKB::BioMart;
use GKB::BioMart::StaticLinkInstances;
use GKB::Utils;
use Getopt::Long;
use strict;

@ARGV || die "Usage: $0 -user db_user -host db_host -pass db_pass -port db_port -db db_name -bdb <BioMart DB name> -interactions -only_interactions -emn";

our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db,$opt_debug,$opt_bdb,$opt_db_ids,$opt_mmax,$opt_interactions,$opt_only_interactions,$opt_emn);

&GetOptions("user:s",
	    "host:s",
	    "pass:s",
	    "port:i",
	    "db=s",
	    "debug",
	    "f",
	    "sudo",
	    "bdb=s",
	    "db_ids=s",
	    "mmax:i",
	    "interactions",
	    "only_interactions",
	    "emn",
	    );
	    
$opt_db || die "Need database name (-db).\n";

if ($opt_only_interactions) {
	$opt_interactions = 1;
}

# Hard-code a whole bunch of parameters that will affect
# the Marts being produced

# The more attributes you ignore, the faster it runs!
my %instance_ignored_attributes = ('_Protege_id' => '_Protege_id',
								   '_timestamp' => '_timestamp',
								   'atomicConnectivity' => 'atomicConnectivity',
								   'fingerPrintString' => 'fingerPrintString',
								   '__is_ghost' => '__is_ghost',
								   'precedingEvent' => 'precedingEvent',
								   'inferredFrom' => 'inferredFrom',
								   'orthologousEvent' => 'orthologousEvent',
								   '_class' => '_class',
								   'accessUrl' => 'accessUrl',
								   'url' => 'url',
								   'dateTime' => 'dateTime',
								   'created' => 'created',
								   'modified' => 'modified',
								   '_applyToAllEditedInstances' => '_applyToAllEditedInstances',
								   
								   'superTaxon' => 'superTaxon',
								   'sequenceLength' => 'sequenceLength',
								   'inferredProt' => 'inferredProt',
								   'maxHomologues' => 'maxHomologues',
								   'definition' => 'definition',
								   'authored' => 'authored',
								   '_doRelease' => '_doRelease',
								   'releaseDate' => 'releaseDate',
								   'totalProt' => 'totalProt',
								   'maxHomologues' => 'maxHomologues',
								   );
my %expanded_instance_ignored_attributes = (
								   'DB_ID' => 'DB_ID',
								   'stableIdentifier' => 'stableIdentifier',
								   );
my %instance_ignored_composite_attributes = (
								   'stableIdentifier.referenceDatabase' => 'stableIdentifier.referenceDatabase',
								   'stableIdentifier._displayName' => 'stableIdentifier._displayName',
								   'goBiologicalProcess.referenceDatabase' => 'goBiologicalProcess.referenceDatabase',
								   'goCellularComponent.referenceDatabase' => 'goCellularComponent.referenceDatabase',
								   'catalystActivity.activity._displayName' => 'catalystActivity.activity._displayName',
								   'catalystActivity.activity.referenceDatabase' => 'catalystActivity.activity.referenceDatabase',
								   'catalystActivity.physicalEntity' => 'catalystActivity.physicalEntity',
								   );

my $i;
my $depth = 2;
my $dba = GKB::DBAdaptor->new
    (
     -user   => $opt_user || '',
     -host   => $opt_host,
     -pass   => $opt_pass,
     -port   => $opt_port,
     -dbname => $opt_db,
     );
my $reference_protein_name = GKB::Utils::get_reference_protein_class($dba);

# Used to modify the way that Mart table names are generated.  Normally,
# table names are constructed based on Reactome instance names.  But,
# as time goes by, these instance names can change, as a result of
# data model changes.  So, you can use this hash to make new instance
# classes compatible with an existing Mart web interface, by mapping
# them onto their 'old' names.
my %table_name_map = ($reference_protein_name => 'ReferencePeptideSequence');

# Main tables
my @reactome_classes = ("Pathway", "Reaction", "Complex", "EntityWithAccessionedSequence");
my @reactome_keys = ("DB_ID", "DB_ID", "DB_ID", "DB_ID");

# Specify which classes to use when creating dimension tables
my %reactome_to_dimension_class_hash = (
										"Pathway" => ["Reaction", "Complex", "EntityWithAccessionedSequence", $reference_protein_name, "ReferenceDNASequence", "ReferenceMolecule", "LiteratureReference"],
										"Reaction" => ["Complex", "EntityWithAccessionedSequence", $reference_protein_name, "ReferenceDNASequence", "ReferenceMolecule", "LiteratureReference"],
										"Complex" => ["EntityWithAccessionedSequence", $reference_protein_name, "ReferenceDNASequence", "ReferenceMolecule", "LiteratureReference"],
										"EntityWithAccessionedSequence" => [$reference_protein_name, "ReferenceDNASequence", "ReferenceMolecule", "Pathway", "Complex", "Reaction"],
										);
# Specify the attribute of the dimension classes that should
# be used as a key.
my %reactome_to_dimension_key_hash = (
										"Pathway" => ["stableIdentifier.identifier", "stableIdentifier.identifier", "stableIdentifier.identifier", "identifier", "identifier", "identifier", "pubMedIdentifier"],
										"Reaction" => ["stableIdentifier.identifier", "stableIdentifier.identifier", "identifier", "identifier", "identifier", "pubMedIdentifier"],
										"Complex" => ["stableIdentifier.identifier", "identifier", "identifier", "identifier", "pubMedIdentifier"],
										"EntityWithAccessionedSequence" => ["identifier", "identifier", "identifier", "stableIdentifier.identifier", "stableIdentifier.identifier", "stableIdentifier.identifier"],
										);
# NN: Specify the direction of linking the instance to target class: default is forward direction i.e. 'undef', 'reverse' direction is for special cases.								
my %reactome_to_dimension_direction_hash = (
										"Pathway" => ["undef","undef","undef","undef","undef","undef","undef"],
										"Reaction" => ["undef","undef","undef","undef","undef","undef"],
										"Complex" => ["undef","undef","undef","undef","undef"],
										# replace the values of this hash with a reference to an anonymous array with as many 'undef' as there are elements in their corresponding %reactome_to_dimension_key_hash defined above
										# e.g. for Pathway, Reaction and Complex [undef,undef, undef, undef, undef] note that there should be a one to one match on the array size. Then put in a mechanism that once it sees undef
										# in the array, it assumes forward directionality by default
										#"EntityWithAccessionedSequence" => ["undef", "undef", "undef", "reverse", "reverse", "reverse",],
										"EntityWithAccessionedSequence" => ["undef", "undef", "undef", "reverse", "reverse", "reverse",],
										);
										
# Specify attributes of dimension classes that should be
# "exported" back to the main class.  These will appear
# as single main class columns, with comma-separated
# lists in the data fields.  The attributes are identified
# by their "internal name" as specified in the BioMart
# XML files e.g. complex.xml.  These columns tend to be used
# when filtering.
my %dimension_export_attribute_hash = (
						"LiteratureReference" => ["literaturereference__dm_pubmedidentifier"],
						$reference_protein_name => ["referencedatabase_uniprot", "referencedatabase_sgd"],
						"ReferenceDNASequence" => ["referencedatabase_entrez_gene", "referencedatabase_ensembl", "referencedatabase_wormbase", "referencedatabase_kegg_gene", "referencedatabase_embl", "referencedatabase_refseq", "referencedatabase_flybase"],
						"ReferenceMolecule" => ["referencedatabase_chebi", "referencedatabase_compound", "referencedatabase_pubchem_compound"],
						"Reaction" => ["stableIdentifier.identifier"],
						"Complex" => ["stableIdentifier.identifier"],
						"EntityWithAccessionedSequence" => ["stableIdentifier.identifier"],
						"Pathway" => ["stableIdentifier.identifier"],
						);

# If a partof hierarchy exists, let the Mart generator know which attribute
# is used for each instance class.  This is used when determinig which
# pathways are canonical.
my %part_of_attribute_hash = (
						"Pathway" => "hasEvent",
						);
						
# Depth in partof hierarchy for counting an instance as canonical.  Minimum
# value is 1.
my %canonical_depth_hash = (
						"Pathway" => "2",
						);

# The species displaynames are a bit different, depending on whether
# we are getting our data from the Reactome database or from a
# file.
#my @modcol_atts = ("hasModifiedResidue.psiMod.topLevelOntologyIdentifier","hasModifiedResidue.psiMod.identifier", "goBiologicalProcess.accession", "species._displayName", "species__displayname", "referencedatabase_tigr");
#my @modcol_substitutes = ("MOD:%s","MOD:%s", "GO:%s", [' ', '_'], [' ', '_'], ['.[0-9]+', '']);
my @modcol_atts = ("hasModifiedResidue.psiMod.identifier", "goBiologicalProcess.accession", "species._displayName", "species__displayname", "referencedatabase_tigr");
my @modcol_substitutes = ("MOD:%s", "GO:%s", [' ', '_'], [' ', '_'], ['.[0-9]+', '']);

# Multi-valued attributes will normally be ignored, but you can
# specify exceptions to this rule, where some attempt will then
# be made to scrunch multiple values into a single column.
my %allow_multiple_hash = ('referenceEntity' => 'referenceEntity', 'species' => 'species', 'psiMod' => 'psiMod', 'author' => 'author', 'hasModifiedResidue' => 'hasModifiedResidue');

my $instance_db_ids_ref = undef;
if (defined $opt_db_ids) {
	my %instance_db_ids = ();
	my @comma_separated_strings = split(/,/, $opt_db_ids);
	my $string_count = scalar(@comma_separated_strings);
	for ($i=0; $i<$string_count; $i++) {
		$instance_db_ids{$comma_separated_strings[$i]} = $comma_separated_strings[$i];
	}
	$instance_db_ids_ref = \%instance_db_ids;
}

my $bio_mart = GKB::BioMart->new();
$bio_mart->set_reactome_db_params($opt_host, $opt_port, $opt_db, $opt_user, $opt_pass);
if (defined $opt_bdb) {
	$bio_mart->set_biomart_db_name($opt_bdb);
}

if (!$opt_only_interactions) {
	# "Regular" BioMart generation, pull the required information directly
	# out of the Reactome database.
	my %expand_multiple_attribute_name_hash = ();
	my %expand_multiple_attribute_value_hash = ();
	if (!$opt_emn) {
		print STDERR "Creating proper expand_multiple_attribute hashes\n";
		
		# This creates the external-database-specific columns for ReferenceEntitys.
		# E.g. the UniProt column in the referencepeptidesequence table.
		# It is slow - you may want to suppress it while testing by using -emn.
		%expand_multiple_attribute_name_hash = ('ReferenceEntity' => 'referenceDatabase._displayName');
		%expand_multiple_attribute_value_hash = ('ReferenceEntity' => 'identifier');
	}
	
	# Set up the Mart generator
	$bio_mart->set_instance_ignored_attributes(\%instance_ignored_attributes);
	$bio_mart->set_expanded_instance_ignored_attributes(\%expanded_instance_ignored_attributes);
	$bio_mart->set_instance_ignored_composite_attributes(\%instance_ignored_composite_attributes);
	$bio_mart->allow_multiple_hash(\%allow_multiple_hash);
	$bio_mart->expand_multiple_attribute_name_hash(\%expand_multiple_attribute_name_hash);
	$bio_mart->expand_multiple_attribute_value_hash(\%expand_multiple_attribute_value_hash);
	$bio_mart->set_instance_substitute_attribute("DB_ID");
	$bio_mart->set_max_expanded_instance_depth($depth);
	$bio_mart->set_max_main_row_count($opt_mmax);
	$bio_mart->set_instance_db_ids($instance_db_ids_ref);
	$bio_mart->set_modcol(\@modcol_atts, \@modcol_substitutes);
	$bio_mart->dimension_export_attribute_hash(\%dimension_export_attribute_hash);
	$bio_mart->part_of_attribute_hash(\%part_of_attribute_hash);
	$bio_mart->canonical_depth_hash(\%canonical_depth_hash);
	$bio_mart->set_table_name_map(\%table_name_map);
	
	# Now cycle over the main classes and generate one dataset per class
	my @dataset_classes;
	my @dataset_keys;
	my $reactome_class;
	my @dimension_classes;
	my @dimension_keys;
	# NN: define new dimension_directions array to be passed over to 'generate_biomart_database' sub of Biomart.PM
	my @dimension_directions;
	for ($i=0; $i<scalar(@reactome_classes); $i++) {
		$reactome_class = $reactome_classes[$i];
		@dataset_classes = ($reactome_class);
		@dataset_keys = ($reactome_keys[$i]);
		
		@dimension_classes = @{$reactome_to_dimension_class_hash{$reactome_class}};
		@dimension_keys = @{$reactome_to_dimension_key_hash{$reactome_class}};
		# NN: Specify the direction of the links being followed
		@dimension_directions = @{$reactome_to_dimension_direction_hash{$reactome_class}};
		
		print STDERR "reactome_class=$reactome_class, dimension_classes=@dimension_classes\n";
		$bio_mart->generate_biomart_database(\@dataset_classes, \@dataset_keys, \@dimension_classes, \@dimension_keys, \@dimension_directions);
	}
	
	eval {
		$bio_mart->optimize_databases(\@reactome_classes);
	};
	
	print STDERR "Extraction from Reactome DB all done\n";
}

if (!$opt_interactions) {
	exit(0);
}

# Have a bash at getting information out of the protein-protein
# interaction files.
# This is very time consuming - at least a day on brie8.
#
# N.B. $GK_ROOT_DIR/website/html/download/current/cache is used
# to store information extracted from the file, and this is
# persistent.  It should be removed if the interaction file is changed.

# Define the columns in the main table that will be used to produce
# dimension tables.
my %dimension_table_columns = (
							   'gene1' => ['gene', 'ENSEMBL:', '\|'],
							   'gene2' => ['gene', 'ENSEMBL:', '\|'],
							   'entrez1' => ['entrez', 'Entrez Gene:', ','],
							   'entrez2' => ['entrez', 'Entrez Gene:', ','],
							   'protein1' => ['protein', '', ','],
							   'protein2' => ['protein', '', ','],
#							   'protein1' => ['protein', '', '<->'], # for default interaction file
#							   'protein2' => ['protein', '', '<->'], # for default interaction file
							   'protein_uniprot' => ['protein_uniprot', 'UniProt:', '<->'],
							   'protein_tigr' => ['protein_tigr', 'TIGR:', '<->'],
							   'protein_wormbase' => ['protein_wormbase', 'Wormbase:', '<->'],
							   'protein_ensembl' => ['protein_ensembl', 'ENSEMBL:', '<->'],
							   'protein_dictybase' => ['protein_dictybase', 'Dictybase:', '<->'],
							   'protein_cme_genome_project' => ['protein_cme_genome_project', 'CME Genome Project:', '<->'],
							   'protein_flybase' => ['protein_flybase', 'Flybase:', '<->'],
							   'protein_broad' => ['protein_broad', 'BROAD:', '<->'],
#							   'id' => ['', '\.[0-9]+', '<->'], # for default interaction file
#							   'protein_count' => ['protein_count', '', ','], # This shouldn't be needed - just experimenting
							   'pubmed' => ['pubmed', '', ','],
							   'id_pathway_db_id' => ['id_pathway_db_id', '', ','],
							   'id_reaction_db_id' => ['id_reaction_db_id', '', ','],
							   'id_complex_db_id' => ['id_complex_db_id', '', ','],
#							   'id_pathway_db_id' => ['id_pathway_db_id', 'Pathway:', ','], # for default interaction file
#							   'id_reaction_db_id' => ['id_reaction_db_id', 'Reaction:', ','], # for default interaction file
#							   'id_complex_db_id' => ['id_complex_db_id', 'Complex:', ','], # for default interaction file
							   'id_pathway_stable_id' => ['id_pathway_stable_id', '', ','],
							   'id_reaction_stable_id' => ['id_reaction_stable_id', '', ','],
							   'id_complex_stable_id' => ['id_complex_stable_id', '', ','],
							   'id_intact' => ['id_intact', '', ',']
							   );

# Used to create new columns by splitting up the contents of existing
# columns.
my @expand_multiple_attribute_name = (['protein', 'protein', ['UniProt.*', 'TIGR.*', 'Wormbase.*', 'ENSEMBL.*', 'Dictybase.*', 'CME Genome Project.*', 'Flybase.*', 'BROAD.*']],
#									  ['type', 'id', ['.*reaction', '.*complex']], # for default interaction file
#									  ['id_reaction', 'id_reaction', [['REACT_.*', 'stable_id'], ['Reaction:.*', 'db_id']]], # for default interaction file
#									  ['id_complex', 'id_complex', [['REACT_.*', 'stable_id'], ['Complex:.*', 'db_id']]] # for default interaction file
									  );
$bio_mart->dataset_from_files(
	'interaction', # dataset name
	"$GK_ROOT_DIR/website/html/download/current/homo_sapiens.interactions.intact.txt", # IntAct formatted file provides more information
#	"$GK_ROOT_DIR/website/html/download/current/homo_sapiens.interactions.intact.P62993.2.txt", # input files V1
#	"$GK_ROOT_DIR/website/html/download/current/homo_sapiens.interactions.intact.P62993.2.2.txt", # input files V2
#	"$GK_ROOT_DIR/website/html/download/current/homo_sapiens.interactions.intact.P62993.2.3.txt", # input files V3
#	"$GK_ROOT_DIR/website/html/download/Current/.*interactions.txt.*", # input files for default interaction file
	"species__displayname", # filename column
	['start_uc', ['\.interactions\.txt', ''], ['[^A-Za-z0-9]+', ' ']], # rules for converting filenames to species names
#	# From an interaction file generated with -col_grps uniprot_ids,context,source_ids,source_st_ids,participating_protein_count,intact
#	['protein1', 'protein2', 'type', 'id_pathway_db_id', 'id_reaction_db_id', 'id_complex_db_id', 'id_pathway_stable_id', 'id_reaction_stable_id', 'id_complex_stable_id', 'protein_count', 'id_intact'], # input file (pseudo) column names for IntAct interaction file V1
	# From an interaction file generated with -col_grps ids,context,source_ids,source_st_ids,intact,participating_protein_count,lit_refs,intact
	['protein1', 'gene1', 'entrez1', 'protein2', 'gene2', 'entrez2', 'type', 'id_pathway_db_id', 'id_reaction_db_id', 'id_complex_db_id', 'id_pathway_stable_id', 'id_reaction_stable_id', 'id_complex_stable_id', 'pubmed', 'protein_count', 'id_intact'], # input file (pseudo) column names for IntAct interaction file V2
#	# From an interaction file generated without the -col_grps option (default Reactome Download page file)
#	['protein1', 'gene1', 'entrez1', 'protein2', 'gene2', 'entrez2', 'type', 'id', 'pubmed'], # input file (pseudo) column names for default interaction file
	\%dimension_table_columns, # columns to turn into dimension tables, with table names, deleteable junk and separators
#	0, # Start row (first row is 0)
	3, # Start row (first row is 0)
#	1, # Start row (first row is 0) for default interaction file
	\@expand_multiple_attribute_name, # allows a single column to be split up according to the value in another column
	['protein_count', '<', 4] # row filter
	);

@reactome_classes = ("Interaction");
$bio_mart->optimize_databases(\@reactome_classes);
	
print STDERR "Interactions all done\n";

print STDERR "martify_reactome.pl has finished its job\n";

