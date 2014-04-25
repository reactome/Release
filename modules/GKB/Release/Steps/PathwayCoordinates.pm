package GKB::Release::Steps::PathwayCoordinates;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => "gkbdev" );
#has '+passwords' => ();
has '+directory' => ( default => "$scripts" );
has '+mail' => ( default => sub { 
					my $self = shift;
					return {
						'to' => '',
						'subject' => $self->name,
						'body' => "",
						'attachment' => ""
					};
				}
);

override 'run_commands' => sub {
	my ($self, $gkbdir) = @_;
    
    cmd("Creating pathway co-ordinates",
    	[
    		["perl create_PathwayCoordinates.pl -db $db"]
		]
	);
};

1;
