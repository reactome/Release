#!/usr/local/bin/perl -w

#This script should be run over a release database as it requires stable identifiers to be present
#This script produces a tab delimited file for submission to goa - including Reactome annotations for cellular components, molecular function and biological process.

#NOTE: after running this script, run goa_submission_stats.pl to produce stats

# Make sure you don't have "competing" libraries...
# for use @CSHL
use lib "/usr/local/gkb/modules";
# for use @HOME
use lib "$ENV{HOME}/bioperl-1.0";
use lib "$ENV{HOME}/GKB/modules";
use GKB::Instance;
use GKB::DBAdaptor;
use GKB::Utils;
use Data::Dumper;
use Getopt::Long;
use strict;

# Database connection
our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db, $opt_date, $opt_debug);

(@ARGV) || die "Usage: $0 -user db_user -host db_host -pass db_pass -port db_port -db db_name -date date(YYYYMMDD) -debug\n";

&GetOptions("user:s", "host:s", "pass:s", "port:i", "db:s", "date:i", "debug");

$opt_db || die "Need database name (-db).\n";
#$opt_date || die "Need date (-date).\n";  #need to revisit this, at present some instances don't have InstanceEdits attached, this should be fixed

my $dba= GKB::DBAdaptor->new
    (
     -user   => $opt_user || '',
     -host   => $opt_host,
     -pass   => $opt_pass,
     -port   => $opt_port,
     -dbname => $opt_db,
     -DEBUG => $opt_debug
     );

my $outfile = "gene_association.reactome"; # Output file for submission to GO

# If creation of a filehandle is unsuccessful, the following error message
# prints and the program terminates.
open(FILE, ">$outfile") or die "$0: could not open file $outfile\n";

print FILE "!gaf-version: 2.0\n"; # Header line for GO

#We had feedback about a number of our submissions, stating that the literature reference provided didn't contain direct evidence for the GO assignment we have made. For these cases, the EXP evidence code needs to be changed to NAS, according to the following list:
my $checkfile = "EXP_NAS_list.txt";

# If the evidence code correction list can't be opened an error is printed
# and the program terminates
open(CHECK, "<$checkfile") or die "$0: could not open file $checkfile\n";


#print FILE "DB\tUniprot_accession\tUniprot_id\tQualifier\tGO_id\tReference\tEvidence_code\tWith_From\tAspect\tDB_object_name\tSynonym\tDB_object_type\tTaxon\tDate\tAssigned_by\n";

my $DB = 'UniProtKB'; # Information on proteins is from UnitProtKB
my $object_type = 'protein'; # The entries always concern proteins
			     #(note: should check with GO to determine
			     #       if they look at rRNA/tRNA genes)
			     
my $assigned_by = 'Reactome';	# Reactome is always the 'assigner' of entries
my %seen; 			# Assures assertions are not duplicated (key -assertion value -number of times occurred)
my @rows; 			# Holds assertions to be printed
my %date; 			# Holds dates of assertions so they are not lost when the assertion is manipulated
my (@manual, @electronic); 	# Holds events that are manually/electronically inferred
my %exp; 			# Holds events/assertions that have EXP codes
my (%source, %elec_source); 	# Holds the events that infer the manual/experimental events
my %assignment;			# Holds the pertinent information for the inferring event
my %seen_in_event;		# Assures assertions in the current event are not duplicated as above
my $protein_class = &GKB::Utils::get_reference_protein_class($dba); # Reference gene product is the string obtained
my %complexes;
my %subcomplexes;
my @microbial_species_to_exclude = (813, 562, 491, 90371, 1280, 5811);
my @species_with_alternate_go_compartment = (11676, 211044, 1491);

my $ar = $dba->fetch_instance(-CLASS => 'Event'); # Obtains a reference to the array of all Reactome events

# Each event in Reactome is processed
foreach my $ev (@{$ar}) {
    print "$0: ev->db_id=" . $ev->db_id() . "\n";
    %seen_in_event = (); # Hash reset for the next event
    
    #collect electronically inferred events to be handled further down
    if ($ev->EvidenceType->[0] && ($ev->EvidenceType->[0]->Name->[0] eq 'inferred by electronic annotation')) { #the plan is to run this script before the electronic inference is run, so this is here just in case it is run afterwards...
		foreach(@{$ev->InferredFrom}) {
			push @{$elec_source{$ev->db_id}}, $_;
		}
		push @electronic, $ev;
		next;
    }
    
    #collect manually inferred events to be handled further down
    if ($ev->InferredFrom->[0]) {  # Executes if the event is inferred
		foreach (@{$ev->InferredFrom}) { # Executes for each place the event was inferred from
	    	push @{$source{$ev->db_id}}, $_; # Stores the sources of inference with the event's id as the key
		}
		push @manual, $ev; # Stores the events to be handled later
		next;
    }
    
    my $reactionid = "REACTOME:" . $ev->StableIdentifier->[0]->Identifier->[0] || next; # Reaction stable id 

    #my ($evidence_code, $reference) = check_reference($ev); # Stores the event's evidence code and
							    # the reference of the event
    
    #next unless $evidence_code; # The current event is skipped if there is no evidence code
    
    # Executes if the event is 'reaction-like'
    if ($ev->is_a('ReactionlikeEvent')) {
		#my $reactionid = "REACTOME:" . $ev->StableIdentifier->[0]->Identifier->[0]; # Reaction stable id 
	
		# Following checks for GO_cellular_component (Hence "C" in the row assignment below)
	
		my $complex_ar = find_complexes($ev); # Find complexes for the event
	
		# Break down and annotate each complex and its parts
		foreach my $complex (@{$complex_ar}) {	  
			break_complex({
				event => $ev, 
				complex => $complex,
				letter => "C",
				reactionid => $reactionid
			}); # Processes each complex (one assertion for the complex and one for each protein in it)
		}
	
		my $prot_ar = find_proteins($ev); # Holds a reference to all proteins associated with the event.
		next unless $prot_ar->[0]; # The event is skipped if no proteins are found for the event
	
		# Executes for each protein in the current event
		PROT:foreach my $prot (@{$prot_ar}) {
	
			# The 'next unless' statements here skip processing the protein unless
			# it meets the conditions specified
			next PROT unless $prot->ReferenceEntity->[0]; # Must have a reference entity
			next PROT unless ($prot->ReferenceEntity->[0]->ReferenceDatabase->[0]->Name->[0] eq 'UniProt'); # Must be referencing UniProt
			next PROT unless $prot->Species->[0]; # Must associate with a species
			next PROT unless $prot->Species->[0]->CrossReference->[0]; # Must have access to the species name 
			
			my $uni = $prot->ReferenceEntity->[0]->Identifier->[0]; # Obtains UniProt ID
			my $object_symbol = assign_object_symbol($prot->ReferenceEntity->[0]); # Obtains alternate UniProt ID
			my $go;
			
			my $taxon = $prot->Species->[0]->CrossReference->[0]->Identifier->[0]; # Species ID (by taxon number) obtained
			
			if ($taxon ~~ @species_with_alternate_go_compartment) {
				if ($prot->GO_CellularComponent->[0]) {
					$go = $prot->GO_CellularComponent->[0]->Accession->[0];
				} else {
					next PROT;
				}
			} else {
				next PROT unless $prot->Compartment->[0]; # Must have a cellular compartment
				$go = $prot->Compartment->[0]->Accession->[0];
				next PROT unless $go;
			}
			next PROT if $taxon ~~ @microbial_species_to_exclude;		
		
			#my @ref_ar = get_prot_reference($prot);
		
			#foreach my $ref (@ref_ar) {
			
				#	my $prot_evidence_code = $ref->[0];
				#	my $prot_reference = $ref->[1];
		
		
				# Entry for the protein stored in $row
				my $row = $DB."\t".$uni."\t".$object_symbol."\t\tGO:".$go."\t".$reactionid."\t"."TAS"."\t\t"."C"."\t\t\t".$object_type."\ttaxon:".$taxon; 
				my $rowreg = qr/$row/;				
				next if (@rows ~~ $rowreg); # If the assertion already exists, skip it here
				next if $seen_in_event{$row}++; # Entry discarded/skipped if duplicated
				push @{$assignment{$ev}}, [$uni, $go, $taxon, $reactionid, "C"]; #this is needed for copying the assignment to manually inferred events further down
				get_date($prot, $row);
				next if $seen{$row}++; # Entry discarded if duplicated with the same modification date
		
				push @rows, $row; #Entry stored for printing to exported file
								  #-- Don't print it yet as the same entry may come up with a later date
			#}
		}
	
		#check for GO_molecular_function
		CAT:foreach my $cat (@{$ev->CatalystActivity}) { # Obtains the entities with catalyst activity
														 # (as defined by Reactome -- enzyme/transport
														 #  capability) in the current event
							 
	    	next unless $cat->Activity->[0]; # Skip the entity unless it has molecular function
	    	my $go = $cat->Activity->[0]->Accession->[0]; # Molecular function GO accession
	          
	    	# Unless the entity has at least one physical component
	    	# skip it and print a message
	    	if (scalar(@{$cat->PhysicalEntity}) < 1) { 
	    		print "$0: no physical entities for catalyst activity DB_ID=" . $cat->db_id() . "\n";
	    		next;
	    	}
	    
	    	my @proteins; # Holds the proteins for the catalyst activity
	    	my $rps_ar; # Holds each unique reference protein sequence
	    
	  		#Note: This logic checks complexes and intentionally excludes heteromeric Complexes and Complexes with EntitySets as components, as it wouldn't be known which of the components actually is the active one - deal with them separately?
	   	 	my $cpe = $cat->PhysicalEntity->[0];
	    	if ($cpe->is_a('Complex')) {
				my $complex = $cpe;
				my @ref_ar = get_prot_reference($cat);
			
				foreach my $ref (@ref_ar) {
					break_complex({
						event => $ev, 
						complex => $complex, 
						letter => "F", 
						go => $go, 
						reactionid => $reactionid,
						catalyst => $cat, 
						reference => $ref
					}); # Processes each complex (one assertion for the complex and one for each protein in it) 
				}
            } else { # Obtains the reference protein sequences from the "non-complex" entity (i.e. monomers)
				my @entities = @{$cat->PhysicalEntity};
		
				# Each entity in the catalyst activity is processed
				foreach my $entity (@entities) {
					if ($entity->hasMember) { # If the entity is a defined set
						push @proteins, @{$entity->hasMember}; 
					} elsif ($entity->is_a('EntityWithAccessionedSequence')) { # If the entity is a protein
						push @proteins, $entity;
					}
				}	
		
				# Obtains all reference protein sequences in the catalyst activity
				$rps_ar = $cat->follow_class_attributes(-INSTRUCTIONS =>
						{
							'CatalystActivity' => {'attributes' => [qw(physicalEntity)]},
							'EntitySet' => {'attributes' => [qw(hasMember)]},
							'EntityWithAccessionedSequence' => {'attributes' => [qw(referenceEntity)]}},
						 
						    -OUT_CLASSES => [($protein_class)]
						);
		
				# Processes each protein for the catalyst activity
				foreach my $prot (@proteins) {
			
					next unless $prot->is_a('EntityWithAccesionedSequence'); # Protein must be an EWAS		
					my $rps = $prot->ReferenceEntity->[0]; # The reference protein sequence of the protein
					# Exclusion of proteins not meeting the following conditions
					next unless ($rps->ReferenceDatabase->[0]->Name->[0] eq 'UniProt'); # Must be a UniProt protein
					next unless $rps->Species->[0]; # Must reference a species
					next unless $rps->Species->[0]->CrossReference->[0]; # Must have access to the species name
				
					my $uni = $rps->Identifier->[0]; # UniProt ID
					my $object_symbol = assign_object_symbol($rps); # Secondary ID
				
					next unless $go; # Protein excluded if there is no GO accession
				
					my $taxon = $rps->Species->[0]->CrossReference->[0]->Identifier->[0]; # Species
					next if $taxon ~~ @microbial_species_to_exclude;
					
					my @ref_ar = get_prot_reference($cat);
			
					# Processes each reference for the catalyst activity
					foreach my $ref (@ref_ar) {
						my $prot_reference = $ref->[0];
						my $prot_evidence_code = $ref->[1];
				
						my $row = $DB."\t".$uni."\t".$object_symbol."\t\tGO:".$go."\t".$reactionid."\t".$prot_evidence_code."\t\t"."F"."\t\t\t".$object_type."\ttaxon:".$taxon; # Holds line
						my $rowreg = qr/$row/;				
						next if (@rows ~~ $rowreg); # If the assertion already exists, skip it here						
						next if $seen_in_event{$row}++; # Protein skipped if it is a duplicate
						push @{$assignment{$ev}}, [$uni, $go, $taxon, $reactionid, "F"]; 
						get_date($cat, $row);
						next if $seen{$row}++; # Protein skipped if it is a duplicate with the same date 
						push @rows, $row; # Line stored for writing to file later on
					}
				}
	    	}	
		} # End of catalyst block
    } elsif ($ev->is_a('Pathway')) { # Executed if the event is a pathway (i.e. not reactionlike)
		#check for GO_biological_process
		next unless $ev->GoBiologicalProcess->[0]; # Pathway skipped if no biological process associated with it
		my $go = $ev->GoBiologicalProcess->[0]->Accession->[0]; # GO accession of biological process for the pathway
		next unless $go; # Pathway skipped if no GO accession
	
		my $prot_ar = find_proteins($ev); # Gets a reference to all proteins in the pathway
		my $complex_ar = find_complexes($ev); # Gets a reference to all complexes in the pathway
	
		foreach my $complex (@{$complex_ar}) { # Processes all complexes
			break_complex({
				event => $ev, 
				complex => $complex, 
				letter => "P", 
				go => $go, 
				reactionid => $reactionid
			}); # Processes each complex (one assertion for the complex and one for each protein in it)
		}
	
		#my $rps_ar = find_rps($ev);  # Obtains reference protein sequences for the event (i.e. the pathway)
		foreach my $prot (@{$prot_ar}) {
	
		    my $rps = $prot->ReferenceEntity->[0]; # The reference protein sequence of the protein
	    
	    	# Protein skipped if not referencing UniProt or no species info available
		    next unless ($rps->ReferenceDatabase->[0]->Name->[0] eq 'UniProt');
		    next unless $rps->Species->[0];
	   		next unless $rps->Species->[0]->CrossReference->[0];
	    
	    	my $uni = $rps->Identifier->[0]; # Uniprot Id
	    	my $object_symbol = assign_object_symbol($rps); # Secondary Id
	    	my $taxon = $rps->Species->[0]->CrossReference->[0]->Identifier->[0]; # Species
	    	next if $taxon ~~ @microbial_species_to_exclude;

	  		#my @ref_ar = get_prot_reference($prot);
	    
	    	#foreach my $ref (@ref_ar) {
		    
		 	   	#my $prot_evidence_code = $ref->[0];
		    	#my $prot_reference = $ref->[1];
		
		   		my $row = $DB."\t".$uni."\t".$object_symbol."\t\tGO:".$go."\t".$reactionid."\t"."TAS"."\t\t"."P"."\t\t\t".$object_type."\ttaxon:".$taxon; # Line for insertion into the export
		    
		    	my $rowreg = qr/$row/;
			next if (@rows ~~ $rowreg); # If the assertion already exists, skip it here
		    	next if $seen_in_event{$row}++; # Skip protein if duplicate
	            push @{$assignment{$ev}}, [$uni, $go, $taxon, $reactionid, "P"];
		    	get_date($ev, $row); 
		    	next if $seen{$row}++; # Skip protein if duplicate with same date
		    	push @rows, $row; # Collect entry to print to export file later
	   		#}
		}
    } # End of pathway block
} # End of event loop

#foreach my $elec (@electronic) {
#	foreach my $elecsource (@{$elec_source{$elec->db_id}}) {
		#$exp{$elecsource->db_id} || next;
		
#		my $rps_ar = find_rps($elecsource);
#		next if $rps_ar->[1];
		
#		$exp{$rps_ar->[0]->db_id} || next;
#		my $gee_ar = find_gee($elecsource);
#		next if $gee_ar->[0];
		
#		unless ($rps_ar->[0]) {
#			print "***no rps*** ", $elec->extended_displayName, "\n";
#			next;
#		}
		
#		my $uni = $rps_ar->[0]->Identifier->[0];
#		my $elec_ar = find_rps($elec);
#		foreach my $elec_rps (@{$elec_ar}) {
#			next unless $elec_rps->Species->[0];
#			my $db = $elec_rps->ReferenceDatabase->[0]->Name->[0];
#			my $id = $elec_rps->Identifier->[0];
#			my $object_symbol = assign_object_symbol($elec_rps);
#			my $taxon = $elec_rps->Species->[0]->CrossReference->[0]->Identifier->[0];
#			foreach my $assignment (@{$assignment{$elecsource}}) {
#				next unless ($assignment->[0] eq $uni);
#				my $go = $assignment->[1];
#				my $taxon_source = $assignment->[2];
#				my $reference = "GO_REF:0000019";
#				my $aspect = $assignment->[4];
#				my $row = $db."\t".$id."\t".$object_symbol."\t\tGO:".$go."\t".$reference."\t"."IEA"."\tUniProt:".$uni."\t".$aspect."\t\t\t".$object_type."\ttaxon:".$taxon;
#				get_date($elec, $row);
#				next if $seen{$row}++;
#				push @rows, $row;
#			}
#		}
#	}
#}
#find manually inferred events that can be given ISS status
#foreach my $man (@manual) {
#    foreach my $source (@{$source{$man->db_id}}) {
#	#$exp{$source->db_id} || next; #only annotations with EXP evidence code can be used to infer ISS annotation
#	
#	#only reactions that contain only one protein can be used, otherwise the orthologous relationship between the proteins in the reaction is ambiguous
#	my $rps_ar = find_rps($source);
#	next if $rps_ar->[1];
#	
#	unless ($rps_ar->[0]) { 
#	    print "***no rps*** ", $man->extended_displayName, "\n";
#	    next;
#	}
#	
#	$exp{$rps_ar->[0]->db_id} || next;
#	#check for GEE as well, otherwise again the orthologous relationship may be ambiguous
#	my $gee_ar = find_gee($source);
#	next if $gee_ar->[0];
#	
#	my $uni = $rps_ar->[0]->Identifier->[0]; 
#	my $man_ar = find_rps($man);
#	foreach my $man_rps (@{$man_ar}) {
#	    next unless $man_rps->Species->[0];
#	    my $db = $man_rps->ReferenceDatabase->[0]->Name->[0];
#	    my $id = $man_rps->Identifier->[0];
#	    my $object_symbol = assign_object_symbol($man_rps);
#	    my $taxon = $man_rps->Species->[0]->CrossReference->[0]->Identifier->[0];
#	    foreach my $assignment (@{$assignment{$source}}) {
#		next unless ($assignment->[0] eq $uni);
#		my $go = $assignment->[1];
#		my $taxon_source = $assignment->[2];
##		next if ($taxon eq $taxon_source);  #not sure, need to confirm with GOA
#		my $reference = "GO_REF:0000024";
#		my $aspect = $assignment->[4]; 
#		my $row = $db."\t".$id."\t".$object_symbol."\t\tGO:".$go."\t".$reference."\t"."ISS"."\tUniProt:".$uni."\t".$aspect."\t\t\t".$object_type."\ttaxon:".$taxon;
#		get_date($man, $row);
#                next if $seen{$row}++;
#                push @rows, $row;
#	    }
#	}
#    }
#}

my $count1 = `wc -l $checkfile`; # Number of lines in EXP->NAS replacement file                                                                                                                                                                             
print "$0: count1=$count1\n";

my $count2 = 0; # Holds count of rows with EXP replaced by NAS



# Each output line is processed and written to the output file
ROW:foreach my $row (@rows) {

    # Skip row if known to contain errors
    open(GOERRORS, "go_errors.txt");
    while (<GOERRORS>) {
	chomp;
	next ROW if $row =~ /$_/;
    }
    close(GOERRORS);

    open(CHECK, "<$checkfile"); # Open the evidence code list
    while (<CHECK>) { # Check the output line against each evidence code replacement line
		chomp; 
		my ($uni, $go, $pmid) = split/\s+/, $_; # Obtain the uniprot id, go accession, and pubmed id for the current replacement line
		#print $uni,"\t", $go, "\t", $pmid, "\n";
	
		# Go to the next replacement line if the current output line
		# doesn't contain the current replacement line's uniprot id,
		# go accession, AND pubmed id
		next unless $row =~ /$uni/;
		next unless $row =~ /$go/;
		next unless $row =~ /$pmid/;
	
	
		# If all 3 identifiers are present in the row, the row
		# is checked if it refers to a complex parent and if it
		# does, the reference is stored in $complex. The EXP evidence
		# code is replaced with NAS, the count of output lines
		# changed is increased by 1 and the next output line is
		# checked (i.e. last; breaks out of the replacement file
		# check loop and so the next $row is processed).
		my $complex;
		if ($row =~ s/(part_of\(.*\))//) {
			$complex = $1;
		} else {
			$complex = "";
		}
		my $date = $date{$row};
		$row =~ s/EXP/NAS/; # Replace NAS code with EXP code
		$row = $row.$complex;
		$date{$row} = $date; #otherwise date is lost
	
		$count2++;
		last;
    }
    close(CHECK); # The replacement list is closed
    
    
    
    # The row is checked if it refers to a complex parent and if it
    # does, the reference is stored in $complex. 
    my $complex;
    if ($row =~ s/(part_of\(.*\))//) {
		$complex = $1;
    } else {
		$complex = "";
    }
    
    # The date is stored
    my $date;
    if ($complex) {
		$date = $date{$row.$complex};
    } else {
		$date = $date{$row};
    }
    
    print FILE $row."\t".$date."\t".$assigned_by."\t".$complex."\n";  #the date has to be added after the check, otherwise duplicate entries are created simply due to different modification dates
}

print "Lines in check file: ", $count1;
print $count2, " replacements from EXP to NAS.\n";

close(FILE); # The output file has all entries now and is closed

print "goa_submission.pl has finished its job\n";


# Creates assertions for the complex and all its proteins as well as any subcomplexes (recursively handles subcomplexes)
sub break_complex {
	my $args = shift;
		
	my $ev = $args->{'event'}; # Event
	my $cmplx = $args->{'complex'}; # Complex within the event
	my $letter = $args->{'letter'}; # C for component, F for function, or P for process
	my $subcomplexes = $args->{'subcomplexes'}; # Retains subcomplexes that have already been processed from a previous, recursive call
	if ($subcomplexes) {
		%subcomplexes = %{$subcomplexes};  
	} else {
		%subcomplexes = ();
	}
	
	#print $cmplx->db_id, "\n";
	return if $subcomplexes{$cmplx->StableIdentifier->[0]->Identifier->[0]}++; # If the current complex is a subcomplex that has already been done, it is skipped
	
	if ($letter eq "F") {
		my $go = $args->{'go'}; # Complex excluded if there is no GO accession
		return unless $go;
		my $reactionid = $args->{'reactionid'};
		my $cat = $args->{'catalyst'};
		my $ref = $args->{'reference'};
		my $parent = $args->{'parent'};
	
		my $parent_complexes = $cmplx->follow_class_attributes(-INSTRUCTIONS =>
									{'Complex' => {'reverse_attributes' => [qw(hasComponent)]}},
								       -OUT_CLASSES => [('Complex')]);
		#print $cmplx->db_id, "\n";
		#foreach (@{$parent_complexes}) {
		#	print $_->db_id, "\n";
		#}
		#<STDIN>;
		return if ($parent_complexes->[1]) && !($parent); # Return if there are multiple parent complexes
		# Check whether the Complex contains a GenomeEncodedEntity (w/o accession) - then it needs to be excluded as no confident assignment can be made
		my $gee_ar = find_gee($cat);
		return if $gee_ar->[0];
		
		my $complex_stable_id = $cmplx->StableIdentifier->[0]->Identifier->[0];
		my $complex_name = trim($cmplx->Name->[0]);
				
		my $complex_evidence_code = $ref->[0];
		my $evidence_code = $complex_evidence_code;
		my $complex_reference = $ref->[1];

		return unless $cmplx->Species->[0];
		return unless $cmplx->Species->[0]->CrossReference->[0];
		my $taxon = $cmplx->Species->[0]->CrossReference->[0]->Identifier->[0]; # Species
		return if $taxon ~~ @microbial_species_to_exclude;		
					
		my $row;# = "Reactome" ."\t".$complex_stable_id."\t".$complex_name."\t\tGO:".$go."\t".$reactionid."\t".$complex_evidence_code."\t\t"."F"."\t\t\t"."complex"."\ttaxon:".$taxon; # Holds line
	
		#if ($parent) {
		#	$parent = "REACTOME:".$parent->StableIdentifier->[0]->Identifier->[0];
		#	$row .= "part_of($parent)";
		#}
		#
		#return if $seen_in_event{$row}++; # Entry discarded/skipped if duplicated
		#push @{$assignment{$ev}}, [$uni, $go, $taxon, $prot_reference, "C"]; #this is needed for copying the assignment to manually inferred events further down
		#get_date($cmplx, $row);
		#return if $seen{$row}++; # Entry discarded if duplicated with the same modification date
		
		#push @rows, $row; # Entry stored for printing to exported file
			    		   #  -- Don't print it yet as the same entry may come up with a later date
			
		
		my @proteins; # Will hold the proteins of the catalyst
		
		# Obtains all reference protein sequences for the catalyst activity
		my $rps_ar = $cat->follow_class_attributes(-INSTRUCTIONS =>
							{'CatalystActivity' => {'attributes' => [qw(physicalEntity)]},
							 'Complex' => {'attributes' => [qw(hasComponent)]},
							 'DefinedSet' => {'attributes' => [qw(hasMember)]},
							 'EntityWithAccessionedSequence' => {'attributes' => [qw(referenceEntity)]}},
							-OUT_CLASSES => [($protein_class)]);
		if ($rps_ar->[1]) { #this indicates it's a heteromer (there are at least two reference protein sequences)
			 #Check whether activeUnit is annotated and unique
			my $au = $cat->ActiveUnit;
			if ($au->[0] && !$au->[1] && ($au->[0]->is_a('EntityWithAccessionedSequence') || $au->[0]->is_a('DefinedSet'))) {
				push @proteins, $au->[0]; # Obtains the reference protein sequence of the active unit
			} else {
				return; #heteromeric complexes without unique activeUnit need to be excluded
				      #ASK GO: assign activity to whole complex
			}
		} else {
			@proteins = @{$cmplx->hasComponent};
		}
		
		# Processes each protein for the catalyst activity
		foreach my $prot (@proteins) {
			if ($prot->is_a('Complex')) {
				break_complex({
					'event' => $ev, 
					'complex' => $prot, 
					'letter' => "F", 
					'go' => $go, 
					'reactionid' => $reactionid, 
					'catalyst' => $cat, 
					'reference' => $ref, 
					'parent' => $cmplx, 
					'subcomplexes' => \%subcomplexes
				});
				next;
			}
			if ($prot->is_a('DefinedSet')) {
				my @members = @{$prot->hasMember};
				foreach my $member (@members) {
					next unless $member->is_a('EntityWithAccessionedSequence');
					next if $member->is_a('CandidateSet');
					my $rps = $member->ReferenceEntity->[0];
#					 Exclusion of proteins not meeting the following conditions
					next unless ($rps->ReferenceDatabase->[0]->Name->[0] eq 'UniProt'); # Must be a UniProt protein
					next unless $rps->Species->[0]; # Must reference a species
					next unless $rps->Species->[0]->CrossReference->[0]; # Must have access to the species name
							
					my $uni = $rps->Identifier->[0]; # UniProt ID
					my $object_symbol = assign_object_symbol($rps); # Secondary ID					
					$taxon = $rps->Species->[0]->CrossReference->[0]->Identifier->[0]; # Species
					next if $taxon ~~ @microbial_species_to_exclude;					

					$row = $DB."\t".$uni."\t".$object_symbol."\t\tGO:".$go."\t".$reactionid."\t".$evidence_code."\t\t".$letter."\t\t\t".$object_type."\ttaxon:".$taxon;
					#$row .= "part_of(REACTOME:$complex_stable_id)";
					next if $seen_in_event{$row}++; # Protein skipped if it is a duplicate
					push @{$assignment{$ev}}, [$uni, $go, $taxon, $reactionid, "F"]; 
					get_date($cat, $row);
					next if $seen{$row}++; # Protein skipped if it is a duplicate with the same date
				
					push @rows, $row; # Line stored for writing to file later on		
				}
				next;
			}
			next unless $prot->is_a('EntityWithAccessionedSequence');
			next if $prot->is_a('CandidateSet');
			my $rps = $prot->ReferenceEntity->[0];
#			 Exclusion of proteins not meeting the following conditions
			next unless ($rps->ReferenceDatabase->[0]->Name->[0] eq 'UniProt'); # Must be a UniProt protein
			next unless $rps->Species->[0]; # Must reference a species
			next unless $rps->Species->[0]->CrossReference->[0]; # Must have access to the species name
					
			my $uni = $rps->Identifier->[0]; # UniProt ID
			my $object_symbol = assign_object_symbol($rps); # Secondary ID					
			$taxon = $rps->Species->[0]->CrossReference->[0]->Identifier->[0]; # Species
			next if $taxon ~~ @microbial_species_to_exclude;
			
			$row = $DB."\t".$uni."\t".$object_symbol."\t\tGO:".$go."\t".$reactionid."\t".$evidence_code."\t\t".$letter."\t\t\t".$object_type."\ttaxon:".$taxon;
			#$row .= "part_of(REACTOME:$complex_stable_id)";
			next if $seen_in_event{$row}++; # Protein skipped if it is a duplicate
			push @{$assignment{$ev}}, [$uni, $go, $taxon, $reactionid, "F"]; 
			get_date($cat, $row);
			next if $seen{$row}++; # Protein skipped if it is a duplicate with the same date
			
			push @rows, $row; # Line stored for writing to file later on
		}
	} elsif ($letter eq "C") {
		return unless $cmplx->Species->[0];
		return unless $cmplx->Species->[0]->CrossReference->[0];
		
		my $complex_id = $cmplx->StableIdentifier->[0]->Identifier->[0];
		my $complex_name = trim($cmplx->Name->[0]);

		my $go;

		my $evidence_code = "TAS";
		
		my $reactionid = $args->{'reactionid'};
		my $parent = $args->{'parent'};
		
		my $parent_complexes = $cmplx->follow_class_attributes(-INSTRUCTIONS =>
									{'Complex' => {'reverse_attributes' => [qw(hasComponent)]}},
								       -OUT_CLASSES => [('Complex')]);
		return if ($parent_complexes->[1]) && !($parent);
		
		my $taxon = $cmplx->Species->[0]->CrossReference->[0]->Identifier->[0];
		return if $taxon ~~ @microbial_species_to_exclude;		
		
		if ($taxon ~~ @species_with_alternate_go_compartment) {
			if ($cmplx->GO_CellularComponent->[0]){
				$go = $cmplx->GO_CellularComponent->[0]->Accession->[0];
			} else {
				return;
			}
		} else {
			return unless $cmplx->Compartment->[0];
			$go = $cmplx->Compartment->[0]->Accession->[0];
			return unless $go;
		}

		my $row = "Reactome"."\t".$complex_id."\t".$complex_name."\t\tGO:".$go."\t".$reactionid."\t".$evidence_code."\t\t"."C"."\t\t\t"."Complex"."\ttaxon:".$taxon; 
		if ($parent) {
			$parent = "REACTOME:".$parent->StableIdentifier->[0]->Identifier->[0];
			$row .= "part_of($parent)";	
		}
		return if $seen_in_event{$row}++; # Entry discarded/skipped if duplicated
		push @{$assignment{$ev}}, [$complex_id, $go, $taxon, $reactionid, "C"]; #this is needed for copying the assignment to manually inferred events further down
		get_date($cmplx, $row);
		return if $seen{$row}++; # Entry discarded if duplicated with the same modification date
		
		push @rows, $row; #Entry stored for printing to exported file
				  		  #-- Don't print it yet as the same entry may come up with a later date
		
		
		foreach my $prot (@{$cmplx->hasComponent}) {
			if ($prot->is_a('Complex')) {
				break_complex({
					'event' => $ev,
					'complex' => $prot, 
					'letter' => "C", 
					'reactionid' => $reactionid,
					'parent' => $cmplx,
					'subcomplexes' => \%subcomplexes
				});
				next;
			}
			if ($prot->is_a('DefinedSet')) {
				my @members = @{$prot->hasMember};
				foreach my $member (@members) {
					next unless $member->ReferenceEntity->[0];
					next unless ($member->ReferenceEntity->[0]->ReferenceDatabase->[0]->Name->[0] eq 'UniProt');
					next unless $member->Species->[0];
					next unless $member->Species->[0]->CrossReference->[0];
					
					my $uni = $member->ReferenceEntity->[0]->Identifier->[0]; # Obtains UniProt ID
					my $object_symbol = assign_object_symbol($member->ReferenceEntity->[0]); # Obtains alternate UniProt ID
					my $go;
					
					$taxon = $member->Species->[0]->CrossReference->[0]->Identifier->[0]; # Species ID (by taxon number) obtained
					next if $taxon ~~ @microbial_species_to_exclude; 
					
					if ($taxon ~~ @species_with_alternate_go_compartment) {
						if ($member->GO_CellularComponent->[0]) {
							$go = $member->GO_CellularComponent->[0]->Accession->[0];
						} else {
							next;
						}
					} else {
						next unless $member->Compartment->[0];
						$go = $member->Compartment->[0]->Accession->[0];
						next unless $go;
					}
 
					 #Entry for the protein stored in $row
					$row = $DB."\t".$uni."\t".$object_symbol."\t\tGO:".$go."\t".$reactionid."\t".$evidence_code."\t\t".$letter."\t\t\t".$object_type."\ttaxon:".$taxon; 
					$row .= "part_of(REACTOME:$complex_id)";			
					next if $seen_in_event{$row}++; # Entry discarded/skipped if duplicated
					push @{$assignment{$ev}}, [$uni, $go, $taxon, $reactionid, $letter]; #this is needed for copying the assignment to manually inferred events further down
					get_date($member, $row);
					next if $seen{$row}++; # Entry discarded if duplicated with the same modification date
	
					push @rows, $row; #Entry stored for printing to exported file
							  #-- Don't print it yet as the same entry may come up with a later date
				}
				next;
			}
			next unless $prot->ReferenceEntity->[0];
			next unless ($prot->ReferenceEntity->[0]->ReferenceDatabase->[0]->Name->[0] eq 'UniProt');
			next unless $prot->Species->[0];
			next unless $prot->Species->[0]->CrossReference->[0];
				
			my $uni = $prot->ReferenceEntity->[0]->Identifier->[0]; # Obtains UniProt ID
			my $object_symbol = assign_object_symbol($prot->ReferenceEntity->[0]); # Obtains alternate UniProt ID
			my $go;
			
			$taxon = $prot->Species->[0]->CrossReference->[0]->Identifier->[0]; # Species ID (by taxon number) obtained
			next if $taxon ~~ @microbial_species_to_exclude;			

			if ($taxon ~~ @species_with_alternate_go_compartment) {
				if ($prot->GO_CellularComponent->[0]) {
					$go = $prot->GO_CellularComponent->[0]->Accession->[0];
				} else {
					next;
				}
			} else {
				next unless $prot->Compartment->[0];
				$go = $prot->Compartment->[0]->Accession->[0];
				next unless $go;
			}

			# Entry for the protein stored in $row
			$row = $DB."\t".$uni."\t".$object_symbol."\t\tGO:".$go."\t".$reactionid."\t".$evidence_code."\t\t".$letter."\t\t\t".$object_type."\ttaxon:".$taxon; 
			$row .= "part_of(REACTOME:$complex_id)";			
			next if $seen_in_event{$row}++; # Entry discarded/skipped if duplicated
			push @{$assignment{$ev}}, [$uni, $go, $taxon, $reactionid, $letter]; #this is needed for copying the assignment to manually inferred events further down
			get_date($prot, $row);
			next if $seen{$row}++; # Entry discarded if duplicated with the same modification date

			push @rows, $row; #Entry stored for printing to exported file
					  		  #-- Don't print it yet as the same entry may come up with a later date
		}
	} elsif ($letter eq "P") {
		my $go = $args->{'go'};
		my $reactionid = $args->{'reactionid'};
		my $parent = $args->{'parent'};
		
		my $parent_complexes = $cmplx->follow_class_attributes(-INSTRUCTIONS =>
									{'Complex' => {'reverse_attributes' => [qw(hasComponent)]}},
								       -OUT_CLASSES => [('Complex')]);
		return if ($parent_complexes->[1]) && !($parent);
		
		return unless $go;
		return unless $cmplx->Species->[0]; # Complex skipped if it does not have a species
		return unless $cmplx->Species->[0]->CrossReference->[0]; # Complex skipped if there is no access to the taxon 
		
		my $complex_id = $cmplx->StableIdentifier->[0]->Identifier->[0]; # Obtains the complex stable id
		my $complex_name = trim($cmplx->Name->[0]); # Obtains the complex name
		my $taxon = $cmplx->Species->[0]->CrossReference->[0]->Identifier->[0]; # Obtains the complex taxon id
		return if $taxon ~~ @microbial_species_to_exclude;

		my $evidence_code = "TAS";
		
		my $row = "Reactome"."\t".$complex_id."\t".$complex_name."\t\tGO:".$go."\t".$reactionid."\t".$evidence_code."\t\t"."P"."\t\t\t"."Complex"."\ttaxon:".$taxon; # Assertion for the complex
		#if ($parent) {
			#$parent = "REACTOME:".$parent->StableIdentifier->[0]->Identifier->[0];
			#$row .= "part_of($parent)";	
		#} 
		
		return if $seen_in_event{$row}++; # Entry discarded/skipped if duplicated
		push @{$assignment{$ev}}, [$complex_id, $go, $taxon, $reactionid, "P"]; #this is needed for copying the assignment to manually inferred events further down
		get_date($cmplx, $row); # Obtains the complex most recent modification date
		return if $seen{$row}++; # Entry discarded if duplicated with the same modification date
			
		push @rows, $row; # Entry stored for printing to exported file
				  		  #-- Don't print it yet as the same entry may come up with a later date
				  
		foreach my $prot (@{$cmplx->hasComponent}) { # Processes each protein and subcomplex in the complex
			if ($prot->is_a('Complex')) {
				break_complex({
					'event' => $ev, 
					'complex' => $prot, 
					'letter' => "P", 
					'go' => $go, 
					'reactionid' => $reactionid, 
					'parent' => $cmplx, 
					'subcomplexes' => \%subcomplexes
				});
				next;
			}
			if ($prot->is_a('DefinedSet')) {
				my @members = @{$prot->hasMember};
				foreach my $member (@members) {
					next unless $prot->ReferenceEntity->[0]; 
					next unless ($prot->ReferenceEntity->[0]->ReferenceDatabase->[0]->Name->[0] eq 'UniProt');
					next unless $prot->Species->[0];
					next unless $prot->Species->[0]->CrossReference->[0];
				
					my $uni = $prot->ReferenceEntity->[0]->Identifier->[0]; # Obtains UniProt ID
					my $object_symbol = assign_object_symbol($prot->ReferenceEntity->[0]); # Obtains alternate UniProt ID
					$taxon = $prot->Species->[0]->CrossReference->[0]->Identifier->[0]; # Species ID (by taxon number) obtained
					next if $taxon ~~ @microbial_species_to_exclude;		

					# Entry for the protein stored in $row
					$row = $DB."\t".$uni."\t".$object_symbol."\t\tGO:".$go."\t".$reactionid."\t"."TAS"."\t\t"."P"."\t\t\t".$object_type."\ttaxon:".$taxon; 
					#$row .= "part_of(REACTOME:$complex_id)";
					next if $seen_in_event{$row}++; # Entry discarded/skipped if duplicated
					push @{$assignment{$ev}}, [$uni, $go, $taxon, $reactionid, "P"]; #this is needed for copying the assignment to manually inferred events further down
					get_date($prot, $row);
					next if $seen{$row}++; # Entry discarded if duplicated with the same modification date
			
					push @rows, $row; #Entry stored for printing to exported file
							 # -- Don't print it yet as the same entry may come up with a later date
				}
				next;
			}
			next unless $prot->ReferenceEntity->[0]; 
			next unless ($prot->ReferenceEntity->[0]->ReferenceDatabase->[0]->Name->[0] eq 'UniProt');
			next unless $prot->Species->[0];
			next unless $prot->Species->[0]->CrossReference->[0];
			
			my $uni = $prot->ReferenceEntity->[0]->Identifier->[0]; # Obtains UniProt ID
			my $object_symbol = assign_object_symbol($prot->ReferenceEntity->[0]); # Obtains alternate UniProt ID
			$taxon = $prot->Species->[0]->CrossReference->[0]->Identifier->[0]; # Species ID (by taxon number) obtained
			next if $taxon ~~ @microbial_species_to_exclude;		

			# Entry for the protein stored in $row
			$row = $DB."\t".$uni."\t".$object_symbol."\t\tGO:".$go."\t".$reactionid."\t"."TAS"."\t\t"."P"."\t\t\t".$object_type."\ttaxon:".$taxon; 
			#$row .= "part_of(REACTOME:$complex_id)";
			next if $seen_in_event{$row}++; # Entry discarded/skipped if duplicated
			push @{$assignment{$ev}}, [$uni, $go, $taxon, $reactionid, "P"]; #this is needed for copying the assignment to manually inferred events further down
			get_date($prot, $row);
			next if $seen{$row}++; # Entry discarded if duplicated with the same modification date
			
			push @rows, $row; #Entry stored for printing to exported file
					  #-- Don't print it yet as the same entry may come up with a later date
		}
	}
}

# Removes white space
sub trim {
	my $name = shift;
	$name =~ s/^ +//;
	$name =~ s/ +$//;
	return $name;
}

sub get_prot_reference {

	my $prot = shift;
	my $refcount = scalar(@{$prot->LiteratureReference});
	my @ref_ar;
	
	if ($refcount) {
		for (my $i = 0; $i < $refcount; $i++) {
			my $pubmed = $prot->LiteratureReference->[$i]->PubMedIdentifier->[0];
			if($pubmed) {
				push @ref_ar, ["EXP", "PMID: " . $pubmed];
				$exp{$prot->db_id}++;
			} else {
				unless ($prot->is_a('CatalystActivity')) {
					push @ref_ar, ["TAS", "REACTOME: " . $prot->StableIdentifier->[0]->Identifier->[0]];
				} else {
					push @ref_ar, ["TAS", "REACTOME: Catalyst Activity:" . $prot->db_id];
				}
			}
		}
	} else {
		unless ($prot->is_a('CatalystActivity')) {
			push @ref_ar, ["TAS", "REACTOME: " . $prot->StableIdentifier->[0]->Identifier->[0]];
		} else {
			push @ref_ar, ["TAS", "REACTOME: Catalyst Activity:" . $prot->db_id];
		}
	}
	
	return @ref_ar;
}

sub check_reference {  #if a reaction has only one lit-ref, the code used is EXP plus the pubmed id. If more than one or no litrefs are attached, the code is TAS and the Reactome stable id is given as reference
    my ($ev) = @_;
    if ($ev->LiteratureReference->[1] || !$ev->LiteratureReference->[0]) {
	return "TAS", "REACTOME:".$ev->StableIdentifier->[0]->Identifier->[0];
	return "TAS", get_stable_identifier($ev);
    } else {
	my $pubmed = $ev->LiteratureReference->[0]->PubMedIdentifier->[0];
	$pubmed || return "TAS", "REACTOME:".$ev->StableIdentifier->[0]->Identifier->[0]; #this is probably a book reference that doesn't have a pubmed id - use TAS evidence code for now
	$exp{$ev->db_id}++;
	return "EXP", "PMID:".$pubmed;
    }
}

=head
Obs - only needed for the first time
sub get_stable_identifier {
    my ($ev) = @_;
    my $ev2 = $dbc->fetch_instance_by_db_id($ev->db_id, $ev->class)->[0];
    return $ev2->StableIdentifier->[0]->Identifier->[0].".".$ev2->StableIdentifier->[0]->IdentifierVersion->[0];
}
=cut

sub find_proteins {
    my ($rxn) = @_;
    my $ar = $rxn->follow_class_attributes(-INSTRUCTIONS =>
                                           {'Pathway' => {'attributes' => [qw(hasEvent)]},
					    'ReactionlikeEvent' => {'attributes' => [qw(input output catalystActivity)]},
					    'Reaction' => {'attributes' => [qw(input output catalystActivity)]},
                                            'CatalystActivity' => {'attributes' => [qw(physicalEntity)]},
                                            'Complex' => {'attributes' => [qw(hasComponent)]},
                                            'EntitySet' => {'attributes' => [qw(hasMember)]},
                                            'Polymer' => {'attributes' => [qw(repeatedUnit)]}},
                                           -OUT_CLASSES => [qw(EntityWithAccessionedSequence)]);
    return $ar;
}

sub find_complexes {
    my ($rxn) = @_;
    my $ar = $rxn->follow_class_attributes(-INSTRUCTIONS =>
					   {'Pathway' => {'attributes' => [qw(hasEvent)]},
					    'ReactionlikeEvent' => {'attributes' => [qw(input output catalystActivity)]},
					    'Reaction' => {'attributes' => [qw(input output catalystActivity)]},
					    'CatalystActivity' => {'attributes' => [qw(physicalEntity)]},
					    'EntitySet' => {'attributes' => [qw(hasMember)]},
					    'Polymer' => {'attributes' => [qw(repeatedUnit)]}},
					   -OUT_CLASSES => [qw(Complex)]);
    
    return $ar;				
}

sub find_rps {
    my ($ev) = @_;
#this ignores candidates in CandidateSets - may need to revisit
    my $rps_ar = $ev->follow_class_attributes(-INSTRUCTIONS =>
					      {'Pathway' => {'attributes' => [qw(hasEvent)]},
					       'ReactionlikeEvent' => {'attributes' => [qw(input output catalystActivity)]},
					       'CatalystActivity' => {'attributes' => [qw(physicalEntity)]},
					       'Complex' => {'attributes' => [qw(hasComponent)]},
					       'EntitySet' => {'attributes' => [qw(hasMember)]},
					       'Polymer' => {'attributes' => [qw(repeatedUnit)]},
					       'EntityWithAccessionedSequence' => {'attributes' => [qw(referenceEntity)]}},
					      -OUT_CLASSES => [($protein_class)]);
    return $rps_ar;
}

sub find_gee {
    my ($ev) = @_;
    my $gee_ar = $ev->follow_class_attributes(-INSTRUCTIONS =>
                                              {'Pathway' => {'attributes' => [qw(hasEvent)]},
                                               'ReactionlikeEvent' => {'attributes' => [qw(input output catalystActivity)]},
                                               'CatalystActivity' => {'attributes' => [qw(physicalEntity)]},
                                               'Complex' => {'attributes' => [qw(hasComponent)]},
                                               'EntitySet' => {'attributes' => [qw(hasMember)]},
					       'CandidateSet' => {'attributes' => [qw(hasCandidate)]},
					       'Polymer' => {'attributes' => [qw(repeatedUnit)]}},
                                              -OUT_CLASSES => [qw(GenomeEncodedEntity)]);
#need to restrict to those instances that actually are GEEs, not subclasses of GEE
    my @tmp;
    foreach my $gee (@{$gee_ar}) {
	if ($gee->class eq 'GenomeEncodedEntity') {
	    push @tmp, $gee;
	}
    }
    return \@tmp;
}

sub assign_object_symbol {
    my ($rps) = @_;
    if ($rps->SecondaryIdentifier->[0]) {
	return $rps->SecondaryIdentifier->[0];
    } elsif ($rps->GeneName->[0]) {
	return $rps->GeneName->[0];
    } else {
	return $rps->Identifier->[0];
    }
}

sub get_date {
    my ($i, $row) = @_;
    my $date;
    if ($i->Modified->[0]) {
	$date = $i->Modified->[-1]->DateTime->[0];
    } elsif ($i->Created->[0]) {
	$date = $i->Created->[0]->DateTime->[0];
    } else { #keep this just in case, even though old CatalystActivities without created or modified slots have been sorted out by now
	$date=$opt_date;
    }
    $date =~ s/-//g;
    $date =~ s/\s.+//;
    unless ($date{$row} && ($date{$row}>$date)) { #don't touch if the already assigned date is more recent than the date for this particular instance
	$date{$row} = $date;
    }
}
