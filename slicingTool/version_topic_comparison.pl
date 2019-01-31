#!/usr/bin/perl
use strict;
use warnings;

use autodie qw/:all/;

opendir my $dir, '.';
my $version_topic_file_pattern = qr/^ver(\d+)_topics.txt$/;
my %version_topic_files =
    map { $version_topic_file_pattern ? $1 => $_}
    grep { $version_topic_file_pattern } readdir $dir;
closedir $dir;

foreach my $version (keys %version_topic_files) {
    print $version . ': ' . $version_topic_files{$version};
}