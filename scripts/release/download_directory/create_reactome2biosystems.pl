#!/usr/local/bin/perl  -w

# This is just an extract from another script, create_download_directory.pl. Since
# otherIdentifier attribute is used for reactome to biosystems dump, but this attribute
# is filled very after parallelization, so this dump has to be executed after running
# "Create skypainter db" (Step 083), which is used to fill this attribute.

# I think it's more user-friendly if the user will not have to practise any
# symlink tricks. Hence the BEGIN block. If the location of the script or
# libraries changes, this will have to be changed.

BEGIN {
    my @a = split('/',$0);
    pop @a;
    push @a, ('..','..');
    my $libpath = join('/', @a);
    unshift (@INC, "$libpath/modules");
    $ENV{PATH} = "$libpath/scripts:$libpath/scripts/release:" . $ENV{PATH};
}

use GKB::Config;
use GKB::DBAdaptor;
use strict;
use Cwd;
use Getopt::Long;
use DBI;
$GKB::Config::NO_SCHEMA_VALIDITY_CHECK = undef;

our($opt_host,$opt_db,$opt_pass,$opt_port,$opt_debug,$opt_user,$opt_r,$opt_sp);

my $usage = "Usage: $0 -db db_name -user db_user -host db_host -pass db_pass -port db_port -r release_num -sp species\nA release number is mandatory.";

&GetOptions("user:s", "host:s", "pass:s", "port:i", "debug", "db=s", "r:i", "sp=s");

$opt_r || die $usage;

my $release_nr = $opt_r;
$opt_db = $opt_db || "test_reactome_$release_nr";
my $db = $opt_db;

unless (-e $release_nr) {
    mkdir $release_nr;
}
chdir($release_nr) || die "Couldn't cd to $release_nr: $!";

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
my $reactome_to_msig_export_db_options = "";
if (defined $opt_host && !($opt_host eq '')) {
	$reactome_to_msig_export_db_options .= $opt_host;
}
$reactome_to_msig_export_db_options .= " $db";
if (defined $opt_user && !($opt_user eq '')) {
	$reactome_to_msig_export_db_options .= " $opt_user";
}
if (defined $opt_pass && !($opt_pass eq '')) {
	# Put a backslash in front of characters that have special meaning to the shell
	my $pass = $opt_pass;
	if ($pass =~ /\$/) {
		$pass =~ s/\$/\\\$/g;
	}
	$reactome_to_msig_export_db_options .= " '$pass'";
}
if (defined $opt_port && !($opt_port eq '')) {
	$reactome_to_msig_export_db_options .= " $opt_port";
}

my $reactome_to_biosystems_db_options = $reactome_to_msig_export_db_options;

my @cmds = (
    "cd $GK_ROOT_DIR/WebELVTool && ./runReactomeToBioSystems.sh $reactome_to_biosystems_db_options BioSystems && cd - && mv $GK_ROOT_DIR/WebELVTool/BioSystems/ReactomeToBioSystems.zip .",
);

my $broken_command_counter = 0;
foreach my $cmd (@cmds) {
    print STDERR "cmd=$cmd\n";
    if (system($cmd) != 0) {
    	print STDERR "WARNING - something went wrong while executing '$cmd'!!\n";
    	$broken_command_counter++;
    }
}

if ($broken_command_counter > 0) {
    print STDERR "$broken_command_counter commands failed, please check the above printout to diagnose the problems\n";
}

print "create_reactome2biosystems.pl has finished its job\n";

