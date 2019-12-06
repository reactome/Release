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
        'body' => '',
        'attachment' => $self->directory . '/go.wiki'
    };
});

override 'run_commands' => sub {
    my ($self) = @_;

    $self->cmd('Setting all GO files to have group permissions',
        [["echo $sudo | sudo -S chgrp $reactome_unix_group *"]]
    );

    $self->cmd('Backing up database',[
        [
            "mysqldump -u$user -p$pass -h$gkcentral_host --lock_tables=FALSE $gkcentral " .
            "> $gkcentral\_before_go_update.dump"
        ]
    ]);
    $self->cmd('Running GO Update script',
        [
            ["perl run_GO_Update.pl $version  > go.out 2> go.err"]
        ]
    );
    $self->cmd('Backing up database',[
        [
            "mysqldump -u$user -p$pass -h$gkcentral_host --lock_tables=FALSE $gkcentral " .
            "> $gkcentral\_after_go_update.dump"
        ]
    ]);
};

1;
