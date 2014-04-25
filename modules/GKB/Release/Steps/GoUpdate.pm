package GKB::Release::Steps::GoUpdate;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => "gkbdev" );
has '+passwords' => ( default => sub { ['mysql', 'sudo'] } );
has '+directory' => ( default => "$release/go_update" );
has '+mail' => ( default => sub { 
					my $self = shift;
					return {
						'to' => 'curation',
						'subject' => $self->name,
						'body' => "",
						'attachment' => $self->directory . '/go.wiki'
					};
				}
);
						
override 'run_commands' => sub {
	my ($self, $gkbdir) = @_;

    cmd("Setting all GO files to have group permissions", [["echo $sudo | sudo -S chgrp gkb *"]]);
        
    cmd("Backing up database",[["mysqldump -u$user -p$pass -h$gkcentral_host --lock_tables=FALSE $gkcentral > $gkcentral\_after_uniprot_update.dump"]]);
 
=head 
    cmd("Updating from cvs",
    	[
    		["cvs up go_obo_update.pl"],
    		["cvs up addEcNumber2Activity_update.pl"],
    		["cvs up updateDisplayName.pl"]
 		]
 	);
=cut
        
    my @args = ("-db", $gkcentral, "-host", $gkcentral_host, "-user", $user, "-pass", $pass);
    
    cmd("Running GO obsolete update script",[["perl -d:NYTProf go_obo_update.pl @args > go.out 2> go.err"]]);
    cmd("Running EC number update script",[["perl addEcNumber2Activity_update.pl @args < ec2go"]]);
    
    foreach my $class ("MolecularFunction", "BiologicalProcess", "CellularComponent") {
        cmd("Updating $class display names",[["perl updateDisplayName.pl @args -class GO_$class"]]);
    }
};

1;
