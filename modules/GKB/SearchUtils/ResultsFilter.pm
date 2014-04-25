package GKB::SearchUtils::ResultsFilter;

=head1 NAME

GKB::SearchUtils::ResultsFilter

=head1 SYNOPSIS

Provides methods for removing obscure instances from a list of instances.

=head1 DESCRIPTION

Removes instances that are probably not going to be interesting
or meaningful to the average user.

=head1 SEE ALSO

=head1 AUTHOR

David Croft E<lt>croft@ebi.ac.ukE<gt>

Copyright (c) 2008 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

use strict;
use vars qw(@ISA $AUTOLOAD %ok_field @RETAIN_INSTANCE_CLASSES);
use Bio::Root::Root;
use Carp;

@ISA = qw(Bio::Root::Root);

for my $attr
    (qw(
        render_result
	) ) { $ok_field{$attr}++; }

@RETAIN_INSTANCE_CLASSES =
	(
		'Event',
		'ReferenceEntity',
		'Complex',
		'Regulation',
		'LiteratureReference',
	);

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
    my($pkg, @args) = @_;
    my $self = bless {}, $pkg;
    
    if (defined $args[1]) {
    	$self->set_render_result($args[1]);
    }
        
    return $self;
}

# Needed by subclasses
sub get_ok_field {
	return %ok_field;
}

# Removes instances that are probably not going to be interesting
# or meaningful to the average user.  Takes as an argument a reference
# to an array of instances, and returns a reference to an array of
# (filtered) instances.
sub filter {
	my($self, $instances) = @_;
	
	my @filtered_instance_array = ();
	if (defined $instances) {
		my $instance;
		my $retain_instance_class;
		foreach $instance (@{$instances}) {
			foreach $retain_instance_class (@RETAIN_INSTANCE_CLASSES) {
				if ($instance->is_a($retain_instance_class)) {
					push(@filtered_instance_array , $instance);
					last;
				}
			}
		}
	}
	
	return \@filtered_instance_array;
}

1;

