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
use strict;

use feature qw/state/;

use GKB::Config;
use GKB::SOAPServer::PICR;
use GKB::IdentifierMapper::Base;
use GKB::EnsEMBLMartUtils qw/:all/;

use Carp;
use Capture::Tiny qw/:all/;
use Try::Tiny;

use vars qw(@ISA $AUTOLOAD %ok_field);

use Log::Log4perl qw/get_logger/;
Log::Log4perl->init(\$LOG_CONF);

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

    update_registry_file();

    # Get class variables from superclass and define any new ones
    # specific to this class.
    $pkg->get_ok_field();

    my $self = $pkg->SUPER::new();

    return $self;
}

# Needed by subclasses to gain access to class variables defined in
# this base class.
sub get_ok_field {
    my ($pkg) = @_;

    %ok_field = $pkg->SUPER::get_ok_field();

    return %ok_field;
}

# For the given input database name, convert the supplied ID into
# appropriate IDs for the named output database.
# Returns a reference to an array of output IDs.
sub convert {

}

# You *must* supply a species name for list conversion to work.
sub convert_list {
    my ($self, $input_db, $input_ids, $output_db, $species) = @_;

    my $logger = get_logger(__PACKAGE__);

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
	if ($output_db eq 'RefSeqRNA') {
	    return $self->convert_list_uniprot_to_refseq_rna($input_ids, $species);
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
	    $self->query_ensembl_mart_with_ensp($input_ids, $output_id_hash, 'wormbase_gene', $species);
	    return $output_id_hash;
	}
	if ($output_db eq 'Entrez Gene') {
	    my $output_id_hash = {};
	    $self->query_ensembl_mart_with_ensp($input_ids, $output_id_hash, 'entrezgene', $species);
	    return $output_id_hash;
	}
    }

    $logger->warn("unknown database pair, i/p=$input_db, o/p=$output_db\n");

    return undef;
}

sub convert_list_uniprot_to_omim {
    my ($self, $input_ids, $species) = @_;

    my $output_id_hash = {};
    if (!$self->query_ensembl_mart($input_ids, $output_id_hash, 'uniprot_swissprot', 'mim_morbid', $species)) {
    	return undef;
    }
    if (!$self->query_ensembl_mart($input_ids, $output_id_hash, 'uniprot_swissprot', 'mim_gene', $species)) {
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
    if (!($self->query_ensembl_mart(\@input_no_variant_ids, $output_id_hash, 'uniprot_swissprot', 'refseq_peptide', $species))) {
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
    if (!($self->query_ensembl_mart(\@input_no_variant_ids, $output_id_hash, 'uniprot_swissprot', 'pdb', $species))) {
        return undef;
    }

    return $output_id_hash;
}

# Also able to handle input IDs with a version number, separated from
# the main UniProt ID by a hyphen.
sub convert_list_uniprot_to_refseq_rna {
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
    if (!($self->query_ensembl_mart(\@input_no_variant_ids, $output_id_hash, 'uniprot_swissprot', 'refseq_mrna', $species))) {
    	return undef;
    }

    return $output_id_hash;
}

sub convert_list_uniprot_to_ensembl {
    my ($self, $input_ids, $species) = @_;

    my $logger = get_logger(__PACKAGE__);
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
    if (!($self->query_ensembl_mart(\@input_no_variant_ids, $output_id_hash, 'uniprot_swissprot', 'ensembl_gene_id', $species))) {
    	return undef;
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
    if (!($self->query_ensembl_mart(\@input_no_variant_ids, $output_id_hash, 'uniprot_swissprot', 'wormbase_gene', $species))) {
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
    if (!($self->query_ensembl_mart(\@input_no_variant_ids, $output_id_hash, 'uniprot_swissprot', 'entrezgene', $species))) {
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

    my $logger = get_logger(__PACKAGE__);

    my $output_id_hash = {};

    if (!($self->query_ensembl_mart($input_ids, $output_id_hash, 'ensembl_peptide_id', 'ensembl_gene_id', $species))) {
    	return undef;
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
#
# Returns 1 if everything ran normally, 0 if something went wrong.
sub query_ensembl_mart {
    my ($self, $input_ids, $output_id_hash, $input_table, $output_table, $species) = @_;

    my $logger = get_logger(__PACKAGE__);

    if (scalar(@{$input_ids})<1) {
    	# Empty input, so no point in doing a query
    	return 1;
    }

    if (!(defined $species)) {
    	$logger->warn("no species has been defined, aborting!\n");
    	return 0;
    }

    my $ensembl_mart_species_abbreviation = $self->generate_ensembl_mart_species_abbreviation($species);
    if (!(defined $ensembl_mart_species_abbreviation)) {
    	return 0;
    }

    my %input;
    $input{$_}++ foreach @{$input_ids};

    my $query_output = get_query_results($ensembl_mart_species_abbreviation, $input_table, $output_table);
    if (!$query_output) {
        $logger->warn("Query results could not be obtained");
        return 0;
    }

    my @lines = split "\n", $query_output;
    foreach my $line (@lines) {
        chomp $line;
        my ($acc,$id) = split "\t", $line;
        next unless $id;
        next unless exists $input{$acc};
        push(@{$output_id_hash->{uc($acc)}}, $id);
    }

    return 1;
}

sub get_query_results {
    my $species = shift;
    my $input_table = shift;
    my $output_table = shift;

    my $species_results;
    my $results_complete;
    my $query_attempts = 0;
    until (($species_results && $results_complete) || $query_attempts == 3) {
        $query_attempts += 1;

        $species_results = capture_stdout {
            system(get_wget_query_for_input_and_output_tables($species . "_gene_ensembl", $input_table, $output_table));
        };

        $results_complete = $species_results =~ /\[success\]$/;
    }

    return $species_results;
}

sub get_wget_query_for_input_and_output_tables {
    my $mart_dataset = shift;
    my $input_table = shift;
    my $output_table = shift;

    return "wget -q -O - 'http://www.ensembl.org/biomart/martservice?query=" .
        get_xml_query_for_input_and_output_tables($mart_dataset, $input_table, $output_table) . "'";
}

sub get_xml_query_for_input_and_output_tables {
    my $dataset = shift;
    my $input_table = shift;
    my $output_table = shift;

    $dataset // confess "No dataset defined\n";

    return <<XML;
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE Query>
<Query  virtualSchemaName = "default" formatter = "TSV" header = "0" uniqueRows = "0" count = "" completionStamp = "1">
	<Dataset name = "$dataset" interface = "default" >
		<Attribute name = "$input_table" />
		<Attribute name = "$output_table" />
	</Dataset>
</Query>
XML
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
#
# Returns 1 if everything ran normally, 0 if something went wrong.
sub query_ensembl_mart_with_ensp {
    my ($self, $input_ids, $output_id_hash, $output_table, $species) = @_;

    return $self->query_ensembl_mart($input_ids, $output_id_hash, 'ensembl_peptide_id', $output_table, $species);
}

# Given a species name like 'Homo sapiens', generate the corresponding
# ENSEMBL BioMart two-character equivalent, e.g. 'hs'.
sub generate_ensembl_mart_species_abbreviation {
    my ($self, $species) = @_;

    my $logger = get_logger(__PACKAGE__);

    if (!(defined $species)) {
        $logger->warn("species not defined!\n");
        return undef;
    }

    my $ensembl_mart_species_abbreviation = undef;
    my $lc_species = lc($species);
    if ($lc_species =~ /^(\w)\w+[\s_]+(\w+)$/) {
        $ensembl_mart_species_abbreviation = lc("$1$2");
    } else {
        $logger->warn("can't form species abbreviation for mart from '$species'.\n");
    }

    return $ensembl_mart_species_abbreviation;
}

sub close {
    my ($self) = @_;
}

1;
