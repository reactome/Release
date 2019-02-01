#!/usr/bin/perl
use strict;
use warnings;

our $VERSION = 1.0;

use autodie qw/:all/;
use Carp;
use Cwd;
use File::Slurp;
use File::Spec::Functions;

opendir my $dir, getcwd;
my $version_topic_file_pattern = qr/^ver(\d+)_topics.txt$/;
my %version_topic_files =
    map { /$version_topic_file_pattern/ ? ($1 => catfile(getcwd, $_)) : ()} # Version to file path
    grep { /$version_topic_file_pattern/ } readdir $dir;
closedir $dir;

if (scalar keys %version_topic_files != 2) {
    die 'There must be only 2 version topic files in the current working directory ' . getcwd . "to compare\n";
}

my %version_to_topic_list =
    map { $_ => read_contents_to_array_ref($version_topic_files{$_}) }
    keys %version_topic_files;

my ($additions, $removals) = compare_topic_lists(\%version_to_topic_list);

foreach my $addition (@{$additions}) {
    print "$addition added to current release\n";
}

foreach my $removal (@{$removals}) {
    print "$removal added to current release\n";
}

sub read_contents_to_array_ref {
    my $file_path = shift;

    if (!(-e $file_path)) {
        confess "$file_path does not exist\n";
    }

    my @file_contents = read_file($file_path, chomp => 1);

    return \@file_contents;
}

sub compare_topic_lists {
    my $version_to_topic_list = shift;

    my @versions = sort { $a <=> $b } keys %{$version_to_topic_list};
    my %previous_topic_list = map { $_ => 1 } @{$version_to_topic_list->{$versions[0]}};
    my %current_topic_list = map { $_ => 1 } @{$version_to_topic_list->{$versions[1]}};

    my (@additions, @removals);

    foreach my $previous_topic (keys %previous_topic_list) {
        if (!(exists $current_topic_list{$previous_topic})) {
            push @removals, $previous_topic;
        }
    }

    foreach my $current_topic (keys %current_topic_list) {
        if (!(exists $previous_topic_list{$current_topic})) {
            push @additions, $current_topic;
        }
    }

    return (\@additions, \@removals);
}