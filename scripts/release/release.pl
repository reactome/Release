#!/usr/bin/perl
use strict;
use warnings;

use lib '/usr/local/gkb/modules';

use constant {
    GKB_DEV_ALIAS => 'gkbdev',
    GKB_LIVE_ALIAS => 'gkb'
};

use GKB::Release::Config;
use GKB::Release::Utils;

my @options = (
    ['UniprotUpdate', GKB_DEV_ALIAS,"-runs uniprot update"],
    ['GoUpdate', GKB_DEV_ALIAS, "-runs go update"],
    ['ChebiUpdate', GKB_DEV_ALIAS, "-runs chebi update"],
    ['Orthopair', GKB_DEV_ALIAS, "-runs orthopair script"],
    ['UpdateStableIds', GKB_DEV_ALIAS, "-updates stable id minor version numbers"],
    ['OrthoInference', GKB_DEV_ALIAS, "-computational prediction for other species"],
    ['UpdateConfig', GKB_DEV_ALIAS, "-update configuration file to current version"],
    ['ExternalResourceLinks', GKB_DEV_ALIAS, "-creates external resource links"],
    ['OrthoInferenceStableIdsHistory', GKB_DEV_ALIAS, "-adds old stable ids to database and updates/saves stable id history"],
    ['OtherIdentifiers', GKB_DEV_ALIAS, "-add other identifiers to the release database"],
    ['BioModels', GKB_DEV_ALIAS, "-creates BioModels cross-references"],
    ['UpdateDOIs', GKB_DEV_ALIAS, "-adds DOIs to requesting pathways in release database and gk_central"],
    ['UpdateGkCurrent', GKB_DEV_ALIAS, "-populate gk_current database with new release content"],
    ['TOC', GKB_DEV_ALIAS, "-create cached toc and doi_toc files"],
    ['DownloadDirectory', GKB_DEV_ALIAS, "-Create the download directory"],
    ['CommitStatsFiles', GKB_DEV_ALIAS, "-commit inference statistics files to GitHub"],
    ['NCBI', GKB_DEV_ALIAS, "-create gene,protein, and omim files as well as hapmap and ucsc"],
    ['MSigDB_GSEA', GKB_DEV_ALIAS, "-creates Reactome_GeneSet_XX file in MSigDB format"],
    ['UncuratedProteins', GKB_DEV_ALIAS, "-creates a list of UniProt identifiers without EWAS referrers"]
);


my @choices;
my @allchoices;

unless (defined $ARGV[0] && $ARGV[0] =~ /\d(\.\.)\d|\d,?/) {
    print "Usage: perl $0 'numbers'\n";
    print "Example: perl $0 1..3,4,12\n\n";
    print "Choose numbers from the following list:\n";
    print "Number\tName\tGKB_Dir\tExplanation\n";
    
    my $number = 0;
    foreach my $option (@options) {
        $number++; 
        local $" = "\t";
        print "$number\t@{$option}\n";
    }
    
    exit;
}

unless ($TEST_MODE) {
    $version = prompt("Enter release version number:");
    $user = prompt("Enter user name - leave blank for default of $user:") || $user;
    die "Current version number must be an integer" unless $version && $version =~ /^\d+$/;
    $prevver = $version - 1;
} else {
    print "REMINDER: You are in TEST MODE as user $user releasing as version $version and previous version of $prevver.\n";
    print "To enable normal operation, edit \$TEST_MODE in the GKB::Release::Config module and restart this script\n";
}
set_version_for_config_variables($version);


@choices = split(',', $ARGV[0]);
foreach my $choice (@choices) {
    if ($choice =~ /\.\./) {
	my ($start, $end) = split('\.\.', $choice);
        for (my $i = $start; $i <= $end; $i++) {
            $allchoices[$i] = 1;
        }
    } else {
        $allchoices[$choice] = 1; 
    }
}	

my @instances;
for (my $i = 1; $i <= scalar @allchoices; $i++) {
    if ($allchoices[$i]) {     		
		my $index = $i - 1;
		my $step = $options[$index];
    
		my $gkbdir = $step->[1]; # gkbdev or gkb

		my $class = 'GKB::Release::Steps::' . $step->[0];
		eval "require $class" or die;
		my $instance = $class->new('gkb' => $gkbdir);
		$instance->set_user_input_and_passwords();
		push @instances, $instance;
    }    
}

foreach my $instance (@instances) {
    $instance->run();
}	
