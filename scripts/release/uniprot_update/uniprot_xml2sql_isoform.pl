#!/usr/local/bin/perl  -w

#This script updates the Uniprot ReferencePeptideSequences in Reactome. Uniprot entries (swissprot section) are imported/updated in Reactome if they are assigned to one of the species specified in @species.
#The script checks for existing entries and updates them.

###############################################
####IMPORTANT STEPS BEFORE RUNNING THE SCRIPT##
#The list of Trembl entries that's needed to check remaining entries in the second round, needs to be downloaded from the UniProt website:
#Go to:  http://www.uniprot.org/  
#Click on Search without entering any search term. 
#Click on "unreviewed" in "Show only reviewed  (UniProtKB/Swiss-Prot) or unreviewed  (UniProtKB/TrEMBL) entries".
#Click on "download" (right top corner). 
#Click on "compressed" in "Download data compressed or uncompressed". 
#Choose "Download" under "LIST".
#This gives you a single column list of all Trembl accessions. 
#Save this list in the /usr/local/gkbdev/scripts/uniprot_update directory on brie8.
#Note that depending on the web browser, the file will be named by default as "uniprot-reviewed_no.list.gz" or "uniprot-reviewed:no.list.gz"
#This script will take care of the filename discrepancy so you don't have to rename the file after downloading
#
#The file to parse for the update of Swissprot entries is at ftp://ftp.ebi.ac.uk/pub/databases/uniprot/knowledgebase/uniprot_sprot.xml.gz
#This file is automatically downloaded and unzipped by this script and saved to /usr/local/gkbdev/scripts/uniprot_update
#
#Save a dump of gk_central in /usr/local/gkbdev/scripts/uniprot_update/ as a backup.
#
#Run the script from /usr/local/gkbdev/scripts: ./uniprot_db_update.pl -db gk_central -user xxxx -pass xxxx

use lib "/usr/local/gkbdev/modules";

use GKB::Instance;
use GKB::DBAdaptor;
use GKB::Utils_esther;
use Data::Dumper;
use Getopt::Long;
use strict;

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

if ($trembl_file =~ /\.gz$/)
{
	system("gunzip -f $update_dir/$trembl_file");
	$trembl_file =~ s/\.gz$//;
}

# Download sprot file
my $sprot_file = "uniprot_sprot.xml";
#my $return = system("wget -Nc --directory-prefix=$update_dir ftp://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/complete/$sprot_file.gz");
#die "ERROR: Downloading of $sprot_file.gz failed\n" unless ($return == 0);
my $return = system("gunzip -f $update_dir/$sprot_file.gz");
die "ERROR: Unzipping of uniprot_sprot.xml failed\n" unless ($return == 0);

# Prepare to update InstanceEdit
# To be replaced by name and initials of a user who runs the update
my $surname = 'Weiser';
my $initial = 'JD';
my $date    = `date \+\%F`;
chomp $date;

#save trembl entries - needed further down to identify non-updated entries
open( TREMBL, "$update_dir/$trembl_file" ) || die "Can't open $update_dir/$trembl_file\n";
my %trembl;
while (<TREMBL>) {
    chomp;
    $trembl{$_}++;
}
close(TREMBL);


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
  

open( UP, "$update_dir/$sprot_file" )  || die "Can't open uniprot_sprot.xml\n";
open REPORT, ">$update_dir/uniprot_report";
print REPORT "Db ids of changed referenceGeneProducts:\n";
# Parse the xml file....

local $/ = "\<\/entry\>\n";

$dba->execute("START TRANSACTION");
while (<UP>) {
    chomp;
    my @ac;
    my $dupl_flag = 0;

    while (/\<accession\>(\w+)\<\/accession\>/gm) {
        push @ac, $1;
    }

    my $ac = shift @ac;
    $sec_ac{$ac} = join( '|', @ac );

    #Taxonomy check

    my ($ox) = /\<dbReference type=\"NCBI Taxonomy\" key=\"\d+\" id=\"(\d+)\"/m;
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

    if (/\<gene\>(.*)\<\/gene\>/ms) {
        $gn_str = $1;
        $gn_str =~ s/\<\/name\>//g;
        $gn_str =~ s/\<name.*\"\>//g;
        $gn_str =~ s/ //g;
        @gene_name = split( /\n/, $gn_str );
        shift @gene_name;
    }

	my $name = $gene_name[0] ? $gene_name[0] : $rec_name; 	

    my @kw;
    while (/\<keyword id=\".*\"\>(.*)\<\/keyword\>/gm) {
        push @kw, $1;
    }
    my $cc;
    while (/\<comment type\=\"([A-Za-z\ ]*)\".*\s+\<text\>(.*)\<\/text\>/gm) {
        my $tt = uc($1);
        $cc .= $tt . " " . $2;
    }

    #check for isoforms
    my %isoids;
    my $is = 0;
    if (/\<comment type\=\"alternative products\"\>/ms) {
        while (/\<isoform\>\n\s+\<id\>([A-Z0-9\-]*)\<\/id\>/gms) {
            $isoids{$1} = 1;
            $is++;
        }
        while (/\<isoform\>\n\s+\<id\>([A-Z0-9\-]*)\,/gms) {
            $isoids{$1} = 1;
            $is++;
        }
        print "\n";
    }

	my %values = (
		'SecondaryIdentifier' => \@ac,
		'Description' => $desc,
		'sequenceLength' => $lngth,
		'Species' => $species_instance,
		'checksum' => $checksum,
		'name' => $name,
		'GeneName' => \@gene_name,
		'Comment' => $cc,
		'Keyword' => \@kw 
	);



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

	updateinstance($sdim, \%values);

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

				updateinstance($sdi, \%values);
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

            updateinstance($sdi, \%values);
         	
            $dupl_flag = 1;
            
            ## master sequence update finished

            foreach my $is_ac ( sort keys %isoids ) {    #isoforms update
                if ( $is_ac =~ /$ac/ ) {
                    my $isst = $dba->fetch_instance_by_attribute( 'ReferenceIsoform', [ [ 'variantIdentifier', [$is_ac] ] ] );
                    if ( defined $isst ) {
                        foreach my $isod ( @{$isst} ) {
                            my $iac = $isod->VariantIdentifier->[0];
                            next if $iac !~ /$ac/;
                            print "Existing isoform update: $is_ac\tMaster: $sdd\n";
                            
                            $isod->inflate();

						    $isod->isoformParent($sdi);
            
                            $isod->Created( @{ $isod->Created } );
                            $isod->Modified( @{ $isod->Modified } );
                            $isod->add_attribute_value( 'modified', $instance_edit );

                            updateinstance($isod, \%values);
                            
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

						print "New isoform: $new_iso\t$ddd\tMaster: $sdd\n";
                        updateinstance($sdi_new, \%values);
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
close REPORT;
#XmL file parsing finished

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

print "Update is finished\n";
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

open( FT, ">./$update_dir/trembl_to_update.acc" ) || die "Can not open Trembl accession output file $!";

open( FD, ">./$update_dir/duplicated_db_id.txt" ) || die "Can't open duplicated db_id file $!";

print "Deleting obsolete instances with no referers....\n";

foreach my $sp_ac ( sort keys %reactome_gp ) {
    if ( defined $trembl{$sp_ac} ) {
        print FT "$sp_ac\n";
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

close FT;

print "Preparing reports...\n";
my %no_referrer = ();

foreach ( sort keys %dup_db_id ) {
    next if defined $trembl{ $dup_db_id{$_} };
    print FD "$dup_db_id{$_}\t$_\n";
}

my @skip_replaceable = ();
my @skip_no_replacement = ();

open( FR, ">./$update_dir/uniprot.wiki" ) || die "Can't open report file\n";

print FR "\{\| class\=\"wikitable\"
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
		
		if ($t_ac ~~ @skip_list) {
			push @skip_replaceable, $report_line;
		} else {
			print FR $report_line;
		}
            } else {
            	$no_referrer{$pid} = ();            	
            }
                        
            print "$t_ac\t$all_ac\t$pid\n";
            delete $reactome_gp{$t_ac};
        }
    }
}

print FR "\|\}\n\n-----\n";

print FR "\{\| class\=\"wikitable\"
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
        $species = $sdi->Species->[0]->Name->[0];

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
    }

    print "$rac\n";
    if (@referrer) {
	my $report_line = "\|\n\|";
	$report_line .= "\|$rac\n\|";
	$report_line .= "\[http\://reactomecurator.oicr.on.ca/cgi-bin/instancebrowser\?DB\=$opt_db\&ID\=$pid\& $pid\]\n\|";    
    	$report_line .= '|' . join('|', @referrer) . "\n\|";
        $report_line .= $species;
    	$report_line .= "\n\|\-\n";
	
	if ($rac ~~ @skip_list) {
		push @skip_no_replacement, $report_line;
	} else {		
		print FR $report_line;
	}
    } else {
    	$no_referrer{$pid} = ();    	
    }
}

foreach my $iac ( sort keys %reactome_iso ) {	
    my $isod = $dba->fetch_instance_by_attribute( 'ReferenceIsoform',
        [ [ 'variantIdentifier', [$iac] ] ] );

    my $species;    
    foreach my $sdi ( @{$isod} )
    {
    	my @referrer = ();
    	my $id = $sdi->db_id;
    	$species = $sdi->Species->[0]->Name->[0];

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
		
		if ($iac ~~ @skip_list) {
			push @skip_no_replacement, $report_line;
		} else {
			print FR $report_line;
		}
    	}
    	else {
    		$no_referrer{$id} = ();    		    		
    	}
    	
    }   
}
print FR "\|\}\n-----\n";

print FR "\{\| class\=\"wikitable\"
\|\+ SKIPLIST Obsolete UniProt instances (with replacement UniProt)
\|\-
\! Replacement UniProt
\! Obsolete UniProt
\! Reactome instances with obsolete UniProt
\! EWAS associated with obsolete UniProt
\|\-\n";

print FR foreach (@skip_replaceable);
print FR "\|\}\n-----\n";

print FR "\{\| class\=\"wikitable\"
\|\+ SKIPLIST Obsolete UniProt instances (deleted forever, no replacement)
\|\-
\|
\! Obsolete UniProt
\! Reactome instances with obsolete UniProt
\! EWAS associated with obsolete UniProt
\|\-\n";

print FR foreach (@skip_no_replacement);
print FR "\|\}\n";

close FR;

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

$dba->execute('COMMIT');

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
	
	my %values = %$values;
	
	my $changed = 0;
	
	if (!$i->checksum->[0] || $i->checksum->[0] eq $values{'checksum'} ) {
		$i->isSequenceChanged("false");
	} else {
		$i->isSequenceChanged("true");
		print REPORT $i->db_id . "\n";
	}
	
	foreach my $value (keys %values) {
		my $new = $values{$value};
					
		next unless $new;
		next if ($value eq 'Species' && same_species($new, $i));
		next if ($value ne 'Species' && $new ~~ $i->$value);
		
		if (ref $new eq 'ARRAY') {
			$i->$value(@$new);
		} else {
			$i->$value($new);
		}
		
		$changed = 1;
		
	}
	
	$dba->update($i) if $changed;
	
	return $i;
}

sub same_species {
	my $new_species = shift;
	my $current_species = shift;
	
	return unless ($new_species && $new_species->name->[0] && $current_species && $current_species->name->[0]);
	return $new_species->name->[0] eq $current_species->name->[0];
}

sub get_skip_list {
	my %skip_list_id = ();
	
	opendir(my $dir, $update_dir) || die "Can't open $update_dir";
	while(my $file = readdir $dir) {
		next unless $file =~ /^skiplist/;
		open(my $skiplist, "<", "$update_dir/$file") || die "Can't open $file";
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
