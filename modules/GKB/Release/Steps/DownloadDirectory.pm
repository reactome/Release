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
    }
};

1;
