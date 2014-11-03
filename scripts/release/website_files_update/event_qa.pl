#!/usr/local/bin/perl  -w
use strict;

# I think it's more user-friendly if the user will not have to practise any
# symlink tricks. Hence the BEGIN block. If the location of the script or
# libraries changes, this will have to be changed.

BEGIN {
    my @a = split('/',$0);
    pop @a;
    push @a, ('..','..','..');
    my $libpath = join('/', @a);
    unshift (@INC, "$libpath/modules");
    $ENV{PATH} = "$libpath/scripts:$libpath/scripts/release:" . $ENV{PATH};
}

use GKB::Config;
use GKB::DBAdaptor;

use autodie;
use Getopt::Long;


our($opt_host,$opt_db,$opt_pass,$opt_port,$opt_debug,$opt_user);

&GetOptions("user:s",
"host:s",
"pass:s",
"port:i",
"debug",
"db=s");

$opt_db || die "Usage: $0 -db db_name [-user db_user -host db_host -pass db_pass -port db_port]";
$opt_host ||= $GK_DB_HOST;
$opt_user ||= $GK_DB_USER;
$opt_pass ||= $GK_DB_PASS;
$opt_port ||= $GK_DB_PORT;

my $dba = GKB::DBAdaptor->new
    (
     -user   => $opt_user,
     -host   => $opt_host,
     -pass   => $opt_pass,
     -port   => $opt_port,
     -dbname => $opt_db,
     -DEBUG => $opt_debug
     );



my %seen; # Prevents duplication in the file
open(my $output, ">", "event_qa_$opt_db.out");
	

my $ar = $dba->fetch_instance(-CLASS => 'Pathway'); # Obtains a reference to the array of all Reactome pathways

# Every pathway in reactome is processed
foreach my $pathway (@{$ar}) {
	foreach my $event ($pathway, @{get_events($pathway)}) {
		next if $seen{$event->db_id}++;
		
		if ($event->species->[1] && !$event->reverse_attribute_value('inferredFrom')) {
			report($event, $output, 'More than 1 species for event: ');
			next;
		}
		report($event, $output, 'Human pathway has non human event: ') if ($pathway->species->[0]->name->[0] eq "Homo sapiens" && $event->species->[0]->name->[0] ne "Homo sapiens"); 
	}
}


sub get_events {
	my $event = shift;
	return ($event->follow_class_attributes(-INSTRUCTIONS => {'Event' => {'attributes' =>[qw(hasEvent)]}},
						      -OUT_CLASSES => ['Event']));
}

sub report {
	my $event = shift;
	my $output = shift;
	my $message = shift;
	
	print $output $message . $event->db_id . '-' . $event->_displayName->[0] . '-' . join(",", map($_->_displayName->[0], (@{$event->species}))) . "\n";
}
