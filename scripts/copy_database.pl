#!/usr/local/bin/perl  -w

# Copies instances hierarchically from a source Reactome
# database to a target Reactome database.  The names of
# both databases need to be specified as arguments.  With
# no DB_IDs specified, performs a one-to one copy.  If
# you specify DB_IDs of pathways on the command line,
# everything except the pathways NOT specified will be
# copied.
#
# Options:
#
# -dbs sdb			Source Reactome database
# -dbt tdb			Target Reactome database
# -db_ids			Pathways to be copied

# Make sure you don't have "competing" libraries...
# for use @CSHL
use lib "/usr/local/gkb/modules";
# for use @HOME
use lib "$ENV{HOME}/bioperl-1.0";
use lib "$ENV{HOME}/GKB/modules";
use GKB::Instance;
use GKB::DBAdaptor;
use GKB::StableIdentifiers;
use Data::Dumper;
use Getopt::Long;
use strict;

our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_sdb,$opt_tdb,$opt_db_ids,$opt_debug);

(@ARGV) || die "Usage: -user db_user -host db_host -pass db_pass -port db_port -sdb source_db_name -tdb target_db_name -db_ids id1,id2,.. -debug\n";

&GetOptions("user:s", "host:s", "pass:s", "port:i", "sdb:s", "tdb:s", "db_ids:s", "debug");

if (!(defined $opt_sdb)) {
	print STDERR "You must specify a source Reactome database, e.g. test_slice_22\n";
	exit(1);
}
if (!(defined $opt_tdb)) {
	print STDERR "You must specify a target Reactome database, e.g. test_reactome_22\n";
	exit(1);
}
if ($opt_sdb eq $opt_tdb) {
	print STDERR "Source database ($opt_sdb) and target database ($opt_tdb) are the same, no need to do a copy!!\n";
	exit(0);
}

my $source_dba = GKB::DBAdaptor->new
    (
     -user   => $opt_user || '',
     -host   => $opt_host,
     -pass   => $opt_pass,
     -port   => $opt_port,
     -dbname => $opt_sdb,
     -DEBUG => $opt_debug
     );

my $ontology = $source_dba->ontology;
print STDERR "ontology=$ontology\n";
print STDERR "New DBAdaptor for $opt_tdb\n";
my $target_dba = GKB::DBAdaptor->new
    (
     -user   => $opt_user || '',
     -host   => $opt_host,
     -pass   => $opt_pass,
     -port   => $opt_port,
     -ontology => $ontology,
     -DEBUG => $opt_debug
     );
print STDERR "DROP DATABASE IF EXISTS $opt_tdb\n";
$target_dba->execute("DROP DATABASE IF EXISTS $opt_tdb");
print STDERR "Create $opt_tdb\n";
$target_dba->create_database($opt_tdb);

my $db_id;
my $pathway_db_id;
my $class;

print STDERR "Limit DB_IDs\n";

# Limit DB_IDs to the list in the command line, if available, otherwise
# use all DB_IDs.  If you are going for the command line option, the
# DB_IDs should be from ReferencePeptideSequence instances.
my %do_copy_db_id_hash = ();
my %type_hash = ();
my @pathway_db_ids = ();
if (defined $opt_db_ids) {
	@pathway_db_ids = split(/,/, $opt_db_ids);
	
	# For each pathway, recursively descend the event hierarchy,
	# right down to the ReferenceEntitys, adding their DB_IDs
	# to our hash of things that we do so want to copy.
	my $instances;
	my $out;
	my $t;
	my $follow_path;
	foreach $pathway_db_id (@pathway_db_ids) {
		$do_copy_db_id_hash{$pathway_db_id} = 1;
		
		print STDERR "Finding instances associated with DB_ID=$pathway_db_id\n";
		
		$instances = $source_dba->fetch_instance_by_db_id($pathway_db_id);
		if (defined $instances && scalar(@{$instances})>0) {
			($out, $t, $follow_path) = $source_dba->follow_attributes(-INSTANCE => $instances->[0]);
			foreach $pathway_db_id (keys(%{$follow_path})) {
				$do_copy_db_id_hash{$pathway_db_id} = 1;
				$type_hash{$follow_path->{$pathway_db_id}->class()} = 1;
			}
		}
		
		print STDERR "Found " . scalar(keys(%{$follow_path})) . " instances associated with DB_ID=$pathway_db_id\n";
		
	}
	
	print STDERR "Classes found: ";
	foreach $class (keys(%type_hash)) {
		print STDERR "$class, ";
	}
	print STDERR "\n";
}

# Create a hash of the things that we don't want to copy over.
my %dont_copy_db_id_hash = ();
my $db_ids;
foreach $class (sort(keys(%type_hash))) {
	print STDERR "Excluding instances of class: $class\n";
	
	$db_ids = $source_dba->fetch_db_ids_by_class($class);
	foreach $db_id (@{$db_ids}) {
		$dont_copy_db_id_hash{$db_id} = 1;
	}
}

print STDERR "Now copying source instances to target database\n";

my $source_instances = $source_dba->fetch_all_class_instances_as_shells('DatabaseObject');
my $source_instance;
my $source_instance_count = scalar(@{$source_instances});
my $source_instance_num = 0;
my $copied_instance_num = 0;
foreach $source_instance (@{$source_instances}) {
	if ($source_instance_num%500 == 0) {
		print STDERR "source_instance_num=$source_instance_num (" . (100 * $source_instance_num)/$source_instance_count . "%)\n";
	}
	
	$db_id = $source_instance->db_id();
	if ($do_copy_db_id_hash{$db_id} || !($dont_copy_db_id_hash{$db_id})) {
		$source_instance->inflate();
		
		foreach $pathway_db_id (@pathway_db_ids) {
			if ($db_id == $pathway_db_id) {
				print STDERR "Copying instance " . $source_instance->class() . ".$db_id: " . $source_instance->_displayName->[0] . "\n";
			}
		}
		$target_dba->store($source_instance, 1);
		$copied_instance_num++;
	}
	
	$source_instance_num++;
}

print STDERR "Copied a total of $copied_instance_num instances\n";

