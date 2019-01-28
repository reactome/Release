#!/usr/bin/perl
use strict;
use warnings;

use autodie qw/:all/;
use Cwd;
use File::Basename;
use File::Spec;
use Getopt::Long;

use GKB::Config;

my ($user, $password, $database, $host, $port, $orthopairs_path, $release, $release_date, $person_id);
GetOptions(
    'user:s' => \$user,
    'pass:s' => \$password,
    'db:s' => \$database,
    'host:s' => \$host,
    'port:i' => \$port,
    'orthopairs_path:s' => \$orthopairs_path,
    'release:s' => \$release,
    'release_date:s' => \$release_date,
    'person_id:s' => \$person_id,
);
$release || die "Need Reactome release version: -release [release version]\n";
$release_date || die "Need release date: -release_date [yyyy-mm-dd]\n";
$person_id || die "Need person id: -person_id [db id of person instance for orthoinference script]\n";
$user ||= $GKB::Config::GK_DB_USER;
$password ||= $GKB::Config::GK_DB_PASS;
$database ||= $GKB::Config::GK_DB_NAME;
$host ||= $GKB::Config::GK_DB_HOST;
$port ||= $GKB::Config::GK_DB_PORT;
$orthopairs_path ||= File::Spec->catfile(dirname(getcwd), 'orthopairs', $release);

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
chdir File::Spec->catfile($starting_directory, $orthoinference_repository, 'orthoinference');

my $resources_dir = File::Spec->catfile(qw/src main resources/);
create_config_properties_file(File::Spec->catfile($resources_dir, 'config.properties'), {
    username => $user,
    password => $password,
    database => $database,
    host => $host,
    port => $port,
    orthopairs_path => $orthopairs_path,
    release_number => $release,
    release_date => $release_date,
    person_id => $person_id,
});

sub create_config_properties_file {
    my $config_properties_file = shift;
    my $options = shift;
    open(my $config_fh, '>', $config_properties_file);
    my @properties = map { $_ . '=' . $options->{$_} } keys %{$options};
    push @properties, 'pathToSpeciesConfig=src/main/resources/Species.json';
    foreach my $property (@properties) {
        print {$config_fh} "$property\n";
    }
    close $config_fh;

    return;
}