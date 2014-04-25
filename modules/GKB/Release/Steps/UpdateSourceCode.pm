package GKB::Release::Steps::UpdateSourceCode;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => "gkbdev" );
has '+passwords' => ( default => sub { ['sudo'] });
has '+directory' => ( default => "$release" );
has '+mail' => ( default => sub { 
					my $self = shift;
					return {
						'to' => 'curation',
						'subject' => $self->name,
						'body' => "",
						'attachment' => ""
					};
				}
);

override 'run_commands' => sub {
	my ($self, $gkbdir) = @_;
     
    my $return;
    $return = (cmd("Updating source code from cvs",[["./run_cvs.pl", ("-release", $version)]]))[0]->{'exit_code'} if ($gkbdir eq "gkbdev"); 
    
    # If on reactome live or production or the script fails on reactomedev, the cvs is updated directory by directory
    if ($return || $gkbdir ne "gkbdev") {
        `rm -f /usr/local/$gkbdir/website/html/userguide/*`; # Userguide is deleted to avoid conflict in cvs (see documentation for release)
        
        my @directories = qw/biopaxexporter modules\/GKB scripts orthomcl_project java BioMart\/reactome ReactomeGWT slicingTool website/; # Directories to be updated
        for (my $i=0; $i < scalar @directories; $i++) {
            $directories[$i] = "/usr/local/$gkbdir/" . $directories[$i]; # Prepend gkb directory 
        }
        push @directories, "/usr/local/caBIG/caBIGR3";
        
        # For each directory, change group to gkb and add group read and write permissions then update cvs
        foreach my $dir (@directories) {
            chdir "$dir" || next;
            cmd("Updating $dir directory from cvs",
            	[
            		["echo $sudo | sudo -S chgrp -R gkb *"],
            		["echo $sudo | sudo -S chmod -R g+rw *"],
            		["cvs up -d"],
            		["cvs up"]
            	]
            );
        }
    }
};

1;
