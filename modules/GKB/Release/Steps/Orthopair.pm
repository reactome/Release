package GKB::Release::Steps::Orthopair;

use GKB::Release::Config;
use GKB::Release::Utils;

use GKB::EnsEMBLMartUtils qw/get_version install_ensembl_api ensembl_api_installed/;

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

    my $ensmbl_ver = get_version();
    install_ensembl_api($self, $ensmbl_ver) unless ensembl_api_installed();

    mkdir $version unless (-d $version); 
    
    for (my $i = 0; $i < 3; $i++) {
    	$self->cmd("Running orthopair script",[["./wrapper_orthopair.sh $version $ensmbl_ver > $version/wrapper_orthopair.out"]]);
	last if files_okay();
    } # Determine if species files are present -- script rerun if not.  After 3 times, it is considered to have failed
            
    # Check orthopair files and attempt repairs if need be
    my $return = ($self->cmd("Checking orthopair files",[["perl check_orthopair_files.pl", ("-release", $version)]]))[0]->{'exit_code'};
    if ($return) {
		$self->cmd("Fixing orthopair files",
			[
				["perl check_orthopair_files.pl",  ("-release", $version, "-fix", "-run_count", 0)], 
				["perl check_orthopair_files.pl", ("-release", $version)]
			]
		);	 
    }
};

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
