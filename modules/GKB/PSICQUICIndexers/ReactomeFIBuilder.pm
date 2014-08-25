=head1 NAME

GKB::PSICQUICIndexers::ReactomeFIBuilder

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

package GKB::PSICQUICIndexers::ReactomeFIBuilder;

use GKB::Config;
use GKB::PSICQUICIndexers::Builder;
use Data::Dumper;
use strict;
use vars qw(@ISA $AUTOLOAD %ok_field);
use Log::Log4perl qw/get_logger/;
Log::Log4perl->init(\$LOG_CONF);

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
   	
   	$self->index_dir_name("reactomefi");
	$self->class_name("ReactomeFIBuilder");
   	
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
    
    my $logger = get_logger(__PACKAGE__);

    my $current_download_dir = $self->get_current_download_dir();
    my $fi_filename = "FIsInMITTab.txt";
	
    $logger->info("ReactomeFIBuilder.get_mitab_path: deleting old FI file");
	
    my $status = system("cd $current_download_dir; rm -f $fi_filename $fi_filename.zip");
    if ($status != 0) {
	$logger->warn("ReactomeFIBuilder.get_mitab_path: WARNING - could not delete file $fi_filename");
	$self->termination_status("could not delete file $fi_filename");
	return undef;					
    }

    $logger->info("ReactomeFIBuilder.get_mitab_path: downloading FI file");

    $status = system("wget -P $current_download_dir http://reactomews.oicr.on.ca:8080/caBigR3WebApp/hosted/org.reactome.r3.fiview.gwt.FIView/$fi_filename.zip");
    if ($status != 0) {
	$logger->warn("ReactomeFIBuilder.get_mitab_path: WARNING - could not get file $fi_filename from reactomedev");
	$self->termination_status("could not get file $fi_filename from reactomedev");
	return undef;					
    }

    $logger->info("ReactomeFIBuilder.get_mitab_path: uncompressing FI file");

    $status = system("cd $current_download_dir; unzip $fi_filename.zip");
    if ($status != 0) {
	$logger->warn("ReactomeFIBuilder.get_mitab_path: WARNING - could not unzip file $fi_filename.zip");
	$self->termination_status("could not unzip file $fi_filename.zip");
	return undef;					
    }

    return "$current_download_dir/$fi_filename";
}

1;

