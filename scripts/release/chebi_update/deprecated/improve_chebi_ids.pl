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

my $reference_molecule_db_ids = $dba->fetch_db_ids_by_class("ReferenceMolecule");
my $molecule_identifier_counter = 0;
my $outdated_molecule_identifier_counter = 0;
$dba->execute('START TRANSACTION');
foreach my $reference_molecule_db_id (@{$reference_molecule_db_ids}) {
    my $reference_molecule = $dba->fetch_instance_by_db_id($reference_molecule_db_id)->[0];
    next unless $reference_molecule;
    next unless $reference_molecule->referenceDatabase->[0] &&
                $reference_molecule->referenceDatabase->[0]->displayName =~ /ChEBI/i;

    my $identifier = $reference_molecule->identifier->[0];
    next unless $identifier;

    $molecule_identifier_counter++;

    my ($up_to_date_identifier, $chebi_name, $chebi_formula) = $chebi->get_up_to_date_identifier_name_formulae($identifier);
    if (!$up_to_date_identifier) {
        print "Can't find up to date identifier from ChEBI for $identifier\n";
        next;
    }

    $up_to_date_identifier =~ s/^CHEBI://;


    if ($chebi_name) {
        my @simple_entities = @{$reference_molecule->reverse_attribute_value('referenceEntity')};
        foreach my $simple_entity (@simple_entities) {
            my @names = @{$simple_entity->name};

            if (lc $names[0] eq lc $reference_molecule->name->[0]) {
                my $index = get_index($chebi_name, \@names);
                next if $index == 0;
                splice @names, $index, 1 if $index != -1;
                unshift @names, $chebi_name;
            } else {
                next if $names[1] && lc $names[1] eq lc $chebi_name && $names[2] && lc $names[2] eq lc $reference_molecule->name->[0];

                my $index = get_index($chebi_name, \@names);
                splice @names, $index, 1 if $index != -1;
                splice @names, 1, 0, $chebi_name; 

                $index = get_index($reference_molecule->name->[0], \@names);
                splice @names, $index, 1 if $index != -1;
                $#names >= 2 ?
                    splice @names, 2, 0, $reference_molecule->name->[0] :
                    push @names, $reference_molecule->name->[0];
            }

            $simple_entity->name(undef);
            $simple_entity->name(@names);
            $dba->update_attribute($simple_entity, "name");
            print 'Simple entity ' . $simple_entity->db_id . ':' . $simple_entity->_displayName->[0] . " names updated\n";
        }
    }

    if ($identifier eq $up_to_date_identifier && $reference_molecule->name->[0] eq $chebi_name) {
        next;
    }

    my $report_line = "$0: old name: " . $reference_molecule->name->[0] . " ($identifier), new name: $chebi_name ($up_to_date_identifier)\n\n";
    if ($identifier eq $up_to_date_identifier && lc $reference_molecule->name->[0] eq lc $chebi_name) {
        print $report_line;
    } else {
        print OUT $report_line;
    }

    # Apply the correction to the database
    $reference_molecule->identifier(undef);
    $reference_molecule->name(undef);
    $reference_molecule->identifier($up_to_date_identifier);
    $reference_molecule->name($chebi_name);
    $reference_molecule->formula(undef);
    $reference_molecule->formula($chebi_formula);
    $dba->update_attribute($reference_molecule, "identifier");
    $dba->update_attribute($reference_molecule, "name");
    $dba->update_attribute($reference_molecule, "formula");
    my $display_name = "$chebi_name [ChEBI:$up_to_date_identifier]";
    $reference_molecule->_displayName(undef);
    $reference_molecule->_displayName($display_name);
    $dba->update_attribute($reference_molecule, "_displayName");
    $outdated_molecule_identifier_counter++;
}
$dba->execute('COMMIT');

print OUT "$0: updated $outdated_molecule_identifier_counter of $molecule_identifier_counter ChEBI identifiers (" . (100 * $outdated_molecule_identifier_counter) / $molecule_identifier_counter . "%)\n\n";
print OUT "$0 has finished its job\n\n";

# Adapted from http://www.perlmonks.org/?node_id=75660 and
# http://www.perlmonks.org/?node_id=998136
sub get_index {
    my $search = shift;
    my $ar_ref = shift;
    my @array = @$ar_ref;
    $_=lc($_) for @array;

    my %index;
    @index{@array} = (0..$#array);
    return $index{lc($search)} // -1;
}
