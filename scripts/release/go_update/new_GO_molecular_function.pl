#!/usr/local/bin/perl
use strict;
use warnings;

use autodie;
use Data::Dumper;

use lib '/usr/local/gkb/modules';
use GKB::CommonUtils;

if (!$ARGV[0] || $ARGV[0] !~ /^\d+$/) {
    print "Usage: perl $0 [db_id of GO update instance edit]\n";
    exit;
}

my $gk_central_dba = get_dba('gk_central', 'curator.reactome.org');

my $instance_edit = $gk_central_dba->fetch_instance_by_db_id($ARGV[0])->[0];
my @GO_molecular_function_instances = grep {$_->is_a('GO_MolecularFunction')} @{$instance_edit->reverse_attribute_value('created')};
foreach (@GO_molecular_function_instances) {
    print join("\t", $_->db_id, $_->accession->[0], $_->displayName, $_->definition->[0]) . "\n";
}
