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

    $self->cmd("Dumping $db",[["mysqldump -u$user -p$pass $db > $db.dump"]]);
 
    my @args = ("-db", 'gk_current', "-source", "$db.dump");
    $self->cmd("Populating gk_current with $db.dump",[["perl restore_database.pl @args > gk_current.out 2> gk_current.err"]]);
};

override 'post_step_tests' => sub {
    my ($self) = shift;
    
    my @errors = super();
    push @errors, _check_database_object_counts_are_equal($db, 'gk_current');
    
    return @errors;
};

sub _check_database_object_counts_are_equal {
    my $db = shift;
    my $gk_current = shift;

    my $db_object_count = get_dba($db)->class_instance_count('DatabaseObject');
    my $gk_current_object_count = get_dba($gk_current)->class_instance_count('DatabaseObject');
    
    return "Database object count for $gk_current ($gk_current_object_count) is different from $db ($db_object_count)"
    unless ($db_object_count == $gk_current_object_count);
}

1;

