=head1 NAME

GKB::AddLinks::BuilderParams

=head1 SYNOPSIS

=head1 DESCRIPTION

This class is used to pass parameters to Builder objects, allowing
you to give your data a bit more structure than a hash.  It provides
getter and setter methods for the parameters.

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

package GKB::AddLinks::BuilderParams;

use strict;
use vars qw(@ISA $AUTOLOAD %ok_field);
use Bio::Root::Root;
use GKB::Utils;
use GKB::InstanceCreator::ReferenceDatabase;
use GKB::InstanceCreator::DatabaseIdentifier;
use GKB::InstanceCreator::Miscellaneous;

@ISA = qw(Bio::Root::Root);

# List the object variables here, so that they can be checked
for my $attr
    (qw(
    db_params
    species_name
    db_ids
    
    dba
    identifier_mapper
    reference_database
    database_identifier
    miscellaneous
    instance_edit
	) ) { $ok_field{$attr}++; }

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

    my $self = bless {}, $pkg;
   	
   	$self->db_params(undef);
   	$self->species_name(undef);
    $self->db_ids(undef);

   	$self->dba(undef);
   	$self->identifier_mapper(undef);
   	$self->reference_database(GKB::InstanceCreator::ReferenceDatabase->new());
   	$self->database_identifier(GKB::InstanceCreator::DatabaseIdentifier->new());
   	$self->database_identifier->set_reference_database($self->reference_database);
    $self->miscellaneous(GKB::InstanceCreator::Miscellaneous->new());
    $self->instance_edit(undef);
   	
    return $self;
}

# Set parameters for the Reactome database.  You must provide
# a db_name; other arguments can be undef.  Arguments:
#
# db_name
# host
# port
# user
# password
#
# Calling this method will also automatically set up a DBAdaptor.
sub set_db_params {
	my ($self, $db_name, $host, $port, $user, $password) = @_;
	
	$self->db_params([$db_name, $host, $port, $user, $password]);
	$self->set_dba(GKB::DBAdaptor->new(-user=>$user || '', -host=>$host, -pass=>$password, -port=>$port, -dbname => $db_name));
}

# Get parameters for the Reactome database as a reference to an array:
#
# [db_name, host, port, user, password]
sub get_db_params {
	my ($self) = @_;
	
	return $self->db_params;
}

# Set the DBAdaptor
sub set_dba {
	my ($self, $dba) = @_;
	
	$self->dba($dba);
    $self->miscellaneous->set_dba($dba);
    $self->instance_edit($self->miscellaneous->get_instance_edit_for_effective_user());
   	$self->reference_database->set_dba($dba);
   	$self->reference_database->set_miscellaneous($self->miscellaneous);
   	$self->database_identifier->set_dba($dba);
   	$self->database_identifier->set_miscellaneous($self->miscellaneous);
   	$self->database_identifier->set_reference_database($self->reference_database);
}

# Get the DBAdaptor
sub get_dba {
	my ($self) = @_;
	
	return $self->dba;
}

# Close old DBAdaptor (if one existed) and set up a new one.
# This can be useful if you have a very long-running pocess
# (2 days or more) and are getting problems with MySQL connection
# timeouts.  Only works if you have done a set_db_params at
# some point beforehand.  Returns the new DBAdaptor object.
sub refresh_dba {
	my ($self) = @_;
	
	if (!(defined $self->db_params)) {
		print STDERR "BuilderParams.refresh_dba: WARNING - db_params not defined, will continue to use old DBA!!";
		return $self->dba; # this might be undef!
	}
	
	if (defined $self->dba) {
		# Close connection to old DBAdaptor
		$self->dba->DESTROY();
		$self->dba(undef);
	}
	
	my ($db_name, $host, $port, $user, $password) = @{$self->db_params};

	$self->set_dba(GKB::DBAdaptor->new(-user=>$user || '', -host=>$host, -pass=>$password, -port=>$port, -dbname => $db_name));

	return $self->dba;
}

# Set the mapper that converts between different types of ID, e.g. from
# UniProt to ENSEMBL gene.
sub set_identifier_mapper {
	my ($self, $identifier_mapper) = @_;
	
	$self->identifier_mapper($identifier_mapper);
}

# Get the mapper that converts between different types of ID, e.g. from
# UniProt to ENSEMBL gene.
sub get_identifier_mapper {
	my ($self) = @_;
	
	return $self->identifier_mapper;
}

# Set the species name, e.g. 'Homo sapiens'.
sub set_species_name {
	my ($self, $species_name) = @_;
	
	$self->species_name($species_name);
}

# Get the species name.
sub get_species_name {
	my ($self) = @_;
	
	return $self->species_name;
}

# Set DB_IDs to be used for limiting the search - mainly needed for diagnostic purposes.
# Should be either a reference to an array of DB_IDs or a string containing comma-
# separated DB_IDs.
sub set_db_ids {
	my ($self, $db_ids) = @_;
	
	if (!(defined $db_ids) || $db_ids eq '') {
		return;
	}
	
	if (!(scalar($db_ids) =~ /ARRAY/)) {
		my @db_id_array = split(/,/, $db_ids);
		$db_ids = \@db_id_array;
	}
	
	$self->db_ids($db_ids);
}

# Get DB_IDs to be used for limiting the search.
# Returns a reference to an array of DB_IDs.
sub get_db_ids {
	my ($self) = @_;
	
	return $self->db_ids;
}

# Sets the InstanceEdit instance.
sub set_instance_edit {
	my ($self, $instance_edit) = @_;
	
	$self->instance_edit($instance_edit);
}

# Gets the InstanceEdit instance.
sub get_instance_edit {
	my ($self) = @_;
	
	return $self->instance_edit;
}

# Closes any services opened by BuilderParams
sub close {
	my ($self) = @_;
	
   	if (defined $self->identifier_mapper) {
   		$self->identifier_mapper->close();
   	}
}

1;

