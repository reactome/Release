=head1 NAME

GKB::StableIdentifiers

=head1 SYNOPSIS

A package of utility subroutines for dealing with stable identifiers.

=head1 DESCRIPTION


=head1 SEE ALSO

GKB::WebUtils

=head1 AUTHOR

David Croft E<lt>croft@ebi.ac.ukE<gt>

Copyright (c) 2006 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

package GKB::StableIdentifiers;

use Data::Dumper;
use strict;
use vars qw(@ISA $AUTOLOAD %ok_field);
use Bio::Root::Root;
use GKB::Config;
use GKB::Utils;

@ISA = qw(Bio::Root::Root);

# List the object variables here, so that they can be checked
for my $attr
    (qw(
	cgi
	current_release_dba
	current_release_instance
	identifier_database_dba
	most_recent_release
	most_recent_release_num
	most_recent_release_db_name
	release_dba_hash
	reactome_release_table_name
	reactome_release_column_name
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
    my($pkg, $cgi) = @_;

    my $self = bless {}, $pkg;
   	$cgi && $self->cgi($cgi);
   	
   	my %release_dba_hash = ();
   	$self->release_dba_hash(\%release_dba_hash);
   	
	$self->reactome_release_table_name('ReactomeRelease');
	$self->reactome_release_column_name('reactomeRelease');
	$self->most_recent_release(undef);
	$self->most_recent_release_num(undef);
		

	# Make sure that release table and column names get set up
	# properly for the current stable ID database, if known.
	$self->get_identifier_database_dba();

    return $self;
}

# Given a string potentially containing stable ID information,
# extract stable ID and version number.  If the string is
# undefined ot does not contain stable ID information,
# both the stable ID and the version returned will be
# null.
sub extract_identifier_and_version_from_string {
	my ($self, $str) = @_;
	
	my @parts_of_extended_stable_id = (undef, undef);
	
	if (!(defined $str)) {
		return @parts_of_extended_stable_id;
	}
	
	# Parse stable ID out of i/p string
	$str =~ /(REACT_[0-9.]+)/;
	my $extended_stable_id = $1;
	
	if (!(defined $extended_stable_id) || !($extended_stable_id =~ /REACT_[0-9.]+/)) {
		return @parts_of_extended_stable_id;
	}
	
	@parts_of_extended_stable_id = split(/\./, $extended_stable_id);
	
	return @parts_of_extended_stable_id;
}

sub set_identifier_database_dba {
	my ($self, $identifier_database_dba) = @_;
	
	if (defined $identifier_database_dba) {
		$self->identifier_database_dba($identifier_database_dba);
		
		# 'Release' bacame a reserved word as of MySQL 5, so
		# the name of this attribute got changed to 'ReactomeRelease'.
		# In order to make this class backwards compatible,
		# check to see which table name is actually being used.
		my $reactome_release_table_name = undef;
		my $reactome_release_column_name = undef;
		eval {
			if ($identifier_database_dba->exists_table('ReactomeRelease')) {
				$reactome_release_table_name = 'ReactomeRelease';
				$reactome_release_column_name = 'reactomeRelease';
			} elsif ($identifier_database_dba->exists_table('Release')) {
				$reactome_release_table_name = 'Release';
				$reactome_release_column_name = 'release';
			}
		};
		if (defined $reactome_release_table_name) {
			$self->reactome_release_table_name($reactome_release_table_name);
			$self->reactome_release_column_name($reactome_release_column_name);
		} else {
			print STDERR "StableIdentifiers.set_identifier_database_dba: WARNING - could not find table names ReactomeRelease or Release, this might not be a stable identifier database!\n";
			$self->identifier_database_dba(undef);
		}
	} else {
		$self->identifier_database_dba(undef);
	}
}

# Gets a database adaptor for the identifier database.  Assumes that
# the name of the database is stored in Config.pm in the GK_IDB_NAME
# variable.  Returns undef if the variable has not been set.  The DBA
# is cached for quicker access
sub get_identifier_database_dba {
	my ($self) = @_;
	
	# Get cached version, if available
	if (defined $self->identifier_database_dba) {
		return $self->identifier_database_dba;
	}
	
	# Look to see if an identifier database has been mentioned
	# in the Config.pm file, and if not, return nothing.
	my $identifier_database_db_name = $GKB::Config::GK_IDB_NAME;
	if (!$identifier_database_db_name) {
		print STDERR "StableIdentifier.get_identifier_database_dba: no identifier database name!  Maybe you need to add GK_IDB_NAME to config.pm?\n";
		return undef;
	}
	
	my $identifier_database_dba = $self->get_dba_from_db_name($identifier_database_db_name);

	# Cache it
	$self->set_identifier_database_dba($identifier_database_dba);

	return $identifier_database_dba;
}


# Gets the database name for the current release, by looking at
# the 'DB' parameter in the CGI.
sub get_current_release_db_name {
	my ($self) = @_;
	
	my $current_release_db_name = $self->cgi->param('DB');

	return $current_release_db_name;
}

# Gets the database adaptor for the current release.  The DBA
# is cached for quicker access
sub get_current_release_dba {
	my ($self) = @_;
	
	if ($self->current_release_dba) {
		return $self->current_release_dba;
	}
	
	my $current_release_db_name = $self->get_current_release_db_name();
	my $current_release_dba = $self->get_dba_from_db_name($current_release_db_name);
	$self->current_release_dba($current_release_dba);
	
	return $current_release_dba;
}

# Gets the database adaptor for the named database.
sub get_dba_from_db_name {
	my ($self, $db_name) = @_;
	
	if (!(defined $db_name)) {
		print STDERR "StableIdentifiers.get_dba_from_db_name: WARNING - db_name is undef!!\n";
		return undef;
	}
	
	my $dba = $self->release_dba_hash->{$db_name};
	
	if (!(defined $dba)) {
		# Get other DB params from Config.pm
		eval {
			$dba = GKB::DBAdaptor->new(
				-dbname => $db_name,
				-user   => $GKB::Config::GK_DB_USER,
				-host   => $GKB::Config::GK_DB_HOST,
				-port   => $GKB::Config::GK_DB_PORT,
				-pass   => $GKB::Config::GK_DB_PASS
			);
			
			$self->release_dba_hash->{$db_name} = $dba;
		};
		
		if (!(defined $dba)) {
			print STDERR "StableIdentifiers.get_dba_from_db_name: WARNING - not able to create a new DBAdaptor for db_name=$db_name\n";
		}
	}
	
	return $dba;
}

# Returns  $db_name
# for the supplied Release number.
sub get_db_name_from_release_num {
	my ($self, $num, $project) = @_;
	
	if (!(defined $num)) {
		print STDERR "StableIdentifiers.get_db_name_from_release_num: WARNING - supplied value for num is undef!!!\n";
		return undef;
	}
	
	my $release = $self->get_release_from_num($num, $project);
	my $db_name = $self->get_db_name_from_release($release);
	
	return $db_name;
}

# Returns date
# for the supplied Release number.
sub get_date_from_release_num {
	my ($self, $num) = @_;
	
	if (!(defined $num)) {
		return undef;
	}

	my $release = $self->get_release_from_num($num);
	my $date = $self->get_date_from_release($release);
	
	return $date;
}

# Returns  $db_name
# for the supplied Release.
sub get_db_name_from_release {
	my ($self, $release) = @_;
	
	my $database_parameters = $self->get_att_value_from_identifier_database_instance($release, 'releaseDbParams');
	my $db_name = $self->get_att_value_from_identifier_database_instance($database_parameters, 'dbName');

	return $db_name;
}

# Returns date
# for the supplied Release.
sub get_date_from_release {
	my ($self, $release) = @_;
	
	my $date = $self->get_att_value_from_identifier_database_instance($release, 'dateTime');

	return $date;
}

# Closes any active database adaptors
sub close_all_dbas {
	my ($self) = @_;
	
	# Identifier database
    ($self->identifier_database_dba) && $self->identifier_database_dba->db_handle->disconnect;
    
    # Release databases
	my $current_release_dba = $self->current_release_dba;
    $current_release_dba && $current_release_dba->db_handle->disconnect;
}

# Gets the current release instance given identifier and version
# information.
sub get_current_instance {
	my ($self, $stable_identifier, $identifier, $version) = @_;
	
	# If a cached instance already exists, don't generate it again
	my $current_release_instance = $self->current_release_instance;
	if ($current_release_instance) {
		return $current_release_instance;
	}
	
	my $current_release_dba = $self->current_release_dba;
	if (!$current_release_dba) {
		$current_release_dba = $self->get_release_dba($stable_identifier, $version);
	}
	
	if (defined $current_release_dba) {
		$self->current_release_dba($current_release_dba);
	} else {
		print STDERR "StableIdentifier.get_current_instance: could not get a dba for identifier=$identifier!!\n";
		return undef;
	}
	
	my $instances = $current_release_dba->fetch_instance_by_remote_attribute(
		'DatabaseObject',
		[['stableIdentifier.identifier','=',[$identifier]]]);
	
	if (!$instances || !($instances =~ /ARRAY/) || scalar(@{$instances})<1) {
		print STDERR "StableIdentifier.get_current_instance: WARNING - could not find a SatabaseObject for identifier=$identifier\n";
	}
		
	$current_release_instance = $self->get_attribute_val_from_list($instances);
	
	# Put into cache
	$self->current_release_instance($current_release_instance);
	
	return $current_release_instance;
}

# Get a reactome release database adaptor for an instance with
# the given stable ID and version number.  If the version
# number is undef, then the highest compatible version
# number will be used.
sub get_release_dba {
	my ($self, $stable_identifier, $identifier_version) = @_;
	
	if (!(defined $stable_identifier)) {
		print STDERR "StableIdentifiers.get_release_dba: WARNING - stable_identifier is undef!!\n";
		return undef;
	}
	
	my $stable_identifier_version = $self->get_stable_identifier_version($stable_identifier, $identifier_version);
	if (!(defined $stable_identifier_version)) {
		print STDERR "StableIdentifiers.get_release_dba: WARNING - no stable_identifier_version could be found!!\n";
		print STDERR "	stable_identifier=$stable_identifier\n";
		if (defined $identifier_version) {
			print STDERR "	identifier_version=$identifier_version\n";
		}
	}
	
	my $release = $self->get_max_release_from_stable_identifier_version_instance($stable_identifier_version);
	if (!(defined $release)) {
		print STDERR "StableIdentifiers.get_release_dba: WARNING - no release could be found!!\n";
		print STDERR "		stable_identifier=$stable_identifier\n";
		if (defined $identifier_version) {
			print STDERR "		identifier_version=$identifier_version\n";
		}
		print STDERR "		stable_identifier_version=$stable_identifier_version\n";
		return undef;
	}
	
	my $release_db_name = $self->get_db_name_from_release($release);
	
	if (!(defined $release_db_name)) {
		print STDERR "StableIdentifiers.get_release_dba: WARNING - could not find a release database name for stable_identifier=$stable_identifier, stable_identifier=$stable_identifier\n";
		return;
	}
	
	my $release_dba = $self->get_dba_from_db_name($release_db_name);

	return $release_dba;
}

# Gets a biologically meaningful name for the current
# instance.  Note: this name may not be quite the same
# as the display name of the current instance, since
# it may be retrieved from a previous instance.
sub get_current_instance_name {
	my ($self, $stable_identifier, $identifier, $version) = @_;
	
	if (!(defined $stable_identifier)) {
		print STDERR "StableIdentifier.get_current_instance_name: WARNING - stable_identifier is undef!!\n";
#		return '';
	}
	if (!(defined $identifier)) {
		print STDERR "StableIdentifier.get_current_instance_name: WARNING - identifier is undef!!\n";
#		return '';
	}
	
	my $instance = $self->get_current_instance($stable_identifier, $identifier, $version);
	
	if (!$instance) {
		print STDERR "StableIdentifier.get_current_instance_name: could not find instance with identifier=$identifier!!\n";
		return '';
	}

	my $display_name = $instance->displayName;
	
	if (!(defined $display_name)) {
		print STDERR "StableIdentifier.get_current_instance_name: WARNING - no display name for instance with ientifier=$identifier\n";
		return '';
	}
	
	return $display_name;
}

# Gets stable ID and version for instance currently being displayed on the page
sub get_current_identifier_and_version {
	my ($self) = @_;
	
	my $identifier = undef;
	my $version = undef;
	my @identifier_and_version = ($identifier, $version);
	
	# Maybe we can get hold of stable ID information from CGI (quick)
	my $st_id = $self->cgi->param('ST_ID');
	if (defined $st_id) {
		($identifier, $version) = $self->extract_identifier_and_version_from_string($st_id);
	}
	
	if (defined $identifier) {
		# We found what we wanted in the ST_ID, so return straight away
		return @identifier_and_version;
	}
	# Just in case...
	$version = undef;
	
	# Maybe we have a current instance already
	my $current_release_instance = $self->current_release_instance;
	my $db_id = undef;
	if (!$current_release_instance) {
		# Try to get DB name and DB_ID from CGI, get a DBA for the
		# named DB and then do a search for an instance with the
		# found DB_ID.
		my $instance = undef;
		$db_id = $self->cgi->param('ID');
		if (defined $db_id) {
			# We explicitly want to get DB from CGI, because in
			# the cases we are interested in, it will always be paired
			# with ID.
			my $current_release_dba = $self->current_release_dba; # use cached DBA, if available
			if (!(defined $current_release_dba)) {
				$current_release_dba = $self->get_dba_from_db_name($self->cgi->param('DB'));
			}
			if (defined $current_release_dba) {
				$self->current_release_dba($current_release_dba); # cache the release DBA
				$current_release_instance = $self->get_attribute_val_from_list($current_release_dba->fetch_instance_by_db_id($db_id));
				if (defined $current_release_instance) {
					$self->current_release_instance($current_release_instance); # cache current instance
				}
			}
		}
	}
		
	if (!(defined $current_release_instance)) {
		print STDERR "StableIdentifiers.get_current_identifier_and_version: WARNING - could not get current instance\n";
		return @identifier_and_version;
	}
		
	# Get the stableIdentifier attribute from the instance
	if (!$current_release_instance->is_valid_attribute('stableIdentifier')) {
		print STDERR "StableIdentifiers.get_current_identifier_and_version: WARNING - stableIdentifier is not a valid attribute for instance with DB_ID: " . $current_release_instance->db_id() . "\n";
		if (defined $db_id) {
			print STDERR "StableIdentifiers.get_current_identifier_and_version: (db_id=$db_id)\n";
		}
		my $db_name = $self->cgi->param('DB');
		if (defined $db_name) {
			print STDERR "StableIdentifiers.get_current_identifier_and_version: (db_name=$db_name)\n";
		}
		# Just for fun, let's see if we can get the information from the identifier database!!
		# (Ha,ah ha ha hah!!)
		if (defined $db_id && defined $db_name) {
			my $release_num = $self->get_release_num_from_db_name($db_name);
			my $stable_identifiers = $self->get_stable_identifiers_from_release_db_id($release_num, $db_id);
			if (scalar(@{$stable_identifiers})<1) {
				print STDERR "StableIdentifiers.get_current_identifier_and_version: gloink, can't find any StableIdentifier instances for DB_ID $db_id in release $release_num!\n";
			} elsif (scalar(@{$stable_identifiers})>1) {
				print STDERR "StableIdentifiers.get_current_identifier_and_version: gadzooks, there are " . scalar(@{$stable_identifiers}) . " instances for DB_ID $db_id in release $release_num!!\n";
			} else {
				$identifier = $stable_identifiers->[0]->identifierString->[0];
				if (defined $stable_identifiers->[0]->identifierString && scalar(@{$stable_identifiers->[0]->identifierString})>0) {
					print STDERR "StableIdentifiers.get_current_identifier_and_version: HAH!!  identifier=$identifier\n";
				} else {
					print STDERR "StableIdentifiers.get_current_identifier_and_version: blurgh, identifierString is empty!!\n";
				}
			}
		}
		return @identifier_and_version;
	}
	if (!(defined $current_release_instance->stableIdentifier) || scalar(@{$current_release_instance->stableIdentifier})<1) {
		print STDERR "StableIdentifiers.get_current_identifier_and_version: WARNING - instance has empty stableIdentifier attribute for instance with DB_ID: " . $current_release_instance->db_id() . "\n";
		if (defined $db_id) {
			print STDERR "StableIdentifiers.get_current_identifier_and_version: (db_id=$db_id)\n";
		}
		my $db_name = $self->cgi->param('DB');
		if (defined $db_name) {
			print STDERR "StableIdentifiers.get_current_identifier_and_version: (db_name=$db_name)\n";
		}
		# Just for fun, let's see if we can get the information from the identifier database!!
		# (Ha,ah ha ha hah!!)
		if (defined $db_id && defined $db_name) {
			my $release_num = $self->get_release_num_from_db_name($db_name);
			my $stable_identifiers = $self->get_stable_identifiers_from_release_db_id($release_num, $db_id);
			if (scalar(@{$stable_identifiers})<1) {
				print STDERR "StableIdentifiers.get_current_identifier_and_version: gloink, can't find any StableIdentifier instances for DB_ID $db_id in release $release_num!\n";
			} elsif (scalar(@{$stable_identifiers})>1) {
				print STDERR "StableIdentifiers.get_current_identifier_and_version: gadzooks, there are " . scalar(@{$stable_identifiers}) . " instances for DB_ID $db_id in release $release_num!!\n";
			} else {
				$identifier = $stable_identifiers->[0]->identifierString->[0];
				if (defined $stable_identifiers->[0]->identifierString && scalar(@{$stable_identifiers->[0]->identifierString})>0) {
					print STDERR "StableIdentifiers.get_current_identifier_and_version: HAH!!  identifier=$identifier\n";
				} else {
					print STDERR "StableIdentifiers.get_current_identifier_and_version: blurgh, identifierString is empty!!\n";
				}
			}
		}
		return @identifier_and_version;
	}
	my $stable_identifier = $current_release_instance->stableIdentifier->[0];
	
	# Pull out identifier and version
	$identifier = $stable_identifier->identifier->[0];
	$version = $stable_identifier->identifierVersion->[0];
	
	@identifier_and_version = ($identifier, $version);
	
	return @identifier_and_version;
}

# Get the StableIdentifier instances from the identifier database
# corresponding to a DB_ID in a given release.  Generally speaking, there
# should only be zero or one instance in the returned Set.
sub get_stable_identifiers_from_release_db_id {
	my ($self, $release_num, $db_id) = @_;

    my $identifier_database_dba = $self->get_identifier_database_dba();
    if (!$identifier_database_dba) {
    	print STDERR "StableIdentifiers.get_stable_identifiers_from_release_db_id: WARNING - no DBA available!!\n";
    	return undef;
    }
	    
    # First, find the release associated with the given release number
    my $release_db_id = $self->get_release_db_id_from_num($release_num);
    if (!(defined $release_db_id)) {
            print STDERR "StableIdentifiers.get_stable_identifiers_from_release_db_id: WARNING - we have no releases for release_num=$release_num, PROJECT_NAME=$PROJECT_NAME!!\n";
            return undef;
    }

    my $release_remote_attribute = 'stableIdentifierVersion.releaseIds.' . $self->reactome_release_column_name;
#    my $release_num_remote_attribute = 'stableIdentifierVersion.releaseIds.' . $self->reactome_release_column_name . '.num';
#    my $project_name_remote_attribute = 'stableIdentifierVersion.releaseIds.' . $self->reactome_release_column_name . '.project.name';
	my $stable_identifiers = $identifier_database_dba->fetch_instance_by_remote_attribute(
		'StableIdentifier',
		[['stableIdentifierVersion.releaseIds.instanceDB_ID','=',[$db_id]],
		 [$release_remote_attribute,'=',[$release_db_id]]
#		 [$project_name_remote_attribute,'=',[$PROJECT_NAME]],
#		 [$release_num_remote_attribute,'=',[$release_num]]
		 ]);
	
	return $stable_identifiers;
}

# Given the supplied identifier, retrieve the corresponding
# StableIdentifier instance from the most recent release.
# Returns undef if the identifier cannot be found there.
sub get_stable_identifier_from_identifier_most_recent_release {
	my ($self, $identifier) = @_;

	my $most_recent_release_num = $self->get_most_recent_release_num();
    my $identifier_database_dba = $self->get_identifier_database_dba();
    if (!$identifier_database_dba) {
    	print STDERR "StableIdentifiers.get_stable_identifier_from_identifier_most_recent_release: WARNING - no DBA available!!\n";
    	return undef;
    }
    
    # First, find the release associated with the given release number
    my $release_db_id = $self->get_release_db_id_from_num($most_recent_release_num);
    if (!(defined $release_db_id)) {
            print STDERR "StableIdentifiers.get_stable_identifier_from_identifier_most_recent_release: WARNING - we have no releases for release_num=$most_recent_release_num, PROJECT_NAME=$PROJECT_NAME!!\n";
            return undef;
    }

    my $release_remote_attribute = 'stableIdentifierVersion.releaseIds.' . $self->reactome_release_column_name;
#    my $release_num_remote_attribute = 'stableIdentifierVersion.releaseIds.' . $self->reactome_release_column_name . '.num';
#    my $project_name_remote_attribute = 'stableIdentifierVersion.releaseIds.' . $self->reactome_release_column_name . '.project.name';
	my $stable_identifiers = $identifier_database_dba->fetch_instance_by_remote_attribute(
		'StableIdentifier',
		[['identifierString','=',[$identifier]],
		 [$release_remote_attribute,'=',[$release_db_id]],
#		 [$project_name_remote_attribute,'=',[$PROJECT_NAME]],
#		 [$release_num_remote_attribute,'=',[$most_recent_release_num]]
		]);
	my $stable_identifier = $self->get_attribute_val_from_list($stable_identifiers);
	
	return $stable_identifier;
}

# Gets stable ID and version for the current instance in the
# most recent release.  If the instance does not exist in
# the most recent release, returns an array of undefs.
sub get_identifier_and_version_in_most_recent_release {
	my ($self) = @_;
	
	my $identifier = undef;
	my $version = undef;
	my @identifier_and_version = ($identifier, $version);
	
	my $current_identifier = undef;
	my $current_version = undef;
	
	($current_identifier, $current_version) = $self->get_current_identifier_and_version();
	
	my $stable_identifier = $self->get_stable_identifier_from_identifier_most_recent_release($current_identifier);
	
	if (!(defined $stable_identifier)) {
#		print STDERR "StableIdentifiers.get_identifier_and_version_in_most_recent_release: could not get a stable identifier instance from the identifier database for identifier $current_identifier\n";
		return @identifier_and_version;
	}
	
	$identifier = $self->get_att_value_from_identifier_database_instance($stable_identifier, 'identifierString');
	
	# current_identifier could not be found in the most recent
	# release, so return an array of undefs
	if (!$identifier) {
		return @identifier_and_version;
	}
	
	$version = $self->get_max_version_num_from_stable_identifier($stable_identifier);
	
	@identifier_and_version = ($identifier, $version);
	
	return @identifier_and_version;
}

# Get the StableIdentifier instance from the identifier database
# corresponding to identifier.
sub get_stable_identifier {
	my ($self, $identifier) = @_;
	
    my $identifier_database_dba = $self->get_identifier_database_dba();
    if (!$identifier_database_dba) {
    	print STDERR "StableIdentifiers.get_stable_identifier: WARNING - no DBA available!!\n";
    	return undef;
    }
    
	my $stable_identifiers = $identifier_database_dba->fetch_instance_by_remote_attribute(
		'StableIdentifier',
		[['identifierString','=',[$identifier]]]);
	my $stable_identifier = $self->get_attribute_val_from_list($stable_identifiers);

	return $stable_identifier;
}

# Get the StableIdentifier instance(s) from the identifier database
# corresponding to a DB_ID in a given release.  There should really
# only be one such StableIdentifier instance; more than one means
# that an inconsistency has crept into the database.
# If release_num is undef, then all StableIdentifier instances corresponding
# to the given DB_ID will be retrieved.
# Returns a
# reference to an array of StableIdentifier instances, which may
# be empty.  Returns undef if an error occurred.
sub get_stable_identifier_from_release_db_id {
	my ($self, $db_id, $release_num) = @_;
	
    my $identifier_database_dba = $self->get_identifier_database_dba();
    if (!$identifier_database_dba) {
    	print STDERR "StableIdentifiers.get_stable_identifier_from_release_db_id: WARNING - no DBA available!!\n";
    	return undef;
    }

    # First, find the release associated with the given release number
    my $release_db_id = $self->get_release_db_id_from_num($release_num);
    if (!(defined $release_db_id)) {
            print STDERR "StableIdentifiers.get_stable_identifier_from_release_db_id: WARNING - we have no releases for release_num=$release_num, PROJECT_NAME=$PROJECT_NAME!!\n";
            return undef;
    }

	my $stable_identifiers = [];
	if (defined $release_num) {
		my $release_ids = $identifier_database_dba->fetch_instance_by_remote_attribute(
			'ReleaseId',
			[['instanceDB_ID','=',[$db_id]],
			['release','=',[$release_db_id]],
#			['release.project.name','=',[$PROJECT_NAME]],
#			['release.num','=',[$release_num]]
			]);
		my $release_id;
		my $release_id_stable_identifiers;
		foreach $release_id (@{$release_ids}) {
			$release_id_stable_identifiers = $identifier_database_dba->fetch_instance_by_remote_attribute(
				'StableIdentifier',
				[['stableIdentifierVersion.releaseIds.DB_ID','=',[$release_id->db_id()]]]);
			
			push(@{$stable_identifiers}, @{$release_id_stable_identifiers});
		}
	} else {
		$stable_identifiers = $identifier_database_dba->fetch_instance_by_remote_attribute(
			'StableIdentifier',
			[['stableIdentifierVersion.releaseIds.instanceDB_ID','=',[$db_id]]]);
	}

	return $stable_identifiers;
}

# Get the StableIdentifier instance(s) from the identifier database
# corresponding to a DB_ID *NOT* in a given release.  Returns a
# reference to an array of StableIdentifier instances, which may
# be empty.  Returns undef if an error occurred.
sub get_stable_identifier_with_db_id_but_not_in_release {
	my ($self, $db_id, $release_num) = @_;
	
    my $identifier_database_dba = $self->get_identifier_database_dba();
    if (!$identifier_database_dba) {
    	print STDERR "StableIdentifiers.get_stable_identifier_with_db_id_but_not_in_release: WARNING - no DBA available!!\n";
    	return undef;
    }

    # First, find the release associated with the given release number
    my $release_db_id = $self->get_release_db_id_from_num($release_num);
    if (!(defined $release_db_id)) {
            print STDERR "StableIdentifiers.get_stable_identifier_with_db_id_but_not_in_release: WARNING - we have no releases for release_num=$release_num, PROJECT_NAME=$PROJECT_NAME!!\n";
            return undef;
    }

	my $release_ids = $identifier_database_dba->fetch_instance_by_remote_attribute(
		'ReleaseId',
		[['instanceDB_ID','=',[$db_id]],
		['release','=',[$release_db_id]],
#		['release.project.name','=',[$PROJECT_NAME]]
#		['release.num','!=',[$release_num]]
		]);
	my $release_id;
	my $stable_identifiers = [];
	my $release_id_stable_identifiers;
	foreach $release_id (@{$release_ids}) {
		$release_id_stable_identifiers = $identifier_database_dba->fetch_instance_by_remote_attribute(
			'StableIdentifier',
			[['stableIdentifierVersion.releaseIds.DB_ID','=',[$release_id->db_id()]]]);
		
		push(@{$stable_identifiers}, @{$release_id_stable_identifiers});
	}

	return $stable_identifiers;
}

# Gets a list of stable identifiers for which the given instance DB_ID
# is not found in the given release, and not in the first releases.
# For all subsequent releases, the value of the instance DB_ID must be
# the same.
sub get_identifiers_where_db_id_is_not_in_release {
	my ($self, $db_id, $release_num) = @_;
	
	my $stable_identifiers = $self->get_stable_identifier_with_db_id_but_not_in_release($db_id, $release_num);
	my $unique_stable_identifiers = [];
	my $stable_identifier;
	my $stable_identifier_versions;
	my $stable_identifier_version;
	my $release_ids;
	my $release_id;
	my $instance_db_id;
	my $instance_release_num;
	my $stable_identifier_version_first_flag;
	my $stable_identifier_version_breakout_flag;
	foreach $stable_identifier (@{$stable_identifiers}) {
		$stable_identifier_versions = $stable_identifier->stableIdentifierVersion;
		$stable_identifier_version_first_flag = 1;
		$stable_identifier_version_breakout_flag = 0;
		foreach $stable_identifier_version (@{$stable_identifier_versions}) {
			$release_ids = $stable_identifier_version->releaseIds;
			foreach $release_id (@{$release_ids}) {
				$instance_db_id = $release_id->instanceDB_ID->[0];
				$instance_release_num = $release_id->release->[0]->num->[0];
				
				if ($stable_identifier_version_first_flag) {
					if ($release_num == $instance_release_num && $db_id == $instance_db_id) {
						$stable_identifier_version_breakout_flag = 1;
						last;
					}
					
					$stable_identifier_version_first_flag = 0;
				} else {
					if ($db_id != $instance_db_id) {
						$stable_identifier_version_breakout_flag = 1;
						last;
					}
				}
			}
			
			if ($stable_identifier_version_breakout_flag) {
				last;
			}
		}
			
		if (!$stable_identifier_version_breakout_flag) {
			# The uniqueness criterion was true for every ReleaseId, so
			# add the current StableIdentifier to the list
			push(@{$unique_stable_identifiers}, $stable_identifier);
		}
	}
	
	return $unique_stable_identifiers;
}

sub get_db_ids_from_stable_identifier {
	my ($self, $stable_identifier) = @_;
	
	my $stable_identifier_versions;
	my $stable_identifier_version;
	my $release_ids;
	my $release_id;
	my $instance_db_id;
	my $stable_identifier_version_breakout_flag;
	$stable_identifier_versions = $stable_identifier->stableIdentifierVersion;
	my $db_ids = [];
	foreach $stable_identifier_version (@{$stable_identifier_versions}) {
		$release_ids = $stable_identifier_version->releaseIds;
		foreach $release_id (@{$release_ids}) {
			$instance_db_id = $release_id->instanceDB_ID->[0];
			push(@{$db_ids}, $instance_db_id);
		}
	}
	
	return $db_ids;
}

# Get the ReleaseId instances from the identifier database
# corresponding to a DB_ID in a release.
sub get_release_id_from_release_db_id {
	my ($self, $db_id, $release_num) = @_;
	
    my $identifier_database_dba = $self->get_identifier_database_dba();
    if (!$identifier_database_dba) {
    	return undef;
    }
    
    # First, find the release associated with the given release number
    my $release_db_id = $self->get_release_db_id_from_num($release_num);
    if (!(defined $release_db_id)) {
            print STDERR "StableIdentifiers.get_release_id_from_release_db_id: WARNING - we have no releases for release_num=$release_num, PROJECT_NAME=$PROJECT_NAME!!\n";
            return undef;
    }

	my $release_ids = $identifier_database_dba->fetch_instance_by_remote_attribute(
		'ReleaseId',
		[
		 ['instanceDB_ID','=',[$db_id]],
		 ['release','=',[$release_db_id]],
#		 ['release.project.name','=',[$PROJECT_NAME]],
#		 ['release.num','=',[$release_num]]
		]);

	return $release_ids;
}

sub get_max_version_num_from_stable_identifier {
	my ($self, $stable_identifier) = @_;
	
	my $max_stable_identifier_version = $self->get_max_stable_identifier_version_from_stable_identifier($stable_identifier);
	my $max_identifier_version = $self->get_att_value_from_identifier_database_instance($max_stable_identifier_version, 'identifierVersion');
	
	return $max_identifier_version;
}

sub get_max_stable_identifier_version_from_stable_identifier {
	my ($self, $stable_identifier) = @_;
	
    my $identifier_database_dba = $self->get_identifier_database_dba();
    if (!$identifier_database_dba) {
    	return undef;
    }
    
	$identifier_database_dba->load_attribute_values( $stable_identifier, 'stableIdentifierVersion' );
	my $stable_identifier_versions = $stable_identifier->stableIdentifierVersion;
	
	if (!(defined $stable_identifier_versions) || !(scalar($stable_identifier_versions) =~ /ARRAY/) || scalar(@{$stable_identifier_versions})<1) {
		print STDERR "StableIdentifiers.get_max_stable_identifier_version_from_stable_identifier: WARNING - no versions for this StableIdentifier instance!!\n";
		return undef;
	}
	
	my $stable_identifier_version;
	my $max_identifier_version = (-1);
	my $max_stable_identifier_version = undef;
	my $identifier_version;
	foreach $stable_identifier_version (@{$stable_identifier_versions}) {
		$identifier_version = $self->get_att_value_from_identifier_database_instance($stable_identifier_version, 'identifierVersion');
		if (defined $identifier_version && $max_identifier_version < $identifier_version) {
			$max_identifier_version = $identifier_version;
			$max_stable_identifier_version = $stable_identifier_version;
		}
	}
	
	return $max_stable_identifier_version;
}

sub get_given_stable_identifier_version_from_stable_identifier {
	my ($self, $stable_identifier, $version) = @_;
	
	my $max_identifier_version = (-1);

	if (!(defined $version) || $version eq "") {
		print STDERR "StableIdentifiers.get_stable_identifier_version: WARNING - no version given!!\n";
		return $max_identifier_version;
	}
	
    my $identifier_database_dba = $self->get_identifier_database_dba();
    if (!$identifier_database_dba) {
    	return $max_identifier_version;
    }
    
	$identifier_database_dba->load_attribute_values( $stable_identifier, 'stableIdentifierVersion' );
	my $stable_identifier_versions = $stable_identifier->stableIdentifierVersion;
	
	if (!$stable_identifier_versions || !(scalar($stable_identifier_versions) =~ /ARRAY/) || scalar(@{$stable_identifier_versions})<1) {
		print STDERR "StableIdentifiers.get_stable_identifier_version: WARNING - no versions for this StableIdentifier instance!!\n";
		return $max_identifier_version;
	}
	
	my $stable_identifier_version;
	my $identifier_version;
	my $best_stable_identifier_version = undef;
	foreach $stable_identifier_version (@{$stable_identifier_versions}) {
		$identifier_version = $self->get_att_value_from_identifier_database_instance($stable_identifier_version, 'identifierVersion');
		if ($identifier_version && $identifier_version eq $version) {
			$best_stable_identifier_version = $stable_identifier_version;
			last;
		}
	}
	
	return $best_stable_identifier_version;
}

# This subroutine returns the changes that have occurred since the
# given release.  The returned value is a hash, with the following
# key/value pairs:
#
# 
sub get_changes_from_stable_identifier {
	my ($self, $stable_identifier, $release_num) = @_;
	
	if (!(defined $release_num) || $release_num eq "") {
		print STDERR "StableIdentifiers.get_changes_from_stable_identifier: WARNING - no release number given!!\n";
		return undef;
	}
	
    my $identifier_database_dba = $self->get_identifier_database_dba();
    if (!$identifier_database_dba) {
    	return undef;
    }
    
	$identifier_database_dba->load_attribute_values($stable_identifier, 'stableIdentifierVersion');
	my $stable_identifier_versions = $stable_identifier->stableIdentifierVersion;
	
	if (!$stable_identifier_versions || !(scalar($stable_identifier_versions) =~ /ARRAY/) || scalar(@{$stable_identifier_versions})<1) {
		print STDERR "StableIdentifiers.get_changes_from_stable_identifier: WARNING - no versions for this StableIdentifier instance!!\n";
		return undef;
	}
	
	my %changes = ();
	
	my $stable_identifier_version;
	my $final_new_schema_class_name = undef;
	my @all_deleted_attributes_with_content = ();
	my @all_added_attributes_with_content = ();
	my @all_changed_attributes = ();
	my $release;
	my $num;
	foreach $stable_identifier_version (@{$stable_identifier_versions}) {
		$release = $self->get_max_release_from_stable_identifier_version_instance($stable_identifier_version);
		$num = $self->get_att_value_from_identifier_database_instance($release, 'num');
		# Dont bother with releases earlier than release_num		
		if ($num<$release_num) {
			next;
		}

		$final_new_schema_class_name = $self->get_att_value_from_identifier_database_instance($stable_identifier_version, 'newSchemaClassName');
		
		$identifier_database_dba->load_attribute_values($stable_identifier_version, 'deletedAttributesWithContent');
		my $deleted_attributes_with_content = $stable_identifier_version->deletedAttributesWithContent;
		if ($deleted_attributes_with_content) {
			@all_deleted_attributes_with_content = (@all_deleted_attributes_with_content, @{$deleted_attributes_with_content});
		}
		
		$identifier_database_dba->load_attribute_values($stable_identifier_version, 'addedAttributesWithContent');
		my $added_attributes_with_content = $stable_identifier_version->addedAttributesWithContent;
		if ($added_attributes_with_content) {
			@all_added_attributes_with_content = (@all_added_attributes_with_content, @{$added_attributes_with_content});
		}
		
		$identifier_database_dba->load_attribute_values($stable_identifier_version, 'changedAttributes');
		my $changed_attributes = $stable_identifier_version->changedAttributes;
		if ($changed_attributes) {
			@all_changed_attributes = (@all_changed_attributes, @{$changed_attributes});
		}
	}
	
	$changes{'newSchemaClassName'} = $final_new_schema_class_name;
	$changes{'deletedAttributesWithContent'} = \@all_deleted_attributes_with_content;
	$changes{'addedAttributesWithContent'} = \@all_added_attributes_with_content;
	$changes{'changedAttributes'} = \@all_changed_attributes;
	
	return %changes;
}

# Get a StableIdentifierVersion instance.  If the version is
# defined, use it in getting the instance, if it isn't, get
# a StableIdentifierVersion instance with the highest version number.
sub get_stable_identifier_version {
	my ($self, $stable_identifier, $version) = @_;
	
	if (!(defined $version) || $version eq "") {
		return $self->get_max_stable_identifier_version_from_stable_identifier($stable_identifier);
	}
	
	return $self->get_given_stable_identifier_version_from_stable_identifier($stable_identifier, $version);
}

# Given an array of values, pick out and return the first value.
# If the supplied argument is undef, or is not an array or is
# empty, then return undef.
sub get_attribute_val_from_list {
	my ($self, $values) = @_;

	my $value = undef;
	if ($values && scalar($values) =~ /ARRAY/ && scalar(@{$values})>0) {
		$value = $values->[0];
	}
	
	return $value;
}

# Given an instance and an attribute name, get a single
# attribute value.
sub get_att_value_from_identifier_database_instance {
	my ($self, $instance, $att_name) = @_;
	
	if (!(defined $att_name)) {
#		print STDERR "StableIdentifiers.get_att_value_from_identifier_database_instance: att_name is not defined for class=" . $instance->class() . "\n";
		return undef;
	}
	
	if (!(defined $instance)) {
#		print STDERR "StableIdentifiers.get_att_value_from_identifier_database_instance: instance is not defined for att_name=$att_name!!!!\n";
		return undef;
	}
	
	if (!($instance->is_valid_attribute($att_name))) {
#		print STDERR "StableIdentifiers.get_att_value_from_identifier_database_instance: att_name=$att_name is not a valid attribute!!!!\n";
		return undef;
	}
	
    my $identifier_database_dba = $self->get_identifier_database_dba();
    if (!$identifier_database_dba) {
    	return undef;
    }
    
	$identifier_database_dba->load_attribute_values( $instance, $att_name );
	my $values = $instance->$att_name;
	my $value = $self->get_attribute_val_from_list($values);
	
	return $value;
}

# Get the most highly numbered ReleaseID instance from the
# supplied StableIdentifierVersion.
sub get_max_release_id_from_stable_identifier_version_instance {
	my ($self, $stable_identifier_version) = @_;
	
	if (!(defined $stable_identifier_version)) {
		print STDERR "StableIdentifiers.get_max_release_id_from_stable_identifier_version_instance: stable_identifier_version is undef!!\n";
    	return undef;
	}
	
    my $identifier_database_dba = $self->get_identifier_database_dba();
    if (!(defined $identifier_database_dba)) {
		print STDERR "StableIdentifiers.get_max_release_id_from_stable_identifier_version_instance: WARNING - identifier database DBA cannot be found!!\n";
    	return undef;
    }
    
	# Find highest numbered release ID
	my $releaseIds = $stable_identifier_version->releaseIds;
	if (!(defined $releaseIds)) {
		print STDERR "StableIdentifiers.get_max_release_id_from_stable_identifier_version_instance: releaseIds is undef!!\n";
    	return undef;
	}
	if (scalar(@{$releaseIds})<1) {
		print STDERR "StableIdentifiers.get_max_release_id_from_stable_identifier_version_instance: releaseIds is empty!!\n";
    	return undef;
	}
	
	my $releaseId;
	my $max_releaseId = undef;
	my $releases;
	my $release;
	my $max_num = (-1);
	my $nums;
	my $num;
	my $reactome_release_column_name = $self->reactome_release_column_name;
	if (!(defined $reactome_release_column_name)) {
		print STDERR "StableIdentifiers.get_max_release_id_from_stable_identifier_version_instance: WARNING - reactome_release_column_name is undef!!\n";
    	return undef;
	}
		
	foreach $releaseId (@{$releaseIds}) {
		if (!(defined $releaseId)) {
			print STDERR "StableIdentifiers.get_max_release_id_from_stable_identifier_version_instance: WARNING - releaseId is undef!!\n";
			next;
		}
		
		$release = $self->get_att_value_from_identifier_database_instance($releaseId, $reactome_release_column_name);
		if (!(defined $release)) {
			print STDERR "StableIdentifiers.get_max_release_id_from_stable_identifier_version_instance: WARNING - release is undef!!\n";
			next;
		}
		
		$num = $self->get_att_value_from_identifier_database_instance($release, 'num');
		if (!(defined $num)) {
			print STDERR "StableIdentifiers.get_max_release_id_from_stable_identifier_version_instance: WARNING - num is undef!!\n";
			next;
		}
		
		if (defined $num && $num > $max_num) {
			$max_num = $num;
			$max_releaseId = $releaseId;
		}
	}

	return $max_releaseId;
}


# Get the most highly numbered Release instance from the
# supplied StableIdentifierVersion.
sub get_max_release_from_stable_identifier_version_instance {
	my ($self, $stable_identifier_version) = @_;
	
    my $identifier_database_dba = $self->get_identifier_database_dba();
    if (!$identifier_database_dba) {
		print STDERR "StableIdentifiers.get_max_release_from_stable_identifier_version_instance: WARNING - identifier database DBA cannot be found!!\n";
    	return undef;
    }
    
	my $releaseId = $self->get_max_release_id_from_stable_identifier_version_instance($stable_identifier_version);
	
	my $reactome_release_column_name = $self->reactome_release_column_name;
	$identifier_database_dba->load_attribute_values($releaseId, $reactome_release_column_name);
	my $releases = $releaseId->$reactome_release_column_name;
	my $release = $self->get_attribute_val_from_list($releases);
	
	return $release;
}

# Given the name of a database, look to see if it is a known
# release or slice in the identifier database.
sub get_release_from_db_name {
    my ($self, $db_name) = @_;
    
    my $identifier_database_dba = $self->get_identifier_database_dba();
    if (!$identifier_database_dba) {
		print STDERR "StableIdentifiers.get_release_from_db_name: WARNING - identifier database DBA cannot be found!!\n";
    	return undef;
    }
    
    my $reactome_release_table_name = $self->reactome_release_table_name;
	my $releases = $identifier_database_dba->fetch_instance_by_remote_attribute(
		$reactome_release_table_name,
		[['releaseDbParams.dbName','=',[$db_name]]]);
	my $release = $self->get_attribute_val_from_list($releases);
	
	if (!(defined $release)) {
		$releases = $identifier_database_dba->fetch_instance_by_remote_attribute(
			$reactome_release_table_name,
			[['sliceDbParams.dbName','=',[$db_name]]]);
		$release = $self->get_attribute_val_from_list($releases);
	}
	
    return $release;
}

# Given a release number, look to see if it is a known
# release in the identifier database.
sub get_release_from_num {
    my ($self, $num, $project) = @_;
    
    my $identifier_database_dba = $self->get_identifier_database_dba();
    if (!$identifier_database_dba) {
		print STDERR "StableIdentifiers.get_release_from_num: WARNING - identifier database DBA cannot be found!!\n";
    	return undef;
    }
    my $project_name = $PROJECT_NAME;
    if (defined $project) {
    	$project_name = $project;
    }
    
	my $releases = $identifier_database_dba->fetch_instance_by_remote_attribute(
		$self->reactome_release_table_name,
		[
		 ['num','=',[$num]],
		 ['project.name','=',[$project_name]]
		]);
	my $release = $self->get_attribute_val_from_list($releases);
	
    return $release;
}

# Given a release number, look to see if it is a known
# release in the identifier database and if so return it's DB_ID;
# if not, return undef.
sub get_release_db_id_from_num {
    my ($self, $num) = @_;
    
    my $release = $self->get_release_from_num($num);
    
    if (defined $release) {
    	return $release->db_id();
    } else {
    	return undef;
    }
}

# Given the name of a database, look to see if it is a known
# release in the identifier database.
sub is_known_release_name {
    my ($self, $db_name) = @_;
    
    if (!(defined $db_name)) {
    	return 0;
    }
    
	my $release = $self->get_release_from_db_name($db_name);
	
	if (defined $release) {
		return 1;
	}
    
    return 0;
}

sub is_old_release {
    my $self = shift;
    
	my $old_release_flag = 0;

	my $current_release_db_name = $self->get_current_release_db_name();
	my $most_recent_release_db_name = $self->get_most_recent_release_db_name();
		
	if ($current_release_db_name && $most_recent_release_db_name &&
		!($current_release_db_name eq $most_recent_release_db_name) &&
		$self->is_known_release_name($current_release_db_name)) {
		$old_release_flag = 1;
	}
	
	return $old_release_flag;
}

# Returns 1 if the stable identifier/version pair can be found
# in the numbered release.  If the version is undef, it will
# be ignored.
sub is_identifier_version_in_release_num {
    my ($self, $identifier, $version, $release_num) = @_;
    
    if (!(defined $identifier) || !(defined $release_num)) {
    	print STDERR "StableIdentifiers.is_identifier_version_in_release_num: WARNING - identifier or release_num is undefined!\n";
    	return 0;
    }
    
    my $identifier_database_dba = $self->get_identifier_database_dba();
    if (!$identifier_database_dba) {
    	return 0;
    }
    
    my $stable_identifiers;
#    my $release_num_remote_attribute = 'stableIdentifierVersion.releaseIds.' . $self->reactome_release_column_name . '.num';
#    my $project_name_remote_attribute = 'stableIdentifierVersion.releaseIds.' . $self->reactome_release_column_name . '.project.name';
    my $release_remote_attribute = 'stableIdentifierVersion.releaseIds.' . $self->reactome_release_column_name;

    # First, find the release associated with the given release number
    my $release_db_id = $self->get_release_db_id_from_num($release_num);
    if (!(defined $release_db_id)) {
            print STDERR "StableIdentifiers.is_identifier_version_in_release_num: WARNING - we have no releases for release_num=$release_num, PROJECT_NAME=$PROJECT_NAME!!\n";
            return 0;
    }

    if (defined $version) {
		$stable_identifiers = $identifier_database_dba->fetch_instance_by_remote_attribute(
			'StableIdentifier',
			[['identifierString','=',[$identifier]],
			 [$release_remote_attribute,'=',[$release_db_id]],
#			 [$project_name_remote_attribute,'=',[$PROJECT_NAME]],
#			 [$release_num_remote_attribute,'=',[$release_num]],
			 ['stableIdentifierVersion.identifierVersion','=',[$version]]]);
    } else {
 		$stable_identifiers = $identifier_database_dba->fetch_instance_by_remote_attribute(
			'StableIdentifier',
			[['identifierString','=',[$identifier]],
			 [$release_remote_attribute,'=',[$release_db_id]],
#			 [$project_name_remote_attribute,'=',[$PROJECT_NAME]],
#			 [$release_num_remote_attribute,'=',[$release_num]]
			 ]);
    }
    
    if ($stable_identifiers && $stable_identifiers =~ /ARRAY/ && scalar(@{$stable_identifiers})>0) {
    	return 1;
    }
    
    return 0;
}

# Checks the given identifier (of the form REACT_...) against the
# release database for the numbered release and returns the
# corresponding instance if it is in there.
sub fetch_instance_by_identifier_in_release_database {
    my ($self, $identifier, $release_num) = @_;
    
    my $instance = undef;

    if (!(defined $identifier)) {
    	print STDERR "StableIdentifiers.fetch_instance_by_identifier_in_release_database: identifier is undef, aborting!!\n";
    	return $instance;
    }
    if (!(defined $release_num)) {
    	print STDERR "StableIdentifiers.fetch_instance_by_identifier_in_release_database: release_num is undef, aborting!!\n";
    	return $instance;
    }
    
	my $release_db_name = $self->get_db_name_from_release_num($release_num);
	
	if (!(defined $release_db_name)) {
		print STDERR "StableIdentifiers.fetch_instance_by_identifier_in_release_database: WARNING - could not find a release database name for release_num=$release_num\n";
		return $instance;
	}
	
	my $release_dba = $self->get_dba_from_db_name($release_db_name);

	if (!$release_dba) {
		print STDERR "StableIdentifiers.fetch_instance_by_identifier_in_release_database: WARNING - could not find a release database name for release_db_name=$release_db_name\n";
		return $instance;
	}
	
	my $instances = $release_dba->fetch_instance_by_remote_attribute(
		'DatabaseObject',
		[['stableIdentifier.identifier','=',[$identifier]]]);
	
    if (scalar(@{$instances})>0) {
    	$instance = $instances->[0];
    }
    
    return $instance;
}

# Checks the given identifier (of the form REACT_...) against the
# release database for the numbered release and returns 1 if
# it is in there.
sub is_identifier_in_release_database {
    my ($self, $identifier, $release_num) = @_;
    
	my $instance = $self->fetch_instance_by_identifier_in_release_database($identifier, $release_num);
    
    if (defined $instance) {
    	return 1;
    } else {
    	return 0;
    }
}

# Given the name of a release database, find the corresponding
# release number.  If nothing can be found, return undef.
sub get_release_num_from_db_name {
    my ($self, $db_name) = @_;
    
	my $release = $self->get_release_from_db_name($db_name);
	
	if (!$release) {
		return undef;
	}
    
    my $identifier_database_dba = $self->get_identifier_database_dba();
    if (!$identifier_database_dba) {
    	return (-1);
    }
    
	$identifier_database_dba->load_attribute_values( $release, "num" );
	my $nums = $release->num;
	my $num = $self->get_attribute_val_from_list($nums);

    return $num;
}

# Returns the most recent release as a shell.
sub get_most_recent_release {
    my $self = shift;
    
    # Use cached value, if available
    if ($self->most_recent_release()) {
    	return $self->most_recent_release();
    }
    
    my $identifier_database_dba = $self->get_identifier_database_dba();
    if (!$identifier_database_dba) {
    	return undef;
    }
    my $releases = $identifier_database_dba->fetch_all_class_instances_as_shells($self->reactome_release_table_name);
    if (!$releases) {
    	return undef;
    }
    
    my $max_release;
    my $nums;
    my $num;
    my $max_num = (-1);
    my $projects;
    my $project;
    my $project_names;
    my $project_name;
    foreach my $release (@{$releases}) {
    	$identifier_database_dba->load_attribute_values($release, "num");
    	$identifier_database_dba->load_attribute_values($release, "project");
    	$nums = $release->num;
		$num = $self->get_attribute_val_from_list($nums);
    	$projects = $release->project;
		$project = $self->get_attribute_val_from_list($projects);
    	$project_names = $project->name;
		$project_name = $self->get_attribute_val_from_list($project_names);
		if (defined $project_name && !($project_name eq $PROJECT_NAME)) {
			next;
		}
		if ($num && $num > $max_num) {
			$max_num = $num;
			$max_release = $release;
		}
    }
    
    # Cache value
    $self->most_recent_release($max_release);
    
    return $max_release;
}

# Returns the most recent release number.
sub get_most_recent_release_num {
    my $self = shift;

    # Use cached value, if available
    if ($self->most_recent_release_num()) {
    	return $self->most_recent_release_num();
    }
    
	my $release = $self->get_most_recent_release();
	my $release_num = $self->get_att_value_from_identifier_database_instance($release, 'num');
	
    # Cache value
    $self->most_recent_release_num($release_num);
    
	return $release_num;
}

sub get_most_recent_release_db_name {
    my $self = shift;
    
    # Use cached value, if available
    if ($self->most_recent_release_db_name()) {
    	return $self->most_recent_release_db_name();
    }
    
    my $release = $self->get_most_recent_release();
	my $db_name = $self->get_db_name_from_release($release);
    
    # Cache value
    $self->most_recent_release_db_name($db_name);
    
    return $db_name;
}

# Returns 1 if the supplied release is the most current one,
# 0 otherwise.  Returns 0 if release is undef.
sub is_most_recent_release {
    my ($self, $release) = @_;
    
    if (!$release) {
    	return 0;
    }
    
	my $most_recent_release_num = $self->get_most_recent_release_num();
	my $release_num = $self->get_att_value_from_identifier_database_instance($release, 'num');
	
	if ($release_num == $most_recent_release_num) {
		return 1;
	}
	
	return 0;
}

# Checks the given stable ID to see if it occurs in the most
# recent release - if yes, returns 1, otherwise returns 0.
# An undef identifier causes 0 to be returned.
sub is_identifier_existant_in_most_recent_release {
	my ($self, $identifier) = @_;
    
    if (!$identifier) {
    	return 0;
    }
    
	my $stable_identifier = $self->get_stable_identifier_from_identifier_most_recent_release($identifier);
	
	if ($stable_identifier) {
		return 1;
	}
	
	return 0;
}

# This commented-out stuff has been moved to:
#
# GKB::StableIdentifiers::Check

## Finds all stable identifiers where there are gaps in the
## version numbers.  E.g. places where the version number jumps
## from 1 to 3.  Returns a hash, keyed by stable ID.  Each
## element is an array of pairs, each pair is a pair of
## non-contiguous version numbers found for that stable ID.
#sub find_version_number_gaps {
#	my ($self) = @_;
#	
#	my %version_number_gaps = ();
#    
#    my $identifier_database_dba = $self->get_identifier_database_dba();
#    if (!$identifier_database_dba) {
#    	return %version_number_gaps;
#    }
#    
#	my $db_ids = $identifier_database_dba->fetch_db_ids_by_class('StableIdentifier');
#	my $db_id;
#	my $stable_identifiers;
#	my $stable_identifier;
#	my $identifier;
#	foreach $db_id (@{$db_ids}) {
#		$stable_identifiers = $identifier_database_dba->fetch_instance_by_db_id($db_id);
#		$stable_identifier = $stable_identifiers->[0];
#		
#		$identifier = $stable_identifier->identifierString->[0];
#		if (!(defined $identifier)) {
#			next;
#		}
#		
#		my @version_number_gap_list = $self->find_version_number_gap_list_for_stable_identifier($stable_identifier);
#		$version_number_gaps{$identifier} = \@version_number_gap_list;
#	}
#	
#	return %version_number_gaps;
#}
#
#sub find_version_number_gap_list_for_stable_identifier {
#	my ($self, $stable_identifier) = @_;
#	
#	
#	
#	# This is here for debug purposes only
#	my $identifier = $stable_identifier->identifierString->[0];
#	
#	my @version_number_gap_list = ();
#	my $stable_identifier_versions = $stable_identifier->stableIdentifierVersion;
#	my $stable_identifier_version;
#	my $last_version = undef;
#	my $version;
#	foreach $stable_identifier_version (@{$stable_identifier_versions}) {
#		$version = $stable_identifier_version->identifierVersion->[0];
#		
#		if ($identifier eq "REACT_1882") {
#			if (defined $last_version) {
#				print STDERR "StableIdentifiers.find_version_number_gap_list_for_stable_identifier: last_version=$last_version\n";
#			}
#			print STDERR "StableIdentifiers.find_version_number_gap_list_for_stable_identifier: version=$version\n";
#		}
#		
#		
#		if (defined $last_version && $version > $last_version + 1) {
#			if ($identifier eq "REACT_1882") {
#				print STDERR "StableIdentifiers.find_version_number_gap_list_for_stable_identifier: creating non-contiguous version pair\n";
#			}
#			
#			my @non_contiguous_versions = ($last_version, $version);
#			if ($identifier eq "REACT_1882") {
#				print STDERR "StableIdentifiers.find_version_number_gap_list_for_stable_identifier: non_contiguous_versions[0]=" . $non_contiguous_versions[0] . ", non_contiguous_versions[1]=" . $non_contiguous_versions[1] . "\n";
#			}
#			push(@version_number_gap_list, \@non_contiguous_versions);
#		}
#		$last_version = $version;
#	}
#	
#	if ($identifier eq "REACT_1882") {
#		print STDERR "StableIdentifiers.find_version_number_gap_list_for_stable_identifier: scalar(version_number_gap_list)=" . scalar(@version_number_gap_list) . "\n";
#	}
#	
#	return @version_number_gap_list;
#}
#
## Finds all stable identifiers where there are gaps in the
## release numbers.  E.g. places where the release number jumps
## from 17 to 19.  Returns a hash, keyed by stable ID.  Each
## element is an array of triplets, each triplet contains a pair of
## non-contiguous release numbers found for that stable ID, plus a
## reference to an array of release numbers, for which the stable
## ID is found in the corresponding slice databases, but not in
## the identifier database.
##
## If correction_flag is non-zero, then an attempt will be made
## to fix missing stable IDs; use this with care.
#sub find_release_number_gaps {
#	my ($self, $correction_flag) = @_;
#	
#	my %release_number_gaps = ();
#    
#    my $identifier_database_dba = $self->get_identifier_database_dba();
#    if (!$identifier_database_dba) {
#    	return %release_number_gaps;
#    }
#    
#	my $db_ids = $identifier_database_dba->fetch_db_ids_by_class('StableIdentifier');
#	my $db_id;
#	my $stable_identifiers;
#	my $stable_identifier;
#	my $identifier;
#	foreach $db_id (@{$db_ids}) {
#		$stable_identifiers = $identifier_database_dba->fetch_instance_by_db_id($db_id);
#		$stable_identifier = $stable_identifiers->[0];
#		
#		$identifier = $stable_identifier->identifierString->[0];
#		if (!(defined $identifier)) {
#			next;
#		}
#		
##		print STDERR "StableIdentifiers.find_release_number_gaps: dealing with identifier=$identifier\n";
#	
#		my @release_number_gap_list = $self->find_release_number_gap_list_for_stable_identifier($stable_identifier, $correction_flag);
#		$release_number_gaps{$identifier} = \@release_number_gap_list;
#	}
#	
#	return %release_number_gaps;
#}
#
#sub find_release_number_gap_list_for_stable_identifier {
#	my ($self, $stable_identifier, $correction_flag) = @_;
#	
#	my @release_number_gap_list = ();
#
#    my $identifier_database_dba = $self->get_identifier_database_dba();
#    if (!$identifier_database_dba) {
#    	return @release_number_gap_list;
#    }
#
#	$identifier_database_dba->inflate_instance($stable_identifier);
#	my $identifier = $self->get_att_value_from_identifier_database_instance($stable_identifier, 'identifierString');
#	my $display_name = $self->get_att_value_from_identifier_database_instance($stable_identifier, '_displayName');
#	if (!(defined $display_name)) {
#		$stable_identifier->_displayName($identifier);
#	}
#	
#	my $stable_identifier_versions = $stable_identifier->stableIdentifierVersion;
#	my $stable_identifier_version;
#	my $release_ids;
#	my $release_id;
#	my $release;
#	my $last_release_num = undef;
#	my $i;
#	my @release_nums = ();
#	my $db_id;
#	foreach $stable_identifier_version (@{$stable_identifier_versions}) {
#		# Get the releases in which this version was
#		# valid
#		$identifier_database_dba->load_attribute_values($stable_identifier_version, 'releaseIds');
#		$release_ids = $stable_identifier_version->releaseIds;
#		foreach $release_id (@{$release_ids}) {
#			$release = $self->get_att_value_from_identifier_database_instance($release_id, $self->reactome_release_column_name);
#			my $release_num = $self->get_att_value_from_identifier_database_instance($release, 'num');
#			
#			if ($identifier eq "REACT_1882") {
#				print STDERR "StableIdentifiers.find_release_number_gap_list_for_stable_identifier: UNSORTED release_num=$release_num\n";
#			}
#
#			if (!(defined $release_num)) {
#				# This shouldn't happen
#				print STDERR "StableIdentifiers.find_release_number_gap_list_for_stable_identifier: missing release number in release!!\n";
#				next;
#			}
#			
#			push(@release_nums, $release_num);
#		}
#		
#	}
#	
#	if ($identifier eq "REACT_1882") {
#		print STDERR "StableIdentifiers.find_release_number_gap_list_for_stable_identifier: BEFORE release num count=" . scalar(@release_nums) . "\n";
#		print STDERR "StableIdentifiers.find_release_number_gap_list_for_stable_identifier: BEFORE release_nums=@release_nums\n";
#	}
#
#	@release_nums = sort {$a <=> $b} @release_nums;
#		
#	if ($identifier eq "REACT_1882") {
#		print STDERR "StableIdentifiers.find_release_number_gap_list_for_stable_identifier: AFTER release num count=" . scalar(@release_nums) . "\n";
#		print STDERR "StableIdentifiers.find_release_number_gap_list_for_stable_identifier: AFTER release_nums=@release_nums\n";
#	}
#
#	my $reactome_release_column_name = $self->reactome_release_column_name;
#	foreach my $release_num (@release_nums) {
#		if ($identifier eq "REACT_1882") {
#			if (defined $last_release_num) {
#				print STDERR "StableIdentifiers.find_release_number_gap_list_for_stable_identifier: last_release_num=$last_release_num\n";
#			}
#			print STDERR "StableIdentifiers.find_release_number_gap_list_for_stable_identifier: SORTED release_num=$release_num\n";
#		}
#			
#			
#		if (defined $last_release_num && $release_num > $last_release_num + 1) {
#			if ($identifier eq "REACT_1882") {
#				print STDERR "StableIdentifiers.find_release_number_gap_list_for_stable_identifier: creating non-contiguous release pair\n";
#			}
#				
#			my @present_in_release = ();
#			for ($i=$last_release_num + 1; $i<$release_num; $i++) {
#				if ($self->is_identifier_in_release_database($identifier, $i)) {
#					if ($identifier eq "REACT_1882") {
#						print STDERR "StableIdentifiers.find_release_number_gap_list_for_stable_identifier: i=$i\n";
#					}
#					push(@present_in_release, $i);
#					
#					if ($correction_flag) {
#						# OK, let's fix things
#						my $instance = $self->fetch_instance_by_identifier_in_release_database($identifier, $i);
#						
#						if ($instance) {
#							# Get version number of missing release
#							my $release_stable_identifier = $instance->stableIdentifier->[0];
#							my $release_version = $release_stable_identifier->identifierVersion->[0];
#							
#							print STDERR "StableIdentifiers.find_release_number_gap_list_for_stable_identifier: release_version=$release_version\n";
#							
#							# Check to see if the version already
#							# exists and create it if not.
#							my $repaired_stable_identifier_version = undef;
#							foreach $stable_identifier_version (@{$stable_identifier_versions}) {
##								my $version = $stable_identifier_version->identifierVersion;
#								my $version = $self->get_att_value_from_identifier_database_instance($stable_identifier_version, 'identifierVersion');
#								if ($version eq $release_version) {
#									$repaired_stable_identifier_version = $stable_identifier_version;
#									$identifier_database_dba->inflate_instance($repaired_stable_identifier_version);
#								}
#							}
#							if (!$repaired_stable_identifier_version) {
#								$repaired_stable_identifier_version = GKB::Instance->new(
#									-ONTOLOGY => $identifier_database_dba->ontology,
#									-CLASS => 'StableIdentifierVersion');  
#							    
#								$repaired_stable_identifier_version->inflated(1);
#								$repaired_stable_identifier_version->_displayName($i);
#								$repaired_stable_identifier_version->identifierVersion($release_version);
#								$repaired_stable_identifier_version->creationComment("Inserted to repair damaged identifier database");
#								
#								print STDERR "StableIdentifiers.find_release_number_gap_list_for_stable_identifier: repaired_stable_identifier_version=$repaired_stable_identifier_version\n";
#								
#								$identifier_database_dba->store($repaired_stable_identifier_version);
#								$stable_identifier->add_attribute_value("stableIdentifierVersion", $repaired_stable_identifier_version);
##								$identifier_database_dba->update($stable_identifier, "stableIdentifierVersion");
#							}
#							
#							# Add release to version
#							$release = $self->get_release_from_num($i);
#							$db_id = $instance->db_id;
#							my $release_id = GKB::Instance->new(
#									-ONTOLOGY => $identifier_database_dba->ontology,
#									-CLASS => 'ReleaseId');
#							$release_id->inflated(1);
#							$release_id->_displayName("release=$i, DB_ID=$db_id");
#							$release_id->$reactome_release_column_name($release);
#							$release_id->instanceDB_ID($db_id);
#								
#							print STDERR "StableIdentifiers.find_release_number_gap_list_for_stable_identifier: release_id=$release_id\n";
#								
#							$identifier_database_dba->store($release_id);
#							$repaired_stable_identifier_version->add_attribute_value("releaseIds", $release_id);
#							$identifier_database_dba->update($repaired_stable_identifier_version, "releaseIds");
#							$identifier_database_dba->update($stable_identifier, "stableIdentifierVersion");
#						}
#					}
#				}
#			}
#				
#			my @non_contiguous_releases = ($last_release_num, $release_num, \@present_in_release);
#			if ($identifier eq "REACT_1882") {
#				print STDERR "StableIdentifiers.find_release_number_gap_list_for_stable_identifier: non_contiguous_releases[0]=" . $non_contiguous_releases[0] . ", non_contiguous_releases[1]=" . $non_contiguous_releases[1] . "\n";
#			}
#			push(@release_number_gap_list, \@non_contiguous_releases);
#		}
#		
#		$last_release_num = $release_num;
#	}
#	
#	if ($identifier eq "REACT_1882") {
#		print STDERR "StableIdentifiers.find_release_number_gap_list_for_stable_identifier: scalar(release_number_gap_list)=" . scalar(@release_number_gap_list) . "\n";
#	}
#	
#	return @release_number_gap_list;
#}
#
## Returns an array of duplicated stable IDs
#sub find_duplicated_stable_identifiers {
#	my ($self) = @_;
#	
#	my @duplicated_stable_identifiers = ();
#
#    my $identifier_database_dba = $self->get_identifier_database_dba();
#    if (!$identifier_database_dba) {
#    	return @duplicated_stable_identifiers;
#    }
#
#	my $stable_identifiers = $identifier_database_dba->fetch_instance(-CLASS => "StableIdentifier");
#	my %stable_identifier_ids = ();
#	my $stable_identifier;
#	foreach $stable_identifier (@{$stable_identifiers}) {
#		my $identifier_string = $stable_identifier->identifierString->[0];
#		
##		print STDERR "StableIdentifiers.find_duplicated_stable_identifiers: identifier_string=$identifier_string\n";
#		
#		if (defined $identifier_string) {
#			if (!(defined $stable_identifier_ids{$identifier_string})) {
#				$stable_identifier_ids{$identifier_string} = 1;
#			} else {
#				$stable_identifier_ids{$identifier_string}++;
#			}
#		} else {
#			$stable_identifier_ids{"NULL"}++;
#		}
#	}
#	
#	my $identifier_string;
#	foreach $identifier_string (sort(keys(%stable_identifier_ids))) {
#		if ($stable_identifier_ids{$identifier_string} > 1) {
#			push(@duplicated_stable_identifiers, $identifier_string);
#		}
#	}
#	
#	return @duplicated_stable_identifiers;
#}
#
## Returns an array of orphan ReleaseId instances (array will be
## empty if none could be found).  You can optionally supply a
## reference to an array of DB_IDs, which will be used to restrict the
## search to ReleaseId instances with instanceDB_ID attributes
## with those values.  This is mainly for debugging purposes.  It
## speeds things up quite a bit.
#sub find_orphan_release_ids {
#	my ($self, $db_ids) = @_;
#	
#	my @orphan_release_ids = ();
#
#    my $identifier_database_dba = $self->get_identifier_database_dba();
#    if (!$identifier_database_dba) {
#    	return @orphan_release_ids;
#    }
#
#	my $release_ids = [];
#	if (defined $db_ids) {
#		my $query = [['instanceDB_ID','=',$db_ids]];
#		$release_ids = $identifier_database_dba->fetch_instance_by_remote_attribute('ReleaseId',$query);
#	} else {
#		$release_ids = $identifier_database_dba->fetch_instance(-CLASS => "ReleaseId");
#	}
#	my $release_id;
#	my $reverse_attribute;
#	foreach $release_id (@{$release_ids}) {
#		$reverse_attribute = $release_id->reverse_attribute_value('releaseIds');
#		
#		if (scalar(@{$reverse_attribute}) == 0) {
#			push(@orphan_release_ids, $release_id);
#		}
#	}
#	
#	return @orphan_release_ids;
#}

# If the identifier database contains a State instance,
# the stable ID will be extracted, incremented, and put back.  If no
# such instance exists, a fresh new instance will be created.
# 
# If a problem occurred while doing the increment which resulted in
# no increment taking place and no new StableIdentifier instance
# being inserted into the database, then this method will return
# false.  If everything went OK, it will return true.
sub increment_most_recent_stable_identifier {
	my ($self) = @_;

	my $state = $self->get_state();
		
	my $dba = $self->get_identifier_database_dba();
	if (!(defined $dba)) {
		return $state;
	}
		
	my $most_recent_stable_identifier = undef;
		
	# A null value most probably means that the identifier database
	# has been freshly created, so create a very first
	# State instance
	if (!(defined $state)) {
		
		print STDERR "Hoppsa, state not defined!!\n";
		
		$state = GKB::Instance->new(-ONTOLOGY => $dba->ontology, -CLASS => 'State');
		$state->inflated(1);
			
		$most_recent_stable_identifier = GKB::Instance->new(-ONTOLOGY => $dba->ontology, -CLASS => 'StableIdentifier');
		$most_recent_stable_identifier->inflated(1);
			
		$self->create_identifier_from_numerical_stub($most_recent_stable_identifier, 0); # The very first stable ID
			
		$state->mostRecentStableIdentifier($most_recent_stable_identifier);
				
		$dba->store($most_recent_stable_identifier);
		$dba->store($state);
	}
		
	# Pull mostRecentStableIdentifier from State instance, increment its
	# stable ID, insert a new mostRecentStableIdentifier containing the
	# incrementedstable ID.
	my $numerical_stub = (-1); # This value should never be used
	if (!(defined $most_recent_stable_identifier)) {
		if (defined $state->mostRecentStableIdentifier && scalar(@{$state->mostRecentStableIdentifier})>0) {
			$most_recent_stable_identifier = $state->mostRecentStableIdentifier->[0];
		}
	}
	if (!(defined $most_recent_stable_identifier)) {
		print STDERR "StableIdentifiers.increment_most_recent_stable_identifier: stableIdentifierInstanceis null!!\n";
		return 0;
	} else {
		if (defined $most_recent_stable_identifier->numericalStub && scalar(@{$most_recent_stable_identifier->numericalStub})>0) {
			$numerical_stub = $most_recent_stable_identifier->numericalStub->[0];
			$numerical_stub++;
		} else {
			print STDERR "StableIdentifiers.increment_most_recent_stable_identifier: numericalStub is null!!\n";
			return 0;
		}
	}
			
	# Create a new StableIdentifier instance containing the
	# updated stableIdentifier and stash it in the database.
	$most_recent_stable_identifier = GKB::Instance->new(-ONTOLOGY => $dba->ontology, -CLASS => 'StableIdentifier');
	$most_recent_stable_identifier->inflated(1);
	$self->create_identifier_from_numerical_stub($most_recent_stable_identifier, $numerical_stub);
	my @time_data = localtime(time());
	$most_recent_stable_identifier->dateTime(GKB::Utils::dateTime_to_string([$time_data[5], $time_data[4], $time_data[3], $time_data[2], $time_data[1], $time_data[0]]));
	$dba->store($most_recent_stable_identifier);
	print STDERR "IGNORE WARNING: \"StableIdentifier w/o identifier....\", this is erroneously produced by NamedInstance->set_displayName\n\n";
	
	# Overwrite the existing StableIdentifier instance with
	# the new one in the "most recent" instance.
	$state->mostRecentStableIdentifier($most_recent_stable_identifier);

	# Save the change to the DB
	
	print STDERR "Updating state\n";
	
	$dba->update_attribute($state, "mostRecentStableIdentifier");
		
	return 1;
}
	
# Gets the State instance (of which there
# should be eith 0 or 1 occurrence in the database).
sub get_state {
	my ($self) = @_;

	my $state = undef;
		
	my $dba = $self->get_identifier_database_dba();
	if (!(defined $dba)) {
		return $state;
	}
		
	my $states = $dba->fetch_instance(-CLASS => "State");
	if (scalar(@{$states})>0) {
		if (scalar(@{$states})>1) {
			print STDERR "IdentifierDatabase.getState: WARNING - more than one State object found in database - using first one!\n";
		}
		$state = $states->[0];
	}
		
	return $state;
}
	
# Does a getState, and then extracts the StableIdentifier
# instance from the "mostRecentStableIdentifier" slot and returns it. 
sub get_most_recent_stable_identifier_from_state {
	my ($self) = @_;

	my $most_recent_stable_identifier = undef;
	my $state = $self->get_state();
	if (defined $state) {
		$most_recent_stable_identifier = $state->mostRecentStableIdentifier->[0];
	}
		
	return $most_recent_stable_identifier;
		
}
	
# Given the numerical stub for a stable identifier, create an Identifier
# instance containing this stub and the string version of the stable
# identifier.
sub create_identifier_from_numerical_stub {
	my ($self, $stable_identifier, $numerical_stub) = @_;

	$self->create_identifier($stable_identifier, $numerical_stub, "REACT_$numerical_stub");
}
	
sub create_identifier {
	my ($self, $stable_identifier, $numerical_stub, $identifier_string) = @_;

	$stable_identifier->numericalStub($numerical_stub);
	$stable_identifier->identifierString($identifier_string);
}

1;

