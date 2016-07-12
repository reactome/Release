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
use constant DB_ID  => 'SELECT DB_ID FROM DatabaseObject WHERE stableIdentifier IS NOT NULL';
use constant ORTHO  => 'SELECT s.identifier FROM History h, StableIdentifier s WHERE h.class = "ortho" AND s.DB_ID = h.ST_ID';
use constant ST_ID  => 'SELECT * FROM StableIdentifier WHERE identifier = ?';
use constant ALL_ST => 'SELECT * FROM StableIdentifier';
use constant LOG_CHANGE  => 'INSERT INTO History VALUES (NULL,?,?,?,?,NOW())';
use constant SET_PARENT  => 'UPDATE StableIdentifier SET instanceId = ? WHERE DB_ID = ?';
use constant SET_VERSION => 'UPDATE StableIdentifier SET identifierVersion = ? WHERE DB_ID = ?';
use constant GET_VERSION => 'SELECT identifierVersion FROM StableIdentifier WHERE DB_ID = ?';
use constant CREATE => 'INSERT INTO StableIdentifier VALUES (NULL, ?, ?, ?)';

use constant DEBUG => 0;

Log::Log4perl->init(\$LOG_CONF);
my $logger = get_logger(__PACKAGE__);

our($pass,$user,$db,$stable_id_db,$release,%parent,%logged,%ortho);

my $usage = "Usage:\n\t$0 -db test_reactome_xx -sdb stable_id_db_name -user db_user user -pass db_pass -release XX";

GetOptions(
    "user:s"  => \$user,
    "pass:s"  => \$pass,
    "db:s"    => \$db,
    "sdb:s"   => \$stable_id_db,
    "release:i" => \$release
);

$pass && $user && $db && $stable_id_db && $release || die $usage;

# DB adaptors
my %dba = get_api_connections();


# load all stable id history into memory
my %stable_id;
get_all_stable_ids(\%stable_id);

# Get list of all instances that have or need ST_IDs
my @db_ids = get_db_ids($db);
$logger->info("Total of " . scalar @db_ids . " instances");

my $identifier_counter;
my $then = time();
foreach my $db_id (@db_ids) {
    my $instance = get_instance($db_id, $db);
    
    my $stable_id_instance = $instance->stableIdentifier->[0];
    if (!$stable_id_instance) {
        $logger->warn("No stable ID for $db_id");
        next;
    }

    my $identifier = $stable_id_instance->identifier->[0];
    if (!$identifier) {
        $logger->warn("No identifier for stable id instance " . $stable_id_instance->db_id);
        next;
    }

    $parent{$identifier} = $db_id;

    if ((++$identifier_counter % 1_000) == 0) {
        my $now = sprintf '%.1f', (time() - $then)/60;
        $logger->info("$identifier ($identifier_counter of " . scalar @db_ids . ") $now minutes elapsed");
    }

    $logger->info(join("\t","STABLE_ID",$db_id,$instance->class,$stable_id_instance->displayName)."\n");

    # retrieve or create history DB entry for ST_ID
    has_history($stable_id_instance);

    my $is_ortho = $identifier !~ /-$db_id$/;
    if ($is_ortho) {
        ortho_parent_in_history($stable_id_instance,$db_id);
        next;
    }

    if (is_incremented($stable_id_instance)) {
        increment_in_history($stable_id_instance);
        next;
    }

    log_exists($stable_id_instance);
}


sub get_api_connections {
    my $r_dba = GKB::DBAdaptor->new(
        -dbname  => $db,
        -user    => $user,
        -pass    => $pass
        );

    my $s_dbh = DBI->connect(
        "dbi:mysql:$stable_id_db",
        $user,
        $pass
        );

    return ( $db      => $r_dba,
             $stable_id_db => $s_dbh
        );
}

sub get_db_ids {
    my $sth = $dba{$db}->prepare(DB_ID);
    $sth->execute;
    while (my $db_id = $sth->fetchrow_array) {
        push @db_ids, $db_id;
    }
    return @db_ids;
}


sub get_all_stable_ids {
    my $stable_id = shift;
    
    my $sth = $dba{$stable_id_db}->prepare(ALL_ST);
    $sth->execute;
    while (my $ary = $sth->fetchrow_arrayref) {
        my ($db_id,$identifier,$version,$parent) = @$ary;
        $stable_id->{$identifier}{db_id}   = $db_id;
        $stable_id->{$identifier}{version} = $version;
        $stable_id->{$identifier}{parent}  = $parent;
        push @{$stable_id->{$parent}}, $db_id;
    }
}

sub handle {
    return $dba{$stable_id_db}->prepare(ST_ID);
}

sub add_stable_id_to_memory {
    my $identifier = shift;

    my $sth = handle();
    $sth->execute($identifier);
    while (my $ary = $sth->fetchrow_arrayref) {
        my ($db_id,$identifier,$version,$parent) = @$ary;
        $stable_id{$identifier}{db_id}   = $db_id;
        $stable_id{$identifier}{version} = $version;
        $stable_id{$identifier}{parent}  = $parent;
        $stable_id{$identifier}{created} = 1;
        last;
    }
    return $stable_id{$identifier}{db_id};
}

sub has_history {
    my $instance = shift;

    my $identifier = $instance->attribute_value('identifier')->[0];

    if ($stable_id{$identifier}) {
        return $stable_id{$identifier}{db_id};
    } else {
        return add_stable_id_to_history($instance);
    }
}

sub is_already_ortho {
    my $identifier = shift;

    unless (keys %ortho) {
        $logger->info("getting list of ortho ST_IDs");
        my $sth = $dba{$stable_id_db}->prepare_cached(ORTHO);
        $sth->execute();
        while ($sth->fetchrow_arrayref) {
            $ortho{$_->[0]}++;
        }
    }

    my $is_ortho = $ortho{$identifier};
    return $is_ortho;
}

sub add_stable_id_to_history {
    my $st_id  = shift;
    
    my $identifier = $st_id->attribute_value('identifier')->[0];
    unless ($identifier) {
        $logger->warn("Stable ID ",$st_id->db_id, " has no identifier.");
        return;
    }

    my $version = $st_id->attribute_value('identifierVersion')->[0];
    $dba{$stable_id_db}->prepare(CREATE)->execute(
        $identifier,
        $version,
        $parent{$identifier}
    );
    $stable_id{new}{$identifier}++;

    my $db_id = add_stable_id_to_memory($identifier);
    if ($db_id) {
        log_creation($st_id);
        return $db_id;
    } else {
        die "Failure creating new stable ID";
    }
}

sub log_exists {
    return undef if $logged{$_[0]->identifier->[0]};
    add_change_to_history($_[0],'exists');
}

sub log_ortho {
    my $identifier = $_[0]->identifier->[0];
    unless (is_already_ortho($identifier)) {
        add_change_to_history($_[0],'ortho');
    }
}

sub log_creation {
    add_change_to_history($_[0],'created');
}

sub log_incrementation {
    add_change_to_history($_[0],'incremented');
}

sub add_change_to_history {
    my ($st_id,$change) = @_;

    $logger->info("Logging $change event for " . $st_id->displayName . " in history database\n");
    my $st_db_id = has_history($st_id);
    unless ($st_db_id) {
        $dba{$db}->throw("No history found for ".$st_id->displayName);
    }

    my $sth = $dba{$stable_id_db}->prepare_cached(LOG_CHANGE);
    $sth->execute(
        $st_db_id,
        $st_id->displayName,
        $change,
        $release
    );
    $logged{$st_id->identifier->[0]}++;

    return $st_db_id;
}

sub is_incremented {
    my $st_id = shift;
    my $identifier = $st_id->identifier->[0];
    my $id_version = $st_id->identifierVersion->[0];
    return 0 if $id_version == 1;

    my $version = $stable_id{$identifier}{version};
    if ($version < $id_version) {
        return 1;
    }

    return 0;
}

sub increment_in_history {
    my $st_id = shift;
    my $identifier = $st_id->identifier->[0];
    my $version  = $st_id->identifierVersion->[0];
    $stable_id{$identifier}{version} = $version;
    $stable_id{$identifier}{updated}{version}++;
    my $sth = $dba{$stable_id_db}->prepare_cached(SET_VERSION);
    $sth->execute($version,$stable_id{$identifier}{db_id});
    log_incrementation($st_id);
}

# set/change the parent instance ID for orth-inferred instance
# make this retroactive for historical ST_IDs.
sub ortho_parent_in_history {
    my $st_id = shift;
    my $db_id = shift;

    my $identifier = $st_id->identifier->[0];

    # this is already done for new stable IDs
    if ($stable_id{new}{$identifier}) {
        return 1;
    }

    my $old_parent_id = $stable_id{$identifier}{parent};
    my @st_id_with_old_parent = @{$stable_id{$old_parent_id}};

    my $sth = $dba{$stable_id_db}->prepare_cached(SET_PARENT);
    for my $old_st_id (@st_id_with_old_parent) {
        $logger->info("Updating parent instanceId from $old_parent_id to $db_id");
        $sth->execute($db_id,$old_st_id);
    }
    log_ortho($st_id);
}

sub get_instance {
    my $db_id = shift;
    my $db    = shift;
    my $dbh = $dba{$db};
    my $instance = $dba{$db}->fetch_instance_by_db_id($db_id)->[0];
    return $instance;
}