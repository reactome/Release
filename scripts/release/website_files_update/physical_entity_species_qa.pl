#!/usr/bin/perl
use strict;
use warnings;

use lib '/usr/local/gkb/modules';

use autodie qw/:all/;
use Getopt::Long;
use List::MoreUtils qw/any all/;

use GKB::Config;
use GKB::DBAdaptor;

use Log::Log4perl qw/get_logger/;
Log::Log4perl->init(\$LOG_CONF);
my $logger = get_logger(__PACKAGE__);

my ($db, $host, $help);
GetOptions(
    'db=s' => \$db,
    'host=s' => \$host,
    'help' => \$help
);

if ($help) {
    print usage_instructions();
    exit;
}

$db 	||= $GKB::Config::GK_DB_NAME;
$host	||= $GKB::Config::GK_DB_HOST;

(my $output_file = $0) =~ s/.pl$/.txt/;
open(my $fh, '>', $output_file);

my @events = @{get_dba($db, $host)->fetch_instance(-CLASS => 'ReactionlikeEvent')};
foreach my $event (@events) {
	next if multiple_species($event) ||
	$event->relatedSpecies->[0] ||
	is_chimeric($event) ||
	$event->inferredFrom->[0];
	
	next unless has_human_species_tag($event);
	
	report($event->db_id) if
		any {!$_->is_a('SimpleEntity') &&
			 !$_->is_a('OtherEntity') &&
             !$_->is_a('Drug') &&
			 !(contains_only_simple_entities($_)) &&
			 scalar get_species_from_instance($_) == 0
			 } get_physical_entities_in_reaction_like_event($event);
}

close($fh);

sub usage_instructions {
    return <<END;

    This script reports the db ids of reaction like events that have
    physical entities that have no species but are not simple or other
    entities, and do not contain only simple or other entities.
    
    Usage: perl $0 [options]
    
    Options:
    
    -db [database]          Database to query ($GKB::Config::GK_DB_NAME by default)
    -host [database_host]   Host of database to query ($GKB::Config::GK_DB_HOST by default)
    -help                   Display these instructions
END
}

sub get_dba {
    my $db = shift;
    my $host = shift;
    
    return GKB::DBAdaptor->new (
	-user => $GKB::Config::GK_DB_USER,
	-pass => $GKB::Config::GK_DB_PASS,
	-host => $host,
	-dbname => $db
    );
}

sub get_physical_entities_in_reaction_like_event {
    my $reaction_like_event = shift;

    my $logger = get_logger(__PACKAGE__);

    my @physical_entities;
    push @physical_entities, @{$reaction_like_event->input};
    push @physical_entities, @{$reaction_like_event->output};
    push @physical_entities, map($_->physicalEntity->[0], @{$reaction_like_event->catalystActivity});
    
    my %physical_entities = map {$_->db_id => $_} grep {$_} @physical_entities;
    
    # ...and all the sub-components of this PE
    for my $pe (values %physical_entities) {
		my @subs = recurse_physical_entity_components($pe);
		for my $sub (@subs) {
			$sub or next;
			#$logger->info("Adding sub component ".join(' ',$sub->class,$sub->displayName));
			$physical_entities{$sub->db_id} = $sub;
		}
    }

    return values %physical_entities;
}

# Recurse through all members/components so all descendent PEs will also
# be linked to the reaction/pathway
sub recurse_physical_entity_components {
    my $pe = shift;

    my %components = map {$_->db_id => $_} grep {$_} @{$pe->hasMember}, @{$pe->hasComponent}, @{$pe->repeatedUnit};
    keys %components || return ();
    
    for my $component (values %components) {
		next unless $component->is_a('EntitySet') || $component->is_a('Complex') || $component->is_a('Polymer');
		for my $sub_component (recurse_physical_entity_components($component)) { 
			$components{$sub_component->db_id} = $sub_component;
		}
		delete $components{$component->db_id};
    }

    return values %components;
}

sub contains_only_simple_entities {
	my $pe = shift;
	
	return all {$_->is_a('SimpleEntity') || $_->is_a('OtherEntity') || $_->is_a('Drug')} recurse_physical_entity_components($pe);
}

sub is_chimeric {
    my $instance = shift;
	
    return $instance->is_valid_attribute('isChimeric') &&
		   $instance->isChimeric->[0] &&
		   $instance->isChimeric->[0] =~ /^true$/i; 
}

sub has_human_species_tag {
	my $instance = shift;
	
	return 0 unless $instance;
	
	foreach my $species (@{$instance->species}) {
		return 1 if $species->displayName =~ /Homo sapiens/i;
	}
	
	return 0;
}

sub multiple_species {
    my $instance = shift;
    
    return 0 unless $instance->species;
    
    return (scalar @{$instance->species} > 1);
}

sub get_species_from_instance {
	my $instance = shift;
	
	return (@{$instance->species});
}

sub report {
	my $message = shift;	
	print "$message\n";
}
