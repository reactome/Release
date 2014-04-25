#!/usr/local/bin/perl  -w
use strict;

#This script provides some stats on the gene_association.reactome file, submitted to GOA.

#File format (15 columns):
#DB Uniprot_accession Uniprot_id Qualifier GO_id Reference Evidence_code With_From Aspect DB_object_name Synonym DB_object_type Taxon Date Assigned_by

open(IN, "$ARGV[0]");
open(OUT, ">$ARGV[1]");
#print $ARGV[1];

my ($count_total, $count_human, $count_exp, $count_human_exp, $count_unique, $count_human_unique, $count_3, $count_2);
my (%seen, %seen_human, %c, %f, %p);
my $count_cf = 0;
my $count_cp = 0;
my $count_fp = 0;

my $header = <IN>;
while (<IN>) {
    chomp;
    my @items = split/\t/, $_;
    $count_total++;
    $seen{$items[1]}++;
    if ($items[6] eq 'EXP') {
	$count_exp++;
    }
    if ($items[12] eq 'taxon:9606') { #only human entries
	$count_human++;
	$seen_human{$items[1]}++;
	if ($items[6] eq 'EXP') {
	    $count_human_exp++;
	}
	($items[8] eq 'C') && $c{$items[1]}++;
	($items[8] eq 'F') && $f{$items[1]}++;
	($items[8] eq 'P') && $p{$items[1]}++;
    }
}

print "$0: count_total=$count_total, count_human=$count_human, count_human_exp=$count_human_exp\n";

foreach my $human_uni (keys %seen_human) {
    if ($c{$human_uni} && $f{$human_uni} && $p{$human_uni}) {
#        print OUT $human_uni, "\tC and F and P\n";
	$count_3++;
	next;
    }
    if ($c{$human_uni} && $f{$human_uni}) {
#	print OUT $human_uni, "\tC and F\n";
	$count_2++;
	$count_cf++;
	next;
    }
    if ($c{$human_uni} && $p{$human_uni}) {
#        print OUT $human_uni, "\tC and P\n";
	$count_2++;
	$count_cp++;
	next;
    }
    if ($p{$human_uni} && $f{$human_uni}) {
#        print OUT $human_uni, "\tP and F\n";
	$count_2++;
	$count_fp++;
	next;
    }
}

print OUT "Total submissions: ", $count_total, "\n";
print OUT "Unique Uniprot ids: ", scalar (keys %seen), "\n";
print OUT "Total submissions with EXP code: ", $count_exp, "\n";
print OUT "Total human submissions: ", $count_human, "\n";
print OUT "Unique human Uniprot ids: ", scalar (keys %seen_human), "\n";
print OUT "Total human submissions with EXP code: ", $count_human_exp, "\n";
print OUT "Number of human UniProt accessions with annotation for three GO aspects: ", $count_3, "\n";
print OUT "Number of human UniProt accessions with annotation for two GO aspects: ", $count_2, "\n";
print OUT "\tC and F: ", $count_cf, "\n";
print OUT "\tC and P: ", $count_cp, "\n";
print OUT "\tF and P: ", $count_fp, "\n";

close(IN);
close(OUT);

print "goa_submission_stats.pl has finished its job\n";

#system("cp $ARGV[1] /usr/local/gkbdev/scripts/release/gene_association.reactome.stats");
