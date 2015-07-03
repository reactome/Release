package GKB::Release::Steps::SearchIndexer;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => "gkbdev" );
has '+passwords' => ( default => sub { ['mysql'] } );
has '+directory' => ( default => "$release/search_indexer" );
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
    cmd("Running search indexer",[["perl search_indexer.pl @args > search.out 2> search.err"]]);
};

1;
