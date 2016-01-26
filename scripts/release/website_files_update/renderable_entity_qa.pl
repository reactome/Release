#!/usr/local/bin/perl  -w
use strict;

use lib "/usr/local/gkb/modules";
use GKB::Config;
use GKB::DBAdaptor;
use GKB::Utils;

use autodie;
use Data::Dumper;
use Getopt::Long;
use List::MoreUtils qw/none/;
use XML::LibXML;

my ($db, $host, $help);

GetOptions(
    "db:s" => \$db,
    "host:s" => \$host,
    "help" => \$help
);

if ($help) {
    print usage_instructions();
    exit;
}

$db ||= 'gk_central';
$host ||= 'reactomecurator.oicr.on.ca';

my $dba = get_dba($db, $host);
my @pathway_diagram_instances = @{$dba->fetch_instance(-CLASS => 'PathwayDiagram')};

my %seen;

(my $outfile = $0) =~ s/\.pl/\.txt/;
open(my $out, '>' , $outfile);
print $out "Pathway Diagram DB ID\tInstance DB ID\tInstance Render Type\tInstance Schema Class\tInstance Author\n";
foreach my $pathway_diagram_instance (@pathway_diagram_instances) {
    my $pathway_diagram_db_id = $pathway_diagram_instance->db_id;
    my $xml = $pathway_diagram_instance->storedATXML->[0];
    unless ($xml) {
        print STDERR "No XML found for pathway diagram instance $pathway_diagram_db_id\n";
        next;
    }
    my $dom = XML::LibXML->load_xml(string => $xml);
    my $root_element = $dom->getDocumentElement;    
    
    foreach my $node ($root_element->getElementsByTagName('Nodes')->[0]->childNodes()) {
        my ($render_type) = $node->nodeName =~ /Renderable(.*)/;
        next unless $render_type;
        my $node_db_id = $node->getAttribute("reactomeId");
        my $node_instance = $dba->fetch_instance_by_db_id($node_db_id)->[0];
        unless ($node_instance) {
            my $record = "$pathway_diagram_db_id\t$node_db_id\t$render_type\tN/A\tN/A\n";
            print $out $record unless $seen{$record}++;
            next;
        }
        my $node_schema_class = $node_instance->class;
        my $node_author = get_author($node_instance);
        
        if (!get_schema_class_to_render_type_map()->{$node_schema_class} ||
            none {$_ eq $render_type} @{get_schema_class_to_render_type_map()->{$node_schema_class}}) {
            my $record = "$pathway_diagram_db_id\t$node_db_id\t$render_type\t$node_schema_class\t$node_author\n";
            print $out $record unless $seen{$record}++;
		}
    }
}
close $out;


sub get_dba {
    my ($db, $host) = @_;
    
    return GKB::DBAdaptor->new(
        -dbname => $db,
        -user => $GKB::Config::GK_DB_USER,
        -pass => $GKB::Config::GK_DB_PASS,
        -host => $host
    );
}

sub get_author {
    my $instance = shift;
    
    return $instance->created->[0]->author->[0]->displayName if $instance->created->[0] && $instance->created->[0]->author->[0];
    return $instance->modified->[-1]->author->[0]->displayName if $instance->modified->[-1] && $instance->modified->[-1]->author->[0];
    return 'Unknown';
}

sub get_schema_class_to_render_type_map {
    return {
        'EntityCompartment' => ['Compartment'],
        'Compartment' => ['Compartment'],
        'GO_CellularComponent' => ['Compartment'],
        'SimpleEntity' => ['Chemical'],
        'EntityWithAccessionedSequence' => ['Protein', 'Gene', 'RNA'],
        'Complex' => ['Complex'],
        'DefinedSet' => ['EntitySet'],
        'OpenSet' => ['EntitySet'],
        'CandidateSet' => ['EntitySet'],
        'Polymer' => ['Entity'],
        'OtherEntity' => ['Entity'],
        'GenomeEncodedEntity' => ['Entity']
    };
}

sub usage_instructions {
    return <<END;

This script checks the stored XML of pathway diagram instances
and reports nodes that have a mis-match between their render
type and their schema class.

The results are output into a tab-delimited file named the same
as this script with a .txt extension.  The file has five columns:
Pathway Diagram DB ID, Instance DB ID, Instance Render Type,
Instance Schema Class, and Instance Author

Usage: perl $0 [options]

-host db_host (default: reactomecurator.oicr.on.ca)
-db db_name (default: gk_central)
-help

END
}