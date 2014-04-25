#!/usr/local/bin/perl -w

# Make sure you don't have "competing" libraries...
# for use @CSHL
use lib "/usr/local/gkb/modules";
# for use @HOME
use lib "$ENV{HOME}/bioperl-1.0";
use lib "$ENV{HOME}/GKB/modules";
use lib "$ENV{HOME}/my_perl_stuff";

use DBI;
use GKB::DBAdaptor;
use GKB::Utils;
use Data::Dumper;
use Getopt::Long;
use strict;

our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db,$opt_debug,
    $opt_martuser,$opt_marthost,$opt_martpass,$opt_martport,$opt_martdb,
    $opt_sp);

(@ARGV) || die "Usage: $0 -sp 'species name'
-user db_user -host db_host -pass db_pass -port db_port -db db_name
-martuser mdb_user -marthost mdb_host -martpass mdb_pass -martport mdb_port -martdb mdb_name\n";

&GetOptions("user:s", "host:s", "pass:s", "port:i", "db:s", "debug",
	    "martuser:s", "marthost:s", "martpass:s", "martport:i", "martdb:s",
	    "sp=s"
);

$opt_db || die "Need database name (-db).\n";    

# Get connection to reactome db
my $dba = GKB::DBAdaptor->new
    (
     -user   => $opt_user,
     -host   => $opt_host,
     -pass   => $opt_pass,
     -port   => $opt_port,
     -dbname => $opt_db,
#     -DEBUG => $opt_debug
     );

# Fetch the species
$opt_sp ||= 'Homo sapiens';
my $sp = $dba->fetch_instance_by_attribute('Species',[['name',[$opt_sp]]])->[0]
    || die "No species '$opt_sp' found.\n";
my $sp_mart_name;
if ($sp->displayName =~ /^(\w)\w+ (\w+)$/) {
    $sp_mart_name = lc("$1$2");
} else {
    die "Can't form species abbreviation for mart from '" . $sp->displayName . "'.\n";
}

my ($dsn,$dbh,$stmt,$sth,$res);
$dbh = GKB::Utils::get_handle_to_ensembl_mart(undef,$opt_marthost,$opt_martport,$opt_martuser,$opt_martpass);

my $protein_class = &GKB::Utils::get_reference_protein_class($dba);
# Fetch ReferenceGeneProducts to be "annotated".
my $rpss = $dba->fetch_instance_by_attribute($protein_class,
					     [
					      ['species',[$sp->db_id]],
					      ['geneName',[],'IS NULL']
					     ]);
unless (@{$rpss}) {
    print "No relevant $protein_class instances.\n";
    exit(0);
}
my %accs;
map {push @{$accs{uc($_->Identifier->[0])}},$_} @{$rpss};
my $tmp = join(',', (('?') x scalar(keys %accs)));

# Gene names by UniProt accessions
$stmt = <<__END__;
SELECT u.dbprimary_id, g.display_id
FROM
$ {sp_mart_name}_gene_ensembl__xref_uniprot_accession__dm u,
$ {sp_mart_name}_gene_ensembl__gene__main g
WHERE
u.dbprimary_id IN ($tmp)
AND u.gene_id_key = g.gene_id_key
AND g.display_id IS NOT NULL
__END__
$opt_debug && print "$stmt\n";
$sth = $dbh->prepare($stmt);
$sth->execute(keys %accs);
while (my ($acc,$geneName) = $sth->fetchrow_array) {
    foreach my $i (@{$accs{uc($acc)}}) {
	$i->add_attribute_value_if_necessary('geneName',$geneName);
    }
}

# Gene names by Ensembl protein identifiers
$stmt = <<__END__;
SELECT u.translation_stable_id, g.display_id
FROM
$ {sp_mart_name}_gene_ensembl__xref_uniprot_accession__dm u,
$ {sp_mart_name}_gene_ensembl__gene__main g
WHERE
u.translation_stable_id IN ($tmp)
AND u.gene_id_key = g.gene_id_key
AND g.display_id IS NOT NULL
__END__
$opt_debug && print "$stmt\n";
$sth = $dbh->prepare($stmt);
$sth->execute(keys %accs);
while (my ($acc,$geneName) = $sth->fetchrow_array) {
    foreach my $i (@{$accs{uc($acc)}}) {
	$i->add_attribute_value_if_necessary('geneName',$geneName);
    }
}

# Write to db
while (my ($acc,$ar) = each %accs) {
    foreach my $i (@{$ar}) {
	print $acc, "\t", join(',',@{$i->GeneName}), "\n";
	$dba->update_attribute($i,'geneName');
    }
}

print STDERR "$0: no fatal errors.\n";

