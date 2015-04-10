#!/usr/local/bin/perl  -w
use strict;

# Generate SBML files for the given Reactome database
#
# Takes the ususal database-related parameters on the command line.
# Additionally takes the following command line parameters:
#
# -sp	limiting species (defaults to all species without this option)

BEGIN {
    my @a = split('/',$0);
    pop @a;
    push @a, ('..','..','modules');
    my $libpath = join('/', @a);
    unshift (@INC, $libpath);
}
# Set the umask so that the files are group writeable
umask(002);

use Getopt::Long;
use GKB::Config;
use Log::Log4perl qw/get_logger/;
Log::Log4perl->init(\$LOG_CONF);
my $logger = get_logger(__PACKAGE__);

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

my $command = "java -classpath $classpath org.gk.sbml.SBMLAndLayoutBuilderCommandLine @ARGV";

system($command) == 0 or $logger->error_die("$command failed");