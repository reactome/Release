#!/usr/bin/perl
use strict;
use warnings;

use lib '/usr/local/gkb/modules';

use Test::More;
use Test::Exception;

use_ok('GKB::Release::Step');
ok(1, "testing works");

ok(GKB::Release::Step::_file_size_ok(10, 5), "bigger file");
ok(!GKB::Release::Step::_file_size_ok(5, 10), "smaller file");
ok(GKB::Release::Step::_file_size_ok(9.5, 10), "smaller file but less than 10 percent difference");
ok(GKB::Release::Step::_file_size_ok(10, 10), "same file size");
ok(!GKB::Release::Step::_file_size_ok(0, 10), "no new file size");
ok(!GKB::Release::Step::_file_size_ok(10, 0), "no old file size");
ok(!GKB::Release::Step::_file_size_ok(0, 0), "no new or old file size");

ok(!GKB::Release::Step::get_file_size_errors(), "no file size errors");

my $step_instance = GKB::Release::Step->new('gkb' => 'gkbdev');
my $command = "perl -e 'print STDERR \"error\"; print \"output\";'";
my @results = $step_instance->cmd("Test command", [[$command]]);

ok(@results, "got results from cmd()");
is($results[0]->{'command'}, $command, "$command archived");
is(@{$results[0]->{'args'}}, 0, "no arguments");
is($results[0]->{'stdout'}, 'output', "output archived");
is($results[0]->{'stderr'}, 'error', "errors archived");
like($results[0]->{'exit_code'}, qr/^\d+$/, "exit code is numeric");
is($results[0]->{'exit_code'}, 0, "command executed successfully");

dies_ok {$step_instance->run_commands()} 'abstract run_commands method dies';

done_testing();