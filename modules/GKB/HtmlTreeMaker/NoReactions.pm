package GKB::HtmlTreeMaker::NoReactions;

use strict;
use vars qw(@ISA);
use GKB::HtmlTreeMaker;

@ISA = qw(GKB::HtmlTreeMaker);

sub _node_in_list {
    $_[1]->is_a('Reaction') && return '';
    $_[1]->is_a('ReactionlikeEvent') && return '';
    return GKB::HtmlTreeMaker::_node_in_list(@_);
}
