
=head1 NAME

GKB::AddLinks::BuilderFactory

=head1 SYNOPSIS

Creates new instances of class Builder

=head1 DESCRIPTION

The single method, construct, creates new instances of class Builder.

=head1 SEE ALSO

GKB::AddLinks::Builder

=head1 AUTHOR

David Croft E<lt>croft@ebi.ac.ukE<gt>

Copyright (c) 2008 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

package GKB::AddLinks::BuilderFactory;

use GKB::AddLinks::EnsemblGeneToUniprotReferencePeptideSequence;
use GKB::AddLinks::ENSGReferenceDNASequenceToReferencePeptideSequence;
use GKB::AddLinks::EntrezGeneToUniprotReferenceDNASequence;
use GKB::AddLinks::BioGPSGeneToUniprotReferenceDNASequence;
use GKB::AddLinks::CTDGeneToUniprotReferenceDNASequence;
use GKB::AddLinks::DbSNPGeneToUniprotReferenceDNASequence;
use GKB::AddLinks::OmimReferenceDNASequenceToReferencePeptideSequence;
use GKB::AddLinks::UCSCReferenceDatabaseToReferencePeptideSequence;
use GKB::AddLinks::GenecardsReferenceDatabaseToReferencePeptideSequence;
use GKB::AddLinks::RefseqReferenceDatabaseToReferencePeptideSequence;
use GKB::AddLinks::RefseqReferenceRNASequenceToReferencePeptideSequence;
use GKB::AddLinks::KEGGReferenceGeneToReferencePeptideSequence;
use GKB::AddLinks::IntActDatabaseIdentifierToComplexOrReactionlikeEvent;
use GKB::AddLinks::BioModelsEventToDatabaseIdentifier;
use GKB::AddLinks::FlyBaseToUniprotReferenceDNASequence;
use GKB::AddLinks::OrphanetToUniprotReferenceDNASequence;
use GKB::AddLinks::PDBToReferencePeptideSequence;
use GKB::AddLinks::PROToReferencePeptideSequence;
use GKB::AddLinks::DOCKBlasterToUniprotDatabaseIdentifier;
use GKB::AddLinks::WormbaseReferenceDNASequenceToReferencePeptideSequence;
use GKB::AddLinks::RHEAIdentifierToReactionlikeEvent;
use GKB::AddLinks::ZincProteins;
use GKB::AddLinks::ZincMolecules;
use GKB::AddLinks::HMDBProteins;
use GKB::AddLinks::HMDBMolecules;

use strict;
use vars qw(@ISA $AUTOLOAD %ok_field);
use Bio::Root::Root;

@ISA = qw(Bio::Root::Root);

# Maps various name variants onto plausible Builder subclasses
my %builder_map = (
    'ZincProteins' => 'GKB::AddLinks::ZincProteins',
    'ZincMolecules' => 'GKB::AddLinks::ZincMolecules',
    'HMDBProteins' => 'GKB::AddLinks::HMDBProteins',
    'HMDBMolecules' => 'GKB::AddLinks::HMDBMolecules',
    'ENSGReferenceDNASequenceToReferencePeptideSequence' => 'GKB::AddLinks::ENSGReferenceDNASequenceToReferencePeptideSequence',
    'EnsemblGeneToUniprotReferencePeptideSequence' => 'GKB::AddLinks::EnsemblGeneToUniprotReferencePeptideSequence',
    'addEnsemblGene2swallSdi' => 'GKB::AddLinks::EnsemblGeneToUniprotReferencePeptideSequence',
    'addEnsemblGene2swallSdi.pl' => 'GKB::AddLinks::EnsemblGeneToUniprotReferencePeptideSequence',
    'EntrezGeneToUniprotReferenceDNASequence' => 'GKB::AddLinks::EntrezGeneToUniprotReferenceDNASequence',
    'addEntrezLinks' => 'GKB::AddLinks::EntrezGeneToUniprotReferenceDNASequence',
    'addEntrezLinks.pl' => 'GKB::AddLinks::EntrezGeneToUniprotReferenceDNASequence',
    'BioGPSGeneToUniprotReferenceDNASequence' => 'GKB::AddLinks::BioGPSGeneToUniprotReferenceDNASequence',
    'CTDGeneToUniprotReferenceDNASequence' => 'GKB::AddLinks::CTDGeneToUniprotReferenceDNASequence',
    'DbSNPGeneToUniprotReferenceDNASequence' => 'GKB::AddLinks::DbSNPGeneToUniprotReferenceDNASequence',
    'OmimReferenceDNASequenceToReferencePeptideSequence' => 'GKB::AddLinks::OmimReferenceDNASequenceToReferencePeptideSequence',
    'add_mim_links' => 'GKB::AddLinks::OmimReferenceDNASequenceToReferencePeptideSequence',
    'add_mim_links.pl' => 'GKB::AddLinks::OmimReferenceDNASequenceToReferencePeptideSequence',
    'GenecardsReferenceDatabaseToReferencePeptideSequence' => 'GKB::AddLinks::GenecardsReferenceDatabaseToReferencePeptideSequence',
    'UCSCReferenceDatabaseToReferencePeptideSequence' => 'GKB::AddLinks::UCSCReferenceDatabaseToReferencePeptideSequence',
    'add_ucsc_links' => 'GKB::AddLinks::UCSCReferenceDatabaseToReferencePeptideSequence',
    'add_ucsc_links.pl' => 'GKB::AddLinks::UCSCReferenceDatabaseToReferencePeptideSequence',
    'RefseqReferenceDatabaseToReferencePeptideSequence' => 'GKB::AddLinks::RefseqReferenceDatabaseToReferencePeptideSequence',
    'add_refseq_peptide_links' => 'GKB::AddLinks::RefseqReferenceDatabaseToReferencePeptideSequence',
    'add_refseq_peptide_links.pl' => 'GKB::AddLinks::RefseqReferenceDatabaseToReferencePeptideSequence',
    'RefseqReferenceRNASequenceToReferencePeptideSequence' => 'GKB::AddLinks::RefseqReferenceRNASequenceToReferencePeptideSequence',
    'add_refseq_mrna_links' => 'GKB::AddLinks::RefseqReferenceRNASequenceToReferencePeptideSequence',
    'add_refseq_mrna_links.pl' => 'GKB::AddLinks::RefseqReferenceRNASequenceToReferencePeptideSequence',
    'KEGGReferenceGeneToReferencePeptideSequence' => 'GKB::AddLinks::KEGGReferenceGeneToReferencePeptideSequence',
    'addKEGGSeq2swallSDI' => 'GKB::AddLinks::KEGGReferenceGeneToReferencePeptideSequence',
    'addKEGGSeq2swallSDI.pl' => 'GKB::AddLinks::KEGGReferenceGeneToReferencePeptideSequence',
    'IntActDatabaseIdentifierToComplexOrReactionlikeEvent' => 'GKB::AddLinks::IntActDatabaseIdentifierToComplexOrReactionlikeEvent',
    'add_links_to_IntAct' => 'GKB::AddLinks::IntActDatabaseIdentifierToComplexOrReactionlikeEvent',
    'add_links_to_IntAct.pl' => 'GKB::AddLinks::IntActDatabaseIdentifierToComplexOrReactionlikeEvent',
    'BioModelsEventToDatabaseIdentifier' => 'GKB::AddLinks::BioModelsEventToDatabaseIdentifier',
    'add_biomodels_links' => 'GKB::AddLinks::BioModelsEventToDatabaseIdentifier',
    'add_biomodels_links.sh' => 'GKB::AddLinks::BioModelsEventToDatabaseIdentifier',
    'FlyBaseToUniprotReferenceDNASequence' => 'GKB::AddLinks::FlyBaseToUniprotReferenceDNASequence',
    'OrphanetToUniprotReferenceDNASequence' => 'GKB::AddLinks::OrphanetToUniprotReferenceDNASequence',
    'RHEAIdentifierToReactionlikeEvent' => 'GKB::AddLinks::RHEAIdentifierToReactionlikeEvent',
    'PDBToReferencePeptideSequence' => 'GKB::AddLinks::PDBToReferencePeptideSequence',
    'PROToReferencePeptideSequence' => 'GKB::AddLinks::PROToReferencePeptideSequence',
    'DOCKBlasterToUniprotDatabaseIdentifier' => 'GKB::AddLinks::DOCKBlasterToUniprotDatabaseIdentifier',
    'WormbaseReferenceDNASequenceToReferencePeptideSequence' => 'GKB::AddLinks::WormbaseReferenceDNASequenceToReferencePeptideSequence',
);

# List the object variables here, so that they can be checked
for my $attr (
    qw(
    instance_edit
    )
  )
{
    $ok_field{$attr}++;
}

sub AUTOLOAD {
    my $self = shift;
    my $attr = $AUTOLOAD;
    $attr =~ s/.*:://;
    return unless $attr =~ /[^A-Z]/;    # skip DESTROY and all-cap methods
    $self->throw("invalid attribute method: ->$attr()") unless $ok_field{$attr};
    $self->{$attr} = shift if @_;
    return $self->{$attr};
}

sub new {
    my ($pkg) = @_;

    my $self = bless {}, $pkg;

    return $self;
}

# Given the name of a Builder subclass, create an object
# of that subclass and return it.  Various possibilities
# for names are available, e.g. the class name or the
# name of the script that it replaces.
sub construct {
    my ( $self, $name ) = @_;

    if ( !( defined $name ) ) {
        print STDERR
          "BuilderFactory.construct: WARNING - name is undef, aborting!\n";
        return undef;
    }

    my $builder = undef;
    my $class   = $builder_map{$name};
    eval {
        if ( defined $class ) {
            print STDERR "BuilderFactory.construct: class=$class\n";
            $builder = $class->new();
        }
        else {
         # Assume a valid class name has been specified and keep fingers crossed
            $builder = $name->new();
        }

        print STDERR
"BuilderFactory.construct: successfully created an object for $name\n";
    };

    if ( !( defined $builder ) ) {
        print STDERR
"BuilderFactory.construct: WARNING - could not find a Builder subclass corresponding to $name\n";
    }

    return $builder;
}

1;

