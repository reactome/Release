#!/usr/local/bin/perl  -w
use strict;

# I think it's more user-friendly if the user will not have to practise any
# symlink tricks. Hence the BEGIN block. If the location of the script or
# libraries changes, this will have to be changed.

BEGIN {
    my @a = split('/',$0);
    pop @a;
    push @a, ('..','..','..');
    my $libpath = join('/', @a);
    unshift (@INC, "$libpath/modules");
    $ENV{PATH} = "$libpath/scripts:$libpath/scripts/release:" . $ENV{PATH};
}

use GKB::Config;

use autodie;
use Cwd;
use Getopt::Long;
use Net::FTP;
use common::sense;

use Log::Log4perl qw/get_logger/;
Log::Log4perl->init(\$LOG_CONF);
my $logger = get_logger(__PACKAGE__);

our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db,$opt_debug,$opt_db_ids,$opt_res);

# Parse commandline
my $usage = "Usage: $0 -user db_user -host db_host -pass db_pass -port db_port -db db_name\n";
&GetOptions("user:s", "host:s", "pass:s", "port:i", "db=s");
$opt_db || die $usage;

$opt_user ||= $GKB::Config::GK_DB_USER;
$opt_pass ||= $GKB::Config::GK_DB_PASS;
$opt_port ||= $GKB::Config::GK_DB_PORT;
$opt_host ||= $GKB::Config::GK_DB_HOST;

my $present_dir = getcwd();
my $biomodels_repository = 'scripts/release/biomodels/repository';

chdir "$GK_ROOT_DIR";
run("
    git stash
    git subtree pull --prefix $biomodels_repository https://github.com/reactome/Models2Pathways.git master --squash
    git stash pop
    ");

my $url = 'ftp.ebi.ac.uk';
my $ftp = Net::FTP->new($url, Passive => 1) or $logger->error_die("Could not create FTP object for $url");
$ftp->login() or $logger->error_die("Could not login to $url: " . $ftp->message);
my $dir = 'pub/databases/biomodels/releases/latest';
$ftp->cwd($dir) or $logger->error_die("Could not change directory to $dir");
my ($xml_tarball) = grep /sbml_files.tar.bz2/, $ftp->ls();
$xml_tarball =~ s/.tar.bz2//;
chdir "$present_dir";
unless (-d $xml_tarball) {
    run("rm -rf BioModels_Database*");
    run("wget ftp://ftp.ebi.ac.uk/pub/databases/biomodels/releases/latest/$xml_tarball.tar.bz2");
    run("tar xvfj $xml_tarball.tar.bz2");
}

chdir "$GK_ROOT_DIR/$biomodels_repository";
run("mvn clean package");
run("mv target/Models2Pathways-1.0-jar-with-dependencies.jar models2pathways.jar");

my $execute_jar = "java -jar -Xms5120M -Xmx10240M models2pathways.jar";
run("$execute_jar -o $present_dir/ -r /usr/local/reactomes/Reactome/production/AnalysisService/input/analysis.bin -b $present_dir/$xml_tarball/curated");

chdir "$present_dir";
run("perl add_links_to_single_resource.pl -user $opt_user -pass $opt_pass -host $opt_host -port $opt_port -db $opt_db -res BioModelsEventToDatabaseIdentifier");

$logger->info("$0 has finished its job\n");

# Runs command and ends program by default if non-zero exit status
sub run {
    my $command = shift;
    my $warn_only = shift;
    
    my $logger = get_logger(__PACKAGE__);
    
    my $exit_code = system($command);
    
    unless ($exit_code == 0) {
	$warn_only ?
	$logger->logcarp("$command failed") :
	$logger->logcroak("$command failed");	
    }
}