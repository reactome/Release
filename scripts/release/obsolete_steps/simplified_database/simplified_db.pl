#!/usr/bin/perl
use strict;
use warnings;
use feature qw/state/;

use lib '/usr/local/gkb/modules';

use autodie qw/:all/;
use Carp;
use DBI;
use Getopt::Long;
use Try::Tiny;

use GKB::Config;
use GKB::DBAdaptor;

use Log::Log4perl qw/get_logger/;
Log::Log4perl->init(\$LOG_CONF);
my $logger = get_logger(__PACKAGE__);

my ($source_db, $source_host, $simplified_db, $overwrite, $help);
GetOptions(
    'source_db=s' => \$source_db,
    'source_host=s' => \$source_host,
    'simplified_db=s' => \$simplified_db,
    'overwrite' => \$overwrite,
    'help' => \$help
);

if ($help) {
    print usage_instructions();
    exit;
}

$source_db 	||= $GKB::Config::GK_DB_NAME;
$source_host	||= $GKB::Config::GK_DB_HOST;
$simplified_db 	||= $source_db . "_dn";

create_simplified_database($simplified_db, $overwrite);
load_simplified_database_schema($simplified_db);

my $dba = get_dba($source_db, $source_host);

my @pathways = @{$dba->fetch_instance(-CLASS => 'Pathway')};
$logger->info("Beginning population of pathway table");
populate_pathway_table(@pathways);
$logger->info("Finished population of pathway table");

my @reaction_like_events = @{$dba->fetch_instance(-CLASS => 'ReactionlikeEvent')};
$logger->info("Beginning population of reaction like event table");
populate_reaction_like_event_table(@reaction_like_events);
$logger->info("Finished population of reaction like event table");

$logger->info("Beginning population of pathway link tables");
populate_pathway_link_tables(@pathways);
$logger->info("Finished population of pathway link tables");

my @physical_entities = @{$dba->fetch_instance(-CLASS => 'PhysicalEntity')};
$logger->info("Beginning population of physical entity table");
populate_physical_entity_table(@physical_entities);
$logger->info("Finished population of physical entity table");

$logger->info("Beginning population of physical entity hierarchy table");
populate_physical_entity_hierarchy_table(@physical_entities);
$logger->info("Finished population of physical entity hierarchy table");

$logger->info("Beginning population of reaction like event to physical entity table");
populate_reaction_like_event_to_physical_entity_table(@reaction_like_events);    
$logger->info("Finished population of reaction like event to physical entity table");

$logger->info("Beginning population of id to external identifier table");
populate_id_to_external_identifier_table(@pathways, @reaction_like_events, @physical_entities);
$logger->info("Finished population of id to external identifier table");

$logger->info("Indexing external identifiers");
index_external_identifiers();
$logger->info("Finished indexing external identifiers"); 

sub get_dba {
    my $db = shift;
    my $host = shift;
    
    return GKB::DBAdaptor->new (
        -user => $GKB::Config::GK_DB_USER,
        -pass => $GKB::Config::GK_DB_PASS,
        -host => $host,
        -dbname => $db
    );
}

sub populate_pathway_table {
    my @pathways = @_;
    
    my $logger = get_logger(__PACKAGE__);
    
    my $dbh = get_simplified_database_handle();
    
    $dbh->begin_work();
    
    my $sth = $dbh->prepare("INSERT INTO Pathway (id, displayName, species, stableId) VALUES (?, ?, ?, ?)")
        or $logger->logconfess($dbh->errstr);

    foreach my $pathway (@pathways) {
        my $db_id = $pathway->db_id;
        my $display_name = $pathway->displayName;
        my $species = get_species($pathway);
        my $stable_id = get_stable_id($pathway);

        try {
            $sth->execute($db_id, $display_name, $species, $stable_id);
        } catch {
            $logger->logcarp("Problem inserting pathway $db_id: $_");
        };
    }
    
    $dbh->commit();
}

sub populate_pathway_link_tables {
    my @pathways = @_;
    
    my $logger = get_logger(__PACKAGE__);
    
    my $dbh = get_simplified_database_handle();
    
    $dbh->begin_work();
    
    my $reaction_statement_handle = $dbh->prepare("INSERT INTO Pathway_To_ReactionLikeEvent (pathwayId, reactionLikeEventId) VALUES (?, ?)")
        or $logger->logconfess($dbh->errstr);

    my $pathway_statement_handle = $dbh->prepare("INSERT INTO PathwayHierarchy (pathwayId, childPathwayId) VALUES (?, ?)")
        or $logger->logconfess($dbh->errstr);

    foreach my $pathway (@pathways) {
        foreach my $event (@{$pathway->hasEvent}) {
            my $sth = $event->is_a('Pathway') ? $pathway_statement_handle : $reaction_statement_handle;
    
            try {
                $sth->execute($pathway->db_id, $event->db_id);
            } catch {
                $logger->logcarp("Problem inserting pathway " . $pathway->db_id . " with event " . $event->db_id . ": $_");
            };
        }
    }
    
    $dbh->commit();
}

sub populate_reaction_like_event_table {
    my @reaction_like_events = @_;
    
    my $logger = get_logger(__PACKAGE__);
    
    my $dbh = get_simplified_database_handle();
    
    $dbh->begin_work();
    
    my $sth = $dbh->prepare("INSERT INTO ReactionLikeEvent (id, displayName, species, class, stableId) VALUES (?, ?, ?, ?, ?)")
        or $logger->logconfess($dbh->errstr);
    
    foreach my $event (@reaction_like_events) {
        my $db_id = $event->db_id;
        my $display_name = $event->displayName;
        my $species = get_species($event);
        my $class = $event->class;
        my $stable_id = get_stable_id($event);

        try {
            $sth->execute($db_id, $display_name, $species, $class, $stable_id);
        } catch {
           $logger->logcarp("Problem inserting reaction like event $db_id: $_");
        };
    }
    
    $dbh->commit();
}

sub populate_physical_entity_table {
    my @physical_entities = @_;
    
    my $logger = get_logger(__PACKAGE__);
    
    my $dbh = get_simplified_database_handle();
    
    $dbh->begin_work();
    
    my $sth = $dbh->prepare("INSERT INTO PhysicalEntity (id, displayName, species, class, stableId) VALUES (?, ?, ?, ?, ?)")
        or $logger->logconfess($dbh->errstr);
    
    foreach my $physical_entity (@physical_entities) {
        my $db_id = $physical_entity->db_id;
        my $display_name = $physical_entity->displayName;
        my $species = get_species($physical_entity);
        my $class = $physical_entity->class;
        my $stable_id = get_stable_id($physical_entity);
        
        try {
            $sth->execute($db_id, $display_name, $species, $class, $stable_id);
        } catch {
            $logger->logcarp("Problem inserting physical entity $db_id: $_");
        };
    }
    
    $dbh->commit();
}

sub populate_physical_entity_hierarchy_table {
    my @physical_entities = @_;
    
    my $logger = get_logger(__PACKAGE__);
    
    my $dbh = get_simplified_database_handle();
    
    $dbh->begin_work();
    
    my $sth = $dbh->prepare("INSERT INTO PhysicalEntityHierarchy (physicalEntityId, childPhysicalEntityId) VALUES (?, ?)")
        or $logger->logconfess($dbh->errstr);

    foreach my $physical_entity (@physical_entities) {
        foreach my $child_entity (@{$physical_entity->hasComponent}, @{$physical_entity->hasMember}, @{$physical_entity->hasCandidate}) {	    
            try {
                $sth->execute($physical_entity->db_id, $child_entity->db_id);
            };
        }
    }
    
    $dbh->commit();
}

sub populate_reaction_like_event_to_physical_entity_table {
    my @reaction_like_events = @_;
    
    my $logger = get_logger(__PACKAGE__);
    
    my $dbh = get_simplified_database_handle();
    
    $dbh->begin_work();
    
    my $sth = $dbh->prepare("INSERT INTO ReactionLikeEvent_To_PhysicalEntity (reactionLikeEventId, physicalEntityId) VALUES (?, ?)")
        or $logger->logconfess($dbh->errstr);

    foreach my $reaction_like_event (@reaction_like_events) {
        foreach my $physical_entity (get_physical_entities_in_reaction_like_event($reaction_like_event)) {
            try {
                $sth->execute($reaction_like_event->db_id, $physical_entity->db_id);
            } catch {
                $logger->logcarp("Problem inserting reaction like event " . $reaction_like_event->db_id . " with physical entity " . $physical_entity->db_id . ": $_")
                unless /duplicate/i;
            };
        }
    }
    
    $dbh->commit();
}

sub get_physical_entities_in_reaction_like_event {
    my $reaction_like_event = shift;

    my $logger = get_logger(__PACKAGE__);

    my @physical_entities;
    push @physical_entities, @{$reaction_like_event->input};
    push @physical_entities, @{$reaction_like_event->output};
    push @physical_entities, map($_->physicalEntity->[0], @{$reaction_like_event->catalystActivity});
    
    my %physical_entities = map {$_->db_id => $_} grep {$_} @physical_entities;
    
    # ...and all the sub-components of this PE
    for my $pe (values %physical_entities) {
        my @subs = recurse_physical_entity_components($pe);
        for my $sub (@subs) {
            $sub or next;
            #$logger->info("Adding sub component ".join(' ',$sub->class,$sub->displayName));
            $physical_entities{$sub->db_id} = $sub;
        }
    }

    return values %physical_entities;
}

# Recurse through all members/components so all descendent PEs will also
# be linked to the reaction/pathway
sub recurse_physical_entity_components {
    my $pe = shift;

    my %components = map {$_->db_id => $_} grep {$_} @{$pe->hasMember}, @{$pe->hasComponent};
    keys %components || return ();
    
    for my $component (values %components) {
        for my $sub_component (recurse_physical_entity_components($component)) { 
            $components{$sub_component->db_id} = $sub_component;
        }
    }

    return values %components;
}

sub populate_id_to_external_identifier_table {
    my @instances = @_;
    
    my $logger = get_logger(__PACKAGE__);

    my $instance_count = 0;
    
    my $dbh = get_simplified_database_handle();
    
    $dbh->begin_work();
    
    my $sth = $dbh->prepare("INSERT INTO Id_To_ExternalIdentifier (id, referenceDatabase, externalIdentifier, description) VALUES (?, ?, ?, ?)")
		or $logger->logconfess($dbh->errstr);
    foreach my $instance (@instances) {
        $instance_count++;
        
        my $db_id = $instance->db_id;

        foreach my $record (get_external_identifier_records($instance)) {
            my $reference_database = $record->[0];
            my $identifier = $record->[1];
            my $description = $record->[2];
	    
            try {
                $sth->execute($db_id, $reference_database, $identifier, $description);
            } catch {
                $logger->logcarp("Problem inserting external identifier $reference_database:$identifier $description for $db_id: $_")
                unless /duplicate/i;
            };
        }
        
        if ($instance_count % 1000 == 0) {
            $instance->dba->instance_cache->empty;
        }
    }
    
    $dbh->commit();
}

sub get_external_identifier_records {
    my $instance = shift;
    
    my @records;
    push @records, process_compartments($instance);
    push @records, process_cross_reference($instance);
    push @records, process_disease($instance);
    push @records, process_GO_biological_process($instance);
    push @records, process_catalyst_activities($instance);
    push @records, process_reference_entity($instance);
    
    return @records;
}

sub index_external_identifiers {
    my $dbh = get_simplified_database_handle();
    $dbh->begin_work();
    my $sth = $dbh->prepare("CREATE INDEX `externalIdentifier` ON Id_To_ExternalIdentifier (`externalIdentifier`)") 
        or $logger->logconfess($dbh->errstr);
    $dbh->commit();
}

sub process_GO_biological_process {
    my $instance = shift;
    
    return unless $instance->is_a('Event');
    
    my $go_biological_process = $instance->goBiologicalProcess->[0];
    return unless $go_biological_process;
    
    return process_GO_instances($go_biological_process);
}

sub process_catalyst_activities {
    my $instance = shift;
    
    return unless $instance->is_a('ReactionlikeEvent');
    
    my @go_molecular_functions = map {$_->activity->[0]} @{$instance->catalystActivity};
    return unless @go_molecular_functions;
    
    return process_GO_instances(@go_molecular_functions);
}

sub process_GO_instances {
    my @go_instances = @_;
    
    my $logger = get_logger(__PACKAGE__);
    
    my @records;
    foreach my $go_instance (@go_instances) {
        next unless $go_instance;
        try {
            push @records, [$go_instance->referenceDatabase->[0]->displayName, $go_instance->accession->[0], $go_instance->displayName];
        } catch {
            $logger->logcarp("Problem processing GO instance: $_");
        };
    }
    return @records;
}

sub process_reference_entity {
    my $instance = shift;
    
    return unless $instance->is_a('PhysicalEntity');
    
    my $reference_entity = $instance->referenceEntity->[0];
    return unless $reference_entity;
    
    my @records;
    push @records, process_generic_identifier($reference_entity);
    push @records, process_cross_reference($reference_entity);
    push @records, ['', $_, ''] foreach (@{$reference_entity->otherIdentifier}, @{$reference_entity->secondaryIdentifier});

    if ($reference_entity->is_a('ReferenceGeneProduct') || $reference_entity->is_a('ReferenceRNASequence')) {
        if ($reference_entity->is_a('ReferenceGeneProduct')) {
            push @records, process_generic_identifier($_) foreach @{$reference_entity->referenceTranscript};
            push @records, process_cross_reference($_) foreach @{$reference_entity->referenceTranscript};
        }

        push @records, process_generic_identifier($_) foreach @{$reference_entity->referenceGene};	
        push @records, process_cross_reference($_) foreach @{$reference_entity->referenceGene};
    }

    return @records;
}

=head
sub get_cross_references {
    my @instances_with_cross_references = @_;
    
    my @cross_references;
    
    my @cross_references_ref_array = map ({$_->crossReference} @instances_with_cross_references);
    foreach my $cross_reference_ref (@cross_references_ref_array) {
	push @cross_references, @{$cross_reference_ref};
    }
    
    return @cross_references;
}
=cut

sub process_compartments {
    my $instance = shift;
    
    return process_GO_instances(@{$instance->compartment});
}

sub process_cross_reference {
    my $instance = shift;
    
    return process_generic_identifier(@{$instance->crossReference});
}

sub process_disease {
    my $instance = shift;
    
    return process_generic_identifier(@{$instance->disease});
}

sub process_generic_identifier {
    my @instances = @_;
    
    my $logger = get_logger(__PACKAGE__);

    my @records;
    
    foreach my $instance (@instances) {
        try {
            push @records, [$instance->referenceDatabase->[0]->displayName, $instance->identifier->[0], $instance->displayName];
        } catch {
            $logger->warn("Can't insert a record for " . $instance->db_id);
        };
    }
    return @records;
}

sub create_simplified_database {
    my $simplified_database = shift;
    my $overwrite = shift;
    
    my $logger = get_logger(__PACKAGE__);
    
    my $dbh = get_simplified_database_handle();
    
    try {
        $dbh->do("create database $simplified_database");
    } catch {
        unless (/database exists/ && $overwrite) {
            my $error = /database exists/ ?
            "$simplified_database exists.  Use the -overwrite flag if you wish to replace it." :
            $_;
	    
            $logger->error_die($error);
        }
	
        $dbh->do("drop database $simplified_database");
        create_simplified_database($simplified_database);
    };
}

sub load_simplified_database_schema {
    my $simplified_database = shift;
    
    my $schema_file = "simplified.sql";
    croak "$schema_file doesn't exist" unless (-e $schema_file);
    
    system("mysql -u $GKB::Config::GK_DB_USER -h$GKB::Config::GK_DB_HOST -p$GKB::Config::GK_DB_PASS $simplified_database < $schema_file");
    my $dbh = get_simplified_database_handle();
    $dbh->do("use $simplified_database");
}

sub get_simplified_database_handle {
    state $dbh = DBI->connect("DBI:mysql:host=$GKB::Config::GK_DB_HOST;port=$GKB::Config::GK_DB_PORT",
			      $GKB::Config::GK_DB_USER,
			      $GKB::Config::GK_DB_PASS,
			      {RaiseError => 1,
			       PrintError => 0}
			      );
    
    return $dbh;
}

sub get_species {
    my $instance = shift;
    
    return unless $instance->species->[0];
    
    return $instance->species->[0]->displayName;
}

sub get_stable_id {
    my $instance = shift;
    
    return unless $instance->stableIdentifier->[0];
    
    return $instance->stableIdentifier->[0]->identifier->[0];
}

sub usage_instructions {
    return <<END;
    perl $0 [options]
    
    Options:
    
    -source_db	[db_name]	Source database used to populate the simplified database (default is $GKB::Config::GK_DB_NAME)
    -source_host [db_host]	Host of source database (default is $GKB::Config::GK_DB_HOST)
    -simplified_db [db_name]	Name of database to be created (default is $GKB::Config::GK_DB_NAME\_dn)
    -overwrite			Overwrite simplified database if it exists
    -help			Display these instructions
END
}
