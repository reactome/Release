=head1 NAME

GKB::SOAPServer::KEGG

=head1 SYNOPSIS

This class sets up a SOAP server for accessing KEGG data.

=head1 DESCRIPTION

=head1 SEE ALSO

GKB::AddLinks::KEGGReferenceGeneToReferencePeptideSequence

=head1 AUTHOR

David Croft E<lt>croft@ebi.ac.ukE<gt>

Copyright (c) 2008 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

package GKB::SOAPServer::KEGG;

use GKB::Config;
use strict;
use vars qw(@ISA $AUTOLOAD %ok_field);
use GKB::SOAPServer::WSDL;

@ISA = qw(GKB::SOAPServer::WSDL);

my $wsdl = 'http://soap.genome.jp/KEGG.wsdl';

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
	%ok_field = $pkg->SUPER::get_ok_field();

   	my $self = $pkg->SUPER::new($wsdl);
   	
    return $self;
}

sub bconv {
	my ($self, $query_string) = @_;
	
	return $self->call("bconv", $query_string);
}

sub bget {
	my ($self, $kegg_gene_id) = @_;
	
	return $self->call("bget", $kegg_gene_id);
}

1;

