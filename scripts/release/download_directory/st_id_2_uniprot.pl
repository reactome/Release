#!/usr/bin/perl
use common::sense;
use Data::Dumper;

# print st_id/uniprot mapping file

use lib '/usr/local/gkb/modules';

use Getopt::Long;

use GKB::Config;
use GKB::DBAdaptor;

my ($user, $pass, $host, $db, $class, $help);
GetOptions(
    'user=s' => \$user,
    'pass=s' => \$pass,
    'host=s' => \$host,
    'db=s' => \$db,
    'class=s' => \$class,
    'help' => \$help
);

if ($help) {
    print usage_instructions();
    exit;
}


my $dba = GKB::DBAdaptor->new(
    -dbname  => $db || $GKB::Config::GK_DB_NAME,
    -user    => $user || $GKB::Config::GK_DB_USER,
    -host    => $host || $GKB::Config::GK_DB_HOST,
    -pass    => $pass || $GKB::Config::GK_DB_PASS,
    -port    => 3306
);

my $sth = $dba->prepare('SELECT DB_ID FROM DatabaseObject WHERE _class = ?');
$sth->execute($class || 'EntityWithAccessionedSequence');
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

sub usage_instructions {
    return <<END;
    
    This script outputs the mapping of instance
    stable identifiers to the UniProt identifier
    it references.
    
    Usage: perl $0
    
    Options:
    
    -user [db_user]         User for source database (default is $GKB::Config::GK_DB_USER)
    -pass [db_pass]         Password for source database (default is password for $GKB::Config::GK_DB_USER user)
    -host [db_host]         Host for source database (default is $GKB::Config::GK_DB_HOST)
    -db [db_name]           Source database (default is $GKB::Config::GK_DB_NAME)
    -class [instance_class] Class of database objects to select (default is EntityWithAccessionedSequence)
    -help                   Prints these instructions
    
END
}