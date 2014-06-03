#!/usr/bin/perl -w
use strict;
use Getopt::Long;

use constant MYSQL   => '/var/lib/mysql';
use constant ARCHIVE => '/nfs/reactome/reactome/archive/mysql';
use constant AGE     => 90; #days
die "Sorry, root permission required.\n" unless $> == 0;

my ($db,$age,$help,$force,$remove);
GetOptions (
    "age:i"  => \$age,
    "db:s"   => \$db,
    "help"   => \$help,
    "force"  => \$force,
    "remove" => \$remove);
die usage() if $help || !($age || $db);


my $no_age = 1 unless $age;
$age ||= AGE;
my $archive = ARCHIVE;

chdir MYSQL or die $!;

while (<*>) {
    -d $_ or next;
    next if $db && $_ ne $db;
    warn "$_ is already a symlink, skipping.\n" and next if -l $_;
    my $mod = -M $_;
    $mod = int($mod + 0.5);
    if ($db && $mod < $age && $no_age) {
	warn "$_ was touched only $mod days ago!\nUse the -a option set to a younger age if you really want to archive it.\n";
	next;
    }
    next unless $db || $mod > $age;
    archive($_);
    last if $db;
}

sub archive {
    my $db = shift;
    warn "I have been asked to archive $db\n";
    my $archive_path = $archive ."/$db";
    if (-d $archive_path) {
	unless ( $force ) {
	    warn "You asked me to force an overwrite in the archive, so here goes...";
	    #system "rm -fr $archive_path";
	    #system "mv $db $archive";
	}
	else {
	    warn "The archive exists and you did not tell me to force overwrite, so I won't...";
	    if ($remove) {
		warn "You asked me to temove the local copy if it is already archived, so here goes...";
		#system rm -fr $db;
	    }
	    else {
		warn "You did not ask me to delete the local copy of $db, so I will not do it...";
	    }
	}
    }
    else {
	warn "OK, archiving\n";
	#system "mv $db $archive";
    }
    

    unless (-d $db || -l $db) {
	warn "This is where I would be symlinking...";
	#system "ln -s $archive_path"; 
    }

    
}

sub usage {
'Usage: ./archive_old_database.pl [options]
 Options:
   a|age     archive databases whose files are older than this
   d|db      specify the name of the database to be archived
   f|force   If a database by that name exists in the archive, force
             an overwrite
   r|remove  remove the database from local storage if it is already
             archived
   h|help    this message
'
}

