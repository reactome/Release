package GKB::Utils::HTML;

use strict;
use Carp qw(confess);

sub section_visibility_js {
    my ($div_id,$off_by_default) = @_;
    my $tmp = ($off_by_default) ? 'true' : 'false';
    return <<__END__;
<script language="javascript" src="/javascript/sectioned_view.js"></script>
<script language="javascript">
setSectionVisibility('$div_id',$tmp);
</script>
__END__
}

sub section_visibility_toggle_button {
    my ($div_id) = @_;
    my $img_id = 'toggle_' . $div_id;
    return qq(<A ONMOUSEOVER="ddrivetip('Show/hide this section','#DCDCDC')" ONMOUSEOUT="hideddrivetip()" ONCLICK="javascript:changeSectionVisibility('$div_id');"><IMG ID="$img_id" SRC="/icons/plus-box.gif"></A>);
}

sub collapsable_section {
    my ($div_id,$title,$content) = @_;
    return
	collapsable_section_start($div_id,$title) . 
        $content .
	collapsable_section_end($div_id);
}

sub collapsable_section_start {
    my ($div_id,$title,$style) = @_;
    $style ||= '';
    return
	qq(<DIV CLASS="section">) .
	section_visibility_toggle_button($div_id) .
	qq(<SPAN CLASS="sectionname">$title</SPAN>) .
	qq(<DIV ID="$div_id" STYLE="$style">\n);
}

sub collapsable_section_end {
    my ($div_id) = @_;
    return
	qq(</DIV><!-- $div_id -->\n) .
	section_visibility_js($div_id) .
	qq(</DIV><!-- section -->\n);
}

1;
