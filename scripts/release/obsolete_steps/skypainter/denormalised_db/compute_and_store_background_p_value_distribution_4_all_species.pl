#!/usr/local/bin/perl -w

BEGIN {
    my @a = split('/',$0);
    pop @a;
    push @a, ('..','..','modules');
    my $libpath = join('/', @a);
    unshift (@INC, $libpath);
}

use strict;
use GKB::Utils;
use GKB::SkyPainter;
use Getopt::Long;

@ARGV || die qq(
$0 -start range_start -stop range_end [-iter num_ierations default 1000] -db db_name [-host db_host default localhost] [-user db_user] [-pass db_pass] [-port db_port default 3306]

This script computes and stores background p-value distributions for all species. You need to specify the range (of length of the identifier list to be analysed) for which the background p-values will be computed. Use -start and -stop to specify the beginning and the end (inclusive), respectively, of the range. You can also specify the number of iterations. If not, 1000 iterations are performed. Please note that the longer the list the longer it takes to compute the p-value.
In adition the script also expects the "normal" commandline parameters specifying the host, port, db name, db user and db pass.
);

my $dba = GKB::Utils::get_db_connection();

our($opt_start,$opt_stop,$opt_iter);
&GetOptions("start=i","stop=i","iter:i");

my $species_ar = $dba->fetch_all_class_instances_as_shells('Species');

foreach my $species (@{$species_ar}) {
    next if (@{$species->reverse_attribute_value('species')}<=100);
#    next if ($species->displayName eq 'Homo sapiens');
    printf "Now handling %s\n", $species->extended_displayName;
    my $sp = GKB::SkyPainter->new_overrepresentation_analysis_only(-DNDBA => $dba);
    $opt_iter && $sp->iterations($opt_iter);
    $sp->compute_and_store_background_p_value_distribution_for_species($species,$opt_start,$opt_stop);
    $dba->instance_cache->empty;
}
print STDERR "$0: no fatal errors.\n";

