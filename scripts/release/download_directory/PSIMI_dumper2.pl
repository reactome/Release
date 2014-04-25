#!/usr/local/bin/perl  -w

# Generate PSIMI files for the given Reactome database
#
# Takes the ususal database-related parameters on the command line.
# Additionally takes the following command line parameters:
#
# -sp	limiting species (defaults to all species without this option)
# -outputdir directory for script diagnostics output file (defaults to current directory) 

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
use autodie;
use Getopt::Long;
use GKB::Config;

our ($opt_outputdir);
&GetOptions("outputdir:s");
$opt_outputdir ||= '.';

open(my $diagnostics, '>', "$opt_outputdir/PSIMI_dumper2.out");

print $diagnostics "PSIMI_dumper2.pl has started\n";

my $gkb = $GK_ROOT_DIR;
my $reactome_gwt = "$gkb/ReactomeGWT";
my $libs = "$reactome_gwt/libs";
my $sbml_libs = "$libs/sbml";
my $psicquic_libs = "$libs/psicquic";
my $default = "$libs/default";
my $classpath = "$default/reactome.jar";

opendir my($dh), $sbml_libs or die "Couldn't open dir '$sbml_libs': $!";
my @sbml_files = readdir $dh;
closedir $dh;

opendir my($pdh), $psicquic_libs or die "Couldn't open dir '$psicquic_libs': $!";
my @psicquic_files = readdir $pdh;
closedir $pdh;

print $diagnostics "psicquic_files=@psicquic_files\n";
# JAR file order is important, because JPSIMI is present in at least 2 of the
# JAR files and we want to get the most up-to-date version.
foreach my $file (sort(@sbml_files)) {
	if ($file eq "reactome.jar") {
		next;
	}
	if (!($file =~ /\.jar$/)) {
		next;
	}
	
	$classpath .= ":$sbml_libs/$file";
	print $diagnostics "Adding $sbml_libs/$file to classpath\n";
}
foreach my $file (sort(@psicquic_files)) {
	if ($file eq "reactome.jar") {
		next;
	}
	if (!($file =~ /\.jar$/)) {
		next;
	}
	
	$classpath .= ":$psicquic_libs/$file";
	print $diagnostics "Adding $psicquic_libs/$file to classpath\n";
}

print $diagnostics "classpath=$classpath\n";

#my $command = "java -classpath $classpath org.gk.psimixml.PSIMIBuilderCommandLine @ARGV";
#my $command = "java -Xmx6144m -classpath $classpath org.gk.psimixml.PSIMIBuilderCommandLine @ARGV";
my $command = "java -Xmx8000M -classpath $classpath org.gk.psimixml.PSIMIBuilderCommandLine @ARGV";

print $diagnostics "command=$command\n";

system($command) == 0 || die "$command failed: $!";

print $diagnostics "PSIMI_dumper2.pl has finished\n";

close $diagnostics;
