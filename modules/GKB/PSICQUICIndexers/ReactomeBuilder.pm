=head1 NAME

GKB::PSICQUICIndexers::ReactomeBuilder

=head1 SYNOPSIS

=head1 DESCRIPTION

This class builds the indexes needed by the Reactome PSICQUIC server.

=head1 SEE ALSO

GKB::DBAdaptor

=head1 AUTHOR

David Croft E<lt>croft@ebi.ac.ukE<gt>

Copyright (c) 2012 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

package GKB::PSICQUICIndexers::ReactomeBuilder;

use GKB::Config;
use GKB::PSICQUICIndexers::Builder;
use Data::Dumper;
use strict;
use vars qw(@ISA $AUTOLOAD %ok_field);

@ISA = qw(GKB::PSICQUICIndexers::Builder);

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
   	
   	$self->index_dir_name("reactome");
	$self->class_name("ReactomeBuilder");
   	
    return $self;
}

# Needed by subclasses to gain access to object variables defined in
# this class.
sub get_ok_field {
	my ($pkg) = @_;
	
	%ok_field = $pkg->SUPER::get_ok_field();

	return %ok_field;
}

sub get_mitab_path {
	my ($self) = @_;
	
	return $self->get_current_download_dir() . "/homo_sapiens.mitab.interactions.txt";
}

1;

