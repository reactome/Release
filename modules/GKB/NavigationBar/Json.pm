=head1 NAME

GKB::NavigationBar::Json

=head1 SYNOPSIS

Generates JSON mapping of the navigation bar used at the top of
the Reactome web pages.  Thi is to allow the Perl CGI to communicate
its idea of what the menus should look like to GWT.

=head1 DESCRIPTION

=head1 SEE ALSO

GKB::WebUtils
GKB::NavigationBar::Model

=head1 AUTHOR

David Croft E<lt>croft@ebi.ac.ukE<gt>

Copyright (c) 2010 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

package GKB::NavigationBar::Json;

use JSON;
use GKB::Config;
use strict;
use vars qw(@ISA $AUTOLOAD %ok_field);
use Bio::Root::Root;
use GKB::NavigationBar::Model;

@ISA = qw(Bio::Root::Root);

# List the object variables here, so that they can be checked
for my $attr
    (qw(
    model
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
    my($pkg, $model) = @_;
    
    my $self = bless {}, $pkg;
   	
   	$self->model($model);
   	
    return $self;
}

# Needed by subclasses to gain access to class variables defined in
# this class.
sub get_ok_field {
	return %ok_field;
}

# Generates the JSON.
sub generate {
    my $self = shift;
    
	my $items = $self->model->get_pulldown_menus();
	
	# Remove redundant subitems_hashes
    for (my $i = 0; $i < @{$items}; $i++) {
		if (defined $items->[$i]->{'subitems_hash'}) {
			delete($items->[$i]->{'subitems_hash'});
		}
    }

	my $json = JSON->new->allow_nonref;
	my $out = $json->pretty->encode($items);
    
	return $out;
}

1;

