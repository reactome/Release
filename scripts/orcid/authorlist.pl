#!/usr/local/bin/perl -w

#This script produces a tab delimited file listing authors and reviewers in Reactome

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


my $outfile = "authorList.txt"; # Output file for author/reviewer list

# If creation of a filehandle is unsuccessful, the following error message
# prints and the program terminates.
open(my $list, ">", $outfile) or die "$0: could not open file $outfile\n";
     
my %seen; # Assures author records are not duplicated (key -author id    value -number of times occurred)


my $ar = $dba->fetch_instance(-CLASS => 'Event'); # Obtains a reference to the array of all Reactome events

# Each event in Reactome is processed
foreach my $ev (@{$ar}) {	    
	foreach my $author_instance_edit (@{$ev->authored}) {
		foreach my $person (@{$author_instance_edit->author}) {
			print $list get_person_record($person, "Author") unless $seen{$person->db_id}++;
		}
	}
	
	foreach my $reviewer_instance_edit (@{$ev->reviewed}) {
		foreach my $person (@{$reviewer_instance_edit->author}) {
			print $list get_person_record($person, "Reviewer") unless $seen{$person->db_id}++;
		}
	}
}

print "$0 has finished its job\n";

sub get_person_record {
	my $person = shift;
	my $person_type = shift;
	
	my $id = $person->db_id;
	my $first_name = $person->firstname->[0] || $person->initial->[0];
	my $last_name = $person->surname->[0];
	my $email = $person->eMailAddress->[0] || '';
	my @affiliations;
	
	if ($person->affiliation) {
		foreach my $affiliation (@{$person->affiliation}) {
			push @affiliations, $affiliation->name->[0];
		}
	}
	
	my $record = join "\t", ($id,
				 $first_name,
				 $last_name,
				 $email,
				 (join ";", @affiliations),
				 $person_type);
	
	return $record . "\n";
}


