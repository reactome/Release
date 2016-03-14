#!/usr/bin/perl
use strict;
use warnings;

use lib '/usr/local/gkb/modules';

use autodie qw/:all/;
use Getopt::Long;

use GKB::Config;
use GKB::DBAdaptor;

use Log::Log4perl qw/get_logger/;
Log::Log4perl->init(\$LOG_CONF);
my $logger = get_logger(__PACKAGE__);

my ($db, $help);
GetOptions(
    'db:s' => \$db,
    'help' => \$help
);

if ($help) {
    print usage_instructions();
    exit;
}

(my $skip_list_file = $0) =~ s/.pl$/.txt/;
open my $skip_list_fh, ">", "$skip_list_file";

my %seen;
 
my @disease_pathways = @{get_dba($db)->fetch_instance_by_remote_attribute('Pathway', [['disease', 'IS NOT NULL',[]]])};
foreach my $disease_pathway (@disease_pathways) {
    foreach my $pathway_event (@{get_events($disease_pathway)}) {
	next unless $pathway_event->is_a('ReactionlikeEvent');
	next if $pathway_event->disease->[0];
	next if $seen{$pathway_event->db_id}++;
	
	report($pathway_event->db_id, $skip_list_fh);
    }
}


sub get_dba {
    my $db = shift;
    
    return GKB::DBAdaptor->new (
	-user => $GKB::Config::GK_DB_USER,
	-pass => $GKB::Config::GK_DB_PASS,
	-dbname => $db || $GKB::Config::GK_DB_NAME
    );
}


sub get_events {
    my $event = shift;
    return $event->follow_class_attributes(-INSTRUCTIONS => {'Event' => {'attributes' =>[qw(hasEvent)]}},
						      -OUT_CLASSES => ['Event']);
}

sub report {
    my $message = shift;
    my $fh = shift;
    
    print "$message\n";
    print $fh "$message\n" if $fh;
}



sub usage_instructions {
    print <<END;
For all disease pathways, a text file (with the same name as this program)
is created listing database identifiers of normal reaction like events.
This is used as a QA check of which normal events in disease pathways we
want the orthoinference release step to skip.

Usage: perl $0 [options]

Options:

-db [db_name]   Source database (default is $GKB::Config:GK_DB_NAME)
-help           Display these instructions

END
}