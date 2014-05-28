=head1 NAME

GKB::SOAPServer::Base

=head1 SYNOPSIS

A base class that sets up a SOAP server and provides a couple of utility methods.

=head1 DESCRIPTION

This class is intended only to be a base class, to be extended by more complex
classes, tailored to specific services.  Classes inheriting from this class
*must* pass their wsdl as an argument to this class' constructor.

=head1 SEE ALSO

GKB::SOAPServer::KEGG
GKB::SOAPServer::MIRIAM

=head1 AUTHOR

David Croft E<lt>croft@ebi.ac.ukE<gt>

Copyright (c) 2008 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

package GKB::SOAPServer::Base;

use SOAP::Lite;
use GKB::Config;
use strict;
use vars qw(@ISA $AUTOLOAD %ok_field);
use Bio::Root::Root;

@ISA = qw(Bio::Root::Root);

# List the object variables here, so that they can be checked
for my $attr
    (qw(
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

# Needed by subclasses to gain access to class variables defined in
# this base class.
sub get_ok_field {
	my ($pkg) = @_;
	
	return %ok_field;
}

# Restarts the SOAP connection
sub restart {
	my ($self) = @_;
	
	print STDERR "Base.restart: WARNING - this is an abstract method and should be implemented by you\n";
	exit(1);
}

# Calls the named method on a list of arguments.
# Return value type depends on the method being called.
sub call {
	my ($self, $method, @args) = @_;
	
	print STDERR "Base.restart: WARNING - this is an abstract method and should be implemented by you\n";
	exit(1);
}

1;

