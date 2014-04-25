#!/usr/local/bin/perl -w

use strict;
use autodie;

!$ARGV[0] or $ARGV[0] eq '-h' and die<<__END__;
A script for creating GKB.tar.gz for local installation of Reactome

Usage: $0 dir_to_deposit_tarball release_number [cvs_tag_name, HEAD by default]
__END__

my $dir_for_tarball = shift;
my $tag_name = shift || 'HEAD';

mkdir $dir_for_tarball unless (-e $dir_for_tarball);

chdir $dir_for_tarball;

my @cmds = (
    ["cvs -d :ext:reactomecurator.oicr.on.ca:/usr/local/cvs_repository co -r $tag_name GKB"],
    ["mkdir GKB/website/logs"],
    ["rm GKB/website/html/redirect_from_survey.html",1],
    ["rm GKB/website/html/graph.html",1],
    ["rm -rf GKB/website/html/gkb",1],
    ['mkdir GKB/website/html/img-tmp', 1],
    ['chmod 777 GKB/website/html/img-tmp', 1],
#    ['mv GKB/modules/GKB/Config.pm-dist GKB/modules/GKB/Config.pm'],
    ['mkdir GKB/scripts_tmp'],
    ['cp GKB/scripts/fetchEmptyProject.pl GKB/scripts_tmp/.', 1],
    ['cp GKB/scripts/addSchemaTable.pl GKB/scripts_tmp/.', 1],
    ['cp GKB/scripts/updateDatabase.pl GKB/scripts_tmp/.', 1],
	    ['cp GKB/scripts/updateDisplayName.pl GKB/scripts_tmp/.', 1],
    ['cp GKB/scripts/createDatabase.pl GKB/scripts_tmp/.', 1],
    ['cp -r GKB/scripts/denormalised_db GKB/scripts_tmp/.', 1],
    ['cp -r GKB/scripts/release GKB/scripts_tmp/.', 1],
    ['cp GKB/scripts/merge_2_instances.pl GKB/scripts_tmp/.', 1],
    ['cp GKB/scripts/merge_2_instances_from_list.pl GKB/scripts_tmp/.', 1],
    ['cp GKB/scripts/myisam2innodb.pl GKB/scripts_tmp/.', 1],
    ['cp GKB/scripts/innodb2myisam.pl GKB/scripts_tmp/.', 1],
	    ['cp GKB/scripts/SBML_dumper.pl GKB/scripts_tmp/.', 1],
	    ['cp GKB/scripts/report_interactions.pl GKB/scripts_tmp/.', 1],
	    ['cp GKB/scripts/uniprot_entries_in_reactome.pl GKB/scripts_tmp/.', 1],
	    ['cp GKB/scripts/uniprot_entries_in_reactome_w_stid.pl GKB/scripts_tmp/.', 1],
	    ['cp GKB/scripts/fetch_and_print_values.pl GKB/scripts_tmp/.', 1],
    ['cp -r GKB/scripts/examples GKB/scripts_tmp/.', 1],
	    ['mkdir GKB/scripts_tmp/QA_scripts'],
	    ['cp GKB/scripts/QA_scripts/remove_unused_PE.pl GKB/scripts_tmp/QA_scripts/.'],
	    ['chmod 755 GKB/scripts_tmp/QA_scripts/*'],
    ['rm -rf GKB/scripts'],
    ['mv GKB/scripts_tmp GKB/scripts'],
	    
	    ['mkdir GKB/orthomcl_project_tmp'],
	    ['mkdir GKB/orthomcl_project_tmp/orthopairs'],	    
	    ['mkdir GKB/orthomcl_project_tmp/reports'],
	    ['mkdir GKB/orthomcl_project_tmp/scripts'],
	    ['cp GKB/orthomcl_project/scripts/infer_events_orthomcl.pl GKB/orthomcl_project_tmp/scripts/.', 1],
	    ['cp GKB/orthomcl_project/scripts/prepare_orthopair_files.pl GKB/orthomcl_project_tmp/scripts/.', 1],
	    ['cp GKB/orthomcl_project/scripts/tweak_datamodel_orthomcl_inference.pl GKB/orthomcl_project_tmp/scripts/.', 1],
	    ['rm -rf GKB/orthomcl_project'],
	    ['mv GKB/orthomcl_project_tmp GKB/orthomcl_project'],
	    ['chmod 755 GKB/orthomcl_project/scripts/*'],
    ['chmod 755 GKB/website/cgi-bin/*'],
    ['chmod 644 GKB/website/cgi-bin/protege2mysql'],
    ['chmod 755 GKB/website/html/*htm*'],
    ['chmod 755 GKB/website/html/download/*htm*'],
#    ['mv GKB/website/html/download/16 GKB/website/html/download/current'],
    ["rm -rf GKB/website/html/download/1*",1],
    ['find GKB -name CVS -exec rm -rf {} \;', 1],
    ['mkdir GKB2'],
    ['cp -r GKB/website GKB2/website'],
    ['cp -r GKB/scripts GKB2/scripts'],
    ['cp -r GKB/modules GKB2/modules'],
    ['cp -r GKB/java GKB2/java'],
    ['cp -r GKB/BioMart GKB2/BioMart'],
    ['cp -r GKB/biopaxexporter GKB2/biopaxexporter'],
    ['cp -r GKB/orthomcl_project GKB2/orthomcl_project'],
    ['cp -r GKB/ReactomeGWT GKB2/ReactomeGWT'],
    ['rm -rf GKB'],
    ['mv GKB2 GKB'],
    ['tar cvzf GKB.tar.gz GKB'],
    ['rm -rf GKB', 1]
);

foreach my $ar (@cmds) {
    my $cmd = $ar->[0];
    print "$cmd\n";
    if ($ar->[1]) {
	system($cmd) == 0 or print STDERR "Something wrong with '$cmd'\n";
    } else {
	system($cmd) == 0 or die "Something wrong with '$cmd'\n";
    }
}

