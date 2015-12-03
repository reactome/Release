#!/usr/bin/perl
use strict;
use warnings;

use constant AND => 0;
use constant OR => 1;

use lib '/usr/local/gkb/modules';

use autodie qw/:all/;
use Carp;
use Data::Dumper;
use List::MoreUtils qw/any uniq/;
use Getopt::Long;
use Try::Tiny;

use GKB::Config;
use GKB::DBAdaptor;

use Log::Log4perl qw/get_logger/;
Log::Log4perl->init(\$LOG_CONF);
my $logger = get_logger(__PACKAGE__);


my ($selected_pathways, $output_file);
GetOptions(
    'pathways=s' => \$selected_pathways,
    'output=s' => \$output_file
);

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
@reactions = grep {!$_->disease->[0]} @reactions;

exit unless @reactions;

my %interactions;
my %parent2child;
my %child2parent;
my %id2instance;

foreach my $reaction (@reactions) {
    populate_graph($reaction);
}

foreach my $reaction (@reactions) {
    add_reaction_to_logic_table($reaction, \@reactions);
}

($output_file = $0) =~ s/.pl$/.tsv/ unless $output_file;
open my $logic_table_fh, ">", "$output_file";
report_interactions(\%interactions, $logic_table_fh);
close $logic_table_fh;

add_line_count($output_file);
`dos2unix -q $output_file`;

sub populate_graph {
    my $reaction = shift;
    my $reaction_id = get_label($reaction);
    
    my @inputs = @{$reaction->input};
    my @outputs = @{$reaction->output};
    my @catalysts =  map($_->physicalEntity->[0], @{$reaction->catalystActivity});
    my @regulations = @{$reaction->reverse_attribute_value('regulatedEntity')};
    
    foreach my $parent (@inputs, @catalysts, @regulations) {
        my $parent_id = get_label($parent);
        $parent2child{$parent_id}{$reaction_id}++;
        $child2parent{$reaction_id}{$parent_id}++;
        $id2instance{$parent_id} = $parent;
        
        process_if_set_or_complex($parent,
            sub {
                my $component = $_[0];
                my $component_id = get_label($component);
                my $complex_or_set_id = get_label($_[1]);
                                
                $parent2child{$component_id}{$complex_or_set_id}++;
                $child2parent{$complex_or_set_id}{$component_id}++;
                $id2instance{$component_id} = $component;
            }
        )
    }
    
    foreach my $child (@outputs) {
        my $child_id = get_label($child);
        $parent2child{$reaction_id}{$child_id}++;
        $child2parent{$child_id}{$reaction_id}++;
        $id2instance{$child_id} = $child;
    }
    
    $id2instance{$reaction_id} = $reaction;
}

sub add_reaction_to_logic_table {
    my $reaction = shift;
    my $all_reactions = shift;
    
    my @inputs = @{$reaction->input};
    my @outputs = @{$reaction->output};
    my @catalysts =  map($_->physicalEntity->[0], @{$reaction->catalystActivity});
    
    process_inputs($reaction, \@inputs);
    process_inputs($reaction, \@catalysts);
    
    foreach my $output (@outputs) {
        my $output_name = $output->name->[0];
        my $reaction_like_events_with_output = get_reactions_with_output($output, $all_reactions);
        
        process_output($output, $reaction_like_events_with_output);
    }
    
    my @regulations = @{$reaction->reverse_attribute_value('regulatedEntity')};
    process_regulations($reaction, \@regulations);
}

sub process_inputs {
    my $reaction = shift;
    my $inputs = shift;
    
    foreach my $input (@$inputs) {
	process_if_set_or_complex($input) unless is_an_output($input);
	process_input($reaction, $input);
    }
}

sub process_input {
    my $reaction = shift;
    my $input = shift;
    
    record(get_label($input), get_label($reaction), 1, AND);
}

sub process_output {
    my $output = shift;
    my $associated_reactions = shift;
    
    my $logic = scalar @$associated_reactions > 1 ? OR : AND;
    foreach my $reaction (@$associated_reactions) {
        if (output_is_component_of_ancestral_input($output, $reaction)) {
            foreach my $child_reaction_id (get_child_reaction_ids($output)) {
                record(get_label($reaction), $child_reaction_id, 1, $logic);
            }
            next;
        } 
        record(get_label($reaction), get_label($output), 1, $logic);
    }
}

sub process_if_set_or_complex {
    my $physical_entity = shift;
    my $action = shift //
        sub {
                record(
                    get_label($_[0]),
                    get_label($_[1]),
                    $_[2],
                    $_[3]
                );
            };
    
    my @elements;
    my $logic;
    if ($physical_entity->is_a('EntitySet')) {
	push @elements, @{$physical_entity->hasMember};
	push @elements, @{$physical_entity->hasCandidate};
	$logic = OR;
    } elsif ($physical_entity->is_a('Complex')) {
	push @elements, @{$physical_entity->hasComponent};
	$logic = AND;
    }
    
    foreach my $element (@elements) {	
        $action->($element, $physical_entity, 1, $logic);
        process_if_set_or_complex($element, $action);
    }
}

sub process_regulations {
    my $reaction  = shift;
    my $regulations = shift;
    
    foreach my $regulation (@$regulations) {
	my $regulator = $regulation->regulator->[0];
	process_if_set_or_complex($regulator) unless is_an_output($regulator);
	
	my $value = $regulation->is_a('NegativeRegulation') ? -1 : 1;
	record(get_label($regulator), get_label($reaction), $value, AND);
    }
}

sub record {    
    my $parent = shift;
    my $child = shift;
    my $value = shift;
    my $logic = shift;
    
    $parent =~ s/\s+/_/g;
    $child =~ s/\s+/_/g;
    
    $interactions{$child}{$logic}{$parent}{$value}++;
}

sub get_label {
    my $instance = shift;
        
    my $label = $instance->referenceEntity->[0] ?
    $instance->referenceEntity->[0]->db_id :
    $instance->db_id;
    
    $label .= "_RLE" if $instance->is_a('ReactionlikeEvent');
    
    return $label;
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

sub output_is_component_of_ancestral_input {
    my $output = shift;
    my $parent_reaction = shift;
    
    my @ancestors = get_ancestors([$parent_reaction]);
    my @ancestor_complexes = grep {$_->is_a('Complex')} @ancestors;
    my @ancestor_components = map {get_components($_)} @ancestor_complexes;
    
    return any {$output->db_id == $_->db_id} @ancestor_components;
}

sub get_components {
    my $complex = shift;
    
    return unless $complex && $complex->is_a('Complex');
        
    my @components;
    foreach my $component (@{$complex->hasComponent}) {
        push @components, ($component, get_components($component));
    }
    return @components;
}

sub get_child_reaction_ids {
    my $input = shift;
    
    return grep {/_RLE$/} keys %{$parent2child{get_label($input)}}
}

sub output_is_ancestral_input {
    my $output = shift;

    return grep({get_label($output) eq get_label($_)} get_ancestors([$output]));
}

sub get_ancestors {
    my $children = shift;
    my $seen_ancestors = shift;

    return () unless @$children;
    
    my @parents;
    foreach my $child (@$children) {
        my @parent_ids = keys %{$child2parent{get_label($child)}};
        foreach my $parent_id (@parent_ids) {
            push @parents, $id2instance{$parent_id} unless $seen_ancestors->{$parent_id}++ || !$id2instance{$parent_id};
        }
    }
    
    push @parents, get_ancestors(\@parents, $seen_ancestors) if @parents;
    
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

sub is_an_output {
    my $entity = shift;
    
    my @parents_of_entity = map({$id2instance{$_}} keys %{$child2parent{get_label($entity)}});

    return @parents_of_entity;
}

sub get_reactions_with_output {
    my $output = shift;
    my $reactions = shift;
    
    my %reaction_output_id_2_reactions;
    foreach my $reaction (@$reactions) {
        foreach my $reaction_output_id (uniq map {get_label($_)} @{$reaction->output}) {
            push @{$reaction_output_id_2_reactions{$reaction_output_id}}, $reaction;
        }
    }
    
    return \@{$reaction_output_id_2_reactions{get_label($output)}};
}

sub report_interactions {
	my $interactions = shift;
	my $fh = shift;
	
	foreach my $child (keys %$interactions) {
		my @logic_keys = keys %{$interactions{$child}};
			
		if (@logic_keys > 1) {
			introduce_dummy_nodes($interactions, $child);
		}
	}
	
    my %seen;
	foreach my $child (keys %{$interactions}) {
		foreach my $logic (keys %{$interactions{$child}}) {
			foreach my $parent (keys %{$interactions{$child}{$logic}}) {
				foreach my $value (keys %{$interactions{$child}{$logic}{$parent}}) {
					next if $seen{$child}{$logic}{$parent}{$value}++;
                    print $fh join("\t", $parent, $child, $value, $logic) . "\n";
				}
			}
		}
	}
}

sub introduce_dummy_nodes {
	my $interactions = shift;
	my $child = shift;
		
	my $AND_parents = $interactions{$child}{AND()};
	my $OR_parents = $interactions{$child}{OR()};
	
	delete $interactions{$child};
	
	$interactions{$child}{AND()}{"$child\_AND"}{1}++;
	$interactions{$child}{AND()}{"$child\_OR"}{1}++;
	
	$interactions{"$child\_AND"}{AND()} = $AND_parents;
	$interactions{"$child\_OR"}{OR()} = $OR_parents;
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
catalysts, and regulators) connected by boolean logic ('0' representing
'AND' and '1' representing 'or').  The output has a header of the number of
node interactions and rows describing those interactions with four columns:

Parent Node, Child Node, Value, Logic


Usage: perl $0 [options]

Options

-pathways	comma delimited list of pathway db_ids or 'all' (required)
-output		name of output file (defaults to name of script with .tsv extension)

END
}
