#!/usr/bin/perl -w
use Cwd;
use common::sense;

my $only  = shift;
chomp(my $stamp = `date`);
$stamp =~ s/\s+|:/-/g;

while (my $war = </usr/local/reactomes/Reactome/production/apache-tomcat/webapps/*.war>) {
    next if $only && $war !~ /$only/i;
    say $war;
    system "mkdir -p bak/$stamp";
    system "cp $war bak/$stamp";
}


