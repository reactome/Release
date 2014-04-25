package GKB::SearchUtils::ResultTypeSelector;

=head1 NAME

GKB::SearchUtils::ResultTypeSelector

=head1 SYNOPSIS

Produces a bar for the search results page that allows a user
to filter the results by type.

=head1 DESCRIPTION

Places a bar on the web page giving checkboxes for "Pathways",
"Reactions", "Proteins" and "Others", which can be used to
define the types of results displayed in the page.  A button,
"Show", is used to apply the selections.

=head1 SEE ALSO

GKB::SearchUtils
GKB::SearchUtils::ResultsPaginatorReactome

=head1 AUTHOR

David Croft E<lt>croft@ebi.ac.ukE<gt>

Copyright (c) 2008 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

use strict;
use vars qw(@ISA $AUTOLOAD %ok_field);
use Bio::Root::Root;
use Carp;

@ISA = qw(Bio::Root::Root);

for my $attr
    (qw(
        visible
        cgi
        type_counts
	) ) { $ok_field{$attr}++; }

sub AUTOLOAD {
    my $self = shift;
    my $attr = $AUTOLOAD;
    $attr =~ s/.*:://;
    return unless $attr =~ /[^A-Z]/;  # skip DESTROY and all-cap methods
    $self->throw("invalid attribute method: ->$attr()") unless $ok_field{$attr};
    $self->{$attr} = shift if @_;
    return $self->{$attr};
}  

sub new {
    my($pkg, $cgi) = @_;
    my $self = bless {}, $pkg;
    
    $self->visible(1);
    $self->cgi($cgi);
    my %type_counts = ();
    $self->type_counts(\%type_counts);
    
    return $self;
}

# Set the visibility of the bar - 1 for visible, 0 for hidden.
sub set_visible {
    my ($self, $visible) = @_;
    
    $self->visible($visible);
}

sub set_type_counts {
	my($self, $type_counts) = @_;
	
	$self->type_counts($type_counts);
}

sub generate_type_selector {
    my ($self) = @_;
    
    if (!($self->visible)) {
    	return '';
    }
    
    my $cgi = $self->cgi;
    my $type_counts = $self->type_counts;
    my $pathway_count = '';
    if (defined $type_counts->{"Pathways"}) {
    	$pathway_count = " (" . $type_counts->{"Pathways"} . ")";
    }
    my $reaction_count = '';
    if (defined $type_counts->{"Reactions"}) {
    	$reaction_count = " (" . $type_counts->{"Reactions"} . ")";
    }
    my $protein_count = '';
    if (defined $type_counts->{"Proteins"}) {
    	$protein_count = " (" . $type_counts->{"Proteins"} . ")";
    }
    my $other_count = '';
    if (defined $type_counts->{"Others"}) {
    	$other_count = " (" . $type_counts->{"Others"} . ")";
    }
    
    my @params = $cgi->param();
    my @export_params = ();
    my $param;
    foreach $param (@params) {
    	if (!($param eq 'Pathways' || $param eq 'Reactions' || $param eq 'Proteins' || $param eq 'Others' || $param eq 'Show')) {
    		push(@export_params, $param);
    	}
    }
    my $pathways = $cgi->param('Pathways');
    if (!(defined $pathways)) {
    	$pathways = 0;
    }
    my $reactions = $cgi->param('Reactions');
    if (!(defined $reactions)) {
    	$reactions = 0;
    }
    my $proteins = $cgi->param('Proteins');
    if (!(defined $proteins)) {
    	$proteins = 0;
    }
    my $others = $cgi->param('Others');
    if (!(defined $others)) {
    	$others = 0;
    }
    my $show = $cgi->param('Show');
    if (!$show) {
    	$pathways = 1;
    	$reactions = 1;
    	$proteins = 1;
    	$others = 1;
    }
    my $type_selector = '';
    my $checked;
    $type_selector .= qq(<DIV CLASS="section">\n);
    $type_selector .= qq(\t<TABLE ALIGN="center" WIDTH="85%" CLASS="results_type" CELLPADDING="0" CELLSPACING="0" BORDER="0">);
    $type_selector .= "\t\t" . $cgi->startform(-action => '/cgi-bin/search2', -method => 'GET', -enctype => 'multipart/form-data', -form_name => 'search_results_type_selector') . "\n";
    foreach $param (@export_params) {
    	if (!($param eq 'JustShown') && !($param eq 'PreviousPathways') && !($param eq 'PreviousReactions') && !($param eq 'PreviousProteins') && !($param eq 'PreviousOthers')) {
	    	$type_selector .= "\t\t\t" . $cgi->hidden(-name => $param,-value => $cgi->param($param)) . "\n";
    	}
    }
    $type_selector .= "\t\t\t" . $cgi->hidden(-name => 'PreviousPathways',-value => $pathways) . "\n";
    $type_selector .= "\t\t\t" . $cgi->hidden(-name => 'PreviousReactions',-value => $reactions) . "\n";
    $type_selector .= "\t\t\t" . $cgi->hidden(-name => 'PreviousProteins',-value => $proteins) . "\n";
    $type_selector .= "\t\t\t" . $cgi->hidden(-name => 'PreviousOthers',-value => $others) . "\n";
    $type_selector .= "\t\t\t" . $cgi->hidden(-name => 'JustShown',-value => '1') . "\n";
    $type_selector .= qq(\t\t\t<tr>\n);
    $type_selector .= qq(\t\t\t<td ALIGN="right">\n);
    $checked = '';
    if ($pathways) {
    	$checked = 'CHECKED';
    }
    $type_selector .= qq(\t\t\t<INPUT NAME="Pathways" TYPE="checkbox" VALUE="1" $checked>\n);
    $type_selector .= qq(\t\t\t</td>\n);
    $type_selector .= qq(\t\t\t<td ALIGN="left">\n);
    $type_selector .= "\t\t\t<B>Pathways$pathway_count</B>\n";
    $type_selector .= qq(\t\t\t</td>\n);
    $type_selector .= qq(\t\t\t<td ALIGN="right">\n);
    $checked = '';
    if ($reactions) {
    	$checked = 'CHECKED';
    }
    $type_selector .= qq(\t\t\t<INPUT NAME="Reactions" TYPE="checkbox" VALUE="1" $checked>\n);
    $type_selector .= qq(\t\t\t</td>\n);
    $type_selector .= qq(\t\t\t<td ALIGN="left">\n);
    $type_selector .= "\t\t\t<B>Reactions$reaction_count</B>\n";
    $type_selector .= qq(\t\t\t</td>\n);
    $type_selector .= qq(\t\t\t<td ALIGN="right">\n);
    $checked = '';
    if ($proteins) {
    	$checked = 'CHECKED';
    }
    $type_selector .= qq(\t\t\t<INPUT NAME="Proteins" TYPE="checkbox" VALUE="1" $checked>\n);
    $type_selector .= qq(\t\t\t</td>\n);
    $type_selector .= qq(\t\t\t<td ALIGN="left">\n);
    $type_selector .= "\t\t\t<B>Proteins$protein_count</B>\n";
    $type_selector .= qq(\t\t\t</td>\n);
    $type_selector .= qq(\t\t\t<td ALIGN="right">\n);
    $checked = '';
    if ($others) {
    	$checked = 'CHECKED';
    }
    $type_selector .= qq(\t\t\t<INPUT NAME="Others" TYPE="checkbox" VALUE="1" $checked>\n);
    $type_selector .= qq(\t\t\t</td>\n);
    $type_selector .= qq(\t\t\t<td ALIGN="left">\n);
    $type_selector .= "\t\t\t<B>Others$other_count</B>\n";
    $type_selector .= qq(\t\t\t</td>\n);
    $type_selector .= qq(\t\t\t<td ALIGN="left">\n);
    $type_selector .= "\t\t\t" . $cgi->submit(-name => 'Show', -onMouseover => "ddrivetip('Show selected result types','#DCDCDC', 250)", onMouseout => "hideddrivetip()") . "\n";
    $type_selector .= qq(\t\t\t</td>\n);
    $type_selector .= qq(\t\t\t</tr>\n);
    $type_selector .= "\t\t" . $cgi->endform . "\n";
    $type_selector .= qq(\t</TABLE>\n);
    $type_selector .= qq(</DIV>\n);

	return $type_selector;
}

# Returns a reference to a hash indicating which of the type selectors (Pathway,
# Reaction, Protein, Other) ave been selected by the user.
sub get_type_selectors {
	my($self, $cgi) = @_;
	
    my $show = $cgi->param('Show');
    my %type_selector_hash = ();
    if (!$show) {
    	$type_selector_hash{'Pathways'} = 1;
    	$type_selector_hash{'Reactions'} = 1;
    	$type_selector_hash{'Proteins'} = 1;
    	$type_selector_hash{'Others'} = 1;
    } else {
    	if ($cgi->param('Pathways')) {
    		$type_selector_hash{'Pathways'} = 1;
    	}
    	if ($cgi->param('Reactions')) {
    		$type_selector_hash{'Reactions'} = 1;
    	}
    	if ($cgi->param('Proteins')) {
    		$type_selector_hash{'Proteins'} = 1;
    	}
    	if ($cgi->param('Others')) {
    		$type_selector_hash{'Others'} = 1;
    	}
    }
    
    return \%type_selector_hash;
}

# Returns 1 if any of the type selectors (Pathway,
# Reaction, Protein, Other) have been selected by the user, 0 otherwise.
sub any_type_selectors_set {
	my($self, $cgi) = @_;
	
	my $type_selector_hash = $self->get_type_selectors($cgi);
	if (scalar(keys(%{$type_selector_hash}))>0) {
		return 1;
	}
	
	return 0;
}

# Returns 1 if any type selectors have changed since the previous page, 0 otherwise.
sub any_type_selectors_changed {
	my($self, $cgi) = @_;
	
	if (!($cgi->param('JustShown'))) {
		return 0;
	}
	
    my $pathways = $cgi->param('Pathways');
    if (!(defined $pathways)) {
    	$pathways = 0;
    }
    my $reactions = $cgi->param('Reactions');
    if (!(defined $reactions)) {
    	$reactions = 0;
    }
    my $proteins = $cgi->param('Proteins');
    if (!(defined $proteins)) {
    	$proteins = 0;
    }
    my $others = $cgi->param('Others');
    if (!(defined $others)) {
    	$others = 0;
    }
    my $previous_pathways = $cgi->param('PreviousPathways');
    if (!(defined $previous_pathways)) {
    	$previous_pathways = 0;
    }
    my $previous_reactions = $cgi->param('PreviousReactions');
    if (!(defined $previous_reactions)) {
    	$previous_reactions = 0;
    }
    my $previous_proteins = $cgi->param('PreviousProteins');
    if (!(defined $previous_proteins)) {
    	$previous_proteins = 0;
    }
    my $previous_others = $cgi->param('PreviousOthers');
    if (!(defined $previous_others)) {
    	$previous_others = 0;
    }
    
    if ($pathways != $previous_pathways || $reactions!= $previous_reactions || $proteins != $previous_proteins || $others != $previous_others) {
    	return 1;
    }

	return 0;
}

1;

