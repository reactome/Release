#!/usr/local/bin/perl
use strict;
use warnings;

use lib "/usr/local/gkb/modules";

use GKB::Config;
use GKB::DBAdaptor;

use autodie;
use Array::Utils qw/:all/;
use Getopt::Long;
use Readonly;
use Term::ReadKey;

my ($help);
GetOptions(
  'help'=> \$help
);

if ($help) {
    print usage_instructions();
    exit;
}

my $recent_db = prompt("Enter recent slice database name:");
while (!$recent_db) {
    print "No value entered for recent slice database name.\n";
    $recent_db = prompt("Enter recent slice database name:");
}

my $previous_db = prompt("Enter previous slice database name:");
while (!$previous_db) {
    print "No value entered for previous slice database name.\n";
    $previous_db = prompt("Enter previous slice database name:");
}

(my $output_file = $0) =~ s/.pl$/.txt/;
$output_file = prompt('Enter name for the output file:') if ($output_file eq $0);
open (my $output, '>', $output_file);

my @new_instance_edit_ids = array_minus(
    @{get_instance_edit_ids_from_db($recent_db)},
    @{get_instance_edit_ids_from_db($previous_db)}
);

my @new_instance_edits = get_instances_from_db_by_ids($recent_db, \@new_instance_edit_ids);

foreach my $attribute (qw/authored reviewed revised/) {
    foreach my $new_instance_edit (@new_instance_edits) {
        foreach my $instance (@{$new_instance_edit->reverse_attribute_value($attribute)}) {
            report(join("\t", $instance->displayName, $instance->class, $instance->db_id, $attribute, get_authors($new_instance_edit)) . "\n", $output);
        }
    }
}

close($output);

sub prompt {
    my $query = shift;
    my $is_password = shift;

    print $query;

    ReadMode 'noecho' if $is_password; # Don't show keystrokes if it is a password
    my $return = ReadLine 0;
    chomp $return;

    ReadMode 'normal';
    print "\n" if $is_password;
    return $return;
}

sub get_instances_from_db_by_ids {
    my $db = shift;
    my $db_ids = shift;

    my @instances;
    my $dba = get_dba($db);

    push @instances, @{$dba->fetch_instance_by_db_id($_)} foreach @{$db_ids};

    return @instances;
}

sub get_instance_edit_ids_from_db {
    my $db = shift;
    return get_dba($db)->fetch_db_ids_by_class('InstanceEdit');
}

sub get_dba {
    my $db = shift;

    return GKB::DBAdaptor->new(
        -dbname => $db,
        -user   => $GKB::Config::GK_DB_USER,
        -host   => $GKB::Config::GK_DB_HOST, # host where mysqld is running
        -pass   => $GKB::Config::GK_DB_PASS,
        -port   => '3306'
    );
}

sub report {
    my $message = shift;
    my @file_handles = @_;

    push @file_handles, *STDOUT;

    foreach my $file_handle (@file_handles) {
        print $file_handle $message;
    }
}

sub get_authors {
    my $instance_edit = shift;

    return 'Unknown' unless ($instance_edit->author->[0]);

    return join(';', map {$_->displayName} @{$instance_edit->author});
}

sub usage_instructions{
    return <<END;
This script compares two databases and reports database objects which
have a new instance edit (i.e. present in the recent database but not
the older one) in the authored, reviewed, or reported attributes.

The output will be a tab delimited file (by default, same name as the
script but ending in .txt) reporting the instance name, class, database
id, attribute with the new instance edit, and the author(s) of the new
instance edit.

Usage: perl $0
END
}
