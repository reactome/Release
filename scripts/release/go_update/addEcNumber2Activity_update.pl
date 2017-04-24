#!/usr/local/bin/perl -w
use strict;

#This is the script to update EC numbers after running the script goxml2mysql_update.pl, which effectively removes all EC mappings for GO_MolecularFunctions.
#The file for EC to GO mapping is downloadable at   http://www.geneontology.org/external2go/ec2go   (or: ftp://ftp.geneontology.org/pub/go/external2go/   )

use lib "/usr/local/gkb/modules";
use GKB::DBAdaptor;
use GKB::Utils_esther;

use Data::Dumper;
use Getopt::Long;
use List::MoreUtils qw/none/;

@ARGV || die "Usage: $0 -user db_user -host db_host -pass db_pass -port db_port -db db_name < ec2go.txt\n";
our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db,$opt_debug);

&GetOptions("user:s","host:s","pass:s","port:i","db=s","debug");
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

my $instance_edit = GKB::Utils_esther::create_instance_edit($dba, 'Weiser', 'JD', 'EC number update');
my (%accession_to_ec_number, %seen_ec_numbers, %seen_GO_accessions);
while(<>) {
    next if /^!/; # Skipping comments in ec2go
    chomp;
    
    my ($ec_number) = $_ =~ /^EC:(\d+(?:\.\d+)*)/;
    $seen_ec_numbers{$ec_number}++;
    print $ec_number, "\n";
    
    my (@go_accessions) = $_ =~ /GO:(\d{7})/g;
    foreach my $go_accession (@go_accessions) {
        $seen_GO_accession{$go_accession}++;
        push @{$accession_to_ec_number{$go_accession}}, $ec_number;
    }
}

foreach my $go_accession (keys %accession_to_ec_number) {
    my $GO_molecular_function_instances = $dba->fetch_instance_by_attribute('GO_MolecularFunction',[['accession', [$go_accession]]);
    if (@{$GO_molecular_function_instances}) {
        foreach my $GO_molecular_function_instance (@{$GO_molecular_function_instances}) {
            my @new_ec_numbers = @{$accession_to_ec_number{$go_accession}};
            my @current_ec_numbers = @{$GO_molecular_function_instance->ecNumber};
            next if same_values(\@new_ec_numbers, \@current_ec_numbers);
            
            $GO_molecular_function_instance->ecNumber(undef);
            $GO_molecular_function_instance->ecNumber(@new_ec_numbers);
            $dba->update_attribute($GO_molecular_function_instance, 'ecNumber');
            GKB::Utils_esther::update_modified_if_necessary($GO_molecular_function_instance, $instance_edit, $dba);
        }
    } else {
        print "GO_MolecularFunction $go_accession was not found!\n";
    }
}

print "\nThese ec numbers have been seen more than once:\n";
print "$_\n" foreach (grep {$seen_ec_number{$_} > 1} keys %seen_ec_numbers);
 
print "\nThese GO accessions have been seen more than once:\n";
print "$_\n" foreach (grep {$seen_GO_accessions{$_} > 1} keys %seen_GO_accessions);

sub same_values {
    my $array1 = shift;
    my $array2 = shift;
    
    return 0 if scalar @{$array1} != scalar @{$array2};
    
    foreach my $array1_value (@{$array1}) {
        return 0 if none {$_ eq $array1_value} @{$array2};
    }
    
    return 1;
}