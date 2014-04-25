#!/usr/local/bin/perl

# Make sure you don't have "competing" libraries...
# for use @CSHL
use lib "/usr/local/gkb/modules";
# for use @HOME
use lib "$ENV{HOME}/bioperl-1.0";
use lib "$ENV{HOME}/GKB/modules";
use lib "$ENV{HOME}/my_perl_stuff";


use GKB::SchemaAdaptor;
use IO::String;
use GKB::ClipsAdaptor;
use GKB::Ontology;
use GKB::DBAdaptor;
use GKB::Utils;
use Data::Dumper;
use Getopt::Long;
use strict;

our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db,$opt_debug,$opt_type);

&GetOptions("user:s", "host:s", "pass:s", "port:i", "db:s", "debug");

(@ARGV) || die "Usage: $0 project.pprj -user db_user -host db_host -pass db_pass -port db_port -db db_name -debug -type table_type\n";

$opt_db || die "Need database name (-db).\n";    

my $ca = GKB::ClipsAdaptor->new(-FILE => $ARGV[0], -DEBUG => $opt_debug);
$ca->attach_pins_file_stub;
my $new_o = $ca->ontology;

my $dba = GKB::DBAdaptor->new
    (
     -user   => $opt_user,
     -host   => $opt_host,
     -pass   => $opt_pass,
     -port   => $opt_port,
     -dbname => $opt_db,
     -DEBUG => $opt_debug
     );
$dba->table_type($dba->fetch_table_type('Ontology'));
my $old_o = $dba->ontology;

# Have to swap in the new ontology since dba adaptor uses it
$dba->ontology($new_o);

$dba->execute('START TRANSACTION');

# Add things in new but not in old
foreach my $class ($new_o->list_classes) {
    if ($old_o->is_valid_class($class)) {
	foreach my $att ($new_o->list_own_attributes($class)) {
	    if ($old_o->is_own_attribute($class,$att)) {
#	    if ($old_o->is_valid_class_attribute($class,$att)) {
#		unless ($new_o->is_multivalue_class_attribute($class,$att) == $old_o->is_multivalue_class_attribute($class,$att)) {
#		    die "Can't handle single<->multivalue attribute conversion for class '$class' attribute '$att'.\n";
#		}
		my $table = ($new_o->is_multivalue_class_attribute($class,$att)) ?
		    "$ {class}_2_$att" :
		    $class;
		if ($new_o->is_multivalue_class_attribute($class,$att) != $old_o->is_multivalue_class_attribute($class,$att)) {
		    my $stmt;
		    if ($new_o->is_multivalue_class_attribute($class,$att)) {
			$dba->create_multivalue_attribute_table($class,$att);
			if ($new_o->is_instance_type_class_attribute($class,$att)) {
			    $stmt = "INSERT INTO $table ($DB_ID_NAME,$att,$ {att}_class,$ {att}_rank) SELECT $DB_ID_NAME,$att,$ {att}_class,0 FROM $class O WHERE O.$att IS NOT NULL";
			} else {
			    $stmt = "INSERT INTO $table ($DB_ID_NAME,$att,$ {att}_rank) SELECT $DB_ID_NAME,$att,0 FROM $class O WHERE O.$att IS NOT NULL";
			}
		    } else {
			foreach (@{$dba->create_attribute_column_definitions($class,$att)}) {
			    my $statement = qq(ALTER TABLE $class ADD $_);
			    $dba->execute($statement);
			}
			my $old_table = "$ {class}_2_$att";
			if ($new_o->is_instance_type_class_attribute($class,$att)) {
			    $stmt = "UPDATE $table N, $old_table O SET N.$att=O.$att,N.$ {att}_class=O.$ {att}_class WHERE N.$DB_ID_NAME = O.$DB_ID_NAME";
			} else {
			    $stmt = "UPDATE $table N, $old_table O SET N.$att=O.$att WHERE N.$DB_ID_NAME = O.$DB_ID_NAME";
			}
		    }
#		    print "$stmt\n";
		    $dba->execute($stmt);
		} elsif ($new_o->class_attribute_db_col_type($class,$att) eq $old_o->class_attribute_db_col_type($class,$att)) {
		    my $t = $new_o->class_attribute_type($class,$att);
		    my $db_col_type = $new_o->class_attribute_db_col_type($class,$att) || $dba->$t;
		    my $table_type = lc($dba->fetch_table_type($table));
		    # Wrap it since this can fail due to existing index...
		    $dba->db_handle->{PrintError} = 0;
		    if (($db_col_type =~ /VARCHAR/i) && ($table_type eq 'myisam')) {
			eval {$dba->execute(qq/ALTER TABLE $table ADD FULLTEXT $ {att}_fulltext ($att)/);};
		    } elsif (($db_col_type =~ /TEXT/i) && ($table_type eq 'innodb')) {
			eval {$dba->execute(qq/ALTER TABLE $table ADD INDEX $att ($att(10))/);};
		    }
		    $dba->db_handle->{PrintError} = 1;
		} else {
		    my $col_defs = $dba->create_attribute_column_definitions($class,$att);
#		    print join("\n",@{$col_defs}), "\n";
		    my $statement = qq(ALTER TABLE $table MODIFY ) . shift @{$col_defs};
		    # Have to drop indeces 1st since the new column definition may be incompatible with existing indeces
		    $dba->execute(qq(ALTER TABLE $table DROP INDEX $att));
		    # Switch off the error reporting for this statement which can fail
		    $dba->db_handle->{PrintError} = 0;
		    eval {$dba->execute(qq(ALTER TABLE $table DROP INDEX $ {att}_fulltext))};
		    $dba->db_handle->{PrintError} = 1;
		    $dba->execute($statement);
		    foreach (grep {/^(INDEX|FULLTEXT)/i} @{$col_defs}) {
			$dba->execute(qq(ALTER TABLE $table ADD $_));
		    }
		}
		
	    } else {
		if ($new_o->is_multivalue_class_attribute($class,$att)) {
		    $dba->create_multivalue_attribute_table($class,$att);
		} else {
		    foreach (@{$dba->create_attribute_column_definitions($class,$att)}) {
			my $statement = qq(ALTER TABLE $class ADD $_);
			$dba->execute($statement);
		    }
		}
	    }
	}
    } else {
	$dba->create_class_tables($class);
	# Right... here we may need to fill the DB_ID column if the subclass of this class has instances.
	if (my @subclasses = grep {$old_o->is_valid_class($_)} $new_o->descendants($class)) {
	    my $root_table = $old_o->root_class;
	    my $stmt = 
		qq/INSERT INTO $class ($DB_ID_NAME) SELECT $DB_ID_NAME FROM $root_table WHERE _class IN(/ .
		join(",",(('?') x scalar(@subclasses))) .
		qq/)/;
	    $dba->execute($stmt,@subclasses);
	}
    }
}

# Transfer values of attributes which have moved from sub-class to superclass
# or the other way round.
foreach my $class (sort {$a cmp $b} $old_o->list_classes) {
    if ($new_o->is_valid_class($class)) {
	foreach my $att ($old_o->list_own_attributes($class)) {
	    if ($new_o->is_valid_class_attribute($class,$att)) {
		my $att_new_origin = $new_o->class_attribute_origin($class,$att);
		if ($att_new_origin ne $class) {
		    # Transfer from sub-class to superclass
		    my ($new_table,$old_table,$stmt);
		    if ($new_o->is_multivalue_class_attribute($att_new_origin,$att)) {
			$new_table = "$ {att_new_origin}_2_$att";
			if ($old_o->is_multivalue_class_attribute($class,$att)) {
			    $old_table = "$ {class}_2_$att";
			    if ($new_o->is_instance_type_class_attribute($att_new_origin,$att)) {
				$stmt = qq/INSERT INTO $new_table ($DB_ID_NAME,$att,$ {att}_rank,$ {att}_class) SELECT O.$DB_ID_NAME,O.$att,O.$ {att}_rank,O.$ {att}_class FROM $old_table O/;
			    } else {
				$stmt = qq/INSERT INTO $new_table ($DB_ID_NAME,$att,$ {att}_rank) SELECT O.$DB_ID_NAME,O.$att,$ {att}_rank FROM $old_table O/;
			    }
			} else {
			    $old_table = $class;
			    if ($new_o->is_instance_type_class_attribute($att_new_origin,$att)) {
				$stmt = qq/INSERT INTO $new_table ($DB_ID_NAME,$att,$ {att}_rank,$ {att}_class) SELECT O.$DB_ID_NAME,O.$att,0,O.$ {att}_class FROM $old_table O/;
			    } else {
				$stmt = qq/INSERT INTO $new_table ($DB_ID_NAME,$att,$ {att}_rank) SELECT O.$DB_ID_NAME,O.$att,0 FROM $old_table O/;
			    }
			}
		    } else {
			$new_table = $att_new_origin;
			if ($old_o->is_multivalue_class_attribute($class,$att)) {
			    $old_table = "$ {class}_2_$att";
			    if ($new_o->is_instance_type_class_attribute($att_new_origin,$att)) {
				$stmt = qq/UPDATE $new_table N, $old_table O SET N.$att=O.$att,N.$ {att}_class=O.$ {att}_class WHERE N.$DB_ID_NAME = O.$DB_ID_NAME AND O.$ {att}_rank = 0/;
			    } else {
				$stmt = qq/UPDATE $new_table N, $old_table O SET N.$att=O.$att WHERE N.$DB_ID_NAME = O.$DB_ID_NAME AND O.$ {att}_rank = 0/;
			    }
			} else {
			    $old_table = $class;
			    if ($new_o->is_instance_type_class_attribute($att_new_origin,$att)) {
				$stmt = qq/UPDATE $new_table N, $old_table O SET N.$att=O.$att,N.$ {att}_class = O.$ {att}_class WHERE N.$DB_ID_NAME = O.$DB_ID_NAME/;
			    } else {
				$stmt = qq/UPDATE $new_table N, $old_table O SET N.$att=O.$att WHERE N.$DB_ID_NAME = O.$DB_ID_NAME/;
			    }
			}
		    }
		    $dba->execute($stmt);
		}
	    } else {
		foreach my $att_new_origin ($new_o->descendants_with_own_attribute($class,$att)) {
		    # Transfer from superclass to sub-class 
		    my ($new_table,$old_table,$stmt);
		    if ($new_o->is_multivalue_class_attribute($att_new_origin,$att)) {
			$new_table = "$ {att_new_origin}_2_$att";
			if ($old_o->is_multivalue_class_attribute($class,$att)) {
			    $old_table = "$ {class}_2_$att";
			    if ($new_o->is_instance_type_class_attribute($att_new_origin,$att)) {
				$stmt = qq/INSERT INTO $new_table ($DB_ID_NAME,$att,$ {att}_rank,$ {att}_class) SELECT O.$DB_ID_NAME,O.$att,O.$ {att}_rank,O.$ {att}_class FROM $old_table O, $class C WHERE O.$DB_ID_NAME=C.$DB_ID_NAME/;
			    } else {
				$stmt = qq/INSERT INTO $new_table ($DB_ID_NAME,$att,$ {att}_rank) SELECT O.$DB_ID_NAME,O.$att,$ {att}_rank FROM $old_table O, $class C WHERE O.$DB_ID_NAME=C.$DB_ID_NAME/;
			    }
			} else {
			    $old_table = $class;
			    if ($new_o->is_instance_type_class_attribute($att_new_origin,$att)) {
				$stmt = qq/INSERT INTO $new_table ($DB_ID_NAME,$att,$ {att}_rank,$ {att}_class) SELECT O.$DB_ID_NAME,O.$att,0,O.$ {att}_class FROM $old_table O, $class C WHERE O.$DB_ID_NAME=C.$DB_ID_NAME/;
			    } else {
				$stmt = qq/INSERT INTO $new_table ($DB_ID_NAME,$att,$ {att}_rank) SELECT O.$DB_ID_NAME,O.$att,0 FROM $old_table O, $class C WHERE O.$DB_ID_NAME=C.$DB_ID_NAME/;
			    }
			}
		    } else {
			$new_table = $att_new_origin;
			if ($old_o->is_multivalue_class_attribute($class,$att)) {
			    $old_table = "$ {class}_2_$att";
			    if ($new_o->is_instance_type_class_attribute($att_new_origin,$att)) {
				$stmt = qq/UPDATE $new_table N, $old_table O SET N.$att=O.$att,N.$ {att}_class=O.$ {att}_class WHERE N.$DB_ID_NAME = O.$DB_ID_NAME AND O.$ {att}_rank = 0/;
			    } else {
				$stmt = qq/UPDATE $new_table N, $old_table O SET N.$att=O.$att WHERE N.$DB_ID_NAME = O.$DB_ID_NAME AND O.$ {att}_rank = 0/;
			    }
			} else {
			    $old_table = $class;
			    if ($new_o->is_instance_type_class_attribute($att_new_origin,$att)) {
				$stmt = qq/UPDATE $new_table N, $old_table O SET N.$att=O.$att,N.$ {att}_class = O.$ {att}_class WHERE N.$DB_ID_NAME = O.$DB_ID_NAME/;
			    } else {
				$stmt = qq/UPDATE $new_table N, $old_table O SET N.$att=O.$att WHERE N.$DB_ID_NAME = O.$DB_ID_NAME/;
			    }
			}
		    }
		    $dba->execute($stmt);			
		}
	    }
	}
    }
}

# Delete in old but not in new
foreach my $class (sort {$a cmp $b} $old_o->list_classes) {
    if ($new_o->is_valid_class($class)) {
	foreach my $att ($old_o->list_own_attributes($class)) {
	    unless ($new_o->is_valid_class_attribute($class,$att) &&
		    ($new_o->class_attribute_origin($class,$att) eq $class) &&
		    ($new_o->is_multivalue_class_attribute($class,$att) == $old_o->is_multivalue_class_attribute($class,$att))) {
		if ($old_o->is_multivalue_class_attribute($class,$att)) {
		    print "Deleting table $ {class}_2_$att\n";
		    $dba->execute(qq(DROP TABLE IF EXISTS $ {class}_2_$att ));
		} else {
		    print "Deleting column $class.$att\n";
		    $dba->execute(qq(ALTER TABLE $class DROP $att));
		    if ($old_o->is_instance_type_class_attribute($class,$att)) {
			print "Deleting column $class.$ {att}_class\n";
			$dba->execute(qq(ALTER TABLE $class DROP $ {att}_class));
		    }
		}
	    }
	    
	}
    } else {
	print "Deleting table $class\n";
	$dba->execute(qq(DROP TABLE $class));
	foreach my $att ($old_o->list_own_attributes($class)) {
	    if ($old_o->is_multivalue_class_attribute($class,$att)) {
		print "Deleting table $ {class}_2_$att\n";
		$dba->execute(qq(DROP TABLE $ {class}_2_$att));
	    }
	}
    }
}

# Store the new schema
$dba->store_schema;

$dba->execute('COMMIT');
