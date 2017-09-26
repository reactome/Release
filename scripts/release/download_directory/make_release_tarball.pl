#!/usr/local/bin/perl -w
use strict;
use lib '/usr/local/gkb/modules';
use GKB::Config;

use constant TARD => '/tmp/tarball';
use constant BASE => '/usr/local/reactomes/Reactome/production';
use constant MAXAGE => 2;

use Log::Log4perl qw/get_logger/;
Log::Log4perl->init(\$LOG_CONF);
my $logger = get_logger(__PACKAGE__);

my $dir_for_tarball = TARD;
system "rm -fr $dir_for_tarball" if -d $dir_for_tarball;

my $release = shift or die "Usage $0 release_num\n";

mkdir $dir_for_tarball unless (-d $dir_for_tarball);
chdir $dir_for_tarball;
my $repo = "$dir_for_tarball/$release/Release";

mkdir $release unless -d $release;
chdir $release;

system "rm -fr reactome" if -d 'reactome';

my $base  = BASE;
my $rhome = "$base/GKB/scripts/release/download_directory/$release";
mkdir $rhome unless -d $rhome;

# Make sure that ancillary data are up-to-date
#check_analysis_data($base);
#check_solr_data($base);

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

chomp(my $cwd = `pwd`);
$logger->info("My working directory is $cwd\n");

my @cmds = (
    qq(git clone https://github.com/reactome/Release.git),
    qq(mkdir reactome),
    qq(mkdir reactome/GKB),
    qq(cp -r $repo/website reactome/GKB),
    qq(cp -r $repo/modules reactome/GKB),
    qq(mkdir reactome/GKB/third_party_install),
    qq(cd $repo/third_party_install),
    qq(tar czvf config.tar.gz etc usr),
    qq(cd $cwd),
    qq(cp $repo/third_party_install/config.tar.gz reactome/GKB/third_party_install),
    qq(rm -f reactome/GKB/website/html/stats*),
    qq(cp $base/GKB/scripts/release/website_files_update/stats.* reactome/GKB/website/html),
    qq(cp -r $base/Solr reactome),
    qq(find reactome/Solr |grep 'tar.gz\|.tgz' |xargs rm -f),
    qq(cp -r $base/apache-tomcat* reactome),
    qq(rm -f reactome/apache-tomcat/webapps/*.war),
    qq(rm -fr $unwanted_webapps),
    qq(rm -f reactome/apache-tomcat/logs/*),
    qq(mkdir reactome/AnalysisService),
    qq(mkdir reactome/AnalysisService/temp),
    qq(mkdir reactome/AnalysisService/input),
    qq(rm -fr reactome/GKB/website/html/download/),
    qq(mkdir -p reactome/GKB/website/html/download/$release),
    qq(cp $repo/website/html/download/*html reactome/GKB/website/html/download/),
    qq(cp -r $rhome/fireworks reactome/GKB/website/html/download/$release),
    qq(cp -r $rhome/diagrams reactome/GKB/website/html/download/$release),
    qq(cp $base/AnalysisService/input/analysis.bin reactome/AnalysisService/input),
    qq(mkdir -p reactome/RESTful/temp),
    qq(rm -fr reactome/GKB/modules/*ensem*),
    qq(mkdir -p /tmp/diagrams_and_fireworks),
    qq(cp -a /usr/local/reactomes/Reactome/production/GKB/website/html/download/current/diagram /tmp/diagrams_and_fireworks),
    qq(cp -a /usr/local/reactomes/Reactome/production/GKB/website/html/download/current/fireworks /tmp/diagrams_and_fireworks),
    qq(tar -czf /usr/local/reactomes/Reactome/production/GKB/website/html/download/current/diagrams_and_fireworks.tgz /tmp/diagrams_and_fireworks && rm -rf /tmp/diagrams_and_fireworks),
    qq(cp /usr/local/reactomes/Reactome/production/ContentService/interactors.db /usr/local/reactomes/Reactome/production/GKB/website/html/download/current/interactors.db),
    qq(gzip /usr/local/reactomes/Reactome/production/GKB/website/html/download/current/interactors.db),
    qq(cp /usr/local/reactomes/Reactome/production/AnalysisService/input/analysis.bin /usr/local/reactomes/Reactome/production/GKB/website/html/download/current/analysis.bin),
    qq(gzip /usr/local/reactomes/Reactome/production/GKB/website/html/download/current/analysis.bin),
    qq(cp -a /usr/local/reactomes/Reactome/production/Solr/data /tmp/solr_data && rm /tmp/solr_data/data/reactome/data_conf.tgz),
    qq(tar -czf /usr/local/reactomes/Reactome/production/GKB/website/html/download/current/solr_data.tgz /tmp/solr_data/data && rm -rf /tmp/solr_data/data)
   );

my @cmds2 = (
    qq(tar czf ../reactome.tar.gz *),
    qq(cp ../reactome.tar.gz $rhome),
    qq(cp $repo/third_party_install/install_reactome.sh $rhome),
    qq(perl -i -pe 's/RELEASENUM/$release/' $rhome/install_reactome.sh)
);

for my $cmd (@cmds) {
    my $logger = get_logger(__PACKAGE__);
    $logger->info("$cmd\n");
    system($cmd) == 0 or $logger->error("Something wrong with '$cmd'\n");
    sleep 1;
}

#cleanse_solr_data();
chdir 'reactome';

$logger->info("CWD: ", `pwd`);

for my $cmd (@cmds2) {
    print "$cmd\n";
    system($cmd) == 0 or $logger->error_die("Something wrong with '$cmd'\n");
    sleep 1;
}

sub check_analysis_data {
    my $base = shift;

    my $logger = get_logger(__PACKAGE__);
    my $data = "$base/AnalysisService/input/analysis_v$release.bin";
    unless (-e $data) {
	$logger->error_die("$data does not exist!  Make sure that you update the AnalysisService data before proceeding\n");
    }
}

sub check_solr_data {
    my $base = shift;

    my $logger = get_logger(__PACKAGE__);
    my $data = "$base/Solr/cores/reactome_v$release.tar.gz";
    unless (-e $data) {
        $logger->error_die("$data does not exist!  Make sure that you update the Solr data before proceeding\n");
    }
}

sub cleanse_solr_data {
    my $logger = get_logger(__PACKAGE__);

    while (my $path = <Solr/cores/reactome_v*>) {
	if ($path !~ /reactome_v$release$/) {
	    $logger->info("Removing old release $path\n");
	    system "rm -fr $path";
	}
    }
}
