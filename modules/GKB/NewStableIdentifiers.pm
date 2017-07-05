package GKB::NewStableIdentifiers;
use strict;
use warnings;

use base 'Exporter';

use Carp;
use List::MoreUtils qw/all any/;
use Log::Log4perl qw/get_logger/;
use Scalar::Util qw/blessed/;

use lib '/usr/local/gkb/modules';
use GKB::CommonUtils;
use GKB::Config;

Log::Log4perl->init(\$LOG_CONF);
my $logger = get_logger(__PACKAGE__);

our @EXPORT = qw/
get_stable_id_QA_problems_as_list_of_strings
get_stable_id_QA_problems_as_hash
get_correct_stable_identifier
stable_identifier_numeric_component_is_correct
stable_identifier_species_prefix_is_correct
get_instances_requiring_stable_identifiers
get_instances_with_stable_identifiers
get_instances_missing_stable_identifiers
get_instances_with_multiple_stable_identifiers
get_instances_with_incorrect_stable_identifiers
get_instances_attached_to_stable_identifier
get_all_stable_identifier_instances
get_stable_identifier_instances_without_referrers
get_stable_identifier_instances_with_multiple_referrers
get_duplicate_stable_identifier_instances
get_old_stable_identifiers
get_duplicate_old_stable_identifiers
get_stable_identifier_species_prefix
get_stable_identifier_numeric_component
get_instance_species_prefix
/;

sub get_stable_id_QA_problems_as_list_of_strings {
    my $dba = shift;
    my %qa_problems = get_stable_id_QA_problems_as_hash($dba);

    return grep { defined } map {
        my $qa_issue_type = $_;
        my @entries = map {
            my @instances_with_stable_identifier = grep { defined } @{$_->{'instance'}};
            my @stable_identifier_instances = grep { defined } @{$_->{'st_id_instance'}};
            my $old_identifier = $_->{'old_stable_identifier'} ? '(old id - ' . $_->{'old_stable_identifier'} . ')' : '';
            
            my $instance_string = (join ', ', map {get_name_and_id($_)} @instances_with_stable_identifier) || "N/A";
            my $stable_id_string = (join ', ', map { get_name_and_id($_) } @stable_identifier_instances) || "N/A";
            
            "\tInstances:$instance_string\n\tStable Id Instances:$stable_id_string $old_identifier\n";
        } @{$qa_problems{$qa_issue_type}};
        
        ucfirst($qa_issue_type) . "\n\n" . join("\n", @entries);
    } keys %qa_problems;
}
sub get_stable_id_QA_problems_as_hash {
    my $dba = shift;
    my %qa_problems;
    
    my %duplicate_stable_identifier_instances = get_identifier_to_multiple_stable_identifier_instances_map($dba);
    my %duplicate_old_stable_identifier_instances = get_old_identifier_to_multiple_stable_identifier_instances_map($dba);
    
    foreach my $stable_identifier_instance (get_all_stable_identifier_instances($dba)) {        
        my @attached_instances = get_instances_attached_to_stable_identifier($stable_identifier_instance);
        if (scalar @attached_instances == 0) {
            push @{$qa_problems{'stable id with no referrers'}}, {'st_id_instance' => [$stable_identifier_instance],  'instance' => [undef]};
        } elsif (scalar @attached_instances > 1) {
            push @{$qa_problems{'stable id has multiple referrers'}}, {'st_id_instance' => [$stable_identifier_instance], 'instance' => \@attached_instances};
        } else {
            if (has_incorrect_stable_identifier($attached_instances[0])) {
                push @{$qa_problems{'incorrect stable identifier'}}, {'st_id_instance' => [$stable_identifier_instance], 'instance' => [$attached_instances[0]]};
            }            
        }
        
        my $identifier = $stable_identifier_instance->identifier->[0]; 
        if (exists($duplicate_stable_identifier_instances{$identifier})) {
            push @{$qa_problems{'duplicate stable identifier instances'}},
                {'st_id_instance' => $duplicate_stable_identifier_instances{$identifier}, 'instance' => [undef]};
        }
        
        my $old_identifier = $stable_identifier_instance->oldIdentifier->[0] // '';
        if ($old_identifier && is_incorrect_old_stable_identifier($old_identifier)) {
            push @{$qa_problems{'incorrect old stable identifier'}},
            {'st_id_instance' => [$stable_identifier_instance], 'instance' => [], 'old_stable_identifier' => $old_identifier};
        }
        
        if ($old_identifier && exists($duplicate_old_stable_identifier_instances{$old_identifier})) {
            push @{$qa_problems{'duplicate old stable identifier instances'}},
                {'st_id_instance' => $duplicate_old_stable_identifier_instances{$old_identifier}, 'instance' => [undef], 'old_stable_identifier' => $old_identifier};
        }
        
    }
    
    foreach my $instance (get_instances_requiring_stable_identifiers($dba)) {
        if (is_missing_stable_identifier($instance)) {
            push @{$qa_problems{'missing stable identifier'}}, {'st_id_instance' => [undef], 'instance' => [$instance]};
        } elsif (has_multiple_stable_identifiers($instance)) {
            push @{$qa_problems{'multiple stable identifiers'}}, {'st_id_instance' => \@{$instance->stableIdentifier}, 'instance' => [$instance]};
        }
    }
    
    return %qa_problems;
}

sub get_correct_stable_identifier {
    my $instance = shift;
    return 'R-' . get_instance_species_prefix($instance) . '-' . get_correct_stable_identifier_numeric_component($instance);
}

sub get_correct_stable_identifier_numeric_component {
    my $instance = shift;
    
    if (is_electronically_inferred($instance)) {
        return $instance->inferredFrom->[0]->db_id;
    }
    
    return $instance->db_id;
}

sub stable_identifier_numeric_component_is_correct {
    my $instance = shift;
    
    my $stable_identifier_instance = $instance->stableIdentifier->[0];
    return 0 unless $stable_identifier_instance;

    return get_correct_stable_identifier_numeric_component($instance) == get_stable_identifier_numeric_component($stable_identifier_instance);
}

sub stable_identifier_species_prefix_is_correct {
    my $instance = shift;
    
    my $stable_identifier_instance = $instance->stableIdentifier->[0];
    return 0 unless $stable_identifier_instance;
    
    return get_instance_species_prefix($instance) eq get_stable_identifier_species_prefix($stable_identifier_instance);
}

sub get_instances_requiring_stable_identifiers {
    my $dba = shift;
    
    return map {@{$dba->fetch_instance(-CLASS => $_)}} get_classes_requiring_stable_identifiers();
}

sub get_instances_missing_stable_identifiers {
    my $dba = shift;
    
    return grep {is_missing_stable_identifier($_)} get_instances_requiring_stable_identifiers($dba);
}

sub is_missing_stable_identifier {
    my $instance = shift;
    
    if (!is_instance_requiring_stable_identifier($instance)) {
        confess "'$instance' is not an instance requiring a stable identifier";
    }
    
    return (scalar @{$instance->stableIdentifier} == 0);
}

sub get_instances_with_multiple_stable_identifiers {
    my $dba = shift;
    
    return grep {has_multiple_stable_identifiers($_)} get_instances_requiring_stable_identifiers($dba);
}

sub has_multiple_stable_identifiers {
    my $instance = shift;
    
    if (!is_instance_requiring_stable_identifier($instance)) {
        confess "'$instance' is not an instance requiring a stable identifier";
    }
    
    return (scalar @{$instance->stableIdentifier} > 1);
}

sub get_instances_with_incorrect_stable_identifiers {
    my $dba = shift;
    
    return grep {(scalar @{$_->stableIdentifier} == 1) &&
                 has_incorrect_stable_identifier($_)}
                 get_instances_requiring_stable_identifiers($dba);
}

sub has_incorrect_stable_identifier {
    my $instance = shift;
    
    if (!is_instance_requiring_stable_identifier($instance)) {
        confess "'$instance' is not an instance requiring a stable identifier";
    }
    
    return (!stable_identifier_numeric_component_is_correct($instance) || !stable_identifier_species_prefix_is_correct($instance));
}
sub is_incorrect_old_stable_identifier {
    my $old_stable_identifier = shift;
    
    return $old_stable_identifier !~ /^REACT_/;
}
sub get_instances_with_stable_identifiers {
    my $dba = shift;
    
    return grep {defined $_->stableIdentifier->[0]} get_instances_requiring_stable_identifiers($dba);
}

sub get_instances_attached_to_stable_identifier {
    my $stable_identifier_instance = shift;
    
    return @{$stable_identifier_instance->reverse_attribute_value('stableIdentifier')};
}

sub get_all_stable_identifier_instances {
    my $dba = shift;
    
    return @{$dba->fetch_instance(-CLASS => 'StableIdentifier')};
}

sub get_stable_identifier_instances_without_referrers {
    my $dba = shift;
    
    return grep { scalar get_instances_attached_to_stable_identifier($_) == 0 } get_all_stable_identifier_instances($dba);
}

sub get_stable_identifier_instances_with_multiple_referrers {
    my $dba = shift;
    
    return grep { scalar get_instances_attached_to_stable_identifier($_) > 1 } get_all_stable_identifier_instances($dba);
}
sub get_duplicate_stable_identifier_instances {
    my $dba = shift;
    
    my %identifier_to_multiple_stable_identifier_instances = get_identifier_to_multiple_stable_identifier_instances_map($dba);
    
    return values %identifier_to_multiple_stable_identifier_instances;
}

sub get_identifier_to_multiple_stable_identifier_instances_map {
    my $dba = shift;
    
    my %identifier_to_stable_identifier_instances;
    foreach my $stable_identifier (get_all_stable_identifier_instances($dba)) {
        push @{$identifier_to_stable_identifier_instances{$stable_identifier->identifier->[0]}}, $stable_identifier;
    }
    
    my @identifiers_with_multiple_stable_identifier_instances = grep {
        scalar @{$identifier_to_stable_identifier_instances{$_}} > 1
    } keys %identifier_to_stable_identifier_instances;
    
    return map {$_ => $identifier_to_stable_identifier_instances{$_}} @identifiers_with_multiple_stable_identifier_instances;
}

sub get_old_identifier_to_multiple_stable_identifier_instances_map {
    my $dba = shift;
    
    my %old_identifier_to_stable_identifier_instances;
    foreach my $stable_identifier (get_all_stable_identifier_instances($dba)) {
        if ($stable_identifier->oldIdentifier->[0]) {
            push @{$old_identifier_to_stable_identifier_instances{$stable_identifier->oldIdentifier->[0]}}, $stable_identifier;
        }
    }
    
    my @old_identifiers_with_multiple_stable_identifier_instances = grep {
        scalar @{$old_identifier_to_stable_identifier_instances{$_}} > 1
    } keys %old_identifier_to_stable_identifier_instances;
    
    return map {$_ => $old_identifier_to_stable_identifier_instances{$_}} @old_identifiers_with_multiple_stable_identifier_instances;
}

sub get_stable_identifier_species_prefix {
    my $stable_identifier_instance = shift;
    return unless $stable_identifier_instance;
    
    my $logger = get_logger(__PACKAGE__);
    
    my $identifier = $stable_identifier_instance->identifier->[0];
    my ($identifier_species_prefix) = $identifier =~ /^R-(\w{3})/;
    
    if (!$identifier_species_prefix) {
        $logger->warn("Unable to get prefix from $identifier");
        return '';
    }    
    
    return $identifier_species_prefix;
}

sub get_stable_identifier_numeric_component {
    my $stable_identifier_instance = shift;
    return unless $stable_identifier_instance;
    
    my $identifier = $stable_identifier_instance->identifier->[0];
    my ($identifier_numeric_component) = $identifier =~ /^R-\w{3}-(\d+)/;
    
    if (!$identifier_numeric_component) {
        $logger->warn("Unable to get numeric component from $identifier");
        return -1;
    }
    
    return $identifier_numeric_component;
}

sub get_instance_species_prefix {
    my $instance = shift;
    
    if (!is_instance_requiring_stable_identifier($instance)) {
        confess "$instance is not a database object requiring stable identifiers";
    }
    
    if ($instance->is_a('PhysicalEntity')) {
        return get_species_prefix_from_physical_entity($instance);
    } elsif ($instance->is_a('Event')) {
        return get_species_prefix_from_event($instance);
    } elsif ($instance->is_a('Regulation')) {
        return get_species_prefix_from_regulation($instance);
    }
}

sub get_species_prefix_from_physical_entity {
    my $instance = shift;
    
    my @species = @{$instance->species};
    if (scalar @species == 1) {
        return get_prefix_from_species_instance($species[0]);
    } elsif (scalar @species > 1) {
        return 'NUL';
    } elsif (scalar @species == 0) {
        my @species_within_physical_entity = get_unique_species_from_all_entities($instance);
        
        if (scalar @species_within_physical_entity == 1) {
            return get_prefix_from_species_instance($species_within_physical_entity[0]);
        } elsif (scalar @species_within_physical_entity > 1) {
            return 'NUL';
        } elsif (scalar @species_within_physical_entity == 0) {
            return 'ALL';
        }
    }
}

sub get_species_prefix_from_event {
    my $instance = shift;
    
    my @species = @{$instance->species};
    if (scalar @species == 1) {
        return get_prefix_from_species_instance($species[0]);
    } else {
        return 'NUL';
    }    
}

sub get_species_prefix_from_regulation {
    my $instance = shift;
    
    my $regulated_entity = $instance->regulatedEntity->[0];
    if ($regulated_entity && $regulated_entity->is_a('Event')) {
        return get_species_prefix_from_event($regulated_entity);
    } elsif ($regulated_entity && $regulated_entity->is_a('CatalystActivity')) {
        if ($regulated_entity->physicalEntity->[0]) {
            return get_species_prefix_from_physical_entity($regulated_entity->physicalEntity->[0]);
        } else {
            return 'NUL';
        }
    } else {
        return 'NUL';
    }    
}

sub get_prefix_from_species_instance {
    my $species_instance = shift;
    
    return unless $species_instance && $species_instance->name->[0];
    
    if (get_preset_prefix_from_species_name($species_instance->name->[0])) {
        return get_preset_prefix_from_species_name($species_instance->name->[0]);
    }
    
    return get_prefix_from_species_name($species_instance->name->[0]);
}

sub get_preset_prefix_from_species_name {
    my $species_name = shift;
    
    my %species_to_prefix = (
      "Hepatitis C virus" => 'HPC',
      "Hepatitis B Virus" => "HPB",
      "Human herpesvirus" => "HER",
      "Molluscum contagiosum virus" => 'MCV',
      "Mycobacterium tuberculosis" => 'MTU',
      "Neisseria meningitidis" => 'NME',
      "Influenza A virus" => 'FLU',
      "Human immunodeficiency virus" => 'HIV',
      "Bacteria" => 'BAC',
      "Viruses" => 'VIR'
    );
    
    foreach my $species (keys %species_to_prefix) {
        return $species_to_prefix{$species} if $species_name =~ /$species/;
    }
}

sub get_prefix_from_species_name {
    my $species_name = shift;
    
    my $logger = get_logger(__PACKAGE__);
    
    my ($first_letter_of_genus, $first_two_letters_of_species);
    eval {
        ($first_letter_of_genus, $first_two_letters_of_species) = $species_name =~ /(\w).*? (\w{2}).*?/;
    };
    if ($@) {
        confess $@;
    }
    
    
    if (!$first_letter_of_genus || !$first_two_letters_of_species) {    
        $logger->warn("Unable to get prefix for species " . $species_name);
        return '';
    }
    
    return uc($first_letter_of_genus . $first_two_letters_of_species);
}

sub get_all_entities {
    my $instance = shift;
    
    my @all_entities = ($instance);
    if ($instance->is_a('Complex')) {
        push @all_entities, map {get_all_entities($_)} @{$instance->hasComponent};
    } elsif ($instance->is_a('EntitySet')) {
        push @all_entities, map {get_all_entities($_)} (@{$instance->hasMember}, @{$instance->hasCandidate});
    } elsif ($instance->is_a('Polymer')) {
        push @all_entities, map {get_all_entities($_)} (@{$instance->repeatedUnit});
    }
    return @all_entities;
}

sub get_unique_species_from_all_entities {
    my $instance = shift;
    
    my @species_instances = map {@{$_->species}} get_all_entities($instance);
    return get_unique_species(@species_instances);
}

sub is_instance_requiring_stable_identifier {
    my $instance = shift;
    
    return (blessed $instance) && ($instance->can('is_a')) && (any {$instance->is_a($_)} get_classes_requiring_stable_identifiers());
}

sub get_classes_requiring_stable_identifiers {
    return ('PhysicalEntity', 'Event', 'Regulation');
}

1;