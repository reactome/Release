=head1 NAME

GKB::PSICQUICIndexers::BuilderParams

=head1 SYNOPSIS

=head1 DESCRIPTION

This class is used to pass parameters to Builder objects, allowing
you to give your data a bit more structure than a hash.  It provides
getter and setter methods for the parameters.

=head1 SEE ALSO

GKB::PSICQUICIndexers::Builder

=head1 AUTHOR

David Croft E<lt>croft@ebi.ac.ukE<gt>

Copyright (c) 2012 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

package GKB::PSICQUICIndexers::BuilderParams;

use strict;
use vars qw(@ISA $AUTOLOAD %ok_field);
use Bio::Root::Root;

@ISA = qw(Bio::Root::Root);

# List the object variables here, so that they can be checked
for my $attr
    (qw(
    release_num
    gkb_root_dir
    create_war_flag
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
   	
    $self->release_num(undef);
    $self->gkb_root_dir(undef);
    $self->create_war_flag(undef);
   	
    return $self;
}

# Set the path to the file containing PSI-MITAB format data to be indexed.
sub set_release_num {
	my ($self, $release_num) = @_;
	
	$self->release_num($release_num);
}

# Get the path to the file containing PSI-MITAB format data to be indexed.
sub get_release_num {
	my ($self) = @_;
	
	return $self->release_num;
}

# Set the path to the target directory that will contain the indexes generated.
sub set_gkb_root_dir {
	my ($self, $gkb_root_dir) = @_;
	
	$self->gkb_root_dir($gkb_root_dir);
}

# Get the path to the target directory that will contain the indexes generated.
sub get_gkb_root_dir {
	my ($self) = @_;
	
	return $self->gkb_root_dir;
}

# Set the flag that indicates if a WAR file should be generated or not.
sub set_create_war_flag {
	my ($self, $create_war_flag) = @_;
	
	$self->create_war_flag($create_war_flag);
}

# Get the flag that indicates if a WAR file should be generated or not.
sub get_create_war_flag {
	my ($self) = @_;
	
	return $self->create_war_flag;
}

# Closes any services opened by BuilderParams
sub close {
	my ($self) = @_;
	
}

1;

