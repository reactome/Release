#!/usr/local/bin/perl -w
use strict;

use lib "/usr/local/gkb/modules";

use GKB::Config;
use GKB::DBAdaptor;

use autodie;
use List::MoreUtils qw/uniq/;
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

if ($help) {
    print usage_instructions();
    exit;
}

(my $output_file = $0) =~ s/.pl$/.txt/;
$output_file = prompt('Enter name for the output file:') if ($output_file eq $0);
open (my $output, ">", $output_file);
report("Regulation author\tRegulated entity id\tRegulated entity name\tRegulator\tDiagrams\tTo be checked\n", $output);

my %seen;
my $regulations = get_dba($user, $pass, $host, $db)->fetch_instance(-CLASS => 'Regulation');
foreach my $regulation (@{$regulations}) {
    my $regulator_display_name = $regulation->regulator->[0]->displayName if $regulation->regulator->[0];
    my $regulation_author = get_authors($regulation->created->[0]);
    
    foreach my $regulated_entity (@{$regulation->regulatedEntity}) {
        next unless is_human($regulated_entity) && !$seen{$regulated_entity}++;
    
        my $regulated_entity_display_name = $regulated_entity->displayName;
        my $regulated_entity_db_id = $regulated_entity->db_id;
        
        my @diagrams;
        foreach my $pathway (get_pathways($regulated_entity)) {
            push @diagrams, get_diagrams($pathway);
        }
        @diagrams = uniq @diagrams;
        
        report(
            join("\t",
                $regulation_author,
                $regulated_entity_db_id,
                $regulated_entity_display_name,
                $regulator_display_name,
                join(';', map {$_->displayName . '(' . $_->db_id . ')'} @diagrams),
                @diagrams == 1 ? 'NO' : 'YES'
            ) . "\n",   
            $output
        );
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
    my $user = shift;
    my $pass = shift;
    my $host = shift;
    my $db = shift;

    return GKB::DBAdaptor->new(
        -dbname => $db // $GKB::Config::GK_DB_NAME,
        -user   => $user // $GKB::Config::GK_DB_USER,
        -host   => $host // $GKB::Config::GK_DB_HOST, # host where mysqld is running                                                                                                                              
        -pass   => $pass // $GKB::Config::GK_DB_PASS,
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
    
    return 'Unknown' unless ($instance_edit && $instance_edit->author->[0]);
    
    return join(';', map {$_->displayName} @{$instance_edit->author});
}

sub is_human {
    my $instance = shift;
    
    return ($instance->species->[0] &&
            $instance->species->[0]->displayName eq 'Homo sapiens' &&
            !($instance->species->[1]) &&
            !(is_chimeric($instance))
            );
}

sub is_chimeric {
    my $instance = shift;
    
    return $instance->isChimeric->[0] && $instance->isChimeric->[0] eq 'TRUE';
}

sub get_pathways {
    my $instance = shift;
    
    return unless $instance;
    return @{$instance->reverse_attribute_value('hasEvent')} if $instance->is_a('Event');
    return map {@{$_->reverse_attribute_value('hasEvent')}} @{$instance->reverse_attribute_value('catalystActivity')} if $instance->is_a('CatalystActivity');
}

sub get_diagrams {
    my $pathway = shift;
    
    return unless $pathway;
    
    my @diagrams = @{$pathway->reverse_attribute_value('representedPathway')};
    unless (@diagrams) {
        push @diagrams, get_diagrams($_) foreach get_pathways($pathway);
    }
    
    return @diagrams;
}

sub usage_instructions{
    return <<END;

This script gets all regulated entities from a database
and reports those that occur in more than one diagram.

The output will be a tab delmited file (by default, same
name as the script but ending in .txt) reporting the
regulation instance author, regulated entity database id,
regulated entity display name, regulator display name,
diagrams (display name and db id) where the regulated
entity occurs, and whether curators must decide which
diagrams should show the regulation.
    
Usage: perl $0

Options:
    
-user [db_user]         User for source database (default is $GKB::Config::GK_DB_USER)
-pass [db_pass]         Password for source database (default is password for $GKB::Config::GK_DB_USER user)
-host [db_host]         Host for source database (default is $GKB::Config::GK_DB_HOST)
-db [db_name]           Source database (default is $GKB::Config::GK_DB_NAME)
-help                   Prints these instructions
    
END
}
