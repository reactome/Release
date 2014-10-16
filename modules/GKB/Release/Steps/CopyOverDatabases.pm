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
    	
	# Backup, drop and recreate stable id database
	cmd("Creating stable id database",
	    [
	 	["mysqldump --opt -u $user -p$pass test_reactome_stable_identifiers > test_reactome_stable_identifiers.dump.$version"],
		["mysql -e -u $user -p$pass 'drop database test_reactome_stable_identifiers'"],
		["mysql -e -u $user -p$pass 'create database test_reactome_stable_identifiers'"],
		["mysqldump --opt -h $release_server -u $user -p$pass test_reactome_stable_identifiers | mysql -u $user -p$pass test_reactome_stable_identifiers"]
	    ],
	    {"ssh" => $live_server}
	);
        
        # Create test_reactome_xx and test_reactome_xx_dn databases
        foreach my $database ($db, "$db\_dn") {
            cmd("Creating $database database",
		[
		    ["mysql -e -u $user -p$pass 'create database $database'"],    
           	    ["mysqldump --opt -h $release_server -u $user -p$pass $database > $database.dump"],  
        	    ["cat $database.dump | mysql -u $user -p$pass $database"]
        	],
		{"ssh" => $live_server}
            );
        }
    	
    	# Drop and recreate live databases
    	my %databases = (reactome_stable_identifiers => "test_reactome_stable_identifiers", gk_current => $db, gk_current_dn => "$db\_dn");
	while (my ($database, $source) = each %databases) {
	    cmd("Creating live $database database",
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
