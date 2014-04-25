package GKB::Compara::Utils;

=head1 NAME

GKB::Compara::Utils

=head1 SYNOPSIS

File manipulation utilities

=head1 DESCRIPTION

A Perl utility module for the orthopair script, providing specialized
methods for manipulating files.

=head1 SEE ALSO

=head1 AUTHOR

Joel Weiser E<lt>joel.weiser@oicr.on.caE<gt>

Copyright (c) 2012 European Bioinformatics Institute, Cold Spring
Harbor Laboratory, and Ontario Institute for Cancer Research.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

use strict;
use vars qw(@ISA $AUTOLOAD %ok_field);
use Bio::Root::Root;
use GKB::Utils;
use GKB::Config;

@ISA = qw(GKB::Utils);

sub AUTOLOAD {
    my $self = shift;
    my $attr = $AUTOLOAD;
    $attr =~ s/.*:://;
    return unless $attr =~ /[^A-Z]/;  # skip DESTROY and all-cap methods
    $self->throw("invalid attribute method: ->$attr()") unless $ok_field{$attr};
    $self->{$attr} = shift if @_;
    return $self->{$attr};
}  

# Create a new instance of this class
sub new {
    my($pkg, @args) = @_;
    my $self = bless {}, $pkg;
}

# Default behaviour creates a release version directory inside
# scripts/compara for orthopair files.  However, a different path
# can also be given when the 'not version' flag is set
sub create_directory {
    my ($self, $dir_name, $not_version) = @_;
    
    $dir_name = "$COMPARA_DIR/$dir_name" unless $not_version;
    `mkdir $dir_name` unless (-e $dir_name);
}

#Removes duplicates from arrays - supply arrayref, returns arrayref
sub uniquify {
    my ($self, $ar) = @_;

    my %seen;
    foreach my $id (@{$ar}) {
	$seen{$id}++;
    }

    return [keys %seen];
}

1;
