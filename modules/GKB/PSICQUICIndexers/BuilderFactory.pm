=head1 NAME

GKB::PSICQUICIndexers::BuilderFactory

=head1 SYNOPSIS

Creates new instances of class Builder

=head1 DESCRIPTION

The single method, construct, creates new instances of class Builder.

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

package GKB::PSICQUICIndexers::BuilderFactory;

use GKB::Config;
use GKB::PSICQUICIndexers::ReactomeBuilder;
use GKB::PSICQUICIndexers::ReactomeFIBuilder;

use strict;
use vars qw(@ISA $AUTOLOAD %ok_field);
use Bio::Root::Root;
use Log::Log4perl qw/get_logger/;
Log::Log4perl->init(\$LOG_CONF);

@ISA = qw(Bio::Root::Root);

# Maps various name variants onto plausible Builder subclasses
my %builder_map = (
	'ReactomeBuilder' => 'GKB::PSICQUICIndexers::ReactomeBuilder',
	'ReactomeFIBuilder' => 'GKB::PSICQUICIndexers::ReactomeFIBuilder',
);

# List the object variables here, so that they can be checked
for my $attr
    (qw(
    instance_edit
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
   	
    return $self;
}

# Given the name of a Builder subclass, create an object
# of that subclass and return it.  Various possibilities
# for names are available, e.g. the class name or the
# name of the script that it replaces.
sub construct {
    my ($self, $name) = @_;

    my $logger = get_logger(__PACKAGE__);

    if (!(defined $name)) {
	$logger->warn("BuilderFactory.construct: WARNING - name is undef, aborting!");
	return undef;
    }

    my $builder = undef;
    my $class = $builder_map{$name};
    eval {
	if (defined $class) {
	    $logger->info("BuilderFactory.construct: class=$class");
	    $builder = $class->new();
	} else {
	    # Assume a valid class name has been specified and keep fingers crossed
	    $builder = $name->new();
	}
	
	$logger->info("BuilderFactory.construct: successfully created an object for $name");
    };

    if (!(defined $builder)) {
	$logger->warn("BuilderFactory.construct: WARNING - could not find a Builder subclass corresponding to $name");
    }

    return $builder;
}

1;

