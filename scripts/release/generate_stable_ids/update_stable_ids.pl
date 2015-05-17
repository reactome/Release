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


use constant DB_IDS => 'SELECT DB_ID FROM DatabaseObject WHERE _class = ?';
use constant MAX_ID => 'SELECT MAX(DB_ID) FROM DatabaseObject';

#Log::Log4perl->init(\$LOG_CONF);
#my $logger = get_logger(__PACKAGE__);

our($pass,$user,$release_db,$prev_release_db,$gk_central,$ghost);

my $usage = "Usage:\n\t" . join("\n\t", 
				"$0 -sdb slice_db_name -gdb gk_central_db_name -pdb prev_release_db_name \\",
				"-ghost gk_central_db_host  -user db_user -pass db_pass");

GetOptions(
    "user:s"  => \$user,
    "pass:s"  => \$pass,
    "gdb:s"   => \$gk_central,
    "ghost:s" => \$ghost,
    "sdb:s"   => \$release_db,
    "pdb:s"   => \$prev_release_db
    );

($release_db && $prev_release_db && $gk_central && $ghost && $user && $pass) || die "$usage\n";

# make sure our requested DBs are slice DBs
check_db_names();

my %st_id_classes = map {$_ => 1} classes_with_stable_ids();

# GK DB adaptors
my %dba = get_api_connections(); 

# Get list of all instances that will need a ST_ID
my @db_ids = get_db_ids($release_db);

# Evaluate each instance
for my $db_id (@db_ids) {
    my $instance   = get_instance($db_id, $release_db);
    say Dumper $instance->{attribute};
    my $p_instance = get_instance($db_id, $prev_release_db);

    my $species = get_species($instance) || "Homo sapiens";
    my $spc     = abbreviate($species);
    my $class   = $instance->class;
    my $name    = $instance->displayName;
#    say join("\t",$db_id,$class,$species,$spc,$name) if $species;

    # testing: create a new stable id instance 
    my $identifier = 'random' . int(1000*rand());
    my $inst = create_st_id($gk_central,$identifier,1);
    last;
}

sub abbreviate {
    local $_ = shift;
    my $short_name = uc(join('',/([A-Za-z])[a-z]+\s+([a-z]{2})/));
    unless ($short_name) {
	warn "Species $_ is not in binomial form!\n";
	return 'UNK'
    }
    return $short_name;
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

    return ( $release_db      => $r_dba,
	     $prev_release_db => $p_dba,
	     $gk_central      => $g_dba );
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

sub get_instance {
    my $db_id = int shift || die "DB_ID must always be an integer";
    my $db    = shift;
    
    my $instance = $dba{$db}->fetch_instance_by_db_id($db_id)->[0];
    return $instance;
}

sub get_species {
    my $thing = shift;
    # GKInstance object or DB_ID allowed
    my $instance = ref $thing ? $thing : get_instance($thing);
    my $species = $instance->attribute_value('species')->[0]  || return undef;
    return $species->name->[0];
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


sub set_st_id_attributes {
    my ($instance,$identifier,$version) = @_;
    $instance->attribute_value('identifier',$identifier);
    $instance->attribute_value('identifierVersion',$version);
    $instance->attribute_value('_class','StableIdentifier');
    $instance->attribute_value('_displayName',"$identifier.version");
}

sub new_identifier {
    my $instance = shift;
    my $species = shift;
    if (my ($one,$twothree) = $species =~ /^([A-Z])[a-z]+\s+([a-z]{2})/) {
	$species = $one . uc $twothree;
    }
    return join('-','R',$species,$instance->db_id());
}

sub create_st_id {
    my ($db,$identifier,$version,$db_id) = @_;
    $db_id ||= new_db_id($db);
    my $instance = $dba{$db}->instance_from_hash({},'StableIdentifier',$db_id);
    set_st_id_attributes($instance,$identifier,$version);
    $dba{$db}->store($instance,1);
    return $instance;
}

sub max_db_id {
    my $db = shift;
    my $sth = $dba{$db}->prepare(MAX_ID);
    $sth->execute;
    my $max_db_id = $sth->fetchrow_arrayref->[0];
    return $max_db_id;
}

# get the largest DB_ID from slice or gk_central
sub new_db_id {
    my $max_id = 0;
    for my $db ($gk_central,$release_db) {
	my $id = max_db_id($db);
	$max_id = $id if $id > $max_id;
    }
    return $max_id + 1;
}

sub make_decision_on_species {
    my $instance = shift;
    my $species = get_species($instance) || 'ALL';
}
