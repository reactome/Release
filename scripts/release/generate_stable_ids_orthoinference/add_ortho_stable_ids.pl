#!/usr/local/bin/perl  -w
use strict;

use common::sense;
use autodie;
use Carp;
use Cwd;
use Getopt::Long;
use Data::Dumper;
use Log::Log4perl qw/get_logger/;
use Scalar::Util 'blessed';

use lib '/usr/local/gkb/modules';
use GKB::DBAdaptor;
use GKB::CommonUtils;
use GKB::Config;
use GKB::Utils_esther;

Log::Log4perl->init(\$LOG_CONF);
my $logger = get_logger(__PACKAGE__);

our($pass,$user,$release_db,$slice_db,$db_host,%seen_id,%species,$release_num,$dry_run);

my $usage = "Usage: $0 -user user -pass pass -db test_reactome_XX -sdb test_slice_XX -release_num XX\n";

GetOptions(
    "user:s"  => \$user,
    "pass:s"  => \$pass,
    "db:s"    => \$release_db,
    "sdb:s"    => \$slice_db,
    "host:s"   => \$db_host,
    "release_num:s" => \$release_num,
    "dry_run" => \$dry_run
);

($release_db && $release_num && $slice_db) || die $usage;

get_api_connections()->{$release_db}->execute("START TRANSACTION") unless $dry_run;
# Get list of all curated instances that have or need ST_IDs
foreach my $db_id (get_db_ids($release_db)) {    
    my $stable_id = fetch_stable_id($db_id, $release_db);
    next unless $stable_id;
    #next unless $stable_id->identifier->[0] =~ /R-HSA/;

    foreach my $orthologous_instance (get_orthologous_instances(get_instance($db_id, $release_db))) {
        eval {
            $orthologous_instance->db_id;
        };
        if ($@) {
            $logger->warn("Could not get db_id for orthologous instance $orthologous_instance inferred from $db_id");
            next;
        }        
        my $species = species($orthologous_instance);
        if (!$species) {
            $logger->warn("Could not get species for orthologous instance " . $orthologous_instance->db_id);
            next;
        }        
        
        my $identifier = $stable_id->identifier->[0];
        $identifier =~ s/R-[A-Z]{3}/R-$species/;
        #$logger->info("$db_id ST_ID $identifier");

        $seen_id{$identifier}++;

        # paralogs?                        
        my $prime = $seen_id{$identifier} if $seen_id{$identifier} > 1;
        if ($prime) {
            $identifier = "$identifier-$prime";
            $logger->info("We have a paralog here $identifier");
        }

        my $st_id = fetch_stable_id($orthologous_instance->db_id, $release_db);
        if (!$st_id) {
            create_stable_id($orthologous_instance->db_id, $release_db, $identifier) unless $dry_run;
            $logger->info("Stable ID $identifier created for inferred instance " .
                          $orthologous_instance->db_id . ' inferred from ' . $db_id);
        } else {
            $logger->info("Stable id already exists in $release_db as " . $st_id->identifier->[0] . " for $species inferred from " . $db_id);
        }
    }
}

get_api_connections()->{$release_db}->execute("COMMIT") unless $dry_run;

sub get_orthologous_instances {
    my $instance = shift;
    
    my $logger = get_logger(__PACKAGE__);
    
    my @orthologous_instances;

    if ($instance->is_a('Event')) {
        return unless is_human($instance);
        push @orthologous_instances, @{$instance->attribute_value('orthologousEvent')};
	} elsif ($instance->is_a('PhysicalEntity')) {
        push @orthologous_instances, @{$instance->attribute_value('inferredTo')};
    } else {
        $logger->warn($instance->displayName . ' (' . $instance->db_id . ') is a(n) ' .
                      $instance->class . ' and does not receive a stable identifier');
    }
    
    return @orthologous_instances;
}

sub get_db_ids {
    my $sth = get_api_connections()->{$release_db}->prepare('SELECT DB_ID FROM DatabaseObject WHERE _class = ?');
    my @db_ids;
    for my $class (classes_with_stable_ids()) {
        $sth->execute($class);
        while (my $db_id = $sth->fetchrow_array) {
            push @db_ids, $db_id;
        } 
    }
    return @db_ids;
}

sub classes_with_stable_ids {
    my $sth = get_api_connections()->{$slice_db}->prepare('SELECT DISTINCT _class FROM DatabaseObject WHERE StableIdentifier IS NOT NULL');
    $sth->execute();
    my $classes = $sth->fetchall_arrayref();
    my @classes = map {@$_} @$classes;
    return @classes;
}

sub species {
    my $instance = shift;
    
    my $species_display_name = eval{$instance->attribute_value('species')->[0]->displayName};
    
    return $species_display_name ? abbreviate($species_display_name) : undef;
}

sub get_species_from_instance {
    my $instance = shift;
    
    if ($instance->species->[0]) {
        return $instance->species->[0]->displayName;
    }
    
    if ($instance->is_a('CatalystActivity')) {
        if ($instance->activeUnit->[0]) {
            foreach my $active_unit (@{$instance->activeUnit}) {
                return $active_unit->species->[0]->displayName if $active_unit->species->[0];
            }
        } elsif ($instance->physicalEntity->[0]) {
            return $instance->physicalEntity->[0]->species->[0]->displayName if $instance->physicalEntity->[0]->species->[0];
        }
    }
    
    return undef;
}

sub abbreviate {
    local $_ = shift;
    my $short_name = uc(join('', /^([A-Za-z])[a-z]+\s+([a-z]{2})[a-z]+$/));
    return $short_name;
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

sub get_api_connections {
    state $api_connections;
    return $api_connections if $api_connections;

    my $release_dba = GKB::DBAdaptor->new(
        -dbname  => $release_db,
        -user    => $user || $GKB::Config::GK_DB_USER,
        -pass    => $pass || $GKB::Config::GK_DB_PASS,
        -host	 => $GKB::Config::GK_DB_HOST
    );

    my $slice_dba = GKB::DBAdaptor->new(
        -dbname  => $slice_db,
        -user    => $user || $GKB::Config::GK_DB_USER,
        -pass    => $pass || $GKB::Config::GK_DB_PASS,
        -host    => $GKB::Config::GK_DB_HOST
    );

    $api_connections = {
        $release_db => $release_dba,
        $slice_db => $slice_dba
    };
    
    return $api_connections;
}

sub get_instance {
    my $db_id = int shift || $logger->error_die("DB_ID must always be an integer");
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
    my $identifier = shift;
    
    my $logger = get_logger(__PACKAGE__);

    my $instance = get_instance($db_id, $db_name);    
    $instance->inflate();

    #$identifier ||= identifier($instance);
    my $version = 1;

    my $stable_id_instance = GKB::Instance->new(
        -CLASS => 'StableIdentifier',
        -ONTOLOGY => get_api_connections()->{$db_name}->ontology,
        -DBA => get_api_connections()->{$db_name}
    );
    $stable_id_instance->inflated(1);
    $stable_id_instance->identifier($identifier);
    $stable_id_instance->identifierVersion($version);
    $stable_id_instance->_displayName("$identifier.$version");
    $stable_id_instance->created(get_instance_edit($db_name));
    get_api_connections()->{$db_name}->store($stable_id_instance);
    $logger->info("Created new stable identifier " . $stable_id_instance->displayName . " for " . $instance->displayName);
    
    # Attach the stable ID to its parent instance
    $instance->stableIdentifier($stable_id_instance);
    $instance->Modified(@{$instance->Modified});
    $instance->add_attribute_value('modified', get_instance_edit($db_name));
    get_api_connections()->{$db_name}->update_attribute($instance, 'stableIdentifier');
    get_api_connections()->{$db_name}->update_attribute($instance, 'modified');
    $logger->info("Stable identifier " . $stable_id_instance->displayName . " attached to parent " . $instance->displayName);
    
    return $stable_id_instance;
}

sub fetch_stable_id {
    my $db_id = shift;
    my $db_name = shift;
    
    return get_instance($db_id, $db_name)->stableIdentifier->[0];
}
