#!/usr/local/bin/perl  -w
use strict;

use feature qw/state/;

use autodie;
use Array::Compare;
use Getopt::Long;
use Data::Dumper;
use List::MoreUtils qw/any none/;
use List::Util qw/max min/;
use Log::Log4perl qw/get_logger/;
use Readonly;
use Storable qw/retrieve/;

use lib '/usr/local/gkb/modules';
use GKB::Config;
use GKB::CommonUtils;
use GKB::DBAdaptor;
use GKB::NewStableIdentifiers;
use GKB::Utils_esther;

my $logger = get_logger(__PACKAGE__);

Readonly my $MANUALLY_CURATED => 'manually_curated';
Readonly my $ELECTRONICALLY_INFERRED => 'electronically_inferred';

our ($db, $host);
GetOptions(
    "db:s" => \$db,
    "host:s" => \$host
);

$db ||= $GKB::Config::GK_DB_NAME;
$host ||= $GKB::Config::GK_DB_HOST;

$logger->info("Using database $db on $host");

my $stored_data_file = 'stable_id_mapping.stored_data';
$logger->error_die("$stored_data_file does not exist or is empty\n") unless (-e $stored_data_file && -f $stored_data_file && -s $stored_data_file);

my @mappings = @{retrieve($stored_data_file)};
my %instance_id_to_stable_id = %{$mappings[0]};
my %stable_id_to_instance_id = %{$mappings[1]};

my $dba = get_dba($db, $host);
chomp(my $date = `date \+\%F`);
my $instance_edit = GKB::Utils_esther::create_instance_edit($dba, 'Weiser', 'JD', $date);
$logger->info('Instance edit db_id is ' . $instance_edit->db_id);

my %existing_stable_identifiers = map { $_->oldIdentifier->[0] => 1 } get_stable_identifier_instances_with_old_identifiers($dba);
foreach my $instance (get_instances_with_stable_identifiers($dba)) {
    #next unless $instance->db_id == 5490313;
    my %instance_data = ('instance_id' => $instance->db_id);
    my %stable_id_to_version;
    if (!is_electronically_inferred($instance)) {
        $instance_data{'type'} = $MANUALLY_CURATED;
        
        foreach my $stable_id (grep {$_ ne $ELECTRONICALLY_INFERRED} keys %{$instance_id_to_stable_id{$instance->db_id}}) {
            my @versions = sort {$a <=> $b} keys %{$instance_id_to_stable_id{$instance->db_id}{$stable_id}};
            $stable_id_to_version{$stable_id} = max(@versions);
        }
    } else {
        my @source_instances = get_source_for_electronically_inferred_instance($instance);
        next unless $source_instances[0];
        
        if (!$instance->species->[0]) {
            $logger->warn("No species for electronically inferred instance " . $instance->db_id);
            next;
        }

        $instance_data{'type'} = $ELECTRONICALLY_INFERRED;
        $instance_data{'instance_species'} = $instance->species->[0]->displayName;
        $instance_data{'source_instance_id'} = $source_instances[0]->db_id;
        
        
        foreach my $inferred_species (keys %{$instance_id_to_stable_id{$source_instances[0]->db_id}{$ELECTRONICALLY_INFERRED}}) {
            next unless $instance->species->[0]->displayName eq $inferred_species;
            
            foreach my $stable_id (keys %{$instance_id_to_stable_id{$source_instances[0]->db_id}{$ELECTRONICALLY_INFERRED}{$inferred_species}}) {
                if ($stable_id =~ / \((.*)\)$/) {
                    next unless $instance->referenceEntity->[0];
                    my $reference_identifier = $instance->referenceEntity->[0]->variantIdentifier->[0] ?
                        $instance->referenceEntity->[0]->variantIdentifier->[0] :
                        $instance->referenceEntity->[0]->identifier->[0];
                        next unless $reference_identifier && $reference_identifier eq $1;
                }
                
                my @versions = sort {$a <=> $b} keys %{$instance_id_to_stable_id{$source_instances[0]->db_id}{$ELECTRONICALLY_INFERRED}{$inferred_species}{$stable_id}};
                $stable_id_to_version{$stable_id} = max(@versions);
            }
        }
    }
        
    my @old_stable_ids = sort {$stable_id_to_version{$b} <=> $stable_id_to_version{$a}} keys %stable_id_to_version;
    if (@old_stable_ids) {
        my ($selected_old_stable_id, @unselected_old_stable_ids);
        foreach my $old_stable_id (@old_stable_ids) {
            #print "$old_stable_id\t$stable_id_to_version{$old_stable_id}\n";
            if (!$selected_old_stable_id && used_old_stable_id_first(\%instance_data, $old_stable_id, \%stable_id_to_instance_id, \%instance_id_to_stable_id)) {
                $selected_old_stable_id = $old_stable_id;
            } else {
                push @unselected_old_stable_ids, $old_stable_id;
            }
        }
            
        if ($selected_old_stable_id) {
            $selected_old_stable_id =~ s/ \(.*\)$//;
            if ($selected_old_stable_id !~ /^REACT_/) {
                $logger->warn("$selected_old_stable_id does not begin with REACT_ -- skipping");
                next;
            }
            if ($existing_stable_identifiers{$selected_old_stable_id}) {
                $logger->warn("$selected_old_stable_id is already in use -- skipping");
                next;
            }
            
            $logger->info(get_name_and_id($instance) . " has old stable identifier $selected_old_stable_id");
            
            my $current_stable_identifier_instance = $instance->stableIdentifier->[0];
            
            my $existing_old_stable_id = $current_stable_identifier_instance->oldIdentifier->[0];
            if ($existing_old_stable_id) {
                if ($existing_old_stable_id ne $selected_old_stable_id) {                
                    $logger->warn("Existing old stable id $existing_old_stable_id differs from selected old stable id $selected_old_stable_id for " . get_name_and_id($instance));
                }
                next;
            }
            
            $existing_stable_identifiers{$selected_old_stable_id}++;
            $current_stable_identifier_instance->inflate();
            $current_stable_identifier_instance->oldIdentifier($selected_old_stable_id);
            $current_stable_identifier_instance->modified(@{$current_stable_identifier_instance->modified});
            $current_stable_identifier_instance->add_attribute_value('modified', $instance_edit);
            
            $dba->update($current_stable_identifier_instance);
        }
        if (@unselected_old_stable_ids) {        
            $logger->warn(get_name_and_id($instance) . " has unused old stable identifier(s): " . join(',', @unselected_old_stable_ids));
        }
    }
}

sub used_old_stable_id_first {
    my $instance_data = shift;
    my $old_stable_id = shift;
    my $stable_id_to_instance_id = shift;
    my $instance_id_to_stable_id = shift;
    
    my $logger = get_logger(__PACKAGE__);
    
    my $version_instance_first_used = get_version_where_stable_id_first_used_by_instance(
        $old_stable_id,
        $instance_data,
        $stable_id_to_instance_id,
        $instance_id_to_stable_id
    );
    $logger->info($instance_data->{'instance_id'} . " uses $old_stable_id\t$version_instance_first_used");
    
    # Manually curated instances using the stable id
    foreach my $other_instance_id (grep {$_ ne $ELECTRONICALLY_INFERRED} keys %{$stable_id_to_instance_id{$old_stable_id}}) {
        next if $instance_data->{'instance_id'} == $other_instance_id;
        
        my $version_other_instance_first_used = get_version_where_stable_id_first_used_by_instance(
            $old_stable_id,
            {'instance_id' => $other_instance_id, 'type'=> $MANUALLY_CURATED},
            $stable_id_to_instance_id,
            $instance_id_to_stable_id
        );
        $logger->info("Other instance using $old_stable_id: $other_instance_id\t$version_other_instance_first_used");
        if ($version_other_instance_first_used < $version_instance_first_used) {
            return 0;
        }
    }
    
    # Electronically inferred instances
    foreach my $inferred_species (keys %{$stable_id_to_instance_id{$old_stable_id}{$ELECTRONICALLY_INFERRED}}) {
        foreach my $other_instance_id (keys %{$stable_id_to_instance_id{$old_stable_id}{$ELECTRONICALLY_INFERRED}{$inferred_species}}) {
            next if $instance_data->{'source_instance_id'} && $instance_data->{'source_instance_id'} == $other_instance_id;
            
            my $version_other_instance_first_used = get_version_where_stable_id_first_used_by_instance(
                $old_stable_id,
                {'source_instance_id' => $other_instance_id, 'instance_species' => $inferred_species, 'type' => $ELECTRONICALLY_INFERRED},
                $stable_id_to_instance_id,
                $instance_id_to_stable_id
            );
            $logger->info("Other instance using $old_stable_id: $other_instance_id ($inferred_species)\t$version_other_instance_first_used");
            if ($version_other_instance_first_used < $version_instance_first_used) {
                return 0;
            }            
        }
    }
    return 1;
}

sub get_version_where_stable_id_first_used_by_instance {
    my $stable_id = shift;
    my $instance_data = shift;
    my $stable_id_to_instance_id = shift;
    my $instance_id_to_stable_id = shift;
    
    my @versions = ($instance_data->{'type'} eq $MANUALLY_CURATED) ?   
        keys %{$instance_id_to_stable_id{$instance_data->{'instance_id'}}{$stable_id}} :
        keys %{$instance_id_to_stable_id{$instance_data->{'source_instance_id'}}{$ELECTRONICALLY_INFERRED}{$instance_data->{'instance_species'}}{$stable_id}};
    return min(@versions);
}