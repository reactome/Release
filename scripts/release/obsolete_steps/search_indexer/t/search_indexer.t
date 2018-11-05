#!/usr/bin/perl
use strict;
use warnings;

use Test::More;

require_ok('search_indexer.pl');
ok(1, "testing works");

my $where_to_clone = '/tmp';
my $repo_path = git_clone_search_repository($where_to_clone);
is($repo_path, "$where_to_clone/Search", "repo path exists");
ok(-d "$where_to_clone/Search", "cloned repo exists as directory");
ok(-d "$where_to_clone/Search/.git", "cloned repo is a git repository");

my $link = 'search';
make_symbolic_link($repo_path, $link);
ok(-l $link, "symbolic link exists");

my $jar_path = build_search_jar("$link/indexer");
ok(-e $jar_path, "jar built successfully");

remove_symbolic_link($link);
is(-e $link, undef, "symbolic link successfully removed");

my $removal_result = remove_cloned_repository($repo_path);
is($removal_result, 1, "repo removal subroutine returned success");
is(-d "$where_to_clone/Search", undef, "cloned repo removed");

done_testing();