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
    
    $self->cmd("saving stable ids to history database",
        [
            ["perl save_stable_id_history.pl -db $db -sdb stable_identifiers -user $user -pass $pass -release $version " .
	     " > save_stable_id_history_$version.out 2>&1"]
        ]
	);

    $self->cmd("Mapping old ortho ST_IDs back to current set",
	       [
            ["perl retrofit_orthos.pl -dbname $db -user $user -pass > retrofit_orths.pl_$version.out 2>&1"]
        ]
        );

    $self->cmd("Backing up $db and stable_identifiers databases",
	[
	    ["mysqldump --opt -u$user -p$pass $db > $db.afterOrthoStableIDs.dump"],
	    ["mysqldump --opt -u$user -p$pass stable_identifiers > stable_identifiers_$version.afterOrthoStableIDs.dump"]
	]
    );
};

override 'post_step_tests' => sub {
    my ($self) = shift;
    
    my @errors = super();
    push @errors, _check_stable_id_count($db, "test_reactome_$prevver");
    
    return @errors;
};

sub _check_stable_id_count {
    my $current_db = shift;
    my $previous_db = shift;

    my $current_stable_id_count = scalar @{get_dba($current_db)->stableIdentifier};
    my $previous_stable_id_count = scalar @{get_dba($previous_db)->stableIdentifier};
    
    my $stable_id_count_change = $current_stable_id_count - $previous_stable_id_count;
    return "Stable id count has gone down from $current_stable_id_count for version $version " .
        " from $previous_stable_id_count for version $prevver" if $stable_id_count_change < 0;
}
