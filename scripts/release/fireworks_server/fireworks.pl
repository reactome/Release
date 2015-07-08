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

use autodie qw/:all/;
use Cwd;
use Getopt::Long;
use common::sense;

use Log::Log4perl qw/get_logger/;
Log::Log4perl->init(\$LOG_CONF);
my $logger = get_logger(__PACKAGE__);


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

my $tmp_dir = "/tmp";
my $present_dir = getcwd();

chdir $tmp_dir;
system("git clone https://github.com/reactome/Fireworks");
system("rm -rf $present_dir/fireworks; ln -s Fireworks $present_dir/fireworks");

#chdir "$present_dir/fireworks/Server";
#system("mvn clean package");
#system("mv target/Reactome-Fireworks-Layout-jar-with-dependencies.jar fireworks.jar");

chdir $present_dir;
my $fireworks_package = "java -jar -Xms5120M -Xmx10240M fireworks.jar";
my $credentials = "-d $opt_db -u $opt_user -p $opt_pass";
my $reactome_graph_binary = "$present_dir/ReactomeGraphs.bin";
system("$fireworks_package GRAPH -s $present_dir/../analysis_core/analysis_v$opt_r.bin -o $reactome_graph_binary --verbose");
my $json_dir = "$present_dir/json";
system("$fireworks_package LAYOUT $credentials -g $reactome_graph_binary -f $present_dir/fireworks/Server/config -o $json_dir");

chdir $tmp_dir;
system("rm -rf Fireworks");

$logger->info("$0 has finished its job\n");
