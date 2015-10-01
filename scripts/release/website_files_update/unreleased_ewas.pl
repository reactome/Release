#!/usr/local/bin/perl  -w
use strict;

use lib "/usr/local/gkb/modules";
use GKB::DBAdaptor;
use GKB::Config;

use Data::Dumper;
use Getopt::Long;


my $help;
GetOptions(
	'help' => \$help	
);

if ($help) {
	print usage_instructions();
    exit;
}

my ($release_user, $release_host, $release_pass, $release_port, $release_db, $release_debug);
GetOptions(
	"release_user:s" => \$release_user,
	"release_host:s" => \$release_host,
	"release_pass:s" => \$release_pass,
	"release_port:i" => \$release_port,
	"release_db=s" => \$release_db,
	"release_debug" => \$release_debug
);

$release_user ||= $GKB::Config::GK_DB_USER;
$release_pass ||= $GKB::Config::GK_DB_PASS;
$release_port ||= $GKB::Config::GK_DB_PORT;
$release_db ||= $GKB::Config::GK_DB_NAME;

if ($release_db eq $GKB::Config::GK_DB_NAME) {
    print "Enter name of release database (leave blank for default of $release_db):";
    my $db = <STDIN>;
    chomp $db;
    $release_db = $db if $db;
}

my $release_dba = GKB::DBAdaptor->new
    (
     -dbname => $release_db,
     -user   => $release_user,
     -host   => $release_host,
     -pass   => $release_pass,
     -port   => $release_port,
     -driver => 'mysql',
     -DEBUG => $release_debug
    );

    
my ($curated_user,$curated_host,$curated_pass,$curated_port,$curated_db,$curated_debug);
GetOptions(
	"curated_user:s" => \$curated_user,
	"curated_host:s" => \$curated_host,
	"curated_pass:s" => \$curated_pass,
	"curated_port:i" => \$curated_port,
	"curated_db=s" => \$curated_db,
	"curated_debug" => \$curated_debug
);

$curated_user ||= $GKB::Config::GK_DB_USER;
$curated_pass ||= $GKB::Config::GK_DB_PASS;
$curated_port ||= $GKB::Config::GK_DB_PORT;
$curated_db ||= 'gk_central';
$curated_host ||= 'reactomecurator.oicr.on.ca';

my $curated_dba = GKB::DBAdaptor->new
    (
     -dbname => $curated_db,
     -user   => $curated_user,
     -host   => $curated_host,
     -pass   => $curated_pass,
     -port   => $curated_port,
     -driver => 'mysql',
     -DEBUG => $curated_debug
    );

print "Obtaining reference gene products referencing UniProt from release database...\n";
my $released_RGPs = $release_dba->fetch_instance_by_remote_attribute('ReferenceGeneProduct', [['referenceDatabase.name', '=', ['UniProt']]]);
print "UniProt reference gene products obtained\n";

print "Obtaining released EWASs for " . scalar @{$released_RGPs} . " UniProt instances...\n";
my %released;
foreach my $rgp (@{$released_RGPs}) {
    next unless $rgp->identifier->[0];
    my $uniprot_id = $rgp->variantIdentifier->[0] ? $rgp->variantIdentifier->[0] : $rgp->identifier->[0];
    my @released_EWASs = grep {$_->is_a('EntityWithAccessionedSequence')} @{$release_dba->fetch_referer_by_instance($rgp)};
    $released{$uniprot_id} = \@released_EWASs;
}
print "Released EWASs obtained\n";

print "Obtaining reference gene products referencing UniProt from curation database...\n";
my $curated_RGPs = $curated_dba->fetch_instance_by_remote_attribute('ReferenceGeneProduct', [['referenceDatabase.name', '=', ['UniProt']]]);
print "UniProt reference gene products obtained\n";

(my $outfile = $0) =~ s/pl$/txt/;
open my $out, '>', $outfile;
print $out "DB Id\tName\tCreated\tLast Modified\n";

open my $mismatch, '>', 'mismatched_ewas_db_id_or_display_name.txt';

print "Comparing curated and released EWASs (referring to obtained reference gene products) to find unreleased EWASs\n";
foreach my $rgp (@{$curated_RGPs}) {
    next unless $rgp->species->[0];
    next unless $rgp->species->[0]->name->[0] =~ /Homo sapiens/;
    next unless $rgp->identifier->[0];
    my $uniprot_id = $rgp->variantIdentifier->[0] ? $rgp->variantIdentifier->[0] : $rgp->identifier->[0];

    my @curated_EWASs = grep {$_->is_a('EntityWithAccessionedSequence')} @{$curated_dba->fetch_referer_by_instance($rgp)};
    
    foreach my $curated_ewas (@curated_EWASs) {
		my $report_ewas = 1;
	
		my @released_EWASs = exists $released{$uniprot_id} ? @{$released{$uniprot_id}} : ();
		foreach my $released_ewas (@released_EWASs) {
			my $same_display_name = $curated_ewas->displayName eq $released_ewas->displayName;
			my $same_db_id = $curated_ewas->db_id == $released_ewas->db_id;
	    
			if ($same_display_name && $same_db_id) {
				$report_ewas = 0;
			} else {
				if ($same_display_name) {
					report_mismatch($curated_ewas, 'database identifiers', $mismatch);
					$report_ewas = 0;
				}
		
				if ($same_db_id) {
					report_mismatch($curated_ewas, 'display names', $mismatch);
					$report_ewas = 0;
				}
			}
		}
		report_unreleased_ewas($curated_ewas, $out) if $report_ewas;
    }
}
close $mismatch;
close $out;

print "$0 has finished its job\n";


sub report_unreleased_ewas {    
    my $ewas = shift;
    my $fh = shift;
    
    print $fh $ewas->db_id . "\t" . $ewas->name->[0];
    print $fh "\t";
    print $fh $ewas->created->[0]->author->[0]->displayName . " " . $ewas->created->[0]->dateTime->[0] if $ewas->created->[0] && $ewas->created->[0]->author->[0];
    print $fh "\t";
    print $fh $ewas->modified->[-1]->author->[0]->displayName . " " . $ewas->modified->[-1]->dateTime->[0] if $ewas->modified->[-1] && $ewas->modified->[-1]->author->[0];
    print $fh "\n";
}

sub report_mismatch {
    my $ewas = shift;
    my $type = shift;
    my $fh = shift;
    
    print $fh $ewas->db_id . " " . $ewas->name->[0];
    print $fh " in curated database has mismatched $type in release database\n";
}

sub usage_instructions {
	return <<END;
		
	A released database (default $GKB::Config::GK_DB_NAME) and a curated but unreleased database (default gk_central)
	are used to find unreleased EWASs in the latter

    Usage: perl $0 [options]
    
    -class instance_class (will prompt if not specified)
    -release_user db_user (default: $GKB::Config::GK_DB_USER)
    -release_host db_host (default: $GKB::Config::GK_DB_HOST)
    -release_pass db_pass (default: $GKB::Config::GK_DB_PASS)
    -release_port db_port (default: $GKB::Config::GK_DB_PORT)
    -release_db db_name (default: $GKB::Config::GK_DB_NAME)
	-release_debug (default: not used)
    -curated_user db_user (default: $GKB::Config::GK_DB_USER)
    -curated_host db_host (default: reactomecurator.oicr.on.ca)
    -curated_pass db_pass (default: $GKB::Config::GK_DB_PASS)
    -curated_port db_port (default: $GKB::Config::GK_DB_PORT)
    -curated_db db_name (default: gk_central)
	-curated_debug (default: not used)

END
}