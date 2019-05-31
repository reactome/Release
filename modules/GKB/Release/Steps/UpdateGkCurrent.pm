package GKB::Release::Steps::UpdateGkCurrent;

use feature qw/say/;

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
});

override 'run_commands' => sub {
    my ($self, $gkbdir) = @_;

    $self->cmd("Creating source file from $db on localhost",[["mysqldump -u$user -p$pass $db > $db.dump"]]);

    my @args = ("-db", $live_db, "-source", "$db.dump");
    $self->cmd("Populating gk_current with $db.dump",
        [["perl restore_database.pl @args >> gk_current.out 2>> gk_current.err"]]
    );

    # TODO:
    # Need remote access to dev and curator servers through SSH or MySQL to update their DBs automatically
    # Need to add MySQL credentials to Release/Config.pm for the dev and curator servers to access them
    # with credentials that are separate from the release server credentials

    # foreach my $remote_server ($dev_server, $curator_server) {
    #     my @backup_results = $self->cmd("Backing up gk_current hosted on $remote_server",
    #         [["mysqldump -u$user -p$pass -h $remote_server gk_current > gk_current.$remote_server.dump"]]
    #     );

    #     if ($backup_results[0]->{'exit_code'} == 0) {
    #         $self->cmd("Populating gk_current on $remote_server with $db.dump",
    #             [["perl restore_database.pl @args -host $remote_server >> gk_current.out 2>> gk_current.err"]]
    #         );
    #     } else {
    #         say releaselog("Not attempting to update gk_current on $remote_server -- back up failed\n");
    #     }
    # }
};

override 'post_step_tests' => sub {
    my ($self) = shift;

    my @errors = super();

    my @gk_current_databases =
        map { get_dba('current', $_) } ($GKB::Config::GK_DB_HOST,
        # See TODO comment above for what needs to be done to re-enable checking for these hosts
        # $dev_server, $curator_server
        );

    push @errors,
        grep { defined } # Ensures @errors remains free of undefined elements
        map { _check_database_object_counts_are_equal(get_dba($db), $_) }
        @gk_current_databases;

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

    return "Database object count for " .
        $first_dba->{'db_name'} . " ($first_count) is different from " .
        $second_dba->{'db_name'} . " ($second_count)";
}

1;
