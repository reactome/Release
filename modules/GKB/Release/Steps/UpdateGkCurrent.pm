package GKB::Release::Steps::UpdateGkCurrent;

use GKB::Release::Utils;
use GKB::Release::Config;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => "gkbdev" );
has '+passwords' => ( default => sub { ['mysql'] } );
has '+directory' => ( default => "$release/update_gk_current" );
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

    $self->cmd("Creating source file from $db on localhost",[["mysqldump -u$user -p$pass $db > $db.dump"]]);
 
    my @args = ("-db", 'gk_current', "-source", "$db.dump");
    $self->cmd("Populating gk_current with $db.dump",[["perl restore_database.pl @args >> gk_current.out 2>> gk_current.err"]]);
    
    $self->cmd("Backing up gk_current hosted on $dev_server",[["mysqldump -u$user -p$pass -h $dev_server gk_current > gk_current.dev.dump"]]);
    $self->cmd("Populating gk_current on $dev_server with $db.dump",[["perl restore_database.pl @args -host $dev_server >> gk_current.out 2>> gk_current.err"]]);
};

override 'post_step_tests' => sub {
    my ($self) = shift;
    
    my @errors = super();
    
    my $error = _check_database_object_counts_are_equal(get_dba($db), get_dba('gk_current'));
    push @errors, $error if $error;
    $error = _check_database_object_counts_are_equal(get_dba($db), get_dba('gk_current', $dev_server));
    push @errors, $error if $error;
    
    return @errors;
};

sub _check_database_object_counts_are_equal {
    my $first_dba = shift;
    my $second_dba = shift;

    my $first_count = $first_dba->class_instance_count('DatabaseObject');
    my $second_count = $second_dba->class_instance_count('DatabaseObject');
    
    if ($first_count == $second_count) {
        return;
    }
    
    return "Database object count for " . $first_dba->{'db_name'} . " ($first_count) is different from " . $second_dba->{'db_name'} . " ($second_count)";
}

1;

