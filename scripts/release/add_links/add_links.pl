#!/usr/local/bin/perl  -w
use strict;

# Given, as a minimum, a database name, this script will
# insert links from Reactome to other databases, such
# as UniProt.  These will appear as hyperlinks on
# displayed web pages.

use lib '/usr/local/gkb/modules';
use Getopt::Long;
use GKB::Config;
use Data::Dumper;
use Cwd;

use Log::Log4perl qw/get_logger/;
Log::Log4perl->init(\$LOG_CONF);
my $logger = get_logger(__PACKAGE__);

use constant EXE  => './add_links_to_single_resource.pl';
use constant LOG  => 'logs';
use constant RLOG => 'resource_log.txt';

our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db,$opt_debug,$opt_edb,$opt_db_ids);
my $pid;

# Parse commandline
my $usage = "Usage: $0 -user db_user -host db_host -pass db_pass -port db_port -db db_name -edb ENSEMBL_db -db_ids 'id1,id2,..'\n";
&GetOptions("user:s", "host:s", "pass:s", "port:i", "db=s", "debug", "test", "db_ids:s");
$opt_db || $logger->error_die($usage);


my $gk_root_dir = $GK_ROOT_DIR;
my $pwd = &Cwd::cwd();

unless ($pwd =~ m!/scripts/release/add_links$! && -d '../../../modules') {
    $logger->error("Current working directory is $pwd:");
    $logger->error_die("Please run this script from $gk_root_dir/scripts/release/add_links");
}

my $exe = EXE;

if (!(defined $opt_host) || $opt_host eq '') {
    $opt_host = $GK_DB_HOST;
}
if (!(defined $opt_user) || $opt_user eq '') {
    $opt_user = $GK_DB_USER;
}
if (!(defined $opt_pass) || $opt_pass eq '') {
    $opt_pass = $GK_DB_PASS;
}
if (!(defined $opt_port) || $opt_port eq '') {
    $opt_port = $GK_DB_PORT;
}

# Pre-create the command line options associated with database access
my $reactome_db_options = "-db $opt_db";
if (defined $opt_host && !($opt_host eq '')) {
    $reactome_db_options .= " -host $opt_host";
}
if (defined $opt_user && !($opt_user eq '')) {
    $reactome_db_options .= " -user $opt_user";
}
if (defined $opt_pass && !($opt_pass eq '')) {
    # Put a backslash in front of characters that have special meaning to the shell
    my $pass = $opt_pass;
    if ($pass =~ /\$/) {
	$pass =~ s/\$/\\\$/g;
    }
    $reactome_db_options .= " -pass $pass";
}
if (defined $opt_port && !($opt_port eq '')) {
    $reactome_db_options .= " -port $opt_port";
}
if (defined $opt_edb && !($opt_edb eq '')) {
    $reactome_db_options .= " -edb $opt_edb";
}

my @resources;


# use an external config file for linkers -- easier to debug
# and hard-coding data into scripts is not good practice anyway
open CONF, "./add_links.conf" or $logger->error_die("Could not open resource config $!");
while (<CONF>) {
    next if /^#/; #skips comments
    chomp;
    push @resources, $_;
}

my $resource;
my $cmd;

unlink RLOG if -e RLOG;
mkdir LOG unless -d LOG;

foreach $resource (@resources) {
    if (!(defined $resource) || $resource eq '') {
    	$logger->warn("missing resource value!\n");
    	next;
    }

    run($exe, "$reactome_db_options -res $resource", $resource);
}

my (@failed, @passed);
if (-e RLOG) {
    open IN, RLOG or die $!;
    while (<IN>) { 
	my ($resource) = split;
	push @passed, $resource if /PASSED/;
	push @failed, $resource if /FAILED/;
    }
}

if (@failed) {
    my $failed = @failed;
    $logger->error("$failed linkers failed to run to completion, please check the diagnostic output!\n");
    my %failed = map { $_ => 1 } @failed;
    $logger->error("The following resources failed: " . join("\t", (sort keys %failed)));
}

if (@passed) {
    my %passed = map { $_ => 1 } @passed;
    $logger->info("The following resources passed: " . join("\t", (sort keys %passed)));
}

$logger->info("$0 has finished its job\n");

sub run {
    my $exe  = shift;
    my $args = shift;
    my $resource = shift;

    my $logger = get_logger(__PACKAGE__);
    $logger->info("Running $exe $args\n");
    _run($exe,$resource,$args);
}

sub _run {
    my ($exe,$resource,$args) = @_;

    my $logger = get_logger(__PACKAGE__);
    chomp(my $timestamp = `date`);
    $logger->info("$timestamp Starting $args\n");

    my $retval = system "$exe $args > ".LOG."/$resource.out 2>&1";

    chomp($timestamp = `date`);
    open LOGFILE, ">>" . RLOG or die $!;
    my $state = $retval ? 'FAILED' : 'PASSED';
    print LOGFILE "$resource $state $retval $timestamp\n";
    close LOGFILE;
}

