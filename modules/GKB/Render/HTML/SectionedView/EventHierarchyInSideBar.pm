package GKB::Render::HTML::SectionedView::EventHierarchyInSideBar;

use strict;
use warnings;

use vars qw(@ISA);
use GKB::Render::HTML::SectionedView;
use GKB::Config;
@ISA = qw(GKB::Render::HTML::SectionedView);

sub render {
    my ($self,$instance) = @_;
    $self->_check_instance($instance);
    return
	$instance->page_title .
	$self->reactionmap_section($instance) .
	$self->diagram_section($instance) .
	$self->details_section($instance) .
	$self->switches_section($instance);
}

sub details_section {
    my ($self,$instance) = @_;
    if ($instance->is_a('Event')) {
	return $self->details_section_for_events($instance);
    } else {
	return $self->SUPER::details_section($instance);
    }
}

sub details_section_for_events {
    my ($self,$instance) = @_;
    my $div_id = 'details_' . $instance->db_id;
    return
	qq(<DIV CLASS="section">) .
	GKB::Utils::HTML::section_visibility_toggle_button($div_id) .
	qq(<SPAN CLASS="sectionname">Details</SPAN>) .
	qq(<DIV ID="$div_id">\n) .
	qq(<TABLE CLASS="sidebar" WIDTH="$HTML_PAGE_WIDTH" BORDER="0" CELLPADDING="0" CELLSPACING="0">\n<TR>) .
	$instance->dynamic_eventhierarchy_and_details_side_by_side .
	qq(</TR>\n</TABLE>\n) .
	qq(</DIV><!-- $div_id -->\n) .
	GKB::Utils::HTML::section_visibility_js($div_id) .
	qq(</DIV><!-- section -->\n);
}

1;
