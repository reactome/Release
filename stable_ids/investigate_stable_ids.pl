#!/usr/bin/perl -w
use common::sense;
use Data::Dumper;
use DBI;

use lib '/usr/local/gkb/modules';
use GKB::DBAdaptor;

use constant USER => 'curator';
use constant PASS => 'r3@ct1v3';
use constant Q1   => 'SELECT DB_ID,_displayName,StableIdentifier FROM DatabaseObject WHERE StableIdentifier IS NOT NULL';
use constant Q2   => 'SELECT identifier FROM StableIdentifier WHERE DB_ID = ?';
use constant Q3   => 'SELECT count(*) FROM StableIdentifier';
use constant REL  => 42..51;

my %stable_id;
my (%spair,%epair,%name,%entity_2_st_id,%report,%total,@count);

my $sname = shift or die "I need a species";
say "SPECIES: $sname";

RELEASE: for my $rel (REL) {
    my $db  = "test_reactome_${rel}";
    my $dsn = "dbi:mysql:$db";
    my $dbh = DBI->connect($dsn, USER, PASS);
    my $ent = $dbh->prepare(Q1);
    my $sta = $dbh->prepare(Q2);
    my $tot = $dbh->prepare(Q3);
    
    $tot->execute;
    my ($total_st_id) = $tot->fetchrow_array;
    $total{$rel} = [$total_st_id];

    say "Grand total stable ids for release $rel is $total_st_id";

    $ent->execute;
    my $idx;
    my $dba = GKB::DBAdaptor->new(
	-dbname => $db,
	-user   => USER,
	-pass   => PASS
	);
    my %rel;
    while (my $res = $ent->fetchrow_arrayref) {
	my ($db_id,$name,$st_id_id) = @$res;

	my $species = get_species($dba,$db_id);
        $species && $species eq $sname or next;

	$sta->execute($st_id_id);
	my ($st_id) = $sta->fetchrow_array;
	
	$st_id || next;

	$spair{$st_id}{$name}++;
	(my $nom = $name) =~ s/\W+//g;
	$epair{$db_id}{$st_id}++;
	$name{$db_id} = $name;
	$stable_id{$st_id}{$rel}++;
	$entity_2_st_id{$db_id}{$rel}{$st_id}++;
	$rel{$st_id}++;
	$idx++;
    }
    
    push @{$total{$rel}}, scalar(keys %rel), $idx;
    say "$rel: I had $idx entities with a stable_id";
    my $running_total = keys %stable_id;
    push @{$total{$rel}}, $running_total;
}

my $num_ids = keys %stable_id;
say "Total of $num_ids stable ids altogether";

my %constant;
my %born;
my %died;
my %weird;
for my $st_id (keys %stable_id) {
    for my $rel (REL) {
	if ($stable_id{$st_id}{$rel}) {
	    push @{$report{$st_id}}, '+';
	}
	else {
	    push @{$report{$st_id}}, '-';
	}
    }
    
    my @rel = sort {$a<=>$b} keys %{$stable_id{$st_id}};
    if (@rel == REL) {
	$constant{$st_id}++;
	next;
    }
    $born{$rel[0]}{$st_id}++  unless $rel[0]  == (REL)[0];
    $died{$rel[-1]}{$st_id}++ unless $rel[-1] == (REL)[-1];
}

my $constant = keys %constant;
say "There are $constant constant stable_ids";

$sname =~ s/\s+/_/g;
system "mkdir -p files/$sname";

open SMULT, ">files/$sname/stable_ids_with_more_than_one_entity.txt";
open EMULT, ">files/$sname/entities_with_more_than_one_stable_id.txt";
open HIST, ">files/$sname/stable_id_history.txt";
say MULT join("\t",'ST_ID','Entity DB_ID:name');
say HIST join("\t",'ST_ID',REL,'Name');
for my $st_id (sort_st_id(keys %report)) {
    my @names = unique(keys %{$spair{$st_id}});
    my $names = join('; ',@names);
    if (@names > 1) {
	say SMULT join("\t",$st_id,$names);
    }

    say HIST join("\t",$st_id,@{$report{$st_id}},$names);
}
for my $db_id (sort {$a<=>$b} keys %epair) {
    my @ids = keys %{$epair{$db_id}};
    if (@ids> 1) {
        say EMULT join("\t",$db_id,$name{$db_id},@ids);
    }
}
close EMULT;
close SMULT;
close HIST;

open OUT,">>summary.txt";

say OUT join("\t",qw/species release,st_ids,entities,born_in,died_in,running_total/);
for my $rel (REL) {
    my @out = @{$total{$rel}};
    my $tot = pop @out;
    if (my @list = keys %{$born{$rel}} ) {
        my $num = @list;
        say "$num stable_ids were born in $rel";
	push @out, $num;
    }
    else {
	push @out, 0;
    }
    if (my @list = keys %{$died{$rel}} ) {
	my $num = @list;
	say "$num stable_ids died in $rel";
	push @out, $num;
    }
    else {
	push @out, 0;
    }
    say OUT join("\t",$sname,"REL-$rel",@out,$tot);
}


# Just make sure entity names are not the same
sub unique {
    my %names = map {$_ => 1} @_;
    my %stripped;
    my @names;
    for my $name (keys %names) {
	(my $nom = $name) =~ s/\W+//g;
	$stripped{$nom} = $name;
    }
    return sort values %stripped;
}

sub sort_st_id {
    map {$_->[0]}
    sort {$a->[1] <=> $b->[1]}
    map {[$_, /_(\d+)$/]} @_;
}

sub get_species {
    my $dba   = shift;
    my $db_id = shift;
    my $instance = $dba->fetch_instance_by_db_id($db_id)->[0] || return '';
    my $species = $instance->attribute_value('species')->[0]  || return '';
    return $species->name->[0];
}
