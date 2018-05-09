#!/usr/local/bin/perl  -w
use strict;

# I think it's more user-friendly if the user will not have to practise any
# symlink tricks. Hence the BEGIN block. If the location of the script or
# libraries changes, this will have to be changed.

BEGIN {
    my @a = split('/',$0);
    pop @a;
    push @a, ('..','..','..');
    my $libpath = join('/', @a);
    unshift (@INC, "$libpath/modules");
    $ENV{PATH} = "$libpath/scripts:$libpath/scripts/release:" . $ENV{PATH};
}

use GKB::CommonUtils;
use GKB::Config;
use GKB::DBAdaptor;

use autodie;
use Carp;
use Getopt::Long;


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

$db ||= $GK_DB_NAME;
$host ||= $GK_DB_HOST;

my $dba = GKB::DBAdaptor->new
    (
     -user   => $GK_DB_USER,
     -host   => $host,
     -pass   => $GK_DB_PASS,
     -port   => $GK_DB_PORT,
     -dbname => $db,
     );

my %seen; # Prevents duplication in the file
open(my $output, ">", "event_qa_$db.txt");

my $ar = $dba->fetch_instance(-CLASS => 'Event'); # Obtains a reference to the array of all Reactome events

# Every event in reactome is processed
foreach my $event (@{$ar}) {
	next if $seen{$event->db_id}++;
	
    if (!$event->species->[0] || !$event->species->[0]->name->[0]) {
        report_issue($event, $output, 'No species name: ');
        next;
    }
    
	my $pathways = get_top_level_events($event);
	if (@$pathways) {
		foreach my $pathway (@$pathways) {
            if (!$pathway->species->[0]) {
                report_issue($event, $output, 'Top level pathway ' . get_name_and_id($pathway) . ' has no species for the contained event: ');
                next;
            }
            
            report_issue($event, $output, 'Human pathway ' . get_name_and_id($pathway) . ' has non-human event: ') if (is_human($pathway) && !is_human($event));
            report_issue($event, $output, 'Non-human pathway ' . get_name_and_id($pathway) . ' has human event: ') if (!is_human($pathway) && is_human($event));
            report_issue($event, $output, 'Non-chimeric pathway ' . get_name_and_id($pathway) . ' has chimeric event: ') if (!is_chimeric($pathway) && is_chimeric($event));
		}
	} else {
        if ($event->reverse_attribute_value('regulator')) {
			foreach my $regulation (@{$event->reverse_attribute_value('regulator')}) {
				my $regulated_entity = $regulation->reverse_attribute_value('regulatedBy');

				if (!$regulated_entity->species->[0]) {
                    report_issue($event, $output, 'Regulated entity ' . get_name_and_id($regulated_entity) . ' has no species for regulating event: ');
                    next;
                }                
                
                report_issue($event, $output, 'Human event ' . get_name_and_id($regulated_entity) . ' is regulated by non-human event: ') if (is_human($regulated_entity) && !is_human($event));
                report_issue($event, $output, 'Non-human event ' . get_name_and_id($regulated_entity) . ' is regulated by human event: ') if (!is_human($regulated_entity) && is_human($event));
			}
		} elsif (!$event->precedingEvent->[0] && !$event->reverse_attribute_value('precedingEvent')->[0]) {
			report_issue($event, $output, 'Orphan event: ');
		}
	}
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
        #	@{$e->reverse_attribute_value('hasMember')} && next;
        push @out, $e; # All top-level events collected here
    }
    # Filter out reactions
    @out = grep {$_->is_a('Pathway')} @out; 
    return \@out; # Returns top-level pathways
}

sub report_issue {
	my $event = shift;
	my $output = shift;
	my $message = shift;
	
	print $output $message . $event->db_id . '-' . $event->_displayName->[0] . '-' . join(",", map($_->_displayName->[0], (@{$event->species}))) . "\n";
}

sub usage_instructions {
    return <<END;
    
    This script checks all events (pathways and reaction like events) in the database
    and reports the following in a text file with the name event_qa_<db>.txt:
    
    * Events with no species
    * Pathway with no species containing an event with a species
    * Regulated entity with no species regulating an even with a species
    * Human pathways with non-human events or vice-versa
    * Non-chimeric pathways with chimeric events (converse situation is okay and not reported)
    * Human events regulated by non-human events or vice-versa
    * Orphan events (not a preceding/following event, regulatory event, or in the hierarchy)
    
    Usage: perl $0 [options]
    
    Options:
    
    -db [db_name]   Source database (default is $GKB::Config::GK_DB_NAME)
    -host [db_host] Host of source database (default is $GKB::Config::GK_DB_HOST)
    -help           Display these instructions
END
}