package GKB::Release::Steps::UpdateDOIs;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => "gkbdev" );
has '+passwords' => ( default => sub { ['mysql'] } );
# has '+user_input' => ( default => sub { {'live_run' => {'query' => 'Is this DOI update a live run -- i.e. databases to be changed? (y/n):'}}});
has '+user_input' => (default => sub {
                    {
                        'props' => {'query' => 'Have you updated update_dois/config.properties? '},
                    }
});
has '+directory' => ( default => "$release/update_dois" );
has '+mail' => ( default => sub {
					my $self = shift;
					return {
						'to' => 'curation',
						'subject' => $self->name,
						'body' => "",
						'attachment' => $self->directory . '/update_dois.log'
					};
				}
);

override 'run_commands' => sub {
    my ($self, $gkbdir) = @_;

    my $host = $self->host;
    $self->cmd("Backing up databases $db and $gkcentral",
        [
            ["mysqldump -u$user -p$pass -h$host --lock-tables=FALSE $db > $db.dump"],
            ["mysqldump -u$user -p$pass -h$gkcentral_host --lock-tables=FALSE $gkcentral > $gkcentral.dump"]
        ]
    );
    # databaseTR indicates Test Reactome db, and since the format is test_reactome_XX, this needs to be updated wirth release number for each release
    # authorId values refer to various user ID's attributed to you in Reactome's databases.
    if ($self->user_input->{'props'}->{'response'} !~ /^y/i) {
      die "Please update the databaseTR, and authorID fields in update_dois/config.properties before running.\n";
    }
    # my $live_run = $self->user_input->{'live_run'}->{'response'} =~ /^y/i ? '-live_run' : '';
    # my @args = ("-user", $user, "-pass", $pass, "-release_db", $db, "-release_db_host", $host, "-curator_db", $gkcentral, "-curator_db_host", $gkcentral_host, $live_run);
		$self->cmd("Running script to update DOIs for $db and $gkcentral",[["perl setup_update_dois.pl > setup_update_dois.out 2>> setup_update_dois.err"]]);
};

1;
