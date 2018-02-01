#!/usr/bin/perl -w
use common::sense;

use lib '/usr/local/gkb/modules';
use GKB::DBAdaptor;

@ARGV || die "Usage ./pathway_info.pl username pass list_of_pathway_ids";

my $user = shift;
my $pass = shift;


# use a list of databases to catch the non-current pathway_ids
my @dbs = reverse map {"test_reactome_$_"} 42..61;
for my $db (@dbs) {
    my $dba = GKB::DBAdaptor->new(
	-dbname => $db,
	-user   => $user,
	-pass   => $pass,
	);
    $db = $dba;
}

while (my $db_id = <>) {
    chomp $db_id;
    my $instance;
    for my $dba (@dbs) {
	$instance = instance($dba,$db_id);
	last if $instance;
    }
    if ($instance) {
	my $inferred = inferred($instance);
	my $species  = species($instance);
	my $class    = $instance->class;
	next if $class ne 'Pathway';
	my $name     = $instance->displayName;
	$name =~ s/,/;/g;

	say join("\t",$db_id,$species,$name,$inferred);
    }
    else {
	say STDERR "No instance for $db_id";
    }
}

sub instance {
    my $dba   = shift;
    my $db_id = shift;
    return $dba->fetch_instance_by_db_id($db_id)->[0];
}

sub inferred {
    my $instance = shift;
    my $evidence = $instance->attribute_value('evidenceType')->[0];
    if ($evidence) {
	return 1 if $evidence->displayName =~ /inferred/;
    }
    return '';
}

sub species {
    my $instance = shift;
    my $species = $instance->attribute_value('species')->[0]  || return 'UNKNOWN';
    return $species->name->[0];
}
