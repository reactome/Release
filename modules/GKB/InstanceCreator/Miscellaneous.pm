=head1 NAME

GKB::InstanceCreator::Miscellaneous

=head1 SYNOPSIS

Miscellaneous methods that create various types of instance.

=head1 DESCRIPTION

Most of these methods follow a similar pattern: given the arguments,
look to see if a corresponding instance exists in the database and
return that where possible.  Otherwise, create a new instance and
return that.

=head1 SEE ALSO

GKB::DBAdaptor

=head1 AUTHOR

David Croft E<lt>croft@ebi.ac.ukE<gt>

Copyright (c) 2008 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

package GKB::InstanceCreator::Miscellaneous;

use GKB::DBAdaptor;
use GKB::MatchingInstanceHandler::Simpler;
use GKB::Instance;
use strict;
use vars qw(@ISA $AUTOLOAD %ok_field);
use Bio::Root::Root;

@ISA = qw(Bio::Root::Root);

# List the object variables here, so that they can be checked
for my $attr
    (qw(
    dba
    instance_edit_for_effective_user
    reference_dna_sequence_cache
    reference_rna_sequence_cache
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
    my($pkg, $dba) = @_;

    my $self = bless {}, $pkg;
   	$dba && $self->dba($dba);
   	
   	my %reference_dna_sequence_cache = ();
   	my %reference_rna_sequence_cache = ();
   	$self->instance_edit_for_effective_user(undef);
   	$self->reference_dna_sequence_cache(\%reference_dna_sequence_cache);
   	$self->reference_rna_sequence_cache(\%reference_rna_sequence_cache);

    return $self;
}

# Set the DBAdaptor
sub set_dba {
	my ($self, $dba) = @_;
	
	$self->dba($dba);
}

# Sets an InstanceEdit instance for the user currently running
# Perl.
# Arguments:
#
# instance_edit - InstanceEdit instance
#
sub set_instance_edit_for_effective_user {
	my ($self, $instance_edit) = @_;
	
	if (!(defined $instance_edit)) {
		print STDERR "Miscellaneous.set_instance_edit_for_effective_user: instance_edit is undef!\n";
		return;
	}
	
	$self->instance_edit_for_effective_user($instance_edit);
}

# Gets an InstanceEdit instance for the user currently running
# Perl.
# Arguments:
#
# Returns a InstanceEdit instance.
sub get_instance_edit_for_effective_user {
	my ($self) = @_;
	
	my $instance_edit_for_effective_user = $self->instance_edit_for_effective_user;
	if (defined $instance_edit_for_effective_user) {
		return $instance_edit_for_effective_user;
	}

    my $dba = $self->dba;
    if (!(defined $dba)) {
		print STDERR "Miscellaneous.get_instance_edit_for_effective_user: dba is null!\n";
#		$self->throw("oh yuk");
    	return undef;
    }
    
	eval {
		# This sometimes breaks catastrophically, I don't know why,
		# hence the eval.
		$instance_edit_for_effective_user = $dba->create_InstanceEdit_for_effective_user();
	};
	
	if (!(defined $instance_edit_for_effective_user)) {
		# For some reason, no instance edit could be created for the
		# current user.  Use a croft instance edit instead.
		my $author = $self->get_person('Croft', 'D');
		$instance_edit_for_effective_user = GKB::Instance->new(
			-ONTOLOGY => $dba->ontology,
			-CLASS    => 'InstanceEdit'
		);
		$instance_edit_for_effective_user->inflated(1);
		$instance_edit_for_effective_user->author($author);
		$dba->store($instance_edit_for_effective_user);
	}
	$self->instance_edit_for_effective_user($instance_edit_for_effective_user);
	
	return $instance_edit_for_effective_user;
}

# Get a person, given initials and surname.
# Arguments:
#
# myName - surname
# myInitial - initial
#
# Returns a Person instance.
sub get_person {
	my ($self, $name, $initial) = @_;

    my $dba = $self->dba;
    if (!(defined $dba)) {
		print STDERR "Miscellaneous.get_person: dba is null!\n";
    	return undef;
    }
    
	# Get a list of all persons with the given name and initial
	my @query = ( [ 'surname', '=', [$name] ], [ 'initial', '=', [$initial] ] );
	my $persons = $dba->fetch_instance_by_remote_attribute( 'Person', \@query );
	my $person = undef;
	if (defined $persons && scalar(@{$persons})>0) {
		$person = $persons->[0];
		$person->inflate();
	}

	if (!(defined $person)) {
		$person = GKB::Instance->new(-ONTOLOGY => $dba->ontology, -CLASS => 'Person');

		$person->inflated(1);
		$person->Surname($name);
		$person->Initial($initial);
		$person->Firstname($initial);

		my @query = ( [ 'name', '=', ['EBI'] ] ); # TODO: this needs to be generalized
		my $affiliations = $dba->fetch_instance_by_remote_attribute( 'Affiliation', \@query );
		if (defined $affiliations && scalar(@{$affiliations})>0) {
			$person->Affiliation($affiliations->[0]);
		}

		$dba->store($person);
	}

	return $person;
}

# Gets reference DNA, based on the supplied ReferenceDatabase
# instance, species instance and the ID.  Will normally obtain the instance from the
# database, but caching is also used, so that frequently used database
# identifier instances can be retrieved quickly.
#
# You may leave the name argument out.
sub get_reference_dna_sequence {
    my ($self, $species, $reference_database, $identifier, $name) = @_;
    
	my $dba = $self->dba;
    my $reference_dna_sequence = $self->reference_dna_sequence_cache->{$reference_database->db_id()}->{$identifier};
    if (defined $reference_dna_sequence) {
    	# We already have a copy in the cache
    	return $reference_dna_sequence;
    }

	# Look to see if we can find the ReferenceDNASequence entry in
	# the database
	my @query = (
		['identifier',  '=', [$identifier]],
		['referenceDatabase.DB_ID', '=', [$reference_database->db_id()]]
	);
	my $reference_dna_sequences = $dba->fetch_instance_by_remote_attribute('ReferenceDNASequence', \@query);
	if (defined $reference_dna_sequences && scalar(@{$reference_dna_sequences})>0) {
		# Yep, it's in there!
		$reference_dna_sequence = $reference_dna_sequences->[0];
		$reference_dna_sequence->inflate();
		$self->reference_dna_sequence_cache->{$reference_database->db_id()}->{$identifier} = $reference_dna_sequence;
		return $reference_dna_sequence;
	}
    
    # Not cached, not in the database - make a new one.
	$reference_dna_sequence = GKB::Instance->new(-ONTOLOGY => $dba->ontology, -CLASS => 'ReferenceDNASequence');
	$reference_dna_sequence->inflated(1);
	$reference_dna_sequence->created($self->get_instance_edit_for_effective_user());
	$reference_dna_sequence->Identifier($identifier);
	$reference_dna_sequence->ReferenceDatabase($reference_database);
	if (defined $species) {
		my @species_list = ($species);
		if (scalar($species) =~ /ARRAY/) {
			@species_list = @{$species};
		}
		$reference_dna_sequence->Species(@species_list);
	}
	if (defined $name) {
		$reference_dna_sequence->name($name);
#		$reference_dna_sequence->_displayName($name);
	}
		
#	$dba->store($reference_dna_sequence);
	$self->reference_dna_sequence_cache->{$reference_database->db_id()}->{$identifier} = $reference_dna_sequence;
    
    return $reference_dna_sequence;
}

sub get_reference_rna_sequence {
    my ($self, $reference_database, $crossreference_database, $identifier) = @_;
    
	my $dba = $self->dba;
    my $reference_rna_sequence = $self->reference_rna_sequence_cache->{$identifier};
    if (defined $reference_rna_sequence) {
    	# We already have a copy in the cache
    	return $reference_rna_sequence;
    }
    
	# Look to see if we can find the ReferenceRNASequence entry in
	# the database
	my @query = (
		['identifier',  '=', [$identifier]],
		['referenceDatabase.DB_ID', '=', [$reference_database->db_id()]]
	);
	my $reference_rna_sequences = $dba->fetch_instance_by_remote_attribute('ReferenceRNASequence', \@query);
	if (defined $reference_rna_sequences && scalar(@{$reference_rna_sequences})>0) {
		# Yep, it's in there!
		$reference_rna_sequence = $reference_rna_sequences->[0];
		$reference_rna_sequence->inflate();
		$self->reference_rna_sequence_cache->{$identifier} = $reference_rna_sequence;
		return $reference_rna_sequence;
	}
    
    # Not cached, not in the database - make a new one.
	$reference_rna_sequence = GKB::Instance->new(-ONTOLOGY => $dba->ontology, -CLASS => 'ReferenceRNASequence');
	$reference_rna_sequence->inflated(1);
	$reference_rna_sequence->created($self->get_instance_edit_for_effective_user());
	$reference_rna_sequence->Identifier($identifier);
	$reference_rna_sequence->ReferenceDatabase($reference_database);
	if ($identifier =~ /^NM_/) {
		my $cr = GKB::Instance->new(-ONTOLOGY => $dba->ontology, -CLASS => 'DatabaseIdentifier');
		$cr->inflated(1);
		$cr->Identifier($identifier);
		$cr->ReferenceDatabase($crossreference_database);
		$reference_rna_sequence->CrossReference($cr);
	}
	$self->reference_rna_sequence_cache->{$identifier} = $reference_rna_sequence;
    return $reference_rna_sequence;
}

1;

