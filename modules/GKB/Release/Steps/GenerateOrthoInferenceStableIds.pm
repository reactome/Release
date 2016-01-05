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
    
    $self->cmd("Backing up $db and stable_identifiers databases",
    	[
            ["mysqldump --opt -u$user -p$pass $db > $db.beforeOrthoStableIDs.dump"],
    	    ["mysqldump --opt -u$user -p$pass stable_identifiers > stable_identifiers_$version.beforeOrthoStableIDs.dump"]
    	]
    );
    
    $self->cmd("Generating stable ids for orthoinferences",
    	[
    	    ["perl add_ortho_stable_ids.pl -user $user -pass $pass -db $db -slice_db $slicedb -release_num $version " .
             "-previous_db test_reactome_$prevver -stable_db stable_identifiers > generate_stable_ids_$version.ortho.out 2>&1"]
    	]
    );
    
    $self->cmd("Backing up $db and stable_identifiers databases",
	[
	    ["mysqldump --opt -u$user -p$pass $db > $db.afterOrthoStableIDs.dump"],
	    ["mysqldump --opt -u$user -p$pass stable_identifiers > stable_identifiers_$version.afterOrthoStableIDs.dump"]
	]
    );
};
