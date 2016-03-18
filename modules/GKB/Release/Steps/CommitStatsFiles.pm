package GKB::Release::Steps::CommitStatsFiles;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => "gkbdev" );
has '+passwords' => ( default => sub { ['mysql'] } );
has '+directory' => ( default => "$release/commit_stats_files" );
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

    $self->cmd("Committing version $version inference statistics files to GitHub", [["perl commit_stats_files.pl -version $version > commit_stats_files.out 2> commit_stats_files.err"]]);
};

1;
