#!/usr/local/bin/perl  -w

# Updates the Reactome Mart, based on the configuration files in
# GKB/BioMart/reactome.  Note, running this script will cause the
# BioMart server to be restarted.
#
# -b			Explicitly specify the BioMart root directory
# -restart		Restart Apache server only, don't do any updates
# -f			Supresses the interactive mode, implicitly answering
#				all questions with "yes".
# -sudo			Runs the server restart commands using sudo.  You will
#				need to supply a password if you use this option.
# -biomart_version		Specify BioMart version number.  Defaults to 0.6.

use lib "$ENV{HOME}/bioperl-1.0";
use lib "$ENV{HOME}/GKB/modules";
use strict;
use Cwd;
use File::Copy;
use Getopt::Long;
use GKB::Config;
use GKB::FileUtils;
use GKB::BioMart::Server;

my $usage = "Usage: $0 -sock socket -user db_user -host db_host -pass db_pass -port db_port -db db_name -b biomart_root_dir -f -restart -biomart_version version_num -h\nOptions:\n-f\t\"Force\" - inhibits interactive questions\n-b\tExplicitly specify the BioMart root directory\n-restart\tRestart Apache server only\n-sudo\tRun restart under sudo\n-biomart_version\tSet version, e.g. 0.7; default is 0.6\n-h\tGet this help menu";

our($opt_sock,$opt_user,$opt_host,$opt_pass,$opt_port,$opt_db,$opt_debug,$opt_b,$opt_f,$opt_restart,$opt_sudo,$opt_biomart_version,$opt_h);

&GetOptions(
	"sock:s",
	"user:s",
	"host:s",
	"pass:s",
	"port:i",
	"db=s",
	"debug",
	"b=s",
	"f",
	"restart",
	"sudo",
	"biomart_version=s",
	"h",
);


if ($opt_h) {
	print STDERR "$usage\n";
	exit(0);
}

my $server = GKB::BioMart::Server->new();
$server->set_biomart_dir($opt_b);
my $bio_mart_dir = $server->get_biomart_dir();

if (!(defined $bio_mart_dir)) {
	print STDERR "$usage\n";
	exit(1);
}

if (!(defined $opt_biomart_version)) {
	$opt_biomart_version = "0.6";
}

my $biomart_reactome_dir = "$bio_mart_dir/reactome";

if (!(-e $biomart_reactome_dir)) {
	print STDERR "Cannot find BioMart Reactome directory $biomart_reactome_dir, giving up!!\n";
	print STDERR "$usage\n";
	exit(1);
}
	
if (!$opt_f) {
	check_bio_mart_dir($bio_mart_dir);

	my $answer;
	if (!$opt_restart) {
		print "Have you checked the files in $biomart_reactome_dir out of CVS [y/n]? ";
		$answer = <STDIN>;
		chop($answer);
		if ($answer eq 'n') {
			exit(0);
		}
	}

	print "This script will overwrite existing BioMart parameters irreversably and restart the server.\n";
	print "Do you wish to continue? [y/n]? ";
	$answer = <STDIN>;
	chop($answer);
	if ($answer eq 'n') {
		exit(0);
	}
}

$server->stop($opt_sudo);
my %variables = $server->environment();
for my $variable_name (keys(%variables)) {
	if ($variable_name eq "MYSQL_TCP_PORT" && !(defined $opt_port)) {
		$opt_port = $variables{$variable_name};
	}
	if ($variable_name eq "MYSQL_UNIX_PORT" && !(defined $opt_sock)) {
		$opt_sock = $variables{$variable_name};
	}
}
if (!$opt_restart) {
	mysql($biomart_reactome_dir);
	registry($bio_mart_dir, $biomart_reactome_dir);
	settings($bio_mart_dir, $biomart_reactome_dir);
	martview($bio_mart_dir, $biomart_reactome_dir);
	tt_files($bio_mart_dir, $biomart_reactome_dir);
	configure_server($bio_mart_dir);
}
$server->start();

print STDERR "$0 has completed successfully\n";

# Checks that the BioMart home directory contains the
# expected things
sub check_bio_mart_dir {
    my($bio_mart_dir) = @_;

	my $apache_dir = "$bio_mart_dir/apache";
	my $biomart_perl_dir = "$bio_mart_dir/biomart-perl";
	my $martj_dir = "$bio_mart_dir/martj-$opt_biomart_version";
	
	if (!(-e $apache_dir)) {
		print STDERR "Directory $apache_dir is missing!!\n";
		print STDERR "You need to install Apache - see www.biomart.org for details.\n";
		exit(1);
	}
	if (!(-e $biomart_perl_dir)) {
		print STDERR "Directory $biomart_perl_dir is missing!!\n";
		print STDERR "You need to install BioMart Perl - see www.biomart.org for details.\n";
		exit(1);
	}
	if (!(-e $martj_dir)) {
		print STDERR "Directory $martj_dir is missing!!\n";
		print STDERR "You need to install martj - see www.biomart.org for details.\n";
		exit(1);
	}
}

sub mysql {
    my($biomart_reactome_dir) = @_;

	# Build mysql command
	if (!$opt_db) {
		if ($MART_DB_NAME) {
			$opt_db = $MART_DB_NAME;
		} else {
			$opt_db = "test_reactome_mart"; # might work
		}
	}
	my $mysql_options = "";
	if ($opt_user) {
		$mysql_options .= " -u $opt_user";
	}
	if ($opt_host) {
		$mysql_options .= " -h $opt_host";
	}
	if ($opt_pass) {
		$mysql_options .= " -p$opt_pass";
	}
	if ($opt_port) {
		$mysql_options .= " -P $opt_port";
	}
	if ($opt_sock) {
		$mysql_options .= " --socket $opt_sock";
	}
	$mysql_options .= " $opt_db";
	my $mysql_command = "mysql $mysql_options";
	my $mysqldump_command = "mysqldump --opt $mysql_options";
	my $filename = "$biomart_reactome_dir/biomart_meta_tables.dump";
	if (-e $filename) {
		# Make backup dump of old tables, just in case
		my $meta_tables = "meta_conf__dataset__main meta_conf__interface__dm meta_conf__user__dm meta_conf__xml__dm meta_template__template__main meta_template__xml__dm meta_version__version__main";
		$mysqldump_command .= " $meta_tables";
		system("$mysqldump_command > $filename.old");
		
		# Install new BioMart meta tables
		system("cat $filename | $mysql_command");
	} else {
		print STDERR "Not able to locate mysqldump file: $filename, aborting!\n";
		exit(1);
	}
}

sub registry {
    my($bio_mart_dir, $biomart_reactome_dir) = @_;

	my $filename = "$biomart_reactome_dir/reactome.xml";
	my $source_filename = "$bio_mart_dir/biomart-perl/conf/reactome.xml";
	safe_copy($filename, $source_filename);
}

sub settings {
    my($bio_mart_dir, $biomart_reactome_dir) = @_;

	my $filename = "$biomart_reactome_dir/settings.conf";
	my $source_filename = "$bio_mart_dir/biomart-perl/conf/settings.conf";
	safe_copy($filename, $source_filename);
}

sub martview {
    my($bio_mart_dir, $biomart_reactome_dir) = @_;

	if ($opt_biomart_version < 0.7) {
		return;
	}
	
	my $filename = "$biomart_reactome_dir/martview.css";
	my $source_filename = "$bio_mart_dir/biomart-perl/conf/martview.css";
	safe_copy($filename, $source_filename);
}

# Copy the BioMart page structure definition files from .../GKB/BioMart/reactome
# to the BioMart default directory.
sub tt_files {
    my($bio_mart_dir, $biomart_reactome_dir) = @_;

	safe_copy_tt($bio_mart_dir, $biomart_reactome_dir, "footer.tt");
	safe_copy_tt($bio_mart_dir, $biomart_reactome_dir, "header.tt");
	safe_copy_tt($bio_mart_dir, $biomart_reactome_dir, "main.tt");
	if ($opt_biomart_version eq "0.6") {
		safe_copy_tt($bio_mart_dir, $biomart_reactome_dir, "summarypanel.tt");
	}
	if ($opt_biomart_version eq "0.5") {
		safe_copy_tt($bio_mart_dir, $biomart_reactome_dir, "biomart.tt");
	} else {
		if (!($opt_biomart_version eq "0.7")) {
			# Nelson's modified version of this file isn't yet working 100%, so
			# keep original in BioMart 0.7.
			safe_copy_tt($bio_mart_dir, $biomart_reactome_dir, "menupanel.tt");
		}
	}
#	if ($opt_biomart_version eq "0.7") {
#		# Nelson has made a couple of small changes to this source file
#		safe_copy_lib_biomart($bio_mart_dir, $biomart_reactome_dir, "Configurator.pm");
#	}
}

sub safe_copy_tt {
    my($bio_mart_dir, $biomart_reactome_dir, $filename) = @_;

	my $source_filename = "$biomart_reactome_dir/tt/$opt_biomart_version/$filename";
	my $target_filename = "$bio_mart_dir/biomart-perl/conf/templates/default/$filename";
	safe_copy($source_filename, $target_filename);
}

sub safe_copy_lib_biomart {
    my($bio_mart_dir, $biomart_reactome_dir, $filename) = @_;

	my $source_filename = "$biomart_reactome_dir/tt/$opt_biomart_version/$filename";
	my $target_filename = "$bio_mart_dir/biomart-perl/lib/BioMart/$filename";
	safe_copy($source_filename, $target_filename);
}

# Makes a .bak copy of the target file *before* it is overwritten
# by the source file.
sub safe_copy {
    my($source_filename, $filename) = @_;
    
	# backup target file
	my $old_filename = "$filename.bak";
	if (-e $filename) {
		copy($filename, $old_filename);
	}
	
	copy($source_filename, $filename);
}

# Install new parameters
sub configure_server {
    my($bio_mart_dir) = @_;
    
	print "HTTP server location: $bio_mart_dir/apache/bin/httpd\n";
    my $biomart_perl_dir = "$bio_mart_dir/biomart-perl";
	my $command = "(cd $biomart_perl_dir; perl bin/configure.pl --clean -r $biomart_perl_dir/conf/reactome.xml)";
	print STDERR "update_reactome_mart.configure_server: command=$command\n";
	system($command);
}	
