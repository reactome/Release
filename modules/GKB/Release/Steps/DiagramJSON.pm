package GKB::Release::Steps::DiagramJSON;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => "gkbdev" );
has '+passwords' => ( default => sub { ['mysql'] } );
has '+directory' => ( default => "$release/diagram_json" );
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

    my @args = ("-db", $db, "-host", $host, "-user", $user, "-pass", $pass, "-version", $version);
    $self->cmd("Running diagram JSON generator",[["perl diagram_json.pl @args > diagram_json.out 2> diagram_json.err"]]);
};

1;
