#!/usr/bin/perl -w
use Cwd;
use common::sense;

# Backs up live web aplications.  Consider doing this before deploying new ones.

my $cwd = check_locale();

my $war  = shift;
chomp(my $stamp = `date "+%m-%d-%y"`);

if ($war && !(-e $war && ! -z $war)) {
    say STDERR "$war does not exist in $cwd!";
    say STDERR usage();
}

while (my $webapp = </usr/local/reactomes/Reactome/production/apache-tomcat/webapps/*.war>) {
    next if $war && $webapp !~ /$war/i;
    say $webapp;
    system "mkdir -p bak/$stamp";
    system "cp $webapp bak/$stamp";
}


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
