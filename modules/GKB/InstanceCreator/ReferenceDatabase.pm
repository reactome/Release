
=head1 NAME

GKB::InstanceCreator::ReferenceDatabase

=head1 SYNOPSIS

Methods that create various ReferenceDatabase instances.

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

package GKB::InstanceCreator::ReferenceDatabase;

use GKB::Config_Orthomcl;    # source for species 3 letter code mapping
use GKB::DBAdaptor;
use GKB::InstanceCreator::Miscellaneous;
use GKB::SOAPServer::MIRIAM;
use strict;
use vars qw(@ISA $AUTOLOAD %ok_field);
use Bio::Root::Root;

@ISA = qw(Bio::Root::Root);

# Contains information about  external databases.  This get used if
# MIRIAM doesn't know the database.
#
# Several pseusdo-parameters are used in the URLs:
#
# ###ID###	This will be mapped elsewhere onto an ID for the corresponding database
# ###SP###	This will be mapped elsewhere onto a species code
# ###SP3###	This will be mapped elsewhere onto a three-letter species code
my %reference_database_info_hash = (
    'BioGPS Gene' => {
        'Name'      => ['BioGPS Gene'],
        'URL'       => "http://biogps.org/",
        'AccessUrl' => "http://biogps.org/gene/###ID###"
    },
    'Brenda' => {
        'Name' => ['Brenda'],
        'URL'  => "http://www.brenda-enzymes.info/index.php4",
        'AccessUrl' => "http://www.brenda-enzymes.info/php/result_flat.php4?ecno=###ID###"
    },
    'CTD Gene' => {
        'Name' => [ 'CTD Gene', 'Comparative Toxicogenomics Database Gene' ],
        'URL' => 'http://ctd.mdibl.org/',
        'AccessUrl' => 'http://ctd.mdibl.org/detail.go?type=gene&db=GENE&acc=###ID###'
    },
    'dbSNP Gene' => {
        'Name' => [ 'dbSNP Gene', 'NCBI dbSNP' ],
        'URL'  => 'http://www.ncbi.nlm.nih.gov/projects/SNP/',
        'AccessUrl' => 'http://www.ncbi.nlm.nih.gov/SNP/snp_ref.cgi?locusId=###ID###'
    },
    'DOCK Blaster' => {
        'Name' => ['DOCK Blaster'],
        'URL'  => 'http://blaster.docking.org',
        'AccessUrl' => 'http://blaster.docking.org/cgi-bin/parser.pl?code=###ID###'
    },
    'EC' => {
        'Name'      => [ 'EC', 'Enzyme' ],
        'URL'       => "http://www.expasy.org/enzyme/",
        'AccessUrl' => "http://enzyme.expasy.org/EC/###ID###"
    },
    'ENSEMBL' => {
        'Name' => [ 'ENSEMBL', 'Ensembl' ],
        'URL'  => 'http://www.ensembl.org',
        'AccessUrl' => 'http://www.ensembl.org/###SP###/geneview?gene=###ID###&db=core'
    },
    'Entrez Gene' => {
        'Name' => ['Entrez Gene'],
        'URL'  => 'http://www.ncbi.nlm.nih.gov/sites/entrez?db=gene',
        'AccessUrl' => 'http://www.ncbi.nlm.nih.gov/sites/entrez?Db=gene&Cmd=ShowDetailView&TermToSearch=###ID###'
    },
    'FlyBase' => {
        'Name'      => ['FlyBase'],
        'URL'       => 'http://flybase.org',
        'AccessUrl' => 'http://flybase.org/.bin/fbidq.html?###ID###'
    },
    'GeneCards' => {
        'Name' => ['GeneCards'],
        'URL'  => 'http://www.genecards.org',
        'AccessUrl' => 'http://www.genecards.org/cgi-bin/carddisp.pl?id=###ID###&id_type="uniprot"'
    },
    'Gene Ontology' => {
        'Name' => [ 'Gene Ontology', 'GO' ],
        'URL'  => 'http://www.geneontology.org'
    },
    'HapMap' => {
        'Name' => ['HapMap'],
        'URL'  => 'http://www.hapmap.org',
        'AccessUrl' => 'http://hapmap.ncbi.nlm.nih.gov/cgi-perl/gbrowse/hapmap3r2_B36/?name=###ID###'
    },
    'IntAct' => {
        'Name' => ['IntAct'],
        'URL'  => "http://www.ebi.ac.uk/intact",
        'AccessUrl' => "http://www.ebi.ac.uk/intact/search/do/search?searchString=###ID###&filter=ac"
    },
    'IntEnz' => {
        'Name' => ['IntEnz'],
        'URL'  => "http://www.ebi.ac.uk/intenz/",
        'AccessUrl' => "http://www.ebi.ac.uk/intenz/query?cmd=SearchEC&ec=###ID###"
    },
    'KEGG Gene' => {
        'Name' => [ 'KEGG', 'KEGG Gene' ],
        'URL'  => "http://www.genome.jp/",
        'AccessUrl' => "http://www.genome.jp/dbget-bin/www_bget?###SP3###+###ID###"
    },
    'OMIM' => {
        'Name' => [ 'OMIM', 'MIM' ],
        'URL'       => 'http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=OMIM',
        'AccessUrl' => 'http://www.ncbi.nlm.nih.gov/omim/###ID###'
    },
    'Orphanet' => {
        'Name' => ['Orphanet'],
        'URL'  => 'http://www.orpha.net',
        'AccessUrl' => 'http://www.orpha.net/consor/cgi-bin/Disease_Genes.php?data_id=###ID###'
    },
    'PDB' => {
        'Name'      => [ 'PDB', 'Protein Data Bank' ],
        'URL'       => 'http://www.pdb.org',
        'AccessUrl' => 'http://pdbe.org/###ID###'
    },
    'PRO' => {
        'Name' => [ 'PRO', 'Protein Ontology' ],
        'URL'  => 'http://pir.georgetown.edu/pro',
        'AccessUrl' => 'http://purl.obolibrary.org/obo/PR_###ID###'
    },
    'RefSeq' => {
        'Name' => ['RefSeq'],
        'URL'  => 'http://www.ncbi.nlm.nih.gov/RefSeq/',
        'AccessUrl' => 'http://www.ncbi.nlm.nih.gov/entrez/viewer.fcgi?val=###ID###'
    },
    'Rhea' => {
        'Name'      => ['Rhea'],
        'URL'       => 'http://www.ebi.ac.uk/rhea//',
        'AccessUrl' => 'http://www.ebi.ac.uk/rhea/reaction.xhtml?id=###ID###'
    },
    'UniProt' => {
        'Name'      => [ 'UniProt', 'SwissProt' ],
        'URL'       => 'http://www.uniprot.org',
        'AccessUrl' => 'http://www.uniprot.org/entry/###ID###'
    },
    'UCSC human' => {
        'Name' => [ 'UCSC human', 'UCSC' ],
        'URL'  => 'http://genome.ucsc.edu/',
        'AccessUrl' => 'http://genome.ucsc.edu/cgi-bin/hgTracks?position=###ID###'
    },
    'Wormbase' => {
        'Name' => ['Wormbase'],
        'URL'  => 'http://www.wormbase.org/',
        'AccessUrl' => 'http://www.wormbase.org/db/seq/sequence?name=###ID###;class=Gene_name'
    },
    'ZINC_target' => {
        'Name' => ['ZINC_target'],
        'URL'  => 'http://zinc.docking.org/',
        'AccessUrl' => ' http://zinc.docking.org/targets/###ID###'
    },
    'ZINC' => {
        'Name' => ['ZINC'],
        'URL'  => 'http://zinc.docking.org/',
        'AccessUrl' => 'http://zinc.docking.org/substance/###ID###'
    },
    'EFO' => {
        'Name' => ['EFO'],
        'URL'  => 'http://www.ebi.ac.uk/efo/',
        'AccessUrl' => 'https://www.google.com/#q=###ID###'
    },
);
$reference_database_info_hash{'RefSeqPeptide'} = $reference_database_info_hash{'RefSeq'};
$reference_database_info_hash{'RefSeqDNA'} = $reference_database_info_hash{'RefSeq'};

my %species_to_3_letter_code_map = ();    # created in the constructor

# MIRIAM gets some things wrong, this hash is here to force the code to use
# Reactome's own definitions where known MIRIAM problems exist.
my %dont_use_muiriam_hash = ( 'IntAct' => 1, );

# List the object variables here, so that they can be checked
for my $attr (
    qw(
    dba
    miscellaneous
    reference_database_cache
    soap_server
    species_to_3_letter_code_map
    )
  )
{
    $ok_field{$attr}++;
}

sub AUTOLOAD {
    my $self = shift;
    my $attr = $AUTOLOAD;
    $attr =~ s/.*:://;
    return unless $attr =~ /[^A-Z]/;    # skip DESTROY and all-cap methods
    $self->throw("invalid attribute method: ->$attr()") unless $ok_field{$attr};
    $self->{$attr} = shift if @_;
    return $self->{$attr};
}

sub new {
    my ( $pkg, $dba ) = @_;

    my $self = bless {}, $pkg;
    $dba && $self->set_dba($dba);

    my %reference_database_cache = ();
    $self->miscellaneous( GKB::InstanceCreator::Miscellaneous->new() );
    $self->reference_database_cache( \%reference_database_cache );
    $self->soap_server( GKB::SOAPServer::MIRIAM->new() );

    $self->create_species_to_3_letter_code_map();

    return $self;
}

# Reverse the mapping in %GKB::Config_Orthomcl::orthomcl, to
# allow a user to come in with a species name and pull out a
# three-letter species name.
sub create_species_to_3_letter_code_map {
    my ($self) = @_;

    # initialize
    my %map = ();
    $self->species_to_3_letter_code_map( \%map );

    my $species_array;
    foreach my $species3 ( keys(%orthomcl) ) {
        my $species_array = $orthomcl{$species3}{'name'};
        if ( !( defined $species_array ) || scalar( @{$species_array} < 1 ) ) {
            next;
        }
        my $species = $species_array->[0];
        if ( defined $species && !( $species eq '' ) ) {
            $species =~ s/ +/_/g;
            $self->species_to_3_letter_code_map->{$species} = $species3;
        }
    }
}

# Set the DBAdaptor
sub set_dba {
    my ( $self, $dba ) = @_;

    $self->dba($dba);
    $self->miscellaneous->set_dba($dba);
}

# Set the InstanceCreator::Miscellaneous object.
sub set_miscellaneous {
    my ( $self, $miscellaneous ) = @_;

    $self->miscellaneous($miscellaneous);
}

# If the named database has different URLs, depending on species, return 1,
# otherwise return 0.
sub is_multiple_species_database {
    my ( $self, $name ) = @_;

    my $access_url = $reference_database_info_hash{$name}->{'AccessUrl'};
    if ( defined $access_url && $access_url =~ /###SP3{0,1}###/ ) {
        return 1;
    }

    return 0;
}

# Creates a reference database instance.
# Arguments:
#
# name - name of database/resource
# url - home page
# access_url - allows user to get specific information from database
#
# Returns a ReferenceDatabase instance.
sub get_reference_database {
    my ( $self, $name, $species ) = @_;

    print STDERR "ReferenceDatabase.get_reference_database: name=$name\n";

    my $dba = $self->dba;

    my $reference_database = undef;
    if ( defined $species && $self->is_multiple_species_database($name) ) {
        print STDERR
          "ReferenceDatabase.get_reference_database: species=$species\n";

        $species =~ s/ /_/g;

        if ( !( $name =~ /$species/ ) ) {

           # See if there is a already species-specific version of this database
            my $species_specific_name = "$name\_$species";
            print STDERR
"ReferenceDatabase.get_reference_database: searching for preexisting ReferenceDatabase for $species_specific_name\n";
            $reference_database = $self->get_predefined_reference_database($species_specific_name);
            if ( defined $reference_database ) {
                print STDERR
"ReferenceDatabase.get_reference_database: found preexisting ReferenceDatabase for $species_specific_name\n";
                $self->rearrange_reference_database_names( $reference_database,
                    $name );
                return $reference_database;
            }
            print STDERR
"ReferenceDatabase.get_reference_database: searching for preexisting ReferenceDatabase for $species_specific_name\_GENE\n";
            $reference_database = $self->get_predefined_reference_database(
                "$species_specific_name\_GENE");
            if ( defined $reference_database ) {
                print STDERR
"ReferenceDatabase.get_reference_database: found preexisting ReferenceDatabase for $species_specific_name\_GENE\n";
                $self->rearrange_reference_database_names( $reference_database,
                    $name );
                return $reference_database;
            }
        }
    }
    else {

        print STDERR
"ReferenceDatabase.get_reference_database: no species-specific database found, look for regular database\n";

        $reference_database = $self->get_predefined_reference_database($name);
        if ( defined $reference_database ) {
            $self->rearrange_reference_database_names( $reference_database,
                $name );
            return $reference_database;
        }
    }

    print STDERR
      "ReferenceDatabase.get_reference_database: create brand new database\n";

    # Create a new ReferenceDatabase class entry
    # if one does not already exist
    $reference_database = GKB::Instance->new(
        -ONTOLOGY => $dba->ontology,
        -CLASS    => 'ReferenceDatabase'
    );

    $reference_database->inflated(1);
    $reference_database->created(
        $self->miscellaneous->get_instance_edit_for_effective_user() );
    my @names = $self->get_names( $name, $species );
    if ( scalar(@names) > 0 ) {
        $reference_database->Name(@names);
    }
    my $url = $self->get_url( $name, $species );
    if ( defined $url ) {
        $reference_database->Url($url);
    }
    my $access_url = $self->get_access_url( $name, $species );
    if ( defined $access_url ) {
        $reference_database->AccessUrl($access_url);
    }
    $dba->store($reference_database);
    $self->rearrange_reference_database_names( $reference_database, $name );
    $self->reference_database_cache->{$name} = $reference_database;

    return $reference_database;
}

# There may already be a copy of the named reference database, either in cache or in
# the Reactome database.  If so, return the cached value, otherwise return undef.
sub get_predefined_reference_database {
    my ( $self, $name ) = @_;

    # Workaround to avoid creating multiple RefSeq databases for DNA, Peptide.
    my $local_name = $name;
    if ( $local_name =~ /^RefSeq/ ) {
        $local_name = 'RefSeq';
    }

    # Is the ReferenceDatabase instance already in cache?
    # Can speed things up a lot if this subroutine is called
    # frequently.
    if ( defined $self->reference_database_cache->{$local_name} ) {
        return $self->reference_database_cache->{$local_name};
    }

    my $dba = $self->dba;

    # Look to see if there is a ReferenceDatabase class entry
    # with the given name already in the database
    my $reference_databases = $dba->fetch_instance_by_attribute( 'ReferenceDatabase',
        [ [ 'name', [$local_name] ] ] );

    my $reference_database = undef;
    if ( defined $reference_databases && scalar( @{$reference_databases} ) > 0 )
    {
        $reference_database = $reference_databases->[0];
        $reference_database->inflate();
    }

    $self->reference_database_cache->{$local_name} = $reference_database;

    return $reference_database;
}

# For the given ReferenceDatabase, if it's access URL requires that the species name
# be explicitly specified, make sure that the supplied name is the first
# name in the list of names, both locally and in the database. This is needed by
# services like BioMart, which like to have simple snappy database names, not names
# that also contain species.
sub rearrange_reference_database_names {
    my ( $self, $reference_database, $name ) = @_;

    if ( !( $self->is_multiple_species_database($name) ) ) {
        return;
    }

    # To avoid breaking other things, particularly BioMart,
    # the first name should be $name.  So, if necessary,
    # re-order the names and write them back to the database.
    my $names     = $reference_database->name;
    my @new_names = ($name);
    foreach my $local_name ( @{$names} ) {
        if ( !( $local_name eq $name ) ) {
            push( @new_names, $local_name );
        }
    }
    $reference_database->name(undef);
    $reference_database->name(@new_names);
    $self->dba->update_attribute( $reference_database, 'name' );
    $reference_database->_displayName(undef);
    $reference_database->_displayName($name);
    $self->dba->update_attribute( $reference_database, '_displayName' );
}

# Given the name of the service you are interested in, return the home
# page URL as a string.  Returns undef if no corresponding URL can
# be found.
#
# The species, if used, should be the name, using underlines as separators.
# Defaults to 'Homo_sapiens' (human) if not given.  In most
# cases, this is not significant, but ENSEMBL likes to have it.
sub get_url {
    my ( $self, $name, $species ) = @_;

    if ( !( defined $species ) || $species eq '' ) {
        $species = 'Homo_sapiens';
    }

    my $url = undef;

    # See if MIRIAM knows about the service, and if so, use the
    # information that MIRIAM provides
    if ( !( $dont_use_muiriam_hash{$name} ) ) {
        my $data_resources = $self->soap_server->getDataResources($name);
        if (   defined $data_resources
            && scalar($data_resources) =~ /ARRAY/
            && scalar( @{$data_resources} ) > 0 )
        {
            $url = $data_resources->[0];
        }
    }

    if ( !( defined $url ) ) {

        # Use hard-coded information if MIRIAM knows nothing about the service.
        $url = $reference_database_info_hash{$name}->{'URL'};
        if ( !( defined $url ) ) {
            my $resource_name = $name;
            $resource_name =~ s/_.*$//;
            $url = $reference_database_info_hash{$resource_name}->{'URL'};
        }
    }

    if ( defined $url ) {
        $url =~ s/###SP###/$species/g;
        if ( $url =~ /###SP3###/ ) {
            my $species3 = $self->generate_3_letter_species_code($species);
            if ( defined $species3 ) {
                $url =~ s/###SP3###/$species3/g;
            }
            else {
                print STDERR
"ReferenceDatabase.get_url: WARNING - species3 is undef for $species!!\n";
            }
        }
    }
    else {
        print STDERR
"ReferenceDatabase.get_url: WARNING - Could not find URL for service $name\n";
    }

    return $url;
}

# Given the name of the service you are interested in, return the access
# URL as a string.  Returns undef if no corresponding URL can
# be found.  This will contain the substring '###ID###'.  Replacing
# this substring with a valid ID for the given service will result in
# a URL that can be used to query the service.
#
# The species, if used, should be a three
# letter abbreviation.  Defaults to 'Homo_sapiens' (human) if not given.  In most
# cases, this is not significant, but ENSEMBL likes to have it.
sub get_access_url {
    my ( $self, $name, $species ) = @_;

    if ( !( defined $species ) || $species eq '' ) {
        $species = 'Homo_sapiens';
    }

    my $access_url = undef;

# Use hard-coded information by preference, since it seems to be more reliable than MIRIAM.
    $access_url = $reference_database_info_hash{$name}->{'AccessUrl'};
    if ( !( defined $access_url ) ) {
        my $resource_name = $name;
        $resource_name =~ s/_.*$//;
        $access_url = $reference_database_info_hash{$resource_name}->{'AccessUrl'};
    }

    if ( !( defined $access_url ) ) {

        # See if MIRIAM knows about the service, and if so, use the
        # information that MIRIAM provides
        if ( !( $dont_use_muiriam_hash{$name} ) ) {
            my $locations = $self->soap_server->getLocations( $name, "###ID###" );
            if (   defined $locations
                && scalar($locations) =~ /ARRAY/
                && scalar( @{$locations} ) > 0 )
            {
                $access_url = $locations->[0];
            }
        }
    }

    if ( defined $access_url ) {
        $access_url =~ s/###SP###/$species/g;
        if ( $access_url =~ /###SP3###/ ) {
            my $species3 = $self->generate_3_letter_species_code($species);
            if ( defined $species3 ) {
                $access_url =~ s/###SP3###/$species3/g;
            }
            else {
                print STDERR
"ReferenceDatabase.get_access_url: WARNING - species3 is undef for $species!!\n";
            }
        }
    }
    else {
        print STDERR
"ReferenceDatabase.get_access_url: WARNING - Could not find access URL for service $name\n";
    }

    return $access_url;
}

# Given a species name like Homo_sapiens, generate the corresponding 3 letter code,
# e.g. hsa.  Retursns undef if problems are encountered.
sub generate_3_letter_species_code {
    my ( $self, $species ) = @_;

    if ( !( defined $species ) ) {
        return undef;
    }

    # Use Esther's 3 letter code, if available (this is only available for
    # some species).
    my $code = $self->species_to_3_letter_code_map->{$species};
    if ( defined $code && !( $code eq "" ) ) {
        return $code;
    }

    my @species_parts = split( /[ _]/, $species );

    $code = "";
    if ( scalar(@species_parts) > 0 ) {
        if ( !( $species_parts[0] eq "" ) ) {
            $species_parts[0] =~ /^(.)/;
            my $first_letter = $1;
            $code = lc($first_letter);
        }

        if ( scalar(@species_parts) > 1 ) {
            if ( !( $species_parts[1] eq "" ) ) {
                $species_parts[1] =~ /^(..)/;
                my $subsequent_letters = $1;
                $code .= lc($subsequent_letters);
            }
        }
    }

    if ( $code eq "" ) {
        $code = undef;
    }

    return $code;
}

# Given the name of the service you are interested in, return an array
# of all alternative names.  Returns an array containing just the supplied
# name if nothing could be found.
sub get_names {
    my ( $self, $name, $species ) = @_;

    print STDERR "ReferenceDatabase.get_names: name=$name\n";

    my @names = ();

    # See if MIRIAM knows about the service, and if so, use the
    # information that MIRIAM provides
    if ( !( $dont_use_muiriam_hash{$name} ) ) {
        my $data_type_synonyms = $self->soap_server->getDataTypeSynonyms($name);
        if (   defined $data_type_synonyms
            && scalar($data_type_synonyms) =~ /ARRAY/
            && scalar( @{$data_type_synonyms} ) > 0 )
        {
            foreach my $local_name ( @{$data_type_synonyms} ) {
                push( @names, $local_name );
            }
        }
        print STDERR "ReferenceDatabase.get_names: MIRIAM names=@names\n";
    }

    print STDERR "ReferenceDatabase.get_names: Use hard-coded information\n";

    # Use hard-coded information if MIRIAM knows nothing about the service.
    my $hard_coded_names = $reference_database_info_hash{$name}->{'Name'};
    if ( !( defined $hard_coded_names ) ) {
        my $resource_name = $name;
        $resource_name =~ s/_.*$//;
        $hard_coded_names = $reference_database_info_hash{$resource_name}->{'Name'};
    }
    if ( defined $hard_coded_names ) {
        my $name_already_known;
        foreach my $local_name ( @{$hard_coded_names} ) {
            $name_already_known = 0;
            foreach my $existing_name (@names) {
                if ( $local_name eq $existing_name ) {
                    $name_already_known = 1;
                    last;
                }
            }
            if ( !$name_already_known ) {
                push( @names, $local_name );
            }
        }
    }

    if ( scalar(@names) == 0 ) {
        print STDERR
"ReferenceDatabase.get_names: Could not find alternative names for service $name\n";
        @names = ();
    }

    print STDERR "ReferenceDatabase.get_names: 1 names=@names\n";

    if (   $self->is_multiple_species_database($name)
        && defined $species
        && !( $name =~ /$species/ ) )
    {
        # Create and add a species-specific name
        my $species_specific_name = "$name\_$species";
        if ( $name eq "ENSEMBL" ) {
            $species_specific_name = "$species_specific_name\_GENE";
        }

        print STDERR
"ReferenceDatabase.get_names: species_specific_name=$species_specific_name\n";

        $self->prepend_new_name_if_necessary( \@names, $species_specific_name );
    }

    print STDERR "ReferenceDatabase.get_names: 2 names=@names\n";

    if ( scalar(@names) < 1 ) {
        @names = ($name);
    }

    print STDERR "ReferenceDatabase.get_names: 3 names=@names\n";

    return @names;
}

# Add the name supplied to the beginning of the list of names, if it isn't already in it.
sub prepend_new_name_if_necessary {
    my ( $self, $names, $name ) = @_;

    my $name_exists_flag = 0;
    foreach my $local_name ( @{$names} ) {
        if ( $local_name eq $name ) {
            $name_exists_flag = 1;
            last;
        }
    }
    if ( !$name_exists_flag ) {
        unshift( @{$names}, $name );
    }
}

# Returns a ReferenceDatabase instance for BioGPS Gene.
sub get_biogps_gene_reference_database {
    my ($self) = @_;

    return $self->get_reference_database('BioGPS Gene');
}

# Returns a ReferenceDatabase instance for Brenda.
sub get_brenda_reference_database {
    my ($self) = @_;

    return $self->get_reference_database('Brenda');
}

# Returns a ReferenceDatabase instance for Comparative Toxicogenomics Database Gene.
sub get_ctd_gene_reference_database {
    my ($self) = @_;

    return $self->get_reference_database('CTD Gene');
}

# Returns a ReferenceDatabase instance for NCBI dbSNP Gene.
sub get_dbsnp_gene_reference_database {
    my ($self) = @_;

    return $self->get_reference_database('dbSNP Gene');
}

# Returns a ReferenceDatabase instance for NCBI dbSNP Gene.
sub get_dockblaster_reference_database {
    my ($self) = @_;

    return $self->get_reference_database('DOCK Blaster');
}

# Returns a ReferenceDatabase instance for EC enzymes.
sub get_ec_reference_database {
    my ($self) = @_;

    return $self->get_reference_database('EC');
}

# Returns a ReferenceDatabase instance for ENSEMBL gene.
# Arguments:
#
# species - name for a species, Default: Homo_sapiens (human)
sub get_ensembl_reference_database {
    my ( $self, $species ) = @_;

    if ( !( defined $species ) || $species eq '' ) {
        $species = 'Homo_sapiens';
    }

    print STDERR
      "ReferenceDatabase.get_ensembl_reference_database: species=$species\n";

    my $reference_database = $self->get_reference_database( "ENSEMBL", $species );

    return $reference_database;
}

## Returns a ReferenceDatabase instance for ENSEMBL gene.
## Arguments:
##
## species - name for a species, Default: Homo_sapiens (human)
#sub get_ensembl_reference_database {
#    my ($self, $species) = @_;
#
#    if (!(defined $species) || $species eq '') {
#    	$species = 'Homo_sapiens';
#    }
#    $species =~ s/ /_/g;
#
#	my $reference_database = $self->get_reference_database("ENSEMBL_$species\_GENE", $species);
#
#    # To avoid breaking other things, particularly BioMart,
#    # the first name should be ENSEMBL.  So, if necessary,
#    # re-order the names and write them back to the database.
#	my $names = $reference_database->name;
#	my @new_names = ('ENSEMBL');
#	foreach my $name (@{$names}) {
#		if (!($name eq 'ENSEMBL')) {
#			push(@new_names, $name);
#		}
#	}
#	$reference_database->name(undef);
#	$reference_database->name(@new_names);
#	$self->dba->update_attribute($reference_database, 'name');
#	$reference_database->_displayName(undef);
#	$reference_database->_displayName('ENSEMBL');
#	$self->dba->update_attribute($reference_database, '_displayName');
#
#	return $reference_database;
#}

# Returns a ReferenceDatabase instance for Entrez Gene.
sub get_entrez_gene_reference_database {
    my ($self) = @_;

    return $self->get_reference_database('Entrez Gene');
}

# Returns a ReferenceDatabase instance for FlyBase.
sub get_flybase_reference_database {
    my ($self) = @_;

    return $self->get_reference_database('FlyBase');
}

sub get_genecards_reference_database {
    my ($self) = @_;

    return $self->get_reference_database('GeneCards');
}

# Returns a ReferenceDatabase instance for the Gene Ontology.
sub get_go_reference_database {
    my ($self) = @_;

    return $self->get_reference_database('Gene Ontology');
}

# Returns a ReferenceDatabase instance for HapMap.
sub get_hapmap_reference_database {
    my ($self) = @_;

    return $self->get_reference_database('HapMap');
}

# Returns a ReferenceDatabase instance for IntAct.
sub get_intact_reference_database {
    my ($self) = @_;

    return $self->get_reference_database('IntAct');
}

# Returns a ReferenceDatabase instance for IntEnz.
sub get_intenz_reference_database {
    my ($self) = @_;

    return $self->get_reference_database('IntEnz');
}

# Returns a ReferenceDatabase instance for KEGG genes.
#
# species - name for a species, Default: Homo_sapiens (human)
sub get_kegg_reference_database {
    my ( $self, $species ) = @_;

    return $self->get_reference_database( 'KEGG Gene', $species );
}

# Returns a ReferenceDatabase instance for Omim.
sub get_omim_reference_database {
    my ($self) = @_;

    return $self->get_reference_database('OMIM');
}

# Returns a ReferenceDatabase instance for Orphanet.
sub get_orphanet_reference_database {
    my ($self) = @_;

    return $self->get_reference_database('Orphanet');
}

# Returns a ReferenceDatabase instance for PDB.
sub get_pdb_reference_database {
    my ($self) = @_;

    return $self->get_reference_database('PDB');
}

# Returns a ReferenceDatabase instance for ZINC substance
sub get_zinc_reference_database {
    my ($self) = @_;

    return $self->get_reference_database('ZINC');
}

# Returns a ReferenceDatabase instance for ZINC target
sub get_zinc_target_reference_database {
    my ($self) = @_;

    return $self->get_reference_database('ZINC_target');
}

# Returns a ReferenceDatabase instance for EFO.
sub get_efo_reference_database {
    my ($self) = @_;

    return $self->get_reference_database('ZINC_target');
}

# Returns a ReferenceDatabase instance for PRO.
sub get_pro_reference_database {
    my ($self) = @_;

    return $self->get_reference_database('PRO');
}

# Returns a ReferenceDatabase instance for RefSeq.
#
# Returns a ReferenceDatabase instance.
sub get_refseq_reference_database {
    my ($self) = @_;

    return $self->get_reference_database('RefSeq');
}

# Returns a ReferenceDatabase instance for Rhea.
#
# Returns a ReferenceDatabase instance.
sub get_rhea_reference_database {
    my ($self) = @_;

    return $self->get_reference_database('Rhea');
}

# Returns a ReferenceDatabase instance for UniProt.
sub get_uniprot_reference_database {
    my ($self) = @_;

    return $self->get_reference_database('UniProt');
}

# Returns a ReferenceDatabase instance for UCSC.
sub get_ucsc_reference_database {
    my ($self) = @_;

    return $self->get_reference_database('UCSC human');
}

# Returns a ReferenceDatabase instance for Wormbase.
sub get_wormbase_reference_database {
    my ($self) = @_;

    return $self->get_reference_database('Wormbase');
}

1;

