#!/usr/local/bin/perl  -w

# Generates images for use in the new website's "pathway of the month" section.
# These will be PNGs and will be put into the same directory as the regular images
# for the top level pathways, and use the same filesnames, except there will be
# the extension "_pom" at the end.  Note that "pom" means "pathway of the month",
# not "English".
#
# Takes the ususal database-related parameters on the command line.
# Additionally takes the following command line parameters:
#
# -width		Image width in pixels; defaults to 300

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
use GKB::DBAdaptor;
use GKB::Config;
use GKB::WebUtils;
use Getopt::Long;
use strict;
use Cwd;

@ARGV || die <<__END__;

Usage: $0 -user db_user -host db_host -pass db_pass -port db_port -db db_name -width <image width px>
__END__

our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db,$opt_debug,$opt_width);

# Parse commandline
&GetOptions("user:s","host:s","pass:s","port:i","db=s","debug","width=s",);
$opt_db || die "Need database name (-db).\n";
$opt_width || ($opt_width = 300);

my $dba = GKB::DBAdaptor->new
    (
     -user   => $opt_user,
     -host   => $opt_host,
     -pass   => $opt_pass,
     -port   => $opt_port,
     -dbname => $opt_db,
     -DEBUG => $opt_debug
     );

# Find the root directory of the GKB hierarchy
my $gk_root_dir = $GK_ROOT_DIR || "GKB directory does not exist";

my $web_utils = GKB::WebUtils->new(-DBA => $dba, -URLMAKER => ' ');
my $command;
my $pathways = $dba->fetch_all_class_instances_as_shells('Pathway');
my $pathway;
my $figures;
my $figure;
my $urls;
my $url;
my $path;
my $pom_path;
my $pathway_with_elv_count = 0;
my $top_level_pathway_count = 0;
my $new_pathway_diagram_count = 0;
my $status;
foreach $pathway (@{$pathways}) {
	if (!(defined $pathway)) {
		next;
	}
	$pathway->inflate();
	# Assume that pathways with diagrams are top level.
	if (!($web_utils->has_diagram_instance($pathway))) {
		next;
	}
	$pathway_with_elv_count++;
	
	$figures = $pathway->figure;
	if (!(defined $figures) || scalar(@{$figures}) < 1) {
		next;
	}
	$figure = $figures->[0];
	if (!(defined $figure)) {
		print STDERR "$0: WARNING - figure is undef!\n";
		next;
	}
	$urls = $figure->url;
	if (!(defined $urls) || scalar(@{$urls}) < 1) {
		next;
	}
	$url = $urls->[0];
	if (!(defined $url)) {
		print STDERR "$0: WARNING - URL is undef!\n";
		next;
	}
	
	$path = $url;
	$path =~ s/^.*figures\///;
	$path = "$gk_root_dir/website/images/$path";
	if (!(-e $path)) {
		print STDERR "$0: WARNING - $path does not exist!\n";
		next;
	}
	
	print STDERR "$0: path=$path\n";

	$top_level_pathway_count++;
	
	$pom_path = $path;
	$pom_path =~ s/\.[a-zA-Z]+$/_pom.png/;
	if (-e $pom_path) {
		print STDERR "$0: POM file already exists: $pom_path\n";
		next;
	}
	
	$command = "identify $path|";
	my $width = 0;
	my $height = 0;
	eval {
		if (!open(COM, $command)) {
			print STDERR "$0: WARNING - cannot execute $command!\n";
		} else {
			while( <COM> ) {
				if ($_ =~ / ([0-9]+)x([0-9]+)[ +]/) {
					$width = $1;
					$height = $2;
					last;
				}
			}
			close(COM);
		}
	};
	if ($width < 1 || $height < 1) {
		print STDERR "$0: WARNING - Problem with width ($width) or height ($height) for image file $path!\n";
		next;
	}
	my $new_width = 300;
	my $new_height = int(($height * $new_width) / $width);
	$command = "convert -quality '100' -scale '${new_width}x$new_height' $path $pom_path";
	print STDERR "$0: command=$command\n";
	$status = system($command);
	if ($status != 0) {
		print STDERR "$0: WARNING - Nonzero exit status for: $command!\n";
		next;
	}
	
	print STDERR "$0: DB_ID=" . $pathway->db_id() . ", _displayName=" . $pathway->_displayName->[0] . "\n";
	
	$new_pathway_diagram_count++;
}

print "Total pathway count: " . scalar(@{$pathways}) . "\n";
print "pathway_with_elv_count=$pathway_with_elv_count\n";
print "top_level_pathway_count=$top_level_pathway_count\n";
print "new_pathway_diagram_count=$new_pathway_diagram_count\n";

print "Script has successfully completed\n";
