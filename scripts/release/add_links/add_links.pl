#!/usr/local/bin/perl  -w

# Given, as a minimum, a database name, this script will
# insert links from Reactome to other databases, such
# as UniProt.  These will appear as hyperlinks on
# displayed web pages.

use Getopt::Long;
use strict;
use GKB::Config;

our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db,$opt_debug,$opt_edb,$opt_db_ids);

# Parse commandline
my $usage = "Usage: $0 -user db_user -host db_host -pass db_pass -port db_port -db db_name -edb ENSEMBL_db -db_ids 'id1,id2,..'\n";
&GetOptions("user:s", "host:s", "pass:s", "port:i", "db=s", "debug", "test", "db_ids:s");
$opt_db || die $usage;

my $gk_root_dir = $GK_ROOT_DIR;
my $add_links_to_single_resource = undef;
if (!(defined $gk_root_dir) || $gk_root_dir eq '') {
	# If no root directory is defined for GKB,
	# assume that the current directory is GKB/scripts/release.
	# A bit nieave, we can but try, sigh.
	my $curr_work_dir = &Cwd::cwd();
		
	$add_links_to_single_resource = "$curr_work_dir/add_links_to_single_resource.pl";
} else {
	$add_links_to_single_resource = "$gk_root_dir/scripts/release/add_links/add_links_to_single_resource.pl";
}
if (!(-e $add_links_to_single_resource)) {
	# Last resort
	my $home = $ENV{HOME};
	if (defined $home && !($home eq '')) {
		$add_links_to_single_resource = "$home/GKB/scripts/release/add_links_to_single_resource.pl";
	}
}
if (!(defined $add_links_to_single_resource)) {
    print STDERR "$0: ERROR - cannot find add_links_to_single_resource.pl\n";
    exit(1);
}
if (!(-e $add_links_to_single_resource)) {
    print STDERR "$0: ERROR - file $add_links_to_single_resource does not exist!\n";
    exit(1);
}

if (!(defined $opt_host) || $opt_host eq '') {
	$opt_host = $GK_DB_HOST;
}
if (!(defined $opt_user) || $opt_user eq '') {
	$opt_user = $GK_DB_USER;
}
if (!(defined $opt_pass) || $opt_pass eq '') {
	$opt_pass = $GK_DB_PASS;
}
if (!(defined $opt_port) || $opt_port eq '') {
	$opt_port = $GK_DB_PORT;
}

# Pre-create the command line options associated with database access
my $reactome_db_options = "-db $opt_db";
if (defined $opt_host && !($opt_host eq '')) {
	$reactome_db_options .= " -host $opt_host";
}
if (defined $opt_user && !($opt_user eq '')) {
	$reactome_db_options .= " -user $opt_user";
}
if (defined $opt_pass && !($opt_pass eq '')) {
	# Put a backslash in front of characters that have special meaning to the shell
	my $pass = $opt_pass;
	if ($pass =~ /\$/) {
		$pass =~ s/\$/\\\$/g;
	}
	$reactome_db_options .= " -pass $pass";
}
if (defined $opt_port && !($opt_port eq '')) {
	$reactome_db_options .= " -port $opt_port";
}
if (defined $opt_edb && !($opt_edb eq '')) {
	$reactome_db_options .= " -edb $opt_edb";
}

my @resources = (
	'ENSGReferenceDNASequenceToReferencePeptideSequence',
	'EntrezGeneToUniprotReferenceDNASequence',
	'BioGPSGeneToUniprotReferenceDNASequence',
	'CTDGeneToUniprotReferenceDNASequence',
	'DbSNPGeneToUniprotReferenceDNASequence',
	'GenecardsReferenceDatabaseToReferencePeptideSequence',
	'OmimReferenceDNASequenceToReferencePeptideSequence',
	'UCSCReferenceDatabaseToReferencePeptideSequence',
	'RefseqReferenceDatabaseToReferencePeptideSequence',
	'RefseqReferenceRNASequenceToReferencePeptideSequence',
	'KEGGReferenceGeneToReferencePeptideSequence',
	'IntActDatabaseIdentifierToComplexOrReactionlikeEvent',
	'BioModelsEventToDatabaseIdentifier',
	'FlyBaseToUniprotReferenceDNASequence',
	'OrphanetToUniprotReferenceDNASequence',
	'PDBToReferencePeptideSequence',
	'DOCKBlasterToUniprotDatabaseIdentifier',
	'RHEAIdentifierToReactionlikeEvent',
);

my $resource;
my $cmd;
my $broken_resource_count = 0;
foreach $resource (@resources) {
    if (!(defined $resource) || $resource eq '') {
    	print STDERR "$0: WARNING - missing resource value!\n";
    	next;
    }
    $cmd = "$add_links_to_single_resource $reactome_db_options -res $resource";
    if (system($cmd) != 0) {
    	print STDERR "$0: WARNING - something went wrong while executing '$cmd'!!\n";
    	$broken_resource_count++;
    }
}

if ($broken_resource_count > 0) {
    print STDERR "$0: $broken_resource_count linkers failed to run to completion, please check the diagnostic output!\n";
}
print STDERR "$0 has finished its job\n";
