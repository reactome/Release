#!/usr/local/bin/perl  -w

use lib "$ENV{HOME}/bioperl-1.0";
use lib "$ENV{HOME}/GKB/modules";
use GKB::DBAdaptor;
use Data::Dumper;
use Getopt::Long;
use strict;

@ARGV || die "Usage: $0 -user db_user -host db_host -pass db_pass -port db_port -db db_name";

our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db,$opt_debug,$opt_class,$opt_db_ids,$opt_remove_db_id,$opt_ignore_class,$opt_ignore_atts,$opt_recursive,$opt_reverse);

&GetOptions("user:s",
	    "host:s",
	    "pass:s",
	    "port:i",
	    "db=s",
	    "debug",
	    "class:s",
	    "db_ids:s",
	    "remove_db_id",
	    "ignore_class:s",
	    "ignore_atts:s",
	    "recursive",
	    "reverse",
	    );

$opt_db || die "Need database name (-db).\n";

my $dba = GKB::DBAdaptor->new
    (
     -dbname => $opt_db,
     -user   => $opt_user,
     -host   => $opt_host,
     -pass   => $opt_pass,
     -port   => $opt_port,
     -DEBUG => $opt_debug
     );
     
my %ignore_atts_hash = ();
if (defined $opt_ignore_atts) {
	my @ignore_atts = split(/,/, $opt_ignore_atts);
	my $ignore_att;
	foreach $ignore_att (@ignore_atts) {
		$ignore_atts_hash{$ignore_att} = 1;
	}
}

my $ar = [];
if (defined $opt_db_ids) {
	# Uses supplied list of DB_IDs to select the instances to print.
	# If you have specified an instance class, this will further
	# filter the instances to be printed.
	my @db_ids = split(/,/, $opt_db_ids);
		
	# For each pathway, recursively descend the event hierarchy,
	# right down to the ReferenceEntitys, adding their DB_IDs
	# to our hash of things that we do so want to copy.
	my $instances;
	my $db_id;
	my $out;
	my $t;
	my $follow_path;
	foreach $db_id (@db_ids) {
		$instances = $dba->fetch_instance_by_db_id($db_id, $opt_class);
		if (defined $instances && scalar(@{$instances})>0) {
			push(@{$ar}, $instances->[0]);
			
			if (defined $opt_recursive) {
				($out, $t, $follow_path) = $dba->follow_attributes(-INSTANCE => $instances->[0]);
				foreach $db_id (sort(keys(%{$follow_path}))) {
					push(@{$ar}, $follow_path->{$db_id});
				}
			}
		}
	}
} else {
	$ar = $dba->fetch_instance(-CLASS => $opt_class, -QUERY => [['_displayName', [], 'IS NOT NULL']]);
}

foreach my $i (@{$ar}) {
    print toString($i), "\n";
}

sub toString {
    my $i = shift;
    
    my $out = '';

    if (!(ref($i)) || !($i->isa("GKB::Instance")) || ($i->class() eq $opt_ignore_class)) {
    	return $out;
    }

    my $vals;
    my $val;
    my $val0;
    # Sort attributes alphabetically by name, just to get repeatable results
    foreach my $att (sort($i->list_valid_attributes)) {
    	if ($ignore_atts_hash{$att}) {
    		next;
    	}
		$vals = $i->attribute_value($att);
		if (@{$vals} && scalar(@{$vals})>0) {
			$val0 = $vals->[0];
			if (ref($val0) && $val0->isa("GKB::Instance") && !($val0->class() eq $opt_ignore_class)) {
				# Sort values, just to get repeatable results
				@{$vals} = sort {$a->extended_displayName cmp $b->extended_displayName} @{$vals};
			    foreach $val (@{$vals}) {
					$out .= "$att\t" . $val->extended_displayName . "\n";
			    }
			} else {
				# Sort values, just to get repeatable results
			    foreach $val (sort(@{$vals})) {
					$val = (defined $val) ? $val : 'null';
					$out .= "$att\t$val\n";
			    }
			}
		} else {
		    $out .= "$att\tnull\n";
		}
    }
    
    if (defined $opt_remove_db_id) {
    	$out =~ s/(\[[A-Za-z]+):[0-9]+\]/$1]/g;
    }
    	
    if (!(defined $opt_reverse)) {
	    foreach my $att ($i->list_valid_reverse_attributes) {
			$vals = $i->reverse_attribute_value($att);
			if (@{$vals}) {
			    foreach my $val (@{$vals}) {
					$out .= "($att)\t" . $val->extended_displayName . "\n";
			    }
			} else {
			    $out .= "($att)\tnull\n";
			}
	    }
    }
    
    return $out;
}                     
