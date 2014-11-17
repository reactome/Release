package GKB::Release::Steps::Orthopair;

use GKB::Release::Config;
use GKB::Release::Utils;

use autodie;
use LWP::UserAgent;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => "gkbdev" );
#has '+passwords' => ();
has '+directory' => ( default => "$release/orthopairs" );
has '+mail' => ( default => sub { 
					my $self = shift;
					return {
						'to' => '',
						'subject' => $self->name,
						'body' => "",
						'attachment' => ""
					};
				}
);

override 'run_commands' => sub {
    my ($self, $gkbdir) = @_;

    require 'ensembl.lib';

    my $ensmbl_ver = get_ensembl_version();
    install_ensembl_api($self, $ensmbl_ver) unless ensembl_api_installed();

    mkdir $version unless (-d $version); 
    
    for (my $i = 0; $i < 3; $i++) {
    	cmd("Running orthopair script",[["./wrapper_orthopair.sh $version $ensmbl_ver > $version/wrapper_orthopair.out"]]);
	last if files_okay();
    } # Determine if species files are present -- script rerun if not.  After 3 times, it is considered to have failed 
    die "Orthopair script has failed." unless files_okay();
    
            
    # Check orthopair files and attempt repairs if need be
    my $return = (cmd("Checking orthopair files",[["perl check_orthopair_files.pl", ("-release", $version)]]))[0]->{'exit_code'};
    if ($return) {
		cmd("Fixing orthopair files",
			[
				["perl check_orthopair_files.pl",  ("-release", $version, "-fix", "-run_count", 0)], 
				["perl check_orthopair_files.pl", ("-release", $version)]
			]
		);	 
    }
};

sub install_ensembl_api {
    my ($self, $version) = @_;
    
    chdir $gkbmodules;
    `perl install_ensembl_api.pl $version`;
    chdir $self->directory;
}

sub ensembl_api_installed {
    my $ensembl_api_dir = $gkbmodules . "/ensembl_api/";
    my @subdirectories = qw/bioperl-1.2.3 ensembl ensembl-compara/;
    
    foreach my $subdirectory (map {$ensembl_api_dir . $_} @subdirectories) {
	return 0 unless (-d $subdirectory);
    }
    
    return 1;
}


sub files_okay {
     # ecol, mtub, saur no longer included due to the absence of bacterial databases in EnsEMBL BioMart
    my @species = ("atha", "btau", "cele", "cfam", "drer", "ddis", "dmel", "ggal", "hsap", "mmus", "osat", "pfal", "rnor", "scer", "spom", "sscr", "tgut", "xtro");
    
    foreach my $species (@species) { 
        my @files = `ls $version/*$species*mapping*.txt 2> /dev/null`;
            
	if (@files) {
	    # Check each file for zero size -- indicating errors
	    foreach my $file (@files) {
		chomp $file;
		my $filesize = (stat $file)[7];
		    
		if ($filesize == 0) {
		    `rm $file`;
		    return 0;
		}
	    }
	} else {
	    return 0;
	}
    }
    
    return 1;
}

1;
