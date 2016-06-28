#!/usr/local/bin/perl  -w
use strict;
use common::sense;
use autodie;
use Carp;
use Cwd;
use Getopt::Long;
use Data::Dumper;
use Log::Log4perl qw/get_logger/;

use lib '/usr/local/gkb/modules';
use GKB::DBAdaptor;
use GKB::Config;
use GKB::Utils_esther;

# A few bare SQL queries
use constant DB_IDS => 'SELECT DB_ID FROM DatabaseObject WHERE _class = ?';
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
($release_db && $prev_release_db && $gk_central && $ghost && $user && $pass && $release_num) || die "$usage\n";


# Make sure our requested DBs are slice DBs
check_db_names();

back_up_databases(
    [$user, $pass, $gk_central, $ghost],
    [$user, $pass, $release_db, 'localhost']
);

get_api_connections()->{$release_db}->execute("START TRANSACTION");
get_api_connections()->{$gk_central}->execute("START TRANSACTION");
# Evaluate each instance
for my $db_id (get_db_ids($release_db)) {
    my $instance   = get_instance($db_id, $release_db);
    my $class      = $instance->class;
    my $name       = $instance->displayName;
    my $stable_id  = stable_id($db_id);

    if (should_be_incremented($instance)) {
        say "$db_id $class $name should be incremented";
        increment_stable_id($db_id, $release_db);
        increment_stable_id($db_id, $gk_central);
    }
    
    if (is_updated($instance)) {
        say "$db_id $class $name should be updated";
        add_updated_flag($db_id, $release_db);
        add_updated_flag($db_id, $gk_central);
    }
    
    $logger->info(join("\t","STABLE_ID",$db_id,$class,$name,$stable_id->displayName)."\n");
}
#remove_orphan_stable_ids();
get_api_connections()->{$release_db}->execute("COMMIT");
get_api_connections()->{$gk_central}->execute("COMMIT");

sub is_updated {
    my $instance = shift;

    return 0 unless $instance->is_a('Event');

    my $db_id = $instance->db_id();
    my $prev_instance = get_instance($db_id,$prev_release_db);

    unless ($prev_instance) {
        $logger->info($instance->displayName  . " is new, no need for updated flag.\n");
        return 0;
    }

    if ($instance->attribute_value('releaseStatus')->[0]) {
        #say $instance->displayName ." ". $instance->attribute_value('releaseStatus')->[0];
        return 1;
    }

    my $revised2 = @{$instance->attribute_value('revised')} || 0;
    my $revised1 = @{$prev_instance->attribute_value('revised')} || 0;
    my $reviewed2 = @{$instance->attribute_value('reviewed')} || 0;
    my $reviewed1 = @{$prev_instance->attribute_value('reviewed')} || 0;

    if ($reviewed2 > $reviewed1 || $revised2 > $revised1) {
        return 1;
    }

    # Check if any children in the hierarchy are updated
    elsif (my @children = @{$instance->attribute_value('hasEvent')}) {
        for my $child (@children) {
            if (is_updated($child)) {
                return 1;
            }
        }
    }
}

sub should_be_incremented {
    my $instance = shift;
    my $db_id = $instance->db_id();
    my $prev_instance = get_instance($db_id,$prev_release_db);
    
    unless ($prev_instance) {
        $logger->info($instance->displayName  . " is new, no increment.\n");
        return 0;
    }

    my $mods2 = @{$instance->attribute_value('modified')} || 0;
    my $mods1 = @{$prev_instance->attribute_value('modified')} || 0;
    if ($mods1 == $mods2) {
        return 0;
    }
    elsif ($mods2 > $mods1) {
        return 1;
    }
    else {
        $logger->warn("Something is fishy with the modifications for instance $db_id");
        return 0;
    }
}

sub increment_stable_id {
    my $db_id = shift;
    my $db_name = shift;
    
    my $stable_id_instance = stable_id($db_id, $db_name);
							  
	$stable_id_instance->inflate();
	my $identifier =  $stable_id_instance->attribute_value('identifier')->[0];
    my $version  = $stable_id_instance->attribute_value('identifierVersion')->[0];
    my $new_version = $version + 1;

    $logger->info("Incrementing ".$stable_id_instance->displayName." version from $version to $new_version\n");

    $stable_id_instance->attribute_value('identifierVersion',$new_version);
    $stable_id_instance->displayName("$identifier.$new_version");
    $stable_id_instance->Modified(@{$stable_id_instance->Modified});
    $stable_id_instance->add_attribute_value('modified', get_instance_edit($db_name));
    
    foreach my $attribute ('identifierVersion', '_displayName', 'modified') {
        get_api_connections()->{$db_name}->update_attribute($stable_id_instance, $attribute);
    }    
}

sub add_updated_flag {
    my $db_id = shift;
    my $db_name = shift;
    
    my $instance = get_instance($db_id, $db_name);
    $instance->inflate();

    $logger->info("Adding updated flag to ".$instance->displayName.".");

    $instance->attribute_value('releaseStatus','UPDATED');
    $instance->Modified(@{$instance->Modified});
    $instance->add_attribute_value('modified', get_instance_edit($db_name));
    
    get_api_connections()->{$db_name}->update_attribute($instance, 'releaseStatus');
    get_api_connections()->{$db_name}->update_attribute($instance, 'modified');
}

sub attached {
    my $st_id = shift;
    my $parent = shift;

    if ($parent) {
        $attached{$st_id} = $parent;
        return $parent;
    }

    if ($attached{$st_id}) {
        return $attached{$st_id};
    }

    my $sth = get_api_connections()->{$release_db}->prepare(ATTACHED);
    $sth->execute($st_id);
    while (my $result = $sth->fetchrow_arrayref) {
        my $id = $result->[0];
        if ($id) {
            #say"calling get_instance $release_db";
            $parent = get_instance($id,$release_db);
            if ($parent) {
            	$attached{$st_id} = $parent;
            	return $parent;
            }
        }
    }
    return undef;
}

# If the stable ID is not attached to an event, put it in the attic
#sub remove_orphan_stable_ids {
#    my $sth = get_api_connections()->{$gk_central}->prepare(ALL_ST);
#    $sth->execute();
#    while (my $res = $sth->fetchrow_arrayref) {
#        my $db_id = $res->[0] || next;
#        next if attached($db_id);
#
#        my $deleted;
#        for my $db ($release_db,$gk_central) {
#            my $st_id = get_instance($db_id,$db) || next;
#
#            $logger->info("Deleting orphan stable identifier ".$st_id->displayName." in $db\n");
#
#            get_api_connections()->{$db}->delete($st_id);
#            $deleted++;
#        }
#    }
#}

sub classes_with_stable_ids {
    # derived from:
    # select distinct _class from DatabaseObject where StableIdentifier is not null 
    qw/
    Pathway SimpleEntity OtherEntity DefinedSet Complex EntityWithAccessionedSequence GenomeEncodedEntity
    Reaction BlackBoxEvent PositiveRegulation CandidateSet NegativeRegulation OpenSet Requirement Polymer
    Depolymerisation EntitySet Polymerisation FailedReaction
    /;
}

sub identifier {
    my $instance = shift;
    my $species = species($instance);
    return join('-','R',$species,$instance->db_id());
}

sub species {
    my $instance = shift;
    my $db_id = $instance->db_id;
    
    return $species{$db_id} if $species{$db_id};
    
    my $long = make_decision_on_species($instance);
    $species{$db_id} = abbreviate($long);
    return $species{$db_id};
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

sub back_up_databases {
    my @dbs = @_;
    
    foreach my $db (@dbs) {
        my ($user, $pass, $name, $host) = @$db;
        next unless $name && $host;
        $user ||= $GKB::Config::GK_DB_USER;
        $pass ||= $GKB::Config::GK_DB_PASS;
        
        my $back_up_successful = (system("mysqldump -h $host -u $user -p$pass $name > $name.dump") == 0);
        die "Unable to back-up $db at $host for $user" unless $back_up_successful;
    }   
}

# If stable ID exists, return instance.  If not, 
# create and store new ST_ID instance and return that.
sub stable_id {
    my $db_id = shift;
	my $db_name = shift // $release_db;

    my $st_id = fetch_stable_id($db_id, $db_name);

    unless ( $st_id ) {
        $st_id = create_stable_id($db_id, $release_db);
        create_stable_id($db_id, $gk_central);
    }
    
    return $st_id;
}


sub get_instance_edit {
    state $db_to_instance_edit;
    my $db = shift;
    
    return $db_to_instance_edit->{$db} if $db_to_instance_edit->{$db};
    
    (my $dba = get_api_connections()->{$db}) // confess "No database adaptor for $db database available to create instance edit";
    my $date = `date \+\%F`;
    chomp $date;
    $db_to_instance_edit->{$db} = GKB::Utils_esther::create_instance_edit($dba, 'Weiser', 'JD', $date);
        
    return $db_to_instance_edit->{$db};
}

sub check_db_names {
    unless ($prev_release_db =~ /slice/ && $release_db =~ /slice/) {
        die "Both of these databases ($release_db and $prev_release_db) should be slice databases";
    }
}

sub get_api_connections {
    state $api_connections;
    return $api_connections if $api_connections;

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

    $api_connections = {
        $release_db => $r_dba,
        $prev_release_db => $p_dba,
        $gk_central => $g_dba,
    };
    
    return $api_connections;
}

sub get_db_ids {
    my $sth = get_api_connections()->{$release_db}->prepare(DB_IDS);
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
    
    state $instance_cache;
    return $instance_cache->{$db}->{$db_id} if $instance_cache->{$db}->{$db_id};
    
    my $instance = get_api_connections()->{$db}->fetch_instance_by_db_id($db_id)->[0];
    $instance_cache->{$db}->{$db_id} = $instance;
    return $instance;
}

# Make a new ST_ID instance from scratch
sub create_stable_id {
    my $db_id = shift;
    my $db_name = shift;

    my $instance = get_instance($db_id, $db_name);    
    $instance->inflate();

    my $identifier = identifier($instance);
    my $version = 1;

    my $stable_id_instance = GKB::Instance->new(
        -CLASS => 'StableIdentifier',
        -ONTOLOGY => get_api_connections()->{$db_name}->ontology
    );
    $stable_id_instance->inflated(1);
    $stable_id_instance->identifier($identifier);
    $stable_id_instance->identifierVersion($version);
    $stable_id_instance->_displayName("$identifier.$version");
    $stable_id_instance->created(get_instance_edit($db_name));
    
    get_api_connections()->{$db_name}->store($stable_id_instance);
    
    # Attach the stable ID to its parent instance
    $instance->stableIdentifier($stable_id_instance);
    $instance->Modified(@{$instance->Modified});
    $instance->add_attribute_value('modified', get_instance_edit($db_name));
    get_api_connections()->{$db_name}->update_attribute($instance, 'stableIdentifier');
    get_api_connections()->{$db_name}->update_attribute($instance, 'modified');
    
    return $stable_id_instance;
}

sub fetch_stable_id {
    my $db_id = shift;
    my $db_name = shift;
    
    return get_instance($db_id, $db_name)->attribute_value('stableIdentifier')->[0];
}

sub fetch_species {
    my $instance = shift;
    my $species = $instance->attribute_value('species');
    return undef if @$species == 0;
    my @species = map {$_->displayName} @$species;
    return wantarray ? @species : $species[0];
}
