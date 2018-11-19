#!/usr/local/bin/perl  -w
use strict;

use autodie;
use Getopt::Long;
use Log::Log4perl qw/get_logger/;

use feature qw/state/;

use lib '/usr/local/gkb/modules';
use GKB::DBAdaptor;
use GKB::CommonUtils;
use GKB::Config;
use GKB::NewStableIdentifiers;
use GKB::Utils_esther;

my ($database, $host, $live_run);
GetOptions(
    "db:s" => \$database,
    "host:s" => \$host,
    "live_run" => \$live_run
);
$database || die "Please specify database: -db [db_name]\n";
$host ||= $GKB::Config::GK_DB_HOST;

Log::Log4perl->init(\$LOG_CONF);
my $logger = get_logger(__PACKAGE__);

my $dba = get_dba($database, $host);
my %qa_problems = get_stable_id_QA_problems_as_hash($dba);

# Evaluate each instance
for my $incorrect_hash_ref (@{$qa_problems{'incorrect stable identifier'}}) {
    correct_stable_id(
        $incorrect_hash_ref->{'instance'}->[0],
        $incorrect_hash_ref->{'st_id_instance'}->[0],
        $incorrect_hash_ref->{'proposed_stable_identifier'}->[0],
        $dba
    );
}

sub correct_stable_id {
    my $instance = shift;
    my $stable_id_instance = shift;
    my $correct_identifier = shift;
    my $dba = shift;
    
    return unless $stable_id_instance;
    
    my $logger = get_logger(__PACKAGE__);

    $stable_id_instance->inflate();
    my $version  = $stable_id_instance->attribute_value('identifierVersion')->[0];
    my $identifier = $stable_id_instance->identifier->[0];
    
    if (get_prefix($stable_id_instance->identifier->[0]) eq get_prefix($correct_identifier)) {
        my $gk_central_instance = instance_in_gk_central($instance);
        if ($gk_central_instance) {
            my $gk_central_st_id_instance = $gk_central_instance->stableIdentifier->[0];
            $version = $gk_central_st_id_instance->identifierVersion->[0];
            $correct_identifier = $gk_central_st_id_instance->identifier->[0];
            $logger->info("Reverting $identifier to gk_central stable id $correct_identifier.$version");
        } else {
            $logger->warn("$identifier NOT changed to $correct_identifier.$version");
        }
    } else {
        $logger->info("$identifier being changed to $correct_identifier.$version");
    }

    if ($live_run) {    
        $stable_id_instance->identifier($correct_identifier);
        $stable_id_instance->identifierVersion($version);
        $stable_id_instance->displayName("$correct_identifier.$version");
        $stable_id_instance->Modified(@{$stable_id_instance->Modified});
        $stable_id_instance->add_attribute_value('modified', get_instance_edit($dba));
    
        foreach my $attribute ('identifier', 'identifierVersion', '_displayName', 'modified') {
            $dba->update_attribute($stable_id_instance, $attribute);
        }
    }
}

sub get_prefix {
    my $identifier = shift;
    
    my ($identifier_prefix) = $identifier =~ /^R-(\w{3})/;
    
    return $identifier_prefix;
}

sub get_instance_edit {
    state $db_to_instance_edit;
    my $dba = shift;
    my $db = $dba->db_name;
    
    return $db_to_instance_edit->{$db} if $db_to_instance_edit->{$db};
    
    my $date = `date \+\%F`;
    chomp $date;
    $db_to_instance_edit->{$db} = GKB::Utils_esther::create_instance_edit($dba, 'Weiser', 'JD', $date);
        
    return $db_to_instance_edit->{$db};
}
