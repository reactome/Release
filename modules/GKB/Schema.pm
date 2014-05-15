package GKB::Schema;

use strict;
use vars qw(@ISA %properties);
use Bio::Root::Root;
use GKB::Ontology;
use GKB::SchemaClass;
use GKB::SchemaClassAttribute;
use GKB::InstanceCache;
@ISA = qw(Bio::Root::Root);

my @properties = qw(
		    classes
		    pont_file_content
		    pprj_file_content
		    pins_file_stub
                    attributes
                    _timestamp
		    );

foreach (@properties) {
    $properties{$_}++;
}

sub new {
    my ($pkg,@args) = @_;
    my $self = bless {}, $pkg;
    $self->cache(new GKB::InstanceCache);
    $self->cache->store($self->id,$self);
    return $self;
}

sub _initialize {
    my $self = shift;
    $self->warn("_initialize not quite implemented yet!!!");
    foreach my $thing ($self->cache->instances) {
	if ($thing->isa("GKB::SchemaClass")) {
	    $self->add_property_value('classes', $thing);
	} elsif ($thing->isa("GKB::SchemaClassAttribute")) {
	    my $ar = $thing->get_property_value('class');
	    if (@{$ar}) {
		foreach (@{$ar}) {
		    $_->add_property_value('attributes', $thing);
		}
	    } else {
		$self->add_property_value('attributes', $thing);
	    }
	}
    }
}

sub _assign_subclasses {
    my ($self) = @_;
    foreach my $cls (@{$self->get_property_value('classes')}) {
	foreach my $super (@{$cls->get_property_value('super_classes')}) {
	    $super->add_property_value('subclasses', $cls);
	}
    }
}

sub _find_root_class {
    my ($self) = @_;
    my @roots;
    foreach my $cls (@{$self->get_property_value('classes')}) {
	unless ($cls->get_property_value('super_classes')->[0]) {
	    push @roots, $cls;
	}
    }
    if (! @roots) {
	$self->throw("No root class. How can this happen?");
    } elsif (scalar @roots > 1) {
	$self->throw("Multiple root classes: " . join(", ", @roots) . ". How come?");
    }
    return $roots[0];
}

sub aaa {
    my ($self) = @_;
    my @classes = ($self->_find_root_class);
    my %seen;
    while (my $cls = shift @classes) {
	foreach my $subcls (@{$cls->get_property_value('subclasses')}) {
	    next unless ($seen{$subcls}++);
	    foreach my $supercls (@{$cls->get_property_value('super_classes')}) {
		foreach my $att (@{$supercls->get_property_value('attributes')}) {
		    
		}
	    }
	}
    }
}

sub list_properties {
    return @properties;
}

sub properties {
    return \@properties;
}

sub id {
    return 'Schema';
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
    return $self->{'keyed'}->{$property}->{$name} || [];
}

sub set_property_value_by_name {
    my ($self,$property,$value,$name) = @_;
    $properties{$property} || $self->throw("Invalid property '$property'");
    $self->{'keyed'}->{$property}->{$name} = $value;
}

sub classes {
    my $self = shift;
    if (@_) {
	$self->{'classes'} = shift;
    }
    return $self->{'classes'} || [];
}

sub list_classes {
    return @{$_[0]->classes};
}

sub cache {
    my $self = shift;
    if (@_) {
	$self->{'cache'} = shift;
    }
    return $self->{'cache'};  
}

sub class_attributes {
    my ($self,$cls) = @_;
    $cls || $self->throw("Need class.");
    my $class = $self->cache->fetch($cls) || $self->throw("Unknown class '$cls'");
    return $class->get_property_value('attributes');
}

sub list_class_attribute_names {
    my ($self,$cls) = @_;
    return map {$_->name->[0]} @{$self->class_attributes($cls)};
}

sub schema_item_from_cache_or_new {
    my ($self,$id,$class) = @_;
    my $out;
    unless ($out = $self->cache->fetch($id)) {
	my $pkg = "GKB::$class";
	$out = $pkg->new;
	# tmp measure
	$out->id($id);
	$self->cache->store($id,$out);
    }
    return $out;
}

1;
