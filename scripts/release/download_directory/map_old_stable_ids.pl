#!/usr/bin/perl
use common::sense;
use autodie;
use DBI;
use Getopt::Long;

use lib '/usr/local/gkb/modules';
use Data::Dumper;
use GKB::Config;

my ($user,$pass,$release,$db);
GetOptions(
    "user:s"  => \$user,
    "pass:s"  => \$pass,
    "db:s"    => \$db,
    "release:i" => \$release
);

$release || die "Usage:\n\t$0 -user user -pass pass -release release [-db stable_id_db]\n";
$user ||= $GKB::Config::GK_DB_USER;
$pass ||= $GKB::Config::GK_DB_PASS;
$db ||= 'stable_identifiers';

my $dbh = DBI->connect(
    "dbi:mysql:$db",
    $user,
    $pass
);

my %db_id;
my $sth = $dbh->prepare('SELECT identifier,instanceId FROM StableIdentifier');
$sth->execute;
while (my $id = $sth->fetchrow_arrayref) {
    my ($st_id,$db_id) = @$id;
    $db_id{$db_id}{$st_id}++;
}

my @hsa_rows;
my @other_rows;
for my $db_id (sort {$a<=>$b} keys %db_id) {
    my @st_ids = sort keys %{$db_id{$db_id}};
    next if @st_ids < 2;
    my $array_to_push = $st_ids[0] =~ /R-HSA/ ? \@hsa_rows : \@other_rows;
    my $primary = shift @st_ids;
    $primary =~ /^R-/ or next;
    my $old = \@st_ids;
    push @$array_to_push, [$primary,$old];
}

my @to_print;
my @deferred;
for (@hsa_rows, @other_rows) {
    my ($st_id,$old) = @$_;
    if (@$old > 1) {
	push @deferred, $_;
    }
    else {
	push @to_print, $_;
    }
}
push @to_print, @deferred;

system "mkdir -p $release";
open my $out, ">", "$release/reactome_stable_ids.txt";
say $out "# Reactome stable IDs for release $release";
say $out join("\t",qw/Stable_ID old_identifier(s)/);
for my $row (@to_print) {
    my ($st_id,$old) = @$row;
    my $old_st_id = join(',',@$old);
    say $out join("\t",$st_id,$old_st_id);
    say join("\t",$st_id,$old_st_id);
}
