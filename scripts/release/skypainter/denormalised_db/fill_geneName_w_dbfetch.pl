#!/usr/local/bin/perl -w

# Make sure you don't have "competing" libraries...
# for use @CSHL
use lib "/usr/local/gkb/modules";
# for use @HOME
use lib "$ENV{HOME}/bioperl-1.0";
use lib "$ENV{HOME}/GKB/modules";
use lib "$ENV{HOME}/my_perl_stuff";

use GKB::DBAdaptor;
use GKB::Utils;
use LWP::UserAgent;
use Carp;
use Data::Dumper;
use Getopt::Long;
use strict;

our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db,$opt_debug);

(@ARGV) || die "Usage: $0 
-user db_user -host db_host -pass db_pass -port db_port -db db_name\n";

&GetOptions("user:s", "host:s", "pass:s", "port:i", "db:s", "debug");

$opt_db || die "Need database name (-db).\n";    

# Get connection to reactome db
my $dba = GKB::DBAdaptor->new
    (
     -user   => $opt_user,
     -host   => $opt_host,
     -pass   => $opt_pass,
     -port   => $opt_port,
     -dbname => $opt_db,
#     -DEBUG => $opt_debug
     );

my $protein_class = &GKB::Utils::get_reference_protein_class($dba);
my $ar = $dba->fetch_instance_by_remote_attribute
    ($protein_class,[
	 ['geneName','IS NULL',[]],
	 ['referenceDatabase.name','=',['UniProt']]
     ]);

printf STDERR "Got %i $protein_class instances\n", scalar(@{$ar});

my %accs;
map {push @{$accs{uc($_->Identifier->[0])}}, $_} @{$ar};

while (my ($acc,$ar) = each %accs) {
    if (my $geneName = get_gene_name($acc)) {
	foreach my $i (@{$ar}) {
	    $i->GeneName($geneName);
	    print $acc, "\t", join(',',@{$i->GeneName}), "\n";
	    $dba->update_attribute($i,'geneName');
	}
    }
}

print STDERR "$0: no fatal errors.\n";

sub get_gene_name {
    my $acc = shift;
    my $url = "http://www.ebi.ac.uk/cgi-bin/dbfetch?db=UniProtKB&style=raw&id=" . $acc;
    my $ua = LWP::UserAgent->new('agent' => "Mozilla/5.0 (Macintosh; U; PPC Mac OS X Mach-O; en-US; rv:1.8.0.1) Gecko/20060111 Firefox/1.5.0.1");
    my $response = $ua->get($url);
    if($response->is_success) {
        if ($response->content =~ /^GN\s+Name=(.+?);/ms) {
	    return $1;
	} elsif ($response->content =~ /No entries found/) {
	    printf STDERR "No record for %s.\n", $acc;
	} else {
	    printf STDERR "Failed to get gene name for %s.\n", $acc;
	}
    } else {
	printf STDERR "Failed to get record for %s.\n", $acc;
    }
    return;
}
