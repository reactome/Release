#!/usr/local/bin/perl -w

# Post-ortho pair generation QA script.  Run this to make sure that all of the ortho
# pair files have been generated.  Take care with the -fix option.

# Author: Croft.

use strict;
use warnings;
use lib "$ENV{HOME}/GKB/modules";
use Getopt::Long;
use DBI;
use GKB::Utils;
use GKB::Config_Species;
use LWP::UserAgent;
use POSIX qw/strftime/;
use File::Copy;

our($opt_release, $opt_from, $opt_fix, $opt_run_count, $opt_ensembl_release);

&GetOptions("release=i",
	    "from=s",
	    "fix",
	    "run_count=i",,
	    "ensembl_release=i",,
	    );

my $MAX_RUN_COUNT = 4;

$opt_release || die "Need Reactome release number, e.g. -release 32\n";
$opt_from || ($opt_from = 'hsap');
defined $opt_run_count || ($opt_run_count = (-1));
$opt_ensembl_release || ($opt_ensembl_release = 58);

my $compara_dir = "$ENV{HOME}/GKB/release/orthopairs";
my $release_dir = "$compara_dir/$opt_release";

print "Compara ortho pair QA for release $opt_release\n\n";

my $is_ok = 1;

# The use of a list and a hash of species in Config_Species is bound to cause
# trouble sooner or later.  Check to see if that is a possible problem.
my %species_seen = ();
my @only_in_species = ();
foreach my $species_4_letter_name (@species) {
	if ($species_info{$species_4_letter_name}) {
		$species_seen{$species_4_letter_name} = $species_4_letter_name;
	} else {
		push(@only_in_species, $species_4_letter_name);
	}
}
my @only_in_species_info = ();
foreach my $species_key (keys %species_info) {
	if (!$species_seen{$species_key}) {
		push(@only_in_species_info, $species_key);
	}
}
if (scalar(@only_in_species) > 0) {
	print "The following species are in Config_Species->species but *not* in Config_Species->species_info: @only_in_species\n";
	$is_ok = 0;
}
if (scalar(@only_in_species_info) > 0) {
	print "The following species are in Config_Species->species_info but *not* in Config_Species->species: @only_in_species_info\n";
	$is_ok = 0;
}
if ($is_ok) {
	print "No discrepancies between Config_Species->species and Config_Species->species_info were found\n"
}

if (!(-e $release_dir)) {
    print "Directory $release_dir does not exist, it seems like you have not yet run ortho pair generation for release $opt_release\n";
    exit(0);
}

my @bad_species = get_bad_species($release_dir, $opt_from);
my @species_files;
my $species;
if (scalar(@bad_species) > 0) {
	print "\n\nSummary: problems were found in the following species files: @bad_species\n";
	if ($opt_fix) {
		print "Recalculating bad species files, opt_run_count=$opt_run_count\n";
		my $species_file;
		foreach $species (@bad_species) {
			@species_files = get_species_files($release_dir, $opt_from, $species);
			foreach $species_file (@species_files) {
				unlink($species_file);
			}
		}
			
		if ($opt_run_count >= 0) {
			if ($opt_run_count < $MAX_RUN_COUNT) {
				system("$compara_dir/prepare_orthopair_files.pl -release $opt_release");
				system("$compara_dir/prepare_orthopair_files.pl -release $opt_release -core");
				system("$compara_dir/check_orthopair_files.pl -fix -release $opt_release -ensembl_release $opt_ensembl_release -run_count " . ($opt_run_count + 1));
			} else {
				my $previous_release = $opt_release - 1;
				my $previous_release_dir = "$compara_dir/$previous_release";
				print "Lets see of files can be copied from previous releases, starting with $previous_release_dir\n";
				my %good_species;
				my @new_bad_species;
				my $good_species;
				my $target_species_file;
				while (scalar(@bad_species) > 0 && -e $previous_release_dir ) {
					print "previous_release_dir=$previous_release_dir\n";
					%good_species = ();
					foreach $species (@bad_species) {
						print "species=$species\n";
						if (check_species($previous_release_dir, $opt_from, $species)) {
							$good_species{$species} = $species;
							@species_files = get_species_files($previous_release_dir, $opt_from, $species);
							print "Copying files for $species from release $previous_release\n";
							foreach $species_file (@species_files) {
								$target_species_file = $species_file;
								$target_species_file =~ s/\/$previous_release\//\/$opt_release\//;
								copy($species_file, $target_species_file);
							}
						}
					}
					@new_bad_species = ();
					foreach $species (@bad_species) {
						if (!defined $good_species{$species}) {
							push(@new_bad_species, $species);
						}
					}
					@bad_species = @new_bad_species;
					
					print "New bad_species=@bad_species\n";
					
					$previous_release--;
					$previous_release_dir = "$compara_dir/$previous_release";
				}
			}
		}
	}
} else {
	print "All species files look OK\n";
}

sub get_bad_species {
	my ($release_dir, $focus_species) = @_;
	
	my @bad_species = ();
	my $species;
	foreach $species (keys %species_info) {
		$is_ok = check_species($release_dir, $opt_from, $species);
		if (!$is_ok) {
			push(@bad_species, $species);
		}
	}

	return @bad_species;
}

sub check_species {
	my ($release_dir, $focus_species, $species) = @_;
	
    print "\nChecking $species...\n";
    my $is_ok = 1;
    my @species_files = get_species_files($release_dir, $focus_species, $species);
    my $species_file;

	foreach $species_file (@species_files) {
		$is_ok = check_file($species_file) && $is_ok;
	}
	
	if ($is_ok) {
		print "\tNo problems found\n"
	}
	
	return $is_ok;
}

sub get_species_files {
	my ($release_dir, $focus_species, $species) = @_;
	
    my @species_files = ();

	if ($species eq $focus_species) {
		push(@species_files, "$release_dir/${focus_species}_protein_gene_mapping.txt");
	} else {
		push(@species_files, "$release_dir/$focus_species\_$species\_mapping.txt");
		push(@species_files, "$release_dir/$focus_species\_$species\_homol_mapping.txt");
		push(@species_files, "$release_dir/$species\_gene_protein_mapping.txt");
	}
	
	return @species_files;
}

sub check_file {
	my ($path) = @_;
	
    my $is_ok = 1;

	if (-e $path) {
		if (-s $path == 0) {
			print "\tFile $path has zero length!\n";
			$is_ok = 0;
		}
	} else {
		print "\tFile $path does not exist!\n";
		$is_ok = 0;
	}
	
	return $is_ok;
}
