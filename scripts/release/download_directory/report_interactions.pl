#!/usr/local/bin/perl
use strict;

# Make sure you don't have "competing" libraries...
# for use @CSHL
use lib "/usr/local/gkb/modules";

use GKB::Config;
use GKB::DBAdaptor;
use GKB::Instance;
use GKB::Utils;
use GKB::InteractionGenerator;

use autodie;
use Getopt::Long;

our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db,$opt_debug,$opt_sp,$opt_output,$opt_useDB_ID,$opt_db_ids,$opt_col_grps,$opt_headers,$opt_uniq,$opt_xrefs,$opt_intact, $opt_mitab,$opt_pathogenic);

(@ARGV) || die "Usage: $0 -user db_user -host db_host -pass db_pass -port db_port -db db_name -sp 'species name' -o 'output filename' -useDB_ID -db_ids 'id1,id2,..' -col_grps 'group1,group2,..' -headers 'header1,header2,..' -uniq -xrefs -intact -mitab -pathogenic -debug\n";

&GetOptions(
    "user:s",
    "host:s",
    "pass:s",
    "port:i",
    "db:s",
    "debug",
    "sp:s",
    "output:s",
    "useDB_ID",
    "db_ids:s",
    "col_grps:s",
    "headers:s",
    "uniq",
    "xrefs",
    "intact",
    "mitab",
    "pathogenic"
);

$opt_db || die "Need database name (-db).\n";

my $dba = GKB::DBAdaptor->new
    (
     -user   => $opt_user || $GKB::Config::GK_DB_USER,
     -host   => $opt_host || $GKB::Config::GK_DB_HOST,
     -pass   => $opt_pass || $GKB::Config::GK_DB_PASS,
     -port   => $opt_port || $GKB::Config::GK_DB_PORT,
     -dbname => $opt_db,
     -DEBUG => $opt_debug
     );

my $protein_class = &GKB::Utils::get_reference_protein_class($dba);

my $query;
my $interaction_generator = new GKB::InteractionGenerator();
$interaction_generator->set_dba($dba);

#Define PSI_MITAB format (setting the mitab flag will be sufficient to produce PSI-MITAB formatted interactions)
if (defined $opt_mitab) {
    $opt_useDB_ID = 1; #not sure whether this is needed....(?)
    $opt_col_grps = 'ids,context';
#    $opt_col_grps = 'ids,context,lit_refs';  #for now, we take the Reactome paper as litref for all entries, so no need to extract individual litrefs
    $opt_uniq = 1;
}

# Limit DB_IDs to the list in the command line, if available, otherwise
# use all DB_IDs.  If you are going for the command line option, the
# DB_IDs should be from ReferencePeptideSequence instances.
if (defined $opt_db_ids) {
	my @db_ids = split(/,/, $opt_db_ids);
	$query = [['DB_ID','=',\@db_ids]];
} else {
	$query = [['DB_ID','IS NOT NULL',[]]];
}

# Column groups can be used to specify what columns to print in the interaction
# report.  Acceptable groups include 'ids', 'context' and 'intact'.  By default
# the column groups for the Reactome download page are used.
if (defined $opt_col_grps) {
	my @col_grps = split(/,/, $opt_col_grps);
	my %column_groups = ();
	foreach my $col_grp (@col_grps) {
		$column_groups{$col_grp} = 1;
	}
	$interaction_generator->set_column_groups(\%column_groups);
}

# Headers allow various additional pieces of information to be included
# at the beginning of the interaction report.  These header lines
# all start with a hash # symbol.  A list of headers is supplied,
# separated by commas.  Acceptable values include 'title', 
if (defined $opt_headers) {
	my @headers = split(/,/, $opt_headers);
	foreach my $header (@headers) {
		if ($header eq "title") {
			$interaction_generator->set_title_headers_flag(1);
		}
		if ($header eq "table") {
			$interaction_generator->set_table_headers_flag(1);
		}
	}
}
# This header line will always be shown.
$interaction_generator->set_interaction_count_headers_flag(1);

# Decide whether all lines should be printed, or only unique ones. 

if (defined $opt_uniq) {
	$interaction_generator->set_line_cache({});
}

# Only show lines that also have IntAct IDs. 
if (defined $opt_intact) {
	$interaction_generator->set_display_only_intact_lines(1);
}

if ($opt_sp) {
    push @{$query}, ['species.name', '=', [$opt_sp]];
}

my $reference_peptide_sequences = $dba->fetch_instance_by_remote_attribute($protein_class,$query);

unless (@{$reference_peptide_sequences}) {
    print "No $protein_class instances. Is the species name correct?\n";
    exit;
}

$opt_useDB_ID and $GKB::Instance::USE_STABLE_ID_IN_DUMPING = 0;

open(my $output_fh, '>', $opt_output) if $opt_output;
$output_fh ||= *STDOUT;
open(my $pathogenic_output_fh, '>', "pathogenic_$opt_output") if $opt_output && $opt_pathogenic;
$pathogenic_output_fh ||= *STDOUT;

# This will also get IntAct IDs for interactions, but only for
# those arising from reactions or complexes involving 3 or
# fewer proteins.

my $interactions_hash = $interaction_generator->find_interactors_for_ReferenceSequences($reference_peptide_sequences, 3, $opt_mitab);
my $normal_interactions_hash = $interactions_hash->{'normal'};
my $pathogenic_interactions_hash = $interactions_hash->{'pathogenic'};
if ($opt_xrefs) {
    # Insert IntAct cross-references into database.
    $interaction_generator->insert_intact_xrefs($normal_interactions_hash, 3);
    if ($opt_pathogenic) {    
        $interaction_generator->insert_intact_xrefs($pathogenic_interactions_hash, 3);
    }
}

if (defined $opt_mitab) {
    $interaction_generator->print_psi_mitab_interaction($normal_interactions_hash, $output_fh);
    if ($opt_pathogenic) {
        $interaction_generator->print_psi_mitab_interaction($pathogenic_interactions_hash, $pathogenic_output_fh);
    }
} else {
    my $normal_interactions_array = $interaction_generator->interaction_hash_2_split_array($normal_interactions_hash);
    $interaction_generator->print_interaction_report($normal_interactions_array, $output_fh);
    
    if ($opt_pathogenic) {
        my $pathogenic_interactions_array = $interaction_generator->interaction_hash_2_split_array($pathogenic_interactions_hash);
        $interaction_generator->print_interaction_report($pathogenic_interactions_array, $pathogenic_output_fh);
    }
}
