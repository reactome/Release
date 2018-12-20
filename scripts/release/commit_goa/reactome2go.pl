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
my($opt_user, $opt_host, $opt_pass, $opt_port, $opt_db, $opt_debug);

(@ARGV) || die "Usage: $PROGRAM_NAME -user db_user -host db_host -pass db_pass -port db_port -db db_name -debug\n";

GetOptions('user:s', 'host:s', 'pass:s', 'port:i', 'db:s', 'debug');

$opt_db || die "Need database name (-db).\n";

my $dba= GKB::DBAdaptor->new(
    -user   => $opt_user || $GKB::Config::GK_DB_USER,
    -host   => $opt_host || $GKB::Config::GK_DB_HOST,
    -pass   => $opt_pass || $GKB::Config::GK_DB_PASS,
    -port   => $opt_port || $GKB::Config::GK_DB_PORT,
    -dbname => $opt_db || $GKB::Config::GK_DB_NAME,
    -DEBUG => $opt_debug
);

my ($version) = $opt_db =~ /(\d+)$/msx;
my $outfile = "Reactome2GoV$version";

open my $file, '>', $outfile;
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


sub get_reactome_to_go_mapping_lines {
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
