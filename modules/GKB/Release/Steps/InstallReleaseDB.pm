package GKB::Release::Steps::InstallReleaseDB;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => "gkbdev" );
has '+passwords' => ( default => sub { ['sudo'] } );
has '+directory' => ( default => "$release/install_release_db" );
has '+mail' => ( default => sub { 
					my $self = shift;
                    my $host = $self->host;
					return {
						'to' => '',
						'subject' => $self->name,
						'body' => "$db installed on $host server",
						'attachment' => 'install_release_db.out'
					};
				}
);
						
override 'run_commands' => sub {
	my ($self, $gkbdir) = @_;

    $self->cmd("Installing $db for the $self->{host} server",[
        ["echo $sudo | sudo -S ./install_release_db.sh $db > $self->{name}.out 2> $self->{name}.err"],
        ["grep -v '^\[sudo\] password for $user:\$' $self->{name}.err | cat > $self->{name}.err"]
    ]);
};

1;
