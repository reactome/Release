#!/usr/bin/perl -w
use Cwd;

my $cwd = getcwd;

my $only = shift;

while (my $war = <*>) {
    next unless -d $war;
    next if $war eq 'bak';
    next if $only && $war !~ /$only/i;
    chdir $cwd;
    print $war, "\n";
    my $dir = $war;
    $war = "$dir.war";

    chdir $dir  or die $!;
    system "jar cvf ../$war *";
    chdir $cwd;
    system "rm -fr $dir";
}


