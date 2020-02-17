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
use English qw( -no_match_vars );
use File::Basename;
use List::MoreUtils qw/uniq/;
use Log::Log4perl qw/get_logger/;
use Readonly;

Log::Log4perl->init(\$LOG_CONF);

Readonly my $start_directory => cwd();
Readonly my $chebi_update_application => 'release-chebi-update';
Readonly my $chebi_update_application_dir => "$start_directory/$chebi_update_application";
Readonly my $chebi_update_repository_url => "https://github.com/reactome/$chebi_update_application";
Readonly my $chebi_update_version => '1.1.4-SNAPSHOT';
Readonly my $chebi_update_version_tag => 'develop';#"v$chebi_update_version";
Readonly my $chebi_update_properties_file => 'chebi-update.properties';
Readonly my $resource_dir => "$chebi_update_application_dir/src/main/resources/";
Readonly my $archive_dir => "$start_directory/archive";

my $reactome_version = $ARGV[0] || die "Reactome version as the first argument to $PROGRAM_NAME\n";

if (-d $chebi_update_application_dir) {
    update_repo($chebi_update_application_dir);
} else {
    clone_repo($chebi_update_repository_url);
}

checkout_branch($chebi_update_application_dir, $chebi_update_version_tag);
copy_properties_file_to_repo($chebi_update_properties_file, $resource_dir);
build_jar_and_execute($chebi_update_application_dir, $chebi_update_version, $chebi_update_properties_file);
collect_logs_and_cache($reactome_version, $chebi_update_application_dir);

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

    my $repository_application_dir = shift || $logger->logconfess('Need repository directory');
    my $repository_version = shift || $logger->logconfess('Need repository version');
    my $properties_file = shift || $logger->logconfess('Need properties file');

    my $start_directory = cwd(); # abs_path($0);

    chdir "$repository_application_dir";
    system 'mvn clean compile assembly:single';

    # If there's a cache file that's been saved in the parent directory, move it back into position, it might be needed.
    if (-e "$start_directory/chebi-cache") {
        system "mv $start_directory/chebi-cache .";
    }

    $logger->info("Executing $repository_application_dir");
    system "java -jar target/chebi-update-$repository_version-jar-with-dependencies.jar src/main/resources/$properties_file";
    $logger->info("Finished executing $repository_application_dir");

    chdir $start_directory;

    return 1; # Built and executed successfully
}

sub collect_logs_and_cache {
    my $logger = get_logger(__PACKAGE__);

    my $release_version = shift || $logger->logconfess('Need Reactome release version');
    my $application_directory = shift || $logger->logconfess('Need application directory in repository');
    my $archive_dir = shift || $logger->logconfess('Need archive directory');
    my $log_directory = "$archive_dir/$release_version";

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
