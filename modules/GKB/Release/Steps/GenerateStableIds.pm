package GKB::Release::Steps::GenerateStableIds;

use GKB::NewStableIdentifiers;

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
my %stable_identifier_to_version;

override 'run_commands' => sub {
	my ($self, $gkbdir) = @_;

	my $host = $self->host;
 	
   	$self->cmd("Backing up databases",
		[
		    ["mysqldump --opt -u $user -h $host -p$pass --lock-tables=FALSE $slicedb > $slicedb.dump"],
		    ["mysqldump --opt -u $user -h $gkcentral_host -p$pass --lock-tables=FALSE $gkcentral > ".
		     "$gkcentral\_$version\_before_st_id.dump"]
		]
	);
    
	$stable_identifier_to_version{$_->identifier->[0]}{'before_update'} = $_->identifierVersion->[0] foreach get_all_stable_identifier_instances(get_dba($slice_db));
    $self->cmd("Updating stable IDs",
		[
		    ["perl update_stable_ids.pl -ghost $gkcentral_host -user $user -pass $pass -sdb $slicedb ".
		     "-pdb test_slice_$prevver -release $version -gdb $gkcentral > generate_stable_ids_$version.out ".
		     "2> generate_stable_ids_$version.err"]
		]
	);
    $stable_identifier_to_version{$_->identifier->[0]}{'after_update'} = $_->identifierVersion->[0] foreach get_all_stable_identifier_instances(get_dba($slice_db));
    
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
    
    my @instances_missing_stable_identifier = map { $_->displayName . ' (' .  $_->db_id . ') is missing a stable identifier' } get_all_instances_missing_stable_identifiers(get_dba($slicedb));
    my @instances_with_multiple_stable_identifiers = map { $_->displayName . ' (' . $_->db_id . ') has more than one stable identifier' } get_all_instances_with_multiple_stable_identifiers(get_dba($slicedb));
    my @instances_with_incorrect_stable_identifier = map {
        $_->displayName . ' (' . $_->db_id . ') has an incorrect stable identifier: ' . $_->stableIdentifier->[0]->identifier->[0]
    } grep { !stable_identifier_species_prefix_is_correct($_) || !stable_identifier_numeric_component_is_correct($_) } get_all_instances_with_stable_identifiers(get_dba($slicedb));
    
    my @duplicated_stable_identifiers = map { "$_ used by more than one stable identifier instance" } get_duplicated_stable_identifiers(get_dba($slicedb));
    my @stable_identifiers_without_referrers = map { $_->identifier->[0] . ' has no instances referring to it' } get_stable_identifier_instances_without_referrers(get_dba($slicedb));
    my @stable_identifiers_with_multiple_referrers = map { $_->identifier->[0] . ' has multiple instances referring to it' } get_stable_identifier_instances_with_multiple_referrers(get_dba($slicedb));
    
    return grep { defined } (
        @instances_missing_stable_identifier,
        @instances_with_multiple_stable_identifiers,
        @instances_with_incorrect_stable_identifier,
        @duplicated_stable_identifiers,
        @stable_identifiers_without_referrers,
        @stable_identifiers_with_multiple_referrers
    );
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
