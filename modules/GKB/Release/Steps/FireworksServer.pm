package GKB::Release::Steps::FireworksServer;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => "gkbdev" );
has '+passwords' => ( default => sub { ['mysql'] } );
has '+directory' => ( default => "$release/fireworks_server" );
has '+mail' => ( default => sub { 
					my $self = shift;
					return {
						'to' => '',
						'subject' => $self->name,
						'body' => "",
						'attachment' => ''
					};
				}
);

override 'run_commands' => sub {
    my ($self, $gkbdir) = @_;

    my $host = $self->host;

    my @args = ("-db", $db, "-host", $host, "-user", $user, "-pass", $pass, "-r", $version);
    $self->cmd("Running fireworks JSON generator",[["perl fireworks.pl @args > fireworks.out 2> fireworks.err"]]);
};

1;
