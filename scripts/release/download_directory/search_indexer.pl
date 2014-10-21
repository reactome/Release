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


my $solr_url = `hostname -f`;
chomp $solr_url;
$solr_url .= ":7080/solr";

my $present_dir = getcwd();
my $output = "$present_dir/ebeye.xml";
chdir "$GK_ROOT_DIR/scripts/release/download_directory/search/indexer";
system("mvn clean package");
system("mv target/Indexer-1.0-jar-with-dependencies.jar indexer.jar");
system("java -jar -Xms5120M -Xmx10240M indexer.jar -d $opt_db -u $opt_user -p $opt_pass -s $solr_url -c src/main/resources/controlledvocabulary.csv -o $output -r $opt_r");
system("gzip $output");

print "$0 has finished its job\n";