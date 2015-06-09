#!/usr/bin/perl
use strict;
use warnings;
use feature qw/state/;

use lib '/usr/local/gkb/modules';

use autodie qw/:all/;
use Carp;
use DBI;
use Try::Tiny;

use GKB::Config;
use GKB::DBAdaptor;

use Log::Log4perl qw/get_logger/;
Log::Log4perl->init(\$LOG_CONF);
my $logger = get_logger(__PACKAGE__);

die usage_instructions() unless ($ARGV[0]);

my @reactions;
if ($ARGV[0] eq 'all') {    
    @reactions = @{get_dba()->fetch_instance(-CLASS => 'ReactionlikeEvent', [['species',['48887']]])};
} else {
    my $db_id_list = $ARGV[0];
    my @db_ids = split(",", $db_id_list);
    my @pathways;
    
    foreach my $db_id (@db_ids) {
	push @pathways, get_dba()->fetch_instance_by_db_id($db_id)->[0];
    }
    
    foreach my $pathway (@pathways) {
	push @reactions, @{get_reaction_like_events($pathway)};
    }
}

exit unless @reactions;

(my $logic_table_output_file = $0) =~ s/.pl$/.txt/;
open my $logic_table_fh, ">", "$logic_table_output_file";
print $logic_table_fh "Parent\tChild\tValue\tLogic\n";

my %interactions;
foreach my $reaction (@reactions) {
    add_reaction_to_logic_table($reaction, $logic_table_fh);
}
close $logic_table_fh;

sub add_reaction_to_logic_table {
    my $reaction = shift;
    my $fh = shift;
    
    my @inputs = @{$reaction->input};
    my @outputs = @{$reaction->output};
    my @catalysts =  map($_->physicalEntity->[0], @{$reaction->catalystActivity});
    
    process_inputs($reaction, \@inputs, $fh);
    process_inputs($reaction, \@catalysts, $fh);
    
    foreach my $output (@outputs) {
	process_if_set_or_complex($output, $fh);
	my $reaction_like_events_with_output = $output->reverse_attribute_value('output');
	
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
	process_if_set_or_complex($input, $fh);
	process_input($reaction, $input, $fh);
    }
}

sub process_input {
    my $reaction = shift;
    my $input = shift;
    my $fh = shift;
    
    my $input_name = $input->displayName;
    my $reaction_name = $reaction->displayName;
    
    report($fh,$input_name,$reaction_name,1,'AND');
}

sub process_output {
    my $output = shift;
    my $associated_reactions = shift;
    my $fh = shift;
    
    process_if_set_or_complex($output, $fh);
    
    my $logic = scalar @$associated_reactions > 1 ? 'OR' : 'AND';
    my $output_name = $output->displayName;
    foreach my $reaction (@$associated_reactions) {
	my $reaction_name = $reaction->displayName;
	
	report($fh,$reaction_name,$output_name,1,$logic);
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
    
    my $physical_entity_name = $physical_entity->displayName;
    foreach my $element (@elements) {
	my $element_name = $element->displayName;
	
	report($fh,$element_name, $physical_entity_name, 1, $logic);
	process_if_set_or_complex($element, $fh);
    }
}

sub process_regulations {
    my $reaction  = shift;
    my $regulations = shift;
    my $fh = shift;
    
    my $reaction_name = $reaction->displayName;
    foreach my $regulation (@$regulations) {
	my $regulator = $regulation->regulator->[0];
	process_if_set_or_complex($regulator, $fh);
	
	my $regulator_name = $regulator->displayName;
	my $value = $regulation->is_a('NegativeRegulation') ? -1 : 1;
	report($fh, $regulator_name, $reaction_name, $value, 'AND');
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

sub usage_instructions {
    print <<END;
perl $0 comma delimited list of pathway db_ids or 'all'
Example:
perl $0 123,234,etc.
perl $0 all

END
}