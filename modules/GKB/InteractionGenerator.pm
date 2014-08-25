package GKB::InteractionGenerator;

=head1 NAME

GKB::InteractionGenerator

=head1 SYNOPSIS

Subroutines that create connections between Reactome's data and
interactions.

=head1 DESCRIPTION

* Subroutines that generate interactions from Reactome's data
* Subroutines for printing interactions
* Subroutines for connecting Reactome data to IntAct

Much of this stuff was originally in Utils.pm, but I thought
it deserved a class of its own.  Changes have been made mainly
to make it flexible enough to be able to generate the output
format that IntAct would like to see, as well as producing
our regular interactor files for the Reactome download page.

=head1 SEE ALSO

GKB::IntAct
GKB::Util

=head1 AUTHOR

Imre Vastrik E<lt>vastrik@ebi.ac.ukE<gt>
David Croft E<lt>croft@ebi.ac.ukE<gt>
Esther Schmidt E<lt>eschmidt@ebi.ac.ukE<gt>

Copyright (c) 2008 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

use vars qw(@ISA $AUTOLOAD %ok_field);
use strict;
use Carp qw(cluck confess);
use Getopt::Long qw(:config pass_through);
use Bio::Root::Root;
use Data::Dumper;
use File::Basename;
use GKB::Config;
use GKB::IntAct;
use GKB::Utils;
use Log::Log4perl qw/get_logger/;
Log::Log4perl->init(\$LOG_CONF);

@ISA = qw(Bio::Root::Root);

my $COLUMN_GROUP_IDS = "ids"; # include the IDs of the interacting proteins
my $COLUMN_GROUP_UNIPROT_IDS = "uniprot_ids"; # just show UniProt IDs of the interacting proteins
my $COLUMN_GROUP_CONTEXT = "context"; # include the generation context of the interacting proteins
my $COLUMN_GROUP_SOURCE_IDS = "source_ids"; # include DB_IDs of reaction/complex used to generate interaction
my $COLUMN_GROUP_SOURCE_STABLE_IDS = "source_st_ids"; # include stable IDs of reaction/complex used to generate interaction
my $COLUMN_GROUP_DECORATED_SOURCE_IDS = "decorated_source_ids"; # include IDs of reaction/complex used to generate interaction
my $COLUMN_GROUP_PARTICIPATING_PROTEIN_COUNT = "participating_protein_count"; # include the number of praticipating proteins
my $COLUMN_GROUP_LIT_REFS = "lit_refs"; # include literature references (PubMed IDs)
my $COLUMN_GROUP_INTACT = "intact"; # include IntAct IDs corresponding to the interaction

# Make a note of local variable names
for my $attr
    (qw(
	intact
	add_intact_ids_flag
	title_headers_flag
	interaction_count_headers_flag
	table_headers_flag
	column_groups
	dba
	line_cache
	previous_line
	previous_source_id_info
	display_only_intact_lines
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
sub new {
    my($pkg, @args) = @_;
    my $self = bless {}, $pkg;
    
    $self->intact(new GKB::IntAct());
    $self->add_intact_ids_flag(0);
    $self->title_headers_flag(0);
    $self->interaction_count_headers_flag(0);
    $self->table_headers_flag(0);
    $self->column_groups({$COLUMN_GROUP_IDS => 1, $COLUMN_GROUP_CONTEXT => 1, $COLUMN_GROUP_DECORATED_SOURCE_IDS => 1, $COLUMN_GROUP_LIT_REFS => 1});
    $self->line_cache(undef);
    $self->previous_line(undef);
    $self->previous_source_id_info({});
    $self->display_only_intact_lines(0);
    
    return $self;
}

sub set_title_headers_flag {
	my ($self, $title_headers_flag) = @_;
	
	$self->title_headers_flag($title_headers_flag);
}

sub set_interaction_count_headers_flag {
	my ($self, $interaction_count_headers_flag) = @_;
	
	$self->interaction_count_headers_flag($interaction_count_headers_flag);
}

sub set_table_headers_flag {
	my ($self, $table_headers_flag) = @_;
	
	$self->table_headers_flag($table_headers_flag);
}

sub set_column_groups {
	my ($self, $column_groups) = @_;
	
	if (defined $column_groups->{$COLUMN_GROUP_INTACT} && $column_groups->{$COLUMN_GROUP_INTACT} == 1) {
		$self->add_intact_ids_flag(1);
	}
	
	$self->column_groups($column_groups);
}

sub set_dba {
	my ($self, $dba) = @_;
	
	$self->dba($dba);
}

# If you want to use line-caching, then you should set this initially to {}.
sub set_line_cache {
	my ($self, $line_cache) = @_;
	
	$self->line_cache($line_cache);
}

# Only show lines that also have IntAct IDs. This only has an
# effect if column_groups contains COLUMN_GROUP_INTACT.
sub set_display_only_intact_lines {
	my ($self, $display_only_intact_lines) = @_;
	
	$self->display_only_intact_lines($display_only_intact_lines);
}

sub find_interactors_for_ReferenceSequences {
    my ($self, $rss, $participating_protein_count_cutoff, $mitab) = @_;
    
    my $logger = get_logger(__PACKAGE__);
    
    my %interactions;
    my $rs_count = scalar(@{$rss});
    
    if (defined $participating_protein_count_cutoff) {
    	$logger->info("InteractionGenerator.find_interactors_for_ReferenceSequences: participating_protein_count_cutoff=$participating_protein_count_cutoff");
    } else {
    	$logger->info("InteractionGenerator.find_interactors_for_ReferenceSequences: participating_protein_count_cutoff is undef");
    }
    $logger->info("InteractionGenerator.find_interactors_for_ReferenceSequences: total number of ReferenceSequences=$rs_count");
    
    my $rs_num = 0;
    foreach my $rs (@{$rss}) {
    	if ($rs_num%100 == 0) {
    	    $logger->info("InteractionGenerator.find_interactors_for_ReferenceSequences: rs_num=$rs_num (" . ($rs_num*100)/$rs_count . "%)");
    	}
	if (defined $mitab) {
            $self->find_mitab_interactors_for_ReferenceSequence($rs,\%interactions, $participating_protein_count_cutoff);
        } else {
            $self->find_interactors_for_ReferenceSequence($rs,\%interactions, $participating_protein_count_cutoff);
        }	
	$rs_num++;
    }
    return \%interactions;
}

sub find_interactors_for_ReferenceSequence {
    my ($self, $rs, $interactions, $participating_protein_count_cutoff) = @_;
    $interactions ||= {};
    my $participating_protein_count;
    
    # Keep ReferenceGeneProduct for backward compatibility
    my $out_classes = 'ReferenceGeneProduct';
    if (!($self->dba->ontology->is_valid_class($out_classes))) {
    	$out_classes = 'ReferencePeptideSequence';
    }
    
    my %neighbouring_reaction_instructions = 
	(
	 -INSTRUCTIONS =>
	 {
#	     'EquivalentEventSet' => {'attributes' => [qw(hasMember)]},
#	     'ConceptualEvent' => {'attributes' => [qw(hasMember)]},
	     'Reaction' => {'attributes' => [qw(input output catalystActivity)]},
	     'CatalystActivity' => {'attributes' => [qw(physicalEntity)]},
	     'Complex' => {'attributes' => [qw(hasComponent)]},
	     'EntitySet' => {'attributes' => [qw(hasMember)]},
	     'EntityWithAccessionedSequence' => {'attributes' => [qw(referenceEntity)]}},
	 -OUT_CLASSES => [$out_classes]
	 );

    # Find PhysicalEntities referring to this ReferenceEntity
    my @pes = grep {$_->is_a('PhysicalEntity')} @{$rs->reverse_attribute_value('referenceEntity')};
    my @tmp;
    # Add Generics of those PhysicalEntities
    foreach my $pe (@pes) {
		my $ar = $pe->follow_class_attributes
		    (-INSTRUCTIONS =>
		     {'PhysicalEntity' => {'reverse_attributes' => ['hasMember']}},
		     -OUT_CLASSES => ['PhysicalEntity']
		     );
		push @tmp, @{$ar};
    }
    @pes = @tmp;
    # Find immediate complexes containing those entities.
    my %direct_complexes;
    foreach my $pe (@pes) {
	foreach my $complex (@{$pe->reverse_attribute_value('hasComponent')}) {
	    $participating_protein_count = $self->participating_protein_count_from_complexes([$complex]);
	    $direct_complexes{$complex->db_id} = $complex;
	    # Skip the original component
	    foreach my $component (grep {$_ != $pe} @{$complex->HasComponent}) {
		my $rss = $component->follow_class_attributes
		    (-INSTRUCTIONS =>
		     {'EntitySet' => {'attributes' => ['hasMember']},
		      'EntityWithAccessionedSequence' => {'attributes' => ['referenceEntity']}},
		     -OUT_CLASSES => [$out_classes]
		     );
		foreach my $rs2 (@{$rss}) {
		    my ($t1,$t2) = sort {$a->db_id <=> $b->db_id} ($rs,$rs2);
		    unless ($interactions->{$t1->db_id}->{$t2->db_id}) {
			$interactions->{$t1->db_id}->{$t2->db_id}->{'interactors'} = [$t1,$t2];
			if ($self->add_intact_ids_flag) {
			    if (!(defined $participating_protein_count_cutoff) || $participating_protein_count <= $participating_protein_count_cutoff) {
				print STDERR "InteractionGenerator.find_interactors_for_ReferenceSequence: participating_protein_count=$participating_protein_count\n";
				
				$self->_insert_intact_ids($interactions->{$t1->db_id}->{$t2->db_id});
			    }
			}
		    }
		    $interactions->{$t1->db_id}->{$t2->db_id}->{'direct_complex_context'}->{$complex->db_id} = $complex;
		}
	    }
	}
    }
    
    my $pes = $rs->follow_class_attributes
	(-INSTRUCTIONS =>
	 {'ReferenceSequence' => {'reverse_attributes' => ['referenceEntity']},
	  'PhysicalEntity' => {'reverse_attributes' => ['hasComponent', 'hasMember']}},
	 -OUT_CLASSES => ['PhysicalEntity']
	 );

    my %pes;
    map {$pes{$_->db_id} = $_} @{$pes};

    foreach my $pe (@{$pes}) {
		#print $pe->extended_displayName, "\n";
		# Only count interaction via reaction if the other ReferenceSequence not in
		# the same complex with the given ReferenceSequence.
	
		# Handle input and output separately to make it easier.
	
		# input
	my $reactions = $pe->follow_class_attributes
	    (-INSTRUCTIONS =>
	     {'PhysicalEntity' => {'reverse_attributes' => ['input','physicalEntity']},
	      'CatalystActivity' => {'reverse_attributes' => ['catalystActivity']}},
	     -OUT_CLASSES => ['Reaction']
	     );
	foreach my $r (@{$reactions}) {
	    $participating_protein_count = $self->participating_protein_count_from_reactions([$r]);
	    my $pes1 = $r->follow_class_attributes
		(-INSTRUCTIONS =>
		 {'Reaction' => {'attributes' => ['input','catalystActivity']},
		  'CatalystActivity' => {'attributes' => ['physicalEntity']}},
		 -OUT_CLASSES => ['PhysicalEntity']
		 );
	    foreach my $pe1 (@{$pes1}) {
		next if ($pe1 == $pe);
		my $rpss = $pe1->follow_class_attributes
		    (-INSTRUCTIONS =>
		     {'Complex' => {'attributes' => [qw(hasComponent)]},
		      'EntitySet' => {'attributes' => [qw(hasMember)]},
		      'EntityWithAccessionedSequence' => {'attributes' => ['referenceEntity']}},
		     -OUT_CLASSES => [$out_classes]
		     );
		foreach my $rps (@{$rpss}) {
		    my ($t1,$t2) = sort {$a->db_id <=> $b->db_id} ($rs,$rps);
		    unless ($interactions->{$t1->db_id}->{$t2->db_id}) {
			$interactions->{$t1->db_id}->{$t2->db_id}->{'interactors'} = [$t1,$t2];
			if ($self->add_intact_ids_flag) {
			    if (!(defined $participating_protein_count_cutoff) || $participating_protein_count <= $participating_protein_count_cutoff) {
				$self->_insert_intact_ids($interactions->{$t1->db_id}->{$t2->db_id});
			    }
			}
		    }
		    #		    $interactions->{$t1->db_id}->{$t2->db_id}->{'context'}->{$r->db_id} = $r;
		    $interactions->{$t1->db_id}->{$t2->db_id}->{'reaction_context'}->{$r->db_id} = $r;
		}
	    }
	    # neighbouring, i.e. preceding/following reactions
	    foreach my $nr (@{$r->PrecedingEvent}, @{$r->reverse_attribute_value('precedingEvent')}) {
		$participating_protein_count = $self->participating_protein_count_from_reactions([$r,$nr]);
		my $rpss = $nr->follow_class_attributes(%neighbouring_reaction_instructions);
		foreach my $rps (@{$rpss}) {
		    my ($t1,$t2) = sort {$a->db_id <=> $b->db_id} ($rs,$rps);
		    unless ($interactions->{$t1->db_id}->{$t2->db_id}) {
			$interactions->{$t1->db_id}->{$t2->db_id}->{'interactors'} = [$t1,$t2];
			if ($self->add_intact_ids_flag) {
			    if (!(defined $participating_protein_count_cutoff) || $participating_protein_count <= $participating_protein_count_cutoff) {
				$self->_insert_intact_ids($interactions->{$t1->db_id}->{$t2->db_id});
			    }
			}
		    }
		    push @{$interactions->{$t1->db_id}->{$t2->db_id}->{'neighbouring_reaction_context'}}, [$r,$nr];
				}
	    }
	}
	
	# output
	$reactions = $pe->follow_class_attributes
	    (-INSTRUCTIONS =>
	     {'PhysicalEntity' => {'reverse_attributes' => ['output','physicalEntity']},
	      'CatalystActivity' => {'reverse_attributes' => ['catalystActivity']}},
	     -OUT_CLASSES => ['Reaction']
	     );
	foreach my $r (@{$reactions}) {
	    $participating_protein_count = $self->participating_protein_count_from_reactions([$r]);
	    my $pes1 = $r->follow_class_attributes
		(-INSTRUCTIONS =>
		 {'Reaction' => {'attributes' => ['output','catalystActivity']},
		  'CatalystActivity' => {'attributes' => ['physicalEntity']}},
		 -OUT_CLASSES => ['PhysicalEntity']
		 );
	    foreach my $pe1 (@{$pes1}) {
		next if ($pe1 == $pe);
		my $rpss = $pe1->follow_class_attributes
		    (-INSTRUCTIONS =>
		     {'Complex' => {'attributes' => [qw(hasComponent)]},
		      'EntitySet' => {'attributes' => [qw(hasMember)]},
		      'EntityWithAccessionedSequence' => {'attributes' => ['referenceEntity']}},
		     -OUT_CLASSES => [$out_classes]
		     );
		foreach my $rps (@{$rpss}) {
		    my ($t1,$t2) = sort {$a->db_id <=> $b->db_id} ($rs,$rps);
		    unless ($interactions->{$t1->db_id}->{$t2->db_id}) {
			$interactions->{$t1->db_id}->{$t2->db_id}->{'interactors'} = [$t1,$t2];
			if ($self->add_intact_ids_flag) {
			    if (!(defined $participating_protein_count_cutoff) || $participating_protein_count <= $participating_protein_count_cutoff) {
				$self->_insert_intact_ids($interactions->{$t1->db_id}->{$t2->db_id});
			    }
			}
		    }
		    $interactions->{$t1->db_id}->{$t2->db_id}->{'reaction_context'}->{$r->db_id} = $r;
		}
	    }
	    # neighbouring, i.e. preceding/following reactions
	    foreach my $nr (@{$r->PrecedingEvent}, @{$r->reverse_attribute_value('precedingEvent')}) {
		$participating_protein_count = $self->participating_protein_count_from_reactions([$r,$nr]);
		my $rpss = $nr->follow_class_attributes(%neighbouring_reaction_instructions);
		foreach my $rps (@{$rpss}) {
		    my ($t1,$t2) = sort {$a->db_id <=> $b->db_id} ($rs,$rps);
		    unless ($interactions->{$t1->db_id}->{$t2->db_id}) {
			$interactions->{$t1->db_id}->{$t2->db_id}->{'interactors'} = [$t1,$t2];
			if ($self->add_intact_ids_flag) {
			    if (!(defined $participating_protein_count_cutoff) || $participating_protein_count <= $participating_protein_count_cutoff) {
				$self->_insert_intact_ids($interactions->{$t1->db_id}->{$t2->db_id});
			    }
			}
		    }
		    push @{$interactions->{$t1->db_id}->{$t2->db_id}->{'neighbouring_reaction_context'}}, [$r,$nr];
		}
	    }
	}
	
	if ($pe->is_a('Complex') and ! $direct_complexes{$pe->db_id}) {
	    # Skip the component(s) which were on the path from the given ReferenceSequence
	    # to this Complex. This way we don't count other (alternative) instances as interactors.
	    # Also, of the nested complexes only the smallest is reported as interaction context.
	    # However, if the given ReferencesSequence is "present" in multiple sub-complexes, i.e
	    # something like:
	    #
	    # C1--p->C2--p->A
	    #   |      |
	    #   |      |-p->B
	    #   |
	    #   |-p->C3--p->A
	    #          |
	    #          |-p->B
	    #          |
	    #          |-p->C
	    #
	    # then also the super-complex, in this case C1 will be reported as context for
	    # A:B interaction
	    
	    $participating_protein_count = $self->participating_protein_count_from_complexes([$pe]);
	    my %seen = %pes;
	    foreach my $pe1 (grep {! $seen{$_->db_id}++} @{$pe->HasComponent}) {
		my $rpss = $pe1->follow_class_attributes
		    (-INSTRUCTIONS =>
		     {'Complex' => {'attributes' => [qw(hasComponent)]},
		      'EntitySet' => {'attributes' => [qw(hasMember)]},
		      'EntityWithAccessionedSequence' => {'attributes' => ['referenceEntity']}},
		     -OUT_CLASSES => [$out_classes]
		     );
		foreach my $rps (@{$rpss}) {
		    my ($t1,$t2) = sort {$a->db_id <=> $b->db_id} ($rs,$rps);
		    unless ($interactions->{$t1->db_id}->{$t2->db_id}) {
			$interactions->{$t1->db_id}->{$t2->db_id}->{'interactors'} = [$t1,$t2];
			if ($self->add_intact_ids_flag) {
			    if (!(defined $participating_protein_count_cutoff) || $participating_protein_count <= $participating_protein_count_cutoff) {
				$self->_insert_intact_ids($interactions->{$t1->db_id}->{$t2->db_id});
			    }
			}
		    }
		    $interactions->{$t1->db_id}->{$t2->db_id}->{'complex_context'}->{$pe->db_id} = $pe;
		}
	    }
	}
    }
    
    return $interactions;
}

sub find_mitab_interactors_for_ReferenceSequence {
    my ($self, $rs, $interactions, $participating_protein_count_cutoff) = @_;
    $interactions ||= {};
    my $participating_protein_count;
    
    # Keep ReferencePeptideSequence for backward compatibility                           
    my $out_classes = 'ReferenceGeneProduct';
    if (!($self->dba->ontology->is_valid_class($out_classes))) {
        $out_classes = 'ReferencePeptideSequence';
    }
    
    # Find PhysicalEntities referring to this ReferenceEntity
    my @pes = grep {$_->is_a('PhysicalEntity')} @{$rs->reverse_attribute_value('referenceEntity')};
    my @tmp;
    # Add Generics of those PhysicalEntities                                    
    foreach my $pe (@pes) {
        my $ar = $pe->follow_class_attributes
            (-INSTRUCTIONS =>
             {'PhysicalEntity' => {'reverse_attributes' => ['hasMember']}},
             -OUT_CLASSES => ['PhysicalEntity']
             );
        push @tmp, @{$ar};
    }
    @pes = @tmp;
    
    # Find immediate complexes containing those entities.        
    foreach my $pe (@pes) {
        foreach my $complex (@{$pe->reverse_attribute_value('hasComponent')}) {
#            $participating_protein_count = $self->participating_protein_count_from_complexes([$complex]);
            # Check other components in the complex for interactors. Skip the original component (otherwise e.g. set members with rs are counted as interactors. However, this excludes dimeric interactions, so they are handled separately below)     
	    foreach my $component (grep {$_ != $pe} @{$complex->HasComponent}) {
                my $rss = $component->follow_class_attributes
                    (-INSTRUCTIONS =>
                     {'EntitySet' => {'attributes' => ['hasMember']},
                      'EntityWithAccessionedSequence' => {'attributes' => ['referenceEntity']}},
                     -OUT_CLASSES => [$out_classes]
                     );
                foreach my $rs2 (@{$rss}) {
                    my ($t1,$t2) = sort {$a->db_id <=> $b->db_id} ($rs,$rs2);
                    unless ($interactions->{$t1->db_id}->{$t2->db_id}) {
                        $interactions->{$t1->db_id}->{$t2->db_id}->{'interactors'} = [$t1,$t2];
                    }
                    push @{$interactions->{$t1->db_id}->{$t2->db_id}->{'complex_context'}}, $complex;
                    push @{$interactions->{$t1->db_id}->{$t2->db_id}->{'complex_context'}}, @{$self->get_upstream_complexes_or_polymers($complex)};
                    push @{$interactions->{$t1->db_id}->{$t2->db_id}->{'reaction_context'}}, @{$self->get_reaction_for_complex_or_polymer($complex)};
                }
            }
	    
#Check for dimers                                                      
	    my %seen_d;
            my $comps = $complex->follow_class_attributes3 #follows attributes stochiometrically                      
		(-INSTRUCTIONS =>
                 {'Complex' => {'attributes' => ['hasComponent']},
                  'EntitySet' => {'attributes' => ['hasMember']},
                  'EntityWithAccessionedSequence' => {'attributes' => ['referenceEntity']}},
                 -OUT_CLASSES => [$out_classes]);
	    foreach my $comp (@{$comps}) {
		$seen_d{$comp->db_id}++;
	    }
	    if ($seen_d{$rs->db_id} > 1) { #indicates dimer or multimer                           
		unless ($interactions->{$rs->db_id}->{$rs->db_id}) {
		    $interactions->{$rs->db_id}->{$rs->db_id}->{'interactors'} = [$rs,$rs];
		}
		push @{$interactions->{$rs->db_id}->{$rs->db_id}->{'complex_context'}}, $complex;
		push @{$interactions->{$rs->db_id}->{$rs->db_id}->{'complex_context'}}, @{$self->get_upstream_complexes_or_polymers($complex)};
		push @{$interactions->{$rs->db_id}->{$rs->db_id}->{'reaction_context'}}, @{$self->get_reaction_for_complex_or_polymer($complex)};
	    }
	}
    }
    
#Check whether this ReferenceSequence is directly involved in a Polymer (i.e. not within a Complex, which would have been handled already) 
    my $polys  = $rs->follow_class_attributes
        (-INSTRUCTIONS =>
         {'ReferenceEntity' => {'reverse_attributes' => ['referenceEntity']},
          'PhysicalEntity' => {'reverse_attributes' => ['hasMember', 'repeatedUnit']}},
         -OUT_CLASSES => ['Polymer']
         );
    foreach my $polymer (@{$polys}) {
	unless ($interactions->{$rs->db_id}->{$rs->db_id}) {
	    $interactions->{$rs->db_id}->{$rs->db_id}->{'interactors'} = [$rs,$rs];
	}
	push @{$interactions->{$rs->db_id}->{$rs->db_id}->{'complex_context'}}, $polymer;
	push @{$interactions->{$rs->db_id}->{$rs->db_id}->{'complex_context'}}, @{$self->get_upstream_complexes_or_polymers($polymer)};
	push @{$interactions->{$rs->db_id}->{$rs->db_id}->{'reaction_context'}}, @{$self->get_reaction_for_complex_or_polymer($polymer)};
    }
    
#get all upstream PhysicalEntities to check for further complex interactors            
    my $pes = $rs->follow_class_attributes
        (-INSTRUCTIONS =>
         {'ReferenceSequence' => {'reverse_attributes' => ['referenceEntity']},
          'PhysicalEntity' => {'reverse_attributes' => ['hasComponent', 'hasMember', 'repeatedUnit']}},
         -OUT_CLASSES => ['PhysicalEntity']
         );
    
    my %pes;
    map {$pes{$_->db_id} = $_} @{$pes};
    
    my %seen = %pes;
    foreach my $pe (@{$pes}) {
	if ($pe->is_a('Complex')) {
#	    $participating_protein_count = $self->participating_protein_count_from_complexes([$pe]);
	    foreach my $pe1 (grep {! $seen{$_->db_id}++} @{$pe->HasComponent}) {
                my $rpss = $pe1->follow_class_attributes
                    (-INSTRUCTIONS =>
                     {'Complex' => {'attributes' => [qw(hasComponent)]},
                      'EntitySet' => {'attributes' => [qw(hasMember)]},
                      'Polymer' => {'attributes' => [qw(repeatedUnit)]},
                      'EntityWithAccessionedSequence' => {'attributes' => ['referenceEntity']}},
                     -OUT_CLASSES => [$out_classes]
                     );
                foreach my $rps (@{$rpss}) {
                    next if ($rs == $rps); #no self-mers at this stage     
		    my ($t1,$t2) = sort {$a->db_id <=> $b->db_id} ($rs,$rps);
                    unless ($interactions->{$t1->db_id}->{$t2->db_id}) {
                        $interactions->{$t1->db_id}->{$t2->db_id}->{'interactors'} = [$t1,$t2];
                    }
                    push @{$interactions->{$t1->db_id}->{$t2->db_id}->{'complex_context'}}, $pe;
                    push @{$interactions->{$t1->db_id}->{$t2->db_id}->{'complex_context'}}, @{$self->get_upstream_complexes_or_polymers($pe)};
                    push @{$interactions->{$t1->db_id}->{$t2->db_id}->{'reaction_context'}}, @{$self->get_reaction_for_complex_or_polymer($pe)};
                }
            }
	}
    }
    
    #Get reactions                          
    foreach my $pe (@{$pes}) {
        my $reactions = $pe->follow_class_attributes
            (-INSTRUCTIONS =>
             {'PhysicalEntity' => {'reverse_attributes' => ['input','output','physicalEntity']}, #do input and output at once     
	      'CatalystActivity' => {'reverse_attributes' => ['catalystActivity']}},
             -OUT_CLASSES => ['Reaction'] #only bona fide Reactions for interactions based on reaction context only  
	     );
        #find other entities(=interactors) in each reaction  
	foreach my $r (@{$reactions}) {
#            $participating_protein_count = $self->participating_protein_count_from_reactions([$r]);
            my $pes1 = $r->follow_class_attributes
                (-INSTRUCTIONS =>
		 {'ReactionlikeEvent' => {'attributes' => ['input','output','catalystActivity']}, #do input and output at once  
		  'CatalystActivity' => {'attributes' => ['physicalEntity']}},
                 -OUT_CLASSES => ['PhysicalEntity']
                 );
            foreach my $pe1 (@{$pes1}) {
		next if $seen{$pe1->db_id}; #add only interactions here that haven't been seen yet in a complex context (also avoids recording interactions between set members - basically one needs to avoid going down the same branch that one has come up on)
		my $rpss = $pe1->follow_class_attributes
                    (-INSTRUCTIONS =>
                     {'Complex' => {'attributes' => ['hasComponent']},
                      'EntitySet' => {'attributes' => ['hasMember']},
                      'Polymer' => {'attributes' => ['repeatedUnit']},
                      'EntityWithAccessionedSequence' => {'attributes' => ['referenceEntity']}},
                     -OUT_CLASSES => [$out_classes]
                     );
                foreach my $rps (@{$rpss}) {
                    next if ($rs == $rps); #again, no self-mers at this point         
		    my ($t1,$t2) = sort {$a->db_id <=> $b->db_id} ($rs,$rps);
                    unless ($interactions->{$t1->db_id}->{$t2->db_id}) {
                        $interactions->{$t1->db_id}->{$t2->db_id}->{'interactors'} = [$t1,$t2];
                    }
                    push @{$interactions->{$t1->db_id}->{$t2->db_id}->{'reaction_context'}}, $r;
                }
            }

# neighbouring, i.e. preceding/following reactions                                          
	    foreach my $nr (@{$r->PrecedingEvent}, @{$r->reverse_attribute_value('precedingEvent')}) {
#		$participating_protein_count = $self->participating_protein_count_from_reactions([$r,$nr]);

		my $pes2 = $nr->follow_class_attributes
		    (-INSTRUCTIONS =>
		     {'ReactionlikeEvent' => {'attributes' => ['input','output','catalystActivity']},
		      'CatalystActivity' => {'attributes' => ['physicalEntity']}},
		     -OUT_CLASSES => ['PhysicalEntity']
		     );
		foreach my $pe2 (@{$pes2}) {
		    next if $seen{$pe2->db_id}; #add only interactions here that haven't been seen yet in a complex context                                                                                                  
		    my $rpss = $pe2->follow_class_attributes
			(-INSTRUCTIONS =>
			 {'Complex' => {'attributes' => ['hasComponent']},
			  'EntitySet' => {'attributes' => ['hasMember']},
			  'Polymer' => {'attributes' => ['repeatedUnit']},
			  'EntityWithAccessionedSequence' => {'attributes' => ['referenceEntity']}},
			 -OUT_CLASSES => [$out_classes]
			 );
		    foreach my $rps (@{$rpss}) {
			next if ($rs == $rps); #again, no self-mers at this point
			my ($t1,$t2) = sort {$a->db_id <=> $b->db_id} ($rs,$rps);
			my %reaction = ($rs->db_id => $r, $rps->db_id => $nr);
			my ($u1, $u2) = ($reaction{$t1->db_id}, $reaction{$t2->db_id}); #to keep the reaction order in parallel with the (sorted) protein identifier order
			unless ($interactions->{$t1->db_id}->{$t2->db_id}) {
			    $interactions->{$t1->db_id}->{$t2->db_id}->{'interactors'} = [$t1,$t2];
			}
			push @{$interactions->{$t1->db_id}->{$t2->db_id}->{'neighbouring_reaction_context'}}, [$u1,$u2];
		    }
		}
	    }
	}
#check for associations via Regulation
	my $upstream_instances = $pe->follow_class_attributes
	    (-INSTRUCTIONS =>
             {'PhysicalEntity' => {'reverse_attributes' => ['input','output','physicalEntity']},
	      'CatalystActivity' => {'reverse_attributes' => ['catalystActivity']}},
             -OUT_CLASSES => ['ReactionlikeEvent', 'CatalystActivity', 'PhysicalEntity']);
	foreach my $i (@{$upstream_instances}) {
	    foreach my $regulation (@{$i->reverse_attribute_value('regulatedEntity')}) {
		my $regulators = $regulation->follow_class_attributes
		    (-INSTRUCTIONS => 
		     {'Regulation' => {'attributes' => [qw(regulator)]},
		      'ReactionlikeEvent' => {'attributes' => ['input','output','catalystActivity']},
                      'CatalystActivity' => {'attributes' => ['physicalEntity']}},
                     -OUT_CLASSES => ['PhysicalEntity']
                     );
		foreach my $pe2 (@{$regulators}) {
                    next if $seen{$pe2->db_id}; #add only interactions here that haven't been seen yet in a complex context                                                 
                    my $rpss = $pe2->follow_class_attributes
                        (-INSTRUCTIONS =>
			 {'Complex' => {'attributes' => [qw(hasComponent)]},
			  'EntitySet' => {'attributes' => [qw(hasMember)]},
			  'Polymer' => {'attributes' => [qw(repeatedUnit)]},
			  'EntityWithAccessionedSequence' => {'attributes' => [qw(referenceEntity)]}},
			 -OUT_CLASSES => [$out_classes]);
		    foreach my $rps (@{$rpss}) {
			next if ($rs == $rps); #again, no self-mers at this point   
			my ($t1,$t2) = sort {$a->db_id <=> $b->db_id} ($rs,$rps);
			unless ($interactions->{$t1->db_id}->{$t2->db_id}) {
			    $interactions->{$t1->db_id}->{$t2->db_id}->{'interactors'} = [$t1,$t2];
			}
			push @{$interactions->{$t1->db_id}->{$t2->db_id}->{'regulation_context'}}, $regulation;
		    }
		}
	    }
	    foreach my $regulation (@{$i->reverse_attribute_value('regulator')}) { #need to do this separately so that one doesn't go down on the branch one has come up on
                my $regulatedEntities = $regulation->follow_class_attributes
                    (-INSTRUCTIONS =>
                     {'Regulation' => {'attributes' => [qw(regulatedEntity)]},
		      'ReactionlikeEvent' => {'attributes' => ['input','output','catalystActivity']},
                      'CatalystActivity' => {'attributes' => ['physicalEntity']}},
                     -OUT_CLASSES => ['PhysicalEntity']
                     );
                foreach my $pe2 (@{$regulatedEntities}) {
                    next if $seen{$pe2->db_id}; #add only interactions here that haven't been seen yet in a complex context    
                    my $rpss = $pe2->follow_class_attributes
                        (-INSTRUCTIONS =>
			 {'Complex' => {'attributes' => [qw(hasComponent)]},
			  'EntitySet' => {'attributes' => [qw(hasMember)]},
			  'Polymer' => {'attributes' => [qw(repeatedUnit)]},
			  'EntityWithAccessionedSequence' => {'attributes' => [qw(referenceEntity)]}},
			 -OUT_CLASSES => [$out_classes]);
		    foreach my $rps (@{$rpss}) {
			next if ($rs == $rps); #again, no self-mers at this point    
			my ($t1,$t2) = sort {$a->db_id <=> $b->db_id} ($rs,$rps);
			unless ($interactions->{$t1->db_id}->{$t2->db_id}) {
			    $interactions->{$t1->db_id}->{$t2->db_id}->{'interactors'} = [$t1,$t2];
			}
			push @{$interactions->{$t1->db_id}->{$t2->db_id}->{'regulation_context'}}, $regulation;
		    }
		}
	    }
	}
    }
    return $interactions;
}

sub get_reaction_for_complex_or_polymer {
    my ($self, $complex) = @_;
    my $reactions = $complex->follow_class_attributes
        (-INSTRUCTIONS =>
         {'PhysicalEntity' => {'reverse_attributes' => ['input','output','physicalEntity', 'hasMember', 'hasComponent', 'repeatedUnit']},
          'CatalystActivity' => {'reverse_attributes' => ['catalystActivity']}},
         -OUT_CLASSES => ['ReactionlikeEvent']
         );
    return $reactions;
}

sub get_upstream_complexes_or_polymers {
    my ($self, $complex) = @_;
    my $complexes = $complex->follow_class_attributes
        (-INSTRUCTIONS =>
         {'PhysicalEntity' => {'reverse_attributes' => ['hasMember', 'hasComponent', 'repeatedUnit']}},
         -OUT_CLASSES => ['Complex', 'Polymer']
         );
    return $complexes;

}

sub stableIdentifier_or_id {
    my ($self, $i) = @_;
    return ($i->is_valid_attribute('stableIdentifier') && $i->StableIdentifier->[0]
            ? $i->StableIdentifier->[0]->Identifier->[0]
            : $i->db_id);
}

sub stableIdentifier_or_class_and_id {
    my ($self, $i) = @_;
    return ($i->is_valid_attribute('stableIdentifier') && $i->StableIdentifier->[0]
            ? $i->StableIdentifier->[0]->Identifier->[0]
            : $i->class_and_id);
}

sub get_refdb {
    my ($i) = @_;
    my %psi_cv = (
                  #'Dictybase' => ?,                                         
                 'EMBL' => 'EMBL',
                  'ENSEMBL' => 'ensembl',
                  'Entrez' => 'refseq',
                  'Entrez Gene' => 'entrez gene/locuslink',
                  'Flybase' => 'flybase',
                  #'GeneDB' => ?,                                                                                               
                  #'PlasmoDB' => ?,                                          
		  'RefSeq' => 'refseq',
                  'SGD' => 'sgd',
                  #'TIGR' => ?,                                                  
                  #'TIGR cna' => ?,                                                               
		  #'TIGR eha' => ?,                                                                             
		  #'TIGR osa' => ?,                                                                
		  'UniProt' => 'uniprotkb',
                  'Wormbase' => 'wormbase'
                  );

    my $refdb = $i->ReferenceDatabase->[0]->Name->[0];
    if ($psi_cv{$refdb}) {
        return $psi_cv{$refdb};
    } else {
        return $refdb;
    }
}

#Provided with interactions_hash                               
#for testing use id 48889 for human, 399786 for mouse                    
#test whether non mitab still produces the same result             
#adjust refdb names for non-human                                             
sub print_psi_mitab_interaction {
    my ($self, $hr) = @_;
#first line                                                                              
    print "unique id A\tunique id B\talternative id A\talternative id B\taliases for A (empty)\taliases for B (empty)\tinteraction detection method\tfirst author\tId of publication\tNCBI taxon A\tNCBI taxon B\tinteraction type\tsource db\tinteraction in source db (NA)\tconfidence score (NA)\tpathway id(s)\treaction ids(s)\tcomplex/polymer id(s)\tregulation id(s)\n";
    foreach my $db_id1 (keys %{$hr}) {
        foreach my $db_id2 (keys %{$hr->{$db_id1}}) {
            my @row;
#add columns 1 and 2                                                      
	    foreach my $i (@{$hr->{$db_id1}->{$db_id2}->{'interactors'}}) {
                my $refdb = get_refdb($i);
		my $identifier = ($i->is_valid_attribute('variantIdentifier') && $i->VariantIdentifier->[0])?$i->VariantIdentifier->[0]:$i->Identifier->[0];
                push @row, ($refdb.":".$identifier);
            }
#add columns 3 and 4                                                                 
	    foreach my $i (@{$hr->{$db_id1}->{$db_id2}->{'interactors'}}) {
                my $refdb = get_refdb($i);
                my $gene_name = $i->GeneName->[0];
                my $alt_name = $gene_name?$refdb.":".$i->Identifier->[0]."_".$i->GeneName->[0]."(shortlabel)":'-';
                push @row, $alt_name;
            }
#add columns 5 and 6                                                            
	    foreach my $i (@{$hr->{$db_id1}->{$db_id2}->{'interactors'}}) {
                push @row, "-";
            }
#add column 7 - interaction detection method                                                       
	    $row[6] = 'psi-mi:"MI:0364"(inferred by curator)';
#add columns 8 and 9 - Reactome literature reference                                                 
	    $row[7] = 'Vastrik et al. (2007)';
            $row[8] = 'pubmed:17367534';
#add columns 10 and 11                                                           
	    $row[9] = "taxid:".$hr->{$db_id1}->{$db_id2}->{'interactors'}->[0]->Species->[0]->CrossReference->[0]->Identifier->[0];
            $row[10] = "taxid:".$hr->{$db_id1}->{$db_id2}->{'interactors'}->[1]->Species->[0]->CrossReference->[0]->Identifier->[0];
	    next unless ($row[9] eq $row[10]); #exclude multi-species interactions
#set column 12 to default (may be overridden for neighbouring reactions)                    
	    $row[11] = 'psi-mi:"MI:0915"(physical association)';
#add column 13 - source db                                                                          
	    $row[12] = 'psi-mi:"MI:0467"(reactome)';
#add columns 14 and 15 - interaction id in source db and confidence score - leave empty        
	    $row[13] = "-";
            $row[14] = "-";
#default for regulation column is empty
	    $row[18] = '-';

#fill complex column                                                         
	    my %seen_complex;
            my @tmp_complex ;
            foreach my $complex (@{$hr->{$db_id1}->{$db_id2}->{'complex_context'}}) {
                next if $seen_complex{$complex->db_id}++;
                push @tmp_complex, 'reactome:'.$self->stableIdentifier_or_id($complex);
            }
            my $tmp_c = $self->stringify_ids(\@tmp_complex,1); #mitab flag set to 1                   
	    $row[17] = $tmp_c?$tmp_c:'-';
#fill reaction column                                                           
	    my %seen_reaction;
            my @tmp_reaction;
            my @tmp_reaction_ids;
	    if ($hr->{$db_id1}->{$db_id2}->{'reaction_context'}) {
		foreach my $reaction (@{$hr->{$db_id1}->{$db_id2}->{'reaction_context'}}) {
		    next unless (defined $reaction);
		    next if $seen_reaction{$reaction->db_id}++;
		    push @tmp_reaction, 'reactome:'.$self->stableIdentifier_or_id($reaction);
		    push @tmp_reaction_ids, $reaction->db_id;
		}
	    } elsif ($hr->{$db_id1}->{$db_id2}->{'neighbouring_reaction_context'} || $hr->{$db_id1}->{$db_id2}->{'regulation_context'}) { #neighbouring reaction or regulation context will only be relevant if there is no reaction context - they are meant to be mutually exclusive
#override column 12
		#next;
		$row[11] = 'psi-mi:"MI:0914"(association)';
		foreach my $reaction_pair (@{$hr->{$db_id1}->{$db_id2}->{'neighbouring_reaction_context'}}) {
		    next unless ($reaction_pair->[0]);
		    my $pair_id = 'reactome:'.$self->stableIdentifier_or_id($reaction_pair->[0]).'|'.'reactome:'.$self->stableIdentifier_or_id($reaction_pair->[1]); #keep same separator as for multiple reactions in physical association context to avoid parsing problems (for now report pairs of reactions - if preferred one could just list each reaction once, without attempting to keep the pairs together)
		    next if $seen_reaction{$pair_id}++;
		    push @tmp_reaction, $pair_id;
		    push @tmp_reaction_ids, ($reaction_pair->[0]->db_id, $reaction_pair->[1]->db_id);
		}
		if ($hr->{$db_id1}->{$db_id2}->{'regulation_context'}) {
		    my %seen_regulation;
		    my @tmp_regulation;
		    foreach my $regulation (@{$hr->{$db_id1}->{$db_id2}->{'regulation_context'}}) {
			next unless (defined $regulation);
			next if $seen_regulation{$regulation->db_id}++;
			push @tmp_regulation, 'reactome:'.$self->stableIdentifier_or_id($regulation);
			#get direct Reactions and Reactions via CatalystActivities that are involved in Regulations (don't follow regulators that are PhysicalEntities upwards, as such reactions would come from a different context than the regulation in question)
			my $reactions = $regulation->follow_class_attributes
			    (-INSTRUCTIONS =>
			     {'Regulation' => {'attributes' => [qw(regulator regulatedEntity)]},
			      'CatalystActivity' => {'reverse_attributes' => [qw(catalystActivity)]}},
			     -OUT_CLASSES => ['ReactionlikeEvent']);
			foreach my $reaction (@{$reactions}) {
			    push @tmp_reaction, 'reactome:'.$self->stableIdentifier_or_id($reaction);
			    push @tmp_reaction_ids, $reaction->db_id;
			}			
		    }
		    my $tmp_reg = $self->stringify_ids(\@tmp_regulation,1);
		    $row[18] = $tmp_reg?$tmp_reg:'-';
		}
	    }
	    my $tmp_r = $self->stringify_ids(\@tmp_reaction,1);
	    $row[16] = $tmp_r?$tmp_r:'-';
#fill pathway column
	    my $pathway_ids = $self->pathway_db_ids_from_reaction_db_ids(\@tmp_reaction_ids, 1);
	    my $tmp_p = $self->stringify_ids($pathway_ids,1);
	    $row[15] = $tmp_p?$tmp_p:'-';
	    
            $self->print_array(\@row);
        }
    }
}


# Returns an array with one element for each unique pair of proteins.
# Each element is a reference to an array, with the following (sub)elements:
#
# 0	UniProt ID for the first interacting protein
# 1	UniProt ID for the second interacting protein
# 2 direct complex information (undef if this is not a direct complex)
# 3 complex information (undef if this is not a complex)
# 4 reaction information (undef if this is not a reaction)
# 5 neighboring reaction information (undef if this is not a neighboring reaction)
# 6 IntAct IDs (if any) associated with this interaction
sub interaction_hash_2_split_array {
    my ($self, $hr) = @_;
    
    my @out = ();
    foreach my $db_id1 (keys %{$hr}) {
		foreach my $db_id2 (keys %{$hr->{$db_id1}}) {
		    push @out, [
		    	@{$hr->{$db_id1}->{$db_id2}->{'interactors'}},
				[values %{$hr->{$db_id1}->{$db_id2}->{'direct_complex_context'}}],
				[values %{$hr->{$db_id1}->{$db_id2}->{'complex_context'}}],
				[values %{$hr->{$db_id1}->{$db_id2}->{'reaction_context'}}],
				$hr->{$db_id1}->{$db_id2}->{'neighbouring_reaction_context'},
				$hr->{$db_id1}->{$db_id2}->{'IntActIds'}
			];
		}
    }
    
    return \@out;
}

# Insert IntAct cross references into the Reactome database, based on
# the interactions generated.
# You can limit the interactions inserted to those involving any
# number of proteins, by setting participating_protein_count_cutoff
# to a numerical value.  Interactions involving less than the given
# cutoff value will be inserted, those involving more proteins will
# be ignored.
sub insert_intact_xrefs {
    my ($self, $hr, $participating_protein_count_cutoff) = @_;
    
    my $logger = get_logger(__PACKAGE__);
    
    $logger->info("InteractionGenerator.insert_intact_xrefs: hey-ho, inserting some cross-references, ");
    if (defined $participating_protein_count_cutoff) {
    	$logger->info("participating_protein_count_cutoff=$participating_protein_count_cutoff");
    } else {
    	$logger->info("participating_protein_count_cutoff is undef");
    }
    
    # Interaction data is stored in a hash of hashes, the outer key
    # being the DB_ID of the first interacting protein, the inner
    # key being the DB_ID of the second interacting protein.  Loop
    # over both to get at the individual interactor pairs.
    foreach my $db_id1 (keys %{$hr}) {
		foreach my $db_id2 (keys %{$hr->{$db_id1}}) {
			$self->insert_intact_xrefs_for_single_interaction($hr->{$db_id1}->{$db_id2}, $participating_protein_count_cutoff);
		}
    }
}

# You can limit the interactions inserted to those involving any
# number of proteins, by setting participating_protein_count_cutoff
# to a numerical value.  Interactions involving less than the given
# cutoff value will be inserted, those involving more proteins will
# be ignored.
sub insert_intact_xrefs_for_single_interaction {
    my ($self, $interaction, $participating_protein_count_cutoff) = @_;
    
	# The IntAct IDs, if there are any for the given protein pair.
	my $intact_ids = $interaction->{'IntActIds'};
	if (!(defined $intact_ids) || scalar(@{$intact_ids})<1) {
		# No xrefs to insert if no IntAct IDs were found, so skip the
		# rest.
		return;
	}
			
	# Pull out the 4 different types of interaction.  Some of these
	# may be undef, since not all types of interaction will necessarily
	# generate the same pair of interacting proteins.
	my $direct_complexes = [values %{$interaction->{'direct_complex_context'}}];
	my $complexes = [values %{$interaction->{'complex_context'}}];
	my $reactions = [values %{$interaction->{'reaction_context'}}];
	my $neighbouring_reaction_pairs = $interaction->{'neighbouring_reaction_context'};
			
	$self->insert_intact_xrefs_into_complexes($direct_complexes, $intact_ids, $participating_protein_count_cutoff);
	$self->insert_intact_xrefs_into_complexes($complexes, $intact_ids, $participating_protein_count_cutoff);
	$self->insert_intact_xrefs_into_reactions($reactions, $intact_ids, $participating_protein_count_cutoff);
	$self->insert_intact_xrefs_into_neighbouring_reaction_pairs($neighbouring_reaction_pairs, $intact_ids, $participating_protein_count_cutoff);
}

# Take the given list of instances, and insert cross-references for
# the list of IntAct IDs.
# You can limit the interactions inserted to those involving any
# number of proteins, by setting participating_protein_count_cutoff
# to a numerical value.  Interactions involving less than the given
# cutoff value will be inserted, those involving more proteins will
# be ignored.
sub insert_intact_xrefs_into_reactions {
    my ($self, $instances, $intact_ids, $participating_protein_count_cutoff) = @_;
    
	if (!(defined $instances) || scalar(@{$instances})<=0) {
		# There wern't any instances, actually, so give up.
		return;
	}
	
    my $instance;
    my $participating_protein_count;
	foreach $instance (@{$instances}) {
		if (defined $participating_protein_count_cutoff) {
    		$participating_protein_count = $self->participating_protein_count_from_reactions([$instance]);
    		if ($participating_protein_count <= $participating_protein_count_cutoff) {
				$self->insert_intact_xrefs_into_instance($instance, $intact_ids);
    		}
		} else {
			$self->insert_intact_xrefs_into_instance($instance, $intact_ids);
		}
	}
}

# Take the given list of instances, and insert cross-references for
# the list of IntAct IDs.
# You can limit the interactions inserted to those involving any
# number of proteins, by setting participating_protein_count_cutoff
# to a numerical value.  Interactions involving less than the given
# cutoff value will be inserted, those involving more proteins will
# be ignored.
sub insert_intact_xrefs_into_complexes {
    my ($self, $instances, $intact_ids, $participating_protein_count_cutoff) = @_;
    
	if (!(defined $instances) || scalar(@{$instances})<=0) {
		# There wern't any instances, actually, so give up.
		return;
	}
	
    my $instance;
    my $participating_protein_count;
	foreach $instance (@{$instances}) {
		if (defined $participating_protein_count_cutoff) {
    		$participating_protein_count = $self->participating_protein_count_from_complexes([$instance]);
    		if ($participating_protein_count <= $participating_protein_count_cutoff) {
				$self->insert_intact_xrefs_into_instance($instance, $intact_ids);
    		}
		} else {
			$self->insert_intact_xrefs_into_instance($instance, $intact_ids);
		}
	}
}

# Take the given list of neighbouring reaction pairs, and insert cross-references for
# the list of IntAct IDs.
# You can limit the interactions inserted to those involving any
# number of proteins, by setting participating_protein_count_cutoff
# to a numerical value.  Interactions involving less than the given
# cutoff value will be inserted, those involving more proteins will
# be ignored.
sub insert_intact_xrefs_into_neighbouring_reaction_pairs {
    my ($self, $neighbouring_reaction_pairs, $intact_ids, $participating_protein_count_cutoff) = @_;
    
	if (!(defined $neighbouring_reaction_pairs) || scalar(@{$neighbouring_reaction_pairs})<=0) {
		# There wern't any neighbouring reaction pairs, actually, so give up.
		return;
	}
	
    my $neighbouring_reaction_pair;
    my $participating_protein_count;
	foreach $neighbouring_reaction_pair (@{$neighbouring_reaction_pairs}) {
		if (defined $participating_protein_count_cutoff) {
    		$participating_protein_count = $self->participating_protein_count_from_reactions($neighbouring_reaction_pair);
    		if ($participating_protein_count <= $participating_protein_count_cutoff) {
				$self->insert_intact_xrefs_into_instance($neighbouring_reaction_pair->[0], $intact_ids);
				$self->insert_intact_xrefs_into_instance($neighbouring_reaction_pair->[1], $intact_ids);
    		}
		} else {
			$self->insert_intact_xrefs_into_instance($neighbouring_reaction_pair->[0], $intact_ids);
			$self->insert_intact_xrefs_into_instance($neighbouring_reaction_pair->[1], $intact_ids);
		}
	}
}

# Take the given instance, and insert cross-references for
# the list of IntAct IDs.
sub insert_intact_xrefs_into_instance {
    my ($self, $instance, $intact_ids) = @_;
    
    my $logger = get_logger(__PACKAGE__);
    
    my $dba = $self->dba;
    my $reference_database_intact = GKB::Utils::get_reference_database_intact($dba);

    my $cross_reference_attribute_name = 'interactionIdentifier';
	if (!($instance->is_valid_attribute($cross_reference_attribute_name))) {
	    $cross_reference_attribute_name = 'crossReference';
		if (!($instance->is_valid_attribute($cross_reference_attribute_name))) {
			$logger->warn("InteractionGenerator.insert_intact_xrefs_into_instance: WARNING - instance of type " . $instance->class()
			. " does not support attribute $cross_reference_attribute_name, ignoring");
			return;
		}
	}
	
	my $cross_references = $instance->$cross_reference_attribute_name;
	if (scalar(@{$cross_references}) == 1 && scalar($cross_references->[0]) =~ /ARRAY/) {
		# Weird but common case: cross references is actually an infinitely
		# recursive, self-referencing array!
		$cross_references = [];
	}

	# Find already-existing IntAct cross-references and note their IDs
	my %existing_intact_ids = ();
    my $cross_reference;
	foreach $cross_reference (@{$cross_references}) {
		if ($cross_reference->referenceDatabase->[0]->db_id() == $reference_database_intact->db_id()) {
			$existing_intact_ids{$cross_reference->identifier->[0]} = 1;
		}
	}
		
	$logger->info("InteractionGenerator.insert_intact_xrefs_into_instance: inserting xrefs into instance " . $instance->db_id() . ":" . $instance->_displayName->[0] . ": ");
		
	# Get or create new cross-references for the IntAct IDs
	# associated with the current interaction.
    my $intact;
    my $intact_id;
	foreach $intact_id (@{$intact_ids}) {
		$intact_id =~ s/^intact://i;
		
		$logger->info("$intact_id");
			
		if (!$existing_intact_ids{$intact_id}) {
			$intact = GKB::Utils::get_database_identifier($dba, $reference_database_intact, $intact_id);
			push(@{$cross_references}, $intact);
		}
	}
		
	# Overwrite existing list of cross-references
	$instance->attribute_value($cross_reference_attribute_name, undef);
	$instance->attribute_value($cross_reference_attribute_name, @{$cross_references});
	
	# Update to database
	$dba->update_attribute($instance, $cross_reference_attribute_name);
}

# Print all interactions, with optional headers.
sub print_interaction_report {
    my ($self, $interactions) = @_;
    
    $self->print_headers($interactions);
    $self->print_interactions($interactions);
}

# Print the header lines that appear at the top of the o/p file.  They
# all begin with a hash symbol.
sub print_headers {
    my ($self, $interactions) = @_;
    
    if (defined $self->title_headers_flag && $self->title_headers_flag == 1) {
    	print "# List of Interactions Generated from Reactome Data\n";
    }
    
    if (defined $self->interaction_count_headers_flag && $self->interaction_count_headers_flag == 1) {
    	print '# ', scalar(@{$interactions}), "\tunique interactions\n";
    }
    
    if (defined $self->table_headers_flag && $self->table_headers_flag == 1) {
    	my $line = "";
    	if ($self->column_groups->{$COLUMN_GROUP_IDS}) {
    		if (!($line eq "")) {
    			$line .= "\t";
    		}
    		$line .= "Protein1\tEnsembl1\tEntrez1\tProtein2\tEnsembl2\tEntrez2";
    	}
     	if ($self->column_groups->{$COLUMN_GROUP_UNIPROT_IDS}) {
    		if (!($line eq "")) {
    			$line .= "\t";
    		}
    		$line .= "Protein1\tProtein2";
    	}
    	if ($self->column_groups->{$COLUMN_GROUP_CONTEXT}) {
    		if (!($line eq "")) {
    			$line .= "\t";
    		}
    		$line .= "Context";
    	}
    	if ($self->column_groups->{$COLUMN_GROUP_SOURCE_IDS}) {
    		if (!($line eq "")) {
    			$line .= "\t";
    		}
    		$line .= "Pathways\tReactions\tComplexes";
    	}
    	if ($self->column_groups->{$COLUMN_GROUP_SOURCE_STABLE_IDS}) {
    		if (!($line eq "")) {
    			$line .= "\t";
    		}
    		$line .= "Pathways\tReactions\tComplexes";
    	}
    	if ($self->column_groups->{$COLUMN_GROUP_DECORATED_SOURCE_IDS}) {
    		if (!($line eq "")) {
    			$line .= "\t";
    		}
    		$line .= "ReactomeIDs";
    	}
    	if ($self->column_groups->{$COLUMN_GROUP_LIT_REFS}) {
    		if (!($line eq "")) {
    			$line .= "\t";
    		}
    		$line .= "LiteratureRefs";
    	}
    	if ($self->column_groups->{$COLUMN_GROUP_PARTICIPATING_PROTEIN_COUNT}) {
    		if (!($line eq "")) {
    			$line .= "\t";
    		}
    		$line .= "ProteinCount";
    	}
    	if ($self->column_groups->{$COLUMN_GROUP_INTACT}) {
    		if (!($line eq "")) {
    			$line .= "\t";
    		}
    		$line .= "IntActIDs";
    	}
    	
    	print "# $line\n";
    }
}

# Print the supplied list of interactions
sub print_interactions {
    my ($self, $interactions) = @_;
    
	my $interaction;
    foreach $interaction (@{$interactions}) {
    	$self->print_interaction($interaction);
    }
}

# Print a single interaction
sub print_interaction {
    my ($self, $interaction) = @_;
        
    my @intact_id = ();
	if ($self->column_groups->{$COLUMN_GROUP_INTACT}) {
		my $intact_id_string = $self->stringify_intact_ids($interaction->[6]);
		
		if ($self->display_only_intact_lines && $intact_id_string eq "") {
			# No IntAct ID was found and the display_only_intact_lines was
			# set, so skip this interaction.
			return;
		}
		
		@intact_id = ($intact_id_string)
	}
	
	my @interactors = ();
	if ($self->column_groups->{$COLUMN_GROUP_IDS}) {
		push(@interactors, (_interactor_info($interaction->[0]), _interactor_info($interaction->[1])));
	}
	if ($self->column_groups->{$COLUMN_GROUP_UNIPROT_IDS}) {
		push(@interactors, ($interaction->[0]->Identifier->[0], $interaction->[1]->Identifier->[0]));
	}
		
	# The interaction between a given pair of proteins may have been
	# noted in several "contexts", e.g. multiple direct complexes,
	# plus a reaction.  For each of these, a new line of output is
	# generated.
	my $context;
	my @context_name = ();
	my $source_ids = "";
	if ($interaction->[2]) {
		if ($self->column_groups->{$COLUMN_GROUP_CONTEXT}) {
			@context_name = ("direct_complex");
		}
		foreach $context (@{$interaction->[2]}) {
			$self->print_array([@interactors, @context_name, @{$self->_interaction_context_info($context, 2)}, @intact_id]);
		}
	}
	if ($interaction->[3]) {
		if ($self->column_groups->{$COLUMN_GROUP_CONTEXT}) {
			@context_name = ("indirect_complex");
		}
		foreach $context (@{$interaction->[3]}) {
			$self->print_array([@interactors, @context_name, @{$self->_interaction_context_info($context, 3)}, @intact_id]);
		}
	}
	if ($interaction->[4]) {
		if ($self->column_groups->{$COLUMN_GROUP_CONTEXT}) {
			@context_name = ("reaction");
		}
		foreach $context (@{$interaction->[4]}) {
			$self->print_array([@interactors, @context_name, @{$self->_interaction_context_info($context, 4)}, @intact_id]);
		}
	}
	if ($interaction->[5]) {
		# Annoying special case
		if ($self->column_groups->{$COLUMN_GROUP_CONTEXT}) {
			@context_name = ("neighbouring_reaction");
		}
		my @source_lit_refs = ();
		if ($self->column_groups->{$COLUMN_GROUP_LIT_REFS}) {
			@source_lit_refs = ("");
		}
		my @source_id_info;
		my @source_stable_id_info;
		my @decorated_source_id_info;
		my @participating_protein_count;
		my $reaction_pair;
		foreach $reaction_pair (@{$interaction->[5]}) {
		    @source_id_info = ();
			if ($self->column_groups->{$COLUMN_GROUP_SOURCE_IDS}) {
		    	@source_id_info = @{$self->neighbouring_reaction_source_id_info_from_reaction_pair($reaction_pair)};
			}
		    @source_stable_id_info = ();
			if ($self->column_groups->{$COLUMN_GROUP_SOURCE_STABLE_IDS}) {
				@source_stable_id_info = @{$self->neighbouring_reaction_source_id_info_from_reaction_pair($reaction_pair, 1)};
			}
		    @decorated_source_id_info = ();
		    if ($self->column_groups->{$COLUMN_GROUP_DECORATED_SOURCE_IDS}) {
				@decorated_source_id_info = ($reaction_pair->[0]->stableIdentifier_or_class_and_id . "<->" . $reaction_pair->[1]->stableIdentifier_or_class_and_id);
			}
		    @participating_protein_count = ();
		    if ($self->column_groups->{$COLUMN_GROUP_PARTICIPATING_PROTEIN_COUNT}) {
		    	my $reactions = [$reaction_pair->[0], $reaction_pair->[1]];
				@participating_protein_count = ($self->participating_protein_count_from_reactions($reactions));
			}

			$self->print_array([@interactors, @context_name, @source_id_info, @source_stable_id_info, @decorated_source_id_info, @source_lit_refs, @participating_protein_count, @intact_id]);
		}
	}
}

# Imre's original version
#sub _find_interactors_for_ReferenceSequence {
#    my ($rs,$interactions) = @_;
#    $interactions ||= {};
#
#    my %neighbouring_reaction_instructions = 
#	(
#	 -INSTRUCTIONS =>
#	 {
##	     'EquivalentEventSet' => {'attributes' => [qw(hasMember)]},
##	     'ConceptualEvent' => {'attributes' => [qw(hasMember)]},
#	     'Reaction' => {'attributes' => [qw(input output catalystActivity)]},
#	     'CatalystActivity' => {'attributes' => [qw(physicalEntity)]},
#	     'Complex' => {'attributes' => [qw(hasComponent)]},
#	     'EntitySet' => {'attributes' => [qw(hasMember)]},
#	     'EntityWithAccessionedSequence' => {'attributes' => [qw(referenceEntity)]}},
#	 -OUT_CLASSES => ['ReferencePeptideSequence']
#	 );
#
#    # Find PhysicalEntities referring to this ReferenceEntity
#    my @pes = grep {$_->is_a('PhysicalEntity')} @{$rs->reverse_attribute_value('referenceEntity')};
#    my @tmp;
#    # Add Generics of those PhysicalEntities
#    foreach my $pe (@pes) {
#	my $ar = $pe->follow_class_attributes
#	    (-INSTRUCTIONS =>
#	     {'PhysicalEntity' => {'reverse_attributes' => ['hasMember']}},
#	     -OUT_CLASSES => ['PhysicalEntity']
#	     );
#	push @tmp, @{$ar};
#    }
#    @pes = @tmp;
#    # Find immediate complexes containing those entities.
#    my %direct_complexes;
#    foreach my $pe (@pes) {
#	#print $pe->extended_displayName, "\n";
#	foreach my $complex (@{$pe->reverse_attribute_value('hasComponent')}) {
#	    $direct_complexes{$complex->db_id} = $complex;
#	    # Skip the original component
#	    foreach my $component (grep {$_ != $pe} @{$complex->HasComponent}) {
#		my $rss = $component->follow_class_attributes
#		    (-INSTRUCTIONS =>
#		     {'EntitySet' => {'attributes' => ['hasMember']},
#		      'EntityWithAccessionedSequence' => {'attributes' => ['referenceEntity']}},
#		     -OUT_CLASSES => ['ReferencePeptideSequence']
#		     );
#		foreach my $rs2 (@{$rss}) {
#		    my ($t1,$t2) = sort {$a->db_id <=> $b->db_id} ($rs,$rs2);
#		    unless ($interactions->{$t1->db_id}->{$t2->db_id}) {
#			$interactions->{$t1->db_id}->{$t2->db_id}->{'interactors'} = [$t1,$t2];
#		    }
#		    $interactions->{$t1->db_id}->{$t2->db_id}->{'direct_complex_context'}->{$complex->db_id} = $complex;
#		}
#	    }
#	}
#    }
#
#    my $pes = $rs->follow_class_attributes
#	(-INSTRUCTIONS =>
#	 {'ReferenceSequence' => {'reverse_attributes' => ['referenceEntity']},
#	  'PhysicalEntity' => {'reverse_attributes' => ['hasComponent', 'hasMember']}},
#	 -OUT_CLASSES => ['PhysicalEntity']
#	 );
#
#    my %pes;
#    map {$pes{$_->db_id} = $_} @{$pes};
#
#    #print $rs->extended_displayName, "\n";
#    foreach my $pe (@{$pes}) {
#	#print $pe->extended_displayName, "\n";
#	# Only count interaction via reaction if the other ReferenceSequence not in
#	# the same complex with the given ReferenceSequence.
#
#	# Handle input and output separately to make it easier.
#
#	# input
#	my $reactions = $pe->follow_class_attributes
#	    (-INSTRUCTIONS =>
#	     {'PhysicalEntity' => {'reverse_attributes' => ['input','physicalEntity']},
#	      'CatalystActivity' => {'reverse_attributes' => ['catalystActivity']}},
#	     -OUT_CLASSES => ['Reaction']
#	     );
#	foreach my $r (@{$reactions}) {
##	    print "\tinput\t", $r->extended_displayName, "\n";
#	    my $pes1 = $r->follow_class_attributes
#		(-INSTRUCTIONS =>
#		 {'Reaction' => {'attributes' => ['input','catalystActivity']},
#		  'CatalystActivity' => {'attributes' => ['physicalEntity']}},
#		 -OUT_CLASSES => ['PhysicalEntity']
#		 );
#	    foreach my $pe1 (@{$pes1}) {
#		next if ($pe1 == $pe);
#		my $rpss = $pe1->follow_class_attributes
#		    (-INSTRUCTIONS =>
#		     {'Complex' => {'attributes' => [qw(hasComponent)]},
#		      'EntitySet' => {'attributes' => [qw(hasMember)]},
#		      'EntityWithAccessionedSequence' => {'attributes' => ['referenceEntity']}},
#		     -OUT_CLASSES => ['ReferencePeptideSequence']
#		     );
#		foreach my $rps (@{$rpss}) {
#		    my ($t1,$t2) = sort {$a->db_id <=> $b->db_id} ($rs,$rps);
#		    unless ($interactions->{$t1->db_id}->{$t2->db_id}) {
#			$interactions->{$t1->db_id}->{$t2->db_id}->{'interactors'} = [$t1,$t2];
#		    }
##		    $interactions->{$t1->db_id}->{$t2->db_id}->{'context'}->{$r->db_id} = $r;
#		    $interactions->{$t1->db_id}->{$t2->db_id}->{'reaction_context'}->{$r->db_id} = $r;
#		}
#	    }
#	    # neighbouring, i.e. preceding/following reactions
#	    foreach my $nr (@{$r->PrecedingEvent}, @{$r->reverse_attribute_value('precedingEvent')}) {
#		my $rpss = $nr->follow_class_attributes(%neighbouring_reaction_instructions);
#		foreach my $rps (@{$rpss}) {
#		    my ($t1,$t2) = sort {$a->db_id <=> $b->db_id} ($rs,$rps);
#		    unless ($interactions->{$t1->db_id}->{$t2->db_id}) {
#			$interactions->{$t1->db_id}->{$t2->db_id}->{'interactors'} = [$t1,$t2];
#		    }
#		    push @{$interactions->{$t1->db_id}->{$t2->db_id}->{'neighbouring_reaction_context'}}, [$r,$nr];
#		}
#	    }
#	}
#
#	# output
#	$reactions = $pe->follow_class_attributes
#	    (-INSTRUCTIONS =>
#	     {'PhysicalEntity' => {'reverse_attributes' => ['output','physicalEntity']},
#	      'CatalystActivity' => {'reverse_attributes' => ['catalystActivity']}},
#	     -OUT_CLASSES => ['Reaction']
#	     );
#	foreach my $r (@{$reactions}) {
##	    print "\toutput\t", $r->extended_displayName, "\n";
#	    my $pes1 = $r->follow_class_attributes
#		(-INSTRUCTIONS =>
#		 {'Reaction' => {'attributes' => ['output','catalystActivity']},
#		  'CatalystActivity' => {'attributes' => ['physicalEntity']}},
#		 -OUT_CLASSES => ['PhysicalEntity']
#		 );
#	    foreach my $pe1 (@{$pes1}) {
#		next if ($pe1 == $pe);
#		my $rpss = $pe1->follow_class_attributes
#		    (-INSTRUCTIONS =>
#		     {'Complex' => {'attributes' => [qw(hasComponent)]},
#		      'EntitySet' => {'attributes' => [qw(hasMember)]},
#		      'EntityWithAccessionedSequence' => {'attributes' => ['referenceEntity']}},
#		     -OUT_CLASSES => ['ReferencePeptideSequence']
#		     );
#		foreach my $rps (@{$rpss}) {
#		    my ($t1,$t2) = sort {$a->db_id <=> $b->db_id} ($rs,$rps);
#		    unless ($interactions->{$t1->db_id}->{$t2->db_id}) {
#			$interactions->{$t1->db_id}->{$t2->db_id}->{'interactors'} = [$t1,$t2];
#		    }
#		    $interactions->{$t1->db_id}->{$t2->db_id}->{'reaction_context'}->{$r->db_id} = $r;
#		}
#	    }
#	    # neighbouring, i.e. preceding/following reactions
#	    foreach my $nr (@{$r->PrecedingEvent}, @{$r->reverse_attribute_value('precedingEvent')}) {
#		my $rpss = $nr->follow_class_attributes(%neighbouring_reaction_instructions);
#		foreach my $rps (@{$rpss}) {
#		    my ($t1,$t2) = sort {$a->db_id <=> $b->db_id} ($rs,$rps);
#		    unless ($interactions->{$t1->db_id}->{$t2->db_id}) {
#			$interactions->{$t1->db_id}->{$t2->db_id}->{'interactors'} = [$t1,$t2];
#		    }
#		    push @{$interactions->{$t1->db_id}->{$t2->db_id}->{'neighbouring_reaction_context'}}, [$r,$nr];
#		}
#	    }
#	}
#
#	if ($pe->is_a('Complex') and ! $direct_complexes{$pe->db_id}) {
#	    # Skip the component(s) which were on the path from the given ReferenceSequence
#	    # to this Complex. This way we don't count other (alternative) instances as interactors.
#	    # Also, of the nested complexes only the smallest is reported as interaction context.
#	    # However, if the given ReferencesSequence is "present" in multiple sub-complexes, i.e
#	    # something like:
#	    #
#	    # C1--p->C2--p->A
#            #   |      |
#	    #   |      |-p->B
#	    #   |
#            #   |-p->C3--p->A
#	    #          |
#	    #          |-p->B
#            #          |
#            #          |-p->C
#	    #
#	    # then also the super-complex, in this case C1 will be reported as context for
#	    # A:B interaction
#
#	    my %seen = %pes;
#	    foreach my $pe1 (grep {! $seen{$_->db_id}++} @{$pe->HasComponent}) {
#		my $rpss = $pe1->follow_class_attributes
#		    (-INSTRUCTIONS =>
#		     {'Complex' => {'attributes' => [qw(hasComponent)]},
#		      'EntitySet' => {'attributes' => [qw(hasMember)]},
#		      'EntityWithAccessionedSequence' => {'attributes' => ['referenceEntity']}},
#		     -OUT_CLASSES => ['ReferencePeptideSequence']
#		     );
#		foreach my $rps (@{$rpss}) {
#		    my ($t1,$t2) = sort {$a->db_id <=> $b->db_id} ($rs,$rps);
#		    unless ($interactions->{$t1->db_id}->{$t2->db_id}) {
#			$interactions->{$t1->db_id}->{$t2->db_id}->{'interactors'} = [$t1,$t2];
#		    }
#		    $interactions->{$t1->db_id}->{$t2->db_id}->{'complex_context'}->{$pe->db_id} = $pe;
#		}
#	    }
#	}
#    }
##    print "\n";
#    return $interactions;
#}

# Imre's original version
#sub print_interaction_report {
#    my ($self, $split_interaction_array) = @_;
#    foreach my $ar (@{$split_interaction_array}) {
#	my $interactor1 = $ar->[0];
#	my $interactor2 = $ar->[1];
#	my $tstr = _interactor_info($interactor1) . "\t" . _interactor_info($interactor2) . "\t";
#	if ($ar->[2]) {
#	    foreach my $c (@{$ar->[2]}) {
#		print $tstr, _interaction_context_info($c, 'direct_complex'), "\n";
#	    }
#	}
#	if ($ar->[3]) {
#	    foreach my $c (@{$ar->[3]}) {
#		print $tstr, _interaction_context_info($c, 'indirect_complex'), "\n";
#	    }
#	}
#	if ($ar->[4]) {
#	    foreach my $c (@{$ar->[4]}) {
#		print $tstr, _interaction_context_info($c, 'reaction'), "\n";
#	    }
#	}
#	if ($ar->[5]) {
#	    foreach my $ar2 (@{$ar->[5]}) {
#		print $tstr, "neighbouring_reaction\t", $ar2->[0]->stableIdentifier_or_class_and_id, "<->", $ar2->[1]->stableIdentifier_or_class_and_id, "\t\n";
#	    }
#	}
#    }
#}

# Prints a single line of text.
sub print_array {
    my ($self, $array) = @_;
    
    $self->print_line(join("\t", @{$array}));
}

# Prints a single line of text.
sub print_line {
    my ($self, $line) = @_;
    
    my $logger = get_logger(__PACKAGE__);
    
    if (defined $self->line_cache && defined $self->line_cache->{$line}) {
    	# If the line is a duplicate of the one just printed, suppress printing.
    	return;
    }
    
    # Do some sanity checking on the lines being produced
    my $previous_line = $self->previous_line;
    if (!($line =~ /^#/) && defined $previous_line && !($previous_line =~ /^#/)) {
    	my $tabs = $line;
    	my $previous_tabs = $previous_line;
    	$tabs =~ s/[^\t]//g;
    	$previous_tabs =~ s/[^\t]//g;
     	my $field_count = length($tabs);
    	my $previous_field_count = length($previous_tabs);
    	
    	if ($field_count != $previous_field_count) {
    		$logger->warn("InteractionGenerator.print_line: WARNING - the following lines have respectively $field_count and $previous_field_count fields, is this a bug?");
    		$logger->warn("$line");
     		$logger->warn("$previous_line");
    	}
    }
    
    print "$line\n";
    
    if (defined $self->line_cache) {
    	$self->line_cache->{$line} = 1;
    }
    $self->previous_line($line);
}

sub _interactor_info {
    return
	_db_name_colon_identifier($_[0]) . "\t" .
	join('|', map {_db_name_colon_identifier($_)} grep {uc($_->ReferenceDatabase->[0]->displayName) =~ /ENSEMBL/} @{$_[0]->ReferenceGene}) . "\t" .
	join('|', map {_db_name_colon_identifier($_)} grep {uc($_->ReferenceDatabase->[0]->displayName) eq 'ENTREZ GENE' or uc($_->ReferenceDatabase->[0]->displayName) eq 'LOCUSLINK'} @{$_[0]->ReferenceGene});
}

sub _db_name_colon_identifier {
    return $_[0]->ReferenceDatabase->[0]->displayName . ':' . $_[0]->Identifier->[0];
}

# Returns "context" information about an interaction.  This comprises of
# a reference to an array containing any or all of the following  pieces
# of information:
#
# * DB_IDs of the reactions or complexes from which interaction derived
# * Stable IDs of the reactions or complexes from which interaction derived
# * Mixed DB_IDs or stable IDs of the reactions or complexes from which interaction derived
# * Comma-separated list of literature refs
sub _interaction_context_info {
    my ($self, $context, $context_num) = @_;
    
    my $logger = get_logger(__PACKAGE__);
    
    my @context_info = ();
    if ($self->column_groups->{$COLUMN_GROUP_SOURCE_IDS}) {
    	push(@context_info, $self->source_id_info_from_context($context, $context_num));
    }
    if ($self->column_groups->{$COLUMN_GROUP_SOURCE_STABLE_IDS}) {
    	my $stable_id_flag = 1;
						
	$logger->info("InteractionGenerator._interaction_context_info: stable_id_flag=$stable_id_flag");
						
    	push(@context_info, $self->source_id_info_from_context($context, $context_num, $stable_id_flag));
    }
    if ($self->column_groups->{$COLUMN_GROUP_DECORATED_SOURCE_IDS}) {
    	push(@context_info, $self->decorated_source_id_info_from_context($context));
    }
	if ($self->column_groups->{$COLUMN_GROUP_LIT_REFS}) {
		push(@context_info, $self->literature_refs_from_context($context));
	}
	if ($self->column_groups->{$COLUMN_GROUP_PARTICIPATING_PROTEIN_COUNT}) {
		push(@context_info, $self->participating_protein_count_from_context($context, $context_num));
	}
    
    return \@context_info;
}

# Returns a string containing information on the DB_IDs or stable IDs
# of the reactions or complexes used to generate the interactions.
# The string contains appropriate tabs for delimiting fields.
sub source_id_info_from_context {
    my ($self, $context, $context_num, $stable_id_flag) = @_;
    
    my $logger = get_logger(__PACKAGE__);
    
    my $source_id_info = "";
    if (!(defined $stable_id_flag)) {
    	$stable_id_flag = 0;
    }
    my $stableIdentifier_or_class_and_id = $context->stableIdentifier_or_class_and_id . $stable_id_flag;
    if (defined $self->previous_source_id_info->{$stableIdentifier_or_class_and_id}->{$context_num}) {
    	# Use cached value if available - speeds things up by a factor of about 4
    	$source_id_info = $self->previous_source_id_info->{$stableIdentifier_or_class_and_id}->{$context_num};
    } else {
    	if ($self->column_groups->{$COLUMN_GROUP_SOURCE_STABLE_IDS}) {
	    $logger->info("InteractionGenerator.source_id_info_from_context: context_num=$context_num, stable_id_flag=$stable_id_flag");
    	}
    			
	    # Generates three columns containing IDs for pathways, reactions and complexes.
	    # This involves a lot of database access, and is consequently slow.
	    my $dba = $self->dba;
	    my $source_pathway_ids = "";
	    my $source_reaction_ids = "";
	    my $source_complex_ids = "";
	    my $reaction_db_ids = [];
	    if ($context_num == 4) {
	    	# Deal with reactions
			$source_reaction_ids = $self->stringify_db_ids([$context->db_id()], $stable_id_flag);
			$reaction_db_ids = [$context->db_id()];
						
			$logger->info("InteractionGenerator.source_id_info_from_context: context 4, source_reaction_ids=$source_reaction_ids");
	    } else {
	    	# Deal with complexes
			$source_complex_ids = $self->stringify_db_ids([$context->db_id()], $stable_id_flag);
			$reaction_db_ids = $self->reaction_db_ids_from_complexes([$context]);
			$source_reaction_ids = $self->stringify_db_ids($reaction_db_ids, $stable_id_flag);
		}
		my $pathway_db_ids = $self->pathway_db_ids_from_reaction_db_ids($reaction_db_ids);
		$source_pathway_ids = $self->stringify_db_ids($pathway_db_ids, $stable_id_flag);
						
		$logger->info("InteractionGenerator.source_id_info_from_context: source_pathway_ids=$source_pathway_ids");
						
		$source_id_info = $source_pathway_ids  . "\t" . $source_reaction_ids  . "\t" . $source_complex_ids;
			
		# Cache most recent values
		$self->previous_source_id_info->{$stableIdentifier_or_class_and_id}->{$context_num} = $source_id_info;
    }
    
    return $source_id_info;
}

# Returns a string containing information on the DB_IDs or stable IDs
# of the reactions or complexes used to generate the interactions.
# The string contains appropriate tabs for delimiting fields.
# Generates a single column with either complex or reaction IDs
sub decorated_source_id_info_from_context {
    my ($self, $context) = @_;
    
    my $decorated_source_id_info = "";
    my $stableIdentifier_or_class_and_id = $context->stableIdentifier_or_class_and_id;
    if ($self->column_groups->{$COLUMN_GROUP_DECORATED_SOURCE_IDS}) {
		$decorated_source_id_info = $stableIdentifier_or_class_and_id;
	}
    
    return $decorated_source_id_info;
}

# Returns a string containing information on the DB_IDs or stable IDs
# of the neighbouring reactions used to generate the interactions.
# The string contains appropriate tabs for delimiting fields.
sub neighbouring_reaction_source_id_info_from_reaction_pair {
    my ($self, $reaction_pair, $stable_id_flag) = @_;
    
	my @source_id_info = ();
	my $reaction_db_ids = [$reaction_pair->[0]->db_id(), $reaction_pair->[1]->db_id()];
	my $source_reaction_ids = $self->stringify_db_ids($reaction_db_ids, $stable_id_flag);

	if (defined $self->previous_source_id_info->{$source_reaction_ids}->{5}) {
		# Use cached value if available - speeds things up by a factor of about 4
    	@source_id_info = @{$self->previous_source_id_info->{$source_reaction_ids}->{5}};
	} else {
		my $pathway_db_ids = $self->pathway_db_ids_from_reaction_db_ids($reaction_db_ids);
		my $source_pathway_ids = $self->stringify_db_ids($pathway_db_ids, $stable_id_flag);
		# Extra tab for (nonexistent) complexes
		push(@source_id_info,  $source_pathway_ids  . "\t" . $source_reaction_ids  . "\t");

		# Cache most recent values
		$self->previous_source_id_info->{$source_reaction_ids}->{5} = \@source_id_info;
	}

    return \@source_id_info;
}

sub participating_protein_count_from_context {
    my ($self, $context, $context_num) = @_;
    
    my $participating_protein_count = 0;
	if ($context_num == 4) {
	    # Deal with reactions
		$participating_protein_count = $self->participating_protein_count_from_reactions([$context]);
	} else {
	    # Deal with complexes
		$participating_protein_count = $self->participating_protein_count_from_complexes([$context]);
	}
    
    return $participating_protein_count;
}

# Given a reference to an array of complex DB_IDs as an argument,
# return a count of the number of proteins involved in the complexes.
sub participating_protein_count_from_complexes {
    my ($self, $complexes) = @_;
    
    my $participating_protein_count = 0;
    my $complex;
    my $ar;
    foreach $complex (@{$complexes}) {
	    $ar = $complex->follow_class_attributes
		(-INSTRUCTIONS =>
		 {'Complex' => {'attributes' => [qw(hasComponent)]},
		  'EntitySet' => {'attributes' => [qw(hasMember)]},
		  'Polymer' => {'attributes' => [qw(repeatedUnit)]},
	          },
		 -OUT_CLASSES => ['EntityWithAccessionedSequence']
		 );
		 $participating_protein_count += scalar(@{$ar});
    }
    
    return $participating_protein_count;
}

# Given a reference to an array of reaction DB_IDs as an argument,
# return a count of the number of proteins involved in the reactions.
sub participating_protein_count_from_reactions {
    my ($self, $reactions) = @_;
    
    my $participating_protein_count = 0;
    my $reaction;
    my $ar;
    foreach $reaction (@{$reactions}) {
	    $ar = $reaction->follow_class_attributes
		(-INSTRUCTIONS =>
		 {'ReactionlikeEvent' => {'attributes' => [qw(input output catalystActivity)]},
		  'CatalystActivity' => {'attributes' => [qw(physicalEntity)]},
		  'Complex' => {'attributes' => [qw(hasComponent)]},
		  'EntitySet' => {'attributes' => [qw(hasMember)]},
		  'Polymer' => {'attributes' => [qw(repeatedUnit)]},
	          },
		 -OUT_CLASSES => ['EntityWithAccessionedSequence']
		 );
		 $participating_protein_count += scalar(@{$ar});
    }
    
    return $participating_protein_count;
}

# Take a string containing a comma-separated list of complex IDs and
# return an array of reaction DB_IDs.
sub reaction_db_ids_from_complexes {
    my ($self, $complexes) = @_;
    
    my $logger = get_logger(__PACKAGE__);
    
    my $source_reaction_ids = "";
    my $dba = $self->dba;
    
	if (!(defined $dba)) {
		return $source_reaction_ids;
	}
	
	# Find the reactions containing these complexes
	my $reaction_instances;
	my $reaction_instance;
	my $complex;
	my $x;
	my $y;
	my %reaction_id_hash = ();
	foreach $complex (@{$complexes}) {
		($reaction_instances, $x, $y) = $dba->follow_reverse_attributes(-INSTANCE => $complex, -ATTRIBUTES => ['hasComponent', 'hasMember', 'input', 'output'], -OUT_CLASSES => ['ReactionlikeEvent']);
					
		$logger->info("InteractionGenerator.reaction_db_ids_from_complexes : scalar(reaction_instances)=" . scalar(@{$reaction_instances}));
					
		if (defined $reaction_instances && scalar(@{$reaction_instances})>0) {
			foreach $reaction_instance (@{$reaction_instances}) {
				# Use a hash to avoid duplications
				$reaction_id_hash{$reaction_instance->db_id()} = $reaction_instance->db_id();
			}
		}
	}
	
	my @reaction_db_ids = sort(keys(%reaction_id_hash));
	
	return \@reaction_db_ids;
}

# Take a reference to an array of reaction DB_IDs and
# return an array of pathway DB_IDs for pathways
# that contain the reactions.
sub pathway_db_ids_from_reaction_db_ids {
    my ($self, $reaction_db_ids, $mitab) = @_;
    
    my $logger = get_logger(__PACKAGE__);
    
    my $source_pathway_ids = "";
    my $dba = $self->dba;
    if (!(defined $dba)) {
	return $source_pathway_ids;
    }
    
    # Find the pathways referring to the reactions
    my $pathway_instances;
    my $reaction_db_id;
    my %pathway_id_hash = ();
    my @pathway_db_ids;

    if (defined $mitab) {
	my %seen;
	foreach $reaction_db_id (@{$reaction_db_ids}) {

            $logger->info("InteractionGenerator.pathway_db_ids_from_reaction_db_ids: reaction_db_id=$reaction_db_id");

            $pathway_instances = $dba->fetch_instance(-CLASS => 'Pathway', -QUERY => [['hasEvent',[$reaction_db_id],0]]);

            $logger->info("InteractionGenerator.pathway_db_ids_from_reaction_db_ids: scalar(pathway_instances)=" . scalar(@{$pathway_instances}));

            if (defined $pathway_instances && scalar(@{$pathway_instances})>0) {
#here the mitab method is different from the one below in that the order in which reaction_db_ids come in is retained for the corresponding pathway ids (unless they are duplicates...)
		foreach my $pathway (@{$pathway_instances}) {
		    next if $seen{$pathway->db_id}++;
		    push @pathway_db_ids, 'reactome:'.$self->stableIdentifier_or_id($pathway);
		}
	    }
	}
    } else {
	foreach $reaction_db_id (@{$reaction_db_ids}) {
	    $logger->info("InteractionGenerator.pathway_db_ids_from_reaction_db_ids: reaction_db_id=$reaction_db_id");
	    
	    $pathway_instances = $dba->fetch_instance(-CLASS => 'Pathway', -QUERY => [['hasEvent',[$reaction_db_id],0]]);
	    
	    $logger->info("InteractionGenerator.pathway_db_ids_from_reaction_db_ids: scalar(pathway_instances)=" . scalar(@{$pathway_instances}));
	    
	    if (defined $pathway_instances && scalar(@{$pathway_instances})>0) {
		# Use a hash to avoid duplications
		    $pathway_id_hash{$pathway_instances->[0]->db_id()} = $pathway_instances->[0]->db_id();
	    }
	}
	@pathway_db_ids = sort(keys(%pathway_id_hash));
    }	
    return \@pathway_db_ids;
}

## Take a string containing a comma-separated list of reaction IDs and
## return a string containing comma-separated pathway IDs for pathways
## that contain the reactions.
#sub pathway_ids_from_reaction_ids {
#    my ($self, $source_reaction_ids) = @_;
#    
#    my $source_pathway_ids = "";
#    my $dba = $self->dba;
#    
#	if (!(defined $dba)) {
#		return $source_pathway_ids;
#	}
#	
#	# Find the pathways referring to the reactions
#	my @reaction_ids = split(/,/, $source_reaction_ids);
#	my $pathway_instances;
#	my $reaction_instance;
#	my %pathway_id_hash = ();
#	foreach my $reaction_id (@reaction_ids) {
#					
#		print STDERR "InteractionGenerator.pathway_ids_from_reaction_ids: reaction_id=$reaction_id\n";
#					
#		$reaction_instance = $self->instance_from_id($reaction_id);
#		if (!(defined $reaction_instance)) {
#			print STDERR "InteractionGenerator.pathway_ids_from_reaction_ids: WARNING - reaction with ID $reaction_id does not seem to exist!\n";
#			next;
#		}
#		$pathway_instances = $dba->fetch_instance(-CLASS => 'Pathway', -QUERY => [['hasEvent',[$reaction_instance->db_id()],0]]);
#					
#		print STDERR "InteractionGenerator.pathway_ids_from_reaction_ids: scalar(pathway_instances)=" . scalar(@{$pathway_instances}) . "\n";
#					
#		if (defined $pathway_instances && scalar(@{$pathway_instances})>0) {
#			# Use a hash to avoid duplications
#			$pathway_id_hash{$pathway_instances->[0]->db_id()} = $pathway_instances->[0]->db_id();
#		}
#	}
#    
#	my $pathway_id;
#	foreach $pathway_id (sort(keys(%pathway_id_hash))) {
#		if (!($source_pathway_ids eq "")) {
#			$source_pathway_ids .= ",";
#		}
#		$source_pathway_ids .= $pathway_id;
#	}
#	
#    return $source_pathway_ids;
#}
#
## Given a DB_ID or a stable ID, finds the corresponding instance.
#sub instance_from_id {
#    my ($self, $id) = @_;
#    
#    my $instance = undef;
#    if (!(defined $id)) {
#    	print STDERR "InteractionGenerator.instance_from_id: WARNING - id is undef!\n";
#    	return $instance;
#    }
#    my $dba = $self->dba;
#	if (!(defined $dba)) {
#    	print STDERR "InteractionGenerator.instance_from_id: WARNING - dba is undef!\n";
#		return $instance;
#	}
#	my $instances;
#    if ($id =~ /^REACT_/) {
#    	if ($id =~ /REACT_6310/) {
#    		print STDERR "InteractionGenerator.instance_from_id: id=|$id|\n";
#    	}
#    	
#    	my $stable_id = $id;
#    	$stable_id =~ s/\.[0-9]+\s*$//;
#    	
#    	if ($id =~ /REACT_6310/) {
#    		print STDERR "InteractionGenerator.instance_from_id: stable_id=|$stable_id|\n";
#    	}
#    	
#    	$instances = $dba->fetch_instance(-CLASS => 'StableIdentifier', -QUERY => [['identifier',[$stable_id],0]]);
#    	
#    	if ($id =~ /REACT_6310/) {
#    		print STDERR "InteractionGenerator.instance_from_id: scalar(instances=" . scalar(@{$instances}) . "\n";
#    	}
#    	
#    	if (defined $instances && scalar(@{$instances})>0) {
#	    	if ($id =~ /REACT_6310/) {
#	    		print STDERR "InteractionGenerator.instance_from_id: found a StableIdentifier instance\n";
#	    	}
#	    	
#    		$instance = $instances->[0];
#    		$instances = $dba->fetch_instance(-CLASS => 'DatabaseObject', -QUERY => [['stableIdentifier',[$instance->db_id()],0]]);
#	    	if (defined $instances && scalar(@{$instances})>0) {
#		    	if ($id =~ /REACT_6310/) {
#		    		print STDERR "InteractionGenerator.instance_from_id: found corresponding DatabaseObject instance\n";
#		    	}
#	    	
#	    		$instance = $instances->[0];
#	    	} else {
#	    		$instance = undef;
#	    	}
#    	}
#    } else {
#    	$instances = $dba->fetch_instance_by_db_id($id);
#    	if (defined $instances && scalar(@{$instances})>0) {
#    		$instance = $instances->[0];
#    	}
#    }
#    
#    return $instance;
#}

sub literature_refs_from_context {
    my ($self, $context) = @_;
    
    my $refs;
    if ($context->is_a('Event')) {
		$refs = $context->LiteratureReference;
    } elsif ($context->is_a('Complex')) {
		$refs = _literature_refs_for_complex($context);
    }
    if ($refs) {
    	my $ref_string = "";
    	my $ref;
    	foreach $ref (@{$refs}) {
    		if (defined $ref->PubMedIdentifier && scalar(@{$ref->PubMedIdentifier})>0) {
    			if (!($ref_string eq "")) {
    				$ref_string .= ",";
    			}
    			$ref_string .= $ref->PubMedIdentifier->[0];
    		}
    	}
    	return $ref_string;
#		return join(',',map {$_->PubMedIdentifier->[0]} @{$refs});
    }
    return '';
}

sub _literature_refs_for_complex {
    my $complex = shift;
    my %refs;
    my @later;
    foreach my $e (@{$complex->reverse_attribute_value('output')}) {
	# Skip evident dissociation reactions in the 1st round.
	if (@{$e->Input} < @{$e->Output}) {
	    push @later, $e;
	    next;
	}
	map {$refs{$_->db_id} = $_} @{$e->LiteratureReference};
    }
#    unless (%refs) {
#	foreach my $e (@later) {
#	    map {$refs{$_->db_id} = $_} @{$e->LiteratureReference};
#	}
#    }
    return [values %refs];
}

# For the given interaction, looks at the interacting pair of proteins,
# finds any IntAct IDs that match, and includes them into the interaction.
sub _insert_intact_ids {
	my ($self, $interaction) = @_;
	
	my $logger= get_logger(__PACKAGE__);
	
	my $interactors = $interaction->{'interactors'};
	my $id1 = $interactors->[0]->identifier->[0];
	my $id2 = $interactors->[1]->identifier->[0];
	my $intact_ids = $self->intact->find_intact_ids_for_uniprot_pair($id1, $id2);
	
	if (defined $intact_ids && scalar(@{$intact_ids})>0) {
	    $logger->info("InteractionGenerator._insert_intact_ids: intact_ids=" . $self->stringify_intact_ids($intact_ids));
	}
	
	$interaction->{"IntActIds"} = $intact_ids;
}

# Given an array of IntAct IDs, creates a string containing the IDs, separated by commas.
sub stringify_intact_ids {
    my ($self, $intact_ids) = @_;

	my $intact_id_string = $self->stringify_ids($intact_ids);
	$intact_id_string =~ s/intact://ig;
	
	return $intact_id_string;
}

# Given an array of DB_IDs, creates a string of IDs separated by commas.
# If no second argument is specified, or it is set to 0, then the returned
# string will contain the supplied DB_IDs.  If the second argument is
# set to 1, DB_IDs will be replaced by corresponding stable IDs, where
# available.
sub stringify_db_ids {
    my ($self, $db_ids, $stable_id_flag) = @_;

	my @new_ids = ();
	if (defined $stable_id_flag && $stable_id_flag == 1) {
		my $db_id;
		my $instance;
		my $instances;
		my $dba = $self->dba;
		foreach $db_id (@{$db_ids}) {
	    	$instances = $dba->fetch_instance_by_db_id($db_id);
	    	if (defined $instances && scalar(@{$instances})>0) {
	    		$instance = $instances->[0];
	    		if (defined $instance->stableIdentifier && scalar(@{$instance->stableIdentifier})>0) {
					push(@new_ids, $instance->stableIdentifier->[0]->identifier->[0]);
	    		}
	    	}
		}
	} else {
		@new_ids = @{$db_ids};
	}
	
	my $id_string = $self->stringify_ids(\@new_ids);

	return $id_string;
}

# Given an array of IDs, creates a string with the IDs separated by commas (or by '|' for mitab format)   
sub stringify_ids {
    my ($self, $ids, $mitab) = @_;

    my $id;
    my $id_string = "";
    foreach $id (@{$ids}) {
	if (!($id_string eq "")) {
	    if ($mitab) {
		$id_string .= "|";
	    } else {
		$id_string .= ",";
	    }
	}
	$id_string .= $id;
    }

    return $id_string;
}


1;
