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
  
    if ($gkbdir eq "gkbdev" || $gkbdir eq "gkb_prod") {
    	#cmd("Removing cached frontpage files",
    	#    [
    	#	["rm -rf $html/img-fp/$db"] 
    	#   ]
    	#) if ($gkbdir eq "gkbdev");
    
    	cmd("Creating table of contents",
    	    [
		#["perl create_frontpage_files.pl DB=$db"],
    		["$website_dir/cgi-bin/toc DB=$db"],
    		["$website_dir/cgi-bin/doi_toc DB=$db"],
    		["echo $sudo | sudo -S chown -R \${USER}:www-data $website_dir/html/img-fp/$db/toc"],
    		["echo $sudo | sudo -S chown -R \${USER}:www-data $website_dir/html/img-fp/$db/doi_toc"]
    	    ],
	    {"ssh" => $ssh}
    	);
    } elsif ($gkbdir eq "gkb_test") {
        #chdir "/usr/local/gkb/website/html/img-fp";
        #cmd("Copying current frontpage to gkb",
        #    [
        #	["echo $sudo | sudo -S rm -rf gk_current"],
        #	["cp -r /usr/local/gkb_prod/website/html/img-fp/$db ."],
        #	["cp -r $db gk_current"],
        #	["find .\/gk_current -name \* -exec /usr/local/gkb/scripts/swop.sh $db gk_current {} \; -print"]
	#    ]
        #);
        
        foreach ($db, "gk_current") {
            cmd("Creating table of contents",
        	[
        	    ["$website_dir/cgi-bin/toc DB=$_"],
            	    ["$website_dir/cgi-bin/doi_toc DB=$_"],
		    ["echo $sudo | sudo -S chown -R \${USER}:www-data $website_dir/html/img-fp/$_/*toc"]
        	],
		{"ssh" => $ssh}
            );
        }
    }
};

1;
