#!/usr/local/bin/perl  -w

BEGIN {
    my ($path) = $0 =~ /^(\S+)$/;
    my @a = split('/',$path);
    pop @a;
    if (@a && !$a[0]) {
        $#a = $#a - 2;
    } else {
        push @a, ('..');
    }
    push @a, 'modules';
    my $libpath = join('/', @a);
    unshift (@INC, $libpath);
}

use GKB::DBAdaptor;
use Data::Dumper;
use Getopt::Long;
use common::sense;
use strict;

@ARGV || die "Usage: $0 -user db_user -host db_host -pass db_pass -port db_port -db db_name -class class -fix\n";

our($opt_user,$opt_host,$opt_pass,$opt_port,$opt_db,$opt_debug,$opt_class,$opt_fix);

&GetOptions("user:s",
	    "host:s",
	    "pass:s",
	    "port:i",
	    "db=s",
	    "debug",
	    "class=s",
	    "fix",
	    );

$opt_db || die "Need database name (-db).\n";
$opt_class || die "Need class name (-class).\n";

my $dba = GKB::DBAdaptor->new
    (
     -dbname => $opt_db,
     -user   => $opt_user,
     -host   => $opt_host,
     -pass   => $opt_pass,
     -port   => $opt_port,
     -DEBUG => $opt_debug
     );

my $ar = $dba->fetch_instance(-CLASS => $opt_class);

my $c = 0;
my $total = scalar(@{$ar});

while (my $i = shift @{$ar}) {
    $i->inflate;
    my $needs_update;
    foreach my $att (grep {$i->is_instance_type_attribute($_)} $i->list_set_attributes) {
	next unless $att =~ /species/i;
	my (@valid,@invalid);
	map {$i->is_attribute_allowed_class_instance($att,$_) ? push @valid, $_ : push @invalid, $_} @{$i->attribute_value($att)};
	if (@invalid) {
	    @valid ? $i->attribute_value($att,@valid) : $i->attribute_value($att,undef);
	    foreach (@valid) {
                print join("\t",$i->extended_displayName,$att,$_->extended_displayName), "\n";
            }
	    $needs_update = 1;
	    foreach (@invalid) {
		print join("\t",$i->extended_displayName,$att,$_->extended_displayName), "\n";
	    }
	    $opt_fix && $dba->update_attribute($i,$att);
	}
    }
    $i->deflate;
    $dba->instance_cache->empty;
    printf STDERR "Done %6d out of %6d", ++$c, $total;
    print STDERR "\b" x 25;   
}
