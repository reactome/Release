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

my $class;
GetOptions(
    'class=s' => \$class
);
unless ($class) {
    my @classes = qw/Event EntityWithAccessionedSequence/;

    until ($class) {
        print_choose_class_query(@classes);
        my $answer = <STDIN>;
        chomp $answer;
        eval {
            $class = $classes[$answer - 1] if $answer =~ /^\d+$/;
        };
    }
}


my($release_user, $release_host, $release_pass, $release_port, $release_db, $release_debug);
GetOptions(
    "release_user=s" => \$release_user,
    "release_host=s" => \$release_host,
    "release_pass=s" => \$release_pass,
    "release_port=i" => \$release_port,
    "release_db=s" => \$release_db,
    "release_debug" => \$release_debug
);

$release_user ||= $GKB::Config::GK_DB_USER;
$release_pass ||= $GKB::Config::GK_DB_PASS;
$release_host ||= $GKB::Config::GK_DB_HOST;
$release_port ||= $GKB::Config::GK_DB_PORT;
$release_db ||= $GKB::Config::GK_DB_NAME;

if ($release_db eq $GKB::Config::GK_DB_NAME) {
    print "Enter name of release database (leave blank for default of $release_db):";
    chomp(my $release_db_name = <STDIN>);
    $release_db = $release_db_name if $release_db_name;
}

my $release_dba = GKB::DBAdaptor->new(
    -dbname => $release_db,
    -user   => $release_user,
    -host   => $release_host,
    -pass   => $release_pass,
    -port   => $release_port,
    -driver => 'mysql',
    -DEBUG => $release_debug
);


my($curated_user, $curated_host, $curated_pass, $curated_port, $curated_db, $curated_debug);
&GetOptions(
    "curated_user=s"=> \$curated_user,
    "curated_host=s" => \$curated_host,
    "curated_pass=s" => \$curated_pass,
    "curated_port=i" => \$curated_port,
    "curated_db=s" => \$curated_db
);

$curated_user ||= $GKB::Config::GK_DB_USER;
$curated_pass ||= $GKB::Config::GK_DB_PASS;
$curated_port ||= $GKB::Config::GK_DB_PORT;
$curated_db ||= 'gk_central';
$curated_host ||= 'curator.reactome.org';

my $curated_dba = GKB::DBAdaptor->new(
    -dbname => $curated_db,
    -user   => $curated_user,
    -host   => $curated_host,
    -pass   => $curated_pass,
    -port   => $curated_port,
    -driver => 'mysql',
    -DEBUG => $curated_debug
);

my $released_instances = $release_dba->fetch_instance(-CLASS => $class);

my %released;
foreach my $instance (@{$released_instances}) {
    next unless $instance->stableIdentifier->[0];
    next unless $instance->stableIdentifier->[0]->released->[0] && $instance->stableIdentifier->[0]->released->[0] =~ /true/i;
    my $stable_id = $instance->stableIdentifier->[0]->identifier->[0];
    $released{$stable_id}++;
}

my $curated_instances = $curated_dba->fetch_instance(-CLASS => $class);

(my $outfile = $0) =~ s/pl$/txt/;
$class = lc $class;
$outfile =~ s/instances/$class/;

open my $out, '>', $outfile;
binmode($out, ":utf8");
foreach my $instance (@{$curated_instances}) {
    next unless $instance->species->[0];
    next unless $instance->species->[0]->name->[0] =~ /Homo sapiens/;
    next unless $instance->stableIdentifier->[0];
    next unless $instance->stableIdentifier->[0]->released->[0] && $instance->stableIdentifier->[0]->released->[0] =~ /true/i;
    my $stable_id = $instance->stableIdentifier->[0]->identifier->[0];
    next if $released{$stable_id};

    my $instance_db_id = $instance->db_id || '';
    my $instance_name = $instance->name->[0] || '';

    print $out $instance_db_id . "\t" . $instance_name;
    print $out "\t";
    print $out $instance->created->[0]->author->[0]->displayName . " " . $instance->created->[0]->dateTime->[0] if $instance->created->[0] && $instance->created->[0]->author->[0];
    print $out "\t";
    print $out $instance->modified->[-1]->author->[0]->displayName . " " . $instance->modified->[-1]->dateTime->[0] if $instance->modified->[-1] && $instance->modified->[-1]->author->[0];
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

sub usage_instructions {
    return <<END;
    A released database (default $GKB::Config::GK_DB_NAME) and a curated but unreleased database (default gk_central)
    are needed to find unreleased instances of the selected class in the latter

    Usage: perl $0 [options]

    -class instance_class (will prompt if not specified)
    -release_user db_user (default: $GKB::Config::GK_DB_USER)
    -release_host db_host (default: $GKB::Config::GK_DB_HOST)
    -release_pass db_pass (default: password for $GKB::Config::GK_DB_USER)
    -release_port db_port (default: $GKB::Config::GK_DB_PORT)
    -release_db db_name (default: $GKB::Config::GK_DB_NAME)
    -release_debug (default: not used)
    -curated_user db_user (default: $GKB::Config::GK_DB_USER)
    -curated_host db_host (default: curator.reactome.org)
    -curated_pass db_pass (default: password for $GKB::Config::GK_DB_USER)
    -curated_port db_port (default: $GKB::Config::GK_DB_PORT)
    -curated_db db_name (default: gk_central)
    -curated_debug (default: not used)

END
}
