#!/usr/local/bin/perl  -w
use strict;

use lib "/usr/local/gkb/modules";
use GKB::DBAdaptor;
use GKB::Utils_esther;

use feature qw/say/;
use Carp;
use Data::Dumper;
use Getopt::Long;
use List::MoreUtils ':all';


@ARGV || die "Usage: $0 -user db_user -host db_host -pass db_pass -port db_port -db db_name -class class";

our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db,$opt_debug,$opt_class);

&GetOptions("user:s",
	    "host:s",
	    "pass:s",
	    "port:i",
	    "db=s",
	    "debug",
	    "class=s",
	    );

$opt_db || die "Need database name (-db).\n";

my $dba = GKB::DBAdaptor->new
    (
     -dbname => $opt_db,
     -user   => $opt_user,
     -host   => $opt_host,
     -pass   => $opt_pass,
     -port   => $opt_port,
     -driver => 'mysql',
     -DEBUG => $opt_debug
     );
    
## Prepare date and author for instanceEdit:
my $surname = 'Weiser';
my $initial = 'JD';
my $date    = `date \+\%F`;
chomp $date;

my $instance_edit = GKB::Utils_esther::create_instance_edit( $dba, $surname, $initial, $date );

my $ar = $dba->fetch_instance(-CLASS => 'ReactionlikeEvent');

my %verbs = get_verbs();
my %transfer_groups = get_transfer_groups();

my %events;
foreach my $event (@{$ar}) {
    next if $event->is_a('BlackBoxEvent');
    
    my $inputs = $event->input;
    my $outputs = $event->output;
    my $catalyst_activity = $event->catalystActivity->[0];
    
    if ($catalyst_activity) {
	my $catalyst = $catalyst_activity->physicalEntity->[0];
	my $verb = get_verb($catalyst_activity->activity->[0]->name->[0], \%verbs);
	
	if ($verb eq 'transfers') {
	    my $accession = $catalyst_activity->activity->[0]->accession->[0];
	    my $transfer_group = $transfer_groups{$accession};
	    
	    unless ($transfer_group) {
		carp "$accession has no transfer group";
		next;
	    }
	    push @{$events{$event}}, get_systematic_name_for_transfer($transfer_group, $event, $inputs, $outputs, $catalyst);
	} elsif ($verb eq 'transports') {
	    push @{$events{$event}}, get_systematic_name_for_transport($event, $inputs, $outputs, $catalyst);
	} elsif ($verb eq 'exchanges') {
	    push @{$events{$event}}, get_systematic_name_for_exchange($event, $inputs, $outputs, $catalyst);
	} elsif ($verb eq 'cotransports') {
	    push @{$events{$event}}, get_systematic_name_for_cotransport($event, $inputs, $outputs, $catalyst)
	} else {
	    push @{$events{$event}}, get_systematic_name_for_catalyst($event, $inputs, $outputs, $catalyst, $verb);
	}
    } else {
	if ($event->is_a('Polymerization')) {
	    push @{$events{$event}}, get_systematic_name_for_polymerization($event, $inputs, $outputs);
	} elsif ($event->is_a('Depolymerization')) {
	    push @{$events{$event}}, get_systematic_name_for_depolymerization($event, $inputs, $outputs);
	} elsif (scalar @$inputs < scalar @$outputs) {
	    push @{$events{$event}}, get_systematic_name_for_dissociation($event, $inputs, $outputs);
	} elsif (scalar @$inputs > scalar @$outputs) {
	    push @{$events{$event}}, get_systematic_name_for_binding($event, $inputs, $outputs);
	} elsif (scalar @$inputs == scalar @$outputs) {
	    push @{$events{$event}}, get_systematic_name_for_equal_inputs_and_outputs($event, $inputs, $outputs);
	}
    }
    
    unless ($events{$event}) {
	push @{$events{$event}}, get_systematic_name_for_transformation($event, $inputs, $outputs);
    }
    
    if (scalar @{$events{$event}} > 1) {
	print STDERR $event->db_id . " has more than one assignment: " . join(';', @{$events{$event}}) . "\n";
	next;
    }
    
    print $event->db_id . "\t" . $events{$event}->[0] . "\n"; 
}

sub get_systematic_name_for_equal_inputs_and_outputs {
    my $event = shift;
    my $inputs = shift;
    my $outputs = shift;
    
    my %outputs;
    
    foreach my $input (@$inputs) {
	my $name = $input->systematicName->[0];
	my $compartment = $input->compartment->[0];
	my $cell_type = $input->cellType->[0] || '';
	
	$outputs{$name}{$compartment}{$cell_type}++;
    }
    
    my $type;
    foreach my $input (@$inputs) {
	my $input_name = $input->name->[0];
	my $input_compartment = $input->compartment->[0];
	
	if ($outputs{$input_name}) {
	    if ($outputs{$input_name}{$input_compartment}) {
		$type //= 'activation';
		my $input_cell_type = $input->cellType->[0];
		if ($input_cell_type && $outputs{$input_name}{$input_compartment}{$input_cell_type}) {
		    $type = 'translocation';
		}
	    } else {
	        $type = 'translocation';
	    }
	} else {
	    $type = undef;
	    last;
	}
    }
    
    return get_systematic_name_for_translocation($event, $inputs, $outputs) if $type && $type eq 'translocation';
    return get_systematic_name_for_activation($event, $inputs, $outputs) if $type && $type eq 'activation'; 
    return get_systematic_name_for_transformation($event, $inputs, $outputs);
}

sub get_systematic_name_for_activation {
    my $event = shift;
    my $inputs = shift;
    my $outputs = shift;
    
    my $input_count = scalar @$inputs;
    
    return join(',', map({$_->name->[0]} @$inputs)) .
	   ($input_count > 1) ? 'are' : 'is' .
	   'ACTIVATED';
}

sub get_systematic_name_for_translocation {
    my $event = shift;
    my $inputs = shift;
    my $outputs = shift;
    
    my $input_count = scalar @$inputs;
    my $input_names = join(',', map({$_->name->[0]} @$inputs));
    
    my @input_compartments = uniq(map({$_->compartment->[0]->name->[0]} @$inputs));
    if (scalar @input_compartments > 1) {
	carp "More than one input compartment - @input_compartments for " . $event->db_id;
	return;
    }

    my @input_cell_types = uniq(map({$_->cellType->[0]->name->[0]} @$inputs));
    if (scalar @input_cell_types > 1) {
	carp "More than one input cell type - @input_cell_types for " . $event->db_id;
	return;
    }
    
    my @output_compartments = uniq(map({$_->compartment->[0]->name->[0]} @$outputs));   
    if (scalar @output_compartments > 1) {
	carp "More than one output compartment - @output_compartments for " . $event->db_id;
	return;
    }

    my @output_cell_types = uniq(map({$_->cellType->[0]->name->[0]} @$outputs));
    if (scalar @output_cell_types > 1) {
	carp "More than one output cell type - @output_cell_types for " . $event->db_id;
	return;
    }
    
    return $input_names . " TRANSLOCATE" .
	    ($input_count == 1 ? 'S' : '') .
	    ' FROM ' . $input_compartments[0] .
	    (defined $input_cell_types[0] ? " of cell type " . $input_cell_types[0] : '') .
	    ' TO ' . $output_compartments[0] .
	    (defined $output_cell_types[0] ? " of cell type " . $output_cell_types[0] : '');
}

sub get_systematic_name_for_binding {
    my $event = shift;
    my $inputs = shift;
    my $outputs = shift;
    
    my $input_count = scalar @$inputs;    
    if ($input_count < 2) {
	carp "Not enough inputs for binding for reaction " . $event->db_id;
	return;
    }
    
    return $inputs->[0]->name->[0] . ' BINDS ' . $inputs->[1]->name->[0] . " forming " .  join(',', map({$_->name->[0]} @$outputs)) if $input_count == 2;
    return join(',', map({$_->name->[0]} @$inputs)) . ' BIND forming ' .  join(',', map({$_->name->[0]} @$outputs));
}

sub get_systematic_name_for_dissociation {    
    return get_systematic_name_for('DISSOCIATE', @_);    
}

sub get_systematic_name_for_polymerization {
    return get_systematic_name_for('POLYMERIZE', @_);
}

sub get_systematic_name_for_depolymerization {
    return get_systematic_name_for('DEPOLYMERIZE', @_);
}

sub get_systematic_name_for_transformation {
    return get_systematic_name_for('TRANSFORM', @_);    
}

sub get_systematic_name_for {
    my $verb = shift;
    my $event = shift;
    my $inputs = shift;
    my $outputs = shift;
        
    my $input_count = scalar @$inputs;
    my $input_names = join(',', map({$_->name->[0]} @$inputs));
    my $output_names = join(',', map({$_->name->[0]} @$outputs));

    return $input_names . " " . $verb .
	    ($input_count == 1 ? 'S' : '') . 
	    ' TO ' . $output_names;
}

sub get_systematic_name_for_transfer {
    my $transfer_group = shift;
    my $event = shift;
    my $inputs = shift;
    my $outputs = shift;
    my $catalyst = shift;
    
    my $input_names = join(',', map({$_->name->[0]} @$inputs));
    my $output_names = join(',', map({$_->name->[0]} @$outputs));
    
    return $catalyst->name->[0] . ' TRANSFERS ' . $transfer_group . ' TO ' .
	    $input_names . ' TO FORM ' . $output_names;
}

sub get_systematic_name_for_transport {
    my $event = shift;
    my $inputs = shift;
    my $outputs = shift;
    my $catalyst = shift;
    
    my $input_names = join(',', map({$_->name->[0]} @$inputs));
    
    my @input_compartments = uniq(map({$_->compartment->[0]->name->[0]} @$inputs));
    if (scalar @input_compartments > 1) {
	carp "More than one input compartment - @input_compartments for " . $event->db_id;
	return;
    }
    
    my @output_compartments = uniq(map({$_->compartment->[0]->name->[0]} @$outputs));   
    if (scalar @output_compartments > 1) {
	carp "More than one output compartment - @output_compartments for " . $event->db_id;
	return;
    }   
    
    return $catalyst->name->[0] . ' TRANSPORTS ' . $input_names .
	    ' FROM ' . $input_compartments[0] . ' TO ' .
	    $output_compartments[0];
}

sub get_systematic_name_for_exchange {
    my $event = shift;
    my $inputs = shift;
    my $outputs = shift;
    my $catalyst = shift;
    
    my $first_input = shift @{$inputs};
    
    return $catalyst->name->[0] . ' EXCHANGES ' . $first_input->name->[0] .
	    ' FOR ' . join(',', map({$_->name->[0]} @$inputs)) .
	    ' (across the ' . $catalyst->compartment->[0]->name->[0] . ' membrane)';
}

sub get_systematic_name_for_cotransport {
    my $event = shift;
    my $inputs = shift;
    my $outputs = shift;
    my $catalyst = shift;
    
    my $first_input = shift @{$inputs};
    
    return $catalyst->name->[0] . ' COTRANSPORTS ' . $first_input->name->[0] .
	    ' WITH ' . join(',', map({$_->name->[0]} @$inputs));
}

sub get_systematic_name_for_catalyst {
    my $event = shift;
    my $inputs = shift;
    my $outputs = shift;
    my $catalyst = shift;
    my $verb = shift;
    
    return unless $catalyst && $verb;
    
    my $input_names = join(',', map({$_->name->[0]} @$inputs));
    my $output_names = join(',', map({$_->name->[0]} @$outputs));
    
    return $catalyst->name->[0] . ' ' . $verb . ' ' .
	    $input_names . ' TO ' . $output_names;

}

sub get_verbs {
    my $verb_lookup = `wget -qO- 'https://docs.google.com/spreadsheets/d/12YAvDGr4r_SI3PI8IDfKMHXkDZcCgRUSKK-kmmRRjKg/export?gid=8&format=csv'`;
    my @rows = split "\n", $verb_lookup;
    
    my %verbs;
    foreach my $row (@rows) {
	my @columns = split ",", $row;
	
	my $activity = $columns[0];
	my $verb = $columns[1];
	
	$activity =~ s/"//g;
	$verb =~ s/\?//g;
	
	$verbs{$activity} = $verb;
    }
    
    return %verbs;
}

sub get_verb {
    my $activity = shift;
    my $verbs = shift;
    
    foreach my $activity_phrase (sort {length($b) <=> length($a)} keys %$verbs) {
	return $verbs->{$activity_phrase} if $activity =~ /$activity_phrase/;
    }
    
    carp "No verbs for $activity";
}

sub get_transfer_groups {
    my $transfer_group_lookup = `wget -qO- 'https://docs.google.com/spreadsheets/d/12YAvDGr4r_SI3PI8IDfKMHXkDZcCgRUSKK-kmmRRjKg/export?gid=5&format=csv'`;
    my @rows = split "\n", $transfer_group_lookup;
    
    my %transfer_groups;
    foreach my $row (@rows) {
	my @columns = split ",", $row;
	
	my $go_accession = $columns[0];
	my $transfer_group = $columns[2];
	
	$transfer_groups{$go_accession} = $transfer_group;
    }
    
    return %transfer_groups;
}
