#!/usr/bin/perl
use strict;
use warnings;

use Test::More;

use_ok('GKB::Release::Utils');
ok(1, "testing works");

my $command = "perl -e 'print STDERR \"error\"; print \"output\";'";
my @results = cmd("Test command", [[$command]]);

ok(@results, "got results from cmd()");
is($results[0]->{'command'}, $command, "$command archived");
is(@{$results[0]->{'args'}}, 0, "no arguments");
is($results[0]->{'stdout'}, 'output', "output archived");
is($results[0]->{'stderr'}, 'error', "errors archived");
like($results[0]->{'exit_code'}, qr/^\d+$/, "exit code is numeric");
is($results[0]->{'exit_code'}, 0, "command executed successfully");

done_testing();