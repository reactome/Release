#!/usr/bin/perl
use strict;
use warnings;

use Test::More;

require_ok('fireworks.pl');
ok(1, "testing works");

my $where_to_clone = '/tmp';
ok(clone_fireworks_repository_from_github($where_to_clone), 'clone_fireworks_repository_from_github successful');
ok(-d "$where_to_clone/Fireworks", "cloned repo exists as directory");
ok(-d "$where_to_clone/Fireworks/.git", "cloned repo is a git repository");

my $link = 'fireworks';
create_symbolic_link_to_fireworks_repository();
ok(-l $link, "symbolic link exists");

ok(remove_fireworks_repository($where_to_clone), 'remove_fireworks_repository successful');
is(-d "$where_to_clone/Fireworks", undef, "cloned repo removed");

done_testing();