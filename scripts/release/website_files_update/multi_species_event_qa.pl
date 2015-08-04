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
	next if $event->inferredFrom->[0];
	
	my $event_name = $event->displayName;
	my $event_id = $event->db_id;
	
	report("$event_name ($event_id) is tagged as human and has multiple species", $fh) if multiple_species($event) && has_human_species_tag($event);
	report("$event_name ($event_id) is tagged as human and has a related species", $fh) if $event->relatedSpecies->[0] && has_human_species_tag($event);
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

sub report {
	my $message = shift;
	my $fh = shift;
	
	print "$message\n";
	print $fh "$message\n";
}

sub usage_instructions {
    return <<END;
    perl $0 [options]
    
    Options:
    
    -db	[db_name]	Source database (default is $GKB::Config::GK_DB_NAME)
    -host [db_host]	Host of source database (default is $GKB::Config::GK_DB_HOST)
    -help			Display these instructions
END
}
