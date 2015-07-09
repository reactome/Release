#!/usr/local/bin/perl
package SearchIndexer;
use strict;
use warnings;
use common::sense;

use lib "/usr/local/gkb/modules";

use GKB::Config;
use autodie;
use Cwd;
use Getopt::Long;
use Log::Log4perl qw/get_logger/;
use constant CONF => '/usr/local/reactomes/Reactome/development/apache-tomcat/webapps/solr/WEB-INF/web.xml';

__PACKAGE__->run(@ARGV) unless caller();

sub run {
    Log::Log4perl->init(\$LOG_CONF);
    my $logger = get_logger(__PACKAGE__);
    
    $GKB::Config::NO_SCHEMA_VALIDITY_CHECK = undef;
    
    our($opt_host,$opt_db,$opt_pass,$opt_port,$opt_debug,$opt_user,$opt_r);
    
    # We need maven 3, die otherwise
    $logger->error_die("No maven3 installed, sorry I can't run") unless `which mvn3`;
    
    my $usage = "Usage: $0 -db db_name -user db_user -host db_host -pass db_pass -port db_port -r release_number";
    
    &GetOptions("user:s",
    "host:s",
    "pass:s",
    "port:i",
    "debug",
    "db=s",
    "r:i");
    
    $opt_db || die $usage;
    $opt_r  || die $usage;
    	
    $opt_host ||= $GKB::Config::GK_DB_HOST;
    $opt_user ||= $GKB::Config::GK_DB_USER;
    $opt_pass ||= $GKB::Config::GK_DB_PASS;
    $opt_port ||= $GKB::Config::GK_DB_PORT;
    
    my $solr_url  = "http://reactomerelease.oicr.on.ca:7080/solr/reactome";
    my $solr_dir  = '/usr/local/reactomes/Reactome/production/Solr/cores';
    
    # No point, they don't work sjm 2015-04-07
    # my $solr_user = $GKB::Config::GK_SOLR_USER;
    # my $solr_pass = $GKB::Config::GK_SOLR_PASS;
    
    disable_authentication();
    
    my $repository_path = git_clone_search_repository('/tmp');
    my $link = 'search';
    make_symbolic_link($repository_path, $link);
    my $jar_path = build_search_jar("$link/indexer");
    my $args = "-d $opt_db -u $opt_user -p $opt_pass -s $solr_url -c src/main/resourcse/controlledvocabulary.csv -r $opt_r";
    create_gzipped_ebeye_xml($jar_path, $args, 'ebeye.xml');
    archive_solr_core_version($solr_dir, $opt_r);
    remove_cloned_repository($repository_path);
    remove_symbolic_link($link);

    enable_authentication();

    $logger->info("$0 has finished its job\n");
}

sub disable_authentication {
    my $logger = get_logger(__PACKAGE__);
    
    my $conf = CONF;
    my $disabled = "$conf.security-disabled";
    my $retval = system "cp $disabled $conf";
    $logger->error_die("problem with $conf") if $retval;
    sleep 60;
}

sub enable_authentication {
    my $logger = get_logger(__PACKAGE__);
    
    my $conf = CONF;
    my $enabled = "$conf.security-enabled";
    my $retval = system "cp $enabled $conf";
    $logger->error_die("problem with $conf") if $retval;
}

sub git_clone_search_repository {
    my $dir = shift;
    my $current_dir = getcwd();
    
    chdir $dir;
    system("git clone https://github.com/reactome/Search.git");
    chdir $current_dir;
    
    return "$dir/Search";
}

sub make_symbolic_link {
    my $target = shift;
    my $link = shift;
    
    system("ln -sf $target $link");
}

sub build_search_jar {
    my $dir = shift;
    my $current_dir = getcwd();
    
    my $logger = get_logger(__PACKAGE__);
    
    chdir $dir;
    $logger->info("I am updating and compiling the indexer now...");
    system("mvn clean package");
    system("ln -sf target/Indexer-1.0-jar-with-dependencies.jar indexer.jar");
    chdir $current_dir;
    
    return "$dir/indexer.jar";
}

sub create_gzipped_ebeye_xml {
    my $jar_path = shift;
    my $args = shift;
    my $output = shift;
    
    my $logger = get_logger(__PACKAGE__);
    
    $logger->info("I am indexing now...");
    system("java -jar -Xms5120M -Xmx10240M $jar_path $args -o $output");
    system("gzip -f $output");
}

sub archive_solr_core_version {
    my $solr_dir = shift;
    my $version = shift;
    my $current_dir = getcwd();
    
    chdir $solr_dir;
    system "tar czvf reactome_v$version.tar.gz reactome";
    chdir $current_dir;
}

sub remove_cloned_repository {
    my $repository_path = shift;
    
    my $logger = get_logger(__PACKAGE__);
    
    unless (-d "$repository_path/.git") {
	$logger->error("$repository_path is not a git repo -- can't remove");
	return;
    }
    
    return (system("rm -rf $repository_path") == 0);
}

sub remove_symbolic_link {
    my $link = shift;
    
    my $logger = get_logger(__PACKAGE__);
    
    unless (-l $link) {
	$logger->error("$link is not a symbolic link -- can't remove");
	return;
    }
    
    return (system("rm -r $link") == 0);
}

__END__