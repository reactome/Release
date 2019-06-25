#!/usr/bin/perl
use strict;
use warnings;

use lib '/usr/local/gkb/modules';

use File::Slurp;
use Getopt::Long;

my ($input_files);
&GetOptions("input_files:s" => \$input_files);
$input_files || die "Usage: $0 -input_files gene_xml_files (comma separated)\n";

my @input_files = split(',', $input_files);
my $file_contents = join('', map { read_file($_) } @input_files);

my %ncbi_gene_to_identifier;
while (my ($link_node) = $file_contents =~ /(<Link>.*?<\/Link>)/gs) {   
   my ($ncbi_gene) = $link_node =~ /<ObjId>(.*?)<\/ObjId>/;
   my ($identifier) = $link_node =~ /<Rule>(.*?)<\/Rule>/;
   
   push @{$ncbi_gene_to_identifier{$ncbi_gene}}, $identifier;
}

foreach my $ncbi_gene (sort keys %ncbi_gene_to_identifier) {
   print $ncbi_gene . "\t" . join(',', sort @{$ncbi_gene_to_identifier{$ncbi_gene}}) . "\n";
}