package GKB::Release::Steps::GWT;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

#has '+gkb' => ();
has '+passwords' => ( default => sub { ['sudo','mysql'] } );
has '+directory' => ( default => "$release/gwt" );
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

    # Set GWT url, where to execute the command (locally by default), and script options
    my ($url, $ssh);
    my $options = "-db $db";
    if ($gkbdir eq "gkbdev") {
        $url = "http://$release_server";
    } else {
	$ssh = $live_server;
	if ($gkbdir eq "gkb_prod") {
            $url = "http://www.reactome.org:8000";
	} else {
	    $options = "-user $user -pass $pass -db gk_current";
	    $url = "http://www.reactome.org -no_cached_data -no_images";
	}
    }
    
    cmd("Building GWT javascript and deploying GWT servlets",[["echo $sudo | sudo -S perl create_gwt3.pl $options -url $url"]], {"ssh" => $ssh});
    
    cmd("Generating static tables for analysis tools", [["./generate_static_table.sh -db $db > generate_static_table.out.$version"]]) if ($gkbdir eq "gkbdev");
};

1;
