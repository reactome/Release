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
use constant Q3  => 'SELECT class, reactomeRelease FROM Changed WHERE ST_ID = ?';


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

    warn "STABLE $stable_id";

    my $query = $self->dbh->prepare(Q1);
    $query->execute($stable_id);
    
    while (my $id = $query->fetchrow_arrayref) {
	return $id->[0];
    } 

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

sub get_events {
    my $self = shift;
    my $db_id = shift;
    my @stable_ids = $self->stable_id_from_db_id($db_id);

    my $query = $self->dbh->prepare(Q3);

    my @events;
    for my $st_id (@stable_ids) {
	my ($st_db_id,$identifier,$version) = @$st_id;
	$query->execute($st_db_id);
	warn "STUFF ($st_db_id,$identifier,$version)";
	while (my $event = $query->fetchrow_arrayref) {
	    my ($class,$release) = @$event;
	    my $dba = $self->get_dba("test_slice_$release");
	    my ($actual_identifier, $actual_version, $display_name);
	    if ($class eq 'deleted' || $class eq 'renamed') {
		warn "ACTUAL $identifier $class";
		$actual_identifier = $identifier;
		$actual_version = $version;
	    }

	    my $instance = $dba->fetch_instance_by_db_id($db_id)->[0];
	    if ($instance) {
		my $actual_st_id = $instance->attribute_value('stableIdentifier')->[0];
		if ( $actual_st_id ) {
		    $actual_identifier ||= $actual_st_id->attribute_value('identifier')->[0];
		    $actual_version ||= $actual_st_id->attribute_value('identifierVersion')->[0];
		}
		push @events, [$instance->displayName,"$actual_identifier.$actual_version",$class,$release];
	    }
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
