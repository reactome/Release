=head1 NAME

GKB::Compara::BioMart;

=head1 SYNOPSIS

=head1 DESCRIPTION

This is a class for .

=head1 SEE ALSO

Related Classes

=head1 AUTHOR

Joel Weiser E<lt>joel.weiser@oicr.on.caE<gt>

Copyright (c) 2012 European Bioinformatics Institute, Cold Spring
Harbor Laboratory, and Ontario Institute for Cancer Research.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

package GKB::Compara::BioMart;

use strict;

use Data::Dumper;
use GKB::Config;
use GKB::Config_Species;
use File::Basename;
use Log::Log4perl qw/get_logger/;
use LWP::UserAgent;

Log::Log4perl->init(dirname(__FILE__) . '/compara_log.conf');

use vars qw(@ISA $AUTOLOAD %ok_field);
use Bio::Root::Root;

@ISA = qw(Bio::Root::Root);

# List the object variables here, so that they can be checked
for my $attr
    (qw(
    
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

    return $self;
}

sub clear_variables {
    my ($self) = @_;
    
}

# Needed by subclasses to gain access to class variables defined in
# this class.
sub get_ok_field {
    return %ok_field;
}

# Prepares mart query and returns the mart result in one big string - 
# extracts ensembl gene ids, swissprot and trembl accessions, and ensembl 
# peptide ids for the given species
sub get_mapping_table_from_mart {
    my ($self, $species_key) = @_;

    my $logger = get_logger(__PACKAGE__);

    # Mart query - E.coli doesn't have the sptrembl attribute, which causes it to fall over when using the standard query -> prepare a separate query with ensembl gene id and swissprot accession only (all E.coli proteins have a swissprot accession!)
    my $xml;
    my $base_attribute_names = ["ensembl_gene_id", "uniprot_swissprot_accession"];
    my $gene_attribute_names = ["ensembl_gene_id", "ensembl_peptide_id"];
    my $attribute_names = ["ensembl_gene_id", "uniprot_swissprot_accession", "uniprot_sptrembl", "ensembl_peptide_id"];
    
    if ($species_key eq 'ecol') {
	$attribute_names = $base_attribute_names;
    }
    
    my $mart_url = $species_info{$species_key}->{'mart_url'};
    if (!(defined $mart_url) || $mart_url eq '') {
       $mart_url = "http://www.biomart.org/biomart/martservice";
    }
    
    my $mart_virtual_schema = $species_info{$species_key}->{'mart_virtual_schema'};
    if (!(defined $mart_virtual_schema) || $mart_virtual_schema eq '') {
       $mart_virtual_schema = "default";
    }
    
    my $mart_group = $species_info{$species_key}->{'mart_group'};
    my $out = $self->schemifier0($mart_url, $attribute_names, $mart_virtual_schema, $mart_group);

    if ($self->is_mart_output_erroneous($out)) {
        $logger->warn("problem while running BioMart query for $species_key, trying with base attributes only\n");
        
        $out = $self->schemifier0($mart_url, $base_attribute_names, $mart_virtual_schema, $mart_group);
    }

    if ($self->is_mart_output_erroneous($out)) {
        $logger->warn("problem while running BioMart query for $species_key, trying with gene attributes only\n");
        
        $out = $self->schemifier0($mart_url, $gene_attribute_names, $mart_virtual_schema, $mart_group);
        if (defined $out && !(is_mart_output_erroneous($out))) {
            # Insert two pseudo-columns into the output, so that it gets
            # parsed properly later
            $out =~ s/\t/\t\t\t/g;
        }
    }

    if ($self->is_mart_output_erroneous($out)) {
        $logger->error("problem while running BioMart query for $species_key, giving up\n");
    }

    if (defined $out) {
        my @outlines = split("\n" , $out);
        
        if (scalar(@outlines) > 0) {
            $logger->info("out=");
	    $logger->info($_) foreach @outlines;
        } else {
            $logger->error("out is empty for $species_key\n");
        }
    } else {
        $logger->error("out is undef for $species_key\n");
    }

    return $out;
}

sub schemifier0 {
    my ($self, $mart_url, $attribute_names, $mart_virtual_schema, $mart_group) = @_;

    my $logger = get_logger(__PACKAGE__);

    my $xml = $self->build_mart_query($attribute_names, $mart_virtual_schema, $mart_group);
    my $out = $self->run_mart_query($mart_url, $xml);

    if ($self->is_mart_output_erroneous($out)) {
        $logger->warn("problem while running BioMart query for $mart_group, trying with base attributes only\n");
        
        $xml = $self->build_mart_query($attribute_names, $mart_virtual_schema, $mart_group);
        $out = $self->run_mart_query($mart_url, $xml);
        
        if ($self->is_mart_output_erroneous($out)) {
            $out = $self->schemifier1($mart_url, $attribute_names, $mart_virtual_schema, $mart_group);
        
            if ($self->is_mart_output_erroneous($out)) {
                $logger->error("problem while running BioMart query for $mart_group, giving up\n");
            }
        }
    }

    return $out;
}


sub schemifier1 {
    my ($self, $mart_url, $attribute_names, $mart_virtual_schema, $mart_group) = @_;

    my $logger = get_logger(__PACKAGE__);
    
    my $out = undef;

    if ($mart_virtual_schema eq "default" || !($mart_virtual_schema =~ /_[0-9]+$/)) {
        $logger->error("mart_virtual_schema=$mart_virtual_schema does not end with an incrementable number\n");
        
        return $out;
    }

    $out = $self->schemifier2($mart_url, $attribute_names, $mart_virtual_schema, $mart_group);
    
    if ($self->is_mart_output_erroneous($out)) {
        $logger->warn("problem while running BioMart query for $mart_group, trying another virtual schema version\n");
        
        my $s_mart_virtual_schema = $mart_virtual_schema;
        $s_mart_virtual_schema =~ s/^([^_]+)_/$1s_/;
        $out = $self->schemifier2($mart_url, $attribute_names, $s_mart_virtual_schema, $mart_group);
        
        if ($self->is_mart_output_erroneous($out)) {
            $logger->error("problem while running BioMart query for $mart_group, giving up!\n");
        }
    }

    return $out;
}

sub schemifier2 {
    my ($self, $mart_url, $attribute_names, $mart_virtual_schema, $mart_group) = @_;

    my $logger = get_logger(__PACKAGE__);

    my $out = undef;

    $logger->info("trying different virtual schema versions\n");

    $mart_virtual_schema =~ /^(.+_)([0-9]+)$/;
    my $mart_virtual_schema_stem = $1;
    my $mart_virtual_schema_num = $2;
    
    for (my $i=0; $i<40; $i++) {
        $mart_virtual_schema_num++;
        
        my $augmented_mart_virtual_schema = $mart_virtual_schema_stem . $mart_virtual_schema_num;
        my $xml = $self->build_mart_query($attribute_names, $augmented_mart_virtual_schema, $mart_group);
        $out = $self->run_mart_query($mart_url, $xml);
        
        if (!($self->is_mart_output_erroneous($out))) {
            last;
        }
    }

    return $out;
}

sub run_mart_query {
    my ($self, $mart_url, $xml) = @_;
    
    my $logger = get_logger(__PACKAGE__);
    
    $logger->info("mart_url=$mart_url\n");
    $logger->info("xml=$xml\n");

    #parameters for mart webservice    
    my $ua = LWP::UserAgent->new;

    my $request = HTTP::Request->new("POST","$mart_url?",,HTTP::Headers->new(),'query='.$xml."\n");
    my $response;
    my $out = "";
    
    $ua->request($request,
                 sub{
                     my($data, $response) = @_;
                     if (defined $response && defined $data && $response->is_success) {
                         $out .= $data;
                     }
                     else {
                         if (defined $response) {
                             $logger->warn("Problems with the web server: ".$response->status_line . "\n");
                         } else {
                             $logger->warn("Problems with the web server\n");
                         }
                     }
                 },1000);
    return $out;
}

sub is_mart_output_erroneous {
    my ($self, $mart_output) = @_;

    my $logger = get_logger(__PACKAGE__);

    if (!(defined $mart_output)) {
        $logger->error("mart_output is undef!!\n");
        return 1;
    }

    if ($mart_output =~ /ERROR/) {
        return 1;
    }

    return 0;
}

sub build_mart_query {
    my ($self, $attributes, $mart_virtual_schema, $mart_group) = @_;

    my $xml = "";
    $xml .= "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
    $xml .= "<!DOCTYPE Query>\n";
    $xml .= "<Query  virtualSchemaName = \"" . $mart_virtual_schema . "\" formatter = \"TSV\" header = \"0\" uniqueRows = \"0\" count = \"\" datasetConfigVersion = \"0.6\" >\n";
    $xml .= "\t<Dataset name = \"" . $mart_group . "\" interface = \"default\" >\n";

    foreach my $attribute (@{$attributes}) {
        if (!($attribute eq "DUMMY")) {
            $xml .= "\t\t<Attribute name = \"$attribute\" />\n";
        }
    }

    $xml .= "\t</Dataset>\n";
    $xml .= "</Query>\n";

    return $xml;
}

1;
