#!/usr/local/bin/perl  -w

# Make sure you don't have "competing" libraries...
# for use @CSHL
use lib "/usr/local/gkb/modules";
# for use @HOME
use lib "$ENV{HOME}/bioperl-1.0";
use lib "$ENV{HOME}/GKB/modules";
use lib "$ENV{HOME}/my_perl_stuff";

use Getopt::Long;
use strict;
use GKB::DocumentGeneration::GenerateTextPDF;
use GKB::DocumentGeneration::ReactomeDatabaseReader;

@ARGV || die <<__END__;

Usage: $0 -user db_user -host db_host -pass db_pass -port db_port -db db_name
__END__

our($opt_file,$opt_depth,$opt_user,$opt_host,$opt_pass,$opt_port,$opt_db,@opt_id,$opt_no_dias,$opt_react_rep,$opt_debug);
$opt_file = "TheReactomeBook";

my $no_dias = '';
my $stdout = '';
my $split = 0;

# Parse commandline
&GetOptions("file:s","depth=i","user:s","host:s","pass:s","port:i","db=s","id:s@",'no_dias' => \$no_dias,'stdout' => \$stdout,"react_rep=i","split" => \$split,"debug");
$opt_db || die "Need database name (-db).\n";

# Set up output formatting parameters
my %params = ("depth_limit" => $opt_depth);
if ($opt_file) {
    $params{"output_file_name"} = $opt_file;
}
if ($stdout) {
    $params{"output_file_stream"} = *STDOUT;
}
if ($no_dias) {
    $params{"include_images_flag"} = 0;
}
if ($split) {
    $params{"split_flag"} = 1;
}

# Set up reader for pulling information out of Reactome
my $reader = GKB::DocumentGeneration::ReactomeDatabaseReader->new();
$reader->set_db_params($opt_db, $opt_user, $opt_host, $opt_pass, $opt_port || 3306,'mysql');
$reader->open_db_connection();
$reader->init_pathways(\@opt_id);
$reader->set_reaction_representation($opt_react_rep);

# Generate PDF
my $text_generator = GKB::DocumentGeneration::GenerateTextPDF->new;
$text_generator->set_params(\%params);
$text_generator->set_reader($reader);
$text_generator->generate_book();
