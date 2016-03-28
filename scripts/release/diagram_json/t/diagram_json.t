#!/usr/bin/perl
use strict;
use warnings;

use Test::More;

require_ok('diagram_json.pl');
ok(1, "testing works");

my $where_to_clone = '/tmp';
ok(clone_diagram_core_repository_from_github($where_to_clone), 'clone_diagram_core_repository_from_github successful');
ok(-d "$where_to_clone/diagram-core", "cloned repo exists as directory");
ok(-d "$where_to_clone/diagram-core/.git", "cloned repo is a git repository");

my $link = 'diagram-core';
create_symbolic_link_to_diagram_core_repository($where_to_clone, $link);
ok(-l $link, "symbolic link exists");

ok(remove_diagram_core_repository($where_to_clone), 'remove_diagram_core_repository successful');
is(-d "$where_to_clone/diagram-core", undef, "cloned repo removed");

done_testing();