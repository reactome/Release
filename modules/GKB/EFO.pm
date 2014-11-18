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
3369 EFO:0000174
5520 EFO:0000181
684  EFO:0000182
2154 EFO:0000212
9952 EFO:0000220
5600 EFO:0000220
3069 EFO:0000272
60166	EFO:0000289
4007	EFO:0000292
1612	EFO:0000305
8584	EFO:0000309
162	EFO:0000311
3620	EFO:0000326
8552	EFO:0000339
4467	EFO:0000349
9256	EFO:0000365
5672	EFO:0000365
1686	EFO:0000516
3068	EFO:0000519
3075	EFO:0000519
1967	EFO:0000564
3264	EFO:0000565
60060	EFO:0000574
3187	EFO:0000632
3969	EFO:0000641
2526	EFO:0000673
10283	EFO:0000673
201	EFO:0000691
3907	EFO:0000708
50731	EFO:0000734
1909	EFO:0000756
934	EFO:0000763
526	EFO:0000764
104	EFO:0000771
399	EFO:0000774
7427	EFO:0000778
4362	EFO:0001061
1324	EFO:0001071
9970	EFO:0001073
2394	EFO:0001075
10871	EFO:0001365
4448	EFO:0001365
5485	EFO:0001376
9538	EFO:0001378
70004	EFO:0001378
8469	EFO:0001669
1793	EFO:0002618
3829	EFO:0002619
3275	EFO:0002626
3277	EFO:0002892
1781	EFO:0002892
1107	EFO:0002916
3247	EFO:0002918
3908	EFO:0003060
60041	EFO:0003756
2596	EFO:0003817
8850	EFO:0003826
5679	EFO:0003839
2891	EFO:0003841
3953	EFO:0003850
263	EFO:0003865
9036	EFO:0003873
10534	EFO:0003897
12960	EFO:0004123
1067	EFO:0004190
1070	EFO:0004190
2513	EFO:0004193
1380	EFO:0004230
3001	EFO:0004230
80001	EFO:0004260
2355	EFO:0004272
2998	EFO:0004281
5557	EFO:0004281
4948	EFO:0004606
1682	EFO:0005207
612	Orphanet:101997
50657	Orphanet:109
5212	Orphanet:137
4480	Orphanet:15
2340	Orphanet:1531
50660	Orphanet:1555
50679	Orphanet:16
627	Orphanet:183660
50572	Orphanet:1872
3471	Orphanet:201
3803	Orphanet:205
2339	Orphanet:207
50534	Orphanet:215
4621	Orphanet:2162
11105	Orphanet:227796
50568	Orphanet:2311
14448	Orphanet:242
60106	Orphanet:2495
50579	Orphanet:263297
13481	Orphanet:2655
50581	Orphanet:294937
3321	Orphanet:309152
2741	Orphanet:309816
50775	Orphanet:3144
3659	Orphanet:3166
50532	Orphanet:35125
9870	Orphanet:352
3322	Orphanet:354
13382	Orphanet:35858
2749	Orphanet:364
2752	Orphanet:365
2750	Orphanet:367
50546	Orphanet:418
50590	Orphanet:42738
80041	Orphanet:429
12305	Orphanet:464
14764	Orphanet:503
9270	Orphanet:56
12802	Orphanet:579
12799	Orphanet:580
12801	Orphanet:581
12804	Orphanet:582
12800	Orphanet:583
12803	Orphanet:584
3429	Orphanet:602
14791	Orphanet:65
5585	Orphanet:65748
2476	Orphanet:685
14705	Orphanet:710
9281	Orphanet:716
50776	Orphanet:777
768	Orphanet:790
10584	Orphanet:791
2747	Orphanet:79201
12798	Orphanet:79213
14695	Orphanet:79237
50715	Orphanet:79282
50716	Orphanet:79283
50717	Orphanet:79284
14749	Orphanet:79311
1699	Orphanet:79394
9120	Orphanet:85443
50544	Orphanet:88618
11661	Orphanet:88629
11193	Orphanet:90025
4810	Orphanet:909
80055	Orphanet:93298
13359	Orphanet:98249
2565	Orphanet:98969
50593	Orphanet:98976
4972	EFO:0000198
50588	Orphanet:371024
9553	EFO:0005539
4897	EFO:0005540
80006	EFO:0005541
11976	EFO:0005542
60108	EFO:0005543
60105	EFO:0002939
50570	EFO:0005545
50571	EFO:0005546
12205	EFO:0005547
60037	EFO:0005548
11405	EFO:0005549
150	EFO:0000677
2679	EFO:0005551
14252	Orphanet:99002
4921	EFO:0005553
13934	Orphanet:156224
60013	EFO:0005555
2739	EFO:0005556
8602	EFO:0005557
583	EFO:0005558
687	Orphanet:449
206	EFO:0005560
4231	EFO:0005561
11111	EFO:0005562
12678	EFO:0005563
655	Orphanet:68367
60008	EFO:0005565
3211	Orphanet:68366
1788	EFO:0005567
50732	EFO:0005568
10629	EFO:0005569
8618	EFO:0005570
2256	EFO:0005571
6212	EFO:0004230
11166	EFO:0001668
1540	Orphanet:143
3449	EFO:0005575
13381	EFO:0005576
60119	EFO:0005577
1785	EFO:0005578
3765	EFO:0005579
13910	EFO:0005580
13909	EFO:0005581
4919	EFO:0005582
10609	EFO:0005583
6498	EFO:0005584
12385	EFO:0005585
3165	EFO:0004198
3151	EFO:0000707
4907	EFO:0005588
80027	Orphanet:253
1701	EFO:0005590
5667	EFO:0005591
715	EFO:0005592
11338	EFO:0005593
8649	EFO:0003871
3602	EFO:0005595
50718	EFO:0005596
50733	EFO:0005597
