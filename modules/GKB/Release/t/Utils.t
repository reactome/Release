#!/usr/bin/perl
use strict;
use warnings;

use lib '/usr/local/gkb/modules';

use Test::More;
use Test::Output;

use_ok('GKB::Release::Utils');
ok(1, "testing works");

is(replace_gkb_alias_in_dir('/usr/local/gkb/scripts', 'gkbdev'), '/usr/local/gkbdev/scripts', 'replaced gkb with gkbdev');
is(replace_gkb_alias_in_dir('/usr/local/gkbdev/website', 'gkb'), '/usr/local/gkb/website', 'replaced gkbdev with gkb');
isnt(replace_gkb_alias_in_dir('/usr/local/gkdev/website', 'gkb'), '/usr/local/gkb/website', 'gkbdev not replaced because of typo');
is(replace_gkb_alias_in_dir('/home/weiserj/release', 'gkb'), '/home/weiserj/release', 'no gkb alias to replace');

set_environment('reactomerelease.oicr.on.ca');
is($ENV{'HOME'}, '/home/' . $ENV{'USER'}, 'home directory set for user');
like($ENV{'PATH'}, qr/^\/usr\/local\/bin/, 'path contains /usr/local/bin');
like($ENV{'PERL5LIB'}, qr/\/usr\/local\/gkbdev\/modules/, 'path contains /usr/local/gkbdev/modules');
set_environment('reactomeprd1.oicr.on.ca');
like($ENV{'PERL5LIB'}, qr/\/usr\/local\/gkb\/modules/, 'path contains /usr/local/gkb/modules');

test_prompt();

done_testing();

sub test_prompt {
    my $query = 'Enter test input: ';
    my $input = 'testing prompt';
    my $response;
    open my $stdin, '<', \ "$input";
    local *STDIN = $stdin;
    stdout_is {$response = prompt($query)} $query, 'query is given to standard output';
    is($input, $response, 'prompt retruns expected response');
}