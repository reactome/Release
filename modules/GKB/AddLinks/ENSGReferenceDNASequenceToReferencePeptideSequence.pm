=head1 NAME

GKB::AddLinks::ENSGReferenceDNASequenceToReferencePeptideSequence

=head1 SYNOPSIS

Adds ENSEMBL ENSG entries to the referenceGene slots of ReferenceGeneProduct
instances with UniProt or ENSP identifiers.

=head1 DESCRIPTION

Original code lifted from the script add_mim_links.pl, probably from Imre.

Note that ENSG entries will only be added to ReferenceGeneProducts that don't
already have them.  This is the opposite of the way that most of the other
add-links scripts work.  Things are done this way because in many cases, the
ortho script will already have inserted ENSG entries, and there would be no
point in repeating this work.

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

package GKB::AddLinks::ENSGReferenceDNASequenceToReferencePeptideSequence;

use GKB::Config;
use GKB::AddLinks::IdentifierMappedReferenceDNASequenceToReferencePeptideSequence;
use strict;
use vars qw(@ISA $AUTOLOAD %ok_field);

@ISA = qw(GKB::AddLinks::IdentifierMappedReferenceDNASequenceToReferencePeptideSequence);

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
	
	$self->class_name("ENSGReferenceDNASequenceToReferencePeptideSequence");
	
	print STDERR "\n\n" . $self->class_name . ".buildPart: entered\n";
	
	$self->insert_xrefs('referenceGene', 'ENSEMBL', 1);
}

1;

