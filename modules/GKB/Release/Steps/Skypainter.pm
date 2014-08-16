package GKB::Release::Steps::Skypainter;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => "gkbdev" );
has '+passwords' => ( default => sub { ['mysql'] } );
has '+directory' => ( default => "$release/skypainter" );
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
    
    cmd("Backing up database",[["mysqldump -u$user -p$pass $db > $db.before_skypainter"]]);
    cmd("Creating skypainter database",[["perl create_skypainter_db.pl -user $user -pass $pass -db $db > create_skypainter_db.out.$version"]]);
};

1;
