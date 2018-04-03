#!/usr/local/bin/perl
use strict;
use warnings;

use lib "/usr/local/gkb/modules";

use GKB::DBAdaptor;
use GKB::Utils;
use Data::Dumper;
use Getopt::Long;
use Unicode::CaseFold;

our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db,$opt_debug,$opt_sp);

(@ARGV) || die "Usage: $0 -sp 'species name' -user db_user -host db_host -pass db_pass -port db_port -db db_name\n";

&GetOptions("user:s", "host:s", "pass:s", "port:i", "db:s", "debug", "sp=s");

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


# Fetch ReferenceGeneProducts to be "annotated".
my $protein_class = &GKB::Utils::get_reference_protein_class($dba);
my $rpss = $dba->fetch_instance_by_attribute($protein_class,[['species',[$sp->db_id]]]);
# Load the values of an attribute to be updated. Not necessary for the 1st time though.
$dba->load_class_attribute_values_of_multiple_instances($protein_class,'otherIdentifier',$rpss);

my %ensp_to_enst;
my %enst_to_ids;
my %uniprot_to_ensp;
opendir(my $dir, 'output');
while(my $resource = readdir($dir)) {
    next unless $resource =~ /^$sp_mart_name/;
    
    my $prefix = get_prefix($resource);
    open(my $resource_file, '<', "output/$resource");
    while (my $line = <$resource_file>) {
	chomp $line;
	my @identifiers = split /\t/, $line;
	my $gene = shift @identifiers;
	my $transcript = shift @identifiers;
	my $protein = shift @identifiers;
	my $id = shift @identifiers;
	$id = $prefix . $id if $id;
	
	next unless $transcript;
	$ensp_to_enst{$protein} = $transcript if $protein;
	$enst_to_ids{$transcript}{$id}++ if $id;
	$enst_to_ids{$transcript}{$gene}++ if $gene;
	$enst_to_ids{$transcript}{$protein}++ if $protein;
	push @{$uniprot_to_ensp{$id}}, $protein if $resource =~ /sptrembl|swissprot/ && $id;
    }
    close $resource_file;
}
closedir $dir;

my %accs;
map {push @{$accs{uc($_->Identifier->[0])}},$_} @{$rpss};

foreach my $rpg_id (keys %accs) {
    foreach my $i (@{$accs{uc($rpg_id)}}) {
	eval {
	    my $ref_db = $i->referenceDatabase->[0]->name->[0];
	    
	    no warnings 'uninitialized';
	    my @ensp;
	    if ($ref_db =~ /ensembl/i) {
		push @ensp, $rpg_id;
	    } elsif ($ref_db =~ /uniprot/i) {
		@ensp = @{$uniprot_to_ensp{$rpg_id}};
	    }
	
	    foreach my $ensp (@ensp) {
		my $enst = $ensp_to_enst{$ensp};
		my $ids = $enst_to_ids{$enst};
		delete $ids->{$rpg_id};
    
		$i->add_attribute_value_if_necessary('otherIdentifier', $enst, keys %$ids);
	    }
	};
    }
}

while (my ($acc,$ar) = each %accs) {
    foreach my $i (@{$ar}) {
	my @otherIdentifiers = sort {fc($a) cmp fc($b)} grep {defined} @{$i->OtherIdentifier};
	$i->OtherIdentifier(undef);
	$i->OtherIdentifier(@otherIdentifiers);
	
	print $acc, "\t", join(',',@otherIdentifiers), "\n";
	$dba->update_attribute($i,'otherIdentifier');
    }
}

print "$0: no fatal errors.\n";

sub get_prefix {
    my $resource = shift;
    
    if ($resource =~ /_locuslink/i) {
	return 'LocusLink';
    } elsif ($resource =~ /_entrezgene/i) {
	return 'EntrezGene:';
    } elsif ($resource =~ /_mim/i) {
	return 'MIM:';
    }
    
    return '';
}
