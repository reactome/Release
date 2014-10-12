package GKB::Release::Steps::GenerateStableIds;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => "gkbdev" );
has '+passwords' => ( default => sub { ['mysql'] } );
has '+directory' => ( default => "$release/generate_stable_ids" );
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

	my $host = $self->host;
 	
   	cmd("Backing up databases and generating stable ids",
		[
	    	["mysqldump --opt -u $user -h $host -p$pass --lock-tables=FALSE $slicedb > $slicedb.dump"],
    		["mysqldump --opt -u $user -h $host -p$pass --lock-tables=FALSE test_reactome_stable_identifiers > test_reactome_stable_identifiers_$version.dump"],
    		["mysqldump --opt -u $user -h $gkcentral_host -p$pass --lock-tables=FALSE $gkcentral > $gkcentral\_$version.dump"],
    		["./generate_stable_ids.sh -f -ghost $gkcentral_host -host $host -user $user -pass $pass -port 3306 -prnum $prevver -cdbname $slicedb -crdbname $db -crnum $version -idbname test_reactome_stable_identifiers -gdbname $gkcentral -nullify > generate_stable_ids_$version.out"]
    	]
    );
};    

1;
