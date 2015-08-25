#!/usr/bin/perl
use strict;
use warnings;

use feature qw/state/;

use lib '/usr/local/gkb/modules';

use autodie qw/:all/;
use Carp;
use Getopt::Long;
use List::MoreUtils qw/any/;

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

if ($help || !$db) {
    print usage_instructions();
    exit;
}

$host	||= $GKB::Config::GK_DB_HOST;

(my $output_file = $0) =~ s/.pl$/.txt/;
open(my $fh, '>', $output_file);

my @physical_entities = @{get_dba($db, $host)->fetch_instance(-CLASS => 'PhysicalEntity')};
foreach my $physical_entity (@physical_entities) {	
	foreach my $physical_entity_issue (get_physical_entity_issues($physical_entity)) {
		report(get_line_for_report($physical_entity, $physical_entity_issue), $fh);
	}
}

my @events = @{get_dba($db, $host)->fetch_instance(-CLASS => 'Event')};
foreach my $event (@events) {
	my @event_issues;

	push @event_issues, get_event_issues($event) if !$event->stableIdentifier->[0]; # Find issues for new events (events without a stable identifier assigned)
	push @event_issues, get_reaction_like_event_issues($event) if $event->is_a('ReactionlikeEvent');
	
	foreach my $event_issue (@event_issues) {
		report(get_line_for_report($event, $event_issue), $fh);
	}
	
}

close($fh);

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

sub get_line_for_report {
	my $instance = shift;
	my $instance_issue = shift;
	
	my $instance_id = $instance->db_id;
	my $instance_name = $instance->name->[0];
	my $instance_class = $instance->class;
	my $instance_modifier = get_instance_modifier($instance);
	
	return join("\t", $instance_id, $instance_name, $instance_class, $instance_issue, $instance_modifier);
}

sub attribute_null {
	my $instance = shift;
	my $attribute = shift;
	
	croak unless $instance && $attribute;
	
	return !$instance->$attribute->[0];
}

sub has_human_species_tag {
	my $instance = shift;
	
	return 0 unless $instance;
	
	foreach my $species (@{$instance->species}) {
		return 1 if $species->displayName =~ /Homo sapiens/i;
	}
	
	return 0;
}

sub get_event_issues {
	my $event = shift;
	
	my @event_issues;
	push @event_issues, 'event with no author' if attribute_null($event, 'authored');
	push @event_issues, 'event with no editor' if attribute_null($event, 'edited');
	push @event_issues, 'event with no reviewer' if attribute_null($event, 'reviewed');
	push @event_issues, 'non-inferred event with no literature references' if attribute_null($event, 'inferredFrom') &&
																			attribute_null($event, 'literatureReference');
	push @event_issues, 'event with no summation' if attribute_null($event, 'summation');
	
	return @event_issues;
}

sub get_physical_entity_issues {
	my $physical_entity = shift;
	
	my @physical_entity_issues;
	if ($physical_entity->is_a('SimpleEntity') && !attribute_null($physical_entity, 'species')) {
		push @physical_entity_issues, 'simple entity with species';
	}
	
	if (attribute_null($physical_entity, 'compartment')) {
		push @physical_entity_issues, 'physical entity with no compartment';
	}
	
	return @physical_entity_issues;
}

sub get_reaction_like_event_issues {
	my $reaction_like_event = shift;
	
	my $logger = get_logger(__PACKAGE__);
	
	my @reaction_like_event_issues;
	
	get_skip_list('reaction_like_event_compartment_skip_list.txt');
	if (attribute_null($reaction_like_event, 'compartment')) {
		if (!(any { $_ == $reaction_like_event->db_id } get_reaction_like_event_compartment_skip_list())) {
			push @reaction_like_event_issues, 'event with no compartment';
		} else {
			$logger->info($reaction_like_event->db_id . " skipped for null compartment check");
		}
	}
	
	if (attribute_null($reaction_like_event, 'input')) {
		if (!(any { $_ == $reaction_like_event->db_id } get_reaction_like_event_input_skip_list())) {
			push @reaction_like_event_issues, 'event with no input';
		} else {
			$logger->info($reaction_like_event->db_id . " skipped for null input check");
		}
	}
	
	if (!$reaction_like_event->is_a('FailedReaction') &&
		attribute_null($reaction_like_event, 'output')) {
		if (!(any { $_ == $reaction_like_event->db_id } get_reaction_like_event_output_skip_list())) {
			push @reaction_like_event_issues, 'event with no output';
		} else {
			$logger->info($reaction_like_event->db_id . " skipped for null output check");
		}
	}
	
	if ($reaction_like_event->is_a('FailedReaction') &&
		!attribute_null($reaction_like_event, 'output')) {
		push @reaction_like_event_issues, 'failed reaction with output';
	}
	
	if ($reaction_like_event->is_a('FailedReaction') &&
		attribute_null($reaction_like_event, 'normalReaction')) {
		push @reaction_like_event_issues, 'failed reaction with no normal reaction';
	}
	
	if (!attribute_null($reaction_like_event, 'normalReaction') &&
		attribute_null($reaction_like_event, 'disease')) {
		push @reaction_like_event_issues, 'event with a normal reaction but no disease tags';
	}
	
	return @reaction_like_event_issues;
}

sub get_reaction_like_event_compartment_skip_list {
	state $skip_list = get_skip_list('reaction_like_event_compartment_skip_list.txt');
	
	return @{$skip_list};
}

sub get_reaction_like_event_input_skip_list {
	state $skip_list = get_skip_list('reaction_like_event_input_skip_list.txt');
	
	return @{$skip_list};
}

sub get_reaction_like_event_output_skip_list {
	state $skip_list = get_skip_list('reaction_like_event_output_skip_list.txt');
	
	return @{$skip_list};
}

sub get_skip_list {
	my $skip_list_file = shift;
	
	my $logger = get_logger(__PACKAGE__);
	
	my @skip_list;
	open(my $fh, '<', $skip_list_file);
	while (my $line = <$fh>) {
		chomp $line;
		
		my ($id) = split "\t", $line, 1;
		$logger->warn("non-numeric id found in $skip_list_file: $id") && next unless $id =~ /^\d+$/;
		push @skip_list, $id;
	}
	close($fh);
	
	return \@skip_list;
}

sub get_instance_modifier {
	my $instance = shift;
	
	return 'Unknown' unless $instance;
	
	my $created_instance = $instance->created->[0];
	my $last_modified_instance = $instance->modified->[-1];
	
	my $author_instance;
	$author_instance = $last_modified_instance->author->[0] if $last_modified_instance;
	$author_instance ||= $created_instance->author->[0] if $created_instance;
	
	my $author_name = $author_instance->displayName if $author_instance;
	
	return $author_name || 'Unknown';
}

sub report {
	my $message = shift;
	my $fh = shift;
	
	print "$message\n";
	print $fh "$message\n" if $fh;
}

sub usage_instructions {
    return <<END;
	
	This script searches through all events and
	physical entities reporting QA issues found
	with any instances.
	
	Specifically, the following will be reported:
	
	* Simple Entity where species is NOT null
	* Physical Entity where compartment is null
	* Reaction Like Event where compartment is null (unless in its skip list file)
	* Reaction Like Event where input is null (unless in its skip list file)
	* Reaction Like Event where output is null (unless in its skip list file)
	* Reaction Like Event having normal reaction but disease is null
	* Failed Reaction where normal reaction is null
	* Failed Reaction where output is NOT null
	* New Event (stable identifier is null) where editor is null
	* New Event (stable identifier is null) where author is null
	* New Event (stable identifier is null) where reviewer is null
	* New Event (stable identifier is null) where it is not inferred
	  and there is no literature reference
	
	The output file (name of this script with .txt extension) is
	tab-delimited with five columns: database id, instance name,
	instance class, instance issue, and instance last modifier.
	
	USAGE: perl $0 [options]
	
	Options:
	
	-db [db_name]	Source database (required)
	-host [db_host]	Host of source database (default is $GKB::Config::GK_DB_HOST)
	-help 		Display these instructions
END
}
