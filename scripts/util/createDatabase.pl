#!/usr/local/bin/perl -w

BEGIN {
    my @a = split('/',$0);
    pop @a;
    push @a, ('..','modules');
    my $libpath = join('/', @a);
    unshift (@INC, $libpath);
}

# for use @HOME
use lib "$ENV{HOME}/bioperl-1.0";
use lib "$ENV{HOME}/GKB/modules";



use GKB::ClipsAdaptor;
use GKB::DBAdaptor;
use Data::Dumper;
use Getopt::Long;
use strict;

our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db,$opt_debug,$opt_drop,$opt_type);

&GetOptions("user:s", "host:s", "pass:s", "port:i", "db:s", "debug", "drop", "type:s");

@ARGV || die "Usage: $0 project.pprj -user db_user -host db_host -pass db_pass -port db_port -db db_name -debug";

$opt_db || die "Need database name (-db).\n";    

my $ca = GKB::ClipsAdaptor->new(-FILE => $ARGV[0], -DEBUG => $opt_debug);

$ca->attach_pins_file_stub;

my $o = $ca->ontology;

my $dba = GKB::DBAdaptor->new
    (
     -user   => $opt_user || '',
     -host   => $opt_host,
     -pass   => $opt_pass,
     -port   => $opt_port,
     -ontology => $o,
     -table_type => $opt_type,
     -DEBUG => $opt_debug
     );

$opt_drop && $dba->execute("DROP DATABASE IF EXISTS $opt_db");
$dba->create_database($opt_db);







