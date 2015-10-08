package GKB::Release::Steps::NCBI;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => "gkbdev" );
has '+passwords' => ( default => sub { ['mysql'] } );
has '+user_input' => (default => sub { 	{'ncbi_ftp_pass' => {'query' => 'Please enter the NCBI ftp password for Reactome user:',
							     'hide_keystrokes' => 1
							    }
					}
				     }
);
has '+directory' => ( default => "$release/ncbi" );
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
    
    # Run entrez scripts
	# OMIM no longer processed by NCBI (October 8, 2015 -- Joel Weiser)
    foreach my $script (qw/gene protein/) {
    	$self->cmd("Running $script script", [['./1' . $script . "entrez.pl -user $user -pass $pass -db $db"]]);
    }

    my $ncbipass = $self->user_input->{'ncbi_ftp_pass'}->{'response'};
    # Connect to ncbi and upload files
    $self->cmd("Uploading NCBI files", [["perl uploadncbi.pl $ncbipass $version"]]);
    
    # Run hapmap and UCSC scripts
    $self->cmd("Running hapmap script", [["./1haprefseq.pl -user $user -pass $pass -db $db"]]);
    $self->cmd("Running UCSC script", [["./1ucscentity.pl -user $user -pass $pass -db $db"]]);
};
    
1;
