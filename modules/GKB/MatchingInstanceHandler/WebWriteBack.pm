package GKB::MatchingInstanceHandler::WebWriteBack;

use strict;
use vars qw(@ISA);
use GKB::MatchingInstanceHandler;
use GKB::DBAdaptor;
use GKB::Ontology;
use GKB::Utils;
use GKB::Config;
use Data::Dumper;
@ISA = qw(GKB::MatchingInstanceHandler);

sub handle_matching_instances {
    my ($self,$instance,$dba) = @_;
    $self->debug && print "<PRE>", join("\n", (caller(0))[3],$self->stack_trace_dump), "</PRE>\n";
    my $ar = $instance->identical_instances_in_db;
    return unless (@{$ar});
    $self->store_project($dba);
    $self->print_selection_form($instance);
    $self->throw("have to handle the matching instance. (This message should not reach the user.)");
}

sub _print_log_message {
}

sub handle_temporarily_stored_instances {
    my ($self,$dba) = @_;
    $self->debug && print "<PRE>", join("\n", (caller(0))[3],$self->stack_trace_dump), "</PRE>\n";
    my $tmp_db_name = $self->cgi->param('TMPDB') || $self->throw("No TMPDB.");
    my $tmp_dba = GKB::DBAdaptor->new(-DBNAME => $tmp_db_name, -HOST => $GK_DB_HOST);
#    my $tmp_dba = GKB::DBAdaptor->new(-DBNAME => $tmp_db_name, -HOST => $GK_DB_HOST, -DEBUG => $dba->debug);
    $self->tmp_db_name($tmp_db_name);
    $self->tmp_dba($tmp_dba);
    my $ar = $self->_fetch_temporarily_stored_instances;
    $self->instance_cache($tmp_dba->instance_cache);
    my $default_ie = $self->_fetch_default_InstanceEdit->[0] ||
	$self->throw("Now default InstanceEdit specified. How can this happen?\n");
    $default_ie->attribute_value('_applyToAllEditedInstances',undef);
    $dba->default_InstanceEdit($default_ie);
    if (my $current_instance_tmp_db_id = $self->cgi->param('CURRENTINSTANCE')) {
	my $current_i = $tmp_dba->instance_cache->fetch($current_instance_tmp_db_id) ||
	    $self->throw("No instance with tmp db_id $current_instance_tmp_db_id.");
	if (my ($val) = $self->cgi->param('STOREAS') =~ /(\d+)/) {
#	    print $self->cgi->param('STOREAS'), "\t$val\n";
	    my $i_in_db = $dba->fetch_instance_by_db_id($val)->[0];
	    $i_in_db || $self->throw("No instance with db_id $val.");
	    $current_i->db_id($val);
	    $current_i->status('STORED AS ' . $i_in_db->extended_displayName);
#	} elsif (($val) = $self->cgi->param('MERGETO') =~ /(\d+)/) {
#	    my $i_in_db = $dba->fetch_instance_by_db_id($val)->[0];
#	    $i_in_db || $self->throw("No instance with db_id $val.");	
##	    $i_in_db->dba->debug(1);print qq(<PRE>\n);
#	    $current_i->db_id($val);
#	    $self->_deal_with_internal_duplicates($current_i);
#	    $i_in_db->inflate;
#	    my $copy_before_merge = $i_in_db->clone;
#	    $i_in_db->merge($current_i);
#	    if ($i_in_db->equals($copy_before_merge)) {
#		$current_i->status('UNCHANGED');
#	    } else {
#		$i_in_db->add_attribute_value('modified',$current_i->Created->[0]);
#		my $ibc = $i_in_db->{'_InstanceBeforeChange'};
#		$ibc->InstanceEdit($current_i->Created->[0] || $default_ie);
#		$self->instance_cache->store("$ibc", $ibc);
#		map {$self->instance_cache->store("$_", $_)} @{$ibc->AttributeValuesBeforeChange};
#		$dba->store($ibc);
#		$ibc->status('NEW');
#		$dba->update($i_in_db);
#		$current_i->status('UPDATED');
#	    }
#	    $i_in_db->dba->debug(undef);print qq(</PRE>\n);
	} elsif ($self->cgi->param('NEW')) {
	    $dba->store($current_i);
	    $current_i->status('NEW');
	} else {
	    $self->throw("Don't know what to do, need either STOREAS, MERGETO, MERGEFROM or NEW parameter.");
	}
    }
    return $ar;
}

sub _fetch_default_InstanceEdit {
    my ($self) = @_;
    $self->debug && print "<PRE>", join("\n", (caller(0))[3],$self->stack_trace_dump), "</PRE>\n";
    return $self->tmp_dba->fetch_instance_by_attribute('InstanceEdit',[['_applyToAllEditedInstances',['TRUE']]]);
}

sub _fetch_temporarily_stored_instances {
    my ($self) = @_;
    $self->debug && print "<PRE>", join("\n", (caller(0))[3],$self->stack_trace_dump), "</PRE>\n";
    my $ar = $self->tmp_dba->fetch_all_class_instances_as_shells($self->tmp_dba->ontology->root_class);
    foreach my $i (@{$ar}) {
	$i->inflate;
	$i->db_id(undef);
	$i->attribute_value($DB_ID_NAME,undef);
	my $val;
	if ($val = $i->attribute_value('_internal1')->[0]) {
	    $i->attribute_value($DB_ID_NAME,$val);
	    unless ($i->attribute_value('_internal2')->[0]) {
		$i->db_id($val);
	    }
	}
	if ($val = $i->attribute_value('_internal3')->[0]) {
	    $i->status($val);
	}
	$i->attribute_value('_internal1', undef);
	$i->attribute_value('_internal2', undef);
	$i->attribute_value('_internal3', undef);
    }
    return $ar;
}

sub store_project {
    my ($self,$dba) = @_;
    $self->debug && print "<PRE>", join("\n", (caller(0))[3],$self->stack_trace_dump), "</PRE>\n";
    # Set this back to true so that we can find it next time around
    $dba->default_InstanceEdit->attribute_value('_applyToAllEditedInstances','TRUE');
    my @instances = $self->instance_cache->key_value_array;
    my %seen;
    for (my $j = 0; $j < $#instances; $j +=2) {
	$seen{"$instances[$j+1]"} = 1;
    }
    while (@instances) {
	my $key = shift @instances;
	my $i = shift @instances;
	# This is to make sure that all the attribute value instances are indeed
	# also going to be stored. This is a precautionary measure since really
	# everything *should* be in the cache, but sometimes somehow that doesn't
	# seem to be the case.
	$self->debug && print "<PRE>", _extended_displayName($i), "</PRE>\n";
	foreach my $att_i (@{$i->get_instance_type_attribute_values}) {
	    unless ($seen{"$att_i"}++) {
		$self->debug && print "<PRE><B>Instance ", _extended_displayName($i), " attribute instance ", _extended_displayName($att_i), " not in cache!</B></PRE>\n";
#		print STDERR "Instance ", $i->class_and_id, " attribute instance ", $att_i->class_and_id, " not in cache!\n",
		push @instances, "$att_i", $att_i;
		$self->instance_cache->store("$att_i", $att_i);
	    }
	}
    }
    #
    $self->_unset_db_ids;
    #
    my $tmp_dba;
    unless ($tmp_dba = $self->tmp_dba) {
	my $tmp_db_name = "test_tmp_$$";
#	my $tmp_db_name = "test_tmp_1111";
	$self->tmp_db_name($tmp_db_name);
#	$tmp_dba = GKB::DBAdaptor->new(-ONTOLOGY => $dba->ontology, -HOST => $GK_DB_HOST, -DEBUG => $dba->debug);
	$tmp_dba = GKB::DBAdaptor->new(-ONTOLOGY => $dba->ontology, -HOST => $GK_DB_HOST);
	$self->tmp_dba($tmp_dba);
    }
    $tmp_dba->drop_database;
    $tmp_dba->create_database($self->tmp_db_name);
    #
    foreach my $i ($self->instance_cache->instances) {
	$tmp_dba->store($i);
    }
}

# If db_id set, store it in _internal1
# else if attribute DB_ID set (i.e. extracted instance but not handled yet),
# store it in _internal1 and put 'NOT STORED' in _internal2.
#
# Put status to _internal3
sub _unset_db_ids {
    my $self = shift;
    foreach my $i ($self->instance_cache->instances) {
	my $val;
	if ($val = $i->db_id) {
	    $i->attribute_value('_internal1', $val);
	    $i->db_id(undef);
	} elsif ($val = $i->attribute_value($DB_ID_NAME)->[0]) {
	    $i->attribute_value('_internal1', $val);
	    $i->attribute_value($DB_ID_NAME, undef);
	    $i->attribute_value('_internal2', 'NOT STORED');
	}
	if ($val = $i->status) {
	    $i->attribute_value('_internal3', $val);
	}
    }
}

sub print_selection_form {
    my ($self,$i) = @_;
#    print qq(<PRE>) . (caller(0))[3] . "\t" . $i->id_string . qq(</PRE>\n);
    # Dunno why but have to delete the values in order to not to get old values on 
    # the new form. Bizarre....
    $self->cgi->delete('CURRENTINSTANCE');
    $self->cgi->delete('TMPDB');
#    print qq(<PRE>) . (caller(0))[3] . qq(</PRE>\n);
    # Give the instance a dba so that it can fetch it's reverse attribute values.
    $i->dba($self->tmp_dba);
#    $i->prettyfy(-URLIFY => sub {return "/cgi-bin/instancebrowser?DB=" . $self->tmp_db_name . join('',map {"&ID=" . $_->db_id} @_);});
    my $urlmaker = $self->webutils->urlmaker->clone;
    $urlmaker->param('DB',$self->tmp_db_name);
    $i->prettyfy(-URLMAKER => $urlmaker);
    my ($s) = $self->cgi->path_info =~ /(\d+)/; $s++;
    print $self->cgi->start_form(-action => $self->cgi->script_name . "/$s");
    print $self->cgi->hidden(-name => 'DB', -value => $self->cgi->param('DB'));
    print $self->cgi->hidden(-name => 'TMPDB', -value => $self->tmp_db_name);
    print $self->cgi->hidden(-name => 'CURRENTINSTANCE', -value => $i->db_id);
    print
	qq(<TABLE CLASS="contents" WIDTH="100%" BORDER="1" CELLPADDING="2" CELLSPACING="0">) .
	qq(<TR><TD COLSPAN="2">) . 
#	qq{The instance immediately below has one or more "identical" matches shown (further down) in the database. Choose one of them to store it exactly as the existing instance [Store as...] or merge to the existing instance [Merge to...] or click [New] to store it as a new instance.} .
	qq{The instance immediately below has one or more "identical" matches shown (further down) in the database. Choose one of them to store it exactly as the existing instance [Store as...] or click [New] to store it as a new instance.} .	qq(</TD></TR>\n) .
	qq(<TR><TD>) .
	$i->html_table2(1) .
	qq(</TD><TD>) .
	$self->cgi->submit(-NAME => 'NEW', -LABEL => 'New') .
	qq(</TD></TR>\n);

    foreach my $ii (@{$i->identical_instances_in_db}) {
	$self->print_identical_instance_section($ii);
    }
    print qq(</TABLE>\n);
    print $self->cgi->endform, "\n";
}

sub print_identical_instance_section {
    my ($self,$ii) = @_;
    # To make sure that we have the most recent values from db.
    $ii->inflate;
    #print "<PRE>", join("\n", (caller(0))[3],$self->stack_trace_dump), "</PRE>\n";
#    $ii->prettyfy(-URLIFY => sub {return "/cgi-bin/instancebrowser?DB=" . $self->cgi->param('DB') . join('',map {"&ID=" . $_->db_id} @_);});
    $ii->prettyfy(-WEBUTILS => $self->webutils);
    print 
	qq(<TR><TD>) . 
	$ii->html_table2(1) .
	qq(</TD><TD>) .
	$self->cgi->submit(-NAME => 'STOREAS', -LABEL => 'Store as ' . $ii->db_id) .
#	qq(<P />) .
#	$self->cgi->submit(-NAME => 'MERGETO', -LABEL => 'Merge to ' . $ii->db_id) .
        qq(</TD></TR>\n);	
}

sub drop_tmp_database {
    my ($self) = @_;
    $self->debug &&print "<PRE>", join("\n", (caller(0))[3],$self->stack_trace_dump), "</PRE>\n";
    $self->tmp_dba->execute("DROP DATABASE " . $self->tmp_db_name);
}

sub print_report {
    my $self = shift;
    #print "<PRE>", join("\n", (caller(0))[3],$self->stack_trace_dump), "</PRE>\n";
    my (%handled,%partially_stored);
    my ($partially_stored_count,$handled_count) = (0,0);
    foreach ($self->instance_cache->instances) {
#	next if ($_->class =~ /^_/);
	if ($_->attribute_value('_internal1')->[0] && !$_->attribute_value('_internal2')->[0]) {
	    #print "<PRE>", $_->dumper_string, "</PRE>\n";
	    if ($_->attribute_value('_partial')->[0]) {
		$self->_group_by_status($_, \%partially_stored);
		$partially_stored_count++;
	    } else {
		$self->_group_by_status($_, \%handled);
		$handled_count++;
	    }
	}
    }
    if (%handled) {
	print qq(<HR /><STRONG>Handled $handled_count instances</STRONG>\n);
	foreach my $k (sort {$a cmp $b} keys %handled) {
	    my $ar = $handled{$k};
#	    print "<BR /><BR />", scalar(@{$ar}), " $k\n";
	    print qq(<BR />\n);
	    $self->_print_status_and_displayName($ar);
	}
    }
    if (%partially_stored) {
	print qq(<HR /><STRONG>Partially stored $partially_stored_count instances</STRONG>\n);
	foreach my $k (sort {$a cmp $b} keys %partially_stored) {
	    my $ar = $partially_stored{$k};
#	    print qq(<BR /><BR />$k ), scalar(@{$ar}), "\n";
	    print qq(<BR />\n);
	    $self->_print_status_and_displayName($ar);
	}
    }
}

sub _print_status_and_displayName {
    my ($self,$ar) = @_;
    foreach (@{$ar}) {
	print qq(<BR />). ($_->status || '') . "\t" . $self->_hyperlinked_extended_displayName($_). "\n";
    }
}

sub _hyperlinked_extended_displayName {
    my ($self,$i) = @_;
    return qq(<A HREF="/cgi-bin/instancebrowser?DB=) .
	$self->cgi->param('DB') .
	'&ID=' .  $_->attribute_value('_internal1')->[0] .
	qq(">[) . $i->class . ":" . $_->attribute_value('_internal1')->[0] . '] ' .
	$i->displayName . qq(</A>);
}

sub _group_by_status {
    my ($self,$i,$hr) = @_;
    if (! $i->db_id) {
	push @{$hr->{'NOT STORED'}}, $i;
    } else {
	my $status = $i->status || '';
	if ($status =~ /^STORED AS/) {
	    push @{$hr->{'STORED AS EXISTING'}}, $i;
	} else {
	    push @{$hr->{$status}}, $i;
	}
    }
}

sub print_final_report {
    my $self = shift;
    #print "<PRE>", join("\n", (caller(0))[3],$self->stack_trace_dump), "</PRE>\n";
    my @instances = grep {$_->class !~ /^_/} $self->instance_cache->instances;
    my %h;
    foreach (@instances) {
	$self->_group_by_status($_, \%h);
    }
    print qq(<HR /><STRONG>Handled ) . scalar @instances . qq( instances</STRONG>\n);
    foreach my $k (sort {$a cmp $b} keys %h) {
	my $ar = $h{$k};
#	print "<BR /><BR />", scalar(@{$ar}), " $k\n";
	print qq(<BR />\n);
	if ($k eq 'NOT STORED') {
	    foreach (@{$ar}) {
		print "<BR />$k\t" . $_->extended_displayName . "\n";
	    }
	} else {
	    foreach my $i (@{$ar}) {
		$i->prettyfy(-URLIFY => sub {return "/cgi-bin/instancebrowser?DB=" . $self->cgi->param('DB') . join('',map {"&ID=" . $_->db_id} @_);});
		print "<BR />" . ($i->status || '') . "\t" . $i->hyperlinked_extended_displayName . "\n";
	    }
	}
    }   
}

sub _extended_displayName {
    my $i = shift;
    return "$i [" . $i->class_and_id . '] ' . ($i->attribute_value('_displayName')->[0] || '');
}

sub _check_if_identical_instances_in_db_are_in_project {
    my ($self,$i) = @_;
    my %h;
    map {$h{$_->db_id} = $_} @{$i->identical_instances_in_db};
    foreach my $ci ($self->instance_cache->instances) {
	if (my $t = $ci->attribute_value($DB_ID_NAME)->[0]) {
	    if ($h{$t}) {

	    }
	}
    }
}

1;


