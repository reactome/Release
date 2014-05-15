=head1 NAME

GKB::Compara::Homology;

=head1 SYNOPSIS

=head1 DESCRIPTION

This is a class for creating the homology relationships between source species genes 
and target species genes using Ensembl gene ids.

=head1 SEE ALSO

=head1 AUTHOR

Joel Weiser E<lt>joel.weiser@oicr.on.caE<gt>

Copyright (c) 2012 European Bioinformatics Institute, Cold Spring
Harbor Laboratory, and Ontario Institute for Cancer Research.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

package GKB::Compara::Homology;

use strict;

use GKB::Config;
use lib "$ENSEMBL_API_DIR/ensembl/modules";
use lib "$ENSEMBL_API_DIR/ensembl-compara/modules";
use lib "$ENSEMBL_API_DIR/bioperl-1.2.3";

use autodie;
use Bio::EnsEMBL::Registry;
use Bio::Root::Root;
use Data::Dumper;
use File::Basename;
use GKB::Config_Species;
use Log::Log4perl qw/get_logger/;
use Try::Tiny;

Log::Log4perl->init(dirname(__FILE__) . "/compara_log.conf");

use vars qw(@ISA $AUTOLOAD %ok_field);

@ISA = qw(Bio::Root::Root);

# List the object variables here, so that they can be checked
for my $attr
    (qw(
		registry
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

	$self->registry(undef);
}

# Needed by subclasses to gain access to class variables defined in
# this class.
sub get_ok_field {
	return %ok_field;
}

sub set_registry {
    my ($self, $core, $ensembl_db_ver) = @_;
	
    my $registry = 'Bio::EnsEMBL::Registry';
	
    my @dbs;
    
    # Need Pan Compara if not a core species
    if (!$core) {	
	push @dbs, { 
	    -host => 'mysql.ebi.ac.uk',
	    -user => 'anonymous',
	    -port => 4157,
	    -DB_VERSION => $ensembl_db_ver
	};
    }
    
    # Always need core ensembl db for human genome (and also for any core species)
    push @dbs, {
	-host => 'ensembldb.ensembl.org',
	-user => 'anonymous',
	-port => 5306,
	-DB_VERSION => $ensembl_db_ver
    };

    $registry->load_registry_from_multiple_dbs(@dbs);
#The following is a "switch" method, which worked once when Ensembl and Ensembl Genomes were on the same Ensembl release version, but not any longer when Ensembl Genomes lacked behind one version, even when setting all db_version flags to the same version number. Reason for this unknown at present, I'm leaving the code in in case this works again in the future. If so, all species can be run within one go, and no core flag is needed. And no wrapper script either...
=head
	$registry->load_registry_from_multiple_dbs(
	{
        '-host'    => 'mysql.ebi.ac.uk',
        '-user'    => 'anonymous',
		'-port'    => 4157,
	},
	
	{
        '-host'     => 'ensembldb.ensembl.org',
        '-user'     => 'anonymous',
	} 
);
=cut
	
	$self->registry($registry);
}

#creates the homology hash with source species gene ids as keys, and target species gene ids as values 
sub prepare_homology_hash {
    my ($self, $sp, $opt_from, $opt_test) = @_;
    
    my $logger = get_logger(__PACKAGE__);
    
    my $db;
    my %homology = ();
    

    if ($species_info{$sp}->{'compara'} && $species_info{$sp}->{'compara'} eq 'core') {
        $db = "Multi"; #name of compara database in core compara
    } else {
        $db = "pan_homology"; #name of compara database in Ensembl Genomes pan compara
    }

    $logger->info("sp=$sp, db=$db\n");
  
    my ($gdb_a, $mlss_a, $h_adaptor) = $self->get_compara_adaptors($db);
    if (!(defined $gdb_a && defined $mlss_a && defined $h_adaptor)) {
    	$logger->error("there was a problem with one of the Compara adaptors for sp=$sp, db=$db, aborting!\n");
    	return %homology;
    }

    $logger->info("prepare_orthopair_files.prepare_homology_hash: from name=" . $species_info{$opt_from}->{'name'}->[0] . "\n");
    if (!(defined $species_info{$opt_from}->{'pan_name'})) {
        $logger->warn("from pan_name is undef for $opt_from!!\n");
    } else {
        $logger->info("from pan_name=" . $species_info{$opt_from}->{'pan_name'} . "\n");
    }

    #in Pan compara, usually the general species name is used, but some species have "unusual" names, specified under 'pan_name' in the species hash
    my $from_name = $species_info{$opt_from}->{'pan_name'}?$species_info{$opt_from}->{'pan_name'}:$species_info{$opt_from}->{'name'}->[0];

    $logger->info("from_name=$from_name\n");

    #    $from_species = $gdb_a->fetch_by_name_assembly($from_name);
    my $from_species = fetch_by_name_assembly($gdb_a, $from_name);
    if (!(defined $from_species)) {
    	$logger->error("there was a problem with from_name=$from_name for sp=$sp, db=$db, opt_from=$opt_from, aborting!\n");
    	return %homology;
    }

    if (defined $species_info{$sp}->{'name'}->[0]) {
        $logger->info("to name=" . $species_info{$sp}->{'name'}->[0] . "\n");
    } else {
        $logger->warn("to name is undef for $sp\n");
    }
    $logger->info("to pan_name=" . $species_info{$sp}->{'pan_name'} . "\n");

    my $to_name = $species_info{$sp}->{'pan_name'}?$species_info{$sp}->{'pan_name'}:$species_info{$sp}->{'name'}->[0];

    $logger->info("to_name=$to_name\n");

    #    my $to_species = $gdb_a->fetch_by_name_assembly($to_name);
    my $to_species = fetch_by_name_assembly($gdb_a, $to_name);
    if (!(defined $to_species)) {
    	$logger->error("there was a problem with to_name=$to_name for sp=$sp, db=$db, opt_from=$opt_from, aborting!\n");
    	return %homology;
    }

    $logger->info("to_species=$to_species\n");

    my $homologies = [];
    try {
	my $orth_mlss = $mlss_a->fetch_by_method_link_type_GenomeDBs('ENSEMBL_ORTHOLOGUES', [$from_species, $to_species]);
    	$homologies = $h_adaptor->fetch_all_by_MethodLinkSpeciesSet($orth_mlss);
    } catch {
	$logger->warn($_);
    };

    $logger->info("from_name=$from_name, from_species=$from_species, to_name=$to_name, to_species=$to_species\n");
    my $homology_count = scalar(@{$homologies});
    $logger->info("looping over $homology_count homologies\n");
    my $homology_counter = 0;
    my $push_counter = 0;

    foreach my $homology (@{$homologies}) {  #these appear to be always pairs, not clusters of more members...  
        if ($homology_counter % 100 == 0) {
            $logger->info("homology_counter=$homology_counter (" . (100*$homology_counter)/$homology_count . "%), homology.description=" . $homology->description . " homology.taxonomy_level=" .  $homology->taxonomy_level . "\n");
        }
        
        last if ($opt_test && $homology_counter == 10); 
        
        my $from_m = $homology->get_Member_by_GenomeDB($from_species);
        my $to_m = $homology->get_Member_by_GenomeDB($to_species);
        foreach my $member (@{$from_m}) {
	    foreach my $to_member (@{$to_m}) {
		next unless $to_member;
		# Diagnostic, feel free to comment out!
		if (($to_name eq "osat" || $to_name eq "Oryza sativa" || $to_name eq "oryza_sativa") && $homology_counter%10 == 0) {
		    $logger->debug("from=" . $member->stable_id . ", to=" . $to_member->stable_id . "\n");
		}
		push @{$homology{$member->gene_member->stable_id}}, $to_member->gene_member->stable_id;
		$push_counter++;
	    }
	}
	$homology_counter++;
    }

    $logger->info("print homology hash, push_counter=$push_counter\n");
    
    #print homology hash
    open(my $homol, '>', "$opt_from\_$sp\_homol_mapping.txt");
    foreach (sort keys %homology) {
	print $homol $_, "\t", join (' ', @{$homology{$_}}), "\n";
    }
    close($homol);

    $logger->info("done\n");
    return %homology;
}

sub fetch_by_name_assembly {
    my ($gdb_a, $name) = @_;

    my $logger = get_logger(__PACKAGE__);

    if (!(defined $name)) {
    	$logger->error("name is undef, aborting!\n");
    	return undef;
    }

    my $species = undef;
    try {$species = $gdb_a->fetch_by_registry_name($name);};
    if (!(defined $species)) {
        $logger->warn("there was a problem with name=$name, trying new name=");
        $name =~ s/ +/_/g;
        $name = lc($name);
        $logger->warn("$name\n");
        try {$species = $gdb_a->fetch_by_registry_name($name);};
        if (!(defined $species)) {
            $logger->error("there was a problem with name=$name, aborting!\n");
            return undef;
        }
        $logger->warn("succeeded with new name=$name!\n");
    }

    return $species;
}

# Takes a single argumant, the name of the compara database for a given species.
# Returns an array:
#
# (GenomeDBAdaptor, MethodLinkSpeciesSetAdaptor, HomologyAdaptor)
sub get_compara_adaptors {
    my ($self, $db) = @_;

    my $compara_dba;
    my $gdb_a = undef;
    my $mlss_a = undef;
    my $h_adaptor = undef;
    my $eval_count = 0;
    while ($eval_count < 10 && !(defined $gdb_a && defined $mlss_a && defined $h_adaptor)) {
	    eval {
	    	$compara_dba = $self->registry->get_DBAdaptor($db,'compara');
	    	$gdb_a = $compara_dba->get_GenomeDBAdaptor();
	    	$mlss_a = $compara_dba->get_MethodLinkSpeciesSetAdaptor();
	    	$h_adaptor = $compara_dba->get_HomologyAdaptor();
	    };
	    if (!(defined $gdb_a && defined $mlss_a && defined $h_adaptor)) {
	    	sleep(10);
	    } else {
	    	last;
	    }

	    $eval_count++;
    }

    my @adaptors = ($gdb_a, $mlss_a, $h_adaptor);

    return @adaptors;
}

1;
