#!/usr/bin/perl
use strict;
use warnings;

use lib '/usr/local/gkb/modules';

use Readonly;
use Test::More;
use Test::Exception;
use Test::Output;
use Tie::IxHash;

use_ok('GKB::CommonUtils');
ok(1, "testing works");

my $release_dba;
lives_ok {$release_dba = get_dba('test_reactome_59', 'localhost')} 'test_reactome_59 DBA created';
isa_ok($release_dba, 'GKB::DBAdaptor');
can_ok($release_dba, qw/fetch_instance_by_db_id/);

Readonly my $ELECTRONICALLY_INFERRED => 1;
Readonly my $MANUALLY_CURATED => 0;

# Values - 1:electronically inferred, 0:not electronically inferred (i.e. manually curated)
my %instances;
tie %instances, 'Tie::IxHash';
%instances = (
    193508 => $MANUALLY_CURATED, # manually curated reaction
    9063027 => $ELECTRONICALLY_INFERRED, # elec inferred reaction
    5419271 => $MANUALLY_CURATED, # manually curated black box event
    9099164 => $ELECTRONICALLY_INFERRED, # elec inferred black box event
    5229194 => $MANUALLY_CURATED, # manually curated depolymerisation
    9304349 => $ELECTRONICALLY_INFERRED, # elec inferred depolymerisation
    5688025 => $MANUALLY_CURATED, # manually curated failed reaction
    936986 => $MANUALLY_CURATED, # manually curated polymerisation
    9028516 => $ELECTRONICALLY_INFERRED, # elec inferred polymerisation
    182065 => $MANUALLY_CURATED, # manually curated non-human reaction
    372742 => $MANUALLY_CURATED, # manually inferred reaction
    8849347 => $MANUALLY_CURATED, # manually curated catalyst activity
    9176553 => $ELECTRONICALLY_INFERRED, # elec inferred catalyst activity
    5692833 => $MANUALLY_CURATED, # manually curated negative regulation
    9254173 => $ELECTRONICALLY_INFERRED, # elec inferred negative regulation
    1663719 => $MANUALLY_CURATED, # manually curated positive regulation
    9363542 => $ELECTRONICALLY_INFERRED, # elec inferred positive regulation
    111928 => $MANUALLY_CURATED, # manually curated positive regulation regulating a catalyst activity
    8952721 => $ELECTRONICALLY_INFERRED, # elec inferred positive regulation regulating a catalyst activity
    927835 => $MANUALLY_CURATED, # manually curated other entity
    937266 => $MANUALLY_CURATED, # manually curated simple entity
    8944351 => $MANUALLY_CURATED, # manually curated complex
    8962101 => $ELECTRONICALLY_INFERRED, # elec inferred complex
    1629806 => $MANUALLY_CURATED, # manually curated EWAS
    9492442 => $ELECTRONICALLY_INFERRED, # elec inferred EWAS
    5652148 => $MANUALLY_CURATED, # manually curated GEE
    8982667 => $ELECTRONICALLY_INFERRED, # elec inferred GEE
    1467470 => $MANUALLY_CURATED, # manually curated defined set
    8986465 => $ELECTRONICALLY_INFERRED, # elec inferred defined set
    983323 => $MANUALLY_CURATED, # manually curated polymer
    9029501 => $ELECTRONICALLY_INFERRED, # elec inferred polymer,
    1214171 => $MANUALLY_CURATED, # manually curated cow EWAS (re-used by elec inferred RLE)
);

foreach my $instance_id (keys %instances) {
    my $instance = $release_dba->fetch_instance_by_db_id($instance_id)->[0];

    is(is_electronically_inferred($instance) ? 1 : 0, $instances{$instance_id}, "testing electronic inference status for $instance_id");
}

my %electronic_inference_to_source;
tie %electronic_inference_to_source, 'Tie::IxHash';
%electronic_inference_to_source = (
    9719690 => [6781939, 6783290], # elec inferred EWAS with multiple sources
    9351974 => [420855, 445788], # elec inferred defined set with multiple sources
    9018150 => [190222, 190230], # elec inferred complex with multiple sources
    9063027 => [193508], # elec inferred reaction
    9099164 => [5419271], # elec inferred black box event
    9304349 => [5229194], # elec inferred depolymerisation
    9028516 => [936986], # elec inferred polymerisation
    9176553 => [8849347], # elec inferred catalyst activity
    9254173 => [71663], # elec inferred negative regulation
    9363542 => [1663719], # elec inferred positive regulation
    8962101 => [5672710], # elec inferred complex
    9492442 => [1629806], # elec inferred EWAS
    8982667 => [5652148], # elec inferred GEE
    8986465 => [1467470], # elec inferred defined set
    9029501 => [983323], # elec inferred polymer
);

foreach my $electronic_inference_db_id (keys %electronic_inference_to_source) {
    my @expected_source_db_ids = sort { $a <=> $b } @{$electronic_inference_to_source{$electronic_inference_db_id}};

    my $electronic_instance = $release_dba->fetch_instance_by_db_id($electronic_inference_db_id)->[0];
    my @received_source_db_ids = sort { $a <=> $b } map {$_->db_id} get_source_for_electronically_inferred_instance($electronic_instance);

    is_deeply(\@received_source_db_ids, \@expected_source_db_ids, "checking source instances for electronic instance $electronic_inference_db_id");
}

# Testing the retrieval of an event and the name of its modifier
my $db_id = 2262749; # Cellular response to hypoxia (human)
my $event = $release_dba->fetch_instance_by_db_id($db_id)->[0];
is($event->db_id, $db_id, "checking db id of retrieved event $db_id");
is($event->displayName, "Cellular response to hypoxia", "checking display name of retrieved event $db_id");
is(get_event_modifier($event), "D'Eustachio, Peter", "checking event modifier for event $db_id");

# Testing the retrieval of an event and the name of its creator when no modifier is present
$db_id = 5669318; # Expression of Rora (mouse)
$event = $release_dba->fetch_instance_by_db_id($db_id)->[0];
is($event->db_id, $db_id, "checking db id of retrieved event $db_id");
is($event->displayName, "Expression of Rora", "checking display name of retrieved event $db_id");
is(get_event_modifier($event), "May, Bruce", "checking event modifier for event $db_id");

done_testing();