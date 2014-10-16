#!/usr/local/bin/perl

# Make sure you don't have "competing" libraries...
# for use @CSHL
use lib "/usr/local/reactomes/Reactome/production/GKB/modules";
# for use @HOME
use lib "$ENV{HOME}/bioperl-1.0";
use lib "$ENV{HOME}/GKB/modules";

use GKB::ClipsAdaptor;
use GKB::DBAdaptor;
use GKB::Utils;
use GKB::Ontology;
use Data::Dumper;
use Getopt::Long;
use strict;

(@ARGV) || die "A script for checking if the values in inverse attribute pairs match.\n\nUsage: $0 -user db_user -host db_host -pass db_pass -port db_port -db db_name -debug\n";

our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db,$opt_debug);

&GetOptions("user:s", "host:s", "pass:s", "port:i", "db:s", "debug");

$opt_db || die "Need database name (-db).\n";    

my $dba = GKB::DBAdaptor->new
    (
     -user   => $opt_user,
     -host   => $opt_host,
     -pass   => $opt_pass,
     -port   => $opt_port,
     -dbname => $opt_db,
     -DEBUG => $opt_debug
     );

foreach my $class ($dba->ontology->list_classes) {
    print "$class\n";
    my $class_instances;
    foreach my $att ($dba->ontology->list_own_attributes($class)) {
	my $inverse_att;
	if ($inverse_att = $dba->ontology->inverse_attribute(':CLIPS_TOP_LEVEL_SLOT_CLASS',$att)) {
	    print "$class\t$att\t $inverse_att\n";
	    $class_instances ||= $dba->fetch_all_class_instances_as_shells($class);
	    foreach my $i (@{$class_instances}) {
		#print "$class\t$att\t $inverse_att\t", $i->extended_displayName, "\n";
		foreach my $vi (grep {$_->is_attribute_allowed_class_instance($inverse_att,$i)}
				grep {$_->is_valid_attribute($inverse_att)} 
				@{$i->attribute_value($att)}) {
		    check_inverse_attribute_values($i,$att,$vi,$inverse_att);
		}
		$i->deflate;
	    }
	}
    }
    $dba->instance_cache->empty;
}

sub check_inverse_attribute_values {
    my ($instance,$att,$value,$inv_att) = @_;
    my @tmp1 = grep {$_ == $value} @{$instance->attribute_value($att)};
    my @tmp2 = grep {$_ == $instance} @{$value->attribute_value($inv_att)};
    return if (scalar @tmp1 == scalar @tmp2);
    print join("\t", scalar(@tmp1),$att,$instance->extended_displayName), "\n",
    join("\t",scalar(@tmp2),$inv_att,$value->extended_displayName), "\n",
    qq(http://brie2.cshl.org:8086/cgi-bin/instancebrowser?ID=) . $instance->db_id . qq(&ID=) . $value->db_id .
    "\n\n";
}
