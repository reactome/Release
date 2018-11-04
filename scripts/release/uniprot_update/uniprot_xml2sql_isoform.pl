#!/usr/local/bin/perl  -w
use strict;
use 5.010;

#This script updates the Uniprot ReferencePeptideSequences in Reactome. Uniprot entries (swissprot section) are imported/updated in Reactome if they are assigned to one of the species specified in @species.
#The script checks for existing entries and updates them.

use lib "/usr/local/gkbdev/modules";

use GKB::Instance;
use GKB::DBAdaptor;
use GKB::EnsEMBLUtils qw/on_EnsEMBL_primary_assembly/;
use GKB::Utils_esther;

use autodie;
use Data::Dumper;
use DateTime;
use Getopt::Long;
use IO::Uncompress::Gunzip qw/gunzip $GunzipError/;
use List::MoreUtils qw/any notall none/;
use Time::Piece;

our ( $opt_user, $opt_host, $opt_pass, $opt_port, $opt_db, $opt_, $opt_species );
(@ARGV)  || die "Usage: $0 -user db_user -host db_host -pass db_pass -port db_port -db db_name\n";
&GetOptions( "user:s", "host:s", "pass:s", "port:i", "db:s", "debug" );
$opt_db  || die "Need database name (-db).\n";

my $dba = GKB::DBAdaptor->new(
    -user => $opt_user || '',
    -host => $opt_host,
    -pass => $opt_pass,
    -port => $opt_port,
    -dbname => $opt_db
);


# FILE DOWNLOAD
my $update_dir = ".";

# Trembl file should have been downloaded manually before running this script
# File may be named as "uniprot-reviewed_no.list.gz" or "uniprot-reviewed:no.list.gz"
my @temp = split(/\n/, `ls -1tr $update_dir/uniprot-reviewed*`);	# look for file in the update directory
(my $trembl_file = pop @temp) =~ s/^$update_dir\///;				# use the latest in case there are multiple files with the same prefix
die "Can't find $update_dir/uniprot-reviewed_no.list.gz\n" unless ($trembl_file);

if ($trembl_file =~ /\.gz$/) {
    (my $unzipped_trembl_file = $trembl_file) =~ s/\.gz$//;
    gunzip "$update_dir/$trembl_file" => "$update_dir/$unzipped_trembl_file" or die "Can't gunzip $trembl_file: $GunzipError\n";
    $trembl_file = $unzipped_trembl_file;
}

# Download sprot file
@temp = split(/\n/, `ls -1tr $update_dir/uniprot_sprot.xml*`);	# look for file in the update directory
(my $sprot_file = pop @temp) =~ s/^$update_dir\///;				# use the latest in case there are multiple files with the same prefix
die "Can't find $update_dir/uniprot_sprot.xml.gz\n" unless ($sprot_file);

if ($sprot_file =~ /\.gz$/) {
    (my $unzipped_sprot_file = $sprot_file) =~ s/\.gz$//;
    gunzip "$update_dir/$sprot_file" => "$update_dir/$unzipped_sprot_file" or die "Can't gunzip $sprot_file: $GunzipError\n";
    $sprot_file = $unzipped_sprot_file;
}

# Prepare to update InstanceEdit
# To be replaced by name and initials of a user who runs the update
my $surname = 'Weiser';
my $initial = 'JD';
my $date = DateTime->now(time_zone => "local")->ymd();

my $db_inst;
unless (
    $db_inst =
    $dba->fetch_instance_by_attribute( 'ReferenceDatabase',
        [ [ 'name', ['UniProt'] ] ] )->[0]
  )
{
    $db_inst = GKB::Instance->new(
        -ONTOLOGY => $dba->ontology,
        -CLASS    => 'ReferenceDatabase',
        'name'    => [qw(UniProt)]
    );
    $db_inst->inflated(1);
}

my $instance_edit = GKB::Utils_esther::create_instance_edit( $dba, $surname, $initial, $date );

#list of species to be maintained within the Reactome repository
my %species = (
    9606,   "Homo sapiens",
    10090,  "Mus musculus",
    10116,  "Rattus norvegicus",
    9913,   "Bos taurus",
    9031,   "Gallus gallus",
    7227,   "Drosophila melanogaster",
    6239,   "Caenorhabditis elegans",
    4932,   "Saccharomyces cerevisiae",
    4896,   "Schizosaccharomyces pombe",
    11695,  "Human immunodeficiency virus type 1",
    11718,  "Human immunodeficiency virus type 2",
    132504, "Influenza A virus"
);
#counters:

my $total_db    = 0;    #number of instances in db
my $iso_xml     = 0;    #number of instances in the file (with correct taxonomy)
my $obsolete_nr = 0;    #number of obsolete instances with no EWAS
my $new_sp      = 0;    #number of new swissprot instances added
my $new_iso     = 0;    #number of new isoforms added
my $obs_iso     = 0;    #number of obsolete isoforms
my $total_xml   = 0;    #number of instances in xml file
my %obsolete_iso;       #report obsolete isoforms with referers
my %dup_db_id;          #duplicated db_id for ReferenceGeneProduct

my (%reactome_gp);      # hash of reactome geneProduct instances
my (%reactome_iso);     # hash of isoforms
my (%reactome_rds);     # hash of reactome Reference DNA Sequence instances
my (%sec_ac);           # hash of secondary accessions
my (%mis_parents);      # hash of parent with AC different from isoform
my @skip_list = get_skip_list();

#Fill the main and isoform hashes....

my $ar =
  $dba->fetch_instance_by_remote_attribute( 'ReferenceIsoform',
    [ [ 'referenceDatabase.name', '=', ['UniProt'] ] ] );

foreach my $iss ( @{$ar} ) {
    my $iac = $iss->VariantIdentifier->[0];
    next unless $iac;
    my $idd = $iss->db_id;
    $reactome_iso{$iac} = $idd;
}

$ar =
  $dba->fetch_instance_by_remote_attribute( 'ReferenceGeneProduct',
    [ [ 'referenceDatabase.name', '=', ['UniProt'] ] ] );

foreach my $rps ( @{$ar} ) {
    my $acc   = $rps->Identifier->[0];
    next unless $acc;
    my $db_id = $rps->db_id;
    $reactome_gp{$acc} = $db_id;
    $total_db++;
}

print "Number of UniProt instances in Reactome: $total_db\t"
  . "Total number of UniProt accessions: "
  . keys(%reactome_gp) . "\n";

my $reference_dna_sequences =
    $dba->fetch_instance(-CLASS => 'ReferenceDNASequence');
    
foreach my $reference_dna_sequence (@{$reference_dna_sequences}) {
    my $identifier = $reference_dna_sequence->identifier->[0];
    next unless $identifier;
    $reactome_rds{$identifier} = $reference_dna_sequence->db_id;
}

open(my $uniprot_records_fh, '<', "$update_dir/$sprot_file");
open(my $sequence_report_fh, '>', "$update_dir/sequence_uniprot_report.txt");
open(my $ref_DNA_seq_report, '>', "$update_dir/reference_DNA_sequence_report.txt");
# Parse the xml file....

local $/ = "\<\/entry\>\n";

my $record_counter = 0;
while (<$uniprot_records_fh>) {
    if ($record_counter % 1000 == 0) {
        if ($record_counter != 0) {
            $dba->execute("COMMIT");
            print "$record_counter records processed and committed\n";
        }
        $dba->execute("START TRANSACTION");
    }
    $record_counter++;
    
    chomp;
    my @ac;
    my $dupl_flag = 0;

    while (/\<accession\>(\w+)\<\/accession\>/gm) {
        push @ac, $1;
    }

    my $ac = shift @ac;
    $sec_ac{$ac} = join( '|', @ac );

    #Taxonomy check

    #my ($ox) = /\<dbReference type=\"NCBI Taxonomy\" key=\"\d+\" id=\"(\d+)\"/m;
    my ($oname) = /\<name type\=\"scientific\"\>(.*)\<\/name\>/m;
    my $taxon = "";
    my %species_cache;
    my $species_instance;
    foreach my $tax_set ( sort keys %species ) {
        if ( $oname =~ /$species{$tax_set}/ ) {
            $taxon = $species{$tax_set};
            $species_instance = species_instance( $taxon, \%species_cache );
        }
    }

    next if ( length($taxon) < 2 && not defined $reactome_gp{$ac} );

    $total_xml++;

    #Parsing the instance attributes: accession, gene names, keywords, comments, isoforms....

    my ($id) = $_ =~ /\<name\>([A-Za-z0-9\_]*)\<\/name\>/ms;
    unshift @ac, $id;
    my ($desc) = /\<protein(.*)\<\/protein\>/ms;

    my $rec_name = "No name";
    if (   $desc =~ /<recommendedName>\n\s+\<fullName\>(.*)<\/fullName\>/m
        || $desc =~
        /<recommendedName ref=\"\d+\"\>\n\s+\<fullName\>(.*)<\/fullName\>/m )
    {
        $rec_name = $1;
    }

    $desc =~ s/\<\/fullName\>//g;
    $desc =~ s/\<fullName\>//g;
    $desc =~ s/\<\/recommendedName\>//g;
    $desc =~ s/\<recommendedName\>/recommendedName\:/g;
    $desc =~ s/\<\/alternativeName\>//g;
    $desc =~ s/\<\/shortName\>//g;
    $desc =~ s/\<\/fullName\>//g;
    $desc =~ s/\<alternativeName\>/alternativeName\:/g;
    $desc =~ s/\<shortName\>/shortName\:/g;
    $desc =~ s/\<fullName\>/fullName\:/g;
    $desc =~ s/\<recommendedName ref=\"\d+\"\>//g;
    $desc =~ s/  //g;
    $desc =~ s/\n/\t/gs;
    $desc =~ s/\t/ /g;
    $desc =~ s/\>//g;
    $desc =~ s/\<//g;
    $desc =~ s/type\=\"fragment\"//g;
    $desc =~ s/type\=\"fragments\"//g;

    $desc =~ s/^\s//;
    $desc =~ s/\s$//;

    my ($lngth) = /\<sequence.*length\=\"(\d+)\"/ms;
    my ($checksum) = /\<sequence.*checksum=\"([0-9A-F]+)\"/ms;
    
    my $gn_str = "";
    my @gene_name;
    while (/\<gene\>(.*?)\<\/gene\>/gms) {
        $gn_str = $1;
        $gn_str =~ s/\<\/name\>//g;
        $gn_str =~ s/\<name.*\"\>//g;
        $gn_str =~ s/ //g;
        my @names = split( /\n/, $gn_str );
        shift @names;
        push @gene_name, @names;
    }

    my $name = $gene_name[0] ? $gene_name[0] : $rec_name;
    
    my @reference_dna_sequences;
    if ($taxon =~ /Homo sapiens/i) {
        my %unique_gene_ids;
        my $type_attribute = qr/type=\"gene ID\"/;
        my $value_attribute = qr/value=\"(?<gene>.*?)\"/;
        my $gene_id_regex = qr/$type_attribute.*?$value_attribute|$value_attribute.*?$type_attribute/;
        while (/\<dbReference.*?type=\"Ensembl\".*?\<property.*?$gene_id_regex.*?\/\>/gms) {
            $unique_gene_ids{$+{gene}}++;
        }
        my @gene_ids = keys %unique_gene_ids;
        if (scalar @gene_ids > 1) {    
            print $ref_DNA_seq_report 'Multiple gene ids -- ' . join("\t", ($ac, $name, @gene_ids)) . "\n";
        }
        
        my $human_ensembl_gene_ref_db = $dba->fetch_instance_by_attribute('ReferenceDatabase',[['name', ['ENSEMBL_Homo_sapiens_GENE'],0]])->[0];
        foreach my $gene_id (@gene_ids) {
            my $reference_dna_sequence;
            
            if ($reactome_rds{$gene_id}) {
                print $ref_DNA_seq_report "Checking existing reference DNA sequence for $gene_id with db_id $reactome_rds{$gene_id}\n";
                
                $reference_dna_sequence = $dba->fetch_instance_by_db_id($reactome_rds{$gene_id})->[0];
                $reference_dna_sequence->inflate();
                my $added_reference_database = $reference_dna_sequence->add_attribute_value_if_necessary('referenceDatabase', $human_ensembl_gene_ref_db);
                my $added_gene_names = $reference_dna_sequence->add_attribute_value_if_necessary('geneName', @gene_name);
                my $added_species_instance = $reference_dna_sequence->add_attribute_value_if_necessary('species', $species_instance);
                
                if (@{$added_reference_database} || @{$added_gene_names} || @{$added_species_instance}) {
                    print $ref_DNA_seq_report "Updating existing reference DNA sequence for $gene_id with db_id $reactome_rds{$gene_id}\n";
                    
                    $reference_dna_sequence->add_attribute_value('modified', $instance_edit);
                    $dba->update($reference_dna_sequence);
                }
            } else {
                if ((scalar @gene_ids > 1) && !on_EnsEMBL_primary_assembly($gene_id)) {
                    # Reference DNA sequences to be created only for primary gene ids for a UniProt entry
                    # When there is only one gene id for a UniProt entry, it is assumed to be the primary gene id
                    print $ref_DNA_seq_report "$gene_id is not a primary/canonical gene -- skipping creation of ReferenceDNASequence\n";
                    next;
                }
                
                $reference_dna_sequence = GKB::Instance->new(
                    -CLASS => 'ReferenceDNASequence',
                    -ONTOLOGY => $dba->ontology,
                    'referenceDatabase' => $human_ensembl_gene_ref_db,
                    'identifier' => $gene_id
                );
                $reference_dna_sequence->inflated(1);
                $reference_dna_sequence->created($instance_edit);
                $reference_dna_sequence->modified(undef);
                $reference_dna_sequence->geneName(@gene_name);
                $reference_dna_sequence->species($species_instance);
                my $reference_dna_sequence_db_id = $dba->store($reference_dna_sequence);
                
                print $ref_DNA_seq_report "Reference DNA sequence with db_id $reference_dna_sequence_db_id created for $gene_id\n";
                $reactome_rds{$gene_id} = $reference_dna_sequence_db_id;
            }
            push @reference_dna_sequences, $reference_dna_sequence;
        }
    }    
    
    my @kw;
    while (/\<keyword id=\".*\"\>(.*)\<\/keyword\>/gm) {
        push @kw, $1;
    }
    my $cc;
    while (/\<comment type\=\"([A-Za-z\ ]*)\".*\s+\<text.*?\>(.*)\<\/text\>/gm) {
        my $tt = uc($1);
        $cc .= $tt . " " . $2;
    }

    #check for isoforms
    my %isoids;
    my $is = 0;
    if (/\<comment type\=\"alternative products\"\>/ms) {
        while (/\<isoform\>\n\s*\<id\>([A-Z0-9\-]*)\<\/id\>/gms) {
            $isoids{$1} = 1;
            $is++;
        }
        while (/\<isoform\>\n\s*\<id\>([A-Z0-9\-]*)\,/gms) {
            $isoids{$1} = 1;
            $is++;
        }
    }
    
    my @chains;
    while (/\<feature.*?type=\"initiator methionine\"(.*?)\<\/feature\>/gms) {
        my $feature = $1;

        my $position;
        if ($feature =~ /\<location\>\n\s+<position position=\"(\d+)\"/ms) {
            $position = $1;
        }

        push @chains, "initiator methionine:$position";
    } 
    
    while (/\<feature.*?type=\"(chain|peptide|propeptide|signal peptide|transit peptide)\"(.*?)\<\/feature\>/gms) {
        my $type = $1;
        my $feature = $2;

        my $begin = '';
        if ($feature =~ /<begin position=\"(\d+)\"/ms) {
            $begin = $1; 	
        }

        my $end = '';
        if ($feature =~ /<end position=\"(\d+)\"/ms) {
            $end = $1;
        }

        push @chains, "$type:$begin-$end";
    }
    
    # Values always use array reference
    my %values = (
        'secondaryIdentifier' => [@ac],
        'description' => [$desc],
        'sequenceLength' => [$lngth],
        'species' => [$species_instance],
        'checksum' => [$checksum],
        'name' => [$name],
        'geneName' => [@gene_name],
        'comment' => [$cc],
        'keyword' => [@kw],
        'chain' => [@chains]
    );
    if ($taxon =~ /Homo sapiens/i) {
        $values{'referenceGene'} = [@reference_dna_sequences];
    }

    if ( not defined $reactome_gp{$ac} ) {   #new UniProt instance if not exists
        $new_sp++;
        my $sdim = GKB::Instance->new(
            -CLASS              => 'ReferenceGeneProduct',
            -ONTOLOGY           => $dba->ontology,
            'referenceDatabase' => $db_inst,
            'identifier'        => $ac
        );
        $sdim->inflated(1);
        my $ddd = $dba->store($sdim);
        $sdim->created($instance_edit);
        $sdim->modified(undef);

        $ddd = $sdim->db_id;

        print "New UniProt\:$ac\t$ddd\n";

        updateinstance($sdim, \%values, $_);

        foreach my $isoid ( sort keys %isoids ) {
            if ( $isoid =~ /$ac/ ) {
                my $sdi = GKB::Instance->new(
                    -CLASS              => 'ReferenceIsoform',
                    -ONTOLOGY           => $dba->ontology,
                    'referenceDatabase' => $db_inst,
                    'identifier'        => $ac
                );
                $sdi->inflated(1);
                my $ddd = $dba->store($sdi);
                $sdi->isoformParent($sdim);
                $sdi->created($instance_edit);
                $sdi->modified(undef);
                $sdi->VariantIdentifier($isoid);

                $ddd = $sdi->db_id;

                updateinstance($sdi, \%values, $_);
            }
            else {
                $mis_parents{$isoid} = $ac;
            }
        }
    }
    else {
        my $sdt = $dba->fetch_instance_by_attribute( 'ReferenceGeneProduct', [ [ 'identifier', [$ac] ] ] );
        foreach my $sdi ( @{$sdt} ) {
            next if ( $sdi->class eq 'ReferenceIsoform' );    #reject ReferenceIsoform class

            my $dbd = $sdi->db_id;
            if ( $dupl_flag == 1 ) {
                $dup_db_id{$dbd} = $ac;
                next;
            }
            $sdi->inflate();
            
            my $sdd = $sdi->db_id;
            print "Updating master sequence...$sdd\t$ac\n";
                        
            $sdi->Created( @{ $sdi->Created } );
            $sdi->Modified( @{ $sdi->Modified } );
            $sdi->add_attribute_value( 'modified', $instance_edit );

            updateinstance($sdi, \%values, $_);
            
            $dupl_flag = 1;
            
            ## master sequence update finished
            
            $values{'species'} ||= $sdi->species->[0]; #Inherit species instance (if not already specified) from isoform parent
            foreach my $is_ac ( sort keys %isoids ) {    #isoforms update                
                if ( $is_ac =~ /$ac/ ) {
                    my $isst = $dba->fetch_instance_by_attribute( 'ReferenceIsoform', [ [ 'variantIdentifier', [$is_ac] ] ] );
                    if ( scalar @{$isst} > 0 ) {
                        foreach my $isod ( @{$isst} ) {
                            my $iac = $isod->VariantIdentifier->[0];
                            next if $iac !~ /$ac/;
                            print "Existing isoform update: $is_ac\tMaster: $sdd\n";
                            
                            $isod->inflate();

                            $isod->isoformParent($sdi);
            
                            $isod->Created( @{ $isod->Created } );
                            $isod->Modified( @{ $isod->Modified } );
                            $isod->add_attribute_value( 'modified', $instance_edit );

                            updateinstance($isod, \%values, $_);
                            
                            delete $isoids{$is_ac};
                            delete $reactome_iso{$is_ac};
                        }
                    }
                    else {    #new isoform
                        my $sdi_new = GKB::Instance->new(
                            -CLASS              => 'ReferenceIsoform',
                            -ONTOLOGY           => $dba->ontology,
                            'referenceDatabase' => $db_inst,
                            'identifier'        => $ac,
                            'isoformParent'     => $sdi
                        );
                        $sdi_new->inflated(1);
                        
                        my $ddd = $dba->store($sdi_new);
                        $sdi_new->created($instance_edit);
                        $sdi_new->modified(undef);
                        $sdi_new->variantIdentifier($is_ac);

                        print "New isoform: $is_ac\t$ddd\tMaster: $sdd\n";
                        updateinstance($sdi_new, \%values, $_);
                        $new_iso++;
                    }
                }
                else {
                    $mis_parents{$is_ac} = $ac;
                }
            }
            delete $reactome_gp{$ac};
        }
    }
}
close $ref_DNA_seq_report;
close $sequence_report_fh;
close $uniprot_records_fh;

#$dba->execute("COMMIT");
print "$record_counter records processed and committed\n";
print "All records in $sprot_file processed\n";
#XmL file parsing finished

print "Starting clean-up tasks after processing UniProt XML\n";
$dba->execute("START TRANSACTION");

print "Updating mis-matched isoforms\n";
#Now updating isoforms where accession does not parent entry
foreach my $mis_iso ( sort keys %mis_parents ) {
    my @parents = ();
    my $isod =
      $dba->fetch_instance_by_attribute( 'ReferenceIsoform',
        [ [ 'variantIdentifier', [$mis_iso] ] ] )->[0];

    if ( defined $isod ) {
        next unless $isod->isoformParent->[0];
        my $par1 = $isod->isoformParent->[0];
        push @parents, $par1;
    }
    my $pard =
      $dba->fetch_instance_by_attribute( 'ReferenceGeneProduct',
        [ [ 'identifier', [ $mis_parents{$mis_iso} ] ] ] )->[0];
    if ( defined $pard && defined $isod ) {
        push @parents, $pard;
        my $ddd = $isod->db_id;
        print "Mismatched parent: $mis_iso\($ddd\)\t$mis_parents{$mis_iso}\n";
        
        $isod->inflate();
        $isod->isoformParent(@parents);
        $dba->update($isod);
    }
}
print "Mis-matched isoform updates complete\n";

print "Updating display names...\n";

my $rps = $dba->fetch_instance( -CLASS => "ReferenceGeneProduct" );

foreach my $i ( @{$rps} ) {
    $i->namedInstance;

    $dba->update_attribute( $i, '_displayName' );
}

$rps = $dba->fetch_instance( -CLASS => "ReferenceIsoform" );

foreach my $i ( @{$rps} ) {
    $i->namedInstance;

    $dba->update_attribute( $i, '_displayName' );
}

print "Done.\n";

print "Remaining instances:" . keys(%reactome_gp) . "\n";

#Preparing reports and deleting obsoletes....

open(my $trembl_fh, '>', "$update_dir/trembl_to_update.acc" );
open(my $duplicate_db_id_fh, '>', "$update_dir/duplicated_db_id.txt" );

print "Deleting obsolete instances with no referers....\n";

foreach my $sp_ac ( sort keys %reactome_gp ) {
    if ( `grep -m 1 '^$sp_ac\$' $update_dir/$trembl_file` ) {
        print $trembl_fh "$sp_ac\n";
        delete( $reactome_gp{$sp_ac} );
    } else {
        my $obs_ac = $dba->fetch_instance_by_attribute( 'ReferenceGeneProduct', [ [ 'identifier', [$sp_ac] ] ] );
        
        foreach my $sdi ( @{$obs_ac} ) {
            my $test = $sdi->VariantIdentifier->[0];
            next if ( defined $test );
            my $db_id = $sdi->db_id;
            my $sz    = 0;
            my $ref   = $dba->fetch_referer_by_instance($sdi);
            $sz = $#{$ref};
            if ( $sz == -1 ) {
                print "Deleting $db_id...\n";
                $dba->delete_by_db_id($db_id);
                $obsolete_nr++;
                delete( $reactome_gp{$sp_ac} );
            }
        }
    }
}

my @skip;

foreach my $sp_ac ( sort keys %reactome_iso ) {
    my $flag = 0;
    my $sdi = $dba->fetch_instance_by_attribute( 'ReferenceIsoform', [ [ 'variantIdentifier', [$sp_ac] ] ] )->[0];
    unless ($sdi) {
        print "$sp_ac is not a variantIdentifier for any ReferenceIsoform\n";
        next;
    }
    
    my $db_id  = $sdi->db_id;    
    my $par    = $sdi->isoformParent->[0];
    unless ($par) {
        print $sdi->db_id,"\n";
        push @skip, $sdi->db_id;
        next;
    }
    next unless $par->identifier;
    
    my $par_ac = $par->identifier->[0];
    
    if ( $sp_ac !~ /$par_ac/ ) { $flag = 1 }
    my $sz  = 0;
    my $ref = $dba->fetch_referer_by_instance($sdi);
    $sz = $#{$ref};

    if ( $sz == -1 ) {
        print "Deleting $db_id...\n";
        $dba->delete_by_db_id($db_id);
        $obsolete_nr++;
        delete( $reactome_iso{$sp_ac} );
    }
}
print "Done.\n";

close $trembl_fh;

print "Preparing reports...\n";
my %no_referrer = ();

foreach ( sort keys %dup_db_id ) {
    next if `grep -m 1 '^$dup_db_id{$_}\$' $update_dir/$trembl_file`;
    print $duplicate_db_id_fh "$dup_db_id{$_}\t$_\n";
}
close $duplicate_db_id_fh;

my @skip_replaceable = ();
my @skip_no_replacement = ();

open(my $wiki_fh, '>', "$update_dir/uniprot.wiki");

print $wiki_fh "\{\| class\=\"wikitable\"
\|\+ Obsolete UniProt instances (with replacement UniProt)
\|\-
\! Replacement UniProt
\! Obsolete UniProt
\! Reactome instances with obsolete UniProt
\! EWAS associated with obsolete UniProt
\! Species
\|\-\n";

foreach my $t_ac ( sort keys %reactome_gp ) {
    foreach my $all_ac ( sort keys %sec_ac ) {
        if ( $sec_ac{$all_ac} =~ /$t_ac/ ) {
            my $pid;
            my @referrer = ();
            my $obs_ac = $dba->fetch_instance_by_attribute( 'ReferenceGeneProduct', [ [ 'identifier', [$t_ac] ] ] );
            my $species;            

            foreach my $sdi ( @{$obs_ac} ) {
                my $test = $sdi->VariantIdentifier->[0];
                next if ( defined $test );
                $pid = $sdi->db_id;              
                $species = $sdi->Species->[0]->Name->[0];

                my $ar2 = $dba->fetch_referer_by_instance($sdi);	#CY addition
                foreach my $ref ( @{$ar2} ) {
                    my $class = $ref->_class->[0];
                    if ( $class =~ /EntityWithAccessionedSequence/ ) {
                        push @referrer, $ref->db_id;
                    }
                }
            }
            if (@referrer) {
                my $report_line = "\|\[http\://www.uniprot.org/uniprot/$all_ac $all_ac\]\n\|";
                $report_line .= "$t_ac\n\|";		#CY addition
                $report_line .= "\[http\://reactomecurator\.oicr\.on.ca/cgi-bin/instancebrowser\?DB=$opt_db\&ID\=$pid\& $pid\]\n\|";
                $report_line .= '|'  . join('|', @referrer) . "\n\|";
                $report_line .= $species;
                $report_line .= "\n\|\-\n";

                if (any { $t_ac eq $_ } @skip_list) {
                    push @skip_replaceable, $report_line;
                } else {
                    print $wiki_fh $report_line;
                }
            } else {
                $no_referrer{$pid} = ();
            }
                        
            print "$t_ac\t$all_ac\t$pid\n";
            delete $reactome_gp{$t_ac};
        }
    }
}

print $wiki_fh "\|\}\n\n-----\n";

print $wiki_fh "\{\| class\=\"wikitable\"
\|\+ Obsolete UniProt instances (deleted forever, no replacement)
\|\-
\|
\! Obsolete UniProt
\! Reactome instances with obsolete UniProt
\! EWAS associated with obsolete UniProt
\! Species
\|\-\n";

foreach my $rac ( sort keys %reactome_gp ) {
    my $pid;
    my $oac;
    my @referrer = ();
    my $obs_ac = $dba->fetch_instance_by_attribute( 'ReferenceGeneProduct', [ [ 'identifier', [$rac] ] ] );
    my $species;

    foreach my $sdi ( @{$obs_ac} ) {
        my $test = $sdi->VariantIdentifier->[0];
        next if ( defined $test );
        $pid = $sdi->db_id;
        $species = $sdi->Species->[0] ? $sdi->Species->[0]->Name->[0] : "" ;

        my $ar2 = $dba->fetch_referer_by_instance($sdi);    #CY addition
        foreach my $ref ( @{$ar2} ) {
            my $class = $ref->_class->[0];
            if ( $class =~ /EntityWithAccessionedSequence/ ) {
                my $refid = $ref->stableIdentifier->[0];
                if ($refid) {
                    push @referrer, $ref->stableIdentifier->[0]->identifier->[0];
                } else {
                    push @referrer, $ref->db_id;
                }
            }
        }         
    }

    print "$rac\n";
    if (@referrer) {
        my $report_line = "\|\n\|";
        $report_line .= "\|$rac\n\|";
        $report_line .= "\[http\://reactomecurator.oicr.on.ca/cgi-bin/instancebrowser\?DB\=$opt_db\&ID\=$pid\& $pid\]\n\|";    
        $report_line .= '|' . join('|', @referrer) . "\n\|";
        $report_line .= $species;
        $report_line .= "\n\|\-\n";

        if ( any { $rac eq $_ } @skip_list) {
            push @skip_no_replacement, $report_line;
        } else {
            print $wiki_fh $report_line;
        }
    } else {
        $no_referrer{$pid} = ();
    }
}

foreach my $iac ( sort keys %reactome_iso ) {	
    my $isod = $dba->fetch_instance_by_attribute( 'ReferenceIsoform',
        [ [ 'variantIdentifier', [$iac] ] ] );

    my $species;    
    foreach my $sdi ( @{$isod} ) {
        my @referrer = ();
        my $id = $sdi->db_id;
        #$species = $sdi->Species->[0]->Name->[0];
        $species = $sdi->Species->[0] ? $sdi->Species->[0]->Name->[0] : "" ;
        my $ar2 = $dba->fetch_referer_by_instance($sdi);	#CY addition
        foreach my $ref ( @{$ar2} ) {
            my $class = $ref->_class->[0];
            if ( $class =~ /EntityWithAccessionedSequence/ ) {
                my $refid = $ref->stableIdentifier->[0];
                if ($refid) {
                    push @referrer, $ref->stableIdentifier->[0]->identifier->[0];
                } else {
                    push @referrer, $ref->db_id;
                }
            }
        }

        if (@referrer) {
            print "$iac\t$id\n";

            my $report_line = "\|\n\|";
            $report_line .= "\|$iac\n\|";
            $report_line .= "\[http\://reactomecurator.oicr.on.ca/cgi-bin/instancebrowser\?DB\=$opt_db\&ID\=$id\& $id\]\n\|";	    	
            $report_line .= '|' . join('|', @referrer) . "\n\|";
            $report_line .= $species;
            $report_line .= "\n\|\-\n";

            if (any { $iac eq $_ } @skip_list) {
                push @skip_no_replacement, $report_line;
            } else {
                print $wiki_fh $report_line;
            }
        } else {
            $no_referrer{$id} = ();
        }
    }   
}
print $wiki_fh "\|\}\n-----\n";

print $wiki_fh "\{\| class\=\"wikitable\"
\|\+ SKIPLIST Obsolete UniProt instances (with replacement UniProt)
\|\-
\! Replacement UniProt
\! Obsolete UniProt
\! Reactome instances with obsolete UniProt
\! EWAS associated with obsolete UniProt
\! Species
\|\-\n";

print $wiki_fh $_ foreach (@skip_replaceable);
print $wiki_fh "\|\}\n-----\n";

print $wiki_fh "\{\| class\=\"wikitable\"
\|\+ SKIPLIST Obsolete UniProt instances (deleted forever, no replacement)
\|\-
\|
\! Obsolete UniProt
\! Reactome instances with obsolete UniProt
\! EWAS associated with obsolete UniProt
\! Species
\|\-\n";

print $wiki_fh $_ foreach (@skip_no_replacement);
print $wiki_fh "\|\}\n";

close $wiki_fh;

print "\nDeleting DBID with obsolete UniProt and no referrers (2nd round during wiki report)...\n";

NEXT:foreach my $id (sort keys %no_referrer)
{
    foreach (@skip) {
        next NEXT if $id == $_;
    }
    next unless $id;
    $dba->delete_by_db_id($id);
    print "Deleting DBID: $id\n";  
}

print "Checking for duplicate isoform instances...\n";

$ar =
  $dba->fetch_instance_by_remote_attribute( 'ReferenceIsoform',
    [ [ 'referenceDatabase.name', '=', ['UniProt'] ] ] );

my %dupl_iso = ();

foreach my $iss ( @{$ar} ) {
    my $iac = $iss->VariantIdentifier->[0];
    my $idd = $iss->db_id;
    
    unless ($iac) {
        print "ReferenceIsoform $idd has no variant identifier\n";
        next;
    }
    
    if ( exists $dupl_iso{$iac} ) {
        $dupl_iso{$iac} .= "\t$idd";
        print "Multiple instances for $iac:\t$dupl_iso{$iac}\n";
    }
    else { $dupl_iso{$iac} = $idd; }
}

$dba->execute("COMMIT");
print "uniprot_xml2sql_isoform.pl has finished its job\n";
print "Total_db:$total_db\nTotal_xml:$total_xml\nObsolete:$obsolete_nr\nNew:$new_sp\tObsolete isoforms: $obs_iso\n";

exit(0);

sub species_instance {
    my $name  = shift;
    my $cache = shift;
    my $i;
    unless ( $i = $cache->{$name} ) {
        unless (
            $i =
            $dba->fetch_instance_by_attribute( 'Species',
                [ [ 'name', [$name] ] ] )->[0]
          )
        {
            $i = GKB::Instance->new(
                -ONTOLOGY => $dba->ontology,
                -CLASS    => 'Species',
                'name'    => [$name]
            );
            $i->inflated(1);
            $dba->store($i);
        }
    }
    return $i;
}

sub updateinstance {
    my $i = shift;
    my $values = shift;
    my $uniprot_entry = shift;

    if (!$i->checksum->[0] || $i->checksum->[0] eq $values->{'checksum'} ) {
        $i->isSequenceChanged("false");
    } else {
        $i->isSequenceChanged("true");
        print $sequence_report_fh $i->db_id . " sequence has changed\n";
    }

    my $changed = 0;
    foreach my $attribute (keys %{$values}) {
        my $new_values = [grep {defined} @{$values->{$attribute}}];
        if (scalar @{$new_values} == 0) {
            print "WARNING: No new values for $attribute on instance $i->{db_id} - skipping attribute update\n";
            next;
        }

        if (values_changed($i, $attribute, $new_values)) {
            $i->$attribute(@{$new_values});
            $changed = 1;
            #print "$uniprot_entry\n";
        }
        
        if ($attribute eq 'Chain') {
            update_chain_log($i, $new_values);
        }
    }
    $dba->update($i) if $changed;

    return $i;
}
    
sub values_changed {
    my $instance = shift;
    my $attribute = shift;
    my $new_values = shift;
    
    my ($current, $new);
    
    my $current_values = $instance->$attribute;
    if ($instance->is_instance_type_attribute($attribute)) {
        $current = [map {$_->db_id} @$current_values];
        $new = [map { $_->db_id } @$new_values];
    } else {
        $current = $current_values;
        $new = $new_values;
    }
    return 0 if same_array_contents($current, $new);
    
    print "$attribute changed for instance $instance->{db_id}:\n";
    print "old attribute values - " . join(',', sort @{$current}) . "\n";
    print "new attribute values - " . join(',', sort @{$new}) . "\n";
    
    return 1;
}

sub same_array_contents {
    my $array1 = shift;
    my $array2 = shift;
    
    # Not the same if different number of elements
    return 0 if scalar @{$array1} != scalar @{$array2};
    
    # Not the same if element in one array but not the other 
    foreach my $array1_element (@{$array1}) {
        return 0 if none {$_ eq $array1_element} @{$array2};
    }
    
    return 1;
}

sub update_chain_log {
    my $i = shift;
    my $new_chains = shift;
    my @old_chains = @{$i->Chain};

    my $t = localtime;
    my $date = $t->day . ' ' . $t->fullmonth . ' ' . $t->mday . ' ' . $t->year;

    my $reference_gene_product = $i->db_id;
    $reference_gene_product .= " - ". $i->name->[0] if $i->name;
    $reference_gene_product .= " (" . $i->species->[0]->name->[0] . ")" if $i->species->[0];

    foreach my $old_chain (@old_chains) {
        unless ( any { $old_chain  eq $_ } @{$new_chains}) {
            my $log_entry = "$old_chain for " . $i->db_id . " removed on $date";
            print $sequence_report_fh $log_entry . " for $reference_gene_product\n";

            my $log = $i->_chainChangeLog->[0] ? $i->_chainChangeLog->[0] . ';' . $log_entry : $log_entry;
            $i->add_attribute_value('_chainChangeLog', $log);
            print "old chain removed for " . $i->db_id . "\n";
        }
    }

    foreach my $new_chain (@{$new_chains}) {
        unless ( any { $new_chain eq $_ } @old_chains) {
            my $log_entry = "$new_chain for " . $i->db_id . " added on $date";
            print $sequence_report_fh $log_entry . " for $reference_gene_product\n";

            my $log = $i->_chainChangeLog->[0] ? $i->_chainChangeLog->[0] . ';' . $log_entry : $log_entry;
            $i->add_attribute_value('_chainChangeLog', $log);
            print "new chain added for " . $i->db_id . "\n";
        }
    }
}

sub get_skip_list {
    my %skip_list_id = ();

    opendir(my $dir, $update_dir);
    while(my $file = readdir $dir) {
        next unless $file =~ /^skiplist/;
        open(my $skiplist, "<", "$update_dir/$file");
        while(my $uniprot_id = <$skiplist>) {
            chomp $uniprot_id;
            next unless (length $uniprot_id == 6 || length $uniprot_id == 10);
            $skip_list_id{$uniprot_id}++;
        }
        close $skiplist;
    }
    closedir $dir;

    return keys %skip_list_id;
}
