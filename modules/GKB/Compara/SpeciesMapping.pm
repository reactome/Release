=head1 NAME

GKB::Compara::SpeciesMapping;

=head1 SYNOPSIS

=head1 DESCRIPTION

This is a class for mapping the source species ensembl gene ids
to target species uniprot ids through target species Ensembl 
gene ids.

=head1 SEE ALSO

=head1 AUTHOR

Joel Weiser E<lt>joel.weiser@oicr.on.caE<gt>

Copyright (c) 2012 European Bioinformatics Institute, Cold Spring
Harbor Laboratory, and Ontario Institute for Cancer Research.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

package GKB::Compara::SpeciesMapping;

use strict;

use Data::Dumper;
use File::Basename;
use GKB::Config;
use GKB::Config_Species;
use GKB::Compara::BioMart;
use GKB::Compara::Target;
use GKB::Compara::Utils;
use Log::Log4perl qw/get_logger/;

Log::Log4perl->init(dirname(__FILE__) . '/compara_log.conf');

use vars qw(@ISA $AUTOLOAD %ok_field);
use Bio::Root::Root;

@ISA = qw(Bio::Root::Root);

# List the object variables here, so that they can be checked
for my $attr
    (qw(
	
	) ) { $ok_field{$attr}++; }

sub AUTOLOAD {
    my $self = shift;
    my $attr = $AUTOLOAD;
    $attr =~ s/.*:://;
    return unless $attr =~ /[^A-Z]/;  # skip DESTROY and all-cap methods
    $self->throw("invalid attribute method: ->$attr()") unless $ok_field{$attr};
    $self->{$attr} = shift if @_;
    return $self->{$attr};
}  

sub new {
    my($pkg) = @_;
    
    my $self = bless {}, $pkg;

    $self->clear_variables();

    return $self;
}

sub clear_variables {
    my ($self) = @_;

}

# Needed by subclasses to gain access to class variables defined in
# this class.
sub get_ok_field {
    return %ok_field;
}

sub print_species_keys {
    print "$0: species keys=", keys %species_info, "\n";
}


#The species_info hash (defined in Config_Species.pm) contains species information, this is now looped over to produce mappings from the "from species" to all others - keys are four letter abbreviations, e.g. hsap for Homo sapiens
sub do_mapping_for_target_species {
    my ($self, $opt_from, $opt_sp, $opt_core, $opt_test, $source_mapping_ref, $homology) = @_;

    my $logger = get_logger(__PACKAGE__);

    my %source_mapping = %{$source_mapping_ref};

    foreach my $species_key (keys %species_info) {
	$logger->info("species_key=$species_key\n");
    
	#skip source species
	if ($species_key eq $opt_from) {
	    $logger->info("skipping source species\n");
	    next;
	}
    
	#if $opt_sp is specified, only this species is taken as target species
	if ($opt_sp && !($species_key eq $opt_sp)) {
	    $logger->info("single species selected ($opt_sp), skipping $species_key\n");
	    next;
	}

	#NOTE: this conditional would have to be removed if the switch was working again, as then all species would be run in one go
	if ($opt_core) { #run only core species
	    if (!(($species_info{$species_key}->{'compara'} && $species_info{$species_key}->{'compara'} eq 'core'))) {
		$logger->info("running core species only\n");
		next;
	    }
	} else { #run only pancompara species, i.e. all non-core-flagged species
	    if (($species_info{$species_key}->{'compara'} && $species_info{$species_key}->{'compara'} eq 'core')) {
	        $logger->info("running non-core species only\n");
	        next;
	    }
	}
    
	$logger->info("processing species_key=$species_key\n");
	if (-s "$opt_from\_$species_key\_mapping.txt") {
	    $logger->info("mapping file $opt_from\_$species_key\_mapping.txt already exists, skipping!\n");
	} else {
	    my $out;
	    unless (open($out, '>', "$opt_from\_$species_key\_mapping.txt")) {
	        $logger->error("could not open file for writing: $opt_from\_$species_key\_mapping.txt\n");
		next;
	    }

	    #Prepare mapping for source species - from Ensembl gene id to protein id
	    my $target = GKB::Compara::Target->new();		
	    my %mapping = $target->prepare_target_mapping_hash($species_key);

	    $logger->info("Target mapping hash size=" . scalar(keys(%mapping)) . "\n");
	
	    #Prepare homology hash based on Ensembl compara (most species are in Pan compara, some only in Ensembl core compara) - this is based on Ensembl gene identifiers
	    my %homology = $homology->prepare_homology_hash($species_key, $opt_from, $opt_test);
    
	    $logger->info("Homology hash size=" . scalar(keys(%homology)) . "\n");
	    $logger->info("source_mapping size=" . scalar(keys(%source_mapping)) . "\n");
	    $logger->info("mapping size=" . scalar(keys(%mapping)) . "\n");
	    my $seen_counter = 0;
		
	    #Prepare homology file for protein ids (= final mapping file)
	    foreach my $source_uni (sort keys %source_mapping) {
	        #drill down for each and keep unique target identifiers
	        $logger->info("source_uni=$source_uni\n");
    
	        my %seen;
	        foreach my $source_ensg (@{$source_mapping{$source_uni}}) {
		    $logger->info("source_ensg=$source_ensg\n");

		    foreach my $to_ensg (@{$homology{$source_ensg}}) {
		        $logger->info("to_ensg=$to_ensg\n");

		        foreach my $target_id (@{$mapping{$to_ensg}}) {
			    $logger->info("target_id=$target_id\n");

			    $seen{$target_id}++;
			}
		    }
		}
		next unless keys %seen;
		$seen_counter++;
		my $row = $source_uni. "\t". join (' ', keys %seen). "\n";
		print $out $row;
		$logger->info("row=$row\n");
	    }
	    $logger->info("Final hash printed, seen_counter=$seen_counter\n");
	    close($out);
	}
    }
}

1;
