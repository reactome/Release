#!/usr/bin/perl -w
use common::sense;
use Cwd;

my $cwd = check_locale();

my $war = shift or die usage();
chomp($war = `basename $war .war`);

if ($war && !-d $war) {
    say STDERR "Directory $war does not exist!";
    say STDERR usage();
    exit;
}

say "Packing $war into $war.war";
chdir $war or die $!;
system "jar cvf ../$war.war *";
chdir $cwd;
system "rm -fr $war";



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
