#!/usr/local/bin/perl  -w
use strict;

use lib '/usr/local/gkb/modules';

use GKB::Config;

use autodie qw/:all/;
use Cwd;
use File::Copy;
use Getopt::Long;
use Net::FTP;

use Log::Log4perl qw/get_logger/;

run(@ARGV) unless caller();

sub run {
    Log::Log4perl->init(\$LOG_CONF);
    my $logger = get_logger(__PACKAGE__);

    $GKB::Config::NO_SCHEMA_VALIDITY_CHECK = undef;

    our($opt_user, $opt_pass, $opt_host, $opt_port, $opt_db);
    my $usage = "Usage: $0 -user db_user -pass db_pass -host db_host -port db_port -db db_name";

    &GetOptions("user:s", "pass:s", "host:s", "port:i", "db:s");

    $opt_db || die $usage;
    
    $opt_user ||= $GK_DB_USER;
    $opt_pass ||= $GK_DB_PASS;
    $opt_host ||= $GK_DB_HOST;
    $opt_port ||= $GK_DB_PORT;

    my $present_dir = getcwd();
    
    my $tmp_dir = "/tmp";
    clone_biomodels_repository_from_github($tmp_dir);
	
	my $link_name = 'biomodels';
    create_symbolic_link_to_biomodels_repository($tmp_dir, $link_name);
	
	my $biomodels_jar_file = 'biomodels.jar';
    create_biomodels_jar_file($biomodels_jar_file, $link_name);

    my $xml_tarball = download_biomodels_package();
    execute_biomodels_jar_file($biomodels_jar_file, $xml_tarball);

    remove_biomodels_repository($tmp_dir);

    chdir "$present_dir";
    system("perl add_links_to_single_resource.pl -user $opt_user -pass $opt_pass -host $opt_host -port $opt_port -db $opt_db -res BioModelsEventToDatabaseIdentifier");

    $logger->info("$0 has finished its job\n");
}

sub clone_biomodels_repository_from_github {
    my $directory = shift;
    
    my $present_dir = getcwd();
    chdir $directory;
    system "rm -fr Models2Pathways" if -d "Models2Pathways";
    my $return_value = system("git clone https://github.com/reactome/biomodels-mapper.git");
    chdir $present_dir;
    
    return ($return_value == 0);
}

sub create_symbolic_link_to_biomodels_repository {
    my $directory = shift;
	my $link_name = shift;
	
	return (system("rm -rf $link_name; ln -s $directory/Models2Pathways $link_name") == 0);
}

sub create_biomodels_jar_file {
	my $jar_file = shift;
	my $symbolic_link_to_repository = shift;
	
    my $logger = get_logger(__PACKAGE__);
    
	my $present_dir = getcwd();
	chdir "$symbolic_link_to_repository";
    system("mvn clean package");
    my $return_value = copy("target/Models2Pathways-1.0-jar-with-dependencies.jar", "$present_dir/$jar_file");
    chdir $present_dir;
	
	return ($return_value == 0);
}

sub download_biomodels_package {    
    my $logger = get_logger(__PACKAGE__);
    
    my $url = 'ftp.ebi.ac.uk';
    my $ftp = Net::FTP->new($url, Passive => 0) or $logger->error_die("Could not create FTP object for $url");
    $ftp->login() or $logger->error_die("Could not login to $url: " . $ftp->message);
    my $dir = 'pub/databases/biomodels/releases/latest';
    $ftp->cwd($dir) or $logger->error_die("Could not change directory to $dir");
    my ($xml_tarball) = grep /sbml_files.tar.bz2/, $ftp->ls();
    $xml_tarball =~ s/.tar.bz2//;
    unless (-d $xml_tarball) {
        system("rm -rf BioModels_Database*");
        system("wget --no-passive ftp://ftp.ebi.ac.uk/pub/databases/biomodels/releases/latest/$xml_tarball.tar.bz2");
        system("tar xvfj $xml_tarball.tar.bz2");
    }
    
    return $xml_tarball;
}

sub execute_biomodels_jar_file {
    my $biomodels_jar_file = shift;
    my $xml_tarball = shift;
    
    my $present_dir = getcwd();
    
    system("java -jar -Xms5120M -Xmx10240M $biomodels_jar_file -o $present_dir/ -r /usr/local/reactomes/Reactome/production/AnalysisService/input/analysis.bin -b $xml_tarball/curated");
}

sub remove_biomodels_repository {
    my $directory = shift;
    
    chdir $directory;
    return (system("rm -rf Models2Pathways") == 0);
}
