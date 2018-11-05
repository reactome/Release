#!/usr/bin/perl
use strict;
use warnings;

use lib '/usr/local/gkb/modules';

use GKB::Config;
use GKB::DBAdaptor;
use GKB::Instance;
use Test::Exception;
use Test::More;

use_ok('GKB::NamedInstance');

my $dba = GKB::DBAdaptor->new(
     -dbname => $GK_DB_NAME,
     -user   => $GK_DB_USER,
     -host   => $GK_DB_HOST, # host where mysqld is running
     -pass   => $GK_DB_PASS,
);

my %compartments = 
        map { $_->displayName => $_ }
        grep { $_->displayName eq 'cytosol' || $_->displayName eq 'plasma membrane' } 
            @{$dba->fetch_instance(-CLASS => 'Compartment')};

my $physical_entity = GKB::Instance->new(
    -CLASS => 'PhysicalEntity',
    -ONTOLOGY => $dba->ontology,
    'name' => ['physical entity name 1', 'physical entity name 2'],
    'compartment' => [$compartments{'cytosol'}, $compartments{'plasma membrane'}]
);

throws_ok {$physical_entity->displayName} qr/Can't access attribute '_displayName'/, 'display name not available yet for physical entity';
my $returned_physical_entity_name = GKB::NamedInstance->new(-INSTANCE => $physical_entity)->set_displayName();
is($physical_entity->displayName, 'physical entity name 1 [cytosol]', 'display name of physical entity set correctly');
is($returned_physical_entity_name, $physical_entity->displayName, 'returned value matches physical entity instance display name');

my $chemical_drug = GKB::Instance->new(
    -CLASS => 'ChemicalDrug',
    -ONTOLOGY => $dba->ontology,
    'name' => ['drug name 1', 'drug name 2'],
    'compartment' => [$compartments{'plasma membrane'}, $compartments{'cytosol'}]
);

throws_ok {$chemical_drug->displayName} qr/Can't access attribute '_displayName'/, 'display name not available yet for chemical drug';
my $returned_chemical_drug_name = GKB::NamedInstance->new(-INSTANCE => $chemical_drug)->set_displayName();
is($chemical_drug->displayName, 'drug name 1 [plasma membrane]', 'display name of chemical drug set correctly');
is($returned_chemical_drug_name, $chemical_drug->displayName, 'returned value matches chemical drug instance display name');

done_testing();