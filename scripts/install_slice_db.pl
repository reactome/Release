#!/usr/local/bin/perl
use strict;
use warnings;

use autodie qw/:all/;
use Cwd;
use English;
use File::Basename;
use Readonly;

use lib '/opt/GKB/modules';
use GKB::Config;

Readonly my $SUDO_USER => 0;
Readonly my $STAGING => "/usr/local/reactomes/Reactome/production/staging";
Readonly my $RESTFUL_API => "$STAGING/webapps/ReactomeRESTfulAPI";
Readonly my $APPLICATION_CONTEXT => "$RESTFUL_API/WEB-INF/applicationContext.xml";
Readonly my $CONFIG_SECRETS => "/opt/GKB/modules/GKB/Secrets.pm";

if ($EFFECTIVE_USER_ID != $SUDO_USER) {
    print STDERR "Please run this script as sudo or root\n";
    exit 1;
}

my ($db_name, $use_cache) = @ARGV;
if (!$db_name) {
    print STDERR "Usage $0 slice_db_name [use_cache=false - default is true if not specified, but only type false after the slice name for no cache]\n\n";
    print STDERR "For example:\n $0 test_slice_68 use_cache=false\n";
    exit 1;
}

if (!db_exists($db_name)) {
    print STDERR "The database provided $db_name does not exist on the local mysql server\n";
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
system(qq(perl -i -p0e "s/<property name=\\"useCache\\".*?<value>\\K\\w+/$use_cache/ms" $APPLICATION_CONTEXT));

if (!(`grep -Pzo "(?s)property name=\\"useCache\\".*?<value>$use_cache" $APPLICATION_CONTEXT`)) {
    print STDERR "Setting cache to $use_cache for $APPLICATION_CONTEXT failed\n";
    exit 1;
}

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

sub db_exists {
    my $db_name = shift;
    my $user = $GK_DB_USER;
    my $pass = $GK_DB_PASS;

    no autodie qw/system/;
    my $return_value = system("mysql -u $user -p$pass -e \"use $db_name\" > /dev/null 2>&1");
    use autodie qw/system/;

    return $return_value == 0; # 0 return value indicates success (i.e. db exists)
}

