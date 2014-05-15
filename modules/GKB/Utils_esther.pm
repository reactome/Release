package GKB::Utils_esther;

use strict;

sub transfer_referers {
    my ($from, $to, $instance_edit, $dba) = @_;
    my ($transfer) = 0;
    $from->inflated || $from->inflate;
    $to->inflated || $to->inflate;
    my @att_list = $from->list_valid_reverse_attributes;
    my $ar = $dba->fetch_referer_by_instance($from);
    foreach my $ref (@{$ar}) {
	$ref->inflate;
	foreach my $att (@att_list) {
	    my $replaced;
	    next unless $ref->is_valid_attribute($att);
	    #next unless grep {$_ == $from} @{$ref->$att};
	    print $att, "\n";

	    print "*****************\n";
	    $ref->print_simple;
	    print "*******************\n";

       	    $replaced = $ref->replace_attribute_value($att, $from, $to);
	    next unless $replaced;
	    $transfer++;
	    print "__________________\n";
	    $ref->print_simple;
	    print "_____________________\n";

	    $dba->update_attribute($ref, $att);

	    print "Replaced ", $from->extended_displayName, " with ", $to->extended_displayName, " in ", $ref->extended_displayName, "\n";
	}
	update_modified_if_necessary($ref,$instance_edit, $dba);
    }
    return $transfer;
}

sub update_modified_if_necessary {
    my ($i, $instance_edit, $dba) = @_;
    return if ($i->Created->[0] && ($i->Created->[0]->db_id == $instance_edit->db_id));
    unless (grep {$_->db_id == $instance_edit->db_id} @{$i->modified}) {
	push @{$i->modified}, $instance_edit;
	$dba->update_attribute($i, 'modified');
    }
}

sub create_instance_edit {
    my ($dba, $surname, $initials, $note) = @_;
    my $ar = $dba->fetch_instance(-CLASS => 'Person',
				  -QUERY => [{-ATTRIBUTE => 'surname',
					      -VALUE => [$surname]
					      },
					     {-ATTRIBUTE => 'initial',
					      -VALUE => [$initials]
					      }]
				  );
    my $author;
    if (@{$ar}) {
	$author = $ar->[0];
    } else {
	$author = GKB::Instance->new(-ONTOLOGY=>$dba->ontology,
				     -CLASS=> 'Person');
	$author->inflated(1);
	$author->Surname($surname);
	$author->Initial($initials);
	$dba->store($author);
    }
    my $instance_edit = GKB::Instance->new(-ONTOLOGY=>$dba->ontology,
					   -CLASS=> 'InstanceEdit');
    $instance_edit->inflated(1);
    $instance_edit->author($author);
    $note && $instance_edit->note($note);
    $dba->store($instance_edit);
    return $instance_edit;
}

sub check_author {
    my ($i) = @_;
    my $author;
    if ($i->is_valid_attribute('edited') && $i->Edited->[0] && $i->Edited->[0]->Author->[0]) {
	$author = $i->Edited->[0]->Author->[0]->Surname->[0];
    } elsif ($i->Created->[0] && $i->Created->[0]->Author->[0]) {
	$author = $i->Created->[0]->Author->[0]->Surname->[0];
#    } elsif ($i->Modified->[0] && $i->Modified->[0]->Author->[0]) {
#	$author = "? (".$i->Modified->[0]->Author->[0]->Surname->[0].")"; #add ? as it's hard to tell who really is the author
    } else {
	$author = 'unknown author';
    }
    return $author;
}

sub resolve_complex {
    my ($compl) = @_;
    my @components;
    foreach my $comp (@{$compl->HasComponent}) {
	if ($comp->is_a('Complex')) {
	    push @components, @{resolve_complex($comp)};
	} else {
	    push @components, $comp;
	}
    }
    return \@components;
}

sub compare_arrays {
    my ($ar1, $ar2) = @_;
    my $a1 = @{$ar1}; #number of elements
    my $a2 = @{$ar2};
    return unless $a1 == $a2;
    @{$ar1} = sort @{$ar1};
    @{$ar2} = sort @{$ar2};
    foreach my $n (1..$a1) {
	return unless $ar1->[$n-1] == $ar2->[$n-1];
    }

    print "These arrays are identical:\n";
    map {print $_, "\t"} @{$ar1};
    print "\n";
    map {print $_, "\t"} @{$ar2};
    print "\n";
    return 1;
}

sub instancebrowser_hyperlink {
    my ($id, $display, $db) = @_;
    $db || ($db = 'gk_central');
    $display || ($display = $id);
    return "<a href=\"http://brie8.cshl.org/cgi-bin/instancebrowser?DB=$db\&ID=$id\&\">$display\</a><br>";
    #return "http://brie8.cshl.org/cgi-bin/instancebrowser?DB=$db\&ID=$id\&";
}

sub eventbrowser_hyperlink {
    my ($id, $display, $db) = @_;
    $db || ($db = 'gk_central');
    $display || ($display = $id);
    return "<a href=\"http://brie8.cshl.org/cgi-bin/eventbrowser?DB=$db\&ID=$id\&\">$display\</a><br>";
    #return "http://brie8.cshl.org/cgi-bin/eventbrowser?DB=$db\&ID=$id\&";
}

sub get_event_slice {
    my ($dba, @id) = @_;
    my @tmp;
    foreach my $id (@id) {
	my $event_ar = $dba->fetch_instance_by_db_id($id);
	my $ar = $event_ar->[0]->follow_class_attributes(-INSTRUCTIONS => 
							 {'Pathway' => {'attributes' => [qw(hasComponent)]},
							  'ConceptualEvent' => {'attributes' => [qw(hasSpecialisedForm)]},
							  'EquivalentEventSet' => {'attributes' => [qw(hasMember)]}
						      },
							 -OUT_CLASSES => [qw(Reaction)]);
	push @tmp, @{$ar};
    }
    return \@tmp;
}

sub sendmail_html {
    my ($subject, $html, $myself) = @_;
    #require Mail::Sendmail;
    my $recipient;
    $myself?($recipient = 'eschmidt@ebi.ac.uk'):($recipient = 'internal@reactome.org');
    my %mail = (
		'from' => 'eschmidt@ebi.ac.uk',
#		'to' => 'internal@reactome.org',
		'to' => $recipient,
		'subject' => $subject,
		'content-type' => 'text/html; charset="iso-8859-1"'
		);
    $mail{'body'} = "<html>$html</html>";
    Mail::Sendmail::sendmail(%mail) || print "Error: $Mail::Sendmail::error\n";
}

1;
