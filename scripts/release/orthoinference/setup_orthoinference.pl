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

my (
    $user, $password, $current_database, $previous_database, $host, $port,
    $orthopairs_path, $release, $release_date, $person_id
);
GetOptions(
    'user:s' => \$user,
    'pass:s' => \$password,
    'current_db:s' => \$current_database,
    'previous_db:s' => \$previous_database,
    'host:s' => \$host,
    'port:i' => \$port,
    'orthopairs_path:s' => \$orthopairs_path,
    'release:s' => \$release,
    'release_date:s' => \$release_date,
    'person_id:s' => \$person_id,
);

# Array of arrays containing argument to check and error message if undefined
my $errors = check_required_arguments_defined([
    [$release, 'Need Reactome release version: -release [release version]'],
    [$release_date, 'Need release date: -release_date [yyyy-mm-dd]'],
    [$person_id, 'Need person id: -person_id [db id of person instance for orthoinference script]'],
    [$current_database, 'Need current database: -current_db [name of database into which inference should be written]'],
    [$previous_database, 'Need previous database: -previous_db [name of previous Reactome release database]']
]);

if ($errors) {
    die "$errors\n";
}

$user ||= $GKB::Config::GK_DB_USER;
$password ||= $GKB::Config::GK_DB_PASS;
$host ||= $GKB::Config::GK_DB_HOST;
$port ||= $GKB::Config::GK_DB_PORT;
$orthopairs_path ||= File::Spec->catfile(dirname(getcwd), 'orthopairs', $release, '');

my $starting_directory = getcwd;
my $orthoinference_repository = 'data-release-pipeline';

if (! (-d "$orthoinference_repository/.git")) {
    system "git clone https://github.com/reactome/$orthoinference_repository.git";
}
chdir $orthoinference_repository;
run_command('git checkout develop', {
    ignore_error => qr/^Already on .*/
});
system 'git pull';

my $orthoinference_project_dir = File::Spec->catfile($starting_directory, $orthoinference_repository, 'orthoinference');
chdir $orthoinference_project_dir;

my $resources_dir = File::Spec->catfile($orthoinference_project_dir, 'src', 'main', 'resources');
my $config_file = File::Spec->catfile($resources_dir, 'config.properties');
my $species_config = File::Spec->catfile($resources_dir, 'Species.json');
create_config_properties_file($config_file, {
    username => $user,
    password => $password,
    currentDatabase => $current_database,
    previousDatabase => $previous_database,
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

sub check_required_arguments_defined {
    my $arguments_2d_array = shift;

    my $error_message;
    foreach my $argument_array (@{$arguments_2d_array}) {
        my $argument = $argument_array->[0];
        if (! defined $argument) {
            if (defined $error_message) {
                $error_message .= "\n";
            }
            $error_message .= $argument_array->[1];
        }
    }

    return $error_message;
}

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
