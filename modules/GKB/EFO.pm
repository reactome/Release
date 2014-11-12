package GKB::EFO;

=head1 NAME

GKB::Zinc

=head1 SYNOPSIS

Methods for mapping DOID Ids to EFO/Orphanet

=head1 DESCRIPTION

=head1 SEE ALSO

GKB::AddLinks::EFO

=head1 AUTHOR

Sheldon McKay E<lt>sheldon.mckay@gmail.com<gt>

Copyright (c) 2014 Ontario Institute for Cancer Research

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

use strict;
use HTTP::Tiny;
use CGI 'url';
use Bio::Root::Root;
use Data::Dumper;


# Use our RESTful API to grab a list of diseases and their DOIDs
use constant DISEASES => '/ReactomeRESTfulAPI/RESTfulWS/getDiseases';

use vars '@ISA';
@ISA = qw(Bio::Root::Root);

sub new {
    my $class = shift;
    my $self  = bless {}, $class;
    my $dba   = shift || $self->throw("The GKB::EFO class requires a DBA object");
    $self->dba($dba);
    return $self;
}

sub dba {
    my $self = shift;
    my $dba  = shift;
    $self->{dba} = $dba if $dba;
    return $self->{dba};
}

sub fetch_diseases {
    my $self = shift;
    my $host = url(-base => 1);
    my $url = $host . DISEASES;
    my $doid2efo = $self->doid_map();

    my @data = $self->fetch_url_content($url);
    my %diseases;

    for (@data) {
        my ($db_id,$doid) = split;
        $doid =~ s/^\D+//g;

        # We only want diseases that map to EFO/Orphanet Ids 
        next unless $doid2efo->{$doid};

        push @{$diseases{$doid}}, $self->get_cached_instance($db_id);
    }

    my %efo;
    for my $doid_id (keys %diseases) {
        my $efo_ids = $doid2efo->{$doid_id};
        my $instances = $diseases{$doid_id};
        for my $efo_id (@$efo_ids) {
            for my $instance (@$instances) {
                push @{$efo{$efo_id}}, $instance;
            }
        }
    }

    my $total = keys %efo;
    print STDERR "I am dealing with a total of about $total EFO IDS!\n";
    sleep 10;
    return \%efo;
}

sub get_instance {
    my $self  = shift;
    my $db_id = shift;
    return $self->dba->fetch_instance_by_db_id($db_id)->[0];
}

sub get_cached_instance {
    my $self  = shift;
    my $db_id = shift;

    $self->{cached_instance}->{$db_id} ||= $self->get_instance($db_id);

    return $self->{cached_instance}->{$db_id};
}

# Simple HTTP query, expects plain text
sub fetch_url_content {
    my $self = shift;
    my $url = shift;

    my $response = HTTP::Tiny->new->get($url);

    if ($response->{success}) {
        return split("\n",$response->{content});
    }
    else {
        $self->throw(join("\t",$response->{status},$response->{reason}));
    }
}

# DOID -> EFO map
sub doid_map {
    my $self = shift;
    my %doid;
    while (<DATA>) {
        chomp;
	$_ or next;
	my ($doid,$efo) = split;
        push @{$doid{$doid}},$efo;
    }
    return \%doid;
}



1;


# This is the current DOID to EFO mapping
__DATA__
3369	EFO:000017
5520	EFO:000018
684	EFO:000018
2154	EFO:000021
9952	EFO:000022
5600	EFO:000022
3069	EFO:000027
60166	EFO:000028
4007	EFO:000029
1612	EFO:000030
8584	EFO:000030
162	EFO:000031
3620	EFO:000032
8552	EFO:000033
4467	EFO:000034
9256	EFO:000036
5672	EFO:000036
1686	EFO:000051
3068	EFO:000051
3075	EFO:0000519
1967	EFO:000056
3264	EFO:000056
60060	EFO:000057
3187	EFO:000063
3969	EFO:000064
2526	EFO:000067
10283	EFO:000067
201	EFO:0000691
3907	EFO:000070
50731	EFO:000073
1909	EFO:000075
934	EFO:000076
526	EFO:000076
104	EFO:0000771
399	EFO:000077
7427	EFO:000077
4362	EFO:000106
1324	EFO:000107
9970	EFO:000107
2394	EFO:000107
10871	EFO:000136
4448	EFO:000136
5485	EFO:000137
9538	EFO:000137
70004	EFO:000137
8469	EFO:000166
1793	EFO:000261
3829	EFO:000261
3275	EFO:000262
3277	EFO:000289
1781	EFO:000289
1107	EFO:000291
3247	EFO:000291
3908	EFO:000306
60041	EFO:000375
2596	EFO:000381
8850	EFO:000382
5679	EFO:000383
2891	EFO:000384
3953	EFO:000385
263	EFO:000386
9036	EFO:000387
10534	EFO:000389
12960	EFO:000412
1067	EFO:000419
1070	EFO:000419
2513	EFO:000419
1380	EFO:000423
3001	EFO:0004230
80001	EFO:000426
2355	EFO:000427
2998	EFO:000428
5557	EFO:000428
4948	EFO:000460
1682	EFO:000520
612	Orphanet:10199
50657	Orphanet:10
5212	Orphanet:13
4480	Orphanet:1
2340	Orphanet:153
50660	Orphanet:155
50679	Orphanet:1
627	Orphanet:18366
50572	Orphanet:187
3471	Orphanet:20
3803	Orphanet:20
2339	Orphanet:20
50534	Orphanet:21
4621	Orphanet:216
11105	Orphanet:22779
50568	Orphanet:231
14448	Orphanet:24
60106	Orphanet:2495
50579	Orphanet:26329
13481	Orphanet:265
50581	Orphanet:29493
3321	Orphanet:30915
2741	Orphanet:30981
50775	Orphanet:314
3659	Orphanet:316
50532	Orphanet:35125
9870	Orphanet:35
3322	Orphanet:35
13382	Orphanet:3585
2749	Orphanet:36
2752	Orphanet:36
2750	Orphanet:36
50546	Orphanet:418
50590	Orphanet:4273
80041	Orphanet:42
12305	Orphanet:46
14764	Orphanet:50
9270	Orphanet:5
12802	Orphanet:57
12799	Orphanet:58
12801	Orphanet:58
12804	Orphanet:58
12800	Orphanet:58
12803	Orphanet:58
3429	Orphanet:60
14791	Orphanet:6
5585	Orphanet:6574
2476	Orphanet:68
14705	Orphanet:71
9281	Orphanet:71
50776	Orphanet:77
768	Orphanet:79
10584	Orphanet:79
2747	Orphanet:7920
12798	Orphanet:7921
14695	Orphanet:7923
50715	Orphanet:7928
50716	Orphanet:7928
50717	Orphanet:7928
14749	Orphanet:7931
1699	Orphanet:7939
9120	Orphanet:8544
50544	Orphanet:8861
11661	Orphanet:8862
11193	Orphanet:9002
4810	Orphanet:90
80055	Orphanet:9329
13359	Orphanet:9824
2565	Orphanet:9896
50593	Orphanet:9897
4972	EFO:000019
50588	Orphanet:37102
9553	EFO:000553
4897	EFO:000554
80006	EFO:000554
11976	EFO:000554
60108	EFO:000554
60105	EFO:000293
50570	EFO:000554
50571	EFO:000554
12205	EFO:000554
60037	EFO:000554
11405	EFO:000554
150	EFO:000067
2679	EFO:000555
14252	Orphanet:9900
4921	EFO:000555
13934	Orphanet:15622
60013	EFO:000555
2739	EFO:000555
8602	EFO:000555
583	EFO:000555
687	Orphanet:44
206	EFO:000556
4231	EFO:000556
11111	EFO:000556
12678	EFO:000556
655	Orphanet:6836
60008	EFO:000556
3211	Orphanet:6836
1788	EFO:000556
50732	EFO:000556
10629	EFO:000556
8618	EFO:000557
2256	EFO:000557
6212	EFO:000423
11166	EFO:000166
1540	Orphanet:14
3449	EFO:000557
13381	EFO:000557
60119	EFO:000557
1785	EFO:000557
3765	EFO:000557
13910	EFO:000558
13909	EFO:000558
4919	EFO:000558
10609	EFO:000558
6498	EFO:000558
12385	EFO:000558
3165	EFO:000419
3151	EFO:000070
4907	EFO:000558
80027	Orphanet:25
1701	EFO:000559
5667	EFO:000559
715	EFO:000559
11338	EFO:000559
8649	EFO:000387
3602	EFO:000559
50718	EFO:000559
50733	EFO:000559

