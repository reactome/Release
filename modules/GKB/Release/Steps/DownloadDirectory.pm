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
						'to' => '',
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
        cmd("Creating download directory",[["perl create_download_directory.pl -host $host -port 3306 -user $user -pass $pass -r $version -db $db > create_download_directory.$version.out"]]);
        cmd("Creating BioSystems export file",[["perl create_reactome2biosystems.pl -host $host -port 3306 -user $user -pass $pass -r $version -db $db > create_reactome2biosystems.$version.out"]]);
	cmd("Moving download directory to website folder",
	    [
		["mkdir -p $download_dir/$version"],
		["mv $version/* $download_dir/$version"],
		["rmdir $version"]
	    ]);
    } elsif ($gkbdir eq "gkb") {
	my $archive_live = replace_gkb_alias_in_dir("$html/download/archive", 'gkb');
	cmd("Archiving version $prevver download directory", [["tar zcvf - $html/download/$prevver | ssh $live_server 'cat > $archive_live/$prevver.tgz'"]]);
    	cmd("Copying current download directory from $host",[["scp -r $html/download/$version $live_server:$download_dir"]]);
	cmd("Removing version $prevver download directory from $live_server (archive still available)",
	    [["rm -r $download_dir/$prevver"]], {'ssh' => $live_server}
	);
        
        my $analysis_dir = '/usr/local/reactomes/Reactome/production/AnalysisService/input';
        my $analysis_binary = "analysis_v$version.bin";
        cmd("Copying analyis binary from $host",
            [
             ["scp $analysis_binary $live_server:$analysis_dir"],
             ["ssh $live_server 'cd $analysis_dir;rm analysis.bin; ln -s $analysis_binary analysis.bin'"]
            ]
        );
	
	my $solr_dir = '/usr/local/reactomes/Reactome/production/Solr';
        cmd("Copying solr index from $host",
            [
             ["ssh $live_server 'rm -r $solr_dir'"],
             ["scp -rp $solr_dir $live_server:$solr_dir"]
            ]
        );
    }

    
    # The command is run on the live server when $gkbdir is gkb_prod or gkb_test
    my $ssh_server = ($gkbdir eq "gkb") ? $live_server : undef;
    cmd("Creating link to current download directory",
    	[
    		["mv $download_dir/$prevver $download_dir/foo 2> /dev/null"],
    		["rm $download_dir/current"],
    		["mv $download_dir/foo $download_dir/$prevver 2> /dev/null"],
    		["ln -s $download_dir/$version $download_dir/current"]
    	],
	{'ssh' => $ssh_server}
    );
};
    
1;
