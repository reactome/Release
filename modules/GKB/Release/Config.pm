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
$port (port for database connection)
$reactome_unix_group (unix group to use for assigning/changing permissions)
$date (today's date)
$version (Reactome release version)
$prevver (previous Reactome release version)
$db (Reactome release database -- e.g. test_reactome_XX)
$previous_db (previous Reactome release database)
$live_db (production MySQL database name)
$slicedb (Reactome slice database -- e.g. test_slice_XX)
$previous_slice_db (Previous release's slice database)
$stable_id_db (Reactome stable identifier database)
$gkcentral (Reactome curator database)
$gkcentral_host (host server for Reactome database)
$base_dir (directory with all Reactome components)
$gkbdev (directory for Release Git repository)
$scripts (scripts directory)
$release (release directory)
$website (website directory)
$website_static (website static content)
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
$curator_server (curator server host name - where gk_central resides)
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
our $port = 3306;
our $reactome_unix_group = 'reactome';

chomp(our $date = `date "+%Y%m%d"`); # Today's date

our $version;
our $prevver;

# Set database names
our $db = 'release_current'; # Current release database (e.g. test_reactome_38)
our $previous_db = 'release_previous'; # Previous release database
our $slicedb = 'slice_current'; # Current slice database (e.g. test_slice_38)
our $previous_slice_db = 'slice_previous'; # Slice database from the previous release
our $live_db = 'current';
our $stable_id_db = 'stable_identifiers';
our $gkcentral = 'gk_central';
our $gkcentral_host = 'curator.reactome.org';

if ($TEST_MODE) {
    $user = 'piper';
    #$stable_id_db = "stable_identifiers",
    $gkcentral = 'central';
    $gkcentral_host = 'localhost';
    $version = 999;
    $prevver = 68;
}

# Set directory paths
our $base_dir = '/usr/local/reactomes/Reactome/production';
our $gkbdev = "$base_dir/Release";
our $scripts = "$gkbdev/scripts";
our $release = "$scripts/release";
our $website = "$base_dir/Website";
our $website_static = "$website/static";
our $gkbmodules = "$gkbdev/modules";
our $dumpdir = "$gkbdev/tmp";
our $tmp = "$gkbdev/tmp";
our $cvs = '/usr/local/cvs_repository';
our $logdir = "$release/logs";
our $logfile = "$logdir/release{version}.log";
our $archive = "$gkbdev/archive";

our %passwords = (
    'sudo' => \$sudo,
    'mysql' => \$pass
);


our $release_server = 'release.reactome.org';
our $live_server = 'reactome.org';
our $dev_server = 'dev.reactome.org';
our $curator_server = 'curator.reactome.org';

if ($TEST_MODE) {
    $live_server = 'localhost';
}

# Host to gkb directory and vice-versa
our %hosts = (
    $release_server => 'gkbdev',
    $dev_server => 'gkbdev',
    $live_server => 'gkb',
    $curator_server => 'gkb',

    'gkbdev' => $release_server,
    'gkb' => $live_server,
);

our %maillist = (
    'internal' => 'internal@reactome.org',
    'curation' => 'lmatthews.nyumc@gmail.com',
    'automation' => 'solomon.shorser@oicr.on.ca, justin.cook@oicr.on.ca, joel.weiser@oicr.on.ca',
    'default_sender' => 'joel.weiser@oicr.on.ca',
    'outreach' => 'robin.haw@oicr.on.ca'
);

our $log_conf = dirname(__FILE__)."/releaselog.conf";

our @EXPORT = qw/
    $TEST_MODE
    $user $pass $sudo $port $reactome_unix_group $date $version $prevver
    $db $previous_db $slicedb $previous_slice_db $live_db $stable_id_db $gkcentral $gkcentral_host
    $base_dir $gkbdev $scripts $release $website $website_static $gkbmodules $dumpdir $tmp $cvs $logdir $logfile $archive
    %passwords $release_server $live_server $dev_server $curator_server %hosts %maillist
    $log_conf
/;

sub set_version_for_config_variables {
    my $version_number = shift;

    s/{version}/$version_number/ foreach ($db, $slicedb, $logfile);
}

push @EXPORT, 'set_version_for_config_variables';

1;
