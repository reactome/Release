#/usr/bin/perl -w
use common::sense;

use constant TEMPLATE => 'csv-heatmap.html';

my $max = 0;
my $csv = qq(var csv = [\n);

chomp(my $now = `date +'%d/%b/%Y:%H:%M'`);
my $stamp = "Updated: $now";

chomp(my $time = `cat /tmp/time`);
say STDERR "MY TIME $time";

while (<>) {
    chomp;
    next if /weight/;
    
    if (/Last updated/) {
	$stamp = $_;
	next;
    }

    my ($count,$lat,$lon) = split ';';
    
    if ($count && $lat && $lon) {
	$max = $count if $count > $max;
	$csv .= qq(  { location: new google.maps.LatLng($lat, $lon), );
	$csv .= qq(weight: $count },\n);
    }
}

$csv =~ s/,\n$/\n/;
$csv .= "];\n";

$csv  = "var max = $max;\n$csv\n";

open TEMP, "cat ".TEMPLATE." | col -b |"  or die $!;

while (<TEMP>) {
    if (/CSV_DATA/) {
	my ($space) = /^(\s+)CSV_DATA/; 
	$csv =~ s/\n/\n$space/g;
	say "$space$csv";
    }
    elsif (/TIME_STAMP/ && $stamp) {
	s/TIME_STAMP/$stamp/;
	print;
    }
    elsif (/<a.+$time<\/a>/) {
	s/<a.+$time<\/a>/$time/;
	print;
    }
    else {
	print;
    }
}



