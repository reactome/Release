#!/usr/bin/perl
use strict;
use warnings;

use autodie qw/:all/;
use Cwd;

my $starting_directory = getcwd;
my $orthoinference_repository = 'data-release-pipeline';

if (! (-d "$orthoinference_repository/.git")) {
    system "git clone https://github.com/reactome/$orthoinference_repository.git";
}
chdir $orthoinference_repository;
system 'git pull';
system 'git checkout develop';
chdir 'release-common-lib';
system 'mvn clean install';
chdir "$starting_directory/$orthoinference_repository/orthoinference";

sub create_config_properties_file {

}