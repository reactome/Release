=head1 NAME

GKB::AddLinks::KEGGReferenceGeneToReferencePeptideSequence

=head1 SYNOPSIS

=head1 DESCRIPTION

This class adds KEGG Gene links to Uniprot ReferenceGeneProducts.
The kegg_gene identifiers are retrieved via the KEGG web services.
This is rather slow, reckon with a run time of at least a day.

Also creates links for EC, BRENDA and IntEnz.

Original code lifted from the script addKEGGSeq2swallSDI.pl, from David.

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

package GKB::AddLinks::KEGGReferenceGeneToReferencePeptideSequence;

use GKB::Config;
use GKB::AddLinks::Builder;
use GKB::HTMLUtils;
use strict;
use Data::Dumper;
use vars qw(@ISA $AUTOLOAD %ok_field);

@ISA = qw(GKB::AddLinks::Builder);

my %id_hash = ();

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

       # Get class variables from superclass and define any new ones
       # specific to this class.
    $pkg->get_ok_field();

       my $self = $pkg->SUPER::new();
       
    return $self;
}

# Needed by subclasses to gain access to object variables defined in
# this class.
sub get_ok_field {
    my ($pkg) = @_;
    
    %ok_field = $pkg->SUPER::get_ok_field();

    return %ok_field;
}

sub clear_variables {
    my ($self) = @_;
    
    $self->SUPER::clear_variables();
}

sub buildPart {
    my ($self) = @_;
    
    print STDERR "\n\nKEGGReferenceGeneToReferencePeptideSequence.buildPart: entered\n";
    $self->class_name("KEGGReferenceGeneToReferencePeptideSequence");
    
    $self->timer->start($self->timer_message);
    my $dba = $self->builder_params->refresh_dba();

    my $reference_peptide_sequences = $self->fetch_reference_peptide_sequences(1);
    my $reference_peptide_sequences_size = scalar(@{$reference_peptide_sequences});
    
    my $attribute = 'referenceGene';
    $self->set_instance_edit_note("{$attribute}s inserted by KEGGReferenceGeneToReferencePeptideSequence");

    # Loop over proteins and add stuff associated with KEGG.
    # This is done chunk-wise rather than one protein at a time, to reduce
    # the total number of KEGG web services calls that have to be made.
    my $reference_peptide_sequence_count = 0;
    my @reference_peptide_sequences_chunk;
    my $chunk_size = 50;
    my $species;
    foreach my $reference_peptide_sequence (@{$reference_peptide_sequences}) {
        if (!(defined $reference_peptide_sequence->species->[0])) {
            print STDERR "KEGGReferenceGeneToReferencePeptideSequence.buildPart: WARNING - No species supplied for DB_ID " . $reference_peptide_sequence->db_id() . "\n";
            next;
        }
        
        my $underline_species = $reference_peptide_sequence->species->[0]->_displayName->[0];
        $underline_species =~ s/ +/_/g;
        my $kegg_reference_database = $self->builder_params->reference_database->get_kegg_reference_database($underline_species);
    
        # Remove KEGG gene identifiers to make sure the mapping is up-to-date, keep others
        # this isn't really necessary as long as the script is run on the slice only
        # But a good thing to have if you need to run the script a second time.
        $self->remove_typed_instances_from_attribute($reference_peptide_sequence, $attribute, $kegg_reference_database);
        
        push(@reference_peptide_sequences_chunk, $reference_peptide_sequence);
    
        if ($reference_peptide_sequence_count>0 && ($reference_peptide_sequence_count%$chunk_size)==0) {
            $self->process_accumulated_reference_peptide_sequences_chunk(\@reference_peptide_sequences_chunk);
            
            # Reset cumulative variable(s)
            @reference_peptide_sequences_chunk = ();
        }
    
        $reference_peptide_sequence_count++;
    }
    
    # Pick up any remaining ReferenceGeneProducts that didn't get
    # processed at an exact chunk multiple
    $self->process_accumulated_reference_peptide_sequences_chunk(\@reference_peptide_sequences_chunk);

    $self->print_insertion_stats_hash();
    
    print STDERR "\n";
    print STDERR "KEGGReferenceGeneToReferencePeptideSequence.insert_xrefs: number of unique IDs inserted: " . scalar(keys(%id_hash)) . "\n";
    print STDERR "\n";

    $self->timer->stop($self->timer_message);
    $self->timer->print();
}

# No return value.
sub process_accumulated_reference_peptide_sequences_chunk {
    my ($self, $reference_peptide_sequences_chunk) = @_;
    
    if (scalar(@{$reference_peptide_sequences_chunk})<1) {
        # Nothing to process
        return;
    }

    my $dba = $self->builder_params->get_dba();
    # Create a query from the accumulated ReferencePeptideSequnces, using
    # the identifier attributes, and assuming that the identifiers are
    # UniProt accession numbers.
    my $query_string = '';
    my $reference_peptide_sequence;
    my $reference_peptide_sequence_identifier;
    foreach $reference_peptide_sequence (@{$reference_peptide_sequences_chunk}) {
        $reference_peptide_sequence_identifier = $reference_peptide_sequence->identifier->[0];
        chomp($reference_peptide_sequence_identifier);
        
        if (!( $query_string eq '')) {
            $query_string .= "+";
        }
        $query_string .= "uniprot:$reference_peptide_sequence_identifier";
    }

    # Run the UniProt IDs against KEGG web services, pulling back information
    # on the associated KEGG gene IDs, in tab-delimited format.
    my $output = $self->bconv($query_string);
    if ($output eq '') {
        print STDERR "KEGGReferenceGeneToReferencePeptideSequence.process_accumulated_reference_peptide_sequences_chunk: WARNING - output is empty!!\n";
        return;
    }
    
    # Extract UniProt IDs and corresponding KEGG gene IDs from output.  There
    # should be one line per UniProt ID, in a possibly arbitrary order, thus
    # we use a hash to store the information pulled out of it.
    my %uniprot_to_kegg_gene = ();
    my @lines = split(/\n/, $output);
    my $line;
    my @cols;
    my $kegg_gene_id;
    my $uniprot_identifier;
    foreach $line (@lines) {
        @cols = split(/\t/, $line);
        if (scalar(@cols)<2) {
            print STDERR "KEGGReferenceGeneToReferencePeptideSequence.process_accumulated_reference_peptide_sequences_chunk: WARNING - expected 2 cols, got " . scalar(@cols) . ", line=$line, skipping\n";
            next;
        }
        
        $uniprot_identifier = $cols[0];
        $uniprot_identifier =~ s/^[a-z]+://;
        $kegg_gene_id = $cols[1];
        $kegg_gene_id =~ s/^[a-z]+://;
        my %kegg_gene_ids = ();
        if (defined $uniprot_to_kegg_gene{$uniprot_identifier}) {
            # Get any KEGG gene IDs for this UniProt ID, if it has
            # already been encountered.
            %kegg_gene_ids = %{$uniprot_to_kegg_gene{$uniprot_identifier}};
        }
        
        # Add the extracted KEGG gene ID to any other KEGG gene IDs
        # that may have been collected for this UniProt ID.  Use a
        # hash, to avoid duplication.
        $kegg_gene_ids{$kegg_gene_id} = $kegg_gene_id;
        $uniprot_to_kegg_gene{$uniprot_identifier} = \%kegg_gene_ids;
    }

    # Create KEGG gene instances for each UniProt ID
    my $reference_peptide_sequence_count = 0;
    my $kegg_name;
    my @enzymes;
    my $kegg_entry;
    my $reference_gene;
    my $j;
    my $ec_num;
    foreach $reference_peptide_sequence (@{$reference_peptide_sequences_chunk}) {
        my $underline_species = $reference_peptide_sequence->species->[0]->_displayName->[0];
        $underline_species =~ s/ +/_/g;
        my $reference_database_kegg = $self->builder_params->reference_database->get_kegg_reference_database($underline_species);
        my $reference_database_kegg_db_id = $reference_database_kegg->db_id();
        $reference_peptide_sequence_identifier = $reference_peptide_sequence->identifier->[0];
        chomp($reference_peptide_sequence_identifier);

        if (!( defined $reference_peptide_sequence_identifier ) || $reference_peptide_sequence_identifier eq '') {
            print STDERR "KEGGReferenceGeneToReferencePeptideSequence.process_accumulated_reference_peptide_sequences_chunk: WARNING - no identifier for ReferenceProteinSequence DB_ID=" . $reference_peptide_sequence->db_id() . "\n";
            next;
        }

        if (!(defined $uniprot_to_kegg_gene{$reference_peptide_sequence_identifier})) {
#            print STDERR "KEGGReferenceGeneToReferencePeptideSequence.process_accumulated_reference_peptide_sequences_chunk: WARNING - we dont seem to have any KEGG gene IDs for UniProt identifier $reference_peptide_sequence_identifier!!\n";
            next;
        }

        my @reference_genes = ();
        $kegg_name = undef;

        # Get the KEGG gene ID(s) associated with the current UniProt ID
        # and loop over them
        foreach $kegg_gene_id (sort(keys(%{$uniprot_to_kegg_gene{$reference_peptide_sequence_identifier}}))) {
            # Try to get KEGG gene associated information from the KEGG RESTful server.
            $kegg_entry = undef;
            if (defined $kegg_gene_id && !($kegg_gene_id eq '')) {
                $kegg_entry = $self->bget("hsa:$kegg_gene_id"); # TODO: this will only fetch human genes
            } else {
                print STDERR "KEGGReferenceGeneToReferencePeptideSequence.process_accumulated_reference_peptide_sequences_chunk: WARNING - kegg_gene_id is not defined or is empty, skipping\n";
                next;
            }
            if (!(defined $kegg_entry)) {
                print STDERR "KEGGReferenceGeneToReferencePeptideSequence.process_accumulated_reference_peptide_sequences_chunk: WARNING - Possible problem when trying to get kegg_entry, skipping\n";
                next;
            }

            # Chop the KEGG entry into lines and parse out
            # things of interest
            @lines = split( /\n/, $kegg_entry );

            # This loop puts EC-number-related DatabaseIdentifier instances
            # into the gene's cross reference slot.
            # Does this really make sense?  Wouldn't it be better to insert
            # these into the (currently nonexistent) crossReference slot
            # of GO_MolecularFunction?
            @enzymes = ();
            my $line_num = 0;
            for ($line_num = 0; $line_num < scalar(@lines); $line_num++) {
                $line = $lines[$line_num];

                # Loop over the lines until we find the DEFINITION line; extract
                # the desired information from this line and then break out of
                # the loop.
                if ($line =~ /DEFINITION/) {
                    # Get a name for the gene
                    $kegg_name = $self->extract_kegg_name($line);
                    my @ec_num_arr = $self->extract_ec_numbers($line);
                    $self->add_enzymes(\@enzymes, \@ec_num_arr);

                    if (scalar(@ec_num_arr) > 0) {
                        last;
                    }
                } elsif ($line =~ /ORTHOLOGY/) {
                    # Get a name for the gene
                    my @ec_num_arr = $self->extract_ec_numbers($line);
                    $self->add_enzymes(\@enzymes, \@ec_num_arr);

                    if (defined $kegg_name) {
                        last;
                    }
                }
            }

            if (!(defined $kegg_name)) {
                $kegg_name = $kegg_gene_id;
            }
            
            my $kegg_ref_dna_instance = $self->get_KEGG_ReferenceDNASequence($kegg_gene_id, $kegg_name, \@enzymes, $reference_peptide_sequence->species);

            push(@reference_genes, $kegg_ref_dna_instance);
            
            $id_hash{$kegg_gene_id}++;
        }

        if (scalar(@reference_genes)<1) {
            print STDERR "KEGGReferenceGeneToReferencePeptideSequence.process_accumulated_reference_peptide_sequences_chunk: WARNING - No genes are associated with UniProt ID $reference_peptide_sequence_identifier\n";
            next;
        }
        
        print STDERR "KEGGReferenceGeneToReferencePeptideSequence.process_accumulated_reference_peptide_sequences_chunk: " . $reference_peptide_sequence->identifier->[0] . "\t", join(',',map {$_->identifier->[0]} @reference_genes), "\n";

        # Remove original KEGG gene identifiers to make sure the mapping is
        # up-to-date, keep non-KEGG reference genes.
        foreach (@{$reference_peptide_sequence->referenceGene}) {
            if ($_->referenceDatabase->[0]->db_id() != $reference_database_kegg_db_id) {
                push(@reference_genes, $_);
            }
        }
        
        # Incorporate KEGG genes as reference genes in UniProt entry
        $reference_peptide_sequence->inflate;
        $reference_peptide_sequence->referenceGene(undef);
        $reference_peptide_sequence->add_attribute_value('referenceGene', @reference_genes);
        $reference_peptide_sequence->add_attribute_value('modified', $self->instance_edit);

        $dba->update($reference_peptide_sequence);

            
        # Keep a count of what we have done
        $self->increment_insertion_stats_hash($reference_peptide_sequence->db_id);
        $reference_peptide_sequence_count++;
    }
}

# Returns a ReferenceDNASequence instance corresponding to a KEGG gene.
# If one could already be found in the database, that will be returned,
# otherwise a new one will be created.  Any cross references supplied
# will be used to replace old cross references of the same type, should
# there be any.
#
# Arguments:
#
# kegg_id - the identifier
# kegg_name - a more descriptive name for the gene
# cross_references - a reference to a list of cross-reference instances
# species - a reference to a list of species instances
#
# Returns a ReferenceDNASequence instance
sub get_KEGG_ReferenceDNASequence {
    my ($self, $kegg_id, $kegg_name, $cross_references, $species) = @_;

    my $dba = $self->builder_params->get_dba();
    my $underline_species = undef;
    if (defined $species && scalar@{$species}>0) {
        my $first_species = $species->[0];
        $first_species->inflate();
        $underline_species = $first_species->_displayName->[0];
        if (!(defined $underline_species)) {
            $underline_species = $first_species->name->[0];
        }
        $underline_species =~ s/ +/_/g;
#        print STDERR "KEGGReferenceGeneToReferencePeptideSequence.get_KEGG_ReferenceDNASequence: underline_species=$underline_species for kegg_id=$kegg_id\n";
    } else {
        print STDERR "KEGGReferenceGeneToReferencePeptideSequence.get_KEGG_ReferenceDNASequence: WARNING - species is undef for kegg_id=$kegg_id!!!\n";
    }
    my $reference_database_kegg = $self->builder_params->reference_database->get_kegg_reference_database($underline_species);

    # Look to see if we can find the ReferenceDatabase entry in
    # the database already
    my @query = (['identifier', '=', [$kegg_id]], ['referenceDatabase._displayName', '=', [$reference_database_kegg->_displayName->[0]]]);
    my $instances = $dba->fetch_instance_by_remote_attribute('ReferenceDNASequence', \@query);

    my $instance = undef;
    if (defined $instances && defined $instances->[0]) {
        $instance = $instances->[0];
        $instance->inflate();
        
        # TODO: do these really need to be changed?
        $instance->species(@{$species});
        $instance->name($kegg_name);
        $instance->_displayName($kegg_name);
        
        $dba->update_attribute($instance, 'species');
        $dba->update_attribute($instance, 'name');
        $dba->update_attribute($instance, '_displayName');
    } else {
        # Construct a new ReferenceDNASequence
        $instance = $self->builder_params->miscellaneous->get_reference_dna_sequence($species, $reference_database_kegg, $kegg_id, $kegg_name);
    }
    
    # TODO: Do we really want to put the xrefs a ReferenceDNASequence
    # instance, or would it be better to put them into the corresponding
    # ReferenceGeneProduct instance?
    if (!(defined $cross_references)) {
        return $instance;
    }
    
    my $previous_cross_references = $instance->crossReference;
        
    # Make a note of existing cross references of the same type
    my %reference_database_db_id_hash = ();
    my $reference_database_db_id;
    foreach my $cross_reference (@{$previous_cross_references}) {
        if (scalar(@{$cross_reference->referenceDatabase})>0) {
            $reference_database_db_id = $cross_reference->referenceDatabase->[0]->db_id();
            $reference_database_db_id_hash{$reference_database_db_id} = $reference_database_db_id;
        }
    }
        
    my @new_cross_references = ();
    if (scalar(keys(%reference_database_db_id_hash))>0) {
        # Harvest previous cross references that arn't of the same types
        # as the new ones we want to add
        foreach my $cross_reference (@{$previous_cross_references}) {
            if (scalar(@{$cross_reference->referenceDatabase})>0) {
                $reference_database_db_id = $cross_reference->referenceDatabase->[0]->db_id();
                if (!$reference_database_db_id_hash{$reference_database_db_id}) {
                    push(@new_cross_references, $cross_reference);
                }
            } else {
                print STDERR "KEGGReferenceGeneToReferencePeptideSequence.get_KEGG_ReferenceDNASequence: WARNING - no referenceDatabase for this xref!!\n";
        
                push(@new_cross_references, $cross_reference);
            }
        }
    }
        
    foreach my $cross_reference (@{$cross_references}) {
        push(@new_cross_references, $cross_reference);
    }
        
            
    if (scalar(@new_cross_references)>0) {
        # Null out pre-existing cross references and replace with the
        # list just harvested.
        $instance->crossReference(undef);
        $instance->add_attribute_value('crossReference', @new_cross_references);
    }

    if (defined $instances && defined $instances->[0]) {
        # Only do an update if instance already exists in database.
        $dba->update_attribute($instance, 'crossReference');
    }

    return $instance;
}

# Get KEGG name from supplied test line, if available.
sub extract_kegg_name {
    my ($self, $line) = @_;

    my $kegg_name = GKB::HTMLUtils->remove_html_tags($line);
    $kegg_name =~ s/^[A-Z]+[ \t]//;
    if ($kegg_name =~ /[\[\(]EC:/) {
        $kegg_name =~ /^(.*)[\[\(](EC:[0-9\.\- ]*[\]\)]{0,1})/;
        $kegg_name = $1;
    }

    return $kegg_name;
}

# Get EC numbers from supplied test line, if available.
sub extract_ec_numbers {
    my ($self, $line) = @_;

    my $kegg_name = GKB::HTMLUtils->remove_html_tags($line);
    my @ec_num_arr = [];
    if ($kegg_name =~ /[\[\(]EC:/) {
        $kegg_name =~ /^(.*)[\[\(](EC:[0-9\.\- ]*[\]\)]{0,1})/;
        my $ec_nums = $2;
        $ec_nums =~ s/^EC://;    # Remove redundant stuff

        # Make sure that there is a closing ] around the
        # list of EC numbers.  If not, don't trust the
        # last one.  That's because the KEGG name field
        # is sometimes arbitrarily truncated.
        my $correctly_terminated = 0;
        if ($ec_nums =~ /[\]\)]$/) {
            $correctly_terminated = 1;
            $ec_nums =~ s/[\]\)]$//;
        }
        
        # there may be more than one EC number present -
        # a space is used as a separator in that case.
        my @incorrect_ec_num_arr = split(' ', $ec_nums);
        my $ec_count = scalar(@incorrect_ec_num_arr);
        if (!$correctly_terminated) {
            $ec_count--;
        }
        for (my $i=0; $i<$ec_count; $i++) {
            $ec_num_arr[$i] = $incorrect_ec_num_arr[$i];
            print STDERR "KEGGReferenceGeneToReferencePeptideSequence.extract_ec_numbers: ec_num_arr[$i]=" . $ec_num_arr[$i] . "\n";
        }
    }

    return @ec_num_arr;
}

# Add various EC-number related instances to the list of "enzymes".
sub add_enzymes {
    my ($self, $enzymes, $ec_num_arr) = @_;

    for (my $j = 0; $j < scalar(@{$ec_num_arr}); $j++) {
        my $ec_num = $ec_num_arr->[$j];
        if (scalar($ec_num) =~ /ARRAY/) {
            print STDERR "KEGGReferenceGeneToReferencePeptideSequence.add_enzymes: scalar(ec_num)=" . scalar($ec_num) . "\n";
            next;
        }
        my $database_identifier = $self->builder_params->database_identifier->get_ec_database_identifier($ec_num);
        push(@{$enzymes}, $database_identifier);
        # Assume that an EC number with hyphens in
        # actually represents a class of EC numbers,
        # rather than a specific enzyme.  IntEnz doesn't
        # need to have any hyphens.
        if ($ec_num =~ /-/) {
            $ec_num =~ s/-//;
            $ec_num =~ s/\.*$//;
        } else {
            # BRENDA doesn't know about enzyme classes
            push(@{$enzymes}, $self->builder_params->database_identifier->get_brenda_database_identifier($ec_num));
        }
        push(@{$enzymes}, $self->builder_params->database_identifier->get_intenz_database_identifier($ec_num));
    }
}

sub bget {
    my ($self, $query) = @_;

    my $url = "http://rest.kegg.jp/get/$query";
    my $content = $self->fetch_content_from_url($url);

    return $content;
}

sub bconv {
    my ($self, $query) = @_;

    my $url = "http://rest.kegg.jp/conv/genes/$query";
    my $content = $self->fetch_content_from_url($url);

    return $content;
}

# Given a URL as an argument, fetch the HTML
sub fetch_content_from_url {
    my ($self, $url) = @_;

    my $content = undef;
    if (defined $url) {
        my $ua = LWP::UserAgent->new();
    
        my $response = $ua->get($url);
        if(defined $response) {
            if ($response->is_success) {
                print STDERR "KEGGReferenceGeneToReferencePeptideSequence.fetch_content_from_url: Ah-ha, we have SUCCESS!!!\n";

                $content = $response->content;
            } else {
                print STDERR "HTMLReader.fetch_content_from_url: GET request failed for url=$url\n";
            }
        } else {
            print STDERR "HTMLReader.fetch_content_from_url: no response!\n";
        }
    } else {
        print STDERR "HTMLReader.fetch_content_from_url: you need to supply a URL!\n";
    }
    
    return $content;
}

1;

