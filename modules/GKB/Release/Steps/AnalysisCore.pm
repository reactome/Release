package GKB::Release::Steps::AnalysisCore;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => "gkbdev" );
has '+passwords' => ( default => sub { ['mysql'] } );
has '+directory' => ( default => "$release/analysis_core" );
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
    $self->cmd("Running analysis core",[["perl analysis_core.pl @args > analysis.out 2> analysis.err"]]);
};

1;
