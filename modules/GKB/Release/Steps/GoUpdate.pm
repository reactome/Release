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
});

override 'run_commands' => sub {
    my ($self) = @_;

    $self->cmd('Setting all GO files to have group permissions',
        [["echo $sudo | sudo -S chgrp $reactome_unix_group *"]]
    );

    $self->cmd('Backing up database',
        [["mysqldump -u$user -p$pass -h$gkcentral_host --lock_tables=FALSE $gkcentral > " .
          "$gkcentral\_after_uniprot_update.dump"]]
    );

    my @args = ('-db', $gkcentral, '-host', $gkcentral_host, '-user', $user, '-pass', $pass);

    $self->cmd('Running GO obsolete update script',
        [["perl go_obo_update.pl @args > go.out 2> go.err"]]
    );
    $self->cmd('Running EC number update script',
        [["perl addEcNumber2Activity_update.pl @args > ec_number.out 2> ec_number.err"]]
    );

    my @classes_to_update = (
        'GO_MolecularFunction',
        'GO_BiologicalProcess',
        'GO_CellularComponent',
        'PhysicalEntity',
        'CatalystActivity',
    );
    foreach my $class (@classes_to_update) {
        $self->cmd("Updating $class display names",
            [["perl updateDisplayName.pl @args -class $class > update_$class.out 2> update_$class.err"]]
        );
    }
};

1;
