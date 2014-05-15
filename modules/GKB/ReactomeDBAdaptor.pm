package GKB::ReactomeDBAdaptor;

use strict;
use vars qw(@ISA);
use GKB::DBAdaptor;
use GKB::Ontology;
use GKB::Utils;
@ISA = qw(GKB::DBAdaptor);

sub fetch_Reaction_by_location_xy {
    my ($self,$x,$y) = @_;
    my ($sth, $res) =
        $self->execute(qq(
                          SELECT $DB_ID_NAME, SQRT(POWER((sourceX + targetX) / 2 - $x,2) +
                                             POWER((sourceY + targetY) / 2 - $y,2))
                          AS N FROM ReactionCoordinates ORDER BY N LIMIT 1));
    my $db_id = $sth->fetchrow_arrayref->[0];
    $db_id || return [];
    my $eventLocation = $self->fetch_instance_by_db_id($db_id)->[0];    
    my $reaction = $eventLocation->attribute_value('locatedEvent')->[0];
    $reaction || return [];
    return [$reaction];    
}

sub fetch_minmax_Reaction_location {
    my ($self,$ar) = @_;
    $ar->[0] || $self->throw("Need Reaction instances - got none.\n");
    my $query = 
	"SELECT MAX(sourceX),MAX(sourceY),MAX(targetX),MAX(targetY)," .
	"MIN(sourceX),MIN(sourceY),MIN(targetX),MIN(targetY)" .
	"\nFROM ReactionCoordinates" .
	"\nWHERE locatedEvent IN (" .
	join(',', map {$_->db_id} @{$ar}) . ")\n";
    my ($sth,$res) = $self->execute($query);
    my ($sx1,$sy1,$tx1,$ty1,$sx2,$sy2,$tx2,$ty2) = @{$sth->fetchrow_arrayref()};
    my ($max_x,$max_y,$min_x,$min_y);
    if (defined $sx1 and defined $tx1) {
	$max_x = (sort{$b <=> $a} ($sx1,$tx1))[0];
    }
    if (defined $sy1 and defined $ty1) {
	$max_y = (sort{$b <=> $a} ($sy1,$ty1))[0];
    }
    if (defined $sx2 and defined $tx2) {
	$min_x = (sort{$b <=> $a} ($sx2,$tx2))[0];
    }
    if (defined $sy2 and defined $ty2) {
	$min_y = (sort{$b <=> $a} ($sy2,$ty2))[0];
    }
    return ($min_x,$min_y,$max_x,$max_y);
}

sub fetch_species_db_ids_and_event_counts {
    my ($self,$event_db_ids) = @_;
    my $event_sp_tbl = GKB::Utils::get_Event_species_table($self);
    my $tmp = join(',', @{$event_db_ids});
    my $stmt = qq/SELECT species,COUNT($DB_ID_NAME) AS N FROM $event_sp_tbl where $DB_ID_NAME IN($tmp) GROUP BY species ORDER BY N DESC/;
    my ($sth,$res) = $self->execute($stmt);
    return $sth->fetchall_arrayref;
}

sub fetch_species_db_id_with_highest_event_count {
    my $self = shift;
    my $ar = $self->fetch_species_db_ids_and_event_counts(@_);
    if (@{$ar}) {
	return $ar->[0]->[0];
    }
    return undef;
}

sub fetch_frontpage_species {
    my $self = shift;
    my $events = $self->fetch_instance_by_remote_attribute
	('Event',
	 [
	  ['frontPageItem:FrontPage','IS NOT NULL',[]]
	  ]
	);
    my %species;
    # Assume that the 1st value of the species slot is the "host" species
    foreach my $e (@{$events}) {
	if (my $sp = $e->Species->[0]) {
	    $species{$sp->db_id} = $sp;
	}
	foreach my $oe (@{$e->OrthologousEvent}) {
	    if (my $sp = $oe->Species->[0]) {
		$species{$sp->db_id} = $sp;
	    }
	}
    }
    return [values %species];
}

1;
