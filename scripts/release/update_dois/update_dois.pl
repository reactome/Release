#!/usr/bin/perl
use strict;
use warnings;

use lib '/usr/local/gkb/modules';

use autodie qw/:all/;
use Getopt::Long;
use Readonly;
use Try::Tiny;

use GKB::Config;
use GKB::DBAdaptor;
use GKB::Utils_esther;

use Log::Log4perl qw/get_logger/;
Log::Log4perl->init(\$LOG_CONF);
my $logger = get_logger(__PACKAGE__);

our ($user, $pass, $release_db, $release_db_host, $curator_db, $curator_db_host, $live_run);
GetOptions(
    'user:s' => \$user,
    'pass:s' => \$pass,
    'release_db:s' => \$release_db,
    'release_db_host:s' => \$release_db_host,
    'curator_db:s' => \$curator_db,
    'curator_db_host:s' => \$curator_db_host,
    'live_run' => \$live_run
);

Readonly my $instance_edit_author_initials => 'JD';
Readonly my $instance_edit_author_last_name => 'Weiser';
Readonly my $doi_prefix => '10.3180';

my $release_dba = get_dba({
    'user' => $user || $GKB::Config::GK_DB_USER,
    'pass' => $pass || $GKB::Config::GK_DB_PASS,
    'host' => $release_db_host || $GKB::Config::GK_DB_HOST,
    'db' => $release_db || $GKB::Config::GK_DB_NAME
});
my $curator_dba = get_dba({
    'user' => $user || $GKB::Config::GK_DB_USER,
    'pass' => $pass || $GKB::Config::GK_DB_PASS,
    'host' => $curator_db_host || 'reactomecurator.oicr.on.ca',
    'db' => $curator_db || 'gk_central'
});

if (!$live_run) {
    $logger->info("DRY RUN - " . $release_dba->db_name() . " on " . $release_dba->host() . " and " . $curator_dba->db_name() . " on " . $curator_dba->host() . " will NOT be modified");
}

chomp(my $date = `date \+\%F`);
my ($release_instance_edit, $curator_instance_edit);
if ($live_run) {
    $release_instance_edit = GKB::Utils_esther::create_instance_edit($release_dba, $instance_edit_author_last_name, $instance_edit_author_initials, $date);
    $curator_instance_edit = GKB::Utils_esther::create_instance_edit($curator_dba, $instance_edit_author_last_name, $instance_edit_author_initials, $date);
}

my @release_pathway_instances = @{$release_dba->fetch_instance(-CLASS => 'Pathway')};
foreach my $release_pathway (@release_pathway_instances) {
    my $doi = $release_pathway->doi->[0];
    
    if ($doi && $doi !~ /^$doi_prefix/) {
        my $stable_id = $release_pathway->stableIdentifier->[0]->displayName;
	
        $logger->info("$doi_prefix/$stable_id for " . $release_pathway->name->[0]);
        next unless $live_run;
	
        try {
            $release_pathway->doi(undef);
            $release_pathway->doi("$doi_prefix/$stable_id");
            $release_pathway->Modified(@{$release_pathway->Modified});
            $release_pathway->add_attribute_value('modified', $release_instance_edit);
            $release_dba->update_attribute($release_pathway, 'modified');
            $release_dba->update_attribute($release_pathway, 'doi');
        } catch {
            $logger->error("Unable to update DOI for " . $release_pathway->name->[0] . " on " .
                           $release_dba->db_name() . " at " . $release_dba->host());
        };
        
        my $curator_pathway;
        try {
            $curator_pathway = $curator_dba->fetch_instance_by_db_id($release_pathway->db_id)->[0];
            $curator_pathway->doi(undef);
            $curator_pathway->doi("$doi_prefix/$stable_id");
            $curator_pathway->Modified(@{$curator_pathway->Modified});
            $curator_pathway->add_attribute_value('modified', $curator_instance_edit);
            $curator_dba->update_attribute($curator_pathway, 'modified');
            $curator_dba->update_attribute($curator_pathway, 'doi');
        } catch {
            $logger->error("Unable to update DOI for " . $curator_pathway->name->[0] . " on " .
                           $curator_dba->db_name() . " at " . $curator_dba->host());
        };
    }
}

$logger->info("$0 has finished\n");

sub get_dba {
    my $parameters = shift;
    
    return GKB::DBAdaptor->new (
        -user => $parameters->{'user'},
        -pass => $parameters->{'pass'},
        -host => $parameters->{'host'},
        -dbname => $parameters->{'db'}
    );
}