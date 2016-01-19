#!/usr/bin/perl
use strict;
use warnings;

use Carp;
use Data::Dumper;
use DateTime;
use File::Remote;
use Getopt::Long;
use Path::Tiny qw/path/;
use Readonly;

Readonly my $FRONT_PAGE_FILE =>
'/usr/local/reactomes/Reactome/development/GKB/website/html/wordpress/wp-content/themes/HS_OICR_2013/front-page.php';

my ($new_version, $new_release_date, $help);
GetOptions(
	'new_version:s' => \$new_version,
	'new_release_date:s' => \$new_release_date,
	'help' => \$help
);

if ($help || !new_version_valid($new_version) || !new_release_date_valid($new_release_date)) {
	print usage_instructions();
	exit;
}

my $current_version_regex = qr/((?i)current version.*?v)\d+/; # (?i) makes the capture group case insensitive
my $current_release_date_regex = qr/(The ${current_version_regex}.*? was released on ).*/;

my $path_object = path($FRONT_PAGE_FILE);
my $data = $path_object->slurp_utf8;

$data =~ s/$current_version_regex/$1$new_version/g;
my $formatted_release_date = get_formatted_date($new_release_date);
$data =~ s/$current_release_date_regex/$1$formatted_release_date.\n/;

$path_object->append({truncate => 1}, $data);

copy_file_to_servers($FRONT_PAGE_FILE,['reactomeprd1.oicr.on.ca', 'reactomedev.oicr.on.ca', 'reactomecurator.oicr.on.ca']);

sub new_version_valid {
	my $version = shift;
	
	return ($version && $version =~ /^\d+$/);
}

sub new_release_date_valid {
	my $release_date = shift;
	
	return ($release_date && $release_date =~ get_date_pattern());
}

sub get_formatted_date {
	my $date = shift;
	
	my ($year, $month, $day) = $date =~ get_date_pattern();
	croak "Unable to extract date from $date" unless ($year && $month && $day);
	
	my $date_time_object = DateTime->new(
		year => $year,
		month => $month,
		day => $day
	);
	
	return $date_time_object->month_name . ' ' . $date_time_object->day . ', ' . $date_time_object->year;
}

sub get_date_pattern {
	return qr/^(\d{4})-(\d{2})-(\d{2})$/;
}

sub copy_file_to_servers {
	my $file = shift;
	my $servers = shift;
	
	my $remote = new File::Remote;
	foreach my $server (@{$servers}) {
		$remote->copy($file, "$server:$file");
	}
}

sub usage_instructions {
	return <<END;
	
This script updates the wordpress front-page file located at
$FRONT_PAGE_FILE by changing the current version and release
date.

Usage: perl $0 [options]

-new_version [reactome_version] (REQUIRED e.g. 55)
-new_release_date [version_release_date] (REQUIRED e.g. 2015-12-15)
-help

END
}