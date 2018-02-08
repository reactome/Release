#!/usr/bin/perl
use strict;
use warnings;

use constant AND => 0;
use constant OR => 1;

use lib '/usr/local/gkb/modules';

use feature qw/state/;

use autodie qw/:all/;
use Carp;
use Data::Dumper;
use List::MoreUtils qw/any none uniq/;
use Getopt::Long;
use Readonly;
use Try::Tiny;

use GKB::Config;
use GKB::DBAdaptor;

use Log::Log4perl qw/get_logger/;
Log::Log4perl->init(\$LOG_CONF);
my $logger = get_logger(__PACKAGE__);


my ($selected_pathways, $output_dir, $all_reactions);
GetOptions(
    'pathways=s' => \$selected_pathways,
    'output_dir=s' => \$output_dir,
    'all_reactions' => \$all_reactions
);

unless ($selected_pathways && $selected_pathways =~ /^all$|^\d+(,\d+)*$/) {
    print usage_instructions();
    exit;
}


my @reactions;
my $dba = get_dba({'host' => 'reactomecurator.oicr.on.ca', 'db' => 'gk_central'});

my $output_file;
if ($selected_pathways eq 'all') {    
    @reactions = @{$dba->fetch_instance(-CLASS => 'ReactionlikeEvent', [['species',['48887']]])};
    $output_file = 'all';
} else {
    my @db_ids = split(",", $selected_pathways);
    my @pathways;
    
    foreach my $db_id (@db_ids) {
        my $pathway = $dba->fetch_instance_by_db_id($db_id)->[0];
    	unless($pathway->is_a('Pathway')) {
            print("$db_id is not a pathway");
            exit;
        }
        push @pathways, $pathway;
    }
    $output_file = scalar @pathways == 1 ?
        $pathways[0]->displayName :
        join("_", @db_ids);
    $output_file =~ s/['\\\/:\(\)&; ]+/_/g;
    
    foreach my $pathway (@pathways) {
        push @reactions, @{get_reaction_like_events($pathway)};
    }
}
@reactions = grep {!$_->disease->[0]} @reactions;
@reactions = grep {$_->_doRelease->[0] && $_->_doRelease->[0] =~ /TRUE/i} @reactions unless $all_reactions;
exit unless @reactions;

my %interactions;

foreach my $reaction (@reactions) {
    add_reaction_to_logic_table($reaction, \@reactions);
}

$output_dir ||= '.';
my $output_file_full_path = "$output_dir/$output_file.tsv";
open my $logic_table_fh, ">", "$output_file_full_path";
report_interactions(\%interactions, $logic_table_fh);
close $logic_table_fh;

#add_line_count($output_file);
`dos2unix -q $output_file`;


sub add_reaction_to_logic_table {
    my $reaction = shift;
    my $all_reactions = shift;
    
    my @inputs = @{$reaction->input};
    my @outputs = @{$reaction->output};
    my @catalysts =  grep { defined } map($_->physicalEntity->[0], @{$reaction->catalystActivity});
    
    process_inputs($reaction, $all_reactions, \@inputs);
    process_inputs($reaction, $all_reactions, \@catalysts);
    
    foreach my $output (@outputs) {
        my $output_name = $output->name->[0];
        
        process_output($output, $all_reactions);
    }
    
    my @regulations = @{$reaction->reverse_attribute_value('regulatedEntity')};
    process_regulations($reaction, $all_reactions, \@regulations);
}

sub process_inputs {
    my $reaction = shift;
    my $all_reactions = shift;
    my $inputs = shift;
    
    foreach my $input (@$inputs) {
        if (is_set_or_complex_or_polymer($input) && !(is_a_reaction_output($input, $all_reactions))) {
            my @outputs_of_preceding_events = get_outputs_of_preceding_events($reaction);
            my $entity_not_an_output_of_preceding_event = entity_not_in_list(\@outputs_of_preceding_events);
            process_if_set_complex_or_polymer($input, undef, $entity_not_an_output_of_preceding_event);
        }
        
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
    my $all_reactions = shift;

    my @reactions_to_output;
    my @reactions_using_output_as_input = get_reactions_using_output_as_input($output, $all_reactions);
    my $break_down = scalar @reactions_using_output_as_input ? 1 : 0;
    
    my @associated_reactions = get_reactions_with_output($output, $all_reactions);
    foreach my $associated_reaction (@associated_reactions) {
        next if (@reactions_using_output_as_input) && none {is_preceding_event($associated_reaction, $_)} @reactions_using_output_as_input;
        $break_down = 0;
        push @reactions_to_output, $associated_reaction;
    }
    
    process_if_set_complex_or_polymer($output) if $break_down;
    
    my $reaction_to_output_logic = scalar @reactions_to_output > 1 ? OR : AND;
    foreach my $reaction (@reactions_to_output) {
        record(get_label($reaction), get_label($output), 1, $reaction_to_output_logic);
    }
}

sub process_if_set_complex_or_polymer {
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
    my $test_to_recurse = shift;
    
    my $logic;
    if ($physical_entity->is_a('EntitySet')) {;
        $logic = OR;
    } elsif ($physical_entity->is_a('Complex')) {
        $logic = AND;
    } elsif ($physical_entity->is_a('Polymer')) {
        $logic = AND;
    }

    foreach my $element (get_set_complex_polymer_elements($physical_entity)) {
        $action->($element, $physical_entity, 1, $logic);
        if (!$test_to_recurse || $test_to_recurse->($physical_entity)) {
            process_if_set_complex_or_polymer($element, $action, $test_to_recurse);
        }
    }
}

sub get_set_complex_polymer_elements {
    my $physical_entity = shift;
    
    my @elements;
    if ($physical_entity->is_a('EntitySet')) {
        push @elements, @{$physical_entity->hasMember};
        push @elements, @{$physical_entity->hasCandidate};
    } elsif ($physical_entity->is_a('Complex')) {
        push @elements, @{$physical_entity->hasComponent};
    } elsif ($physical_entity->is_a('Polymer')) {
        push @elements, @{$physical_entity->repeatedUnit};
    }
    
    return @elements;
}

sub process_regulations {
    my $reaction  = shift;
    my $all_reactions = shift;
    my $regulations = shift;
    
    foreach my $regulation (@$regulations) {
        foreach my $active_regulator_component (get_active_regulator_component($regulation)) {
            process_if_set_complex_or_polymer($active_regulator_component) unless is_a_reaction_output($active_regulator_component, $all_reactions);
	
            my $value = $regulation->is_a('NegativeRegulation') ? -1 : 1;
            record(get_label($active_regulator_component), get_label($reaction), $value, AND);
        }
    }
}

sub get_active_regulator_component {
    my $regulation = shift;
    
    croak unless $regulation->is_a('Regulation');
    
    return $regulation->activeUnit->[0] ? @{$regulation->activeUnit} : $regulation->regulator->[0];
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
    state %label_cache;
    
    confess unless $instance;
    
    return $label_cache{$instance->db_id} if $label_cache{$instance->db_id};
        
    my $label = $instance->referenceEntity->[0] ?
    $instance->displayName . '_' . $instance->referenceEntity->[0]->db_id :
    $instance->db_id;
    
    $label .= "_RLE" if $instance->is_a('ReactionlikeEvent');
    $label =~ s/[ \,+]/_/g;
    
    $label_cache{$instance->db_id} = $label;
    
    return $label;
}

sub get_dba {
    my $parameters = shift // {};
    
    return GKB::DBAdaptor->new (
	-user => $GKB::Config::GK_DB_USER,
	-pass => $GKB::Config::GK_DB_PASS,
    -host => $parameters->{'host'} || $GKB::Config::GK_DB_HOST,
	-dbname => $parameters->{'db'} || $GKB::Config::GK_DB_NAME
    );
}


sub get_reaction_like_events {
    my $event = shift;
    return $event->follow_class_attributes(-INSTRUCTIONS => {'Event' => {'attributes' =>[qw(hasEvent)]}},
						      -OUT_CLASSES => ['ReactionlikeEvent']);
}

sub entity_not_in_list {
    my $list_elements = shift;
    
    return sub {
        my $physical_entity = shift;
        return none { $physical_entity->db_id == $_->db_id } @$list_elements;
    };
}

sub get_outputs_of_preceding_events {
    my $reaction = shift;
    my @preceding_events = @{$reaction->precedingEvent};
    my @outputs = map {@{$_->output}} @preceding_events;
    return @outputs;
}

sub is_preceding_event {
    my $parent_reaction = shift;
    my $child_reaction = shift;
    
    my @events_parent_precedes = @{$parent_reaction->reverse_attribute_value('precedingEvent')};
    return any {$_->db_id == $child_reaction->db_id} @events_parent_precedes;
}

sub is_set_or_complex_or_polymer {
    my $entity = shift;
    
    return $entity->is_a('EntitySet') || $entity->is_a('Complex') || $entity->is_a('Polymer');
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

sub is_a_reaction_output {
    my $entity = shift;
    my $reactions = shift;
    
    croak unless defined $entity && $entity->is_a('PhysicalEntity');
    croak unless defined $reactions && ref($reactions) eq "ARRAY";
    
    my @reactions_with_output = get_reactions_with_output($entity, $reactions);
                   
    return (scalar @reactions_with_output > 0) ? 1 : 0;
}

sub get_reactions_with_output {
    my $output = shift;
    my $reactions = shift;
    
    croak unless defined $output && $output->is_a('PhysicalEntity');
    croak unless defined $reactions && ref($reactions) eq "ARRAY";
    
    my %reaction_output_id_2_reactions;
    foreach my $reaction (@$reactions) {
        foreach my $reaction_output_id (uniq map {get_label($_)} @{$reaction->output}) {
            push @{$reaction_output_id_2_reactions{$reaction_output_id}}, $reaction;
        }
    }
      
    return (exists $reaction_output_id_2_reactions{get_label($output)}) ?
        @{$reaction_output_id_2_reactions{get_label($output)}} :
        ();        
}

sub get_reactions_using_output_as_input {
    my $output = shift;
    my $reactions = shift;
    
    croak unless defined $output && $output->is_a('PhysicalEntity');
    croak unless defined $reactions && ref($reactions) eq "ARRAY";
    
    return grep {
        my @reaction_inputs = @{$_->input};
        any { $output->db_id == $_->db_id } @reaction_inputs;
    } @$reactions;
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
                    next if on_skip_list($parent, $child);
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

sub on_skip_list {
    my $parent = shift;
    my $child = shift;
    
    my $parent_id = get_id_from_label($parent);
    my $child_id = get_id_from_label($child);
    
    Readonly my $ANYTHING => -1;
    my %skip_list = (
        5693589 => [1638790],
        5686410 => [1638790],
        5649637 => [3785704],
        1369085 => [$ANYTHING]
    );
    my @small_molecules = qw/114729 114735 114736 114754 114749 114728 114925 5316201 114733 114732 114964/;
    $skip_list{$ANYTHING} = [@small_molecules];
    $skip_list{$_} = [$ANYTHING] foreach @small_molecules;
    
    return 1 if any {$_ eq $child_id} @{$skip_list{$ANYTHING}};
    return any {($_ == $ANYTHING) || ($_ eq $child_id)} @{$skip_list{$parent_id}};
}

sub get_id_from_label {
    my $label = shift;
    
    my ($id) = $label =~ /^(\d+)_RLE(?:_AND|_OR)?$/;
    ($id) = $label =~ /_(\d+)(?:_AND|_OR)?$/ unless $id;
    $id = $label unless $id;
    
    return $id;
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

-pathways [db_id list or 'all'] comma delimited list of pathway db_ids or 'all' (required)
-output	[file name] name of output file (defaults to name of script with .tsv extension)
-all_reactions  flag used to include unreleased reactions (default is to use only released reactions)  

END
}
