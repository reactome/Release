#!/usr/local/bin/perl -w

BEGIN {
    my ($path) = $0 =~ /^(\S+)$/;
    my @a = split('/',$path);
    pop @a;
    if (@a && !$a[0]) {
        $#a = $#a - 2;
    } else {
        push @a, ('..','..','..');
    }
    push @a, 'modules';
    my $libpath = join('/', @a);
    unshift (@INC, $libpath);
}

use strict;
use Getopt::Long;
use GKB::Utils;
use GKB::Instance;
use GKB::Config;
use Data::Dumper;

$GKB::Config::NO_SCHEMA_VALIDITY_CHECK = undef;

@ARGV || die("Usage: $0 -db db_name ...\n");

my @params = @ARGV;

our ($opt_user, $opt_host, $opt_pass, $opt_port, $opt_db, $opt_debug);

&GetOptions("user:s", "host:s", "pass:s", "port:i", "db:s", "debug");

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

my @species = $dba->species_for_ref_dbs('ENSEMBL','UniProt');
print STDERR "My species:\n",Dumper \@species;

my @cmds = (qq(./retrieve_indirectIdentifiers_from_mart.pl @params),
	    qq(./indirectIdentifiers_from_mart.pl @params),
	    qq(./gene_names_from_mart.pl @params));

foreach my $sp (@species) {
    foreach my $cmd (@cmds) {
		my $tmp = "$cmd -sp '$sp'";
		print STDERR "Command to be run: $tmp\n";
		system($tmp) == 0 or print "$tmp failed.\n";
    }
}

print STDERR "$0: no fatal errors.\n";

sub ref_db_ids {
    my $dba = shift;
    my @rdbs = @_;
    my $query = 'SELECT DB_ID FROM DatabaseObject WHERE _displayName = ? ' .
	'AND _Protege_id is NOT NULL';

    my $sth = $dba->prepare($query);
    my @out;
    for my $rdb (@rdbs) {
	$sth->execute($rdb);
        while (my $id = $sth->fetchrow_arrayref) {
            push @out, $id->[0];
	}
    }
    return @out;
}


sub species_for_ref_dbs {
    my $dba  = shift;
    my @rdbs = @_;

    my $query = <<"END";
    SELECT DISTINCT(do._displayName)
    FROM ReferenceEntity re, ReferenceSequence rs, DatabaseObject do
    WHERE re.referenceDatabase = ?
    AND do.DB_ID = rs.species
    AND rs.DB_ID = re.DB_ID
END
;

    my $sth = $dba->prepare($query);
    my @out;
    for my $rdb (@rdbs) {
        $sth->execute($rdb);
        while (my $id = $sth->fetchrow_arrayref) {
	    push @out, $id->[0];
	}
    }

    # unique species names
    my %out = map {$_ => 1} @out;

    return sort keys %out;
}


