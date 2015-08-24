package GKB::StableIdentifierDatabase;

# Basic wrapper for stable id database
use common::sense;

use vars qw/@ISA/;
use Exporter();

use lib '/usr/local/gkb/modules';
use GKB::Secrets;
use GKB::DBAdaptor;
use Data::Dumper;

use constant DB  => 'stable_identifiers';
use constant Q1  => 'SELECT instanceId FROM StableIdentifier WHERE identifier = ?';
use constant Q2  => 'SELECT DB_ID,identifier,identifierVersion FROM StableIdentifier WHERE instanceId = ?';
use constant Q3  => '
SELECT h.class, r.release_num, n.name, r.database_name
FROM History h, Name n, ReactomeRelease r 
WHERE h.ST_ID = ?
AND h.name = n.DB_ID
AND r.DB_ID = h.ReactomeRelease
ORDER BY h.ReactomeRelease';

sub new {
    my $class = shift;
    my $self = {};
    return bless $self, $class;
}

sub db_id_from_stable_id {
    my $self = shift;
    my $stable_id = shift;

    $stable_id = uc($stable_id);
    $stable_id =~ /^REACT|^R-/ or die "$stable_id does not look like a stable ID to me";
    $stable_id =~ s/\.\d+$//;

    my $query = $self->dbh->prepare(Q1);
    $query->execute($stable_id);
    
    while (my $id = $query->fetchrow_arrayref) {
	return $id->[0];
    } 
}

sub db_ids_from_stable_id {
    my $self = shift;
    my $stable_id = shift;

    $stable_id = uc($stable_id);
    $stable_id =~ /^REACT|^R-/ or die "$stable_id does not look like a stable ID to me";
    $stable_id =~ s/\.\d+$//;

    my $query = $self->dbh->prepare(Q1);
    $query->execute($stable_id);

    my $ids = [];
    while (my $id = $query->fetchrow_arrayref) {
        push @$ids, $id->[0];
    }
    return $ids;
}

sub stable_id_from_db_id {
    my $self = shift;
    my $db_id = shift || die("No DB_ID");
    warn "DB_ID IS $db_id";
    my $query = $self->dbh->prepare(Q2);
    $query->execute($db_id);
    
    my @st_id;
    while (my $st_id = $query->fetchrow_arrayref) {
	push @st_id, [@$st_id]; #deref/ref weird recycle of reference otherwise
    }

    return @st_id;
}

sub get_history {
    my $self = shift;
    my $db_id = shift;
    my @stable_ids = $self->stable_id_from_db_id($db_id);

    my $query = $self->dbh->prepare(Q3);

    my @events;
    for my $st_id (@stable_ids) {
	my ($st_db_id,$identifier,$version) = @$st_id;
	$query->execute($st_db_id);

	while (my $event = $query->fetchrow_arrayref) {
	    my ($class,$release,$actual_name,$database) = @$event;
	    push @events, [$actual_name,$class,$release,$database];
	}
    }

    return @events;
}

sub dbh {
    my $self = shift;
    $self->{dbh} ||= DBI->connect(
	"dbi:mysql:".DB,
	$GKB::Secrets::GK_DB_USER,
	$GKB::Secrets::GK_DB_PASS
	);
    return $self->{dbh};
}

sub get_dba {
    my $self = shift;
    my $db   = shift ||  $GKB::Secrets::GK_DB_NAME;
    $self->{dba}->{$db} ||= GKB::DBAdaptor->new(
	-dbname  => $db,
	-user    => $GKB::Secrets::GK_DB_USER,
	-pass    => $GKB::Secrets::GK_DB_PASS
	);
    return $self->{dba}->{$db};
}


1;
