=head1 NAME

GKB::SOAPServer::ChEBI

=head1 SYNOPSIS

This class sets up a SOAP server for accessing the ChEBI compound database.

=head1 DESCRIPTION

This class allows you to access the ChEBI web services, and provides methods
that call identically-named queries on the ChEBI server.  Note that only a
subset of these queries has been implemented, but it would be easy to
implement more as required.

=head1 SEE ALSO

=head1 AUTHOR

David Croft E<lt>croft@ebi.ac.ukE<gt>

Copyright (c) 2010 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

package GKB::SOAPServer::ChEBI;

use GKB::Config;
use Data::Dumper;
#use SOAP::Data;
use strict;
use vars qw(@ISA $AUTOLOAD %ok_field);
use GKB::SOAPServer::ProxyPlusURI;

@ISA = qw(GKB::SOAPServer::ProxyPlusURI);

#my $proxy = 'http://www.ebi.ac.uk/webservices/chebi/webservice?wsdl';
my $proxy = 'http://www.ebi.ac.uk/webservices/chebi/2.0/webservice?wsdl';
my $uri = 'http://www.ebi.ac.uk/webservices/chebi';

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

   	my $self = $pkg->SUPER::new($proxy, $uri);
   	
    return $self;
}

# Looks to see if there is a ChEBI entity with the given identifier,
# returns it if available.
sub getCompleteEntity {
	my ($self, $identifier) = @_;
	
	print STDERR "ChEBI.getCompleteEntity: identifier=|$identifier|\n";

#	my $params = ['chebiId', "CHEBI:$identifier"];
#	
#	return $self->call("getCompleteEntity", $params);

	# Setup method and parameters
	my $method = SOAP::Data->name('getCompleteEntity')->attr({xmlns => $uri});
	my @params = ( SOAP::Data->name(chebiId => "CHEBI:$identifier"));
	
	# Call method
	my $som = $self->soap->call($method => @params);
	
	
#	print STDERR "ChEBI.getCompleteEntity: som=" . Dumper($som) . "\n";

	# Retrieve for example all ChEBI identifiers for the ontology parents
	my @stuff = $som->valueof('//chebiId');
	
	print STDERR "ChEBI.getCompleteEntity: amount of stuff=" . scalar(@stuff) . "\n";
	print STDERR "ChEBI.getCompleteEntity: stuff=|@stuff|\n";

	return \@stuff;
}

# Looks to see if there is a ChEBI entity with the given identifier,
# returns it if available.
sub getLiteEntity {
	my ($self, $identifier) = @_;
	
	# Setup method and parameters
	my $method = SOAP::Data->name('getLiteEntity')->attr({xmlns => $uri});
#	my @params = (SOAP::Data->name(chebiId => "CHEBI:$identifier"));
#	my @params = (SOAP::Data->name(search => "CHEBI:44819"),
#				  SOAP::Data->name(searchCategory => "CHEBI ID"),
##				  SOAP::Data->name(maximumResults => "200"),
##				  SOAP::Data->name(Stars => "ALL"),
#				 );
my @params = ( SOAP::Data->name(search => 'group'),
SOAP::Data->name(searchCategory => 'CHEBI NAME'));
	
	# Call method
	my $som = $self->soap->call($method => @params);
	
	print STDERR "ChEBI.getLiteEntity: som=" . Dumper($som) . "\n";

	# Retrieve for example all ChEBI identifiers for the ontology parents
	my @stuff = $som->valueof('//ListElement//chebiId');
	
	print STDERR "ChEBI.getLiteEntity: amount of stuff=" . scalar(@stuff) . "\n";
	print STDERR "ChEBI.getLiteEntity: stuff=|@stuff|\n";

	return \@stuff;
}


# Looks to see if there are ChEBI entities with the given (possibly
# outdated) identifier, returns their IDs if available.
sub get_up_to_date_identifier_and_name {
	my ($self, $identifier) = @_;
	
#	print STDERR "ChEBI.getUpToDateIdentifiers: identifier=|$identifier|\n";

	# Setup method and parameters
	my $method = SOAP::Data->name('getCompleteEntity')->attr({xmlns => $uri});
	my @params = ( SOAP::Data->name(chebiId => "CHEBI:$identifier"));
	
	# Call method
	my $som = $self->soap->call($method => @params);
	
	
#	print STDERR "ChEBI.getCompleteEntity: som=" . Dumper($som) . "\n";

	# Retrieve ChEBI ID(s)
	my @stuff = $som->valueof('//chebiId');
	
#	print STDERR "ChEBI.getCompleteEntity: ChEBI IDs: @stuff\n";

	# Assume that the first identifier in the list is the actual compound,
	# and everything else comes higher up in the ChEBI ontology
	my $up_to_date_identifier = undef;
	if (scalar(@stuff) > 0) {
		$up_to_date_identifier = $stuff[0];
	}

	# Retrieve ChEBI name
	@stuff = $som->valueof('//chebiAsciiName');
	
	# Assume that the first identifier in the list is the actual compound,
	# and everything else comes higher up in the ChEBI ontology
	my $chebi_name = undef;
	if (scalar(@stuff) > 0) {
		$chebi_name = $stuff[0];
	}

	return ($up_to_date_identifier, $chebi_name);
}
1;

