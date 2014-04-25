#!/usr/local/bin/perl  -w

# Creates indexes for a single PSICQUIC server, and puts them into a place
# where they will be accessible to the site hosting the servers for pick up.

use Getopt::Long;
use strict;
use GKB::PSICQUICIndexers::Director;
use GKB::PSICQUICIndexers::BuilderParams;

our($opt_release,$opt_gk_root,$opt_builder);

# Parse commandline
my $usage = "Usage: $0 -release release_num -gk_root GKB_root_dir -builder builder_name\n";
&GetOptions("release:s","gk_root:s","builder:s");
$opt_release || die $usage;
$opt_gk_root || die $usage;

my $builder_params = GKB::PSICQUICIndexers::BuilderParams->new();
$builder_params->set_release_num($opt_release);
$builder_params->set_gkb_root_dir($opt_gk_root);

my $director = GKB::PSICQUICIndexers::Director->new();
$director->set_builder_params($builder_params);
$director->add_builder($opt_builder);
$director->construct();
