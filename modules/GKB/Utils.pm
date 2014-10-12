package GKB::Utils;

=head1 NAME

GKB::Utils

=head1 SYNOPSIS

A mixed bag of utility subroutines.

=head1 DESCRIPTION

The synopsis says it all.

=head1 SEE ALSO

GKB::FileUtils
GKB::HTMLUtils
GKB::SearchUtils
GKB::UtilsEsther

=head1 AUTHOR

Too diverse to be traceable.

Copyright (c) 2008 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

use strict;
use Carp qw(cluck confess);
use Getopt::Long qw(:config pass_through);
#use Log::Handler;
use GKB::Ontology;
use GKB::Config;
use GKB::DBAdaptor;

sub find_default_InstanceEdit {
    my ($ar) = @_;
    ($ar && ref($ar) && (ref($ar) eq 'ARRAY')) || die("Need ref to array of GKB::Instances, got '$ar'.");
    my @tmp;
    foreach (grep {$_->class eq 'InstanceEdit'} @{$ar}) {
	if (@{$_->attribute_value('_applyToAllEditedInstances')} &&
	    uc($_->attribute_value('_applyToAllEditedInstances')->[0]) eq 'TRUE') {
	    push @tmp, $_;
	}
	$_->attribute_value('_applyToAllEditedInstances',undef);
    }
    if (@tmp) {
	if (@tmp > 1) {
	    confess("More than 1 default InstanceEdit.\n");
	} else {
	    return $tmp[0];
	}
    } else {
	return;
    }
}

sub compare_ontologies {
    my ($o1,$o2) = @_;
    foreach my $class ($o2->list_classes) {
	unless ($o1->is_valid_class($class)) {
	    confess("Different ontologies in projects. Class '$class' not present in the 1st project.");
	}
	foreach my $att ($o2->list_own_attributes($class)) {
	    $att =~ /_id$/ and next;
	    unless ($o1->is_valid_class_attribute($class,$att)) {
		confess("Different ontologies in projects. Attribute '$att' of class '$class' not present in the 1st project.");
	    }
	}
    }
}


### Function: order_Events
# Orders a set of Events according to their precedingEvent attribute values. However, only those values
# which are part of the set are used for ordering. Suitable for ordering Events within a given Pathway.
# Returns the original array ref if the Events are unorderable due to:
# -no one Event in the set is a precedinhgEvent for another Event in the set.
# -circularity, i.e. all Events in the set have a precedingEvent in the set.
# This function was created for ordering the Events in the eventbrowser output.
# Arguments:
# 1) Reference to an array containing Events.
# Returns:
# 1) Reference to an array containing Events.
# NOTE: THIS FUNCTION IS SIMPLE AND DOES NOT DO ANYTHING VERY SOPHISTICATED. IT USES JUST THOSE
# precedingEvent ATTRIBUTE VALUES WHICH ARE IN THE ORIGINAL SET.
###
sub order_Events {
    my ($ar) = @_;
    @{$ar} || return $ar;
    my (%set,%seen,@precedingless,@out);
    map {$set{"$_"} = $_} @{$ar};
    foreach my $e (values %set) {
	if (scalar(grep {$set{"$_"}} @{$e->PrecedingEvent}) == 0) {
	    push @precedingless, $e;
	}
    }
    unless (@precedingless) {
#	warn("Circular Events!");
	return $ar;
    }
    if (@precedingless == scalar(values %set)) {
#	warn("Events unordered!");
	return $ar;
    }
#    if (@precedingless > 1) {
#	warn("Multiple (" . scalar(@precedingless) . ") starting points!");
#	return $ar;
#    }
    my @aa;
    foreach my $e (sort {$a->db_id <=> $b->db_id} @precedingless) {
		my @tmp = ($e);
		my @tmp2;
		while (my $i = shift @tmp) {
		    $seen{"$i"}++ && next;
		    push @tmp, grep {$set{"$_"}} @{$i->reverse_attribute_value('precedingEvent')};
		    push @tmp2, $i;
		}
		push @aa, \@tmp2;
    }
    @aa = sort {scalar(@{$b}) <=> scalar(@{$a}) || $a->[0]->db_id <=> $b->[0]->db_id} @aa;
    return [map {@{$_}} @aa];
}

sub collapse_Events_to_focus_taxon {
    my ($events,$species) = @_;
    my $start = time();
    my @out;
    if ($species && @{$species}) {
	@out = (map {[$_]} @{grep_for_instances_with_given_species($events,$species)});
	@out = sort {uc($a->[0]->displayName) cmp uc($b->[0]->displayName)} @out;
#	print qq(<PRE>), (caller(0))[3],"\t",  time() - $start, qq(</PRE>\n);
	return \@out;
    } else {
	my %taxa;
	foreach my $i (grep {$_->Species->[0]} @{$events}) {
	    $taxa{$i->Species->[0]->db_id}->{'count'}++;
	    $taxa{$i->Species->[0]->db_id}->{'obj'} = $i->Species->[0];
	}
	if (%taxa) {
	    my @descending = sort {$b->{'count'} <=> $a->{'count'}} values %taxa;
	    my @top = 
		sort {$b->{'obj'}->displayName cmp $a->{'obj'}->displayName}
	        grep {$descending[0]->{'count'} == $_->{'count'}} @descending;
	    if ((@top > 1) && (my $hs = (grep {lc($_->{'obj'}->displayName) eq 'homo sapiens'} @top)[0])) {
		$species = $hs->{'obj'};
	    } else {
		$species = $top[0]->{'obj'};
	    }
	}
    }
    my %h;
    map {$h{$_->db_id} = $_} @{$events};    
    #print qq(<PRE>) . ($species ? $species->extended_displayName : 'undef') . qq(</PRE>\n);
    my %seen;
    foreach my $e (@{$events}) {
	my $ar = $e->follow_class_attributes(-INSTRUCTIONS => {'Event' => {'attributes' => ['orthologousEvent'], 'reverse_attributes' => ['orthologousEvent']}});
	# Since there can be one->many relationship the follow_class_attributes call can return
	# Events from the "original" organism which have to be filtered out. This filtering would
	# be redundant if the orthologousEvents attribute is populated properly so that we could
	# just call event->OrthologousEvents.
	@{$ar} = grep {$_->Species->[0] != $e->Species->[0]} @{$ar};
	if ($species) {
	    if ($e->Species->[0] == $species) {
		push @out, [$e, grep {$h{$_->db_id}} @{$ar}];
	    } else {
		next if (grep {$h{$_->db_id}} @{$ar});
		push @out, [$e];
		$h{$e->db_id} = $e;
	    }
	} else {
	    unless ($seen{$e->db_id}) {
		my @tmp = ($e, grep {$h{$_->db_id}} @{$ar});
		map {$seen{$_->db_id}++} @tmp;
		push @out, \@tmp;
	    }
	}
    }
    @out = sort {uc($a->[0]->displayName) cmp uc($b->[0]->displayName)} @out;
#    print qq(<PRE>), (caller(0))[3],"\t",  time() - $start, qq(</PRE>\n);
    return \@out;
}

sub hyperlinked_collapsed_Events {
    my ($urlmaker,$events) = @_;
    my $out;
    if ($events->[0]->isa("GKB::PrettyInstance::Event")) {
	$out = $events->[0]->hyperlinked_displayName;
    } elsif ($events->[0]->Species->[0]) {
	$out = qq(<A HREF=\") . $urlmaker->urlify($events->[0]) . qq(\">) . $events->[0]->displayName . ' [' . $events->[0]->Species->[0]->displayName . ']' . qq(</A>);
    } else {
	$out = qq(<A HREF=\") . $urlmaker->urlify($events->[0]) . qq(\">) . $events->[0]->displayName . qq(</A>);
    }
    foreach (@{$events}[1 .. $#{$events}]) {
	if ($_->Species->[0]) {
	    $out .= qq( [<A HREF=\") . $urlmaker->urlify($_) . qq(\">) . $_->Species->[0]->displayName . qq(</A>]);
	} else {
	    $out .= qq( [<A HREF=\") . $urlmaker->urlify($_) . qq(\">species not specified</A>]);
	}
    }
    return $out;
}

sub check_for_cyclic_defining_attribute_values {
    my ($ar) = @_;
    my %checked;
    my @out;
    foreach my $i (@{$ar}) {
	my %seen;
	my $cyclic = _check_for_cyclic_defining_attribute_values($i,\%seen,\%checked);
	if ($cyclic) {
	    push @out, $cyclic;
	}
    }
    return @out;
}

sub _check_for_cyclic_defining_attribute_values {
    my ($i,$seen,$checked) = @_;
    if ($seen->{"$i"}) {
	delete $seen->{"$i"};
	return $i;
    }
    $seen->{"$i"} = $i;	
    if ($checked->{"$i"}++) {
	delete $seen->{"$i"};
	return;
    }
    my $ontology = $i->ontology;
    foreach my $attribute ($ontology->list_class_defining_attributes($i->class)) {
	next unless ($i->is_instance_type_attribute($attribute));
	foreach my $val (@{$i->attribute_value($attribute)}) {
	    my $ret =_check_for_cyclic_defining_attribute_values($val,$seen,$checked);
	    $ret && return $ret;
	}
    }
    delete $seen->{"$i"};
    return;
}

sub species_abbreviation {
    my ($sp) = @_;
    my $dn = $sp->displayName;
    if ($dn =~ /^(\w)\w*\s+(\w\w)/) {
	return $1 . $2;
    }
    return $dn;
}

sub translate_query_chain {
    my ($str,$operator,$values) = @_;
    my @a = split(/\./,$str);
    my $tmp = pop @a;
    my ($att,$rev_att_cls) = split(/:/,$tmp);
    my $queriette = [$att,$values,$operator,$rev_att_cls];
    while ($tmp = pop @a) {
	($att,$rev_att_cls) = split(/:/,$tmp);
	$queriette = [$att, [$queriette], '=', $rev_att_cls];
    }
    return $queriette;
}

sub rectangular_polygon_coordinates {
    my ($sx,$sy,$tx,$ty,$margin) = @_;
    my $dx = $tx - $sx;
    my $dy = $ty - $sy;
    my $d = (sort {$b <=> $a} (1, sqrt($dx * $dx + $dy * $dy)))[0];
    my $ax = - ($dx / $d);
    my $ay = - ($dy / $d);
    $margin = 2 unless (defined $margin);
    return
	int($sx + $ax * $margin + $ay * $margin),
	int($sy + $ay * $margin - $ax * $margin),
	int($sx + $ax * $margin - $ay * $margin),
	int($sy + $ay * $margin + $ax * $margin),
	int($tx - $ax * $margin - $ay * $margin),
	int($ty - $ay * $margin + $ax * $margin),
	int($tx - $ax * $margin + $ay * $margin),
	int($ty - $ay * $margin - $ax * $margin);
}

sub get_COMPOUND_page_by_id {
    my ($id) = @_;
    require LWP::UserAgent;
#    require HTTP::Request::Common;
    my $url = "http://www.genome.ad.jp/dbget-bin/www_bget?-s+cpd:$id";
    my $ua = LWP::UserAgent->new();
#    print "URL\t$url\n";
#    my $response = $ua->request(GET $url);
    my $response = $ua->get($url);
    if($response->is_success) {
	return $response->content;
    }
    confess("Failed to GET $url");

}

sub get_COMPOUND_names_by_id {
    my ($id) = @_;
    if (my $page = get_COMPOUND_page_by_id($id)) {
	if (my ($name_str) = $page =~ /^NAME\s+(.+?)\n\S+/ms) {
	    $name_str =~ s/\s+\$//gms;
	    return split(/\n\s+/,$name_str);
	} else {
	    confess("Unable to parse name for id '$id'.\n$page\n");
	}
    }
    
}

sub split_reactions_by_evidence_type {
    my ($reactions,$dba,$preload_necessary_values) = @_;
    $dba ||= $reactions->[0]->dba || confess("Need DBAdaptor.");
    my $iea = $dba->fetch_instance_by_attribute('EvidenceType',[['name',['IEA']]])->[0];
    if ($preload_necessary_values) {
	$dba->load_class_attribute_values_of_multiple_instances('Event','inferredFrom',$reactions);
	$dba->load_class_attribute_values_of_multiple_instances('Event','evidenceType',$reactions);
    }
    my (@confirmed,@manually_inferred,@iea);
    foreach my $r (@{$reactions}) {
	if ($r->InferredFrom->[0]) {
	    ($r->EvidenceType->[0] && ($r->EvidenceType->[0] == $iea))
		? push @iea, $r
		: push @manually_inferred, $r;
	} else {
	    push @confirmed, $r
	}
    }
    return (\@confirmed,\@manually_inferred,\@iea);
}

# Returns handle to specified database or latest ensembl mart if not specified.
sub get_handle_to_ensembl_mart {
    require DBI;
    my ($db,$host,$port,$user,$pass) = @_;
    $host ||= 'martdb.ensembl.org';
    $port ||= 3316;
    $user ||= 'anonymous';
    my ($dsn,$dbh,$stmt,$sth,$res);
    if ($db) {
		$dsn = "DBI:mysql:host=$host;port=$port;database=$db";
		$dbh = eval { DBI->connect_cached($dsn,$user,$pass, {RaiseError => 1}); };
		($@ || !$dbh) && die("Could not connect to database '$db' user '$user' using [$dsn] as a locator.\n$@");
    } else {
		# Find the latest mart
		$dsn = "DBI:mysql:host=$host;port=$port";
		$dbh = eval { DBI->connect_cached($dsn,$user,$pass, {RaiseError => 1}); };
		($@ || !$dbh) && die("Could not connect as user '$user' using [$dsn] as a locator.\n$@");
		$stmt = qq(SHOW DATABASES LIKE "ensembl_mart_%");
		$sth = $dbh->prepare($stmt);
		$res = $sth->execute || die $sth->errstr;
		my $ar = $sth->fetchall_arrayref;
		$db = (sort {$b cmp $a} map {$_->[0]} @{$ar})[0];
		$sth = $dbh->prepare("USE $db");
		$res = $sth->execute || die $sth->errstr;
    }
#    print STDERR "Using db $db\n";
    return $dbh;
}

# This method now (20060407) pools the original authors and "revisors".
sub get_authors_recursively {
    my $event = shift;

    if ($event->is_valid_attribute('revised')) {
	return $event->follow_class_attributes
	    (-INSTRUCTIONS =>
	     {
		 'ConceptualEvent' => {'attributes' => [qw(hasSpecialisedForm)]},
		 'EquivalentEventSet' => {'attributes' => [qw(hasMember)]},
		 'Pathway' => {'attributes' => [qw(hasComponent hasEvent)]},
		 'BlackBoxEvent' => {'attributes' => [qw(hasComponent)]},
		 'Reaction' => {'attributes' => [qw(hasMember)]},
		 'Event' => {'attributes' => [qw(authored revised)]},
		 'InstanceEdit' => {'attributes' => [qw(author)]}
	     },
	     -OUT_CLASSES => [qw(Person)]
	    );
    } else {
	return $event->follow_class_attributes
	    (-INSTRUCTIONS =>
	     {
		 'ConceptualEvent' => {'attributes' => [qw(hasSpecialisedForm)]},
		 'EquivalentEventSet' => {'attributes' => [qw(hasMember)]},
		 'Pathway' => {'attributes' => [qw(hasComponent hasEvent)]},
		 'BlackBoxEvent' => {'attributes' => [qw(hasComponent)]},
		 'Reaction' => {'attributes' => [qw(hasMember)]},
		 'Event' => {'attributes' => [qw(authored)]},
		 'InstanceEdit' => {'attributes' => [qw(author)]}
	     },
	     -OUT_CLASSES => [qw(Person)]
	    );
    }
}


sub get_reviewers_recursively {
    my $event = shift;
    return $event->follow_class_attributes
	(-INSTRUCTIONS =>
	 {
	     'ConceptualEvent' => {'attributes' => [qw(hasSpecialisedForm)]},
	     'EquivalentEventSet' => {'attributes' => [qw(hasMember)]},
	     'Pathway' => {'attributes' => [qw(hasComponent hasEvent)]},
	     'BlackBoxEvent' => {'attributes' => [qw(hasComponent)]},
	     'Reaction' => {'attributes' => [qw(hasMember)]},
	     'Event' => {'attributes' => [qw(reviewed)]},
	     'InstanceEdit' => {'attributes' => [qw(author)]}
	 },
	 -OUT_CLASSES => [qw(Person)]
	);
}

sub get_editors_recursively {
    my $event = shift;
    return $event->follow_class_attributes
	(-INSTRUCTIONS =>
	 {
	     'ConceptualEvent' => {'attributes' => [qw(hasSpecialisedForm)]},
	     'EquivalentEventSet' => {'attributes' => [qw(hasMember)]},
	     'Pathway' => {'attributes' => [qw(hasComponent hasEvent)]},
	     'BlackBoxEvent' => {'attributes' => [qw(hasComponent)]},
	     'Reaction' => {'attributes' => [qw(hasMember)]},
	     'Event' => {'attributes' => [qw(edited)]},
	     'InstanceEdit' => {'attributes' => [qw(author)]}
	 },
	 -OUT_CLASSES => [qw(Person)]
	);
}

# This is the old way of doing things - the newer way is in the
# two commented-out methods that follow.  The newer way has the
# disadvantage that it doesn't seem to work.
sub get_event_release_status {
    my ($event,$last_release_date) = @_;

    if (!(defined $last_release_date)) {
    	print STDERR "Utils.get_event_release_status: WARNING - last_release_date is undef!\n";
    	return undef;
    }

    if (my $date = $event->ReleaseDate->[0]) {
	$date =~ s/\D//g;
	if ($date > $last_release_date) {
	    return 'NEW';
	}
    }
    my $ar = $event->follow_class_attributes
	(-INSTRUCTIONS =>
	     {
		 'ConceptualEvent' => {'attributes' => [qw(hasSpecialisedForm)]},
		 'EquivalentEventSet' => {'attributes' => [qw(hasMember)]},
		 'Pathway' => {'attributes' => [qw(hasComponent hasEvent)]},
		 'BlackBoxEvent' => {'attributes' => [qw(hasComponent)]},
		 'Reaction' => {'attributes' => [qw(hasMember)]},
	     }
	);
    my %h;
    my @dates = map {$_->ReleaseDate->[0]} grep {$_->ReleaseDate->[0]} @{$ar};
#    foreach my $e (@{$ar}) {
#	if ($e->Modified->[0]) {
#	    push @dates, $e->Modified->[-1]->DateTime->[0];
#	} elsif ($e->Created->[0]) {
#	    push @dates, $e->Created->[0]->DateTime->[0];
#	}
#    }
    foreach (@dates) {
	$_ =~ s/\D//g;
    }
    my $latest = (sort {$b <=> $a} @dates)[0];
    if (defined $latest && $latest > $last_release_date) {
	return 'UPDATED';
    }
    return;
}

#sub get_single_event_release_status {
#    my ($event,$last_release_date) = @_;
#    if (my $date = $event->ReleaseDate->[0]) {
#	$date =~ s/\D//g;
#	#printf STDERR "%s\t%s\n", $date, $last_release_date;
#	if ($date > $last_release_date) {
#	    return 'NEW';
#	}
#    } elsif (my $ie = $event->Revised->[-1]) {
#	if (my $date = $ie->DateTime->[0]) {
#	    $date =~ s/\D//g;
#	    #printf STDERR "%s\t%s\n", $date, $last_release_date;
#	    if ($date > $last_release_date) {
#		return 'UPDATED';
#	    }
#	}
#    }
#}
#
#sub get_event_release_status { 
#    my ($event,$last_release_date) = @_;
#    if (my $s = get_single_event_release_status($event,$last_release_date)) {
#	return $s;
#    }
#    if ($event->is_a('Pathway')) {
#	my @ea = @{$event->HasEvent};
#	while (my $e = shift @ea) {
#	    if (get_single_event_release_status($event,$last_release_date)) {
#		return 'UPDATED';
#	    }
#	    if ($e->is_a('Pathway')) {
#		push @ea, @{$e->HasEvent};
#	    }
#	}
#    }
#    return;
#}

sub get_db_connection {
    require GKB::ReactomeDBAdaptor;
    our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_debug,$opt_db);
    &GetOptions("user:s", "host:s", "pass:s", "port:i", "debug", "db=s");
    return GKB::ReactomeDBAdaptor->new
	(
	 -user   => $opt_user,
	 -host   => $opt_host,
	 -pass   => $opt_pass,
	 -port   => $opt_port,
	 -dbname => $opt_db,
	 -DEBUG => $opt_debug
	);
}

sub get_db_connections {
    require GKB::ReactomeDBAdaptor;
    our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_debug,$opt_db,$opt_dndb,$opt_from_db,$opt_to_db);
    &GetOptions("user:s", "host:s", "pass:s", "port:i", "debug", "db:s","dndb:s","from_db:s","to_db:s");
    return (GKB::ReactomeDBAdaptor->new
	    (
	     -user   => $opt_user,
	     -host   => $opt_host,
	     -pass   => $opt_pass,
	     -port   => $opt_port,
	     -dbname => ($opt_db || $opt_from_db),
	     -DEBUG => $opt_debug
	    ),
	    GKB::ReactomeDBAdaptor->new
	    (
	     -user   => $opt_user,
	     -host   => $opt_host,
	     -pass   => $opt_pass,
	     -port   => $opt_port,
	     -dbname => ($opt_dndb || $opt_to_db),
	     -DEBUG => $opt_debug
	    ));
}

sub group_inputs_and_catalysts {
    my $reaction = shift;
    my @cs = map {@{$_->PhysicalEntity}} @{$reaction->CatalystActivity};
    my $is = $reaction->Input;
    my (@catalysts,@input_catalysts,@inputs);
    foreach my $pe (@cs) {
	if (grep {$_ == $pe} @{$is}) {
	    push @input_catalysts, $pe;
	} else {
	    push @catalysts, $pe;
	}
    }
    foreach my $pe (@{$is}) {
	if (! grep {$_ == $pe} @cs) {
	    push @inputs, $pe;
	}
    }
    return (\@inputs,\@input_catalysts,\@catalysts);
}

# Checks the supplied pathway and looks to see if it has text or an
# image associated with it, or if it has subpathways or reactions.
# Returns 1 if yes, 0 otherwise.  The depth limit tells it whether
# to include subpathways/reactions in the content criterion or not.
# A false include_diagrams_flag means that the presence of a diagram
# will be ignored in determining the content criterion.
sub is_pathway_with_content {
    my ($self, $event, $depth, $depth_limit, $include_diagrams_flag) = @_;

    if (!$event) {
	return 0;
    }

    # This event has text?  Great!
    if (get_pathway_text($self, $event)) {
	return 1;
    }

    # This event has an image?  Great!
    if ($include_diagrams_flag && find_image_file($self, $event)) {
	return 1;
    }

    # Don't recurse deeper than the user-specified depth limit, since we
    # won't be emitting any text/images below that level anyway.
    if ($depth eq $depth_limit) {
	return 0;
    }

    # If the event has components, then it is also considered as having content.
    my @components = ();
    my $components_ref = \@components;
    if ($event->is_a("Pathway")) {
	$components_ref = $event->attribute_value($event->is_valid_attribute('hasEvent') ? 'hasEvent' : 'hasComponent');
    } elsif ($event->is_a("EquivalentEventSet")) {
	$components_ref = $event->attribute_value("hasMember");
    } elsif ($event->is_a("ConceptualEvent")) {
	$components_ref = $event->attribute_value("hasSpecialisedForm");
    } else {
	#print STDERR "is_pathway_with_content: WARNING - unkown event type!!\n";
    }

    if ($components_ref && scalar($components_ref) =~ /ARRAY/ && scalar(@{$components_ref})>0) {
	return 1;
    }

    return 0;
}

# Extracts the text associated with a pathway - either from the summation
# if available, or otherwise from the description.
sub get_pathway_text {
    my ($self, $pathway) = @_;

    my $text = "";

    if (!$pathway) {
	return $text;
    }

    # By preference, return the text in the summation (usually
    # more complete).
    my $summation = $pathway->attribute_value("summation")->[0];
    if ($summation) {
	$text = $summation->attribute_value("text")->[0];
	if ($text) {
	    return $text;
	}
    }

    # If the summation is empty, make do with the definition.
    $text = $pathway->attribute_value("definition")->[0];
    if ($text) {
	return $text;
    }

    return "";
}

# Givent an instance as an argument, return an array of all
# sub-events, e.g. the contents of the "hasEvent" attribute,
# if the instance was a Pathway, for example.
sub get_instance_sub_events {
    my ($self, $instance) = @_;

	my @components = ();
	my $components_ref = \@components;
	if ($instance->is_valid_attribute("hasEvent")) {
	    $components_ref = $instance->attribute_value("hasEvent");
	} elsif ($instance->is_valid_attribute("hasComponent")) {
	    $components_ref = $instance->attribute_value("hasComponent");
	} elsif ($instance->is_valid_attribute("hasMember")) {
	    $components_ref = $instance->attribute_value("hasMember");
	} elsif ($instance->is_valid_attribute("hasSpecialisedForm")) {
	    $components_ref = $instance->attribute_value("hasSpecialisedForm");
	}
	my @sub_events = ();
	foreach my $component (@{$components_ref}) {
	    if ($component && ($component->is_a("Event"))) {
			push @sub_events, $component;
	    }
	}
	
	return @sub_events;
}

# Basically the same as get_event_literature_references, but it
# drills down the event hierarchy and picks up the literature
# references for sub-events, sub-sub-events, etc as well.
sub get_event_literature_references_recursive {
    my ($self, $event) = @_;
    
    my %reference_hash = ();
    my @references = $self->get_event_literature_references($event);
    my $reference;
    foreach $reference (@references) {
    	$reference_hash{$reference} = $reference;
    }
    
    my @sub_events = $self->get_instance_sub_events($event);
    my $sub_event;
    foreach $sub_event (@sub_events) {
    	@references = $self->get_event_literature_references_recursive($sub_event);
	    foreach $reference (@references) {
	    	$reference_hash{$reference} = $reference;
	    }
    }
    
    my @sorted_references = sort values(%reference_hash);
	return @sorted_references;
}

# Get all of the literature references from the supplied Event instance
# These include both the references directly associated with the Event
# as well as the references associated with any Summation instances
# attached to the event.  An attempt is made to avoid duplication of
# references.  Returns an array of LiteratureReference instances.
sub get_event_literature_references {
    my ($self, $event) = @_;
    
    my %reference_hash = ();

    if (!(defined $event)) {
		return %reference_hash;
    }

    my @references = $self->get_literature_references($event);
    my $reference;
    foreach $reference (@references) {
    	$reference_hash{$reference} = $reference;
    }
    
    my $summations= $event->Summation;
    if (defined $summations) {
	    foreach my $summation (@{$summations}) {
	    	@references = $self->get_literature_references($summation);
		    foreach $reference (@references) {
		    	$reference_hash{$reference} = $reference;
		    }
	    }
    }
    
    my @sorted_references = sort values(%reference_hash);
	return @sorted_references;
}

# Extracts the references associated with an instance.
# The instance should have an attribute "literatureReference",
# containing zero or more instances of type LiteratureReference.
# Returns a list of strings, each string contains one reference.
sub get_literature_references {
    my ($self, $instance) = @_;

    my @references = ();

    if (!$instance) {
		return @references;
    }
    
    my $literatureReferences = $instance->LiteratureReference;
    if (!$literatureReferences) {
		return @references;
    }

	my $initial;
	my $surname;
	my $author;
	my $authors;
	my $journal;
	my $pages;
	my $title;
	my $volume;
	my $year;

	my $i = 0;
	foreach my $literatureReference (@{$literatureReferences}) {
	    $references[$i] = "";

	    $authors = "";
	    foreach $author (@{$literatureReference->attribute_value("author")}) {
			unless ($authors eq "") {
			    $authors .= ", ";
			}
			$initial = $author->attribute_value("initial")->[0] || '';
			$surname = $author->attribute_value("surname")->[0];
			$surname = ucfirst($surname); # to help sorting
#			$authors .= $initial . " " . $surname;
			$authors .= "$surname $initial";
	    }
	    unless ($references[$i] eq "") {
			$references[$i] .= ", ";
	    }
	    $references[$i] .= "<b>$authors</b>";

	    $title = $literatureReference->attribute_value("title")->[0];
	    if ($title) {
		if ($references[$i]) {
		    $references[$i] .= ", ";
		}
		$references[$i] .= "\"$title\"";
	    }

	    $journal = $literatureReference->attribute_value("journal")->[0];
	    if ($journal) {
		if ($references[$i]) {
		    $references[$i] .= ", ";
		}
		$references[$i] .= $journal;
	    }

	    $volume = $literatureReference->attribute_value("volume")->[0];
	    if ($volume) {
		if ($references[$i]) {
		    $references[$i] .= ", ";
		}
		$references[$i] .= $volume;
	    }

	    $year = $literatureReference->attribute_value("year")->[0];
	    if ($year) {
		if ($references[$i]) {
		    $references[$i] .= ", ";
		}
		$references[$i] .= $year;
	    }

	    $pages = $literatureReference->attribute_value("pages")->[0];
	    if ($pages) {
		if ($references[$i]) {
		    $references[$i] .= ", ";
		}
		$references[$i] .= $pages;
	    }

	    $references[$i] .= ".";
	    
	    $i++;
	}

    return @references;
}

# Finds the location of the image associated with the given
# pathway (the diagram).
sub find_image_file {
    my ($self, $pathway) = @_;

    unless ($pathway) {
#		print STDERR "find_image_file: WARNING - pathway is null!!!\n";
		return "";
    }

    # Lets see if there is an image associated with this pathway
    my $figure = $pathway->attribute_value("figure")->[0];
    my $image_file = "";
    if ($figure) {
		# find the file associated with the figure
		$image_file = $figure->attribute_value("url")->[0];
		if (defined $image_file) {
		    my $images_dir = $GK_TMP_IMG_DIR;
		    $images_dir =~ s/html\/img-tmp/images/;
	
		    $image_file =~ s/\/figures/$images_dir/;
		} else {
	        print STDERR "find_image_file: WARNING - crikey, no image file!!\n";
        }
    } else {
#        print STDERR "find_image_file: WARNING - for the current pathway, we have no figure.\n";
    }

    return $image_file;
}

# Finds the location of the vector graphics associated with the given
# pathway (the diagram).
#
# Makes the assumption that the current directory is:
#
# .../GKB/scripts/TheReactomeBook
#
# This is not very nice!!
sub find_vector_graphics_file {
    my ($self, $pathway) = @_;

    unless ($pathway) {
	#print STDERR "find_vector_graphics_file: WARNING - pathway is null!!!\n";
	return "";
    }

    # Lets see if there is an vector graphics associated with this pathway
    my $figure = $pathway->attribute_value("figure")->[0];
    my $vector_graphics_file = "";
    if ($figure) {
	# find the file associated with the figure
	$vector_graphics_file = $figure->attribute_value("url")->[0];
	if ($vector_graphics_file) {
	    if ($vector_graphics_file =~ /\./) {
		$vector_graphics_file =~ s/\..*$/.pdf/;
	    }
	    my $vector_graphics_dir = $GK_TMP_IMG_DIR . "/PDF";
	    $vector_graphics_dir =~ s/html\/img-tmp/images/;

	    $vector_graphics_file =~ s/\/figures/$vector_graphics_dir/;
	} else {
            #print STDERR "find_vector_graphics_file: WARNING - crikey, no vector graphics file name!!\n";
        }
    } else {
        #print STDERR "find_vector_graphics_file: WARNING - for the current pathway, we have no figure.\n";
    }

    # Make sure the file exists before passing its name on
    unless ( -e $vector_graphics_file) {
	$vector_graphics_file = "";
    }

    return $vector_graphics_file;
}

# Return a list of pathways corresponding to the supplied DB_IDs
#
# Arguments:
#
# dba - database adaptor for Reactome database
# ids - reference to an array of IDs
#
# Returns array of pathway instances
sub get_pathways_by_id {
    my ($self, $dba, $ids) = @_;

    my @pathways = ();

    foreach my $id (@{$ids}) {
	@pathways = (@pathways, @{$dba->fetch_instance_by_db_id($id)});
    }

    return @pathways;
}

# Return a list of all known pathways
#
# Arguments:
#
# dba - database adaptor for Reactome database
#
# Returns array of pathway instances
sub get_all_pathways {
    my ($self, $dba) = @_;

    # retrieve all pathways with components
    my $pathway_test_attribute;
    if ($dba->ontology->is_valid_class_attribute('Pathway','hasComponent')) {
	$pathway_test_attribute = 'hasComponent';
    } else {
	$pathway_test_attribute = 'hasEvent';
    }
    my $pathway_test_value = 'IS NOT NULL';
    my $pathway_class = 'Pathway';
    my @query = ([$pathway_test_attribute,$pathway_test_value,[]]);
    my $pathways = $dba->fetch_instance_by_remote_attribute($pathway_class, \@query);

    return @{$pathways};
}

# Return a list of all known top-level pathways.
# Will first look to see if there are any known front page pathways;
# if so, these will be returned.  Otherwise, returns a list of all
# sensible root pathways.
#
# Arguments:
#
# dba - database adaptor for Reactome database
#
# Returns array of top level pathway instances
sub get_top_level_pathways {
    my ($self, $dba) = @_;

    my @top_level_pathways = get_frontPageItem_pathways($self, $dba);

    if (!(@top_level_pathways) || scalar(@top_level_pathways)<1) {
	@top_level_pathways = get_root_pathways($self, $dba);
    }

    return @top_level_pathways;
}

# Return a list of top-level pathways as seen on the front
# page of the Reactome web site.
#
# Arguments:
#
# dba - database adaptor for Reactome database
#
# Returns array of top level pathway instances
sub get_frontPageItem_pathways {
    my ($self, $dba) = @_;

    # Check first to see if the database contains some hints about
    # which pathways ought to be at the top of the pecking order.
    my $frontpage_test_attribute = 'frontPageItem';
    my $frontpage_test_value = 'IS NOT NULL';
    my $frontpage_class = 'FrontPage';
    my @query = ([$frontpage_test_attribute,$frontpage_test_value,[]]);
    my $frontpages = $dba->fetch_instance_by_remote_attribute($frontpage_class, \@query);    

    # Loop over front pages until one is found containing pathways
    my $frontpage_pathways;
    my @top_level_pathways;
    foreach my $frontpage (@{$frontpages}) {
	if (!$frontpage) {
	    next;
	}

	$frontpage_pathways = $frontpage->attribute_value("frontPageItem");
	foreach my $frontpage_pathway (@{$frontpage_pathways}) {
	    if ($frontpage_pathway->is_a("Pathway")) {
		push(@top_level_pathways, $frontpage_pathway);
	    }
	}
	if (scalar(@top_level_pathways)>0) {
	    last;
	}
    }

    return @top_level_pathways;
}

# Return a list of pathways which are "root" in the sense that they
# are not contained by any other pathways.  Some filtering is done,
# based on pathway name, to keep the pathways chosen sensible.
#
# Arguments:
#
# dba - database adaptor for Reactome database
#
# Returns array of top level pathway instances
sub get_root_pathways {
    my ($self, $dba) = @_;

    my @top_level_pathways = ();

    # If there isn't anything useful in the database, do things the
    # hard way.
    my @pathways = get_all_pathways($self, $dba);

    # Loop over the pathways and keep only those which have
    # no referring pathway instances - these must be the "top-level"
    # or root pathways.
    my $top_level_pathway_count = 0;
    my $top_level_pathway_flag;
    my $referrers;
    my $name;
    foreach my $pathway (@pathways) {
	$referrers = $dba->fetch_referer_by_instance($pathway);
	$top_level_pathway_flag = 0;
	if (!$referrers) {
	    $top_level_pathway_flag = 1;
	} else {
	    $top_level_pathway_flag = 1;
	    foreach my $referrer (@{$referrers}) {
		if ($referrer->is_a("Pathway")) {
		    $top_level_pathway_flag = 0;
		    last;
		}
	    }
	}

	if ($top_level_pathway_flag == 1) {
	    # Throw out pathways that don't really belong at the
	    # top level
	    $name = $pathway->attribute_value("name")->[0];
	    if (
		# Sentence endings
		!($name=~/ catabolism$/) &&
		!($name=~/ elongation$/) &&
		!($name=~/ hydrolysis$/) &&
		!($name=~/ kinases$/) &&
		!($name=~/ pathway$/) &&
		!($name=~/ proteins$/) &&
		!($name=~/ recovery$/) &&
		# Sentence starts
		!($name=~/^Breakdown of /) &&
		!($name=~/^DNR /) &&
		!($name=~/^Formation of /) &&
		!($name=~/^Recruitment of /) &&
		# Forbidden words
		!($name=~/[Cc]omplex/) &&
		!($name=~/[Cc]ontainer/) &&
		!($name=~/[Pp]laceholder/) &&
		# Whole sentences
		$name ne "Chromosome Maintenance" &&
		$name ne "Eukaryotic Translation Termination" &&
		$name ne "Integration of pathways involved in energy metabolism" &&
		$name ne "Transcription"
		) {
		$top_level_pathways[$top_level_pathway_count] = $pathway;
		$top_level_pathway_count++;
	    }
	}
    }

    return @top_level_pathways;
}

sub mart_species_abbreviation {
    my ($sp) = @_;
    if ($sp->displayName =~ /^(\w)\w+ (\w+)$/) {
	return lc("$1$2");
    } else {
	confess("Can't form species abbreviation for mart from '" . $sp->displayName . "'");
    }
}

sub grep_for_instances_with_given_species {
    my ($iar,$sar) = @_;
    @{$sar} || return $iar;
    my (@out, %h);
    map {$h{$_->db_id} = $_} @{$sar};
    INSTANCE: foreach my $i (@{$iar}) {
	if ($i->is_valid_attribute('species')) {
	    foreach my $sp (@{$i->Species}) {
		if ($h{$sp->db_id}) {
		    push @out, $i;
		    next INSTANCE;
		}
	    }
	} else {
	    push @out, $i;
	}
    }
    return \@out;
}

sub get_Event_species_table {
    my $dba = shift;
    return ($dba->ontology->is_multivalue_class_attribute('Event','species'))
	? 'Event_2_species'
	: 'Event';
}

sub find_focus_species_or_default {
    my ($dba,$cgi,$instance) = @_;
#    cluck((caller(0))[3]);
    my $out = find_focus_species(@_);
    if (@{$out}) {
	return $out;
    }
    return [find_default_species($dba)];
=head
    $dba || confess("Need dba to fetch focus species.");
    my @focus_names;
    if (my $tmp = $dba->db_parameter('DEFAULT_SPECIES')) {
	@focus_names = ($tmp);
    } else {
	@focus_names = ('Homo sapiens');
    }
#    cluck(@focus_names);
#    print qq(<PRE>HERE: @focus_names </PRE>\n);
    $out = $dba->fetch_instance_by_attribute('Species',[['name',\@focus_names]]);
    @{$out} || confess("Couldn't fetch default species '@focus_names' from db.");
    return $out;
=cut
}

sub find_default_species {
    my $dba = shift;
    $dba || confess("Need dba to fetch focus species.");
    my $sp_name = $dba->db_parameter('DEFAULT_SPECIES') || 'Homo sapiens';
    return $dba->fetch_instance_by_attribute('Species',[['name',[$sp_name]]])->[0] 
	|| confess("Couldn't fetch default species '$sp_name' from db.");
}

sub find_focus_species1 {
    my ($dba,$cgi,$instance) = @_;
    my $out = [];
    if ($instance) {
	if ($instance->is_a('Species')) {
	    $out = [$instance];
	} elsif ($instance->is_valid_attribute('species') && $instance->is_instance_type_attribute('species') && $instance->Species->[0]) {
	    $out = $instance->Species;
	}
	@{$out} && return $out;
    }
    $dba || confess("Need dba to fetch focus species.");
    my @focus_names;
    if ($cgi && $cgi->param('FOCUS_SPECIES')) {
	@focus_names = $cgi->param('FOCUS_SPECIES');
    } elsif ($cgi && $cgi->cookie('FOCUS_SPECIES')) {
	@focus_names = ($cgi->cookie('FOCUS_SPECIES'));
    }
    if (@focus_names) {
	$out = $dba->fetch_instance_by_attribute('Species',[['name',\@focus_names]]);
    } else {
	my @focus_ids;
	if ($cgi && $cgi->param('FOCUS_SPECIES_ID')) {
	    @focus_ids= $cgi->param('FOCUS_SPECIES_ID');
	} elsif ($cgi && $cgi->cookie('FOCUS_SPECIES_ID')) {
	    @focus_ids = ($cgi->cookie('FOCUS_SPECIES_ID'));
	}
	$out = $dba->fetch_instance_by_attribute('Species',[[$GKB::Ontology::DB_ID_NAME,\@focus_ids]]);
    }
    return $out;
}

sub find_focus_species {
    my ($dba,$cgi,$instance) = @_;
    my $out = [];
    if ($instance) {
		if ($instance->is_a('Species')) {
		    $out = [$instance];
		} elsif ($instance->is_valid_attribute('species') && $instance->is_instance_type_attribute('species') && $instance->Species->[0]) {
		    push @{$out}, $instance->Species->[0];
		}
		@{$out} && return $out;
    }
    $dba || confess("Need dba to fetch focus species.");
    my $focus_name;
    if ($cgi && $cgi->param('FOCUS_SPECIES')) {
		$focus_name = $cgi->param('FOCUS_SPECIES');
    } elsif ($cgi && $cgi->cookie('FOCUS_SPECIES')) {
		$focus_name = ($cgi->cookie('FOCUS_SPECIES'));
    }
    
    $out = $dba->fetch_instance_by_attribute('Species',[['name',[$focus_name]]]);
    
    return $out;
}

sub find_focus_species_by_id {
    my ($dba,$cgi,$instance) = @_;
    if ($instance) {
	if ($instance->is_a('Species')) {
	    return $instance;
	} elsif ($instance->is_valid_attribute('species') && $instance->is_instance_type_attribute('species') && $instance->Species->[0]) {
	    return $instance->Species->[0];
	}
    }
    $dba || confess("Need dba to fetch focus species.");
    my $focus_id;
    if ($cgi && $cgi->param('FOCUS_SPECIES_ID')) {
	$focus_id = $cgi->param('FOCUS_SPECIES_ID');
	if (my $sp = $dba->fetch_instance_by_attribute('Species',[[$GKB::Ontology::DB_ID_NAME,[$focus_id]]])->[0]) {
	    return $sp;
	}
    }
    return find_default_species($dba);
}


sub find_focus_species_names {
    my ($dba,$cgi,$instance) = @_;
    my $sps = find_focus_species($dba,$cgi,$instance);
    my @out = map {$_->displayName} @{$sps};
    return \@out;
}

sub print_values_according_to_instructions1 {
    my ($instances,$instructions,$instance_end,$attribute_separator,$value_separator) = @_;
    return "#NO VALUES\n" unless(@{$instances});
    $instance_end = "\n" unless (defined $instance_end);
    $attribute_separator = "\t" unless (defined $attribute_separator);
    $value_separator = '|' unless (defined $value_separator);
    my @instructions;
    foreach my $str (@{$instructions}) {
	my @tmp;
	foreach my $str2 (split(/\./,$str)) {
	    my ($att,$idx) = split(/[\[\]]/,$str2);
	    push @tmp, [$att,$idx];
	}
	push @instructions, \@tmp;
    }
    print '#', join("$attribute_separator#", @{$instructions}), $instance_end;

    foreach my $i (@{$instances}) {
	for my $j (0 .. $#instructions) {
	    my $ar = $instructions[$j];
	    my @is = ($i);
	    my @out;
	    foreach my $ar2 (@{$ar}) {
		my ($att,$idx) = @{$ar2};
		my @tmp;
		foreach my $i2 (@is) {
		    if ($i2->is_valid_attribute($att)) {
			if ($i2->is_instance_type_attribute($att)) {
			    push @tmp, (defined $idx) ?
				$i2->attribute_value($att)->[$idx]:
				@{$i2->attribute_value($att)};
			} else {
			    push @out, (defined $idx) ?
				$i2->attribute_value($att)->[$idx]:
				@{$i2->attribute_value($att)};
			}
		    }
		}
		@is = @tmp;
	    }
	    print join($value_separator, @out, map {$_->class_and_id} @is);
	    if ($j < $#instructions) {
		print $attribute_separator;
	    }
	}
	print $instance_end;
    }
}

sub print_values_according_to_instructions {
    my ($instances,$instructions,$instance_start,$instance_end,$attribute_separator,$value_separator) = @_;
    $instance_start = '' unless (defined $instance_start);
    $instance_end = "\n" unless (defined $instance_end);
    $attribute_separator = "\t" unless (defined $attribute_separator);
    $value_separator = '|' unless (defined $value_separator);
    unless (@{$instances}) {
	print $instance_start, "#NO VALUES", $instance_end;
	return;
    }
    my @instructions;
    foreach my $str (@{$instructions}) {
	my @tmp;
	foreach my $str2 (split(/\./,$str)) {
	    my ($str3,$idx) = split(/[\[\]]/,$str2);
	    my ($att,$cls) = split(/:/,$str3);
	    push @tmp, [$att,$cls,$idx];
	}
	push @instructions, \@tmp;
    }
    if ($instance_start =~ /<TD>/i) {
	print qq(<TR><TH>), join('</TH><TH>', @{$instructions}), qq(</TH></TR>\n);
    } else {
	print $instance_start, '#', join("$attribute_separator#", @{$instructions}), $instance_end;
    }
    my %seen;
    foreach my $i (@{$instances}) {
	my $out = $instance_start;
	for my $j (0 .. $#instructions) {
	    my $ar = $instructions[$j];
	    my @is = ($i);
	    my @out;
	    foreach my $ar2 (@{$ar}) {
		my ($att,$cls,$idx) = @{$ar2};
		my @tmp;
		foreach my $i2 (@is) {
		    defined $i2 || next;
		    if ($cls && $i2->is_valid_reverse_attribute($att) 
			&& $i2->ontology->is_valid_class_attribute($cls,$att)) {
			my $ar3 = $i2->dba->fetch_instance_by_attribute($cls,[[$att,[$i2->db_id]]]);
			push @tmp, (defined $idx) ? $ar3->[$idx]: @{$ar3};
		    } elsif ($i2->is_valid_attribute($att)) {
			if ($i2->is_instance_type_attribute($att)) {
			    push @tmp, (defined $idx) ?
				$i2->attribute_value($att)->[$idx]:
				@{$i2->attribute_value($att)};
			} else {
			    push @out, (defined $idx) ?
				$i2->attribute_value($att)->[$idx]:
				@{$i2->attribute_value($att)};
			}
		    }
		}
		@is = @tmp;
	    }
	    $out .= join($value_separator, @out, map {$_->class_and_id} @is);
	    if ($j < $#instructions) {
		$out .= $attribute_separator;
	    }
	}
	$out .= $instance_end;
	unless ($seen{$out}++) {
	    print $out;
	}
    }
}

# Generates a dateTime version of the supplied string.
#
# Argument: string of format:
#
# YYYYMMDDhhmmss
#
# (Year, Month, Day, Hour, Minute, Second)
#
# Returns a reference to a list with 6 elements:
#
# (Year, Month, Day, Hour, Minute, Second)
# If the string is invalid, returns undef.
sub string_to_dateTime {
    my ($string) = @_;
    
    if (!$string) {
    	return undef;
    }
    
    if (length($string)!=14) {
    	return undef;
    }
    
    my $year = substr($string, 0, 4);
    my $month = substr($string, 4, 2);
    my $day = substr($string, 6, 2);
    my $hour = substr($string, 8, 2);
    my $minute = substr($string, 10, 2);
    my $second = substr($string, 12, 2);
    
    my @dateTime = ($year, $month, $day, $hour, $minute, $second);
    
    return \@dateTime;
}

# Generates a String version of the supplied date/time.
#
# Argument: reference to a list with 6 elements:
#
# (Year, Month, Day, Hour, Minute, Second)
#
# Returns string of format: YYYYMMDDhhmmss
# If one of the list elements was empty, returns undef.
sub dateTime_to_string {
    my ($dateTime) = @_;
    
    if (!$dateTime) {
    	return undef;
    }
    
    my $year = $dateTime->[0];
    my $month = $dateTime->[1];
    my $day = $dateTime->[2];
    my $hour = $dateTime->[3];
    my $minute = $dateTime->[4];
    my $second = $dateTime->[5];
    
#    print STDERR "dateTime_to_string: year=$year, month=$month, day=$day, hour=$hour, minute=$minute, second=$second\n";
    
    my $string = "";
    if ($year) {
    	if (length($year)==2) {
    		$string .= "19" . $year;
    	} elsif (length($year)==4) {
    		$string .= $year;
    	} else {
    		if (length($year)==3) {
    			$string .= 1900 + $year;
    		} else {
    			return undef;
    		}
    	}
    } else {
    	return undef;
    }
    if ($month) {
    	if (length($month)==1) {
    		$string .= "0" . $month;
    	} elsif (length($month)==2) {
    		$string .= $month;
    	} elsif ($month eq "Jan" || $month eq "January") {
    		$string .= "01";
    	} elsif ($month eq "Feb" || $month eq "February") {
    		$string .= "02";
    	} elsif ($month eq "Mar" || $month eq "March") {
    		$string .= "03";
    	} elsif ($month eq "Apr" || $month eq "April") {
    		$string .= "04";
    	} elsif ($month eq "May") {
    		$string .= "05";
    	} elsif ($month eq "Jun" || $month eq "June") {
    		$string .= "06";
    	} elsif ($month eq "Jul" || $month eq "July") {
    		$string .= "07";
    	} elsif ($month eq "Aug" || $month eq "August") {
    		$string .= "08";
    	} elsif ($month eq "Sep" || $month eq "September") {
    		$string .= "09";
    	} elsif ($month eq "Oct" || $month eq "October") {
    		$string .= "10";
    	} elsif ($month eq "Nov" || $month eq "November") {
    		$string .= "11";
    	} elsif ($month eq "Dec" || $month eq "December") {
    		$string .= "12";
    	} else {
    		return undef;
    	}
    } else {
    	return undef;
    }
    if ($day) {
    	if (length($day)==1) {
    		$string .= "0" . $day;
    	} elsif (length($day)==2) {
    		$string .= $day;
    	}
    } else {
    	return undef;
    }
    if ($hour) {
    	if ($hour>24) {
    		return undef;
    	}
    	if (length($hour)==1) {
    		$string .= "0" . $hour;
    	} elsif (length($hour)==2) {
    		$string .= $hour;
    	}
    } else {
    	return undef;
    }
    if ($minute) {
    	if ($minute>60) {
    		return undef;
    	}
    	if (length($minute)==1) {
    		$string .= "0" . $minute;
    	} elsif (length($minute)==2) {
    		$string .= $minute;
    	}
    } else {
    	return undef;
    }
    if ($second) {
    	if ($second>60) {
    		return undef;
    	}
    	if (length($second)==1) {
    		$string .= "0" . $second;
    	} elsif (length($second)==2) {
    		$string .= $second;
    	}
    } else {
    	return undef;
    }
}

# Compare two date/time lists.
#
# Arguments: 2 references to a strings of format:
#
# YYYYMMDDhhmmss
#
# Returns 0 if string1 > string2, 1 otherwise.
# Also returns 0 if either dateTime1 or dateTime2 are undef.
sub compare_dateTime {
    my ($string1, $string2) = @_;
    
    my $clean_dateTime1 = string_to_dateTime($string1);
    my $clean_dateTime2 = string_to_dateTime($string2);
    
    if (!$clean_dateTime1 || !$clean_dateTime2) {
    	# This is rather arbitrary...
    	return 0;
    }
    
    if ($clean_dateTime1->[0] > $clean_dateTime2->[0]) {
    	return 0;
    }
    if ($clean_dateTime1->[1] > $clean_dateTime2->[1]) {
    	return 0;
    }
    if ($clean_dateTime1->[2] > $clean_dateTime2->[2]) {
    	return 0;
    }
    if ($clean_dateTime1->[3] > $clean_dateTime2->[3]) {
    	return 0;
    }
    if ($clean_dateTime1->[4] > $clean_dateTime2->[4]) {
    	return 0;
    }
    if ($clean_dateTime1->[5] > $clean_dateTime2->[5]) {
    	return 0;
    }
    
    return 1;
}

sub fetch_top_level_events_for_species {
    my ($dba,$sp) = @_;
    my @query = (
	['species','=',[$sp->db_id]],
        ['_class','!=',['Reaction','BlackBoxEvent','Polymerisation','Depolymerisation']],
#        ['hasComponent:Pathway','IS NULL',[]]
    );
    if ($dba->ontology->is_valid_class_attribute('Pathway','hasComponent')) {
	push @query, ['hasComponent:Pathway','IS NULL',[]];
    }
    if ($dba->ontology->is_valid_class_attribute('Pathway','hasEvent')) {
	push @query, ['hasEvent:Pathway','IS NULL',[]];
    }
    if ($dba->ontology->is_valid_class_attribute('Reaction','hasMember')) {
	push @query, ['hasMember:Reaction','IS NULL',[]];
    }
    if ($dba->ontology->is_valid_class_attribute('EquivalentEventSet','hasMember')) {
	push @query, ['hasMember:EquivalentEventSet','IS NULL',[]];
    }
    if ($dba->ontology->is_valid_class_attribute('ConceptualEvent','hasSpecialisedForm')) {
        push @query, ['hasSpecialisedForm:ConceptualEvent','IS NULL',[]];
    }
    if ($dba->ontology->is_valid_class_attribute('BlackBoxEvent','hasComponent')) {
        push @query, ['hasComponent:BlackBoxEvent','IS NULL',[]];
    }
    my $ar = $dba->fetch_instance_by_remote_attribute('Event',\@query);
    return $ar;
}

sub fisher_yates_shuffle {
    my $array = shift;
    @{$array} || return;
    my $i;
    for ($i = @$array; --$i; ) {
        my $j = int rand ($i+1);
        next if $i == $j;
        @$array[$i,$j] = @$array[$j,$i];
    }
    return $array;
}

sub escape {
    $_[0] =~ s/&/&amp;/sg;
    $_[0] =~ s/</&lt;/sg;
    $_[0] =~ s/>/&gt;/sg;
    $_[0] =~ s/"/&quot;/sg;
    $_[0] =~ s/'/&apos;/sg;
    $_[0] =~ s/\342\200\234/&quot;/sg;
    $_[0] =~ s/\342\200\235/&quot;/sg;
    $_[0] =~ s/\342\200\231/&quot;/sg;
    return $_[0];
}

sub is_top_level_pathway {
    my $i = shift;
    if ($i->is_a('Pathway') && 
	! $i->reverse_attribute_value('hasEvent')->[0] &&
	! $i->reverse_attribute_value('hasMember')->[0]) {
	return 1;
    }
    return;
}

# Adds information to a logfile.  The first argument is a string to be
# added to the logfile.  For legibility and ease of parsing, it is
# recommended that this doesn't contain any newlines.
#
# The second
# argument is optional, and specifies the name of the file into
# which logs are written.  If not specified, then the default "general"
# will be used.
#
# If the file contains forward slashes ("/"), then it is
# assumed to be a full path to the logfile, and it overwrites the
# default path.  Otherwise, the default path will be used, $LOG_DIR,
# obtained from Config.pm.
#
# Logging will be switched off if $LOG_DIR is undef *and* the file
# cannot be interpreted as a path.
sub log {
    return;
	my ($string, $file) = @_;
	
	my $dir = undef;
	if (defined $file && $file =~ /\//) {
		# Assume file contains path
		$file =~ /^(.*)\/[^\/]$/;
		$dir = $1;
	}
	if (!(defined $dir)) {
		if (defined $LOG_DIR) {
			$dir = $LOG_DIR;
		} else {
			return;
		}
	}
#	if (!(defined $dir)) {
#		if (defined $GK_ROOT_DIR) {
#			$dir = "$GK_ROOT_DIR/log";
#		} else {
#			$dir = ".";
#		}
#	}
	my $suffix = "log";
	if (defined $file) {
		if ($file =~ /\.([^\.]+)$/) {
			$suffix = $1;
			$file =~ s/\.[^\.]+$//;
		}
	} else {
		$file = "general";
	}
	
	my $path = "$dir/$file.$suffix";
	
	my $log = Log::Handler->new(filename => $path, mode => 'append', newline  => 1, maxlevel => 7, minlevel => 0);
	$log->info($string);
}

# Gets an InstanceEdit instance for the user currently running
# Perl.
# Arguments:
#
# dba - database adaptor
#
# Returns a InstanceEdit instance.
my $instance_edit_for_effective_user = undef;
sub get_instance_edit_for_effective_user {
	my ($dba) = @_;
	
	if (defined $instance_edit_for_effective_user) {
		return $instance_edit_for_effective_user;
	}

	eval {
		$instance_edit_for_effective_user = $dba->create_InstanceEdit_for_effective_user();
	};
	
	if (!(defined $instance_edit_for_effective_user)) {
		# For some reason, no instance edit could be created for the
		# current user.  Use a croft instance edit instead.
		my $author = get_person( $dba, 'Croft', 'D' );
		$instance_edit_for_effective_user = GKB::Instance->new(
			-ONTOLOGY => $dba->ontology,
			-CLASS    => 'InstanceEdit'
		);
		$instance_edit_for_effective_user->inflated(1);
		$instance_edit_for_effective_user->author($author);
		$dba->store($instance_edit_for_effective_user);
	}
	
	return $instance_edit_for_effective_user;
}

# Get a person, given initials and surname.
# Arguments:
#
# dba - database adaptor
# myName - surname
# myInitial - initial
#
# Returns a Person instance.
sub get_person {
	my ($dba, $myName, $myInitial) = @_;

	# Get a list of all persons with the given name and initial
	my @query = ( [ 'surname', '=', [$myName] ], [ 'initial', '=', [$myInitial] ] );
	my $persons = $dba->fetch_instance_by_remote_attribute( 'Person', \@query );
	my $person = undef;
	if (defined $persons && scalar(@{$persons})>0) {
		$person = $persons->[0];
		$person->inflate();
	}

	unless (defined $person) {
		$person = GKB::Instance->new(
			-ONTOLOGY => $dba->ontology,
			-CLASS    => 'Person'
		);

		$person->inflated(1);
		$person->Surname($myName);
		$person->Initial($myInitial);
		$person->Firstname($myInitial);

		my @query = ( [ 'name', '=', ['EBI'] ] ); # TODO: this needs to be generalized
		my $affiliations = $dba->fetch_instance_by_remote_attribute( 'Affiliation', \@query );
		if (defined $affiliations && scalar(@{$affiliations})>0) {
			$person->Affiliation($affiliations->[0]);
		}

		$dba->store($person);
	}

	return $person;
}

# Creates a reference database instance.
# Arguments:
#
# dba - database adaptor
#
# Returns a ReferenceDatabase instance.
my %reference_database_cache = ();
sub get_reference_database {
	my ($dba, $name, $url, $access_url) = @_;

	# Is the ReferenceDatabase instance already in cache?
	# Can speed things up a lot if this subroutine is called
	# frequently.
	if ( defined $reference_database_cache{$name} ) {
		return $reference_database_cache{$name};
	}

	# Look to see if there is a ReferenceDatabase class entry
	# with the given name already in the database
	my @query = ( [ 'name', '=', [$name] ] );
	my $reference_databases = $dba->fetch_instance_by_remote_attribute('ReferenceDatabase', \@query);

	my $reference_database = undef;
	if (defined $reference_databases && scalar(@{$reference_databases})>0) {
		$reference_database = $reference_databases->[0];
		$reference_database->inflate();
	}

	# Create a new ReferenceDatabase class entry
	# if one does not already exist
	if ( !( defined $reference_database ) ) {
		$reference_database = GKB::Instance->new(-ONTOLOGY => $dba->ontology, -CLASS    => 'ReferenceDatabase');

		$reference_database->inflated(1);
		$reference_database->created(get_instance_edit_for_effective_user($dba));
		$reference_database->Name($name);
		$reference_database->Url($url);
		$reference_database->AccessUrl($access_url);
		$dba->store($reference_database);
	}
	$reference_database_cache{$name} = $reference_database;

	return $reference_database;
}

# Gets a ReferenceDatabase instance for KEGG genes.
# Arguments:
#
# dba - database adaptor
#
# Returns a ReferenceDatabase instance.
sub get_reference_database_kegg {
	my ($dba) = @_;

	return get_reference_database($dba, 'KEGG Gene', "http://www.genome.jp/", "http://www.genome.jp/dbget-bin/www_bget?hsa+###ID###" );
}

# Gets a ReferenceDatabase instance for EC enzymes.
# Arguments:
#
# dba - database adaptor
#
# Returns a ReferenceDatabase instance.
sub get_reference_database_ec {
	my ($dba) = @_;

	return get_reference_database($dba, 'EC',"http://ca.expasy.org/enzyme/","http://ca.expasy.org/enzyme/###ID###");
}

# Gets a ReferenceDatabase instance for IntEnz.
# Arguments:
#
# dba - database adaptor
#
# Returns a ReferenceDatabase instance.
sub get_reference_database_intenz {
	my ($dba) = @_;

	return get_reference_database($dba, 'IntEnz',"http://www.ebi.ac.uk/intenz/","http://www.ebi.ac.uk/intenz/query?cmd=SearchEC&ec=###ID###");
}

# Gets a ReferenceDatabase instance for Brenda.
# Arguments:
#
# dba - database adaptor
#
# Returns a ReferenceDatabase instance.
sub get_reference_database_brenda {
	my ($dba) = @_;

	return get_reference_database($dba, 'Brenda',"http://www.brenda-enzymes.info/index.php4","http://www.brenda-enzymes.info/php/result_flat.php4?ecno=###ID###");
}

# Gets a ReferenceDatabase instance for IntAct.
# Arguments:
#
# dba - database adaptor
#
# Returns a ReferenceDatabase instance.
sub get_reference_database_intact {
	my ($dba) = @_;

	return get_reference_database($dba, 'IntAct',"http://www.ebi.ac.uk/intact","http://www.ebi.ac.uk/intact/search/do/search?searchString=###ID###&filter=ac");
}

# Get a DatabaseIdentifier instance, given reference database and ID
# Arguments:
#
# dba - database adaptor
# reference_database - ReferenceDatabase instance
# id - valid ID for the ReferenceDatabase
#
# Returns a DatabaseIdentifier instance.
sub get_database_identifier {
	my ($dba, $reference_database, $id) = @_;
	
	# Look to see if we can find the ReferenceDatabase entry in
	# the database already
	my @query = (
		[ 'identifier',                     '=', [$id] ],
		[ 'referenceDatabase._displayName', '=', [$reference_database->_displayName->[0]] ]
	);
	my $instances = $dba->fetch_instance_by_remote_attribute('DatabaseIdentifier', \@query);

	my $instance = undef;
	if (defined $instances && defined $instances->[0]) {
		$instance = $instances->[0];
		$instance->inflate();
	} else {
		$instance = GKB::Instance->new(
			-ONTOLOGY => $dba->ontology,
			-CLASS    => 'DatabaseIdentifier'
		);
		$instance->inflated(1);
		$instance->created(get_instance_edit_for_effective_user($dba));
		$instance->identifier($id);
		$instance->referenceDatabase($reference_database);
		$dba->store($instance);
	}

	return $instance;
}

# Depending on the data model being used, the Reactome instance class
# used for holding proteins could either be 'ReferencePeptideSequence'
# (old data model, pre March 2009) or 'ReferenceGeneProduct' (new data
# model).  This subroutine returns the appropriate class name.
sub get_reference_protein_class {
    require GKB::ReactomeDBAdaptor;
    my ($dba) = @_;
    my $reference_peptide_sequence_class = 'ReferenceGeneProduct';
    if (!($dba->ontology->is_valid_class($reference_peptide_sequence_class))) {
	$reference_peptide_sequence_class = 'ReferencePeptideSequence';
    }
    return $reference_peptide_sequence_class;
}   

1;
