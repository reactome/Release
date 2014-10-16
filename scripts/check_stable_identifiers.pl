#!/usr/local/bin/perl  -w

# Checks for repeated stable identifiers and other things
# that could indicate problems with the stable identifier
# generation process.
#
# Options:
#
# -fix			If specified, the script will attempt to
#				fix the problems it finds.  Use with care!
# -rel			Do checks on release databases, rather than slices.

# TODO: This script needs to be merged with
# QA_scripts/check_stable_identifiers.pl.

# Make sure you don't have "competing" libraries...
# for use @CSHL
use lib "/usr/local/reactomes/Reactome/production/GKB/modules";
# for use @HOME
use lib "$ENV{HOME}/bioperl-1.0";
use lib "$ENV{HOME}/GKB/modules";
use GKB::Instance;
use GKB::DBAdaptor;
use GKB::StableIdentifiers;
use GKB::StableIdentifiers::Check;
use Data::Dumper;
use Getopt::Long;
use strict;

# Command line options have been deliberately chosen so that they
# are identical to IDGenerationCommandLine.java.
our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_idbname,$opt_fix,$opt_rel,$opt_crnum,$opt_project,$opt_debug);

(@ARGV) || die "Usage: -user db_user -host db_host -pass db_pass -port db_port -idbname st_id_db_name -fix -rel -crnum release_num -project project_name-debug\n";

&GetOptions("user:s", "host:s", "pass:s", "port:i", "db:s", "idbname:s", "crnum:s", "project:s", "fix", "rel", "debug");

if (!(defined $opt_idbname)) {
	$opt_idbname = "test_reactome_stable_identifiers";
}
if (!(defined $opt_fix)) {
	$opt_fix = 0;
}

my $identifier_database_dba = GKB::DBAdaptor->new
    (
     -user   => $opt_user || '',
     -host   => $opt_host,
     -pass   => $opt_pass,
     -port   => $opt_port,
     -dbname => $opt_idbname,
     -DEBUG => $opt_debug
     );

my $si = GKB::StableIdentifiers->new(undef);
$si->set_identifier_database_dba($identifier_database_dba);
my $check = GKB::StableIdentifiers::Check->new($si);
my $stable_identifier;
my @stable_identifiers;
if (!$opt_crnum) {
	$opt_crnum = $si->get_most_recent_release_num();
}
if ($opt_rel) {
	$check->set_use_release_flag(1);
}

# Look for repeating stable IDs
@stable_identifiers = $check->find_duplicated_stable_identifiers($opt_fix);
if (scalar(@stable_identifiers)>0) {
	print STDERR "The following " . scalar(@stable_identifiers) . " stable IDs are duplicated for $opt_idbname:\n";
	
	foreach $stable_identifier (@stable_identifiers) {
		print STDERR "    $stable_identifier";
	}
} else {
	print STDERR "No stable IDs are duplicated for $opt_idbname\n";
}

# Look for stable IDs that are present in the release but not in the
# stable ID database
@stable_identifiers = $check->find_stable_identifiers_only_in_release($opt_fix, $opt_crnum, $opt_project);
if (scalar(@stable_identifiers)>0) {
	print STDERR "The following " . scalar(@stable_identifiers) . " stable IDs are only in the release $opt_crnum:\n";
	
	foreach $stable_identifier (@stable_identifiers) {
		print STDERR "    $stable_identifier";
	}
} else {
	print STDERR "All stable IDs in release $opt_crnum are also in $opt_idbname\n";
}

# Look for stable IDs that are present both in the release and in the
# stable ID database but with different version numbers
@stable_identifiers = $check->find_version_incompatibility_with_release($opt_fix, $opt_crnum, $opt_project);
if (scalar(@stable_identifiers)>0) {
	print STDERR "The following " . scalar(@stable_identifiers) . " stable IDs in release $opt_crnum have different versions to those in $opt_idbname:\n";
	
	foreach $stable_identifier (@stable_identifiers) {
		print STDERR "    $stable_identifier";
	}
} else {
	print STDERR "All versions in release $opt_crnum are consistent with $opt_idbname\n";
}

# Look into the stable ID database and find ReleaseId instances which
# have no associated StableIdentifierVersions.
my @orphan_release_ids = $check->find_orphan_release_ids();
if (scalar(@orphan_release_ids)>0) {
	print STDERR scalar(@orphan_release_ids) . " ReleaseId instances in $opt_idbname are orphans\n";
} else {
	print STDERR "$opt_idbname contains no orphan ReleaseId instances\n";
}

# See if DOIs are consistent with stable IDs
my @stable_ids_not_in_dois = $check->find_stable_ids_not_in_dois($opt_fix, $opt_crnum, $opt_project);
if (scalar(@stable_ids_not_in_dois)>0) {
	print STDERR "The following " . scalar(@stable_ids_not_in_dois) . " stable IDs in release $opt_crnum have no correspondence with associated DOIs:\n";
	
	foreach $stable_identifier (@stable_ids_not_in_dois) {
		print STDERR "    $stable_identifier";
	}
} else {
	print STDERR "All release $opt_crnum database DOIs contain the corresponding stable IDs\n";
}

#my %version_number_gaps = $check->find_version_number_gaps();
#@stable_identifiers = keys(%version_number_gaps);
#if (scalar(@stable_identifiers)>0) {
#	print STDERR "The following " . scalar(@stable_identifiers) . " stable IDs have version number gaps in $opt_idbname:\n";
#	
#	foreach $stable_identifier (@stable_identifiers) {
#		print STDERR "    $stable_identifier";
#	}
#} else {
#	print STDERR "No stable IDs have version number gaps in $opt_idbname\n";
#}

#my %release_number_gaps = $check->find_release_number_gaps($opt_fix);
#@stable_identifiers = keys(%release_number_gaps);
#if (scalar(@stable_identifiers)>0) {
#	print STDERR "The following " . scalar(@stable_identifiers) . " stable IDs have release number gaps in $opt_idbname:\n";
#	
#	foreach $stable_identifier (@stable_identifiers) {
#		print STDERR "    $stable_identifier";
#	}
#} else {
#	print STDERR "No stable IDs have release number gaps in $opt_idbname\n";
#}
