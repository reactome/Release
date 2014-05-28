package GKB::ClipsAdaptor::ToBeUsedWithInstanceExtractor;

use strict;
use vars qw(@ISA);
use GKB::ClipsAdaptor;

@ISA = qw(GKB::ClipsAdaptor);

sub new {
    my ($caller, @args) = @_;
    my $self = $caller->SUPER::new(@args);
    my ($ar) = $self->_rearrange
        (
         [qw(CLASS_X_REVERSE_ATTRIBUTE)],@args
         );
    $self->{'class_x_reverse_attribute_list'} = $ar;
    return $self;
}

sub _store_instance_get_attribute_instances {
    my ($self,$i) = @_;
    $i->{'stored_by_ClipsAdaptor'} && return [];
    my $out = $self->SUPER::_store_instance_get_attribute_instances($i);
    foreach my $ar (@{$self->{'class_x_reverse_attribute_list'}}) {
	if ($i->is_a($ar->[0])) {
	    push @{$out}, @{$i->reverse_attribute_value($ar->[1])};
	}
    }
    return $out;
}

1;
