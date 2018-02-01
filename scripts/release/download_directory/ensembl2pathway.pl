#!/usr/bin/perl
use common::sense;
use Data::Dumper;
use DBI;

@ARGV >= 4 or die "$0 user pass DB";
my ($user, $pass, $db, $rel) = @ARGV;

my $dba = DBI->connect(
    "dbi:mysql:$db",
    $user,
    $pass
    );

my $sql = '
SELECT e.externalIdentifier, pa.stableId, pa.displayName 
FROM PhysicalEntity p, Id_To_ExternalIdentifier e, 
Pathway_To_ReactionLikeEvent pr,  
ReactionLikeEvent_To_PhysicalEntity r, Pathway pa  
WHERE e.id = p.id and p.id = r.physicalEntityId  
AND pa.id = pr.pathwayId 
AND r.reactionLikeEventId = pr.reactionLikeEventId 
AND p.species = "Homo sapiens" 
AND e.externalIdentifier LIKE "ENSG00%"  
AND e.referenceDatabase = "ENSEMBL" 
ORDER BY e.externalIdentifier';

my $sth = $dba->prepare($sql);
$sth->execute;

system "mkdir -p $rel";
open OUT, "| sort -u >$rel/homo_sapiens_ensembl_gene_to_pathways.csv" or die $!;
while (my $res = $sth->fetchrow_arrayref) {
    say OUT join(",",@$res);
}




