#!/usr/bin/perl
use strict;
use warnings;
use feature qw/state/;

use lib '/usr/local/gkb/modules';

use autodie qw/:all/;
use Carp;
use DBI;
use Try::Tiny;

use GKB::Config;
use GKB::DBAdaptor;

use Log::Log4perl qw/get_logger/;
Log::Log4perl->init(\$LOG_CONF);
my $logger = get_logger(__PACKAGE__);


my $dba = get_dba();

my @stable_identifier_instances = @{$dba->fetch_instance(-CLASS => 'StableIdentifier')};
my %unique_stable_ids;
foreach my $stable_id_instance (@stable_identifier_instances) {
    my $stable_id = $stable_id_instance->identifier->[0];
    
    if ($unique_stable_ids{$stable_id} && $unique_stable_ids{$stable_id} == 1) {
	print "$stable_id is duplicated\n";
	$unique_stable_ids{$stable_id}++;
    }
    
    my @referrers = @{$stable_id_instance->reverse_attribute_value('stableIdentifier')};
    if (@referrers) {
	if (scalar @referrers > 1) {
	    print "$stable_id is used more than once: " . join("\t", map({$_->db_id} @referrers)) . "\n";
	}
    } else {
	print "$stable_id is not used\n";
    }
}

sub get_dba {
    return GKB::DBAdaptor->new (
	-user => $GKB::Config::GK_DB_USER,
	-pass => $GKB::Config::GK_DB_PASS,
	-dbname => $GKB::Config::GK_DB_NAME
    );
}