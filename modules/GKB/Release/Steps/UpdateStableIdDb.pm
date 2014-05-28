package GKB::Release::Steps::UpdateStableIdDb;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

#has '+gkb' => ();
has '+passwords' => ( default => sub { ['mysql'] } );
has '+directory' => ( default => "$release/update_stable_id_db");
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
    
    cmd("Updating stable id database",
	[["mysql -e -p$pass use reactome_stable_identifiers; select * from DbParams; update DbParams set dbName='gk_current' where dbName='$db';"]],
	{"ssh" => $live_server}
    );
};
 
1;
