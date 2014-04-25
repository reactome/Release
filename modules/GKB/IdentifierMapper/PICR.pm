=head1 NAME

GKB::IdentifierMapper::PICR

=head1 SYNOPSIS

Uses PICR for doing identifier mapping.

=head1 DESCRIPTION

=head1 SEE ALSO

=head1 AUTHOR

David Croft E<lt>croft@ebi.ac.ukE<gt>

Copyright (c) 2008 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

package GKB::IdentifierMapper::PICR;

use GKB::Config;
use GKB::SOAPServer::PICR;
use strict;
use vars qw(@ISA $AUTOLOAD %ok_field);
use GKB::IdentifierMapper::Base;

@ISA = qw(GKB::IdentifierMapper::Base);

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
   	
   	$self->picr_soap_server(GKB::SOAPServer::PICR->new());
   	
    return $self;
}

# Needed by subclasses to gain access to class variables defined in
# this base class.
sub get_ok_field {
	my ($pkg) = @_;
	
	%ok_field = $pkg->SUPER::get_ok_field();
 	$ok_field{"picr_soap_server"}++;

	return %ok_field;
}

# For the given input database name, convert the supplied ID into
# appropriate IDs for the named output database.
#
# Input DB is, strictly speaking, redundant, but keep it in the
# argument line as a matter of good form.
	
# Returns a reference to an array of output IDs.
sub convert {
	my ($self, $input_db, $input_id, $output_db) = @_;
	
	# Some database names may need to be modified so that
	# PICR can understand them.
	my $picr_output_db = $output_db;
	if ($picr_output_db =~ /^ENSEMBL_/) {
		$picr_output_db = 'ENSEMBL';
	}
	
    my $output_ids = $self->picr_soap_server->getUPIForAccession($input_id, $picr_output_db);
	
	return $output_ids;
}

sub convert_list {
	my ($self, $input_db, $input_ids, $output_db, $species) = @_;
	
# Not yet implemented.

	return undef;
}

sub close {
	my ($self) = @_;
	
# No need to explicitly disconnect from the SOAP server, so this
# method can be blank, but is has to be here, otherwise the
# superclass will throw a wobbly.
}

1;

