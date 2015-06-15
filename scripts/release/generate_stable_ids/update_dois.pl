#!/usr/bin/perl
use strict;
use warnings;

use lib '/usr/local/gkb/modules';

use autodie qw/:all/;

use GKB::Config;
use GKB::DBAdaptor;

use Log::Log4perl qw/get_logger/;
Log::Log4perl->init(\$LOG_CONF);
my $logger = get_logger(__PACKAGE__);

my $doi_prefix = '10.3180';

my $dba = get_dba();
my @pathway_instances = @{$dba->fetch_instance(-CLASS => 'Pathway')};
foreach my $pathway (@pathway_instances) {
    my $doi = $pathway->doi->[0];
    
    if ($doi && $doi !~ /^$doi_prefix/) {
	my $stable_id = $pathway->stableIdentifier->[0]->displayName;
	
	$logger->info("$doi_prefix/$stable_id for " . $pathway->name->[0]);
	
	$pathway->doi(undef);
	$pathway->doi("$doi_prefix/$stable_id");
	$dba->update_attribute($pathway, 'doi');
    }
}

$logger->info("$0 has finished\n");

sub get_dba {
    return GKB::DBAdaptor->new (
	-user => $GKB::Config::GK_DB_USER,
	-pass => $GKB::Config::GK_DB_PASS,
	-dbname => $GKB::Config::GK_DB_NAME
    );
}