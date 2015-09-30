=head1 NAME

GKB::AddLinks::RefseqReferenceRNASequenceToReferencePeptideSequence

=head1 SYNOPSIS

Adds RefSeq RNA to the referenceTranscript slots of ReferenceGeneProduct
instances with UniProt identifiers.

=head1 DESCRIPTION

Original code lifted from the script add_refseq_mrna_links.pl, probably from Imre.

=head1 SEE ALSO

GKB::DBAdaptor

=head1 AUTHOR

David Croft E<lt>croft@ebi.ac.ukE<gt>

Copyright (c) 2008 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

package GKB::AddLinks::RefseqReferenceRNASequenceToReferencePeptideSequence;
use strict;

use GKB::Config;
use GKB::AddLinks::IdentifierMappedReferenceDNASequenceToReferencePeptideSequence;

use Log::Log4perl qw/get_logger/;
Log::Log4perl->init(\$LOG_CONF);

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

    my $logger = get_logger(__PACKAGE__);

    $self->class_name("RefseqReferenceRNASequenceToReferencePeptideSequence");

    $logger->info("entered\n");

    $self->insert_xrefs('referenceTranscript', 'RefSeqRNA', 1);
}

1;

