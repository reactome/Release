#!/usr/local/bin/perl -w

#This script wraps the various steps needed for inference from one species to another. It creates the ortho database, tweaks the datamodel needed for the inference, and runs the inference script, followed by two clean-up scripts.
#The standard run (for Reactome releases) only requires the reactome release version on the command line. One can also change the source species, restrict the run to one target species, or indicate a source database other than the default test_slice_reactomeversion_myisam. It's also possible to limit inference to specific Events by giving the internal id of the upstream event(s) on the command line. Inference will then be performed for these Events and all their downstream Events.

use lib "$ENV{HOME}/bioperl-1.0";
use lib "$ENV{HOME}/GKB/modules";
use lib "$ENV{HOME}/my_perl_stuff";
use GKB::DBAdaptor;
use GKB::Instance;
use GKB::Utils_esther;
use GKB::Config_Species;
use Data::Dumper;
use Getopt::Long;
use DBI;
use strict;

our($opt_r, $opt_from, $opt_sp, $opt_source_db, $opt_debug, $opt_host, $opt_port, $opt_user, $opt_pass);

@ARGV || die "Usage: $0 -r reactome_version(e.g.14)  -from source species (default = hsap) -sp sp_abbreviation (only for species-specific Reactome) -source_db name of source database (default = test_slice_reactomeversion_myisam)   (optional list of top event ids for limited inference) -debug\n";

&GetOptions("r:i", "from:s", "sp:s", "source_db:s", "debug", "host:s", "port:s", "user:s", "pass:s");


my %db_options = ("-host" => ($opt_host ||= "localhost"),
		  "-port" => ($opt_port ||= 3306),
		  "-user" => $opt_user,
		  "-pass" => $opt_pass		  
		  );
my $db_option_string = create_db_option_string(\%db_options);


#Define the source database
my $source = ($opt_source_db) ? $opt_source_db : "test_slice_$opt_r\_myisam";

#create database handle - needed to create the source database copy which will be used to add orthology data
my $dbc = DBI->connect("DBI:mysql:database=$source;host=$opt_host;port=$opt_port", $opt_user, $opt_pass,
            { RaiseError => 1, AutoCommit => 1});
if (!$dbc) {
     print STDERR "Error connecting to database; $DBI::errstr\n";
}

# create database - depending on whether all target species are used, or only one ($opt_sp),
# whether a source species ($opt_from) is specified, and whether a source database ($opt_source_db) is given,
# or the default source (test_reactome_XX) is used, the database name is constructed to reflect this
my $db = construct_db_name($opt_sp, $opt_from, $opt_source_db, $opt_r);

# Create and populate test_reactome_XX from test_slice_XX_myisam
system("mysql -u$opt_user -p$opt_pass -e 'drop database if exists $db'") == 0 or die "$?";
system("mysql -u$opt_user -p$opt_pass -e 'create database $db'") == 0 or die "$?";
run("mysqldump --opt -u$opt_user -p$opt_pass -h$opt_host $source | mysql -u$opt_user -p$opt_pass -h$opt_host $db") == 0 or die "$?";

#Human is defined as default source species
$opt_from || ($opt_from = 'hsap');

#Some datamodel changes are required for running the script, mainly to adjust defining attributes in order to avoid either duplication or merging of instances in the inference procedure, plus introduction of some additional attributes
my $exit_value = run("perl tweak_datamodel.pl -db $db $db_option_string");
if ($exit_value != 0) {
    print STDERR "wrapper_ortho_inference: WARNING - problem encountered during tweak_datamodel, aborting\n";
    exit($exit_value);
}

#run script for each species (order defined in config.pm)
foreach my $sp (@species) {
    print "wrapper_ortho_inference: considering sp=$sp\n";
    next if $sp eq $opt_from; #skip source species in species list
    if ($opt_sp) {
       next unless $sp eq $opt_sp;
    } else {
       next if $sp eq 'mtub';
    }
    print "wrapper_ortho_inference: running infer_events script\n";
    run("perl infer_events.pl -db $db -r $opt_r -from $opt_from -sp $sp -thr 75 @ARGV $db_option_string"); #run script with 75% complex threshold
}
`chgrp gkb $opt_r/* 2> /dev/null`; # Allows all group members to read/write compara release files

print "wrapper_ortho_inference: run clean up scripts\n";
#These are two "clean-up" scripts to remove unused PhysicalEntities and to update display names
run("perl remove_unused_PE.pl -db $db $db_option_string");
run("perl updateDisplayName.pl -db $db -class PhysicalEntity $db_option_string");

print "$0 has finished its job\n";


sub create_db_option_string {
	my $db_options_hashref = shift;
	
	my $db_option_string = "";	
	foreach my $option_key (keys %{$db_options_hashref}) {
		my $option_value = $db_options_hashref->{$option_key};
		
		if ($option_value) {
			$db_option_string .= " $option_key $option_value";
		}
	}
	
	print "$0 db_option_string:$db_option_string\n";
	
	return $db_option_string;
}

sub construct_db_name {
	my $target_species = shift;
	my $source_species = shift;
	my $source_db = shift;
	my $release_number = shift;
	
	# Default is test_reactome_XX
	my $db = ($source_db) ? "$source_db\_ortho" : "test_reactome_$release_number";
	
	if ($source_species) { # Other than default (i.e. human) as source species
		my $sp = ($target_species) ? $target_species : 'all'; # Individual target species
		
		$db .= "\_$source_species\_$sp";
	} elsif ($target_species) { # Individual target species
		$db .= "\_$target_species";
	}
	
	return $db;
}

sub run {
    my $cmd = shift;
    
    print "Now starting: $cmd\n";
    my $exit_value = system($cmd);
    if ($exit_value != 0) {
        print STDERR "wrapper_ortho_inference.run: WARNING - potential problem encountered during $cmd (exit value $exit_value)!\n";
    }
    return $exit_value;
}
