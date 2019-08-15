#!/usr/local/bin/perl

# This script prepares orthopair files based on Ensembl compara
# Most species needed for Reactome orthology inference mapping are contained within Pan Compara, which provides a cross-section through a wide range of species. However a few species we want to include in the Reactome mapping are not part of Pan Compara, but are vertebrates - they can be obtained via the Core Ensembl Compara. This necessitates some switching between different commpara databases.
# Species information is collected in the Config_Species module. Species keys are four letter abbreviations, e.g. hsap for Homo sapiens.

#Procedure:
# Gene/protein mappings are extracted via BioMart webservices (in a single query for speed reasons) a) for the source species (default = hsap; UniProt id to Ensembl gene id), and b) for all target species (Ensembl gene id to protein id, where a swissprot id is preferred over a trembl id over an Ensembl peptide id). 
# Homology information is obtained via the Compara API as this information is not currently available via BioMart webservices. As the API method takes much longer than the BioMart query (due to brie8 connecting to the Ensembl databases over a transatlantic connection...) this procedure could/should be reviewed once homology data are available via BioMart.

use strict;
use warnings;

BEGIN {
	my @a = split "/", $0;
	pop @a;
	push @a, ("..","..",".."); 
	my $path = join "/", @a;  	
	unshift (@INC, "$path/bioperl-1.0");
	unshift (@INC, "$path/GKB/modules");		
}

use Getopt::Long;
use autodie;

use lib '/usr/local/gkbdev/modules';
use GKB::Compara::Homology;
use GKB::Compara::Source;
use GKB::Compara::Utils;
use GKB::Compara::SpeciesMapping;

our($opt_release, $opt_ensembl, $opt_from, $opt_sp, $opt_core, $opt_debug, $opt_test);
# if $opt_sp is given, only this species will be run, otherwise all keys of %species_info

&GetOptions(
	"release=i",
	"ensembl=i",
	"from=s",
	"sp=s",
	"core",
	"debug",
	"test"
);

$opt_release || die "Need Reactome release number, e.g. -release 32\n";
$opt_ensembl || die "Need EnsEMBL release number, e.g. -ensembl 73\n";
$opt_from || ($opt_from = 'hsap');

my $utils = GKB::Compara::Utils->new();
$utils->create_directory($opt_release);
chdir $opt_release;

my $homology = GKB::Compara::Homology->new();
$homology->set_registry($opt_core, $opt_ensembl);


print "$0: start\n";

# The first mapping required is the mapping from the source species protein id to the source species Ensembl gene id - via mart webservices
my $source = GKB::Compara::Source->new();
my %source_mapping = $source->prepare_source_mapping_hash($opt_from);
print "$0: Source hash size=" . scalar(keys(%source_mapping)) . "\n";

# Create homology mappings for ensembl genes between source and target species and
# target mappings for target species ensembl genes to target species uniprot ids
my $mapping = GKB::Compara::SpeciesMapping->new();
$mapping->print_species_keys();
$mapping->do_mapping_for_target_species($opt_from, $opt_sp, $opt_core, $opt_test, \%source_mapping, $homology);

print "$0: end\n";

=head
#this method is not needed for the current procedure - it's here in case the Ensembl API will be considered in the future to obtain gene to protein mappings (Currently the API is not used as cross-atlantic connection speed is too \slow for these operations, getting all mapping data in one go via BioMart is much quicker.)
#just meant as a reminder how to get swissprot/trembl/ensp mappings for ensembl genes
sub get_protein_id {
    my ($gene) = @_;
    my @protein_ids;
 #check for Uniprot/SWISSPROT first                                                                                                                                                                                                    
    if ($gene->get_all_DBLinks("%SWISSPROT")->[0]) {
        foreach my $p (@{$gene->get_all_DBLinks("%SWISSPROT")}) {
            push @protein_ids, $p->primary_id;
        }
    } elsif ($gene->get_all_DBLinks("%SPTREMBL")->[0]) {
        foreach my $p (@{$gene->get_all_DBLinks("%SPTREMBL")}) {
            push @protein_ids, $p->primary_id;
        }
    } else {
#get Ensembl protein instead                                                                                                                                                                                                           
        foreach my $transcript (@{$gene->get_all_Transcripts()}) {
            if ( $transcript->translation() ) {
                push @protein_ids, $transcript->translation()->stable_id();
                #print( $transcript->translation()->stable_id(), "\n" );                                                                                                                                                               
            }
        }
    }
    return \@protein_ids;
}
=cut
