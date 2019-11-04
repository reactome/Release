#!/usr/local/bin/perl
use strict;
use warnings;

use lib '/usr/local/gkb/modules';
use GKB::Config;

use autodie qw/:all/;
use Carp;
use Cwd;
use Getopt::Long;
use Data::Dumper;
use English qw/-no-match-vars/;
use File::Basename;
use List::MoreUtils qw/uniq/;
use Log::Log4perl qw/get_logger/;
use Readonly;

Log::Log4perl->init(\$LOG_CONF);

Readonly my $data_release_pipeline_repository => 'data-release-pipeline';
Readonly my $repo_url => "https://github.com/reactome/$data_release_pipeline_repository";
Readonly my $repo_local_directory => cwd() . "/$data_release_pipeline_repository"; # Must be an absolute path
Readonly my $repo_application => 'chebi-update';
Readonly my $repo_version => '1.1.2';
Readonly my $repo_tag => "$repo_application-$repo_version";
Readonly my $application_properties => "$repo_application.properties";

Readonly my $application_directory => "$repo_local_directory/$repo_application";
Readonly my $resource_dir => "$application_directory/src/main/resources/";

my $reactome_version = $ARGV[0] || die "Reactome version as the first argument to $PROGRAM_NAME\n";

if (-e $repo_local_directory) {
    update_repo($repo_local_directory);
} else {
    clone_repo($repo_url);
}

checkout_branch($repo_local_directory, $repo_tag);
copy_properties_file_to_repo($application_properties, $resource_dir);
build_jar_and_execute($repo_local_directory, $repo_application, $repo_tag, $application_properties);
collect_logs_and_cache($reactome_version, $application_directory);

sub update_repo {
    my $logger = get_logger(__PACKAGE__);

    my $repository_root = shift || $logger->confess('Need repository directory');

    $logger->info("$repository_root exists, pulling");
    my $start_directory = cwd(); # abs_path($0);
    chdir $repository_root;
    # Start on develop. Pull will fail if we're not already on a branch
    checkout_branch($repository_root);
    system 'git pull';
    chdir $start_directory;

    return 1; # Repo updated successfully
}

sub clone_repo {
    my $logger = get_logger(__PACKAGE__);

    my $repo_location = shift || $logger->logconfess('Need repository url to clone');

    $logger->info("Cloning $repo_location");
    system "git clone $repo_location";

    return 1; # Cloned successfully
}

sub checkout_branch {
    my $logger = get_logger(__PACKAGE__);

    my $repository_root = shift || $logger->confess('Need repository directory');
    my $branch_or_tag = shift // 'develop';

    my $start_directory = cwd(); # abs_path($0);
    chdir $repository_root;
    system "git checkout $branch_or_tag";
    chdir $start_directory;

    return 1; # Branch checkout out successfully
}

sub copy_properties_file_to_repo {
    my $logger = get_logger(__PACKAGE__);

    my $properties_file = shift || $logger->logconfess('A properties file is required');
    my $target_dir = shift || $logger->logconfess('Need a target directory to copy properties files');
    if (! -e $properties_file) {
        $logger->logconfess("$properties_file file does not exist.  Please create it.");
    }
    system "cp $properties_file $resource_dir";

    return 1; # Properties file copied to repo successfully
}

sub build_jar_and_execute {
    my $logger = get_logger(__PACKAGE__);

    my $repository_root = shift || $logger->logconfess('Need repository directory');
    my $repository_application = shift || $logger->logconfess('Need repository application directory');
    my $repository_tag = shift || $logger->logconfess('Need repository tag');
    my $properties_file = shift || $logger->logconfess('Need properties file');

    my $start_directory = cwd(); # abs_path($0);

    # Need to build/install release-common-lib first.
    chdir "$repository_root/release-common-lib";
    system 'mvn clean compile install';
    chdir "$repository_root/$repository_application";
    system 'mvn clean compile assembly:single';

    # If there's a cache file that's been saved in the parent directory, move it back into position, it might be needed.
    if (-e "$start_directory/chebi-cache") {
        system "mv $start_directory/chebi-cache .";
    }

    $logger->info("Executing $repository_application");
    system "java -jar target/$repository_tag-jar-with-dependencies.jar src/main/resources/$properties_file";
    $logger->info("Finished executing $repository_application");

    chdir $start_directory;

    return 1; # Built and executed successfully
}

sub collect_logs_and_cache {
    my $logger = get_logger(__PACKAGE__);

    my $release_version = shift || $logger->logconfess('Need Reactome release version');
    my $application_directory = shift || $logger->logconfess('Need application directory in repository');
    my $log_directory = cwd() . "/archive/$release_version";

    # TODO: Zip all the logs into an archive and then email them when the step finishes, instead of the old chebi.wiki
    if (! -e $log_directory) {
        system "mkdir -p $log_directory";
    }
    system "cp -a $application_directory/logs $log_directory";
    system "tar -czf $application_directory/chebi_update_logs_R$release_version.tgz $log_directory/logs";
    # keep a copy of any cache file
    system "mv $application_directory/chebi-cache .";

    return 1; # Logs and cache saved successfully
}
