=head1 NAME

GKB::IdentifierMapper::ENSEMBLMart

=head1 SYNOPSIS

Uses ENSEMBL Mart for doing identifier mapping.

=head1 DESCRIPTION

=head1 SEE ALSO

=head1 AUTHOR

David Croft E<lt>croft@ebi.ac.ukE<gt>

Copyright (c) 2008 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

# TODO: I think there may be timeout problems with the database handle for long-
# running linking scripts.  To fix this, you probably need to write a get_handle
# script, which tests the connection first with a simple, arbitrary SQL command
# inside an eval, and which tries to reconnect if this fails (due to a stale handle).

package GKB::IdentifierMapper::ENSEMBLMart;

use GKB::Config;
use GKB::SOAPServer::PICR;
use strict;
use vars qw(@ISA $AUTOLOAD %ok_field);
use GKB::IdentifierMapper::Base;

@ISA = qw(GKB::IdentifierMapper::Base);

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
    my($pkg, $wsdl) = @_;

   	# Get class variables from superclass and define any new ones
   	# specific to this class.
	$pkg->get_ok_field();
 	
   	my $self = $pkg->SUPER::new();
   	
   	my %ensembl_mart_table_hash = ();
   	$self->ensembl_mart_table_hash(\%ensembl_mart_table_hash);
   	$self->ensembl_mart_params(undef);
   	$self->handle_to_ensembl_mart(undef);
   	
    return $self;
}

# Needed by subclasses to gain access to class variables defined in
# this base class.
sub get_ok_field {
	my ($pkg) = @_;
	
	%ok_field = $pkg->SUPER::get_ok_field();
	$ok_field{"ensembl_mart_table_hash"}++;
	$ok_field{"ensembl_mart_params"}++;
	$ok_field{"handle_to_ensembl_mart"}++;
	$ok_field{"mart_up"}++;

	return %ok_field;
}

sub refresh {
	my ($self) = @_;
	
	my $e = $self->ensembl_mart_params;
	if (defined $e) {
		print STDERR "ENSEMBLMart.refresh: reconnecting to ENSEMBL BioMart database, just to be on the safe side\n";
		$self->set_ensembl_mart_params($e->[0], $e->[1], $e->[2], $e->[3], $e->[4]);
	}
}

# Set parameters for accessing the ENSEMB Mart.  You may set db_name
# to undef, in which case, the current ENSEMBL Mart will be used.
# Arguments:
#
# db_name
# host
# port
# user
# password
#
# Calling this method will also automatically set up an ENSEMBL Mart handle.
sub set_ensembl_mart_params {
	my ($self, $db_name, $host, $port, $user, $password) = @_;
	
	my $ensembl_mart_params = [$db_name, $host, $port, $user, $password];
	$self->ensembl_mart_params($ensembl_mart_params);
	$self->handle_to_ensembl_mart(GKB::Utils::get_handle_to_ensembl_mart(@{$ensembl_mart_params}));
	$self->mart_up(1);
}

# Gets an ENSEMBL mart table name that can be used for mapping
# from the $input_db database to a (species-specific) $ensembl_db
# database.  If something goes wrong, e.g. an appropriate table
# could not be found, returns undef.
sub get_ensembl_mart_xref_table_name {
	my ($self, $input_db, $ensembl_db) = @_;
	
	my $table_name = $self->ensembl_mart_table_hash->{$input_db}->{$ensembl_db};
	if (defined $table_name) {
		return $table_name;
	}
	
    my $ensembl_mart_species_abbreviation = $ensembl_db;
    $ensembl_mart_species_abbreviation =~ s/^ENSEMBL_//;
    $ensembl_mart_species_abbreviation = $self->generate_ensembl_mart_species_abbreviation($ensembl_mart_species_abbreviation);
	if (!(defined $ensembl_mart_species_abbreviation)) {
		return undef;
	}
	
    $table_name = "$ensembl_mart_species_abbreviation\_gene_ensembl__xref_" . lc($input_db) . "_accession__dm";
    if ($self->check_ensembl_mart_table_existence($table_name)) {
    	$self->ensembl_mart_table_hash->{$input_db}->{$ensembl_db} = $table_name;
    } else {
		print STDERR "ENSEMBLMart.get_ensembl_mart_xref_table_name: WARNING - could not find a Mart table called: $table_name\n";
		return undef;
    }
    
	return $table_name;
}

# Check to see if the named BioMart table exists.
# Returns 0 if it doesnt (!!).
sub check_ensembl_mart_table_existence {
    my ($self, $table) = @_;
    eval {
    	if (!(defined $self->handle_to_ensembl_mart)) {
			print STDERR "ENSEMBLMart.check_ensembl_mart_table_existence: WARNING - handle_to_ensembl_mart is undef\n";
    	}
		$self->handle_to_ensembl_mart()->do("select count(*) from $table"); 
    };
    
    if ($@) {
		if ($@ !~ /doesn\'t exist/) {
			print STDERR "ENSEMBLMart.check_ensembl_mart_table_existence: WARNING - $table exists produces unknown response\n";
			print STDERR "dollars at=" . $@ . "\n";
			$self->mart_up(0);
		}
    } else {
		return 1;
    };
    
    return 0;
}

# For the given input database name, convert the supplied ID into
# appropriate IDs for the named output database.
# Returns a reference to an array of output IDs.
sub convert {
	my ($self, $input_db, $input_id, $output_db) = @_;
	
	my $handle_to_ensembl_mart = $self->handle_to_ensembl_mart;
	if (!(defined $handle_to_ensembl_mart)) {
		print STDERR "ENSEMBLMart.convert: WARNING - handle_to_ensembl_mart is undef!\n";
		return undef;
	}
	
    my $output_ids = [];

    my $table_name = $self->get_ensembl_mart_xref_table_name($input_db, $output_db);
    if (!(defined $table_name)) {
    	return $output_ids;
    }
    
	# Build and execute query against ENSEMBL Mart to get the
	# ENSEMBL gene IDs assocaited with $input_id.
    my $query = "select gene_stable_id from $table_name where dbprimary_id=?";
    my $cur = $handle_to_ensembl_mart->prepare($query);
    $cur->execute($input_id);
    
    # Loop over the rows returned by the database query and add unique
    # ENSEMBL gene identifiers to the list associated with
    # $reference_peptide_sequence.
    my %seen;
    while (my ($ensembl_gene_id) = $cur->fetchrow) {
		if ($seen{$ensembl_gene_id}) {
			next;
		}
		$seen{$ensembl_gene_id} = 1;
		push(@{$output_ids}, $ensembl_gene_id);
    }
	
	return $output_ids;
}

# You *must* supply a species name for list conversion to work.
sub convert_list {
	my ($self, $input_db, $input_ids, $output_db, $species) = @_;
	
	if ($input_db eq 'UniProt') {
		if ($output_db eq 'OMIM') {
			return $self->convert_list_uniprot_to_omim($input_ids, $species);
		}
		if ($output_db eq 'PDB') {
			return $self->convert_list_uniprot_to_pdb($input_ids, $species);
		}
		if ($output_db eq 'RefSeqPeptide') {
			return $self->convert_list_uniprot_to_refseq_peptide($input_ids, $species);
		}
		if ($output_db eq 'RefSeqDNA') {
			return $self->convert_list_uniprot_to_refseq_dna($input_ids, $species);
		}
		if ($output_db eq 'ENSEMBL') {
			return $self->convert_list_uniprot_to_ensembl($input_ids, $species);
		}
		if ($output_db eq 'Wormbase') {
			return $self->convert_list_uniprot_to_wormbase($input_ids, $species);
		}
		if ($output_db eq 'Entrez Gene') {
			return $self->convert_list_uniprot_to_entrez_gene($input_ids, $species);
		}
	} elsif ($input_db eq 'ENSP') {
		if ($output_db eq 'ENSEMBL') {
			return $self->convert_list_ensp_to_ensembl($input_ids, $species);
		}
		if ($output_db eq 'Wormbase') {
			my $output_id_hash = {};
			$self->query_ensembl_mart_with_ensp($input_ids, $output_id_hash, 'wormbase_gene', $species, 0);
			return $output_id_hash;
		}
		if ($output_db eq 'Entrez Gene') {
			my $output_id_hash = {};
			$self->query_ensembl_mart_with_ensp($input_ids, $output_id_hash, 'entrezgene', $species, 0);
			return $output_id_hash;
		}
	}
	
	print STDERR "ENSEMBLMart.convert_list: unknown database pair, i/p=$input_db, o/p=$output_db\n";
	
	return undef;
}

sub convert_list_uniprot_to_omim {
	my ($self, $input_ids, $species) = @_;
	
	my $output_id_hash = {};
	if (!$self->query_ensembl_mart($input_ids, $output_id_hash, 'uniprot_accession', 'mim_morbid', $species)) {
		return undef;
	}
	if (!$self->query_ensembl_mart($input_ids, $output_id_hash, 'uniprot_accession', 'mim_gene', $species)) {
		return undef;
	}

	return $output_id_hash;
}

# Also able to handle input IDs with a version number, separated from
# the main UniProt ID by a hyphen.
sub convert_list_uniprot_to_refseq_peptide {
	my ($self, $input_ids, $species) = @_;
	
	my @input_variant_ids = ();
	my @input_no_variant_ids = ();
	foreach my $input_id (@{$input_ids}) {
		if ($input_id =~ /-/) {
			push(@input_variant_ids, $input_id);
		} else {
			push(@input_no_variant_ids, $input_id);
		}
	}
	
	my $output_id_hash = {};
	if (!($self->query_ensembl_mart(\@input_no_variant_ids, $output_id_hash, 'uniprot_accession', 'refseq_peptide', $species, 1))) {
		return undef;
	}
	if (!($self->query_ensembl_mart(\@input_variant_ids, $output_id_hash, 'uniprot_varsplic', 'refseq_peptide', $species, 1))) {
		return undef;
	}

	return $output_id_hash;
}

# Also able to handle input IDs with a version number, separated from
# the main UniProt ID by a hyphen.
sub convert_list_uniprot_to_pdb {
	my ($self, $input_ids, $species) = @_;
	
	my @input_variant_ids = ();
	my @input_no_variant_ids = ();
	foreach my $input_id (@{$input_ids}) {
		if ($input_id =~ /-/) {
			push(@input_variant_ids, $input_id);
		} else {
			push(@input_no_variant_ids, $input_id);
		}
	}
	
	my $output_id_hash = {};
	if (!($self->query_ensembl_mart(\@input_no_variant_ids, $output_id_hash, 'uniprot_accession', 'pdb', $species, 1))) {
		return undef;
	}
	if (!($self->query_ensembl_mart(\@input_variant_ids, $output_id_hash, 'uniprot_varsplic', 'pdb', $species, 1))) {
		return undef;
	}

	return $output_id_hash;
}

# Also able to handle input IDs with a version number, separated from
# the main UniProt ID by a hyphen.
sub convert_list_uniprot_to_refseq_dna {
	my ($self, $input_ids, $species) = @_;
	
	my @input_variant_ids = ();
	my @input_no_variant_ids = ();
	foreach my $input_id (@{$input_ids}) {
		if ($input_id =~ /-/) {
			push(@input_variant_ids, $input_id);
		} else {
			push(@input_no_variant_ids, $input_id);
		}
	}
	
	my $output_id_hash = {};
	if (!($self->query_ensembl_mart(\@input_no_variant_ids, $output_id_hash, 'uniprot_accession', 'refseq_dna', $species, 1))) {
		return undef;
	}
	if (!($self->query_ensembl_mart(\@input_variant_ids, $output_id_hash, 'uniprot_varsplic', 'refseq_dna', $species, 1))) {
		return undef;
	}

	return $output_id_hash;
}

sub convert_list_uniprot_to_ensembl {
	my ($self, $input_ids, $species) = @_;
	
	my $handle_to_ensembl_mart = $self->handle_to_ensembl_mart;
	if (!(defined $handle_to_ensembl_mart)) {
		print STDERR "ENSEMBLMart.convert_list_uniprot_to_ensembl: WARNING - handle_to_ensembl_mart is undef!\n";
		return undef;
	}
	
	my $underscore_species = $species;
	$underscore_species =~ s/\s+/_/g;
    my $table_name = $self->get_ensembl_mart_xref_table_name("UniProt", "ENSEMBL_$underscore_species");
    if (!(defined $table_name)) {
    	return undef;
    }
    
	# Build and execute query against ENSEMBL Mart to get the
	# ENSEMBL gene IDs assocaited with $input_id.
	my $comma_separated_input_ids = join(',', (('?') x scalar(@{$input_ids})));
    my $query = "select dbprimary_id,gene_stable_id from $table_name where dbprimary_id IN ($comma_separated_input_ids)";
    my $sth = $handle_to_ensembl_mart->prepare($query);
    $sth->execute(@{$input_ids});
    
	my $output_id_hash = {};
	while (my ($acc,$id) = $sth->fetchrow_array) {
	    push(@{$output_id_hash->{uc($acc)}}, $id);
	}

	return $output_id_hash;
}

# Also able to handle input IDs with a version number, separated from
# the main UniProt ID by a hyphen.
sub convert_list_uniprot_to_wormbase {
	my ($self, $input_ids, $species) = @_;
	
	my @input_variant_ids = ();
	my @input_no_variant_ids = ();
	foreach my $input_id (@{$input_ids}) {
		if ($input_id =~ /-/) {
			push(@input_variant_ids, $input_id);
		} else {
			push(@input_no_variant_ids, $input_id);
		}
	}
	
	my $output_id_hash = {};
	if (!($self->query_ensembl_mart(\@input_no_variant_ids, $output_id_hash, 'uniprot_accession', 'wormbase_gene', $species, 1))) {
		return undef;
	}
	if (!($self->query_ensembl_mart(\@input_variant_ids, $output_id_hash, 'uniprot_varsplic', 'wormbase_gene', $species, 1))) {
		return undef;
	}

	return $output_id_hash;
}

# Also able to handle input IDs with a version number, separated from
# the main UniProt ID by a hyphen.
sub convert_list_uniprot_to_entrez_gene {
	my ($self, $input_ids, $species) = @_;
	
	my @input_variant_ids = ();
	my @input_no_variant_ids = ();
	foreach my $input_id (@{$input_ids}) {
		if ($input_id =~ /-/) {
			push(@input_variant_ids, $input_id);
		} else {
			push(@input_no_variant_ids, $input_id);
		}
	}
	
	my $output_id_hash = {};
	if (!($self->query_ensembl_mart(\@input_no_variant_ids, $output_id_hash, 'uniprot_accession', 'entrezgene', $species, 1))) {
		return undef;
	}
	if (!($self->query_ensembl_mart(\@input_variant_ids, $output_id_hash, 'uniprot_varsplic', 'entrezgene', $species, 1))) {
		return undef;
	}

	return $output_id_hash;
}

# This is a special case, because ENSP (and other ENSEMBL-internally used
# protein IDs) are not cross references, so it doesn't make sense to use
# one of the xref tables to do this match.  Instead, use one of the main
# tables.
sub convert_list_ensp_to_ensembl {
	my ($self, $input_ids, $species) = @_;
	
	my $handle_to_ensembl_mart = $self->handle_to_ensembl_mart;
	if (!(defined $handle_to_ensembl_mart)) {
		print STDERR "ENSEMBLMart.convert_list_uniprot_to_ensembl: WARNING - handle_to_ensembl_mart is undef!\n";
		return undef;
	}
	
	my $underscore_species = $species;
	$underscore_species =~ s/\s+/_/g;
    my $ensembl_mart_species_abbreviation = $self->generate_ensembl_mart_species_abbreviation($underscore_species);
    my $table_name = "$ensembl_mart_species_abbreviation\_gene_ensembl__transcript__main";
    
#    print STDERR "ENSEMBLMart.convert_list_uniprot_to_ensembl: table_name=$table_name\n";

    if (!($self->check_ensembl_mart_table_existence($table_name))) {
                print STDERR "ENSEMBLMart.convert_list_ensp_to_ensembl: WARNING - could not find a Mart table called: $table_name\n";
                return undef;
    }
    
	# Build and execute query against ENSEMBL Mart to get the
	# ENSEMBL gene IDs assocaited with $input_id.
	my $comma_separated_input_ids = join(',', (('?') x scalar(@{$input_ids})));
	my $output_id_hash = {};
	if ($self->check_ensembl_mart_table_existence($table_name)) {
	    my $query = "select translation_stable_id,gene_stable_id from $table_name where translation_stable_id IN ($comma_separated_input_ids)";
	
#    	print STDERR "ENSEMBLMart.convert_list_uniprot_to_ensembl: query=$query\n";
#    	print STDERR "ENSEMBLMart.convert_list_uniprot_to_ensembl: input_ids=@{$input_ids}\n";
	    
	    my $sth = $handle_to_ensembl_mart->prepare($query);
	    $sth->execute(@{$input_ids});
	    
		while (my ($acc,$id) = $sth->fetchrow_array) {
		    push(@{$output_id_hash->{uc($acc)}}, $id);
		}
	}

	return $output_id_hash;
}

# Runs a query against the ENSEMBL BioMart, using a join
#
# input_ids			Reference to an array of IDs
# output_id_hash	Reference to a hash to store query result (keys=i/p IDs, values=lists of o/p IDs)
# input_table		Table against which input IDs is queried
# output_table		Table containing output IDs
# species			Species name, e.g. 'Homo sapiens'
# translation		If set to 1, uses 'translation_id' instead of 'gene_id_key' as common key between tables.
#
# Returns 1 if everything ran normally, 0 if something went wrong.
sub query_ensembl_mart {
	my ($self, $input_ids, $output_id_hash, $input_table, $output_table, $species, $translation) = @_;
	
	if (scalar(@{$input_ids})<1) {
		# Empty input, so no point in doing a query
		return 1;
	}
	
	if (!(defined $species)) {
		print STDERR "ENSEMBLMart.query_ensembl_mart: WARNING - no species has been defined, aborting!\n";
		return 0;
	}
	
	my $handle_to_ensembl_mart = $self->handle_to_ensembl_mart;
	if (!(defined $handle_to_ensembl_mart)) {
		print STDERR "ENSEMBLMart.query_ensembl_mart: WARNING - handle_to_ensembl_mart is undef!\n";
		return 0;
	}
	
	my $ensembl_mart_species_abbreviation = $self->generate_ensembl_mart_species_abbreviation($species);
	if (!(defined $ensembl_mart_species_abbreviation)) {
		return 0;
	}
	my $full_input_table = "${ensembl_mart_species_abbreviation}_gene_ensembl__xref_${input_table}__dm";
	if (!($self->check_ensembl_mart_table_existence($full_input_table))) {
		print STDERR "ENSEMBLMart.query_ensembl_mart: WARNING - no ENSEMBL Mart input table: $full_input_table\n";
		return 0;
	}
	my $full_output_table = "${ensembl_mart_species_abbreviation}_gene_ensembl__xref_${output_table}__dm";
	if (!($self->check_ensembl_mart_table_existence($full_output_table))) {
		print STDERR "ENSEMBLMart.query_ensembl_mart: WARNING - no ENSEMBL Mart output table: $full_output_table\n";
		return 0;
	}

	my $shared_key = 'gene_id_key';
	if ($translation) {
		$shared_key = 'translation_id';
	}
	
	my $comma_separated_input_ids = join(',', (('?') x scalar(@{$input_ids})));

	my $stmt = <<__END__;
SELECT A.dbprimary_id, B.dbprimary_id
FROM
$full_input_table A,
$full_output_table B
WHERE
A.${shared_key} = B.${shared_key} AND
B.dbprimary_id IS NOT NULL AND
A.dbprimary_id IN ($comma_separated_input_ids)
__END__
	
	my $sth = $handle_to_ensembl_mart->prepare($stmt);
	$sth->execute(@{$input_ids});
	
	while (my ($acc,$id) = $sth->fetchrow_array) {
	    push(@{$output_id_hash->{uc($acc)}}, $id);
	}
	
	return 1;
}

# Runs a query against the ENSEMBL BioMart, using a join.  This works
# specifically only with ENSP identifiers, where the tables used in
# the join are somewhat different from usual, because ENSP have a special
# status, they are not xrefs, they are internal protein IDs, and are to
# be found in a main table.
#
# input_ids			Reference to an array of IDs
# output_id_hash	Reference to a hash to store query result (keys=i/p IDs, values=lists of o/p IDs)
# output_table		Table containing output IDs
# species			Species name, e.g. 'Homo sapiens'
# translation		If set to 1, uses 'translation_id' instead of 'gene_id_key' as common key between tables.
#
# Returns 1 if everything ran normally, 0 if something went wrong.
sub query_ensembl_mart_with_ensp {
	my ($self, $input_ids, $output_id_hash, $output_table, $species, $translation) = @_;
	
	if (scalar(@{$input_ids})<1) {
		# Empty input, so no point in doing a query
		return 1;
	}
	
	if (!(defined $species)) {
		print STDERR "ENSEMBLMart.query_ensembl_mart_with_ensp: WARNING - no species has been defined, aborting!\n";
		return 0;
	}
	
	my $handle_to_ensembl_mart = $self->handle_to_ensembl_mart;
	if (!(defined $handle_to_ensembl_mart)) {
		print STDERR "ENSEMBLMart.query_ensembl_mart_with_ensp: WARNING - handle_to_ensembl_mart is undef!\n";
		return 0;
	}
	
	my $ensembl_mart_species_abbreviation = $self->generate_ensembl_mart_species_abbreviation($species);
	if (!(defined $ensembl_mart_species_abbreviation)) {
		return 0;
	}
	my $full_input_table = "${ensembl_mart_species_abbreviation}_gene_ensembl__transcript__main";
	if (!($self->check_ensembl_mart_table_existence($full_input_table))) {
		print STDERR "ENSEMBLMart.query_ensembl_mart_with_ensp: WARNING - no ENSEMBL Mart input table: $full_input_table\n";
		return 0;
	}
	my $full_output_table = "${ensembl_mart_species_abbreviation}_gene_ensembl__xref_${output_table}__dm";
	if (!($self->check_ensembl_mart_table_existence($full_output_table))) {
		print STDERR "ENSEMBLMart.query_ensembl_mart_with_ensp: WARNING - no ENSEMBL Mart output table: $full_output_table\n";
		return 0;
	}

	my $shared_key = 'gene_id_key';
	if ($translation) {
		$shared_key = 'translation_id';
	}
	
	my $comma_separated_input_ids = join(',', (('?') x scalar(@{$input_ids})));

	my $stmt = <<__END__;
SELECT A.translation_stable_id, B.dbprimary_id
FROM
$full_input_table A,
$full_output_table B
WHERE
A.${shared_key} = B.${shared_key} AND
B.dbprimary_id IS NOT NULL AND
A.translation_stable_id IN ($comma_separated_input_ids)
__END__
	
	my $sth = $handle_to_ensembl_mart->prepare($stmt);
	$sth->execute(@{$input_ids});
	
	while (my ($acc,$id) = $sth->fetchrow_array) {
	    push(@{$output_id_hash->{uc($acc)}}, $id);
	}
	
	return 1;
}

# Given a species name like 'Homo sapiens', generate the corresponding
# ENSEMBL BioMart two-character equivalent, e.g. 'hs'.
sub generate_ensembl_mart_species_abbreviation {
	my ($self, $species) = @_;
	
	if (!(defined $species)) {
	    print STDERR "ENSEMBLMart.buildPart: WARNING - species not defined!\n";
	    return undef;
	}
	
	my $ensembl_mart_species_abbreviation = undef;
	my $lc_species = lc($species);
	if ($lc_species =~ /^(\w)\w+[\s_]+(\w+)$/) {
	    $ensembl_mart_species_abbreviation = lc("$1$2");
	} else {
	    print STDERR "ENSEMBLMart.buildPart: WARNING - can't form species abbreviation for mart from '$species'.\n";
	}
	
	return $ensembl_mart_species_abbreviation;
}

sub close {
	my ($self) = @_;
	
	$self->handle_to_ensembl_mart->disconnect();
	$self->mart_up(0);
}

1;

