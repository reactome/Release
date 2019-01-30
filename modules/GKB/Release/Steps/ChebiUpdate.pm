package GKB::Release::Steps::ChebiUpdate;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => "gkbdev" );
has '+passwords' => ( default => sub { ['mysql'] } );
has '+directory' => ( default => "$release/chebi_update" );
has '+mail' => ( default => sub {
    my $self = shift;
    return {
        'to' => 'curation',
        'subject' => $self->name,
        'body' => '',
        'attachment' => $self->directory . "/archive/$version/chebi_update_logs_R$version.tgz"
    };
});

override 'run_commands' => sub {
    my ($self) = @_;
    $self->cmd("Backing up database $gkcentral on $gkcentral_host",
        [
            ["mysqldump -u$user -p$pass -h$gkcentral_host --lock_tables=FALSE $gkcentral " . 
	     "> $gkcentral\_before_chebi_update.dump"]
        ]
    );
    $self->cmd('Running ChEBI Update script',
        [["perl run_ChEBI_Update.pl $version  > improve_chebi_ids.out 2> improve_chebi_ids.err"]]
    );
    $self->cmd("Backing up database $gkcentral on $gkcentral_host after ChEBI update",
        [
            ["mysqldump -u$user -p$pass -h$gkcentral_host --lock_tables=FALSE $gkcentral " . 
             "> $gkcentral\_after_chebi_update.dump"]
        ]
    );
};

1;
