#!/usr/bin/perl -w
use common::sense;
use English;
use Cwd;


unless ($EFFECTIVE_USER_ID == 0) {
    say STDERR "Please run this script as sudo or root. ", usage();
    exit;
}

my $cwd = check_locale();

my $war = shift or die usage();
my $target = shift || $war;

chomp($target = `basename $target`);
my $path = '/usr/local/reactomes/Reactome/production/apache-tomcat/webapps';
my $dest = "$path/$target";

if ($war && !(-e $war && ! -z $war)) {
    say STDERR "$war does not exist in $cwd!";
    say STDERR usage();
}

say "Backing up $target...";

say "Deploying $war to $dest...";
system "sudo chown -R tomcat7:gkb $path"; 
my $retval = system "cp $war $dest";
if ($retval) {
    $retval = system "sudo cp $war $dest";
}

if ($retval || !-e "$dest") {
    die "Deploying $war failed for some reason";
}

say "Done.";

sub usage {
    "\nUsage: sudo $0 my_webapp.war [deployed_name.war]"
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
