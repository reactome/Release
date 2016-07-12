#!/usr/local/bin/perl  -w
use strict;

use autodie qw/:all/;
use Getopt::Long;

my ($version, $statistic_file_directory);
GetOptions(
    "version:s" => \$version,
    'stats_file_dir:s' => \$statistic_file_directory
);
$version || die "Usage: $0 -version reactome_version [-stats_file_dir statistics_file_directory_path]";
$statistic_file_directory ||= '/usr/local/gkb/scripts/release/website_files_update';

my @statistic_files = qw/stats.html stats.png inference_stats.png report_ortho_inference.txt release_stats/;
system("git stash");
system("git add $statistic_file_directory/$_") foreach @statistic_files;
system("git commit -m 'version $version'");
system("git push");
system("git stash pop");