=head1 NAME

GKB::AddLinks::Director

=head1 SYNOPSIS

=head1 DESCRIPTION

The main purpose of this class is to put links into the Reactome
database, pointing to external databases.  It uses objects subclassed
from the Builder base class to do this, by running their "buildPart"
methods.

Use the "add_builder" method to tell it which Builder objects to
use.

Use the "construct" method to run the "buildPart" of the Builder
objects.

It has a number of setter methods that allow you to pass things
like database parameters onto the individual Builder objects.

It forms part of the Builder design pattern.

=head1 SEE ALSO

GKB::AddLinks::Builder

Subclasses:
GKB::AddLinks::RefseqReferenceDatabaseToReferencePeptideSequence
GKB::AddLinks::UCSCReferenceDatabaseToReferencePeptideSequence
GKB::AddLinks::OmimReferenceDNASequenceToReferencePeptideSequence
GKB::AddLinks::EnsemblGeneToUniprotReferencePeptideSequence
GKB::AddLinks::EntrezGeneToUniprotReferenceDNASequence
GKB::AddLinks::IntActDatabaseIdentifierToComplexOrReactionlikeEvent
GKB::AddLinks::RefseqReferenceRNASequenceToReferencePeptideSequence

=head1 AUTHOR

David Croft E<lt>croft@ebi.ac.ukE<gt>

Copyright (c) 2008 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

package GKB::AddLinks::Director;

use GKB::AddLinks::BuilderFactory;
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
   	$self->builder_factory(GKB::AddLinks::BuilderFactory->new());
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
    my $defined_termination_status_count = 0;
    print STDERR "\n\n\n";
    foreach $builder (@{$self->builders}) {
    	$builder->print_termination_status();
    	if (defined $builder->termination_status) {
    	    $defined_termination_status_count++;
    	}
    }
    if ($defined_termination_status_count > 0) {
        print STDERR "Bad exit status count=$defined_termination_status_count\n";
        exit(1);
    }
}

1;

