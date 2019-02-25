#!/usr/bin/perl
use strict;
use warnings;

use Data::Dumper;
use File::Path qw/make_path/;
use Getopt::Long;

use GKB::Config;
use GKB::Config_Species;

$GKB::Config::NO_SCHEMA_VALIDITY_CHECK = undef;

my @params = @ARGV;

our ($opt_user, $opt_host, $opt_pass, $opt_port, $opt_db, $opt_debug);

&GetOptions("user:s", "host:s", "pass:s", "port:i", "db:s", "debug");

$opt_db || die "Need database name (-db).\n";

print "My species:\n", Dumper(\@species);

make_path('output', { mode => 0775 });
my @cmds = (
    qq(./retrieve_indirectIdentifiers_from_mart.pl @params),
    qq(./indirectIdentifiers_from_mart.pl @params),
    qq(./gene_names_from_mart.pl @params)
);

foreach my $sp (@species) {
    foreach my $cmd (@cmds) {
        my $tmp = "$cmd -sp '$sp'";
        print "Command to be run: " . hide_password($tmp) . "\n";
        system($tmp) == 0 or print(hide_password($tmp) . " failed.\n");
    }
}

print "$0: no fatal errors.\n";

sub hide_password {
    my $string = shift;

    $string =~ /-pass (.*?) /;
    my $password = $1;

    if (!$password) {
        return $string;
    }

    my $asterisks = '*' x 5;
    $string =~ s/-pass $password/-pass $asterisks/;
    return $string;
}
