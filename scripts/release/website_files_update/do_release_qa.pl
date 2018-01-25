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

my $reaction_like_events = get_dba($db, $host)->fetch_instance(-CLASS => 'ReactionlikeEvent');

foreach my $reaction_like_event (@{$reaction_like_events}) {
	my $do_release = $reaction_like_event->_doRelease->[0];
    my $inferred_events = $reaction_like_event->reverse_attribute_value('inferredFrom');
    
    if ((!$do_release || $do_release =~ /FALSE/i) && any { $_->_doRelease->[0] && $_->_doRelease->[0] =~ /TRUE/i } @{$inferred_events}) {
        report_issue($reaction_like_event, $output, '');
    }
}

sub report_issue {
	my $event = shift;
	my $output = shift;
	my $message = shift;
	
	print $output $message . $event->db_id . '-' . $event->_displayName->[0] . '-' . join(",", map($_->_displayName->[0], (@{$event->species}))) . "\n";
}

sub usage_instructions {
    return <<END;
    
    This script checks all reaction like events in the database
    and reports those that have _doRelease set to null or false but
    have inferred reaction like events which have _doRelease set to
    true.  The output is a text file with the script name and a .txt
    extension:
    
    
    Usage: perl $0 [options]
    
    Options:
    
    -db [db_name]   Source database (default is gk_central)
    -host [db_host] Host of source database (default is reactomecurator.oicr.on.ca)
    -help           Display these instructions
END
}