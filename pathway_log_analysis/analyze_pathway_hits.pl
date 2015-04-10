#!/usr/bin/perl -w
use common::sense;


use constant COUNTS  => 'hit_counts.txt';
use constant INFO    => 'pathway_info.txt';
use constant TREE    => 'pathway_hierarchy.txt';


my (%top,%parent);
open PARENT, TREE or die $!;
while (<PARENT>) {
    chomp;
    my ($parent,$child) = split;
    if ($parent == $child) {
	$top{$parent}++;
	next;
    }
    $parent{$child}{$parent}++;
}

my (%species,%name,%inferred);
open INF, INFO or die $!;
while (<INF>) {
    chomp;
    my ($id,$species,$name,$inferred) = split "\t";
    $species{$id} = $species;
    $name{$id} = $name;
    $inferred{$id} = $inferred if $inferred;
}


open TOP, ">top_level_hits.csv";
open CUR, ">curated_hits.csv";
open INF, ">inferred_hits.csv";

open C, COUNTS or die $!;
while (<C>) {
    chomp;
    my ($count,$id) = split;
    # only pathways
    my $name = $name{$id} || next;
    my $species = $species{$id};
    my $inferred = $inferred{$id};

    my @parents = sort {$a<=>$b} keys %{$parent{$id}} if $parent{$id};
    my @p;
    for (@parents) {
	my $parent_name = $name{$_};
	push @p, ($_,$parent_name);
    }

    my $out = join(",",$count,$species,$id,$name,@p);

    if ($top{$id}) {
	say TOP $out;
	next;
    }
    
    if ($inferred{$id}) {
	say INF $out;
	next;
    }
    else {
	say CUR $out;
    }
}
