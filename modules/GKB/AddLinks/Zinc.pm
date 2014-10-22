
=head1 NAME

GKB::AddLinks::Zinc

=head1 SYNOPSIS

=head1 DESCRIPTION


Original code lifted by David Croft from the script add_ucsc_links.pl, probably from Imre.

=head1 SEE ALSO

GKB::DBAdaptor

=head1 AUTHOR

Sheldon McKay <lt>sheldon.mckay@gmail.com<gt>

Copyright (c) 2014 Ontario Institure for Cancer Research

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

package GKB::AddLinks::Zinc;

use GKB::Config;
use GKB::AddLinks::Builder;
use strict;
use vars qw(@ISA $AUTOLOAD %ok_field);

@ISA = qw(GKB::AddLinks::Builder);

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

    my $pkg = __PACKAGE__;

    print STDERR "\n\n$pkg.buildPart: entered\n";

    $self->timer->start( $self->timer_message );
    my $dba = $self->builder_params->refresh_dba();
    $dba->matching_instance_handler(
        new GKB::MatchingInstanceHandler::Simpler );

    my $ref_seqs = $self->fetch_reference_peptide_sequences(1);

    my $attribute = 'crossReference';
    $self->set_instance_edit_note("${attribute}s inserted by $pkg");

    # Load the values of an attribute to be updated. Not necessary for the 1st time though.
    $dba->load_class_attribute_values_of_multiple_instances(
        'DatabaseIdentifier', 'identifier', $ref_seqs );

    my $zinc_ref_db =
      $self->builder_params->reference_database->get_zinc_reference_database();

    for my $ref_seq ( @{$ref_seqs} ) {
        print STDERR "$pkg.buildPart: i->Identifier="
          . $ref_seq->Identifier->[0] . "\n";

    # Remove identifiers to make sure the mapping is up-to-date, keep others
    # this isn't really necessary as long as the script is run on the slice only
    # But a good thing to have if you need to run the script a second time.
        $self->remove_typed_instances_from_attribute( $ref_seq, $attribute,
            $zinc_ref_db );

        my $zinc_database_identifier = $self->builder_params->database_identifier
          ->get_zinc_database_identifier( $ref_seq->Identifier->[0] );
        $ref_seq->add_attribute_value( $attribute, $zinc_database_identifier );
        $dba->update_attribute( $ref_seq, $attribute );
        $ref_seq->add_attribute_value( 'modified', $self->instance_edit );
        $dba->update_attribute( $ref_seq, 'modified' );
        $self->increment_insertion_stats_hash( $ref_seq->db_id() );
    }

    $self->print_insertion_stats_hash();

    $self->timer->stop( $self->timer_message );
    $self->timer->print();
}

1;

