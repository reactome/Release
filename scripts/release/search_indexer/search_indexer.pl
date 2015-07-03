#!/usr/local/bin/perl  -w
use strict;
use common::sense;

use lib "/usr/local/gkb/modules";

use GKB::Config;

use autodie;
use Cwd;
use Getopt::Long;

use Log::Log4perl qw/get_logger/;

use constant CONF => '/usr/local/reactomes/Reactome/development/apache-tomcat/webapps/solr/WEB-INF/web.xml';

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


my $present_dir = getcwd();
my $output = "$present_dir/ebeye.xml";
my $prev_release = $opt_r - 1;

disable_authentication();

chdir $GK_ROOT_DIR;
system("git stash; git subtree pull --prefix scripts/release/search_indexer/search --squash search master; git stash pop");

# then index.  This actually uses tomcat/solr to write to the filesystem!
$logger->info("I am updating and compiling the indexer now...");
chdir "$present_dir/search/indexer";
system("mvn clean package");
system("ln -sf target/Indexer-1.0-jar-with-dependencies.jar indexer.jar");
$logger->info("I am indexing now...");
system("java -jar -Xms5120M -Xmx10240M indexer.jar -d $opt_db -u $opt_user ".
       "-p $opt_pass -s $solr_url ".#-e $solr_user -a $solr_pass ".
       "-c src/main/resources/controlledvocabulary.csv -o $output -r $opt_r");
system("gzip -f $output");

chdir $solr_dir;
system "tar czvf reactome_v$opt_r.tar.gz reactome";

enable_authentication();

$logger->info("$0 has finished its job\n");


sub disable_authentication {
    my $conf = CONF;
    my $disabled = "$conf.security-disabled";
    my $retval = system "cp $disabled $conf";
    die "problem with $conf" if $retval;
    sleep 60;
}

sub enable_authentication {
    my $conf = CONF;
    my $enabled = "$conf.security-enabled";
    my $retval = system "cp $enabled $conf";
    die "problem with $conf" if $retval;
}
