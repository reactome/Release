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

my %seen;

report(join("\t", 'Preceding Event Name', 'Preceding Event DB Id', 'Preceding Event Species', 'Preceding Event Related Species',
                  'Following Event Name', 'Following Event DB Id', 'Following Event Species', 'Following Event Related Species'), $fh);
my @events = @{get_dba($db, $host)->fetch_instance(-CLASS => 'ReactionlikeEvent')};
foreach my $event (@events) {
	next unless $event->precedingEvent->[0];
	foreach my $preceding_event (@{$event->precedingEvent}) {
		next if $seen{$event->db_id}{$preceding_event->db_id}++;
		
        my @event_species = @{$event->species};
        my @preceding_event_species = @{$preceding_event->species};
        next if (scalar @event_species == 1) &&
				(scalar @preceding_event_species == 1) &&
				(
                 ($event->species->[0]->displayName eq $preceding_event->species->[0]->displayName) ||
                 (any {$event->species->[0]->displayName eq $_->displayName} @{$preceding_event->relatedSpecies}) ||
                 (any {$preceding_event->species->[0]->displayName eq $_->displayName} @{$event->relatedSpecies})
                );
		
        my $event_name = $event->displayName;
        my $event_id = $event->db_id;
        my $event_species = $event->species->[0] ? $event->species->[0]->displayName : "N/A";
        my $event_related_species = join('|', map {$_->displayName} @{$event->relatedSpecies});
		my $preceding_event_name = $preceding_event->displayName;
		my $preceding_event_id = $preceding_event->db_id;
        my $preceding_event_species = $preceding_event->species->[0] ? $preceding_event->species->[0]->displayName : "N/A";
        my $preceding_event_related_species = join('|', map {$_->displayName} @{$preceding_event->relatedSpecies});
			
		report(join("\t", $preceding_event_name, $preceding_event_id, $preceding_event_species, $preceding_event_related_species,
                          $event_name, $event_id, $event_species, $event_related_species), $fh);
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

sub multiple_species {
    my $instance = shift;
    
    return 0 unless $instance->species;
    
    return (scalar (@{$instance->species}) > 1);
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
    and reports those events that are preceding events if
    it or the event it precedes have zero or multiple species
    or if the species between them differs.
	
    The output file (name of this script with .txt extension) is
    tab-delimited with eight columns: event name, event database id,
    event species, event related species, preceding event name, preceding
    event database id, preceding event species, preceding event related
    species.
	
    USAGE: perl $0 [options]
	
    Options:
	
    -db [db_name]	Source database (default is $GKB::Config::GK_DB_NAME)
    -host [db_host]	Host of source database (default is $GKB::Config::GK_DB_HOST)
    -help 		    Display these instructions
END
}
