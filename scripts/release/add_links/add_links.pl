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

use constant EXE => './add_links_to_single_resource.pl';

our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db,$opt_debug,$opt_edb,$opt_db_ids);

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

foreach $resource (@resources) {
    if (!(defined $resource) || $resource eq '') {
    	print STDERR "$0: WARNING - missing resource value!\n";
    	next;
    }

    print STDERR "$resource\n";
    run($exe, "$reactome_db_options -res $resource", $resource);
}

# pause until all jobs are done
while(1) {
    sleep 300 and next if check_running($exe);
    last;
}

my (@failed, @passed);
if (-e "resources.txt") {
    open IN, "resources.txt";
    while (<IN>) { 
	push @passed, $_ if /PASSED/;
	push @failed, $_ if /FAILED/;
    }
}

if (@failed) {
    my $failed = @failed;
    print STDERR "$0: $failed linkers failed to run to completion, please check the diagnostic output!\n",
    join("\n",@failed), "\n";
}
if (@passed) {
    print STDERR join("\n",@passed), "\n";
}

print STDERR "$0 has finished its job\n";

sub run {
    my $exe  = shift;
    my $args = shift;
    my $resource = shift;

    my $stdout = "logs/$resource.stdout.txt";
    my $stderr = "logs/$resource.stderr.txt";

    my $howmany = 99;

    while ($howmany > 6) {
	$howmany = check_running($exe);
	print STDERR "$howmany processes running at this time\n"; 
	sleep 300 if $howmany > 6;
    }

    print "running $exe $args\n";
    system "./run.pl $exe $args > $stdout 2> $stderr &";
}


sub check_running {
    my $exe = shift;
    my @running = `ps aux |grep $exe | grep -v 'run.pl' | grep -v grep`;
    #print STDERR "These are running:\n@running\n";
    return scalar(@running);
}
