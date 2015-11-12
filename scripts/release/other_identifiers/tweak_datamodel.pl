#!/usr/local/bin/perl

# Make sure you don't have "competing" libraries...
# for use @CSHL
use lib "/usr/local/gkbdev/modules";
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

unless ($dba->ontology->is_valid_class_attribute('ReferenceEntity','otherIdentifier',)) {
    # Add the attribute
    $dba->ontology->_create_multivalue_attribute('ReferenceEntity','otherIdentifier','db_string_type');
    $dba->ontology->initiate;

    # Create the table
    $dba->create_multivalue_attribute_table('ReferenceEntity','otherIdentifier');

    # Store the new schema
    $dba->store_schema;
}

print STDERR "$0: no fatal errors.\n";

