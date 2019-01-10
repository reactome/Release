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

use feature qw/state/;

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

sub restore_database {
    my $db = shift;
    my $host = shift;
    my $source = shift;
    my $overwrite = shift;
    my $backup = shift;

    if (database_exists($db, $host) && $backup) {
        backup_database($db, $host);
    }

    create_database($db, $host, $overwrite);
    populate_database($db, $host, $source);
}

sub backup_database {
    my $db = shift;
    my $host = shift;

    my $user = $GKB::Config::GK_DB_USER;
    my $pass = $GKB::Config::GK_DB_PASS;
    my $timestamp = localtime(time);

    return (system("mysqldump -u $user -p$pass -h $host $db > $db.$timestamp.dump") == 0);
}

sub create_database {
    my $db = shift;
    my $host = shift;
    my $overwrite = shift;

    if (database_exists($db, $host) && !$overwrite) {
        confess "Database $db on $host already exists\n";
    }

    my $user = $GKB::Config::GK_DB_USER;
    my $pass = $GKB::Config::GK_DB_PASS;

    return (system("mysql -u $user -p$pass -h $host -e 'drop database if exists $db; create database $db'") == 0);
}

sub populate_database {
    my $db = shift;
    my $host = shift;
    my $source = shift;

    my $user = $GKB::Config::GK_DB_USER;
    my $pass = $GKB::Config::GK_DB_PASS;

    my $stream = ($source =~ /\.gz$/) ? 'zcat' : 'cat';
    return (system("$stream $source | mysql -u $user -p$pass -h $host $db") == 0);
}

sub database_exists {
    my $db = shift;
    my $host = shift;

    my $user = $GKB::Config::GK_DB_USER;
    my $pass = $GKB::Config::GK_DB_PASS;

    return (system("mysql -u $user -p$pass -h $host -e 'use $db' 2> /dev/null") == 0);
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

    my $dba = $instance->dba();
    my $db_name = $dba->db_name();
    return 0 unless $db_name =~ /^test_reactome_\d+$/ || $db_name eq 'gk_current';

    # first, let's check to see if the "magic" note is in the Created InstanceEdit for this object.
    my $created = $instance->created->[0];
    if ($created && $created->note && $created->note =~ /inferred events based on (ensembl compara|panther)/) {
        return 1;
    }
    # If there's no note to indicate that the object was created during orthoinference,
    # try the old logic.
    if ($instance->is_a('Event')) {
        return $instance->evidenceType->[0] && $instance->evidenceType->[0]->displayName =~ /electronic/i;
    } elsif ($instance->is_a('PhysicalEntity')) {
        return !in_curator_database($instance) && has_human_source_instance($instance);
    } elsif ($instance->is_a('CatalystActivity')) {
        return any { is_electronically_inferred($_)} @{$instance->reverse_attribute_value('catalystActivity')};
    } elsif ($instance->is_a('Regulation')) {
        # For backwards compatability when regulations contained the reaction like event (RLE) they regulated
        # As of Reactome version 65, RLEs have contained their regulation instances in the 'regulatedBy' attribute
        my @regulated_events = $instance->regulatedEntity->[0] ?
            @{$instance->regulatedEntity} : @{$instance->reverse_attribute_value('regulatedBy')};

        return any { is_electronically_inferred($_)} @regulated_events;
    }
}

sub in_curator_database {
    my $instance = shift;

    state $curator_database_physical_entity_lookup = fetch_curator_database_physical_entity_lookup();

    my $curator_database_instance = $curator_database_physical_entity_lookup->{$instance->db_id};

    return $curator_database_instance &&
        $instance->class eq $curator_database_instance->class &&
        $instance->displayName eq $curator_database_instance->displayName &&
        have_same_species($instance, $curator_database_instance);
}

sub have_same_species {
    my $first_instance = shift;
    my $second_instance = shift;

    my @first_instance_species = map {$_->displayName} @{$first_instance->species};
    my @second_instance_species = map {$_->displayName} @{$second_instance->species};

    if (scalar @first_instance_species != scalar @second_instance_species) {
        return 0;
    }

    foreach my $first_instance_species (@first_instance_species) {
        # The same species list do not match if the first instance's currently
        # considered species is not matched to any species in the second
        # instance
        return 0 if (!(any {$_ eq $first_instance_species} @second_instance_species));
    }

    # The species lists must match if they have the same size and all species in
    # the first list are found in the second list
    return 1;
}

sub has_human_source_instance {
    my $instance = shift;

    # If manually inferred physical entities ever start using the inferredFrom slot, this logic
    # can be augmented to check the event(s) to which the physical entity is/are attached
    # to see if the event(s) are electronically inferred
    return $instance->inferredFrom->[0] &&
        $instance->inferredFrom->[0]->species->[0] &&
        $instance->inferredFrom->[0]->species->[0]->displayName =~ /^Homo sapiens$/i;
}

sub fetch_curator_database_physical_entity_lookup {
    my $database = shift // $GKB::Config::GK_CURATOR_DB_NAME;
    my $host = shift // $GKB::Config::GK_CURATOR_DB_HOST;

    my $dba = get_dba($database, $host);

    my @physical_entities = @{$dba->fetch_instance(-CLASS => 'PhysicalEntity')};

    my %physical_entity_lookup = map { $_->db_id => $_ } @physical_entities;

    return \%physical_entity_lookup;
}

sub get_source_for_electronically_inferred_instance {
    my $instance = shift;
    return unless is_electronically_inferred($instance);

    if ($instance->is_a('Event') || $instance->is_a('PhysicalEntity')) {
        return @{$instance->inferredFrom};
    } elsif ($instance->is_a('CatalystActivity')) {
        my @source_physical_entities = get_source_for_electronically_inferred_instance($instance->physicalEntity->[0]);

        # The physical entity of the inferred catalyst activity may not be an electronically inferred instance
        # but the same instance used by the source catalyst activity (e.g. simple entities without species).
        # This is assumed when a source physical entity can't be found from the inferred catalyst activity's
        # physical entity
        @source_physical_entities = ($instance->physicalEntity->[0]) if scalar @source_physical_entities == 0;

        my @potential_source_catalyst_activities = grep {$instance->db_id != $_->db_id} map {@{$_->reverse_attribute_value('physicalEntity')}} @source_physical_entities;
        return grep {$_->activity->[0]->db_id == $instance->activity->[0]->db_id} @potential_source_catalyst_activities;
    } elsif ($instance->is_a('Regulation')) {
        if ($instance->is_valid_attribute('inferredFrom')) {
            return @{$instance->inferredFrom} if @{$instance->inferredFrom}; # Only present for version 59 and onward
        }

        my @source_regulated_entities = get_source_for_electronically_inferred($instance->reverse_attribute_value('regulatedBy'));
        my @source_regulators = get_source_for_electronically_inferred($instance->regulator->[0]);

        # Source regulation instance(s) will have both a source regulated entity and
        # a source regulator used to infer the inferred regulation instance passed
        # to the subroutine
        return intersection_of_database_instance_lists(
            [map {@{$_->regulatedBy}} @source_regulated_entities],
            [map {@{$_->reverse_attribute_value('regulator')}} @source_regulators]
        );
    }
}

sub intersection_of_database_instance_lists {
    my $first_database_instance_list = shift;
    my $second_database_instance_list = shift;

    my %db_id_to_instance;
    my %db_id_to_count;
    foreach my $instance (unique_database_instances(@{$first_database_instance_list}), unique_database_instances(@{$second_database_instance_list})) {
        $db_id_to_instance{$instance->db_id} = $_;
        $db_id_to_count{$instance->db_id}++;
    }

    confess "Instance with db id $_ seen " . $db_id_to_count{$_} . " times" foreach grep {$db_id_to_count{$_} != 1 || $db_id_to_count{$_} != 2} keys %db_id_to_count;

    return values %db_id_to_instance;
}

sub unique_database_instances {
    my @database_instances = @_;

    my %db_id_to_instance;
    $db_id_to_instance{$_->db_id} = $_ foreach @database_instances;
    return values %db_id_to_instance;
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

    my @sorted_unique_species_instances = sort { $a->displayName cmp $b->displayName } @unique_species_instances;
    return @sorted_unique_species_instances;
}

sub XOR {
    my $expression1 = shift;
    my $expression2 = shift;

    return ($expression1 || $expression2) && (!($expression1 && $expression2));
}

sub get_components {
    my $composite_entity = shift;

    my @components;
    if ($composite_entity) {
        foreach my $component (get_composite_entity_subunits($composite_entity)) {
            push @components, $component;
            my @sub_components = grep {defined} get_components($component);
            if (@sub_components) {
                push @components, @sub_components;
            }
        }
    }

    return @components;
}

sub get_composite_entity_subunits {
    my $composite_entity = shift;

    return () if !$composite_entity;

    my @subunits;

    push @subunits, @{$composite_entity->hasComponent};
    push @subunits, @{$composite_entity->hasMember};
    push @subunits, @{$composite_entity->hasCandidate};
    push @subunits, @{$composite_entity->repeatedUnit};

    return grep { defined } @subunits;
}

sub get_instance_creator {
    my $instance = shift;

    if ($instance->created->[0] && $instance->created->[0]->author->[0]) {
        return $instance->created->[0]->author->[0]->displayName || 'Unknown';
    }

    return 'Unknown';
}

sub get_instance_modifier {
    my $instance = shift;

    return get_event_modifier($instance);
}

sub get_event_modifier {
    my $event = shift;

    return 'Unknown' unless $event;

    my $author_instance;
    try {
        foreach my $modified_instance (reverse @{$event->modified}) {
            $author_instance ||= $modified_instance->author->[0] unless $modified_instance->author->[0] && $modified_instance->author->[0]->db_id == 140537;
        }
        $author_instance ||= $event->created->[0]->author->[0];
    } catch {
        confess "Error caught: $_ \nFor event: ".$event->extended_displayName;
    };
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

sub get_unique_instances {
    my @instances = @_;

    my %db_id_to_instance;
    foreach my $instance (@instances) {
        $db_id_to_instance{$instance->db_id} = $instance;
    }

    return values %db_id_to_instance;
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
backup_database
restore_database
database_exists
get_name_and_id
get_all_species_in_entity
is_electronically_inferred
in_curator_database
have_same_species
get_source_for_electronically_inferred_instance
has_multiple_species
is_chimeric
get_unique_species
get_components
get_instance_creator
get_instance_modifier
get_event_modifier
is_human
get_unique_instances
report
date_correctly_formatted
dates_do_not_match
/;

1;
