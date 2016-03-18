#!/usr/local/bin/perl  -w
use strict;

use autodie qw/:all/;
use Getopt::Long;

my ($version);
GetOptions("version:s" => \$version);
$version || die "Usage: $0 -version reactome_version";

my @statistic_files = qw/stats.html stats.png inference_stats.png report_ortho_inference.txt release_stats/;
system("git add $_") foreach @statistic_files;
system("git commit -m 'version $version'");
system("git push");