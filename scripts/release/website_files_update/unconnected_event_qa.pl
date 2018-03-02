#!/usr/local/bin/perl  -w
use strict;

use lib '/usr/local/gkb/modules';

use GKB::CommonUtils;
use GKB::Config;
use GKB::DBAdaptor;

use autodie;
use Carp;
use Getopt::Long;
use List::MoreUtils qw/any/;


my ($host, $db, $help);

&GetOptions(
    "host:s" => \$host,
    "db:s" => \$db,
    "help" => \$help
);

if ($help) {
    print usage_instructions();
    exit;
}

$db ||= 'gk_central';
$host ||= 'reactomecurator.oicr.on.ca';

(my $outfile = $0) =~ s/\.pl/\.txt/;
open(my $output, ">", "$outfile");
binmode($output, ":utf8");

my $dba = get_dba($db, $host);
my $events = $dba->fetch_instance(-CLASS => 'Event');
my @top_level_pathways = get_all_top_level_pathways($dba);

print $output join("\t", "Event DB ID", "Event Display Name", "Event created author", "Event last modified author") . "\n";
foreach my $event (grep {is_human($_)} @{$events}) {
	#next unless $event->db_id == 194632;
    my $do_release = $event->_doRelease->[0];
    
    if (($do_release && $do_release =~ /TRUE/i) && !connected_to_hierarchy($event, \@top_level_pathways)) {
        print $output join("\t", (
            $event->db_id,
            $event->_displayName->[0],
            get_instance_creator($event),
            get_instance_modifier($event)
        )) . "\n";
    }
}

sub get_all_top_level_pathways {
    my $dba = shift;
    
    my $front_page = $dba->fetch_instance(-CLASS => 'FrontPage')->[0];
    return @{$front_page->frontPageItem};
}

sub connected_to_hierarchy {
    my $event = shift;
    my $all_top_level_pathways = shift;
    
    my $event_top_levels = get_top_level_events($event);
    
    return unless @$event_top_levels;
    return 1 if any {is_top_level_pathway($_, $all_top_level_pathways)} @$event_top_levels;
}

sub is_top_level_pathway {
    my $event = shift;
    my $all_top_level_pathways = shift;
    
    return any {$_->db_id == $event->db_id} @{$all_top_level_pathways};
}

sub get_top_level_events {
    my $event = shift;
    return top_events($event->follow_class_attributes(-INSTRUCTIONS => {'Event' => {'reverse_attributes' =>[qw(hasEvent)]}},
						      -OUT_CLASSES => ['Event']));
}

sub top_events {
    my ($events) = @_;
    my @out;
    foreach my $e (@{$events}) {
        @{$e->reverse_attribute_value('hasEvent')} && next; # If the event has a higher level event, it is not a top-level event and is skipped
        push @out, $e; # All top-level events collected here
    }
    # Filter out reactions
    @out = grep {$_->is_a('Pathway')} @out; 
    return \@out; # Returns top-level pathways
}

sub usage_instructions {
    return <<END;
    
    This script checks all events in the database and reports those
    that have _doRelease set to true and are not connected to the human
    pathway hierarchy.  The output is a text file with the script name
    and a .txt extension:
    
    
    Usage: perl $0 [options]
    
    Options:
    
    -db [db_name]   Source database (default is gk_central)
    -host [db_host] Host of source database (default is reactomecurator.oicr.on.ca)
    -help           Display these instructions
END
}