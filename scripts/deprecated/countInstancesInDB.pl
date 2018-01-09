#!/usr/local/bin/perl  -w

# Counts instances of the instance classes specified on
# the command line.

use lib "$ENV{HOME}/bioperl-1.0";
use lib "$ENV{HOME}/GKB/modules";
use GKB::DBAdaptor;
use Data::Dumper;
use Getopt::Long;
use strict;

@ARGV || die "Usage: $0 -user db_user -host db_host -pass db_pass -port db_port -db db_name -classes class1[,class2,...]";

our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db,$opt_debug,$opt_classes);

&GetOptions("user:s",
	    "host:s",
	    "pass:s",
	    "port:i",
	    "db=s",
	    "debug",
	    "classes=s",
	    );

$opt_db || die "Need database name (-db).\n";
$opt_classes || die "Need at least one class (-classes).\n";

my $dba = GKB::DBAdaptor->new
    (
     -dbname => $opt_db,
     -user   => $opt_user,
     -host   => $opt_host,
     -pass   => $opt_pass,
     -port   => $opt_port,
     -DEBUG => $opt_debug
     );

my @classes = split(/,/, $opt_classes);
my $db_ids_ref;
my @db_ids;
my $total_count = 0;
my $class_count;
foreach my $class (@classes) {
    $db_ids_ref = $dba->fetch_db_ids_by_class($class);
    @db_ids = @{$db_ids_ref};
    $class_count = scalar(@db_ids);

    print STDERR "Number of instances for $class is: $class_count\n";
    
    $total_count += $class_count;
}

print STDERR "Total number of instances is: $total_count\n";