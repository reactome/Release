#!/usr/local/bin/perl -w
use strict;

use Data::Dumper;
use File::Path qw/make_path/;
use Getopt::Long;

use GKB::Utils;
use GKB::Instance;
use GKB::Config;

$GKB::Config::NO_SCHEMA_VALIDITY_CHECK = undef;

@ARGV || die("Usage: $0 -db db_name ...\n");

my @params = @ARGV;

our ($opt_user, $opt_host, $opt_pass, $opt_port, $opt_db, $opt_debug);

&GetOptions("user:s", "host:s", "pass:s", "port:i", "db:s", "debug");

$opt_db || die "Need database name (-db).\n";    

# Get connection to reactome db
my $dba = GKB::DBAdaptor->new
    (
     -user   => $opt_user,
     -host   => $opt_host,
     -pass   => $opt_pass,
     -port   => $opt_port,
     -dbname => $opt_db,
#     -DEBUG => $opt_debug
     );

my @species = $dba->species_for_ref_dbs('ENSEMBL','UniProt');
print STDERR "My species:\n", Dumper(\@species);

make_path('output', { mode => 0775 });
my @cmds = (
	qq(./retrieve_indirectIdentifiers_from_mart.pl @params),
	qq(./indirectIdentifiers_from_mart.pl @params),
	qq(./gene_names_from_mart.pl @params)
);

foreach my $sp (@species) {
    foreach my $cmd (@cmds) {
		my $tmp = "$cmd -sp '$sp'";
		print STDERR "Command to be run: " . hide_password($tmp) . "\n";
		system($tmp) == 0 or print(hide_password($tmp) . " failed.\n");
    }
}

print STDERR "$0: no fatal errors.\n";

sub hide_password {
    my $string = shift;
    
    $string =~ /-pass (.*?) /;
    my $password = $1;
    my $asterisks = '*' x 5;
    
    $string =~ s/-pass $password/-pass $asterisks/;
    
    return $string;
}
