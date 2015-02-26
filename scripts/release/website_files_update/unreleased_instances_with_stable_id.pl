#!/usr/local/bin/perl  -w
use strict;

use lib "/usr/local/gkb/modules";
use GKB::DBAdaptor;
use GKB::Config;

use Data::Dumper;
use Getopt::Long;

if ($ARGV[0] && $ARGV[0] =~ /-h(elp)?$/) {
    print <<END;
A released database (default $GKB::Config::GK_DB_NAME) and a curated but unreleased database (default gk_central) are needed to find unreleased instances of the selected class in the latter

Usage: perl $0 -class instance_class -release_user db_user -release_host db_host -release_pass db_pass -release_port db_port -release_db db_name -curated_user db_user -curated_host db_host -curated_pass db_pass -curated_port db_port -curated_db db_name

END
    exit;
}

our($opt_class);
&GetOptions("class");

unless ($opt_class) {
    my @classes = qw/Event EntityWithAccessionedSequence/;
    
    until ($opt_class) {
	print_choose_class_query(@classes);
	my $answer = <STDIN>;
	chomp $answer;
	eval {
	    $opt_class = $classes[$answer - 1] if $answer =~ /^\d+$/;
	};
    }
}

our($opt_debug);
&GetOptions("debug");

our($opt_release_user,$opt_release_host,$opt_release_pass,$opt_release_port,$opt_release_db);
&GetOptions("release_user:s", "release_host:s", "release_pass:s", "release_port:i", "release_db=s");

$opt_release_user ||= $GKB::Config::GK_DB_USER;
$opt_release_pass ||= $GKB::Config::GK_DB_PASS;
$opt_release_port ||= $GKB::Config::GK_DB_PORT;
$opt_release_db ||= $GKB::Config::GK_DB_NAME;

if ($opt_release_db eq $GKB::Config::GK_DB_NAME) {
    print "Enter name of release database (leave blank for default of $opt_release_db):";
    my $release_db = <STDIN>;
    chomp $release_db;
    $opt_release_db = $release_db if $release_db;
}

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

my $released_instances = $release_dba->fetch_instance(-CLASS => $opt_class);

my %released;
foreach my $instance (@{$released_instances}) {
    next unless $instance->stableIdentifier->[0];
    my $stable_id = $instance->stableIdentifier->[0]->identifier->[0];
    $released{$stable_id}++;
}

my $curated_instances = $curated_dba->fetch_instance(-CLASS => $opt_class);

(my $outfile = $0) =~ s/pl$/txt/;
my $class = lc $opt_class;
$outfile =~ s/instances/$class/;

open my $out, '>', $outfile;
foreach my $instance (@{$curated_instances}) {
    next unless $instance->species->[0];
    next unless $instance->species->[0]->name->[0] =~ /Homo sapiens/;
    next unless $instance->stableIdentifier->[0];
    my $stable_id = $instance->stableIdentifier->[0]->identifier->[0];
    next if $released{$stable_id};
    
    print $out $instance->db_id . "\t" . $instance->name->[0];
    print $out "\t" . $instance->created->[0]->author->[0]->displayName . " " . $instance->created->[0]->dateTime->[0] if $instance->created->[0] && $instance->created->[0]->author->[0];
    print $out "\t" . $instance->modified->[-1]->author->[0]->displayName . " " . $instance->modified->[-1]->dateTime->[0] if $instance->modified->[-1] && $instance->modified->[-1]->author->[0];
    print $out "\n";
}
close $out;

sub print_choose_class_query {
    my @classes = @_;
    print "Choose class - \n";
    for (my $index = 0; $index < scalar @classes; $index++) {
	my $class = $classes[$index];
	my $num = $index + 1;
	print "$class($num)\n";
    }
    print "Select number:"
}
