#!/usr/local/bin/perl -w
use strict;

use autodie;
use XML::Simple;

if (@ARGV != 2) {
	print "Usage: $0 'path_to_author_list' 'desired_path_to_output_file\n'";
	exit;
}

open(my $list, "<", $ARGV[0]);
open(my $output, ">", $ARGV[1]);


while (my $record = <$list>) {
	chomp $record;
	my @fields = split "\t", $record;
	
	my $first_name = $fields[1];
	my $last_name = $fields[2];
	my $email = $fields[3];
	my @affiliations = split ";", $fields[4];
	
	my @potential_orcid_ids = query_orcid($first_name, $last_name, $email, \@affiliations);
	
	print $output $record . "\t" . (join "\t", @potential_orcid_ids) . "\n";
	
	sleep 5;
}	


print "$0 has finished its job\n";

sub query_orcid {
	my $first_name = shift;
	my $last_name = shift;
	my $email = shift;
	my $affiliations_array_ref = shift;
	
	my $orcid_xml = `curl -H "Accept:application/orcid+xml" "http://pub.orcid.org/search/orcid-bio?q=family-name:$last_name+AND+given-names:$first_name"`;
	
	my @potential_orcid_ids = ();
	if ($orcid_xml =~ /orcid-search-result/) {
		while ($orcid_xml =~ /\<orcid-id\>(.*?)\<\/orcid-id\>/gms) {
			push @potential_orcid_ids, $1;	
		}
	}
	
	return @potential_orcid_ids;
}