package GKB::CommonUtils;

=head1 NAME

GKB::CommonUtils

=head1 DESCRIPTION

Common subroutines used in Reactome scripting.

=head2 METHODS

=over 12

=item C<get_dba>

Takes the database name and host (in that
order) as strings and returns a database
adaptor object connected to that database

=item C<get_all_species_in_entity>

Takes a physical entity instance and
gets all species associated with the
entity (will recurse over sets and
complexes to find species of
members/candidates and components,
respectively)

=item C<has_multiple_species>

Takes a database object instance and returns
true if there is more than one species
in the instance's species attribute slot

=item C<is_chimeric>

Takes an instance and returns true if
the isChimeric attribute has the string
'true' (case insensitive)

=item C<get_event_modifier>

Takes an event instance and returns the
name of the curator to last modify the
instance

=item C<is_human>

Takes an instance and returns true if
the only value in the species slot is
the human species instance and the
instance is not chimeric

=item C<report>

Takes a message (a string) and a file
handle and prints the message to the
file handle and standard out

=item C<date_correctly_formatted>

Takes a date as a string and returns
true if the format is yyyy-mm-dd

=item C<dates_do_not_match>

Takes two dates as strings and returns
true if the dates are different in terms
of day, month, or year

=back

=head1 AUTHOR
Joel Weiser E<lt>joel.weiser@oicr.on.caE<gt>

Copyright (c) 2016 Ontario Institute for Cancer Research

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

use strict;
use warnings;

use base 'Exporter';
use Carp;
use DateTime;
use List::MoreUtils qw/any all/;

use lib '/usr/local/gkb/modules';
use GKB::Config;
use GKB::DBAdaptor;

sub get_dba {
    my $db = shift;
    my $host = shift;
    
    return GKB::DBAdaptor->new (
        -user => $GKB::Config::GK_DB_USER,
        -pass => $GKB::Config::GK_DB_PASS,
        -host => $host,
        -dbname => $db
    );
}

sub get_name_and_id {
    my $instance = shift;
    
    return '' unless $instance && $instance->is_a('DatabaseObject');
    return $instance->displayName . '(' . $instance->db_id . ')';
}

sub get_all_species_in_entity {
    my $entity = shift;
    return unless $entity;
    
    my @species;
    push @species, @{$entity->species} if @{$entity->species};
    
    if ($entity->is_a('Complex')) {
        push @species, get_all_species_in_entity($_) foreach @{$entity->hasComponent};
    } elsif ($entity->is_a('EntitySet')) {
        push @species, get_all_species_in_entity($_) foreach (@{$entity->hasMember}, @{$entity->hasComponent});
    }
    
    my %species;
    $species{$_->db_id} = $_ foreach @species;
    return values %species;
}

sub is_electronically_inferred {
    my $instance = shift;
    
    return 0 unless $instance->inferredFrom->[0] &&
                    $instance->inferredFrom->[0]->species->[0] &&
                    $instance->inferredFrom->[0]->species->[0]->displayName =~ /^Homo sapiens$/i;
                    
    my $dba = $instance->dba();
    my $db_name = $dba->db_name();
    return 0 unless $db_name =~ /^test_reactome_\d+$/ || $db_name eq 'gk_current';
    
    my $release_instance = $dba->fetch_instance(-CLASS => '_Release')->[0];
    return 0 unless $release_instance;
    
    my $release_number = $release_instance->releaseNumber->[0];
    my $slice_dba = get_dba("test_slice_$release_number",$dba->host());
    return 0 unless $slice_dba;
    
    return (defined $slice_dba->fetch_instance_by_db_id($instance->db_id)->[0]) ? 0 : 1;
}

sub has_multiple_species {
    my $instance = shift;
    
    confess 'Instance is not a database object' unless $instance->is_a('DatabaseObject');
    
    return 0 unless $instance->species;
    
    return (scalar @{$instance->species} > 1);
}

sub is_chimeric {
    my $instance = shift;
	
    return $instance->is_valid_attribute('isChimeric') &&
		   $instance->isChimeric->[0] &&
		   $instance->isChimeric->[0] =~ /^true$/i; 
}

sub get_unique_species {
    my @species_instances = @_;
    return unless @species_instances;
    
    croak "\@species_instances contains elements which are not species instances\n"
        unless all { defined $_ && ref($_) eq 'GKB::Instance' } @species_instances;
    
    my @unique_species_instances;
    foreach my $species_instance (sort { length($a->displayName) <=> length($b->displayName) } @species_instances) {
        push @unique_species_instances, $species_instance unless (
            any {
                my $seen_species_display_name = $_->displayName;
                $species_instance->displayName =~ /$seen_species_display_name/ &&
                !XOR(($species_instance->displayName =~ /virus/) ? 1 : 0, ($seen_species_display_name =~ /virus/) ? 1 : 0)
            } @unique_species_instances);
    }
    
    return sort { $a->displayName cmp $b->displayName } @unique_species_instances;    
}

sub XOR {
    my $expression1 = shift;
    my $expression2 = shift;
    
    return ($expression1 || $expression2) && (!($expression1 && $expression2));
}

sub get_instance_modifier {
    my $instance = shift;
    
    return get_event_modifier($instance);
}

sub get_event_modifier {
	my $event = shift;
	
	return 'Unknown' unless $event;
	
	my $author_instance;
    foreach my $modified_instance (reverse @{$event->modified}) {
        $author_instance ||= $modified_instance->author->[0] unless $modified_instance->author->[0] && $modified_instance->author->[0]->db_id == 140537;
    }
	$author_instance ||= $event->created->[0]->author->[0];
	
	my $author_name = $author_instance->displayName if $author_instance;
	
	return $author_name || 'Unknown';
}

sub is_human {
    my $instance = shift;
    
    return (
        $instance->species->[0] &&
        $instance->species->[0]->displayName eq 'Homo sapiens' &&
        !($instance->species->[1]) &&
        !(is_chimeric($instance))
    );
}

sub report {
	my $message = shift;
	my $fh = shift;
	
    return unless $message;
    
	print "$message\n";
	print $fh "$message\n" if $fh;
}

sub date_correctly_formatted {
    my $date = shift;
        
    return 1 if defined get_date_time_object($date);
}

sub dates_do_not_match {
    my $first_date = shift;
    my $second_date = shift;
    
    return DateTime->compare(get_date_time_object($first_date), get_date_time_object($second_date)) != 0;
}

sub get_date_time_object {
    my $date = shift;
    
    my ($year, $month, $day) = $date =~ /^(\d{4})-(\d{2})-(\d{2})$/;
    
    return DateTime->new(
        year => $year,
        month => $month,
        day => $day
    ) if ($year && $month && $day);
}

our @EXPORT = qw/
get_dba
get_name_and_id
get_all_species_in_entity
is_electronically_inferred
has_multiple_species
is_chimeric
get_unique_species
get_instance_modifier
get_event_modifier
is_human
report
date_correctly_formatted
dates_do_not_match
/;

1;
