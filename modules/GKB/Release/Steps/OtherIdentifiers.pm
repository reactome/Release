package GKB::Release::Steps::OtherIdentifiers;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => "gkbdev" );
has '+passwords' => ( default => sub { ['mysql'] } );
has '+directory' => ( default => "$release/other_identifiers" );
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
    
    $self->cmd("Backing up database",[["mysqldump -u$user -p$pass $db > $db.before_other_identifiers.dump"]]);
    $self->cmd("Adding other identifiers",[["perl add_identifiers_from_mart.pl -user $user -pass $pass -db $db > other_identifiers.$version.out"]]);
};

1;
