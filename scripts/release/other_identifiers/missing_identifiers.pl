#!/usr/local/bin/perl
use strict;
use warnings;

use lib '/usr/local/gkb/modules';

use autodie;

use GKB::CommonUtils;
use GKB::Config_Species;

my $directory = 'missing_identifiers';
mkdir $directory unless (-e $directory);
chdir $directory;

foreach my $species (@species) {
    my $species_name = $species_info{$species}->{'name'}->[0];
    my %current_RGPs = map { get_identifier($_) => $_ } _get_RGPs_with_other_identifiers("test_reactome_63", $species_name);
    my %previous_RGPs = map { get_identifier($_) => $_ } _get_RGPs_with_other_identifiers("test_reactome_62", $species_name);
    
    my (@common_RGP_identifiers, @missing_RGP_identifiers);
    foreach my $previous_RGP_identifier (keys %previous_RGPs) {
        if (exists $current_RGPs{$previous_RGP_identifier}) {
            push @common_RGP_identifiers, $previous_RGP_identifier;
        } else {
            push @missing_RGP_identifiers, $previous_RGP_identifier;
        }
    }
    report_to_file($species_name, ["Missing RGP identifiers", @missing_RGP_identifiers]) if @missing_RGP_identifiers;
    
    foreach my $common_identifier (@common_RGP_identifiers) {
        
        my %current_other_identifiers = map { $_ => 1 } @{$current_RGPs{$common_identifier}->otherIdentifier};
        my %previous_other_identifiers = map { $_ => 1 } @{$previous_RGPs{$common_identifier}->otherIdentifier};
        
        my (@common_other_identifiers, @missing_other_identifiers);
        foreach my $previous_other_identifier (keys %previous_other_identifiers) {
            if (exists $current_other_identifiers{$previous_other_identifier}) {
                push @common_other_identifiers, $previous_other_identifier;
            } else {
                push @missing_other_identifiers, $previous_other_identifier;
            }
        }
        report_to_file($species_name, ["Missing other identifiers for $common_identifier", @missing_other_identifiers]) if @missing_other_identifiers;
    }
}

sub get_identifier {
    my $RGP_instance = shift;
    
    return $RGP_instance->variantIdentifier->[0] || $RGP_instance->identifier->[0];
}

sub report_to_file {
    my $file_name = shift;
    my $collection = shift;
    
    open(my $fh, '>>', $file_name);
    print $fh "$_\n" foreach @{$collection};
    close $fh;
}

# RGP is the database class ReferenceGeneProduct
sub _get_RGPs_with_other_identifiers {
    my $db = shift;
    my $species_name = shift;
    
    my $reference_gene_products = get_dba($db, 'reactomerelease.oicr.on.ca')->fetch_instance_by_remote_attribute('ReferenceGeneProduct', [['species._displayName', '=', [$species_name]]]);
    return grep {$_->otherIdentifier->[0]} @{$reference_gene_products};
}
