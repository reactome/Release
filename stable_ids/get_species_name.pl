#!/usr/bin/perl -w
use common::sense;
use Data::Dumper;

use lib '/usr/local/gkb/modules';
use GKB::DBAdaptor;

my $db = shift;
my $db_id = shift;

my $dba = GKB::DBAdaptor->new(                                                                                                                                                                                                               
     -dbname => $db,
     -user   => 'curator', 
     -pass   => 'r3@ct1v3',
     -host   => 'localhost'
    );   

my $instances = $dba->fetch_instance_by_db_id($db_id);
for my $instance (@$instances) {
    say $instance->class;
    my $species = $instance->attribute_value('species');
    for (@$species) {
	say $_->name->[0];
    }
}
