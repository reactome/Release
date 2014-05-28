package GKB::SearchUtils::ExplanatoryInstances;

=head1 NAME

GKB::SearchUtils::ExplanatoryInstances

=head1 SYNOPSIS

Add explanatory instances to a list of instances.

=head1 DESCRIPTION

Explanatory instances help to make sense of the existing instances
in a list, e.g. the pathways in which a given PhysicalEntity is
involved.

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
use vars qw(@ISA $AUTOLOAD %ok_field);
use Bio::Root::Root;
use Carp;
use GKB::SearchUtils::ExplanatoryInstanceDatabase;

@ISA = qw(Bio::Root::Root);

for my $attr
    (qw(
    explanatory_instance_database
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

# Optionally, you can supply an ExplanatoryInstanceDatabase object as
# an argument to the constructor.  This can make add_explanatory_instances
# run faster.
sub new {
    my($pkg, $explanatory_instance_database) = @_;
    my $self = bless {}, $pkg;
    
    $self->explanatory_instance_database($explanatory_instance_database);
    
    return $self;
}

# Needed by subclasses
sub get_ok_field {
	return %ok_field;
}

# For selected classes of instances, this method looks to see
# what other instances can be found that might help
# to make sense of them.
#
# The first argument is a reference to the array of instances
# that we wish to find additional explanatory instances for.
#
# The second argument is a species DB_ID, which constrains
# the species that this method looks for.
#
# The third argument is an optional flag; if set to 1, then
# this method will look to see if it can first get the information
# it needs out of a pre-generated explanatory instance database.
# This ought to run a bit faster.
#
# The returned value is a reference to an expanded array of
# instances, which contains the original instances plus any
# extra ones found.
sub add_explanatory_instances {
	my($self, $instances, $species_db_id, $use_ie_database_flag) = @_;
	
	if (!(defined $instances)) {
		return [];
	}
	if (!(defined $use_ie_database_flag)) {
		$use_ie_database_flag = 0;
	}
	
	my $instance;
	my $instance_hash;
	foreach $instance (@{$instances}) {
		# Check the species before adding this instance.  If
		# $species_db_id is undef, then accept all species.
		if (!(defined $species_db_id) || $species_db_id eq '' ||
			!($instance->is_valid_attribute("species"))  ||
			!(defined $instance->species) ||
			!(scalar(@{$instance->species})>0) ||
			($instance->is_valid_attribute("species") &&
			 defined $instance->species &&
			 scalar(@{$instance->species})>0 &&
			 $instance->species->[0]->db_id() == $species_db_id)) {
			 	
			$instance_hash->{$instance->db_id()} = $instance;
		}
	}
	
	$self->add_explanatory_instances_to_hash($instance_hash);
		
	my @explanatory_instance_array = values(%{$instance_hash});
	
	# Remove anything coming from other species, if they have
	# been inserted.  If $species_db_id is undef, then accept
	# all species.
	if (defined $species_db_id) {
		my @new_explanatory_instance_array = ();
		foreach $instance (@explanatory_instance_array) {
			# Check the species before adding this instance
			if (!(defined $species_db_id) || $species_db_id eq '' ||
				!($instance->is_valid_attribute("species"))  ||
				!(defined $instance->species) ||
				!(scalar(@{$instance->species})>0) ||
				($instance->is_valid_attribute("species") &&
				 defined $instance->species &&
				 scalar(@{$instance->species})>0 &&
				 $instance->species->[0]->db_id() == $species_db_id)) {
				 	
				push(@new_explanatory_instance_array, $instance);
			}
		}
		@explanatory_instance_array = @new_explanatory_instance_array;
	}

	return \@explanatory_instance_array;
}

# This returns exactly the same information as add_explanatory_instances,
# but it tries to get the information from an explanatory instance database
# if it can.  If it can't, then it will call add_explanatory_instances, so
# you have nothing to lose by using this method.  The advantage is, if the
# database exists, then it is a much quicker way of getting the information.
sub add_explanatory_instances_from_database {
	my($self, $instances, $species_db_id, $use_ie_database_flag) = @_;
	
	if (!(defined $instances)) {
		return [];
	}
	
	# If we can find an explanatory instance database, loop over the
	# supplied instances and pull the explanatory instances for each one
	# from the database.
	my $eid = $self->explanatory_instance_database;
	if (defined $eid) {
		my $dbh = $eid->connect_database();
		if (defined $dbh) {
			my $instance;
			my $db_id;
			my @db_ids = ();
			foreach $instance (@{$instances}) {
				$db_id = $instance->db_id();
				push(@db_ids, $eid->select($db_id, $species_db_id));
			}
			
			# TODO: extract instances from list of DB_IDs and append to $instances
			
			$eid->disconnect_database();
			return $instances;
		}
	}
	
	# No explanatory instance database - do things the slow way.
	return $self->add_explanatory_instances($instances, $species_db_id, $use_ie_database_flag);
}

sub add_explanatory_instances_to_hash {
	my($self, $instance_hash) = @_;
	
	# Oo-er, this is a bit dodgy!  If we have lots of results, don't
	# add any extra physical entities, it slows things down too much.
	# Some interesting stuff might get missed because of this.  So it
	# goes.
#	if (scalar(keys(%{$instance_hash}))<=200) {
		$self->add_physicalentitys_from_referenceentity_instances_to_hash($instance_hash);
#	}
	$self->add_complexes_and_sets_from_physicalentity_instances_to_hash($instance_hash);
	$self->add_reactions_catalysts_regulators_from_physicalentity_instances_to_hash($instance_hash);
	$self->add_catalystactivitys_from_physical_entity_instances_to_hash($instance_hash);
	$self->add_catalystactivitys_from_go_molecularfunction_instances_to_hash($instance_hash);
	$self->add_reactions_regulators_from_catalystactivity_instances_to_hash($instance_hash);
	$self->add_events_from_summation_instances_to_hash($instance_hash);
	$self->add_pathways_from_event_instances_to_hash($instance_hash);
	$self->add_entitys_from_modifiedresidue_instances_to_hash($instance_hash);
	$self->add_referenceentitys_from_physicalentitys_instances_to_hash($instance_hash);
	$self->add_events_from_other_species($instance_hash);
}

# Get PhysicalEntitys from any of the reference entities present.
sub add_physicalentitys_from_referenceentity_instances_to_hash {
	my($self, $instance_hash) = @_;
	
	my $instance;
	my $explanatory_instance;
	my $explanatory_instances;

	foreach $instance (values(%{$instance_hash})) {
		if ($instance->is_a("ReferenceEntity")) {
			$explanatory_instances = $instance->reverse_attribute_value('referenceEntity');
			foreach $explanatory_instance (@{$explanatory_instances}) {
				$instance_hash->{$explanatory_instance->db_id()} = $explanatory_instance;
			}
		}
	}	
}

## Get PhysicalEntitys from any of the reference entities present.
#sub add_physicalentitys_from_definedset_instances_to_hash {
#	my($self, $instance_hash) = @_;
#	
#	my $instance;
#
#	foreach $instance (values(%{$instance_hash})) {
#		$self->collect_super_definedsetes($instance, $instance_hash);
#	}
#}

# Get complexes by reverse attribute from PhysicalEntitys
sub add_complexes_and_sets_from_physicalentity_instances_to_hash {
	my($self, $instance_hash) = @_;
	
	my $instance;

	foreach $instance (values(%{$instance_hash})) {
		$self->collect_super_complexes_and_sets($instance, $instance_hash);
	}
}

# Get reactions, catalysts and regulators by reverse attribute from PhysicalEntitys
sub add_reactions_catalysts_regulators_from_physicalentity_instances_to_hash {
	my($self, $instance_hash) = @_;
	
	my $instance;
	my $explanatory_instance;
	my $explanatory_instances;

	foreach $instance (values(%{$instance_hash})) {
		if ($instance->is_a("PhysicalEntity")) {
			$explanatory_instances = $instance->reverse_attribute_value('input');
			foreach $explanatory_instance (@{$explanatory_instances}) {
				$instance_hash->{$explanatory_instance->db_id()} = $explanatory_instance;
			}
			$explanatory_instances = $instance->reverse_attribute_value('output');
			foreach $explanatory_instance (@{$explanatory_instances}) {
				$instance_hash->{$explanatory_instance->db_id()} = $explanatory_instance;
			}
			$explanatory_instances = $instance->reverse_attribute_value('physicalEntity');
			foreach $explanatory_instance (@{$explanatory_instances}) {
				$instance_hash->{$explanatory_instance->db_id()} = $explanatory_instance;
			}
			$explanatory_instances = $instance->reverse_attribute_value('regulator');
			foreach $explanatory_instance (@{$explanatory_instances}) {
				$instance_hash->{$explanatory_instance->db_id()} = $explanatory_instance;
			}
		}
	}
}

# Get catalyst activities by reverse attribute from GO_MolecularFunctions
sub add_catalystactivitys_from_go_molecularfunction_instances_to_hash {
	my($self, $instance_hash) = @_;
	
	my $instance;
	my $explanatory_instance;
	my $explanatory_instances;

	foreach $instance (values(%{$instance_hash})) {
		if ($instance->is_a("GO_MolecularFunction")) {
			$explanatory_instances = $instance->reverse_attribute_value('activity');
			foreach $explanatory_instance (@{$explanatory_instances}) {
				$instance_hash->{$explanatory_instance->db_id()} = $explanatory_instance;
			}
		}
	}	
}

# Get catalyst activities by reverse attribute from PhysicalEntity
sub add_catalystactivitys_from_physical_entity_instances_to_hash {
	my($self, $instance_hash) = @_;
	
	my $instance;
	my $explanatory_instance;
	my $explanatory_instances;

	foreach $instance (values(%{$instance_hash})) {
		if ($instance->is_a("PhysicalEntity")) {
			$explanatory_instances = $instance->reverse_attribute_value('physicalEntity');
			foreach $explanatory_instance (@{$explanatory_instances}) {
				if ($explanatory_instance->is_a("CatalystActivity")) {
				    $instance_hash->{$explanatory_instance->db_id()} = $explanatory_instance;
				}
			}
		}
	}	
}

# Get reactions and regulators by reverse attribute from CatalystActivitys
sub add_reactions_regulators_from_catalystactivity_instances_to_hash {
	my($self, $instance_hash) = @_;
	
	my $instance;
	my $explanatory_instance;
	my $explanatory_instances;

	foreach $instance (values(%{$instance_hash})) {
		if ($instance->is_a("CatalystActivity")) {
			# If there is only one regulated instance, just take
			# that; if there are many, then (somewhat arbitrarily)
			# take only the human one.  Really, we ought to take
			# the one for the species over which the query was made.
			$explanatory_instances = $instance->reverse_attribute_value('catalystActivity');
			if (scalar(@{$explanatory_instances})==1) {
				$explanatory_instance = $explanatory_instances->[0];
				$instance_hash->{$explanatory_instance->db_id()} = $explanatory_instance;
			} else {
				foreach $explanatory_instance (@{$explanatory_instances}) {
					if ($explanatory_instance->species->[0]->name->[0] eq "Homo sapiens") {
						$instance_hash->{$explanatory_instance->db_id()} = $explanatory_instance;
					}
				}
			}
			$explanatory_instances = $instance->reverse_attribute_value('regulator');
			foreach $explanatory_instance (@{$explanatory_instances}) {
				$instance_hash->{$explanatory_instance->db_id()} = $explanatory_instance;
			}
		}
	}	
}

# Get events by reverse attribute from Summations
sub add_events_from_summation_instances_to_hash {
	my($self, $instance_hash) = @_;
	
	my $instance;
	my $explanatory_instance;
	my $explanatory_instances;

	foreach $instance (values(%{$instance_hash})) {
		if ($instance->is_a("Summation")) {
			$explanatory_instances = $instance->reverse_attribute_value('summation');
			foreach $explanatory_instance (@{$explanatory_instances}) {
				$instance_hash->{$explanatory_instance->db_id()} = $explanatory_instance;
			}
		}
	}	
}

# Get pathways by reverse attribute from Reactions
sub add_pathways_from_event_instances_to_hash {
	my($self, $instance_hash) = @_;
	
	my $instance;
	my $explanatory_instance;
	my $explanatory_instances;

	foreach $instance (values(%{$instance_hash})) {
		if ($instance->is_a("Event")) {
		$self->collect_super_pathways($instance, $instance_hash);
#			# Do 'hasComponent' for backward compatibility to old data model
#			$explanatory_instances = $instance->reverse_attribute_value('hasComponent');
#			foreach $explanatory_instance (@{$explanatory_instances}) {
#				$instance_hash->{$explanatory_instance->db_id()} = $explanatory_instance;
#			}
#			$explanatory_instances = $instance->reverse_attribute_value('hasEvent');
#			foreach $explanatory_instance (@{$explanatory_instances}) {
#				my $explanatory_instance_name = $explanatory_instance->_displayName->[0];
#				if ($explanatory_instance_name =~ /Cell Cycle/ || $explanatory_instance_name =~ /p53 and Sin3a bind c-Myb/ || $explanatory_instance_name =~ /Hemostasis/) {
#				    print STDERR "ExplanatoryInstances.add_pathways_from_event_instances_to_hash: explanatory_instance_name=$explanatory_instance_name\n";
#				}
#				$instance_hash->{$explanatory_instance->db_id()} = $explanatory_instance;
#			}
#			$explanatory_instances = $instance->reverse_attribute_value('regulator');
#			foreach $explanatory_instance (@{$explanatory_instances}) {
#				$instance_hash->{$explanatory_instance->db_id()} = $explanatory_instance;
#			}
		}
	}	
}

# Get entities by reverse attribute from ModifiedResidues
sub add_entitys_from_modifiedresidue_instances_to_hash {
	my($self, $instance_hash) = @_;
	
	my $instance;
	my $explanatory_instance;
	my $explanatory_instances;

	foreach $instance (values(%{$instance_hash})) {
		if ($instance->is_a("ModifiedResidue")) {
			$explanatory_instances = $instance->reverse_attribute_value('hasModifiedResidue');
			foreach $explanatory_instance (@{$explanatory_instances}) {
				$instance_hash->{$explanatory_instance->db_id()} = $explanatory_instance;
			}
		}
	}	
}

# For PhysicalEntitys, it would probably be better
# to have the corresponding ReferenceEntitys
sub add_referenceentitys_from_physicalentitys_instances_to_hash {
	my($self, $instance_hash) = @_;
	
	my $instance;
	my $explanatory_instance;
	my $explanatory_instances;

	foreach $instance (values(%{$instance_hash})) {
			$explanatory_instances = $instance->reverse_attribute_value('catalystActivity');
			if ($instance->is_a("PhysicalEntity") && $instance->is_valid_attribute("referenceEntity") && (defined $instance->referenceEntity)) {
			foreach $explanatory_instance (@{$instance->referenceEntity}) {
				$instance_hash->{$explanatory_instance->db_id()} = $explanatory_instance;
			}
		}
	}
}

# Get events that have been inferred by orthology from other species.
sub add_events_from_other_species {
	my($self, $instance_hash) = @_;
	
	my $instance;
	my $explanatory_instance;
	my $explanatory_instances;

	foreach $instance (values(%{$instance_hash})) {
		if ($instance->is_a("Event")) {
			$explanatory_instances = $instance->orthologousEvent;
			foreach $explanatory_instance (@{$explanatory_instances}) {
				$instance_hash->{$explanatory_instance->db_id()} = $explanatory_instance;
			}
		}
	}	
}

# If the supplied instance is a PhysicalEntity, find all complexes,
# super-complexes, super-super-complexes, etc. that is is involved
# in.  The second argument must be a reference to a hash, mapping
# DB_IDs to instances.  The level argument is there to allow you to
# insert diagnostics; it can be left out without causing problems.
sub collect_super_complexes_and_sets {
	my($self, $instance, $instance_hash, $level) = @_;
	
	# Get complexes by reverse attribute from PhysicalEntitys
	if ($instance->is_a("PhysicalEntity")) {
		if (!(defined $level)) {
			$level = 0;
		}
	
		my $explanatory_instance;
		my $explanatory_instances;
		$explanatory_instances = $instance->reverse_attribute_value('hasComponent');
		foreach $explanatory_instance (@{$explanatory_instances}) {
			if (!$explanatory_instance->is_a("Complex")) {
			    next;
			}
			$instance_hash->{$explanatory_instance->db_id()} = $explanatory_instance;
			$self->collect_super_complexes_and_sets($explanatory_instance, $instance_hash, $level+1); # recurse
		}
		$explanatory_instances = $instance->reverse_attribute_value('hasMember');
		foreach $explanatory_instance (@{$explanatory_instances}) {
			if (!$explanatory_instance->is_a("EntitySet")) {
			    next;
			}
			$instance_hash->{$explanatory_instance->db_id()} = $explanatory_instance;
			$self->collect_super_complexes_and_sets($explanatory_instance, $instance_hash, $level+1); # recurse
		}
	}
}

## If the supplied instance is a PhysicalEntity, find all definedsetes,
## super-definedsetes, super-super-definedsetes, etc. that is is involved
## in.  The second argument must be a reference to a hash, mapping
## DB_IDs to instances.  The level argument is there to allow you to
## insert diagnostics; it can be left out without causing problems.
#sub collect_super_definedsetes {
#	my($self, $instance, $instance_hash, $level) = @_;
#	
#	# Get definedsetes by reverse attribute from PhysicalEntitys
#	if ($instance->is_a("PhysicalEntity")) {
#		if (!(defined $level)) {
#			$level = 0;
#		}
#	
#		my $explanatory_instance;
#		my $explanatory_instances;
#		$explanatory_instances = $instance->reverse_attribute_value('hasMember');
#		foreach $explanatory_instance (@{$explanatory_instances}) {
#			$instance_hash->{$explanatory_instance->db_id()} = $explanatory_instance;
#			$self->collect_super_definedsetes($explanatory_instance, $instance_hash, $level+1); # recurse
#		}
#	}
#}
#
1;


# If the supplied instance is a PhysicalEntity, find all definedsetes,
# super-definedsetes, super-super-definedsetes, etc. that is is involved
# in.  The second argument must be a reference to a hash, mapping
# DB_IDs to instances.  The level argument is there to allow you to
# insert diagnostics; it can be left out without causing problems.
sub collect_super_pathways {
	my($self, $instance, $instance_hash, $level) = @_;
	
	# Get Pathways by reverse attribute from Events
	if ($instance->is_a("Event")) {
		if (!(defined $level)) {
			$level = 0;
		}
	
		my $explanatory_instance;
		my $explanatory_instances;
		# Do 'hasComponent' for backward compatibility to old data model
		$explanatory_instances = $instance->reverse_attribute_value('hasComponent');
		foreach $explanatory_instance (@{$explanatory_instances}) {
			$instance_hash->{$explanatory_instance->db_id()} = $explanatory_instance;
			$self->collect_super_pathways($explanatory_instance, $instance_hash, $level+1); # recurse
		}
		$explanatory_instances = $instance->reverse_attribute_value('hasEvent');
		foreach $explanatory_instance (@{$explanatory_instances}) {
			$instance_hash->{$explanatory_instance->db_id()} = $explanatory_instance;
			$self->collect_super_pathways($explanatory_instance, $instance_hash, $level+1); # recurse
		}
		$explanatory_instances = $instance->reverse_attribute_value('regulator');
		foreach $explanatory_instance (@{$explanatory_instances}) {
			$instance_hash->{$explanatory_instance->db_id()} = $explanatory_instance;
			$self->collect_super_pathways($explanatory_instance, $instance_hash, $level+1); # recurse
		}
	}
}

1;

