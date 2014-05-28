package GKB::Graphics::ReactomeReaction::Node;

use strict;
use GD;
use GKB::Graphics::SimpleDraw::Box;
use CGI::Util 'rearrange';
use Carp;

use constant SMALLMOL_RENDERING_PARAMS => {box=>1,fill=>[255,255,255]};
use constant COMPLEX_RENDERING_PARAMS => {box=>1,fill=>[255,130,71]};
use constant PROTEIN_RENDERING_PARAMS => {box=>1,fill=>[176,196,222]};
use constant SEQUENCE_RENDERING_PARAMS => {box=>1,fill=>[222,176,196]};
#use constant GENERIC_RENDERING_PARAMS => {box=>1,fill=>[255,0,255]};
use constant OTHER_RENDERING_PARAMS => {fill=>[200,200,200]};
#Use constant EVENT_RENDERING_PARAMS => {fill=>[200,200,200]};
#use constant HELPER_RENDERING_PARAMS => {fill=>[230,230,230]};
use constant EVENT_RENDERING_PARAMS => {};
use constant HELPER_RENDERING_PARAMS => {};

use constant GEE_RENDERING_PARAMS => {box=>1,fill=>[200,100,100]};
use constant DEFINEDSET_RENDERING_PARAMS => {box=>1,fill=>[0,200,200]};
use constant OPENSET_RENDERING_PARAMS => {box=>1,fill=>[0,200,100]};
use constant CANDIDATESET_RENDERING_PARAMS => {box=>1,fill=>[0,100,200],fontcolor=>[255,255,255]};

sub new {
    my $class = shift;
    $class = ref($class) if ref $class;
    my ($instance,$coefficient,$rp) = rearrange([qw(INSTANCE COEFFICIENT RENDERING_PARAMS)],@_);
    return bless {
	instance => $instance,
	coefficient => $coefficient,
#	rendering_params => $rp,
    },$class;
}

sub instance { return shift->{instance} }
sub coefficient { return shift->{coefficient} }
sub box { if (@_ > 1) { $_[0]->{box} = $_[1];} return $_[0]->{box}}
sub left { if (@_ > 1) { $_[0]->{left} = $_[1];} return $_[0]->{left}}
sub top { if (@_ > 1) { $_[0]->{top} = $_[1];} return $_[0]->{top}}
sub rendering_params {
    if (@_ > 1) {
	$_[0]->{rendering_params} = $_[1];
    }
    if (my $t = $_[0]->{rendering_params}) {
	return $t;
    }
    return $_[0]->default_rendering_params;
}

#sub width { return $_[0]->box->width }
#sub height { return $_[0]->box->height }
sub width { return shift->box->width(@_) }
sub height { return shift->box->height(@_) }

sub col_left { if (@_ > 1) { $_[0]->{col_left} = $_[1];} return $_[0]->{col_left}}
sub col_right { if (@_ > 1) { $_[0]->{col_right} = $_[1];} return $_[0]->{col_right}}

sub render {
    my $self = shift;
#    print STDERR "render @_\n";
    $self->left($_[1]);
    $self->top($_[2]);
#    $self->box->render(@_[0..2],$self->rendering_params);
    $self->box->render_with_coefficient(@_[0..2],$self->rendering_params);
}

sub out_edge_y {
    return $_[0]->find_edge_y($_[1], $_[0]->out_edges);
}

sub in_edge_y {
#    warn("in_edge_y: " . scalar(@{$_[0]->in_edges}) . " " . $_[0]->instance->extended_displayName . "\n" .
#	join("\n", map {$_->type . " " . $_->from->instance->extended_displayName} @{$_[0]->in_edges}) . "\n");
    return $_[0]->find_edge_y($_[1], $_[0]->in_edges);
}

sub find_edge_y {
    my ($self,$edge,$ar) = @_;
    foreach my $i (0 .. $#{$ar}) {
	if ($edge == $ar->[$i]) {
#	    warn("$self, $edge, $i\n");
	    return $self->top + $self->height/(@{$ar} + 1) * ($i + 1);
	}
    }
#    warn("Edge not found for this node.\n");
    return $self->top + $self->height/2;
}

sub in_edges {return shift->{in_edges} || [] }
sub out_edges {return shift->{out_edges} || []}
sub add_in_edge { push @{shift->{in_edges}},@_ }
sub add_out_edge { push @{shift->{out_edges}},@_ }

sub coords {
    my $self = shift;
    return (
	$self->left,
	$self->top,
	$self->left + $self->width,
	$self->top + $self->height
    );
}

sub default_rendering_params {
    my $self = shift;
    my $i = $self->instance;
    unless ($i) {
	return HELPER_RENDERING_PARAMS;
    }
    if ($i->is_a('PhysicalEntity')) {
	if ($i->is_a('SequenceEntity') || $i->is_a('EntityWithAccessionedSequence')) {
	    if ($i->ReferenceEntity->[0] && ($i->ReferenceEntity->[0]->is_a('ReferenceGeneProduct') || $i->ReferenceEntity->[0]->is_a('ReferencePeptideSequence'))) {
		return PROTEIN_RENDERING_PARAMS;
	    } else {
		return SEQUENCE_RENDERING_PARAMS;
	    }
	} elsif ($i->is_a('GenomeEncodedEntity')) {
	    return GEE_RENDERING_PARAMS;
	} elsif ($i->is_a('Complex')) {
	    return COMPLEX_RENDERING_PARAMS;
	} elsif ($i->is_a('ConcreteSimpleEntity') || ($i->class eq 'SimpleEntity')) {
	    return SMALLMOL_RENDERING_PARAMS;
	} elsif ($i->is_a('EntitySet')) {
	    if ($i->is_a('DefinedSet')) {
		return DEFINEDSET_RENDERING_PARAMS;
	    } elsif ($i->is_a('OpenSet')) {
		return OPENSET_RENDERING_PARAMS;
	    } else {
		return CANDIDATESET_RENDERING_PARAMS;
	    }
	} else {
	    return OTHER_RENDERING_PARAMS;
	}
    } else {
	return EVENT_RENDERING_PARAMS;
    }
}

sub clean {
    my $self = shift;
    foreach my $k (keys %{$self}) {
	delete $self->{$k};
    }
}

1;
