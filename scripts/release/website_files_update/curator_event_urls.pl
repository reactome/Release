#!/usr/local/bin/perl  -w
use strict;

# Created by: Joel Weiser (joel.weiser@oicr.on.ca)
# Created on: March 26, 2015 
# Purpose: Create a tab delimited list of pathway browser links for each human event in reactomecurator.
#          List contains the columns database id, event name, and event url using the database id

use lib "/usr/local/gkb/modules";

use GKB::Config;
use GKB::Instance;
use GKB::DBAdaptor;

use autodie;
use Data::Dumper;
use Getopt::Long;


my ($help);
GetOptions(
  'help' => \$help  
);

if ($help) {
    print usage_instructions();
    exit;
}

my $outfile = "curator_event_urls.txt"; # Output file for website

my $dba = GKB::DBAdaptor->new
    (
     -user   => $GKB::Config::GK_DB_USER,
     -host   => 'reactomecurator.oicr.on.ca',
     -pass   => $GKB::Config::GK_DB_PASS,
     -dbname => 'gk_central'
     );

my $id_file = $ARGV[0];
my @events_from_file;

if ($id_file) {
    `dos2unix $id_file`;
    open my $in, '<', $id_file;
    while(my $id = <$in>) {
	chomp $id;
	next unless $id =~ /^\d+$/;
	
	my $instance = $dba->fetch_instance_by_db_id($id)->[0];
	unless ($instance && $instance->is_a('Event')) {
	    print "$id is not an event -- skipping\n";
	    next;
	}
	
	push @events_from_file, $instance;
    }
    close $in;
}

my %seen; # Prevents duplication in the file

my @events = @events_from_file ? @events_from_file : @{$dba->fetch_instance(-CLASS => 'Event')}; # Reactome events to be processed

open my $out, '>', $outfile;
print $out join("\t", ('Db Id', 'Human Event Name', 'Url')) . "\n";

# Each event is processed and output is sent to file
foreach my $event (@events) {    
    next unless $event->species->[0] &&
		$event->species->[0]->name->[0] =~ /Homo sapiens/i;

    my $db_id = $event->db_id;
    my $name = $event->name->[0];
    my $url = 'http://reactomecurator.oicr.on.ca/PathwayBrowser/#' . $db_id;

    #next if $seen{$db_id}++;

    print $out join("\t", ($db_id, $name, $url)) . "\n";
}
close $out;

sub usage_instructions {
    return <<END;
Usage: perl $0 [input file]

An optional input file can be specified containing a list of human event
database identifiers for which pathway browser links should be output.

The file must have one numeric database identifier per line.

Without a specified file, links will be output for all human event database identifiers.

END

}
