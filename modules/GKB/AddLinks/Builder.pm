=head1 NAME

GKB::AddLinks::Builder
=head1 SYNOPSIS

=head1 DESCRIPTION

This is an abstract class for inserting links into a Reactome database.
It provides an abstract method, buildPart, which should be implemented
in subclasses, and which does the actual hard work of inserting links.
It also provides a number of utility methods, which may be used by
subclasses.  Additionally, it provides setter methods, which are used
by the Director class to pass on various parameters.

It forms part of the Builder design pattern.

=head1 SEE ALSO

GKB::AddLinks::Director

Subclasses:
GKB::AddLinks::RefseqReferenceDatabaseToReferencePeptideSequence
GKB::AddLinks::UCSCReferenceDatabaseToReferencePeptideSequence
GKB::AddLinks::OmimReferenceDNASequenceToReferencePeptideSequence
GKB::AddLinks::EnsemblGeneToUniprotReferencePeptideSequence
GKB::AddLinks::EntrezGeneToUniprotReferenceDNASequence
GKB::AddLinks::IntActDatabaseIdentifierToComplexOrReactionlikeEvent
GKB::AddLinks::RefseqReferenceRNASequenceToReferencePeptideSequence
...etc.

=head1 AUTHOR

David Croft E<lt>croft@ebi.ac.ukE<gt>

Copyright (c) 2008 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

package GKB::AddLinks::Builder;

use Data::Dumper;
use GKB::MatchingInstanceHandler::Simpler;
use GKB::InstanceCreator::ReferenceDatabase;
use GKB::InstanceCreator::Miscellaneous;
use GKB::Utils;
use GKB::Config;
use GKB::Utils::Timer;
use strict;
use vars qw(@ISA $AUTOLOAD %ok_field);
use Bio::Root::Root;

@ISA = qw(Bio::Root::Root);

# List the object variables here, so that they can be checked
# 
for my $attr
    (qw(
    builder_params
    instance_edit
    reference_peptide_sequences
    insertion_stats_hash
    timer
    timer_message
    dba
    termination_status
    class_name
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
    my($pkg) = @_;
    
    my $self = bless {}, $pkg;
   	
   	$self->clear_variables();
	my $timer = GKB::Utils::Timer->new();
   	$self->timer($timer);
   	$self->timer_message("Builder execution time (seconds): ");
   	$self->termination_status(undef);

    return $self;
}

sub clear_variables {
    my ($self) = @_;
    
   	$self->builder_params(undef);
   	$self->instance_edit(undef);
   	$self->reference_peptide_sequences(undef);
   	my $insertion_stats_hash = {};
   	$self->insertion_stats_hash($insertion_stats_hash);
   	$self->timer(undef);
   	$self->timer_message(undef);
   	if (defined $self->dba) {
   		$self->dba->DESTROY();
   	}
   	$self->dba(undef);
}

# Needed by subclasses to gain access to class variables defined in
# this class.
sub get_ok_field {
	return %ok_field;
}

# Sets the BuilderParams object that will be used to pass parameters
# from a Director object to individual Builder objects.
# Also creates a local InstanceEdit, to be used in any instances created
sub set_builder_params {
	my ($self, $builder_params) = @_;
	
	$self->builder_params($builder_params);
	
		my $miscellaneous = $self->builder_params->miscellaneous;
	if (defined $self->builder_params->instance_edit) {
		my $instance_edit = $self->builder_params->instance_edit->clone();
		$self->instance_edit($instance_edit);
		if (defined $miscellaneous) {
			# Ensure that anything that doesn't have direct access
			# to the Builder object also uses this InstanceEdit
	   		$miscellaneous->set_instance_edit_for_effective_user($instance_edit);
		}
	} else {
		if (defined $miscellaneous) {
	   		$self->instance_edit($miscellaneous->get_instance_edit_for_effective_user());
		}
	}
}

# Sets InstanceEdit note
sub set_instance_edit_note {
	my ($self, $note) = @_;
	
	my $dba = $self->builder_params->get_dba();
	if (defined $dba && defined $self->instance_edit) {
   		$self->instance_edit->note($note);
   		my $db_id = $self->instance_edit->db_id();
   		if (defined $db_id && $db_id>=0) {
   			$dba->update($self->instance_edit, 'note');
   		}
	}
}

# Abstract run method, which needs to be implemented by a subclass.
# This method inserts links into the Reactome database.
sub buildPart {
	my ($self) = @_;
	
	die "Builder.buildPart: this method must be implemented in a subclass\n";
}

# Given a reference to an array of ReferencePeptideSequence instances, put
# them into a hash keyed by species and identifier, and return this hash.
sub create_reference_peptide_sequence_hash {
	my ($self, $reference_peptide_sequences) = @_;
	
	my $identifier;
	my $species;
	my $accs = {};
	foreach my $i (@{$reference_peptide_sequences}) {
		$identifier = $i->Identifier->[0];
		$species = $i->species->[0]->name->[0];
		push(@{$accs->{$species}->{uc($identifier)}}, $i);
	}
	
	return $accs;
}

# Given a reference to an array of ReferencePeptideSequence instances, put
# them into a hash keyed by species and identifier, and return this hash.
# If a variant ID could be found for a peptide sequence, use that as the
# key.
sub create_reference_peptide_sequence_hash_variant_ids {
	my ($self, $reference_peptide_sequences) = @_;
	
	my $identifier;
	my $variant_identifier;
	my $species;
	my $accs = {};
	foreach my $i (@{$reference_peptide_sequences}) {
		$species = $i->species->[0]->name->[0];
		if ($i->is_valid_attribute('VariantIdentifier')) {
			# ReferenceIsoform (new data model) and ReferencePeptideSequence
			# (old data model) both know about variant identifiers, but in
			# the case of ReferencePeptideSequence, there may be no variant
			# identifier present, because the instance is the base form of
			# the protein.  So we need to check that the variant identifier
			# exists before progressing further.
		    if (defined $i->VariantIdentifier && scalar(@{$i->VariantIdentifier})>0) {
				$variant_identifier = $i->VariantIdentifier->[0];
			    if (defined $variant_identifier && !($variant_identifier eq '')) {
			    	# This code is complicated in order to allow for backwards
			    	# compatibility; if instance $i is a ReferencePeptideSequence
			    	# (old data model), then assume that the -1 isoform is the
			    	# same as the base sequence.  If the instance is ReferenceIsoform,
			    	# then we can safely use the -1 isoform, because we know that
			    	# an instance of the base protein already exists.
			    	if ($i->isa('ReferenceIsoform') || ($variant_identifier =~ /-/ && !($variant_identifier =~ /-1$/))) {
						push(@{$accs->{$species}->{uc($variant_identifier)}}, $i);
						next;
			    	}
			    }
		    }
		}
		
		# If no variant ID could be found, use the plain vanilla identifier.
		push(@{$accs->{$species}->{uc($i->Identifier->[0])}}, $i);
	}
	
	return $accs;
}

sub check_for_identical_instances {
    my ($self, $instance) = @_;
    
    if (!(defined $instance)) {
    	print STDERR "Builder.check_for_identical_instances: WARNING - instance is undef!!\n";
    	return undef;
    }

	my $dba = $self->builder_params->get_dba();
    $dba->fetch_identical_instances($instance);
    my $identical_instances_in_db = $instance->identical_instances_in_db();
    my $count_ii = 0;
    if (defined $identical_instances_in_db) {
    	$count_ii = @{$instance->identical_instances_in_db};
    } else {
    	print STDERR "Builder.check_for_identical_instances: WARNING - identical_instances_in_db is undef, assuming there are no identical instances\n";
    }
    
    if ($count_ii == 0) {
#    	print STDERR "Builder.check_for_identical_instances: adding InstanceEdit\n";
		$instance->created($self->instance_edit());
		my $ID = $dba->store($instance);
		return $instance;
    } elsif ($count_ii == 1) {
    	print STDERR "Builder.check_for_identical_instances: an identical instance has been found in the database\n";
		$instance->db_id($instance->identical_instances_in_db->[0]->db_id);
		return $instance->identical_instances_in_db->[0];
    } else {
    	print STDERR "Builder.check_for_identical_instances: multiple identical instances have been found in the database\n";
		$dba->store_if_necessary($instance);
		return $instance;
    }
}

# Depending on the data model being used, the Reactome instance class
# used for holding proteins could either be 'ReferencePeptideSequence'
# (old data model, pre March 2009) or 'ReferenceGeneProduct' (new data
# model).  This subroutine returns the approprite class name.
sub get_reference_protein_class {
    my ($self) = @_;
    
	my $dba = $self->builder_params->get_dba();
    my $reference_peptide_sequence_class = 'ReferenceGeneProduct';
    if (!($dba->ontology->is_valid_class($reference_peptide_sequence_class))) {
    	$reference_peptide_sequence_class = 'ReferencePeptideSequence';
    }
    
    return $reference_peptide_sequence_class;
}   

# For the named attribute in the given instance, removes all instances
# referring to the given reference database.  This only works for attributes that
# take instances.
sub remove_typed_instances_from_attribute {
    my ($self, $instance, $attribute, $reference_database) = @_;
    my @attribute_instances;
    my $attribute_instance;
    my $we_need_to_do_some_cleanup;
    foreach $attribute_instance (@{$instance->$attribute}) {
	my $ref_db = $attribute_instance->referenceDatabase || next;
	my $first  = $ref_db->[0]  || next;
	my $db_id  = $first->db_id || next;
	#print STDERR "DEBUG: Checking on XREF $db_id\n";
	if ($db_id != $reference_database->db_id) {
	    #print STDERR "DEBUG: We have a XREF we want to preserve here! $db_id\n";
	    push(@attribute_instances, $attribute_instance);
	}
	elsif ($db_id == $reference_database->db_id) {
	    $we_need_to_do_some_cleanup++;
	}
    }
    if (@attribute_instances || $we_need_to_do_some_cleanup) {
	#print STDERR "DEBUG: We are doing some cleanup\n";
	$instance->$attribute(undef);
	$instance->$attribute(@attribute_instances);
    }
}   

# For the named attribute in the given instance, checks to see if there instances
# referring to the given reference database.  This only works for attributes that
# take instances.
sub exists_typed_instances_from_attribute {
    my ($self, $instance, $attribute, $reference_database) = @_;
    
#    print STDERR "Builder.exists_typed_instances_from_attribute: DB_ID=" . $reference_database->db_id() . ", reference_database->name=";
#    foreach my $name (@{$reference_database->name}) {
#    	print STDERR "$name, ";
#    }
#    print STDERR  "\n";
    
    my $exists_typed_instance_flag = 0;
	my @attribute_instances;
	my $attribute_instance;
	foreach $attribute_instance (@{$instance->$attribute}) {
#		
#		
#		print STDERR "Builder.exists_typed_instances_from_attribute: DB_ID=" . $attribute_instance->referenceDatabase->[0]->db_id() . ", attribute_instance->referenceDatabase->[0]->_displayName=";
#	    foreach my $name (@{$attribute_instance->referenceDatabase->[0]->name}) {
#	    	print STDERR "$name, ";
#	    }
#	    print STDERR  "\n";
#	    
#	    
#	    
#	    
#	    
		if ($attribute_instance->referenceDatabase->[0]->db_id == $reference_database->db_id) {
			$exists_typed_instance_flag = 1;
			last;
		}
	}
	
	return $exists_typed_instance_flag;
}   

# Given a hash of reference sequences, keyed by species and identifier,
# take each one and for the named attribute, removes all instances
# referring to the given reference database.  limiting_species is
# optional, if you specify this, then the subroutine will run faster.
sub remove_typed_instances_from_reference_peptide_sequence_hash {
	my ($self, $accs, $attribute, $reference_database, $limiting_species) = @_;
	
	my $instances;
	my $instance;
	my $species;
	foreach $species (keys(%{$accs})) {
		if (defined $limiting_species && !($species eq $limiting_species)) {
			next;
		}
		
		foreach $instances (values(%{$accs->{$species}})) {
			foreach $instance (@{$instances}) {
				$self->remove_typed_instances_from_attribute($instance, $attribute, $reference_database);
			}
		}
	}		
}

# General purpose fetching script to get the ReferencePeptideSequences
# into which the subclass will insert its cross-references.  If you
# set the restrict_to_uniprot_flag to 1, then only ReferencePeptideSequences
# with UniProt entries associated with them will be retrieved.
sub fetch_reference_peptide_sequences {
	my ($self, $restrict_to_uniprot_flag) = @_;
	
	my $dba = $self->builder_params->get_dba();
	my $query = [];
	
	# Retrieve all UniProt reference peptide sequences from GK central
	if ($restrict_to_uniprot_flag) {
		my $uniprot_reference_database = $self->builder_params->reference_database->get_uniprot_reference_database();
		push(@{$query}, ['referenceDatabase.DB_ID', '=', [$uniprot_reference_database->db_id()]]);
	}
	
	my $limiting_species = $self->builder_params->get_species_name();
	if (defined $limiting_species) {
		my $species = $dba->fetch_instance_by_attribute('Species',[['name',[$limiting_species]]])->[0];
		if (defined $species) {
			push(@{$query}, ['species', '=', [$species->db_id]]);
		} else {
		    print STDERR "Builder.fetch_reference_peptide_sequences: WARNING - no species '$limiting_species' found.\n";
		}
	}

	# Limit DB_IDs to the list in the command line, if available, otherwise
	# use all DB_IDs.  If you are going for the command line option, the
	# DB_IDs should be from ReferenceGeneProduct instances.
	my $db_ids = $self->builder_params->get_db_ids();
	if (defined $db_ids) {
		push(@{$query}, ['DB_ID', '=', $db_ids]);
	}
	
	my $reference_peptide_sequences = undef;
	if (scalar(@{$query})==0) {
		$reference_peptide_sequences = $dba->fetch_all_class_instances_as_shells($self->get_reference_protein_class());
	} else {
		$reference_peptide_sequences = $dba->fetch_instance_by_remote_attribute($self->get_reference_protein_class(), $query);
	}
	
	$self->reference_peptide_sequences($reference_peptide_sequences);
	
	return $reference_peptide_sequences;
}

# Get a directory where file can be stored temporarily, e.g. a
# place where things downloaded from the internet can be
# processed.
sub get_tmp_dir {
	my ($self) = @_;
	
	my $tmp_dir = "/tmp";
	if (defined $GK_TMP_IMG_DIR && (-e $GK_TMP_IMG_DIR) && (-d $GK_TMP_IMG_DIR)) {
		$tmp_dir = $GK_TMP_IMG_DIR;
	}
	
	return $tmp_dir;
}

sub increment_insertion_stats_hash {
	my ($self, $db_id) = @_;
	
	$self->insertion_stats_hash->{$db_id}++;
}

sub print_insertion_stats_hash {
	my ($self) = @_;
	
	if (!(defined $self->reference_peptide_sequences)) {
		return;
	}
	
	my $none = 0;
	my $one = 0;
	my $two = 0;
	my $more = 0;
	my $db_id;
	my $insertion_stats;
	my $reference_peptide_sequence;
	foreach $reference_peptide_sequence (@{$self->reference_peptide_sequences}) {
	    $db_id = $reference_peptide_sequence->db_id();
		$insertion_stats = $self->insertion_stats_hash->{$db_id};
		if (!(defined $insertion_stats) || $insertion_stats==0) {
			$none++;
		} elsif ($insertion_stats==1) {
			$one++;
		} elsif ($insertion_stats==2) {
			$two++;
		} elsif ($insertion_stats>2) {
			$more++;
		}
	}
	print STDERR scalar(@{$self->reference_peptide_sequences}), " uniprot entries.\n";
	print STDERR $none, " have no gene ID associated.\n";
	print STDERR $one, " have 1 gene ID associated.\n";
	print STDERR $two, " have 2 gene IDs associated.\n";
	print STDERR $more, " have more than 2 gene IDs associated.\n";
}

sub print_termination_status {
	my ($self) = @_;
	
	if (defined $self->termination_status) {
		if (defined $self->class_name) {
			print STDERR $self->class_name . ": " . $self->termination_status . "\n";
		} else {
			print STDERR $self->termination_status . "\n";
		}
	}
}

## Create a fresh, new DatabaseAdaptor for this Builder.  This
## can help to circumvent potential timeout problems for jobs
## that run for 2 days or more.
#sub get_dba {
#	my ($self) = @_;
#	
#	my $dba = $self->dba;
#	if (defined $dba) {
#		return $dba;
#	}
#	
#	if (!(defined $self->builder_params) || !(defined $self->builder_params->db_params)) {
#		print STDERR "Builder.get_dba: builder_params or builder_params->db_params not defined!!\n";
#		return undef;
#	}
#	
#	my ($db_name, $host, $port, $user, $password) = @{$self->builder_params->db_params};
#
#	print STDERR "Builder.get_dba: db_name=$db_name, host=$host, port=$port, user=$user, password=$password\n";
#
#	$dba = GKB::DBAdaptor->new(-user=>$user || '', -host=>$host, -pass=>$password, -port=>$port, -dbname => $db_name);
#
#	return $dba;
#}

1;

