=head1 NAME

GKB::SOAPServer::ProxyPlusURI

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

package GKB::SOAPServer::ProxyPlusURI;

use SOAP::Lite;
use GKB::Config;
use Data::Dumper;
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
    my($pkg, $proxy, $uri) = @_;

   	# Get class variables from superclass and define any new ones
   	# specific to this class.
	$pkg->get_ok_field();
 	
   	my $self = $pkg->SUPER::new();
   	
	$self->soap(undef);
	$self->proxy($proxy);
	$self->uri($uri);
	$self->call_arg_type('string');

   	$self->restart();
 	
    return $self;
}

# Needed by subclasses to gain access to class variables defined in
# this class.
sub get_ok_field {
	my ($pkg) = @_;
	
	%ok_field = $pkg->SUPER::get_ok_field();
	$ok_field{"soap"}++;
	$ok_field{"proxy"}++;
	$ok_field{"uri"}++;
	$ok_field{"call_arg_type"}++;

	return %ok_field;
}

# Connects to the SOAP service.
sub restart {
	my ($self) = @_;
	
	my $proxy = $self->proxy;
	if (!(defined $proxy)) {
		print STDERR "ProxyPlusURI.restart: WARNING - no proxy defined, aborting\n";
		return;
	}
	
	my $uri = $self->uri;
	if (!(defined $uri)) {
		print STDERR "ProxyPlusURI.restart: WARNING - no URI defined, aborting\n";
		return;
	}

   	my $soap = $self->get_soap($proxy, $uri);
	my $start_service_counter = 0;
	while (!(defined $soap) && $start_service_counter<10) {
		sleep(10);
		$soap = $self->get_soap($proxy, $uri);
		$start_service_counter++;
	}
 	if (!(defined $soap)) {
		print STDERR "ProxyPlusURI.restart: WARNING - cannot create a new SOAP connection, giving up!\n";
		return;
	}
   	
   	$self->soap($soap);
}

# Calls the named method on a list of arguments.  @args
# should be an array of key-value pairs, where the key
# gives the name of a variable in the corresponding
# subroutine on the server side.  You may give multiple
# arguments containing the same key, if a list of values
# needs to be passed for a given server-side argument.
# Example argument list:
#
# (['accession', 'P29375'], ['searchDatabases', "SWISSPROT"], ['searchDatabases', "TREMBL"])
#
# You may also need to set the parameter 'call_arg_type'
# before the call method, because the SOAP data type used
# for subroutine arguments may not be the default 'string'.
# Look at the WSDL file for the SOAP service to figure
# out what you will need.  There is one limiting factor
# used to simplify things: all arguments to the remote
# subroutine must be of the same type.  If that does not
# hold, you will have to write low-level code to do the
# subroutine call.
#
# Return value type depends on the method being called,
# and can be anything from a simple string to a complex
# nesting of references to arrays and hashes.
sub call {
	my ($self, $method, @args) = @_;
	
	my $output = undef;
	
	my $soap = $self->soap;
 	if (!(defined $soap)) {
		print STDERR "ProxyPlusURI.call: WARNING - cannot create a new SOAP connection, aborting\n";
		return $output;
	}
	
	my @remote_args = ();
	my $arg;
	foreach $arg (@args) {
		my $remote_arg = SOAP::Data->type($self->call_arg_type);
		$remote_arg->name($arg->[0]);
		$remote_arg->value($arg->[1]);
		push(@remote_args, $remote_arg);
	}
	
	my $return = undef;
	my $error = 1;
	eval {
		$return = $soap->$method(@remote_args);
		$error = 0;
	};
	if (!(defined $return)) {
		# Restart web services if something went wrong.
		$self->restart();
		eval {
			$return = $soap->$method(@remote_args);
			$error = 0;
		};
	}
	
	if (defined $return) {
		if ($return->fault) {
			print STDERR "ProxyPlusURI.call: WARNING - fault, code=" . $return->faultcode . ", string=" . $return->faultstring . "\n";
		} else {
			my $result = $return->result();
			if (defined $result) {
				my @paramsout = $return->paramsout();
				if (scalar(@paramsout)<1) {
					@paramsout = ($result);
				}
				$output = \@paramsout;
			} else {
#				print STDERR "ProxyPlusURI.call: WARNING - result is undef!!\n";
			}
		}
	} else {
		if ($error) {
			print STDERR "ProxyPlusURI.call: WARNING - fatal error occurred during SOAP call\n";
		}
	}

	return $output;
}

sub get_soap {
	my ($self, $proxy, $uri) = @_;
	
	my $soap = SOAP::Lite->new('uri' => $uri, 'proxy' => $proxy);
	
	return $soap;
}

1;

