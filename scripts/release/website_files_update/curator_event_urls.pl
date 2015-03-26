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

my $outfile = "curator_event_urls.txt"; # Output file for website

my $dba = GKB::DBAdaptor->new
    (
     -user   => $GKB::Config::GK_DB_USER,
     -host   => 'reactomecurator.oicr.on.ca',
     -pass   => $GKB::Config::GK_DB_PASS,
     -dbname => 'gk_central'
     );

my %seen; # Prevents duplication in the file

my $ar = $dba->fetch_instance(-CLASS => 'Event'); # Obtains a reference to the array of all Reactome events

open my $out, '>', $outfile;
print $out join("\t", ('Db Id', 'Human Event Name', 'Url')) . "\n";

# Every pathway in reactome is processed
foreach my $event (@{$ar}) {
	next unless $event->species->[0] &&
		    $event->species->[0]->name->[0] =~ /Homo sapiens/i;
	
	my $db_id = $event->db_id;
	my $name = $event->name->[0];
	my $url = 'http://reactomecurator.oicr.on.ca/PathwayBrowser/#' . $db_id;
	
	next if $seen{$db_id}++;
	
	print $out join("\t", ($db_id, $name, $url)) . "\n";
}
close $out;