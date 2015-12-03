#!/usr/local/bin/perl -w
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

use GKB::Utils;
use GKB::Instance;
use GKB::Config;
use GKB::DBAdaptor;

use Data::Dumper;
use Getopt::Long;
use List::MoreUtils qw/any/;

use Log::Log4perl qw/get_logger/;
Log::Log4perl->init(\$LOG_CONF);
my $logger = get_logger(__PACKAGE__);


$GKB::Config::NO_SCHEMA_VALIDITY_CHECK = undef;

our($opt_host,$opt_db,$opt_pass,$opt_port,$opt_debug,$opt_user,$opt_outputdir,$opt_skip_human);

my $usage = "Usage: $0 -db db_name -user db_user -host db_host -pass db_pass -port db_port -outputdir output_directory -skip_human -debug\n";

&GetOptions("db:s", "user:s", "host:s", "pass:s", "port:i", "outputdir:s", "skip_human", "debug");

$opt_db || die $usage;
$opt_outputdir ||= '.';

my $dba = GKB::DBAdaptor->new
(
	     -user   => $opt_user,
	     -host   => $opt_host,
	     -pass   => $opt_pass,
	     -port   => $opt_port,
	     -dbname => $opt_db,
	     -DEBUG => $opt_debug
);

## Get all species that have front page items or that have orthologous
## pathways with front page items.
my $species = $dba->fetch_instance_by_remote_attribute
    (
     'Species',
     [
        ['species:Pathway.representedPathway:PathwayDiagram','IS NOT NULL',[]],
     ]
    );
    
my $related_species = $dba->fetch_instance_by_remote_attribute
    (
     'Species',
     [
      ['relatedSpecies:Pathway.representedPathway:PathwayDiagram','IS NOT NULL',[]],
     ]
    );

my @pathogenic_species_names = (
    "Human immunodeficiency virus 1",
    "Influenza A virus",
    "Mycobacterium tuberculosis"
);

foreach (@$related_species) {
    next unless $_->_displayName->[0] =~ /influenza|immunodeficiency/i;
       
    push @$species, $_;
}

# TODO: Probably we should get all species whose pathways have diagrams rather than FrontPage items.

print "$0: species=";
foreach my $specie (@{$species}) {
    print $specie->_displayName->[0] . ", ";
}
print "\n";

# Pre-create the command line options associated with database access
my $reactome_db_options = "-db $opt_db";
if (defined $opt_host && !($opt_host eq '')) {
	$reactome_db_options .= " -host $opt_host";
}
if (defined $opt_user && !($opt_user eq '')) {
	$reactome_db_options .= " -user $opt_user";
}
if (defined $opt_pass && !($opt_pass eq '')) {
	# Put a backslash in front of characters that have special meaning to the shell
	my $pass = $opt_pass;
	if ($pass =~ /\$/) {
		$pass =~ s/\$/\\\$/g;
	}
	$reactome_db_options .= " -pass $pass";
}
if (defined $opt_port && !($opt_port eq '')) {
	$reactome_db_options .= " -port $opt_port";
}

my $db = $dba->db_name;
foreach my $sp (@{$species}) {
    my $sp_name = $sp->displayName;
    next if $sp_name eq "Homo sapiens" && $opt_skip_human;
    
    my $pathogenic_flag = any {$_ eq $sp_name} @pathogenic_species_names ? '-pathogenic' : '';
    my $tmp = lc($sp_name);

    $logger->info("$0: Processing species: $tmp\n");

    $tmp =~ s/\s+/_/g;
    my $cmd = "perl report_interactions.pl $reactome_db_options -sp '$sp_name' $pathogenic_flag | sort | uniq | gzip -c > $opt_outputdir/$tmp.interactions.txt.gz";
    print "$cmd\n";
    system($cmd) == 0 or $logger->error("$cmd failed.\n");
}

