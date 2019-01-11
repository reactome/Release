package GKB::PrettyInstance;

use strict;
use warnings;

use vars qw(@ISA);
use GKB::Instance;
use GKB::HtmlTreeMaker;
use GKB::HtmlTreeMaker::SpringloadedHierarchy;
use GKB::HtmlTreeMaker::NoReactions;
use GKB::Utils;
use GKB::WebUtils;
use GKB::Config;
use GKB::ReactionMap;
use GKB::Utils::InstructionLibrary;
use GKB::DTreeMaker::EntityHierarchy::Downwards;
use GKB::DTreeMaker::EntityHierarchy::ComponentsAndRepeatedUnits::Downwards;
use GKB::DTreeMaker::EventHierarchy;
use Log::Log4perl qw/get_logger/;
Log::Log4perl->init(\$LOG_CONF);

@ISA = qw(GKB::Instance);

sub new {
    my ($pkg,@args) = @_;
    my ($self,$urlmaker,$subclassify,$wu) = $pkg->SUPER::_rearrange
	([qw(
	     INSTANCE
	     URLMAKER
	     SUBCLASSIFY
	     WEBUTILS
     )], @args);
    (ref($self) && $self->isa("GKB::Instance")) ||
	$pkg->throw("Need GKB::Instance, got '$self'.");
    if ($subclassify && ($pkg eq 'GKB::PrettyInstance') && _subclass_loaded($self->class)) {
        if (defined $self->debug && $self->debug > 1) {
            $DB::single=1;
        }
	my $destination = "GKB::PrettyInstance::" . $self->class;
	return $destination->new(@args);
    }
    bless $self, $pkg;
    if ($urlmaker) {
	$self->urlmaker($urlmaker);
    } elsif ($wu && $wu->urlmaker) {
	$self->urlmaker($wu->urlmaker);
    }
#    $cgi && $self->cgi($cgi);
    $wu && $self->webutils($wu);
    $self->_make_attribute_registry;
    return $self;
}

sub swap_pathwaybrowserdata_for_eventbrowser {
    my ($self, $pathwaybrowserdata_string) = @_;
    my $eventbrowser_string = $pathwaybrowserdata_string;
    $eventbrowser_string =~ s/entitylevelview\/pathwaybrowserdata/eventbrowser/g;
    return $eventbrowser_string;
}

sub _subclass_loaded {
    return $main::GKB::PrettyInstance::{$_[0] . '::'};
}

sub html_table_tmp {
    my ($self,@args) = @_;
    return $self->html_table(@args);
}

sub prettyfy_instance {
    my ($self,$instance) = @_;
    $self->debug && print join("\t",(caller(0))[3], $instance, $instance->class, $instance->db_id), "\n";
    ($instance && $instance->isa("GKB::Instance")) || $self->throw("Need GKB::Instance, got '$instance'.");
    unless ($instance->isa("GKB::PrettyInstance")) {
        $DB::single=1;
	GKB::PrettyInstance->new(
				 -INSTANCE => $instance,
				 -URLMAKER => $self->urlmaker,
				 -DEBUG => $self->debug,
				 -SUBCLASSIFY => $self->isa("GKB::PrettyInstance::".$self->class),
				 -WEBUTILS => $self->webutils,
				 );
    }
    return $instance;
}

sub webutils {
    my ($self) = shift;
    if (@_) {
	$self->{'webutils'} = shift;
    }
    return $self->{'webutils'};
}

sub cgi {
    my ($self) = shift;
    unless ($self->webutils) {
	$self->throw("No WebUtils instance.");
    }
    return $self->webutils->cgi;
#    if (@_) {
#	$self->{'cgi'} = shift;
#    }
#    return $self->{'cgi'};
}

# Historical method for backward compatibility
sub all {
    my ($self) = @_;
    $self->debug && print join("\t", (caller(0))[3], $self,  $self->class, ($self->db_id || '')), "\n";
    my @out;
    foreach my $att ($self->list_valid_attributes) {
	($att =~ /^_/ or $att =~ /_id$/) && next;
	if ($self->can("handle_attribute_$att")) {
	    my $fn = "handle_attribute_$att";
	    push @out, [$att, eval "\$self->$fn"];
	} elsif ($self->ontology->class($self->class)->{'attribute'}->{$att}->{'type'} eq 'db_instance_type') {
	    push @out, [$att,
			map {$self->prettyfy_instance($_)->small} @{$self->attribute_value($att)}];
	} else {
	    push @out, [$att,
			map {[$_, undef]} grep {defined $_} @{$self->attribute_value($att)}];
	}
    }
    foreach my $att ($self->list_valid_reverse_attributes) {
	push @out, ["($att)",
		   map {$self->prettyfy_instance($_)->small} @{$self->referer_value($att)}];
    }
    return \@out;
}

sub _make_attribute_registry {
    my ($self) = @_;
    map {$self->{'_attribute_registry'}->{'attributes'}->{$_}++}
	grep {! /^_/} grep {! /_id$/} $self->list_valid_attributes;
    map {$self->{'_attribute_registry'}->{'reverse_attributes'}->{$_}++} $self->list_valid_reverse_attributes;
}

sub _deregister_attribute {
    my ($self,$att) = @_;
    delete $self->{'_attribute_registry'}->{'attributes'}->{$att};
}

sub _empty_attribute_registry {
    my ($self) = @_;
    $self->{'_attribute_registry'}->{'attributes'} = {};
    $self->{'_attribute_registry'}->{'reverse_attributes'} = {};
}

sub _deregister_reverse_attribute {
    my ($self,$att) = @_;
    delete $self->{'_attribute_registry'}->{'reverse_attributes'}->{$att};
}

sub registered_attributes_as_rows {
    my ($self) = @_;
    my $out = '';
    foreach (keys %{$self->{'_attribute_registry'}->{'attributes'}}) {
	if ($self->is_recursive_attribute($_)) {
	    $out .= $self->make_attribute_tree_as_2rows(-TITLE => $_, -ATTRIBUTES => [$_]);
	} else {
	    $out .= $self->attribute_value_as_1row($_,[$_]);
	}
	$self->_deregister_attribute($_);
    }
    return $out;
}

sub registered_reverse_attributes_as_rows {
    my ($self) = @_;
    my $out = '';
    foreach (keys %{$self->{'_attribute_registry'}->{'reverse_attributes'}}) {
        $out .= $self->reverse_attribute_value_as_1row("($_)",[$_]);
        $self->_deregister_reverse_attribute($_);
    }
    return $out;
}

sub p_print {
    my $self = shift;
    print qq(<PRE>@_</PRE>\n);
}

sub all_registered_attributes_as_rows {
    my ($self) = @_;
    return $self->registered_attributes_as_rows . $self->registered_reverse_attributes_as_rows;
}

# Subclasses have to contain the implementation if necessary. Basically the point is to have
# a method which defaults to html_table_rows if it itself is not implemented.
sub few_details {
    my $self = shift;
    return $self->html_table_rows(@_);
}

sub html_table_rows {
    my ($self) = @_;
    $DB::single=1;
    $self->debug && print join("\t", (caller(0))[3],$self,  $self->id_string), "\n";
    return $self->all_registered_attributes_as_rows;
}

sub html_table_rows_if_necessary {
    my $self = shift;
#    return $self->html_table_rows . $self->_view_switch_html;
    return $self->html_table_rows;
}

sub html_table_rows_if_necessary_1 {
    my $self = shift;
    my $start = time();
    unless ($self->can("create_image")) {
	# Dont bother with the expand/collapse button if theres no
	# reactionmap
	return $self->html_table_rows;
    }
    my $cgi = $self->cgi;
    my %preferences = $cgi->cookie('preferences');
    my $out;
    my $db_id = $self->db_id;
    if (lc($preferences{'description'}) eq 'off') {
	# Description hidden
	$out .=
	    qq(<TR><TD ALIGN="left" CLASS="descButtonCell">) .
	    qq(<A ONMOUSEOVER="ddrivetip('Show description','#DCDCDC',150)" ONMOUSEOUT="hideddrivetip()" ONCLICK="setValue($db_id,'description','on');setValue($db_id,'ID',$db_id);submitForm($db_id);"><IMG SRC="/icons/plus-box.gif" HEIGHT="10" WIDTH="10" BORDER="0"></A>) .
	    qq(</TD></TR>\n);
    } else {
	# Description shown
	$out .=
	    qq(<TR><TD COLSPAN="2" ALIGN="left" CLASS="descButtonCell"><A ONMOUSEOVER="ddrivetip('Hide description','#DCDCDC',150)" ONMOUSEOUT="hideddrivetip()" ONCLICK="setValue($db_id,'description','off');setValue($db_id,'ID',$db_id);submitForm($db_id);"><IMG SRC="/icons/minus-box.gif" HEIGHT="10" WIDTH="10" BORDER="0"></A></TD></TR>\n) .
	    $self->html_table_rows .
	    $self->_view_switch_html;
    }
#    print qq(<PRE>), (caller(0))[3],"\t",  time() - $start, qq(</PRE>\n);
    return $out;
}

sub hyperlinked_string {
    my ($self,$str) = @_;

    return qq(<A HREF=") . $self->urlify . qq(">$str</A>);
}

sub hyperlinked_displayName {
    my $self = shift;
    return $self->hyperlinked_string($self->displayName || $self->class_and_id);
}

sub hyperlinked_extended_displayName {
    my $self = shift;
    return $self->hyperlinked_string
	('['.$self->class.':'.$self->db_id.'] ' .
	 ($self->displayName || '')
	 . (($self->is_valid_attribute('species') && $self->is_instance_type_attribute('species') && $self->Species->[0]) ?  ' [' . $self->Species->[0]->displayName . ']' : '')
	 );
}

# as above but given protege id if present.
sub hyperlinked_extended_displayName2 {
    my $self = shift;
    return $self->hyperlinked_string('['.$self->class.':'. ($self->attribute_value('_Protege_id')->[0] || $self->db_id) . '] '.($self->displayName || ''));
}

sub soft_displayName {
    my $self = shift;
    # Should be implemented by subclasses
    return $self->hyperlinked_string('['.$self->class.'] '.($self->displayName || ''));
}

sub class_and_id_as_row {
    my ($self) = @_;
    return qq(<TR><TH CLASS="id">Class:Id</TH><TD CLASS="id">) . $self->class_and_id . qq(</TD></TR>\n);
}

# Historical method for backward compatibility
sub small {
    my ($self) = @_;
    $self->debug && print join("\t", (caller(0))[3], $self,  $self->class, ($self->db_id || '')), "\n";
    return [$self->attribute_value('_displayName')->[0], $self->urlify];
}

sub urlmaker {
    my ($self) = shift;
    if (@_) {
	$self->{'urlmaker'} = shift;
    }
    return $self->{'urlmaker'};
}

sub urlify {
    my ($self,@args) = @_;
    $self->debug && print join("\t", (caller(0))[3], $self,  $self->class, ($self->db_id || '')), "\n";
    @args = ($self) unless @args;
    if (my $urlmaker = $self->urlmaker) {
	$urlmaker->urlify(@args);
    } else {
	return @args;
    }
}

sub make_attribute_tree {
    my ($self,@args) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my ($attributes,$classes,$depth,$reverse) =
	$self->_rearrange([qw(
			      ATTRIBUTES
			      CLASSES
			      DEPTH
			      REVERSE
			      )],@args);
    $self->debug && print "", (caller(0))[3], "\n";
    my $str = $self->_make_tree_html(undef,undef,$attributes,$reverse,{},{},$depth);
    return \$str;
}

sub _make_tree_html {
    my ($self,$node,$att_name,$attributes,$reverse_flag,
	$seen_hr,$followed_path,$max_depth,$curr_depth,$use_extended_displayName) = @_;
    $self->debug && print "<BR />", join("\t", (caller(0))[3], ($node ? $node->id_string : $self->id_string)), "\n";
    my $out;
    $curr_depth ||= 0;
    $seen_hr ||= {};
    my ($fn1,$fn2);
    if ($reverse_flag) {
	$fn1 = 'is_valid_reverse_attribute';
	$fn2 = 'reverse_attribute_value';
    } else {
	$fn1 = 'is_valid_attribute';
	$fn2 = 'attribute_value';
    }
    if ($node) {
	$self->prettyfy_instance($node);
        if ($att_name) {
	    if (@{$attributes} == 1) {
	        $att_name = '';
	    } else {
		$att_name = $reverse_flag ?
		    "<A CLASS=\"attributename\">[<-$att_name]</A> " :
		    "<A CLASS=\"attributename\">[$att_name->]</A> ";
	    }
        }
	my $tmp = ($use_extended_displayName)
	    ? $node->hyperlinked_extended_displayName
	    : $node->hyperlinked_displayName;
	if ($node == $self) {
	    return qq(<LI>$att_name<I>$tmp</I></LI>\n);
	} elsif ($seen_hr->{$node->db_id || "$node"}) {
	    return qq(<LI>$att_name$tmp</LI>\n);
	} elsif (defined $max_depth && ($curr_depth >= $max_depth)) {
	    my $has_child;
	    foreach my $attribute (grep {$node->$fn1($_)} @{$attributes}) {
		if ($node->$fn2($attribute)->[0]) {
		    $has_child = 1;
		    last;
		}
	    }
	    return ($has_child) ? qq(<LI>$att_name<B>$tmp</B></LI>\n) : qq(<LI>$att_name$tmp</LI>\n);
	} else {
	    $out = qq(<LI>$att_name$tmp</LI>\n);
	}
    } else {
	$node = $self;
    }
    $seen_hr->{$node->db_id || "$node"}++;
    $curr_depth++;
    my $tmp;
    foreach my $attribute (grep {$node->$fn1($_)} @{$attributes}) {
	my @list = ($reverse_flag) ?
	    ((%{$followed_path}) ?
	     grep {$followed_path->{$_->db_id}} @{$node->$fn2($attribute)} :
	     @{$node->$fn2($attribute)}) :
	     @{$node->$fn2($attribute)};
	@list = @{$self->grep_for_instances_with_focus_species(\@list)};
	foreach my $instance2 (@list) {
	    $tmp .= $self->_make_tree_html($instance2,$attribute,
					   $attributes,$reverse_flag,
					   $seen_hr,$followed_path,
					   $max_depth,$curr_depth,
					   $use_extended_displayName);
	}
    }
    if ($tmp) {
	$out .= qq(<UL CLASS="attributes">$tmp</UL>\n);
    }
    $seen_hr->{$node->db_id || "$node"}--;
    return $out;
}

sub make_attribute_tree_as_2rows {
    my ($self,@args, $tip) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my ($title,$attributes,$reverse) =
	$self->_rearrange([qw(
			      TITLE
			      ATTRIBUTES
			      REVERSE
			      )],@args);
    my $th_class = $attributes->[0];
    if ($reverse) {
	$th_class .= 'Rev';
	map {$self->_deregister_reverse_attribute($_)} @{$attributes};
    } else {
	map {$self->_deregister_attribute($_)} @{$attributes};
    }
    my $str_r = $self->make_attribute_tree(@args);
    $ {$str_r} || return '';
    my $desc_cell;
    if ($tip) {
	$desc_cell = qq(<TH COLSPAN="2" CLASS="$th_class"><A NAME="${th_class}Anchor" HREF="javascript:void(0)" onMouseover="ddrivetip('$th_class$tip','#DCDCDC', 250)" onMouseout="hideddrivetip()">$title</A></TH>);
    } else {
	$desc_cell = qq(<TH COLSPAN="2" CLASS="$th_class"><A NAME="${th_class}Anchor">$title</A></TH>);
    }
    return qq(<TR>$desc_cell</TR>\n) .
	qq(<TR><TD CLASS="$th_class" COLSPAN="2">$ {$str_r}</TD></TR>);
}

sub recursive_value_list_as_1row {
    my ($self,@args) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my ($attributes,$title,$reverse) =
	$self->_rearrange([qw(
			      ATTRIBUTES
			      TITLE
			      REVERSE
			      )],@args);
    my $fr;
    my $th_class = $attributes->[0];
    if ($reverse) {
	$fr = 'follow_reverse_attributes';
	$th_class .= 'Rev';
	map {$self->_deregister_reverse_attribute($_)} @{$attributes};
    } else {
	$fr = 'follow_attributes';
	map {$self->_deregister_attribute($_)} @{$attributes};
    }
    my ($ar1,$ar2,$followed_path) = $self->dba->$fr
	(-INSTANCE => $self,
	 @args
	 );
    my @values = map {$self->prettyfy_instance($_)->hyperlinked_displayName}
         sort {lc($a->displayName) cmp lc($b->displayName)} grep {$_ != $self} values %{$followed_path};
    @values || return '';
    return qq(<TR><TH CLASS="$th_class"><A NAME="${th_class}Anchor">$title</A></TH><TD CLASS="$th_class">) . join("<BR>",@values) . qq(</TD></TR>\n);
}

sub recursive_value_list_as_2rows {
    my ($self,@args, $tip) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my ($attributes,$title,$reverse,$delimiter) =
        $self->_rearrange([qw(
                              ATTRIBUTES
                              TITLE
                              REVERSE
			      DELIMITER
                              )],@args);
    my $fr = ($reverse) ? 'follow_reverse_attributes' : 'follow_attributes';
    $reverse ? $self->_deregister_reverse_attribute($attributes->[0]) : $self->_deregister_attribute($attributes->[0]);
    my ($ar1,$ar2,$followed_path) = $self->dba->$fr
        (-INSTANCE => $self,
	 @args
         );
    my @values = map {$self->prettyfy_instance($_)->hyperlinked_displayName}
    sort {lc($a->displayName) cmp lc($b->displayName)} grep {$_ != $self} values %{$followed_path};
    @values || return '';
    my $desc_cell;
    my $th_class = ($reverse) ? "$attributes->[0]Rev" : $attributes->[0];
    if ($tip) {
	$desc_cell = qq(<TH COLSPAN="2" CLASS="$th_class"><A NAME="${th_class}Anchor" HREF="javascript:void(0)" onMouseover="ddrivetip('$tip','#DCDCDC', 250)" onMouseout="hideddrivetip()">$title</A></TH>);
    } else {
	$desc_cell = qq(<TH COLSPAN="2" CLASS="$th_class"><A NAME="${th_class}Anchor">$title</A></TH>);
    }
    return qq(<TR>$desc_cell</TR>\n) . qq(<TD COLSPAN="2" CLASS="$th_class">) .
	   join($delimiter, @values) . qq(</TD></TR>\n);
#    return qq(<TR><TH COLSPAN="2" CLASS="$th_class"><A HREF="javascript:void(0)" onMouseover="ddrivetip('','#DCDCDC', 250)" onMouseout="hideddrivetip()">$title</A></TH></TR>\n) .
#	   qq(<TD COLSPAN="2">) . join($delimiter,@values) . qq(</TD></TR>\n);
}

sub multiple_attribute_values_as_1row {
    my ($self,$desc,$instructions) = @_;
    my @values;
    ($instructions && (ref($instructions) eq 'ARRAY')) || $self->throw("Need an array ref, got '$instructions'.");
    foreach my $instructions2 (@{$instructions}) {
	($instructions2 && (ref($instructions2) eq 'ARRAY')) || $self->throw("Need an array ref, got '$instructions2'.");
	$self->_deregister_attribute($instructions2->[0]);
	push @values, @{$self->_attributes_values([$self],$instructions2)};
    }
    @values || return '';
    my $th_class = $instructions->[0]->[0];
    return qq(<TR><TH CLASS="$th_class" WIDTH="25%"><A NAME="${th_class}Anchor">$desc</A></TH><TD CLASS="$instructions->[0]->[0]" WIDTH="75%">) .
	   join("<BR>",@values) . qq(</TD></TR>\n);
}

sub attribute_value_as_1row {
    my ($self,$desc,$instructions,$tip,$delimiter) = @_;
    $delimiter = '<BR>' unless (defined $delimiter);
    $self->isa("GKB::PrettyInstance") || $self->throw("Need 'GKB::PrettyInstance', got '$self'.");
    $self->debug && print join("\t", (caller(0))[3], $self, $self->id_string), "\n";
    my $attribute = $instructions->[0];
    $self->_deregister_attribute($attribute);
    my $values = $self->_attributes_values([$self],$instructions);
    @{$values} || return '';
    my $desc_cell;
    if ($tip) {
	$desc_cell = qq(<TH CLASS="$attribute" WIDTH="25%"><A NAME="${attribute}Anchor" HREF="javascript:void(0)" onMouseover="ddrivetip('$tip','#DCDCDC', 250)" onMouseout="hideddrivetip()">$desc</A></TH>);
    } else {
	$desc_cell = qq(<TH CLASS="$attribute" WIDTH="25%"><A NAME="${attribute}Anchor">$desc</A></TH>);
    }
    return qq(<TR>$desc_cell<TD CLASS="$attribute" WIDTH="75%">) .
	   join($delimiter,@{$values}) . qq(</TD></TR>\n);
}

sub attribute_value_as_1cell_condensed {
    my ($self,$instructions) = @_;
    $self->isa("GKB::PrettyInstance") || $self->throw("Need 'GKB::PrettyInstance', got '$self'.");
    $self->debug && print join("\t", (caller(0))[3], $self, $self->id_string), "\n";
    my $attribute = $instructions->[-1];
    $self->_deregister_attribute($attribute);
    my $values = $self->_attributes_values([$self],$instructions);
    @{$values} or @{$values} = ('&nbsp;');
    return qq(<TD CLASS="$attribute">) . join(", ",@{$values}) . qq(</TD>\n);
}

sub attribute_value_wo_duplicates_as_1row {
    my ($self,$desc,$instructions, $tip) = @_;
    $self->isa("GKB::PrettyInstance") || $self->throw("Need 'GKB::PrettyInstance', got '$self'.");
    $self->debug && print join("\t", (caller(0))[3], $self, $self->id_string), "\n";
    my $attribute = $instructions->[0];
    $self->_deregister_attribute($attribute);
    my $values = $self->_attributes_values([$self],$instructions);
    @{$values} || return '';
    my $desc_cell;
    if ($tip) {
	$desc_cell = qq(<TH CLASS="$attribute" WIDTH="25%"><A NAME="$ {attribute}Anchor" HREF="javascript:void(0)" onMouseover="ddrivetip('$tip','#DCDCDC', 250)" onMouseout="hideddrivetip()">$desc</A></TH>);
    } else {
	$desc_cell = qq(<TH CLASS="$attribute" WIDTH="25%"><A NAME="$ {attribute}Anchor">$desc</A></TH>);
    }
    $values = _collapse_duplicate_values($values);
    return qq(<TR>$desc_cell<TD CLASS="$attribute" WIDTH="75%">) .
	   join("<BR>",@{$values}) . qq(</TD></TR>\n);

}

sub _collapse_duplicate_values {
    my $ar = shift;
    my (%count,%seen,@out);
    map {$count{$_}++} @{$ar};
    map {($count{$_} > 1) ? push @out, $_ . ' x ' . $count{$_} : push @out, $_} grep {! $seen{$_}++} @{$ar};
    return \@out;
}

sub prepared_attribute_value_as_1row {
    my ($self,$desc,$attribute,$values, $tip) = @_;
    $self->isa("GKB::PrettyInstance") || $self->throw("Need 'GKB::PrettyInstance', got '$self'.");
    $self->debug && print join("\t", (caller(0))[3], $self, $self->id_string), "\n";
    $self->_deregister_attribute($attribute);
    @{$values} || return '';
    my $desc_cell;
    if ($desc) {
	if ($tip) {
	    $desc_cell = qq(<TH CLASS="$attribute"><A NAME="$ {attribute}Anchor" HREF="javascript:void(0)" onMouseover="ddrivetip('$tip','#DCDCDC', 250)" onMouseout="hideddrivetip()">$desc</A></TH>);
	} else {
	    $desc_cell = qq(<TH CLASS="$attribute"><A NAME="$ {attribute}Anchor">$desc</A></TH>);
	}
	return qq(<TR>$desc_cell<TD CLASS="$attribute">) .
	    join("<BR>",@{$values}) . qq(</TD></TR>\n);
    } else {
	return qq(<TR><TD CLASS="$attribute" COLSPAN="2">) .
	    join("<BR>",@{$values}) . qq(</TD></TR>\n);
    }
#    if ($desc) {
#	return qq(<TR><TH CLASS="$attribute">$desc</TH><TD CLASS="$attribute">) .
#	    join("<BR>",@{$values}) . qq(</TD></TR>\n);
#    } else {
#	return qq(<TR><TD CLASS="$attribute" COLSPAN="2">) .
#	    join("<BR>",@{$values}) . qq(</TD></TR>\n);
#    }
}


sub _attributes_values {
    my ($self,$instances,$instructions) = @_;
    my (@values);
    while (@{$instructions}) {
	my @instances2;
	my $att = shift @{$instructions};
	while (@{$instances}) {
	    my $i = shift @{$instances};
	    $self->prettyfy_instance($i);
#	    print "<PRE>HERE1:\t", $i->id_string, "\n</PRE>";
	    if (@{$instructions}) {
		my $class = $instructions->[0];
		if ($class) {
		    push @instances2, grep{$_->is_a($class)} @{$i->attribute_value($att)};
		} else {
		    push @instances2, @{$i->attribute_value($att)};
		}
	    } else {
		push @values, map {(ref($_)) ? $self->prettyfy_instance($_)->hyperlinked_displayName : $_} @{$i->attribute_value($att)};
#		print "<PRE>HERE2: @values\n</PRE>";
	    }
	}
	@{$instances} = @instances2;
	shift @{$instructions};
    }
    push @values, map {$self->prettyfy_instance($_)->hyperlinked_displayName}
    sort {$a->displayName cmp $b->displayName} @{$instances};
    return \@values;
}

sub reverse_attribute_value_as_1row {
    my ($self,$desc,$instructions, $tip) = @_;
    $self->isa("GKB::PrettyInstance") || $self->throw("Need 'GKB::PrettyInstance', got '$self'.");
    $self->debug && print join("\t", (caller(0))[3], $self, $self->id_string), "\n";
    my @instances = $self;
    my @values;
    $self->_deregister_reverse_attribute($instructions->[0]);
    my $th_class = $instructions->[0] . 'Rev';
    while (@{$instructions}) {
	my @instances2;
	my $att = shift @{$instructions};
	while (@instances) {
	    my $i = shift @instances;
	    if (@{$instructions}) {
		my $class = $instructions->[0];
		if ($class) {
		    push @instances2, @{$self->dba->fetch_instance_by_attribute($class,[[$att, [$i->db_id]]])};
		} else {
		    push @instances2, @{$i->reverse_attribute_value($att)};
		}
	    } else {
		push @values, map {$self->prettyfy_instance($_)->hyperlinked_displayName} @{$i->reverse_attribute_value($att)};
	    }
	}
	@instances = @instances2;
	shift @{$instructions};
    }
    push @values, map {$self->prettyfy_instance($_)->hyperlinked_displayName} @instances;
    @values || return '';
    my $desc_cell;
    if ($tip) {
	$desc_cell = qq(<TH CLASS="$th_class" WIDTH="25%"><A NAME="${th_class}Anchor" HREF="javascript:void(0)" onMouseover="ddrivetip('$tip','#DCDCDC', 250)" onMouseout="hideddrivetip()">$desc</A></TH>);
    } else {
	$desc_cell = qq(<TH CLASS="$th_class" WIDTH="25%"><A NAME="${th_class}Anchor">$desc</A></TH>);
    }
    return qq(<TR>$desc_cell<TD CLASS="$th_class" WIDTH="75%">) .
	   join("<BR>",@values) . qq(</TD></TR>\n);

}

# David Croft: attempt to fix problems that Peter encountered with EWAS in
# clostridium, where the "Other forms of this molecule"
# links were inactive (ID=181473, if you want to try it).
# Substitute pathwaybrowserdata with eventbrowser.
sub attribute_value_as_1row_swap_pathwaybrowserdata_for_eventbrowser {
    my ($self,$desc,$instructions, $tip) = @_;

    my $row_specification = $self->attribute_value_as_1row($desc, $instructions, $tip);
    $row_specification = $self->swap_pathwaybrowserdata_for_eventbrowser($row_specification);
    return $row_specification;

}

# David Croft: attempt to fix problems that Peter encountered with EWAS in
# clostridium, where the "Other forms of this molecule"
# links were inactive (ID=181473, if you want to try it).
# Substitute pathwaybrowserdata with eventbrowser.
sub reverse_attribute_value_as_1row_swap_pathwaybrowserdata_for_eventbrowser {
    my ($self,$desc,$instructions, $tip) = @_;

    my $row_specification = $self->reverse_attribute_value_as_1row($desc, $instructions, $tip);
    $row_specification = $self->swap_pathwaybrowserdata_for_eventbrowser($row_specification);
    return $row_specification;

}

sub as_1row {
    my ($self,$desc,$th_class,$instructions, $tip) = @_;
    my $ar = $self->follow_class_attributes2(%{$instructions});
    my @values = map {ref($_) ? $self->prettyfy_instance($_)->hyperlinked_displayName : $_} @{$ar};
    @values || return '';
    my $desc_cell;
    if ($tip) {
	$desc_cell = qq(<TH CLASS="$th_class"><A NAME="${th_class}Anchor" HREF="javascript:void(0)" onMouseover="ddrivetip('$tip','#DCDCDC', 250)" onMouseout="hideddrivetip()">$desc</A></TH>);
    } else {
	$desc_cell = qq(<TH CLASS="$th_class"><A NAME="${th_class}Anchor">$desc</A></TH>);
    }
    return qq(<TR>$desc_cell<TD CLASS="$th_class">) .
	   join("<BR>", @values) . qq(</TD></TR>\n);

#    return qq(<TR><TH CLASS="$th_class">$desc</TH><TD CLASS="$th_class">) . join("<BR>", @values) . qq(</TD></TR>\n);
}

sub values_as_2rows {
    my ($self,$desc,$th_class,$values,$delimiter,$tip) = @_;
    my $out = qq(<TR><TH CLASS="$th_class" COLSPAN="2"><A NAME="${th_class}Anchor");
    if ($tip) {
	$out .= qq( HREF="javascript:void(0)" onMouseover="ddrivetip('$tip','#DCDCDC', 250)" onMouseout="hideddrivetip()");
    }
    $out .= ">$desc</A>";
    $out .= qq(</TH></TR>\n<TR><TD CLASS="$th_class" COLSPAN="2">) .
	join($delimiter,@{$values}) . qq(</TD></TR>\n);
    return $out;
}

sub instances_as_2rows {
    my ($self,$desc,$th_class,$ar,$tip) = @_;
    @{$ar} || return '';
    my $out = qq(<TR><TH CLASS="$th_class" COLSPAN="2"><A NAME="${th_class}Anchor");
    if ($tip) {
	$out .= qq( HREF="javascript:void(0)" onMouseover="ddrivetip('$tip','#DCDCDC', 250)" onMouseout="hideddrivetip()");
    }
    $out .= ">$desc</A>";
    my $form_name = $th_class . '_form_' . $self->db_id;
    $out .= qq(</TH></TR>\n<TR><TD CLASS="$th_class" COLSPAN="2">) .
	$self->hyperlinked_instances_as_truncated_list($ar,$form_name) .
	qq(</TD></TR>\n);
    return $out;
}

sub instances_as_1row {
    my ($self,$desc,$th_class,$ar,$tip) = @_;
    @{$ar} || return '';
    my $out = qq(<TR><TH CLASS="$th_class"><A NAME="${th_class}Anchor");
    if ($tip) {
	$out .= qq( HREF="javascript:void(0)" onMouseover="ddrivetip('$tip','#DCDCDC', 250)" onMouseout="hideddrivetip()");
    }
    $out .= ">$desc</A>";
    my $form_name = $th_class . '_form_' . $self->db_id;
    $out .= qq(</TH><TD CLASS="$th_class">) .
	$self->hyperlinked_instances_as_truncated_list($ar,$form_name) .
	qq(</TD></TR>\n);
    return $out;
}

sub values_as_1row {
    my ($self,$desc,$th_class,$values,$delimiter,$tip) = @_;
    @{$values}|| return '';
    my $out = qq(<TR><TH CLASS="$th_class"><A NAME="${th_class}Anchor");
    if ($tip) {
	$out .= qq(<A HREF="javascript:void(0)" onMouseover="ddrivetip('$tip','#DCDCDC', 250)" onMouseout="hideddrivetip()");
    }
    $out .= ">$desc</A>";
    $out .= qq(</TH><TD CLASS="$th_class">) .
	join($delimiter,@{$values}) . qq(</TD></TR>\n);
    return $out;
}

sub hyperlinked_instances_as_truncated_list {
    my ($self,$ar,$form_name) = @_;
    @{$ar} || return '';
    my $out = '';
    if (@{$ar} > 10) {
	$out .= join("<BR />", (map {$self->prettyfy_instance($_)->hyperlinked_displayName} @{$ar}[0..9]));
	$out .= qq(<BR />...);
	$form_name ||= 'f_' . $self->db_id . '_' . time();
	$out .= $self->cgi->start_form(-name => $form_name, -method => 'POST', -action => '/cgi-bin/eventbrowser');
	$out .= $self->cgi->hidden(-name => 'DB', -value => $self->dba->db_name);
	$self->cgi->delete('FORMAT');
	$out .= $self->cgi->hidden(-name => 'FORMAT', -value => 'list');
	# cgi->delete is necessary to make CGI to forget the "old" ID so that it can "fill it" with
	# new ones from the $values.
	$self->cgi->delete('ID');
	$out .= $self->cgi->hidden(-name => 'FOCUS_SPECIES', -value => [map {$_->displayName} @{$self->focus_species}]);
	$out .= $self->cgi->hidden(-name => 'ID', -value => [map {$_->db_id} @{$ar}]);
	$out .= qq{<A ONCLICK="document.$form_name.submit(); return false">List all } . scalar(@{$ar}) . qq{ items</A>};
	$out .= $self->cgi->end_form;
    } else {
	$out .=
	    join("<BR />", map {$self->prettyfy_instance($_)->hyperlinked_displayName}
		 sort {lc($a->displayName) cmp lc($b->displayName)} @{$ar});
    }
    return $out;
}

sub hyperlinked_instances_as_truncated_list_1 {
    my ($self,$ar,$form_name) = @_;
    @{$ar} || return '';
    my $out = '';
    if (@{$ar} > 10) {
	$out .= qq(<UL><LI>) . join("</LI>\n<LI>", (map {$self->prettyfy_instance($_)->hyperlinked_displayName} @{$ar}[0..9]));
	$out .= qq(<LI>...);
	$form_name ||= 'f_' . $self->db_id . '_' . time();
	$out .= $self->cgi->start_form(-name => $form_name, -method => 'POST');
	$out .= $self->cgi->hidden(-name => 'DB', -value => $self->dba->db_name);
	$self->cgi->delete('FORMAT');
	$out .= $self->cgi->hidden(-name => 'FORMAT', -value => 'list');
	# cgi->delete is necessary to make CGI to forget the "old" ID so that it can "fill it" with
	# new ones from the $values.
	$self->cgi->delete('ID');
	$out .= $self->cgi->hidden(-name => 'FOCUS_SPECIES', -value => [map {$_->displayName} @{$self->focus_species}]);
	$out .= $self->cgi->hidden(-name => 'ID', -value => [map {$_->db_id} @{$ar}]);
	$out .= qq{<A ONCLICK="document.$form_name.submit(); return false">List all } . scalar(@{$ar}) . qq{ items</A>};
	$out .= $self->cgi->end_form;
	$out .= qq(</LI></UL>);
    } else {
	$out .=
	    qq(<UL><LI>) .
	    join("</LI>\n<LI>", map {$self->prettyfy_instance($_)->hyperlinked_displayName}
		 sort {lc($a->displayName) cmp lc($b->displayName)} @{$ar}) .
	    qq(</LI></UL>);
    }
    return $out;
}

sub multiple_reverse_attribute_values_as_1row {
    my ($self,$desc,$instructions2) = @_;
    $self->debug && print join("\t", (caller(0))[3], $self, $self->id_string), "\n";
    ($instructions2 && (ref($instructions2) eq 'ARRAY')) || $self->throw("Need an array ref, got '$instructions2'.");
    my @values;
    my $th_class = $instructions2->[0]->[0] . 'Rev';
    foreach my $instructions (@{$instructions2}) {
	($instructions && (ref($instructions) eq 'ARRAY')) || $self->throw("Need an array ref, got '$instructions'.");
	my @instances = $self;
	$self->_deregister_reverse_attribute($instructions->[0]);
	while (my $att = shift @{$instructions}) {
	    my $class = shift @{$instructions};
	    my @instances2;
	    while (my $i = shift @instances) {
		if ($class) {
		    push @instances2, grep {$_->is_a($class)} @{$i->reverse_attribute_value($att)};
		} else {
		    push @instances2, @{$i->reverse_attribute_value($att)};
		}
	    }
	    @instances = @instances2;
	}
	push @values, @instances;
    }
    @values || return '';
    @values =
	map {ref($_->[1]) ? $self->prettyfy_instance($_->[1])->hyperlinked_displayName : $_->[1]}
	sort {$a->[0] cmp $b->[0]}
	map {ref($_) ? [$_->displayName, $_] : [$_, $_]} @values;
    return
	qq(<TR><TH CLASS="$th_class">$desc</TH><TD CLASS="$th_class">) .
	join("<BR>", @values) .
	qq(</TD></TR>\n);
}

sub compound_attribute_value_as_2rows {
    my ($self,$desc,$instructionset,$empty_str) = @_;
    my @values;
    my $i = 0;
    my $out;
    my $att = $instructionset->[0]->[0];
    $self->_deregister_attribute($att);
    foreach my $instructions (@{$instructionset}) {
	$values[$i++] = $self->_attributes_values([$self],$instructions);
    }
    for (my $j = 0; $j < @{$values[0]}; $j++) {
	$out .= qq(<TR>);
	map {$out .= qq(<TD CLASS="$att">) .
	(defined $values[$_]->[$j] ? $values[$_]->[$j] : $empty_str) . qq(</TD>)} (0 .. $i - 1);
	$out .= qq(</TR>\n);
    }
    @{$values[0]} || return '';
    return qq(<TR><TH CLASS="$att" COLSPAN="$i"><A NAME="$ {att}Anchor">$desc</A></TH></TR>\n$out);
}

sub collapsed_Events_as_1_row {
    my ($self,$desc,$th_td_class,$instructions, $tip) = @_;
    my $ar = $self->follow_class_attributes(%{$instructions});
    my $aar = &GKB::Utils::collapse_Events_to_focus_taxon($ar,$self->focus_species);
    @{$aar} || return '';
    my $desc_cell = qq(<TH CLASS="$th_td_class"><A NAME="$ {th_td_class}Anchor");
    if ($tip) {
	$desc_cell .= qq( HREF="javascript:void(0)" onMouseover="ddrivetip('$tip','#DCDCDC', 250)" onMouseout="hideddrivetip()");
    }
    $desc_cell .= qq(>$desc</A></TH>);
    return qq(<TR>$desc_cell<TD CLASS="$th_td_class">) .
	   join("<BR>\n",map {&GKB::Utils::hyperlinked_collapsed_Events($self->urlmaker,$_)} @{$aar}) . qq(</TD></TR>\n);
}

sub html_table {
    my ($self) = @_;
    $self->debug && print join("\t", (caller(0))[3], $self,  $self->id_string), "\n";
    return $self->html_table2(@_) unless($self->isa("GKB::PrettyInstance::" . $self->class));
    if ($self->is_valid_attribute('_html') && $self->attribute_value('_html')->[0]) {
	return $self->attribute_value('_html')->[0];
    }
    return
	$self->page_title
	. $self->reactionmap_html
	. $LINK_TO_SURVEY
	. qq(<DIV CLASS="section">)
	. qq(\n<TABLE cellspacing="0" WIDTH="$HTML_PAGE_WIDTH" CLASS=") . $self->class . qq(">\n)
	. $self->html_table_rows_if_necessary
#	. $self->all_registered_attributes_as_rows
	. $self->_view_switch_html
        . qq(</TABLE>\n)
	. qq(</DIV>\n)
	;
}

sub html_table2 {
    my ($self,$no_reverse_attributes) = @_;

    my $logger = get_logger(__PACKAGE__);

    if ($self->db_id == 194550) {
	$logger->info("entered, DB_ID=" . $self->db_id() . "\n");
    }

    $self->debug && print join("\t", (caller(0))[3], $self,  $self->id_string), "\n";
    my $out =  qq(\n<TABLE cellspacing="0" CLASS="instancebrowser" WIDTH="$HTML_PAGE_WIDTH">\n) .
    $self->class_and_id_as_row;
    foreach my $attribute (sort {lc($a) cmp lc($b)} $self->inflated ? $self->list_set_attributes : $self->list_valid_attributes) {
	next if ($attribute eq 'DB_ID' or $attribute eq '_class' or $attribute eq '_html');
	if ($self->db_id == 194550) {
	    $logger->info("attribute=$attribute\n");
	}

	if ($self->is_recursive_attribute($attribute)) {
	    my $max_depth = ($attribute eq 'orthologousEvent' or $attribute eq 'precedingEvent') ? 2 : undef;
	    my $str = $self->_make_tree_html(undef,undef,[$attribute],undef,{},{},$max_depth,0,1);

	    if ($self->db_id == 194550) {
		$logger->info("recursive attribute\n");
		if (defined $str) {
		    $logger->info("str=$str\n");
		} else {
		    $logger->info("str not defined!\n");
		}
		if (defined $self->attribute_value($attribute)) {
		    $logger->info("attribute element count: " . scalar(@{$self->attribute_value($attribute)}) . "\n");
		} else {
		    $logger->info("attribute values undefined!\n");
		}
	    }

	    if (!$str && defined $self->attribute_value($attribute) && scalar(@{$self->attribute_value($attribute)})>0) {
	        $logger->error("ERROR!\n");
	        $str = "ERROR: valid values do exist for this attribute, but tree HTML could not be generated!";
#	        $str = "<UL CLASS=\"attributes\">ERROR: valid values do exist for this attribute, but could not generate tree HTML</UL>";
	        $str .= ".  Values: ";
	        foreach my $attrb (@{$self->attribute_value($attribute)}) {
	        	$str .= $attrb->displayName() . " [" . $attrb->species->[0]->displayName() . "], ";
	        }
	    }

	    $str || next;
	    $out .= qq(<TR><TH><A NAME="$ {attribute}Anchor">$attribute</A></TH><TD>$str</TD></TR>\n);
	} else {
	    if ($self->db_id == 194550) {
		$logger->info("non-recursive attribute\n");
	    }

#	    my @values = map {(ref($_)) ? $self->prettyfy_instance($_)->hyperlinked_extended_displayName : $_}
#	        @{$self->attribute_value($attribute)};
	    my @values;
	    foreach my $val (@{$self->attribute_value($attribute)}) {
		if (ref($val)) {
		    push @values, $self->prettyfy_instance($val)->hyperlinked_extended_displayName;
		} else {
		    # This is to deal with ReferenceEntity.actomicConnectivity which seems to contain '\n'
		    # instead of newline.
		    if ($attribute eq 'atomicConnectivity') {
			$val =~ s/\\n/\n/g;
			$val = "<PRE>$val</PRE>";
		    } elsif ($attribute eq 'storedATXML') {
			$val = GKB::Utils::escape($val);
		    }
		    push @values, $val;
		}
	    }
	    @values || next;
	    $out .= qq(<TR><TH>$attribute</TH><TD>) .
		join("<BR>",@values) . qq(</TD></TR>\n);
	}
    }
    unless ($no_reverse_attributes) {
	foreach my $attribute (sort {lc($a) cmp lc($b)} $self->list_valid_reverse_attributes) {
	    my $values = $self->reverse_attribute_value($attribute);
	    @{$values} || next;
	    $out .= qq(<TR><TH><A NAME="$ {attribute}RevAnchor">($attribute)</A></TH><TD>);
	    if (@{$values} > 10) {
		$out .= join("<BR />", (map {$self->prettyfy_instance($_)->hyperlinked_extended_displayName} @{$values}[0..9]));
		$out .= $self->cgi->start_form(-name => "$ {attribute}_form");
		$out .= $self->cgi->hidden(-name => 'DB', -value => $self->dba->db_name);
		$self->cgi->delete('FORMAT');
		$out .= $self->cgi->hidden(-name => 'FORMAT', -value => 'list');
		# cgi->delete is necessary to make CGI to forget the "old" ID so that it can "fill it" with
		# new ones from the $values.
		$self->cgi->delete('ID');
		$out .= $self->cgi->hidden(-name => 'ID', -value => [map {$_->db_id} @{$values}]);
		$out .= qq(<A ONCLICK="document.$ {attribute}_form.submit(); return false">List all ) . scalar(@{$values}) . qq( refering instances</A>);
		#$out .= $self->cgi->submit(qq(List all ) . scalar(@{$values}) . qq( refering instances));
		$out .= $self->cgi->end_form;
	    } else {
		$out .= join("<BR />", (map {$self->prettyfy_instance($_)->hyperlinked_extended_displayName} @{$values}));
	    }
	    $out .= qq(</TD></TR>\n);
	}
    }
    $out .= $self->_view_switch_html;
    $out .= qq(</TABLE>\n);
    return $out;
}

# "Makes" the guts of the switch at the bottom of the
# page, but doesn't embed it in any additional HTML.
# That means you can overwrite this method and add
# extra switches.
sub _make_switch_html {
    my $self = shift;
    my $tmp = GKB::Utils::InstructionLibrary::taboutputter_popup_form($self);
    unless ($self->webutils && $self->webutils->omit_view_switch_link) {
		my $db = $self->dba->db_name;
		$tmp = qq(<A HREF="javascript:void(0)" onMouseover="ddrivetip('Allows to change the default format in which the content is presented','#DCDCDC', 250)" onMouseout="hideddrivetip()" onClick="X=window.open('/cgi-bin/formatselector?DB=$db','formatselector','height=200,width=400,left=10,screenX=10,top=10,screenY=10,resizable,scrollbars=yes');X.focus" CLASS="viewswitch">[Change default viewing format]</A>\n) . $tmp;
    }
    $tmp || return '';
    return $tmp;
}

sub _view_switch_html {
    my $self = shift;
    my $tmp = $self->_make_switch_html();
    $tmp || return '';
    return qq(<TR><TD COLSPAN="2" CLASS="viewswitch">\n$tmp\n</TD></TR>\n);
}

# Adds a link that allows
# the user to bookmark according to stable ID rather
# than DB_ID.
sub _view_stable_link {
    my $self = shift;

    my $logger = get_logger(__PACKAGE__);

    $logger->info("entered\n");

    my $wu = $self->webutils;
    if (!(defined $wu)) {
    	return '';
    }

    # Don't insert a link if no stable ID is available
    if (!($self->is_valid_attribute('stableIdentifier'))) {
    	return '';
    }

    if (!($self->exists_identifier_database())) {
    	return '';
    }

    my $stable_identifier = $self->stableIdentifier->[0];
    if (!$stable_identifier) {
    	return '';
    }

    if (!$stable_identifier->is_valid_attribute('identifier')) {
	$logger->error("Could not extract identifier from StableIdentifier instance\n");
	return '';
    }
    my $identifier = $stable_identifier->identifier->[0];

#	my $version = undef;
#	if ($stable_identifier->is_valid_attribute('identifierVersion')) {
#		$version = $stable_identifier->identifierVersion->[0];
#	}

    my $style = "font-weight: bold; text-decoration: underline;";
    my $link = $wu->link_to_eventbrowser_st_id($style, $identifier, '', '', 'Stable link for this page', '');
    my $cgi = $self->cgi;
    my $format = GKB::WebUtils::get_format($cgi);
    if (defined $format && $format eq 'elv') {
    	$link = $identifier;
    }

    my $stable_link = "<P>$link</P>\n";

    return $stable_link;
}

=head
# Ideally this method should reside in appropriate subclasses and be w/o the validity check.
# However, that would mean duplicated code.
sub hyperlinked_identifier {
    my ($self) = @_;
    ($self->is_valid_attribute('identifier') && $self->is_valid_attribute('referenceDatabase')) ||
	$self->throw("Class " . $self->class . " does not have attributes 'identifier' and 'referenceDatabase' and you shouldn't call this method on this instance.");
    my ($refdb,$url);
    if ($refdb = $self->ReferenceDatabase->[0] and
	$url = $refdb->AccessUrl->[0]) {
	if (my $id = $self->Identifier->[0]) {
	    $url =~ s/###ID###/$id/g;
	    return qq(<A HREF="$url">) . $self->displayName . qq(</A>);
	}
    }
    return GKB::PrettyInstance::hyperlinked_displayName($self);
}
=cut

sub regulation_as_rows {
    my ($self) = @_;
    my $out = '';
    if ($self->is_valid_reverse_attribute('regulatedBy')) {
	my $regulation = $self->reverse_attribute_value('regulatedBy');
	@{$regulation} = grep {$_->Regulator->[0]} @{$regulation};
	my @a = grep {$_->class eq 'Requirement'} @{$regulation};
	if (@a) {
	    $out .= qq(<TR><TH CLASS="regulation"><A NAME="requirementAnchor" HREF="javascript:void(0)" onMouseover="ddrivetip('Regulation may be a Requirement if the regulator is required for the CatalystActivity or for the Event to occur.','#DCDCDC', 250)" onMouseout="hideddrivetip()">Requires</A></TH><TD>) .
		join ("<BR />", map {$self->prettyfy_instance($_->Regulator->[0])->hyperlinked_displayName} @a) .
		qq(</TD></TR>\n);
	}
	@a = grep {$_->class eq 'Regulation'} @{$regulation};
	if (@a) {
	    $out .= qq(<TR><TH CLASS="regulation"><A NAME="regulationAnchor" HREF="javascript:void(0)" onMouseover="ddrivetip('In GK, Events and CatalystActivities may be regulated by other Events, PhysicalEntities, CatalystActivities.  The description of an instance of regulation includes 1\) the regulated entity \(Event or CatalystActivity\) , the regulator \(Event, PhysicalEntity, or CatalystActivity\) and the mechanism by which regulation is achieved \(Regulationtype\). Regulation may be positive, negative or a Requirement if the regulator is required for the CatalystActivity or for the Event to occur.','#DCDCDC', 250)" onMouseout="hideddrivetip()">Regulated by</A></TH><TD>) .
		join ("<BR />", map {$self->prettyfy_instance($_->Regulator->[0])->hyperlinked_displayName} @a) .
		qq(</TD></TR>\n);
	}
	@a = grep {$_->class eq 'PositiveRegulation'} @{$regulation};
	if (@a) {
	    $out .= qq(<TR><TH CLASS="regulation"><A NAME="positiveRegulationAnchor" HREF="javascript:void(0)" onMouseover="ddrivetip('Regulation can be positive \(if the Regulator facilitates an Event\/increases a CatalystActivity\).','#DCDCDC', 250)" onMouseout="hideddrivetip()">Positively regulated by</A></TH><TD>) .
		join ("<BR />", map {$self->prettyfy_instance($_->Regulator->[0])->hyperlinked_displayName} @a) .
		qq(</TD></TR>\n);
	}
	@a = grep {$_->class eq 'NegativeRegulation'} @{$regulation};
	if (@a) {
	    $out .= qq(<TR><TH CLASS="regulation"><A NAME="negativeRegulationAnchor" HREF="javascript:void(0)" onMouseover="ddrivetip('Regulation can be negative \(if the Regulator inhibits an Event\/decreases CatalystActivity\).','#DCDCDC', 250)" onMouseout="hideddrivetip()">Negatively regulated by</A></TH><TD>) .
		join ("<BR />", map {$self->prettyfy_instance($_->Regulator->[0])->hyperlinked_displayName} @a) .
		qq(</TD></TR>\n);
	}
    }
    if ($self->is_valid_reverse_attribute('regulator')) {
	my $regulation = $self->reverse_attribute_value('regulator');
	@{$regulation} = grep {$_->RegulatedEntity->[0]} @{$regulation};
	my @a = grep {$_->class eq 'Requirement'} @{$regulation};
	if (@a) {
	    $out .= qq(<TR><TH CLASS="regulation"><A HREF="javascript:void(0)" onMouseover="ddrivetip('Regulation can be a requirement if the regulator is required for the CatalystActivity or for the Event to occur.','#DCDCDC', 250)" onMouseout="hideddrivetip()">Required for</A></TH><TD>) .
		join ("<BR />", map {$self->prettyfy_instance($_->RegulatedEntity->[0])->hyperlinked_displayName} @a) .
		qq(</TD></TR>\n);
	}
	@a = grep {$_->class eq 'Regulation'} @{$regulation};
	if (@a) {
	    $out .= qq(<TR><TH CLASS="regulation"><A HREF="javascript:void(0)" onMouseover="ddrivetip('In GK, Events and CatalystActivities may be regulated by other Events, PhysicalEntities, CatalystActivities.  The description of an instance of regulation includes 1\) the regulated entity \(Event or CatalystActivity\) , the regulator \(Event, PhysicalEntity, or CatalystActivity\) and the mechanism by which regulation is achieved \(Regulationtype\). Regulation may be positive, negative or a Requirement if the regulator is required for the CatalystActivity or for the Event to occur.','#DCDCDC', 250)" onMouseout="hideddrivetip()">Regulates</A></TH><TD>) .
		join ("<BR />", map {$self->prettyfy_instance($_->RegulatedEntity->[0])->hyperlinked_displayName} @a) .
		qq(</TD></TR>\n);
	}
	@a = grep {$_->class eq 'PositiveRegulation'} @{$regulation};
	if (@a) {
	    $out .= qq(<TR><TH CLASS="regulation"><A HREF="javascript:void(0)" onMouseover="ddrivetip('Regulation can be positive \(if the Regulator facilitates an Event\/increases a CatalystActivity\).','#DCDCDC', 250)" onMouseout="hideddrivetip()">Regulates positively</A></TH><TD>) .
		join ("<BR />", map {$self->prettyfy_instance($_->RegulatedEntity->[0])->hyperlinked_displayName} @a) .
		qq(</TD></TR>\n);
	}
	@a = grep {$_->class eq 'NegativeRegulation'} @{$regulation};
	if (@a) {
	    $out .= qq(<TR><TH CLASS="regulation"><A HREF="javascript:void(0)" onMouseover="ddrivetip('Regulation can be negative \(if the Regulator inhibits an Event\/decreases CatalystActivity\).','#DCDCDC', 250)" onMouseout="hideddrivetip()">Regulates negatively</A></TH><TD>) .
		join ("<BR />", map {$self->prettyfy_instance($_->RegulatedEntity->[0])->hyperlinked_displayName} @a) .
		qq(</TD></TR>\n);
	}
    }
    return $out;
}

sub participant_in_event_tree_as_2rows {
    my ($self,@args, $tip) = @_;
    my ($title,$physicalentities) = $self->_rearrange([qw(
							  TITLE
							  PHYSICALENTITIES
							  )],@args);
    $physicalentities || $self->throw("Need a reference to an array contining PhysicalEntities.");
    my $treemaker = GKB::HtmlTreeMaker->new(-URLMAKER => $self->urlmaker, -SORTING_FN => \&GKB::Utils::order_Events);
    my $str = $treemaker->participant_in_event_tree($physicalentities, $self->focus_species);
    $str || return '';
	return qq(<TR><TH COLSPAN="2" CLASS="participantInEvents"><A NAME="participantInEventsAnchor" HREF="javascript:void(0)" onMouseover="ddrivetip('Heirachical path to the events the named entity is involved in','#DCDCDC', 250)" onMouseout="hideddrivetip()">$title</A></TH></TR>\n<TD COLSPAN="2" CLASS="participantInEvents">$str</TD></TR>\n );
}

sub literaturereferences_as_2rows {
    my ($self) = @_;
    $self->is_valid_attribute('literatureReference') ||
	$self->throw("'literatureReference' is not a valid attribute for class " . $self->class);
    my %h;
    map {$h{$_->db_id} = $_} @{$self->LiteratureReference};
    if ($self->is_valid_reverse_attribute('assertion')) {
	foreach my  $s (@{$self->reverse_attribute_value('assertion')}) {
	    if ($s->is_valid_attribute('literatureReference')) {
		map {$h{$_->db_id} = $_} @{$s->LiteratureReference};
	    }
	    foreach my $cs (@{$s->ComponentSection}) {
		map {$h{$_->db_id} = $_} @{$cs->LiteratureReference};
	    }
	}
    }
    %h || return '';
    my $out = qq(<TR><TH COLSPAN="2" CLASS="literatureReference"><A NAME="literatureReferenceAnchor" HREF="javascript:void(0)" onMouseover="ddrivetip('Reference(s) from which this event was sourced from','#DCDCDC', 250)" onMouseout="hideddrivetip()">References</A></TH></TR>\n<TR><TD COLSPAN="2" CLASS="literatureReference">);
    map {$out .= "<P />" . $self->prettyfy_instance($_)->hyperlinked_displayName . "\n"} values %h;
    $out .= "</TD></TR>\n";
    return $out;
}

sub creation_info_as_one_row_1 {
    my $self = shift;
    if ($self->is_valid_attribute('summation') && @{$self->Summation} && @{$self->Summation->[0]->Created}) {
	return $self->prepared_attribute_value_as_1row
	    (undef,
	     'created',
	     [map {$self->prettyfy_instance($_)->hyperlinked_displayName} map {$_->Created->[0]} @{$self->Summation}]);
    } elsif (@{$self->Created}) {
	return $self->prepared_attribute_value_as_1row
	    (undef,
	     'created',
	     [map {$self->prettyfy_instance($_)->hyperlinked_displayName} @{$self->Created}]);
    } else {
	return '';
    }
}

sub creation_info_as_one_row {
    my $self = shift;
    unless ($self->is_valid_attribute('authored')) {
	return $self->creation_info_as_one_row_1;
    }
    my $out = '';
    if ($self->Authored->[0]) {
	$out .= $self->prepared_attribute_value_as_1row
	    (undef,
	     'created',
	     [map {$self->prettyfy_instance($_)->hyperlinked_displayName} @{$self->Authored}]);
    }
    return $out;
}

sub focus_species_or_default {
    my $self = shift;
    unless (exists $self->{'focus_species'}) {
	$self->{'focus_species'} = $self->webutils->get_focus_species($self);
    }
    return $self->{'focus_species'};
}

sub focus_species {
    my $self = shift;
    if ($self->cgi->param('FOCUS_SPECIES') || ($self->is_valid_attribute('species') && $self->Species->[0])) {
	return $self->focus_species_or_default;
    } else {
	return [];
    }
}

sub focus_taxon {
    return focus_species(@_);
}

sub grep_for_instances_with_focus_species_or_default {
    my ($self,$ar) = @_;
    $self->dba->load_class_attribute_values_of_multiple_instances('DatabaseObject','species',$ar);
    return GKB::Utils::grep_for_instances_with_given_species($ar,$self->focus_species_or_default);
}

sub grep_for_instances_with_focus_species {
    my ($self,$ar) = @_;
    if ($self->cgi->param('FOCUS_SPECIES') && $self->is_valid_attribute('species') && $self->Species->[0]) {
	return GKB::Utils::grep_for_instances_with_given_species($ar,$self->focus_species_or_default);
    } else {
	return $ar;
    }
}

sub _stable_identifier {
    my $self = shift;
    if ($self->is_valid_attribute('stableIdentifier')) {
		return $self->attribute_value_as_1row('Stable identifier', ['stableIdentifier']);
    }
    return '';
}

sub _doi {
    my $self = shift;
    if ($self->is_valid_attribute('doi')) {
		return $self->attribute_value_as_1row('DOI', ['doi']);
    }
    return '';
}

# Creates a table row that allows users to submit feedback by email
sub _user_feedback {
    my ($self) = @_;

	my $title = $self->name->[0] . " (DB_ID=" . $self->db_id() .")";
    my $subject = "Comment on: $title";
    my $body = "Hi\%2C\%0D\%0A\%0D\%0AI took a look at\%3A\%0D\%0A\%0D\%0A$title\%0D\%0A\%0D\%0A..and here's what I think:";
    my $mail_link= "<A HREF=\"" . $PROJECT_HELP_URL . "?subject=$subject&body=$body\">Let us know what you think of this article (click here)</A>";
	my $desc_cell = qq(<TH CLASS="summation" WIDTH="25%"><B>Your feedback</B></TH>);
    return qq(<TR>$desc_cell<TD CLASS="summation" WIDTH="75%">$mail_link</TD></TR>\n);
}

sub reactionmap_js {
    my $self = shift;
    my $db = $self->cgi->param('DB');
    return <<__END__;
<script language="JavaScript">

var previousBg;
var currentInstanceId;
var db = "$db";

function handleClick(id,e) {
    var x;
    var y;
    if (window.event) {
	x = window.event.offsetX;
	y = window.event.offsetY;
    } else {
	x = e.pageX - (window.innerWidth - $REACTIONMAP_WIDTH) / 2 + 10;
//	x = e.pageX - 10;
	y = e.pageY - 70;
    }
    document.reactionmap.REACTIONMAP_x.value = x;
    document.reactionmap.REACTIONMAP_y.value = y;
    if (id) {
	document.reactionmap.ID.value = id;
    }
    document.reactionmap.tip.value = "";
    document.reactionmap.submit();
}

function handleClick2(form_id,id,e) {
    var x;
    var y;
//    var tmp;
    if (!e) var e = window.event;
    if (e.pageX || e.pageY) {
	x = e.pageX - (window.innerWidth - $REACTIONMAP_WIDTH) / 2 + 10;
	y = e.pageY - 100;
//        tmp = "pageXY";
    }
    else if (e.clientX || e.clientY) {
	x = e.clientX + document.body.scrollLeft - (document.body.offsetWidth - $REACTIONMAP_WIDTH) / 2 + 10;
	y = e.clientY + document.body.scrollTop - 100;
//        tmp = "clientXY";
    }
    //Have to use eval to keep IE on OS X happy.
    eval("document.reactionmap_" + form_id + ".REACTIONMAP_x.value = " + x);
    eval("document.reactionmap_" + form_id + ".REACTIONMAP_y.value = " + y);
    if (id) {
	eval("document.reactionmap_" + form_id + ".ID.value = " + id);
    }
//    window.alert(tmp + ":" + x + "," + y);
    eval("document.reactionmap_" + form_id + ".submit()");
    return false;
}

function setValueAndSubmit(form_id,element_name,value) {
    var form = document.forms["reactionmap_" + form_id];
    var el;
    if (el = form.elements[element_name]) {
        el.value = value;
        form.submit();
        return false;
    } else {
        var input = document.createElement('INPUT');
        if (document.all) {
            input.type = 'hidden';
            input.name = element_name;
            input.value = value;
        } else if (document.getElementById) {
            input.setAttribute('type', 'hidden');
            input.setAttribute('name', element_name);
            input.setAttribute('value', value);
        }
        form.appendChild(input);
        form.submit();
        return false;
    }
}

function setValue(form_id,element_name,value) {
    var form = document.forms["reactionmap_" + form_id];
    var el;
    if (el = form.elements[element_name]) {
        el.value = value;
    } else {
	//Something doesnt quite work on Konqueror, i.e.
	//the element has to exist which mean that things work
        //only if we dont get here.
        var input = document.createElement('INPUT');
        if (document.all) {
	    //input.type doesnt work on IE5.2 (OS X)
            input.type = 'hidden';
            input.name = element_name;
            input.value = value;
        } else if (document.getElementById) {
            input.setAttribute('type', 'hidden');
            input.setAttribute('name', element_name);
            input.setAttribute('value', value);
        }
        form.appendChild(input);
    }
}

function setMultiValue(form_id,element_name,valueArray) {
    var form = document.forms["reactionmap_" + form_id];
    var el;
    if (el = form.elements[element_name]) {
        el.value = valueArray[0];
    } else {
        var input = document.createElement('INPUT');
        if (document.all) {
            input.type = 'hidden';
            input.name = element_name;
            input.value = valueArray[0];
        } else if (document.getElementById) {
            input.setAttribute('type', 'hidden');
            input.setAttribute('name', element_name);
            input.setAttribute('value', valueArray[0]);
        }
        form.appendChild(input);
    }
    for (var i=1; i<valueArray.length; i++) {
	var input = document.createElement('INPUT');
	if (document.all) {
            input.type = 'hidden';
            input.name = element_name;
            input.value = valueArray[i];
        } else if (document.getElementById) {
            input.setAttribute('type', 'hidden');
            input.setAttribute('name', element_name);
            input.setAttribute('value', valueArray[i]);
        }
        form.appendChild(input);
    }
}

function submitForm(form_id) {
    eval("document.reactionmap_" + form_id + ".submit()");
    return false;
}

function handleMouseOver(instance_id,tip_label,tip_bg,tip_width) {
    ddrivetip(tip_label,tip_bg,tip_width);
//    var tmp = '<IMG SRC="/cgi-bin/image4reaction?ID=' + instance_id + '&DB=' + db + '">';
//    ddrivetip(tmp);
    currentInstanceId = instance_id;
    var el;
    if (el = document.getElementById("h_" + instance_id)) {
	previousBg = el.style.backgroundColor;
	el.style.backgroundColor = "#E0FFFF";
    }
}

function handleMouseOut() {
    hideddrivetip();
    if (currentInstanceId) {
	var el;
	if (el = document.getElementById("h_" + currentInstanceId)) {
	    el.style.backgroundColor = previousBg;
	}
    }
}

</script>
__END__
}

sub reactionmap_html {
    my ($self) = @_;
    $self->debug && print join("\t", (caller(0))[3], $self,  $self->class, ($self->db_id || '')), "\n";
    unless ($self->can("create_image")) {
	return '';
    }
    my $start = time();
    my $mapwidth = $REACTIONMAP_WIDTH + 3;
    my $cgi = $self->cgi;
    my %preferences = $cgi->cookie('preferences');
    my $form_name = 'reactionmap_' . $self->db_id;
    my $db_id = $self->db_id;
    if (lc($preferences{'reactionmap'}) eq 'off') {
#    if (lc($preferences{'reactionmap'}) ne 'on') {
	my $out =
	    $self->reactionmap_js() .
	    qq(<TABLE cellspacing="0" ALIGN="center" WIDTH="$mapwidth" CLASS="reactionmap">) .
	    $cgi->start_form(-method =>'POST',-name =>$form_name,-action => '/cgi-bin/eventbrowser');
	foreach my $param ($cgi->param) {
	    my $val = $cgi->param($param);
#	    print qq(<PRE>$param\t$val</PRE>\n);
	    if (defined $val) {
		$out .= qq(<INPUT TYPE="hidden" NAME="$param" VALUE="$val" />);
	    }
	}
        $out .=
            qq(<INPUT TYPE="hidden" NAME="reactionmap" VALUE="" />) .
            qq(<INPUT TYPE="hidden" NAME="eventhierarchy" VALUE="" />) .
            qq(<INPUT TYPE="hidden" NAME="description" VALUE="" />);
	$out .= qq(<TR CLASS="reactionmap"><TD CLASS="reactionmap"><A ONMOUSEOVER="ddrivetip('Show reaction map','#DCDCDC',150)" ONMOUSEOUT="hideddrivetip()" ONCLICK="setValue($db_id,'reactionmap','on');setValue($db_id,'ID',$db_id);submitForm($db_id);"><IMG SRC="/icons/plus-box.gif" HEIGHT="10" WIDTH="10" BORDER="0"></A> Reaction map</TD></TR>);
	$out .= $cgi->end_form;
	$out .= qq(</TABLE>\n);
	return $out;
    }
    my $rm;
    if ($self->cgi->param('FOCUS') and $self->cgi->param('FOCUS') == $self->db_id) {
	#zoom and/or move, the highlites will come from cgi params.
	$rm = new GKB::ReactionMap(-DBA => $self->dba,-CGI => $self->cgi,-ORIGIN_INSTANCE=>$self);
	$rm->create_image_for_species($self->focus_species_or_default);
    } else {
	$rm = $self->create_image || return '';
    }
    my $name = rand() * 1000 . $$ . '.png';
    open(OUT, ">$GK_TMP_IMG_DIR/$name") || $self->throw("Can't create '$GK_TMP_IMG_DIR/$name': $!");
    binmode OUT;
    print OUT $rm->image->png;
    close OUT;

    my $x_mag = $rm->x_magnification * $rm->zoom;
    my $y_mag = $rm->y_magnification * $rm->zoom;
    my $x_offset = $rm->x_offset;
    my $y_offset = $rm->y_offset;
    my $show_hierarchy_types = $cgi->param('SHOW_HIERARCHY_TYPES')
	? qq(<INPUT TYPE="hidden" NAME="SHOW_HIERARCHY_TYPES" VALUE="1" />)
	: '';
    $cgi->param("X_MAG", $x_mag);
    $cgi->param("Y_MAG", $y_mag);
    $cgi->param("X_OFFSET", $x_offset);
    $cgi->param("Y_OFFSET", $y_offset);
    $cgi->param("FOCUS", $self->db_id);
    $rm->highlites_as_hidden_param;
#    print qq(<PRE>), (caller(0))[3],"\t",  time() - $start, qq(</PRE>\n);
    my $key_str = '';
    if (my $tmp = $self->reactionmap_key) {
	$key_str = qq(<TR><TD COLSPAN="12" ALIGN="center">$tmp</TD></TR>\n);
    }
    return
	$self->reactionmap_js() .
	qq(<DIV CLASS="section">\n) .
	qq(<TABLE cellspacing="0" ALIGN="center" CLASS="reactionmap" WIDTH="$mapwidth">) .
	$cgi->start_form(-method =>'POST',-name =>$form_name,-action => '/cgi-bin/eventbrowser') .
	$cgi->hidden(-name => 'DB',-value => $self->cgi->param('DB')) .
	$show_hierarchy_types .
	qq(<INPUT TYPE="hidden" NAME="X_MAG" VALUE=\") . $cgi->param("X_MAG") . qq(\" />) .
	qq(<INPUT TYPE="hidden" NAME="Y_MAG" VALUE=\") . $cgi->param("Y_MAG") . qq(\" />) .
	qq(<INPUT TYPE="hidden" NAME="X_OFFSET" VALUE=\") . $cgi->param("X_OFFSET") . qq(\" />) .
	qq(<INPUT TYPE="hidden" NAME="Y_OFFSET" VALUE=\") . $cgi->param("Y_OFFSET") . qq(\" />) .
#	qq(<INPUT TYPE="hidden" NAME="ID" VALUE=\") . $self->db_id . qq(\" />) .
	qq(<INPUT TYPE="hidden" NAME="ID" VALUE="" />) .
	qq(<INPUT TYPE="hidden" NAME="REACTIONMAP_x" VALUE="" />) .
	qq(<INPUT TYPE="hidden" NAME="REACTIONMAP_y" VALUE="" />) .
        qq(<INPUT TYPE="hidden" NAME="reactionmap" VALUE="" />) .
        qq(<INPUT TYPE="hidden" NAME="eventhierarchy" VALUE="" />) .
        qq(<INPUT TYPE="hidden" NAME="description" VALUE="" />) .
	qq(<INPUT TYPE="hidden" NAME="FOCUS" VALUE=\") . $self->db_id . qq(\" />) .
	qq(<INPUT TYPE="hidden" NAME="FOCUS_SPECIES" VALUE=\") . $cgi->param('FOCUS_SPECIES') . qq(\" />) .
	$rm->highlites_as_hidden_param .
#	qq(<INPUT TYPE="hidden" NAME="HIGHLITES" VALUE=\") . $cgi->param("HIGHLITES") . qq(\" />) .
	qq(<TR CLASS="reactionmap">) .
	qq(<TD CLASS="reactionmap" ALIGN="left"><A ONMOUSEOVER="ddrivetip('Hide reaction map','#DCDCDC',150)" ONMOUSEOUT="hideddrivetip()" ONCLICK="setValue($db_id,'reactionmap','off');setValue($db_id,'ID',$db_id);submitForm($db_id);"><IMG SRC="/icons/minus-box.gif" HEIGHT="10" WIDTH="10" BORDER="0"></A></TD>) .
	qq(<TD NOWRAP CLASS="reactionmap">Click on the map:</TD>) .
	qq(<TD CLASS="reactionmap" ALIGN="right"><INPUT CLASS="reactionmap" TYPE="radio" NAME="ZOOM" VALUE="1" CHECKED /></TD>) .
	qq(<TD CLASS="reactionmap" ALIGN="left">shifts focus</TD>) .
	qq(<TD CLASS="reactionmap" ALIGN="right"><INPUT CLASS="reactionmap" TYPE="radio" NAME="ZOOM" VALUE="2" /></TD>) .
qq(<TD CLASS="reactionmap" ALIGN="left">zooms in</TD>) .
	qq(<TD CLASS="reactionmap" ALIGN="right"><INPUT CLASS="reactionmap" TYPE="radio" NAME="ZOOM" VALUE="0.5" /></TD>) .
	qq(<TD CLASS="reactionmap" ALIGN="left">zooms out</TD>) .
	qq(<TD CLASS="reactionmap"><INPUT CLASS="reactionmap" TYPE="image" NAME="MOVE_L" VALUE="-0.25" SRC="/icons/left.png" ALT="move left" HEIGHT="10" WIDTH="10" /></TD>) .
	qq(<TD CLASS="reactionmap"><INPUT CLASS="reactionmap" TYPE="image" NAME="MOVE_R" VALUE="0.25" SRC="/icons/right.png" ALT="move right" HEIGHT="10" WIDTH="10" /></TD>) .
	qq(<TD CLASS="reactionmap"><INPUT CLASS="reactionmap" TYPE="image" NAME="MOVE_U" VALUE="-0.25" SRC="/icons/up.png" ALT="move up" HEIGHT="10" WIDTH="10" /></TD>) .
	qq(<TD CLASS="reactionmap"><INPUT CLASS="reactionmap" TYPE="image" NAME="MOVE_D" VALUE="0.25" SRC="/icons/down.png" ALT="move down" HEIGHT="10" WIDTH="10" /></TD>) .
	qq(</TR>) .
	qq(<TR><TD COLSPAN="12">) .
	qq(<MAP NAME=img_map>) . $rm->usemap . qq(</MAP>) .
	qq(<img USEMAP=\#img_map BORDER="0" SRC="/img-tmp/$name">) .
	qq(</TD></TR>) .
        $cgi->end_form .
	$key_str .
	qq(</TABLE>\n) .
	qq(</DIV>\n);
}

# Implementation, if any, has to come from sub-classes
sub reactionmap_key {
    return '';
}

sub html_table_w_dynamic_eventhierarchy {
    return html_table(@_);
}

sub old_eventbrowser_html_table {
    return html_table(@_);
}

sub create_EntityHierarchy {
    my $self = shift;
    my $eh = GKB::DTreeMaker::EntityHierarchy::Downwards->new(-WEBUTILS => $self->webutils);
    return
	qq(<script language="javascript" src="/javascript/dtree/dtree.js"></script>\n) .
	qq(<link href="/javascript/dtree/dtree.css" rel="stylesheet" type="text/css">\n) .
	$eh->create_tree($self);
}

sub entityHierarchy_as_1_row {
    my $self = shift;
    return qq(<TR><TH CLASS="entityhierarchy"><A NAME="entityhierarchyAnchor">Hierarchical view of the components</A></TH><TD CLASS="entityhierarchy">) . $self->create_EntityHierarchy . qq(</TD></TR>\n);
}

sub complex_components_dynamic_tree {
    my $self = shift;
    my $eh = GKB::DTreeMaker::EntityHierarchy::ComponentsAndRepeatedUnits::Downwards->new(-WEBUTILS => $self->webutils);
    return
	qq(<script language="javascript" src="/javascript/dtree/dtree.js"></script>\n) .
	qq(<link href="/javascript/dtree/dtree.css" rel="stylesheet" type="text/css">\n) .
	$eh->create_tree($self);
}

sub complex_components_dynamic_tree_as_1_row {
    my $self = shift;
    return qq(<TR><TH CLASS="entityhierarchy"><A NAME="entityhierarchyAnchor">Hierarchical view of the components</A></TH><TD CLASS="entityhierarchy">) . $self->complex_components_dynamic_tree . qq(</TD></TR>\n);
}

sub complexes_and_polymers {
    my $self = shift;
    my $ar = $self->follow_class_attributes
	(-INSTRUCTIONS =>
	 {
	  'ReferenceSequence' => {'reverse_attributes' => [qw(referenceEntity)]},
	  'ReferenceMolecule' => {'reverse_attributes' => [qw(referenceEntity)]},
	  'ReferenceMoleculeClass' => {'reverse_attributes' => [qw(referenceEntity)]},
	  'PhysicalEntity' => {'reverse_attributes' => [qw(hasComponent repeatedUnit hasMember)]}
	 },
	 -OUT_CLASSES => ['Complex','Polymer']
	 );
    @{$ar} = grep {$_ != $self} @{$ar};
#    @{$ar} = grep {$_->reverse_attribute_value('input')->[0]
#		       || $_->reverse_attribute_value('output')->[0]
#		       || $_->reverse_attribute_value('physicalEntity')->[0]
#		       || $_->reverse_attribute_value('regulator')->[0]} @{$ar};
    # David Croft: attempt to fix problems that Peter encountered with EWAS in
    # clostridium, where the "Other forms of this molecule"
    # links were inactive (ID=181473, if you want to try it).
    # Substitute pathwaybrowserdata with eventbrowser.
    my $components = $self->instances_as_1row('Component of','hasComponentRev',$ar);
    $components = $self->swap_pathwaybrowserdata_for_eventbrowser($components);
    return $components;
}

# Returns 1 if an identifier database can be found, 0 otherwise.
sub exists_identifier_database {
    my $self = shift;

	# Gives access to a whole bunch of methods for dealing with
	# previous releases and stable identifiers.
	my $si = GKB::StableIdentifiers->new($self->cgi);

	my $identifier_database_dba = $si->get_identifier_database_dba();
	if ($identifier_database_dba) {
		return 1;
	}

	return 0;
}

sub page_title {
    return
	qq(<DIV CLASS="section">) .
	qq(<DIV CLASS="pagetitle">) .
	#$_[0]->hyperlinked_displayName .
	GKB::PrettyInstance::hyperlinked_displayName(@_) .
	qq(</DIV>) .
	qq(</DIV><!-- section -->\n);
}

# Has to be properly implemented by subclasses.
sub hyperlinked_abbreviation {
    return '';
}

sub other_cell_aware_entity_display_names {
    my ($self,$entities_ar) = @_;
    my $h = {};
    my $total;
    foreach my $i (@{$entities_ar}) {
	$total++;
	$h->{$i->db_id}++;
    }
    my $h2 = {};
    my $flag;
    my $total_other;
    if ($self->is_valid_attribute('entityOnOtherCell')) {
        unless ($h2 = $self->get_cached_value('entitiesOnOtherCell')) {
	    $flag++; #no cached value available
            foreach my $e (@{$self->EntityOnOtherCell}) {
		$total_other++;
                $h2->{$e->db_id}++;
            }
            $self->set_cached_value('entitiesOnOtherCell',$h2);
        }
    }
    my @out;
    foreach my $e (@{$entities_ar}) {
	if ($flag && $total_other && ($total == $total_other)) {
#there is no cached value, i.e. this should come via 'input', and all entities are on the other cell, i.e. this is probably a transport reaction => for input, the normal compartment should be applied (if there is no cached value, the entities could also come in via a catalyst, but in this case one shouldn't expect the second condition to be fulfilled, namely that all entities are on the other cell)
	    push @out, $self->prettyfy_instance($e)->hyperlinked_displayName;
	} elsif ($h->{$e->db_id} > $h2->{$e->db_id}) {  #the number of instances with the same id that should be placed onto the other cell is smaller than the total number with the same id => use normal compartment display name first
	    push @out, $self->prettyfy_instance($e)->hyperlinked_displayName;
	    $h->{$e->db_id}--;
	} else { #other cell compartment display name
	    push @out, $self->prettyfy_instance($e)->hyperlinked_displayName(1);
	}
    }
    return _collapse_duplicate_values(\@out);
}

#########################################################################################
#
# SUBCLASSES START HERE
#
#########################################################################################

package GKB::PrettyInstance::Affiliation;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::PrettyInstance);

sub html_table_rows {
    my ($self) = @_;
    return
	$self->attribute_value_as_1row('Name',['name']) .
	$self->attribute_value_as_1row('Address',['address']) .
	$self->reverse_attribute_value_as_1row('Affiliated people', ['affiliation']);
}


package GKB::PrettyInstance::CatalystActivity;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::PrettyInstance);

sub html_table_rows {
    my ($self) = @_;
    return
	$self->prepared_attribute_value_as_1row('Name','name',[$self->displayName]) .
	$self->reverse_attribute_value_as_1row('Catalyses reactions', ['catalystActivity']) .
	$self->regulation_as_rows;}


package GKB::PrettyInstance::Complex;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::PrettyInstance::PhysicalEntity);

sub soft_displayName {
    my $self = shift;
    return $self->hyperlinked_string('[Molecular complex] '.($self->displayName || ''));
}

package GKB::PrettyInstance::Polymer;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::PrettyInstance::PhysicalEntity);


package GKB::PrettyInstance::DatabaseIdentifier;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance);

sub hyperlinked_identifier {
    my ($self) = @_;
    ($self->is_valid_attribute('identifier') && $self->is_valid_attribute('referenceDatabase')) ||
	$self->throw("Class " . $self->class . " does not have attributes 'identifier' and 'referenceDatabase' and you shouldn't call this method on this instance.");
    my ($refdb,$url);
    if ($refdb = $self->ReferenceDatabase->[0] and
	$url = $refdb->AccessUrl->[0]) {
	if (my $id = $self->Identifier->[0]) {
	    $url =~ s/###ID###/$id/g;
	    return qq(<A HREF="$url">) . $self->displayName . qq(</A>);
	}
    }
    return GKB::PrettyInstance::hyperlinked_displayName($self);
}

sub create_image {
    my ($self,$format) = @_;
    my $reactions = $self->follow_class_attributes
	(-INSTRUCTIONS =>
	 {
	  'DatabaseIdentifier' => {'reverse_attributes' => [qw(crossReference interactionIdentifier)]},
	  'PhysicalEntity' => {'reverse_attributes' => [qw(hasComponent physicalEntity input output)]},
	  'CatalystActivity' => {'reverse_attributes' => [qw(catalystActivity)]}},
	 -OUT_CLASSES => ['Reaction']
	 );
    my $rm;
    $rm = new GKB::ReactionMap(-DBA => $self->dba,-CGI => $self->cgi,-ORIGIN_INSTANCE=>$self,-FORMAT=>$format);
    $rm->set_reaction_color(0,0,128,$reactions);
    $rm->create_image_for_species($self->focus_species_or_default);
    return $rm;
}

sub hyperlinked_displayName {
    my ($self) = @_;
    return $self->hyperlinked_identifier;
}

sub internal_and_external_link {
    my ($self) = @_;
    return $self->SUPER::hyperlinked_displayName . ' [' .$self->hyperlinked_displayName . ']';
}

sub html_table {
    my ($self) = @_;
    $self->debug && print join("\t", (caller(0))[3], $self,  $self->class, ($self->db_id || '')), "\n";
    if (@{$self->reverse_attribute_value('crossReference')} == 1) {
	return $self->prettyfy_instance($self->reverse_attribute_value('crossReference')->[0])->html_table;
    } else {
	return
	    $self->page_title
	    . $self->reactionmap_html
	    . $LINK_TO_SURVEY
	    . qq(<DIV CLASS="section">)
	    . qq(\n<TABLE WIDTH="$HTML_PAGE_WIDTH" BORDER="0" CLASS=") . $self->class . qq(">\n)
	    . $self->html_table_rows_if_necessary
	    . qq(</TABLE>\n)
	    . qq(</DIV>\n)
	    ;
    }
}

sub html_table_rows {
    my ($self) = @_;
    return
	$self->prepared_attribute_value_as_1row('Identifier','identifier', [$self->hyperlinked_identifier], 'The name of the external database identifier') .
	$self->attribute_value_as_1row('Database',['referenceDatabase']) .
	$self->reverse_attribute_value_as_1row('Used as a crossreference in',['crossReference']) .
	($self->is_valid_reverse_attribute('interactionIdentifier')
	 ? $self->reverse_attribute_value_as_1row('This interaction occurs in',['interactionIdentifier'])
	 : ''
	);

}

sub soft_displayName {
    my $self = shift;
    return
	$self->hyperlinked_string('[Identifier] '.($self->displayName || ''))
	#. ($self->reverse_attribute_value('crossReference')->[0]
	#   ? ' ' . $self->reverse_attribute_value('crossReference')->[0]->displayName
	#   : '')
	    ;
}

sub hyperlinked_abbreviation {
    my ($self) = @_;
    if ($self->is_valid_attribute('identifier') && $self->is_valid_attribute('referenceDatabase')) {
	if (my $refdb = $self->ReferenceDatabase->[0]) {
	    if (my $id = $self->Identifier->[0]) {
		if (my $url = $refdb->AccessUrl->[0]) {
		    $url =~ s/###ID###/$id/g;
		    my $dn = $refdb->displayName;
		    my ($abbr) =  $dn =~ /^(\w)/;
		    (my $acls = $dn) =~ s/\s+//g;
		    return qq(<A HREF="$url" CLASS="$acls" onMouseover="ddrivetip('Go to $dn:$id','#DCDCDC', 250)" onMouseout="hideddrivetip()">$abbr</A>);
		}
	    }
	}
    }
    return '';
}


package GKB::PrettyInstance::ConceptualEvent;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::PrettyInstance::Event);


package GKB::PrettyInstance::EquivalentEventSet;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::PrettyInstance::Event);


package GKB::PrettyInstance::Event;
use vars qw(@ISA);
use strict;
use GKB::Config;

use Data::Dumper;
use Log::Log4perl qw/get_logger/;
Log::Log4perl->init(\$LOG_CONF);

@ISA = qw(GKB::PrettyInstance);


sub create_image {
    my ($self,$format) = @_;
    my $start = time();
    my $reactions = $self->follow_class_attributes
	(-INSTRUCTIONS =>
	 {
	     'ConceptualEvent' => {'attributes' => [qw(hasSpecialisedForm)]},
	     'EquivalentEventSet' => {'attributes' => [qw(hasMember)]},
	     'Pathway' => {'attributes' => [qw(hasComponent hasEvent)]},
	     'Reaction' => {'attributes' => [qw(hasMember)]},
	     'BlackBoxEvent' => {'attributes' => [qw(hasComponent)]},
	 },
	 -OUT_CLASSES => ['Reaction', 'ReactionlikeEvent']
	);
    my $rm;
    $rm = new GKB::ReactionMap(-DBA => $self->dba,-CGI => $self->cgi,-NO_DEFAULT_IMAGE => 1,-ORIGIN_INSTANCE=>$self,-FORMAT=>$format);
    $rm->reset_offset_if_necessary($reactions);
    $rm->colour_reactions_by_evidence_type($reactions,1);
    $rm->create_image_for_species($self->focus_species_or_default);
#   print qq(<PRE>), (caller(0))[3],"\t",  time() - $start, qq(</PRE>\n);
    return $rm;
}

sub reactionmap_key {
    return qq(<IMG SRC="/icons/event_reactionmap_key.png">);
}

sub top_browsing_view {
    my ($self, $doi_flag) = @_;

    #$self->urlmaker->turn_off_pwb_link();
    my $current_url_maker = $self->urlmaker();
    $self->urlmaker($self->create_url_maker('author_contributions'));

    my $authors = GKB::Utils::get_authors_recursively($self);
    my $authors_str = join(", ",map {get_person_link($_)} @{$authors}) || '&nbsp';

    my $reviewers = GKB::Utils::get_reviewers_recursively($self);
    my $reviewers_str = join(", ",map {get_person_link($_)} @{$reviewers}) || '&nbsp';

    my $editors = GKB::Utils::get_editors_recursively($self);
    my $editors_str = join(", ",map {get_person_link($_)} @{$editors}) || '&nbsp';

    my $out = qq(<TR CLASS="contents"><TD CLASS="sidebar" WIDTH="33%">);
    if ($doi_flag) {
        $out .= $self->displayName();
        $out .= qq(</TD>);
        my $doi = "";
        if (defined $self->doi && scalar(@{$self->doi})>0 && defined $self->doi->[0]) {
            $doi = $self->doi->[0];
        }
        $out .= qq(<TD>$doi</TD>);
    } else {
        my $urlmaker_instance = $current_url_maker->clone();
        $urlmaker_instance->_delete_cached_url();
        my $treemaker = GKB::HtmlTreeMaker::NoReactions->new(-ROOT_INSTANCES => [$self],
					    -URLMAKER => $urlmaker_instance,
					    -INSTRUCTIONS => {
						'Pathway' => {'attributes' => ['hasComponent','hasEvent']},
						'ConceptualEvent' => {'attributes' => ['hasSpecialisedForm']},
						'EquievalentEventSet' => {'attributes' => ['hasMember']},
						'Reaction' => {'attributes' => [qw(hasMember)]},
						'BlackBoxEvent' => {'attributes' => ['hasComponent']},
						},
					    -DEPTH => 1,
					    -DEFAULT_CLASS => 'sidebar',
					    -ATTRIBUTE_LABELS => {'hasComponent' => '', 'hasMember' => '', 'hasSpecialisedForm' => '', 'hasEvent' => ''});
        $treemaker->force_pwb_link(1);
        $out .= $treemaker->tree;
        $out .= qq(</TD>);
    }
    $out .= qq(<TD CLASS="author">$authors_str</TD>);

    my $tmp = $self->ReleaseDate->[0];
    my $class = 'releaseDate';
    if (my $status = GKB::Utils::get_event_release_status($self,$LAST_RELEASE_DATE)) {
	$tmp .= '<BR />' . qq(<SPAN CLASS="newtopic">$status</SPAN>);
	$class = 'newsandnotes';
    }
    my $revised_date = '&nbsp;';
    my $revised_class = 'releaseDate';
    # Want to get the latest revision
    if ($self->is_valid_attribute('revised') && $self->Revised->[0] && $self->Revised->[-1]->DateTime->[0]) {
	($revised_date = $self->Revised->[-1]->DateTime->[0]) =~ s/^(\d{4})-?(\d{2})-?(\d{2}).*/$1\-$2\-$3/;
	my $t = $revised_date;
	$t =~ s/\D//g;
	if ($t > $LAST_RELEASE_DATE) {
	    $revised_date .= '<BR />' . '<SPAN CLASS="newtopic">NEW</SPAN>';
	    $revised_class = 'newsandnotes';
	}
    }
    if (!(defined $tmp)) {
       $tmp = "";
    }
    $out .= qq(<TD CLASS="$class">$tmp</TD>)
	. qq(<TD CLASS="$revised_class">$revised_date</TD>)
	. qq(<TD CLASS="reviewer">$reviewers_str</TD>)
	. qq(<TD CLASS="editor">$editors_str</TD>)
	. qq(</TR>\n);
    return $out;
}

sub get_person_link {
    my $person_instance = shift;

    my $person_id = $person_instance->db_id;
    my $person_name = $person_instance->displayName;

    return qq{<a href="content/detail/person/$person_id">$person_name</a>};
}

sub create_url_maker {
    my ($self, $script_name) = @_;

    return GKB::URLMaker->new(
        -SCRIPTNAME => $script_name,
        'DB' => scalar $self->cgi->param('DB')
    );
}

# reference from ensembl pages for mouseover representation
sub mouse_over {
    my ($caption, $str) = @_;

    if(defined $str && $caption ne "") {
	return "onmouseover=\"showtip(\'$caption\',\'$str\')\"";
    } elsif(defined $str) {
	return "onmouseover=\"showtip(\'$str\');\"";
    } elsif(defined $caption) {
	return "onmouseover=\"showtip(\'$caption\')\"";
    } else {
	return "";
    }
}

sub html_table {
##    return html_table_w_dynamic_eventhierarchy(@_);
    return old_eventbrowser_html_table(@_);
}

sub html_table_w_dynamic_eventhierarchy {
    my ($self) = @_;
    my $cgi = $self->cgi;
    my %preferences = $cgi->cookie('preferences');
    my $out =
	$self->reactionmap_html .
	qq(<DIV CLASS="section">\n<TABLE CLASS="sidebar" WIDTH="$HTML_PAGE_WIDTH" BORDER="0" CELLPADDING="0" CELLSPACING="0">\n<TR>);

    $out .= $self->dynamic_eventhierarchy_and_details_side_by_side;
    $out .= $self->_view_switch_html;
    $out .= qq(</TD>\n</TR>\n</TABLE>\n</DIV>);
    return $out;
}

sub dynamic_eventhierarchy_and_details_side_by_side {
    my $self = shift;
    my $eh = GKB::DTreeMaker::EventHierarchy->new(-WEBUTILS => $self->webutils);
    return
	qq(<script language="javascript" src="/javascript/dtree/dtree.js"></script>\n)
	. qq(<link href="/javascript/dtree/dtree.css" rel="stylesheet" type="text/css">\n)
	. qq(<TD CLASS="sidebar" VALIGN="top" WIDTH="300px">)
	. $eh->boxed_controls($self)
	. qq{<DIV CLASS="eventhierarchy">}
        . $eh->create_tree_wo_controls($self)
	. qq{</DIV>}
        . qq(</TD>\n<TD VALIGN="top">)
	. qq(<TABLE WIDTH="100%" CELLSPACING="0" CLASS=\") . $self->class . qq(\">\n)
	. $self->html_table_rows_if_necessary
	. qq(</TABLE>\n</TD>);
}

sub old_eventbrowser_html_table {
    my ($self) = @_;
    my $cgi = $self->cgi;
    my %preferences = $cgi->cookie('preferences');
    my $out =
	$self->page_title
	. $self->reactionmap_html
	. $LINK_TO_SURVEY
	. qq(<DIV CLASS="section">\n<TABLE CLASS="sidebar" WIDTH="$HTML_PAGE_WIDTH" BORDER="0" CELLPADDING="0" CELLSPACING="0">\n<TR>);

    my $db_id = $self->db_id;
    # Process hierarchy
    if (lc($preferences{'eventhierarchy'}) eq 'off') {
	# hierarchy hidden
	$out .=
	    qq(<TD CLASS="sidebar" VALIGN="top">) .
	    qq(<A ONMOUSEOVER="ddrivetip('Show process hierarchy','#DCDCDC',150)" ONMOUSEOUT="hideddrivetip()" ONCLICK="setValue($db_id,'eventhierarchy','on');setValue($db_id,'ID',$db_id);submitForm($db_id);"><IMG SRC="/icons/plus-box.gif" HEIGHT="10" WIDTH="10" BORDER="0"></A>);
    } else {
#	$out .= qq(<script language="JavaScript" src="/javascript/hierarchy.js"></script>);
	# hierarchy shown
	my $treemaker = GKB::HtmlTreeMaker::SpringloadedHierarchy->new
	    (-INSTANCES => [$self],
	     -URLMAKER => $self->urlmaker,
	     -CGI => $self->cgi,
#	     -SORTING_FN => \&GKB::Utils::order_Events
	     );
	$out .= qq(<TD CLASS="sidebar" VALIGN="top">) .
	    qq(<A ONMOUSEOVER="ddrivetip('Hide process hierarchy','#DCDCDC',150)" ONMOUSEOUT="hideddrivetip()" ONCLICK="setValue($db_id,'eventhierarchy','off');setValue($db_id,'ID',$db_id);submitForm($db_id);"><IMG SRC="/icons/minus-box.gif" HEIGHT="10" WIDTH="10" BORDER="0"></A>) .
	    ($self->cgi && $self->cgi->param('SHOW_HIERARCHY_TYPES')
	     ? qq(<P CLASS="level0">&nbsp;<A CLASS="attributename">C</A> - component, <A CLASS="attributename">I</A> - instance</P>\n)
	     : '') .
	     $treemaker->super_event_hierarchy_and_direct_subevents_in_sidebar2;
    }
    $out .= qq(</TD>\n<TD VALIGN="top">);
    $out .= qq(<TABLE WIDTH="100%" CELLSPACING="0" CLASS=\") . $self->class . qq(\">\n);
    $out .= $self->html_table_rows_if_necessary;
    $out .= $self->_view_switch_html;
    $out .= qq(</TABLE>\n);
    $out .= qq(</TD>\n</TR>\n</TABLE>\n</DIV>);
    return $out;
}

sub html_table_rows {
    my ($self) = @_;
    my $start = time();
    my $out =
	qq(<TR><TD CLASS="eventname" COLSPAN="2">) . $self->Name->[0] . ($self->EvidenceType->[0] ? ' <A CLASS="evidenceType">[' . $self->EvidenceType->[0]->displayName . ']</A>' : '') . qq(</TD></TR>\n) .
#	$self->creation_info_as_one_row .
	$self->_doi() .
	$self->_stable_identifier() .
	$self->attribute_value_as_1row('Authored', ['authored']) .
	$self->attribute_value_as_1row('Reviewed', ['reviewed']) .
	($self->is_valid_attribute('revised') ? $self->attribute_value_as_1row('Revised', ['revised']) : '') .
	$self->_user_feedback() .
	$self->prepared_attribute_value_as_1row(undef,'summation',[map {$self->prettyfy_instance($_)->html_text} @{$self->Summation}]) .
	$self->prepared_attribute_value_as_1row(undef,'figure',[map {$self->prettyfy_instance($_)->html_text} @{$self->Figure}]) .
#	$self->attribute_value_wo_duplicates_as_1row('Input (present at start of reaction)',['input'], 'The entities which constitute all of the input\(s\) of the event') .
#	$self->attribute_value_wo_duplicates_as_1row('Output (present at end of reaction)',['output'], 'The entities which constitute all of the output\(s\) of the event') .
	$self->prepared_attribute_value_as_1row('Input (present at start of reaction)','input',$self->other_cell_aware_entity_display_names($self->Input)) .
	$self->prepared_attribute_value_as_1row('Output (present at end of reaction)','output',$self->other_cell_aware_entity_display_names($self->Output)) .
	$self->attribute_value_as_1row('Essential input component',['requiredInputComponent']) .
	$self->_catalyst_stuff .
	$self->attribute_value_as_1row('Preceding event(s)',['precedingEvent'], 'The events that directly precede the current event') .
	$self->reverse_attribute_value_as_1row('Following event(s)',['precedingEvent'], 'The events that proceed directly after the current event') .
	$self->_reverseReactions .
	$self->attribute_value_as_1row('Organism',['species']) .
	$self->attribute_value_as_1row('Cellular compartment',['compartment'], 'The compartment in the cell where the event takes place') .
	$self->attribute_value_as_1row('Cell type',['cellType'], 'The type of the cell in which this instance resides') .
	$self->attribute_value_as_1row('External identifier',['crossReference'], 'The name of the external database identifier') .
	($self->is_valid_attribute('entityOnOtherCell') ? $self->attribute_value_as_1row('Entity on other cell',['entityOnOtherCell'], 'Entity on other cell') : '') .
	$self->regulation_as_rows .
	$self->literaturereferences_as_2rows .

	($self->is_valid_attribute('goBiologicalProcess') ?
	 $self->attribute_value_as_1row('Represents GO biological process',['goBiologicalProcess'], 'This constitutes the GO Biological Process terms which is used for cross-referencing our Events') :
	 '') .
	$self->_orthologous_and_inferredFrom .
	$self->_normal_reaction .
	$self->_participating_molecules .
	'';
#    print qq(<PRE>), (caller(0))[3],"\t",  time() - $start, qq(</PRE>\n);
    return $out;
}

sub few_details {
    my ($self) = @_;
    my $start = time();
    my $out =
	qq(<TR><TD CLASS="eventname" COLSPAN="2">) . $self->Name->[0] . ($self->EvidenceType->[0] ? ' <A CLASS="evidenceType">[' . $self->EvidenceType->[0]->displayName . ']</A>' : '') . qq(</TD></TR>\n) .
#	$self->creation_info_as_one_row .
	$self->_doi() .
	$self->_stable_identifier() .
	$self->attribute_value_as_1row('Authored', ['authored']) .
	$self->attribute_value_as_1row('Reviewed', ['reviewed']) .
	($self->is_valid_attribute('revised') ? $self->attribute_value_as_1row('Revised', ['revised']) : '') .
	$self->prepared_attribute_value_as_1row(undef,'summation',[map {$self->prettyfy_instance($_)->html_text} @{$self->Summation}]) .
	$self->prepared_attribute_value_as_1row(undef,'figure',[map {$self->prettyfy_instance($_)->html_text} @{$self->Figure}]) .
#	$self->attribute_value_wo_duplicates_as_1row('Input (present at start of reaction)',['input'], 'The entities which constitute all of the input\(s\) of the event') .
#	$self->attribute_value_wo_duplicates_as_1row('Output (present at end of reaction)',['output'], 'The entities which constitute all of the output\(s\) of the event') .
	$self->prepared_attribute_value_as_1row('Input (present at start of reaction)','input',$self->other_cell_aware_entity_display_names($self->Input)) .
	$self->prepared_attribute_value_as_1row('Output (present at end of reaction)','output',$self->other_cell_aware_entity_display_names($self->Output)) .
	$self->attribute_value_as_1row('Essential input component',['requiredInputComponent']) .
	$self->_catalyst_stuff .
	$self->attribute_value_as_1row('Preceding event(s)',['precedingEvent'], 'The events that directly precede the current event') .
	$self->reverse_attribute_value_as_1row('Following event(s)',['precedingEvent'], 'The events that proceed directly after the current event') .
	$self->_reverseReactions .
	$self->attribute_value_as_1row('Organism',['species']) .
	$self->attribute_value_as_1row('Cellular compartment',['compartment'], 'The compartment in the cell where the event takes place') .
	$self->attribute_value_as_1row('Cell type',['cellType'], 'The type of the cell in which this instance resides') .
	$self->attribute_value_as_1row('External identifier',['crossReference'], 'The name of the external database identifier') .
	$self->regulation_as_rows .
	$self->literaturereferences_as_2rows .

	($self->is_valid_attribute('goBiologicalProcess') ?
	 $self->attribute_value_as_1row('Represents GO biological process',['goBiologicalProcess'], 'This constitutes the GO Biological Process terms which is used for cross-referencing our Events') :
	 '') .
	$self->_orthologous_and_inferredFrom .
	$self->_normal_reaction .
	'';
#    print qq(<PRE>), (caller(0))[3],"\t",  time() - $start, qq(</PRE>\n);
    return $out;
}

sub _reverseReactions {
    my $self = shift;
    $self->is_valid_attribute('reverseReaction') || return '';
    my $out = '';
    my @tmp = @{$self->ReverseReaction};
    eval {
	$self->add_attribute_value_if_necessary
	    ('reverseReaction', @{$self->reverse_attribute_value('reverseReaction')});
	$out .= $self->attribute_value_as_1row('Reverse reaction',[qw(reverseReaction)]);
    };
    $self->ReverseReaction(@tmp);
    if ($@) {
	$self->throw($@);
    }
    return $out;
}

sub _orthologous_and_inferredFrom {
    my $self = shift;
    my $out = '';
    my %seen;
    map {$seen{$_->db_id}++} @{$self->InferredFrom};
    $out .= $self->attribute_value_as_1row('This event is deduced on the basis of event(s)',[qw(inferredFrom)], 'Points to the event(s), usually in another species, that this event has been inferred from. If the inference is based on computation only, this is indicated under evidenceType \(IEA\)');
    # The point here is to avoid showing Events which are in the inferredFrom slot.
    # As I'm lazy I temporarily change orthologousEvent values of the current instance.
    my @orthologousEvents = @{$self->OrthologousEvent};
    # Remove the Events which are also in the inferredFrom slot.
    $self->attribute_value('orthologousEvent', undef);
    $self->attribute_value('orthologousEvent', grep {! $seen{$_->db_id}} @orthologousEvents);
    # eval is used to make sure that the original is restored even if something goes wrong
    eval {
	$out .= $self->attribute_value_as_1row('Equivalent event(s) in other organism(s)',[qw(orthologousEvent)], 'Points to the event or entity in another species that this event\/entity has been inferred from. If the inference is based on computation only, this is indicated under evidenceType \(IEA\)');
    };
    if ($@) {
	$self->attribute_value('orthologousEvent',@orthologousEvents);
	$self->throw($@);
    }
    # Restore original
    $self->attribute_value('orthologousEvent',@orthologousEvents);
    return $out;
}

sub _normal_reaction {
    my $self = shift;

    my $logger = get_logger(__PACKAGE__);

    my $out = '';
    $logger->info("entered\n");
    if ($self->is_valid_attribute("normalReaction") && (defined $self->normalReaction) && scalar(@{$self->normalReaction})>0) {
        $out .= $self->attribute_value_as_1row('Normal reaction', ['normalReaction']);
        $logger->info("found a normal reaction\n");
    }
    return $out;
}

sub _catalyst_stuff {
    my ($self) = @_;
    my $out = '';
    if ($self->CatalystActivity->[0]) {
	my %seen;
	my @a;
	foreach my $ca (@{$self->CatalystActivity}) {
	    if (my $pe = $ca->PhysicalEntity->[0]) {
		unless ($seen{$pe->db_id}++) {
		    #push @a, $self->prettyfy_instance($pe)->hyperlinked_displayName;
		    push @a, $self->other_cell_aware_entity_display_names([$pe])->[0];
		}
	    } else {
		if (my $a = $ca->Activity->[0]) {
		    (my $str = $a->Name->[0]) =~ s/\s*activity\s*//;
		    push @a, 'Unknown ' . $str;
		}
	    }
	}
	$out .= qq(<TR><TH CLASS="catalystActivity"><A NAME="catalystActivityAnchor">Catalyst</A></TH><TD CLASS="catalystActivity">) .
	    join ("<BR />", @a) . qq(</TD></TR>\n);
    }
    if ($self->ontology->is_valid_class_attribute('CatalystActivity','activeUnit')) {
	if (my @au = map {@{$_->ActiveUnit}} @{$self->CatalystActivity}) {
	    $out .= qq(<TR><TH CLASS="catalystActivity">Essential catalyst component</TH><TD CLASS="inputCatalyst">) .
		join ("<BR />", map {$self->prettyfy_instance($_)->hyperlinked_displayName} @au) .
		qq(</TD></TR>\n);
	}
    }
    if ($self->CatalystActivity->[0] && $self->CatalystActivity->[0]->Activity->[0]) {
	$out .= qq(<TR><TH CLASS="catalystActivity">GO molecular function</TH><TD CLASS="catalystActivity">) .
	    join ("<BR />", map {$self->prettyfy_instance($_->Activity->[0])->hyperlinked_displayName} @{$self->CatalystActivity}) .
	    qq(</TD></TR>\n);
    }
    return $out;
}

sub get_participating_molecules {
    my ($self) = shift;
    my $ar = $self->follow_class_attributes
	(-INSTRUCTIONS =>
	 {'Event' => {'attributes' => [qw(input output catalystActivity)]},
	  'ReactionlikeEvent' => {'attributes' => [qw(input output catalystActivity)]},
	  'Pathway' => {'attributes' => [qw(hasComponent hasEvent)]},
	  'BlackBoxEvent' => {'attributes' => [qw(hasComponent)]},
	  'CatalystActivity' => {'attributes' => [qw(physicalEntity)]},
	  'Complex' => {'attributes' => [qw(hasComponent)]},
	  'ConceptualEvent' => {'attributes' => [qw(hasSpecialisedForm)]},
	  'EquivalentEventSet' => {'attributes' => [qw(hasMember)]},
	  'EntitySet' => {'attributes' => [qw(hasMember)]},
	  'Polymer' => {'attributes' => [qw(repeatedUnit)]},
	  'CandidateSet' => {'attributes' => [qw(hasMember hasCandidate)]},
          },
	 -OUT_CLASSES => [qw(SimpleEntity GenomeEncodedEntity OtherEntity EntitySet)]
	 );
    @{$ar} = grep {! ($_->is_a('Set') && $_->HasMember->[0])} @{$ar};
    return [] unless (@{$ar});
    @{$ar} = sort {lc($a->displayName) cmp lc($b->displayName)} @{$ar};

    return $ar;
}

sub _participating_molecules {
    my ($self) = shift;
    $DB::single=1;
    my $ar = $self->get_participating_molecules();
    return '' unless (@{$ar});
    my $out = qq(<TR><TH COLSPAN="2" CLASS="participants"><A NAME="participantsAnchor" HREF="javascript:void(0)" onMouseover="ddrivetip('Molecules that are involved in this event','#DCDCDC', 250)" onMouseout="hideddrivetip()">Participating molecules</A></TH></TR>\n<TR><TD COLSPAN="2" CLASS="participants">);
    if (@{$ar} > 10) {
	$out .= qq(<UL><LI>) . join("</LI>\n<LI>", (map {$self->prettyfy_instance($_)->hyperlinked_displayName} @{$ar}[0..9]));
	$out .= qq(<LI>...);
	my $form_name = 'participants_form_' . $self->db_id;
	$out .= $self->cgi->start_form(-name => $form_name, -method => 'POST', -action => $self->urlmaker->script_name);
	$out .= $self->cgi->hidden(-name => 'DB', -value => $self->dba->db_name);
	$self->cgi->delete('FORMAT');
	$out .= $self->cgi->hidden(-name => 'FORMAT', -value => 'list');
	# cgi->delete is necessary to make CGI to forget the "old" ID so that it can "fill it" with
	# new ones from the $values.
	$self->cgi->delete('ID');
	$out .= $self->cgi->hidden(-name => 'FOCUS_SPECIES', -value => [map {$_->displayName} @{$self->Species}]);
	$out .= $self->cgi->hidden(-name => 'ID', -value => [map {$_->db_id} @{$ar}]);
	$out .= qq{<A ONCLICK="document.$form_name.submit(); return false">List all } . scalar(@{$ar}) . qq{ participating molecules</A>};
#	$out .= $self->cgi->submit(-name => qq(List all ) . scalar(@{$ar}) . qq( participating molecules),
#				   );
	$out .= $self->cgi->end_form;
	$out .= qq(</LI></UL>);
    } else {
	$out .=
	    qq(<UL><LI>) .
	    join("</LI>\n<LI>", map {$self->prettyfy_instance($_)->hyperlinked_displayName}
		 sort {lc($a->displayName) cmp lc($b->displayName)} @{$ar}) .
	    qq(</LI></UL>);
    }
    $out.= qq(</TD></TR>\n);
    return $out;
}

sub displayName {
    my $self = shift;

    if (@_) {
		return $self->SUPER::displayName(@_);
    }
    if ($self->is_valid_attribute('species') && $self->Species->[0]) {
		my $species_names = "";
		my $species_counter = 0;
		my $species;
		foreach $species (@{$self->Species}) {
			if (!($species_names eq "")) {
				$species_names .= ", ";
			}
			if ($species_counter>1) {
				$species_names .= "etc.";
				last;
			}
			$species_names .= $species->_displayName->[0];
			$species_counter++;
		}
		return $self->SUPER::displayName . ' [' . $species_names . ']';
    } else {
		return $self->SUPER::displayName;
    }
}

# Override superclass method to avoid appending species name twice
sub hyperlinked_extended_displayName {
    my $self = shift;
    return $self->hyperlinked_string
	('['.$self->class.':'.$self->db_id.'] ' .
	 ($self->displayName || '')
	 );
}

sub _make_switch_html {
    my $self = shift;
    my $urlmaker = $self->urlmaker;
    (my $sbmlurl = $urlmaker->script_name) =~ s/\w+$/sbml_export/;
    $sbmlurl .= '?DB=' . ($urlmaker->param('DB'))[0] . '&ID=' . $self->db_id;
    (my $biopaxurl = $urlmaker->script_name) =~ s/\w+$/biopaxexporter/;
    #$biopaxurl.= '?DB=' . ($urlmaker->param('DB'))[0] . '&level=2' . '&ID=' . $self->db_id;
    my $biopaxurl2 = ($biopaxurl . '?DB=' . ($urlmaker->param('DB'))[0] . '&level=2' . '&ID=' . $self->db_id);
    my $biopaxurl3 = ($biopaxurl . '?DB=' . ($urlmaker->param('DB'))[0] . '&level=3' . '&ID=' . $self->db_id);
    (my $pdfurl = $urlmaker->script_name) =~ s/\w+$/pdfexporter/;
    $pdfurl .= '?DB=' . ($urlmaker->param('DB'))[0] . '&ID=' . $self->db_id;
    (my $rtfurl = $urlmaker->script_name) =~ s/\w+$/rtfexporter/;
    $rtfurl .= '?DB=' . ($urlmaker->param('DB'))[0] . '&ID=' . $self->db_id;
    (my $cytolaunchurl = $urlmaker->script_name) =~ s/\w+$/launchcytoscape/;
    $cytolaunchurl .= '?DB=' . ($urlmaker->param('DB'))[0] . '&ID=' . $self->db_id;
    (my $protegeurl = $urlmaker->script_name) =~ s/\w+$/protegeexporter/;
    $protegeurl .= '?DB=' . ($urlmaker->param('DB'))[0] . '&ID=' . $self->db_id;
    (my $svgexporterurl = $urlmaker->script_name) =~ s/\w+$/svgexporter/;
    $svgexporterurl .= '?DB=' . ($urlmaker->param('DB'))[0] . '&ID=' . $self->db_id;
    (my $networkviewerurl = $urlmaker->script_name) =~ s/\w+$/genmapp_reactome_navigator/;
    $networkviewerurl .= '?DB=' . ($urlmaker->param('DB'))[0] . '&ID=' . $self->db_id;

    my $tmp=<<__HERE__;
<A onMouseover="ddrivetip('View/download process in SBML level 2 format.','#DCDCDC', 250)" onMouseout="hideddrivetip()" HREF="$sbmlurl" CLASS="viewswitch">[SBML]</A>
<A onMouseover="ddrivetip('Download process in BioPAX level 2 format.','#DCDCDC', 250)" onMouseout="hideddrivetip()" HREF="$biopaxurl2" CLASS="viewswitch">[BioPAX2]</A>
<A onMouseover="ddrivetip('Download process in BioPAX level 3 format.','#DCDCDC', 250)" onMouseout="hideddrivetip()" HREF="$biopaxurl3" CLASS="viewswitch">[BioPAX3]</A>
<A onMouseover="ddrivetip('View/download process in PDF format.','#DCDCDC', 250)" onMouseout="hideddrivetip()" HREF="$pdfurl" CLASS="viewswitch">[PDF]</A>
<!--
<A onMouseover="ddrivetip('View/download process in RTF format.','#DCDCDC', 250)" onMouseout="hideddrivetip()" HREF="$rtfurl" CLASS="viewswitch">[RTF]</A>
-->
<A onMouseover="ddrivetip('View this Event in Cytoscape (requires Java Web Start).','#DCDCDC', 250)" onMouseout="hideddrivetip()" HREF="$cytolaunchurl" CLASS="viewswitch">[Cytoscape]</A>
<A onMouseover="ddrivetip('Download process as a Prot&eacute;g&eacute; project.','#DCDCDC', 250)" onMouseout="hideddrivetip()" HREF="$protegeurl" CLASS="viewswitch">[Prot&eacute;g&eacute;]</A>
<A onMouseover="ddrivetip('Download event diagram in SVG format.','#DCDCDC', 250)" onMouseout="hideddrivetip()" HREF="$svgexporterurl" CLASS="viewswitch">[SVG]</A>
<!--
<A onMouseover="ddrivetip('View pathway diagram','#DCDCDC', 250)" onMouseout="hideddrivetip()" HREF="$networkviewerurl" CLASS="viewswitch">[Diagram]</A>
-->
__HERE__

	if (!$CACHE_GENERATED_DOCUMENTS) {
		$tmp .= qq(<A onMouseover="ddrivetip('View/download process in RTF format.','#DCDCDC', 250)" onMouseout="hideddrivetip()" HREF="$rtfurl" CLASS="viewswitch">[RTF]</A>);
	}

    $tmp .= GKB::Utils::InstructionLibrary::taboutputter_popup_form($self);
    unless ($self->webutils && $self->webutils->omit_view_switch_link) {
		my $db = $self->dba->db_name;
		$tmp = qq(<A HREF="javascript:void(0)" onMouseover="ddrivetip('Allows to change the default format in which the content is presented','#DCDCDC', 250)" onMouseout="hideddrivetip()" onClick="X=window.open('/cgi-bin/formatselector?DB=$db','formatselector','height=200,width=400,left=10,screenX=10,top=10,screenY=10,resizable,scrollbars=yes');X.focus" CLASS="viewswitch">[Change default viewing format]</A>\n) . $tmp;
    }
#    return qq(<TR><TD COLSPAN="2" CLASS="viewswitch">\n$tmp\n</TD></TR>\n);
    return $tmp;
}

#-start of new method to swich to pdf and SVG-------------------


sub _reactionmap_view_switch_html {
    my $self = shift;
    my $urlmaker = $self->urlmaker;

#parametreina pitaa varmaan antaa kartan parametrit, jotta tietaa mita piirtaa
# I'll second that.  DC.


    (my $svgurl = $urlmaker->script_name) =~ s/\w+$/SVGexporter.pl/;
    $svgurl .= '?DB=' . ($urlmaker->param('DB'))[0] . '&ID=' . $self->db_id;

#    $svgurl .= '?ID=' . $self;
#have to get the prettyinstance instance!!!



#parametrina pitaa antaa svg-kuva, jotta tietaa mita lahdetaan kaantamaan pdf:ksi
    (my $reactionmap_pdfurl = $urlmaker->script_name) =~ s/\w+$/reactionmap_pdfexporter/;
   # $reactionmap_pdfurl .= '?DB=' . ($urlmaker->param('DB'))[0] . '&ID=' . $self->db_id;
  $reactionmap_pdfurl .= '?SVG=' . 'koe2.svg';#HUOM tanne mika svg-kuva muutetaan!!!! - jos ei kuvaaa ole olemassa caschessa se pitaa luoda....


    my $tmp=<<__HERE__;
<A onMouseover="ddrivetip('View/download reactionmap in SVG format.','#DCDCDC', 350)" onMouseout="hideddrivetip()" HREF="$svgurl" CLASS="viewswitch">[SVG]</A>

<A onMouseover="ddrivetip('View/download reactionmap in PDF format.','#DCDCDC', 250)" onMouseout="hideddrivetip()" HREF="$reactionmap_pdfurl" CLASS="viewswitch">[PDF]</A>
__HERE__


return qq(<TR><TD COLSPAN="2" CLASS="viewswitch">\n$tmp\n</TD></TR>\n);
}


#------------------

package GKB::PrettyInstance::Figure;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance);

sub html_text {
    my ($self) = @_;
    $self->debug && print join("\t", (caller(0))[3], $self,  $self->class, ($self->db_id || '')), "\n";
    return '<IMG SRC="' . $self->attribute_value('url')->[0] . '" />';

}

sub html_table {
    my ($self) = @_;
    $self->debug && print join("\t", (caller(0))[3], $self,  $self->class, ($self->db_id || '')), "\n";
    if (my $referer = $self->reverse_attribute_value('figure')->[0]) {
	return $self->prettyfy_instance($referer)->html_table;
    } else {
#	if ($self->is_valid_attribute('_html') && $self->attribute_value('_html')->[0]) {
#	    return $self->attribute_value('_html')->[0];
#	}
	return $self->html_text;

    }

}


package GKB::PrettyInstance::GO_BiologicalProcess;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance::GO_Like_Thing);

sub create_image {
    my ($self,$format) = @_;
    my $rm = new GKB::ReactionMap(-DBA => $self->dba,-CGI => $self->cgi,-ORIGIN_INSTANCE=>$self,-FORMAT=>$format);
    my $reactions = $self->follow_class_attributes
	(-INSTRUCTIONS =>
#	 {'GO_BiologicalProcess' => {'reverse_attributes' => [qw(instanceOf componentOf goBiologicalProcess)]},
	 {'GO_BiologicalProcess' => {'reverse_attributes' => [$self->go_relationship_attributes(), 'componentOf', 'goBiologicalProcess']},
	  'Pathway' => {'attributes' => [qw(hasComponent hasEvent)]}, #GO_BiologicalProcesses are often associated with higher level events which are not shown themselves -> go down to reactions
	  'EquivalentEventSet' => {'attributes' => [qw(hasMember)]},
	  'ConceptualEvent' => {'attributes' => [qw(hasSpecialisedForm)]},
	  'Reaction' => {'attributes' => [qw(hasMember)]},
	  'BlackBoxEvent' => {'attributes' => [qw(hasComponent)]},
	 },
	 -OUT_CLASSES => ['Reaction','ReactionlikeEvent']
	 );
    $rm->set_reaction_color(0,0,128,$reactions);
    $rm->create_image_for_species($self->focus_species_or_default);
    return $rm;
}

sub html_table_rows {
    my ($self) = @_;
    my $events = $self->follow_class_attributes
	(
	 -INSTRUCTIONS => {
#	     'GO_BiologicalProcess' => {'reverse_attributes' => [qw(instanceOf goBiologicalProcess)]}
             'GO_BiologicalProcess' => {'reverse_attributes' => [qw(goBiologicalProcess)]}
#	     'GO_BiologicalProcess' => {'reverse_attributes' => [$self->go_relationship_attributes(), 'goBiologicalProcess']}
	 },
	 -OUT_CLASSES => ['Event']
	 );
    $events = $self->grep_for_instances_with_focus_species($events);
    @{$events} = sort {lc($a->displayName) cmp lc($b->displayName)} @{$events};

    my $parents = $self->follow_class_attributes
	(
#	 -INSTRUCTIONS => {'GO_BiologicalProcess' => {'attributes' => [qw(instanceOf componentOf)]}}
	 -INSTRUCTIONS => {'GO_BiologicalProcess' => {'attributes' => [$self->go_relationship_attributes(), 'componentOf']}}
	 );
    my $followed_path;
    map {$followed_path->{$_->db_id} = $_} @{$parents};
    my $root_instances;
    foreach (@{$parents}) {
	! $_->InstanceOf->[0] and ! $_->ComponentOf->[0] and push @{$root_instances}, $_;
    }
    # want to show also immediate kids
    map {$followed_path->{$_->db_id} = $_}
    ($self->go_relationship_reverse_attribute_values(),@{$self->reverse_attribute_value('componentOf')});
#    (@{$self->reverse_attribute_value('instanceOf')},@{$self->reverse_attribute_value('componentOf')});
    my $treeMaker = GKB::HtmlTreeMaker->new
	(-ROOT_INSTANCES => $root_instances,
	 -FOLLOWED_PATH_HASH => $followed_path,
#	 -INSTRUCTIONS => {'GO_BiologicalProcess' => {'reverse_attributes' => [qw(instanceOf componentOf)]}},
	 -INSTRUCTIONS => {'GO_BiologicalProcess' => {'reverse_attributes' => [$self->go_relationship_attributes(), 'componentOf']}},
#	 -ATTRIBUTE_LABELS => {'instanceOf' => 'I', 'componentOf'=> 'C'},
	 -ATTRIBUTE_LABELS => {$self->go_relationship_abbreviations_hash(), 'componentOf'=> 'C'},
	 -URLMAKER => $self->urlmaker,
	 -HIGHLITE1_HASH => {$self->db_id => $self},
	 -HIGHLITE1_CLASS => 'bold'
	 );
    $treeMaker->show_attribute(1);
    my $tree_str =
	qq(<TR><TH CLASS="instanceOf" COLSPAN="2">Parent processes and immediate children</TH></TR><TR><TD CLASS="instanceOf" COLSPAN="2">) .
	$treeMaker->tree .
	qq(</TD></TR>\n);
    @{$events} = map {$self->prettyfy_instance($_)->hyperlinked_displayName} @{$events};
    return
	$self->attribute_value_as_1row('name',['name']) .
	$self->prepared_attribute_value_as_1row('Accession','accesson', [$self->hyperlinked_identifier]) .
	$self->attribute_value_as_1row('Definition',['definition']) .
	$self->values_as_1row('Instances of this process',
			      'goBiologicalProcessRev',
			      $events,
			      '<BR />'
			      ) .
	$tree_str
	;
}

sub few_details {
    my ($self) = @_;
    my $events = $self->follow_class_attributes
	(
	 -INSTRUCTIONS => {
#	     'GO_BiologicalProcess' => {'reverse_attributes' => [qw(instanceOf goBiologicalProcess)]}
	     'GO_BiologicalProcess' => {'reverse_attributes' => [$self->go_relationship_attributes(), 'goBiologicalProcess']}
	 },
	 -OUT_CLASSES => ['Event']
	 );
    $events = $self->grep_for_instances_with_focus_species_or_default($events);
    @{$events} = sort {lc($a->displayName) cmp lc($b->displayName)} @{$events};

    my $parents = $self->follow_class_attributes
	(
#	 -INSTRUCTIONS => {'GO_BiologicalProcess' => {'attributes' => [qw(instanceOf componentOf)]}}
	 -INSTRUCTIONS => {'GO_BiologicalProcess' => {'attributes' => [$self->go_relationship_attributes(), 'componentOf']}}
	 );
    my $followed_path;
    map {$followed_path->{$_->db_id} = $_} @{$parents};
    my $root_instances;
    foreach (@{$parents}) {
	! $_->InstanceOf->[0] and ! $_->ComponentOf->[0] and push @{$root_instances}, $_;
    }
    # want to show also immediate kids
    map {$followed_path->{$_->db_id} = $_}
    ($self->go_relationship_reverse_attribute_values(),@{$self->reverse_attribute_value('componentOf')});
#    (@{$self->reverse_attribute_value('instanceOf')},@{$self->reverse_attribute_value('componentOf')});
    my $treeMaker = GKB::HtmlTreeMaker->new
	(-ROOT_INSTANCES => $root_instances,
	 -FOLLOWED_PATH_HASH => $followed_path,
#	 -INSTRUCTIONS => {'GO_BiologicalProcess' => {'reverse_attributes' => [qw(instanceOf componentOf)]}},
	 -INSTRUCTIONS => {'GO_BiologicalProcess' => {'reverse_attributes' => [$self->go_relationship_attributes(), 'componentOf']}},
#	 -ATTRIBUTE_LABELS => {'instanceOf' => 'I', 'componentOf'=> 'C'},
	 -ATTRIBUTE_LABELS => {$self->go_relationship_abbreviations_hash(), 'componentOf'=> 'C'},
	 -URLMAKER => $self->urlmaker,
	 -HIGHLITE1_HASH => {$self->db_id => $self},
	 -HIGHLITE1_CLASS => 'bold'
	 );
    $treeMaker->show_attribute(1);
    my $tree_str =
	qq(<TR><TH CLASS="instanceOf" COLSPAN="2">Parent processes and immediate children</TH></TR><TR><TD CLASS="instanceOf" COLSPAN="2">) .
	$treeMaker->tree .
	qq(</TD></TR>\n);
    @{$events} = map {$self->prettyfy_instance($_)->hyperlinked_displayName} @{$events};
    return
	$self->attribute_value_as_1row('name',['name']) .
	$self->prepared_attribute_value_as_1row('Accession','accesson', [$self->hyperlinked_identifier]) .
	$self->attribute_value_as_1row('Definition',['definition']) .
	$self->values_as_1row('Instances of this process',
			      'goBiologicalProcessRev',
			      $events,
			      '<BR />'
			      ) .
	$tree_str
	;
}

sub soft_displayName {
    my $self = shift;
    return $self->hyperlinked_string('[GO biological process] '.($self->displayName || ''));
}

# Returns a list of all attributes pertaining to relationships between GO terms.
sub go_relationship_attributes {
    my ($self) = @_;

    my @potential_relationship_attributes = ('instanceOf', 'hasPart', 'regulate', 'positivelyRegulate', 'negativelyRegulate');
    my @relationship_attributes = ();
    foreach my $potential_relationship_attribute (@potential_relationship_attributes) {
        if ($self->is_valid_attribute($potential_relationship_attribute)) {
            push(@relationship_attributes, $potential_relationship_attribute);
        }
    }

    return @relationship_attributes;
}

sub go_relationship_abbreviations_hash {
    my ($self) = @_;

    my @relationship_attributes = $self->go_relationship_attributes();
    my %relationship_abbreviations_hash = ();
    foreach my $relationship_attribute (@relationship_attributes) {
        $relationship_attribute =~ /^(.)/;
        my $relationship_abbreviation = $1;
        if (defined $relationship_abbreviation) {
            $relationship_abbreviation = uc($relationship_abbreviation);
        } else {
            $relationship_abbreviation = 'X';
        }
        $relationship_abbreviations_hash{$relationship_attribute} = $relationship_abbreviation;
    }

    return %relationship_abbreviations_hash;
}

sub go_relationship_reverse_attribute_values {
    my ($self) = @_;

    my @relationship_attributes = $self->go_relationship_attributes();
    my @relationship_reverse_attribute_values = ();
    foreach my $relationship_attribute (@relationship_attributes) {
        my $reverse_attribute_values = $self->reverse_attribute_value($relationship_attribute);
        if (defined $reverse_attribute_values && scalar(@{$self->reverse_attribute_value($relationship_attribute)}) > 0) {
            push(@relationship_reverse_attribute_values, @{$self->reverse_attribute_value($relationship_attribute)});
        }
    }

    return @relationship_reverse_attribute_values;
}


package GKB::PrettyInstance::InstanceEdit;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::PrettyInstance);

sub hyperlinked_displayName {
    my $self = shift;
    # MySQL 4.1 returns timestamp fields in format 'yyyy-mm-dd hh:mm:ss' while 4.0 uses 'yyyymmddhhmmss'
    (my $date = $self->DateTime->[0]) =~ s/^(\d{4})-?(\d{2})-?(\d{2}).*/$1\-$2\-$3/;
    return join(", ", (map {$self->prettyfy_instance($_)->hyperlinked_displayName} @{$self->Author}))
	. (($date ne '0000-00-00') ? ", $date" : '');
}

package GKB::PrettyInstance::URL;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance::LiteratureReference);


package GKB::PrettyInstance::Book;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance::LiteratureReference);

sub html_table_rows {
    my $self = shift;
    return
	$self->attribute_value_as_1row('Chapter title', ['chapterTitle']) .
	$self->attribute_value_as_1row('Chapter author(s)',['chapterAuthors']) .
	$self->attribute_value_as_1row('Book title', ['title']) .
	$self->attribute_value_as_1row('Book author(s)',['author']) .
	$self->attribute_value_as_1row('ISBN', ['ISBN']) .
	$self->attribute_value_as_1row('Publication year', ['year']) .
	$self->attribute_value_as_1row('Journal',['journal']) .
	$self->attribute_value_as_1row('Volume',['volume']) .
	$self->attribute_value_as_1row('Pages',['pages']) .
	$self->attribute_value_as_1row('Publisher',['publisher']) .
	($self->PubMedIdentifier->[0]
	 ? $self->prepared_attribute_value_as_1row('PMID', 'pubMedIdentifier', [$self->hyperlinked_pubMedIdentifier], 'The PubMed Identifier link takes you to the NCBI PubMed site to this reference')
	 : '') .
        $self->make_attribute_tree_as_2rows(-TITLE =>'Is a reference for',
                                            -ATTRIBUTES => ['literatureReference'],
					    -DEPTH => 1,
                                            -REVERSE => 1);
}

sub hyperlinked_displayName {
    my ($self) = @_;
    my $text = '';
    my $chapter_authors = '';
    if ($self->is_valid_attribute('chapterAuthors') && defined $self->chapterAuthors && defined $self->chapterAuthors->[0]) {
    	$chapter_authors = join(", ", map {$self->prettyfy_instance($_)->hyperlinked_displayName} @{$self->chapterAuthors});
    }
    my $chapter_title = '';
    if ($self->is_valid_attribute('chapterTitle') && defined $self->chapterTitle && defined $self->chapterTitle->[0]) {
    	$chapter_title = qq( <I>) . $self->hyperlinked_string('"' . $self->chapterTitle->[0] . '"') . qq(</I>);
    }
    my $chapter_details = '';
    if (!($chapter_authors eq '')) {
    	$chapter_details = $chapter_authors . ' ' . $chapter_title;
    }
    if (!($chapter_details eq "")) {
    	$chapter_details .= ' in ';
    }

    my $title = '';
    if ($self->is_valid_attribute('title') && defined $self->title && defined $self->title->[0]) {
    	$title = qq( <I>) . $self->hyperlinked_string($self->title->[0]) . qq(</I>);
    }
    my $authors = '';
    if ($self->is_valid_attribute('author') && defined $self->author && defined $self->author->[0]) {
    	if (!($chapter_details eq "")) {
    		$authors .= 'Ed. ';
    	}
    	$authors .= join(", ", map {$self->prettyfy_instance($_)->hyperlinked_displayName} @{$self->author});
    }
    my $pages = '';
    if ($self->is_valid_attribute('pages') && defined $self->pages && defined $self->pages->[0]) {
    	$pages = $self->pages->[0];
    }
    my $publisher = '';
    if ($self->is_valid_attribute('publisher') && defined $self->publisher && defined $self->publisher->[0]) {
    	$publisher = $self->publisher->[0]->name->[0];
    }
    my $year = '';
    if ($self->is_valid_attribute('year') && defined $self->year && defined $self->year->[0]) {
    	$year = $self->year->[0];
    }
    my $isbn = '';
    if ($self->is_valid_attribute('ISBN') && defined $self->ISBN && defined $self->ISBN->[0]) {
    	$isbn = $self->ISBN->[0];
    }
    my $book_details = $title . ' ' . $authors . ' ' . $pages . ' ' . $publisher . ' ' . $year . ' ' . $isbn ;

    return $chapter_details . $book_details;
}


package GKB::PrettyInstance::LiteratureReference;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance);

sub html_table_rows {
    my $self = shift;
    return
	$self->attribute_value_as_1row('Title', ['title']) .
	$self->attribute_value_as_1row('Publication year', ['year']) .
	$self->attribute_value_as_1row('Journal',['journal']) .
	$self->attribute_value_as_1row('Volume',['volume']) .
	$self->attribute_value_as_1row('Pages',['pages']) .
	$self->attribute_value_as_1row('Author(s)',['author']) .
	($self->PubMedIdentifier->[0]
	 ? $self->prepared_attribute_value_as_1row('PMID', 'pubMedIdentifier', [$self->hyperlinked_pubMedIdentifier], 'The PubMed Identifier link takes you to the NCBI PubMed site to this reference')
	 : '') .
        $self->make_attribute_tree_as_2rows(-TITLE =>'Is a reference for',
                                            -ATTRIBUTES => ['literatureReference'],
					    -DEPTH => 1,
                                            -REVERSE => 1);
}

sub hyperlinked_pubMedIdentifier {
    my ($self) = @_;
    if (my $pmid = $self->PubMedIdentifier->[0]) {
	#return qq(<A HREF=\") . $self->pubMed_URL . qq(\">$pmid</A>);
	return qq(<A HREF=") . $self->pubMed_URL . qq(" CLASS="PubMed" onMouseover="ddrivetip('Go to PubMed:$pmid','#DCDCDC', 250)" onMouseout="hideddrivetip()">PubMed</A>);
    }
    return $self->SUPER::hyperlinked_displayName;
}

sub pubMed_URL {
    my ($self) = @_;
    if (my $pmid = $self->PubMedIdentifier->[0]) {
	return "http://www.ncbi.nlm.nih.gov:80/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=$pmid";
    }
    return;
}

sub hyperlinked_displayName {
    my ($self) = @_;
    return
	join(", ", map {$self->prettyfy_instance($_)->hyperlinked_displayName} @{$self->Author}) .
	qq( <I>) . $self->hyperlinked_string($self->Title->[0]) . qq(</I> <STRONG>) .
	$self->Year->[0] . qq(</STRONG> ) . $self->Journal->[0] . ' ' .
	$self->hyperlinked_pubMedIdentifier;
}

sub author_year {
    my ($self) = @_;
    my $out;
    eval {
		if ($self->Author->[0]) {
	    	my $year = $self->Year->[0];
	    	if (!(defined $year)) {
	    		$year = "";
	    	}
		    if (@{$self->Author} == 1) {
				$out = $self->Author->[0]->Surname->[0] . " " . $year;
		    } elsif (@{$self->Author} == 2) {
		    	my $surname0 = $self->Author->[0]->Surname->[0];
		    	if (!(defined $surname0)) {
		    		$surname0 = "";
		    	}
		    	my $surname1 = $self->Author->[1]->Surname->[0];
		    	if (!(defined $surname1)) {
		    		$surname1 = "";
		    	}
				$out = $surname0 . " & " . $surname1 . " " . $year;
		    } else {
				$out = $self->Author->[0]->Surname->[0] . " <I>et al</I> " . $year;
		    }
		    if ($self->PubMedIdentifier->[0]) {
				$out = qq(<A HREF=\") . $self->pubMed_URL . qq(\">$out</A>);
		    } else {
				$out = $self->hyperlinked_string($out);
		    }
		}
    };
    if ($@ || ! $out) {
		return $self->SUPER::hyperlinked_displayName;
    } else {
		return $out;
    }
}

sub soft_displayName {
    my $self = shift;
    return $self->hyperlinked_string('[Reference] '.($self->displayName || ''));
}


package GKB::PrettyInstance::AbstractModifiedResidue;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance);



package GKB::PrettyInstance::ModifiedResidue;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance::AbstractModifiedResidue);

sub html_table_rows {
    my $self = shift;
    return
	$self->reverse_attribute_value_as_1row('Molecule', ['hasModifiedResidue'], 'The entity which contains the modification') .
	$self->attribute_value_as_1row('PSI-MOD',['psiMod'], 'The PSI-MOD term for this modification') .
	($self->is_valid_attribute('residue') ? $self->attribute_value_as_1row('Modified residue',['residue']) : '') .
	$self->attribute_value_as_1row('Modification',['modification'], 'The modifying entity which causes the modification') .
        $self->attribute_value_as_1row('Coordinate of modified residue',['coordinate'], 'The position at which the molecule is modified');
}

=head  #use default displayName for now
sub displayName {
    my ($self) = @_;
    $self->debug && print join("\t", (caller(0))[3], $self,  $self->class, ($self->db_id || '')), "\n";
    my $displayName;
    $displayName = (($self->Modification->[0]) ? $self->Modification->[0]->displayName : 'unknown modification') . ' on ';
    $displayName .= 'unknown ' unless ($self->Coordinate->[0]);
    $displayName .= (($self->Residue->[0]) ? $self->Residue->[0]->displayName : 'residue') . ' ';
    $displayName .= $self->Coordinate->[0] if ($self->Coordinate->[0]);
    return $displayName;
}
=cut

sub soft_displayName {
    my $self = shift;
    return $self->hyperlinked_string('[Modified residue] '.($self->displayName || ''));
}


package GKB::PrettyInstance::NegativeRegulation;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance::Regulation);

sub html_table_rows {
    my ($self) = @_;
    return
	$self->_stable_identifier() .
	$self->attribute_value_as_1row('Authored', ['authored']) .
	$self->attribute_value_as_1row('Reviewed', ['reviewed']) .
	($self->is_valid_attribute('revised') ? $self->attribute_value_as_1row('Revised', ['revised']) : '') .
	$self->attribute_value_as_1row('Regulated entity',['regulatedEntity'], 'The event\/entity which is being regulated') .
	$self->attribute_value_as_1row('Negative regulator',['regulator'], 'The entity which is causing the negative regulation') .
	$self->attribute_value_as_1row('Regulation type',['regulationType'], 'The actual type of regulation occuring') .
	$self->prepared_attribute_value_as_1row(undef,'summation',[map {$self->prettyfy_instance($_)->html_text} @{$self->Summation}]) .
	$self->literaturereferences_as_2rows;
}


package GKB::PrettyInstance::Pathway;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance::Event);

sub soft_displayName {
    my $self = shift;
    return $self->hyperlinked_string('[Pathway] '.($self->displayName || ''));
}


package GKB::PrettyInstance::Person;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance);

sub html_table_rows {
    my ($self) = @_;
    return
	$self->attribute_value_as_1row('Surname',['surname']) .
	$self->attribute_value_as_1row('First name',['firstname']) .
	$self->attribute_value_as_1row('Initials',['initial']) .
	$self->attribute_value_as_1row('Affiliation',['affiliation']) .
	$self->recursive_value_list_as_2rows(-ATTRIBUTES => [qw(author created)],
					     -CLASSES => [qw(Summation InstanceEdit Person)],
					     -REVERSE => 1,
					     -OUT_CLASSES => ['Summation'],
					     -DELIMITER => '<BR />',
					     -TITLE => 'Author of summation(s)') .
	$self->recursive_value_list_as_2rows(-ATTRIBUTES => [qw(author created)],
					     -CLASSES => [$self->ontology->root_class, qw(InstanceEdit Person)],
					     -REVERSE => 1,
					     -OUT_CLASSES => [qw(Event PhysicalEntity DatabaseIdentifier Activity CatalystActivity AbstractModifiedResidue Modification Regulation RegulationType Taxon Figure)],
					     -DELIMITER => '<BR />',
					     -TITLE => 'Author of entries') .
	$self->recursive_value_list_as_2rows(-ATTRIBUTES => ['reviewer'],
					     -CLASSES => ['Summation'],
					     -REVERSE => 1,
					     -DELIMITER => '<BR />',
					     -TITLE => 'Reviewer of summation(s)') .
	$self->recursive_value_list_as_2rows(-ATTRIBUTES => ['editor'],
					     -CLASSES => ['Summation'],
					     -REVERSE => 1,
					     -DELIMITER => '<BR />',
					     -TITLE => 'Editor of summation(s)') .
	$self->recursive_value_list_as_2rows(-ATTRIBUTES => ['author'],
					     -CLASSES => ['LiteratureReference'],
					     -REVERSE => 1,
					     -DELIMITER => '<BR />',
					     -TITLE => 'Publication(s)') .
	'';
}

sub displayName {
    my $self = shift;
    if (@_) {
	return $self->SUPER::displayName(@_);
    }
    return $self->Surname->[0] . ', ' . $self->Initial->[0];
}


package GKB::PrettyInstance::Drug;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance::PhysicalEntity);


package GKB::PrettyInstance::ChemicalDrug;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance::Drug);


package GKB::PrettyInstance::ProteinDrug;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance::Drug);


package GKB::PrettyInstance::RNADrug;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance::Drug);


package GKB::PrettyInstance::EntitySet;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance::PhysicalEntity);


package GKB::PrettyInstance::OpenSet;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance::EntitySet);


package GKB::PrettyInstance::DefinedSet;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance::EntitySet);


package GKB::PrettyInstance::CandidateSet;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance::EntitySet);


package GKB::PrettyInstance::OtherEntity;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance::PhysicalEntity);


package GKB::PrettyInstance::PhysicalEntity;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance);

sub create_image {
    my ($self,$format) = @_;
     my $start = time();
#    $self->dba->debug(1); print qq(<PRE>\n);
    my (%i, %o, %c); #flags for coloring

#entities are followed 'upwards' as well, i.e. complexes that they are part of are included
    my $reactions1 = $self->follow_class_attributes
	(-INSTRUCTIONS =>
	 {'PhysicalEntity' => {'reverse_attributes' => [qw(hasComponent input hasMember repeatedUnit)]}},
	 -OUT_CLASSES => ['Reaction']
	 );

    my $reactions2 = $self->follow_class_attributes
	(-INSTRUCTIONS =>
	 {'PhysicalEntity' => {'reverse_attributes' => [qw(hasComponent output hasMember repeatedUnit)]}},
	 -OUT_CLASSES => ['Reaction']
	 );

    my $reactions3 = $self->follow_class_attributes
	(-INSTRUCTIONS =>
	 {'PhysicalEntity' => {'reverse_attributes' => [qw(hasComponent physicalEntity hasMember repeatedUnit)]},
	  'CatalystActivity' => {'reverse_attributes' => [qw(catalystActivity)]}},
	 -OUT_CLASSES => ['Reaction']
	 );

    $reactions1 = $self->grep_for_instances_with_focus_species_or_default($reactions1);
    $reactions2 = $self->grep_for_instances_with_focus_species_or_default($reactions2);
    $reactions3 = $self->grep_for_instances_with_focus_species_or_default($reactions3);

    map {$i{$_}++} @{$reactions1};
    map {$o{$_}++} @{$reactions2};
    map {$c{$_}++} @{$reactions3};

    my %seen;
    my (@all, @io, @ic, @oc, @i, @o, @c); #combinations of input, output, catalysts
    foreach (@{$reactions1}, @{$reactions2}, @{$reactions3})  {
	next if $seen{$_}++;
	$i{$_} && $o{$_} && $c{$_} && (push @all, $_) && next;
	$i{$_} && $o{$_} && (push @io, $_) && next;
	$i{$_} && $c{$_} && (push @ic, $_) && next;
	$o{$_} && $c{$_} && (push @oc, $_) && next;
	$i{$_} && (push @i, $_) && next;
	$o{$_} && (push @o, $_) && next;
	$c{$_} && (push @c, $_) && next;
    }

    my $rm = new GKB::ReactionMap(-DBA => $self->dba,-CGI => $self->cgi,-NO_DEFAULT_IMAGE => 1,-ORIGIN_INSTANCE=>$self,-FORMAT=>$format);
    $rm->set_reaction_color(128,0,0,\@i);
    $rm->set_reaction_color(0,128,0,\@o);
    $rm->set_reaction_color(0,0,128,\@c);
    $rm->set_reaction_color(0,0,0,\@all);
    $rm->set_reaction_color(255,128,0,\@io);
    $rm->set_reaction_color(153,0,153,\@ic);
    $rm->set_reaction_color(0,153,153,\@oc);
#    print qq(</PRE>\n); $self->dba->debug(undef);
    $rm->create_image_for_species($self->focus_species_or_default);
#    print qq(<PRE>), (caller(0))[3],"\t",  time() - $start, qq(</PRE>\n);
    return $rm;
}

sub html_table {
    my ($self) = @_;
    $self->debug && print join("\t", (caller(0))[3], $self,  $self->class, ($self->db_id || '')), "\n";
    return
	$self->page_title
	. $self->reactionmap_html
	. qq(<DIV CLASS="section">)
	. qq(<TABLE cellspacing="0" WIDTH="$HTML_PAGE_WIDTH" BORDER="0" CLASS=") . $self->class . qq(">\n)
	. $self->html_table_rows_if_necessary
	. $self->_view_switch_html
	. qq(</TABLE>\n)
	. qq(</DIV>\n)
	;
}

sub html_table_rows {
    my ($self) = @_;
    return
        $self->attribute_value_as_1row('Name',['name']) .
	$self->_stable_identifier() .
	$self->attribute_value_as_1row('Authored', ['authored']) .
	$self->attribute_value_as_1row('Reviewed', ['reviewed']) .
	($self->is_valid_attribute('revised') ? $self->attribute_value_as_1row('Revised', ['revised']) : '') .
	($self->is_a('EntityWithAccessionedSequence')
	 ? $self->links_2_external_sequence_records .
           $self->html_participating_molecules .
	   $self->other_identifiers .
	   $self->attribute_value_as_1row('Reference entity',['referenceEntity'], '') .
	   $self->fragmentCoordinates .
	   $self->modifications_as_1row
	 : ($self->is_valid_attribute('referenceEntity')
	    ? $self->attribute_value_as_1row('Reference entity',['referenceEntity'], '') .
	      $self->links_2_external_referenceEntity_records
	    : ''
	 )
	) .
	$self->attribute_value_as_1row('Cellular compartment',['compartment'], 'The compartment in the cell where the entity is localized') .
	$self->attribute_value_as_1row('Cell type',['cellType'], 'The type of the cell in which this instance resides') .
	($self->is_valid_attribute('species')
	 ? $self->attribute_value_as_1row('Organism',['species'])
	 : '') .
        ($self->is_a('EntitySet') && $self->attribute_value_as_1row('Represents entities',['hasMember'])) .
        ($self->is_a('CandidateSet') && $self->attribute_value_as_1row('<B>May</B> represent entities',['hasCandidate'])) .
        $self->reverse_attribute_value_as_1row('Is represented by generalisation(s)',['hasMember']) .
        $self->reverse_attribute_value_as_1row('<B>May</B> be represented by generalisation(s)', ['hasCandidate']) .
	(($self->is_a('Complex') || $self->is_a('Polymer') || $self->is_a('EntitySet'))
	 ? $self->entityHierarchy_as_1_row
	 : ''
	) .
	($self->is_valid_attribute('entityOnOtherCell') ? $self->attribute_value_as_1row('Entity on other cell',['entityOnOtherCell'], 'Entity on other cell') : '') .
	$self->complexes_and_polymers .
	$self->as_1row('Biochemical activities',
		       'physicalEntityRev',
		       {-INSTRUCTIONS => {
			   'PhysicalEntity' => {'reverse_attributes' => ['physicalEntity']},
			   'CatalystActivity' => {'attributes' => ['activity']}},
			-OUT_CLASSES => ['GO_MolecularFunction']
			}, 'The specific activities the entity possesses') .
	$self->participant_in_processes .
	$self->collapsed_Events_as_1_row('Catalyses events',
					 'physicalEntityRev',
					 {-INSTRUCTIONS => {
					     'PhysicalEntity' => {'reverse_attributes' => [qw(physicalEntity hasMember hasCandidate repeatedUnit)]},
					     'CatalystActivity' => {'reverse_attributes' => ['catalystActivity']},
					     #'EntitySet' => {'attributes' => [qw(hasMember)]},
					     #'CandidateSet' => {'attributes' => [qw(hasCandidate)]}
					  }, -OUT_CLASSES => ['Event']}, 'The events catalysed by the named entity'
					 ) .
	$self->collapsed_Events_as_1_row('Produced by events',
					 'outputRev',
					 {-INSTRUCTIONS => {
					     'PhysicalEntity' => {'reverse_attributes' => [qw(output hasMember hasCandidate repeatedUnit)]},
					     #'EntitySet' => {'attributes' => [qw(hasMember)]},
					     #'CandidateSet' => {'attributes' => [qw(hasCandidate)]}
					  }, -OUT_CLASSES => ['Event']}, 'The named entity is an output for these events'
					 ) .
	$self->collapsed_Events_as_1_row('Consumed by events',
					 'inputRev',
					 {-INSTRUCTIONS => {
					     'PhysicalEntity' => {'reverse_attributes' => [qw(input hasMember hasCandidate repeatedUnit)]},
					     #'EntitySet' => {'attributes' => [qw(hasMember)]},
					     #'CandidateSet' => {'attributes' => [qw(hasCandidate)]}
					  }, -OUT_CLASSES => ['Event']}, 'The named entity is an input for these events'
					 ) .
	$self->other_PhysicalEntities_with_same_referenceEntity .
	$self->literaturereferences_as_2rows .
	($self->InferredFrom->[0]
	 ? $self->values_as_1row('This entity is deduced on the basis of','inferredFrom',
				 [map {$self->prettyfy_instance($_); $_->hyperlinked_displayName_w_species} @{$self->InferredFrom}],
				 '<BR />')
	 : $self->values_as_1row('Entities deduced on the basis of this entity','inferredFromRev',
				 [map {$self->prettyfy_instance($_); $_->hyperlinked_displayName_w_species} @{$self->reverse_attribute_value('inferredFrom')}],
				 '<BR />')
	) .
	($self->is_valid_attribute('interactionIdentifier')
	 ? $self->attribute_value_as_1row('Interactions in this complex', ['interactionIdentifier'], undef, ', ')
	 : ''
	) .
        '';
}

sub few_details {
    my ($self) = @_;
    return
        $self->attribute_value_as_1row('Name',['name']) .
	$self->_stable_identifier() .
	$self->attribute_value_as_1row('Authored', ['authored']) .
	$self->attribute_value_as_1row('Reviewed', ['reviewed']) .
	($self->is_valid_attribute('revised') ? $self->attribute_value_as_1row('Revised', ['revised']) : '') .
	($self->is_a('EntityWithAccessionedSequence')
	 ? $self->links_2_external_sequence_records .
           $self->html_participating_molecules .
	   $self->other_identifiers .
	   $self->attribute_value_as_1row('Reference entity',['referenceEntity'], '') .
	   $self->fragmentCoordinates .
	   $self->modifications_as_1row
	 : ($self->is_valid_attribute('referenceEntity')
	    ? $self->attribute_value_as_1row('Reference entity',['referenceEntity'], '') .
	      $self->links_2_external_referenceEntity_records
	    : ''
	 )
	) .
	$self->attribute_value_as_1row_swap_pathwaybrowserdata_for_eventbrowser('Cellular compartment',['compartment'], 'The compartment in the cell where the entity is localized') .
#	$self->attribute_value_as_1row('Cellular compartment',['compartment'], 'The compartment in the cell where the entity is localized') .
	$self->attribute_value_as_1row('Cell type',['cellType'], 'The type of the cell in which this instance resides') .
	($self->is_valid_attribute('species')
	 ? $self->attribute_value_as_1row_swap_pathwaybrowserdata_for_eventbrowser('Organism',['species'])
#	 ? $self->attribute_value_as_1row('Organism',['species'])
	 : '') .
        ($self->is_a('EntitySet') && $self->attribute_value_as_1row('Represents entities',['hasMember'])) .
        ($self->is_a('CandidateSet') && $self->attribute_value_as_1row('<B>May</B> represent entities',['hasCandidate'])) .
    # David Croft: attempt to fix problems that Peter encountered with EWAS in
    # clostridium, where the "Other forms of this molecule"
    # links were inactive (ID=181473, if you want to try it).
    # Substitute pathwaybrowserdata with eventbrowser.
        $self->reverse_attribute_value_as_1row_swap_pathwaybrowserdata_for_eventbrowser('Is represented by generalisation(s)',['hasMember']) .
#        $self->reverse_attribute_value_as_1row('Is represented by generalisation(s)',['hasMember']) .
        $self->reverse_attribute_value_as_1row('<B>May</B> be represented by generalisation(s)', ['hasCandidate']) .
	(($self->is_a('Complex') || $self->is_a('Polymer') || $self->is_a('EntitySet'))
	 ? $self->entityHierarchy_as_1_row
	 : ''
	) .
	($self->is_valid_attribute('disease') ? $self->attribute_value_as_1row('Disease involvement',['disease'], 'Disease(s) in which this entity plays a role') : '') .
	$self->complexes_and_polymers .
	$self->as_1row('Biochemical activities',
		       'physicalEntityRev',
		       {-INSTRUCTIONS => {
			   'PhysicalEntity' => {'reverse_attributes' => ['physicalEntity']},
			   'CatalystActivity' => {'attributes' => ['activity']}},
			-OUT_CLASSES => ['GO_MolecularFunction']
			}, 'The specific activities the entity possesses') .
	$self->collapsed_Events_as_1_row('Catalyses events',
					 'physicalEntityRev',
					 {-INSTRUCTIONS => {
					     'PhysicalEntity' => {'reverse_attributes' => [qw(physicalEntity hasMember hasCandidate repeatedUnit)]},
					     'CatalystActivity' => {'reverse_attributes' => ['catalystActivity']},
					     #'EntitySet' => {'attributes' => [qw(hasMember)]},
					     #'CandidateSet' => {'attributes' => [qw(hasCandidate)]}
					  }, -OUT_CLASSES => ['Event']}, 'The events catalysed by the named entity'
					 ) .
	$self->collapsed_Events_as_1_row('Produced by events',
					 'outputRev',
					 {-INSTRUCTIONS => {
					     'PhysicalEntity' => {'reverse_attributes' => [qw(output hasMember hasCandidate repeatedUnit)]},
					     #'EntitySet' => {'attributes' => [qw(hasMember)]},
					     #'CandidateSet' => {'attributes' => [qw(hasCandidate)]}
					  }, -OUT_CLASSES => ['Event']}, 'The named entity is an output for these events'
					 ) .
	$self->collapsed_Events_as_1_row('Consumed by events',
					 'inputRev',
					 {-INSTRUCTIONS => {
					     'PhysicalEntity' => {'reverse_attributes' => [qw(input hasMember hasCandidate repeatedUnit)]},
					     #'EntitySet' => {'attributes' => [qw(hasMember)]},
					     #'CandidateSet' => {'attributes' => [qw(hasCandidate)]}
					  }, -OUT_CLASSES => ['Event']}, 'The named entity is an input for these events'
					 ) .
	$self->other_PhysicalEntities_with_same_referenceEntity .
	$self->literaturereferences_as_2rows .
	($self->InferredFrom->[0]
	 ? $self->values_as_1row('This entity is deduced on the basis of','inferredFrom',
				 [map {$self->prettyfy_instance($_); $_->hyperlinked_displayName_w_species} @{$self->InferredFrom}],
				 '<BR />')
	 : $self->values_as_1row('Entities deduced on the basis of this entity','inferredFromRev',
				 [map {$self->prettyfy_instance($_); $_->hyperlinked_displayName_w_species} @{$self->reverse_attribute_value('inferredFrom')}],
				 '<BR />')
	) .
	($self->is_valid_attribute('interactionIdentifier')
	 ? $self->attribute_value_as_1row('Interactions in this complex', ['interactionIdentifier'], undef, ', ')
	 : ''
	) .
        '';
}

sub geneIdentifiers {
    my $self = shift;
    return '' unless ($self->is_a("AccessionedEntity"));
    return '' unless ($self->DatabaseIdentifier->[0]);
    my $gis = $self->DatabaseIdentifier->[0]->GeneIdentifier;
    return '' unless (@{$gis});
    return $self->prepared_attribute_value_as_1row
	('Link(s) to genes',
	 'geneIdentifier',
	 [map {$self->prettyfy_instance($_)->hyperlinked_identifier} @{$gis}], 'The link to the external database gene identifier'
	 );
}

sub components_as_2rows {
    my ($self) = @_;
    (($self->is_a('Complex') && $self->HasComponent->[0]) || ($self->is_a('Polymer') && $self->RepeatedUnit->[0])) || return '';
    return
	qq(<TR><TH COLSPAN="2" CLASS="hasComponent"><A NAME="hasComponentAnchor" HREF="javascript:void(0)" onMouseover="ddrivetip('The entities which comprise the named entity','#DCDCDC', 250)" onMouseout="hideddrivetip()">Has subunits</TH></TR>\n<TR><TD COLSPAN="2" CLASS="hasComponent"><UL CLASS="attributes">) .

	$ {$self->_component_tree_wo_duplicates} .
	qq(</UL></TD></TR>\n);
}

sub _component_tree_wo_duplicates {
    my ($self) = @_;
    my $out;
    my (%count,%seen,@components);
    if ($self->is_a('Complex')) {
	foreach (@{$self->HasComponent}) {
	    unless($count{$_->db_id}++) {
		push @components,$_;
	    }
	}
    } elsif ($self->is_a('Polymer')) {
	foreach (@{$self->RepeatedUnit}) {
	    unless($count{$_->db_id}) {
		push @components,$_;
		if (defined $self->MinUnitCount->[0] || defined $self->MaxUnitCount->[0]) {
		    $count{$_->db_id} = $self->MinUnitCount->[0] .
			($self->MaxUnitCount->[0] ? '..' . $self->MaxUnitCount->[0] : '+ ');
		} else {
		    $count{$_->db_id} = 1;
		}
	    }
	}
    }
    foreach my $i (@components) {
	$self->prettyfy_instance($i);
	$out .= qq(<LI CLASS="attributes">) . ($count{$i->db_id} > 1 ? $count{$i->db_id} . "x " : '') . $i->hyperlinked_displayName . qq(</LI>);
	if ($i->is_a('Complex') || $i->is_a('Polymer')) {
	    $out .= qq(<UL CLASS="attributes">) . $ {$i->_component_tree_wo_duplicates} . qq(</UL>);
	}
    }
    return \$out;
}

sub modifications_as_1row {
    my $self = shift;
    return '' unless ($self->is_valid_attribute('hasModifiedResidue'));
    return '' unless ($self->HasModifiedResidue->[0]);
    if ($self->DatabaseIdentifier->[0]->ReferenceDatabase->[0]->displayName =~ /(SWALL|SPTR|PROT)/i) {
	return $self->attribute_value_as_1row('Post-translational modification(s)',['hasModifiedResidue']);
    } else {
	return $self->attribute_value_as_1row('Modification(s)',['hasModifiedResidue']);
    }
}

sub soft_displayName {
    my $self = shift;
    return $self->hyperlinked_string('[Molecular entity] '.($self->displayName || ''));
}

sub other_PhysicalEntities_with_same_referenceEntity {
    my $self = shift;
    return '' unless ($self->is_valid_attribute('referenceEntity'));
    return '' unless ($self->ReferenceEntity->[0]);
    # HACK to catch cases where invalid values have been assigned (by converter probably)
    return '' unless ($self->ReferenceEntity->[0]->is_valid_reverse_attribute('referenceEntity'));
    my @others = grep {! $_->is_a('Domain')} grep {$_ != $self} @{$self->ReferenceEntity->[0]->reverse_attribute_value('referenceEntity')};

    return '' unless (@others);
#    print STDERR "other_PhysicalEntities_with_same_referenceEntity: others=@others\n";
    my @prettified_others = map {$self->prettyfy_instance($_)->SUPER::hyperlinked_displayName} @others;
#    print STDERR "other_PhysicalEntities_with_same_referenceEntity: prettified_others=@prettified_others\n";
    my @this_prettified_others = map {$self->prettyfy_instance($_)->hyperlinked_displayName} @others;
#    print STDERR "other_PhysicalEntities_with_same_referenceEntity: this_prettified_others=@this_prettified_others\n";
    # David Croft: attempt to fix problems that Peter encountered with EWAS in
    # clostridium, where the "Other forms of this molecule"
    # links were inactive (ID=181473, if you want to try it).
    # Substitute pathwaybrowserdata with eventbrowser.
    my @eventbrowser_prettified_others = map {$self->swap_pathwaybrowserdata_for_eventbrowser($_)} @this_prettified_others;
    my $prepared_attribute_value_as_1row_value = $self->prepared_attribute_value_as_1row
	('Other forms of this molecule',
	 'databaseIdentifierRev',
	 # Don't want to get [DatabaseIdentifier] appended and hence the use of SUPER
	 [@eventbrowser_prettified_others], 'forms'
#	 [map {$self->prettyfy_instance($_)->SUPER::hyperlinked_displayName} @others], 'forms'
	 );
#    print STDERR "other_PhysicalEntities_with_same_referenceEntity: prepared_attribute_value_as_1row_value=$prepared_attribute_value_as_1row_value\n";
    return $prepared_attribute_value_as_1row_value;
}

sub hyperlinked_displayName_w_species {
    my ($self) = @_;

    if ($self->is_valid_attribute('species') && $self->Species->[0]) {
#		return $self->hyperlinked_string($self->displayName . ' [' . $self->Species->[0]->displayName . ']');
		my $species_names = "";
		my $species_counter = 0;
		my $species;
		foreach $species (@{$self->Species}) {
			if (!($species_names eq "")) {
				$species_names .= ", ";
			}
			if ($species_counter>1) {
				$species_names .= "etc.";
				last;
			}
			$species_names .= $species->_displayName->[0];
			$species_counter++;
		}
		return $self->hyperlinked_displayName . ' [' . $species_names . ']';
    }
    return $self->hyperlinked_displayName;
}

sub hyperlinked_displayName {
    my ($self, $is_on_other_cell) = @_;
    my $out;
    my $display_name = $self->displayName;
    if ($self->Compartment->[0]) {
	if ($is_on_other_cell) {
	    $display_name = $self->Name->[0] . ' [other cell ' . $self->Compartment->[0]->displayName . ']';
	}
    } else {
	$display_name .= ' [compartment not specified]';
    }
    my $cell_type = $self->CellType->[0];
    if ($cell_type) {
        my $cell_type_name = $cell_type->name->[0];
        $display_name =~ s/\[/[$cell_type_name, /;
    }
    $out = $self->hyperlinked_string($display_name);
    if ($self->is_valid_attribute('referenceEntity')) {
	if (my $re = $self->ReferenceEntity->[0]) {
	    my @a = ($re);
	    if ($re->is_valid_attribute('referenceGene')) {
		push @a, @{$re->ReferenceGene};
	    }
	    if ($re->is_valid_attribute('referenceTranscript')) {
		push @a, @{$re->ReferenceTranscript};
		map {push @a, @{$_->CrossReference}} @{$re->ReferenceTranscript};
	    }
	    if ($re->is_valid_attribute('crossReference')) {
		push @a, @{$re->CrossReference};
	    }
	    $out .= ' ' . join('',grep {$_}
			       map {$self->prettyfy_instance($_)->hyperlinked_abbreviation} @a);
	}
    }
    return $out;
}

sub links_2_external_referenceEntity_records {
    my $self = shift;
    my $out = '';
    if ($self->is_valid_attribute('referenceEntity')) {
	if (my $re = $self->ReferenceEntity->[0]) {
	    my @tmp = ($self->prettyfy_instance($re));
	    if ($re->is_valid_attribute('crossReference')) {
		push @tmp, map {$self->prettyfy_instance($_)} @{$re->CrossReference};
	    }
	    $out .= $self->prepared_attribute_value_as_1row('Links to compound in external databases','identifier', [map {$_->hyperlinked_identifier} @tmp]);
	}
    }
    return $out;
}

sub html_participating_molecules {
    my $self = shift;
    $DB::single=1;
    my $instances = $self->webutils->handle_query_form;
    #my $pip = GKB::PrettyInstance::Pathway->new(-INSTANCE => $instances->[0], -SUBCLASSIFY=> 1, -WEBUTILS=>$self->webutils);
    #print {*STDERR} "\n\n" . $instances->_participating_molecules . "\n\n";
    return "<tr><th>CLASS</th><td>".$instances->[0]->class()."</td></tr>";
    #return $pip->_participating_molecules;
}


sub participant_in_processes {
    my $self = shift;
    # Moved the attribute following logic to HtmlTreeMaker::participant_in_event_tree
    return $self->participant_in_event_tree_as_2rows(
	-PHYSICALENTITIES => [$self],
	-TITLE => 'Participates in processes'
	);
}

sub _make_switch_html {
    my $self = shift;
    my $tmp = $self->SUPER::_make_switch_html();
    $tmp || return '';
    return $tmp;
}


package GKB::PrettyInstance::PositiveRegulation;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance::Regulation);

sub html_table_rows {
    my ($self) = @_;
    return
	$self->_stable_identifier() .
	$self->attribute_value_as_1row('Authored', ['authored']) .
	$self->attribute_value_as_1row('Reviewed', ['reviewed']) .
	($self->is_valid_attribute('revised') ? $self->attribute_value_as_1row('Revised', ['revised']) : '') .
	$self->attribute_value_as_1row('Regulated entity',['regulatedEntity'], 'The entity\/event which is regulated') .
	$self->attribute_value_as_1row('Positive regulator',['regulator'], 'The entity which causes a positive regulation of the event\/entity') .
	$self->attribute_value_as_1row('Regulation type',['regulationType']) .
	$self->prepared_attribute_value_as_1row(undef,'summation',[map {$self->prettyfy_instance($_)->html_text} @{$self->Summation}]) .
	$self->literaturereferences_as_2rows;
}


package GKB::PrettyInstance::ReactionlikeEvent;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance::Event);


package GKB::PrettyInstance::BlackBoxEvent;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance::ReactionlikeEvent);


package GKB::PrettyInstance::Depolymerisation;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance::ReactionlikeEvent);


package GKB::PrettyInstance::Polymerisation;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance::ReactionlikeEvent);


package GKB::PrettyInstance::Reaction;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance::ReactionlikeEvent);

sub soft_displayName {
    my $self = shift;
    return $self->hyperlinked_string('[Reaction] '.($self->displayName || ''));
}


package GKB::PrettyInstance::ReferenceDatabase;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance);

sub hyperlinked_displayName {
    my ($self) = @_;
    if (my $url = $self->Url->[0]) {
	return qq(<A HREF="$url">) . $self->displayName . qq(</A>);
    }
    return $self->SUPER::hyperlinked_displayName;
}

sub html_table_rows {
    my ($self) = @_;
    my $url = $self->Url->[0];
    return
	$self->attribute_value_as_1row('Name(s)',['name']) .
	($url ?
	 $self->prepared_attribute_value_as_1row('URL',
						 'url',
						 [qq(<A HREF="$url">$url</A>)]) :
	 ''
	 );
}

sub soft_displayName {
    my $self = shift;
    return $self->hyperlinked_string('[Database] '.($self->displayName || ''));
}

package GKB::PrettyInstance::PsiMod;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance);

sub html_table_rows {
    my ($self) = @_;
    return
        $self->attribute_value_as_1row('Name',['name']) .
        $self->prepared_attribute_value_as_1row('Identifier','identifier', [$self->hyperlinked_identifier]) .
        $self->attribute_value_as_1row('Definition',['definition']) .
	$self->attribute_value_as_1row('Synonym(s)',['synonym']) .
        $self->instances_as_2rows('Molecules with this modification',
                                  'modRev',
                                  $self->get_entities
				  ) .
        $self->make_attribute_tree_as_2rows(-TITLE =>'Is a specialisation of modifications',
                                            -CLASSES => [$self->class],
                                            -ATTRIBUTES => ['instanceOf']) .
        $self->make_attribute_tree_as_2rows(-TITLE =>'Is a generalisation of more specialised modifications',
                                            -ATTRIBUTES => ['instanceOf'],
                                            -DEPTH => 1,
                                            -REVERSE => 1);
}

sub hyperlinked_identifier {
    my ($self) = @_;
    if ($self->is_valid_attribute('identifier') && $self->is_valid_attribute('referenceDatabase')) {
        if (my $refdb = $self->ReferenceDatabase->[0]) {
            if (my $id = $self->Identifier->[0]) {
                if (my $url = $refdb->AccessUrl->[0]) {
                    $url =~ s/###ID###/$id/g;
                    #return qq(<A HREF="$url">$id</A>);
                    return qq(<A HREF="$url">) . $refdb->displayName . ':' . $id . qq(</A>);
                } else {
                    #return $id;
                    return $refdb->displayName . ':' . $id;
                }
            }
        }
    }
    return undef;
}

sub get_descendants {
    my $self = shift;
    my $out;
    unless ($out = $self->get_cached_value('descendants')) {
        $out = $self->follow_class_attributes
	    (-INSTRUCTIONS => {'PsiMod' => {'reverse_attributes' => ['instanceOf']}});
        $self->set_cached_value('descendants',$out);
    }
    return $out;
}

sub get_entities {
    my ($self) = @_;
    my $out;
    unless ($out = $self->get_cached_value('entities')) {
        my $descendants = $self->get_descendants;
        my $focus_species = $self->focus_species;
        if (@{$focus_species}) {
            $out = $self->dba->fetch_instance_by_remote_attribute
                ('EntityWithAccessionedSequence',
                 [['hasModifiedResidue.psiMod','=',[map {$_->db_id} @{$descendants}]],
                  ['species','=',[map {$_->db_id} @{$focus_species}]]]
		 );
        } else {
            $out = $self->dba->fetch_instance_by_remote_attribute
                ('EntityWithAccessionedSequence',
                 [['hasModifiedResidue.psiMod','=',[map {$_->db_id} @{$descendants}]]]
		 );
        }
        $self->set_cached_value('entities',$out);
    }
    return $out;
}

sub soft_displayName {
    my $self = shift;
    return $self->hyperlinked_string('[PsiMod] '.($self->displayName || ''));
}



package GKB::PrettyInstance::Regulation;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance);

sub html_table_rows {
    my ($self) = @_;
    return
	$self->_stable_identifier() .
	$self->attribute_value_as_1row('Authored', ['authored']) .
	$self->attribute_value_as_1row('Reviewed', ['reviewed']) .
	($self->is_valid_attribute('revised') ? $self->attribute_value_as_1row('Revised', ['revised']) : '') .
	$self->attribute_value_as_1row('Regulated entity',['regulatedEntity'], 'The entity that is being regulated') .
	$self->attribute_value_as_1row('Regulator',['regulator'], 'The entity wich is causing the regualtion') .
	$self->attribute_value_as_1row('Regulation type',['regulationType'], 'The type of regulation caused by the regulator') .
	$self->prepared_attribute_value_as_1row(undef,'summation',[map {$self->prettyfy_instance($_)->html_text} @{$self->Summation}]) .
	$self->literaturereferences_as_2rows;
}

sub soft_displayName {
    my $self = shift;
    return $self->hyperlinked_string('[Regulation] '.($self->displayName || ''));
}

sub _make_switch_html {
    my $self = shift;
    my $tmp = $self->SUPER::_make_switch_html();
    $tmp || return '';
    return $tmp;
}

package GKB::PrettyInstance::RegulationType;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance);

sub html_table_rows {
    my ($self) = @_;
    return $self->attribute_value_as_1row('Name',['name']);
}

sub soft_displayName {
    my $self = shift;
    # Should be implemented by subclasses
    return $self->hyperlinked_string('[Regulation type] '.($self->displayName || ''));
}


package GKB::PrettyInstance::ReplacedResidue;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance::ModifiedResidue);

#sub displayName {
#    my ($self) = @_;
#    $self->debug && print join("\t", (caller(0))[3], $self,  $self->class, ($self->db_id || '')), "\n";
#    my $displayName = "";
#    $displayName = (($self->Modification->[0]) ? $self->Modification->[0]->displayName : 'unknown mmodification') . ' at ';
#    $displayName .= ($self->Coordinate->[0]) ? 'position ' . $self->Coordinate->[0] : 'unknown postion';
#    return $displayName;
#}

package GKB::PrettyInstance::Requirement;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance::PositiveRegulation);

sub html_table_rows {
    my ($self) = @_;
    return
	$self->_stable_identifier() .
	$self->attribute_value_as_1row('Authored', ['authored']) .
	$self->attribute_value_as_1row('Reviewed', ['reviewed']) .
	($self->is_valid_attribute('revised') ? $self->attribute_value_as_1row('Revised', ['revised']) : '') .
	$self->attribute_value_as_1row('Regulated entity',['regulatedEntity'], 'The entity which is being regulated') .
	$self->attribute_value_as_1row('Requirement',['regulator'], 'The entity required for the regulation to take place') .
	$self->attribute_value_as_1row('Regulation type',['regulationType'], 'The type of regulation that has occured') .
	$self->prepared_attribute_value_as_1row(undef,'summation',[map {$self->prettyfy_instance($_)->html_text} @{$self->Summation}]) .
	$self->literaturereferences_as_2rows;
}

package GKB::PrettyInstance::SimpleEntity;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance::PhysicalEntity);

#sub soft_displayName {
#    my $self = shift;
#    # Should be implemented by subclasses
#    return $self->hyperlinked_string('[Molecule] '.($self->displayName || ''));
#}


package GKB::PrettyInstance::Species;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance::Taxon);

sub create_image {
    my ($self,$format) = @_;
    my $reactions1 = undef;
    my $class = 'ReactionlikeEvent';
    my $attribute = 'species';
    if (!($self->dba->ontology->is_valid_class($class))) {
    	$class = 'Reaction';
    }
    if (!($self->dba->ontology->is_valid_class_attribute($class, $attribute))) {
    	$attribute = 'taxon';
    }
    $reactions1 = $self->dba->fetch_instance_by_attribute($class,[[$attribute,[$self->db_id]]]);
    my $rm;
    $rm = new GKB::ReactionMap(-DBA => $self->dba,-CGI => $self->cgi,-NO_DEFAULT_IMAGE => 1,-ORIGIN_INSTANCE=>$self, -FORMAT => $format);
    $rm->colour_reactions_by_evidence_type($reactions1,1);
#    $rm->set_reaction_color(0,0,128,$reactions1);
    $rm->create_image_for_species([$self],1);
#    print qq(<PRE>), (caller(0))[3],"\t",  time() - $start, qq(</PRE>\n);
    return $rm;
}

sub reactionmap_key {
    return qq(<IMG SRC="/icons/event_reactionmap_key.png">);
}

sub html_table_rows {
    my ($self) = @_;
    my $str = '';
    my $ar = GKB::Utils::fetch_top_level_events_for_species($self->dba,$self);
    if (@{$ar}) {
	my $treemaker = GKB::HtmlTreeMaker::NoReactions->new(-ROOT_INSTANCES => $ar,
							     -URLMAKER => $self->urlmaker,
							     -INSTRUCTIONS => {
								 'Pathway' => {'attributes' => ['hasComponent','hasEvent']},
								 'ConceptualEvent' => {'attributes' => ['hasSpecialisedForm']},
								 'EquievalentEventSet' => {'attributes' => ['hasMember']},
								 'Reaction' => {'attributes' => ['hasMember']},
								 'BlackBoxEvent' => {'attributes' => ['hasComponent']},
							     },
#							 -DEFAULT_CLASS => 'sidebar',
							     -DEPTH => 1,
							     -ATTRIBUTE_LABELS => {'hasComponent' => '', 'hasMember' => '', 'hasSpecialisedForm' => '', 'hasMember' => ''});
	$str = $treemaker->tree;
	if ($str) {
	    $str = qq(<TR><TH COLSPAN="2" CLASS="species"><A HREF="javascript:void(0)" onMouseover="ddrivetip('Pathways curated or predicted in this species','#DCDCDC', 250)" onMouseout="hideddrivetip()">Pathways</A></TH></TR>\n<TD COLSPAN="2" CLASS="species">$str</TD></TR>\n);
	}
    }
    return
	$self->attribute_value_as_1row('Names and abbreviations',['name']) .
	$self->attribute_value_as_1row('Crossreference',['crossReference'], 'Link to the NCBI Taxonomy browser') .
	$self->lineage_as_2_rows .
	$self->make_attribute_tree_as_2rows(-TITLE =>'Sub-taxa',
                                            -CLASSES => ['Taxon'],
                                            -ATTRIBUTES => ['superTaxon'],
					    -REVERSE => 1,
					    -DEPTH => 3) .
       $str;
}


package GKB::PrettyInstance::Summation;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance);

sub html_text {
    my ($self) = @_;
    $self->debug && print join("\t", (caller(0))[3], $self,  $self->class, ($self->db_id || '')), "\n";
    my $str =
	$self->Text->[0] .
	(($self->LiteratureReference->[0])
	 ? ' ['. join(', ',
		      map{$_->author_year}
		      map{$self->prettyfy_instance($_)}
		      @{$self->LiteratureReference}
		      ) . ']'
	 : '');
    return $str;
}

sub html_table {
    my ($self) = @_;
    $self->debug && print join("\t", (caller(0))[3], $self,  $self->class, ($self->db_id || '')), "\n";
    if (@{$self->reverse_attribute_value('summation')} == 1) {
	return $self->prettyfy_instance($self->reverse_attribute_value('summation')->[0])->html_table;
    } else {
	return
	    qq(<DIV CLASS="section">) .
	    qq(\n<TABLE WIDTH="$HTML_PAGE_WIDTH" CLASS=") . $self->class . qq(">\n) .
	    $self->html_table_rows_if_necessary .
	    $self->_view_switch_html .
	    qq(</TABLE>\n) .
	    qq(</DIV>\n)
	    ;
    }
}

sub html_table_rows {
    my ($self) = @_;
    return
#	$self->attribute_value_as_1row('Author(s)',[qw(created InstanceEdit author)]) .
        $self->prepared_attribute_value_as_1row(undef, 'text', $self->Text) .
	$self->recursive_value_list_as_2rows(-TITLE =>'Event(s) described',
					     -CLASSES => ['Event'],
					     -ATTRIBUTES => ['summation'],
					     -DELIMITER => '<BR />',
					     -REVERSE => 1);
}

sub soft_displayName {
    my $self = shift;
    return $self->hyperlinked_string('[Descriptive text] '.($self->displayName || ''));
}


package GKB::PrettyInstance::Taxon;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance);

sub html_table_rows {
    my ($self) = @_;
    return
	$self->attribute_value_as_1row('Names and abbreviations',['name']) .
	$self->attribute_value_as_1row('Crossreference',['crossReference'], 'Link to the NCBI Taxonomy browser') .
	$self->lineage_as_2_rows .
	$self->make_attribute_tree_as_2rows(-TITLE =>'Sub-taxa',
                                            -CLASSES => ['Taxon'],
                                            -ATTRIBUTES => ['superTaxon'],
					    -REVERSE => 1,
					    -DEPTH => 3);

}

sub lineage_as_2_rows {
    my $self = shift;
    my $tmp = $self;
    my @tmp = ($tmp);
    while ($tmp->SuperTaxon->[0]) {
	$tmp = $tmp->SuperTaxon->[0];
	$self->prettyfy_instance($tmp);
	unshift @tmp, $tmp;
    }
    return
#	qq(<TR><TH COLSPAN="2" CLASS="superTaxon">Lineage</TH></TR>\n<TR><TD COLSPAN="2" CLASS="superTaxon">) .
	qq(<TR><TH COLSPAN="2" CLASS="superTaxon"><A HREF="javascript:void(0)" onMouseover="ddrivetip('Any continuous line of descent in the taxonomic tree','#DCDCDC', 250)" onMouseout="hideddrivetip()">Lineage</TH></TR>\n<TR><TD COLSPAN="2" CLASS="superTaxon">) .
	join(", ", map {$_->hyperlinked_displayName} @tmp) .
	qq(</TD></TR>\n);
}

package GKB::PrettyInstance::ReferenceEntity;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance);

sub create_image {
    my ($self,$format) = @_;
    my (%i, %o, %c); #flags for coloring
#entities are followed 'upwards' as well, i.e. complexes that they are part of are included
    my $reactions1 = $self->follow_class_attributes
	(-INSTRUCTIONS =>
	 {'ReferenceMolecule' => {'reverse_attributes' => [qw(referenceEntity)]},
	  'ReferenceSequence' => {'reverse_attributes' => [qw(referenceEntity)]},
	  'ReferenceDNASequence' => {'reverse_attributes' => [qw(referenceGene)]},
	  'ReferenceRNASequence' => {'reverse_attributes' => [qw(referenceTranscript)]},
	  'PhysicalEntity' => {'reverse_attributes' => [qw(hasComponent input hasMember repeatedUnit)]}},
	 -OUT_CLASSES => ['Reaction']
	 );

    my $reactions2 = $self->follow_class_attributes
	(-INSTRUCTIONS =>
	 {'ReferenceMolecule' => {'reverse_attributes' => [qw(referenceEntity)]},
	  'ReferenceSequence' => {'reverse_attributes' => [qw(referenceEntity)]},
	  'ReferenceDNASequence' => {'reverse_attributes' => [qw(referenceGene)]},
	  'ReferenceRNASequence' => {'reverse_attributes' => [qw(referenceTranscript)]},
	  'PhysicalEntity' => {'reverse_attributes' => [qw(hasComponent output hasMember repeatedUnit)]}},
	 -OUT_CLASSES => ['Reaction']
	 );

    my $reactions3 = $self->follow_class_attributes
	(-INSTRUCTIONS =>
	 {'ReferenceMolecule' => {'reverse_attributes' => [qw(referenceEntity)]},
	  'ReferenceSequence' => {'reverse_attributes' => [qw(referenceEntity)]},
	  'ReferenceDNASequence' => {'reverse_attributes' => [qw(referenceGene)]},
	  'ReferenceRNASequence' => {'reverse_attributes' => [qw(referenceTranscript)]},
	  'PhysicalEntity' => {'reverse_attributes' => [qw(hasComponent physicalEntity hasMember repeatedUnit)]},
	  'CatalystActivity' => {'reverse_attributes' => [qw(catalystActivity)]}},
	 -OUT_CLASSES => ['Reaction']
	 );
    $reactions1 = $self->grep_for_instances_with_focus_species_or_default($reactions1);
    $reactions2 = $self->grep_for_instances_with_focus_species_or_default($reactions2);
    $reactions3 = $self->grep_for_instances_with_focus_species_or_default($reactions3);

    map {$i{$_}++} @{$reactions1};
    map {$o{$_}++} @{$reactions2};
    map {$c{$_}++} @{$reactions3};

    my %seen;
    my (@all, @io, @ic, @oc, @i, @o, @c); #combinations of input, output, catalysts
    foreach (@{$reactions1}, @{$reactions2}, @{$reactions3})  {
	next if $seen{$_}++;
	$i{$_} && $o{$_} && $c{$_} && (push @all, $_) && next;
	$i{$_} && $o{$_} && (push @io, $_) && next;
	$i{$_} && $c{$_} && (push @ic, $_) && next;
	$o{$_} && $c{$_} && (push @oc, $_) && next;
	$i{$_} && (push @i, $_) && next;
	$o{$_} && (push @o, $_) && next;
	$c{$_} && (push @c, $_) && next;
    }

    my $rm = new GKB::ReactionMap(-DBA => $self->dba,-CGI => $self->cgi,-NO_DEFAULT_IMAGE => 1,-ORIGIN_INSTANCE=>$self, -FORMAT=>$format);
    $rm->set_reaction_color(128,0,0,\@i);
    $rm->set_reaction_color(0,128,0,\@o);
    $rm->set_reaction_color(0,0,128,\@c);
    $rm->set_reaction_color(0,0,0,\@all);
    $rm->set_reaction_color(220,220,0,\@io);
    $rm->set_reaction_color(220,0,220,\@ic);
    $rm->set_reaction_color(0,220,220,\@oc);
    $rm->create_image_for_species($self->focus_species_or_default);
    return $rm;
}

sub hyperlinked_identifier {
    my ($self) = @_;
    if ($self->is_valid_attribute('identifier') && $self->is_valid_attribute('referenceDatabase')) {
	if (my $refdb = $self->ReferenceDatabase->[0]) {
	    if (my $id = $self->Identifier->[0]) {
		if (my $url = $refdb->AccessUrl->[0]) {
		    $url =~ s/###ID###/$id/g;
		    #return qq(<A HREF="$url">$id</A>);
		    return qq(<A HREF="$url">) . $refdb->displayName . ':' . $id . qq(</A>);
		} else {
		    #return $id;
		    return $refdb->displayName . ':' . $id;
		}
	    }
	}
    }
    return undef;
}

sub hyperlinked_abbreviation {
    my ($self) = @_;
    if ($self->is_valid_attribute('identifier') && $self->is_valid_attribute('referenceDatabase')) {
	if (my $refdb = $self->ReferenceDatabase->[0]) {
	    if (my $id = $self->Identifier->[0]) {
		if (my $url = $refdb->AccessUrl->[0]) {
		    $url =~ s/###ID###/$id/g;
		    my $dn = $refdb->displayName;
		    (my $acls = $dn) =~ s/\s+//g;
		    # An ugly hack to keep Ewan happy and give Entrez Gene 'G' as abbreviation
		    # rather than the 1st letter of the name as for everything else.
		    # On the longer run we might want to add a 'symbol' attribute to
		    # ReferenceDatabase class to hold this information
		    my $abbr;
		    if ($dn eq 'Entrez Gene') {
			$abbr = 'G';
		    } else {
			($abbr) =  $dn =~ /^(\w)/;
		    }
		    return qq(<A HREF="$url" CLASS="$acls" onMouseover="ddrivetip('Go to $dn:$id','#DCDCDC', 250)" onMouseout="hideddrivetip()">$abbr</A>);
		}
	    }
	}
    }
    return '';
}

sub refererenceGroups {
    my $self = shift;
    my $rgcs = $self->ReferenceGroupCount;
    @{$rgcs} || return '';
    my $out = qq(<TR><TH CLASS="referenceGroupCount">Chemical fingerprint</TH><TD CLASS="referenceGroupCount">);
    foreach my $rgc (@{$rgcs}) {
	if ($rgc->MaxCount->[0] == $rgc->MinCount->[0]) {
	    $out .= $rgc->MaxCount->[0] . ' x ';
	} else {
	    $out .= $self->MinCount->[0] . ' .. ' . $self->MaxCount->[0] . ' x ';
	}
	$out .= $self->prettyfy_instance($rgc->ReferenceGroup->[0])->hyperlinked_displayName . '<BR />';
    }
    $out .= "</TD></TR>\n";
    return $out;
}

package GKB::PrettyInstance::ReferenceMolecule;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance::ReferenceEntity);

sub html_table_rows {
    my ($self) = @_;

    my $ar = $self->follow_class_attributes
	(-INSTRUCTIONS =>
	 {'ReferenceMolecule' => {'reverse_attributes' => [qw(referenceEntity)]},
	  'PhysicalEntity' => {'reverse_attributes' => [qw(hasMember hasComponent)]}},
	 -OUT_CLASSES => [qw(PhysicalEntity)]
	 );
    my $tmp = '';
    foreach my $pe (map {$self->prettyfy_instance($_)} @{$self->reverse_attribute_value('referenceEntity')}) {
	$tmp .= "<P />" . $self->prettyfy_instance($pe)->hyperlinked_displayName . "\n";
	$tmp .= $ {$pe->make_attribute_tree(-CLASSES => [qw(Complex)],
					    -ATTRIBUTES => ['hasComponent'],
					    -REVERSE => 1)};
    }
    return
	$self->prepared_attribute_value_as_1row('Identifier','identifier', [$self->hyperlinked_identifier], 'Contains the external database identifier') .
	$self->attribute_value_as_1row('Database',['referenceDatabase'], 'Name of the database that is being referred to') .
	$self->attribute_value_as_1row('Name(s)',['name'], 'Names and synonyms') .
	$self->prepared_attribute_value_as_1row('Crossreference(s)','crossReference',[map {$self->prettyfy_instance($_)->hyperlinked_identifier} @{$self->CrossReference}]) .
	$self->refererenceGroups .
#	($tmp ?
#	 qq(<TR><TH CLASS="hasComponentRev" COLSPAN="2">Molecules and complexes referring to this reference molecule</TH></TR>\n) .
#	 qq(<TR><TD CLASS="hasComponentRev" COLSPAN="2">$tmp</TD></TR>\n):
#	 '') .
#	$self->entityHierarchy_as_1_row .
        $self->participant_in_event_tree_as_2rows(-TITLE => 'Processes where this chemical is involved',
						  -PHYSICALENTITIES => $ar) .
	'';
}

sub few_details {
    my ($self) = @_;

    my $ar = $self->follow_class_attributes
	(-INSTRUCTIONS =>
	 {'ReferenceMolecule' => {'reverse_attributes' => [qw(referenceEntity)]},
	  'PhysicalEntity' => {'reverse_attributes' => [qw(hasMember hasComponent)]}},
	 -OUT_CLASSES => [qw(PhysicalEntity)]
	 );
    my $tmp = '';
    foreach my $pe (map {$self->prettyfy_instance($_)} @{$self->reverse_attribute_value('referenceEntity')}) {
	$tmp .= "<P />" . $self->prettyfy_instance($pe)->hyperlinked_displayName . "\n";
	$tmp .= $ {$pe->make_attribute_tree(-CLASSES => [qw(Complex)],
					    -ATTRIBUTES => ['hasComponent'],
					    -REVERSE => 1)};
    }
    return
	$self->prepared_attribute_value_as_1row('Identifier','identifier', [$self->hyperlinked_identifier], 'Contains the external database identifier') .
	$self->attribute_value_as_1row('Database',['referenceDatabase'], 'Name of the database that is being referred to') .
	$self->attribute_value_as_1row('Name(s)',['name'], 'Names and synonyms') .
	$self->prepared_attribute_value_as_1row('Crossreference(s)','crossReference',[map {$self->prettyfy_instance($_)->hyperlinked_identifier} @{$self->CrossReference}]) .
	$self->refererenceGroups .
	'';
}

package GKB::PrettyInstance::ReferenceMoleculeClass;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance::ReferenceEntity);

package GKB::PrettyInstance::ReferenceGroup;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance::ReferenceEntity);

sub html_table_rows {
    my ($self) = @_;

    my $ar = $self->follow_class_attributes
	(-INSTRUCTIONS =>
	 {
#	  'ReferenceGroup' => {'reverse_attributes' => [qw(referenceGroup referenceEntity)]},
	  'ReferenceGroup' => {'reverse_attributes' => [qw(referenceGroup)]},
	  'ReferenceGroupCount' => {'reverse_attributes' => [qw(referenceGroupCount)]},
	  'PhysicalEntity' => {'reverse_attributes' => [qw(hasMember hasComponent)]},
	  'ReferenceMolecule'  => {'reverse_attributes' => [qw(referenceEntity)]}
         },
	 -OUT_CLASSES => [qw(PhysicalEntity)]
	 );
    my $tmp = '';
    foreach my $pe (map {$self->prettyfy_instance($_)} @{$ar}) {
	$tmp .= "<P />" . $self->prettyfy_instance($pe)->hyperlinked_displayName . "\n";
	$tmp .= $ {$pe->make_attribute_tree(-CLASSES => [qw(Complex)],
					    -ATTRIBUTES => ['hasComponent'],
					    -REVERSE => 1)};
    }
    my $ar2 = $self->follow_class_attributes
	(-INSTRUCTIONS =>
	 {'ReferenceGroup' => {'reverse_attributes' => [qw(modification)]},
	  'AbstractModifiedResidue' => {'reverse_attributes' => [qw(hasModifiedResidue)]},
         },
	 -OUT_CLASSES => [qw(PhysicalEntity)]
	 );
    my $tmp2 = '';
    foreach my $pe (map {$self->prettyfy_instance($_)} sort {$a->displayName cmp $b->displayName} @{$ar2}) {
	$tmp2 .= GKB::PrettyInstance::hyperlinked_displayName($pe) . "<BR >\n";
    }
    return
	$self->prepared_attribute_value_as_1row('Identifier','identifier', [$self->hyperlinked_identifier], 'Contains the external database identifier') .
	$self->attribute_value_as_1row('Database',['referenceDatabase'], 'Name of the database that is being referred to') .
	$self->attribute_value_as_1row('Name(s)',['name'], 'Names and synonyms') .
	($tmp2 ?
	 qq(<TR><TH CLASS="hasComponentRev" COLSPAN="2">Molecules containing this group as a modification</TH></TR>\n) .
	 qq(<TR><TD CLASS="hasComponentRev" COLSPAN="2">$tmp2</TD></TR>\n):
	 '') .
	($tmp ?
	 qq(<TR><TH CLASS="hasComponentRev" COLSPAN="2">Molecules and complexes containing this reference group</TH></TR>\n) .
	 qq(<TR><TD CLASS="hasComponentRev" COLSPAN="2">$tmp</TD></TR>\n):
	 '') .
        $self->participant_in_event_tree_as_2rows(-TITLE => 'Processes where this chemical is involved',
						  -PHYSICALENTITIES => $ar) .
	'';
}


package GKB::PrettyInstance::ReferenceSequence;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance::ReferenceEntity);

sub other_identifiers {
    my $self = shift;
    my @tmp = @{$self->SecondaryIdentifier};
    if ($self->is_valid_attribute('otherIdentifier')) {
	push @tmp, @{$self->OtherIdentifier};
    }
    return '' unless (@tmp);
    return $self->prepared_attribute_value_as_1row('Other identifiers related to this sequence','identifier',[join(', ', @tmp)]);
}

sub hyperlinked_extended_displayName {
    my $self = shift;
    return $self->SUPER::hyperlinked_extended_displayName . ' '
	. $self->Description->[0]
	. ($self->Species->[0] ? ' [' .$self->Species->[0]->displayName . ']' : '');
}

sub soft_displayName {
    my $self = shift;
    return $self->hyperlinked_string('[Sequence] '
				     . ($self->displayName || '')
				     . ' ' . $self->Description->[0]
				     . ($self->Species->[0] ? ' [' .$self->Species->[0]->displayName . ']' : '')
				     );
}

sub extended_crossreferences {
    my $self = shift;
    my $protein_class = &GKB::Utils::get_reference_protein_class($self->dba);
    my $ar = $self->follow_class_attributes
	(-INSTRUCTIONS =>
	 {
	     'ReferenceRNASequence' => {'attributes' => [qw(referenceGene)]},
	     $protein_class => {'attributes' => [qw(referenceGene referenceTranscript)]},
	     'ReferenceEntity' => {'attributes' => [qw(crossReference)]}}
	);
    @{$ar} = grep {defined $_} map {$self->prettyfy_instance($_)->hyperlinked_identifier}
    sort {$a->displayName cmp $b->displayName} @{$ar};
    return '' unless (@{$ar});
    return qq(<TR><TH CLASS="crossReference" WIDTH="25%"><A NAME="crossReferenceAnchor">Links to corresponding entries in other databases</A></TH><TD CLASS="crossReference" WIDTH="75%">) .
    join('<BR />', @{$ar}) . qq(</TD></TR>\n);
}

sub html_table_rows {
    my ($self) = @_;

    my $ar = $self->follow_class_attributes
	(-INSTRUCTIONS =>
	 {'ReferenceSequence' => {'reverse_attributes' => [qw(referenceEntity)]},
	  'PhysicalEntity' => {'reverse_attributes' => [qw(hasComponent hasMember hasCandidate)]}},
	 -OUT_CLASSES => [qw(EntityWithAccessionedSequence Complex EntitySet)]
	 );
    $DB::single=1;
    return
	$self->prepared_attribute_value_as_1row('Identifier','identifier', [$self->hyperlinked_identifier || $self->Identifier->[0]], 'Contains the external database identifier') .
	$self->attribute_value_as_1row('Database',['referenceDatabase']) .
	$self->attribute_value_as_1row('Species',['species'], 'Species of origin of the sequence') .
	$self->attribute_value_as_1row('Description',['description'], 'A description of the sequence from the external DB') .
	$self->extended_crossreferences .
	$self->other_identifiers .
        $self->reverse_attribute_value_as_1row('Molecules with this sequence',['referenceEntity']) .
	$self->complexes_and_polymers .
	$self->as_1row('Represented by generalisation(s)','referenceEntityRev',
		       {-INSTRUCTIONS =>{'ReferenceSequence'=>{'reverse_attributes'=>[qw(referenceEntity)]},
					 'PhysicalEntity'=>{'reverse_attributes'=>[qw(hasMember hasCandidate)]}},
			-OUT_CLASSES=>['EntitySet']}) .
#       $self->entityHierarchy_as_1_row .
        $self->participant_in_event_tree_as_2rows(-TITLE => 'Processes where molecules and complexes with this sequence are involved',
						  -PHYSICALENTITIES => $ar) .
	$self->reverse_attribute_value_as_1row('Modifications in molecules with this sequence',
					       [qw(referenceSequence AbstractModifiedResidue)]) .
	$self->reverse_attribute_value_as_1row('Domains',[qw(referenceEntity SequenceDomain)]) .
	'';
}

sub few_details {
    my ($self) = @_;

    my $ar = $self->follow_class_attributes
	(-INSTRUCTIONS =>
	 {'ReferenceSequence' => {'reverse_attributes' => [qw(referenceEntity)]},
	  'PhysicalEntity' => {'reverse_attributes' => [qw(hasComponent hasMember hasCandidate)]}},
	 -OUT_CLASSES => [qw(EntityWithAccessionedSequence Complex EntitySet)]
	 );
    return
	$self->prepared_attribute_value_as_1row('Identifier','identifier', [$self->hyperlinked_identifier || $self->Identifier->[0]], 'Contains the external database identifier') .
	$self->attribute_value_as_1row('Database',['referenceDatabase']) .
	$self->attribute_value_as_1row('Species',['species'], 'Species of origin of the sequence') .
	$self->attribute_value_as_1row('Description',['description'], 'A description of the sequence from the external DB') .
	$self->extended_crossreferences .
	$self->other_identifiers .
        $self->reverse_attribute_value_as_1row('Molecules with this sequence',['referenceEntity']) .
	$self->complexes_and_polymers .
	$self->as_1row('Represented by generalisation(s)','referenceEntityRev',
		       {-INSTRUCTIONS =>{'ReferenceSequence'=>{'reverse_attributes'=>[qw(referenceEntity)]},
					 'PhysicalEntity'=>{'reverse_attributes'=>[qw(hasMember hasCandidate)]}},
			-OUT_CLASSES=>['EntitySet']}) .
	$self->reverse_attribute_value_as_1row('Modifications in molecules with this sequence',
					       [qw(referenceSequence AbstractModifiedResidue)]) .
	$self->reverse_attribute_value_as_1row('Domains',[qw(referenceEntity SequenceDomain)]) .
	'';
}

package GKB::PrettyInstance::ReferenceIsoform;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance::ReferenceGeneProduct);

package GKB::PrettyInstance::ReferenceGeneProduct;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance::ReferenceSequence);

# ReferencePeptideSequence is deprecated, since it is no longer part of the Reactome                                                                                                      # data model, but it is being kept for backwards compatibility.
package GKB::PrettyInstance::ReferencePeptideSequence;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance::ReferenceSequence);


package GKB::PrettyInstance::ReferenceDNASequence;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance::ReferenceSequence);


package GKB::PrettyInstance::ReferenceRNASequence;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance::ReferenceSequence);


package GKB::PrettyInstance::GenomeEncodedEntity;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance::PhysicalEntity);


package GKB::PrettyInstance::EntityWithAccessionedSequence;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance::GenomeEncodedEntity);

sub fragmentCoordinates {
    my $self = shift;
    return '' if (($self->StartCoordinate->[0] == 1) && ($self->EndCoordinate->[0] == -1));
    $self->prepared_attribute_value_as_1row
	(
	 'Coordinates in the reference sequence',
	 'startEndCoordinates',
	 [$self->StartCoordinate->[0] . ' .. ' . $self->EndCoordinate->[0]]
	);
}

sub links_2_external_sequence_records_old {
    my $self = shift;
    my $out = '';
    if (my $re = $self->ReferenceEntity->[0]) {
	$out = $self->prettyfy_instance($re)->extended_crossreferences;
#	$out = $self->prepared_attribute_value_as_1row('Link to sequence','identifier', [$self->prettyfy_instance($re)->hyperlinked_identifier], 'Hyperlink to the sequence record in external database.');
#	if ($re->is_valid_attribute('referenceGene')) {
#	    $out .= $self->prepared_attribute_value_as_1row('Corresponding entries in other databases','identifier', [map {$self->prettyfy_instance($_)->hyperlinked_identifier} @{$re->ReferenceGene}], 'Hyperlinks to entries in external databases');
#	}
    }
    return $out;
}

sub links_2_external_sequence_records {
    my $self = shift;
    my $ar = ();
    if (my $re = $self->ReferenceEntity->[0]) {
	    my $protein_class = &GKB::Utils::get_reference_protein_class($self->dba);
	    $ar = $re->follow_class_attributes
		(-INSTRUCTIONS =>
		 {
		     'ReferenceRNASequence' => {'attributes' => [qw(referenceGene)]},
		     $protein_class => {'attributes' => [qw(referenceGene referenceTranscript)]},
		     'ReferenceEntity' => {'attributes' => [qw(crossReference)]}}
		);
    }
    if (my $cr = $self->crossReference->[0]) {
	    push(@{$ar}, @{$self->crossReference});
    }
	@{$ar} = grep {defined $_} map {$self->prettyfy_instance($_)->hyperlinked_identifier}
	sort {$a->displayName cmp $b->displayName} @{$ar};
    return '' unless (@{$ar});
    return qq(<TR><TH CLASS="crossReference" WIDTH="25%"><A NAME="crossReferenceAnchor">Links to corresponding entries in other databases</A></TH><TD CLASS="crossReference" WIDTH="75%">) .
    join('<BR />', @{$ar}) . qq(</TD></TR>\n);
}

sub modifications_as_1row {
    my $self = shift;
    return '' unless ($self->is_valid_attribute('hasModifiedResidue'));
    return '' unless ($self->HasModifiedResidue->[0]);
    my $protein_class = &GKB::Utils::get_reference_protein_class($self->dba);
    if ($self->ReferenceEntity->[0] && $self->ReferenceEntity->[0]->is_a($protein_class)) {
	return $self->attribute_value_as_1row('Post-translational modification(s)',['hasModifiedResidue']);
    } else {
	return $self->attribute_value_as_1row('Modification(s)',['hasModifiedResidue']);
    }
}

sub other_identifiers {
    my $self = shift;
    my $re = $self->ReferenceEntity->[0] || return '';
    return $self->prettyfy_instance($re)->other_identifiers;
}

=head
sub hyperlinked_displayName {
    my ($self) = @_;
    my $out = $self->SUPER::hyperlinked_displayName;
    if (my $re = $self->ReferenceEntity->[0]) {
	my @a = ($re);
	if ($re->is_valid_attribute('referenceGene')) {
	    push @a, @{$re->ReferenceGene};
	}
	$out .= ' ' . join('',grep {$_}
			   map {$self->prettyfy_instance($_)->hyperlinked_abbreviation} @a);
    }
    return $out;
}
=cut

package GKB::PrettyInstance::GO_CellularComponent;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance::GO_Like_Thing);

package GKB::PrettyInstance::Compartment;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance::GO_CellularComponent);

sub create_image {
    my ($self,$format) = @_;
    my $start = time();
#    print qq(<PRE>\n); $self->dba->debug(1);
    my $gocc = $self->follow_class_attributes
	(-INSTRUCTIONS =>
	 {'Compartment' => {
	     'reverse_attributes' => [qw(instanceOf componentOf)]
	 }}
	 );
    my $reactions = $self->dba->fetch_instance_by_attribute
    ('Reaction',
     [
      ['compartment',[map {$_->db_id} @{$gocc}]],
      ['species',[map {$_->db_id} @{$self->focus_species_or_default}]]
     ]);
    my $rm = new GKB::ReactionMap(-DBA => $self->dba,-CGI => $self->cgi,-NO_DEFAULT_IMAGE => 1,-ORIGIN_INSTANCE=>$self,-FORMAT=>$format);
    $rm->set_reaction_color(0,0,128,$reactions);
    $rm->create_image_for_species($self->focus_species_or_default);
#    print qq(</PRE>\n); $self->dba->debug(0);
#    print qq(<PRE>), (caller(0))[3],"\t",  time() - $start, qq(</PRE>\n);
    return $rm;
}

sub html_table_rows1 {
    my ($self) = @_;
    my $start = time();
    my $parents = $self->follow_class_attributes
	(
	 -INSTRUCTIONS => {'GO_CellularComponent' => {'attributes' => [qw(instanceOf componentOf)]}}
	 );
    my $followed_path;
    map {$followed_path->{$_->db_id} = $_} @{$parents};
    my $root_instances;
    foreach (@{$parents}) {
	! $_->InstanceOf->[0] and ! $_->ComponentOf->[0] and push @{$root_instances}, $_;
    }
#    print qq(<PRE>find parents\t), (caller(0))[3],"\t",  time() - $start, qq(</PRE>\n);
    # want to show also immediate kids
    map {$followed_path->{$_->db_id} = $_}
    (@{$self->reverse_attribute_value('instanceOf')},@{$self->reverse_attribute_value('componentOf')});
    my $parentTreeMaker = GKB::HtmlTreeMaker->new
	(-ROOT_INSTANCES => $root_instances,
	 -FOLLOWED_PATH_HASH => $followed_path,
	 -INSTRUCTIONS => {'GO_CellularComponent' => {'reverse_attributes' => [qw(instanceOf componentOf)]}},
	 -ATTRIBUTE_LABELS => {'instanceOf' => 'I', 'componentOf'=> 'C'},
	 -URLMAKER => $self->urlmaker,
	 -HIGHLITE1_HASH => {$self->db_id => $self},
	 -HIGHLITE1_CLASS => 'bold'
	 );
    $parentTreeMaker->show_attribute(1);
    my $tree_str =
	qq(<TR><TH CLASS="instanceOf">Parent components and immediate children</TH><TD CLASS="instanceOf">) .
	$parentTreeMaker->tree .
	qq(</TD></TR>\n);

    my $children = $self->follow_class_attributes
	(-INSTRUCTIONS => {'GO_CellularComponent' => {'reverse_attributes' => [qw(instanceOf componentOf)]}});
    my $focus_species = $self->focus_species;
    my $results;
    $start = time();
    if ($focus_species && @{$focus_species}) {
	$results = $self->dba->fetch_instance_by_subclass_attribute
	    ('PhysicalEntity',[
		 ['compartment', [map {$_->db_id} @{$children}]],
		 ['species', [map {$_->db_id} @{$focus_species}]]]);
	push @{$results}, @{$self->dba->fetch_instance_by_subclass_attribute
				('PhysicalEntity',[
				     ['compartment', [map {$_->db_id} @{$children}]],
				     ['species', [], 'IS NULL']])};
    } else {
	$results = $self->dba->fetch_instance_by_attribute
	    ('PhysicalEntity',[['compartment', [map {$_->db_id} @{$children}]]]);
    }
#    print qq(<PRE>find entities\t), (caller(0))[3],"\t",  time() - $start, qq(</PRE>\n);
    $start = time();
    $self->dba->load_class_attribute_values_of_multiple_instances('PhysicalEntity','species',$results);
    $self->dba->load_class_attribute_values_of_multiple_instances('PhysicalEntity','compartment',$results);
    $self->dba->load_class_attribute_values_of_multiple_instances('PhysicalEntity','referenceEntity',$results);
    my @tmp = $self->dba->instance_cache->values;
    my $protein_class = &GKB::Utils::get_reference_protein_class($self->dba);
    $self->dba->load_class_attribute_values_of_multiple_instances($protein_class,'referenceGene',\@tmp);
    $self->dba->load_class_attribute_values_of_multiple_instances('ReferenceEntity','crossReference',\@tmp);
    @tmp = $self->dba->instance_cache->values;
    $self->dba->load_class_attribute_values_of_multiple_instances('DatabaseIdentifier','referenceDatabase',\@tmp);
    $self->dba->load_class_attribute_values_of_multiple_instances('ReferenceEntity','referenceDatabase',\@tmp);
    $self->dba->load_class_attribute_values_of_multiple_instances('DatabaseIdentifier','identifier',\@tmp);
    $self->dba->load_class_attribute_values_of_multiple_instances('ReferenceEntity','identifier',\@tmp);
    @tmp = $self->dba->instance_cache->values;
    $self->dba->load_class_attribute_values_of_multiple_instances('DatabaseObject','_displayName',\@tmp);
#    print qq(<PRE>load att values\t), (caller(0))[3],"\t",  time() - $start, qq(</PRE>\n);
    $start = time();
#    $self->dba->debug(1); print qq(<PRE>);
#    my @entities = map {$_->db_id . "\t" . $self->prettyfy_instance($_)->hyperlinked_displayName_w_species} @{$results};
    my @entities = map {$self->prettyfy_instance($_)->hyperlinked_displayName_w_species} @{$results};
#    $self->dba->debug(0); print qq(</PRE>);
#   print qq(<PRE>prettyfy entities\t), (caller(0))[3],"\t",  time() - $start, qq(</PRE>\n);
    return
	$self->attribute_value_as_1row('Name',['name']) .
	$self->prepared_attribute_value_as_1row('Accession','accesson', [$self->hyperlinked_identifier]) .
	$self->attribute_value_as_1row('Definition',['definition']) .
	$tree_str .
	$self->values_as_2rows('Entities in this compartment',
			       'compartmentRev',
			       \@entities,
			       '<BR />'
			      );
}

sub html_table_rows {
    my ($self) = @_;
    my $start = time();
    my $parents = $self->follow_class_attributes
	(
	 -INSTRUCTIONS => {'GO_CellularComponent' => {'attributes' => [qw(instanceOf componentOf)]}}
	 );
    my $followed_path;
    map {$followed_path->{$_->db_id} = $_} @{$parents};
    my $root_instances;
    foreach (@{$parents}) {
	! $_->InstanceOf->[0] and ! $_->ComponentOf->[0] and push @{$root_instances}, $_;
    }
#    print qq(<PRE>find parents\t), (caller(0))[3],"\t",  time() - $start, qq(</PRE>\n);
    # want to show also immediate kids
    map {$followed_path->{$_->db_id} = $_}
    (@{$self->reverse_attribute_value('instanceOf')},@{$self->reverse_attribute_value('componentOf')});
    my $parentTreeMaker = GKB::HtmlTreeMaker->new
	(-ROOT_INSTANCES => $root_instances,
	 -FOLLOWED_PATH_HASH => $followed_path,
	 -INSTRUCTIONS => {'GO_CellularComponent' => {'reverse_attributes' => [qw(instanceOf componentOf)]}},
	 -ATTRIBUTE_LABELS => {'instanceOf' => 'I', 'componentOf'=> 'C'},
	 -URLMAKER => $self->urlmaker,
	 -HIGHLITE1_HASH => {$self->db_id => $self},
	 -HIGHLITE1_CLASS => 'bold'
	 );
    $parentTreeMaker->show_attribute(1);
    my $tree_str =
	qq(<TR><TH CLASS="instanceOf">Parent components and immediate children</TH><TD CLASS="instanceOf">) .
	$parentTreeMaker->tree .
	qq(</TD></TR>\n);

    my $children = $self->follow_class_attributes
	(-INSTRUCTIONS => {'GO_CellularComponent' => {'reverse_attributes' => [qw(instanceOf componentOf)]}});
    my $focus_species = $self->focus_species;
    my $results;
    $start = time();
    if ($focus_species && @{$focus_species}) {
	$results = $self->dba->fetch_instance_by_subclass_attribute
	    ('PhysicalEntity',[
		 ['compartment', [map {$_->db_id} @{$children}]],
		 ['species', [map {$_->db_id} @{$focus_species}]]]);
	push @{$results}, @{$self->dba->fetch_instance_by_subclass_attribute
				('PhysicalEntity',[
				     ['compartment', [map {$_->db_id} @{$children}]],
				     ['species', [], 'IS NULL']])};
    } else {
	$results = $self->dba->fetch_instance_by_attribute
	    ('PhysicalEntity',[['compartment', [map {$_->db_id} @{$children}]]]);
    }
#    print qq(<PRE>find entities\t), (caller(0))[3],"\t",  time() - $start, qq(</PRE>\n);
    $start = time();
    return
	$self->attribute_value_as_1row('Name',['name']) .
	$self->prepared_attribute_value_as_1row('Accession','accesson', [$self->hyperlinked_identifier]) .
	$self->attribute_value_as_1row('Definition',['definition']) .
	$tree_str .
	$self->instances_as_2rows('Entities in this compartment',
			       'compartmentRev',
			       $results
			      );
}

sub few_details {
    my ($self) = @_;
    my $start = time();
    my $parents = $self->follow_class_attributes
	(
	 -INSTRUCTIONS => {'GO_CellularComponent' => {'attributes' => [qw(instanceOf componentOf)]}}
	 );
    my $followed_path;
    map {$followed_path->{$_->db_id} = $_} @{$parents};
    my $root_instances;
    foreach (@{$parents}) {
	! $_->InstanceOf->[0] and ! $_->ComponentOf->[0] and push @{$root_instances}, $_;
    }
#    print qq(<PRE>find parents\t), (caller(0))[3],"\t",  time() - $start, qq(</PRE>\n);
    # want to show also immediate kids
    map {$followed_path->{$_->db_id} = $_}
    (@{$self->reverse_attribute_value('instanceOf')},@{$self->reverse_attribute_value('componentOf')});
    my $parentTreeMaker = GKB::HtmlTreeMaker->new
	(-ROOT_INSTANCES => $root_instances,
	 -FOLLOWED_PATH_HASH => $followed_path,
	 -INSTRUCTIONS => {'GO_CellularComponent' => {'reverse_attributes' => [qw(instanceOf componentOf)]}},
	 -ATTRIBUTE_LABELS => {'instanceOf' => 'I', 'componentOf'=> 'C'},
	 -URLMAKER => $self->urlmaker,
	 -HIGHLITE1_HASH => {$self->db_id => $self},
	 -HIGHLITE1_CLASS => 'bold'
	 );
    $parentTreeMaker->show_attribute(1);
    my $tree_str =
	qq(<TR><TH CLASS="instanceOf">Parent components and immediate children</TH><TD CLASS="instanceOf">) .
	$parentTreeMaker->tree .
	qq(</TD></TR>\n);

    my $children = $self->follow_class_attributes
	(-INSTRUCTIONS => {'GO_CellularComponent' => {'reverse_attributes' => [qw(instanceOf componentOf)]}});
    return
	$self->attribute_value_as_1row('Name',['name']) .
	$self->prepared_attribute_value_as_1row('Accession','accesson', [$self->hyperlinked_identifier]) .
	$self->attribute_value_as_1row('Definition',['definition']) .
	$tree_str;
}

package GKB::PrettyInstance::EntityCompartment;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance::Compartment);

package GKB::PrettyInstance::GO_Like_Thing;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance);

sub hyperlinked_identifier {
    my ($self) = @_;
    my ($refdb,$url);
    if ($self->is_valid_attribute('accession') && $self->is_valid_attribute('referenceDatabase')) {
	if ($refdb = $self->ReferenceDatabase->[0] and
	    $url = $refdb->AccessUrl->[0]) {
	    if (my $id = $self->Accession->[0]) {
		$url =~ s/###ID###/$id/g;
		#return qq(<A HREF="$url">) . $refdb->displayName . ':' . $id . qq(</A>);
		my $dn = $refdb->displayName;
		(my $acls = $dn) =~ s/\s+//g;
		#my ($abbr) =  $dn =~ /^(\w)/;
		return qq(<A HREF="$url" CLASS="$acls" onMouseover="ddrivetip('Go to $dn:$id','#DCDCDC', 250)" onMouseout="hideddrivetip()">$acls</A>);
	    }
	}
    }
    return undef;
}

sub hyperlinked_displayName {
    my $self = shift;
    my $tmp;
    if ($self->is_valid_attribute('crossReference') && $self->CrossReference->[0]) {
	return $self->SUPER::hyperlinked_displayName . ' [' .
            $self->prettyfy_instance($self->CrossReference->[0])->hyperlinked_displayName . ']';

    } elsif ($self->is_valid_attribute('accession') &&
	     $self->Accession->[0] &&
	     ($tmp = $self->hyperlinked_identifier)) {
	return $self->SUPER::hyperlinked_displayName . ' ' . $tmp . '';
    } else {
	return $self->SUPER::hyperlinked_displayName;
    }
}

package GKB::PrettyInstance::GO_MolecularFunction;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::PrettyInstance::GO_Like_Thing);

sub create_image {
    my ($self,$format) = @_;
    my $rm = new GKB::ReactionMap(-DBA => $self->dba,-CGI => $self->cgi,-NO_DEFAULT_IMAGE => 1,-ORIGIN_INSTANCE=>$self,-FORMAT=>$format);
    $rm->set_reaction_color(0,0,128,$self->get_reactions);
    $rm->create_image_for_species($self->focus_species_or_default);
    return $rm;
}

sub html_table_rows {
    my ($self) = @_;
    return
	$self->attribute_value_as_1row('name',['name']) .
	$self->prepared_attribute_value_as_1row('Accession','accesson', [$self->hyperlinked_identifier]) .
#	$self->attribute_value_as_1row('Crossreference(s)',['crossReference']) .
	$self->attribute_value_as_1row('Definition',['definition']) .
	$self->attribute_value_as_1row('Enzyme Classification (EC) number',['ecNumber']) .
	$self->instances_as_2rows('Molecules/complexes with this activity',
				  'activityRev',
				  $self->get_entities
	) .
	$self->instances_as_2rows('Reactions catalysed by this activity',
			      'catalystActivityRev',
			      $self->get_reactions
	) .
	$self->make_attribute_tree_as_2rows(-TITLE =>'Is a specialisation of activities',
                                            -CLASSES => [$self->class],
                                            -ATTRIBUTES => [$self->go_relationship_attributes()]) .
#                                            -ATTRIBUTES => ['instanceOf']) .
	$self->make_attribute_tree_as_2rows(-TITLE =>'Is a generalisation of more specialised activities',
                                            -ATTRIBUTES => [$self->go_relationship_attributes()],
#                                            -ATTRIBUTES => ['instanceOf'],
					    -DEPTH => 1,
                                            -REVERSE => 1);
}

sub few_details {
    my ($self) = @_;
    return
	$self->attribute_value_as_1row('name',['name']) .
	$self->prepared_attribute_value_as_1row('Accession','accesson', [$self->hyperlinked_identifier]) .
	$self->attribute_value_as_1row('Definition',['definition']) .
	$self->attribute_value_as_1row('Enzyme Classification (EC) number',['ecNumber']) .
	$self->make_attribute_tree_as_2rows(-TITLE =>'Is a specialisation of activities',
                                            -CLASSES => [$self->class],
                                            -ATTRIBUTES => [$self->go_relationship_attributes()]) .
#                                            -ATTRIBUTES => ['instanceOf']) .
	$self->make_attribute_tree_as_2rows(-TITLE =>'Is a generalisation of more specialised activities',
#                                            -ATTRIBUTES => ['instanceOf'],
                                            -ATTRIBUTES => [$self->go_relationship_attributes()],
					    -DEPTH => 1,
                                            -REVERSE => 1);
}

sub get_descendants {
    my $self = shift;
    my $out;
    unless ($out = $self->get_cached_value('descendants')) {
	$out = $self->follow_class_attributes
	(-INSTRUCTIONS => {'GO_MolecularFunction' => {'reverse_attributes' => [$self->go_relationship_attributes(),'componentOf']}});
#	(-INSTRUCTIONS => {'GO_MolecularFunction' => {'reverse_attributes' => ['instanceOf','componentOf']}});
	$self->set_cached_value('descendants',$out);
    }
    return $out;
}

sub get_reactions {
    my ($self) = @_;
    my $out;
    unless ($out = $self->get_cached_value('reactions')) {
	my $descendants = $self->get_descendants;
	my $focus_species = $self->focus_species;
	if (@{$focus_species}) {
	    $out = $self->dba->fetch_instance_by_remote_attribute
		('Reaction',
		 [['catalystActivity.activity','=',[map {$_->db_id} @{$descendants}]],
		  ['species','=',[map {$_->db_id} @{$focus_species}]]]
		);
	} else {
	    $out = $self->dba->fetch_instance_by_remote_attribute
		('Reaction',
		 [['catalystActivity.activity','=',[map {$_->db_id} @{$descendants}]]]
		);
	}
	$self->set_cached_value('reactions',$out);
    }
    return $out;
}

sub get_entities {
    my ($self) = @_;
    my $out;
    unless ($out = $self->get_cached_value('entities')) {
	my $descendants = $self->get_descendants;
	my $focus_species = $self->focus_species;
	if (@{$focus_species}) {
	    $out = $self->dba->fetch_instance_by_remote_attribute
		('PhysicalEntity',
		 [['physicalEntity:CatalystActivity.activity','=',[map {$_->db_id} @{$descendants}]],
		  ['species','=',[map {$_->db_id} @{$focus_species}]]]
		);
	} else {
	    $out = $self->dba->fetch_instance_by_remote_attribute
		('PhysicalEntity',
		 [['physicalEntity:CatalystActivity.activity','=',[map {$_->db_id} @{$descendants}]]]
		);
	}
	$self->set_cached_value('entities',$out);
    }
    return $out;
}

# Returns a list of all attributes pertaining to relationships between GO terms.
sub go_relationship_attributes {
    my ($self) = @_;

    my @potential_relationship_attributes = ('instanceOf', 'hasPart', 'regulate', 'positivelyRegulate', 'negativelyRegulate');
    my @relationship_attributes = ();
    foreach my $potential_relationship_attribute (@potential_relationship_attributes) {
        if ($self->is_valid_attribute($potential_relationship_attribute)) {
            push(@relationship_attributes, $potential_relationship_attribute);
        }
    }

    return @relationship_attributes;
}

package GKB::PrettyInstance::StableIdentifier;
use vars qw(@ISA);
use strict;
use GKB::Config;
@ISA = qw(GKB::PrettyInstance);

=head
sub html_table_rows {
    my $self = shift;
    return $self->prettyfy_instance($self->reverse_attribute_value('stableIdentifier')->[0])->html_table_rows;
}

sub html_table {
    my $self = shift;
    return $self->prettyfy_instance($self->reverse_attribute_value('stableIdentifier')->[0])->html_table;
}

sub create_image {
    my $self = shift;
    return $self->prettyfy_instance($self->reverse_attribute_value('stableIdentifier')->[0])->create_image;
}

sub reactionmap_key {
    my $self = shift;
    return $self->prettyfy_instance($self->reverse_attribute_value('stableIdentifier')->[0])->reactionmap_key;
}
=cut

# Hyperlink to a "control panel" for this stable ID.
sub hyperlinked_displayName {
    my $self = shift;
    my $identifier = $self->identifier->[0];
    my $out = "";
    if ($identifier) {
    	my $identifier_version = $self->identifierVersion->[0];
    	if (defined $identifier_version) {
    		$identifier .= '.' . $identifier_version;
    	}

    	$out = $identifier;

	    # Only create a clickable link if an identifier database is
	    # available.
		my $cgi = $self->cgi;
		my $format = GKB::WebUtils::get_format($cgi);
	    if ($self->exists_identifier_database()) {
			# use $CACHE_GENERATED_DOCUMENTS to detect dev/curator server
#		    if ($CACHE_GENERATED_DOCUMENTS eq 0 || !(defined $format) || (defined $format && !($format eq 'elv'))) {
		     	my $control_panel = "control_panel_st_id?ST_ID=$identifier";
		    	$out = "<A HREF=\"$control_panel\">$identifier</A>";
#	    	}
	    }
    }
	return $out;
}

1;
