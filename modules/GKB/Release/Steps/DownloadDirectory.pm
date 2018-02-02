package GKB::Release::Steps::DownloadDirectory;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

#has '+gkb' => ();
has '+passwords' => ( default => sub { ['mysql', 'sudo'] } );
has '+directory' => ( default => "$release/download_directory" );
has '+mail' => ( default => sub {
                    my $self = shift;
                    return {
                        'to' => '',
                        'subject' => $self->name,
                        'body' => "",
                        'attachment' => ""
                    };
});

override 'run_commands' => sub {
    my ($self, $gkbdir) = @_;

    my $host = $self->host;

    my $download_dir = $website_static . '/download';

    if ($gkbdir eq "gkbdev") {
        $self->cmd("Creating download directory",[["perl create_download_directory.pl -host $host -port 3306 -user $user -pass $pass -r $version -db $db > create_download_directory.$version.out"]]);
        $self->cmd("Creating BioSystems export file",[["perl create_reactome2biosystems.pl -host $host -port 3306 -user $user -pass $pass -r $version -db $db > create_reactome2biosystems.$version.out"]]);
        $self->cmd("Moving download directory to website folder",
            [
                ["mkdir -p $download_dir/$version"],
                ["mv $version/* $download_dir/$version"],
                ["rmdir $version"]
            ]
        );
    } elsif ($gkbdir eq "gkb") {
        my $archive_live = "$download_dir/archive";

        my @archiving_results = $self->cmd("Archiving version $prevver download directory",
            [
                ["echo $sudo | sudo -S chmod -R g+rw $download_dir/$prevver"],
                ["echo $sudo | sudo -S chown -R www-data:gkb $download_dir/$prevver"],
                ["tar zcvf - $download_dir/$prevver | ssh $live_server 'cat > $archive_live/$prevver.tgz'"]
            ]
        );
        my @copying_dir_results = $self->cmd("Copying current download directory from $host",
            [
                ["echo $sudo | sudo -S chmod -R g+rw $download_dir/$version"],
                ["echo $sudo | sudo -S chown -R www-data:gkb $download_dir/$version"],
                ["rsync -arv $download_dir/$version $live_server:$download_dir"],
                ["ssh -t $live_server 'echo $sudo | sudo -S chown -R www-data:gkb $download_dir'"]
            ]
        );

        if ($self->cmd_successful(\@archiving_results) && $self->cmd_successful(\@copying_dir_results)) {
            $self->cmd("Removing version $prevver download directory from $live_server (archive still available)",
                [["rm -r $download_dir/$prevver"]], {'ssh' => $live_server}
            );
        }

        my $analysis_dir = '/usr/local/reactomes/Reactome/production/AnalysisService';
        my $analysis_input_dir = "$analysis_dir/input";
        my $analysis_temp_dir = "$analysis_dir/temp";
        my $analysis_binary = "analysis_v$version.bin";
        $self->cmd("Copying analysis binary from $host and emptying temp directory",
            [
                ["echo $sudo | sudo -S chmod -R g+rw $analysis_dir"],
                ["echo $sudo | sudo -S chown -R tomcat7:gkb $analysis_dir"],
                ["rsync -arv $analysis_input_dir/$analysis_binary $live_server:$analysis_input_dir"],
                ["ssh -t $live_server 'cd $analysis_input_dir; rm analysis.bin; ln -s $analysis_binary analysis.bin'"],
                ["ssh -t $live_server 'mv --backup=numbered $analysis_temp_dir $analysis_temp_dir.$prevver; mkdir -p $analysis_temp_dir'"],
                ["ssh -t $live_server 'echo $sudo | sudo -S chown -R tomcat7:gkb $analysis_dir'"]
            ]
        );

        my $solr_dir = '/var/solr/data/reactome/data';
        $self->cmd("Copying solr index from $host",
            [
                ["ssh -t $live_server 'echo $sudo | sudo -S mv $solr_dir ./data.solr-$prevver'"],
                ["echo $sudo | sudo -S rsync -arv $solr_dir/ $live_server:$solr_dir"],
                ["ssh -t $live_server 'echo $sudo | sudo -S chown -R solr:solr $solr_dir'"],
                ["ssh -t $live_server 'echo $sudo | sudo -S service solr restart'"]
            ]
        );
    }

    # The command is run on the live server when $gkbdir is gkb rather than gkbdev
    my $ssh_server = ($gkbdir eq "gkb") ? $live_server : undef;
    $self->cmd("Creating link to current download directory",
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
