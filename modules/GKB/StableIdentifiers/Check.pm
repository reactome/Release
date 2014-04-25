=head1 NAME

GKB::StableIdentifiers::Check

=head1 SYNOPSIS

Validation subroutines to cross-check stable identifiers and
make sure that they are consistent.

=head1 DESCRIPTION

An object of this class needs to have a StableIdentifiers object
in order to function correctly:

my $stable_identifiers = GKB::StableIdentifiers->new();

 :
 :
 
my $check = GKB::StableIdentifiers::Check->new($stable_identifiers);


=head1 SEE ALSO


=head1 AUTHOR

David Croft E<lt>croft@ebi.ac.ukE<gt>

Copyright (c) 2009 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

package GKB::StableIdentifiers::Check;

use Data::Dumper;
use strict;
use vars qw(@ISA $AUTOLOAD %ok_field);
use Bio::Root::Root;
use GKB::Config;

@ISA = qw(Bio::Root::Root);

# List the object variables here, so that they can be checked
for my $attr
    (qw(
	stable_identifiers
	use_release_flag
	release_stable_identifier_to_version_hash
	stable_identifier_hash
	release_database_dba
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
    my($pkg, $stable_identifiers) = @_;

    my $self = bless {}, $pkg;
    
    $self->stable_identifiers($stable_identifiers);
    $self->use_release_flag(0);
    $self->release_stable_identifier_to_version_hash(undef);
    $self->stable_identifier_hash(undef);
    $self->release_database_dba(undef);
	
    return $self;
}

# If this flag is set, then check release databases, rather than
# the (default) slice databases.
sub set_use_release_flag {
	my ($self, $use_release_flag) = @_;
	
	$self->use_release_flag($use_release_flag);
}

# Finds all stable identifiers where there are gaps in the
# version numbers.  E.g. places where the version number jumps
# from 1 to 3.  Returns a hash, keyed by stable ID.  Each
# element is an array of pairs, each pair is a pair of
# non-contiguous version numbers found for that stable ID.
#
# TODO: this does not seem to be working properly, it flags
# all stable IDs as having gaps!!
sub find_version_number_gaps {
	my ($self) = @_;
	
	my %version_number_gaps = ();
    
    my $identifier_database_dba = $self->stable_identifiers->get_identifier_database_dba();
    if (!$identifier_database_dba) {
    	return %version_number_gaps;
    }
    
	my $db_ids = $identifier_database_dba->fetch_db_ids_by_class('StableIdentifier');
	my $db_id;
	my $stable_identifiers;
	my $stable_identifier;
	my $identifier;
	foreach $db_id (@{$db_ids}) {
		$stable_identifiers = $identifier_database_dba->fetch_instance_by_db_id($db_id);
		$stable_identifier = $stable_identifiers->[0];
		
		$identifier = $stable_identifier->identifierString->[0];
		if (!(defined $identifier)) {
			next;
		}
		
		my @version_number_gap_list = $self->find_version_number_gap_list_for_stable_identifier($stable_identifier);
		$version_number_gaps{$identifier} = \@version_number_gap_list;
	}
	
	return %version_number_gaps;
}

sub find_version_number_gap_list_for_stable_identifier {
	my ($self, $stable_identifier) = @_;
	
	
	
	# This is here for debug purposes only
	my $identifier = $stable_identifier->identifierString->[0];
	
	my @version_number_gap_list = ();
	my $stable_identifier_versions = $stable_identifier->stableIdentifierVersion;
	my $stable_identifier_version;
	my $last_version = undef;
	my $version;
	foreach $stable_identifier_version (@{$stable_identifier_versions}) {
		$version = $stable_identifier_version->identifierVersion->[0];
		
		if ($identifier eq "REACT_1882") {
			if (defined $last_version) {
				print STDERR "Check.find_version_number_gap_list_for_stable_identifier: last_version=$last_version\n";
			}
			print STDERR "Check.find_version_number_gap_list_for_stable_identifier: version=$version\n";
		}
		
		
		if (defined $last_version && $version > $last_version + 1) {
			if ($identifier eq "REACT_1882") {
				print STDERR "Check.find_version_number_gap_list_for_stable_identifier: creating non-contiguous version pair\n";
			}
			
			my @non_contiguous_versions = ($last_version, $version);
			if ($identifier eq "REACT_1882") {
				print STDERR "Check.find_version_number_gap_list_for_stable_identifier: non_contiguous_versions[0]=" . $non_contiguous_versions[0] . ", non_contiguous_versions[1]=" . $non_contiguous_versions[1] . "\n";
			}
			push(@version_number_gap_list, \@non_contiguous_versions);
		}
		$last_version = $version;
	}
	
	if ($identifier eq "REACT_1882") {
		print STDERR "Check.find_version_number_gap_list_for_stable_identifier: scalar(version_number_gap_list)=" . scalar(@version_number_gap_list) . "\n";
	}
	
	return @version_number_gap_list;
}

# Finds all stable identifiers where there are gaps in the
# release numbers.  E.g. places where the release number jumps
# from 17 to 19.  Returns a hash, keyed by stable ID.  Each
# element is an array of triplets, each triplet contains a pair of
# non-contiguous release numbers found for that stable ID, plus a
# reference to an array of release numbers, for which the stable
# ID is found in the corresponding slice databases, but not in
# the identifier database.
#
# If correction_flag is non-zero, then an attempt will be made
# to fix missing stable IDs; use this with care.
#
# TODO: this does not seem to be working properly, it flags
# all stable IDs as having gaps!!
sub find_release_number_gaps {
	my ($self, $correction_flag) = @_;
	
	my %release_number_gaps = ();
    
    my $identifier_database_dba = $self->stable_identifiers->get_identifier_database_dba();
    if (!$identifier_database_dba) {
    	return %release_number_gaps;
    }
    
	my $db_ids = $identifier_database_dba->fetch_db_ids_by_class('StableIdentifier');
	my $db_id;
	my $stable_identifiers;
	my $stable_identifier;
	my $identifier;
	foreach $db_id (@{$db_ids}) {
		$stable_identifiers = $identifier_database_dba->fetch_instance_by_db_id($db_id);
		$stable_identifier = $stable_identifiers->[0];
		
		$identifier = $stable_identifier->identifierString->[0];
		if (!(defined $identifier)) {
			next;
		}
		
		print STDERR "Check.find_release_number_gaps: #### dealing with identifier=$identifier\n";
	
		my @release_number_gap_list = $self->find_release_number_gap_list_for_stable_identifier($stable_identifier, $correction_flag);
		$release_number_gaps{$identifier} = \@release_number_gap_list;
	}
	
	return %release_number_gaps;
}

sub find_release_number_gap_list_for_stable_identifier {
	my ($self, $stable_identifier, $correction_flag) = @_;
	
	my @release_number_gap_list = ();

    my $identifier_database_dba = $self->stable_identifiers->get_identifier_database_dba();
    if (!$identifier_database_dba) {
    	return @release_number_gap_list;
    }

	$identifier_database_dba->inflate_instance($stable_identifier);
	my $identifier = $self->stable_identifiers->get_att_value_from_identifier_database_instance($stable_identifier, 'identifierString');
	my $display_name = $self->stable_identifiers->get_att_value_from_identifier_database_instance($stable_identifier, '_displayName');
	if (!(defined $display_name)) {
		$stable_identifier->_displayName($identifier);
	}
	
	my $stable_identifier_versions = $stable_identifier->stableIdentifierVersion;
	my $stable_identifier_version;
	my $release_ids;
	my $release_id;
	my $release;
	my $last_release_num = undef;
	my $i;
	my @release_nums = ();
	my $db_id;
	foreach $stable_identifier_version (@{$stable_identifier_versions}) {
		# Get the releases in which this version was
		# valid
		$identifier_database_dba->load_attribute_values($stable_identifier_version, 'releaseIds');
		$release_ids = $stable_identifier_version->releaseIds;
		foreach $release_id (@{$release_ids}) {
			$release = $self->stable_identifiers->get_att_value_from_identifier_database_instance($release_id, $self->stable_identifiers->reactome_release_column_name);
			my $release_num = $self->stable_identifiers->get_att_value_from_identifier_database_instance($release, 'num');
			
			if ($identifier eq "REACT_1882") {
				print STDERR "Check.find_release_number_gap_list_for_stable_identifier: UNSORTED release_num=$release_num\n";
			}

			if (!(defined $release_num)) {
				# This shouldn't happen
				print STDERR "Check.find_release_number_gap_list_for_stable_identifier: missing release number in release!!\n";
				next;
			}
			
			push(@release_nums, $release_num);
		}
		
	}
	
	if ($identifier eq "REACT_1882") {
		print STDERR "Check.find_release_number_gap_list_for_stable_identifier: BEFORE release num count=" . scalar(@release_nums) . "\n";
		print STDERR "Check.find_release_number_gap_list_for_stable_identifier: BEFORE release_nums=@release_nums\n";
	}

	@release_nums = sort {$a <=> $b} @release_nums;
		
	if ($identifier eq "REACT_1882") {
		print STDERR "Check.find_release_number_gap_list_for_stable_identifier: AFTER release num count=" . scalar(@release_nums) . "\n";
		print STDERR "Check.find_release_number_gap_list_for_stable_identifier: AFTER release_nums=@release_nums\n";
	}

	my $reactome_release_column_name = $self->stable_identifiers->reactome_release_column_name;
	foreach my $release_num (@release_nums) {
		if ($identifier eq "REACT_1882") {
			if (defined $last_release_num) {
				print STDERR "Check.find_release_number_gap_list_for_stable_identifier: last_release_num=$last_release_num\n";
			}
			print STDERR "Check.find_release_number_gap_list_for_stable_identifier: SORTED release_num=$release_num\n";
		}
			
			
		if (defined $last_release_num && $release_num > $last_release_num + 1) {
			if ($identifier eq "REACT_1882") {
				print STDERR "Check.find_release_number_gap_list_for_stable_identifier: creating non-contiguous release pair\n";
			}
				
			my @present_in_release = ();
			for ($i=$last_release_num + 1; $i<$release_num; $i++) {
				if ($self->stable_identifiers->is_identifier_in_release_database($identifier, $i)) {
					if ($identifier eq "REACT_1882") {
						print STDERR "Check.find_release_number_gap_list_for_stable_identifier: i=$i\n";
					}
					push(@present_in_release, $i);
					
					if ($correction_flag) {
						# OK, let's fix things
						my $instance = $self->stable_identifiers->fetch_instance_by_identifier_in_release_database($identifier, $i);
						
						if ($instance) {
							# Get version number of missing release
							my $release_stable_identifier = $instance->stableIdentifier->[0];
							my $release_version = $release_stable_identifier->identifierVersion->[0];
							
							print STDERR "Check.find_release_number_gap_list_for_stable_identifier: release_version=$release_version\n";
							
							# Check to see if the version already
							# exists and create it if not.
							my $repaired_stable_identifier_version = undef;
							foreach $stable_identifier_version (@{$stable_identifier_versions}) {
#								my $version = $stable_identifier_version->identifierVersion;
								my $version = $self->stable_identifiers->get_att_value_from_identifier_database_instance($stable_identifier_version, 'identifierVersion');
								if ($version eq $release_version) {
									$repaired_stable_identifier_version = $stable_identifier_version;
									$identifier_database_dba->inflate_instance($repaired_stable_identifier_version);
								}
							}
							if (!$repaired_stable_identifier_version) {
								$repaired_stable_identifier_version = GKB::Instance->new(
									-ONTOLOGY => $identifier_database_dba->ontology,
									-CLASS => 'StableIdentifierVersion');  
							    
								$repaired_stable_identifier_version->inflated(1);
								$repaired_stable_identifier_version->_displayName($i);
								$repaired_stable_identifier_version->identifierVersion($release_version);
								$repaired_stable_identifier_version->creationComment("Inserted to repair damaged identifier database");
								
								print STDERR "Check.find_release_number_gap_list_for_stable_identifier: repaired_stable_identifier_version=$repaired_stable_identifier_version\n";
								
								$identifier_database_dba->store($repaired_stable_identifier_version);
								$stable_identifier->add_attribute_value("stableIdentifierVersion", $repaired_stable_identifier_version);
#								$identifier_database_dba->update($stable_identifier, "stableIdentifierVersion");
							}
							
							# Add release to version
							$release = $self->stable_identifiers->get_release_from_num($i);
							$db_id = $instance->db_id;
							my $release_id = GKB::Instance->new(
									-ONTOLOGY => $identifier_database_dba->ontology,
									-CLASS => 'ReleaseId');
							$release_id->inflated(1);
							$release_id->_displayName("release=$i, DB_ID=$db_id");
							$release_id->$reactome_release_column_name($release);
							$release_id->instanceDB_ID($db_id);
								
							print STDERR "Check.find_release_number_gap_list_for_stable_identifier: release_id=$release_id\n";
								
							$identifier_database_dba->store($release_id);
							$repaired_stable_identifier_version->add_attribute_value("releaseIds", $release_id);
							$identifier_database_dba->update($repaired_stable_identifier_version, "releaseIds");
							$identifier_database_dba->update($stable_identifier, "stableIdentifierVersion");
						}
					}
				}
			}
				
			my @non_contiguous_releases = ($last_release_num, $release_num, \@present_in_release);
			if ($identifier eq "REACT_1882") {
				print STDERR "Check.find_release_number_gap_list_for_stable_identifier: non_contiguous_releases[0]=" . $non_contiguous_releases[0] . ", non_contiguous_releases[1]=" . $non_contiguous_releases[1] . "\n";
			}
			push(@release_number_gap_list, \@non_contiguous_releases);
		}
		
		$last_release_num = $release_num;
	}
	
	if ($identifier eq "REACT_1882") {
		print STDERR "Check.find_release_number_gap_list_for_stable_identifier: scalar(release_number_gap_list)=" . scalar(@release_number_gap_list) . "\n";
	}
	
	return @release_number_gap_list;
}

# Returns an array of duplicated stable IDs
sub find_duplicated_stable_identifiers {
	my ($self) = @_;
	
	my @duplicated_stable_identifiers = ();

    my $identifier_database_dba = $self->stable_identifiers->get_identifier_database_dba();
    if (!$identifier_database_dba) {
    	return @duplicated_stable_identifiers;
    }

	my $stable_identifiers = $identifier_database_dba->fetch_instance(-CLASS => "StableIdentifier");
	my %stable_identifier_ids = ();
	my $stable_identifier;
	foreach $stable_identifier (@{$stable_identifiers}) {
		my $identifier_string = $stable_identifier->identifierString->[0];
		
#		print STDERR "Check.find_duplicated_stable_identifiers: identifier_string=$identifier_string\n";
		
		if (defined $identifier_string) {
			if (!(defined $stable_identifier_ids{$identifier_string})) {
				$stable_identifier_ids{$identifier_string} = 1;
			} else {
				$stable_identifier_ids{$identifier_string}++;
			}
		} else {
			$stable_identifier_ids{"NULL"}++;
		}
	}
	
	my $identifier_string;
	foreach $identifier_string (sort(keys(%stable_identifier_ids))) {
		if ($stable_identifier_ids{$identifier_string} > 1) {
			push(@duplicated_stable_identifiers, $identifier_string);
		}
	}
	
	return @duplicated_stable_identifiers;
}

# Returns an array of orphan ReleaseId instances (array will be
# empty if none could be found).  You can optionally supply a
# reference to an array of DB_IDs, which will be used to restrict the
# search to ReleaseId instances with instanceDB_ID attributes
# with those values.  This is mainly for debugging purposes.  It
# speeds things up quite a bit.
sub find_orphan_release_ids {
	my ($self, $db_ids) = @_;
	
	my @orphan_release_ids = ();

    my $identifier_database_dba = $self->stable_identifiers->get_identifier_database_dba();
    if (!$identifier_database_dba) {
    	return @orphan_release_ids;
    }

	my $release_ids = [];
	if (defined $db_ids) {
		my $query = [['instanceDB_ID','=',$db_ids]];
		$release_ids = $identifier_database_dba->fetch_instance_by_remote_attribute('ReleaseId',$query);
	} else {
		$release_ids = $identifier_database_dba->fetch_instance(-CLASS => "ReleaseId");
	}
	my $release_id;
	my $reverse_attribute;
	foreach $release_id (@{$release_ids}) {
		$reverse_attribute = $release_id->reverse_attribute_value('releaseIds');
		
		if (scalar(@{$reverse_attribute}) == 0) {
			push(@orphan_release_ids, $release_id);
		}
	}
	
	return @orphan_release_ids;
}

# Generate hashes of stable IDs for the release database and for the
# identifier database. This is used internally to produce information
# needed by a couple of the checking subroutines.  It is cached, so
# that it doesn't need to be generated more than once.
sub generate_stable_identifiers_hashes {
	my ($self, $release_num, $project) = @_;
	
	if (defined $self->release_stable_identifier_to_version_hash && defined $self->stable_identifier_hash && defined $self->release_database_dba) {
		# No need to repeat this task if we already have the hashes
		return;
	}
	
    if (!(defined $release_num)) {
    	print STDERR "Check.generate_stable_identifiers_hashes: WARNING - release_num is not defined, aborting!\n";
    	return;
    }
    my $identifier_database_dba = $self->stable_identifiers->get_identifier_database_dba();
    if (!(defined $identifier_database_dba)) {
    	print STDERR "Check.generate_stable_identifiers_hashes: WARNING - could not create an identifier database DBA\n";
    	return;
    }
    my $release_database_name = $self->stable_identifiers->get_db_name_from_release_num($release_num, $project);
    if (!(defined $release_database_name)) {
    	print STDERR "Check.generate_stable_identifiers_hashes: WARNING - no release database found corresponding to release_num=$release_num!\n";
    	return;
    }
    my $slice_database_name = $release_database_name;
    if (!($self->use_release_flag)) {
    	$slice_database_name =~ s/reactome/slice/;
    }
    my $release_database_dba = $self->stable_identifiers->get_dba_from_db_name($slice_database_name);
    if (!(defined $release_database_dba)) {
    	$release_database_dba = $self->stable_identifiers->get_dba_from_db_name($release_database_name);
    }
    if (!(defined $release_database_dba)) {
    	print STDERR "Check.generate_stable_identifiers_hashes: WARNING - could not create a release DBA for release number $release_num\n";
    	return;
    }

	# Get all the StableIdentifier instances from the release
	my $release_stable_identifiers = $release_database_dba->fetch_instance(-CLASS => "StableIdentifier");
	my $stable_identifier;
	my $stable_identifier_string;
	my $stable_identifier_version_string;
	# Create a hash, with stable IDs as keys and versions as values
	my %release_stable_identifier_to_version_hash = ();
	foreach $stable_identifier (@{$release_stable_identifiers}) {
		$stable_identifier_string = $stable_identifier->identifier->[0];
		$stable_identifier_version_string = $stable_identifier->identifierVersion->[0];
		$release_stable_identifier_to_version_hash{$stable_identifier_string} = $stable_identifier_version_string;
	}
		
	# Get all the StableIdentifier instances from the identifier database which
	# are also known to be in the current release
	my $stable_identifiers = $identifier_database_dba->fetch_all_class_instances_as_shells('StableIdentifier');
	# Create a hash, with stable IDs as keys and StableIdentifier instances as values
	my %stable_identifier_hash = ();
	foreach my $stable_identifier (@{$stable_identifiers}) {
		$stable_identifier_string = $stable_identifier->identifierString->[0];
		$stable_identifier_hash{$stable_identifier_string} = $stable_identifier;
	}
		
    $self->release_stable_identifier_to_version_hash(\%release_stable_identifier_to_version_hash);
    $self->stable_identifier_hash(\%stable_identifier_hash);
    $self->release_database_dba($release_database_dba);
}

# Look for stable IDs that are present in the release but not in the
# stable ID database.
#
# TODO: the fix mode has not been implemented, and will not make
# any changes to the databases.
sub find_stable_identifiers_only_in_release {
	my ($self, $fix, $release_num, $project) = @_;
	
	my @stable_ids = ();

	$self->generate_stable_identifiers_hashes($release_num, $project);
		
	# Loop over the release stable IDs and look to see if there
	# are any corresponding identifier database instances.
	foreach my $stable_identifier_string (keys(%{$self->release_stable_identifier_to_version_hash})) {
		if ((!defined $self->stable_identifier_hash->{$stable_identifier_string})) {
			push(@stable_ids, $stable_identifier_string);
		}
	}
		
	return @stable_ids;
}

# Look for versions that are present in the release but different
# stable ID database.
#
# TODO: the current implementation of this subroutine ignores the
# project name, so if there is more than one project with the
# given release number, the return value will not be reliable.
#
# TODO: the fix mode has not been implemented, and will not make
# any changes to the databases.
sub find_version_incompatibility_with_release {
	my ($self, $fix, $release_num, $project) = @_;
	
	my @stable_ids = ();
		
	$self->generate_stable_identifiers_hashes($release_num, $project);
		
	my $reactome_release_column_name = $self->stable_identifiers->reactome_release_column_name;

	# Loop over the release stable IDs and look to see if there
	# are any corresponding identifier database instances.
	# For those that are found, identify the ones where there
	# is a discrepancy between the version stored in the
	# release and the version stored in the identifier database.
	my $found_release_flag;
	my $stable_identifier;
	my $stable_identifier_version_string;
	my $release_stable_identifiers;
	my $release_database_dba = $self->release_database_dba;
	foreach my $stable_identifier_string (keys(%{$self->release_stable_identifier_to_version_hash})) {
		$stable_identifier = $self->stable_identifier_hash->{$stable_identifier_string};
		if ((!defined $stable_identifier)) {
			next;
		}
		$stable_identifier_version_string = $self->release_stable_identifier_to_version_hash->{$stable_identifier_string};

		# Loop over the versions known to the identifier
		# database to see if any of them are the same
		# as the one found in the release database.
		my $stable_identifier_version;
		my $release_id;
		foreach $stable_identifier_version (@{$stable_identifier->stableIdentifierVersion}) {
			# Check those cases where the release version is different
			# from the identifier database version.
			if (!($stable_identifier_version->identifierVersion->[0] eq $stable_identifier_version_string)) {
				# Look to see if the current release number corresponds
				# to the release number of the releaseId; if so, then
				# we have a version number incompatibility.
				$found_release_flag = 0;
				foreach $release_id (@{$stable_identifier_version->releaseIds}) {
					if ($release_id->$reactome_release_column_name->[0]->num->[0] eq $release_num) {
						$found_release_flag = 1;
						last;
					}
				}
					
				if ($found_release_flag) {
					push(@stable_ids, $stable_identifier_string);
					# OK, let's try to fix the problem, by transferring
					# the version known to the identifier database for
					# the current release to the corresponding StableIdentifier
					# instance in the release database.
					if ($fix) {
						print STDERR "Check.find_version_incompatibility_with_release: about to fix release database for $stable_identifier_string, release version=$stable_identifier_version_string, stable ID database version=" . $stable_identifier_version->identifierVersion->[0] . "\n";
						
						$release_stable_identifiers = $release_database_dba->fetch_instance(-CLASS => "StableIdentifier", -QUERY => [['identifier', [$stable_identifier_string],0]]);
						my $release_stable_identifier = $release_stable_identifiers->[0];
						$release_database_dba->inflate_instance($release_stable_identifier);
						print STDERR "Check.find_version_incompatibility_with_release: original version=" . $release_stable_identifier->identifierVersion->[0] . "\n";
						$release_stable_identifier->identifierVersion($stable_identifier_version->identifierVersion->[0]);
						print STDERR "Check.find_version_incompatibility_with_release: NEW version=" . $release_stable_identifier->identifierVersion->[0] . "\n";
						
						$release_database_dba->update($release_stable_identifier, "identifierVersion");
					}
					last;
				}
			}
		}
	}
	
	return @stable_ids;
}

sub find_stable_ids_not_in_dois {
	my ($self, $fix, $release_num, $project) = @_;
	
	$self->generate_stable_identifiers_hashes($release_num, $project);
		
	my $dba = $self->release_database_dba();
	
	my @query = (['stableIdentifier', 'IS NOT NULL', []]);
	my $instances = $dba->fetch_instance_by_remote_attribute('DatabaseObject', \@query);
	my @stable_ids = ();
	my $instance;
	my $stable_identifier_instances;
	my $stable_identifier_instance;
	my $identifier;
	my $doi_instances;
	my $doi;
	foreach $instance (@{$instances}) {
		$instance->inflate;
		if ($instance->is_valid_attribute("doi")) {
			$stable_identifier_instances = $instance->stableIdentifier;
			if (scalar(@{$stable_identifier_instances})<1) {
				print STDERR "Instance " .  $instance->db_id() . " does not have a corresponding stable identifier!!!\n";
				# Should never happen!!
				next;
			}
			$stable_identifier_instance = $stable_identifier_instances->[0];
			$stable_identifier_instance->inflate;
			$identifier = $stable_identifier_instance->identifier->[0];

			$doi_instances = $instance->doi;
			if (scalar(@{$doi_instances})<1) {
				next;
			}
			$doi = $doi_instances->[0];

			if (!($doi =~ /$identifier/)) {
				push(@stable_ids, $identifier);
			}
		}
	}
		
	return @stable_ids;
}

1;

