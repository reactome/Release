#!/usr/local/bin/perl -w
use strict;

use lib "/usr/local/gkb/modules";

use GKB::Config;
use GKB::DBAdaptor;

use autodie qw/:all/;
use Getopt::Long;
use Term::ReadKey;

my ($user, $pass, $host, $db, $help);
GetOptions(
  'user=s' => \$user,
  'pass=s' => \$pass,
  'host=s' => \$host,
  'db=s'   => \$db,
  'help'   => \$help
);

$user ||= $GKB::Config::GK_DB_USER;
$pass ||= $GKB::Config::GK_DB_PASS;
$host ||= $GKB::Config::GK_DB_HOST;
$db   ||= $GKB::Config::GK_DB_NAME;

if ($help) {
    print usage_instructions();
    exit;
}

(my $output_file = $0) =~ s/.pl$/.txt/;
open(my $output, '>', $output_file);

my @records = `perl regulated_entity_in_pathway.pl -user $user -pass $pass -host $host -db $db`;

my $dba = get_dba($db, $user, $host, $pass);
shift @records; #discard header
foreach my $record (@records) {    
    chomp $record;
    my @fields = split "\t", $record;
    my $regulated_entity_id = $fields[2];
    my $manual_check = $fields[8];
    
    my $regulated_entity_instance = $dba->fetch_instance_by_db_id($regulated_entity_id)->[0];
    my $regulation_instances = $regulated_entity_instance->reverse_attribute_value('regulatedEntity');
    
    if (!$manual_check) {
        my $pathway_with_diagram = $fields[7];
        my ($pathway_id) = $pathway_with_diagram =~ /\((\d+)\)$/;
        my $pathway_instance = $dba->fetch_instance_by_db_id($pathway_id)->[0];
        foreach my $regulation_instance (@{$regulation_instances}) {
            if ($regulation_instance->containedInPathway->[0]) {
                report($regulation_instance->displayName . '(' . $regulation_instance->db_id . ')' .
                     " has a pathway already in its containedInPathway attribute\n", $output);
                next;
            }
            
            #$regulation_instance->containedInPathway(undef);
            #$regulation_instance->containedInPathway($pathway_instance);
            #$dba->update_attribute($regulation_instance, "containedInPathway");
            report($regulation_instance->displayName . '(' . $regulation_instance->db_id . ')' .
                  ' populated with ' . $pathway_instance->displayName . '(' . $pathway_instance->db_id . ")\n", $output);
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

sub get_dba {
    my $db = shift;
    my $user = shift;
    my $host = shift;
    my $pass = shift;
    
    return GKB::DBAdaptor->new(
        -dbname => $db,
        -user   => $user,
        -host   => $host, # host where mysqld is running                                                                                                                              
        -pass   => $pass,
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

sub usage_instructions{
    return <<END;
 
Usage: perl $0

Options:
    
-user [db_user]         User for source database (default is $GKB::Config::GK_DB_USER)
-pass [db_pass]         Password for source database (default is password for $GKB::Config::GK_DB_USER user)
-host [db_host]         Host for source database (default is $GKB::Config::GK_DB_HOST)
-db [db_name]           Source database (default is $GKB::Config::GK_DB_NAME)
-help                   Prints these instructions
    
END
}
