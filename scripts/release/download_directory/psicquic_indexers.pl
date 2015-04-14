#!/usr/local/bin/perl  -w
use strict;

# Creates indexes for the various PSICQUIC servers, and puts them into a place
# where they will be accessible to the site hosting the servers for pick up.

use Getopt::Long;
use GKB::Config;
use Log::Log4perl qw/get_logger/;
Log::Log4perl->init(\$LOG_CONF);
my $logger = get_logger(__PACKAGE__);

our($opt_release);

# Parse commandline
my $usage = "Usage: $0 -release release_num\n";
&GetOptions("release:s");
$opt_release || die $usage;

my $gk_root_dir = $GK_ROOT_DIR;
#if (!(defined $gk_root_dir) || $gk_root_dir eq '') {
	# If no root directory is defined for GKB,
	# assume that the current directory is GKB/scripts/release.
	# A bit nieave, we can but try, sigh.
#	my $curr_work_dir = &Cwd::cwd();
		
	#$gk_root_dir = "$curr_work_dir/../..";
#}
my $psicquic_indexers_to_single_resource = "psicquic_indexers_to_single_resource.pl";
#if (!(-e $psicquic_indexers_to_single_resource)) {
	## Last resort
	#my $home = $ENV{HOME};
	#if (defined $home && !($home eq '')) {
		#$gk_root_dir = "$home/GKB";
		#$psicquic_indexers_to_single_resource = "$home/GKB/scripts/release/psicquic_indexers_to_single_resource.pl";
	#}
#}
if (!(-e $psicquic_indexers_to_single_resource)) {
    $logger->error_die("file $psicquic_indexers_to_single_resource does not exist!\n");
}

my @resources = (
    'ReactomeBuilder',
    'ReactomeFIBuilder',
);

# Farm each build out to a separate script, rather than running them all in one
# script, so that if one build fails, it doesn't stop the others from running.
my $resource;
my $cmd;
foreach $resource (@resources) {
    if (!(defined $resource) || $resource eq '') {
    	$logger->error("missing resource value!\n");
    	next;
    }
    $cmd = "perl $psicquic_indexers_to_single_resource -release $opt_release -gk_root $gk_root_dir -builder $resource";
    if (system($cmd) != 0) {
    	$logger->error("something went wrong while executing '$cmd'!!\n");
    }
}

print "$0 has finished its job\n";
