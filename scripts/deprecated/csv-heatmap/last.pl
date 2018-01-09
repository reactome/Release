#!/usr/bin/perl -w
use common::sense;

use constant LOG => '/usr/local/gkb/website/logs/extended_log';
use constant GEO => '/home/smckay/csv-heatmap/geo_ip.py';
use constant CON => 'perl /home/smckay/csv-heatmap/convert_count_format.pl';
use constant WEB => '/usr/local/gkb/website/html/who';

my $time = shift || 'year';

system "echo $time >/tmp/time";

my $log = LOG;
my $geo = GEO;
my $con = CON;
my $web = WEB;

my $past = $time eq 'now' ? '5 minutes ago' : "1 $time ago";
chomp(my $start = `date --date '$past'  +'%d/%b/%Y:%H:%M'`);
chomp(my $now = `date +'%d/%b/%Y:%H:%M'`);

# server is not that old yet
$start = '17/Aug/2014' if $time eq 'year';
$start =~ s!/!\\/!g;

my $tail = 'cat';
unless ($time eq 'year') {
    $tail = 20000;
    $tail *= 24      if $time eq 'day';
    $tail *= 24 * 7  if $time eq 'week';
    $tail *= 24 * 30 if $time eq 'month';
    $tail = "tail -$tail";
}

open HITS, "$tail $log | sed -n '/$start/,\$ p' | cut -f1 -d' ' | sort | uniq -c |";
my $tmp = "/tmp/$time.html";
open HTML, "| python $geo | $con > $tmp";

my $saw_something;

while (<HITS>) {
    s/^\s+//;
    s/\s+/,/;
    $_ or next;
    $saw_something .= $_;
    print;
}

if ($saw_something) {
    print HTML "Last updated: $now\n$saw_something\n";
    system "mv $tmp $web";
}




