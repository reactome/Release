package GKB::Release::Steps::UpdateGkCurrent;

use GKB::Release::Utils;
use GKB::Release::Config;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => "gkbdev" );
has '+passwords' => ( default => sub { ['mysql'] } );
has '+directory' => ( default => "$release/update_gk_current" );
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

    cmd("Dumping $db",[["mysqldump -u$user -p$pass $db > $db.dump"]]); 
 
    my @args = ("-db", 'gk_current', "-source", "$db.dump");
    cmd("Populating gk_current with $db.dump",[["perl restore_database.pl @args > gk_current.out 2> gk_current.err"]]);
};

1;

