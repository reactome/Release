#!/usr/bin/perl
use strict;
use warnings;

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

my ($output_file, $help);
GetOptions(
    'output=s' => \$output_file,
    'help' => \$help
);

if ($help) {
    print usage_instructions();
    exit;
}

my @complexes = @{get_dba()->fetch_instance(-CLASS => 'Complex')};

($output_file = $0) =~ s/.pl$/.tsv/ unless $output_file;
open my $complex_table_fh, ">", "$output_file";
print $complex_table_fh join("\t", "Complex Name", "Complex Stable Id", "Component Name", "Component Class", "Component Stable Id", "Component Reference Entity Name") . "\n";
foreach my $complex (@complexes) {
    next unless fully_human($complex);
    
    my $complex_name = $complex->displayName;
    my $complex_stable_id = $complex->stableIdentifier->[0]->identifier->[0] if $complex->stableIdentifier->[0];
    
    foreach my $component (get_components($complex)) {
        my $component_name = $component->displayName;
        my $component_class = $component->class;
        my $component_stable_id = $component->stableIdentifier->[0]->identifier->[0] if $component->stableIdentifier->[0];
        my $component_ref_entity_name = get_component_ref_entity_name($component);
        
        print $complex_table_fh join("\t", $complex_name, $complex_stable_id, $component_name, $component_class, $component_stable_id, $component_ref_entity_name) . "\n";
    }
}
close $complex_table_fh;

sub get_dba {
    return GKB::DBAdaptor->new (
        -user => $GKB::Config::GK_DB_USER,
        -pass => $GKB::Config::GK_DB_PASS,
        -dbname => $GKB::Config::GK_DB_NAME
    );
}

sub fully_human {
    my $instance = shift;
    
    return ($instance->species->[0] &&
            $instance->species->[0]->displayName eq 'Homo sapiens' &&
            !($instance->species->[1]) &&
            !($instance->relatedSpecies->[0]) &&
            !(is_chimeric($instance))
            );
}

sub is_chimeric {
    my $instance = shift;
    
    return $instance->isChimeric->[0] && $instance->isChimeric->[0] eq 'TRUE';
}

sub get_components {
    my $complex = shift;
    
    return unless $complex && $complex->is_a('Complex');
        
    my @components;
    foreach my $component (@{$complex->hasComponent}) {
        if ($component->is_a('Complex')) {        
            push @components, (get_components($component));
        } else {
            push @components, $component;
        }
    }
    return @components;
}

sub get_component_ref_entity_name {
    my $component = shift;
    
    my $component_reference_entity = $component->referenceEntity->[0];
    
    return '' unless $component_reference_entity;
    return $component_reference_entity->name->[0] // '';    
}

sub usage_instructions {
    return <<END;

Usage: perl $0 [options]

Options

-output		name of output file (defaults to name of script with .tsv extension)

END
}
