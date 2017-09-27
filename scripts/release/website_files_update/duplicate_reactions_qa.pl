#!/usr/local/bin/perl  -w
use strict;

use autodie;
use Getopt::Long;

use lib '/usr/local/gkb/modules';
use GKB::CommonUtils;
use GKB::DBAdaptor;

my ($db, $host, $help);

GetOptions(
    "db:s" => \$db,
    "host:s" => \$host,
    "help" => \$help
);

if ($help) {
    print usage_instructions();
    exit;
}

$db ||= 'gk_central';
$host ||= 'reactomecurator.oicr.on.ca';

my $dba = get_dba($db, $host);
my @reaction_like_events =
    grep {
        $_->_doRelease->[0] &&
        $_->_doRelease->[0] =~ /TRUE/i &&
        has_value_for_any_attribute($_, get_defining_attributes_for_reaction_class($dba))
    } @{$dba->fetch_instance(-CLASS => 'ReactionlikeEvent')};
my @reaction_like_event_buckets = (\@reaction_like_events);
foreach my $attribute (get_defining_attributes_for_reaction_class($dba), 'compartment','species') {
   @reaction_like_event_buckets = grep { scalar @{$_} > 1 } get_buckets_by_attribute_ids($attribute, @reaction_like_event_buckets);
}

(my $output_file = $0) =~ s/\.pl$/.txt/;
open(my $fh, '>', $output_file);
foreach my $bucket (@reaction_like_event_buckets) {    
    print $fh join('|', map {
        join('|', $_->db_id, $_->displayName, get_event_modifier($_))
    } @{$bucket}) . "\n";
}
close $fh;

sub get_buckets_by_attribute_ids {
    my $attribute = shift;
    my @old_buckets = @_;
    
    my @new_buckets;
    foreach my $old_bucket (@old_buckets) {
        my %attribute_ids_to_reaction_like_events;
        foreach my $reaction_like_event (@{$old_bucket}) {
            my $attribute_ids = join '_', sort {$a <=> $b} map {$_->db_id} @{$reaction_like_event->$attribute};
            push @{$attribute_ids_to_reaction_like_events{$attribute_ids}}, $reaction_like_event;
        }
        push @new_buckets, values %attribute_ids_to_reaction_like_events;
    }
    
    return @new_buckets;
}

sub has_value_for_any_attribute {
    my $reaction = shift;
    my @attributes = @_;
    
    foreach my $attribute (@attributes) {
        return 1 if $reaction->$attribute->[0];
    }
    return 0;    
}

sub get_defining_attributes_for_reaction_class {
    my $dba = shift;
    
    return $dba->ontology->list_class_attributes_with_defining_type('Reaction', 'all')
}

sub usage_instructions {
    return <<END;

    This script reports released reactions whose defining attributes (input,
    output, catalyst activity, and entity functional status) and compartment
    attribute contain the same instances.
    
    The output file for this script is the name of the script with a .txt
    extension.
    
    USAGE: perl $0 [options]
    
    Options:
    
    -db [db_name]   Source database (default is gk_central)
    -host [db_host] Host of source database (default is reactomecurator.oicr.on.ca)
    -help           Display these instructions
END
}