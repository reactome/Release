#!/usr/local/bin/perl

# Make sure you don't have "competing" libraries...
# for use @CSHL
use lib "/usr/local/gkb/modules";
# for use @HOME
use lib "$ENV{HOME}/bioperl-1.0";
use lib "$ENV{HOME}/GKB/modules";
use lib "$ENV{HOME}/my_perl_stuff";

use GKB::ClipsAdaptor;
use GKB::Ontology;
use GKB::DBAdaptor;
use GKB::Utils;
use Data::Dumper;
use Getopt::Long;
use strict;

our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db,$opt_debug);

(@ARGV) || die "Usage: $0 -user db_user -host db_host -pass db_pass -port db_port -db db_name -debug\n";

&GetOptions("user:s", "host:s", "pass:s", "port:i", "db:s", "debug");

$opt_db || die "Need database name (-db).\n";

my $dba = GKB::DBAdaptor->new
    (
     -user   => $opt_user,
     -host   => $opt_host,
     -pass   => $opt_pass,
     -port   => $opt_port,
     -dbname => $opt_db,
     -DEBUG => $opt_debug
     );

print STDERR "tweak_datamodel: about to do a bunch of _create_singlevalue_attribute\n";

# Add attributes to keep track of protein numbers (used to calculate inference success) - it's not absolutely necessary to store these in the database, so if they are unwanted in the future they could be removed
$dba->ontology->_create_singlevalue_attribute('Complex','totalProt','db_string_type');
$dba->ontology->_create_singlevalue_attribute('Polymer','totalProt','db_string_type');
$dba->ontology->_create_singlevalue_attribute('Reaction','totalProt','db_string_type');
$dba->ontology->_create_singlevalue_attribute('EntitySet','totalProt','db_string_type');
$dba->ontology->_create_singlevalue_attribute('EntitySet','inferredProt','db_string_type');
$dba->ontology->_create_singlevalue_attribute('Complex','inferredProt','db_string_type');
$dba->ontology->_create_singlevalue_attribute('Polymer','inferredProt','db_string_type');
$dba->ontology->_create_singlevalue_attribute('Reaction','inferredProt','db_string_type');
$dba->ontology->_create_singlevalue_attribute('EntitySet','maxHomologues','db_string_type');
$dba->ontology->_create_singlevalue_attribute('Complex','maxHomologues','db_string_type');
$dba->ontology->_create_singlevalue_attribute('Polymer','maxHomologues','db_string_type');
$dba->ontology->_create_singlevalue_attribute('Reaction','maxHomologues','db_string_type');

print STDERR "tweak_datamodel: about to change defining attributes\n";

#change defining attributes
my $protein_class = &GKB::Utils::get_reference_protein_class($dba);
if ($protein_class eq 'ReferencePeptideSequence') {
    $dba->ontology->class_attribute_check('ReferencePeptideSequence', 'variantIdentifier'); #the variantIdentifier attribute should not be a defining attribute in the orthology procedure as otherwise newly created other species instances (which don't have variantIdentifiers) are not recognised as duplicates of existing other species entries (which may or may not have variantIdentifiers) - this issue was only relevant in the old data model where isoforms were represented in the same class as "non-isoforms", this has been addressed by the new data model that distinguishes ReferenceGeneProducts and ReferenceIsoforms
}
$dba->ontology->class_attribute_check('OpenSet', 'name', 'any'); #until ReferenceEntities are all sorted out (ideally the referencEntity attribute would define an OpenSet, but we currently don't have ReferenceEntities for all OpenSets, so the name attribute needs to be defining attribute)
$dba->ontology->class_attribute_check('OpenSet', 'species', 'all'); 
$dba->ontology->class_attribute_check('BlackBoxEvent', 'species', 'all');
$dba->ontology->class_attribute_check('DefinedSet', 'species', 'all'); #hack for now as there are DefinedSets with species, but with members that don't have species

#change defining attributes to make sure the various Ensembl dbs are stored separately (they all have 'name' Ensembl in common, need to be distinguished by a further defining attribute)
$dba->ontology->class_attribute_check('ReferenceDatabase', 'accessUrl', 'all');

$dba->ontology->initiate;

print STDERR "tweak_datamodel: about to do a bunch of create_attribute_column_definitions\n";

# Create the table
foreach (@{$dba->create_attribute_column_definitions('Complex','totalProt')}) {
			my $statement = qq(ALTER TABLE Complex ADD $_);
			$dba->execute($statement);
		    }
foreach (@{$dba->create_attribute_column_definitions('Polymer','totalProt')}) {
			my $statement = qq(ALTER TABLE Polymer ADD $_);
			$dba->execute($statement);
		    }
foreach (@{$dba->create_attribute_column_definitions('Reaction','totalProt')}) {
			my $statement = qq(ALTER TABLE Reaction ADD $_);
			$dba->execute($statement);
		    }
foreach (@{$dba->create_attribute_column_definitions('EntitySet','totalProt')}) {
			my $statement = qq(ALTER TABLE EntitySet ADD $_);
			$dba->execute($statement);
		    }
foreach (@{$dba->create_attribute_column_definitions('EntitySet','inferredProt')}) {
			my $statement = qq(ALTER TABLE EntitySet ADD $_);
			$dba->execute($statement);
		    }
foreach (@{$dba->create_attribute_column_definitions('EntitySet','maxHomologues')}) {
			my $statement = qq(ALTER TABLE EntitySet ADD $_);
			$dba->execute($statement);
		    }
foreach (@{$dba->create_attribute_column_definitions('Complex','maxHomologues')}) {
                        my $statement = qq(ALTER TABLE Complex ADD $_);
			$dba->execute($statement);
		    }
foreach (@{$dba->create_attribute_column_definitions('Polymer','maxHomologues')}) {
                        my $statement = qq(ALTER TABLE Polymer ADD $_);
			$dba->execute($statement);
		    }
foreach (@{$dba->create_attribute_column_definitions('Reaction','maxHomologues')}) {
                        my $statement = qq(ALTER TABLE Reaction ADD $_);
			$dba->execute($statement);
		    }
foreach (@{$dba->create_attribute_column_definitions('Complex','inferredProt')}) {
                        my $statement = qq(ALTER TABLE Complex ADD $_);
			$dba->execute($statement);
		    }
foreach (@{$dba->create_attribute_column_definitions('Polymer','inferredProt')}) {
                        my $statement = qq(ALTER TABLE Polymer ADD $_);
			$dba->execute($statement);
		    }
foreach (@{$dba->create_attribute_column_definitions('Reaction','inferredProt')}) {
                        my $statement = qq(ALTER TABLE Reaction ADD $_);
			$dba->execute($statement);
		    }


# Store the new schema
$dba->store_schema;