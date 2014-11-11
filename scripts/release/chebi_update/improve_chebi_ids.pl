#!/usr/local/bin/perl  -w

# Given, as a minimum, a database name, this script will
# check all existing ChEBI IDs against the ChEBI database,
# and in cases where they have been turned into secondary
# identifiers, will insert up-to-date primary identifiers
# into the database.

use Getopt::Long;
use strict;
use lib '/usr/local/gkb/modules';
use GKB::DBAdaptor;
use GKB::SOAPServer::ChEBI;

our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db,$opt_debug);

# Parse commandline
my $usage = "Usage: $0 -user db_user -host db_host -pass db_pass -port db_port -db db_name'\n";
&GetOptions("user:s", "host:s", "pass:s", "port:i", "db=s", "debug");
$opt_db || die $usage;

my $dba = GKB::DBAdaptor->new
    (
     -dbname => $opt_db,
     -user   => $opt_user,
     -host   => $opt_host,
     -pass   => $opt_pass,
     -port   => $opt_port,
     -DEBUG => $opt_debug
     );
if (!(defined $dba)) {
	print STDERR "$0: could not connect to database $opt_db, aborting\n";
	exit(1);
}

open OUT, ">chebi.wiki";
print "NOTE: This script prints stable chEBI ids to its report.  Should chEBI change its ids, this will have to be addressed\n";

my $chebi = GKB::SOAPServer::ChEBI->new();
my $identifier;
my $up_to_date_identifier;
my $chebi_name;
my $reference_molecule;
my $reference_molecule_db_ids = $dba->fetch_db_ids_by_class("ReferenceMolecule");
my $molecule_identifier_counter = 0;
my $outdated_molecule_identifier_counter = 0;
foreach my $reference_molecule_db_id (@{$reference_molecule_db_ids}) {
	$reference_molecule = $dba->fetch_instance_by_db_id($reference_molecule_db_id)->[0];
	if (!(defined $reference_molecule)) {
		next;
	}	
#	print STDERR "$0: reference_molecule=$reference_molecule\n";

	$identifier = $reference_molecule->identifier->[0];
	if (!(defined $identifier) || $identifier eq '') {
		next;
	}
	
#	print STDERR "$0: identifier=$identifier\n";

	$molecule_identifier_counter++;

	($up_to_date_identifier, $chebi_name) = $chebi->get_up_to_date_identifier_and_name($identifier);
	
	if (!(defined $up_to_date_identifier)) {
		next;
	}
	
	$up_to_date_identifier =~ s/^CHEBI://;
	
#	print STDERR "$0: old identifier: $identifier, new identifier: $up_to_date_identifier\n";
	
	if ($identifier eq $up_to_date_identifier && $reference_molecule->name->[0] eq $chebi_name) {
		next;
	}
		
	my $report_line = "$0: old name: " . $reference_molecule->name->[0] . " ($identifier), new name: $chebi_name ($up_to_date_identifier)\n";
	if ($identifier eq $up_to_date_identifier &&
	    lc $reference_molecule->name->[0] eq lc $chebi_name) {
		print $report_line;
	} else {
		print OUT $report_line;
	}
	
	
	# Apply the correction to the database
	$reference_molecule->identifier(undef);
	$reference_molecule->name(undef);
	$reference_molecule->identifier($up_to_date_identifier);
	$reference_molecule->name($chebi_name);
	$dba->update_attribute($reference_molecule, "identifier");
	$dba->update_attribute($reference_molecule, "name");
	
	my $display_name = "$chebi_name [ChEBI:$up_to_date_identifier]";
	$reference_molecule->_displayName(undef);
	$reference_molecule->_displayName($display_name);
	$dba->update_attribute($reference_molecule, "_displayName");		
	
	$outdated_molecule_identifier_counter++;
	
}
print OUT "$0: updated $outdated_molecule_identifier_counter of $molecule_identifier_counter ChEBI identifiers (" . (100 * $outdated_molecule_identifier_counter) / $molecule_identifier_counter . "%)\n";

print OUT "$0 has finished its job\n";
