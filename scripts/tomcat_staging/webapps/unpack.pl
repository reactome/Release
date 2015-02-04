#!/usr/bin/perl -w
use Cwd;

my $cwd = getcwd;

my $only = shift;

while (my $war = <*.war>) {
    next if $only && $war !~ /$only/i;
    chdir($cwd);
    print $war, "\n";
    (my $dir = $war) =~ s/.war$//;
    system mkdir $dir unless -d $dir;
    chdir($dir);
    system "jar xvf ../$war";
}


