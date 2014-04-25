#!/usr/local/bin/perl  -w

use lib "$ENV{HOME}/bioperl-1.0";
use lib "$ENV{HOME}/GKB/modules";
use GKB::Utils;
use GKB::Config;
use GKB::DBAdaptor;
use Data::Dumper;
use Getopt::Long;
use strict;

$| = 1;

$GKB::Config::NO_SCHEMA_VALIDITY_CHECK = undef;

our($opt_curatedonly,$opt_noevidencecodes,$opt_host,$opt_db,$opt_pass,$opt_port,$opt_debug,$opt_user,$opt_spname);

(@ARGV) || die "Usage: $0 -db db_name -user db_user -host db_host -pass db_pass -port db_port -curatedonly -noevidencecodes -spname\n";

&GetOptions("user:s", "host:s", "pass:s", "port:i", "debug", "db=s","curatedonly","noevidencecodes","spname");

my $dba = GKB::DBAdaptor->new
    (
     -user   => $opt_user,
     -host   => $opt_host,
     -pass   => $opt_pass,
     -port   => $opt_port,
     -dbname => $opt_db,
     -DEBUG => $opt_debug
     );

my $ref_db = $dba->fetch_instance_by_attribute('ReferenceDatabase', [['name', ['UniProt', 'SWALL', 'SPTREMBL', 'SPTR']]])->[0] ||
    die("No ReferenceDatabase with name 'UniProt', 'SWALL','SPTR' or 'SPTREMBL'.\n");
my $protein_class = &GKB::Utils::get_reference_protein_class($dba);

# For getting the immediate events
my %instructions1 = (-INSTRUCTIONS =>
		    {
			$protein_class => {'reverse_attributes' => [qw(referenceEntity)]},
			'CatalystActivity' => {'reverse_attributes' =>[qw(catalystActivity)]},
#			'PhysicalEntity' => {'reverse_attributes' => [qw(hasComponent input output physicalEntity hasMember hasCandidate regulator)]},
#			'Regulation' => {'attributes' =>[qw(regulatedEntity)]}
			'PhysicalEntity' => {'reverse_attributes' => [qw(hasComponent input output physicalEntity hasMember hasCandidate repeatedUnit)]}
		    },
		    -OUT_CLASSES => ['Event']
		    );

my $accessions =  fetch_unique_accessions($dba,$ref_db->db_id);
foreach my $acc (@{$accessions}) {
#    print "$acc\n";
    my ($ar,$species_ar) = top_level_events_with_evidence($acc,$ref_db->db_id);
    (@{$species_ar} > 1) && print STDERR "$acc\tmore than 1 species.\n";
#    print "$acc\t", scalar(@{$ar}), " top level events\n";
#    @{$ar} || print "$acc\t no associated events.\n";
#    next;
    if ($opt_curatedonly) {
	@{$ar} = grep {! $_->[1]} @{$ar};
    }
#    print "$acc\t", scalar(@{$ar}), " curated top level events\n"; next;
    @{$ar} || next;
    if ($opt_noevidencecodes) {
	map {$#$_ = 0} @{$ar};
    }
    @{$ar} = sort {($a->[0]->Name->[0] || $a->[0]->displayName) cmp ($b->[0]->Name->[0] || $b->[0]->displayName)} @{$ar};
    my $taxid_str = '';
#    if ($opt_taxid) {
#	$taxid_str = "\t" . $species_ar->[0]->CrossReference->[0]->Identifier->[0];
#    }
    my $spname = '';
    if ($opt_spname) {
	$spname = "\t" . $species_ar->[0]->displayName;
    }
    foreach my $ar2 (@{$ar}) {
	if (my $stid_i = $ar2->[0]->StableIdentifier->[0]) {
	    #my $stid_str = $stid_i->displayName;
	    my $stid_str = $stid_i->Identifier->[0];
	    print 
		"$acc\t$stid_str\t",
		$ar2->[0]->Name->[0], "\t",
		linking_url($stid_str),
		($ar2->[1] ? "\t" . $ar2->[1] : ''), "$spname\n";
	}
    }
}

sub linking_url {
    return "http://www.reactome.org/cgi-bin/eventbrowser_st_id?ST_ID=" . $_[0];
}

sub top_level_events_with_evidence {
    my ($acc,$ref_db_id) = @_;
    my %h;
    my $rpss = $dba->fetch_instance_by_attribute($protein_class, [['identifier',[$acc]],['referenceDatabase', [$ref_db_id]]]);
    foreach my $rps (@{$rpss}) {
	my $immediate_events = $rps->follow_class_attributes(%instructions1);
	foreach my $ie (@{$immediate_events}) {
	    foreach my $te (@{get_top_level_events($ie)}) {
		$h{$te->db_id}->{SELF} = $te;
		$h{$te->db_id}->{KIDS}->{$ie->db_id} = $ie;
	    }
	}
    }
    my @out;
    foreach my $te_id (keys %h) {
	if (grep {! $_->EvidenceType->[0] || ($_->EvidenceType->[0]->Name->[1] ne 'IEA')} values (%{$h{$te_id}->{KIDS}})) {
	    push @out, [$h{$te_id}->{SELF}];
	} elsif (grep {$_->EvidenceType->[0] && ($_->EvidenceType->[0]->Name->[1] eq 'IEA')} values (%{$h{$te_id}->{KIDS}})) {
	    push @out, [$h{$te_id}->{SELF}, 'IEA'];
	} else {
	    die("Unknown EvidenceType for events involving $acc:\t" . (grep {$_->EvidenceType->[0] && ($_->EvidenceType->[0]->Name->[1] ne 'IEA')} values (%{$h{$te_id}->{KIDS}}))[0]->extended_displayName);
	}
    }
    return (\@out,$rpss->[0]->Species);
}

sub top_events {
    my ($events) = @_;
    my @out;
    foreach my $e (@{$events}) {
	@{$e->reverse_attribute_value('hasEvent')} && next;
#	@{$e->reverse_attribute_value('hasMember')} && next;
	push @out, $e;
    }
    
    # Grab all pathways
    @out = grep {$_->is_a('Pathway')} @out;
    return \@out;
}

sub get_top_level_events {
    my $event = shift;
    return top_events($event->follow_class_attributes(-INSTRUCTIONS => {'Event' => {'reverse_attributes' =>[qw(hasEvent)]}},
						      -OUT_CLASSES => ['Event']));
}

sub fetch_unique_accessions {
    my ($dba,$ref_db_id) = @_;
    my $query = qq{SELECT DISTINCT(identifier) FROM ReferenceEntity WHERE referenceDatabase=$ref_db_id};
    my ($sth,$res) = $dba->execute($query);
    my @out;
    while (my $ar = $sth->fetchrow_arrayref) {
	push @out, uc($ar->[0]);
    }
    print STDERR "Got ", scalar(@out), " unique accession numbers.\n";
    return \@out;
}
