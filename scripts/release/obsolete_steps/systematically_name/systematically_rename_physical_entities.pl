#!/usr/local/bin/perl  -w
use strict;

use lib "/usr/local/gkb/modules";

use GKB::Config;
use GKB::DBAdaptor;
use GKB::Utils_esther;

use autodie;
use Data::Dumper;
use Getopt::Long;

@ARGV || die "Usage: $0 -user db_user -host db_host -pass db_pass -port db_port -db db_name -class class";

our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db,$opt_debug,$opt_class);

&GetOptions("user:s",
	    "host:s",
	    "pass:s",
	    "port:i",
	    "db=s",
	    "debug",
	    "class=s",
	    );

$opt_db || die "Need database name (-db).\n";

my $dba = GKB::DBAdaptor->new
    (
     -dbname => $opt_db,
     -user   => $opt_user || $GKB::Config::GK_DB_USER,
     -host   => $opt_host,
     -pass   => $opt_pass || $GKB::Config::GK_DB_PASS,
     -port   => $opt_port,
     -driver => 'mysql',
     -DEBUG => $opt_debug
     );
    
## Prepare date and author for instanceEdit:
my $surname = 'Weiser';
my $initial = 'JD';
my $date    = `date \+\%F`;
chomp $date;

my $instance_edit = GKB::Utils_esther::create_instance_edit( $dba, $surname, $initial, $date );

my $ar = $dba->fetch_instance(-CLASS => 'PhysicalEntity');

open(my $gee_fh, '>', 'GEEsWithoutReferenceEntities.txt');
(my $outfile = $0) =~ s/pl$/txt/;
open(my $out_fh, '>', $outfile);
foreach my $pe (@{$ar}) {
    next if $pe->is_a('SimpleEntity') || $pe->is_a('OtherEntity');
    next unless $pe->species->[0] && $pe->species->[0]->name->[0] =~ /homo sapiens/i;
    
    if ($pe->is_a('GenomeEncodedEntity') && !$pe->referenceEntity->[0]) {
	print $gee_fh $pe->db_id . "\t" . $pe->name->[0] . "\n";
	next;
    } 
    
    print $out_fh $pe->db_id . "\t" . $pe->name->[0] . "\t" . get_systematic_name($pe) . "\n";
}
close $gee_fh;
close $out_fh;

sub get_systematic_name {
    my $pe = shift;
    
    if ($pe->is_a('Complex')) {
	return get_complex_systematic_name($pe);
    } elsif ($pe->is_a('EntitySet')) {
	return get_set_systematic_name($pe);
    } elsif ($pe->is_a('Polymer')) {
	return get_polymer_systematic_name($pe);
    } else {
	return get_systematic_name_from_name_slot($pe);
    }
}

sub get_complex_systematic_name {
    my $complex = shift;
    
    my @component_systematic_names = map {get_systematic_name($_)} (@{$complex->hasComponent});
    
    return '[' . join(':', @component_systematic_names) . ']';
}

sub get_set_systematic_name {
    my $set = shift;
    
    my $set_systematic_name = '[';
    
    my @member_systematic_names = map {get_systematic_name($_)} (@{$set->hasMember});
    $set_systematic_name .= join '/', @member_systematic_names;
    
    if ($set->hasCandidate->[0]) {
	$set_systematic_name .= '/' if @member_systematic_names;
	
	my @candidate_systematic_names = map {get_systematic_name($_)} (@{$set->hasCandidate});
	$set_systematic_name .= '(' . join('/', @candidate_systematic_names) . ')';
    }
    
    $set_systematic_name .= ']';
    
    return $set_systematic_name;
}

sub get_polymer_systematic_name {
    my $polymer = shift;
    
    my @repeated_unit_systematic_names = map {get_systematic_name($_)} (@{$polymer->repeatedUnit});
    
    return join('+', @repeated_unit_systematic_names) . ' polymer';
}

sub get_systematic_name_from_name_slot {
    my $pe = shift;
    
    return $pe->name->[0];
}