package GKB::Release::Steps::DownloadDirectory;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

#has '+gkb' => ();
has '+passwords' => ( default => sub { ['mysql'] } );
has '+directory' => ( default => "$release/download_directory" );
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

    my $host = $self->host;         	

    my $download_dir = replace_gkb_alias_in_dir($html, $gkbdir) . '/download';

    if ($gkbdir eq "gkbdev") {
        cmd("Creating download directory",[["perl create_download_directory.pl -host $host -port 3306 -user $user -pass $pass -r $version -db $db > create_download_directory.out.$version"]]);
        cmd("Creating BioSystems export file",[["perl create_reactome2biosystems.pl -host $host -port 3306 -user $user -pass $pass -r $version -db $db > create_reactome2biosystems.out.$version"]]);
	cmd("Creating tarball",[["perl make_release_tarball.pl $version"]]);
	cmd("Moving download directory to website folder",[["mv $version $download_dir/$version"]]);
    } elsif ($gkbdir eq "gkb_prod") {
	#my $html_prod = replace_gkb_alias_in_dir($html, $gkbdir);
    	cmd("Copying current download directory from $host",[["scp -r $html/download/$version $live_server:$download_dir"]]);
    } elsif ($gkbdir eq "gkb_test") {
        my $html_prod = replace_gkb_alias_in_dir($html, "gkb_prod");
	#my $html_live = replace_gkb_alias_in_dir($html, $gkbdir);
        
	cmd("Moving gkb_prod download directory to $gkbdir and establishing symbolic links",
            [
            	["mv $html_prod/download/$version $download_dir/$version"],
            	["ln -s $download_dir/$version $html_prod/download/$version"]
            ],
	    {'ssh' => $live_server}
        );
    }
    
    # The command is run on the live server when $gkbdir is gkb_prod or gkb_test
    my $ssh_server = ($gkbdir eq "gkb_prod" || $gkbdir eq "gkb_test") ? $live_server : undef;
    cmd("Creating link to current download directory",
    	[
    		["mv $download_dir/$prevver $download_dir/foo"],
    		["rm $download_dir/current"],
    		["mv $download_dir/foo $download_dir/$prevver"],
    		["ln -s $download_dir/$version $download_dir/current"]
    	],
	{'ssh' => $ssh_server}
    );
};
    
1;
