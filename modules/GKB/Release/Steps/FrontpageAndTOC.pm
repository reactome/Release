package GKB::Release::Steps::FrontpageAndTOC;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

#has '+gkb' => ();
has '+passwords' => ( default => sub { ['sudo'] } );
has '+directory' => ( default => "$release/toc" );
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
    	cmd("Removing cached frontpage files",
    	    [
    		["echo $sudo | sudo rm -rf $html/img-fp"] 
    	   ]
    	);
	
	
    
    	cmd("Creating table of contents",
    	    [
    		["$website_dir/cgi-bin/toc DB=$db"],
    		["$website_dir/cgi-bin/doi_toc DB=$db"],
    		["echo $sudo | sudo -S chown -R \${USER}:www-data $website_dir/html/img-fp/$db/toc"],
    		["echo $sudo | sudo -S chown -R \${USER}:www-data $website_dir/html/img-fp/$db/doi_toc"]
    	    ]
    	);
    } elsif ($gkbdir eq "gkb") {
        cmd("Creating table of contents",
	    [
		["echo $sudo | sudo -S rm -rf $website_dir/html/img-fp/gk_current"],
        	["$website_dir/cgi-bin/toc DB=$livedb"],
            	["$website_dir/cgi-bin/doi_toc DB=$livedb"],
		["echo $sudo | sudo -S chown -R \${USER}:www-data $website_dir/html/img-fp/$livedb/*toc"]
		    
            ],
	    {"ssh" => $ssh}
        );
    }
};

1;
