#!/usr/local/bin/perl  -w
use strict;

use lib '/usr/local/gkb/modules';
use GKB::Config;

use autodie;
use Cwd;
use Getopt::Long;
use Data::Dumper;
use File::Basename;
use List::MoreUtils qw/uniq/;

use Log::Log4perl qw/get_logger/;

Log::Log4perl->init(\$LOG_CONF);
my $logger = get_logger(__PACKAGE__);

my $data_release_pipeline_application_version = "1.0.0";
my $data_release_pipeline_application = "go-update";
my $data_release_pipeline_tag = "feature/data-model-changes-for-GO";
my $data_release_pipeline_repository = "https://github.com/reactome/data-release-pipeline";
my $resource_dir = "data-release-pipeline/".$data_release_pipeline_application."/src/main/resources";
my $start_directory = cwd(); # abs_path($0);

my $release_version = $ARGV[0];

if (!$ARGV[0]) {
    die "Usage: perl $0 [reactome_release_version]. E.g. perl $0 70\n";
}

print "start_directory: ".$start_directory."\n";

if (-e "data-release-pipeline") {
  $logger->info("data-release-pipeline exists, pulling");
  # Start on develop. Pull will fail if we're not already on a branch
  chdir "data-release-pipeline";
  if (!$data_release_pipeline_tag) {
    system("git checkout develop");
  } else {
    # Still need to do a pull, since the tag might be been created after the last pull
    system("git checkout $data_release_pipeline_tag");
  }
  system("git pull");

  chdir $start_directory;
  system("cp ".$data_release_pipeline_application.".properties $resource_dir");
  chdir "data-release-pipeline";
} else {
  $logger->info("Cloning data-release-pipeline");
  clone_repo();
  chdir "data-release-pipeline/".$data_release_pipeline_application;
}

chdir($start_directory."/data-release-pipeline/".$data_release_pipeline_application."/");
$logger->info("Getting the GO files...");
system("wget -O $start_directory/$resource_dir/go.obo http://current.geneontology.org/ontology/go.obo");
system("wget -O $start_directory/$resource_dir/ec2go http://geneontology.org/external2go/ec2go");

$logger->info("Executing ".$data_release_pipeline_application);
build_jar_and_execute();
# system("mv UpdateDOIs.log ../..");
$logger->info("Finished executing ".$data_release_pipeline_application);

sub clone_repo {
  system("git clone ".$data_release_pipeline_repository);
  # if there is a tag defined, we should check that out.
  if ($data_release_pipeline_tag ne '')
  {
    chdir "data-release-pipeline";
    system("git checkout ".$data_release_pipeline_tag);
    chdir $start_directory;
  }
  system("cp ".$data_release_pipeline_application.".properties $resource_dir");
}

sub build_jar_and_execute {
  # Need to build/install release-common-lib first.
  chdir($start_directory."/data-release-pipeline/");
  print "currently in: ".cwd()."\n";
  chdir "release-common-lib";
  system("mvn clean compile install");
  chdir "../".$data_release_pipeline_application;
  system("mvn clean compile assembly:single");

  system("java -jar target/".$data_release_pipeline_application."-".$data_release_pipeline_application_version."-jar-with-dependencies.jar src/main/resources/".$data_release_pipeline_application.".properties");
  # Move the logs up to the main directory so that they can get archived.
#  system("cp logs/* ../../");
  # TODO: Zip all the logs into an archive and then email them when the step finishes, instead of the old go.wiki
  system("cp -a logs ../../archive/$release_version/logs");
  system("tar -czf go_update_logs_R$release_version.tgz -C ../../archive/$release_version/logs .");
}
