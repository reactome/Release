=head1 NAME

GKB::AddLinks::DbSNPGeneToUniprotReferenceDNASequence

=head1 SYNOPSIS

=head1 DESCRIPTION

Creates links for NCBI dbSNP genes and inserts them into the referenceGene slot of
ReferenceGeneProducts.

=head1 SEE ALSO

GKB::DBAdaptor

=head1 AUTHOR

David Croft E<lt>croft@ebi.ac.ukE<gt>

Copyright (c) 2010 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

package GKB::AddLinks::DbSNPGeneToUniprotReferenceDNASequence;

use GKB::Config;
use GKB::AddLinks::GeneToUniprotReferenceDNASequence;
use strict;
use vars qw(@ISA $AUTOLOAD %ok_field);

@ISA = qw(GKB::AddLinks::GeneToUniprotReferenceDNASequence);

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
	$pkg->get_ok_field();

   	my $self = $pkg->SUPER::new();
   	
    return $self;
}

# Needed by subclasses to gain access to object variables defined in
# this class.
sub get_ok_field {
	my ($pkg) = @_;
	
	%ok_field = $pkg->SUPER::get_ok_field();

	return %ok_field;
}

sub buildPart {
	my ($self) = @_;
	
	print STDERR "\n\nDbSNPGeneToUniprotReferenceDNASequence.buildPart: entered\n";
	
	my $dbsnp_gene_reference_database = $self->builder_params->reference_database->get_dbsnp_gene_reference_database();
	$self->target_gene_reference_database($dbsnp_gene_reference_database);
	
	$self->SUPER::buildPart();
}

1;

