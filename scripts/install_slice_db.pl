#!/usr/local/bin/perl
use strict;
use warnings;

use autodie qw/:all/;
use Cwd;
use English;
use File::Basename;
use Readonly;

Readonly my $SUDO_USER => 0;
Readonly my $STAGING => "/usr/local/reactomes/Reactome/production/staging";
Readonly my $RESTFUL_API => "$STAGING/webapps/ReactomeRESTfulAPI";
Readonly my $APPLICATION_CONTEXT => "$RESTFUL_API/WEB-INF/applicationContext_Production.xml";
Readonly my $CONFIG_SECRETS => "/opt/GKB/modules/GKB/Secrets.pm";

if ($EFFECTIVE_USER_ID != $SUDO_USER) {
    print STDERR "Please run this script as sudo or root\n";
    exit 1;
}

my ($db_name, $use_cache) = @ARGV;
if (!$db_name) {
    print STDERR "Usage $0 slice_db_name [use_cache=true/false]\n";
    exit 1;
}
$use_cache && $use_cache =~ /^use_cache=(true|false)/;
$use_cache = $1 || 'true'; 

if (-e $RESTFUL_API) {
    print "Backing up $RESTFUL_API directory\n";
    system("mv -f $RESTFUL_API $RESTFUL_API.bak");
}

my $cwd = getcwd;
chdir "$STAGING/webapps";
system("perl unpack.pl " . basename("$RESTFUL_API.war"));

print "Changing database being used to $db_name\n";
system(qq(perl -i -pe "s/<constructor-arg index=\\"1\\" value=\\K\\".*?\\"/\\"${db_name}\\"/" $APPLICATION_CONTEXT));
if (!(`grep $db_name $APPLICATION_CONTEXT`)) {
    print STDERR "Database renaming for $APPLICATION_CONTEXT failed\n";
    exit 1;
}

print "Changing cache use to $use_cache\n";
system(qq(perl -i -pe "s/<property name=\\"useCache\\" value=\\K\\"true\\"/\\"$use_cache\\"/" $APPLICATION_CONTEXT));

if (!(`grep "property name=\\"useCache\\" value=\\"$use_cache\\"" $APPLICATION_CONTEXT`)) {
    print STDERR "Setting cache to $use_cache for $APPLICATION_CONTEXT failed\n";
    exit 1;
}

print "Moving " . basename($APPLICATION_CONTEXT) . " to applicationContext.xml";
system("cp -f $APPLICATION_CONTEXT $RESTFUL_API/WEB-INF/applicationContext.xml");

if (-e "$RESTFUL_API.war") {
    print "Backing up $RESTFUL_API.war\n";
    system("mv -f $RESTFUL_API.war $RESTFUL_API.war.bak");
}

print "Packing $RESTFUL_API directory into war file\n";
system("perl pack.pl " . basename("$RESTFUL_API.war"));

print "Deploying $RESTFUL_API.war file\n";
system("perl deploy.pl " . basename("$RESTFUL_API.war"));

system(qq(perl -i -pe "s/GK_DB_NAME\\s*=\\s*\\K'\\S+';/'$db_name';/" $CONFIG_SECRETS));
if (!(`grep $db_name $CONFIG_SECRETS`)) {
    print STDERR "Database renaming for $CONFIG_SECRETS failed\n";
    exit 1;
}

