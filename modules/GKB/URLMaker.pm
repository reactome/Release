package GKB::URLMaker;

use strict;
use Carp;

sub new {
    my($pkg, @args) = @_;
    unless (scalar(@args) % 2 == 0) {
	confess("Odd number of arguments.");
    }
    my $self = bless {}, $pkg;
    for(my $i = 0; $i < @args; $i += 2) {
	if (uc($args[$i]) eq '-SCRIPTNAME') {
	    $self->script_name($args[$i + 1]);
	} else {
	    $args[$i] =~ s/^-//;
	    $self->param($args[$i],$args[$i + 1]);
	}
    }
    return $self;
}

sub urlify {
    my $self = shift;
    my $url;
    unless ($url = $self->_get_cached_url) {
	$url = $self->script_name . '?';
	while (my ($name,$ar) = each %{$self->{'param'}}) {
	    $url .= join('&', map {"$name=$_"} @{$ar}) . '&';
	}
	$self->_set_cached_url($url);
    }
    foreach (@_) {
	ref $_ || confess("Need GKB::Instance, got '$_'.");
	$url .= 'ID=' . $_->db_id . '&';
    }
    return $url;
}

sub urlify_db_ids {
    my $self = shift;
    my $url;
    unless ($url = $self->_get_cached_url) {
	$url = $self->script_name . '?';
	while (my ($name,$ar) = each %{$self->{'param'}}) {
	    $url .= join('&', map {"$name=$_"} @{$ar}) . '&';
	}
	$self->_set_cached_url($url);
    }
    foreach (@_) {
	$url .= 'ID=' . $_ . '&';
    }
    return $url;
}

sub _set_cached_url {
    $_[0]->{'_cached_url'} = $_[1];
}

sub _get_cached_url {
    return $_[0]->{'_cached_url'};
}

sub _delete_cached_url {
    delete $_[0]->{'_cached_url'};
}

sub script_name {
    my $self = shift;
    if (@_) {
	$self->{'script_name'} = shift;
	$self->_delete_cached_url;
    }
    return $self->{'script_name'};
}

sub delete {
    my ($self,$param_name) = @_;
    delete $self->{'param'}->{$param_name};
    $self->_delete_cached_url;
}

sub param {
    my $self = shift;
    if (my $param_name = shift) {
	if (@_) {
	    if (ref($_[0]) && (ref($_[0]) eq 'ARRAY')) {
		@{$self->{'param'}->{$param_name}} = @{$_[0]};
	    } else {
		@{$self->{'param'}->{$param_name}} = @_;
	    }
	    $self->_delete_cached_url;
	} else {
	    return @{$self->{'param'}->{$param_name}};
	}
    } else {
	return keys %{$self->{'param'}};
    }
}

sub clone {
    my $self = shift;
    my ($pkg) = $self =~ /^(\S+)=/;
    my $clone = bless {}, $pkg;
    $clone->{'_cached_url'} = $self->{'_cached_url'};
    $clone->{'script_name'} = $self->{'script_name'};
    while (my ($name,$ar) = each %{$self->{'param'}}) {
	@{$clone->{'param'}->{$name}} = @{$self->{'param'}->{$name}};
    }
    return $clone;
}

1;
