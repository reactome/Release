#!/usr/local/bin/perl  -w
use strict;

use lib "/usr/local/gkb/modules";
use GKB::DBAdaptor;
use GKB::Config;

use Data::Dumper;
use Getopt::Long;

if ($ARGV[0] && $ARGV[0] =~ /-h(elp)?$/) {
    print <<END;
A released database (default $GKB::Config::GK_DB_NAME) and a curated but unreleased database (default gk_central) are needed to find unreleased EWASs in the latter

Usage: $0 -release_user db_user -release_host db_host -release_pass db_pass -release_port db_port -release_db db_name -curated_user db_user -curated_host db_host -curated_pass db_pass -curated_port db_port -curated_db db_name

END
    exit;
}

our($opt_debug);
&GetOptions("debug");

our($opt_release_user,$opt_release_host,$opt_release_pass,$opt_release_port,$opt_release_db);
&GetOptions("release_user:s", "release_host:s", "release_pass:s", "release_port:i", "release_db=s");

$opt_release_user ||= $GKB::Config::GK_DB_USER;
$opt_release_pass ||= $GKB::Config::GK_DB_PASS;
$opt_release_port ||= $GKB::Config::GK_DB_PORT;
$opt_release_db ||= $GKB::Config::GK_DB_NAME;

my $release_dba = GKB::DBAdaptor->new
    (
     -dbname => $opt_release_db,
     -user   => $opt_release_user,
     -host   => $opt_release_host,
     -pass   => $opt_release_pass,
     -port   => $opt_release_port,
     -driver => 'mysql',
     -DEBUG => $opt_debug
    );

    
our($opt_curated_user,$opt_curated_host,$opt_curated_pass,$opt_curated_port,$opt_curated_db);
&GetOptions("curated_user:s", "curated_host:s", "curated_pass:s", "curated_port:i", "curated_db=s");

$opt_curated_user ||= $GKB::Config::GK_DB_USER;
$opt_curated_pass ||= $GKB::Config::GK_DB_PASS;
$opt_curated_port ||= $GKB::Config::GK_DB_PORT;
$opt_curated_db ||= 'gk_central';
$opt_curated_host = 'reactomecurator.oicr.on.ca';

my $curated_dba = GKB::DBAdaptor->new
    (
     -dbname => $opt_curated_db,
     -user   => $opt_curated_user,
     -host   => $opt_curated_host,
     -pass   => $opt_curated_pass,
     -port   => $opt_curated_port,
     -driver => 'mysql',
     -DEBUG => $opt_debug
    );

my $released_RGPs = $release_dba->fetch_instance_by_remote_attribute('ReferenceGeneProduct', [['referenceDatabase.name', '=', ['UniProt']]]);

my %released;
foreach my $rgp (@{$released_RGPs}) {
    next unless $rgp->identifier->[0];
    my $uniprot_id = $rgp->variantIdentifier->[0] ? $rgp->variantIdentifier->[0] : $rgp->identifier->[0];
    $released{$uniprot_id} = scalar grep {$_->is_a('EntityWithAccessionedSequence')} @{$release_dba->fetch_referer_by_instance($rgp)};
}

my $curated_RGPs = $curated_dba->fetch_instance_by_remote_attribute('ReferenceGeneProduct', [['referenceDatabase.name', '=', ['UniProt']]]);

(my $outfile = $0) =~ s/pl$/txt/;
open my $out, '>', $outfile;
foreach my $rgp (@{$curated_RGPs}) {
    next unless $rgp->species->[0];
    next unless $rgp->species->[0]->name->[0] =~ /Homo sapiens/;
    next unless $rgp->identifier->[0];
    my $uniprot_id = $rgp->variantIdentifier->[0] ? $rgp->variantIdentifier->[0] : $rgp->identifier->[0];

    my @curated_EWASs = grep {$_->is_a('EntityWithAccessionedSequence')} @{$curated_dba->fetch_referer_by_instance($rgp)};
    if ($released{$uniprot_id}) {
	my $unreleased = scalar @curated_EWASs - $released{$uniprot_id};
	print $out "$uniprot_id has $unreleased unreleased EWASs\n" if $unreleased;
    } else {
	foreach my $ewas (@curated_EWASs) {
	    print $out $ewas->db_id . "\t" . $ewas->name->[0];
	    print $out "\t" . $ewas->created->[0]->author->[0]->displayName . " " . $ewas->created->[0]->dateTime->[0] if $ewas->created->[0] && $ewas->created->[0]->author->[0];
	    print $out "\t" . $ewas->modified->[-1]->author->[0]->displayName . " " . $ewas->modified->[-1]->dateTime->[0] if $ewas->modified->[-1] && $ewas->modified->[-1]->author->[0];
	    print $out "\n";
	}
    }
}
close $out;
