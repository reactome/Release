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
use constant Q3  => 'SELECT class,reactomeRelease,name FROM History WHERE ST_ID = ? ORDER BY reactomeRelease';
use constant STABLE   => 'SELECT DB_ID FROM StableIdentifier WHERE (identifier = ? OR oldIdentifier = ?)';
use constant ST2DB_ID => 'SELECT DB_ID FROM DatabaseObject WHERE stableIdentifier = ?';

sub DESTROY {
    my $self = shift;
    my $db = $GKB::Secrets::GK_DB_NAME;
    if ($self->{dba} && $self->{dba}->{$db}) {
	my $dba = $self->{dba}->{$db};
	$dba->db_handle && $dba->db_handle->disconnect();
	$self->{dba} = undef;
    }
    if ($self->{dbh}) {
	$self->{dbh}->disconnect();
	$self->{dbh} = undef;
    }
}

sub new {
    my $class = shift;
    my $self = {};
    return bless $self, $class;
}


sub duh_give_me_db_id {
    my $self = shift;
    my $stable_id = shift;
    if ($stable_id =~ /R-HSA-(\d+)|R-NUL-(\d+)|R-ALL-(\d+)/) {
	my $db_id = $1 || $2 || $3;
	#say STDERR "GOT DB_ID from the actual ID itself";
	return $db_id;
    }
    return undef;
}

sub db_id_from_stable_id_gkb {
    my $self = shift;
    my $stable_id = shift;

    # If it's human, the DB_ID is in the stable ID
    my $db_id = $self->duh_give_me_db_id($stable_id);
    
    unless ($db_id) {
	my $dba = $self->get_dba();
	my $sth = $dba->prepare(STABLE);
	$sth->execute($stable_id,$stable_id);
	#say STDERR "searching for $stable_id in regular database";
	while (my $res = $sth->fetchrow_arrayref) {
	    my $st_id = $res->[0];
	    if ($st_id) {
		my $sth2 = $dba->prepare(ST2DB_ID);
		$sth2->execute($st_id);
		while (my $res2 = $sth2->fetchrow_arrayref) {
		    my $db_id = $res2->[0];
		    #say STDERR "MY DB_ID is $db_id (from regular database)";
		    last if $db_id;
		}
	    }
	    last if $db_id;
	}
    }

    return $db_id;
}

sub db_id_from_stable_id {
    my $self = shift;
    my $stable_id = shift;

    $stable_id = uc($stable_id);
    $stable_id =~ /^REACT|^R-/ or die "$stable_id does not look like a stable ID to me";
    $stable_id =~ s/\.\d+$//;

    
    # first, let's try to find it in the main DB
    my $db_id = $self->db_id_from_stable_id_gkb($stable_id);

    if ($db_id) {
	#say STDERR "I found an DB_ID without using the stable_id database";
	return $db_id;
    }

    my $query = $self->dbh->prepare(Q1);
    $query->execute($stable_id);
    
    while (my $id = $query->fetchrow_arrayref) {
	$db_id = $id->[0];
	last if $db_id;
    } 

    return $db_id;
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
	    my ($class,$release,$actual_name) = @$event;
	    push @events, [$actual_name,$class,$release];
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
