
=head1 NAME

GKB::AddLinks::HmdbMolecules

=head1 SYNOPSIS

=head1 DESCRIPTION

Adds HMDB linkers to ReferenceMolecules having chebi-IDs

=head1 SEE ALSO

GKB::DBAdaptor

=head1 AUTHOR

Sheldon McKay <lt>sheldon.mckay@gmail.com<gt>

Copyright (c) 2014 Ontario Institure for Cancer Research

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

package GKB::AddLinks::HMDBMolecules;

use GKB::Config;
use GKB::AddLinks::Builder;
use GKB::HMDB;
use strict;
use vars qw(@ISA $AUTOLOAD %ok_field);
use Data::Dumper;

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

sub mapper {
    my $self = shift;
    my $dba = shift;
    if ($dba) {
	$self->{mapper} = GKB::HMDB->new($dba);
    }
    return $self->{mapper};
}

# Needed by subclasses to gain access to object variables defined in
# this class.
sub get_ok_field {
    my ($pkg) = @_;

    %ok_field = $pkg->SUPER::get_ok_field();

    return %ok_field;
}

sub buildPart {
    my $self = shift;

    my $pkg = __PACKAGE__;

    print STDERR "\n\n$pkg.buildPart: entered\n";

    $self->timer->start( $self->timer_message );
    my $dba = $self->builder_params->refresh_dba();
    $dba->matching_instance_handler(
        new GKB::MatchingInstanceHandler::Simpler );

    my $mapper = $self->mapper($dba);

    # A hash where the keys are HMDB Ids and the values are GKInstances
    my $molecules = $mapper->fetch_molecules();

    my $attribute = 'crossReference';
    $self->set_instance_edit_note("${attribute}s inserted by $pkg");

    # get a unique list of ReferenceMolecule instances
    my %instances;
    for my $group (values %$molecules) {
	my @molecules = @$group;
	for my $molecule (@molecules) {
	    next unless $molecule && ref $molecule;
	    $instances{$molecule->db_id} = $molecule;
	}
    }
    my $instances = [values %instances];

    # Load the values of an attribute to be updated. Not necessary for the 1st time though.
    $dba->load_class_attribute_values_of_multiple_instances(
        'DatabaseIdentifier', 'identifier', $instances );

    my $hmdb_ref_db =
      $self->builder_params->reference_database->get_hmdb_metabolite_reference_database();

    while (my ($hmdb,$molecules) = each %$molecules) {
        print STDERR "$pkg.buildPart: i->Identifier=HMDB:$hmdb\n";
	

	for my $molecule (@$molecules) { 
	    $molecule || next;
	    print STDERR "\n\nI am working on ReferenceMolecule ", $molecule->db_id(), "\n\n";
	    ## Careful! ReferenceMolecules can be associated with > 1 HMDB ID.
	    ## Only do this the first time the instance is encountered!
	    unless ( $self->{seen}->{$molecule->db_id()}++ ) {
		$self->remove_typed_instances_from_attribute( $molecule, $attribute,
							      $hmdb_ref_db );
	    }
	    
	    my $hmdb_database_identifier = $self->builder_params->database_identifier
		->get_hmdb_metabolite_database_identifier( $hmdb );
	    $molecule->add_attribute_value( $attribute, $hmdb_database_identifier );
	    $dba->update_attribute( $molecule, $attribute );
	    $molecule->add_attribute_value( 'modified', $self->instance_edit );
	    $dba->update_attribute( $molecule, 'modified' );
	    $self->increment_insertion_stats_hash( $molecule->db_id() );
	}
    }

    $self->print_insertion_stats_hash();

    $self->timer->stop( $self->timer_message );
    $self->timer->print();
}

1;

