#!/usr/bin/perl
use strict;
use warnings;

use lib '/usr/local/gkb/modules';

use autodie qw/:all/;

use List::MoreUtils qw/any/;
use Getopt::Long;
use GKB::Config;
use GKB::DBAdaptor;

use Log::Log4perl qw/get_logger/;
Log::Log4perl->init(\$LOG_CONF);
my $logger = get_logger(__PACKAGE__);


my $selected_pathways;
GetOptions('pathways=s' => \$selected_pathways);

unless ($selected_pathways && $selected_pathways =~ /^all$|^\d+(,\d+)*$/) {
    print usage_instructions();
    exit;
}

my @reactions;
if ($selected_pathways eq 'all') {    
    @reactions = @{get_dba()->fetch_instance(-CLASS => 'ReactionlikeEvent', [['species',['48887']]])};
} else {
    my @db_ids = split(",", $selected_pathways);
    my @pathways;
    
    foreach my $db_id (@db_ids) {
	my $pathway = get_dba()->fetch_instance_by_db_id($db_id)->[0];
	unless($pathway->is_a('Pathway')) {
	    warn("$db_id is not a pathway");
	    next;
	}
	push @pathways, $pathway;
    }
    
    foreach my $pathway (@pathways) {
	push @reactions, @{get_reaction_like_events($pathway)};
    }
}

exit unless @reactions;

(my $logic_table_output_file = $0) =~ s/.pl$/.tsv/;
open my $logic_table_fh, ">", "$logic_table_output_file";
#print $logic_table_fh "Parent\tChild\tValue\tLogic\n";

my %interactions;
my %parent2child;
my %child2parent;
my %id2instance;

foreach my $reaction (@reactions) {
    populate_graph($reaction);
}

foreach my $reaction (@reactions) {
    add_reaction_to_logic_table($reaction, \@reactions, $logic_table_fh);
}
close $logic_table_fh;

add_line_count($logic_table_output_file);
`dos2unix $logic_table_output_file`;

sub populate_graph {
    my $reaction = shift;
    
    my @inputs = @{$reaction->input};
    my @outputs = @{$reaction->output};
    my @catalysts =  map($_->physicalEntity->[0], @{$reaction->catalystActivity});
    my @regulations = @{$reaction->reverse_attribute_value('regulatedEntity')};
    
    foreach my $parent (@inputs, @catalysts, @regulations) {
	$parent2child{$parent->db_id}->{$reaction->db_id}++;
	$child2parent{$reaction->db_id}->{$parent->db_id}++;
	$id2instance{$parent->db_id} = $parent;
    }
    
    foreach my $child (@outputs) {
	$parent2child{$reaction->db_id}->{$child->db_id}++;
	$child2parent{$child->db_id}->{$reaction->db_id}++;
	$id2instance{$child->db_id} = $child;
    }
    
    $id2instance{$reaction->db_id} = $reaction;
}

sub add_reaction_to_logic_table {
    my $reaction = shift;
    my $all_reactions = shift;
    my $fh = shift;
    
    my @inputs = @{$reaction->input};
    my @outputs = @{$reaction->output};
    my @catalysts =  map($_->physicalEntity->[0], @{$reaction->catalystActivity});
    
    process_inputs($reaction, \@inputs, $fh);
    process_inputs($reaction, \@catalysts, $fh);
    
    foreach my $output (@outputs) {
	my $output_name = $output->name->[0];
	my $reaction_like_events_with_output = get_reactions_with_output($output, $all_reactions);
	
	process_output($output, $reaction_like_events_with_output, $fh);
    }
    
    my @regulations = @{$reaction->reverse_attribute_value('regulatedEntity')};
    process_regulations($reaction, \@regulations, $fh);
}

sub process_inputs {
    my $reaction = shift;
    my $inputs = shift;
    my $fh = shift;
    
    foreach my $input (@$inputs) {
	process_if_set_or_complex($input, $fh) unless is_an_output_in_binding_reaction($input);
	process_input($reaction, $input, $fh);
    }
}

sub process_input {
    my $reaction = shift;
    my $input = shift;
    my $fh = shift;
    
    report($fh, get_label($input), get_label($reaction), 1, 'AND');
}

sub process_output {
    my $output = shift;
    my $associated_reactions = shift;
    my $fh = shift;
    
    my $logic = scalar @$associated_reactions > 1 ? 'OR' : 'AND';
    foreach my $reaction (@$associated_reactions) {
	next if output_is_ancestral_input($output) && $reaction->catalystActivity->[0];	
	
	report($fh, get_label($reaction), get_label($output), 1, $logic);
    }
}

sub process_if_set_or_complex {
    my $physical_entity = shift;
    my $fh = shift;
    
    my @elements;
    my $logic;
    if ($physical_entity->is_a('EntitySet')) {
	push @elements, @{$physical_entity->hasMember};
	push @elements, @{$physical_entity->hasCandidate};
	$logic = 'OR';
    } elsif ($physical_entity->is_a('Complex')) {
	push @elements, @{$physical_entity->hasComponent};
	$logic = 'AND';
    }
    
    foreach my $element (@elements) {	
	report($fh, get_label($element), get_label($physical_entity), 1, $logic);
	process_if_set_or_complex($element, $fh);
    }
}

sub process_regulations {
    my $reaction  = shift;
    my $regulations = shift;
    my $fh = shift;
    
    foreach my $regulation (@$regulations) {
	my $regulator = $regulation->regulator->[0];
	process_if_set_or_complex($regulator, $fh) unless is_an_output_in_binding_reaction($regulator);
	
	my $value = $regulation->is_a('NegativeRegulation') ? -1 : 1;
	report($fh, get_label($regulator), get_label($reaction), $value, 'AND');
    }
}

sub report {
    my $fh = shift;
    
    my $parent = shift;
    my $child = shift;
    my $value = shift;
    my $logic = shift;
    
    if ($interactions{$parent}{$child}++) {
	$logger->info("Skipping $parent to $child association with logic '$logic' and value $value");
	return;
    }
    
    print $fh join("\t", $parent, $child, $value, $logic) . "\n";
}

sub get_label {
    my $instance = shift;
    
    if (is_set_or_complex($instance) || $instance->is_a('ReactionlikeEvent')) {
	return $instance->db_id;
    }
    
    return $instance->name->[0];
}

sub get_dba {
    return GKB::DBAdaptor->new (
	-user => $GKB::Config::GK_DB_USER,
	-pass => $GKB::Config::GK_DB_PASS,
	-dbname => $GKB::Config::GK_DB_NAME
    );
}


sub get_reaction_like_events {
    my $event = shift;
    return $event->follow_class_attributes(-INSTRUCTIONS => {'Event' => {'attributes' =>[qw(hasEvent)]}},
						      -OUT_CLASSES => ['ReactionlikeEvent']);
}

sub output_is_ancestral_input {
    my $output = shift;

    return grep({$output->db_id == $_->db_id} get_ancestors([$output]));
}

sub get_ancestors {
    my $children = shift;
    my $seen_ancestors = shift;
    
    return () unless @$children;
    
    my @parents;
    foreach my $child (@$children) {
	my @reaction_ids = keys %{$child2parent{$child->db_id}};
	foreach my $reaction_id (@reaction_ids) {
	    push @parents, map({$id2instance{$_}} grep({!$seen_ancestors->{$_}++} keys %{$child2parent{$reaction_id}}));
	}
    }
    
    push @parents, get_ancestors(\@parents, $seen_ancestors);
    
    return @parents;
}

sub is_set_or_complex {
    my $entity = shift;
    
    return $entity->is_a('EntitySet') || $entity->is_a('Complex');
}

sub is_binding_reaction {
    my $reaction = shift;
    
    return
    $reaction &&
    $reaction->is_a('ReactionlikeEvent') &&
    !$reaction->catalystActivity->[0] &&
    scalar @{$reaction->input} >= 2 &&
    scalar @{$reaction->input} > scalar @{$reaction->output};
}

sub is_an_output_in_binding_reaction {
    my $entity = shift;
    
    my @reactions_with_entity_as_output = map({$id2instance{$_}} keys %{$child2parent{$entity->db_id}});

    return any {is_binding_reaction($_)} @reactions_with_entity_as_output;
}

sub get_reactions_with_output {
    my $output = shift;
    my $reactions = shift;
    
    my %reaction_output_id_2_reactions;
    foreach my $reaction (@$reactions) {
	foreach my $reaction_output_id (map {$_->db_id} @{$reaction->output}) {
	    push @{$reaction_output_id_2_reactions{$reaction_output_id}}, $reaction;
	}
    }
    
    return \@{$reaction_output_id_2_reactions{$output->db_id}};
}

sub add_line_count {
    my $file = shift;
    
    open(my $fh, '<', $file);
    my @lines = <$fh>;
    close $fh;
    
    open($fh, '>', $file);
    print $fh scalar(@lines) . "\n\n";
    print $fh $_ foreach @lines;
    close $fh;
}

sub usage_instructions {
    return <<END;

This script creates a tab delimited text file describing a directed graph
with nodes being reactions and their physical entities (inputs, outputs,
catalysts, and regulators) connected by boolean logic.

perl $0 -pathways comma delimited list of pathway db_ids or 'all'
Example:
perl $0 -pathways 123,234,etc.
perl $0 -pathways all

END
}