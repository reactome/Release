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
    my $p_instance = get_instance($db_id, $prev_release_db);

    my $species = get_species($instance) || "Homo sapiens";
    my $spc     = abbreviate($species);
    my $class   = $instance->class;
    my $name    = $instance->displayName;
    say join("\t",$db_id,$class,$species,$spc,$name) if $species;
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
    my $sth = $dba{$release_db}->prepare(query('get_db_ids'));
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

sub create_st_id {
    my ($identifier,$version) =@_;

}

sub query {
    my $q = shift;
    my %Q = (
	get_db_ids         => 'SELECT DB_ID FROM DatabaseObject WHERE _class = ?',
	get_max_db_id      => 'SELECT MAX(DB_ID) FROM DatabaseObject',    
	update_version     => 'UPDATE StableIdentifier SET identifierVersion = ? where DB_ID = ?',
	update_identifier  => 'UPDATE StableIdentifier SET identifier = ? WHERE DB_ID = ?',
	update_db_object   => 'UPDATE DatabaseObject   SET _displayName = ? WHERE DB_ID = ?',
	archive_identifier => 'UPDATE StableIdentifier SET oldIdentifier = ? WHERE DB_ID = ?',
	archive_version    => 'UPDATE StableIdentifier SET oldIdentifierVersion = ? WHERE DB_ID = ?',
	);
    return $Q{$q};
}
