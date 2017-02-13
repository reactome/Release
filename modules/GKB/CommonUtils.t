#!/usr/bin/perl
use strict;
use warnings;

use lib '/usr/local/gkb/modules';

use Test::More;
use Test::Exception;
use Test::Output;

use_ok('GKB::CommonUtils');
ok(1, "testing works");

my $release_dba;
lives_ok {$release_dba = get_dba('test_reactome_59', 'localhost')} 'test_reactome_59 DBA created';
isa_ok($release_dba, 'GKB::DBAdaptor');
can_ok($release_dba, qw/fetch_instance_by_db_id/);

# Values - 1:electronically inferred, 0:not electronically inferred
my %instances = (
    193508 => 0, # manually curated reaction
    9063027 => 1, # elec inferred reaction
    5419271 => 0, # manually curated black box event
    9099164 => 1, # elec inferred black box event
    5229194 => 0, # manually curated depolymerisation
    9304349 => 1, # elec inferred depolymerisation
    5688025 => 0, # manually curated failed reaction
    936986 => 0, # manually curated polymerisation
    9028516 => 1, # elec inferred polymerisation
    182065 => 0, # manually curated non-human reaction
    372742 => 0, # manually inferred reaction
    8849347 => 0, # manually curated catalyst activity
    9176553 => 1, # elec inferred catalyst activity
    5692833 => 0, # manually curated negative regulation
    9254173 => 1, # elec inferred negative regulation
    1663719 => 0, # manually curated positive regulation
    9363542 => 1, # elec inferred positive regulation
    927835 => 0, # manually curated other entity
    937266 => 0, # manually curated simple entity
    8944351 => 0, # manually curated complex
    8962101 => 1, # elec inferred complex
    1629806 => 0, # manually curated EWAS
    9492442 => 1, # elec inferred EWAS
    5652148 => 0, # manually curated GEE
    8982667 => 1, # elec inferred GEE
    1467470 => 0, # manually curated defined set
    8986465 => 1, # elec inferred defined set
    983323 => 0, # manually curated polymer
    9029501 => 1, # elec inferred polymer    
);

foreach my $instance_id (keys %instances) {
    my $instance = $release_dba->fetch_instance_by_db_id($instance_id)->[0];
    
    is(is_electronically_inferred($instance) ? 1 : 0, $instances{$instance_id}, "testing electronic inference status for $instance_id");
}
done_testing();