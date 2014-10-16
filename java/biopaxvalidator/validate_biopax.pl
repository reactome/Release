#!/usr/bin/perl -w
use strict;

use constant EXE => join(' ',
			 'validate.sh',
			 'file:../biopaxexporter/RELEASE/SPECIES.owl',
                         '--out-format=xml',
			 '--output=../biopaxexporter/RELEASE/SPECIES_validator_output.xml',
			 '--profile=notstrict');

my $release_nr = shift or die "Usage: ./validate_biopax.pl release_num\n";

opendir D, "../biopaxexporter/$release_nr" or die $!;
my @owl = grep {!/_/} grep {/owl/} readdir D;
closedir D;			 

for my $species (@owl) {
    (my $nospace = $species) =~ s/\s+/_/g;
    system "mv '../biopaxexporter/$release_nr/$species' ../biopaxexporter/$release_nr/$nospace";
    $species = $nospace;
    $species =~ s/.owl$//;

    my $cmd = EXE;
    $cmd =~ s/SPECIES/$species/g;
    $cmd =~ s/RELEASE/$release_nr/g;
    
    print STDERR "Validating $species.owl with command '$cmd'\n";
    system "bash $cmd";
    print STDERR "Done with $species.owl\n";
}

exit 0;



