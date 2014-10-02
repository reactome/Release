#!/usr/bin/perl
use strict;
use warnings;

use lib '/usr/local/gkbdev/modules';

use constant {
    GKB_DEV_ALIAS => 'gkbdev',
    GKB_LIVE_ALIAS => 'gkb'
};

my @options = (
	['UniprotUpdate', GKB_DEV_ALIAS,"-runs uniprot update"],
	['GoUpdate', GKB_DEV_ALIAS, "-runs go update"],
    	['ChebiUpdate', GKB_DEV_ALIAS, "-runs chebi update"],
    	['Orthopair', GKB_DEV_ALIAS, "-runs orthopair script"],
    	['UpdateSourceCode', GKB_DEV_ALIAS, "-updates source code"],
    	['ClearData', GKB_DEV_ALIAS, "-clear data from a previous run \[NB: do not do this if this is the first run\]"],
    	['GenerateStableIds', GKB_DEV_ALIAS, "-generates stable ids"],
    	['GoaPrepare', GKB_DEV_ALIAS, "-prepares GOA submission file"],
    	['Myisam', GKB_DEV_ALIAS, "-convert database to myisam format"],
    	['OrthoInference', GKB_DEV_ALIAS, "-computational prediction for other species"],
    	['UpdateConfig', GKB_DEV_ALIAS, "-update configuration file to current version"],
    	['ExternalResourceLinks', GKB_DEV_ALIAS, "-creates external resource links"],
    	['GenerateOrthoInferenceStableIds', GKB_DEV_ALIAS, "-assign stable identifiers to ortho-predicted instances"],    	
    	['Skypainter', GKB_DEV_ALIAS, "-creates skypainter database"],
    	['FrontpageAndTOC', GKB_DEV_ALIAS, "-create frontpage images and cached toc file"],
    	['Orthodiagrams', GKB_DEV_ALIAS, "-create diagrams for predicted pathways"],
	['DownloadDirectory', GKB_DEV_ALIAS, "-update the download directory"],
    	['RerouteRequests', GKB_LIVE_ALIAS, "-reroute website to port 8000"],
    	['UpdateSourceCode', GKB_LIVE_ALIAS, "-update source code"],
    	['DownloadDirectory', GKB_LIVE_ALIAS, "-update download directory"],
    	['CopyOverDatabases', GKB_LIVE_ALIAS, "-copy over databases"],
    	['UpdateStableIdDb', GKB_LIVE_ALIAS, "-update stable id databases"],
    	['UpdateConfig', GKB_LIVE_ALIAS, "-update configuration file to current version"],
    	['FrontpageAndTOC', GKB_LIVE_ALIAS, "-copy over front page images and create cached toc file"],
    	['ClearSearchCache', GKB_LIVE_ALIAS, "-clear search cache"],
    	['RerouteRequests', GKB_LIVE_ALIAS, "-switch back to public server"],
    	['RestartTomcat', GKB_LIVE_ALIAS, "-restart tomcat for WS SOAP API"],
    	['CommitGoa', GKB_DEV_ALIAS, "-commit goa files to cvs"],
    	['NCBI', GKB_DEV_ALIAS, "-create gene,protein, and omim files as well as hapmap and ucsc"],
    	['SetEmailReminders', GKB_DEV_ALIAS,"-sets automation of e-mail reminders for next release"],
    	['EventsAndEntities', GKB_DEV_ALIAS, "-creates reaction and pathway to/from genes and molecules"]
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

@choices = split ',', $ARGV[0];
foreach my $choice (@choices) {
    if ($choice =~ /\.\./) {
	my ($start, $end) = split '..', $choice;
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
