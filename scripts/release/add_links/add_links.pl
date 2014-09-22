#!/usr/local/bin/perl  -w

# Given, as a minimum, a database name, this script will
# insert links from Reactome to other databases, such
# as UniProt.  These will appear as hyperlinks on
# displayed web pages.

BEGIN {
    my ($path) = $0 =~ /^(\S+)$/;
    my @a = split('/',$path);
    pop @a;
    if (@a && !$a[0]) {
        $#a = $#a - 2;
    } else {
        push @a, ('..','..','..');
    }
    push @a, 'modules';
    my $libpath = join('/', @a);
    unshift (@INC, $libpath);
}

use Getopt::Long;
use strict;
use GKB::Config;
use Cwd;

use constant EXE  => './add_links_to_single_resource.pl';
use constant LOG  => 'logs2';
use constant RLOG => 'resource_log2.txt';

our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db,$opt_debug,$opt_edb,$opt_db_ids);
my $pid;

# Parse commandline
my $usage = "Usage: $0 -user db_user -host db_host -pass db_pass -port db_port -db db_name -edb ENSEMBL_db -db_ids 'id1,id2,..'\n";
&GetOptions("user:s", "host:s", "pass:s", "port:i", "db=s", "debug", "test", "db_ids:s");
$opt_db || die $usage;


my $gk_root_dir = $GK_ROOT_DIR;
my $pwd = &Cwd::cwd();

unless ($pwd =~ m!/scripts/release/add_links$! && -d '../../../modules') {
    die "Current working directory is $pwd:\n",
    "Please run this script from $gk_root_dir/scripts/release/add_links.\n"; 
}

my $exe = EXE;

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
#	'ENSGReferenceDNASequenceToReferencePeptideSequence',
#	'EntrezGeneToUniprotReferenceDNASequence',
#	'BioGPSGeneToUniprotReferenceDNASequence',
#	'CTDGeneToUniprotReferenceDNASequence',
#	'DbSNPGeneToUniprotReferenceDNASequence',
	'PROToReferencePeptideSequence',
	'GenecardsReferenceDatabaseToReferencePeptideSequence',
 #       'PDBToReferencePeptideSequence',
#	'OmimReferenceDNASequenceToReferencePeptideSequence',
#	'UCSCReferenceDatabaseToReferencePeptideSequence',
#	'RefseqReferenceDatabaseToReferencePeptideSequence',
#	'RefseqReferenceRNASequenceToReferencePeptideSequence',
#	'KEGGReferenceGeneToReferencePeptideSequence',
#	'IntActDatabaseIdentifierToComplexOrReactionlikeEvent',
#	'BioModelsEventToDatabaseIdentifier',
#	'FlyBaseToUniprotReferenceDNASequence',
#	'OrphanetToUniprotReferenceDNASequence',
#	'DOCKBlasterToUniprotDatabaseIdentifier',
#	'RHEAIdentifierToReactionlikeEvent',
);

my $resource;
my $cmd;

unlink RLOG if -e RLOG;

foreach $resource (@resources) {
    if (!(defined $resource) || $resource eq '') {
    	print STDERR "$0: WARNING - missing resource value!\n";
    	next;
    }

    run($exe, "$reactome_db_options -res $resource", $resource);
}

my (@failed, @passed);
if (-e RLOG) {
    open IN, RLOG or die $!;
    while (<IN>) { 
	my ($resource) = split;
	push @passed, $resource if /PASSED/;
	push @failed, $resource if /FAILED/;
    }
}

if (@failed) {
    my $failed = @failed;
    print STDERR "$0: $failed linkers failed to run to completion, please check the diagnostic output!\n",
    my %failed = map { $_ => 1 } @failed;
    print "The following resources failed:\n", join("\n", (sort keys %failed)), "\n";
}
if (@passed) {
    my %passed = map { $_ => 1 } @passed;
    print "The following resources passed:\n", join("\n", (sort keys %passed)), "\n";
}

print STDERR "$0 has finished its job\n";

sub run {
    my $exe  = shift;
    my $args = shift;
    my $resource = shift;

    print "Running $exe $args\n";
    _run($exe,$resource,$args);
}

sub _run {
    my ($exe,$resource,$args) = @_;

    chomp(my $timestamp = `date`);
    print STDERR "$timestamp Starting $args\n";

    my $retval = system "$exe $args > ".LOG."/$resource.out 2>&1";

    chomp($timestamp = `date`);
    open LOG, ">>" . RLOG or die $!;
    my $state = $retval ? 'FAILED' : 'PASSED';
    print LOG "$resource $state $retval $timestamp\n";
    close LOG;
}

