#!/usr/local/bin/perl  -w

use lib "$ENV{HOME}/bioperl-1.0";
use lib "$ENV{HOME}/GKB/modules";
use GKB::DBAdaptor;
use Data::Dumper;
use Getopt::Long;
use strict;

@ARGV || die "Usage: $0 -user db_user -host db_host -pass db_pass -port db_port -db db_name\n";

our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db,$opt_debug);

&GetOptions("user:s",
	    "host:s",
	    "pass:s",
	    "port:i",
	    "db=s",
	    "debug",
	    );

$opt_db || die "Need database name (-db).\n";

my $dba = GKB::DBAdaptor->new
    (
     -dbname => $opt_db,
     -user   => $opt_user,
     -host   => $opt_host,
     -pass   => $opt_pass,
     -port   => $opt_port,
     -DEBUG => $opt_debug
     );

my $o = $dba->ontology;
my $root_cls_name = $o->root_class;
my ($sth,$res) = $dba->execute(qq(SELECT COUNT(*) FROM  $root_cls_name WHERE _class IS NULL));
my $count = $sth->fetchrow_arrayref->[0];
if ($count > 0) {
    print "DatabaseObject\t_class\t$count\n";
}

foreach my $cls ($o->list_classes) {
    foreach my $att (grep {$o->is_instance_type_class_attribute($cls,$_)} $o-> list_own_attributes($cls)) {
	my $table_name = ($o->is_multivalue_class_attribute($cls,$att)) ?
	    "$ {cls}_2_$ {att}" : $cls;
	($sth,$res) = $dba->execute(qq(SELECT COUNT(*) FROM  $table_name WHERE $ {att}_class IS NULL AND $att IS NOT NULL));
	$count = $sth->fetchrow_arrayref->[0];
	if ($count > 0) {
	    print "$cls\t$att\t$count\n";
	}
    }
}

