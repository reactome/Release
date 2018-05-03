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

if ($help) {
    print usage_instructions();
    exit;
}

$host	||= $GKB::Config::GK_DB_HOST;
$db	||= $GKB::Config::GK_DB_NAME;

(my $output_file = $0) =~ s/.pl$/.txt/;
open(my $fh, '>', $output_file);

my @reaction_like_events = @{get_dba($db, $host)->fetch_instance(-CLASS => 'ReactionlikeEvent')};
my @catalyst_activities = @{get_dba($db, $host)->fetch_instance(-CLASS => 'CatalystActivity')};
foreach my $instance (@reaction_like_events, @catalyst_activities) {
    my $regulations = $instance->regulatedBy;
    
    foreach my $regulation (@$regulations) {
		my $regulator = $regulation->regulator->[0];
        next unless $regulator;
    
		report($regulator->db_id . "\t" . $regulator->displayName) if $regulator->is_a('Event');
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

sub report {
	my $message = shift;
	my $fh = shift;
	
	print "$message\n";
	print $fh "$message\n" if $fh;
}

sub usage_instructions {
    return <<END;
	
	This script looks at all ReactionlikeEvent and
	CatalystActivity instances to find and report
	regulators which are events.
	
	The output file (name of this script with .txt extension)
	is tab-delimited with two columns: regulator database
	identifier and regulator display name.
	
	USAGE: perl $0 [options]
	
	Options:
	
	-db [db_name]	Source database (default is $GKB::Config::GK_DB_NAME)
	-host [db_host]	Host of source database (default is $GKB::Config::GK_DB_HOST)
	-help 		Display these instructions
END
}
