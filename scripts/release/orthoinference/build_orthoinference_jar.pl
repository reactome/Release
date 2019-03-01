#!/usr/bin/perl
use strict;
use warnings;

use autodie qw/:all/;
use Cwd;
use File::Basename;
use File::Spec;
use Getopt::Long;

use GKB::Config;
use GKB::CommonUtils;

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
$orthopairs_path ||= File::Spec->catfile(dirname(getcwd), 'orthopairs', $release, '/');

my $starting_directory = getcwd;
my $orthoinference_repository = 'data-release-pipeline';

if (! (-d "$orthoinference_repository/.git")) {
    system "git clone https://github.com/reactome/$orthoinference_repository.git";
}
chdir $orthoinference_repository;
system 'git pull';
run_command('git checkout develop', {
    ignore_error => qr/^Already on .*/;
});
chdir 'release-common-lib';
system 'mvn clean install';
my $orthoinference_project_dir = File::Spec->catfile($starting_directory, $orthoinference_repository, 'orthoinference');
chdir $orthoinference_project_dir;

my $resources_dir = File::Spec->catfile($orthoinference_project_dir, 'src', 'main', 'resources');
my $config_file = File::Spec->catfile($resources_dir, 'config.properties');
my $species_config = File::Spec->catfile($resources_dir, 'Species.json');
create_config_properties_file($config_file, {
    username => $user,
    password => $password,
    database => $database,
    host => $host,
    port => $port,
    pathToSpeciesConfig => $species_config,
    pathToOrthopairs => $orthopairs_path,
    releaseNumber => $release,
    dateOfRelease => $release_date,
    personId => $person_id,
});
system "git update-index --assume-unchanged $config_file"; 
system "ln -sf $starting_directory/normal_event_skip_list.txt $resources_dir";
system "ln -sf $orthoinference_project_dir/runOrthoinference.sh $starting_directory/runOrthoinference.sh";

sub create_config_properties_file {
    my $config_properties_file = shift;
    my $options = shift;
    open(my $config_fh, '>', $config_properties_file);
    my @properties = map { $_ . '=' . $options->{$_} } keys %{$options};
    foreach my $property (@properties) {
        print {$config_fh} "$property\n";
    }
    close $config_fh;

    return;
}
