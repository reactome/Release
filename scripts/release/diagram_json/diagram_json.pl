#!/usr/local/bin/perl  -w
use strict;

use lib '/usr/local/gkb/modules';

use GKB::Config;

use autodie qw/:all/;
use Cwd;
use File::Copy;
use File::Path qw/make_path/;
use Getopt::Long;
use Try::Tiny;

use Log::Log4perl qw/get_logger/;

run(@ARGV) unless caller();

sub run {
    Log::Log4perl->init(\$LOG_CONF);
    my $logger = get_logger(__PACKAGE__);

    our($opt_user, $opt_pass, $opt_host, $opt_port, $opt_db, $opt_version);
    my $usage = "Usage: $0 -user db_user -pass db_pass -host db_host -port db_port -db db_name -version reactome_version";

    &GetOptions("user:s", "pass:s", "host:s", "port:i", "db:s", "version:i");

    $opt_db || die $usage;
    ($opt_version) = $opt_db =~ /_(\d+)$/ unless $opt_version;
    $opt_version || die $usage;
    
    $opt_user ||= $GK_DB_USER;
    $opt_pass ||= $GK_DB_PASS;
    $opt_host ||= $GK_DB_HOST;
    $opt_port ||= $GK_DB_PORT;

    my $tmp_dir = "/tmp";
    clone_diagram_core_repository_from_github($tmp_dir);
	
	my $link_name = 'diagram-core';
    create_symbolic_link_to_diagram_core_repository($tmp_dir, $link_name);
    
	my $diagram_core_jar_file = 'diagram-core.jar';
    create_diagram_core_jar_file($diagram_core_jar_file, $link_name);

    my $output_directory = 'diagram';
    my $options = "-h $opt_host -d $opt_db -u $opt_user -p $opt_pass -o $output_directory -r diagram-core/src/main/resources/trivialchemicals.txt";
    make_path($output_directory, { mode => 0775 });
    execute_diagram_core_jar_file($diagram_core_jar_file, $options);

    remove_symbolic_link_to_diagram_core_repository($link_name);
    remove_diagram_core_repository($tmp_dir);
    
    my $current_download_directory = "/usr/local/gkb/website/html/download/$opt_version";
    move_output_directory_to_download_directory($output_directory, $current_download_directory);

    $logger->info("$0 has finished its job\n");
}

sub clone_diagram_core_repository_from_github {
    my $directory = shift;
    
    my $present_dir = getcwd();
    chdir $directory;
    system "rm -fr diagram-core" if -d "diagram-core";
    my $return_value = system("git clone https://github.com/reactome-pwp/diagram-core");
    chdir $present_dir;
    
    return ($return_value == 0);
}

sub create_symbolic_link_to_diagram_core_repository {
    my $directory = shift;
	my $link_name = shift;
	
    remove_symbolic_link_to_diagram_core_repository($link_name);
	return (system("ln -sf $directory/diagram-core $link_name") == 0);
}

sub create_diagram_core_jar_file {
	my $jar_file = shift;
	my $symbolic_link_to_repository = shift;
	
    my $logger = get_logger(__PACKAGE__);
    
	my $present_dir = getcwd();
	chdir "$symbolic_link_to_repository";
    my $return_value = 0;
    try {
        system("mvn clean package");
        $return_value = copy("target/tools-jar-with-dependencies.jar", "$present_dir/$jar_file");
    } catch {
        $logger->warn("Unable to compile diagram-core.jar file.  Using pre-compiled diagram-core.jar file.");
    };
    chdir $present_dir;
	
	return ($return_value == 0);
}

sub execute_diagram_core_jar_file {
    my $diagram_core_jar_file = shift;
    my $options = shift;
    
    return (system("java -jar $diagram_core_jar_file Convert $options") == 0);
}

sub remove_symbolic_link_to_diagram_core_repository {
    my $link_name = shift;
    
    return unlink $link_name;
}
sub remove_diagram_core_repository {
    my $directory = shift;
    
    chdir $directory;
    return (system("rm -rf diagram-core") == 0);
}

sub move_output_directory_to_download_directory {
    my $output_directory = shift;
    my $current_download_directory = shift;
    
    make_path($current_download_directory, { mode => 0775 });
    system("ln -sf $current_download_directory current");
    system("mv --backup=existing $output_directory $current_download_directory");
}