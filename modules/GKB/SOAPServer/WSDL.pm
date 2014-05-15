=head1 NAME

GKB::SOAPServer::WSDL

=head1 SYNOPSIS

Sets up a SOAP server based on a WSDL file, and provides a couple of utility methods.

=head1 DESCRIPTION

This class is intended to be extended by more complex classes, tailored
to specific services.

Classes inheriting from this class
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

package GKB::SOAPServer::WSDL;

use SOAP::Lite;
use GKB::Config;
use strict;
use vars qw(@ISA $AUTOLOAD %ok_field);
use GKB::SOAPServer::Base;

@ISA = qw(GKB::SOAPServer::Base);

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
    my($pkg, $wsdl) = @_;

   	# Get class variables from superclass and define any new ones
   	# specific to this class.
	$pkg->get_ok_field();
 	
   	my $self = $pkg->SUPER::new();
   	
   	$self->wsdl($wsdl);
   	$self->consecutive_connect_failure_count(0);
   	$self->restart();
   	
    return $self;
}

# Needed by subclasses to gain access to class variables defined in
# this base class.
sub get_ok_field {
	my ($pkg) = @_;
	
	%ok_field = $pkg->SUPER::get_ok_field();
	$ok_field{"wsdl"}++;
 	$ok_field{"service"}++;
 	$ok_field{"consecutive_connect_failure_count"}++;

	return %ok_field;
}

# Connects to the SOAP service.
sub restart {
	my ($self) = @_;
	
	my $service = undef;
	eval {
		$service = SOAP::Lite->service($self->wsdl);
	};
	
	if (!(defined $service) && $self->consecutive_connect_failure_count>5) {
		print STDERR "WSDL.restart: WARNING - too many connect failures, giving up!!\n";
		return;
	}
	
	my $start_service_counter = 0;
	while (!(defined $service) && $start_service_counter<10) {
		sleep(10);
		eval {
			$service = SOAP::Lite->service($self->wsdl);
		};
		$start_service_counter++;
	}
	if (defined $service) {
		$self->consecutive_connect_failure_count(0);
	} else {
		print STDERR "WSDL.restart: WARNING - could not establish a connection to " . $self->wsdl . ", giving up!!\n";
		$self->consecutive_connect_failure_count++;
		return;
	}
	
	$self->service($service);
}

# Calls the named method on a list of arguments.  The order
# of the arguments in @args should correspond to the order
# required by the subroutine on the server side.  Only
# accepts scalar arguments, i.e. strings or numbers.
# Return value type depends on the method being called,
# but will normally be either a string or a reference to
# an array.
sub call {
	my ($self, $method, @args) = @_;
	
	my $service = $self->service;
	my $output = undef;
	eval {
		$output = $service->$method(@args);
	};
	if (!(defined $output)) {
		# Restart web services if something went wrong.
		$self->restart();
		eval {
			$output = $service->$method(@args);
		};
	}
	
	return $output;
}

1;

