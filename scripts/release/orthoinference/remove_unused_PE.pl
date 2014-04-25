#!/usr/local/bin/perl  -w

# Make sure you don't have "competing" libraries...
# for use @CSHL
use lib "/usr/local/gkb/modules";
# for use @HOME
use lib "$ENV{HOME}/bioperl-1.0";
use lib "$ENV{HOME}/GKB/modules";

use GKB::Instance;
use GKB::DBAdaptor;
use Data::Dumper;
use Getopt::Long;
use strict;

our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db,$opt_debug);

(@ARGV) || die "Usage: $0 -user db_user -host db_host -pass db_pass -port db_port -db db_name -debug\n";

&GetOptions("user:s", "host:s", "pass:s", "port:i", "db:s", "debug");

$opt_db || die "Need database name (-db).\n";


my $dba = GKB::DBAdaptor->new
    (
     -user   => $opt_user || '',
     -host   => $opt_host,
     -pass   => $opt_pass,
     -port   => $opt_port,
     -dbname => $opt_db,
     -DEBUG => $opt_debug
     );
open(FILE, ">deleted_unused_pe_$opt_db\.txt");
my $MAX_RECURSION_DEPTH = 50;
my %test;
my $ar = $dba->fetch_instance(-CLASS => 'PhysicalEntity');
my ($total, $deleted);
foreach my $i (@{$ar}) {
    next if ($i->is_a('SimpleEntity') || $i->is_a('OtherEntity'));#they don't have species and are not created by the ortho script
    $total++;
#    print "____________________________________________\n";
    my $test = check_for_referers($i, 0);
    next if $test;
    $deleted++;
    print FILE $i->extended_displayName, "\n";
    $dba->delete_by_db_id($i->db_id);
}
print FILE $total, " PhysicalEntities,", $deleted, " have been deleted.\n";

close(FILE);

sub check_for_referers {
    my ($i, $recursion_depth) = @_;
    if ($recursion_depth == $MAX_RECURSION_DEPTH) {
    	print "Recursion depth limit exceeded for " . $i->_displayName->[0] . ", bailing out\n";
    	$test{$i} = 1;
    	return 1;
    }
    print $i->extended_displayName, "\n";
    $test{$i} && return $test{$i};
    my $rr = $dba->fetch_referer_by_instance($i);
    foreach my $r (@{$rr}) {
	print "\t", $r->extended_displayName, "\n";
	if ($r->is_a('PhysicalEntity') || $r->is_a('CatalystActivity')) { #these may have been created during the run of the ortho script even as they may not have been used by any reaction
	    print "********* recursion_depth=$recursion_depth\t";
	    $test{$r} = check_for_referers($r, $recursion_depth + 1);
	    next unless $test{$r};
	}
	$test{$i} = 1;
	return 1; #no need to check further referers
    }
    $test{$i} = 0;
    return;
}
