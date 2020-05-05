#!/usr/local/bin/perl  -w
use strict;

use lib "/usr/local/gkb/modules";
use GKB::Config;
use GKB::CommonUtils;
use GKB::DBAdaptor;

use Carp;
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

my $class = get_instance_class_to_check();
open my $out, '>', get_output_file_name($class);

# Transformed into a hash for easy lookup (i.e. presence/absence) of db identifiers in the slice database
my %slice_db_ids = map {$_->db_id => 1} @{get_slice_dba()->fetch_instance(-CLASS => $class)};

# Transformed into a hash for easy lookup (i.e. presence/absence) of db identifiers for deleted instances
my %deleted_instance_db_ids = map { $_ => 1 } get_deleted_instance_db_ids();
# print "Got " . (scalar keys %deleted_instance_db_ids) . " deleted instance ids\n"; # For debugging

foreach my $production_instance (@{get_production_dba()->fetch_instance(-CLASS => $class)}) {
    next if !is_human($production_instance) && $production_instance->inferredFrom->[0];
    next if $slice_db_ids{$production_instance->db_id}; # If the production db id exists in the newer slice, it is not missing

    # If the production db id was a deleted db id, it is missing for a known and acceptable reason and doesn't need to be reported
    if ($deleted_instance_db_ids{$production_instance->db_id}) {
        print "Note:" . $production_instance->extended_displayName . " exists in the production database but is now a deleted instance\n";
        next;
    }

    print $out $production_instance->db_id . "\t" . $production_instance->name->[0];
    print $out "\t";
    print $out get_created_author_as_string($production_instance);
    print $out "\t";
    print $out get_last_modified_author_as_string($production_instance);
    print $out "\n";
}
close $out;

# Take script name and replace 'instances' with passed class name and replace
# .pl extension with .txt
sub get_output_file_name {
    my $class = shift;

    (my $outfile = $0) =~ s/pl$/txt/;
    my $lower_case_class = lc $class;
    $outfile =~ s/instances/$lower_case_class/;

    return $outfile;
}

# Get the instance class type to check for missing instances from command line
# or prompt the user running the script to select a specific class from a list
sub get_instance_class_to_check {
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
}

sub get_slice_dba {
    my $command_line_parameter_prefix = 'slice';
    my $default_values = {
        user => $GKB::Config::GK_DB_USER,
        pass => $GKB::Config::GK_DB_PASS,
        host => $GKB::Config::GK_DB_HOST,
        port => $GKB::Config::GK_DB_PORT
    };

    return get_generic_dba_from_command_line_options($command_line_parameter_prefix, $default_values);
}

sub get_production_dba {
    my $command_line_parameter_prefix = 'production';
    my $default_values = {
        user => $GKB::Config::GK_DB_USER,
        pass => $GKB::Config::GK_DB_PASS,
        host => $GKB::Config::GK_DB_HOST,
        port => $GKB::Config::GK_DB_PORT,
        db => 'current'
    };

    return get_generic_dba_from_command_line_options($command_line_parameter_prefix, $default_values);
}

sub get_curator_dba {
    my $command_line_parameter_prefix = 'curator';
    my $default_values = {
        user => $GKB::Config::GK_CURATOR_DB_USER,
        pass => $GKB::Config::GK_CURATOR_DB_PASS,
        host => $GKB::Config::GK_CURATOR_DB_HOST,
        port => $GKB::Config::GK_CURATOR_DB_PORT,
        db => $GKB::Config::GK_CURATOR_DB_NAME
    };

    return get_generic_dba_from_command_line_options($command_line_parameter_prefix, $default_values);
}

# Get database connection parameters from the command line or use defaults provided
# to this subroutine within this script.
# If command line parameters or default values aren't provided for a database name,
# the user will be prompted.  For other values, the script will terminate with an
# error message.
sub get_generic_dba_from_command_line_options {
    my $command_line_parameter_prefix = shift;
    my $default_values = shift;

    my($user, $host, $pass, $port, $db, $debug);
    GetOptions(
        "$command_line_parameter_prefix\_user=s"=> \$user,
        "$command_line_parameter_prefix\_host=s" => \$host,
        "$command_line_parameter_prefix\_pass=s" => \$pass,
        "$command_line_parameter_prefix\_port=i" => \$port,
        "$command_line_parameter_prefix\_db=s" => \$db,
        "$command_line_parameter_prefix\_debug" => \$debug
    );

    $user ||= $default_values->{'user'};
    $pass ||= $default_values->{'pass'};
    $host ||= $default_values->{'host'};
    $port ||= $default_values->{'port'};
    $db ||= $default_values->{'db'};

    if (db_info_not_provided($user, $pass, $host, $port)) {
        confess "Database connection for $command_line_parameter_prefix database requires a user, password, host, and port\n";
    }

    if (!$db) {
        print "Please enter name of $command_line_parameter_prefix database:";
        chomp($db = <STDIN>);
    }

    return GKB::DBAdaptor->new(
        -dbname => $db,
        -user   => $user,
        -host   => $host,
        -pass   => $pass,
        -port   => $port,
        -driver => 'mysql',
        -DEBUG => $debug
    );
}

sub db_info_not_provided {
    my @db_parameters = @_;

    return (any { !(defined $_) || trim($_) eq '' } @db_parameters);
}

sub trim {
    my $string = shift;

    my $trimmed_string = $string;
    $trimmed_string =~ s/^\s+|\s+$//g;
    return $trimmed_string;
}

sub get_deleted_instance_db_ids {
    my $curator_dba = get_curator_dba();

    my @deleted_instance_db_ids =
        map { @{$_->deletedInstanceDB_ID} } @{$curator_dba->fetch_instance(-CLASS => '_Deleted')};

    return @deleted_instance_db_ids;
}

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

sub get_last_modified_author_as_string {
    my $instance = shift;

    return 'Unknown' unless $instance;

    my $last_modified_string;
    foreach my $modified_instance (reverse @{$instance->modified}) {
        $last_modified_string = $modified_instance->author->[0]->displayName . " " . $modified_instance->dateTime->[0]
            unless $modified_instance->author->[0] && $modified_instance->author->[0]->db_id == 140537;
        last if $last_modified_string;
    }

    $last_modified_string ||= get_created_author_as_string($instance);
    return $last_modified_string;
}

sub get_created_author_as_string {
    my $instance = shift;

    return 'Unknown' unless $instance;

    my $created_instance = $instance->created->[0];
    return 'Unknown' unless $created_instance;

    return $created_instance->author->[0]->displayName . " " . $created_instance->dateTime->[0];
}

sub usage_instructions {
    return <<END;
    This script looks for and reports curated events that are present in
    the production database (default is the 'current' database at host $GKB::Config::GK_DB_HOST) but
    are not in the slice database (db name provided by the user) and do not have
    database identifiers represented in the _Deleted class within the curator database
    (default is '$GKB::Config::GK_CURATOR_DB_NAME' database at host $GKB::Config::GK_CURATOR_DB_HOST).

    The output file (name of this script with .txt extension and
    'instances' replaced by the selected instance class) is
    tab-delimited with four columns: event db_id, event name,
    event created author and date, and event last modified author
    and date.

    Usage: perl $0 [options]

    -class instance_class (e.g. Event or EntityWithAccessionedSequence - will prompt if not specified)

    -curator_user [db_user] (default: $GKB::Config::GK_CURATOR_DB_USER)
    -curator_host [db_host] (default: $GKB::Config::GK_CURATOR_DB_HOST)
    -curator_pass [db_pass] (default: configured password for $GKB::Config::GK_CURATOR_DB_USER)
    -curator_port [db_port] (default: $GKB::Config::GK_CURATOR_DB_PORT)
    -curator_db [db_name] (default: $GKB::Config::GK_CURATOR_DB_NAME)
    -curator_debug (default: not used)

    -slice_user [db_user] (default: $GKB::Config::GK_DB_USER)
    -slice_host [db_host] (default: $GKB::Config::GK_DB_HOST)
    -slice_pass [db_pass] (default: configured password for $GKB::Config::GK_DB_USER)
    -slice_port [db_port] (default: $GKB::Config::GK_DB_PORT)
    -slice_db [db_name] (will prompt if not specified)
    -slice_debug (default: not used)

    -production_user [db_user] (default: $GKB::Config::GK_DB_USER)
    -production_host [db_host] (default: $GKB::Config::GK_DB_HOST)
    -production_pass [db_pass] (default: configured password for $GKB::Config::GK_DB_USER)
    -production_port [db_port] (default: $GKB::Config::GK_DB_PORT)
    -production_db [db_name] (default: current)
    -production_debug (default: not used)

END
}
