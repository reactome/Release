package GKB::Release::Steps::UpdateStableIds;

use GKB::CommonUtils;
use GKB::NewStableIdentifiers;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => "gkbdev" );
has '+passwords' => ( default => sub { ['mysql'] } );
has '+directory' => ( default => "$release/update_stable_ids" );
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
my %stable_identifier_to_version;

override 'run_commands' => sub {
	my ($self, $gkbdir) = @_;

	#my $host = $self->host;
 	my $host = $slice_host;
   	$self->cmd("Backing up databases",
		[
		    ["mysqldump --opt -u $user -h $host -p$pass --lock-tables=FALSE $slicedb > $slicedb.dump"],
		    ["mysqldump --opt -u $user -h $gkcentral_host -p$pass --lock-tables=FALSE $gkcentral > ".
		     "$gkcentral\_$version\_before_st_id.dump"]
		]
	);
    
	$stable_identifier_to_version{$_->identifier->[0]}{'before_update'} = $_->identifierVersion->[0] foreach get_all_stable_identifier_instances(get_dba($slicedb));
    $self->cmd("Updating stable IDs",
		[
		    ["perl update_stable_ids.pl -ghost $gkcentral_host -user $user -pass $pass -sdb $slicedb ".
		     "-pdb test_slice_$prevver -host $slice_host -release $version -gdb $gkcentral > generate_stable_ids_$version.out ".
		     "2> generate_stable_ids_$version.err"]
		]
	);
    $stable_identifier_to_version{$_->identifier->[0]}{'after_update'} = $_->identifierVersion->[0] foreach get_all_stable_identifier_instances(get_dba($slicedb));
    
    $self->cmd("Backing up databases",
        [
		 ["mysqldump --opt -u $user -h $gkcentral_host -p$pass --lock-tables=FALSE $gkcentral > $gkcentral\_$version\_after_st_id.dump"],
		 ["mysqldump --opt -u $user -h $host -p$pass --lock-tables=FALSE $slicedb > $slicedb\_after_st_id.dump"],
		]
    );
};

# Collect and return problems with pre-requisites of stable identifiers
override 'pre_step_tests' => sub {
    my $self = shift;

    return get_stable_id_QA_problems_as_list_of_strings(get_dba($slicedb));
};

override 'post_step_tests' => sub {
    my $self = shift;

    my @stable_identifiers_missing_before_update = map {
        "$_ is missing from $slicedb before update"
    } grep { exists $stable_identifier_to_version{$_}{'before_update'} } keys %stable_identifier_to_version;

    my @stable_identifiers_missing_after_update = map {
        "$_ is missing from $slicedb after update"
    } grep { exists $stable_identifier_to_version{$_}{'after_update'} } keys %stable_identifier_to_version;

    my @stable_identifiers_with_incorrect_version = map {
       my $version_before_update = $stable_identifier_to_version{$_}{'before_update'};
       my $version_after_update = $stable_identifier_to_version{$_}{'after_update'};

       "Version change for $_ is wrong.  Version before update: $version_before_update.  Version after update: $version_after_update."
    } grep {
       my $version_before_update = $stable_identifier_to_version{$_}{'before_update'};
       my $version_after_update = $stable_identifier_to_version{$_}{'after_update'};

       $version_before_update && $version_after_update && !(($version_after_update - $version_before_update == 0) || ($version_after_update - $version_before_update == 1));
    } keys %stable_identifier_to_version;

    my $stable_id_count_error = _check_stable_id_count($slicedb, "test_slice_$prevver");

    return grep { defined } (
        super(),
        $self->pre_step_tests(),
        @stable_identifiers_missing_before_update,
        @stable_identifiers_missing_after_update,
        @stable_identifiers_with_incorrect_version,
        $stable_id_count_error
    );
};

sub _check_stable_id_count {
    my $current_db = shift;
    my $previous_db = shift;

    my $current_stable_id_count = get_dba($current_db)->class_instance_count('StableIdentifier');
    my $previous_stable_id_count = get_dba($previous_db)->class_instance_count('StableIdentifier');

    my $stable_id_count_change = $current_stable_id_count - $previous_stable_id_count;
    return "Stable id count has gone down from $current_stable_id_count for version $version " .
        " from $previous_stable_id_count for version $prevver" if $stable_id_count_change < 0;
}

1;
