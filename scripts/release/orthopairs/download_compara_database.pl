#!/usr/local/bin/perl  -w
use strict;

# Created by: Joel Weiser (joel.weiser@oicr.on.ca)
# Purpose: Download and setup the databases from ensembl compara necessary
#          to create the mapping files for the orthopair release step.
#		   Each database has its own folder under /nfs/reactome/reactome/
#		   archive/compara_dbs housing the downloaded SQL schema file
#		   and the text files with the table content.
#		   The active databases are stored under /nfs/reactome/reactome/
#		   archive/mysql and are pointed to by symbolic links under
#		   /var/lib/mysql.
# TODO:  This script needs a way to continue downloading the ensembl
#		 compara content if it crashes or is interrupted.
#		 A way to remove older downloaded versions is also needed
#		 (this includes the downloaded content, the active databases
#		 and the symbolic links pointing to them).


use lib "/usr/local/gkb/modules";

use GKB::Config;
use GKB::Config_Species;
use GKB::EnsEMBLMartUtils qw/get_version get_ensembl_genome_version/;

use autodie qw/:all/;
use Cwd;
use Net::FTP;

my $user = $GKB::Config::GK_DB_USER;
my $pass = $GKB::Config::GK_DB_PASS;

my $compara_db_dir = 'compara_dbs';
my $nfs_dir = "/nfs/reactome/reactome/archive";

mkdir "$nfs_dir/$compara_db_dir" unless (-e "$nfs_dir/$compara_db_dir");
symlink "$nfs_dir/$compara_db_dir", $compara_db_dir unless (-e $compara_db_dir);
chdir "$compara_db_dir";

my $ensembl_url = 'ftp.ensembl.org';
my $ensembl_mysql_dir = 'pub/release-' . get_version() . "/mysql";
my $ensembl_compara_url = join '/', ($ensembl_url, $ensembl_mysql_dir, "ensembl_compara_" . get_version());

my $ensembl_genomes_url = 'ftp.ensemblgenomes.org';
my $ensembl_pan_homology_url = $ensembl_genomes_url . '/pub/pan_ensembl/current/mysql/ensembl_compara_pan_homology_' .
    get_ensembl_genome_version() . "_" . get_version();

my @tables = qw/genome_db species_set method_link method_link_species_set homology homology_member gene_member seq_member meta/;
    
download_and_setup_db($ensembl_compara_url, \@tables, 'core_tables');    
download_and_setup_db($ensembl_pan_homology_url, \@tables, 'pan_homology_tables');

download_and_setup_species_db($ensembl_url, $ensembl_mysql_dir);
download_and_setup_species_db($ensembl_genomes_url, "pub/current/$_/mysql") foreach qw/plants protists fungi/;

sub download_and_setup_species_db {
    my $url = shift;
    my $mysql_dir = shift;
    
    my $ftp = get_ftp_object($url, $mysql_dir);
    
    my @files = $ftp->ls();
    my @core_db_folders = grep(/core/, @files);
    
    foreach my $core_db_folder (@core_db_folders) {
	$ftp = get_ftp_object($url, "$mysql_dir/$core_db_folder");
    
	my @tables = grep(/.txt.gz$/, $ftp->ls());
	s/.txt.gz$// foreach @tables;
    
	my ($genus, $species) = $core_db_folder =~ /^(\w)\w+?_(\w{3})/;
	
	download_and_setup_db("$url/$mysql_dir/$core_db_folder",
			       \@tables,
			       $core_db_folder) if exists($species_info{$genus.$species});
    }
}

sub download_and_setup_db {
    my $url = shift;
    my $tables = shift;
    my $dir = shift;
    
    my $cwd = getcwd;
    
    mkdir $dir unless (-e $dir);
    chdir $dir;
    
    my ($db) = $url =~ /.*\/(.*)/;
    system("wget --passive-ftp 'ftp://$url/$db.sql.gz'; gunzip -f $db.sql.gz")
		   unless (-e "$db.sql");
    
    system("mysql -u $user -p$pass -e 'drop database if exists $db;'");
    my $nfs_db = "$nfs_dir/mysql/$db";
    mkdir $nfs_db unless (-e $nfs_db);
    system("sudo chmod 777 $nfs_db");
    chdir '/var/lib/mysql';
    symlink $nfs_db, $db unless (-e $db);
    system("sudo chown mysql:mysql $db");
    chdir "$cwd/$dir";
    system("mysql -u $user -p$pass $db < $db.sql");
    
    foreach my $table (@$tables) {
	print "Processing $table\n";
	my $table_file = "$table.txt.gz";
	system("wget --passive-ftp -N 'ftp://$url/$table_file'; gunzip -f $table_file") unless (-s "$table.txt");
	system("mysqlimport -u $user -p$pass $db -L $table.txt");
    }
    
    chdir $cwd;
}

sub get_ftp_object {
    my $url = shift;
    my $dir = shift;
    
    my $ftp = Net::FTP->new($url, Passive => 1, Debug => 0);
    $ftp->login();
    $ftp->cwd($dir);
    
    return $ftp;
}

