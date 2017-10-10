#!/usr/local/bin/perl -w
use strict;

use lib "/usr/local/gkb/modules";

use GKB::Config;
use GKB::DBAdaptor;

use autodie;
use List::MoreUtils qw/uniq/;
use Getopt::Long;
use Term::ReadKey;

my ($user, $pass, $host, $db, $output_file, $help);
GetOptions(
  'user=s' => \$user,
  'pass=s' => \$pass,
  'host=s' => \$host,
  'db=s'   => \$db,
  'output=s' => \$output_file,
  'help'   => \$help
);

if ($help) {
    print usage_instructions();
    exit;
}

open (my $output, ">", $output_file) if $output_file;
report("Regulation author\tRegulation instance id\tRegulated entity id\tRegulated entity name\tRegulated entity is disease\t" .
       "Regulated entity do release\tRegulator\tParent Pathways\tReason to check\n", $output);

my %seen;
my $regulations = get_dba($user, $pass, $host, $db)->fetch_instance(-CLASS => 'Regulation');
foreach my $regulation (@{$regulations}) {
    my $regulator_display_name = $regulation->regulator->[0] ?
                                 $regulation->regulator->[0]->displayName :
                                 '';
    my $regulation_author = get_authors($regulation->created->[0]);
    my $regulation_db_id = $regulation->db_id;
    
    foreach my $regulated_entity (@{$regulation->regulatedEntity}) {
        next unless is_human($regulated_entity) && !$seen{$regulated_entity}++;
    
        my $regulated_entity_display_name = $regulated_entity->displayName;
        my $regulated_entity_db_id = $regulated_entity->db_id;
        my $regulated_entity_is_disease = $regulated_entity->disease->[0] ? 'YES' : 'NO';
        my $regulated_entity_do_release = do_release($regulated_entity) ? 'YES' : 'NO';
        my @parent_pathways = get_pathways_with_diagram($regulated_entity);
     
        report(
            join("\t",
                $regulation_author,
                $regulation_db_id,
                $regulated_entity_db_id,
                $regulated_entity_display_name,
                $regulated_entity_is_disease,
                $regulated_entity_do_release,
                $regulator_display_name,
                join(';', map {$_->displayName . '(' . $_->db_id . ')'} @parent_pathways),
                get_reason_to_check(@parent_pathways)
            ) . "\n",   
            $output
        );
    }
}
close($output) if $output_file;

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
    my $file_handle = shift // *STDOUT;
    
    print $file_handle $message;
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

sub do_release {
    my $instance = shift;
    
    return $instance->_doRelease->[0] && $instance->_doRelease->[0] eq 'TRUE';
}

sub get_pathways {
    my $instance = shift;
    
    return unless $instance;
    return @{$instance->reverse_attribute_value('hasEvent')} if $instance->is_a('Event');
    return map {@{$_->reverse_attribute_value('hasEvent')}} @{$instance->reverse_attribute_value('catalystActivity')} if $instance->is_a('CatalystActivity');
}

sub get_pathways_with_diagram {
    my $base_pathway = shift;
    
    my $base_pathway_diagram = $base_pathway->reverse_attribute_value('representedPathway')->[0];
    return $base_pathway if ($base_pathway_diagram);
    
    my @pathways_with_diagram;    
    push @pathways_with_diagram, get_pathways_with_diagram($_) foreach
        @{$base_pathway->reverse_attribute_value('hasEvent')};
        
    return @pathways_with_diagram;
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

sub get_reason_to_check {
    my @pathways = @_;
    
    return 'no pathways' unless @pathways;
    return 'more than one pathway' if scalar @pathways > 1;
    return '';        
}

sub usage_instructions{
    return <<END;

This script gets all regulated entities from a database
and reports the closest pathways with diagrams in which
they are present.

The output will be a tab delimited file reporting the
regulation instance author, regulation databse id, regulated
entity database id, regulated entity display name, whether a
regulated entity has a disease tag, whether a regulated entity's
'do release' flag is set to true, regulator display name, pathways
(display name and db id) where the regulated entity occurs, and if
there is a reason curators must manually decide which pathways should
show the regulation.
    
Usage: perl $0

Options:
    
-user [db_user]         User for source database (default is $GKB::Config::GK_DB_USER)
-pass [db_pass]         Password for source database (default is password for $GKB::Config::GK_DB_USER user)
-host [db_host]         Host for source database (default is $GKB::Config::GK_DB_HOST)
-db [db_name]           Source database (default is $GKB::Config::GK_DB_NAME)
-output [output_file]   File name for script output (default is STDOUT)
-help                   Prints these instructions
    
END
}
