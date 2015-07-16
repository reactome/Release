#!/usr/local/bin/perl -w
use strict;

use lib "/usr/local/gkb/modules";

use GKB::Config;
use GKB::DBAdaptor;

use autodie;

my $output_dir = 'reaction_tables';
system("mkdir -p $output_dir; rm -f $output_dir/*");

foreach my $pathway (get_human_pathways()) {
    my $pathway_id = $pathway->db_id;
    
    my $return_value = system("perl reaction_logic_table.pl -pathways $pathway_id -output $output_dir/$pathway_id.tsv");
    
    unless ($return_value == 0) {
	print STDERR "Pathway $pathway_id produced an error in generating a reaction logic table\n";
    }
}

sub get_human_pathways {    
    my $human_pathways = get_dba($GKB::Config::GK_DB_NAME)->fetch_instance(-CLASS => 'Pathway', [['species', ['48887']]]);
    
    return @{$human_pathways};
}

sub get_dba {
    my $db = shift;

    return GKB::DBAdaptor->new(
	-dbname => $db,
	-user   => $GKB::Config::GK_DB_USER,
	-host   => $GKB::Config::GK_DB_HOST, # host where mysqld is running                                                                                                                              
	-pass   => $GKB::Config::GK_DB_PASS,
	-port   => '3306'
    );	
}
