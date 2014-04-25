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

our($opt_curatedonly,$opt_compressed,$opt_noevidencecodes,$opt_host,$opt_db,$opt_pass,$opt_port,$opt_debug,$opt_user,$opt_spname);

(@ARGV) || die "Usage: $0 -db db_name -user db_user -host db_host -pass db_pass -port db_port -curatedonly -compressed -noevidencecodes -spname\n";

&GetOptions("user:s", "host:s", "pass:s", "port:i", "debug", "db=s","curatedonly","compressed","noevidencecodes","spname");

my $dba = GKB::DBAdaptor->new
    (
     -user   => $opt_user,
     -host   => $opt_host,
     -pass   => $opt_pass,
     -port   => $opt_port,
     -dbname => $opt_db,
     -DEBUG => $opt_debug
     );

print STDERR "Starting\n";

my $ref_db = $dba->fetch_instance_by_attribute('ReferenceDatabase', [['name', ['UniProt', 'SWALL', 'SPTREMBL', 'SPTR']]])->[0] ||
    die("No ReferenceDatabase with name 'UniProt', 'SWALL','SPTR' or 'SPTREMBL'.\n");
my $protein_class = &GKB::Utils::get_reference_protein_class($dba);
my $LINK_URL = "http://www.reactome.org/cgi-bin/link?SOURCE=UniProt&ID=";

# For getting the immediate events
my %instructions1 = (-INSTRUCTIONS =>
		    {
			$protein_class => {'reverse_attributes' => [qw(referenceEntity)]},
			'CatalystActivity' => {'reverse_attributes' =>[qw(catalystActivity)]},
#			'PhysicalEntity' => {'reverse_attributes' => [qw(hasComponent input output physicalEntity hasMember hasCandidate regulator)]},
#			'Regulation' => {'attributes' =>[qw(regulatedEntity)]}
			'PhysicalEntity' => {'reverse_attributes' => [qw(hasComponent input output physicalEntity hasMember hasCandidate)]}
		    },
		    -OUT_CLASSES => ['Event']
		    );

my $accessions =  fetch_unique_accessions($dba,$ref_db->db_id);
my $previous_acc = "No previous acc";

my $acc_count = scalar(@{$accessions});
my $acc_num = 0;
my $boiling_flag = 0;
my $drilling_flag = 0;
print STDERR "Looping over $acc_count accessions\n";

foreach my $acc (@{$accessions}) {
	if ($boiling_flag || $acc_num%100 == 0) {
    	print STDERR "$0: acc=$acc, acc_num=$acc_num (" . (100.0*$acc_num)/$acc_count . "%)\n";
	}
	if ($acc eq "P54310") {
		$drilling_flag = 1;
	}
	if ($drilling_flag) {
		print STDERR "$0: about to do a top_level_events_with_evidence\n";
	}
    my ($ar,$species_ar) = top_level_events_with_evidence($acc,$ref_db->db_id);
    (@{$species_ar} > 1) && print STDERR "$acc\tmore than 1 species.\n";
#    print "$acc\t", scalar(@{$ar}), " top level events\n";
#    @{$ar} || print "$acc\t no associated events.\n";
#    next;
	if ($drilling_flag) {
		print STDERR "$0: about to grep for something\n";
	}
    if ($opt_curatedonly) {
		@{$ar} = grep {! $_->[1]} @{$ar};
    }
	if ($boiling_flag || $acc =~ /O88521/ || $previous_acc =~ /O88521/) {
	    print STDERR "$0: ", scalar(@{$ar}), " curated top level events\n";
	    $boiling_flag = 1;
	}
    @{$ar} || next;
	if ($drilling_flag) {
		print STDERR "$0: about to map something\n";
	}
    if ($opt_noevidencecodes) {
		map {$#$_ = 0} @{$ar};
    }
	if ($drilling_flag) {
		print STDERR "$0: about to sort something\n";
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
    if ($opt_compressed) {
		my $tmp = '';
		if (@{$ar} > 1) {
		    $tmp = '[' . scalar(@{$ar}) . ' processes]: ';
		}
		if ($drilling_flag) {
			print STDERR "$0: about to join\n";
		}
		my $name = join('; ', map {$_->[0]->Name->[0] . ($_->[1] ? ' ' . $_->[1] : '')} @{$ar});
		my $new_name = $name;
		$new_name =~ s/[^ -\~]//g;
		if ($boiling_flag || $acc =~ /O88521/ || $previous_acc =~ /O88521/) {
			print STDERR "$0: printing compressed acc=|$acc|\n";
			print STDERR "$0: printing compressed tmp=|$tmp|\n";
			print STDERR "$0: printing compressed spname=|$spname|\n";
			print STDERR "$0: printing compressed name=|$name|\n";
			print STDERR "$0: printing compressed new_name=|$new_name|\n";
			if (!($new_name eq $name)) {
				print STDERR "$0: printing compressed name != new_name\n";
			}
		}
#		print 
#		    "$acc\tUniProt:$acc\t$tmp",
#		    join('; ', map {$_->[0]->Name->[0] . ($_->[1] ? ' ' . $_->[1] : '')} @{$ar}), "\t", 
#		    $LINK_URL . $acc, "$spname\n";
		print "$acc\tUniProt:$acc\t$tmp$new_name\t$LINK_URL$acc$spname\n";
#		print "$acc\n";
		if ($acc =~ /O88521/ || $previous_acc =~ /O88521/) {
			print STDERR "$0: printing complete\n";
		}
    } else {
		foreach my $ar2 (@{$ar}) {
			if ($boiling_flag || $acc =~ /O88521/ || $previous_acc =~ /O88521/) {
				print STDERR "$0: printing acc=$acc\n";
			}
		    print 
			"$acc\tUniProt:$acc\t",
			$ar2->[0]->Name->[0], "\t",
			$LINK_URL . $acc,
			($ar2->[1] ? "\t" . $ar2->[1] : ''), "$spname\n";
		}
    }
    
    $previous_acc = $acc;
    $acc_num++;
}

print STDERR "$0: done\n";

sub top_level_events_with_evidence {
    my ($acc,$ref_db_id) = @_;
    my %h;
    my $rpss = $dba->fetch_instance_by_attribute($protein_class, [['identifier',[$acc]],['referenceDatabase', [$ref_db_id]]]);
    foreach my $rps (@{$rpss}) {
	if ($boiling_flag || $acc =~ /O88521/ || $previous_acc =~ /O88521/) {
		print STDERR "$0.top_level_events_with_evidence: about to follow instructions\n";
	}
	my $immediate_events = $rps->follow_class_attributes(%instructions1);
	if ($boiling_flag || $acc =~ /O88521/ || $previous_acc =~ /O88521/) {
		print STDERR "$0.top_level_events_with_evidence: instructions followed\n";
	}
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
		#@{$e->reverse_attribute_value('hasEvent')} && next;
#		@{$e->reverse_attribute_value('hasMember')} && next;
		push @out, $e;
    }
    # Skip Reactions
    # @out = grep {! $_->is_a('Reaction')} @out;
    
    # Grab Pathways
    @out = grep { $_->is_a('Pathway')} @out;
    
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
#    print STDERR "$0.fetch_unique_accessions: Got ", scalar(@out), " unique accession numbers.\n";
    return \@out;
}
