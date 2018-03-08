#!/usr/local/bin/perl  -w
use strict;

use lib "/usr/local/gkb/modules";
use GKB::Config;
use GKB::DBAdaptor;
use GKB::Utils;
use GKB::Utils_esther;

use autodie;
use Data::Dumper;
use feature qw/state/;
use Getopt::Long;
use List::MoreUtils qw/any/;
use XML::LibXML;

my ($db, $host, $fix, $help);

GetOptions(
    "db:s" => \$db,
    "host:s" => \$host,
    "fix" => \$fix,
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
print $out "Pathway Diagram DB ID\tInstance DB ID\tInstance Render Type\tInstance Schema Class\tInstance Author\tSpecies\tProject\n";
DIAGRAM:foreach my $pathway_diagram_instance (@pathway_diagram_instances) {
    my $pathway_diagram_db_id = $pathway_diagram_instance->db_id;
    my $xml = $pathway_diagram_instance->storedATXML->[0];
    unless ($xml) {
        print STDERR "No XML found for pathway diagram instance $pathway_diagram_db_id\n";
        next;
    }
    my $dom = XML::LibXML->load_xml(string => $xml);
    my $root_element = $dom->getDocumentElement;    
    
    my $updated;
    foreach my $node ($root_element->getElementsByTagName('Nodes')->[0]->childNodes()) {
        my $node_name = $node->nodeName;
        my ($current_render_type) = $node_name =~ /Renderable(.*)/;
        next unless $current_render_type;
        
        my $node_db_id = $node->getAttribute("reactomeId");
        my $node_instance = $dba->fetch_instance_by_db_id($node_db_id)->[0];
        unless ($node_instance) {
            my $record = "$pathway_diagram_db_id\t$node_db_id\t$current_render_type\tN/A\tN/A\tN/A\tN/A\n";
            print $out $record unless $seen{$record}++;
            
            next;
        }
        
        my $node_schema_class = $node_instance->class;
        my $node_author = get_author($node_instance) ? get_author($node_instance)->displayName : 'Unknown';
        my $node_project = get_project($node_instance);
        my $node_species = $node_instance->species->[0] ? $node_instance->species->[0]->name->[0] : '';
                        
        if ($current_render_type eq "Pathway") {
            $root_element->getElementsByTagName('Nodes')->[0]->removeChild($node);
            $updated = 1;
            
            print STDERR "Removing RenderablePathway node for instance $node_db_id [$node_schema_class] in pathway diagram $pathway_diagram_db_id\n";
        }
        
        my $proper_render_type = get_proper_render_type($node_instance);
        unless ($proper_render_type) {
            print STDERR "No render type known for instance $node_db_id [$node_schema_class]\n";
        } elsif ($current_render_type ne $proper_render_type) {
            $node_name =~ s/(Renderable).*/$1$proper_render_type/;
            $node->setNodeName($node_name);
            $updated = 1;
            
            my $record = "$pathway_diagram_db_id\t$node_db_id\t$current_render_type\t$node_schema_class\t$node_author\t$node_species\t$node_project\n";
            print $out $record unless $seen{$record}++;
        }
    }
    
    if ($updated && $fix) {    
        $pathway_diagram_instance->modified(@{$pathway_diagram_instance->modified});
        $pathway_diagram_instance->add_attribute_value('modified', get_instance_edit($dba));
        $pathway_diagram_instance->storedATXML(undef);
        $pathway_diagram_instance->storedATXML($dom->toString);
        $dba->update_attribute($pathway_diagram_instance, 'storedATXML');
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
    
    return $instance->created->[0]->author->[0] if $instance->created->[0] && $instance->created->[0]->author->[0];
    return $instance->modified->[-1]->author->[0] if $instance->modified->[-1] && $instance->modified->[-1]->author->[0];
}

sub get_project {
    my $instance = shift;
    
    return get_author($instance) && get_author($instance)->project->[0] ? get_author($instance)->project->[0] : 'Unknown';
}

sub get_proper_render_type {
    my $instance = shift;
    
    return 'Protein' if is_protein_without_reference_entity($instance);
    
    my %schema_class_to_render_type = (
        'EntityCompartment' => 'Compartment',
        'Compartment' => 'Compartment',
        'GO_CellularComponent' => 'Compartment',
        'SimpleEntity' => 'Chemical',
        'ChemicalDrug' => 'Chemical',
        'EntityWithAccessionedSequence' =>
            {
                'ReferenceGeneProduct' => 'Protein',
                'ReferenceIsoform' => 'Protein',
                'ReferenceDNASequence' => 'Gene',
                'ReferenceRNASequence' => 'RNA'
            },
        'Complex' => 'Complex',
        'DefinedSet' => 'EntitySet',
        'OpenSet' => 'EntitySet',
        'CandidateSet' => 'EntitySet',
        'Polymer' => 'Entity',
        'OtherEntity' => 'Entity',
        'GenomeEncodedEntity' => 'Entity'
    );
    
    my $render_type = $schema_class_to_render_type{$instance->class};
    return $render_type unless ref($render_type);
    return $render_type->{$instance->referenceEntity->[0]->class} if ((ref($render_type) eq 'HASH') && ($instance->referenceEntity->[0]));
    return;
}

sub is_protein_without_reference_entity {
    my $instance = shift;
    
    return unless $instance;
    return any {$_ == $instance->db_id} get_id_list_for_proteins_without_reference_entities();
}

sub get_id_list_for_proteins_without_reference_entities {
    return (1599370, 6788097);
}

sub get_instance_edit {
    my $dba = shift;
    state $db_to_instance_edit;
    
    my $db_name = $dba->db_name;
    unless ($db_to_instance_edit && $db_to_instance_edit->{$db_name}) {
        chomp(my $date = `date \+\%F`);
        my $instance_edit = GKB::Utils_esther::create_instance_edit($dba, "Weiser", 'JD', $date);
        $db_to_instance_edit->{$db_name} = $instance_edit;
        print STDERR "Created instance edit with db id " . $instance_edit->db_id . "\n";
    }
    
    return $db_to_instance_edit->{$db_name};
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

If the 'fix' option is used, the schema class of instances with
a mis-matched render type will be used to determine the correct
render type and fix the node's XML stored in the database.

Usage: perl $0 [options]

-host "db_host" (default: reactomecurator.oicr.on.ca)
-db "db_name" (default: gk_central)
-fix
-help

END
}