#!/usr/local/bin/perl -w

use strict;

use GKB::Instance;
use GKB::DBAdaptor;
use GKB::Utils_esther;
use Data::Dumper;
use Getopt::Long;

## Database connection and command line parsing:
our ($opt_user, $opt_host,  $opt_pass, $opt_port, $opt_db, $opt_debug, $opt_species);
(@ARGV)  || die "Usage: $0 -user db_user -host db_host -pass db_pass -port db_port -db db_name -debug\n";
&GetOptions( "user:s", "host:s", "pass:s", "port:i", "db:s", "debug" );
$opt_db  || die "Need database name (-db).\n";

# Host for gk_central
my $gk_central_host = 'http://reactomecurator.oicr.on.ca';

## Download GO data file in OBO format
my $obo_file = "gene_ontology_ext.obo";

if (-e "$obo_file") { system("mv $obo_file $obo_file.old"); }
my $return = system("wget -a go.err -Nc http://geneontology.org/ontology/obo_format_1_2/$obo_file");
if ($return != 0) { die "ERROR: Download of $obo_file failed."; }

$return = system("wget -a go.err -Nc http://geneontology.org/external2go/ec2go");
if ($return != 0) { die "ERROR: Download of ec2go file failed."; }

my $file_age = int((time - (stat("$obo_file"))[9])/60/60/24);
print "Today's date: ", scalar(localtime), "\n";
print "$obo_file dated ", scalar(localtime((stat("$obo_file"))[9])), " is $file_age day(s) old\n";
#($file_age <= 30) ? print "Proceeding...\n" : die "Terminating since 
#file is more than 30 days old\nCheck the file version and rerun 
#script\n";

## Prepare date and author for instanceEdit:
my $surname = 'Weiser';
my $initial = 'JD';
my $date    = `date \+\%F`;
chomp $date;

my $dba = GKB::DBAdaptor->new(
    -user => $opt_user || '',
    -host => $opt_host,
    -pass   => $opt_pass,
    -port   => $opt_port,
    -dbname => $opt_db,
    -DEBUG  => $opt_debug
);

my $db_inst;
unless (
    $db_inst =
    $dba->fetch_instance_by_attribute( 'ReferenceDatabase',
        [ [ 'name', ['GO'] ] ] )->[0]
  )
{
    $db_inst = GKB::Instance->new(
        -ONTOLOGY => $dba->ontology,
        -CLASS    => 'ReferenceDatabase',
        'name'    => [qw(GO)]
    );
    $db_inst->inflated(1);
}

my $instance_edit =
  GKB::Utils_esther::create_instance_edit( $dba, $surname, $initial, $date );

## Then we extract all the GO terms from database and put them in two hashes,
## total_acc contains accessions, total_name contains names.
## There are also hashes to report obsolete, replaced, consider_to_replace entries

my (%total_acc);
my (%categories) = (
    "biological_process", "GO_BiologicalProcess",
    "molecular_function", "GO_MolecularFunction",
    "cellular_component", "GO_CellularComponent"
);
my (%obsolete)   = ();
my (%alternate, %altacc_to_primaryacc)  = ();
my (%replaced)   = ();
my (%consider)   = ();
my (%checked)    = ();
my (%pending_obsoletion) = ();
my (%total_name) = ();
my (%new_go)     = ();
my (%new_isa)    = ();
my (%new_partof, %new_haspart, %new_regulates, %new_positively_regulates, %new_negatively_regulates) = ();

foreach my $terms ( sort keys %categories ) {
    my $cc = 0;

    my $ar = $dba->fetch_all_class_instances_as_shells( $categories{$terms} );
    foreach my $go_inst ( @{$ar} ) {
        my $acc   = $go_inst->accession->[0];
        my $name  = $go_inst->Name->[0];
        my $db_id = $go_inst->db_id;
        if ( defined $acc ) {
            $total_acc{$acc}  = $db_id;
            $total_name{$acc} = $name;
            $cc++;
        }
    }
    print "$categories{$terms}\t$cc\n";
}


## Then we process the OBO file
## and extract all the information we need:
## accession, names, definition, synonyms.
## For each entry in the file we also check if it already exists in the database using the hashes created on the previous step


open( GO, "$obo_file" ) or die;

local $/ = "\n\[Term\]\n";
while (<GO>) {
    my ($acc) = /^id\:\s+GO\:(\d+)/ms;

    next if not defined $acc;
    my ($name) = $_ =~ /^name\:\s+(.*)$/m;				# term name
    my ($def)  = $_ =~ /^def\:\s+\"(.*)\"/m;			# definition of the term
    my ($cat)  = $_ =~ /^namespace\:\s+([a-z\_]*)/m;	# namespace(GO categories): biological_process, cellular_component, molecular_function
        
    my @synonyms;										# synonym of the term
    while ( $_ =~ /^synonym\:\s+\"(.*)\"/gms ) {
        push @synonyms, $1;
    }

    while ( $_ =~ /^alt_id\:\s+GO\:(\d+)/gms ) {		# alternate id for the term
        my $alt_id = $1;
        if ( defined $total_acc{$alt_id} ) {			
            $alternate{$alt_id} = $total_acc{$alt_id};
	    $altacc_to_primaryacc{$alt_id} = $total_acc{$acc}; 
        }
    }
            
## check if the entry is obsolete and prepare report, to remove it or consider a replacement:
    if ( /(is_obsolete: true)/m && defined $total_acc{$acc} ) {

        #	print  $acc, "\t", $1, "\n";
        $obsolete{$acc} = $total_acc{$acc};
        while (/^replaced\_by\:\s+GO\:(\d+)/gm) {
            push( @{ $replaced{$acc} }, $1 );

            #   print "replaced: $acc\t$1\n";
        }
        while (/^consider\:\s+GO\:(\d+)/gm) {
            push( @{ $consider{$acc} }, $1 );
        }

    }

## extract is_a, part_of, has_part, regulates, postively_regulates, negatively_regulates components of the given entry and put them in arrays:
    while (/^is_a\:\s+GO\:(\d+)/gms) {
        # if ( not defined $total_acc{$1} ) {
        push @{ $new_isa{$acc} }, $1;
    }

    while (/^relationship\:\s+part_of GO\:(\d+)/gms) {
        #if ( not defined $total_acc{$1} ) {
        push @{ $new_partof{$acc} }, $1;
    }

    while (/^relationship\:\s+has_part GO\:(\d+)/gms) {
        #if ( not defined $total_acc{$1} ) {
        push @{ $new_haspart{$acc} }, $1;
    }

    while (/^relationship\:\s+regulates GO\:(\d+)/gms) {
        #if ( not defined $total_acc{$1} ) {
        push @{ $new_regulates{$acc} }, $1;
    }

    while (/^relationship\:\s+positively_regulates GO\:(\d+)/gms) {
        #if ( not defined $total_acc{$1} ) {
        push @{ $new_positively_regulates{$acc} }, $1;
    }

    while (/^relationship\:\s+negatively_regulates GO\:(\d+)/gms) {
        #if ( not defined $total_acc{$1} ) {
        push @{ $new_negatively_regulates{$acc} }, $1;
    }
    

## when all checks are completed we update the entry:
    if ( defined $total_acc{$acc} ) {  	
        my $ar = $dba->fetch_instance_by_db_id( $total_acc{$acc} );
        foreach my $ginst ( @{$ar} ) {
            my $ginst_name = $ginst->attribute_value('name')->[0];
	    my $ginst_def = $ginst->attribute_value('definition')->[0];
	    
	    if ($ginst_name ne $name || $ginst_def ne $def) {    
		$ginst->inflate();
		$ginst->Name($name);
		$ginst->Definition($def);
		$ginst->InstanceOf(undef);
		$ginst->ComponentOf(undef);
		$ginst->Created( @{ $ginst->Created } );
		$ginst->Modified( @{ $ginst->Modified } );
		$ginst->add_attribute_value( 'modified', $instance_edit );
		
		$dba->update($ginst);
	    }
        }
    }
    else {
    	my @acc = ($acc);    	
        if ( !/(is_obsolete: true)/m ) {
            my $sdi = GKB::Instance->new(
                -CLASS              => $categories{$cat},
                -ONTOLOGY           => $dba->ontology,
                'referenceDatabase' => $db_inst,
                'accession'        => $acc
            );
            $sdi->inflated(1);
            $sdi->accession(@acc);			# CY addition            
            $sdi->created($instance_edit);
            $sdi->modified(undef);
            $sdi->Name($name);
            $sdi->Definition($def);
            $sdi->InstanceOf(undef);
            $sdi->ComponentOf(undef);
            my $ddd = $dba->store($sdi);
            $total_acc{$acc} = $ddd;
            print "New $categories{$cat} instance\: $ddd\n";
        }
    }
    $checked{$acc} = 1;
    
    # check if updated entry is pending obsoletion by GO    
    if (/(pending|scheduled for|slated for) obsoletion/mi) {
	$pending_obsoletion{$acc} = $total_acc{$acc};
    }
}

## Check for new instances in is_a and part_of

print "Main update is finished, now checking is_a and part_of....\n";

my %relationships = ('instanceOf' => \%new_isa,
		     'componentOf' => \%new_partof,
		     'hasPart' => \%new_haspart,
		     'regulate' => \%new_regulates,
		     'positivelyRegulate' => \%new_positively_regulates,
		     'negativelyRegulate' => \%new_negatively_regulates
		     );

foreach my $attribute (keys %relationships) {
    my %new_relationship = %{$relationships{$attribute}};
    foreach my $ac_upd ( sort keys %new_relationship ) {
        next if ( not defined $total_acc{$ac_upd} );
        my $ar = $dba->fetch_instance_by_db_id( $total_acc{$ac_upd} );
        my @new_instances;
        foreach my $sdi ( @{$ar} ) {
            foreach my $new_instance_id ( @{ $new_relationship{$ac_upd} } ) {
                my $new_a = $dba->fetch_instance_by_db_id( $total_acc{$new_instance_id} );
                foreach my $sdn ( @{$new_a} ) {
                    push @new_instances, $sdn;
                }
            }
            
	    my $current_instances = $sdi->attribute_value($attribute);
	    if (different_go_instances($current_instances, \@new_instances)) {
	        $sdi->inflate();
	        $sdi->$attribute(@new_instances);
	        $dba->update($sdi);
	    }
	}
    }
}

## Report preparation in the wiki format:

open( FR, ">go.wiki" ) || die "Can't open report file\n";
print FR "\<h4\> Report date\: $date\n
		<br>
		Database name\: $opt_db\n
		</h4>\n\n\n----\n";

print FR "\{\| class \=\"wikitable\"
\|\+ Incorrect GO IDs in Reactome
\|\-
\! GO ID
\! Reactome Instance
\|\-\n";

foreach my $check_acc ( sort keys %total_acc ) {
    next if ( defined $checked{$check_acc} );
    next if ( defined $obsolete{$check_acc} );
    next if ( defined $alternate{$check_acc} );    
    
    print FR "\|$check_acc\|";
    print FR "\|\[$gk_central_host/cgi-bin/instancebrowser\?DB\=$opt_db\&ID\=$total_acc{$check_acc}\& $total_acc{$check_acc}\]\n";
    print FR "\|\-\n";
}
print FR "\|\}\n";

## Check and delete obsolete instances if they do not have referers to Reactome instances except GO ones

print FR "\n-----\n";
print FR "\{\| class \=\"wikitable\"
\|\+ Deleted Obsolete GO instances in Reactome
\|\-
\! GO ID
\|\-\n";

foreach my $obs_id ( sort keys %obsolete ) {

    my $obs_ac = $dba->fetch_instance_by_db_id( $obsolete{$obs_id} );
    foreach my $sdi ( @{$obs_ac} ) {
 		$sdi->inflate();
        my $db_id = $sdi->db_id;
        my $sz    = 0;
        my $ref   = $dba->fetch_referer_by_instance($sdi);

		foreach my $go_ref ( @{$ref} ) {
        	$go_ref->inflate();
            my $dbr = $go_ref->referenceDatabase->[0]; 
            if (!$dbr || $dbr->db_id != 1 ) {
                $sz = 1;
            }
        }
        if ( $sz == 0 ) {
	    	print FR "";
	    	print FR "\|GO:$obs_id\n";
	    	print FR "\|\-\n";
	    
            print "Deleting $db_id...\n";
            $dba->delete_by_db_id($db_id);
            delete( $obsolete{$obs_id} );
            delete( $checked{$obs_id} );
        }
    }
}

print FR "\|\}\n";

print FR "\n-----\n";
print FR "\{\| class \=\"wikitable\"
\|\+ Obsolete GO terms
\|\-
\! Reactome Instance
\! Obsolete Term
\! Suggested action
\! New GO terms
\|\-\n";

## Checked above for obsolete instances without referers. If an instance contains only GO referers (referenceDatabase id=1) we remove it without reporting.
## Report the rest of the obsolete instances here if they still exist.  GO instances that have become secondary/alternate to other instances are also reported here.  

%obsolete = (%obsolete, %alternate, %pending_obsoletion);
foreach my $obs_id ( sort keys %obsolete) {
    
    print FR "\|\[$gk_central_host/cgi-bin/instancebrowser\?DB\=$opt_db\&ID\=$obsolete{$obs_id}\& $total_name{$obs_id}\]\n";
    print FR "\|\[http://www.ebi.ac.uk/ego/GTerm\?id\=GO:$obs_id GO:$obs_id\]\n";
            
    my $action;
    my @terms = ();
    if ( $replaced{$obs_id}[0] ) {
        $action = 'replace by:';
        @terms = @{ $replaced{$obs_id} };
    } elsif ( $consider{$obs_id}[0]) {
    	$action = 'consider:';
    	@terms = @{ $consider{$obs_id}};
    } elsif ( $alternate{$obs_id}) {
    	$action = 'alternate id -- no action required';
	
	# Move all referrers to the primary GO term
	my $alternate_acc = $obs_id;
	my $alt_ac = $dba->fetch_instance_by_db_id( $alternate{$alternate_acc} );
	my $primary = $dba->fetch_instance_by_db_id($altacc_to_primaryacc{$alternate_acc})->[0];
	
	foreach my $sdi ( @{$alt_ac} ) {
	    $sdi->inflate();
	    my $alternate_accession = $sdi->accession->[0];
	    my $ref   = $dba->fetch_referer_by_instance($sdi);
	    foreach my $go_ref ( @{$ref} ) {
	        $go_ref->inflate();
		
		# Skip referrers to other GO instances
	        my $dbr = $go_ref->referenceDatabase->[0]; 
	        next unless (!$dbr || $dbr->db_id != 1 );
	        
		
		foreach my $attribute ($go_ref->list_attributes()) {
		    my $attribute_value = $go_ref->$attribute->[0];
		    next unless $attribute_value;
		    
		    my $accession;
		    
		    eval {
			$accession = $attribute_value->accession->[0];
		    };
		    
		    next unless $accession;
		    
		    # If the attribute's value is a GO instance with an
		    # accession matching the alternate term, give the
		    # primary GO instance as the attribute value instead.
		    # This, in effect, moves the referrers from the alternate
		    # GO term to the primary GO term
		    if ($accession == $alternate_accession) {
			$go_ref->$attribute($primary);
			$dba->update($go_ref);
		    }
		}
	    }	
        }
    } elsif ( $pending_obsoletion{$obs_id}) {
    	$action = 'pending obsoletion -- no action required';	
    } else {
    	$action = 'search for replacement -- none offered';
    }
    
    print FR "\|$action\n\|";
    foreach (@terms) {
		print FR "\[http://www.ebi.ac.uk/ego/GTerm\?id\=GO\:$_ GO\:$_\]\t";    	
    }
    print FR "\n\|\-\n";
}

print FR "\|\}\n";

print "go_obo_update.pl has finished its job";

sub different_go_instances {
    my $first_list_ref = shift;
    my $second_list_ref = shift;
    
    return 1 if (scalar @{$first_list_ref} != scalar @{$second_list_ref});
    
    INSTANCE:foreach my $list1_instance (@{$first_list_ref}) {
	foreach my $list2_instance (@{$second_list_ref}) {
	    next INSTANCE if $list1_instance->accession->[0] == $list2_instance->accession->[0];
	}
	return 1;
    }
    
    return 0;
}