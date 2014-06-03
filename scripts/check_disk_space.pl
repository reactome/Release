#!/usr/bin/perl -w
use strict;

# Check disk usage, report if we are getting too full
# sheldon.mckay@gmail.com

use constant THRESHOLD => 90;
use constant EMAIL     => 'sheldon.mckay@gmail.com,Joel.Weiser@oicr.on.ca'; 

chomp(my $host = `hostname`);
my @df = `df`;

my $last_line;
while (my $line = shift @df) {
    next if $line =~ /^Filesystem/;
    chomp $line;

    $line = $last_line . $line if $last_line;

    my ($fs,$percent,$mount) = (split /\s+/, $line)[0,4,5];

    # broken line
    unless ($percent) {
	$last_line = $line;
	next;
    }
    else {
	$last_line = '';
	print "$line\n";
    }

    $percent =~ s/\%$//;
    if ($percent > THRESHOLD) {
	report($fs,$mount,$line)
    } 
}

sub report {
    my ($fs,$mount,$line) = @_;
    my $email = EMAIL;
    my $subject = "TEST IGNORE $fs ($mount) is getting full on $host";
    system qq(echo '$host: $line' | mailx -s '$subject' '$email');
}



__END__
Filesystem           1K-blocks      Used Available Use% Mounted on
/dev/cciss/c0d0p2    141080440 108899168  25014760  82% /
tmpfs                  8234932         0   8234932   0% /lib/init/rw
udev                   8230044       140   8229904   1% /dev
tmpfs                  8234932         0   8234932   0% /dev/shm
/dev/cciss/c0d1p1    141087608  77020776  56899960  58% /opt
