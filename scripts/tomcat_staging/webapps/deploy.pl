#!/usr/bin/perl -w
use Cwd;

my $only = shift;

while (my $war = <*.war>) {
    next if $only && $war !~ /$only/i;
    system "cp $war /usr/local/reactomes/Reactome/production/apache-tomcat/webapps";
}


