#!/usr/bin/perl -w
use strict;

use constant EXPORTER  => '/usr/local/gkb/java/biopaxexporter';
use constant EXE => join(' ',
			 'validate.sh',
			 'file:EXPORTER/RELEASE/SPECIES.owl',
                         '--out-format=xml',
			 '--output=EXPORTER/RELEASE/SPECIES_validator_output.xml',
			 '--profile=notstrict');

my $release_nr = shift or die "Usage: ./validate_biopax.pl release_num\n";

my $exporter = EXPORTER;
opendir D, "$exporter/$release_nr" or die $!;
my @owl = grep {/owl/} readdir D;
closedir D;			 

for my $species (@owl) {
    (my $nospace = $species) =~ s/\s+/_/g;
    system "mv '$exporter/$release_nr/$species' $exporter/$release_nr/$nospace"
	unless $species eq $nospace;
    $species = $nospace;
    $species =~ s/.owl$//;

    my $cmd = EXE;
    $cmd =~ s/SPECIES/$species/g;
    $cmd =~ s/RELEASE/$release_nr/g;
    $cmd =~ s/EXPORTER/$exporter/g;

    print STDERR "Validating $species.owl with command '$cmd'\n";
    system "bash $cmd";
    print STDERR "Done with $species.owl\n";
}

exit 0;



