#!/usr/local/bin/perl
use strict;
use warnings;

use lib '/usr/local/gkb/modules';
use GKB::Config;

use autodie qw/:all/;
use Cwd;
use Getopt::Long;
use Data::Dumper;
use English qw( -no_match_vars );
use File::Basename;
use List::MoreUtils qw/uniq/;

use Log::Log4perl qw/get_logger/;

Log::Log4perl->init(\$LOG_CONF);
my $logger = get_logger(__PACKAGE__);

my $start_directory = cwd();
my $go_update_application = 'release-go-update';
my $go_update_application_dir = "$start_directory/$go_update_application";
my $go_update_repository = "https://github.com/reactome/$go_update_application";
my $go_update_version = '1.0.2';
my $go_update_version_tag = "v$go_update_version";
my $go_update_properties_file = 'go-update.properties';
my $resource_dir = "$go_update_application_dir/src/main/resources";
my $archive_dir = "$start_directory/archive";

my $release_version = $ARGV[0] || die "Usage: perl $PROGRAM_NAME [reactome_release_version]. E.g. perl $PROGRAM_NAME 70\n";

print "start_directory: $start_directory\n";

if (!(-d $go_update_application_dir)) {
    $logger->info("Cloning $go_update_repository");
    system "git clone $go_update_repository";
} else {
    $logger->info("$go_update_application_dir exists, pulling");
}

chdir $go_update_application_dir;
if (!$go_update_version_tag) {
    # Start on develop. Pull will fail if we're not already on a branch
    system 'git checkout develop';
} else {
    # Still need to do a pull, since the tag might be been created after the last pull
    system "git checkout $go_update_version_tag";
}
system 'git pull';


system "cp $start_directory/$go_update_properties_file $resource_dir";

$logger->info('Getting the GO files...');
system "wget -O $resource_dir/go.obo http://current.geneontology.org/ontology/go.obo";
system "wget -O $resource_dir/ec2go http://geneontology.org/external2go/ec2go";

$logger->info("Executing $go_update_application");
build_jar_and_execute($go_update_version, $go_update_properties_file, $release_version, $archive_dir);
$logger->info("Finished executing $go_update_application");

sub build_jar_and_execute {
    my $go_update_version_tag = shift;
    my $go_update_properties_file = shift;
    my $release_version = shift;
    my $archive_dir = shift;

    my $logs_archive_dir = "$archive_dir/$release_version/logs";

    system 'mvn clean compile assembly:single';

    system "java -jar target/go-update-$go_update_version-jar-with-dependencies.jar src/main/resources/$go_update_properties_file";
    # Move the logs up to the main directory so that they can get archived.
    # system("cp logs/* ../../");
    # TODO: Zip all the logs into an archive and then email them when the step finishes, instead of the old go.wiki
    system "mkdir -p $logs_archive_dir";
    system "cp -a logs $logs_archive_dir";
    system "tar -czf go_update_logs_R$release_version.tgz -C $logs_archive_dir .";
}
