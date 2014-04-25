#!/usr/local/bin/perl

# Make sure you don't have "competing" libraries...
# for use @CSHL
use lib "/usr/local/reactomes/Reactome/development/GKB/modules";
# for use @HOME
use lib "$ENV{HOME}/my_perl_stuff";
use lib "$ENV{HOME}/bioperl-1.0";
use lib "$ENV{HOME}/GKB/modules";

use GKB::DBAdaptor;
use GKB::Utils;
use Data::Dumper;
use Getopt::Long;
use strict;

our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_dbfrom,$opt_dbto,$opt_debug);

(@ARGV) || die "Usage: $0 -user db_user -host db_host -pass db_pass -port db_port -dbto db1 -dbfrom db2 -debug\n";

&GetOptions("user:s", "host:s", "pass:s", "port:i", "dbfrom:s", "dbto:s", "debug");

print "opt_dbfrom=$opt_dbfrom, opt_dbto=$opt_dbto\n";

($opt_dbfrom && $opt_dbto) || die "Need database names (-dbfrom -dbto).\n";    

my $dbafrom = GKB::DBAdaptor->new
    (
     -user   => $opt_user,
     -host   => $opt_host,
     -pass   => $opt_pass,
     -port   => $opt_port,
     -dbname => $opt_dbfrom,
     -DEBUG => $opt_debug
     );

my $dbato = GKB::DBAdaptor->new
    (
     -user   => $opt_user,
     -host   => $opt_host,
     -pass   => $opt_pass,
     -port   => $opt_port,
     -ontology => $dbafrom->ontology,
     -DEBUG => $opt_debug
     );

$dbato->create_database($opt_dbto);

my $stmt1 = "SHOW TABLES";
my ($sth,$res) = $dbafrom->execute($stmt1);
while (my $ar = $sth->fetchrow_arrayref) {
    my $tblname = $ar->[0];
    (lc($tblname) eq 'ontology') && next;
    (lc($tblname) eq 'schema') && next;
    (lc($tblname) eq 'release_info') && next;
    my @cols = get_table_columns($dbato,$tblname);
    print "$tblname\n";
    my $stmt2 =
	"INSERT INTO $opt_dbto.$tblname ("
	. join(",",@cols)
	. ") SELECT "
	. join(",",@cols)
	. " FROM $opt_dbfrom.$tblname";
    print "$stmt2\n";
    $dbafrom->execute($stmt2);
}

print "innodb2myisam.pl has finished its job\n";

sub get_table_columns {
    my ($dba,$tbl) = @_;
    my $q = qq(DESCRIBE $tbl);
    my ($sth,$res) = $dba->execute($q);
    my @out;
    while (my $h = $sth->fetchrow_hashref) {
	push @out, $h->{'Field'};
    }
    return @out;
}
