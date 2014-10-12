package GKB::Release::Config;

use strict;
use warnings;

use base 'Exporter';

use File::Basename;

# Created by: Joel Weiser (joel.weiser@oicr.on.ca for questions/comments)
# Purpose: A module to set and export reactome release variables

my $TEST_MODE = 1;

# Set user variables
my $defaultuser = `whoami`;
chomp $defaultuser;
print 'Enter user name - leave blank for default of ' . $defaultuser . ': ' unless $TEST_MODE;
our $user = $TEST_MODE ? $defaultuser : <STDIN>; # User name or default
chomp $user;
$user ||= $defaultuser;

our $pass; # mysql password
our $sudo; # Sudo password

our $date = `date "+%Y%m%d"`; # Today's date
chomp $date;

print 'Enter current version number: ' unless $TEST_MODE;
our $version = $TEST_MODE ? 999 : <STDIN>; # New Reactome Version
chomp $version;

our $prevver = $version - 1; # Previous Reactome Version

# Set database names
our $db = "test_reactome_$version"; # Test Reactome Database (e.g. test_reactome_38)
our $slicedb = "test_slice_$version"; # Slice Database (e.g. test_slice_38)
our $releasedb = "test_release_$version"; # Release Database (e.g. test_release_38)
our $biomartdb = "test_reactome_mart";
our $gkcentral = "gk_central";
our $gkcentral_host = "reactomecurator.oicr.on.ca";

if ($TEST_MODE) {
    $gkcentral = "test_gk_central";
    $gkcentral_host = "reactomerelease.oicr.on.ca";
    $prevver = 46;
}

# Set directory paths
our $gkbdev = "/usr/local/gkbdev";
our $scripts = "$gkbdev/scripts";
our $release = "$scripts/release";
our $website = "$gkbdev/website";
our $html = "$website/html";
our $gkbmodules = "$gkbdev/modules";
our $dumpdir = "$gkbdev/tmp";
our $tmp = "$gkbdev/tmp";
our $cvs = "/usr/local/cvs_repository";
our $logdir = "$release/logs";
our $logfile = "$logdir/release$version.log";

our %passwords = (
    'sudo' => \$sudo, 
    'mysql' => \$pass
);


our $release_server = "reactomerelease.oicr.on.ca";
our $live_server = "reactomeprd1.oicr.on.ca";
our $dev_server = "reactomedev.oicr.on.ca";

if ($TEST_MODE) {
    $live_server = "reactomeclean.oicr.on.ca";
}

# Host to gkb directory and vice-versa
our %hosts = (
    $release_server => "gkbdev",
    $dev_server => "gkbdev",
    $live_server => "gkb_prod",
   
    "gkbdev" => $release_server,
    "gkb_prod" => $live_server,
    "gkb_test" => $live_server,

    # Alternate servers    
    "brie8.cshl.edu" => "gkbdev",
    "reactomeclean.oicr.on.ca" => "gkbdev" 
);
   
our %maillist = (
    'internal' => 'internal@reactome.org',
    'release' => 'croft@ebi.ac.uk',
    'curation' => 'lmatthews.nyumc@gmail.com',
    'automation' => 'joel.weiser@oicr.on.ca',
    'outreach' => 'robin.haw@oicr.on.ca'
);

our $log_conf = dirname(__FILE__)."/releaselog.conf";

our @EXPORT = qw/
    $user $pass $sudo $date $version $prevver 
    $db $slicedb $releasedb $biomartdb $gkcentral $gkcentral_host
    $gkbdev $scripts $release $compara $website $html $gkbmodules $go $dumpdir $tmp $cvs $logdir $logfile
    %passwords $release_server $live_server $dev_server %hosts %maillist
    $log_conf
/;

1;
