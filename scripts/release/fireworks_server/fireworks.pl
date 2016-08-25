#!/usr/local/bin/perl  -w
use strict;

use lib '/usr/local/gkb/modules';

use GKB::Config;

use autodie qw/:all/;
use Cwd;
use File::Copy;
use Getopt::Long;
use common::sense;

use Log::Log4perl qw/get_logger/;

run(@ARGV) unless caller();

sub run {
    Log::Log4perl->init(\$LOG_CONF);
    my $logger = get_logger(__PACKAGE__);

    $GKB::Config::NO_SCHEMA_VALIDITY_CHECK = undef;

    our($opt_host,$opt_db,$opt_pass,$opt_port,$opt_debug,$opt_user,$opt_r);

    my $usage = "Usage: $0 -db db_name -user db_user -host db_host -pass db_pass -port db_port -r release_number";

    &GetOptions("user:s",
    "host:s",
    "pass:s",
    "port:i",
    "debug",
    "db=s",
    "r:i");

    $opt_db || die $usage;
    $opt_r || die $usage;
    	
    $opt_host ||= $GK_DB_HOST;
    $opt_user ||= $GK_DB_USER;
    $opt_pass ||= $GK_DB_PASS;
    $opt_port ||= $GK_DB_PORT;

    my $tmp_dir = "/tmp";
    clone_fireworks_repository_from_github($tmp_dir);
	
	my $link_name = 'fireworks';
    create_symbolic_link_to_fireworks_repository($tmp_dir, $link_name);
	
	my $fireworks_jar_file = 'fireworks.jar';
	create_fireworks_jar_file($fireworks_jar_file, $link_name);

    my $fireworks_package = "java -jar -Xms5120M -Xmx10240M $fireworks_jar_file";
    my $credentials = "-d $opt_db -u $opt_user -p $opt_pass";
    my $reactome_graph_binary = "ReactomeGraphs.bin";
    create_reactome_graph_binary_file($fireworks_package, $credentials, $opt_r, $reactome_graph_binary);

    my $json_dir = "json";
    create_fireworks_json($fireworks_package, $credentials, $reactome_graph_binary, $json_dir);
    
    remove_fireworks_repository($tmp_dir);
    
    $logger->info("$0 has finished its job\n");
}

sub clone_fireworks_repository_from_github {
    my $directory = shift;
    
    my $present_dir = getcwd();
    chdir $directory;
    system "rm -fr Fireworks" if -d "Fireworks";
    my $return_value = system("git clone https://github.com/reactome/Fireworks");
    chdir $present_dir;
    
    return ($return_value == 0);
}

sub create_symbolic_link_to_fireworks_repository {
    my $directory = shift;
	my $link_name = shift;
	
	return (system("rm -rf $link_name; ln -s $directory/Fireworks $link_name") == 0);
}

sub create_fireworks_jar_file {
	my $jar_file = shift;
	my $symbolic_link_to_repository = shift;
	
	my $present_dir = getcwd();
	chdir "$symbolic_link_to_repository/Server";
	system("mvn clean package -U");
	my $return_value = copy("target/fireworks-jar-with-dependencies.jar", "$present_dir/$jar_file");
	chdir $present_dir;
	
	return ($return_value == 0);
}

sub create_reactome_graph_binary_file {
    my $fireworks_jar = shift;
    my $credentials = shift;
    my $reactome_version = shift;
    my $reactome_graph_binary = shift;
    
    return (system("$fireworks_jar GRAPH -s ../analysis_core/analysis_v$reactome_version.bin -o $reactome_graph_binary --verbose") == 0);
}

sub create_fireworks_json {
    my $fireworks_jar = shift;
    my $credentials = shift;
    my $reactome_graph_binary = shift;
    my $json_dir = shift;
    
    system("mkdir -p $json_dir");
    return (system("$fireworks_jar LAYOUT $credentials -g $reactome_graph_binary -f fireworks/Server/config -o $json_dir") == 0);
}

sub remove_fireworks_repository {
    my $directory = shift;
    
    chdir $directory;
    return (system("rm -rf Fireworks") == 0);
}
