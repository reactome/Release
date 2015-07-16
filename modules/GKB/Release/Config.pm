package GKB::Release::Config;

use strict;
use warnings;

use base 'Exporter';

use File::Basename;

# Created by: Joel Weiser (joel.weiser@oicr.on.ca for questions/comments)
# Purpose: A module to set and export reactome release variables

our $TEST_MODE = 0;

# Set user variables
chomp(our $user = `whoami`);

our $pass; # mysql password
our $sudo; # Sudo password

chomp(our $date = `date "+%Y%m%d"`); # Today's date

our $version;
our $prevver;

# Set database names
our $db = "test_reactome_{version}"; # Test Reactome Database (e.g. test_reactome_38)
our $slicedb = "test_slice_{version}"; # Slice Database (e.g. test_slice_38)
our $gkcentral = "gk_central";
our $gkcentral_host = "reactomecurator.oicr.on.ca";

if ($TEST_MODE) {
    $gkcentral = "test_gk_central";
    $gkcentral_host = "reactomerelease.oicr.on.ca";
    $version = 999;
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
our $logfile = "$logdir/release{version}.log";
our $archive = "/nfs/reactome/reactome/archive/release";

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
    $live_server => "gkb",
   
    "gkbdev" => $release_server,
    "gkb" => $live_server,
    
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
    $TEST_MODE
    $user $pass $sudo $date $version $prevver
    $db $slicedb $gkcentral $gkcentral_host
    $gkbdev $scripts $release $compara $website $html $gkbmodules $go $dumpdir $tmp $cvs $logdir $logfile $archive
    %passwords $release_server $live_server $dev_server %hosts %maillist
    $log_conf
/;

sub set_version_for_config_variables {
    my $version_number = shift;
    
    s/{version}/$version_number/ foreach ($db, $slicedb, $logfile);
}

push @EXPORT, 'set_version_for_config_variables';

1;
