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
    ['UpdateSourceCode', GKB_DEV_ALIAS, "-updates source code"],
    ['ClearData', GKB_DEV_ALIAS, "-clear data from a previous run \[NB: do not do this if this is the first run\]"],
    ['CVSUpdate', GKB_DEV_ALIAS, "-CVS update of the images directory on the release, live, and curator servers"],
    ['UpdateStableIds', GKB_DEV_ALIAS, "-updates stable id minor version numbers"],
    ['Myisam', GKB_DEV_ALIAS, "-convert database to myisam format"],
    ['OrthoInference', GKB_DEV_ALIAS, "-computational prediction for other species"],
    ['UpdateConfig', GKB_DEV_ALIAS, "-update configuration file to current version"],
    ['ExternalResourceLinks', GKB_DEV_ALIAS, "-creates external resource links"],
    ['GenerateOrthoInferenceStableIds', GKB_DEV_ALIAS, "-assign stable identifiers to ortho-predicted instances"],
    ['GoaPrepare', GKB_DEV_ALIAS, "-prepares GOA submission file"],
    ['OtherIdentifiers', GKB_DEV_ALIAS, "-add other identifiers to the release database"],
    ['Orthodiagrams', GKB_DEV_ALIAS, "-create diagrams for predicted pathways"],
    ['AnalysisCore', GKB_DEV_ALIAS, "-creates analysis binary and mapping files for export"],
    ['SearchIndexer', GKB_DEV_ALIAS, "-creates search index content and ebeye.xml"],
    ['BioModels', GKB_DEV_ALIAS, "-creates BioModels cross-references"],
    ['DiagramJSON', GKB_DEV_ALIAS, "-creates pathway diagram JSON and underlying graph JSON"],
    ['FireworksServer', GKB_DEV_ALIAS, "-creates fireworks JSON for pathways in each species"],
    ['InstallReleaseDB', GKB_DEV_ALIAS, "-installs test_reactome_XX for use as the default database"],
    ['SimplifiedDatabase', GKB_DEV_ALIAS, "-creates simplified database from release database"],
    ["UpdateDOIs", GKB_DEV_ALIAS, "-adds DOIs to requesting pathways in release database and gk_central"],
    ['UpdateGkCurrent', GKB_DEV_ALIAS, "-populate gk_current database with new release content"],
    ['TOC', GKB_DEV_ALIAS, "-create cached toc and doi_toc files"],
    ['DownloadDirectory', GKB_DEV_ALIAS, "-update the download directory"],
    ['RerouteRequests', GKB_LIVE_ALIAS, "-reroute website to reactomerelease"],
    ['CommitStatsFiles', GKB_DEV_ALIAS, "-commit inference statistics files to GitHub"],
    ['UpdateSourceCode', GKB_LIVE_ALIAS, "-update source code"],
    ['DownloadDirectory', GKB_LIVE_ALIAS, "-update download directory"],
    ['CopyOverDatabases', GKB_LIVE_ALIAS, "-copy over databases"],
    ['UpdateConfig', GKB_LIVE_ALIAS, "-update configuration file to current version"],
    ['TOC', GKB_LIVE_ALIAS, "-create cached toc and doi_toc files"],
    ['ClearSearchCache', GKB_LIVE_ALIAS, "-clear search cache"],
    ['RerouteRequests', GKB_LIVE_ALIAS, "-switch back to public server"],
    ['RestartTomcat', GKB_LIVE_ALIAS, "-restart tomcat for WS SOAP API"],
    ['UpdateFrontPage', GKB_DEV_ALIAS, "-update Reactome version and release date on front page of all servers"],
    ['CommitGoa', GKB_DEV_ALIAS, "-commit goa files to cvs"],
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
    $user = prompt("Enter user name - leave blank for default of $user:") || $user;
    $version = prompt("Enter release version number:");
    die "Current version number must be an integer" unless $version && $version =~ /^\d+$/;
    $prevver = $version - 1;
} else {
    print "REMINDER: You are in TEST MODE.  To enable normal operation, edit \$TEST_MODE in the GKB::Config::Release module and restart this script\n";
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
