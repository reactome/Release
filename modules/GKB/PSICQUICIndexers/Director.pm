=head1 NAME

GKB::PSICQUICIndexers::Director

=head1 SYNOPSIS

=head1 DESCRIPTION

The main purpose of this class is to build the indexes for all
of the Reactome PSICQUIC servers.  It uses objects subclassed
from the Builder base class to do this, by running their "buildPart"
methods.

Use the "add_builder" method to tell it which Builder objects to
use.

Use the "construct" method to run the "buildPart" of the Builder
objects.

It has a number of setter methods that allow you to pass things
parameters onto the individual Builder objects.

It forms part of the Builder design pattern.

=head1 SEE ALSO

GKB::PSICQUICIndexers::Builder


=head1 AUTHOR

David Croft E<lt>croft@ebi.ac.ukE<gt>

Copyright (c) 2012 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

package GKB::PSICQUICIndexers::Director;

use GKB::PSICQUICIndexers::BuilderFactory;
use strict;
use vars qw(@ISA $AUTOLOAD %ok_field);
use Bio::Root::Root;

@ISA = qw(Bio::Root::Root);

# List the object variables here, so that they can be checked
for my $attr
    (qw(
    builder_factory
    builder_params
    builders
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
    my($pkg) = @_;

    my $self = bless {}, $pkg;
   	
   	my @builders = ();
   	$self->builder_factory(GKB::PSICQUICIndexers::BuilderFactory->new());
   	$self->builder_params(undef);
   	$self->builders(\@builders);
   	
    return $self;
}

# Sets the BuilderParams object that will be used to pass parameters
# from a Director object to individual Builder objects.
sub set_builder_params {
	my ($self, $builder_params) = @_;
	
	$self->builder_params($builder_params);
}

sub add_builder {
    my ($self, $builder_name) = @_;
    
    my $builder = $self->builder_factory->construct($builder_name);
    if (defined $builder) {
    	push(@{$self->builders}, $builder);
    }
}

sub construct {
    my ($self) = @_;
    
    my $builder;
    foreach $builder (@{$self->builders}) {
    	# Pass over parameters to Builder object
    	$builder->set_builder_params($self->builder_params);
    	
    	# Run the linking functionality of the Builder
    	$builder->buildPart();
    	
    	# Do this so that the Perl garbage collector can free up some memory
    	$builder->clear_variables();
    }
    $self->builder_params->close();
    
    # Loop over the builders a second time and report their
    # exit status
    print STDERR "\n\n\n";
    foreach $builder (@{$self->builders}) {
    	$builder->print_termination_status();
    }
}

1;

