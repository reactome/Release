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
});
my %stable_identifier_to_version;

override 'run_commands' => sub {
    my ($self, $gkbdir) = @_;

    #my $host = $self->host;
    # The $self->host will evaluate to the hostname of the machine where the code is run. 
    # This won't be useful inside a docker container, where the database is not hosted in the same
    # container as where the code runs. So we'll use the GK_DB_HOST from the config.
    my $host = $GKB::Config::GK_DB_HOST;

    $self->cmd("Backing up databases",
        [
            ["mysqldump --opt -u $user -h $host -p$pass --lock-tables=FALSE $slicedb > $slicedb.dump"],
            ["mysqldump --opt -u $user -h $gkcentral_host -p$pass --lock-tables=FALSE $gkcentral > ".
             "$gkcentral\_$version\_before_st_id.dump"]
        ]
    );

    $self->cmd("Loading previous slice into $previous_slice_db database",
        [
            ["mysql -e 'drop database if exists $previous_slice_db; create database $previous_slice_db'"],
            ["zcat $archive/$prevver/$slicedb.dump.gz | mysql $previous_slice_db"],
        ]
    );
    
    foreach get_all_stable_identifier_instances(get_dba($slicedb)) {
        $stable_identifier_to_version{$_->identifier->[0]}{'before_update'} = $_->identifierVersion->[0];
    }
    $self->cmd("Updating stable IDs",
        [
            ["perl update_stable_ids.pl -ghost $gkcentral_host -host $host -user $user -pass $pass -sdb $slicedb ".
             "-pdb $previous_slice_db -release $version -gdb $gkcentral > generate_stable_ids_$version.out ".
             "2> generate_stable_ids_$version.err"]
        ]
    );
    foreach get_all_stable_identifier_instances(get_dba($slicedb)) {
        $stable_identifier_to_version{$_->identifier->[0]}{'after_update'} = $_->identifierVersion->[0];
    }

    $self->cmd("Backing up databases",
        [
            ["mysqldump --opt -u $user -h $gkcentral_host -p$pass --lock-tables=FALSE $gkcentral > $gkcentral\_$version\_after_st_id.dump"],
            ["mysqldump --opt -u $user -h $host -p$pass --lock-tables=FALSE $slicedb > $slicedb\_after_st_id.dump"],
        ]
    );
};

## Collect and return problems with pre-requisites of stable identifiers
override 'pre_step_tests' => sub {
    my $self = shift;   

    return get_stable_id_QA_problems_as_list_of_strings(get_dba($slicedb));
};

override 'post_step_tests' => sub {
    my $self = shift;

    my @stable_identifiers_missing_before_update = map {
        "$_ is missing from $slicedb before update"
    } grep { not exists $stable_identifier_to_version{$_}{'before_update'} } keys %stable_identifier_to_version;

    my @stable_identifiers_missing_after_update = map {
        "$_ is missing from $slicedb after update"
    } grep { not exists $stable_identifier_to_version{$_}{'after_update'} } keys %stable_identifier_to_version;

    my @stable_identifiers_with_incorrect_version = map {
       my $version_before_update = $stable_identifier_to_version{$_}{'before_update'};
       my $version_after_update = $stable_identifier_to_version{$_}{'after_update'};

       "Version change for $_ is wrong.  Version before update: $version_before_update.  Version after update: $version_after_update."
    } grep {
       my $version_before_update = $stable_identifier_to_version{$_}{'before_update'};
       my $version_after_update = $stable_identifier_to_version{$_}{'after_update'};

       $version_before_update && $version_after_update && !(($version_after_update - $version_before_update == 0) || ($version_after_update - $version_before_update == 1));
    } keys %stable_identifier_to_version;

    my $stable_id_count_error = _check_stable_id_count($slicedb, $previous_slice_db);

    return grep { defined } (
        super(),
        $self->pre_step_tests(),
        @stable_identifiers_missing_before_update,
        @stable_identifiers_missing_after_update,
        @stable_identifiers_with_incorrect_version,
        $stable_id_count_error
    );
};

sub get_test_slice {
    my $version = shift;
    my $host = shift;
    
    my $snapshot = "test_slice_$version\_snapshot";
    
    return database_exists($snapshot, $host) ? $snapshot : "test_slice_$version";
}

sub _check_stable_id_count {
	my $current_db = shift;
	my $previous_db = shift;

	my $current_stable_id_count = get_dba($current_db)->class_instance_count('StableIdentifier');
	my $previous_stable_id_count = get_dba($previous_db)->class_instance_count('StableIdentifier');
	
	my $stable_id_count_change = $current_stable_id_count - $previous_stable_id_count;

	if ($stable_id_count_change < 0)
	{
		return "Stable id count has gone down from $current_stable_id_count for version $version from $previous_stable_id_count for version $prevver"
	}
	else
	{
		# *EXPLICITLY* return undef because post-step test relies on defined vs. undefined.
		# If you don't return undef, then it seems the empty string '' will be returned and that breaks
		# the logic of the post step test because '' is defined and the test checked for variables that are
		# UNdefined.
		return undef;
	}
}

1;
