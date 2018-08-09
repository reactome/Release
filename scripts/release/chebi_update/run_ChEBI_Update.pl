#!/usr/local/bin/perl  -w
use strict;

use lib '/usr/local/gkb/modules';
use GKB::Config;

use autodie qw/:all/;
use Cwd;
use Getopt::Long;
use Data::Dumper;
use File::Basename;
use List::MoreUtils qw/uniq/;

use Log::Log4perl qw/get_logger/;

Log::Log4perl->init(\$LOG_CONF);
my $logger = get_logger(__PACKAGE__);

my $data_release_pipeline_application_version = "0.0.1-SNAPSHOT";
my $data_release_pipeline_application = "chebi-update";
my $data_release_pipeline_tag = "";
my $data_release_pipeline_repository = "https://github.com/reactome/data-release-pipeline";

if (-e "data-release-pipeline") {
  $logger->info("data-release-pipeline exists, pulling");
  chdir "data-release-pipeline/".$data_release_pipeline_application;
  system("git pull");
  # if there is a tag defined, we should check that out.
  if ($data_release_pipeline_tag ne '')
  {
    chdir "data-release-pipeline";
    system("git checkout ".$data_release_pipeline_tag);
    chdir "..";
  }
} else {
  $logger->info("Cloning data-release-pipeline");
  clone_repo();
  chdir "data-release-pipeline/".$data_release_pipeline_application;
}

$logger->info("Executing ".$data_release_pipeline_application);
build_jar_and_execute();
# system("mv UpdateDOIs.log ../..");
$logger->info("Finished executing ".$data_release_pipeline_application);

sub clone_repo {
  my $resource_dir = "data-release-pipeline/".$data_release_pipeline_application."/src/main/resources/";
  system("git clone ".$data_release_pipeline_repository);
  system("cp ".$data_release_pipeline_application.".properties $resource_dir");
  # if there is a tag defined, we should check that out.
  if ($data_release_pipeline_tag ne '')
  {
    chdir "data-release-pipeline";
    system("git checkout ".$data_release_pipeline_tag);
    chdir "..";
  }
}

sub build_jar_and_execute {
  # Need to build/install release-common-lib first.
  chdir "../release-common-lib";
  system("mvn clean compile install");
  chdir "../".$data_release_pipeline_application;
  system("mvn clean compile assembly:single");
  system("java -jar target/".$data_release_pipeline_application."-".$data_release_pipeline_application_version."-jar-with-dependencies.jar");
  # Move the logs up to the main directory so that they can get archived.
  system("cp logs/* ../../");
}
