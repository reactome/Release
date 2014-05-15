package GKB::HtmlTreeMaker::SpringloadedHierarchy;

use strict;
use vars qw(@ISA);
use GKB::HtmlTreeMaker;
use GKB::Config;

@ISA = qw(GKB::HtmlTreeMaker);

sub tree {
    my ($self) = @_;
#    print "", (caller(0))[3],"\n";
    my $ar = $self->root_instances;
    ($ar && ref($ar) && (ref($ar) eq 'ARRAY')) || $self->throw("Need a ref to array containing instances, got '$ar'.");
#    my $out = qq(<UL CLASS="level0">\n);
    my $out = '';
    foreach my $i (@{$ar}) {
	$out .= $self->_make_tree_html($i);
    }
#    $out .= "</UL>\n";
    return $out;
}

sub _make_tree_html {
    my ($self,$node,$att) = @_;
#    print "", (caller(0))[3],"\n";
    my $out = $self->_node_in_list($node,$att);
    my $sh = $self->stop_hash;
    return $out if ($sh && %{$sh} && $sh->{$node->db_id});
    return $out if ($self->depth && ($self->current_depth >= $self->depth));
    $self->current_depth($self->current_depth + 1);
    my $tmp= '';
    foreach my $cls (grep {$node->is_a($_)} keys %{$self->instructions}) {
	foreach my $attribute (@{$self->instructions->{$cls}->{'attributes'}}) {
	    foreach my $instance (@{&{$self->sorting_function}($self->_valid_attribute_values($node,$attribute))}) {
		$tmp .= $self->_make_tree_html($instance,$attribute);
	    }
	}
	foreach my $attribute (@{$self->instructions->{$cls}->{'reverse_attributes'}}) {
	    foreach my $instance (@{&{$self->sorting_function}($self->_valid_reverse_attribute_values($node,$attribute))}) {
		$tmp .= $self->_make_tree_html($instance,$attribute);
	    }
	}
    }
    if ($tmp) {
	$out .= qq(<UL CLASS="level) . $self->current_depth . qq(">$tmp</UL>\n);
    }
    $self->current_depth($self->current_depth - 1);
    return $out;
}

sub _node_in_list {
    my ($self, $node, $att) = @_;
#    print "", (caller(0))[3],"\n";
    if ($self->current_depth) {
	return qq(<LI>) . $self->_node_hyperlink($node,$att) . qq(</LI>\n);
    }
    return qq(<P CLASS="level0">) . $self->_node_hyperlink($node,$att) . qq(</P>\n);
#    return qq(<LI>) . $self->_node_hyperlink($node,$att) . qq(</LI>\n);
}

sub _node_hyperlink {
    my ($self, $node, $att) = @_;
#    print "", (caller(0))[3],"\n";
    my $att_str = '';
    if ($att) {
	if ($self->show_attribute) {
	    $att_str = qq(<A CLASS="attributename">) . $self->attribute_label($att) . qq(</A> );
	}
    }
    my $class_str = '';
    if (my $class = $self->_highlite($node)) {
	$class_str = qq( CLASS="$class");
    }
    return 
	$self->_expand_or_collapse_button($node) . ' ' .
	$att_str . 
	qq(<A$ {class_str} HREF=\") . 
	$self->_make_url($node) .
	qq(\" ID=\"h_) .
	$node->db_id .
	qq(\">) .
	$node->displayName . qq(</A>);
}

sub super_event_hierarchy_and_direct_subevents_in_sidebar2 {
    my ($self) = @_;
    my $start = time();
    my $dba = $self->instances->[0]->dba;
    my $cgi = $self->cgi;
    my $ontology = $dba->ontology;
#    print "", (caller(0))[3],"\n";
    my %ascending_instructions = ('Event' => {'reverse_attributes' => [qw(hasComponent hasMember hasSpecialisedForm hasEvent)]});

    my %descending_instructions = ('EquivalentEventSet' => {'attributes' => ['hasMember']},
				   'ConceptualEvent' => {'attributes' => ['hasSpecialisedForm']},
				   'Reaction' => {'attributes' => ['hasMember']},
				   'BlackBoxEvent' => {'attributes' => ['hasComponent']},
				   'Pathway' => {'attributes' => ['hasComponent','hasEvent']});

    my (%roots,%followed_path,%hlh);
    foreach my $event (@{$self->instances}) {
	my $ar = $event->follow_class_attributes(-INSTRUCTIONS => \%ascending_instructions);
	LEVEL2: foreach my $i (@{$ar}) {
	    $followed_path{$i->db_id} = $i;
	    foreach my $class (grep {$i->is_a($_)} keys %ascending_instructions) {
		foreach my $att (@{$ascending_instructions{$class}->{'attributes'}}) {
		    $i->attribute_value($att)->[0] && next LEVEL2;
		}
		foreach my $rev_att (@{$ascending_instructions{$class}->{'reverse_attributes'}}) {
		    $i->reverse_attribute_value($rev_att)->[0] && next LEVEL2;
		}
	    }
	    # if we reach here $i is a "root" instance
	    $roots{$i->db_id} = $i;
	}
	$hlh{$event->db_id}++;
    }
    my @expanded = $cgi->param('E');
    push @expanded, map {$_->db_id} @{$self->instances};
    # Don't want to expand the node which the user wants to collapse
    # Note that the path from the focus node to roots is in %followed_path already.
    my %check_list;
    if (my $collapse =  $cgi->param('C')) {
	$check_list{$collapse}++;
	# Put the "descendants" of the collapsed node on the check_list to avoid
	# them being displayed if they happen to be one the expanded list.
	# Path to the focus instance will be unaffected.
	foreach my $i (@{$dba->fetch_instance_by_db_id($collapse)}) {
	    map {$check_list{$_->db_id}++}
		 @{$i->follow_class_attributes(-INSTRUCTIONS => \%descending_instructions)};
# This code just puts "kids" and would result in "memory", i.e. when expanding afer
# collapsing again the nodes which were expended then would appear expanded also now.
#	    foreach my $class (grep {$i->is_a($_)} keys %descending_instructions) {
#		foreach my $att (@{$descending_instructions{$class}->{'attributes'}}) {
#		    map {$check_list{$_->db_id}++} @{$i->attribute_value($att)};
#		}
#		foreach my $rev_att (@{$descending_instructions{$class}->{'reverse_attributes'}}) {
#		    map {$check_list{$_->db_id}++} @{$i->reverse_attribute_value($rev_att)};
#		}
#	    }
	}
    }
    my %expanded;
    foreach my $id (grep {! $check_list{$_}} @expanded) {
	if (my $i = $dba->fetch_instance_by_db_id($id)->[0]) {
	    $expanded{$id}++;
	    $followed_path{$id}++;
	    # Tuck the db_ids of all the "kids" of all nodes to be expanded into %followed_path
	    foreach my $class (grep {$i->is_a($_)} keys %descending_instructions) {
		foreach my $att (@{$descending_instructions{$class}->{'attributes'}}) {
		    map {$followed_path{$_->db_id}++} @{$i->attribute_value($att)};
		}
		foreach my $rev_att (@{$descending_instructions{$class}->{'reverse_attributes'}}) {
		    map {$followed_path{$_->db_id}++} @{$i->reverse_attribute_value($rev_att)};
		}
	    }
	}
    }
    if (my @expand_all = $cgi->param('EA')) {
	foreach my $id (@expand_all) {
	    if (my $i = $dba->fetch_instance_by_db_id($id)->[0]) {
		$expanded{$id}++;
		$followed_path{$id}++;
		foreach my $ii (@{$i->follow_class_attributes(-INSTRUCTIONS => \%descending_instructions)}) {
		    $expanded{$ii->db_id}++;
		    $followed_path{$ii->db_id}++;
		}
	    }
	}
    }
    # Not sure if this is necessary
    $cgi->delete('EA');

    # Have to do that in case the keys %expanded returns nothing and the old value would remain.
    $cgi->delete('E');
    # will be used later by _make_url
    $cgi->param('E', keys %expanded);

    if ($cgi && $cgi->param('SHOW_HIERARCHY_TYPES')) {
	$self->show_attribute(1);
	$self->attribute_label('hasComponent', 'c');
	$self->attribute_label('hasMember', 'm');
	$self->attribute_label('hasSpecialisedForm', 's');
	$self->attribute_label('hasEvent', '');
    } else {
#	$self->attribute_label('hasComponent', '');
#	$self->attribute_label('hasInstance', '');
    }
    $self->instructions(\%descending_instructions);
    $self->followed_path_hash(\%followed_path);
    my @ordered_roots;
    my %seen;
#    if (my $tmp = $cgi->param('RORDER')) {
#	foreach my $id (split(/,/,$tmp)) {
#	    if ($roots{$id}) {
#		push @ordered_roots, $roots{$id};
#		$seen{$id}++;
#	    }
#	}
#    }
    

    push @ordered_roots, sort {$a->db_id <=> $b->db_id} grep {! $seen{$_->db_id}} values %roots;
    $self->root_instances(\@ordered_roots);
    $self->highlite1_hash(\%hlh);
    $self->highlite1_class('current');
    $self->default_class('sidebar');
    my $str = '/cgi-bin/frontpage?DB=' . ($self->urlmaker->param('DB'))[0];
#    print qq(<PRE>), (caller(0))[3],"\t",  time() - $start, qq(</PRE>\n);

#    my $out = $cgi->start_form(-method =>'GET');
#    foreach my $i (@{$self->instances}) {
#	$out .= qq(<INPUT TYPE="hidden" NAME="ID" VALUE=\") . $i->db_id . qq(\" />);
#    }
#    $out .= $cgi->hidden(-name => 'E', -value => [keys %expanded]);
#    $out .= $cgi->hidden(-name => 'RORDER', -value => [map {$_->db_id} @{$self->root_instances}]);
#    $out .= $cgi->end_form;
    return 
	qq(<P CLASS="level0"><A CLASS="sidebar" HREF="$str">[$PROJECT_NAME Home]</A></P>\n) .
	$self->tree .
	$self->_hierarchy_types_button;
}

sub _is_expanded_node {
    my ($self,$node) = @_;
    my $fph = $self->followed_path_hash;
    my $descending_instructions = $self->instructions;
    foreach my $class (grep {$node->is_a($_)} keys %{$descending_instructions}) {
	foreach my $att (@{$descending_instructions->{$class}->{'attributes'}}) {
	    foreach my $i (@{$node->attribute_value($att)}) {
		$fph->{$i->db_id} || return undef;
	    }
	}
	foreach my $rev_att (@{$descending_instructions->{$class}->{'reverse_attributes'}}) {
	    foreach my $i (@{$node->reverse_attribute_value($rev_att)}) {
		$fph->{$i->db_id} || return undef;
	    }
	}
    }
    return 1;
}

sub _node_has_descendants {
    my ($self,$node) = @_;
    my $descending_instructions = $self->instructions;
    foreach my $class (grep {$node->is_a($_)} keys %{$descending_instructions}) {
	foreach my $att (@{$descending_instructions->{$class}->{'attributes'}}) {
	    return 1 if (@{$node->attribute_value($att)});
	}
	foreach my $rev_att (@{$descending_instructions->{$class}->{'reverse_attributes'}}) {
	    return 1 if (@{$node->reverse_attribute_value($rev_att)});
	}
    }
    return undef;
}

sub _expand_or_collapse_button {
    my ($self,$node) = @_;
    if ($self->_is_expanded_node($node)) {
	if ($self->_node_has_descendants($node)) {
	    return '<A CLASS="ecbutton" HREF="' . $self->_make_url . '&C=' . $node->db_id . '">-</A>';
	} else {
	    return '<A CLASS="ecbutton">&nbsp;</A>';
	}
    } else {
	return
	    '<A CLASS="ecbutton" HREF="' . $self->_make_url . '&E=' . $node->db_id . '">+</A>' .
	    '<A CLASS="ecbutton" HREF="' . $self->_make_url . '&EA=' . $node->db_id . '">#</A>';
	
    }
}

=head
sub _expand_or_collapse_button {
    my ($self,$node) = @_;
    my $rorder = 'new Array(' . join(',', map {'"' . $_->db_id . '"'} @{$self->root_instances}) . ')';
    if ($self->_is_expanded_node($node)) {
	if ($self->_node_has_descendants($node)) {
	    my $expanded = 'new Array(' . join(',', map {'"' . $_ . '"'} $self->cgi->param('E')) . ')';
	    return
		qq(<A CLASS=\"ecbutton\" ONCLICK=\') .
		sprintf(qq/setValue("%d","%s","%d");/,
			$self->instances->[0]->db_id,
			'C',
			$node->db_id) .
		sprintf(qq/setMultiValue("%d","%s",%s);/,
			$self->instances->[0]->db_id,
			'E',
			$expanded) .
		sprintf(qq/setMultiValue("%d","%s",%s);/,
			$self->instances->[0]->db_id,
			'RORDER',
			$rorder) .
		sprintf(qq/submitForm("%d");/,
			$self->instances->[0]->db_id) .
	        qq(\'>-</A>);
	} else {
	    return '<A CLASS="ecbutton">&nbsp;</A>';
	}
    } else {
	my $expanded = 'new Array(' . join(',', map {'"' . $_ . '"'} $self->cgi->param('E'),$node->db_id) . ')';
	return
		qq(<A CLASS=\"ecbutton\" ONCLICK=\') .
		sprintf(qq/setValue("%d","%s","%s");/,
			$self->instances->[0]->db_id,
			'C',
			"") .
		sprintf(qq/setMultiValue("%d","%s",%s);/,
			$self->instances->[0]->db_id,
			'E',
			$expanded) .
		sprintf(qq/setMultiValue("%d","%s",%s);/,
			$self->instances->[0]->db_id,
			'RORDER',
			$rorder) .
		sprintf(q/submitForm("%d");/,
			$self->instances->[0]->db_id) .	
	        qq(\'>+</A>);
    }
}
=cut

sub _make_url {
    my $self = shift;
    push @_, @{$self->instances} unless (@_);
    return 
	$self->urlmaker->urlify(@_) . 
	'&RORDER=' . join(",",map {$_->db_id} @{$self->root_instances}) . 
	join("", map {"&E=$_"} $self->cgi->param('E'));
}

sub _hierarchy_types_button {
    my $self = shift;
    my $out;
    my $cgi = $self->cgi;
    $out = $cgi->start_form(-method =>'POST');
    $out .= $cgi->hidden(-name => 'DB', -value => $cgi->param('DB'));
    if (@{$self->instances}) {
	foreach my $i (@{$self->instances}) {
	    $out .= qq(<INPUT TYPE="hidden" NAME="ID" VALUE=\") . $i->db_id . qq(\" />);
	}
    } else {
	$out .= qq(<INPUT TYPE="hidden" NAME="ID" VALUE=\") . $self->instances->[0]->db_id . qq(\" />);
    }
    my @tmp = $cgi->param('E');
    $out .= $cgi->hidden(-name => 'E', -default => \@tmp);
    @tmp = $cgi->param('C');
    $out .= $cgi->hidden(-name => 'C', -default => \@tmp);
    @tmp = $cgi->param('RORDER');
    $out .= $cgi->hidden(-name => 'RORDER', -default => \@tmp);
    if (my $tmp = $cgi->param('SHOW_HIERARCHY_TYPES')) {
	$cgi->delete('SHOW_HIERARCHY_TYPES');
	$out .= $cgi->submit(-name => qq(Hide hierarchy types), -class => 'sidebar');
	$cgi->param('SHOW_HIERARCHY_TYPES',$tmp);
    } else {
	$cgi->delete('SHOW_HIERARCHY_TYPES');
	$out .= $cgi->hidden(-name => 'SHOW_HIERARCHY_TYPES', -value => 1);
	$out .= $cgi->submit(-name => qq(Show hierarchy types), -class => 'sidebar');
    }
    $out .= $cgi->end_form;
    return $out;
}

1;
