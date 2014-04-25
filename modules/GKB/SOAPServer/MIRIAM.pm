=head1 NAME

GKB::SOAPServer::MIRIAM

=head1 SYNOPSIS

This class sets up a SOAP server for accessing the MIRIAM repository of biological
database descriptors.

=head1 DESCRIPTION

Full details of MIRIAM can be read at:

http://www.ebi.ac.uk/compneur-srv/miriam-main/

This class allows you to access the MIRIAM web services, and provides methods
that call identically-named queries on the MIRIAM server.  Note that only a
subset of these queries has been implemented, but it would be easy to
implement more as required.

=head1 SEE ALSO

GKB::InstanceCreator::ReferenceDatabase

=head1 AUTHOR

David Croft E<lt>croft@ebi.ac.ukE<gt>

Copyright (c) 2008 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

package GKB::SOAPServer::MIRIAM;

use GKB::Config;
use strict;
use vars qw(@ISA $AUTOLOAD %ok_field);
use GKB::SOAPServer::WSDL;

@ISA = qw(GKB::SOAPServer::WSDL);

my $wsdl = "http://www.ebi.ac.uk/compneur-srv/miriamws-main/MiriamWebServices?wsdl";

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

   	# Get class variables from superclass and define any new ones
   	# specific to this class.
	%ok_field = $pkg->SUPER::get_ok_field();

   	my $self = $pkg->SUPER::new($wsdl);
   	
    return $self;
}

sub getDataTypesName {
	my ($self) = @_;
	
	return $self->call("getDataTypesName");
}

sub getDataResources {
	my ($self, $service_name) = @_;
	
	return $self->call("getDataResources", $service_name);
}

sub getDataTypeDef {
	my ($self, $service_name) = @_;
	
	return $self->call("getDataTypeDef", $service_name);
}

sub getDataTypePattern {
	my ($self, $service_name) = @_;
	
	return $self->call("getDataTypePattern", $service_name);
}

sub getDataTypeSynonyms {
	my ($self, $service_name) = @_;
	
	return $self->call("getDataTypeSynonyms", $service_name);
}

sub getDataTypeURI {
	my ($self, $service_name) = @_;
	
	return $self->call("getDataTypeURI", $service_name);
}

sub getDataTypeURIs {
	my ($self, $service_name) = @_;
	
	return $self->call("getDataTypeURIs", $service_name);
}

sub getLocations {
	my ($self, $service_name, $id) = @_;
	
	return $self->call("getLocations", $service_name, $id);
}

1;

