#!/usr/local/bin/perl  -w
use strict;

use lib "/usr/local/gkb/modules";
use GKB::Config;
use GKB::CommonUtils;
use GKB::DBAdaptor;

use Data::Dumper;
use Getopt::Long;
use List::MoreUtils qw/any/;

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


my($slice_user, $slice_host, $slice_pass, $slice_port, $slice_db, $slice_debug);
GetOptions(
    "slice_user=s" => \$slice_user,
    "slice_host=s" => \$slice_host,
    "slice_pass=s" => \$slice_pass,
    "slice_port=i" => \$slice_port,
    "slice_db=s" => \$slice_db,
    "slice_debug" => \$slice_debug
);

$slice_user ||= $GKB::Config::GK_DB_USER;
$slice_pass ||= $GKB::Config::GK_DB_PASS;
$slice_host ||= $GKB::Config::GK_DB_HOST;
$slice_port ||= $GKB::Config::GK_DB_PORT;

if (!$slice_db) {
    print "Please enter name of slice database:";
    chomp($slice_db = <STDIN>);
}

my $slice_dba = GKB::DBAdaptor->new
    (
     -dbname => $slice_db,
     -user   => $slice_user,
     -host   => $slice_host,
     -pass   => $slice_pass,
     -port   => $slice_port,
     -driver => 'mysql',
     -DEBUG => $slice_debug
    );


my($production_user, $production_host, $production_pass, $production_port, $production_db, $production_debug);
&GetOptions(
    "production_user=s"=> \$production_user,
    "production_host=s" => \$production_host,
    "production_pass=s" => \$production_pass,
    "production_port=i" => \$production_port,
    "production_db=s" => \$production_db,
    "production_debug" => \$production_debug
);

$production_user ||= $GKB::Config::GK_DB_USER;
$production_pass ||= $GKB::Config::GK_DB_PASS;
$production_host ||= $GKB::Config::GK_DB_HOST;
$production_port ||= $GKB::Config::GK_DB_PORT;
$production_db ||= 'current';

my $production_dba = GKB::DBAdaptor->new(
    -dbname => $production_db,
    -user   => $production_user,
    -host   => $production_host,
    -pass   => $production_pass,
    -port   => $production_port,
    -driver => 'mysql',
    -DEBUG => $production_debug
);

(my $outfile = $0) =~ s/pl$/txt/;
my $lower_case_class = lc $class;
$outfile =~ s/instances/$lower_case_class/;
open my $out, '>', $outfile;

my %slice_db_ids = map {$_->db_id => 1} @{$slice_dba->fetch_instance(-CLASS => $class)};
foreach my $production_instance (@{$production_dba->fetch_instance(-CLASS => $class)}) {
    next if !is_human($production_instance) && $production_instance->inferredFrom->[0];
    next if $slice_db_ids{$production_instance->db_id};

    print $out $production_instance->db_id . "\t" . $production_instance->name->[0];
    print $out "\t";
    print $out get_created_as_string($production_instance);
    print $out "\t";
    print $out get_last_modified_as_string($production_instance);
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

sub get_last_modified_as_string {
    my $instance = shift;

    return 'Unknown' unless $instance;

    my $last_modified_string;
    foreach my $modified_instance (reverse @{$instance->modified}) {
        $last_modified_string = $modified_instance->author->[0]->displayName . " " . $modified_instance->dateTime->[0]
            unless $modified_instance->author->[0] && $modified_instance->author->[0]->db_id == 140537;
        last if $last_modified_string;
    }

    $last_modified_string ||= get_created_as_string($instance);
    return $last_modified_string;
}

sub get_created_as_string {
    my $instance = shift;

    return 'Unknown' unless $instance;

    my $created_instance = $instance->created->[0];
    return 'Unknown' unless $created_instance;

    return $created_instance->author->[0]->displayName . " " . $created_instance->dateTime->[0];
}

sub usage_instructions {
    return <<END;
    This script looks for and reports curated events that are present
    in the production database (default is the local gk_current) but
    not in the slice database which is provided by the user.

    The output file (name of this script with .txt extension and
    'instances' replaced by the selected instance class) is
    tab-delimited with four columns: event db_id, event name,
    event created author and date, and event last modified author
    and date.

    Usage: perl $0 [options]

    -class instance_class (will prompt if not specified)

    -slice_user db_user (default: $GKB::Config::GK_DB_USER)
    -slice_host db_host (default: $GKB::Config::GK_DB_HOST)
    -slice_pass db_pass (default: password for $GKB::Config::GK_DB_USER)
    -slice_port db_port (default: $GKB::Config::GK_DB_PORT)
    -slice_db db_name (will prompt if not specified)
    -slice_debug (default: not used)

    -production_user db_user (default: $GKB::Config::GK_DB_USER)
    -production_host db_host (default: $GKB::Config::GK_DB_HOST)
    -production_pass db_pass (default: password for $GKB::Config::GK_DB_USER)
    -production_port db_port (default: $GKB::Config::GK_DB_PORT)
    -production_db db_name (default: current)
    -production_debug (default: not used)

END
}
