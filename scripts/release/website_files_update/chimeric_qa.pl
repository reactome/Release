#!/usr/bin/perl
use strict;
use warnings;

use lib '/usr/local/gkb/modules';

use autodie qw/:all/;
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
	my $event_name = $event->displayName;
	my $event_id = $event->db_id;
	
	report(make_record($event_name, $event_id, 'not chimeric', 'multiple species', get_species($event)), $fh) if multiple_species($event) && !is_chimeric($event);
	report(make_record($event_name, $event_id, 'chimeric', 'one species', get_species($event)), $fh) if !multiple_species($event) && is_chimeric($event);
	my @chimeric_components = grep {is_chimeric($_)} get_physical_entities_in_reaction_like_event($event);
	report(make_record($event_name, $event_id , 'not chimeric', 'chimeric components', get_db_ids(@chimeric_components)), $fh) if @chimeric_components && !is_chimeric($event);
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

    my %components = map {$_->db_id => $_} grep {$_} @{$pe->hasMember}, @{$pe->hasComponent};
    keys %components || return ();
    
    for my $component (values %components) {
		for my $sub_component (recurse_physical_entity_components($component)) { 
			$components{$sub_component->db_id} = $sub_component;
		}
    }

    return values %components;
}

sub multiple_species {
    my $instance = shift;
    
    return 0 unless $instance->species;
    
    return (scalar @{$instance->species} > 1);
}

sub is_chimeric {
    my $instance = shift;
	
    return $instance->is_valid_attribute('isChimeric') &&
		   $instance->isChimeric->[0] &&
		   $instance->isChimeric->[0] =~ /^true$/i; 
}

sub make_record {
	return join("\t", @_);
}

sub get_species {
	my $instance = shift;
	return unless $instance;
	
	return join "|", map {$_->displayName} @{$instance->species};
}

sub get_db_ids {
	my @instances = @_;
	
	return unless @instances;
	
	return join "|", map {$_->db_id} @instances;
}

sub report {
	my $message = shift;
	my $fh = shift;
	
	print "$message\n";
	print $fh "$message\n";
}

sub usage_instructions {
    return <<END;
	
	This script searches through all reaction like events and
	reports any events that are:
	
	- Not chimeric but have multiple species
	- Chimeric but have one species
	- Not chimeric but have chimeric components
	
	The output file (name of this script with .txt extension) is
	tab-delimited with three columns: event name, event database
	id, isChimeric, issue with event, instances (species or chimeric
	components).
	
    perl $0 [options]
    
    Options:
    
    -db	[db_name]	Source database (default is $GKB::Config::GK_DB_NAME)
    -host [db_host]	Host of source database (default is $GKB::Config::GK_DB_HOST)
    -help			Display these instructions
END
}
