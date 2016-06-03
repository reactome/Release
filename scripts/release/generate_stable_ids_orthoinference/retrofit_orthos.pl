#!/usr/bin/perl -w
use strict;

use Carp;
use common::sense;
use Getopt::Long;
use Log::Log4perl qw/get_logger/;

use lib "/usr/local/gkb/modules";
#use GKB::StableIdentifierDatabase;
use GKB::Config;
use GKB::DBAdaptor;
use GKB::Utils_esther;

Log::Log4perl->init(\$LOG_CONF);
my $logger = get_logger(__PACKAGE__);

#my $stable = GKB::StableIdentifierDatabase->new();
our ($user, $pass, $db);
GetOptions(
    "user:s" => \$user,
    "pass:s" => \$pass,
    "db:s" => \$db
);
$user && $pass && $db || die "Usage: perl $0 -user user -pass pass -db test_reactome_XX";

back_up_databases(
    [$user, $pass, $db, 'localhost']
);

get_api_connections()->{$db}->execute("START TRANSACTION");
my $sth = get_api_connections()->{$db}->prepare('SELECT DB_ID FROM DatabaseObject WHERE stableIdentifier IS NOT NULL');
$sth->execute;
while (my $ar = $sth->fetchrow_arrayref) {
    my $instance = get_api_connections()->{$db}->fetch_instance_by_db_id($ar->[0])->[0];
    my $stable_id = $instance->StableIdentifier->[0];
    my $identifier = $stable_id->identifier->[0];
    my $old_id = $stable_id->oldIdentifier->[0];
    next if $old_id;
    
    my $sth = get_api_connections()->{'stable_identifiers'}->prepare('SELECT identifier FROM StableIdentifier WHERE instanceId = ?');
    $sth->execute($ar->[0]);
    my $old_ids = [];
    while (my $res = $sth->fetchrow_arrayref) {
        my $oid = $res->[0];
        next if $oid eq $identifier;
        $logger->info("OLD ID $oid");
        
        push @$old_ids, $oid;
    }
    if ($old_ids->[0]) {
        $stable_id->inflate;
        $stable_id->oldIdentifier($old_ids);
        $stable_id->Modified(@{$stable_id->Modified});
        $stable_id->add_attribute_value('modified', get_instance_edit($db));
        get_api_connections()->{$db}->update_attribute($stable_id, 'oldIdentifier');
        get_api_connections()->{$db}->update_attribute($stable_id, 'modified');
        $logger->info("updated $identifier");
    }
}
get_api_connections()->{$db}->execute("COMMIT");
    
sub back_up_databases {
    my @dbs = @_;
    
    foreach my $db (@dbs) {
        my ($user, $pass, $name, $host) = @$db;
        next unless $name && $host;
        $user ||= $GKB::Config::GK_DB_USER;
        $pass ||= $GKB::Config::GK_DB_PASS;
        
        my $back_up_successful = (system("mysqldump -h $host -u $user -p$pass $name > $name.dump") == 0);
        die "Unable to back-up $db at $host for $user" unless $back_up_successful;
    }   
}

sub get_instance_edit {
    state $db_to_instance_edit;
    my $db = shift;
    
    return $db_to_instance_edit->{$db} if $db_to_instance_edit->{$db};
    
    (my $dba = get_api_connections()->{$db}) // confess "No database adaptor for $db database available to create instance edit";
    my $date = `date \+\%F`;
    chomp $date;
    $db_to_instance_edit->{$db} = GKB::Utils_esther::create_instance_edit($dba, 'Weiser', 'JD', $date);
        
    return $db_to_instance_edit->{$db};
}

sub get_api_connections {
    state $api_connections;
    return $api_connections if $api_connections;

    my $release_dba = GKB::DBAdaptor->new(
        -dbname  => $db,
        -user    => $user || $GKB::Config::GK_DB_USER,
        -pass    => $pass || $GKB::Config::GK_DB_PASS
    );
    
    my $stable_id_dbh = DBI->connect(
        "dbi:mysql:stable_identifiers",
        $user || $GKB::Config::GK_DB_USER,
        $pass || $GKB::Config::GK_DB_PASS
    );

    $api_connections = {
        $db => $release_dba,
        'stable_identifiers' => $stable_id_dbh
    };
    
    return $api_connections;
}
