package GKB::HtmlTreeMaker;

use strict;
use GKB::Instance;
use vars qw(@ISA $AUTOLOAD %ok_field);
use Bio::Root::Root;
use GKB::Utils;
use GKB::Config;
#use Data::Dumper;

@ISA = qw(Bio::Root::Root);

for my $attr
    (qw(
	depth
	current_depth
	stop_hash
	highlite1_hash
	highlite1_class
	highlite2_hash
	highlite2_class
	default_class
	instances
	root_instances
	urlmaker
	followed_path_hash
	sorting_function
	instructions
	show_attribute
	cgi
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
    my($pkg, @args) = @_;
    my $self = bless {}, $pkg;
    my ($depth,
	$stop_hash,
	$highlite1_hash,
	$highlite1_class,
	$highlite2_hash,
	$highlite2_class,
	$default_class,
	$instances,
	$root_instances,
	$followed_path_hash,
	$sorting_fn_ref,
	$labels,
	$instructions,
	$cgi,
	$urlmaker) = $self->_rearrange([qw(
 					 DEPTH
					 STOP_HASH
				         HIGHLITE1_HASH
				         HIGHLITE1_CLASS
				         HIGHLITE2_HASH
				         HIGHLITE2_CLASS
				         DEFAULT_CLASS
					 INSTANCES
					 ROOT_INSTANCES
				         FOLLOWED_PATH_HASH
					 SORTING_FN
					 ATTRIBUTE_LABELS
					 INSTRUCTIONS
					 CGI
				         URLMAKER)], @args);
    $depth && $self->depth($depth);
    $stop_hash && %{$stop_hash} && $self->stop_hash($stop_hash);
    $highlite1_hash && %{$highlite1_hash} && $self->highlite1_hash($highlite1_hash);
    $highlite1_class && $self->highlite1_class($highlite1_class);
    $highlite2_hash && %{$highlite2_hash} && $self->highlite2_hash($highlite2_hash);
    $highlite2_class && $self->highlite2_class($highlite2_class);
    $default_class && $self->default_class($default_class);
    $instances && @{$instances} && $self->instances($instances);
    $root_instances && @{$root_instances} && $self->root_instances($root_instances);
    $followed_path_hash && %{$followed_path_hash} && $self->followed_path_hash($followed_path_hash);
    $instructions && %{$instructions} && $self->instructions($instructions);
    $urlmaker && $self->urlmaker($urlmaker);
    $cgi && $self->cgi($cgi);
    $self->current_depth(0);
    if ($sorting_fn_ref) {
	$self->sorting_function($sorting_fn_ref);
    } else {
	$self->sorting_function(sub {return $_[0]});
    }
    if ($labels) {
	$self->{'attribute_label'} = $labels;
    } else {
	$self->{'attribute_label'} = {};
    }
    return $self;
}

sub tree {
    my ($self) = @_;

#    print "", (caller(0))[3],"\n";
    my $ar = $self->root_instances;
    ($ar && ref($ar) && (ref($ar) eq 'ARRAY')) || $self->throw("Need a ref to array containing instances, got '$ar'.");
    my $out = qq(<UL CLASS="level0">\n);
    foreach my $i (@{$ar}) {
	$out .= $self->_make_tree_html($i);
    }
    $out .= "</UL>\n";
    return $out;
}

sub attribute_label {
    my ($self,$att) = (shift,shift);
    if (@_) {
	$self->{'attribute_label'}->{$att} = shift;
    }
    return (exists $self->{'attribute_label'}->{$att} ? $self->{'attribute_label'}->{$att} : "[$att]");
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
    $self->urlmaker || $self->throw("Need URLMaker object.");
    my $hyperlink = qq($ {att_str}<A$ {class_str} HREF=\") . $self->urlmaker->urlify($node) . qq(\">) . $node->displayName . qq(</A>);
    if (defined $node->doi && scalar(@{$node->doi})>0 && defined $node->doi->[0]) {
    	$hyperlink .=  "<A CLASS=\"DOI\" ONMOUSEOVER='ddrivetip(\"" . $node->doi->[0] . "\",\"#FFFFFF\",250)' ONMOUSEOUT='hideddrivetip()'> (DOI)</A>";
    }
    return $hyperlink;
}

sub _highlite {
    my ($self,$node) = @_;
#    print "", (caller(0))[3],"\n";
    my $hlh = $self->highlite1_hash;
    if ($hlh && %{$hlh} && $hlh->{$node->db_id}) {
	return $self->highlite1_class;
    }
    $hlh = $self->highlite2_hash;
    if ($hlh && %{$hlh} && $hlh->{$node->db_id}) {
	return $self->highlite2_class;
    }
    return $self->default_class;
}

sub _node_in_list {
    my ($self, $node, $att) = @_;
#    print "", (caller(0))[3],"\n";
    if ($self->current_depth) {
	return qq(<LI>- ) . $self->_node_hyperlink($node,$att) . qq(</LI>\n);
    }
#    return qq(<P CLASS="level0">) . $self->_node_hyperlink($node,$att) . qq(</P>\n);
    return qq(<LI>) . $self->_node_hyperlink($node,$att) . qq(</LI>\n);
}

sub _valid_attribute_values {
    my ($self, $node, $att) = @_;
#    print "", (caller(0))[3],"\n";
    my $fph = $self->followed_path_hash;
    if ($fph && %{$fph}) {
	return [grep {$fph->{$_->db_id}} @{$node->attribute_value($att)}];
    }
    return $node->attribute_value($att);
}

sub _valid_reverse_attribute_values {
    my ($self, $node, $att) = @_;
#    print "", (caller(0))[3],"\n";
    my $fph = $self->followed_path_hash;
    if ($fph && %{$fph}) {
	return [grep {$fph->{$_->db_id}} @{$node->reverse_attribute_value($att)}];
    }
    return $node->reverse_attribute_value($att);
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

sub participant_in_event_tree {
    my ($self,$instances,$focus_species) = @_;
    my $start = time();
#    print qq(<PRE>\$focus_species = $focus_species</PRE>\n);
    (ref($instances) && (ref($instances) eq 'ARRAY')) ||
	$self->throw("Need a reference to an array of GKB::Instance:s, got '$instances'.");
    # Find all events where any given PhysicalEntity is involved
    my %shoots;
    foreach my $instance (@{$instances}) {
	my $ar = $instance->follow_class_attributes
	    (-INSTRUCTIONS => {'PhysicalEntity' => {'reverse_attributes' => [qw(hasComponent hasMember hasCandidate repeatedUnit input output physicalEntity)]},
			       'CatalystActivity' => {'reverse_attributes' => [qw(catalystActivity)]}
			   },
	     -OUT_CLASSES => ['Event']
	     );
	#map {print qq(<PRE>) . $_->extended_displayName . qq(</PRE>\n)} @{$ar};
	my $aar = &GKB::Utils::collapse_Events_to_focus_taxon($ar,$focus_species);
	foreach (@{$aar}) {
	    $shoots{$_->[0]->db_id} = $_;
	    #print "<PRE>", join("\t", map {$_->extended_displayName} $instance,@{$_}), "</PRE>\n";
	}
    }

    # Find "root" Events and the path to them
    my (%roots,%path);
    foreach my $event (map {$_->[0]} values %shoots) {
	my $ar = $event->follow_class_attributes(-INSTRUCTIONS => {'Event' => {'reverse_attributes' => [qw(hasComponent hasMember hasSpecialisedForm hasEvent)]}});
	foreach (@{$ar}) {
	    $path{$_->db_id} = $_;
	    if (! $_->reverse_attribute_value('hasSpecialisedForm')->[0]
		&& ! $_->reverse_attribute_value('hasComponent')->[0]
		&& ! $_->reverse_attribute_value('hasEvent')->[0]
		&& ! $_->reverse_attribute_value('hasMember')->[0]) {
		$roots{$_->db_id} = $_;
	    }
	}
    }

    $self->attribute_label('hasComponent', '');
    $self->attribute_label('hasMember', '');
    $self->attribute_label('hasSpecialisedForm', '');
    $self->attribute_label('hasEvent', '');
    $self->instructions({'ConceptualEvent' => {'attributes' => ['hasSpecialisedForm']},
			 'EquivalentEventSet' => {'attributes' => ['hasMember']},
			 'Reaction' => {'attributes' => ['hasMember']},
			 'BlackBoxEvent' => {'attributes' => ['hasComponent']},
			 'Pathway' => {'attributes' => ['hasComponent','hasEvent']}});
    $self->highlite1_hash(\%shoots);
    $self->highlite1_class('bold');
    $self->followed_path_hash(\%path);
    my $out = '';
    foreach my $i (values %roots) {
	$out .= $self->_participant_in_event_tree_make_html($i);
    }
#    print qq(<PRE>), (caller(0))[3],"\t",  time() - $start, qq(</PRE>\n);
    return $out;
}

sub _participant_in_event_tree_make_html {
    my ($self,$node,$att) = @_;
#    print "", (caller(0))[3],"\n";
    my $out = $self->_participant_in_event_tree_node_in_list($node,$att);
    $self->current_depth($self->current_depth + 1);
    my $tmp= '';
    foreach my $cls (grep {$node->is_a($_)} keys %{$self->instructions}) {
	foreach my $attribute (@{$self->instructions->{$cls}->{'attributes'}}) {
	    foreach my $instance (@{&{$self->sorting_function}($self->_valid_attribute_values($node,$attribute))}) {
		$tmp .= $self->_participant_in_event_tree_make_html($instance,$attribute);
	    }
	}
	foreach my $attribute (@{$self->instructions->{$cls}->{'reverse_attributes'}}) {
	    foreach my $instance (@{&{$self->sorting_function}($self->_valid_reverse_attribute_values($node,$attribute))}) {
		$tmp .= $self->_participant_in_event_tree_make_html($instance,$attribute);
	    }
	}
    }
    if ($tmp) {
	$out .= qq(<UL CLASS="level) . $self->current_depth . qq(">$tmp</UL>\n);
    }
    $self->current_depth($self->current_depth - 1);
    return $out;
}

sub _participant_in_event_tree_node_hyperlink {
    my ($self, $node, $att) = @_;
#    print "", (caller(0))[3],"\n";
    my $att_str = '';
    my $taxon_str = '';
    if ($att) {
	if ($self->show_attribute) {
	    $att_str = qq(<A CLASS="attributename">) . $self->attribute_label($att) . qq(</A> );
	}
    }
    if (my $class = $self->_highlite($node)) {
	my $ar = $self->highlite1_hash->{$node->db_id};
	my $tmp = &GKB::Utils::hyperlinked_collapsed_Events($self->urlmaker,$ar);
	$tmp =~ s/<A HREF=/<A CLASS="$class" HREF=/gi;
	return qq($att_str$tmp);
#	return qq($ {att_str}<A CLASS="$class">) . &GKB::Utils::hyperlinked_collapsed_Events($self->urlmaker,$ar) . qq(</A>);
    }
    return qq($ {att_str}<A HREF=\") . $self->urlmaker->urlify($node) . qq(\">) . $node->displayName . qq(</A>);
}

sub _participant_in_event_tree_node_in_list {
    my ($self, $node, $att) = @_;
#    print "", (caller(0))[3],"\n";
    if ($self->current_depth) {
	return qq(<LI>- ) . $self->_participant_in_event_tree_node_hyperlink($node,$att) . qq(</LI>\n);
    }
    return qq(<P CLASS="level0">) . $self->_participant_in_event_tree_node_hyperlink($node,$att) . qq(</P>\n);
}

1;
