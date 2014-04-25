package GKB::Release::Steps::ExternalResourceLinks;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => "gkbdev" );
has '+passwords' => ( default => sub { ['mysql'] } );
has '+directory' => ( default => "$release/add_links" );
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

    # Backup database and run add links script
    cmd("Backup database", [["mysqldump --opt -p$pass $db > $db\_before_addlinks.dump"]]);
    my $return = cmd("Running add links script", [["perl add_links.pl -db $db > add_links_$version.out"]],1);
    
    # Backup the database or else drop and remake the database if the add links script fails  
    if (!$return) {
    	cmd("Backing up database $db",
    		[
    			["mysqldump --opt -p$pass $db > $db\_after_addlinks.dump"]
    		]
    	);
    } else {
        cmd("Recreating database $db",
        	[
        		["mysql -e drop database $db"],
        		["mysql -e create database $db"],
        		["cat $db\_before_addlinks.dump | mysql $db"]
    		]
    	);
    }
};

1;
