package GKB::InstanceCache;

# Copyright 2002 Imre Vastrik <vastrik@ebi.ac.uk>

use strict;
use vars qw(@ISA);
use Bio::Root::Root;
@ISA = qw(Bio::Root::Root);


sub new {
  my($pkg, @args) = @_;
  my $self = bless {}, $pkg;
  my ($hash,$debug) = $self->_rearrange([qw(
					    HASH
					    DEBUG
					    )],@args);
  $self->{'instance_hash_ref'} = $hash || {};
  $self->debug($debug);
  return $self;
}

### Function: store
# Stores instance in cache.
# Arguments:
# 1) string/number key. Usually db_id or id.
# 2) object value.
###
sub store {
    my $self = shift;
    my $key = shift;
    if (@_){
	my $val = shift;
	$self->{'instance_hash_ref'}->{$key} = $val;
	$self->debug && print join("\t","Cached",$key,($self->{'instance_hash_ref'}->{$key}->class || 'undef'), $val), "\n";
	#print "<PRE>$self\t$key\t$val\t", $val->class_and_id, "</PRE>\n";
    }
    return $self->{'instance_hash_ref'}->{$key};
}

### Function: fetch
# Fetches instance from cache.
# Argument:
# 1) string/number key.
# Returns:
# value (object) for the given key or undef if teh key does not exist or there's no value.
###
sub fetch {
    my ($self, $key) = @_;
    if (my $val = $self->{'instance_hash_ref'}->{$key}) {
	$self->debug && print join("\t","Found in cache",$key,($val->class || 'undef'), $val), "\n";
	return $val;
#	return $self->{'instance_hash_ref'}->{$key};
    } else {
	return undef;
    }
}

### Function: keys
# Returns the list of all keys in the cache.
###
sub keys {
    my ($self) = @_;
    return keys %{$self->{'instance_hash_ref'}};
}

sub values {
    return CORE::values %{$_[0]->{'instance_hash_ref'}};
}

sub key_value_array {
    return %{$_[0]->{'instance_hash_ref'}};
}

### Function: instances
# Returns the list of all values (instances) in the cache.
###
sub instances {
    my ($self) = @_;
    no strict 'refs';
    my %seen;
    my @out = grep {! $seen{$_}++} CORE::values(%{$self->{'instance_hash_ref'}});
    use strict 'refs';
    return @out;
}

### Function: empty
# Empties the cache.
# NOTE: This function begs for a proper implementation which would
# destroy circular references.
###
sub empty {
    my ($self) = @_;
    #delete $self->{'instance_hash_ref'};
    $self->{'instance_hash_ref'} = {};
}

### Function: clean
# Empties the cache and destroys all the instances. Call this method only
# when you are done with your script to clean things up. If you hold a
# reference to an Instance in cache the reference will be unusable after
# the clean. So, don't call it too early.
###
sub clean {
    my ($self) = @_;
    foreach my $i (CORE::values %{$_[0]->{'instance_hash_ref'}}) {
	$i->DESTROY;
    }
    $self->{'instance_hash_ref'} = {};
}

sub delete {
    my ($self, $key) = @_;
    delete $self->{'instance_hash_ref'}->{$key};
}

sub delete_value {
    my ($self, $value) = @_;
    my @to_be_deleted;
    while (my ($key,$val) = (%{$self->{'instance_hash_ref'}})) {
	push @to_be_deleted, $key if ($val == $value);
    }
    map {delete $self->{'instance_hash_ref'}->{$_}} @to_be_deleted;
}

sub debug {
    my $self = shift;
    if (@_) {
	$self->{'debug'} = shift;
    }
    return $self->{'debug'};
}

sub unset_attribute {
    my ($self,$att) = @_;
    foreach my $i ($self->instances) {
	$i->attribute_value($att,undef);
    }
}

1;
