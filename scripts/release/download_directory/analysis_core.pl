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

use autodie;
use Cwd;
use Getopt::Long;
use common::sense;

use constant TEMP => '/usr/local/reactomes/Reactome/production/AnalysisService/temp';


$GKB::Config::NO_SCHEMA_VALIDITY_CHECK = undef;

our($opt_host,$opt_db,$opt_pass,$opt_port,$opt_debug,$opt_user,$opt_r);

my $usage = "Usage: $0 -db db_name -user db_user -host db_host -pass db_pass -port db_port -r release_number";

&GetOptions("user:s",
"host:s",
"pass:s",
"port:i",
"debug",
"db=s",
"r:i");

$opt_db || die $usage;
$opt_r || die $usage;
	
$opt_host ||= $GK_DB_HOST;
$opt_user ||= $GK_DB_USER;
$opt_pass ||= $GK_DB_PASS;
$opt_port ||= $GK_DB_PORT;


my $present_dir = getcwd();

chdir "$GK_ROOT_DIR/scripts/release/download_directory/analysis/Core";
system("mvn clean package -PAnalysis-Core-Command-Line");
system("mv target/tools-jar-with-dependencies.jar analysis_core.jar");
my $analysis_core = "java -jar -Xms5120M -Xmx10240M analysis_core.jar";
my $credentials = "-d $opt_db -u $opt_user -p $opt_pass";
system("$analysis_core build $credentials -o $present_dir/analysis_v$opt_r.bin");

foreach my $resource (qw/UniProt ChEBI Ensembl miRBase/) {
    my $export = "$analysis_core export $credentials -i $present_dir/analysis_v$opt_r.bin";
    system("$export -r $resource -o $present_dir/$resource"."2Reactome.txt");
    system("$export -r $resource -o $present_dir/$resource"."2Reactome_All_Levels.txt --all");
}

my %hierarchy = (details => "ReactomePathways.txt", relationship => "ReactomePathwaysRelation.txt");
while(my ($type, $output) = each %hierarchy) {
    system("$analysis_core hierarchy -t $type -i $present_dir/analysis_v$opt_r.bin -o $present_dir/$output");
}

my $tempfiles = TEMP.'/*';
say "Removing analysis temp files...";
system "rm -f $tempfiles";

print "$0 has finished its job\n";
