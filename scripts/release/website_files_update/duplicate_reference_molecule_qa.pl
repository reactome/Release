#!/usr/local/bin/perl  -w
use strict;

use Carp;
use List::MoreUtils qw/any uniq/;
use Try::Tiny;
use Getopt::Long;

use lib '/usr/local/gkb/modules';
use GKB::Config;
use GKB::DBAdaptor;
use GKB::Utils_esther;

use Log::Log4perl qw/get_logger/;
Log::Log4perl->init(\$LOG_CONF);
my $logger = get_logger(__PACKAGE__);

my %seen_identifiers;

my ($db, $host, $fix, $help);

GetOptions(
    "db:s" => \$db,
    "host:s" => \$host,
    "fix" => \$fix,
    "help" => \$help
);

if ($help) {
    print usage_instructions();
    exit;
}

$db ||= 'gk_central';
$host ||= 'reactomecurator.oicr.on.ca';

my $dba = get_dba({'db' => $db, 'host' => $host});

foreach my $reference_molecule (@{$dba->fetch_instance(-CLASS => 'ReferenceMolecule')}) {
    my $identifier = $reference_molecule->identifier->[0];
    next unless $identifier;
    my $name = $reference_molecule->displayName;
    push @{$seen_identifiers{$identifier}}, $reference_molecule;

    if (scalar @{$seen_identifiers{$identifier}} == 2) {
        $logger->info("Processing $identifier\n");
        my ($first_reference_molecule, $second_reference_molecule) = @{$seen_identifiers{$identifier}};
        ATTRIBUTE:foreach my $attribute ($first_reference_molecule->list_valid_attributes) {
            next if any {$attribute eq $_} qw/DB_ID __is_ghost _timestamp created modified/;
            my @attribute_values;
            try {
                $logger->info("Attribute: $attribute\n");
                @attribute_values = get_combined_attribute_values($attribute, $first_reference_molecule, $second_reference_molecule);
            } catch {
                no warnings 'exiting';
                $logger->warn($_ . "\n");
                $logger->warn("During fix, will take attribute values from first reference molecule instance\n");
            };
            
            if ($fix) {            
                next unless (@attribute_values);
                $first_reference_molecule->$attribute(undef);
                $first_reference_molecule->$attribute(@attribute_values);
                $first_reference_molecule->add_attribute_value('modified', get_instance_edit($dba));
                
                $dba->update_attribute($first_reference_molecule, $attribute);
                $dba->update_attribute($first_reference_molecule, 'modified');
            }
        }
        
        foreach my $reverse_attribute ($first_reference_molecule->list_valid_reverse_attributes) {
            $logger->info("Reverse attribute: $reverse_attribute\n");
            
            my @referrer_instances = get_referrer_instances($reverse_attribute, $first_reference_molecule, $second_reference_molecule);
            foreach my $referrer_instance (@referrer_instances) {
                $logger->info($referrer_instance->db_id . "\n");
                
                if ($fix) {                
                    $referrer_instance->add_attribute_value_if_necessary($reverse_attribute, $first_reference_molecule);
                    $referrer_instance->add_attribute_value('modified', get_instance_edit($dba));
                    
                    $dba->update_attribute($referrer_instance, $reverse_attribute);
                    $dba->update_attribute($referrer_instance, 'modified');
                }
            }
        }
        
        if ($fix) {        
            $dba->delete_by_db_id($second_reference_molecule->db_id);
        }
    
        pop @{$seen_identifiers{$identifier}};
    }
}

=head
foreach my $identifier (keys %seen_identifiers) {
    next if scalar @{$seen_identifiers{$identifier}} < 2;
    
    print "ChEBI:$identifier has the following reference molecules:\n";
    foreach my $reference_molecule (@{$seen_identifiers{$identifier}}) {
        print instance_url($reference_molecule) . "\n";    
    }
    print "\n";
}
=cut
                                  
sub get_dba {
    my $parameters = shift;
    
	return GKB::DBAdaptor->new(
	    -dbname => $parameters->{'db'} || $GKB::Config::GK_DB_NAME,
	    -user   => $parameters->{'user'} || $GKB::Config::GK_DB_USER,
	    -host   => $parameters->{'host'} || $GKB::Config::GK_DB_HOST, # host where mysqld is running                                                                                                                              
	    -pass   => $parameters->{'pass'} || $GKB::Config::GK_DB_PASS,
	    -port   => '3306'
	);	
}

{
    my $instance_edit;
    sub get_instance_edit {
        my $dba = shift;
        return $instance_edit if $instance_edit;
        
        chomp(my $date = `date \+\%F`);
        $instance_edit = GKB::Utils_esther::create_instance_edit( $dba, 'Weiser', 'JD', $date);
        $instance_edit->displayName($instance_edit->displayName . ', ' . $date);
        $dba->update_attribute($instance_edit, '_displayName');
        return $instance_edit;
    }
}

sub get_combined_attribute_values {
    my $attribute = shift;
    my $first_reference_molecule = shift;
    my $second_reference_molecule = shift;
    
    my @first_attribute_values = @{$first_reference_molecule->attribute_value($attribute)};
    my @second_attribute_values = @{$second_reference_molecule->attribute_value($attribute)};
    
    return if (!@first_attribute_values && !@second_attribute_values);
    return @second_attribute_values unless @first_attribute_values;
    return @first_attribute_values unless @second_attribute_values;

    my $name = $first_reference_molecule->displayName;
    if ($first_reference_molecule->is_instance_type_attribute($attribute)) {
        if ($first_reference_molecule->is_multivalue_attribute($attribute)) {
            my @combined_attribute_values;
            push @combined_attribute_values, @first_attribute_values;
            foreach my $second_attribute_value (@second_attribute_values) {
                next if any {$second_attribute_value->equals($_)} @first_attribute_values;
                push @combined_attribute_values, $second_attribute_value;
            }
            return @combined_attribute_values;
        } else {
            return $first_attribute_values[0] if $first_attribute_values[0]->equals($second_attribute_values[0]);
            
            die "Different values for $attribute attribute in $name:\n" .
            "First molecule: " . $first_attribute_values[0]->displayName . ' (' . $first_attribute_values[0]->db_id . ')';
            instance_url($first_reference_molecule) . "\n" .
            "Second molecule: " . $second_attribute_values[0]->displayName . ' (' . $second_attribute_values[0]->db_id . ')';
            instance_url($second_reference_molecule) . "\n";
        }
    } else {
        return uniq(@first_attribute_values, @second_attribute_values) if $first_reference_molecule->is_multivalue_attribute($attribute);
        return $first_attribute_values[0] if $first_attribute_values[0] eq $second_attribute_values[0];
        return ($first_reference_molecule->db_id < $second_reference_molecule->db_id ? $first_attribute_values[0] : $second_attribute_values[0]) if $attribute eq "_Protege_id";
        
        die "Different values for $attribute attribute in $name:\n" .
        "First molecule: " . $first_attribute_values[0] . ' ' . instance_url($first_reference_molecule) . "\n" .
        "Second molecule: " . $second_attribute_values[0] . ' ' . instance_url($second_reference_molecule) . "\n";
    }
}

sub instance_url {
    my $instance = shift;
    
    return "http://$host/cgi-bin/instancebrowser?DB=$db&ID=" . $instance->db_id;
}

sub get_referrer_instances {
    my $reverse_attribute = shift;
    my $first_reference_molecule = shift;
    my $second_reference_molecule = shift;
    
    my @first_reference_molecule_referrers = @{$first_reference_molecule->reverse_attribute_value($reverse_attribute)};
    my @second_reference_molecule_referrers = @{$second_reference_molecule->reverse_attribute_value($reverse_attribute)};
    
    my %referrer_id_to_instance;
    $referrer_id_to_instance{$_->db_id} = $_ foreach (@first_reference_molecule_referrers, @second_reference_molecule_referrers);
    return values %referrer_id_to_instance;
}

sub usage_instructions {
    print <<END;
For all reference molecules, duplicate instances are detected by
the identifier slot and a log of how they would be merged is
generated.  By default, the script is run over gk_central hosted on
reactomecurator and the merges can be applied by using the '-fix' flag.

Usage: perl $0 [options]

Options:

-db [db_name]   Source database (default is gk_central)
-host [db_host] Host of source database (default is reactomecurator.oicr.on.ca)
-fix            Merge duplicate reference molecules
-help           Display these instructions

}