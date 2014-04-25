#!/usr/local/bin/perl -w

# Complies, builds and deploys the various bits needed by Reactome v3.x for
# analyses and website.

# I think it's more user-friendly if the user will not have to practise any
# symlink tricks. Hence the BEGIN block. If the location of the script or
# libraries changes, this will have to be changed.

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
use Cwd;
use Getopt::Long;
use GKB::Config;

# Get database parameters from command line, if present - these will
# be needed when cached data is generated.
our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db,$opt_debug,$opt_no_cached_data,$opt_no_dn_cleanup,$opt_no_images,$opt_ids,$opt_sp,$opt_restart,$opt_no_deploy,$opt_url,$opt_no_cached_gwt_data);
&GetOptions("user:s","host:s","pass:s","port:i","db=s","debug","no_cached_data","no_dn_cleanup","no_images","ids:s","sp:s","restart","no_deploy","url=s","no_cached_gwt_data");

my $deploy = 1;
if (defined $opt_no_deploy) {
	$deploy = 0;
}
if (!(defined $opt_host)) {
	$opt_host = $GK_DB_HOST;
}
if (!(defined $opt_db)) {
	$opt_db = $GK_DB_NAME;
}
if (!(defined $opt_user)) {
	$opt_user = $GK_DB_USER;
}
if (!(defined $opt_pass)) {
	$opt_pass = $GK_DB_PASS;
}
if (!(defined $opt_port)) {
	$opt_port = $GK_DB_PORT;
}
if (!(defined $opt_url) || $opt_url eq "") {
	$opt_url = "http://localhost";
}

# Find the root directory of the GKB hierarchy
my $gk_root_dir = $GK_ROOT_DIR || die "GKB directory does not exist";

my $servlet_container_deploy_dir = $SERVLET_CONTAINER_DEPLOY_DIR;
if (!(defined $servlet_container_deploy_dir)) {
	print STDERR "SERVLET_CONTAINER_DEPLOY_DIR is undef, I dont know where to deploy the servlets to.  Modify Config.pm to set this.";
	exit(1);
}
my $servlet_container_bin_dir = $servlet_container_deploy_dir;
$servlet_container_bin_dir =~ s/webapps/bin/;

if ($opt_restart) {
	# Shut down Tomcat server
	run("cd $servlet_container_bin_dir; sudo ./shutdown.sh");
}

my $options = "";
if (defined $opt_host) {
	$options .= "-host $opt_host ";
}
if (defined $opt_db) {
	$options .= "-db $opt_db ";
}
if (defined $opt_user) {
	$options .= "-user $opt_user ";
}
if (defined $opt_pass) {
	$options .= "-pass $opt_pass ";
}
if (defined $opt_port) {
	$options .= "-port $opt_port ";
}
if (defined $opt_no_cached_data) {
	$options .= "-no_cached_data ";
}
if (defined $opt_no_dn_cleanup) {
	$options .= "-no_dn_cleanup ";
}
if (defined $opt_no_images) {
	$options .= "-no_images ";
}
if (defined $opt_ids) {
	$options .= "-ids $opt_ids ";
}
if (defined $opt_sp) {
	$options .= "-sp $opt_sp ";
}
$options =~ s/ *$//;
	
# Prepare new instances
run("cd $gk_root_dir/Site/Web2; ./clean.sh $gk_root_dir $opt_db $opt_url");
run("cd $gk_root_dir/Site/Web2; ./update_web_xml.sh $gk_root_dir $opt_db $opt_url");
run("cd $gk_root_dir/Site/Web3; ./clean.sh $gk_root_dir $opt_db $opt_url");
run("cd $gk_root_dir/Site/Web3; ./update_web_xml.sh $gk_root_dir $opt_db $opt_url");

# Build
run("cd $gk_root_dir/Site; mvn clean install -Dexec.args=\"$options\"");

if ($deploy) {
	# Deploy new instances
	run("cd $gk_root_dir/Site/Web2; ./deploy.sh $servlet_container_deploy_dir");
	run("cd $gk_root_dir/Site/Web3; ./deploy.sh $servlet_container_deploy_dir");
}

if (!(defined $opt_no_images) || !$opt_no_images) {
	run("cd $gk_root_dir/scripts/release/gwt; ./generate_pathway_of_the_month_images.pl $options");
}

if ($opt_restart) {
	# Re-start Tomcat server
	run("cd $servlet_container_bin_dir; sudo ./startup.sh");
}

sub run {
	my ($command) = @_;
	print STDERR "command: $command\n";
	my $status = system($command);

	if ($status != 0) {
        	print STDERR "Nonzero exit status for: $command\n";
        	exit(1);
	}
}
