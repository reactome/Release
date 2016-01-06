#!/usr/bin/perl -w
use lib "/usr/local/gkb/modules";
use GKB::StableIdentifierDatabase;
use GKB::DBAdaptor;use common::sense;

my $stable = GKB::StableIdentifierDatabase->new();
my ($user, $pass, $db) = @ARGV;
$user && $pass && $db || die "Need username pass!";
my $dba = GKB::DBAdaptor->new(
            -dbname  => $db,
            -user    => $user,
            -pass    => $pass
    );

my $hist = DBI->connect(
    "dbi:mysql:stable_identifiers",
    $user,
    $pass
    );


my $sth = $dba->prepare('SELECT DB_ID FROM DatabaseObject WHERE stableIdentifier IS NOT NULL');
$sth->execute;
while (my $ar = $sth->fetchrow_arrayref) {
    my $instance = $dba->fetch_instance_by_db_id($ar->[0])->[0];
    my $stable_id = $instance->StableIdentifier->[0];
    my $identifier = $stable_id->identifier->[0];
    my $old_id = $stable_id->oldIdentifier->[0];
    next if $old_id;
    
    my $sth = $hist->prepare('SELECT identifier FROM StableIdentifier WHERE instanceId = ?');
    $sth->execute($ar->[0]);
    my $old_ids = [];
    while (my $res = $sth->fetchrow_arrayref) {
	my $oid = $res->[0];
	next if $oid eq $identifier;
	say "OLD ID $oid";
	push @$old_ids, $oid;
    }
    if ($old_ids->[0]) {
	$stable_id->inflate;
	$stable_id->oldIdentifier($old_ids);
	$dba->update($stable_id);
	say "updated $identifier";
    }
}
    

