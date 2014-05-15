=head1 NAME

GKB::Utils::Timer

=head1 SYNOPSIS

Allows you to insert named timers into your program, so that
you can follow how much time various chunks of code are
consuming.  Time measured in seconds.

Typical usage:

my $timer = GKB::Utils::Timer->new();

$timer->start("timer1");

# Chunk of your code

$timer->stop("timer1");

# More of your code

$timer->start("timer2");

# Another chunk of your code

$timer->stop("timer2");

# End of your code

$timer->print();


=head1 DESCRIPTION


=head1 SEE ALSO

GKB::DBAdaptor

=head1 AUTHOR

David Croft E<lt>croft@ebi.ac.ukE<gt>

Copyright (c) 2006 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

package GKB::Utils::Timer;

use strict;
use vars qw(@ISA $AUTOLOAD %ok_field);
use Bio::Root::Root;

@ISA = qw(Bio::Root::Root);

# List the object variables here, so that they can be checked
for my $attr
    (qw(
	timers
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
   	
   	my %timers = ();
   	
   	$self->timers(\%timers);
	
    return $self;
}

# Starts (or restarts) the timer with the name $key
sub start {
    my($self, $key) = @_;
    
    # Timer array - [total time, start time, #calls]
    my @timer = (0, 0, 0);
    my $ref_timer = $self->timers->{$key};
    if (defined $ref_timer) {
    	@timer = @{$ref_timer};
    }
    $timer[1] = time();
    $timer[2]++;
    $self->timers->{$key} = \@timer;
}

# Stops the timer with the name $key
sub stop {
    my($self, $key) = @_;
    
    # Timer array - [total time, start time, #calls]
    my $ref_timer = $self->timers->{$key};
    if (defined $ref_timer) {
    	$ref_timer->[0] += time() - $ref_timer->[1];
    }
}

# Prints all timers to STDERR.  Does not reset times, i.e.
# you can use this method more than once if you want to
# get intermediate reports.
sub print {
    my($self) = @_;
    
    print STDERR "TIMERS:\n\n";
    foreach my $key (sort(keys(%{$self->timers}))) {
    	print STDERR "   $key   " . $self->timers->{$key}->[0] . " (" . $self->timers->{$key}->[2] . " calls)\n";
    }
    print STDERR "\n\n";
}

1;

