#!/usr/local/bin/perl
use strict;
use warnings;

use lib "/usr/local/gkb/modules";

use GKB::Config;
use GKB::ClipsAdaptor;
use GKB::Ontology;
use GKB::DBAdaptor;
use GKB::Utils;

use Data::Dumper;
use Getopt::Long;
use Log::Log4perl qw/get_logger/;
use Try::Tiny;

Log::Log4perl->init(\$LOG_CONF);
my $logger = get_logger(__PACKAGE__);

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

$logger->info("starting _create_singlevalue_attribute invocations");

# Add attributes to keep track of protein numbers (used to calculate inference success) - it's not absolutely necessary to store these in the database, so if they are unwanted in the future they could be removed
foreach my $attribute (qw/totalProt inferredProt maxHomologues/) {
    foreach my $class (qw/Complex Polymer Reaction EntitySet/) {
        $logger->info("Creating $attribute attribute for $class class");
        try {
            $dba->ontology->_create_singlevalue_attribute($class,$attribute,'db_string_type');
        } catch {
            $logger->error_die("Problem creating $attribute attribute for $class class: $_");
        };
    }
}

$logger->info('about to change defining attributes');
try {
    #change defining attributes
    my $protein_class = &GKB::Utils::get_reference_protein_class($dba);
    if ($protein_class eq 'ReferencePeptideSequence') {
        $dba->ontology->class_attribute_check('ReferencePeptideSequence', 'variantIdentifier'); #the variantIdentifier attribute should not be a defining attribute in the orthology procedure as otherwise newly created other species instances (which don't have variantIdentifiers) are not recognised as duplicates of existing other species entries (which may or may not have variantIdentifiers) - this issue was only relevant in the old data model where isoforms were represented in the same class as "non-isoforms", this has been addressed by the new data model that distinguishes ReferenceGeneProducts and ReferenceIsoforms
    }
    $dba->ontology->class_attribute_check('BlackBoxEvent', 'species', 'all');
    $dba->ontology->class_attribute_check('DefinedSet', 'species', 'all'); #hack for now as there are DefinedSets with species, but with members that don't have species
    
    #change defining attributes to make sure the various Ensembl dbs are stored separately (they all have 'name' Ensembl in common, need to be distinguished by a further defining attribute)
    $dba->ontology->class_attribute_check('ReferenceDatabase', 'accessUrl', 'all');
    
    $dba->ontology->initiate;
} catch {
    $logger->error_die("Problem changing defining attributes: $_");
};

$logger->info("about to do a bunch of create_attribute_column_definitions");

foreach my $attribute (qw/totalProt inferredProt maxHomologues/) {
    foreach my $class (qw/Complex Polymer Reaction EntitySet/) {
        # Create the table
        foreach (@{$dba->create_attribute_column_definitions($class,$attribute)}) {
            $logger->info("Creating attribute column definitions for $attribute attribute for $class class");
            try {
                my $statement = qq(ALTER TABLE $class ADD $_);
                $dba->execute($statement);
            } catch {
                $logger->error_die("Problem creating attribute column definitions for $attribute attribute for $class class: $_");  
            };
        }
    }
}

# Store the new schema
$dba->store_schema;
