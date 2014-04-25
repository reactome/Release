package GKB::MatchingInstanceHandler;

use strict;
use vars qw(@ISA $AUTOLOAD %ok_field);
use Bio::Root::IO;
@ISA = qw(Bio::Root::IO);

for my $attr
    (qw(
	instance_cache
	tmp_db_name
	tmp_dba
	cgi
	sloppy
	webutils
	) ) { $ok_field{$attr}++; }

sub AUTOLOAD {
    my $self = shift;
    my $attr = $AUTOLOAD;
    $attr =~ s/.*:://;
    return unless $attr =~ /[^A-Z]/;  # skip DESTROY and all-cap methods
    $self->throw("invalid attribute method: ->$attr()") unless $ok_field{$attr};
    $self->{$attr} = shift if @_;
    return $self->{$attr};
} 

sub new {
    my($caller,@args) = @_;
    my $self = $caller->SUPER::new(@args);
    my ($cache,
	$cgi,
	$webutils
	) = $self->_rearrange([qw(
				  CACHE
				  CGI
				  WEBUTILS
				  )],@args);
    $cache && $self->instance_cache($cache);
    $cgi && $self->cgi($cgi);
    $webutils && $self->webutils($webutils);
    return $self;
}

sub debug {
    my $self = shift;
    if (@_) {
	$self->{'debug'} = shift;
    }
    return $self->{'debug'};
}

sub handle_matching_instances {
    my ($self,$instance,$dba) = @_;
    my $ar = $instance->identical_instances_in_db;
    return unless (@{$ar});
    if (@{$ar} == 1) {
	$self->_handle_single_matching_instance($instance,$dba);
    } else {
	$self->_handle_multiple_matching_instances($instance,$dba);
    }
}

sub _handle_single_matching_instance {
    my ($self,$instance,$dba) = @_;
    my $ar = $instance->identical_instances_in_db;
    $self->throw($self->_message($instance,$ar)) unless (-t STDIN && -t STDOUT);
    print $self->_message($instance,$ar);
    $| = 1;
    while (1) {
#	print "Enter nothing (i.e. just click Return) to store the current instance as this instance, 'n' to store instance as new, '>' to merge current instance to this instance, '<' to merge this instance to the current instance: ";
	print "Enter nothing (i.e. just click Return) to store the current instance as this instance, 'n' to store instance as new: ";
	my $str = <STDIN>;
	chomp $str;
	if ($str eq "n") {
	    $dba->store($instance,undef,1);
	    print "Instance stored as: ", $instance->extended_displayName, "\n";
	    $self->_print_log_message('NEW (but has matching instances)', $instance);
	    last;
	} elsif ($str eq '') {
	    $instance->db_id($ar->[0]->db_id);
	    print "Instance stored as: ", $ar->[0]->extended_displayName, "\n";
	    $self->_print_log_message('EXISTS', $instance);
	    last;
#	} elsif ($str =~ /^>/) {
#	    $ar->[0]->inflate;
#	    $ar->[0]->merge($instance);
#	    $dba->update($ar->[0]);
#	    # Have to set db_id to indicate that this instance has been dealt with.
#	    $instance->db_id($ar->[0]->db_id);
#	    print "Instance merged to: ", $ar->[0]->extended_displayName, "\n";
##	    $self->_print_log_message('MERGE TO', $instance);		
#	    last;
#	} elsif ($str =~ /^</) {
#	    $instance->db_id($ar->[0]->db_id);
#	    $instance->merge($ar->[0]);
#	    $dba->update($instance);
#	    print "Instance merged with: ", $instance->extended_displayName, "\n";
##	    $self->_print_log_message('MERGE FROM', $instance);
#	    last;
	}
    }
    print "\n";
}

sub _handle_multiple_matching_instances {
    my ($self,$instance,$dba) = @_;
    my $ar = $instance->identical_instances_in_db;
    $self->throw($self->_message($instance,$ar)) unless (-t STDIN && -t STDOUT);
    print $self->_message($instance,$ar);
    $| = 1;
    while (1) {
#	print "Enter 'db_id' of a stored instance the current instance is equal to, 'n' to store instance as new, '>db_id' to merge current instance to this instance: ";
	print "Enter 'db_id' of a stored instance the current instance is equal to, 'n' to store instance as new: ";
	my $str = <STDIN>;
	chomp $str;
	if ($str eq "n") {
	    $dba->store($instance,undef,1);
	    print "Instance stored as: ", $instance->extended_displayName, "\n";
	    $self->_print_log_message('NEW (but has matching instances)', $instance);
	    last;
	} elsif (($str =~ /(\d+)/) && (my @t = grep {$_->db_id == $1} @{$ar})) {
	    if ($str =~ /^>/) {
		$t[0]->inflate;
		$t[0]->merge($instance);
		$dba->update($t[0]);
		# Have to set db_id to indicate that this instance has been dealt with.
		$instance->db_id($t[0]->db_id);
		print "Instance merged to: ", $t[0]->extended_displayName, "\n";
		$self->_print_log_message('MERGE TO', $instance);
#	    } elsif ($str =~ /^</) {
#		$instance->db_id($t[0]->db_id);
#		$instance->merge($t[0]);
#		$dba->update($instance);
#		print "Instance merged with: ", $instance->extended_displayName, "\n";
##		$self->_print_log_message('MERGE FROM', $instance);
	    } else {
		$instance->db_id($t[0]->db_id);
		print "Instance stored as: ", $t[0]->extended_displayName, "\n";
		$self->_print_log_message('EXISTS', $instance);
	    }
	    last;
	}
    }
    print "\n";
}

sub _message {
    my ($self, $instance, $ar) = @_;
    return "Instance '" . $instance->extended_displayName . "' has " . scalar(@{$ar})
	. " matches in database:\n" . join("\n",(map {$_->extended_displayName} @{$ar})) . "\n";
}

sub _print_log_message {
    my ($self,$str,$i) = @_;
    $self->_print(join("\t", $str, $i->extended_displayName, ($i->id || 'id_not_set')));
    if (my $ar = $i->identical_instances_in_db) {
	$self->_print("\t", join("\t", map {$_->extended_displayName} @{$ar}));
    }
    $self->_print("\n");
}

1;
