=head1 NAME

GKB::IdentifierMapper::Base

=head1 SYNOPSIS

A base class that provides abstract methods for identifier mapping.

=head1 DESCRIPTION

Identifier mapping involves taking one or more identifiers from one
database (e.g. UniProt) and looking for corresponding identifier
in some other database (e.g. ENSEMBL).  This bas class provides
the core methods required to do this.  Any other class that wants
to do identifier mapping can use the base class' method set.  You
can subclass for specific identifier mapping techniques, e.g. using
ENSEMBL, using PICR, etc.

=head1 SEE ALSO

=head1 AUTHOR

David Croft E<lt>croft@ebi.ac.ukE<gt>

Copyright (c) 2008 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

package GKB::IdentifierMapper::Base;

use GKB::Config;
use strict;
use vars qw(@ISA $AUTOLOAD %ok_field);
use Bio::Root::Root;

@ISA = qw(Bio::Root::Root);

# List the object variables here, so that they can be checked
for my $attr
    (qw(
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
   	
    return $self;
}

# Needed by subclasses to gain access to class variables defined in
# this base class.
sub get_ok_field {
	my ($pkg) = @_;
	
	return %ok_field;
}

sub refresh {
	my ($self) = @_;
	
}

# For the given input database name, convert the supplied ID into
# appropriate IDs for the named output database.
# Returns a reference to an array of output IDs.
sub convert {
	my ($self, $input_db, $input_id, $output_db) = @_;
	
	print STDERR "Base.convert: WARNING - this is an abstract method and should be implemented by you\n";
	exit(1);
}

# For the given input database name, convert the supplied IDs into
# appropriate IDs for the named output database.
#
# Some converters need to know the species you are dealing with
# and will return undef if you do not supply it.  Check the
# corresponding class file.
#
# Returns a reference to a hash, whose keys are the input IDs and
# whose values are references to arrays of corresponding output
# IDs.  If no IDs can be found, an empty array reference is returned.
# If something goes wrong while trying to do the conversion, undef
# will be returned.
sub convert_list {
	my ($self, $input_db, $input_ids, $output_db, $species) = @_;
	
	print STDERR "Base.convert_list: WARNING - this is an abstract method and should be implemented by you\n";
	exit(1);
}

# Closes the connection to the conversion service
sub close {
	my ($self) = @_;
	
	print STDERR "Base.close: WARNING - this is an abstract method and should be implemented by you\n";
	exit(1);
}

1;

