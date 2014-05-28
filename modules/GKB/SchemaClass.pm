package GKB::SchemaClass;

use strict;
use vars qw(@ISA %properties);
use Bio::Root::Root;
use GKB::Ontology;
use GKB::SchemaClass;
@ISA = qw(Bio::Root::Root);


my @properties = qw(
		    name
		    super_classes
		    attributes
		    abstract
		    subclasses
		    );

foreach (@properties) {
    $properties{$_}++;
}

sub new {
    my ($pkg) = @_;
    my $self = bless {}, $pkg;
    return $self;
}

sub list_properties {
    return @properties;
}

sub properties {
    return \@properties;
}

sub id {
    my $self = shift;
    if (@_) {
	$self->{'id'} = shift;
    }
    $self->{'id'};
}

sub name {
    return $_[0]->{'name'}->[0];
}

sub add_property_value {
    my ($self,$property,$value) = @_;
    $properties{$property} || $self->throw("Invalid property '$property'");
    unless ($self->{$property}) {
	$self->{$property} = [];
    }
    push @{$self->{$property}}, $value;
}

sub get_property_value {
    my ($self,$property) = @_;
    $properties{$property} || $self->throw("Invalid property '$property'");
    return $self->{$property} || [];
}

sub set_property_value {
    my ($self,$property,$value) = @_;
    $properties{$property} || $self->throw("Invalid property '$property'");
    return $self->{$property} = [$value];
}

sub add_property_value_by_rank {
    my ($self,$property,$value,$pos) = @_;
    $properties{$property} || $self->throw("Invalid property '$property'");
    $self->{$property}->[$pos] = $value;
}

sub get_property_value_by_name {
    my ($self,$property,$name) = @_;
    $properties{$property} || $self->throw("Invalid property '$property'");
#    return $self->{'keyed'}->{$property}->{$name} || [];
    my @out;
    foreach my $val (@{$properties{$property}}) {
	if (ref $val && $val->can('name')) {
	    push @out, $val if ($val->name eq $name);
	}
    }
    return \@out;
}

#sub set_property_value_by_name {
#    my ($self,$property,$value,$name) = @_;
#    $properties{$property} || $self->throw("Invalid property '$property'");
#    $self->{'keyed'}->{$property}->{$name} = $value;
#}

1;
