#!/usr/local/bin/perl -w
use strict;

my $dir_for_tarball = 'reactome_tarball';
my $release = shift or die "Usage $0 release_num\n";

mkdir $dir_for_tarball unless (-e $dir_for_tarball);
chdir $dir_for_tarball;

mkdir $release unless -d $release;
chdir $release;

system "rm -fr reactome" if -d 'reactome';

# Since the github repo is not public, we will assume it is up-to-date
# on reactomerelease
chomp (my $bakdir = `pwd`);
$bakdir .= '/../../../../..';

my @cmds = (
    qq(mkdir reactome),
    qq(cp -r $bakdir/website reactome),
    qq(cp -r $bakdir/modules reactome),
    qq(cp -r $bakdir/third_party_install/tomcat reactome),
    qq(cp $bakdir/third_party_install/website/conf/httpd.conf reactome/website/conf),
    qq(rm -f reactome/website/html/stats*),
    qq(cp $bakdir/scripts/release/website_files_update/stats.* reactome/website/html),
    qq(mkdir reactome/AnalysisService),
    qq(mkdir reactome/AnalysisService/input),
    qq(mkdir reactome/AnalysisService/temp),
    qq(cp $bakdir/../AnalysisService/input/analysis_v$release.bin reactome/AnalysisService/input),
    qq(rm -fr reactome/website/html/img-tmp/*),
    qq(rm -fr reactome/website/html/img-fp/*),
    qq(rm -fr reactome/website/logs/*),
    qq(rm -fr reactome/website/html/download/*),
    qq(rm -fr reactome/modules/*ensem*),
    qq(find ./ -name .gitignore | xargs rm -f),
    qq(perl -i -pe "s/GK_DB_USER = '\\S+'/GK_DB_USER = 'reactome_user'/" reactome/modules/GKB/Config.pm),
    qq(perl -i -pe "s/GK_DB_PASS = '\\S+'/GK_DB_PASS = 'reactome_pass'/" reactome/modules/GKB/Config.pm),
    qq(perl -i -pe "s/GK_DB_NAME = '\\S+'/GK_DB_NAME = 'gk_current'/" reactome/modules/GKB/Config.pm),
    qq(perl -i -pe "s/GK_IDB_NAME = '\\S+'/GK_IDB_NAME = 'gk_stable_ids'/" reactome/modules/GKB/Config.pm),
    qq(perl -i -pe "s/GK_ROOT_DIR = '\\S+'/GK_ROOT_DIR = '\\/usr\\/local\\/gkb'/" reactome/modules/GKB/Config.pm),
    qq(perl -i -pe "s/'DB_USER', '\\S+'/'DB_USER', 'reactome_user'/" reactome/website/html/wordpress/wp-config.php),
    qq(perl -i -pe "s/'DB_PASSWORD', '\\S+'/'DB_PASSWORD', 'reactome_pass'/" reactome/website/html/wordpress/wp-config.php),
    qq(perl -i -pe "s/'DB_NAME', '\\S+'/'DB_NAME', 'gk_wordpress'/" reactome/website/html/wordpress/wp-config.php),
    qq(tar czvf reactome.tar.gz reactome)
);


for my $cmd (@cmds) {
    print "$cmd\n";
    system($cmd) == 0 or die "Something wrong with '$cmd'\n";
    sleep 1;
}



