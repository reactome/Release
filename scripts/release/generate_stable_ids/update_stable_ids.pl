#!/usr/local/bin/perl  -w
use strict;
use common::sense;
use autodie;
use Cwd;
use Getopt::Long;
use Data::Dumper;
#use Log::Log4perl qw/get_logger/;

use lib '/usr/local/gkb/modules';
use GKB::DBAdaptor;


# A few bare SQL queries
use constant DB_IDS => 'SELECT DB_ID FROM DatabaseObject WHERE _class = ?';
use constant MAX_ID => 'SELECT MAX(DB_ID) FROM DatabaseObject';
use constant ST_ID  => 'SELECT DB_ID FROM StableIdentifier WHERE identifier = ?';
use constant ALL_ST => 'SELECT DB_ID FROM StableIdentifier';

#Log::Log4perl->init(\$LOG_CONF);
#my $logger = get_logger(__PACKAGE__);

our($pass,$user,$release_db,$prev_release_db,$gk_central,$ghost,%attached,$release_num);

my $usage = "Usage:\n\t" . join("\n\t", 
				"$0 -sdb slice_db_name -gdb gk_central_db_name -pdb prev_release_db_name \\",
				"-ghost gk_central_db_host  -user db_user -pass db_pass");

GetOptions(
    "user:s"  => \$user,
    "pass:s"  => \$pass,
    "gdb:s"   => \$gk_central,
    "ghost:s" => \$ghost,
    "sdb:s"   => \$release_db,
    "pdb:s"   => \$prev_release_db,
    "release:i" => \$release_num
    );

($release_db && $prev_release_db && $gk_central && $ghost && $user && $pass && $release_num) || die "$usage\n";


# Make sure our requested DBs are slice DBs
check_db_names();

my %st_id_classes = map {$_ => 1} classes_with_stable_ids();

# DB adaptors
my %dba = get_api_connections(); 

# Get list of all instances that have or need ST_IDs
my @db_ids = get_db_ids($release_db);

# Evaluate each instance
for my $db_id (@db_ids) {
    my $instance   = get_instance($db_id, $release_db);
    my $class      = $instance->class;
    my $name       = $instance->displayName;
    my $stable_id  = stable_id($instance);

    if ($stable_id->displayName =~ /^REACT/) {
	say "Reassigning " . $stable_id->displayName . " to " . identifier($instance);
	archive_old_identifier($stable_id);
    }

#    if (should_be_incremented($instance)) {
#	increment_stable_id($stable_id);
#    }

    $attached{$stable_id->db_id()} = 1;
}

remove_orphan_stable_ids();


# If stable ID exists, return instance.  If not, 
# create and store new ST_ID instance and return that.
sub stable_id {
    my $instance = shift;
    my $identifier = identifier($instance);

    my $st_id = fetch_stable_id($instance,$identifier);

    unless ( $st_id ) {
	my $version = 1;
	$st_id = create_stable_id($instance,$identifier,$version);
    }

    $attached{$st_id->db_id} = 1;
    return $st_id;
}

sub archive_old_identifier {
    my $instance = shift;
    $instance->inflate();
    my $identifier = identifier($instance);
    my $old_id = $instance->attribute_value('identifier')->[0];
    my $old_version =  $instance->attribute_value('identifierVersion')->[0];

    $instance->attribute_value('identifier',$identifier);
    $instance->attribute_value('identifierVersion',1);
    $instance->displayName("$identifier.1");
    $instance->attribute_value('oldIdentifier',$old_id);
    $instance->attribute_value('oldIdentifierVersion',$old_version);

    $dba{$gk_central}->update($instance);
    $dba{$release_db}->update($instance);

    log_creation($instance);
    add_stable_id_to_history($instance);
}

sub should_be_incremented {
    my $instance = shift;
    my $db_id = $instance->db_id();
    my $prev_instance = get_instance($db_id,$prev_release_db) || return 0;
    my $mods2 = @{$instance->attribute_value('modified')} || 0;
    my $mods1 = @{$prev_instance->attribute_value('modified')} || 0;
    if ($mods1 == $mods2) {
	return 0;
    }
    elsif ($mods2 > $mods1) {
	return 1;
    }
}

sub check_db_names {
    unless ($prev_release_db =~ /slice/ && $release_db =~ /slice/) {
        die "Both of these databases ($release_db and $prev_release_db) should be slice databases";
    }
}

sub get_api_connections {
    my $r_dba = GKB::DBAdaptor->new(
	-dbname  => $release_db,
	-user    => $user,
	-pass    => $pass
	);

    my $p_dba = GKB::DBAdaptor->new(
	-dbname  => $prev_release_db,
	-user    => $user,
	-pass    => $pass
	);

    my $g_dba = GKB::DBAdaptor->new(
        -dbname  => $gk_central,
	-host    => $ghost,
        -user    => $user,
        -pass    => $pass
	);

    my $s_dbh = DBI->connect(
	"dbi:mysql:stable_identifiers",
	$user,
	$pass
	);

    return ( $release_db      => $r_dba,
	     $prev_release_db => $p_dba,
	     $gk_central      => $g_dba,
	     'history'        => $s_dbh
	);
}

sub get_db_ids {
    my $sth = $dba{$release_db}->prepare(DB_IDS);
    my @db_ids;
    for my $class (classes_with_stable_ids()) {
	$sth->execute($class);
	while (my $db_id = $sth->fetchrow_array) {
	    push @db_ids, $db_id;
	} 
    }
    return @db_ids;
}


sub increment_stable_id {
    my $instance = shift;
    $instance->inflate();

    my $identifier =  $instance->attribute_value('identifier')->[0];
    my $version  = $instance->attribute_value('identifierVersion')->[0];
    my $new_version = $version + 1;

    say "Incrementing ".$instance->displayName." from $version to $new_version";

    $instance->attribute_value('identifierVersion',$new_version);
    $instance->displayName("$identifier.$new_version");
    
    $dba{$gk_central}->update($instance);
    $dba{$release_db}->update($instance);

    my $sth = $dba{history}->prepare("UPDATE StableIdentifier SET identifierVersion = $version");
    $sth->execute();
    log_incrementation($instance);
}

# If the stable ID is not attached to an event, put it in the attic
sub remove_orphan_stable_ids {
    my $sth = $dba{$gk_central}->prepare(ALL_ST);
    $sth->execute();
    while (my $res = $sth->fetchrow_arrayref) {
	my $db_id = $res->[0] || next;
	next if $attached{$db_id};

	my $deleted;
	for my $db ($release_db,$gk_central) {
	    my $st_id =  get_instance($db_id,$db) || next;
	    $st_id->inflate();

	    my $history_id = add_stable_id_to_history($st_id);
	    $history_id || $self->throw("Problem getting/setting history for " . $st_id->displayName());

	    log_deletion($st_id) unless $deleted;

	    say "Deleting orphan stable identifier ".$st_id->displayName unless $deleted;

	    $dba{$db}->delete($st_id);
	    $deleted++;
	}
    }
}

#####################################################################
##  This set of functions deal with the stable identifier history db
sub stable_id_has_history {
    my $instance = shift;
    my $dbh = $dba{history};
    my $sth = $dbh->prepare("SELECT DB_ID FROM StableIdentifier WHERE identifier = ?");
    $sth->execute();
    my $ary = $sth->fetchrow_arrayref || [];
    return $ary->[0];
}

sub add_stable_id_to_history {
    my $instance = shift;

    my $history_id = stable_identifier_has_history($instance);
    if ($history_id) {
	return $history_id;
    }

    my $dbh = $dba{history};
    my $identifier = $instance->attribute_value('identifier')->[0];
    my $version = $instance->attribute_value('identifierVersion')->[0];
    my $oidentifier = $instance->attribute_value('oldIdentifier')->[0] || 'NULL';
    my $oversion = $instance->attribute_value('oldIdentifierVersion')->[0] || 'NULL';

    my $sth = $dbh->prepare('INSERT INTO StableIdentifier VALUES (?, ?, ?, ?)');
    $sth->execute($identifier,$version,$oidentifier,$oversion);

    $sth = $dbh->prepare("SELECT DB_ID FROM StableIdentifier WHERE identifier = ?");
    $sth->execute($identifier);
    return $sth->fetchrow_arrayref->[0];
}

sub log_deletion {
    add_change_to_history(@_,'deleted');
}

sub log_creation {
    add_change_to_history(@_,'created');
}

sub log_incrementation {
    add_change_to_history(@_,'incremented');
}

sub add_change_to_history {
    my ($instance,$change) = @_;
    my $st_db_id = stable_id_has_history($instance);
    my $dbh = $dba{history};
    my $sth = $dbh->prepare('INSERT INTO Changed values (NULL, ?, ?, ?, NOW())');
    $sth->execute($st_db_id,$change,$release_num);
}
##
##################################################################### 

sub get_instance {
    my $db_id = int shift || die "DB_ID must always be an integer";
    my $db    = shift;
    
    my $instance = $dba{$db}->fetch_instance_by_db_id($db_id)->[0];
    return $instance;
}

sub get_species {
    my $instance = shift;
    my $sp = $instance->attribute_value('species')->[0]  || return undef;
    return $sp->displayName;
}

sub classes_with_stable_ids {
    # derived from:
    # select distinct _class from DatabaseObject where StableIdentifier is not null 
    qw/
    Pathway SimpleEntity OtherEntity DefinedSet Complex EntityWithAccessionedSequence GenomeEncodedEntity
    Reaction BlackBoxEvent PositiveRegulation CandidateSet NegativeRegulation OpenSet Requirement Polymer
    Depolymerisation EntitySet Polymerisation FailedReaction
    /;
}

sub update_st_id {
    my ($identifier,$version) = @_;
    
}


# Add the necessary attributes to our stable ID instance 
sub set_st_id_attributes {
    my ($instance,$identifier,$version) = @_;
    $instance->attribute_value('identifier',$identifier);
    $instance->attribute_value('identifierVersion',$version);
    $instance->attribute_value('_class','StableIdentifier');
    $instance->attribute_value('_displayName',"$identifier.$version");
}

sub identifier {
    my $instance = shift;
    my $spc = make_decision_on_species($instance);
    my $species = abbreviate($spc);
    return join('-','R',$species,$instance->db_id());
}

sub abbreviate {
    local $_ = shift;
    return $_ if /ALL/;
    my $short_name = uc(join('', /([A-Za-z])[a-z]+\s+([a-z]{2})/));
    unless ($short_name) {
	warn "Species $_ is not in binomial form!\n";
    }
    return $short_name;
}

# Make a new ST_ID instance from scratch
sub create_stable_id {
    my ($instance,$identifier,$version,$db_id) = @_;
    $instance->inflate;

    $db_id ||= new_db_id($gk_central);
    my $st_id = $dba{$gk_central}->instance_from_hash({},'StableIdentifier',$db_id);
    set_st_id_attributes($st_id,$identifier,$version);

    say STDERR "creating new ST_ID " . $st_id->displayName . " for " . $instance->displayName;

    $dba{$gk_central}->store($st_id,1);
    $dba{$release_db}->store($st_id,1);
    add_stable_id_to_history($st_id);
    log_creation($st_id);
    
    $instance->stableIdentifier($st_id);

    return $st_id;
}

sub fetch_stable_id {
    my ($instance,$identifier) = @_;

    # first, see if the instance has a stable ID
    my $st_id = $instance->attribute_value('stableIdentifier')->[0];

    # second, see if there is an old ST_ID instance
    unless( $st_id ) {
	my $sth = $dba{history}->prepare(ST_ID);
	$sth->execute($identifier);
	my $res = $sth->fetchrow_arrayref || [];
	my $db_id = $res->[0];
	if ($db_id) {
	    $st_id = get_instance($db_id,$release_db);
	}
    }

    return $st_id;
}

sub max_db_id {
    my $db = shift;
    my $sth = $dba{$db}->prepare(MAX_ID);
    $sth->execute;
    my $max_db_id = $sth->fetchrow_arrayref->[0];
    return $max_db_id;
}

# Get the largest DB_ID from slice or gk_central
sub new_db_id {
    my $max_id = 0;
    for my $db ($gk_central,$release_db) {
	my $id = max_db_id($db);
	$max_id = $id if $id > $max_id;
    }
    return $max_id + 1;
}

# Hopefully not too compicated reasoner to deal with entities that lack a species
sub make_decision_on_species {
    my $instance = shift;
    my $species = get_species($instance) || 'ALL';
}
