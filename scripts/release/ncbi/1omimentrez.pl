#!/usr/bin/perl  -w
use strict;
use Net::FTP;

#to get mim2gene file from Entrez gene ftp site


my $instructions =<<END;

The mim2gene file did not download properly.  To manually download it,
go to ftp.ncbi.nih.gov and click "gene" then "DATA" and save the
mim2gene file to your hard drive.  Then, transfer the mim2gene file
to /usr/local/gkbdev/scripts on the release server.

IGNORE THIS MESSAGE IF YOU HAVE ALREADY DOWNLOADED mim2gene.

END

my  $ftp = Net::FTP->new("ftp.ncbi.nih.gov", Debug => 0);
    $ftp->login("anonymous",'-anonymous@');
    
    $ftp->cwd("/gene/DATA");
    $ftp->get("mim2gene_medgen") or print $instructions;
    $ftp->quit;

my $num = $ARGV[0];
chomp $num;

my $output = 'genemim'.$num;  #this is for gene to mim id mapping; for test not for sending out;
open (OUTPUT, ">archive/$output");

print OUTPUT "UniProt ","\t","MIM"."\n";

my $output2 = 'omim_reactome'.$num.'.ft';
open (OUTPUT2, ">archive/$output2");


#make sure the path is correct to these 2 files if not in the same directory
system("mv mim2gene_medgen archive/mim2gene");

my $in1 = 'archive/mim2gene';  # already transferred from ncbi ftpsite as above
my $in2 = 'archive/prot_gene'.$num; # get this file first by running 1proteinentrez.pl 



open (FH1,"<$in1");
open (FH2,"<$in2");


my $tag = "\t"."http://www.reactome.org/cgi-bin/link?SOURCE=UniProt&ID=";

my $X;
 
my %content;
my $string;

my %arr1 =();
my %arr2 =();


my $label5 = "\nprid:     4914\n"."dbase:    gene\n"."stype:    meta-databases\n"."!base:    $tag\n";
my $label6 = "\nlinkid:   0\n";

my $label7 = "-"x56;
my $label8 = "base:   &base;\n";
 

while (<FH1>){
    if(/^(\d+)\s+(\d+)/){
        my $Y  = $2;
        my $Y1 = $1;


        push (@{$arr1{$Y}{mim}},$Y1);

    }
}

my $val;

my $c =0;


while(<FH2>){
    if(/^(\w{6})\-*\d*\s+(\d+)$/){
        my $X  = $1;
        my $X1 = $2;

        push (@{$arr2{$X1}{genes}},$X);

        foreach(@{$arr2{$X1}{genes}}){


            my $G = $X1;
            my $P = $_;

            my $uid = "uids:   $G\n";
            my $rule ="rule:   $P\n";


 

        $c++;
    }

    #if ($arr1{$X1}){
    #
    #push (@{$content{$X}{genes}},$arr1{$X1});
    ##push (@{$content{$X}{url}},$tag.$X1);
    #
    #
    #}

    }
}

print "No. of entries for Gene:$c\n";

my $label1 = "\nprid:     4914\n"."dbase:    omim\n"."stype:    meta-databases\n"."!base:    $tag\n";
my $label2= "\nlinkid:   0\n";

my $label3 = "-"x56;
my $label4 = "base:   &base;\n";

print OUTPUT2 $label3.$label1.$label3;

my $count = 0;

foreach $val (keys %arr2){

    foreach(@{$arr2{$val}{genes}}){

        my $M = $_;

        if($arr1{$val}{mim}){

            #print OUTPUT "@{$arr2{$val}{genes}}"."\t"."@{$arr1{$val}{mim}}\n";
            print OUTPUT $M."\t"."@{$arr1{$val}{mim}}\n";

            foreach (@{$arr1{$val}{mim}}){

                my $Z = $_;
                my $uid = "uids:   $Z\n";
                my $rule ="rule:   $M\n";

                print OUTPUT2 $label2,$uid,$label4,$rule,$label3;
 
                $count++;
            }
        }
    }
}

print "No. of entries for OMIM:$count\n";
