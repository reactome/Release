package GKB::IntAct;

=head1 NAME

GKB::IntAct

=head1 SYNOPSIS

Methods for querying the IntAct protein-protein interaction
database.

=head1 DESCRIPTION


=head1 SEE ALSO

GKB::Utils

=head1 AUTHOR

David Croft E<lt>croft@ebi.ac.ukE<gt>

Copyright (c) 2007 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

use vars qw(@ISA $AUTOLOAD %ok_field);
use strict;
use Bio::Root::Root;
use Data::CTable;
use Data::Dumper;
use List::Compare;
use LWP::UserAgent;
use GKB::Config;
use SOAP::Lite;

@ISA = qw(Bio::Root::Root);

# Make a note of local variable names
for my $attr
    (qw(
	file
	ctable
	uniprot_to_selection_hash
	uniprot2_to_selection_hash
	interactor_a_hash
	interactor_b_hash
	interaction_identifiers
	find_binary_interactions_call
	intact_soap
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

# Create a new instance of this class
# Is this the right thing to do for a subclass?
sub new {
    my($pkg, @args) = @_;
    my $self = bless {}, $pkg;
    
    my %uniprot_to_selection_hash = ();
    $self->uniprot_to_selection_hash(\%uniprot_to_selection_hash);
    
    my %uniprot2_to_selection_hash = ();
    $self->uniprot2_to_selection_hash(\%uniprot2_to_selection_hash);
    
    $self->find_binary_interactions_call(undef);
    $self->intact_soap(undef);
    
    return $self;
}

# Set the path to the PSIMITAB interaction file from
# IntAct.  This can be obtained from:
#
# ftp://ftp.ebi.ac.uk/pub/databases/intact/current/psimitab/intact.txt
#
# Also reads in the data from this file and sets up the
# Data::CTable used to access this data.
sub set_file {
    my ($self, $file) = @_;
    
    $self->file($file);

    my %params = ('_FDelimiter' => "\t");

	print STDERR "IntAct.set_file: create ctable, with file=$file\n";
	
    my $ctable = Data::CTable->new(\%params, $file);
    $self->ctable($ctable);
    
    my $interaction_identifiers = $ctable->col("Interaction identifier(s)");
    $self->interaction_identifiers($interaction_identifiers);

	print STDERR "IntAct.set_file: all done\n";
}

# Gets the path to the PSIMITAB interaction file.  If this
# file has not been set already, then try to get the file from
# the IntAct FTP site and put it into a temporary directory -
# in the worst case, this will be /tmp.
sub get_file {
    my ($self) = @_;
    
    my $file = $self->file;
    
    print STDERR "IntAct.get_file: file=$file\n";
    
    if (!(defined $file)) {
	    my $url = 'ftp://ftp.ebi.ac.uk/pub/databases/intact/current/psimitab/intact.txt';
	    my $content = $self->get_url_content($url);
	    if (defined $content) {
	    	my $dir = $GK_TMP_IMG_DIR;
	    	if (!(defined $dir) || $dir eq '' || !(-w $dir)) {
	    		$dir = "/tmp";
	    	}
	    	$file = "$dir/intact.txt";
	    	
	    	print STDERR "IntAct.get_file: NEW file=$file\n";
	    	
	    	if (open(FILE, ">$file")) {
	    		print FILE $content;
	    		close(FILE);
	    		
	    		$self->set_file($file);
	    	} else {
	    		print STDERR "IntAct.get_file: could not open $file\n";
	    	}
	    } else {
	    	print STDERR "IntAct.get_file: could not get intact.txt from IntAct FTP site\n";
	    }
    }
    
    return $file;
}

sub get_url_content {
    my ($self, $url) = @_;
    
    my $content = undef;
    my $ua = LWP::UserAgent->new();
    my $response = $ua->get($url);
    if (defined $response) {
        $content =  $response->content;
    }
    
	return $content;
}

# Given a pair of UniProt IDs, will attempt to get the corresponding
# IntAct IDs.
sub find_intact_ids_for_uniprot_pair {
    my ($self, $uniprot1, $uniprot2) = @_;
    
	my $ctable = $self->ctable;
	my $field1 = "ID interactor A";
	my $field2 = "ID interactor B";
	my $col1 = $ctable->col($field1);
	my $col2 = $ctable->col($field2);
	my $uniprot1_string = "uniprotkb:$uniprot1";
	my $uniprot2_string = "uniprotkb:$uniprot2";
	my $row_num;
	my $selection;
	my $selection2 = $self->uniprot2_to_selection_hash->{$uniprot1}->{$uniprot2};
	if (defined $selection2) {
		# TODO: just occasionally, cached selections don't
		# work, I don't know why.  Anyway, things go missing
		# rather than being spuriously added, so I have
		# decided to live with it to gain the speed advantages.
#		print "IntAct.find_intact_ids_for_uniprot_pair: using existing FIRST uniprot1, uniprot2 selection\n";
	} else {
		# Get cached selection, if it exists
		$selection = $self->uniprot_to_selection_hash->{$uniprot1};
		
		if (defined $selection) {
			# TODO: just occasionally, cached selections don't
			# work, I don't know why.  Anyway, things go missing
			# rather than being spuriously added, so I have
			# decided to live with it to gain the speed advantages.
#			print "IntAct.find_intact_ids_for_uniprot_pair: using existing FIRST uniprot1 selection\n";
		} else {
			$selection = [grep {$col1->[$_] eq $uniprot1_string} @{$ctable->all()}];
			
			if (scalar(@{$selection})>0) {
				$self->uniprot_to_selection_hash->{$uniprot1} = $selection;
				my %tmp = ();
				$self->uniprot2_to_selection_hash->{$uniprot1} = \%tmp;
			}
		}
	
		$selection2 = [grep {$col2->[$_] eq $uniprot2_string} @{$selection}];

		if (scalar(@{$selection2})>0) {
			$self->uniprot2_to_selection_hash->{$uniprot1}->{$uniprot2} = $selection2;
		}
	}

	# If we can't find an interaction matching A,B, try B,A.
	if (scalar(@{$selection2})<1) {
		$selection2 = $self->uniprot2_to_selection_hash->{$uniprot2}->{$uniprot1};
		
		if (defined $selection2) {
			# TODO: just occasionally, cached selections don't
			# work, I don't know why.  Anyway, things go missing
			# rather than being spuriously added, so I have
			# decided to live with it to gain the speed advantages.
#			print "IntAct.find_intact_ids_for_uniprot_pair: using existing SECOND uniprot1, uniprot2 selection\n";
		} else {
			$selection = $self->uniprot_to_selection_hash->{$uniprot2};
			if (defined $selection) {
				# TODO: just occasionally, cached selections don't
				# work, I don't know why.  Anyway, things go missing
				# rather than being spuriously added, so I have
				# decided to live with it to gain the speed advantages.
#				print "IntAct.find_intact_ids_for_uniprot_pair: using existing SECOND uniprot1 selection\n";
			} else {
				$selection = [grep {$col1->[$_] eq $uniprot2_string} @{$ctable->all()}];
			
				if (scalar(@{$selection})>0) {
					$self->uniprot_to_selection_hash->{$uniprot2} = $selection;
					my %tmp = ();
					$self->uniprot2_to_selection_hash->{$uniprot2} = \%tmp;
				}
			}
	
			$selection2 = [grep {$col2->[$_] eq $uniprot1_string} @{$selection}];

			if (scalar(@{$selection2})>0) {
				$self->uniprot2_to_selection_hash->{$uniprot2}->{$uniprot1} = $selection2;
			}
		}
	}
	
	my $interaction_identifiers = $ctable->col("Interaction identifier(s)");
	my %intact_ids = ();
	my @temp_intact_ids;
	my $intact_id_string;
	my $intact_id;
	foreach $row_num (@{$selection2}) {
		$intact_id_string = $interaction_identifiers->[$row_num];
		
		@temp_intact_ids = split(/\|/, $intact_id_string);
		foreach $intact_id (@temp_intact_ids) {
			$intact_id =~ s/^intact://;
			$intact_ids{$intact_id} = $intact_id;
		}
	}
	
	my @intact_id_array = sort(keys(%intact_ids));
	
	return @intact_id_array;
}

# Assume that the supplied CTable column contains
# interactor IDs and create a hash mapping interactor
# IDs to the table lines in which they appear.
sub create_interactor_hash {
    my ($self, $col) = @_;
    
	my $ctable = $self->ctable;
    my %interactor_hash = ();
    my $col_length = $ctable->length();
    
    print STDERR "IntAct.create_interactor_hash: col_length=$col_length\n";
    
    my $interactor;
    my $i;
    for ($i=0; $i<$col_length; $i++) {
    	if ($i%1000 == 0) { # diagnostic
    		print STDERR "IntAct.create_interactor_hash: i=$i\n";
    	}
    	$interactor = $col->[$i];
    	if (!(defined $interactor_hash{$interactor})) {
    		$interactor_hash{$interactor} = [grep {$col->[$_] eq $interactor} @{$ctable->all()}];
    	}
#
#
#
#
#
#
#		# Doing this for testing only, delete ASAP!!
#		if ($i == 3) {
#			$interactor_hash{"Q9UM13"} = [$i];
#			$interactor_hash{"P24941"} = [$i];
#			last;
#		}
#		
#		
#		
#		
#		
#		
#		
    }
    
    return \%interactor_hash;
}

# Create hashes for the interactor columns
sub create_interactor_hashes {
    my ($self) = @_;
    
	my $ctable = $self->ctable;
	my $field1 = "ID interactor A";
	my $field2 = "ID interactor B";
	my $col1 = $ctable->col($field1);
	my $col2 = $ctable->col($field2);
	
	my $interactor_a_hash = $self->retrieve_hash("interactor_a_hash");
	if (!(defined $interactor_a_hash)) {
		print STDERR "IntAct.create_interactor_hashes: doing col1\n";
		$interactor_a_hash = $self->create_interactor_hash($col1);
		# Cache this hash for future runs of this script
		$self->store_hash($interactor_a_hash, "interactor_a_hash");
	}
	$self->interactor_a_hash($interactor_a_hash);
	
	my $interactor_b_hash = $self->retrieve_hash("interactor_b_hash");
	if (!(defined $interactor_b_hash)) {
		print STDERR "IntAct.create_interactor_hashes: doing col1\n";
		$interactor_b_hash = $self->create_interactor_hash($col2);
		# Cache this hash for future runs of this script
		$self->store_hash($interactor_b_hash, "interactor_b_hash");
	}
	$self->interactor_b_hash($interactor_b_hash);
}

# Store the contents of the hash reference in the
# given filename.  If possible, put this file in
# the directory where the IntAct file is stored,
# otherwise put it into /tmp.
sub store_hash {
    my ($self, $hash, $filename) = @_;
    
    
    my $file = $self->get_file();
    
    print STDERR "IntAct.store_hash: filename=$filename\n";
    print STDERR "IntAct.store_hash: file=$file\n";
    
    $file =~ /(.+)\/[^\/]+$/;
    my $dir = $1;
    if (!(-w $dir)) {
    	$dir = "/tmp";
    }
    my $path = "$dir/$filename";
    my @keys = sort(keys(%{$hash}));
    my $key;
    my @line_nums;
    my $i;
    open(FILE, ">$path");
    foreach $key (@keys) {
    	print FILE "$key	";
    	@line_nums = @{$hash->{$key}};
    	for ($i=0; $i<scalar(@line_nums); $i++) {
    		if ($i>0) {
    			print FILE ",";
    		}
    		print FILE $line_nums[$i];
    	}
    	print FILE "\n";
    }
    close(FILE);
}

# Retrieve the contents of the hash reference from the
# given filename.  If possible, get this file from
# the directory where the IntAct file is stored,
# otherwise get it from /tmp.
sub retrieve_hash {
    my ($self, $filename) = @_;
    
    print STDERR "IntAct.retrieve_hash: filename=$filename\n";
    
    my $file = $self->get_file();
    $file =~ /(.+)\/[^\/]+$/;
    my $dir = $1;
    my $path = "$dir/$filename";
    if (!(-e $path)) {
    	my $path = "/tmp/$filename";
    }
    
    print STDERR "IntAct.retrieve_hash: path=$path\n";
    
    my $hash = undef;
    my $vars;
    if (-e $path) {
    	
    	print STDERR "IntAct.retrieve_hash: found file!\n";
    	
    	my %h = ();
    	my $key;
    	my $line_num_string;
    	if (open(FILE, $path)) {
	    	while (<FILE>) {
	    		($vars) = split(/\n/, $_); # get rid of trailing newlines
	    		($key, $line_num_string) = split(/\t/, $vars);
	    		my @line_nums = split(/,/, $line_num_string);
	    		$h{$key} = \@line_nums;
	    	}
	    	close(FILE);
    	} else {
    		print STDERR "IntAct.retrieve_hash: WARNING - could not open $path\n";
    	}
    	$hash = \%h;
    }
    
    return $hash;
}

# Given a pair of UniProt IDs, will attempt to get the corresponding
# IntAct IDs.
#
# Uses hashes of interactor IDs.  Computing these hashes is slow,
# so this method is not efficient if you are only dealing with
# a small number of interactions.  It could be better for large numbers
# of interactions though. 
sub find_intact_ids_for_uniprot_pair_using_hashes {
    my ($self, $uniprot1, $uniprot2) = @_;
    
    print STDERR "IntAct.find_intact_ids_for_uniprot_pair_using_hashes: uniprot1=$uniprot1, uniprot2=$uniprot2\n";
    
    $self->get_file(); # make sure we have interaction file and init ctable

    if (!(defined $self->interactor_a_hash) || !(defined $self->interactor_b_hash)) {
    	print STDERR "IntAct.find_intact_ids_for_uniprot_pair_using_hashes: pickled zilch, creating interactor hashes\n";
    	
    	# Likely to be *REALLY* slow the first time
    	# round
    	$self->create_interactor_hashes();
    }
    
	my $ctable = $self->ctable;
	my $row_num;
	my $line_nums1;
	my $line_nums2;
	my $uniprot1_string = "uniprotkb:$uniprot1";
	my $uniprot2_string = "uniprotkb:$uniprot2";
	my $lca;
	my $selection2 = $self->uniprot2_to_selection_hash->{$uniprot1}->{$uniprot2};
	if (!(defined $selection2)) {
		$line_nums1 = $self->interactor_a_hash->{$uniprot1_string};
		$line_nums2 = $self->interactor_b_hash->{$uniprot2_string};
		
		if (defined $line_nums1 && defined$line_nums2) {
			print STDERR "IntAct.find_intact_ids_for_uniprot_pair_using_hashes: FIRST scalar(line_nums1)=" . scalar(@{$line_nums1}) . " . scalar(line_nums2)=" . scalar(@{$line_nums2}) . "\n";
			if ($uniprot1 eq "P61024" && $uniprot2 eq "P24941") {
				my @l1 = @{$line_nums1};
				my @l2 = @{$line_nums2};
				print STDERR "IntAct.find_intact_ids_for_uniprot_pair_using_hashes: FIRST l1=@l1, l2=@l2\n";
			}
			
			$lca = List::Compare->new('-u', '-a', $line_nums1, $line_nums2);
			
			my @s2 = $lca->get_intersection();
			if ($uniprot1 eq "P61024" && $uniprot2 eq "P24941") {
				print STDERR "IntAct.find_intact_ids_for_uniprot_pair_using_hashes: s2=@s2\n";
			}
			$selection2 = \@s2;
			if (scalar(@{$selection2})>0) {
				print STDERR "IntAct.find_intact_ids_for_uniprot_pair_using_hashes: FIRST intersection found\n";
				$self->uniprot2_to_selection_hash->{$uniprot1}->{$uniprot2} = $selection2;
			}
		}
	}

	# If we can't find an interaction matching A,B, try B,A.
	if (!(defined $selection2) || scalar(@{$selection2})<1) {
		$selection2 = $self->uniprot2_to_selection_hash->{$uniprot2}->{$uniprot1};
		if (!(defined $selection2)) {
			$line_nums1 = $self->interactor_a_hash->{$uniprot2_string};
			$line_nums2 = $self->interactor_b_hash->{$uniprot1_string};
		
			if (defined $line_nums1 && defined$line_nums2) {
				print STDERR "IntAct.find_intact_ids_for_uniprot_pair_using_hashes: SECOND scalar(line_nums1)=" . scalar(@{$line_nums1}) . ", scalar(line_nums2)=" . scalar(@{$line_nums2}) . "\n";
				if ($uniprot1 eq "P61024" && $uniprot2 eq "P24941") {
					my @l1 = @{$line_nums1};
					my @l2 = @{$line_nums2};
					print STDERR "IntAct.find_intact_ids_for_uniprot_pair_using_hashes: SECOND l1=@l1, l2=@l2\n";
				}
		
				$lca = List::Compare->new('-u', '-a', $line_nums1, $line_nums2);
				my @s2 = $lca->get_intersection();
				$selection2 = \@s2;
				if (scalar(@{$selection2})>0) {
					print STDERR "IntAct.find_intact_ids_for_uniprot_pair_using_hashes: SECOND intersection found\n";
					$self->uniprot2_to_selection_hash->{$uniprot1}->{$uniprot2} = $selection2;
				}
			}
		}
	}
	
	my @intact_id_array = ();
	if (defined $selection2) {
		print STDERR "IntAct.find_intact_ids_for_uniprot_pair_using_hashes: scalar(selection2)=" . scalar(@{$selection2}) . "\n";
	
		my %intact_ids = ();
		my $interaction_identifiers = $self->interaction_identifiers;
		my @temp_intact_ids;
		my $intact_id_string;
		my $intact_id;
		foreach $row_num (@{$selection2}) {
			$intact_id_string = $interaction_identifiers->[$row_num];
			
			@temp_intact_ids = split(/\|/, $intact_id_string);
	
			print STDERR "IntAct.find_intact_ids_for_uniprot_pair_using_hashes: muesli-smuesli, intact_id_string=$intact_id_string\n";
			
			foreach $intact_id (@temp_intact_ids) {
				$intact_id =~ s/^intact://;
				$intact_ids{$intact_id} = $intact_id;
			}
		}
		
		@intact_id_array = sort(keys(%intact_ids));
	}
	
	return @intact_id_array;
}

# Given a hash of interactions, will attempt to get the corresponding
# IntAct IDs.  Uses the IntAct web services, so potentially slow.
sub find_intact_ids_for_interactions {
    my ($self, $interactions) = @_;

	my @db_ids1 = keys(%{$interactions});
	my @db_ids2;
	my $db_id1;
	my $db_id2;
	my $interactor1;
	my $interactor2;
	my $id1;
	my $id2;
	my $interaction;
	foreach $db_id1 (@db_ids1) {
		@db_ids2 = keys(%{$interactions->{$db_id1}});
		foreach $db_id2 (@db_ids2) {
			$interaction = $interactions->{$db_id1}->{$db_id2};
			$interactor1 = $interaction->{'interactors'}->[0];
			$interactor2 = $interaction->{'interactors'}->[1];
			
			# TODO: we may also need to get the name of the
			# reference database in order to build an
			# unequivocal query.
			$id1 = $interactor1->identifier->[0];
			$id2 = $interactor2->identifier->[0];
			
			my $intact_ids = $self->find_intact_ids_for_uniprot_pair_using_web_services($id1, $id2);
			
			$interaction->{'intact_ids'} = $intact_ids;
		}
	}
}

# Given a pair of UniProt IDs, will attempt to get the corresponding
# IntAct IDs.
#
# Uses IntAct's web services.
sub find_intact_ids_for_uniprot_pair_using_web_services {
    my ($self, $uniprot1, $uniprot2) = @_;
    
	my @intact_ids = ();

    if (!(defined $self->find_binary_interactions_call)) {
		$self->setup_soap();
    }
    
    print STDERR "IntAct.find_intact_ids_for_uniprot_pair_using_web_services: uniprot1=$uniprot1, uniprot2=$uniprot2\n";
    
	my $query = "+id:$uniprot1 +id:$uniprot2";
    my @params = (SOAP::Data->name(query => $query));
    my $result = undef;
    eval {
    	# Put into eval, because SOAP calls to IntAct website crash the
    	# program after a certain time - probably some kind of timeout
    	# mechanism on the IntAct side.
    	$result = $self->intact_soap->call($self->find_binary_interactions_call => @params);
    };
	if (!(defined $result)) {
		print STDERR "IntAct.find_intact_ids_for_uniprot_pair_using_web_services: WARNING - problem with SOAP access for IDs $uniprot1 and $uniprot2, trying again\n";

		# Set up SOAP afresh, and repeat the call
		$self->setup_soap();
	    eval {
	    	$result = $self->intact_soap->call($self->find_binary_interactions_call => @params);
	    };
	}

	if (!(defined $result)) {
		print STDERR "IntAct.find_intact_ids_for_uniprot_pair_using_web_services: WARNING - problem with SOAP access for IDs $uniprot1 and $uniprot2, giving up.\n";
	} elsif (!($result->fault)) {
	    # Retrieve all interactions in PSIMITAB format
	    my @rows = ();
	    eval {
	    	# Put in eval, voodoo protection against mysterious program
	    	# termination on brie8 (doesn't happen on picard!!).
	    	@rows = $result->valueof('//interactionLines');
	    };

		my $row;
		my @cols;
		my $intact_id_string;
		my @temp_intact_ids;
		my $intact_id;
		for $row (@rows) {
		    @cols = split(/\t/, $row);
		    $intact_id_string = undef;
		    if ($uniprot1 eq $uniprot2) {
		    	# We have a bit of a special case if both UniProt IDs
		    	# are the same, because in that case, the IntAct
		    	# web services try to find everything that intereacts
		    	# with $uniprot1, ignoring $uniprot2.  This isn't
		    	# really what we want, because when both IDs are the
		    	# same, it means we have a homodimer.  So, be extra
		    	# picky if this situation is detected.
		    	if ($cols[0] eq $cols[1]) {
			    	if (defined $cols[13] && !($cols[13] eq "")) {
			    		$intact_id_string = $cols[13];
			    	}
		    	}
		    } else {
		    	if (defined $cols[13] && !($cols[13] eq "")) {
		    		$intact_id_string = $cols[13];
		    	}
		    }
		    
		    # More than one IntAct ID may be associated with the given pair of
		    # proteins.  In this case, they will be returned separated by vertical
		    # bars | .  Furthermore, they are likely to have a superfluous "intact:"
		    # prepended onto them, which we will remove here.
		    if (defined $intact_id_string) {
				@temp_intact_ids = split(/\|/, $intact_id_string);
				foreach $intact_id (@temp_intact_ids) {
					$intact_id =~ s/^intact://;
					
					# The IntAct SOAP services have been sending back
					# some weird identifiers since release 36 of Reactome,
					# containing the string "(rigid)".  These cause
					# trouble, so filter them out.
					if (!($intact_id =~ /(rigid)$/)) {
						push(@intact_ids,  $intact_id);
					}
				}
		    }
		}
	} else {
		# Deal with errors
		print STDERR "IntAct.find_intact_ids_for_uniprot_pair_using_web_services: WARNING - problem retrieving data, fault code=" . $result->faultcode . ", fault=" . $result->faultstring . ", detail=" . $result->faultdetail . "\n";
	}

	return \@intact_ids;
}

sub setup_soap {
    my ($self) = @_;
    
	# Setup service
	my $WSDL = 'http://www.ebi.ac.uk/intact/binary-search-ws/binarysearch?wsdl';
	my $nameSpace = 'http://ebi.ac.uk/intact/binarysearch/wsclient/generated';
	$self->intact_soap(SOAP::Lite->uri($nameSpace)->proxy($WSDL));
		
	# Setup method and parameters
	$self->find_binary_interactions_call(SOAP::Data->name('findBinaryInteractions')->attr({xmlns => $nameSpace}));
}

# Treats the given URL as a web service, and tries to get
# something out of it.  params should
# be a reference to a hash of name/value pairs that will
# be passed as hidden parameters to the target URL.
sub extract_content_from_url {
    my ($self, $url, $params) = @_;
    
    my $ua = LWP::UserAgent->new();
    my $response = $ua->post($url, $params);
    my $content = "";
    
    if (defined $response) {
		$content = $response->content;
    }

    return $content;
}

1;
