#This is the script to update EC numbers after running the script goxml2mysql_update.pl, which effectively removes all EC mappings for GO_MolecularFunctions.
#The file for EC to GO mapping is downloadable at   http://www.geneontology.org/external2go/ec2go   (or: ftp://ftp.geneontology.org/pub/go/external2go/   )


#!/usr/local/bin/perl  -w

use lib "$ENV{HOME}/bioperl-1.0";
use lib "$ENV{HOME}/GKB/modules";
use lib "/usr/local/gkb/modules";
use GKB::DBAdaptor;
use GKB::Utils_esther;
use Data::Dumper;
use Getopt::Long;
use strict;

@ARGV || die "Usage: $0 -user db_user -host db_host -pass db_pass -port db_port -db db_name < ec2go.txt\n";
our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db,$opt_debug);

&GetOptions("user:s",
	    "host:s",
	    "pass:s",
	    "port:i",
	    "db=s",
	    "debug",
	    );
$opt_db || die "Need database name (-db).\n";

my $dba = GKB::DBAdaptor->new
    (
     -dbname => $opt_db,
     -user   => $opt_user || '',
     -host   => $opt_host,
     -pass   => $opt_pass,
     -port   => $opt_port,
     -DEBUG => $opt_debug
     );

my $note = 'EC number update';
my $instance_edit = GKB::Utils_esther::create_instance_edit($dba, 'Yung', 'CK', $note);
my %seen;
while(<>) {
    /^!/ && next;
#    print $_;
    chomp;
    my ($ec) = $_ =~ /^(\S+)/;
    $ec =~ s/EC://;
    $seen{$ec}++;
    print $ec, "\n";
    my (@go) = $_ =~ /GO:(\d{7})/g;
    my $ar = $dba->fetch_instance_by_attribute('GO_MolecularFunction',[['accession', \@go]]);
    if (@{$ar}) {
	foreach my $mf (@{$ar}) {
	    my $go = $mf->Accession->[0];
	    $seen{$go}++;
	    $mf->add_attribute_value('ecNumber', $ec);
	    $dba->update_attribute($mf, 'ecNumber');
	    GKB::Utils_esther::update_modified_if_necessary($mf, $instance_edit, $dba);
	}
    } else {
	print "GO_MolecularFunction @go was not found!\n";
    }
}

print "\nThese ids have been seen more than once:\n";
foreach my $id (keys %seen) {
    next if $seen{$id} == 1;
    print $id, "\n";
}

