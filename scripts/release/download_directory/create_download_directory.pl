#!/usr/local/bin/perl  -w
use strict;

# I think it's more user-friendly if the user will not have to practise any
# symlink tricks. Hence the BEGIN block. If the location of the script or
# libraries changes, this will have to be changed.

BEGIN {
    my @a = split('/',$0);
    pop @a;
    push @a, ('..','..','..');
    my $libpath = join('/', @a);
    unshift (@INC, "$libpath/modules");
    $ENV{PATH} = "$libpath/scripts:$libpath/scripts/release:" . $ENV{PATH};
}

use GKB::Config;
use GKB::DBAdaptor;

use autodie;
use Cwd;
use Getopt::Long;
use DBI;

$GKB::Config::NO_SCHEMA_VALIDITY_CHECK = undef;

our($opt_host,$opt_db,$opt_pass,$opt_port,$opt_debug,$opt_user,$opt_r,$opt_sp);

my $usage = "Usage: $0 -db db_name -user db_user -host db_host -pass db_pass -port db_port -r release_num -sp species\nA release number is mandatory.";

&GetOptions("user:s",
"host:s",
"pass:s",
"port:i",
"debug",
"db=s",
"r:i",
"sp=s");

$opt_r || die $usage;

my $release_nr = $opt_r;
$opt_db = $opt_db || "test_reactome_$release_nr";
my $db = $opt_db;

unless (-e $release_nr) {
    mkdir($release_nr) || die "Couldn't make '$release_nr': $!";
}

if (!(defined $opt_host) || $opt_host eq '') {
	$opt_host = $GK_DB_HOST;
}
if (!(defined $opt_user) || $opt_user eq '') {
	$opt_user = $GK_DB_USER;
}
if (!(defined $opt_pass) || $opt_pass eq '') {
	$opt_pass = $GK_DB_PASS;
}
if (!(defined $opt_port) || $opt_port eq '') {
	$opt_port = $GK_DB_PORT;
}


# Check for presence of optional databases
$opt_host ||= 'localhost';
my $dsn = "DBI:mysql:host=$opt_host;port=$opt_port";
my $dbh = eval { DBI->connect($dsn,$opt_user,$opt_pass, {RaiseError => 1}); };

my ($sth,@row);
my $exists_stable_identifier_db = 0;
if (defined $GK_IDB_NAME && !($GK_IDB_NAME eq '')) {
	$sth = $dbh->prepare("SHOW DATABASES LIKE '$GK_IDB_NAME'");
	$sth->execute();
	@row = $sth->fetchrow_array();
	$exists_stable_identifier_db = scalar(@row);
}

eval {
	my $dba = GKB::DBAdaptor->new
	    (
	     -user   => $opt_user,
	     -host   => $opt_host,
	     -pass   => $opt_pass,
	     -port   => $opt_port,
	     -dbname => $opt_db,
	     -DEBUG => $opt_debug
	     );
};
if ($@) {
    die "Problems connecting to db:\n$@\n";
}

# Pre-create the command line options associated with database access
my $reactome_db_options = "-db $db";
my $fetch_empty_project_db_options = "-db $db";
my $create_ebeye_db_options = "";
if (defined $GK_IDB_NAME && !($GK_IDB_NAME eq '')) {
	$create_ebeye_db_options = "-db $GK_IDB_NAME";
}
my $biopaxexporter_db_options = "";
my $reactome_to_msig_export_db_options = "";
my $diagram_dump_options = "";
my $mysqldump_db_options = $db;
my $mysqldump_dn_db_options = "$db\_dn";
my $mysqldump_mart_db_options = "test_reactome_mart";
my $mysqldump_identifier_db_options = "test_reactome_stable_identifiers";
my $mysqldump_wordpress_db_options = "wordpress";

if (defined $opt_host && !($opt_host eq '')) {
    $reactome_db_options .= " -host $opt_host";
    $fetch_empty_project_db_options .= " -host $opt_host";
    $create_ebeye_db_options .= " -host $opt_host";
    $biopaxexporter_db_options .= $opt_host;
    $reactome_to_msig_export_db_options .= $opt_host;
    $diagram_dump_options .= $opt_host;
    $mysqldump_db_options .= " -h $opt_host";
    $mysqldump_mart_db_options .= " -h $opt_host";
    $mysqldump_dn_db_options .= " -h $opt_host";
    $mysqldump_identifier_db_options .= " -h $opt_host";
    $mysqldump_wordpress_db_options .= " -h $opt_host";
}

$biopaxexporter_db_options .= " $db";
$reactome_to_msig_export_db_options .= " $db";
$diagram_dump_options .= " $db";

if (defined $opt_user && !($opt_user eq '')) {
    $reactome_db_options .= " -user $opt_user";
    $fetch_empty_project_db_options .= " -user $opt_user";
    $create_ebeye_db_options .= " -user $opt_user";
    $biopaxexporter_db_options .= " $opt_user";
    $reactome_to_msig_export_db_options .= " $opt_user";
    $diagram_dump_options .= " $opt_user";
    $mysqldump_db_options .= " -u $opt_user";
    $mysqldump_dn_db_options .= " -u $opt_user";
    $mysqldump_mart_db_options .= " -u $opt_user";
    $mysqldump_identifier_db_options .= " -u $opt_user";
    $mysqldump_wordpress_db_options .= " -u $opt_user";
}

if (defined $opt_pass && !($opt_pass eq '')) {
	# Put a backslash in front of characters that have special meaning to the shell
	my $pass = $opt_pass;
	if ($pass =~ /\$/) {
		$pass =~ s/\$/\\\$/g;
	}
	$reactome_db_options .= " -pass '$pass'";
	$fetch_empty_project_db_options .= " -pass '$pass'";
	$create_ebeye_db_options .= " -pass '$pass'";
	$biopaxexporter_db_options .= " '$pass'";
	$reactome_to_msig_export_db_options .= " '$pass'";
	$diagram_dump_options .= " '$pass'";
	$mysqldump_db_options .= " -p$pass";
	$mysqldump_dn_db_options .= " -p$pass";
	$mysqldump_mart_db_options .= " -p$pass";
	$mysqldump_identifier_db_options .= " -p$pass";
	$mysqldump_wordpress_db_options .= " -p$pass";
}
if (defined $opt_port && !($opt_port eq '')) {
	$reactome_db_options .= " -port $opt_port";
	$fetch_empty_project_db_options .= " -port $opt_port";
	$create_ebeye_db_options .= " -port $opt_port";
	$biopaxexporter_db_options .= " $opt_port";
	$reactome_to_msig_export_db_options .= " $opt_port";
	$diagram_dump_options .= " $opt_port";
	$mysqldump_db_options .= " -P $opt_port";
	$mysqldump_dn_db_options .= " -P $opt_port";
	$mysqldump_mart_db_options .= " -P $opt_port";
	$mysqldump_identifier_db_options .= " -P $opt_port";
	$mysqldump_wordpress_db_options .= " -P $opt_port"
}

$biopaxexporter_db_options .= " $release_nr";

my $reactome_to_biosystems_db_options = $reactome_to_msig_export_db_options;
my $reactome_to_msig_export_db_filename = "ReactomePathways.gmt";
$reactome_to_msig_export_db_options .= " $reactome_to_msig_export_db_filename";
my $diagram_dump_filename = "diagrams";
$diagram_dump_options .= " $diagram_dump_filename";

my $sbml2_species = "48887";
if (!(defined $opt_sp) || $opt_sp eq '') {
	$opt_sp = 'Homo sapiens';
} else {
	$sbml2_species = $opt_sp;
}
my $species_file_stem = lc($opt_sp);
$species_file_stem =~ s/ +/_/g;

print "mysqldump_db_options=$mysqldump_db_options\n";
print "reactome_db_options=$reactome_db_options\n";
print "opt_sp=$opt_sp\n";

# For logging purposes, code groups will be reported in an array:
# [label, STDERR[BOOL], STDOUT[BOOL], command(s)[may be a list of commands]]
# [eg: 'foo_script', 1, 1, "perl fool.pl"]
# log files foo_script.err, foo_script.out
my @cmds = (
    [
     "$species_file_stem.interactions.stid", 
     1, 
     0, # set to 0 if STDOUT is redirected in command
     "perl report_interactions.pl $reactome_db_options -sp '$opt_sp' ".
     "| sort | uniq | gzip -c > $release_nr/$species_file_stem.interactions.stid.txt.gz"
    ],

    [
     "$species_file_stem.interactions.intact",
     1,
     0,
     "perl report_interactions.pl $reactome_db_options -sp '$opt_sp' -col_grps ids,context,".
     "source_ids,source_st_ids,participating_protein_count,lit_refs,intact -headers title,table | ".
     "sort | uniq | gzip -c > $release_nr/$species_file_stem.interactions.intact.txt.gz"
    ],

    [
     "$species_file_stem.mitab.interactions",
     1,
     0,
     "perl report_interactions.pl $reactome_db_options -sp '$opt_sp' -mitab | ".
     "gzip -c > $release_nr/$species_file_stem.mitab.interactions.txt.gz"
    ],

    [
     "database_dumps",
     1,
     0,
     "mkdir $release_nr/databases",
     "mysqldump --opt $mysqldump_db_options | gzip -c > $release_nr/databases/gk_current.sql.gz",
     "mysqldump --opt $mysqldump_identifier_db_options | gzip -c > $release_nr/databases/gk_stable_ids.sql.gz",
     "mysqldump --opt $mysqldump_wordpress_db_options | gzip -c > $release_nr/databases/gk_wordpress.sql.gz",
     "mysqldump --opt $mysqldump_dn_db_options | gzip -c > $release_nr/databases/gk_current_dn.sql.gz",
    ],

    [
     "release_tarball",
     1,
     1,
     "./make_release_tarball.pl $release_nr"
    ],

    [
     "SBML_dumpers",
     1,
     0,
     "perl SBML_dumper.pl $reactome_db_options -sp '$opt_sp' | gzip -c > $release_nr/$species_file_stem.sbml.gz",
     "perl SBML_dumper2.pl $reactome_db_options -sp '$sbml2_species' | gzip -c > $release_nr/$species_file_stem.2.sbml.gz",
    ],

    [
     "interactions_for_all_species",
     1,
     1,
     "perl interactions_for_all_species.pl -outputdir $release_nr $reactome_db_options"
    ],

    [
    "psicquic_indexers",
     1,
     1,
     "perl psicquic_indexers.pl -release $release_nr"
    ],

    [
     "gene_association.reactome",
     1,
     0,
     "cp ../goa_prepare/GO_submission/go/gene-associations/submission/gene_association.reactome ".
     "$release_nr/gene_association.reactome",
    ],

    [
     "runDiagramDumper",
     1,
     1,
    "cd WebELVTool",
    "rm -f $diagram_dump_filename/PNG/*",
    "rm -f $diagram_dump_filename/PDF/*",
    "./runDiagramDumper.sh $diagram_dump_options",
    "cd $diagram_dump_filename",
    "rm -f *.zip",
    "zip -r diagrams.pdf.zip PDF",
    "zip -r diagrams.png.zip PNG",
    "mv *.zip ../download_directory/$release_nr",
    ],

    [
     "fetch_and_print_values",
     1,
     0,
     qq(perl fetch_and_print_values.pl -query "[['inferredFrom','IS NULL',[]]]" ).
     qq(-class Complex $reactome_db_options -output DB_ID -output 'species.name[0]' ).
     qq(-output _displayName > $release_nr/curated_complexes.txt 2> fetch_and_print_values.err),
    
     qq(perl fetch_and_print_values.pl -query "[['inferredFrom','IS NULL',[]]]" ).
     qq(-class Complex $reactome_db_options -output 'stableIdentifier._displayName' -output ).
     qq('species.name[0]' -output _displayName > $release_nr/curated_complexes.stid.txt)
    ],

    [
     "run_biopax",
     1,
     1,
    "./run_biopax.pl $biopaxexporter_db_options",
    ],
    
    [
     "runGSEAOutput",
     1,
     1,
     "cd WebELVTool",
     "./runGSEAOutput.sh $reactome_to_msig_export_db_options",
     "zip $reactome_to_msig_export_db_filename.zip $reactome_to_msig_export_db_filename",
     "cd -",
     "mv WebELVTool/$reactome_to_msig_export_db_filename.zip $release_nr"
    ],
    
    [
     "generate_packaged_pathway_diagrams",
     1,
     2,
    "./generate_packaged_pathway_diagrams.sh $diagram_dump_options"
    ],
    
    [
     "TheReactomeBook - pdf version",
     1,
     0,    
     "perl genbook_pdf.pl -depth 100 $reactome_db_options -stdout -react_rep 2 > TheReactomeBook.pdf",
     "zip TheReactomeBook.pdf.zip TheReactomeBook.pdf",
     "rm TheReactomeBook.pdf",
     "mv TheReactomeBook.pdf.zip $release_nr"
    ],
    
    [
     "TheReactomeBook - rtf version",
     1,
     1,
     "perl genbook_rtf.pl -depth 100 $reactome_db_options -split -react_rep 2",
     "zip -r TheReactomeBook.rtf.zip TheReactomeBook",
     "rm -rf TheReactomeBook",
     "mv TheReactomeBook.rtf.zip $release_nr",
    ],
    
    [
     "fetchEmptyProject",
     1,
     1,
     "perl fetchEmptyProject.pl reactome_data_model -outputdir $release_nr $fetch_empty_project_db_options"
    ],
    
    [
     "Search Indexer",
     1,
     1,
     "cd $GK_ROOT_DIR",
     "git subtree pull --prefix scripts/release/download_directory/search --squash search master",
     "cd -",
     "perl search_indexer.pl -release $release_nr $reactome_db_options",
     "mv ebeye.xml.gz $release_nr"
    ],
    
    [
     "Analysis Core",
     1,
     1,
     "cd $GK_ROOT_DIR",
     "git subtree pull --prefix scripts/release/download_directory/analysis --squash analysis master",
     "cd -",
     "perl analysis_core.pl -release $release_nr $reactome_db_options",
     "mv *.txt $release_nr"
    ]
);


my $broken_command_counter = 0;
foreach my $cmd (@cmds) {
    $broken_command_counter += run($cmd);
}

if ($broken_command_counter > 0) {
    print STDERR "$broken_command_counter commands failed, please check the above printout to diagnose the problems\n";
}

print "create_download_directory.pl has finished its job\n";

sub run {
    my $cmd = shift;
    my ($label,$stderr,$stdout,@commands) = @$cmd;
    my $redirect = '';

    my $log = "${label}.out"  if $stdout;
    my $elog = "${label}.err" if $stderr;
    unlink $log  if $log  && -e $log;
    unlink $elog if $elog && -e $elog;

    $redirect = ">> $log" if $log;
    $redirect .= " 2>> $elog" if $elog;

    print STDERR "\nExecuting task $label\n";
    my $not_good = 0;

    # create the list of commands with individual logging 
    my $command = join(';', map {"$_ $redirect"} @commands);
		       
    print STDERR "Executing: ", hide_password($command), "\n";

    my $retval = system $command; 
    if ($retval) {
	print STDERR "WARNING - something went wrong while executing '" . hide_password($command) . " ($!)'!!\n";
	$not_good++;
    }
    else {
	print STDERR "Success!\n";
    }

    if ($log || $elog) {
	my $files = $log && $elog ? 'files are' : 'file is';
	$files .= $log && $elog ? " $log and $elog." : $log ? " $log." : " $elog.";
	print STDERR "The log $files\n"; 
    }

    return $not_good;
}

sub hide_password {
    my @input = @_;
    
    my $asterisks = '*' x length $opt_pass;
    
    s/$opt_pass/$asterisks/ foreach @input;
    
    return $input[0] if (scalar @input == 1);
    return @input;
}
