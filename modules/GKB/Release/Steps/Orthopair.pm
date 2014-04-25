package GKB::Release::Steps::Orthopair;

use GKB::Release::Config;
use GKB::Release::Utils;

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
 
    # Find current ensembl version
    my $browser = LWP::UserAgent->new;
    my $response = $browser->get("http://www.ensembl.org/index.html");
    my $content = $response->content;
    
    my ($ensmbl_ver) = ($content =~ /release (\d+)/g)[-1];
    $ensmbl_ver = $ensmbl_ver - 1; # Must be previous version to be in sync with the PanCompara database
      
    # ecol, mtub, saur no longer included due to the absence of bacterial databases in EnsEMBL BioMart
    my @species = ("atha", "btau", "cele", "cfam", "drer", "ddis", "dmel", "ggal", "hsap", "mmus", "osat", "pfal", "rnor", "scer", "spom", "sscr", "tgut", "xtro");
    
    # Run script, check files and rerun script if errors 
    my $repeat;
    my $count = 0;
    do {
    	mkdir $version unless (-d $version); 
    	
    	cmd("Running orthopair script",[["./wrapper_orthopair.sh $version $ensmbl_ver > $version/wrapper_orthopair.out"]]);
    	    
        $count++;
        $repeat = 0;
        
        foreach my $species (@species) { 
        	 
            my @files = `ls $version/*$species*mapping*.txt 2> /dev/null`;
            next if $?;
        
            # Check each file for zero size -- indicating errors
            foreach my $file (@files) {
                chomp $file;
                my $filesize = (stat $file)[7];
                
                if ($filesize == 0) {
                    `rm $file`;
                    $repeat = 1;
                }
            }
        }
    } while ($repeat && $count < 3); # Determine if species files are present -- script rerun if not.  After 3 times, it is considered to have failed 
    
    # Fail after 3rd try
    if ($repeat) {
        die "Orthopair script has failed.";
    }
            
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

1;
