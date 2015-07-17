#!/usr/bin/perl
use strict;
use warnings;

use lib '/usr/local/gkb/modules';

use Test::More;

use_ok('GKB::Release::Utils');
ok(1, "testing works");

is(replace_gkb_alias_in_dir('/usr/local/gkb/scripts', 'gkbdev'), '/usr/local/gkbdev/scripts', 'replaced gkb with gkbdev');
is(replace_gkb_alias_in_dir('/usr/local/gkbdev/website', 'gkb'), '/usr/local/gkb/website', 'replaced gkbdev with gkb');
isnt(replace_gkb_alias_in_dir('/usr/local/gkdev/website', 'gkb'), '/usr/local/gkb/website', 'gkbdev not replaced because of typo');
is(replace_gkb_alias_in_dir('/home/weiserj/release', 'gkb'), '/home/weiserj/release', 'no gkb alias to replace');

done_testing();