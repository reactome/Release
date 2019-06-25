package GKB::Release::Steps::UpdateDOIs;

use GKB::Release::Config;
use GKB::Release::Utils;

use Carp;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => 'gkbdev' );
has '+passwords' => ( default => sub { ['mysql'] } );
# has '+user_input' => ( default => sub { {'live_run' => {'query' => 'Is this DOI update a live run -- i.e. databases to be changed? (y/n):'}}});
has '+user_input' => (default => sub {
    {
        'props' => {'query' => 'Have you updated update-dois/config.properties? '},
    }
});
has '+directory' => ( default => "$release/update_dois" );
has '+mail' => ( default => sub {
    my $self = shift;
    return {
        'to' => 'curation',
        'subject' => $self->name,
        'body' => '',
        'attachment' => $self->directory . '/update_dois.log'
    };
});

override 'run_commands' => sub {
    my ($self, $gkbdir) = @_;

    my $host = $self->host;
    my $gkcentral_user = $GKB::Config::GK_CURATOR_DB_USER;
    my $gkcentral_pass = $GKB::Config::GK_CURATOR_DB_PASS;

    $self->cmd("Backing up databases $db and $gkcentral",
        [
            ["mysqldump -u$user -p$pass -h$host --lock-tables=FALSE $db > $db.dump"],
            ["mysqldump -u$gkcentral_user -p$gkcentral_pass -h$gkcentral_host --lock-tables=FALSE $gkcentral > $gkcentral.dump"]
        ],
        {'passwords' => { 'gkcentral_pass' => \$gkcentral_pass }}
    );
    # databaseTR indicates Test Reactome db, and since the format is test_reactome_XX, this needs to be updated with
    # release number for each release
    # authorId values refer to various user ID's attributed to you in Reactome's databases.
    if ($self->user_input->{'props'}->{'response'} !~ /^y/i) {
        confess "Please update the databaseTR, and authorID fields in update-dois/config.properties before running.\n";
    }

    $self->cmd("Running script to update DOIs for $db and $gkcentral",
        [['perl setup_update_dois.pl > setup_update_dois.out 2>> setup_update_dois.err']]
    );
};

1;
