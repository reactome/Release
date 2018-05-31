#!/usr/bin/perl
use strict;
use warnings;

use lib '/usr/local/gkb/modules';

use autodie qw/:all/;
use Getopt::Long;
Getopt::Long::Configure("pass_through");

use GKB::Config;
use GKB::DBAdaptor;

#use Log::Log4perl qw/get_logger/;
#Log::Log4perl->init(\$LOG_CONF);
#my $logger = get_logger(__PACKAGE__);

my ($db, $help);
GetOptions(
    'db=s' => \$db,
    'help' => \$help
);

if (@ARGV) {
    die "\"@ARGV\" are invalid options.  Please see usage instructions:\n\n" . usage_instructions();
}

if ($help) {
    print usage_instructions();
    exit;
}

$db ||= $GKB::Config::GK_DB_NAME;
print "Checking $db\n";
my $dba = get_dba($db);

my @stable_identifier_instances = @{$dba->fetch_instance(-CLASS => 'StableIdentifier')};
my %unique_stable_ids;

(my $outfile = $0) =~ s/\.pl/\.txt/;
open my $out, '>', $outfile;
foreach my $stable_id_instance (@stable_identifier_instances) {
    my $stable_id = $stable_id_instance->identifier->[0];
    
    unless ($stable_id) {
	report("Identifier empty for stable id instance " . $stable_id_instance->db_id, $out);
	next;
    }
    
    if ($unique_stable_ids{$stable_id} && $unique_stable_ids{$stable_id} == 1) {
	report("$stable_id is duplicated", $out);
    }
    $unique_stable_ids{$stable_id}++;
    
    
    my @referrers = @{$stable_id_instance->reverse_attribute_value('stableIdentifier')};
    if (@referrers) {
	if (scalar @referrers > 1) {
	    report("$stable_id is used more than once: " . join("\t", map({$_->db_id} @referrers)), $out);
	}
    } else {
	report("$stable_id is not used", $out);
    }
}
report("$0 has finished");
close $out;

sub get_dba {
    my $db = shift;
    return GKB::DBAdaptor->new (
        -user => $GKB::Config::GK_DB_USER,
        -pass => $GKB::Config::GK_DB_PASS,
        -host => $GKB::Config::GK_DB_HOST,
        -dbname => $db
    );
}

sub report {
    my $message = shift;
    my $fh = shift;
    
    print "$message\n";
    print $fh "$message\n" if $fh;
}

sub usage_instructions {
    return <<END;
For all stable identifiers in a database (default is $GKB::Config::GK_DB_NAME),
the following types of issues will be reported:

A stable id instance has an empty identifier slot
A stable id is duplicated
A stable id is used by more than one instance in the database
A stable id is not used

The output file will be the name of this script with
a .txt extension.

Usage: perl $0 [options]

Options:

-db [db_name]   Source database (default is $GKB::Config::GK_DB_NAME)
-help           Display these instructions

END
}