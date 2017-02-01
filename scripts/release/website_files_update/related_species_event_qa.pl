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
	next unless $event->relatedSpecies->[0];
	
	my $event_name = $event->displayName;
	my $event_id = $event->db_id;
	my $event_modifier = get_event_modifier($event);
	
	my $event_disease_tags = get_disease_tags($event);
	my $is_chimeric = is_chimeric($event) ? 'chimeric' : 'not chimeric';
	my $base_species = $event->species->[0]->displayName;
	my $related_species = $event->relatedSpecies->[0]->displayName;
	
	report(join("\t", $event_name, $event_id, $base_species, $related_species, $event_disease_tags, $is_chimeric, $event_modifier), $fh);
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

sub get_disease_tags {
    my $event = shift;
    
    my @diseases = @{$event->disease};
    
    return join(",", map({$_->displayName} @diseases)); 
}

sub is_chimeric {
    my $instance = shift;
	
    return $instance->is_valid_attribute('isChimeric') &&
		   $instance->isChimeric->[0] &&
		   $instance->isChimeric->[0] =~ /^true$/i; 
}

sub multiple_species {
    my $instance = shift;
    
    return 0 unless $instance->species;
    
    return (scalar @{$instance->species} > 1);
}

sub has_human_species_tag {
	my $instance = shift;
	
	return 0 unless $instance;
	
	foreach my $species (@{$instance->species}) {
		return 1 if $species->displayName =~ /Homo sapiens/i;
	}
	
	return 0;
}

sub get_event_modifier {
	my $event = shift;
	
	return 'Unknown' unless $event;
	
	my $created_instance = $event->created->[0];
	my $last_modified_instance = $event->modified->[-1];
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
	print $fh "$message\n";
}

sub usage_instructions {
    return <<END;
	
	This script searches through all reaction like events
	and reports those events that have any value in their
	related species attribute.
	
	The output file (name of this script with .txt extension) is
	tab-delimited with seven columns: event name, event database
	id, base species of event, related species of event, disease
	tags on event, chimeric status of event, and event author.
	
	USAGE: perl $0 [options]
	
	Options:
	
	-db [db_name]	Source database (default is $GKB::Config::GK_DB_NAME)
	-host [db_host]	Host of source database (default is $GKB::Config::GK_DB_HOST)
	-help 		Display these instructions
END
}
