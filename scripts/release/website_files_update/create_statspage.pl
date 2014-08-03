#!/usr/local/bin/perl  -w

# This script prints a table meant for stats page and the stats page with that table
# check the path for the stats html file. Default is '.' .

# I think it's more user-friendly if the user will not have to practise any
# symlink tricks. Hence the BEGIN block. If the location of the script or
# libraries changes, this will have to be changed.

BEGIN {
    my @a = split('/',$0);
    pop @a;
    push @a, ('..','..','..','modules');
    my $libpath = join('/', @a);
    unshift (@INC, $libpath);
}

#use GKB::ClipsAdaptor;
use GKB::DBAdaptor;
use GKB::Utils;
use GKB::Config_Species;
#use Data::Dumper;
use Getopt::Long;
use strict;

our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db,$opt_debug);

# Parse commandline
my $usage = "Usage: $0 -user db_user -host db_host -pass db_pass -port db_port -db db_name'\n";
&GetOptions("user:s", "host:s", "pass:s", "port:i", "db=s", "debug");
$opt_db || die $usage;

my $version;
if ($opt_db =~ /_(\d+)$/) {
    $version = $1;
} else {
    until ($version && $version =~ /^\d+$/) {
	print "Enter release version number: ";
	$version = <>;
	chomp $version;
    }
}

my $dba = GKB::DBAdaptor->new
    (
     -dbname => $opt_db,
     -user   => $opt_user,
     -host   => $opt_host,
     -pass   => $opt_pass,
     -port   => $opt_port,
     -DEBUG => $opt_debug
     );
if (!(defined $dba)) {
	print STDERR "$0: could not connect to database $opt_db, aborting\n";
	exit(1);
}

my $sdis = $dba->fetch_instance(-CLASS => 'Species');

# Locally known list of species.  This may contain species that are not in Config_Species.
# This list was inherited from Gopal.
my @local_species_order = (
	"Entamoeba histolytica",
	"Dictyostelium discoideum",
	"Plasmodium falciparum",
	"Cyanidioschyzon merolae",
	"Schizosaccharomyces pombe",
	"Saccharomyces cerevisiae",
	"Neurospora crassa",
	"Cryptococcus neoformans A/D",
	"Caenorhabditis elegans",
	"Mus musculus",
	"Rattus norvegicus",
    	"Homo sapiens",
	"Gallus gallus",
	"Tetraodon nigroviridis",
	"Drosophila melanogaster",
	"Thalassiosira pseudonana",
	"Arabidopsis thaliana",
	"Oryza sativa",
	"Synechococcus sp.",
	"Mycobacterium tuberculosis",
	"Sulfolobus solfataricus",
	"Methanocaldococcus jannaschii",
);

# To maintain consistency with previous statistics, use Gopal's ordering of species
# as the gold standard.  If any new species are introduced to the Species_Config
# file, fold these into Gopal's list in the order in which they occur in
# the Species_Config list.
my @config_species_order = ();
foreach my $species_4_letter_code (@species) {
	my $species_name = $species_info{$species_4_letter_code}{"name"}->[0];
	push(@config_species_order, $species_name);
}
my $list_of_sublists = [];
sublist_list(\@local_species_order, \@config_species_order, $list_of_sublists, undef);
my @species_order = ();
foreach my $sublist (@{$list_of_sublists}) {
	if (!(defined $sublist->[0])) {
		foreach my $species_name (@{$sublist->[1]}) {
			push(@species_order, $species_name);
		}
		last;
	}
}
foreach my $local_species_name (@local_species_order) {
	push(@species_order, $local_species_name);
	foreach my $sublist (@{$list_of_sublists}) {
		if (defined $sublist->[0] && $sublist->[0] eq $local_species_name) {
			foreach my $species_name (@{$sublist->[1]}) {
				push(@species_order, $species_name);
			}
			last;
		}
	}
}

# Get the instances corresponding to the species names, in the same order.
my @si = ();
foreach my $species_name (@species_order) {
	print "Considering Config_Species species_name=$species_name\n";
	foreach my $species_instance (@{$sdis}) {
		if ($species_instance->name->[0] eq $species_name) {
			print "Adding $species_name\n";
			push(@si, $species_instance);
			last;
		}
	}
}

my $s;
my $protein;
my $name;
my $id;
my @rows;
my $p =0;
my $p_all =0;
my $c = 0;
my $r =0;
my $path =0;
my $all_human = 0;

#create 2 letter acronym for each species
foreach my $x (@si) {
	$name = $x->name->[0];
	if ($name =~ /^(\w)\w+\s(\w+)/){
		$name = $1."."." ".$2;
	}
	
	$id = $x->db_id;
	
	#count instances species-wise
	my $protein_class = &GKB::Utils::get_reference_protein_class($dba);
	$protein = $dba->fetch_instance(-CLASS => $protein_class,
	                                  -QUERY => [{-ATTRIBUTE => 'species',
	                                             -VALUE => [$id]
					              }]
						      );
	
	# Determine the "unique protein identifier count" for all species, for human also determine a count including isoforms. For the latter, all ReferenceGeneProducts (both parents and isoforms) that have been used directly in annotation are counted. (This means that RGPs that only act as a parent for an isoform used in annotation are not counted.)    
	my %seen;
	foreach my $p (@{$protein}) {
		#check for EWAS referers
	    my $ewas = $p->follow_class_attributes(-INSTRUCTIONS =>
	                                           {$protein_class => {'reverse_attributes' => [qw(referenceEntity)]}},
	                                           -OUT_CLASSES => ['EntityWithAccessionedSequence']);
	    next unless $ewas->[0];
	    $seen{$p->Identifier->[0]}++; #for counting unique protein identifiers
	    
		#for human also count total with annotation                                                                                        
	    if ($name eq 'H. sapiens') {    
			$all_human++;                 
	    }                      
	}
	my $unique = scalar (keys %seen);
	$p += $unique;
	if ($name eq 'H. sapiens') {
	#    $unique .= "*";
	    $name = "*".$name;
	}
	
	
=head
	#The following block is not used right now - only kept in case this "more exact" method for counting is needed in the future
	#Note: Counting all instances of the ReferenceGeneProduct class leads to an inflated score as the parent of a ReferenceIsoform is counted along with the ReferenceIsoform instance(s). Therefore, for each UniProt accession that has any annotation on the ReferenceIsoform level, the count should be reduced by one.
	my $protein_count = 0;
	my $correction = 0;
	if ($protein_class eq 'ReferenceGeneProduct') {
	    my %seen;
	    foreach my $p (@{$protein}) {
		next unless $p->class eq 'ReferenceIsoform';
		$seen{$p->Identifier->[0]}++; #counting UniProt accessions, not variantIdentifiers (correction only once per UniProt accession)
	    }
	    $correction += keys %seen; 
	}	
	$protein_count = @{$protein} - $correction;	      
	$p += $protein_count;		
=cut
	
	
	my $complex = $dba->fetch_instance(-CLASS => 'Complex',
	                                  -QUERY => [{-ATTRIBUTE => 'species',
	                                             -VALUE => [$id]
					              }]
						      );
	$c += @{$complex};
	
	
	my $reaction = $dba->fetch_instance(-CLASS => 'ReactionlikeEvent',
	                                  -QUERY => [{-ATTRIBUTE => 'species',
	                                             -VALUE => [$id]
	
					              }]
						      );
	$r += @{$reaction};
						      
	my $pathway = $dba->fetch_instance(-CLASS => 'Pathway',
	                               -QUERY => [{-ATTRIBUTE => 'species',
	                                            -VALUE => [$id]
	                                          
	                                   
					              }]
						      );
	$path += @{$pathway};
	
	# Don't bother with species that have no proteins, pathways, etc.
	if ($unique == 0 && @{$complex} == 0 && @{$reaction} == 0 && @{$pathway} == 0) {
		print "All counts are 0 for $name, skipping\n";
		next;
	}
	
	push ( @rows,$name."\t".$unique."\t".@{$complex}."\t".@{$reaction}."\t".@{$pathway}."\n");
}

print $all_human, "\n";
 
print "Species\tProteins\tComplexes\tReactions\tPathways\n";
print (@rows);
print "\nTotal\t$p\t$c\t$r\t$path\n";

# Write into  output file called 'release_stats'.
my $output = 'release_stats';
if (!open (OUTPUT, ">$output")) {
	print STDERR "$0: cannot open file $output for writing\n";
	exit(1);
}

print OUTPUT "SPECIES\tPROTEINS\tCOMPLEXES\tREACTIONS\tPATHWAYS\n";
print OUTPUT (@rows); 

close OUTPUT;

#write the stats page html. 
my $out1 = 'stats.html';
if (!open (OUTPUT1,">$out1")) {
	print STDERR "$0: cannot open file $out1 for writing\n";
	exit(1);
}

print OUTPUT1 <<HTML;
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<title>Stats</title>
<link rel="stylesheet" type="text/css" href="/stylesheet.css" />
</head>
<body>
<!--#include virtual="/cgi-bin/navigation_bar" -->
<table    WIDTH="100%" CLASS="contents">
<tr>
<td CLASS="summation">
<h1 CLASS="frontpage">Reactome Statistics (Version $version)</h1>
<!-- Start of the stats table  -->
<table border="0" width="90%" CELLPADDING="0" CELLSPACING="0" CLASS="classbrowser">
<td valign="top" align="center">
<table class="class attributes">
<tr height="40">
<th><b>Species</b>
</td>
<th><b>PROTEINS</b></th>
<th><b>COMPLEXES</b></th>
<th><b>REACTIONS</b></th>
<th><b>PATHWAYS</b></th>

HTML
 
my $own = "\"own\"";
my $center = "\"center\"";
	
print $output."\n";
	
foreach my $row (@rows) {
	my ($zero, $one, $two, $three, $four) = split ("\t", $row);
	
	chomp $zero;
	chomp $one;
	chomp $two;
	chomp $three;
	chomp $four;
			  
	print  OUTPUT1 "<tr height=18 class =$own>\n<td heigth=25 class=$own>$zero</td>\n\t<td class=$own align=$center>$one</td>\n\t<td class=$own align=$center>$two</td>\n\t<td class=$own align=$center>$three</td>\n\t<td class=$own align=$center>$four</td>\n\t</tr>\n\n";
}

print OUTPUT1 '</table><!-- Close the stat table --><td valign="top" align="justify"><IMG height="400" title="" src="stats.png"><BR><BR><BR></tr>   </td></TABLE> <!-- Close the page table --></table><br><DIV STYLE="font-size:9pt;text-align:left;color:black;padding-top:10px;width=40%">*Reactome annotates to protein isoforms when this information is available.<br>The total number of curated human proteins including isoforms is '. $all_human .'.<p></DIV><!--#include virtual="/cgi-bin/footer" --></BODY></HTML>'."\n";

close(OUTPUT1);

print STDERR "$0 has successfully completed\n";

sub sublist_list {
    my ($local_species_order, $config_species_order, $list_of_sublists, $previous_config_species_name) = @_;
    my @config_species_order_array = @{$config_species_order};
    print "sublist_list: scalar config_species_order_array=" . scalar(@config_species_order_array) . "\n";
    
	my $config_species_name = shift(@config_species_order_array);
	if (defined $config_species_name) {
		if (!is_in_list($config_species_name, $local_species_order)) {
			my $previous_sublist = pop(@{$list_of_sublists});
			my $sublist;
			if (!(defined $previous_sublist)) {
				$sublist = [$previous_config_species_name, [$config_species_name]];
			} else {
				if ((!(defined $previous_sublist->[0]) && !(defined $previous_config_species_name)) || $previous_sublist->[0] eq $previous_config_species_name) {
					my $config_species_name_list = $previous_sublist->[1];
					push(@{$config_species_name_list}, $config_species_name);
					$sublist = [$previous_config_species_name, $config_species_name_list];
				} else {
					push(@{$list_of_sublists}, $previous_sublist);
					$sublist = [$previous_config_species_name, [$config_species_name]];
				}
			}
			push(@{$list_of_sublists}, $sublist);
		} else {
			$previous_config_species_name = $config_species_name;
		}
		sublist_list($local_species_order, \@config_species_order_array, $list_of_sublists, $previous_config_species_name);
	}
}

sub is_in_list {
    my ($value, $list) = @_;
    
    my $element;
    foreach $element (@{$list}) {
    	if ($element eq $value) {
    		return 1;
    	}
    }
	return 0;
}
