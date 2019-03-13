#!/usr/local/bin/perl  -w

use lib "/usr/local/gkb/modules";
use lib "$ENV{HOME}/bioperl-1.0";
use lib "$ENV{HOME}/GKB/modules";

use GKB::Config;
use GKB::DBAdaptor;
use GKB::Utils_esther;
use Data::Dumper;
use Getopt::Long;
use strict;

@ARGV || die "Usage: $0 -user db_user -host db_host -pass db_pass -port db_port -db db_name -class class";

our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db,$opt_debug,$opt_class);

&GetOptions("user:s",
	    "host:s",
	    "pass:s",
	    "port:i",
	    "db=s",
	    "debug",
	    "class=s",
	    );

$opt_db || die "Need database name (-db).\n";
$opt_class || die "Need class name (-class).\n";

my $dba = GKB::DBAdaptor->new
    (
     -dbname => $opt_db,
     -user   => $opt_user || $GKB::Config::GK_DB_USER,
     -host   => $opt_host || $GKB::Config::GK_DB_HOST,
     -pass   => $opt_pass || $GKB::Config::GK_DB_PASS,
     -port   => $opt_port || $GKB::Config::GK_DB_PORT,
     -driver => 'mysql',
     -DEBUG => $opt_debug
     );
    
## Prepare date and author for instanceEdit:
my $surname = 'Weiser';
my $initial = 'JD';
my $date    = `date \+\%F`;
chomp $date;

my $instance_edit = GKB::Utils_esther::create_instance_edit( $dba, $surname, $initial, $date );

my $ar = $dba->fetch_instance(-CLASS => $opt_class);

print "New Display Name\tOld Display Name\n";
foreach my $i (@{$ar}) {
    $i->inflate();

    my $old_display_name = $i->attribute_value("_displayName")->[0];
    $i->namedInstance; # display name updated here
    next if $old_display_name eq $i->displayName;
    
    print $i->displayName, "\t", $old_display_name, "\n";
    
    $i->Modified( @{ $i->Modified } );
    $i->add_attribute_value( 'modified', $instance_edit );
    $dba->update($i);
}

