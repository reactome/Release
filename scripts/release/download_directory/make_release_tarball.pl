#!/usr/local/bin/perl -w
use strict;

use constant REPO => '/tmp/Release';
use constant TARD => '/tmp';
use constant BASE => '/usr/local/reactomes/Reactome/production';

# You need to be root to run this script!
#die "Sorry, root permission required.\n" unless $> == 0;

my $dir_for_tarball = TARD;

my $release = shift or die "Usage $0 release_num\n";

mkdir $dir_for_tarball unless (-d $dir_for_tarball);
chdir $dir_for_tarball;

mkdir $release unless -d $release;
chdir $release;

system "rm -fr reactome" if -d 'reactome';

my $base  = BASE;
my $repo  = REPO;
my $rhome = "$base/GKB/scripts/release/download_directory/$release";
mkdir $rhome unless -d $rhome;

# Since the github repo is not public, we will have to make sure it is checked
# out into /tmp on reactomerelease
check_github_repo();

# Make sure that other data are up-to-date
check_analysis_data($base);
check_solr_data($base);

my $unwanted_webapps = join(' ',(
"reactome/apache-tomcat/webapps/Analysis",
"reactome/apache-tomcat/webapps/reactomepathwaysummary",
"reactome/apache-tomcat/webapps/ELVWebApp",
"reactome/apache-tomcat/webapps/SBMLsqueezer",
"reactome/apache-tomcat/webapps/PDMap",
"reactome/apache-tomcat/webapps/ReactomeTools",
"reactome/apache-tomcat/webapps/solr",
"reactome/apache-tomcat/webapps/AnalysisService_Antonio"
));

print `pwd`;
my @cmds = (
    qq(mkdir reactome),
    qq(mkdir reactome/GKB),
    qq(cp -r $repo/website reactome/GKB),
    qq(cp -r $repo/modules reactome/GKB),
    qq(mkdir reactome/GKB/third_party_install),
    qq(cp $repo/third_party_install/config.tar.gz reactome/GKB/third_party_install),
    qq(rm -f reactome/GKB/website/html/stats*),
    qq(cp $base/GKB/scripts/release/website_files_update/stats.* reactome/GKB/website/html),
    qq(cp -r $base/Solr reactome),
    qq(cp -r $base/apache-tomcat* reactome),
    qq(rm -f reactome/apache-tomcat/webapps/*.war),
    qq(rm -fr $unwanted_webapps),
    qq(rm -f reactome/apache-tomcat/logs/*),
    qq(mkdir reactome/AnalysisService),
    qq(mkdir reactome/AnalysisService/temp),
    qq(mkdir reactome/AnalysisService/input),
    qq(cp $base/AnalysisService/input/analysis.bin reactome/AnalysisService/input),
    qq(mkdir reactome/RESTful),
    qq(mkdir reactome/RESTful/temp),
    qq(rm -fr reactome/GKB/modules/*ensem*),
    qq(find ./ -name .git* | xargs rm -f),
    qq(tar czf reactome.tar.gz reactome),
    qq(cp reactome.tar.gz $rhome),
    qq(cp $repo/third_party_install/install_reactome.sh $rhome),
);

for my $cmd (@cmds) {
    print "$cmd\n";
    system($cmd) == 0 or die "Something wrong with '$cmd'\n";
    sleep 1;
}

sub check_github_repo {
    # existence
    unless (-d REPO && -d REPO . '/.git') {
	die "Please get a copy of the github repo:\n",
	"cd /tmp\ngit clone https://github.com/reactome/Release.git\n";
    }

    # ageism
    my $now = time();
    my $max_age = 
	  7   # days
	* 24  # hours
	* 60  # minutes
	* 60; # seconds

    my $mod_date = (stat(REPO))[9];

    my $age = ($now - $mod_date)/60/60/24;
    if ($age > $max_age) {
	die "Your github clone is more than a week old.\n",
	"Please pull, checkout or clone a fresh copy to /tmp/Release\n";
    }

}


sub check_analysis_data {
    my $base = shift;
    my $data = "$base/AnalysisService/input/analysis_v$release.bin";
    unless (-e $data) {
	die "$data does not exist!  Make sure that you update the AnalysisService data before proceeding\n";
    }
}

sub check_solr_data {
    my $base = shift;
    my $data = "$base/Solr/cores/reactome_v$release";
    unless (-e $data) {
        die "$data does not exist!  Make sure that you update the Solr data before proceeding\n";
    }
}
