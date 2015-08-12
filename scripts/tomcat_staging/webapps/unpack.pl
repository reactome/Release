#!/usr/bin/perl -w
use common::sense;
use Cwd;

my $cwd = check_locale();

my $war = shift or die usage();

if ($war && !(-e $war && ! -z $war)) {
    say STDERR "$war does not exist!";
    say STDERR usage();
}

say "Unpacking $war";
(my $dir = $war) =~ s/.war$//;
system mkdir $dir unless -d $dir;
chdir($dir);
system "jar xvf ../$war";


sub usage {
    "Usage: $0 my_webapp.war"
}


sub check_locale {
    my $cwd = getcwd;
    my $locale = '/usr/local/reactomes/Reactome/production/staging/webapps';
    unless ($cwd eq $locale) {
	#say STDERR "Hey!  Your are supposed to run me from $locale!";
	#say STDERR "Changing directories....";
	chdir($locale);
	$cwd = getcwd;
    }
    return $cwd;
}
