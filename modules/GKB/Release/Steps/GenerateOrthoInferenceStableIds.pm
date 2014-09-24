package GKB::Release::Steps::GenerateOrthoInferenceStableIds;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => "gkbdev" );
has '+passwords' => ( default => sub { ['mysql'] } );
has '+directory' => ( default => "$release/generate_stable_ids_orthoinference" );
has '+mail' => ( default => sub { 
					my $self = shift;
					return {
						'to' => 'curation',
						'subject' => $self->name,
						'body' => "",
						'attachment' => ""
					};
				}
);

override 'run_commands' => sub {
    my ($self, $gkbdir) = @_;
    
    cmd("Backing up $db and test_reactome_stable_identifiers databases",
    	[
	    ["mysqldump --opt -u$user -p$pass $db > $db.beforeOrthoStableIDs.dump"],
    	    ["mysqldump --opt -u$user -p$pass test_reactome_stable_identifiers > test_reactome_stable_identifiers_$version.beforeOrthoStableIDs.dump"]
    	]
    );
    
    cmd("Generating stable ids for orthoinferences",
    	[
    	    ["./generate_stable_ids.sh -f  -user $user -pass $pass -port 3306 -prnum $prevver -crdbname $db -crnum $version -idbname test_reactome_stable_identifiers -o  -nullify > generate_stable_ids_$version.ortho.out 2>&1"]
    	]
    );
    
    cmd("Backing up $db and test_reactome_stable_identifiers databases",
	[
	    ["mysqldump --opt -u$user -p$pass $db > $db.afterOrthoStableIDs.dump"],
	    ["mysqldump --opt -u$user -p$pass test_reactome_stable_identifiers > test_reactome_stable_identifiers_$version.afterOrthoStableIDs.dump"]
	]
    );
};
