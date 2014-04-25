#!/usr/local/bin/perl -w

# Make sure you don't have "competing" libraries...
# for use @CSHL
use lib "/usr/local/reactomes/Reactome/development/GKB/modules";
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

# Check for the presence of relevant tables in martdb
$stmt = qq(SHOW TABLES LIKE "$ {sp_mart_name}_gene_ensembl__xref%");
$opt_debug && print "$stmt\n";
$sth = $dbh->prepare($stmt);
$res = $sth->execute || die $sth->errstr;
my (@tables,@tables2);
while (my ($tbl_name) = $sth->fetchrow_array) {
    next if ($tbl_name =~ /(_gkb__|_go__)/);
    push @tables, $tbl_name;
}
$stmt = qq(SHOW TABLES LIKE "$ {sp_mart_name}_gene_ensembl__prot_interpro__dm");
$opt_debug && print "$stmt\n";
$sth = $dbh->prepare($stmt);
$res = $sth->execute || die $sth->errstr;
while (my ($tbl_name) = $sth->fetchrow_array) {
    push @tables2, $tbl_name;
}
@tables || @tables2 || die "No mart tables for '$sp_mart_name'.\n";

# Fetch ReferenceGeneProducts to be "annotated".
my $protein_class = &GKB::Utils::get_reference_protein_class($dba);
my $rpss = $dba->fetch_instance_by_attribute($protein_class,[['species',[$sp->db_id]]]);
# Load the values of an attribute to be updated. Not necessary for the 1st time though.
$dba->load_class_attribute_values_of_multiple_instances($protein_class,'otherIdentifier',$rpss);

my %accs;
map {push @{$accs{uc($_->Identifier->[0])}},$_} @{$rpss};
my $tmp = join(',', (('?') x scalar(keys %accs)));

# Ensembl identifiers by UniProt accessions
print STDERR "Ensembl identifiers by UniProt accessions\n";
$stmt = <<__END__;
SELECT dbprimary_id, gene_stable_id, transcript_stable_id, translation_stable_id
FROM
$ {sp_mart_name}_gene_ensembl__xref_uniprot_accession__dm
WHERE
dbprimary_id IN ($tmp)
__END__
$opt_debug && print "$stmt\n";
$sth = $dbh->prepare($stmt);
$sth->execute(keys %accs);
while (my ($acc,@ids) = $sth->fetchrow_array) {
    foreach my $i (@{$accs{uc($acc)}}) {
	$i->add_attribute_value_if_necessary('otherIdentifier',@ids);
    }
}

# UniProt accessions by ENSP
print STDERR "UniProt accessions by ENSP\n";
$stmt = <<__END__;
SELECT translation_stable_id, dbprimary_id
FROM
$ {sp_mart_name}_gene_ensembl__xref_uniprot_accession__dm
WHERE
translation_stable_id IN ($tmp)
AND dbprimary_id IS NOT NULL
__END__
$opt_debug && print "$stmt\n";
$sth = $dbh->prepare($stmt);
$sth->execute(keys %accs);
while (my ($acc,@ids) = $sth->fetchrow_array) {
    foreach my $i (@{$accs{uc($acc)}}) {
	$i->add_attribute_value_if_necessary('otherIdentifier',@ids);
    }
}

# ENSGs and ENSTs by ENSP
print STDERR "ENSGs and ENSTs by ENSP\n";
$stmt = <<__END__;
SELECT translation_stable_id, gene_stable_id, transcript_stable_id
FROM
$ {sp_mart_name}_gene_ensembl__transcript__main
WHERE
translation_stable_id in ($tmp)

__END__
$opt_debug && print "$stmt\n";
$sth = $dbh->prepare($stmt);
$sth->execute(keys %accs);
while (my ($acc,@ids) = $sth->fetchrow_array) {
    foreach my $i (@{$accs{uc($acc)}}) {
	$i->add_attribute_value_if_necessary('otherIdentifier',@ids);
    }
}


# Others
print STDERR "Others\n";
foreach my $tbl (@tables) {
    # Starting from UniProt
    $stmt = <<__END__;
SELECT A.dbprimary_id, B.dbprimary_id
FROM
$ {sp_mart_name}_gene_ensembl__xref_uniprot_accession__dm A,
$tbl B
WHERE
A.gene_id_key = B.gene_id_key AND
A.dbprimary_id IN ($tmp)
AND B.dbprimary_id IS NOT NULL
__END__
    $opt_debug && print "$stmt\n";
    $sth = $dbh->prepare($stmt);
    $sth->execute(keys %accs);
    my $flag;
    if ($tbl =~ /protein_id/) {
	$flag = 1;
    }
    my $prefix = get_prefix($tbl);
    while (my ($acc,@ids) = $sth->fetchrow_array) {
	foreach my $id (grep {defined $_} @ids) {
	    if ("$prefix$id" =~ /^\d+$/) {
		print "Skipping purely numeric identifier $id for $acc from $tbl.\n";
		next;
	    }
	    foreach my $i (@{$accs{uc($acc)}}) {
		$i->add_attribute_value_if_necessary('otherIdentifier',"$prefix$id");
		if ($flag) {
		    $id =~ s/\.\d+$//;
		    $i->add_attribute_value_if_necessary('otherIdentifier',$id);
		}
	    }
	}
    }
    # Starting from ENSP
    $stmt = <<__END__;
SELECT A.translation_stable_id, B.dbprimary_id
FROM
$ {sp_mart_name}_gene_ensembl__transcript__main A,
$tbl B
WHERE
A.gene_id_key = B.gene_id_key AND
A.translation_stable_id IN ($tmp)
AND B.dbprimary_id IS NOT NULL
__END__
    $opt_debug && print "$stmt\n";
    $sth = $dbh->prepare($stmt);
    $sth->execute(keys %accs);
    $flag = undef;
    if ($tbl =~ /protein_id/) {
	$flag = 1;
    }
    while (my ($acc,@ids) = $sth->fetchrow_array) {
#	foreach my $i (@{$accs{uc($acc)}}) {
#	    foreach my $id (grep {defined $_} @ids) {
#		if ("$prefix$id" =~ /^\d+$/) {
#		    print "Skipping purely numeric identifier $id for $acc from $tbl.\n";
#		    next;
#		}
#		$i->add_attribute_value_if_necessary('otherIdentifier',"$prefix$id");
#		if ($flag) {
#		    $id =~ s/\.\d+$//;
#		    $i->add_attribute_value_if_necessary('otherIdentifier',$id);
#		}
#	    }
#	}
	foreach my $id (grep {defined $_} @ids) {
	    if ("$prefix$id" =~ /^\d+$/) {
		print "Skipping purely numeric identifier $id for $acc from $tbl.\n";
		next;
	    }
	    foreach my $i (@{$accs{uc($acc)}}) {
		$i->add_attribute_value_if_necessary('otherIdentifier',"$prefix$id");
		if ($flag) {
		    $id =~ s/\.\d+$//;
		    $i->add_attribute_value_if_necessary('otherIdentifier',$id);
		}
	    }
	}
    }
}

# Interprot
print STDERR "Interprot\n";
foreach my $tbl (@tables2) {
    my ($colname) = $tbl =~ /prot_(\w+?)__dm$/;
    $colname .= '_list';
    # Starting from UniProt
    $stmt = <<__END__;
SELECT A.dbprimary_id, B.$colname
FROM
$ {sp_mart_name}_gene_ensembl__xref_uniprot_accession__dm A,
$tbl B
WHERE
A.transcript_id_key = B.transcript_id_key AND
A.dbprimary_id IN ($tmp)
AND B.$colname IS NOT NULL
__END__
    $opt_debug && print "$stmt\n";
    $sth = $dbh->prepare($stmt);
    $sth->execute(keys %accs);
    my $prefix = get_prefix($tbl);
    while (my ($acc,$id) = $sth->fetchrow_array) {
	if ("$prefix$id" =~ /^\d+$/) {
	    print "Skipping purely numeric identifier $id for $acc from $tbl.\n";
	    next;
	}
	foreach my $i (@{$accs{uc($acc)}}) {
	    $i->add_attribute_value_if_necessary('otherIdentifier',"$prefix$id");
	}
    }
    # Starting from ENSP
    $stmt = <<__END__;
SELECT A.translation_stable_id, B.$colname
FROM
$ {sp_mart_name}_gene_ensembl__transcript__main A,
$tbl B
WHERE
A.transcript_id_key = B.transcript_id_key AND
A.translation_stable_id IN ($tmp)
AND B.$colname IS NOT NULL
__END__
    $opt_debug && print "$stmt\n";
    $sth = $dbh->prepare($stmt);
    $sth->execute(keys %accs);
    while (my ($acc,$id) = $sth->fetchrow_array) {
	if ("$prefix$id" =~ /^\d+$/) {
	    print "Skipping purely numeric identifier $id for $acc from $tbl.\n";
	    next;
	}
	foreach my $i (@{$accs{uc($acc)}}) {
	    $i->add_attribute_value_if_necessary('otherIdentifier',"$prefix$id");
	}
    }
}


while (my ($acc,$ar) = each %accs) {
    foreach my $i (@{$ar}) {
	print $acc, "\t", join(',',@{$i->OtherIdentifier}), "\n";
	$dba->update_attribute($i,'otherIdentifier');
    }
}

print STDERR "$0: no fatal errors.\n";

sub get_prefix {
    my ($tblname) = shift;
    if ($tblname =~ /_locuslink_/i) {
	return 'LocusLink:';
    } elsif ($tblname =~ /_entrezgene_/i) {
	return 'EntrezGene:';
    } elsif ($tblname =~ /_mim_/i) {
	return 'MIM:';
    }
    return '';
}
