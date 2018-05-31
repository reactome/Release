#!/usr/local/bin/perl  -w
use strict;
use common::sense;
use autodie;
use Cwd;
use Getopt::Long;
use Data::Dumper;
use Log::Log4perl qw/get_logger/;

use lib '/usr/local/gkb/modules';
use GKB::DBAdaptor;
use GKB::Config;

$| = 1;

# A few bare SQL queries
use constant DB_ID       => 'SELECT DB_ID FROM DatabaseObject WHERE stableIdentifier IS NOT NULL';
use constant ORTHO       => 'SELECT s.identifier FROM History h, StableIdentifier s WHERE h.class = "ortho" AND s.DB_ID = h.ST_ID';
use constant ST_ID       => 'SELECT * FROM StableIdentifier WHERE identifier = ?';
use constant ALL_ST      => 'SELECT * FROM StableIdentifier';
use constant LOG_CHANGE  => 'INSERT INTO History VALUES (NULL,?,?,?,?,NOW())';
use constant SET_PARENT  => 'UPDATE StableIdentifier SET instanceId = ? WHERE DB_ID = ?';
use constant SET_VERSION => 'UPDATE StableIdentifier SET identifierVersion = ? WHERE DB_ID = ?';
use constant GET_VERSION => 'SELECT identifierVersion FROM StableIdentifier WHERE DB_ID = ?';
use constant CREATE      => 'INSERT INTO StableIdentifier VALUES (NULL, ?, ?, ?)';

use constant DEBUG => 0;

Log::Log4perl->init( \$LOG_CONF );
my $logger = get_logger(__PACKAGE__);

our ( $pass, $user, $db, $stable_id_db, $db_host, $release, %parent, %logged, %ortho );

my $usage = "Usage:\n\t$0 -db test_reactome_xx -sdb stable_id_db_name -user db_user user -pass db_pass -release XX";

GetOptions(
	"user:s"    => \$user,
	"pass:s"    => \$pass,
	"db:s"      => \$db,
	"host:s"    => \$db_host,
	"sdb:s"     => \$stable_id_db,
	"release:i" => \$release
);

$pass && $user && $db && $stable_id_db && $release || die $usage;

# DB adaptors
my %dba = get_api_connections();

# load all stable id history into memory
# Here's how %stable_id will work:
# There are 2 types of keys and one subcache.
# The two types of keys:
# - StableIdentifier strings: You will find things with keys like "REACT_1234" or "R-HSA-5678". These keys point to Stable Identifier objects.
# - Instance Identifiers: You will find things keyed by their Database Object DB_IDs, such as "948573" or "2200459". These keys point to arrays of DB_IDs that are referred
#   to by that instanceId.
# The subcache:
# - there will be a single key named "new". This key points to another hash that is keyed by Stable Identifier Strings,
#   and these keys point to an integer which indicates how many times that Stable Identifier has been seen.
# TODO: Re-write this all into three separate structures.
my %stable_id;
get_all_stable_ids( \%stable_id );

# Get list of all instances that have or need ST_IDs
my @db_ids = get_db_ids($db);
$logger->info( "Total of " . scalar @db_ids . " instances" );

my $identifier_counter;
my $then = time();
foreach my $db_id (@db_ids) {
	my $instance = get_instance( $db_id, $db );

	my $stable_id_instance = $instance->stableIdentifier->[0];
	if ( !$stable_id_instance ) {

		# The code in here should probably never execute. But it's been here a while and I don't want to break anything and there's no harm in leaving it.
		$logger->warn("No stable ID for $db_id");
		next;
	}

	my $identifier = $stable_id_instance->identifier->[0];
	if ( !$identifier ) {

		# The code in here should probably never execute. But it's been here a while and I don't want to break anything and there's no harm in leaving it.
		$logger->warn( "No identifier for stable id instance " . $stable_id_instance->db_id );
		next;
	}

	# map $identifier to $db_id in the %parent hash.
	# This hash is used in add_stable_id_to_history to set the instanceID in StableIdentifier
	$parent{$identifier} = $db_id;

	# This condition is just for logging. Doesn't really affect execution of main algorithm.
	if ( ( ++$identifier_counter % 1_000 ) == 0 ) {
		my $now = sprintf '%.1f', ( time() - $then ) / 60;
		$logger->info( "$identifier ($identifier_counter of " . scalar @db_ids . ") $now minutes elapsed" );
	}

	$logger->info( join( "\t", "STABLE_ID", $db_id, $instance->class, $stable_id_instance->displayName ) . "\n" );

	# retrieve or create history DB entry for ST_ID
	has_history($stable_id_instance);

	# Check for ortho-inferred Stable Identifiers. You can tell because they (stable identifiers) don't have the numeric db_id portions.
	my $is_ortho = $identifier !~ /-$db_id$/;
	if ($is_ortho) {
		ortho_parent_in_history( $stable_id_instance, $db_id );

		# Go straight to the next iteration of the loop because orthoinferred stuff doesn't get incremented.
		next;
	}

	if ( is_incremented($stable_id_instance) ) {
		increment_in_history($stable_id_instance);
		next;
	}

	log_exists($stable_id_instance);
}

sub get_api_connections {
	my $r_dba = GKB::DBAdaptor->new(
		-dbname => $db,
		-user   => $user,
		-host 	=> $db_host,
		-pass   => $pass
	);

	my $s_dbh = DBI->connect( "dbi:mysql:$stable_id_db;host=$db_host", $user, $pass );

	return (
		$db           => $r_dba,
		$stable_id_db => $s_dbh
	);
}

# Get the DB_IDs of ALL DatabaseObjects that HAVE a Stable Identifier.
sub get_db_ids {
	my $sth = $dba{$db}->prepare(DB_ID);
	$sth->execute;
	while ( my $db_id = $sth->fetchrow_array ) {
		push @db_ids, $db_id;
	}
	return @db_ids;
}

#Populate main cache of stable identifiers (%stable_id)
sub get_all_stable_ids {
	my $stable_id = shift;

	my $sth = $dba{$stable_id_db}->prepare(ALL_ST);
	$sth->execute;
	while ( my $ary = $sth->fetchrow_arrayref ) {
		my ( $db_id, $identifier, $version, $parent ) = @$ary;
		$stable_id->{$identifier}{db_id}   = $db_id;
		$stable_id->{$identifier}{version} = $version;
		$stable_id->{$identifier}{parent}  = $parent;
		# Keep track in an array of the DB_IDs that this instanceId are referred to by. 
		# the fact that multiple DB_IDs exist for a single instanceId is because
		# multiple stableIDs exist because of that time they converted from the OLD
		# stable identifier system to the new one. Also, the old system had 
		# stable identifier replacements done to it, resulting in this sort of situation.
		push @{ $stable_id->{$parent} }, $db_id;
	}
}

# This will return a SELECT-statement handle
sub handle {
	return $dba{$stable_id_db}->prepare(ST_ID);
}

# This function populates the global hash %stable_id with a Stable Identifier, as specified by the parameter (the actual stable identifier string).
# also, it returns the DB_ID for that StableIdentifier.
sub add_stable_id_to_memory {
	my $identifier = shift;

	# Get the Stable Identifier from the database.
	my $sth = handle();
	$sth->execute($identifier);

	# Really should only execute once, but hey, the library will always return an array...
	while ( my $ary = $sth->fetchrow_arrayref ) {

		# NOTE: the 4th column which is mapped to $parent is ACTUALLY named "instanceId" in stable_identifiers.StableIdentifier (<database>.<table>)
		# It represents the Instance that this stable identifier identifies.
		my ( $db_id, $identifier, $version, $parent ) = @$ary;
		$stable_id{$identifier}{db_id}   = $db_id;
		$stable_id{$identifier}{version} = $version;
		$stable_id{$identifier}{parent}  = $parent;
		$stable_id{$identifier}{created} = 1;
		last;
	}
	return $stable_id{$identifier}{db_id};
}

# Warning: The function name sounds like it will return a boolean
# but ACTUALLY... it will return the db_id of the identifier if it's already in the main map.
# OR it will create a NEW entry in stable_id_history and add the parameter to the history as the first
# entry for that instance.
sub has_history {
	my $instance = shift;

	my $identifier = $instance->attribute_value('identifier')->[0];

	if ( $stable_id{$identifier} ) {
		return $stable_id{$identifier}{db_id};
	}
	else {
		return add_stable_id_to_history($instance);
	}
}

sub is_already_ortho {
	my $identifier = shift;

	unless ( keys %ortho ) {
		$logger->info("getting list of ortho ST_IDs");
		my $sth = $dba{$stable_id_db}->prepare_cached(ORTHO);
		$sth->execute();
		while ( $sth->fetchrow_arrayref ) {
			$ortho{ $_->[0] }++;
		}
	}

	my $is_ortho = $ortho{$identifier};
	return $is_ortho;
}

sub add_stable_id_to_history {
	my $st_id = shift;

	my $identifier = $st_id->attribute_value('identifier')->[0];

	# Not sure this condition will ever pass because the queries at the top should bever get such entities...?
	unless ($identifier) {
		$logger->warn( "Stable ID ", $st_id->db_id, " has no identifier." );
		return;
	}

	# Get the most recent version number for the stable identifier
	my $version = $st_id->attribute_value('identifierVersion')->[0];

	# NOTE: the DML statement reference by CREATE inserts into StableIdentifier, NOT into History, which is what the name of this function would suggest...
	# This statement should create a parent-child relationship between the old Stable Identifier and the new one.
	$dba{$stable_id_db}->prepare(CREATE)->execute( $identifier, $version, $parent{$identifier} );

	# Now, let's track the new identifier in the internal-to-stable_id "New things" cache.
	# TODO: Refactor this to a separate top-level hash.
	$stable_id{new}{$identifier}++;

	# Add the new thing to the global hash which should return the db_id
	my $db_id = add_stable_id_to_memory($identifier);
	# If there IS a DB_ID then things were OK and it logs it.
	if ($db_id) {
		log_creation($st_id);
		return $db_id;
	}

	#Otherwise, DIE! Things went badly, need help.
	else {
		die "Failure creating new stable ID";
	}
}

sub log_exists {
	return undef if $logged{ $_[0]->identifier->[0] };
	add_change_to_history( $_[0], 'exists' );
}

# This isn't just for logging, it calls "add_change_to_history" which updates History.
sub log_ortho {
	my $identifier = $_[0]->identifier->[0];
	unless ( is_already_ortho($identifier) ) {
		add_change_to_history( $_[0], 'ortho' );
	}
}

sub log_creation {
	add_change_to_history( $_[0], 'created' );
}

sub log_incrementation {
	add_change_to_history( $_[0], 'incremented' );
}

# This is the *only* place that data will be inserted in to the History table.
sub add_change_to_history {
	my ( $st_id, $change ) = @_;

	$logger->info( "Logging $change event for " . $st_id->displayName . " in history database\n" );
	my $st_db_id = has_history($st_id);
	unless ($st_db_id) {
		$dba{$db}->throw( "No history found for " . $st_id->displayName );
	}

	my $sth = $dba{$stable_id_db}->prepare_cached(LOG_CHANGE);
	$sth->execute( $st_db_id, $st_id->displayName, $change, $release );
	$logged{ $st_id->identifier->[0] }++;

	return $st_db_id;
}

# Checks to see if a stable ID is the base version (version 1), or an incremented version.
sub is_incremented {
	my $st_id      = shift;
	my $identifier = $st_id->identifier->[0];
	my $id_version = $st_id->identifierVersion->[0];

	# return 0 - this is NOT an incremented version
	return 0 if $id_version == 1;

	# Get the Version of the corresponding stable identifier in the global hash.
	my $version = $stable_id{$identifier}{version};

	# If the version of the input stable identifier is smaller than the corresponding stable identifier's
	# version in the global stable_id hash, then it IS incremented!
	if ( $version < $id_version ) {
		return 1;
	}

	# Got here? Then it is NOT an incremented version.
	return 0;
}

sub increment_in_history {
	my $st_id      = shift;
	my $identifier = $st_id->identifier->[0];
	my $version    = $st_id->identifierVersion->[0];
	$stable_id{$identifier}{version} = $version;
	$stable_id{$identifier}{updated}{version}++;
	my $sth = $dba{$stable_id_db}->prepare_cached(SET_VERSION);
	$sth->execute( $version, $stable_id{$identifier}{db_id} );
	log_incrementation($st_id);
}

# set/change the parent instance ID for orth-inferred instance
# make this retroactive for historical ST_IDs.
sub ortho_parent_in_history {
	my $st_id = shift;
	my $db_id = shift;

	my $identifier = $st_id->identifier->[0];

	# this is already done for new stable IDs
	if ( $stable_id{new}{$identifier} ) {
		return 1;
	}

	my $old_parent_id         = $stable_id{$identifier}{parent};
	# Here, we use the instanceId fron the stableIdentifier record
	# to get an array of DatabaseObject IDs (DB_IDs) which are referred to
	# by the current instanceId.
	my @st_id_with_old_parent = @{ $stable_id{$old_parent_id} };

	# For each of the DB_IDs that the instanceId referred to,
	# Do an update:
	# set the instanceId to $db_id where DB_ID = $old_st_id
	# in other words: we are updating the stableIdentifier table
	# by changing the instance to which a StableIdentifier refers to.
	my $sth = $dba{$stable_id_db}->prepare_cached(SET_PARENT);
	for my $old_st_id (@st_id_with_old_parent) {
		$logger->info("Updating parent instanceId from $old_parent_id to $db_id");
		$sth->execute( $db_id, $old_st_id );
	}
	log_ortho($st_id);
}

sub get_instance {
	my $db_id    = shift;
	my $db       = shift;
	my $dbh      = $dba{$db};
	my $instance = $dba{$db}->fetch_instance_by_db_id($db_id)->[0];
	return $instance;
}
