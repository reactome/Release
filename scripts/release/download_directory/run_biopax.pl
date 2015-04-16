#!/usr/bin/perl -w
use strict;

use Log::Log4perl qw/get_logger/;
Log::Log4perl->init(\$LOG_CONF);

my ($host,$db,$user,$pass,$port,$release) = @ARGV;
$host && $db && $user && $pass && $port && $release || die "Usage: ./run_biopax.pl host db user pass port releasenum\n";


use constant EXPORTER  => '/usr/local/gkb/java/biopaxexporter';
use constant VALIDATOR => '/usr/local/gkb/java/biopaxvalidator';

die unless -e EXPORTER && VALIDATOR;
my $biopaxexporter  = EXPORTER;
my $biopaxvalidator = VALIDATOR;

chomp(my $cwd = `pwd`);
mkdir $release unless -d $release;

chdir $biopaxexporter or die $!;
run_or_die("rm -fr $release") if -d $release;
run_or_die("./runAllSpecies.sh $host $db $user $pass $port $release");
chdir $biopaxvalidator or die $!;
run_or_die("./validate_biopax.pl $release");
chdir "$biopaxexporter/$release" or die $!;
run_or_die("zip biopax2 *.owl");
run_or_die("zip biopax2_validator *.xml");
chdir $cwd or die $!;
run_or_die("mv $biopaxexporter/$release/biopax*.zip $release");

chdir $biopaxexporter or die $!;
run_or_die("rm -fr $release") if -d $release;
run_or_die("./runAllSpeciesLevel3.sh $host $db $user $pass $port $release");
chdir $biopaxvalidator or die $!;
run_or_die("./validate_biopax.pl $release");
chdir "$biopaxexporter/$release" or die $!;
run_or_die("zip biopax *.owl");
run_or_die("zip biopax_validator *.xml");
chdir $cwd or die $!;
run_or_die("mv $biopaxexporter/$release/biopax*.zip $release");

sub run_or_die {
    my $cmd = shift;
    
    my $logger = get_logger(__PACKAGE__);
    
    $logger->info("Executing: $cmd\n");
    my $retval = system $cmd;
    $logger->error_die($!) if $retval;
}
