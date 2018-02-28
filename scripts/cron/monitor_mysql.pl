#!/usr/bin/perl -w
use common::sense;
use JSON;
use DateTime::Format::Strptime;
use Data::Dumper;

use constant MYSQL  => '/usr/local/gkb/website/logs/mysql_log.txt';
use constant APACHE => '/usr/local/gkb/website/logs/transfer_log';
use constant MYSQL_JSON  => '/usr/local/gkb/website/logs/mysql.json';
use constant APACHE_JSON => '/usr/local/gkb/website/logs/apache.json';

my $strp1 = DateTime::Format::Strptime->new(
    pattern => "%a %b %d %H:%M:%S %Y",
    on_error  => 'croak',
    time_zone => 'local'
    );

my $strp2 = DateTime::Format::Strptime->new(
    pattern => "%d/%b/%Y:%H:%M:%S %z",
    on_error  => 'croak',
    time_zone => 'local'
    );

my $now = time;

my $mysql = [];
open MLOG, MYSQL or die "Could not open ".MYSQL;
while (<MLOG>) {
    chomp;
    next unless /\d+:\d+:\d+/;

    my @line = split;
    my $stamp = join ' ', @line[0..3,5];
    my $dt = $strp1->parse_datetime($stamp);
    my $then = $dt->epoch;
    my $age = $now - $then;

    next if $age > 24*3600*2;

    if (/restart/) {
	push @$mysql, [$stamp,0,150];
	next;
    }

    my ($cpu,$conn) = /([.0-9]+)% CPU; (\d+) active connections/;
    $stamp =~ s/\s+\d+$//;
    push @$mysql, [$stamp,$cpu,$conn];
}

open JSONF, ">".MYSQL_JSON or die "Could not open ".MYSQL_JSON." for writing";
print JSONF encode_json($mysql);
close JSONF;

my $apache_hist = APACHE_JSON;
chomp(my $sites_hist_string = `cat $apache_hist`); 
my $sites_hist = $sites_hist_string ? decode_json($sites_hist_string) : [];

my $now = time;
my $sites = {};
open WLOG, "tail -5000 ".APACHE." |" or die "Could not open ".APACHE;
while (<WLOG>) {
    my ($stamp) = /\[([^\]]+)\]/;
    my $dt = $strp2->parse_datetime($stamp);
    my $then = $dt->epoch;
    next unless $now - $then <= 600; # last 10 minutes
    chomp;
    s/^.+$stamp\] "[A-Z]+ (\S+).+$/$1/;
    next unless $1;
    if (/cgi-bin/) {
	s/\?\S+//;
    }
    if (/\.css|download|favicon|\.js|.html|.xml|figures|.png|.jpg|.txt|.php/) {
	$_ = 'static_content';
    }
    if (/wordpress|wp-/ || /^\/category/ || /^\/pages/ || /^\/\?/) {
	$_ = 'wordpress_content';
    }
    if (/skypainter/) {
	$_ = 'skypainter';
    }
    if (m!^/[\-a-z0-9]+/$!) {
	$_ = "announcements";
    }
    s!content/([a-zA-Z]+)\S+!content/$1!;
    s!RESTfulAPI\S+!RESTfulAPI!;
    s!download\S+!download!;
    s!PathwayBrowser\S+!PathwayBrowser!;
    s!AnalysisService\S+!AnalysisService!;
    #s!eventbrowser\S+!eventbrowser!;
    s!_st_id\S+!_st_id!;
    next if $_ eq '*';
    next if /\s+/;
    s!^/!!;
    $_ ||= "/";
    $sites->{$_}++;
}

chomp(my $timestamp = `date`);
$timestamp =~ s/ [A-Z]{3}\s+\d{4}$//; 
push @$sites_hist, [$timestamp,$sites];

open JSONF, ">".APACHE_JSON or die "Could not open ".APACHE_JSON." for writing";
print JSONF encode_json($sites_hist);
close JSONF;

__END__
188.165.15.210 - - [15/Nov/2015:12:09:43 -0500] "GET /content/detail/R-HSA-174312 HTTP/1.1" 200 8963
66.249.75.1 - - [15/Nov/2015:12:09:45 -0500] "GET /content/query?q=Unfolded+Protein+Response&species=Homo+sapiens&species=Entries+without+species&cluster=true&page=23 HTTP/1.1" 200 15262
188.165.15.210 - - [15/Nov/2015:12:09:46 -0500] "GET /content/detail/R-SPO-65918 HTTP/1.1" 200 13251
212.122.209.18 - - [15/Nov/2015:12:09:47 -0500] "GET / HTTP/1.1" 200 25780
212.122.209.18 - - [15/Nov/2015:12:09:48 -0500] "GET /wordpress/wp-content/themes/HS_OICR_2013/960_24_col.css HTTP/1.1" 200 8575
212.122.209.18 - - [15/Nov/2015:12:09:48 -0500] "GET /wordpress/wp-content/themes/HS_OICR_2013/reset.css HTTP/1.1" 200 2074
212.122.209.18 - - [15/Nov/2015:12:09:48 -0500] "GET /wordpress/wp-content/themes/HS_OICR_2013/text.css HTTP/1.1" 200 1063
212.122.209.18 - - [15/Nov/2015:12:09:48 -0500] "GET /wordpress/wp-content/themes/HS_OICR_2013/style.css HTTP/1.1" 200 15115
212.122.209.18 - - [15/Nov/2015:12:09:48 -0500] "GET /wordpress/wp-content/themes/HS_OICR_2013/buttons.css HTTP/1.1" 200 2658
212.122.209.18 - - [15/Nov/2015:12:09:48 -0500] "GET /content/resources/css/main.css?v=3 HTTP/1.1" 304 -
212.122.209.18 - - [15/Nov/2015:12:09:48 -0500] "GET /content/resources/css/ebi-fluid.css?v=3 HTTP/1.1" 304 -
