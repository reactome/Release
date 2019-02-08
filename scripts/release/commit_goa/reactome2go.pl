#!/usr/local/bin/perl
use strict;
use warnings;

# This script should be run over a release database as it requires stable identifiers to be present
# This script produces a tab delimited file for submission to goa - including Reactome annotations for
# cellular components, molecular function and biological process.

use lib '/usr/local/gkb/modules';

use GKB::Config;
use GKB::Instance;
use GKB::DBAdaptor;
use GKB::Utils;

use autodie;
use Data::Dumper;
use English qw/-no_match_vars/;
use Getopt::Long;

# Database connection
my($user, $host, $pass, $port, $db, $output_file, $debug);

if (@ARGV == 0) {
    die "Usage: $PROGRAM_NAME -user db_user -host db_host -pass db_pass " .
        "-port db_port -db db_name -output_file file_name -debug\n";
}

GetOptions(
    'user:s' => \$user,
    'host:s' => \$host,
    'pass:s' => \$pass,
    'port:i' => \$port,
    'db:s' => \$db,
    'output_file:s' => \$output_file,
    'debug' => \$debug
);

$db || die "Need database name (-db).\n";
$user ||= $GKB::Config::GK_DB_USER;
$host ||= $GKB::Config::GK_DB_HOST;
$pass ||= $GKB::Config::GK_DB_PASS;
$port ||= $GKB::Config::GK_DB_PORT;

my $dba= GKB::DBAdaptor->new(
    -user   => $user,
    -host   => $host,
    -pass   => $pass,
    -port   => $port,
    -dbname => $db,
    -DEBUG => $debug
);

if (!$output_file) {
    $output_file = 'Reactome2GoV' . get_version($db);
}

open my $file, '>', $output_file;
binmode $file, ':encoding(UTF-8)';
binmode STDOUT, ':encoding(UTF-8)';

foreach my $event (@{$dba->fetch_instance(-CLASS => 'Event')}) {
    print "$PROGRAM_NAME: event->db_id=" . $event->db_id() . "\n";
    foreach my $line (get_reactome_2_go_mapping_lines($event)) {
        print {$file} "$line\n";
    }
}
close $file;

print "$PROGRAM_NAME has finished its job\n";

sub get_version {
    my $db_name = shift;

    my ($version) = $db_name =~ /(\d+)$/msx;
    $version ||= $db_name;

    return $version;
}

sub get_reactome_2_go_mapping_lines {
    my $event = shift;

    if (!$event->stableIdentifier->[0]) {
        return;
    }

    my $event_id = $event->stableIdentifier->[0]->identifier->[0];
    (my $event_displayname = trim($event->displayName)) =~ s/\s+/ /msxg; # Removes extra whitespace from displayName
    my $event_species = $event->species->[0] ? $event->species->[0]->displayName : 'N/A';

    my @go = grep { defined } (
        $event->GoBiologicalProcess->[0], # GO Biological Process instance
        map { $_->activity->[0] } @{$event->catalystActivity} # GO Molecular Function instances
    );

    return map {
        "Reactome:$event_id $event_displayname, $event_species > " .
        'GO:' . $_->displayName . ' ; ' .
        'GO:' . $_->accession->[0]
    } @go;
}

# Trims white space from beginning and end of string
sub trim {
    my $name = shift;
    $name =~ s/^\s+//msx;
    $name =~ s/\s+$//msx;
    return $name;
}
