package GKB::Release::Steps::SimplifiedDatabase;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => "gkbdev" );
has '+passwords' => ( default => sub { [] } );
has '+directory' => ( default => "$release/simplified_database" );
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
has '+user_input' => (default => sub {{'overwrite' => {'query' => 'Overwrite simplified database if it exists (y/n):'}}});

override 'run_commands' => sub {
    my ($self, $gkbdir) = @_;

    my $overwrite = $self->user_input->{'overwrite'}->{'response'} =~ /^y/i ? '-overwrite' : '';
    $self->cmd("Creating simplified database",[["perl simplified_db.pl -source_db $db $overwrite"]]);
};

1;
