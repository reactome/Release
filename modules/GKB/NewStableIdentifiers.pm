package GKB::NewStableIdentifiers;
use strict;
use warnings;

use base 'Exporter';

use Carp;
use List::MoreUtils qw/all/;
use Log::Log4perl qw/get_logger/;

use lib '/usr/local/gkb/modules';
use GKB::Config;

Log::Log4perl->init(\$LOG_CONF);
my $logger = get_logger(__PACKAGE__);

our @EXPORT = qw/
stable_identifier_numeric_component_is_correct 
stable_identifier_species_prefix_is_correct 
get_all_instances_requiring_stable_identifiers
get_all_instances_with_stable_identifiers
get_instances_attached_to_stable_identifier
get_stable_identifier_species_prefix
get_stable_identifier_numeric_component 
get_instance_species_prefix
/;

sub stable_identifier_numeric_component_is_correct {
    
}

sub stable_identifier_species_prefix_is_correct {
    
}

sub get_all_instances_requiring_stable_identifiers {
    my $dba = shift;
}

sub get_all_instances_with_stable_identifiers {
    my $dba = shift;
}

sub get_instances_attached_to_stable_identifier {
    my $stable_identifier_instance = shift;
    
    return @{$stable_identifier_instance->reverse_attribute_value('stableIdentifier')};
}

sub get_stable_identifier_species_prefix {
    my $identifier = shift;
    
    my ($identifier_species_prefix) = $identifier =~ /^R-(\w{3})/;
    
    return $identifier_species_prefix;
}

sub get_stable_identifier_numeric_component {
    my $identifier = shift;
    
    my ($identifier_numeric_component) = $identifier =~ /^R-\w{3}-(\d+)/;
    
    return $identifier_numeric_component;
}

sub get_instance_species_prefix {
    my $instance = shift;
    
    if (should_use_ALL_prefix($instance)) {
        return 'ALL';
    } elsif (should_use_NUL_prefix($instance)) {
        return 'NUL';
    } elsif (is_set_without_species($instance) && scalar get_species_from_members_and_candidates($instance) == 1) {
        my $species_instance = (get_species_from_members_and_candidates($instance))[0];
        return get_prefix_from_species_name($species_instance);  
    } else {
        return get_preset_prefix($instance) || get_prefix_from_species_name($instance->species->[0]);
    }
}

sub should_use_ALL_prefix {
    my $instance = shift;
    
    return 1 if
        is_simple_or_other_entity_without_species($instance) ||
        (is_set_without_species($instance) && all {is_simple_or_other_entity_without_species($_)} (get_members_and_candidates($instance))) ||
        (is_polymer_without_species($instance) && has_repeated_unit_that_should_use_ALL_prefix($instance)) || 
        is_regulation_that_should_use_ALL_prefix($instance);
    return 0;
}

sub should_use_NUL_prefix {
    my $instance = shift;
    
    return 1 if
        is_complex_without_species($instance) ||
        is_GEE_without_species($instance) ||
        (is_set_without_species($instance) && scalar get_species_from_members_and_candidates($instance) != 1) ||
        (is_polymer_without_species($instance) && (!$instance->repeatedUnit->[0] || should_use_NUL_prefix($instance->repeatedUnit->[0]))) ||
        is_chimeric($instance) ||
        has_multiple_species($instance) ||
        is_event_without_species($instance) ||
        is_regulation_that_should_use_NUL_prefix($instance);
    
    return 0;
}

sub get_preset_prefix {
    my $instance = shift;
    
    return unless $instance && $instance->species->[0];
    
    my %species_to_prefix = (
      "Hepatitis C virus genotype 2a" => 'HEP',
      "Molluscum contagiosum virus subtype 1" => 'MCV',
      "Mycobacterium tuberculosis H37Rv" => 'MTU',
      "Neisseria meningitidis serogroup B" => 'NME',
      "Influenza A virus" => 'FLU',
      "Human immunodeficiency virus 1" => 'HIV',
      "Bacteria" => 'BAC',
      "Viruses" => 'VIR'
    );
    
    return $species_to_prefix{$instance->species->[0]->name->[0]};
}

sub get_prefix_from_species_name {
    my $species_instance = shift;
    
    my ($first_letter_of_genus, $first_two_letters_of_species) = $species_instance->name->[0] =~ /(\w).*?(\w{2}).*?/;
    
    return uc($first_letter_of_genus . $first_two_letters_of_species);
}

sub is_simple_or_other_entity_without_species {
    my $instance = shift;
    
    return ($instance->is_a('OtherEntity') || $instance->is_a('SimpleEntity')) &&
        (!$instance->species->[0]);
}

sub is_set_without_species {
    my $instance = shift;
    
    return ($instance->is_a('EntitySet') && !$instance->species->[0]);
}

sub get_members_and_candidates {
    my $instance = shift;
    
    croak "$instance is not a set" unless $instance->is_a('EntitySet');
    
    return (@{$instance->hasMember}, @{$instance->hasCandidate});
}

sub is_polymer_without_species {
    my $instance = shift;
    
    return ($instance->is_a('Polymer') && !$instance->species->[0]);
}

sub has_repeated_unit_that_should_use_ALL_prefix {
    my $instance = shift;
    
    return $instance->repeatedUnit->[0] &&
    (
     is_simple_or_other_entity_without_species($instance->repeatedUnit->[0]) ||
     is_set_without_species($instance->repeatedUnit->[0])
    );    
}

sub is_regulation_that_should_use_ALL_prefix {
    my $instance = shift;
    
    return unless $instance->is_a('Regulation');
    
    my $regulated_entity = $instance->regulatedEntity->[0];
    return unless $regulated_entity;
     
    return
        $regulated_entity->is_a('CatalystActivity') &&
        $regulated_entity->[0]->physicalEntity->[0] &&
        is_simple_or_other_entity_without_species($regulated_entity->physicalEntity->[0]);
}

sub is_complex_without_species {
    my $instance = shift;
    
    return ($instance->is_a('Complex') && !$instance->species->[0]);
}

sub is_GEE_without_species {
    my $instance = shift;
    
    return ($instance->is_a('GenomeEncodedEntity') && !$instance->species->[0]);
}

sub get_species_from_members_and_candidates {
    my $instance = shift;
    
    my @species_from_members_and_candidates = map {@{$_->species}} get_members_and_candidates($instance);
    my %unique_species_from_members_and_candidates = map {$_->db_id => $_} @species_from_members_and_candidates;
    return values %unique_species_from_members_and_candidates;
}

sub is_chimeric {
    my $instance = shift;
    
    return $instance->isChimeric->[0] && $instance->isChimeric->[0] eq 'TRUE';
}

sub has_multiple_species {
    my $instance = shift;
    
    return scalar @{$instance->species} > 1;
}
sub is_event_without_species {
    my $instance = shift;
    
    return ($instance->is_a('Event') && !$instance->species->[0]);
}

sub is_regulation_that_should_use_NUL_prefix {
    my $instance = shift;
    
    return unless $instance->is_a('Regulation');
    
    my $regulated_entity = $instance->regulatedEntity->[0];
    return if $regulated_entity;
    
    return (!$regulated_entity->[0]->is_a('Event') && !$regulated_entity->is_a('CatalystActivity')) ||
            is_event_without_species($regulated_entity) ||
            ($regulated_entity->is_a('CatalystActivity') &&
          (!$regulated_entity->physicalEntity->[0] ||
           (!$regulated_entity->physicalEntity->[0]->species->[0] && !is_simple_or_other_entity_without_species($regulated_entity->physicalEntity->[0])
          )));
}