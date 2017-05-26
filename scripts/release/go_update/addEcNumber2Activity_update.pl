#!/usr/local/bin/perl -w
use strict;

#This is the script to update EC numbers after running the script go_obo_update.pl.
#The file for EC to GO mapping is downloadable at   http://www.geneontology.org/external2go/ec2go   (or: ftp://ftp.geneontology.org/pub/go/external2go/   )

use lib "/usr/local/gkb/modules";
use GKB::DBAdaptor;
use GKB::Utils_esther;

use autodie;
use Data::Dumper;
use Getopt::Long;
use List::MoreUtils qw/none uniq/;

@ARGV || die "Usage: $0 -user db_user -host db_host -pass db_pass -port db_port -db db_name\n";
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

open(my $ec2go_fh, '<', 'ec2go');
my $instance_edit = GKB::Utils_esther::create_instance_edit($dba, 'Weiser', 'JD', 'EC number update');
print 'Using instance edit with db_id ' . $instance_edit->db_id . "\n";

my (%accession_to_EC_numbers, %seen_EC_numbers, %seen_GO_accessions);
while(<$ec2go_fh>) {
    next if /^!/; # Skipping comments in ec2go
    chomp;
    
    my ($EC_number) = $_ =~ get_EC_number_format();
    unless ($EC_number) {
        print STDERR "No EC number could be obtained from this line: $_\n";
        next;
    }
    $seen_EC_numbers{$EC_number}++;
    
    my (@GO_accessions) = $_ =~ /GO:(\d{7})/g;
    unless (@GO_accessions) {
        print STDERR "No GO accessions could be obtained from this line: $_\n";
        next;
    }
    foreach my $GO_accession (@GO_accessions) {
        $seen_GO_accessions{$GO_accession}++;
        push @{$accession_to_EC_numbers{$GO_accession}}, $EC_number;
    }
}
close $ec2go_fh;

foreach my $GO_molecular_function_instance (@{$dba->fetch_instance(-CLASS => 'GO_MolecularFunction')}) {
    my @new_EC_numbers = get_new_EC_numbers($GO_molecular_function_instance, \%accession_to_EC_numbers);
    my @current_EC_numbers = @{$GO_molecular_function_instance->ecNumber};
    next if same_values(\@new_EC_numbers, \@current_EC_numbers);
            
    $GO_molecular_function_instance->ecNumber(undef);
    $GO_molecular_function_instance->ecNumber(@new_EC_numbers);
    $dba->update_attribute($GO_molecular_function_instance, 'ecNumber');
    GKB::Utils_esther::update_modified_if_necessary($GO_molecular_function_instance, $instance_edit, $dba);
    
    print 'GO:' . $GO_molecular_function_instance->accession->[0] . ' (' . $GO_molecular_function_instance->db_id . ') ' .
        "EC number(s) updated: @new_EC_numbers\n";
}

print "\nThese ec numbers have been seen more than once:\n";
print "$_\n" foreach (grep {$seen_EC_numbers{$_} > 1} keys %seen_EC_numbers);
 
print "\nThese GO accessions have been seen more than once:\n";
print "$_\n" foreach (grep {$seen_GO_accessions{$_} > 1} keys %seen_GO_accessions);

sub get_new_EC_numbers {
    my $GO_molecular_function_instance = shift;
    my $accession_to_EC_numbers = shift;
    
    my $GO_accession = $GO_molecular_function_instance->accession->[0];
    my @new_EC_numbers;
    
    if ($accession_to_EC_numbers->{$GO_accession}) {        
        @new_EC_numbers = @{$accession_to_EC_numbers->{$GO_accession}};
    } else {
        @new_EC_numbers = uniq grep { has_EC_number_format($_) } @{$GO_molecular_function_instance->ecNumber};
    }
        
    return @new_EC_numbers;
}

sub has_EC_number_format {
    my $candidate_EC_number = shift;
    
    return ($candidate_EC_number =~ get_EC_number_format());
}

sub get_EC_number_format {
    return qr/^(?:EC:)?(\d+(?:\.\d+)*)/;
}

sub same_values {
    my $array1 = shift;
    my $array2 = shift;
    
    return 0 if scalar @{$array1} != scalar @{$array2};
    
    foreach my $array1_value (@{$array1}) {
        return 0 if none {$_ eq $array1_value} @{$array2};
    }
    
    return 1;
}