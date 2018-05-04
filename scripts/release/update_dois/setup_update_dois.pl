#!/usr/local/bin/perl  -w
use strict;

use lib '/usr/local/gkb/modules';
# use GKB::Config;

use autodie qw/:all/;
use Cwd;
use Getopt::Long;
use Data::Dumper;
use File::Basename;
use List::MoreUtils qw/uniq/;

use Log::Log4perl qw/get_logger/;
# Log::Log4perl->init(\$LOG_CONF);
my $logger = get_logger(__PACKAGE__);

if (-e "data-release-pipeline") {
  chdir "data-release-pipeline/update-dois";
} else {
  clone_update_dois_repo();
}
chdir "data-release-pipeline/update-dois";
build_jar_and_execute();

sub clone_update_dois_repo {
  my $resource_dir = "data-release-pipeline/update-dois/src/main/resources/";
  system("git clone https://github.com/reactome/data-release-pipeline");
  system("cp config.properties $resource_dir");
  system("cp log4j.properties $resource_dir");
  system("cp UpdateDOIs.report $resource_dir");
}

sub build_jar_and_execute {
  system("mvn clean compile assembly:single");
  system("java -jar target/update-dois-0.0.1-jar-with-dependencies.jar");
  system("cp UpdateDOIs.log ../..");
}
