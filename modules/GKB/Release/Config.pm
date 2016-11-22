package GKB::Release::Config;

=head1 NAME

GKB::Release::Config

=head1 DESCRIPTION

Exports variables needed by the Reactome
release pipeline.

=head2 VARIABLES

$TEST_MODE (1 for test; 0 for production)
$user (user running release)
$pass (mysql password for user)
$sudo (sudo password for user)
$date (today's date)
$version (Reactome release version)
$prevver (previous Reactome release version)
$db (Reactome release database -- i.e. test_reactome_XX)
$slicedb (Reactome slice database -- i.e. test_slice_XX)
$stable_id_db (Reactome stable identifier database)
$gkcentral (Reactome curator database)
$gkcentral_host (host server for Reactome database)
$gkbdev (directory for Release Git repository)
$scripts (scripts directory)
$release (release directory)
$website (website directory)
$html (html directory)
$gkbmodules (GKB modules directory)
$dumpdir (database dump directory)
$tmp (temporary directory)
$cvs (cvs repository directory)
$logdir (directory for logging files)
$logfile (log file -- $logdir/releaseXX.log)
$archive (base archive directory)
%passwords (password hash of name to variable reference)
$release_server (release server host name)
$live_server (live server host name)
$dev_server (development server host name)
%hosts (hash of host to gkb alias and vice-versa)
%maillist (hash of 'role' to e-mail address)
$log_conf (configuration file for Log4perl)

=head2 METHODS

=over 12
	
=item C<set_version_for_config_variables>

Replace {version} with actual numeric
value of Reactome release version for
exported variables in this module.

Parameters:
	Reactome release version (Number - required)

	
=back
	
=head1 SEE ALSO

GKB::Release::Step
GKB::Release::Steps::*
GKB::Release::Utils

=head1 AUTHOR
Joel Weiser E<lt>joel.weiser@oicr.on.caE<gt>

Copyright (c) 2015 Ontario Institute for Cancer Research

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

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
our $stable_id_db = "stable_identifiers";
our $gkcentral = "gk_central";
our $gkcentral_host = "reactomecurator.oicr.on.ca";

if ($TEST_MODE) {
    $stable_id_db = "test_stable_identifiers",
    $gkcentral = "test_gk_central";
    $gkcentral_host = "reactomerelease.oicr.on.ca";
    $version = 999;
    $prevver = 57;
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
    'curation' => 'lmatthews.nyumc@gmail.com',
    'automation' => 'joel.weiser@oicr.on.ca',
    'outreach' => 'robin.haw@oicr.on.ca'
);

our $log_conf = dirname(__FILE__)."/releaselog.conf";

our @EXPORT = qw/
    $TEST_MODE
    $user $pass $sudo $date $version $prevver
    $db $slicedb $stable_id_db $gkcentral $gkcentral_host
    $gkbdev $scripts $release $website $html $gkbmodules $dumpdir $tmp $cvs $logdir $logfile $archive
    %passwords $release_server $live_server $dev_server %hosts %maillist
    $log_conf
/;

sub set_version_for_config_variables {
    my $version_number = shift;
    
    s/{version}/$version_number/ foreach ($db, $slicedb, $logfile);
}

push @EXPORT, 'set_version_for_config_variables';

1;
