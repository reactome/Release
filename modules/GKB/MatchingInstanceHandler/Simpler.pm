package GKB::MatchingInstanceHandler::Simpler;

use strict;
use vars qw(@ISA);
use GKB::MatchingInstanceHandler;
@ISA = qw(GKB::MatchingInstanceHandler);

sub handle_matching_instances {
    my ($self,$instance,$dba) = @_;
    my $ar = $instance->identical_instances_in_db;
    return unless (@{$ar});
    if (@{$ar} == 1) {
	$instance->db_id($ar->[0]->db_id);
    } else {
	$self->_handle_multiple_matching_instances($instance,$dba);
    }
}

sub _print_log_message {
}

1;
