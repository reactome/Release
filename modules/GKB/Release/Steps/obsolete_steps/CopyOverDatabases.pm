package GKB::Release::Steps::CopyOverDatabases;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

#has '+gkb' => ();
has '+passwords' => ( default => sub { ['mysql'] } );
has '+directory' => ( default => "$release/copy_over_databases" );
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
    	
    # Drop and recreate live databases
    my %databases = (gk_current => $db, $stable_id_db => $stable_id_db);
    while (my ($database, $source) = each %databases) {
	$self->cmd("Creating live $database database",
	    [
		["mysql -e -u $user -p$pass 'drop database $database'"],
    		["mysql -e -u $user -p$pass 'create database $database'"],
	    	["mysqldump --opt -h $release_server -u $user -p$pass $source > $source.dump"],
    	    	["cat $source.dump | mysql -u $user -p$pass $database"]
    	    ],
	    {"ssh" => $live_server}
    	);
    }
};

1;
