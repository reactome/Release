#!/usr/local/bin/perl  -w

# Prints out the number of instances found for each instance
# class in each release.  Needs database params for identifier
# database.

# Make sure you don't have "competing" libraries...
# for use @CSHL
use lib "/usr/local/reactomes/Reactome/production/GKB/modules";
# for use @HOME
use lib "$ENV{HOME}/bioperl-1.0";
use lib "$ENV{HOME}/GKB/modules";
use GKB::Instance;
use GKB::DBAdaptor;
use Data::Dumper;
use Getopt::Long;
use GKB::StableIdentifiers;
use strict;

our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db,$opt_release_num,$opt_release_name);

(@ARGV) || die "Usage: -user db_user -host db_host -pass db_pass -port db_port -db db_name\n";

&GetOptions("user:s", "host:s", "pass:s", "port:i", "db:s");

$opt_db || die "Need database name (-db).\n";

my $dba = GKB::DBAdaptor->new
    (
     -user   => $opt_user || '',
     -host   => $opt_host,
     -pass   => $opt_pass,
     -port   => $opt_port,
     -dbname => $opt_db,
     );

my $si = GKB::StableIdentifiers->new(undef);

# Create a hash indexed by release number
my %instance_class_hashes = ();
my %release_dba_hash = ();
my $releases = $dba->fetch_instance(-CLASS => 'Release');
my $release;
my $nums;
my $num;
foreach $release (@{$releases}) {
	my %instance_class_hash = ();
	$nums = $release->num;
	$num = $nums->[0];
	my $release_dba = $si->get_dba_from_db_name($si->get_db_name_from_release_num($num));
	# Populate with subhashes, which will be indexed by
	# instance class.
	$instance_class_hashes{$num} = \%instance_class_hash;
	$release_dba_hash{$num} = $release_dba;
}

print STDERR "keys instance_class_hashes=" . keys(%instance_class_hashes) . "\n";

# Collect counts of instances of all event type for each release
my $stable_identifier_db_ids = $dba->fetch_db_ids_by_class("StableIdentifier");
my $stable_identifiers;
my $stable_identifier;
my $stable_identifier_versions;
my $stable_identifier_version;
my $release_ids;
my $release_id;
my $db_ids;
my $db_id;
my $release_dba;
my $instances;
my $instance;
my $class;
my $instance_class_hash;
my $instance_class;
foreach my $stable_identifier_db_id (@{$stable_identifier_db_ids}) {
	$stable_identifiers = $dba->fetch_instance_by_db_id($stable_identifier_db_id);
	$stable_identifier = $stable_identifiers->[0];
	$stable_identifier_versions = $stable_identifier->stableIdentifierVersion;
	foreach $stable_identifier_version (@{$stable_identifier_versions}) {
		$release_ids = $stable_identifier_version->releaseIds;
		foreach $release_id (@{$release_ids}) {
			$releases = $release_id->release;
			$release = $releases->[0];
			$db_ids = $release_id->instanceDB_ID;
			$db_id = $db_ids->[0];
			$nums = $release->num;
			$num = $nums->[0];
			$release_dba = $release_dba_hash{$num};
			$instances = $release_dba->fetch_instance_by_db_id($db_id);
			$instance = $instances->[0];
			if (!$instance) {
				print "Spiky crikey, no instance found in release $num for DB_ID $db_id\n";
				next;
			}
			$class = $instance->class();
			$instance_class_hash = $instance_class_hashes{$num};
			$instance_class = $instance_class_hash->{$class};
			if (defined $instance_class) {
				$instance_class++;
			} else {
				$instance_class = 1;
			}
			$instance_class_hash->{$class} = $instance_class;
			$instance_class_hashes{$num} = $instance_class_hash;
		}
	}
}

# Print results
foreach $num (keys(%instance_class_hashes)) {
	print "Release number $num:\n";
	$instance_class_hash = $instance_class_hashes{$num};
	foreach $class (keys(%{$instance_class_hash})) {
		$instance_class = $instance_class_hash->{$class};
		print "    $class: $instance_class\n";
	}
}

$dba->db_handle->disconnect;
