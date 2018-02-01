#!/usr/local/bin/perl  -w
use strict;

# Generate SBML files for the given Reactome database
#
# Takes the ususal database-related parameters on the command line.
# Additionally takes the following command line parameters:
#
# -sp	limiting species (defaults to all species without this option)

# Set the umask so that the files are group writeable
umask(002);

use lib '/usr/local/gkb/modules';

use autodie;
use Getopt::Long;
use GKB::Config;
use GKB::DBAdaptor;

use Log::Log4perl qw/get_logger/;
Log::Log4perl->init(\$LOG_CONF);
my $logger = get_logger(__PACKAGE__);

my ($user, $host, $pass, $port, $db, $species, $sbgn_output_dir);
GetOptions(
    "user=s" => \$user,
    "host=s" => \$host,
    "pass=s" => \$pass,
    "port=i" => \$port,
    "db=s" => \$db,
    "sp=s" => \$species,
    "output_dir=s" => \$sbgn_output_dir
);

$user ||= $GKB::Config::GK_DB_USER;
$host ||= $GKB::Config::GK_DB_HOST;
$pass ||= $GKB::Config::GK_DB_PASS;
$port ||= $GKB::Config::GK_DB_PORT;
$db ||= $GKB::Config::GK_DB_NAME;
$sbgn_output_dir ||= 'sbgn';

if (-e $LIBSBML_LD_LIBRARY_PATH) {
    my $ld_library_path = $ENV{'LD_LIBRARY_PATH'};
    if (!(defined $ld_library_path) || $ld_library_path eq "") {
	$ld_library_path = $LIBSBML_LD_LIBRARY_PATH;
    } else {
	$ld_library_path = "$LIBSBML_LD_LIBRARY_PATH:$ld_library_path";
    }
    $ENV{'LD_LIBRARY_PATH'} = $ld_library_path;
} else {
    $logger->error("libSBML library path: $LIBSBML_LD_LIBRARY_PATH does not exist!");
    exit(1);
}

my $libs = $GK_ROOT_DIR . "/java/libs";
my $sbml_libs = "$libs/sbml";
my $default = "$libs/default";
my $classpath = "$default/reactome.jar";

opendir my($dh), $sbml_libs or die "Couldn't open dir '$sbml_libs': $!";
my @files = readdir $dh;
closedir $dh;

# JAR file order is important, because JSBML is present in at least 2 of the
# JAR files and we want to get the most up-to-date version.
foreach my $file (sort(@files)) {
    if ($file eq "reactome.jar") {
	next;
    }
    if (!($file =~ /\.jar$/)) {
	next;
    }
    
    $classpath .= ":$sbml_libs/$file";
#	print STDERR "Adding $sbml_libs/$file to classpath\n";
}

#print STDERR "classpath=$classpath\n";
my $dba = GKB::DBAdaptor->new(
    -user => $user,
    -pass => $pass,
    -host => $host,
    -port => $port,
    -dbname => $db
);

my $species_instance = $species ? $dba->fetch_instance_by_db_id($species)->[0] : undef;
system("mkdir -p $sbgn_output_dir");

my @pathways = @{$dba->fetch_instance(-CLASS => 'Pathway')};

foreach my $pathway (@pathways) {
    next if $species_instance && $species_instance->db_id != $pathway->species->[0]->db_id;
    my $pathway_id = $pathway->db_id;
    my $outfile = "$sbgn_output_dir/" . trim($pathway->name->[0]) . '.sbgn';
    
    my $command = qq(java -classpath $classpath org.gk.sbgn.SBGNBuilderCommandLine -user $user -pass $pass -host $host -db $db -port $port -sp $species -pid $pathway_id -o "$outfile");
    system($command) == 0 or $logger->warn("$command failed");
    remove_if_empty_sbgn_file($outfile) if (-e $outfile);
}

sub trim {
    my $string = shift;
    
    $string =~ s/^\s+//;
    $string =~ s/\s+$//;
    
    return $string;
}

sub remove_if_empty_sbgn_file {
    my $file = shift;
    
    my $logger = get_logger(__PACKAGE__);
    
    open(my $fh, '<', $file);
    my $contents = join('', <$fh>);
    close $fh;
    
    unless ($contents && $contents =~ /<\/sbgn>/) {
	system("rm \"$file\"");
	$logger->info("$file removed");
    }
}