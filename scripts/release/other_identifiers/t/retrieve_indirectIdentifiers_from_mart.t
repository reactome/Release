#!/usr/bin/perl
use strict;
use warnings;

use Test::More;
use Test::Exception;

use File::Spec;
use Log::Log4perl::Level; # Imports $OFF
use Path::Tiny qw/path/;

my $script_dir = path($0)->absolute->parent(2)->stringify;
require_ok(File::Spec->catdir($script_dir, 'retrieve_indirectIdentifiers_from_mart.pl'));
Log::Log4perl->get_logger(__PACKAGE__)->level($OFF); # Suppress logging during tests

my @species_to_query = get_species_to_query('hsap');
is(scalar @species_to_query, 1, 'got single species to query');
is($species_to_query[0], 'hsap', 'single species to query is hsap');

throws_ok { get_species_to_query('xxx') } qr/information is unknown/, 'no species to query for unknown species code';

@species_to_query = get_species_to_query();
ok(scalar @species_to_query > 1, 'got many species to query');
ok((grep { $_ eq 'hsap' } @species_to_query), 'got hsap in array of species to query');
ok((grep { $_ eq 'mmus' } @species_to_query), 'got mmus in array of species to query');

is(get_species_mart_name('hsap'), 'hsapiens', 'got species mart name for hsap');

throws_ok { get_identifiers() } qr/^Need species name/, 'no identifiers without species name';
my @identifiers = get_identifiers('hsapiens');
ok(scalar @identifiers > 6, 'got more than default identifiers');

my $results = run_wget_query('hsap', 'ccds');
like($results->{'output'}, qr/\[success\]/, 'identifier file completely downloaded');
is($results->{'error'}, '', 'no errors with wget query to EnsEMBL Mart');

done_testing();