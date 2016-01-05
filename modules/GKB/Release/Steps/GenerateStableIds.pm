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
 	
   	$self->cmd("Backing up databases and generating stable ids",
		[
	    	["mysqldump --opt -u $user -h $host -p$pass --lock-tables=FALSE $slicedb > $slicedb.dump"],
    		["mysqldump --opt -u $user -h $gkcentral_host -p$pass --lock-tables=FALSE $gkcentral > $gkcentral\_$version\_before_st_id.dump"],
    		["perl update_stable_ids.pl -ghost $gkcentral_host -user $user -pass $pass -sdb $slicedb -pdb test_slice_$prevver -release $version -gdb $gkcentral > generate_stable_ids_$version.out 2> generate_stable_ids_$version.err"],
            ["mysqldump --opt -u $user -h $gkcentral_host -p$pass --lock-tables=FALSE $gkcentral > $gkcentral\_$version\_after_st_id.dump"]
    	]
    );
};

override 'post_step_tests' => sub {
    my ($self) = shift;
    
    my @errors = super();
    push @errors, _check_stable_id_count($slicedb, "test_slice_$prevver");
    
    return @errors;
}

sub _check_stable_id_count {
    my $current_slice = shift;
    my $previous_slice = shift;
    
    my $current_slice_dba = get_dba($current_slice);
    my $previous_slice_dba = get_dba($previous_slice);
    
    my $current_stable_id_count = scalar @{$current_slice_dba->stableIdentifier};
    my $previous_stable_id_count = scalar @{$previous_slice_dba->stableIdentifier};
    
    my $stable_id_count_change = $current_stable_id_count - $previous_stable_id_count;
    return "Stable id count has gone down from $current_stable_id_count for version $version " .
        " from $previous_stable_id_count for version $prevver" if $stable_id_count_change < 0;
}
1;
