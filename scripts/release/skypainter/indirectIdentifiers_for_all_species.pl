#!/usr/local/bin/perl -w

use lib "$ENV{HOME}/GKB/modules";
use lib "$ENV{HOME}/bioperl-1.0";
use lib "$ENV{HOME}/my_perl_stuff";

use strict;
use GKB::Utils;
use GKB::Instance;
use GKB::Config;

$GKB::Config::NO_SCHEMA_VALIDITY_CHECK = undef;

@ARGV || die("Usage: $0 -db db_name ...\n");

my @params = @ARGV;

my $dba = GKB::Utils::get_db_connection();

my $protein_class = &GKB::Utils::get_reference_protein_class($dba);
my $species = $dba->fetch_instance_by_remote_attribute
    (
     'Species',
     [
      ["species:$protein_class",'IS NOT NULL',[]],
      ["species:$protein_class.referenceDatabase.name",'=',['UniProt','Ensembl']],
#      ['species:ReferencePeptideSequence','IS NOT NULL',[]],
#      ['species:ReferencePeptideSequence.referenceDatabase.name','=',['UniProt','Ensembl']],
     ]
    );

my @cmds = (qq(./indirectIdentifiers_from_mart.pl @params), qq(./gene_names_from_mart.pl @params));
foreach my $sp (@{$species}) {
    foreach my $cmd (@cmds) {
		my $tmp = "$cmd -sp '" . $sp->displayName . "'";
		print "$tmp\n";
		system($tmp) == 0 or print "$tmp failed.\n";
    }
}

print STDERR "$0: no fatal errors.\n";

