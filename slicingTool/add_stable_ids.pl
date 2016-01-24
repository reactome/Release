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

# A few bare SQL queries
use constant DB_IDS => 'SELECT DB_ID FROM DatabaseObject WHERE _class = ?';
use constant MAX_ID => 'SELECT MAX(DB_ID) FROM DatabaseObject';
use constant ST_ID  => 'SELECT DB_ID FROM StableIdentifier WHERE identifier = ?';
use constant ALL_ST => 'SELECT DB_ID FROM StableIdentifier';
use constant ATTACHED => 'SELECT DB_ID FROM DatabaseObject WHERE StableIdentifier = ?';

use constant DEBUG => 0; 

# a few hard to place species names
use constant SPECIES => {
    'Hepatitis C virus genotype 2a'         => 'HEP',
    'Human herpesvirus 8'                   => 'HER',
    'Molluscum contagiosum virus subtype 1' => 'MCV',
    'Mycobacterium tuberculosis H37Rv'      => 'MTU',
    'Neisseria meningitidis serogroup B'    => 'NME',
    'Influenza A virus'                     => 'FLU',
    'Human immunodeficiency virus 1'        => 'HIV'
};

#say Dumper \@ARGV;

Log::Log4perl->init(\$LOG_CONF);
my $logger = get_logger(__PACKAGE__);

our($pass,$user,$release_db,$prev_release_db,$gk_central,$ghost,$release_num,%history,%species,%attached);

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

$ghost ||= 'localhost';
($release_db && $prev_release_db && $gk_central && $ghost && $user && $pass && $release_num) || say "$usage\n";


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
    my $identifier = identifier($instance);
    my $class      = $instance->class;
    my $name       = $instance->displayName;
    my $stable_id  = stable_id($instance);

    $logger->info(join("\t","STABLE_ID",$db_id,$class,$name,$stable_id->displayName)."\n");
}

# If stable ID exists, return instance.  If not, 
# create and store new ST_ID instance and return that.
sub stable_id {
    my $instance = shift;
    my $identifier = identifier($instance);

    my $st_id = fetch_stable_id($instance);

    unless ( $st_id ) {
	$st_id = create_stable_id($instance,$identifier,1);
    }
    
    return $st_id;
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
        -host    => $ghost || 'localhost',
        -user    => $user,
        -pass    => $pass
        );

    return ( $release_db      => $r_dba,
             $prev_release_db => $p_dba,
             $gk_central      => $g_dba,
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


sub get_instance {
    my $db_id = int shift || die "DB_ID must always be an integer";
    my $db    = shift;
    my $instance = $dba{$db}->fetch_instance_by_db_id($db_id)->[0];
    return $instance;
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
    my $species = species($instance);
    return join('-','R',$species,$instance->db_id());
}

sub species {
    my $instance = shift;
    my $name = $instance->displayName;
    return $species{$name} if $species{$name};
    my $long = make_decision_on_species($instance);
    $species{$name} = abbreviate($long);
    return $species{$name};
}

sub abbreviate {
    local $_ = shift;
    return $_ if /ALL|NUL/;

    # an instance?
    $_ = $_->displayName if ref($_);

    my $other_species = SPECIES;

    my $short_name = uc(join('', /^([A-Za-z])[a-z]+\s+([a-z]{2})[a-z]+$/));
    unless ($short_name) {
	if (/Bacteria/) {
	    $short_name = 'BAC';
	}
	elsif (/Virus/) {
            $short_name = 'VIR';
        }
	else {
	    $short_name = $other_species->{$_} || 'NUL';
	}
	$logger->info("Set short name for '$_' to $short_name\n");
    }
    return $short_name;
}

# Make a new ST_ID instance from scratch
sub create_stable_id {
    my ($instance,$identifier,$version,$db_id) = @_;
    $instance->inflate();

    $db_id ||= new_db_id($gk_central);
    my $st_id = $dba{$gk_central}->instance_from_hash({},'StableIdentifier',$db_id);
    set_st_id_attributes($st_id,$identifier,$version);

    $logger->info("creating new ST_ID " . $st_id->displayName . " for " . $instance->displayName);
    
    store($st_id,'store');
    
    # Attach the stable ID to its parent instance
    $instance->stableIdentifier($st_id);
    store($instance,'update');

    return $st_id;
}


#########################################################################
## failure tolerant(?) wrapper for the GKInstance store and update methods
my $attempt = 1;
sub store {
    my $instance = shift;
    my $action   = shift;
    my @dbs = @_;

    if ($attempt > 2) {
	$logger->warn("Oops, I tried to $action $attempt times, there must be a good reason it failed, giving up");
	$attempt = 1;
	return undef;
    }

    my $force = $action eq 'store' ? 1 : 0;

    unless (@dbs > 0) {
	@dbs =($gk_central,$release_db);
    }
    for my $db (@dbs) {
        my $stored = eval {$dba{$db}->$action($instance,$force)};
	unless ($stored) {
	    $logger->warn("Oops, the $action operation (attempt $attempt) failed for $db:\n$@_\nI'll try again!");
	    sleep 1;
	    $attempt++;
            store($instance,$action,$db);
	}
	else {
	    $attempt = 1;
	}
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

sub fetch_species {
    my $instance = shift;
    my $species = $instance->attribute_value('species');
    return undef if @$species == 0;
    my @species = map {$_->displayName} @$species;
    return wantarray ? @species : $species[0];
}

# Hopefully not-too-compicated reasoner to deal with entities that lack a species
sub make_decision_on_species {
    my $instance = shift;
    my $class = $instance->class;
    my @all_species = fetch_species($instance);
    my $species = $all_species[0];
    my $last_species  = $all_species[-1];
    
    # Regulator?  Get last species if applicable
    if ($class =~ /regulation|requirement/i) {
	$species = $last_species || $species;
	unless ($species) {
	    $logger->info("Looking for species of pathways or regulators for this $class\n");
	    my @entities = @{$instance->attribute_value('containedinPathway')};
	    push @entities, @{$instance->attribute_value('regulator')};
	    for my $entity (@entities) {
		$logger->info("Checking species for ".$entity->displayName);
		$species = fetch_species($entity);
		$logger->info("No species found") unless $species;
		last if $species;
	    }
	    $species ||= 'ALL';
	}
    }
    elsif ($class =~ /SimpleEntity|Polymer/) {
	$species ||= 'ALL';
    }
    elsif (!$species && $class eq 'Complex') {
	my $members = $instance->attribute_value('hasComponent');
	while (!$species && @$members >0) {
            my $member = shift @$members;
            $species = $member->attribute_value('species')->[0];
        }
    }
    elsif (!$species && $class =~ /Set/) {
	my $members = $instance->attribute_value('hasMember');
	while (!$species && @$members > 0) {
	    my $member = shift @$members;
            $species = $member->attribute_value('species')->[0];
        }
    }
    else {
	$species ||= 'NUL';
    }
    
    $logger->info(join("\t","SPECIES",$class,$species,abbreviate($species))."\n");
    return $species;
}
