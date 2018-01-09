#!/usr/bin/perl -w
use common::sense;

use constant LOG => '/usr/local/gkb/website/logs/extended_log';
use constant GEO => '/usr/local/gkb/pathway_log_analysis/AnalysisService/geo_ip.py';
use constant CON => 'perl /home/smckay/csv-heatmap/convert_count_format.pl';

my $log = LOG;
my $geo = GEO;
my $con = CON;

chomp(my $start = `date -d -1hour +'%d/%b/%Y:%H:%M'`);

$start =~ s!/!\\/!g;

open HITS, "tail -200000 $log | sed -n '/$start/,\$ p' | cut -f1 -d' ' | sort | uniq -c |";
open HTML, "| python $geo | $con ";

while (<HITS>) {
    s/^\s+//;
    s/\s+/,/;
    print HTML;
}
