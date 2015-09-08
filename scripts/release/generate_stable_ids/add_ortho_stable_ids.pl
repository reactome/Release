#!/usr/local/bin/perl  -w
use strict;
use common::sense;
use autodie;
use Cwd;
use Getopt::Long;
use Data::Dumper;
use Log::Log4perl qw/get_logger/;
use Data::Dumper;

use lib '/usr/local/gkb/modules';
use GKB::DBAdaptor;
use GKB::Config;

use lib '.';
use GKB::StableIdentifierDatabase;

Log::Log4perl->init(\$LOG_CONF);
my $logger = get_logger(__PACKAGE__);

our($pass,$user,$release_db,$slice_db,%seen_id,%species,$release_num);

my $usage = "Usage: $0 -user user -pass pass -db test_release_XX -sdb test_slice_XX -release_num XX\n";

GetOptions(
    "user:s"  => \$user,
    "pass:s"  => \$pass,
    "db:s"    => \$release_db,
    "sdb:s"    => \$slice_db,
    "release_num:s" => \$release_num,
    );

($release_db && $user && $pass && $release_num && $slice_db) || die $usage;

# DB adaptors
my %dba = get_api_connections(); 
my $stable = GKB::StableIdentifierDatabase->new(
    database_name => $release_db,
    release_num => $release_num);



# Get list of all curated instances that have or need ST_IDs
my @db_ids = get_db_ids($release_db);

for my $db_id (@db_ids) {
    my $prospective_parent = get_instance($db_id, $release_db) || next;

    my $stable_id = fetch_stable_id($prospective_parent) || next;
    my $stable_id_name = $stable_id->identifier->[0];
    
    my $children = paternity_test($prospective_parent,$stable_id_name);
    
    for my $instance (@$children) {
	my $identifier = $stable_id_name;
	my $species = species($instance);
	$identifier =~ s/HSA/$species/;
	say"$db_id ST_ID $identifier"; 

	$seen_id{$identifier}++;
	my $st_id = fetch_stable_id($instance);

	# paralogs?                        
	my $prime = $seen_id{$identifier} if $seen_id{$identifier} > 1;
	if ($prime) {
	    $identifier = "$identifier-$prime";
	    say("We have a paralog here $identifier");
	}

	if ($st_id) {
	    unless ($st_id->identifier->[0] eq $identifier) {
		$st_id->inflate();
		$st_id->identifier($identifier);
		$st_id->identifierVersion(1);
		$st_id->displayName("$identifier.1");
		store($st_id,'update');
		$stable->log_exists($st_id);
		say("Stable ID updated for ".$instance->db_id." (".$instance->displayName.")");
	    }
	}
	else {
	    create_stable_id($instance,$identifier,1);
	    say("New stable ID $identifier created for ".$instance->db_id." (".$instance->displayName.")");
	}
    }
}

sub paternity_test {
    my $instance = shift;
    my $stable_id = shift;

    return undef unless $stable_id =~ /R-HSA/;

    my $ortho_events = $instance->attribute_value('orthologousEvent');
    push @$ortho_events, @{$instance->attribute_value('inferredTo')};
    return undef unless $ortho_events->[0];
    return $ortho_events;
}

sub get_api_connections {

    return 
	( $release_db => GKB::DBAdaptor->new(
	  -dbname  => $release_db,
	  -user    => $user,
	  -pass    => $pass
	  ),
	  $slice_db => GKB::DBAdaptor->new(
	      -dbname  => $slice_db,
	      -user    => $user,
	      -pass    => $pass
	  )
	);
}

sub get_db_ids {
    my $sth = $dba{$release_db}->prepare('SELECT DB_ID FROM DatabaseObject WHERE _class = ?');
    my @db_ids;
    for my $class (classes_with_stable_ids()) {
	$sth->execute($class);
	while (my $db_id = $sth->fetchrow_array) {
	    push @db_ids, $db_id;
	} 
    }
    return @db_ids;
}

sub get_instance {
    my $db_id = int shift || die "DB_ID must always be an integer";
    my $db    = shift;
    my $instance = $dba{$db}->fetch_instance_by_db_id($db_id)->[0];
    return $instance;
}

sub classes_with_stable_ids {
    my $sth = $dba{$slice_db}->prepare('SELECT DISTINCT _class FROM DatabaseObject WHERE StableIdentifier IS NOT NULL');
    $sth->execute();
    my $classes = $sth->fetchall_arrayref();
    my @classes = map {@$_} @$classes;
    return @classes;
}

# Add the necessary attributes to our stable ID instance 
sub set_st_id_attributes {
    my ($instance,$identifier,$version) = @_;
    $instance->attribute_value('identifier',$identifier);
    $instance->attribute_value('identifierVersion',$version);
    $instance->attribute_value('_class','StableIdentifier');
    $instance->attribute_value('_displayName',"$identifier.$version");
}

sub species {
    my $instance = shift;
    my $name = $instance->displayName;
    my $long = eval{$instance->attribute_value('species')->[0]->displayName};
    $long or return undef;
    $species{$name} = abbreviate($long);
    return $species{$name};
}

sub abbreviate {
    local $_ = shift;
    my $short_name = uc(join('', /^([A-Za-z])[a-z]+\s+([a-z]{2})[a-z]+$/));
    return $short_name;
}

# Make a new ST_ID instance from scratch
sub create_stable_id {
    my ($instance,$identifier,$version) = @_;

    my $db_id = max_db_id();
    my $st_id = $dba{$release_db}->instance_from_hash({},'StableIdentifier',$db_id);
    set_st_id_attributes($st_id,$identifier,$version);

    say("creating new ST_ID " . $st_id->displayName . " for " . $instance->displayName);
    
    store($st_id,'store');
    
    # Attach the stable ID to its parent instance
    $instance->inflate();
    $instance->stableIdentifier($st_id);
    store($instance,'update');

    $stable->add_stable_id_to_history($st_id,$instance);
    $stable->log_ortho($st_id);
    return $st_id;
}


#########################################################################
## failure tolerant(?) wrapper for the GKInstance store and update methods
sub store {
    my $instance = shift;
    my $action   = shift;

    say("Performing $action operation for ".$instance->displayName);

    my $force = $action eq 'store' ? 1 : 0;

    my $stored = eval {$dba{$release_db}->$action($instance,$force)};
    unless ($stored) {
	warn("Oops, the $action operation failed:\n$@_\nI'll try again!");
	sleep 1;
	store($instance,$action);
    }

}
##
######################################################################### 

sub fetch_stable_id {
    my $instance = shift;
    return $instance->attribute_value('stableIdentifier')->[0];
}

sub max_db_id {
    my $db = shift;
    my $sth = $dba{$release_db}->prepare('SELECT MAX(DB_ID) FROM DatabaseObject');
    $sth->execute;
    my $max_db_id = $sth->fetchrow_arrayref->[0];
    return ++$max_db_id;
}
