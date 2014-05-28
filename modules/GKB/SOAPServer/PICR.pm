=head1 NAME

GKB::SOAPServer::PICR

=head1 SYNOPSIS

This class sets up a SOAP server for accessing the PICR sequence identifier
mapping service.

=head1 DESCRIPTION

Full details of PICR can be read at:

http://www.ebi.ac.uk/Tools/picr/

This class allows you to access the PICR web services, and provides methods
that call identically-named queries on the PICR server.  Note that only a
subset of these queries has been implemented, but it would be easy to
implement more as required.

=head1 SEE ALSO

GKB::InstanceCreator::DatabaseIdentifier

=head1 AUTHOR

David Croft E<lt>croft@ebi.ac.ukE<gt>

Copyright (c) 2008 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

package GKB::SOAPServer::PICR;

use GKB::Config;
use Data::Dumper;
use strict;
use vars qw(@ISA $AUTOLOAD %ok_field);
use GKB::SOAPServer::ProxyPlusURI;

@ISA = qw(GKB::SOAPServer::ProxyPlusURI);

my $proxy = 'http://www.ebi.ac.uk:80/Tools/picr/service';
my $uri = 'http://www.ebi.ac.uk/picr/AccessionMappingService';

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
   	$self->call_arg_type('xsd:string'); # overwrites default - see PICR WSDL
   	
    return $self;
}

# Retuens a reference to an array of database names known to PICR.
sub getMappedDatabaseNames {
	my ($self) = @_;
	
	return $self->call("getMappedDatabaseNames");
}

# Takes as arguments an accession number plus the name of a target database.
# Finds the entities in the target database that are equivalent to the
# entity that the accession corresponds to.  The accession can be a protein
# identifier for any of the databases that PICR knows about - it is generally
# clever enough to be able to decide which database the identifier belongs to.
#
# Returns a reference to an array of accessions in the target database that
# map on to the source accession supplied as an argument.  If nothing could
# be found, an empty list will be returned.
sub getUPIForAccession {
	my ($self, $accession, $search_database) = @_;
	
	my $target_accessions = [];

	if (!(defined $accession)) {
		print STDERR "PICR.getUPIForAccession: WARNING - accession not defined!\n";
		return $target_accessions;
	}
	if (!(defined $search_database)) {
		print STDERR "PICR.getUPIForAccession: WARNING - search_database not defined!\n";
		return $target_accessions;
	}
	
	my $accession_pair = ['accession', $accession];
	my $search_database_pair = ['searchDatabases', $search_database];
	
	my $output = $self->call("getUPIForAccession", $accession_pair, $search_database_pair);
	if (!(defined $output) || !(scalar($output) =~ /ARRAY/) || scalar(@{$output})<1) {
#		print STDERR "PICR.getUPIForAccession: WARNING - no results for accession=$accession, search_database=$search_database\n";
		return $target_accessions;
	}
	
	my $upi;
	my $logicalCrossReferences;
	my $identicalCrossReferences;
	my %accession_hash = ();
	my $accession_hash_ref = \%accession_hash;
	foreach $upi (@{$output}) {
		$logicalCrossReferences = $upi->{'logicalCrossReferences'};
		$accession_hash_ref = $self->extract_accession_hash_from_cross_references($logicalCrossReferences, $accession_hash_ref);

		$identicalCrossReferences = $upi->{'identicalCrossReferences'};
		$accession_hash_ref = $self->extract_accession_hash_from_cross_references($identicalCrossReferences, $accession_hash_ref);
	}

	@{$target_accessions} = keys(%{$accession_hash_ref});
	
	return $target_accessions;
}

sub extract_accession_hash_from_cross_references {
	my ($self, $cross_references, $accession_hash_ref) = @_;
	
	my %accession_hash = ();
	if (defined $accession_hash_ref) {
		%accession_hash = %{$accession_hash_ref};
	}
	
	if (!(defined $cross_references)) {
		return \%accession_hash;
	}
	
	my $accession;
	if ($cross_references =~ /ARRAY/) {
		my $cross_reference;
		foreach $cross_reference (@{$cross_references}) {
			$accession = $self->extract_accession_from_cross_reference($cross_reference);
			if (defined $accession) {
				$accession_hash{$accession} = 1;
			}
		}
	} else {
		$accession = $self->extract_accession_from_cross_reference($cross_references);
		if (defined $accession) {
			$accession_hash{$accession} = 1;
		}
	}
	
	return \%accession_hash;
}

sub extract_accession_from_cross_reference {
	my ($self, $cross_reference) = @_;
	
	if ($cross_reference->{'dateDeleted'}) {
		return undef;
	}
	
	return $cross_reference->{'accession'};
}

1;

