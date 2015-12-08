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
					return {
						'to' => 'curation',
						'subject' => $self->name,
						'body' => '',
						'attachment' => ''
					};
				}
);
						
override 'run_commands' => sub {
	my ($self, $gkbdir) = @_;

    $self->cmd("Installing $db for the $self->{host} server",[["echo $sudo | sudo -S ./install_release_db.sh $db > $self->{name}.out 2> $self->{name}.err"]]);
};

1;
