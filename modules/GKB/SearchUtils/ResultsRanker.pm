package GKB::SearchUtils::ResultsRanker;

=head1 NAME

GKB::SearchUtils::ResultsRanker

=head1 SYNOPSIS

Provides methods for ranking a list of instances.

=head1 DESCRIPTION

Removes instances that are probably not going to be interesting
or meaningful to the average user.

=head1 SEE ALSO

=head1 AUTHOR

David Croft E<lt>croft@ebi.ac.ukE<gt>

Copyright (c) 2008 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

use strict;
use vars qw(@ISA $AUTOLOAD %ok_field %INSTANCE_CLASS_RANK_FACTORS);
use Bio::Root::Root;
use Carp;

@ISA = qw(Bio::Root::Root);

for my $attr
    (qw(
	) ) { $ok_field{$attr}++; }

%INSTANCE_CLASS_RANK_FACTORS = 
    (
     'Pathway' => '50',
     'ReactionlikeEvent' => '25',
     'ReferencePeptideSequence' => '20',
     'ReferenceGeneProduct' => '20',
     'ReferenceIsoform' => '20',
     'ReferenceDNASequence' => '15',
     'ReferenceEntity' => '10',
     'Regulation' => '5',
     'UNKNOWN' => '1',
    );

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
    
    if (defined $args[1]) {
    	$self->set_render_result($args[1]);
    }
        
    return $self;
}

# Needed by subclasses
sub get_ok_field {
	return %ok_field;
}

# Creates rankings for the supplied list of instances.
# The instances are given scores according to their degree
# of "interest".  These scores are a weighted combination
# of a number of factors, and are intended to be used for
# sorting the instances for presentation as a set of search
# results.
#
# Arguments are:
# instances		A reference to an array of instances
# terms			A reference to an array of search terms.
#
# The rank value is put into the _timestamp attributes of
# the instances.  So: do not try to save the instances back
# to the database once you have run this subroutine over
# them!!!!
#
# Returns a reference to an array of sorted instances.
sub rank {
	my($self, $instances, $terms) = @_;
	
	my $instance;
	my $rank;
	foreach $instance (@{$instances}) {
		$rank = $self->rank_instance($instance, $terms);

		# This is a bit wicked - hijack the _timestamp
		# attribute for storing rank.
		$instance->attribute_value("_timestamp", $rank);
	}
	
	$instances = $self->sort_instances_by_rank($instances);
	
	# If the top-scoring instance is a gene, give the corresponding
	# protein a boost too.
	if (defined $instances && scalar(@{$instances})>0) {
		my $gene_instance = $instances->[0];
		my $reference_gene;
		my $boost_factor = 3;
		if ($gene_instance->is_a("ReferenceDNASequence")) {
			my $gene_instance_db_id = $gene_instance->db_id();
			foreach $instance (@{$instances}) {
				if ($instance->is_valid_attribute("referenceGene")) {
					foreach $reference_gene (@{$instance->referenceGene}) {
						if ($reference_gene->db_id() == $gene_instance_db_id) {
							$rank = $instance->attribute_value("_timestamp");
							$rank += 6 * $boost_factor * $INSTANCE_CLASS_RANK_FACTORS{"UNKNOWN"};
							$instance->attribute_value("_timestamp", $rank);
							last;
						}
					}
				}
			}
			$instances = $self->sort_instances_by_rank($instances);
		}
	}
	
	return $instances;
}

# Works out and returns a ranking value for the given instance.
sub rank_instance {
	my($self, $instance, $terms) = @_;
	
	my $rank = undef;
	my $previous_rank = undef;
	my @instance_class_rank_names = keys(%INSTANCE_CLASS_RANK_FACTORS);
	my $instance_class_rank_name;
	
	$rank = $self->rank_instance_by_class($instance);
	my $unit_rank = $rank; # ???
	$rank += $self->rank_instance_by_matching_terms_in_name($instance, $unit_rank, $terms);
	$rank += $self->rank_instance_by_matching_terms_in_description($instance, $unit_rank, $terms);
	$rank += $self->rank_instance_by_computational_inference($instance, $unit_rank, $terms);
	$rank += $self->rank_instance_by_species($instance, $unit_rank, $terms);
	$rank += $self->rank_instance_by_has_diagram($instance, $unit_rank, $terms);
	$rank += $self->rank_instance_by_is_root_pathway($instance, $unit_rank, $terms);
	
#	print "For DB_ID=" . $instance->db_id() . "class=" . $instance->class() . ", unit_rank=$unit_rank, rank=$rank, test_rank=$test_rank<br>\n";
#
	return $rank;
}

sub rank_instance_by_class {
	my($self, $instance) = @_;
	
	my $rank = undef;
	my $previous_rank = undef;
	my @instance_class_rank_names = keys(%INSTANCE_CLASS_RANK_FACTORS);
	my $instance_class_rank_name;
		
	# Rank instances according to class
	my $assigned_instance_class_rank_name = "UNKNOWN";
	foreach $instance_class_rank_name (@instance_class_rank_names) {
		if ($instance->is_a($instance_class_rank_name)) {
			$rank = $INSTANCE_CLASS_RANK_FACTORS{$instance_class_rank_name};
			if (defined $previous_rank && $rank<$previous_rank) {
				# We do this because there may be more than one matching
				# class for this instance (e.g. Pathway and Event), and
				# we want to choose the most appropriate rank.
				$rank = $previous_rank;
			} else {
				$assigned_instance_class_rank_name = $instance_class_rank_name;
			}
			$previous_rank = $rank;
		}
	}
		
	if (!(defined $rank)) {
		$rank = $INSTANCE_CLASS_RANK_FACTORS{"UNKNOWN"};
	}
	
	return $rank;
}

sub rank_instance_by_matching_terms_in_name {
	my($self, $instance, $unit_rank, $terms) = @_;
	
	my $text = $instance->_displayName->[0];
	my $boost_factor = 3;
	my $rank =  $self->rank_instance_by_matching_terms($instance, $unit_rank, $terms, $text, $boost_factor);
	
	my $combined_term = "";
	foreach my $term (@{$terms}) {
		if (!($combined_term eq "")) {
			$combined_term .= "[^a-zA-Z]+";
		}
		$combined_term .= $term;
	}
	if ($text =~ /^$combined_term$/i) {
		# If the title ONLY contains the terms and no other words,
		# this has got to be a really great hit, so give it lots
		# of boost.
		$rank += 8 * $boost_factor * $unit_rank;
	} elsif ($text =~ /^[A-Za-z ]+:$combined_term\s/ || $text =~ /^[A-Za-z ]+:$combined_term$/) {
		# perhaps the first thing in the string is an external database ID
		# (corresponding to the term)
		$rank += 6 * $boost_factor * $unit_rank;
	} else {
		# Incrementally strip bits from $text that are probably dross.
		my $dross;
		
		# perhaps the first thing in the string is an external database ID
		# (but it does'nt contain the search term)
		if ($text =~ /^([A-Za-z ]+:[A-Za-z0-9-_.]+ +)/) {
			$dross=$1;
			if (defined $dross && !($dross =~ /$combined_term$/)) {
				$text =~ s/^[A-Za-z ]+:[A-Za-z0-9-_.]+ +//;
			}
		}
		
		# a special case to handle things like l-serine
		$text =~ /^([^A-Za-z]+)$combined_term/i;
		$dross=$1;
		if (defined $dross && !($dross =~ /$combined_term$/)) {
			$text =~ s/^[^A-Za-z]+//i;
		}
		
		# a special case to handle things like 2-hydroxy...
		$text =~ /^([A-Za-z][^A-Za-z])$combined_term/i;
		$dross=$1;
		if (defined $dross && !($dross =~ /$combined_term$/)) {
			$text =~ s/^[A-Za-z][^A-Za-z]//i;
		}
		
		# a special case to handle things like ...serine [Chebi]
		$text =~ /( +\[[^\]]+\])$/i;
		$dross=$1;
		if (defined $dross && !($dross =~ /$combined_term$/)) {
			$text =~ s/ +\[[^\]]+\]$//i;
		}
		
		if ($text =~ /^$combined_term$/i) {
			$rank += 6 * $boost_factor * $unit_rank;
		}
	}
	
	return $rank;
}

sub rank_instance_by_matching_terms_in_description {
	my($self, $instance, $unit_rank, $terms) = @_;
	
	my $text = $instance->get_description();
	return $self->rank_instance_by_matching_terms($instance, $unit_rank, $terms, $text, 1);
}

sub rank_instance_by_matching_terms {
	my($self, $instance, $unit_rank, $terms, $text, $boost_factor) = @_;
	
	my $rank = 0;
	if (scalar(@{$terms})<1) {
		return $rank;
	}
	my $term;
	
	# Rank instances by matching terms
	foreach $term (@{$terms}) {
		if ($text =~ /$term/i) {
			$rank += $boost_factor * $unit_rank;
		}
	}
	
	# Crude proximity score: if all terms are next to each other,
	# give an even bigger boost
	my $combined_term = join("[^a-zA-Z]+", @{$terms});
	if ($text =~ /$combined_term/i) {
		$rank += 2 * $boost_factor * $unit_rank;
	}
	
	# The closer the first term is to the beginning of the text,
	# the better.
	# Using the regular "index" function is not so convenient, because
	# it is case-sensetive.
	$term = $terms->[0];
	$text =~ /^(.*)$term/i;
	my $prestring = $1;
	if (defined $prestring) {
		my $index = length($prestring);
		if ($index == 0) {
			$rank += $boost_factor * $unit_rank;
		} elsif ($index < 20 && ($index < index($text, '.') || $index < length($text))) {
			$rank += ($boost_factor * $unit_rank)/2;
		}
	}
	
	return $rank;
}

# Punish those nasty computationally inferred events
sub rank_instance_by_computational_inference {
	my($self, $instance, $unit_rank, $terms) = @_;
	
	my $rank = 0;
	
	my $text = $instance->get_description() . " " . $instance->_displayName->[0];
	if ($text =~ /has been computationally inferred/i) {
		$rank -= $unit_rank/2;
	}

	return $rank;
}

# Some species are more equal than others - give them a bit of a boost
sub rank_instance_by_species {
	my($self, $instance, $unit_rank, $terms) = @_;
	
	my $rank = 0;
	
	if ($instance->is_valid_attribute("species") && defined $instance->species && defined $instance->species->[0]) {
		if ($instance->species->[0]->_displayName->[0] eq "Homo sapiens") {
			$rank += $unit_rank/5;
		} elsif ($instance->species->[0]->_displayName->[0] eq "Mus musculus") {
			$rank += $unit_rank/6;
		} elsif ($instance->species->[0]->_displayName->[0] eq "Drosophila melanogaster") {
			$rank += $unit_rank/7;
		} elsif ($instance->species->[0]->_displayName->[0] eq "Caenorhabditis elegans") {
			$rank += $unit_rank/8;
		} elsif ($instance->species->[0]->_displayName->[0] eq "Saccharomyces cerevisiae") {
			$rank += $unit_rank/9;
		} elsif ($instance->species->[0]->_displayName->[0] eq "Escherichia coli") {
			$rank += $unit_rank/10;
		}
	}

	return $rank;
}

# Reward pathways with associated diagrams
sub rank_instance_by_has_diagram {
	my($self, $instance, $unit_rank, $terms) = @_;
	
	my $rank = 0;
	
	my $pathway_diagrams = $instance->reverse_attribute_value('representedPathway');
	if (defined $pathway_diagrams && scalar(@{$pathway_diagrams}) > 0) {
		$rank += $unit_rank*5;
	}

	return $rank;
}

# Reward pathways with associated diagrams
sub rank_instance_by_is_root_pathway {
	my($self, $instance, $unit_rank, $terms) = @_;
	
	my $rank = 0;

	if (!$instance->is_a("Pathway")) {
	    return $rank;
	}
	
	my $parent_pathways = $instance->reverse_attribute_value('hasEvent');
	if (!(defined $parent_pathways) || scalar(@{$parent_pathways}) == 0) {
		$rank += $unit_rank;
	}

	my $front_pages = $instance->reverse_attribute_value('frontPageItem');
	if (defined $front_pages && scalar(@{$front_pages}) > 0) {
		$rank += $unit_rank*3;
	}

	return $rank;
}

# Takes as argument a reference to an array of instances, and returns
# a reference to an array of instances which have been sorted
# by rank.
sub sort_instances_by_rank {
	my($self, $instances) = @_;
	
	my @sorted_instance_array = ();
	if (defined $instances) {
		@sorted_instance_array = sort {$b->_timestamp->[0] <=> $a->_timestamp->[0]} @{$instances};
	}
	
	return \@sorted_instance_array;
}

1;

