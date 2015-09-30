package GKB::Release::Steps::FrontpageAndTOC;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+passwords' => ( default => sub { ['sudo'] } );
#has '+directory' => ( default => "$release/toc" );
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

    my $ssh = $gkbdir eq "gkbdev" ? undef : $live_server; 
    my $website_dir = replace_gkb_alias_in_dir($website, $gkbdir);
    my $livedb = 'gk_current';
  
    if ($gkbdir eq "gkbdev") {
    	$self->cmd("Removing cached frontpage files",
    	    [
    		["echo $sudo | sudo -S rm -rf $html/img-fp/$db"],
		["echo $sudo | sudo -S rm $html/img-fp/$livedb"]
    	   ]
    	);
    
    	$self->cmd("Creating table of contents",
    	    [
    		["mkdir -p $website_dir/html/img-fp"],
		["$website_dir/cgi-bin/toc DB=$db"],
    		["$website_dir/cgi-bin/doi_toc DB=$db"],
		["ln -s $website_dir/html/img-fp/$db $website_dir/html/img-fp/$livedb"],
    		["echo $sudo | sudo -S chown -R \${USER}:www-data $website_dir/html/img-fp/$db/toc"],
    		["echo $sudo | sudo -S chown -R \${USER}:www-data $website_dir/html/img-fp/$db/doi_toc"]
    	    ]
    	);
    } elsif ($gkbdir eq "gkb") {
        $self->cmd("Creating table of contents",
	    [
		["echo $sudo | sudo -S rm -rf $website_dir/html/img-fp/$livedb"],
        	["$website_dir/cgi-bin/toc DB=$livedb"],
            	["$website_dir/cgi-bin/doi_toc DB=$livedb"],
		["echo $sudo | sudo -S chown -R \${USER}:www-data $website_dir/html/img-fp/$livedb/*toc"]
		    
            ],
	    {"ssh" => $ssh}
        );
    }
};

1;
