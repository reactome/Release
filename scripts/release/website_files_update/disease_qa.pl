#!/usr/local/bin/perl  -w
use strict;

use lib '/usr/local/gkb/modules';

use GKB::CommonUtils;
use GKB::Config;
use GKB::DBAdaptor;

use autodie;
use Carp;
use Getopt::Long;
use List::MoreUtils qw/any uniq/;

my ($host, $db, $help);

&GetOptions(
    "host:s" => \$host,
    "db:s" => \$db,
    "help" => \$help
);

if ($help) {
    print usage_instructions();
    exit;
}

$db ||= $GK_DB_NAME;
$host ||= $GK_DB_HOST;

(my $outfile = $0) =~ s/\.pl/\.txt/;
open(my $output, ">", $outfile);

my $dba = get_dba($db, $host);
my $GEEs = $dba->fetch_instance(-CLASS => 'GenomeEncodedEntity');
my $simple_entities = $dba->fetch_instance(-CLASS => 'SimpleEntity');

my @non_human_entities_with_disease = grep {$_->disease->[0] && !is_human($_)} (@$GEEs, @$simple_entities);
print $output "Non-human entities with disease\n";
print $output join("\t", "DB_ID", "Class", "Display name", "Disease name", "Created by") . "\n";
foreach my $entity (@non_human_entities_with_disease) {
    print $output join("\t",
        $entity->db_id,
        $entity->class,
        $entity->displayName,
        $entity->disease->[0]->displayName,
        get_instance_creator($entity)
    ) . "\n";
}
print $output "\n\n"; 

my $RLEs = $dba->fetch_instance(-CLASS => 'ReactionlikeEvent');
my @disease_RLEs = grep {$_->disease->[0]} @$RLEs;
print $output "Human disease reaction inferred from non-human RLE\n";
print $output join("\t", "Disease DB_ID", "Disease name", "DB_ID of inference source", "Name of inference source") . "\n";
foreach my $disease_RLE (@disease_RLEs) {
    my @disease_RLE_inferred_source = @{$disease_RLE->inferredFrom};
    my @disease_RLE_non_human_inferred_source = grep {!is_human($_)} @disease_RLE_inferred_source;
    if (@disease_RLE_non_human_inferred_source) {
        print $output join("\t",
            $disease_RLE->db_id,
            $disease_RLE->displayName,
            map { $_->db_id . "\t" . $_->displayName } @disease_RLE_non_human_inferred_source
        ) . "\n";
    }
}
print $output "\n\n";

my $EWASs = $dba->fetch_instance(-CLASS => 'EntityWithAccessionedSequence');
my @EWASs_with_GMR_and_null_disease = grep {
    (any { $_->is_a('GeneticallyModified') } @{$_->hasModifiedResidue}) &&
    !$_->disease->[0]
} @$EWASs;
print $output "EWASs with genetically modified residues but no disease\n";
print $output join("\t", "EWAS DB_ID", "EWAS display name", "Created by") . "\n";
foreach my $EWAS (@EWASs_with_GMR_and_null_disease) {
    print $output join("\t",
        $EWAS->db_id,
        $EWAS->displayName,
        get_instance_creator($EWAS)
    ) . "\n";
}
print $output "\n\n";

my $sets = $dba->fetch_instance(-CLASS => 'EntitySet');
my $complexes = $dba->fetch_instance(-CLASS => 'Complex');
my $polymers = $dba->fetch_instance(-CLASS => 'Polymer');
my @composite_entities = (@$sets, @$complexes, @$polymers);

my @composite_entities_with_disease = grep {$_->disease->[0]} @composite_entities;
print $output "Sets, complexes, and polymers with disease having members/candidates, components, or repeated units without disease\n";
print $output join("\t", "Entity DB_ID", "Entity Class", "Entity Display name", "Created by", "DB_IDs of components without disease") . "\n";
foreach my $composite_entity (@composite_entities_with_disease) {
    my @components = get_components($composite_entity);
    my @components_without_disease = grep {!$_->disease->[0]} @components;
    if (@components_without_disease) {
        print $output join("\t",
            $composite_entity->db_id,
            $composite_entity->class,
            $composite_entity->displayName,
            get_instance_creator($composite_entity),
            join("|", uniq map {$_->db_id} @components_without_disease)
        ) . "\n";
    }
}
print $output "\n\n";

my @composite_entities_without_disease = grep {!$_->disease->[0]} @composite_entities;
print $output "Sets, complexes, and polymers without disease having members/candidates, components, or repeated units with disease\n";
print $output join("\t", "Entity DB_ID", "Entity Class", "Entity Display name", "Created by", "DB_IDs of components with disease") . "\n";
foreach my $composite_entity (@composite_entities_without_disease) {
    my @components = get_components($composite_entity);
    my @components_with_disease = grep {$_->disease->[0]} @components;
    if (@components_with_disease) {
        print $output join("\t",
            $composite_entity->db_id,
            $composite_entity->class,
            $composite_entity->displayName,
            get_instance_creator($composite_entity),
            join("|", uniq map {$_->db_id} @components_with_disease)
        ) . "\n";
    }
}
print $output "\n\n";

my @RLEs_with_disease = grep {$_->disease->[0]} @$RLEs;
print $output "Reaction like events with disease having participants without disease\n";
print $output join("\t", "RLE DB_ID", "RLE Display name", "Created by", "DB_IDs of participants without disease") . "\n";
foreach my $RLE (@RLEs_with_disease) {
    my @participants = map { get_components($_) } get_participants($RLE);
    my @participants_without_disease = grep {!$_->disease->[0]} @participants;
    if (@participants_without_disease) {
        print $output join("\t",
            $RLE->db_id,
            $RLE->displayName,
            get_instance_creator($RLE),
            join("|", uniq map {$_->db_id} @participants_without_disease)
        ) . "\n";
    }
}
print $output "\n\n";

my @RLEs_without_disease = grep {!$_->disease->[0]} @$RLEs;
print $output "Reaction like events without disease having participants with disease\n";
print $output join("\t", "RLE DB_ID", "RLE Display name", "Created by", "DB_IDs of participants with disease") . "\n";
foreach my $RLE (@RLEs_without_disease) {
    my @participants = map { get_components($_) } get_participants($RLE);
    my @participants_with_disease = grep {$_->disease->[0]} @participants;
    if (@participants_with_disease) {
        print $output join("\t",
            $RLE->db_id,
            $RLE->displayName,
            get_instance_creator($RLE),
            join("|", uniq map {$_->db_id} @participants_with_disease)
        ) . "\n";
    }
}
print $output "\n\n";

sub get_participants {
    my $RLE = shift;
    
    my @inputs = @{$RLE->input};
    my @outputs = @{$RLE->output};
    my @catalysts = map {$_->physicalEntity->[0]} @{$RLE->catalystActivity};
    my @regulators = map {$_->regulator->[0]} @{$RLE->reverse_attribute_value('regulatedEntity')};
    
    return (@inputs, @outputs, @catalysts, @regulators);
}

sub usage_instructions {
    return <<END;
    
    This script checks for disease inconsistencies in the database
    and reports the following in a text file with the same name as
    the script:
    
    * Non-human EWASs, GEEs, and Simple Entities with disease specified
    * Human disease RLEs inferred from non-human RLE(s)
    * EWAS with genetically modified residue but no disease
    * Sets, complexes, polymers with disease having components without disease
    * Sets, complexes, polymers without disease having components with disease 
    * RLEs with disease that have participants without disease
    * RLEs without disease that have participants with disease
    
    Usage: perl $0 [options]
    
    Options:
    
    -db [db_name]   Source database (default is $GKB::Config::GK_DB_NAME)
    -host [db_host] Host of source database (default is $GKB::Config::GK_DB_HOST)
    -help           Display these instructions
END
}