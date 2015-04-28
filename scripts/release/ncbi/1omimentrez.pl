#!/usr/bin/perl -w
use strict;

use autodie qw/:all/;
use Net::FTP;
use Getopt::Long;

my $archive = 'archive';
#to get mim2gene file from Entrez gene ftp site
my $mim2gene_path = "$archive/mim2gene";

my $ftp = Net::FTP->new(Host => "ftp.ncbi.nih.gov", Debug => 0, Passive => 1);
$ftp->login("anonymous",'-anonymous@') or die get_instructions() . $ftp->message;
$ftp->cwd("/gene/DATA") or die get_instructions() . $ftp->message;
$ftp->get("mim2gene_medgen") or die get_instructions() . $ftp->message;
$ftp->quit;

system("mv mim2gene_medgen $mim2gene_path");

our ( $opt_user, $opt_host, $opt_pass, $opt_port, $opt_db);
(@ARGV)  || die "Usage: $0 -user db_user -host db_host -pass db_pass -port db_port -db db_name\n";
&GetOptions( "user:s", "host:s", "pass:s", "port:i", "db:s");
$opt_db  || die "Need database name (-db).\n";

my ($num) = $opt_db =~ /(\d+)/;

my %ncbi_gene_2_mim =();
open (my $mim2gene_in, "<", "$mim2gene_path");
while (<$mim2gene_in>){
    if(/^(\d+)\s+(\d+)/){
        my $mim_number = $1;
        my $gene_id = $2;

        push @{$ncbi_gene_2_mim{$gene_id}},$mim_number;
    }
}

my %ncbi_gene_2_uniprot =();
open (my $prot_gene_in, "<", "$archive/prot_gene$num"); # get this file first by running 1proteinentrez.pl
while(<$prot_gene_in>){
    if(/^(\w{6})\-*\d*\s+(\d+)$/){
        my $uniprot_id  = $1;
        my $gene_id = $2;

        push @{$ncbi_gene_2_uniprot{$gene_id}},$uniprot_id;
    }
}

#this is for gene to mim id mapping; for test not for sending out;
open (my $genemim_out, ">", "$archive/genemim$num");
print $genemim_out "UniProt ","\t","MIM"."\n";

open (my $omim_reactome_out, ">", "$archive/omim_reactome$num.ft");
print $omim_reactome_out get_header();

my $count = 0;
foreach my $gene_id (keys %ncbi_gene_2_uniprot){
    foreach my $uniprot_id (@{$ncbi_gene_2_uniprot{$gene_id}}){
        if($ncbi_gene_2_mim{$gene_id}){
            print $genemim_out $uniprot_id."\t"."@{$ncbi_gene_2_mim{$gene_id}}\n";

            foreach my $mim_number (@{$ncbi_gene_2_mim{$gene_id}}){
                print $omim_reactome_out
                    get_omim_record($mim_number,$uniprot_id) .
                    get_separator();
 
                $count++;
            }
        }
    }
}
print "No. of entries for OMIM:$count\n";

close($mim2gene_in);
close($prot_gene_in);
close($genemim_out);
close($omim_reactome_out);

sub get_instructions {
    return <<END;

The mim2gene file did not download properly.  To manually download it,
go to ftp.ncbi.nih.gov and click "gene" then "DATA" and save the
mim2gene file to your hard drive.  Then, transfer the mim2gene file
to /usr/local/gkbdev/scripts/ncbi/archive on the release server.

END

}

sub get_header {
    return
get_separator() .
<<END

prid:     4914
dbase:    omim
stype:    meta-databases
!base:    http://www.reactome.org/content/query?q=UniProt:
END
.get_separator()
}

sub get_separator {
    return "-" x 56;
}

sub get_omim_record {
    my $uid = shift;
    my $rule = shift;

    return <<END;

linkid:   0
uids:   $uid
base:   &base;
rule:   $rule
END
}
