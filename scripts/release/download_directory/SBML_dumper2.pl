#!/usr/local/bin/perl  -w

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

use strict;
use Getopt::Long;
use GKB::Config;

if (-e $LIBSBML_LD_LIBRARY_PATH) {
	my $ld_library_path = $ENV{'LD_LIBRARY_PATH'};
	if (!(defined $ld_library_path) || $ld_library_path eq "") {
		$ld_library_path = $LIBSBML_LD_LIBRARY_PATH;
	} else {
		$ld_library_path = "$LIBSBML_LD_LIBRARY_PATH:$ld_library_path";
	}
	$ENV{'LD_LIBRARY_PATH'} = $ld_library_path;
} else {
	print STDERR "libSBML library path: $LIBSBML_LD_LIBRARY_PATH does not exist!";
	exit(1);
}

my $gkb = $GK_ROOT_DIR;
my $reactome_gwt = "$gkb/ReactomeGWT";
my $libs = "$reactome_gwt/libs";
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

system($command) == 0 or die "$command failed";