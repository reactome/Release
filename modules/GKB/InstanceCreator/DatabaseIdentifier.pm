=head1 NAME

GKB::InstanceCreator::DatabaseIdentifier

=head1 SYNOPSIS

Methods that create various DatabaseIdentifier instances.

=head1 DESCRIPTION

Most of these methods follow a similar pattern: given the arguments,
look to see if a corresponding instance exists in the database and
return that where possible.  Otherwise, create a new instance and
return that.

This class must be instantiated before you can use it, there are no
static methods.  It makes use of caching, so for maximum speed,
you should try to create only one object from this class, and use
it everywhere.

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

package GKB::InstanceCreator::DatabaseIdentifier;

use GKB::DBAdaptor;
use GKB::InstanceCreator::ReferenceDatabase;
use strict;
use vars qw(@ISA $AUTOLOAD %ok_field);
use Bio::Root::Root;

@ISA = qw(Bio::Root::Root);

# List the object variables here, so that they can be checked
for my $attr
    (qw(
    dba
    database_identifier_cache
    miscellaneous
    reference_database
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
   	$dba && $self->set_dba($dba);

   	my %database_identifier_cache = ();
   	$self->database_identifier_cache(\%database_identifier_cache);
   	$self->miscellaneous(GKB::InstanceCreator::Miscellaneous->new());
   	$self->reference_database(GKB::InstanceCreator::ReferenceDatabase->new());

    return $self;
}

# Set the DBAdaptor
sub set_dba {
	my ($self, $dba) = @_;
	
	$self->dba($dba);
	$self->miscellaneous->set_dba($dba);
   	$self->reference_database->set_dba($dba);
}

# Set the ReferenceDatabase object
sub set_reference_database {
	my ($self, $reference_database) = @_;
	
	$self->reference_database($reference_database);
}

# Set the Miscellaneous object
sub set_miscellaneous {
	my ($self, $miscellaneous) = @_;
	
	$self->miscellaneous($miscellaneous);
}

# Get a DatabaseIdentifier instance, given reference database and ID
# Arguments:
#
# reference_database - ReferenceDatabase instance
# id - valid ID for the ReferenceDatabase
# use_existing - if 1, don't try to create a new instance if one couldn't be found in DB
#
# Returns a DatabaseIdentifier instance.
sub get_database_identifier {
	my ($self, $reference_database, $identifier, $use_existing) = @_;
	
    if (!(defined $reference_database)) {
    	print STDERR "DatabaseIdentifier.get_database_identifier: reference_database not defined!\n";
    	return undef;
    }
    if (!(defined $identifier)) {
    	print STDERR "DatabaseIdentifier.get_database_identifier: identifier not defined!\n";
    	return undef;
    }
    
    my $database_identifier = $self->database_identifier_cache->{$reference_database->db_id()}->{$identifier};
    if (defined $database_identifier) {
    	# We already have a copy in the cache
    	return $database_identifier;
    }

    my $dba = $self->dba;
    
	# Look to see if we can find the ReferenceDatabase entry in
	# the database
	my @query = (
		['identifier',  '=', [$identifier]],
		['referenceDatabase.DB_ID', '=', [$reference_database->db_id()]]
	);
	my $database_identifiers = $dba->fetch_instance_by_remote_attribute('DatabaseIdentifier', \@query);
	$database_identifier = undef;
	if (defined $database_identifiers && scalar(@{$database_identifiers})>0) {
		# Yep, it's in there!
		$database_identifier = $database_identifiers->[0];
		$database_identifier->inflate();
		$self->database_identifier_cache->{$reference_database->db_id()}->{$identifier} = $database_identifier;
		return $database_identifier;
	}
	if ($use_existing) {
		# Don't try to create a new instance if this flag is set
		return undef;
	}
	
    # Not cached, not in the database - make a new one.
	$database_identifier = GKB::Instance->new(-ONTOLOGY => $dba->ontology, -CLASS => 'DatabaseIdentifier');
	$database_identifier->inflated(1);
	$database_identifier->created($self->miscellaneous->get_instance_edit_for_effective_user());
	$database_identifier->identifier($identifier);
	$database_identifier->referenceDatabase($reference_database);
	$dba->store($database_identifier);
	$self->database_identifier_cache->{$reference_database->db_id()}->{$identifier} = $database_identifier;

	return $database_identifier;
}

# Gets a DatabaseIdentifier instance for BioGPS Gene.
#
# Returns a DatabaseIdentifier instance.
sub get_biogps_gene_database_identifier {
	my ($self, $identifier) = @_;

	return $self->get_database_identifier($self->reference_database->get_biogps_gene_reference_database(), $identifier);
}

# Gets a DatabaseIdentifier instance for Brenda.
#
# Returns a DatabaseIdentifier instance.
sub get_brenda_database_identifier {
	my ($self, $identifier) = @_;

	return $self->get_database_identifier($self->reference_database->get_brenda_reference_database(), $identifier);
}

# Gets a DatabaseIdentifier instance for CTD Gene.
#
# Returns a DatabaseIdentifier instance.
sub get_ctd_gene_database_identifier {
    my ($self, $identifier) = @_;
    
	return $self->get_database_identifier($self->reference_database->get_ctd_gene_reference_database(), $identifier);
}

# Gets a DatabaseIdentifier instance for NCBI dbSNP Gene.
#
# Returns a DatabaseIdentifier instance.
sub get_dbsnp_gene_database_identifier {
    my ($self, $identifier) = @_;
    
	return $self->get_database_identifier($self->reference_database->get_dbsnp_gene_reference_database(), $identifier);
}

# Gets a DatabaseIdentifier instance for DOCK Blaster.
#
# Returns a DatabaseIdentifier instance.
sub get_dockblaster_database_identifier {
    my ($self, $identifier) = @_;
    
	return $self->get_database_identifier($self->reference_database->get_dockblaster_reference_database(), $identifier);
}

# Gets a DatabaseIdentifier instance for EC enzymes.
#
# Returns a DatabaseIdentifier instance.
sub get_ec_database_identifier {
	my ($self, $identifier) = @_;

	return $self->get_database_identifier($self->reference_database->get_ec_reference_database(), $identifier);
}

# Gets a DatabaseIdentifier instance for ENSEMBL.
# Arguments:
#
# ensembl_species - name for a species.  Default: Homo_sapiens (human)
#
# Returns a DatabaseIdentifier instance.
sub get_ensembl_database_identifier {
    my ($self, $identifier, $species) = @_;
    
	return $self->get_database_identifier($self->reference_database->get_ensembl_reference_database($species), $identifier);
}

# Gets a DatabaseIdentifier instance for Entrez Gene.
#
# Returns a DatabaseIdentifier instance.
sub get_entrez_gene_database_identifier {
    my ($self, $identifier) = @_;
    
	return $self->get_database_identifier($self->reference_database->get_entrez_gene_reference_database(), $identifier);
}

# Gets a DatabaseIdentifier instance for FlyBase.
#
# Returns a DatabaseIdentifier instance.
sub get_flybase_database_identifier {
    my ($self, $identifier) = @_;
    
	return $self->get_database_identifier($self->reference_database->get_flybase_reference_database(), $identifier);
}

# 
sub get_genecards_database_identifier {
	my ($self, $identifier) = @_;
	
	return $self->get_database_identifier($self->reference_database->get_genecards_reference_database(), $identifier);
}

# Gets a DatabaseIdentifier instance for HapMap.
#
# Returns a DatabaseIdentifier instance.
sub get_hapmap_database_identifier {
    my ($self, $identifier) = @_;
    
	return $self->get_database_identifier($self->reference_database->get_brenda_reference_database(), $identifier);
}

# Gets a DatabaseIdentifier instance for IntAct.
#
# Returns a DatabaseIdentifier instance.
sub get_intact_database_identifier {
	my ($self, $identifier) = @_;

#	return $self->get_database_identifier($self->reference_database->get_hapmap_reference_database(), $identifier);
	return $self->get_database_identifier($self->reference_database->get_intact_reference_database(), $identifier);
}

# Gets a DatabaseIdentifier instance for IntEnz.
#
# Returns a DatabaseIdentifier instance.
sub get_intenz_database_identifier {
	my ($self, $identifier) = @_;

	return $self->get_database_identifier($self->reference_database->get_intenz_reference_database(), $identifier);
}

# Gets a DatabaseIdentifier instance for KEGG genes.
#
# Returns a DatabaseIdentifier instance.
sub get_kegg_database_identifier {
	my ($self, $identifier, $species) = @_;

	return $self->get_database_identifier($self->reference_database->get_kegg_reference_database($species), $identifier);
}

# Gets a DatabaseIdentifier instance for Omim.
#
# Returns a DatabaseIdentifier instance.
sub get_omim_database_identifier {
    my ($self, $identifier) = @_;
    
	return $self->get_database_identifier($self->reference_database->get_omim_reference_database(), $identifier);
}

# Gets a DatabaseIdentifier instance for Orphanet.
#
# Returns a DatabaseIdentifier instance.
sub get_orphanet_database_identifier {
    my ($self, $identifier) = @_;
    
	return $self->get_database_identifier($self->reference_database->get_orphanet_reference_database(), $identifier);
}

# Gets a DatabaseIdentifier instance for PDB.
#
# Returns a DatabaseIdentifier instance.
sub get_pdb_database_identifier {
    my ($self, $identifier) = @_;
    
	return $self->get_database_identifier($self->reference_database->get_pdb_reference_database(), $identifier);
}

# Gets a DatabaseIdentifier instance for Rhea.
#
# Returns a DatabaseIdentifier instance.
sub get_rhea_database_identifier {
    my ($self, $identifier) = @_;
    
	return $self->get_database_identifier($self->reference_database->get_rhea_reference_database(), $identifier);
}

# Gets a DatabaseIdentifier instance for RefSeq.
#
# Returns a DatabaseIdentifier instance.
sub get_refseq_database_identifier {
    my ($self, $identifier) = @_;
    
	return $self->get_database_identifier($self->reference_database->get_refseq_reference_database(), $identifier);
}

# Gets a DatabaseIdentifier instance for UniProt.
#
# Returns a DatabaseIdentifier instance.
sub get_uniprot_database_identifier {
    my ($self, $identifier) = @_;
    
	return $self->get_database_identifier($self->reference_database->get_uniprot_reference_database(), $identifier);
}

# Gets a DatabaseIdentifier instance for UCSC.
#
# Returns a DatabaseIdentifier instance.
sub get_ucsc_database_identifier {
    my ($self, $identifier) = @_;
    
	return $self->get_database_identifier($self->reference_database->get_ucsc_reference_database(), $identifier);
}

# Gets a DatabaseIdentifier instance for Wormbase.
#
# Returns a DatabaseIdentifier instance.
sub get_wormbase_database_identifier {
    my ($self, $identifier) = @_;
    
	return $self->get_database_identifier($self->reference_database->get_wormbase_reference_database(), $identifier);
}

1;

