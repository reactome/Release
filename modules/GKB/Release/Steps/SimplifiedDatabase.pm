package GKB::Release::Steps::SimplifiedDatabase;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => "gkbdev" );
has '+passwords' => ( default => sub { ['mysql'] } );
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
						
override 'run_commands' => sub {
	my ($self, $gkbdir) = @_;
	
    cmd("Creating simplified database",[["perl simplified_database.pl"]]);
};

1;
