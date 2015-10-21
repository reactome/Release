#!/usr/bin/perl
use common::sense;
use Data::Dumper;

# print st_id/uniprot mapping file

use lib '/usr/local/gkb/modules';
use GKB::DBAdaptor;

@ARGV >= 3 or die "$0 user pass db [class]";
my ($user, $pass, $db) = @ARGV;

my $dba = GKB::DBAdaptor->new(
    -dbname  => $db,
    -user    => $user,
    -pass    => $pass
    );

my $sth = $dba->prepare('SELECT DB_ID FROM DatabaseObject WHERE _class = ?');
$sth->execute($ARGV[3] || 'EntityWithAccessionedSequence');
my @db_ids;
while (my $ary = $sth->fetchrow_arrayref) {
    push @db_ids, $ary->[0];
}

for my $db_id (@db_ids) {
    my $instance = $dba->fetch_instance_by_db_id($db_id)->[0];
    my $st_id = $instance->stableIdentifier->[0]->displayName;
    my $refseq = $instance->referenceEntity->[0];
    my $uniprot = $refseq->displayName;
    if ($uniprot && $uniprot =~ /^uniprot:/i) {
	$uniprot =~ s/^[^:]+://;
	($uniprot) = split(/\s+/, $uniprot);
	say join("\t",$st_id,$uniprot);
    }
    
}


