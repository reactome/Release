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
    cmd("Backup database", [["mysqldump --opt -u$user -p$pass $db > $db\_before_addlinks.dump"]]);
    my @results = cmd("Running add links script", [["perl add_links.pl -db $db > add_links_$version.out"]]);
    
    my $exit_code = ($results[0])->{'exit_code'};
    # Backup the database or else drop and remake the database if the add links script fails  
    if ($exit_code == 0) {
    	cmd("Backing up database $db",
    		[
    			["mysqldump --opt -u$user -p$pass $db > $db\_after_addlinks.dump"]
    		]
    	);
    }
};

1;
