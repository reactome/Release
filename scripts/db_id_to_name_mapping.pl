#!/usr/local/bin/perl -w
use strict;

use lib "/usr/local/gkb/modules";

use GKB::Config;
use GKB::DBAdaptor;

use autodie qw/:all/;
use Array::Utils qw/:all/;
use Fcntl qw/:flock/;
use File::Basename;
use Getopt::Long;
use Term::ReadKey;

my ($host, $db, $output_dir, $output_file, $help);
GetOptions(
  'host:s' => \$host,
  'db:s' => \$db,
  'output_dir:s' => \$output_dir,
  'output_file:s' => \$output_file,
  'help'=> \$help 
);

if ($help) {
    print usage_instructions();
    exit;
}

$host ||= 'localhost';
$db ||= 'gk_current';
$output_dir ||= '.';
if (!$output_file) {
    unless (($output_file = basename($0)) =~ s/.pl$/.txt/) {
        $output_file = prompt('Enter name for the output file:');
    }
}

my $dba = get_dba({'host' => $host, 'db' => $db});
my $events = $dba->fetch_instance(-CLASS => 'Event');
my $physical_entities = $dba->fetch_instance(-CLASS => 'PhysicalEntity');

open (my $output, ">", "$output_dir/$output_file");
flock($output, LOCK_EX);
seek($output, 0, 0);
truncate($output, 0);
binmode($output, ":utf8");
report(join("\t", 'Database_Identifier', 'Node_Name', 'Node_Type', 'Display_Name', 'Reference_Entity_Name', 'Reference_Entity_Identifier', 'Instance_Class') . "\n", $output);

my %seen;
foreach my $instance (@{$events},@{$physical_entities}) {
    next unless is_human($instance);
    
    my $db_id = $instance->db_id;
    my $reference_entity_instance = $instance->referenceEntity->[0];
    my $representative_instance = $reference_entity_instance || $instance;
    
    my $node_name = $representative_instance->db_id;
    $node_name .= '_RLE' if $representative_instance->is_a('ReactionlikeEvent');
    $node_name = $instance->displayName . '_' . $node_name if $reference_entity_instance;
    
    my $node_type = get_node_type($instance) || 'N/A';
    
    #next unless $instance->is_a('EntityWithAccessionedSequence');
    #next if $representative_instance->is_a('ReferenceGeneProduct');
        
    my $display_name = $representative_instance->geneName->[0] ||
               $representative_instance->name->[0] ||
               $representative_instance->displayName ||
               'N/A';
    my $class = $representative_instance->class;
    
    my $reference_entity_name;
    if ($reference_entity_instance) {
        $reference_entity_name = $reference_entity_instance->name->[0] || $reference_entity_instance->geneName->[0] || 'N/A';
    } else {
        $reference_entity_name = 'N/A';
    }
    my $reference_entity_identifier = $reference_entity_instance && $reference_entity_instance->identifier->[0] ? $reference_entity_instance->identifier->[0] : 'N/A';
    
    next if $seen{$node_name}++;
    
    $node_name =~ s/[ \,+]/_/g;
    $display_name =~ s/[ \,+]/_/g;
    report(join("\t", $db_id, $node_name, $node_type, $display_name, $reference_entity_name, $reference_entity_identifier, $class) . "\n",$output);
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
    my $parameters = shift;

    return GKB::DBAdaptor->new(
        -dbname => $parameters->{'db'} // $GKB::Config::GK_DB_NAME,
        -user   => $GKB::Config::GK_DB_USER,
        -host   => $parameters->{'host'} // $GKB::Config::GK_DB_HOST, # host where mysqld is running
        -pass   => $GKB::Config::GK_DB_PASS,
        -port   => '3306'
    );
}

sub get_node_type {
    my $instance = shift;
    
    return 'N/A' unless $instance;
    
    if ($instance->is_a('ReactionlikeEvent')) {
        return 'reactionlikeevent';
    } elsif ($instance->is_a('Complex')) {
        return 'complex';
    } elsif ($instance->is_a('Drug')) {
        return 'drug';
    } elsif ($instance->is_a('EntitySet')) {
        return 'set';
    } elsif ($instance->is_a('Polymer')) {
        return 'polymer';
    } elsif ($instance->is_a('OtherEntity')) {
        return 'other-entity';
    }
    
    my $reference_entity = $instance->referenceEntity->[0];
    
    if ($reference_entity && $reference_entity->referenceDatabase->[0]) {
        my $reference_database = $reference_entity->referenceDatabase->[0];
        my $node_type = $reference_database->displayName || '';
    
        if ($node_type eq 'UniProt') {
            return 'protein';
        } elsif ($node_type eq 'ENSEMBL') {
            my $reference_entity_class = $reference_entity->class;
            if ($reference_entity_class eq 'ReferenceGeneProduct') {
                return 'protein';
            } elsif ($reference_entity_class eq 'ReferenceRNASequence') {
                return 'rna';
            } elsif ($reference_entity_class eq 'ReferenceDNASequence') {
                return 'dna';
            }
        } elsif ($node_type eq 'ChEBI') {
            return 'small-molecule';
        } elsif ($node_type eq 'miRBase') {
            return 'miRNA';
        }
    }
    
    return 'N/A';
}

sub report {
    my $message = shift;
    my @file_handles = @_;
    
    #push @file_handles, *STDOUT;
    
    foreach my $file_handle (@file_handles) {
        print $file_handle $message;
    }
}

sub is_human {
    my $instance = shift;
    
    return !$instance->species->[0] ||
           ($instance->species->[0] &&
            $instance->species->[0]->displayName eq 'Homo sapiens' &&
            !($instance->species->[1]) &&
            !(is_chimeric($instance))
            );
}

sub is_chimeric {
    my $instance = shift;
    
    return $instance->isChimeric->[0] && $instance->isChimeric->[0] eq 'TRUE';
}

sub usage_instructions{
    return <<END;

Usage: perl $0
    
END
}
