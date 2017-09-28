#!/usr/bin/perl 
use common::sense;
use HTTP::Tiny;
use JSON;

use constant HOST => 'http://reactome.org/';

die "Sorry, root permission required.\n" unless $> == 0;

my $url = HOST . "ReactomeRESTfulAPI/RESTfulWS/frontPageItems/homo+sapiens";

my $response = HTTP::Tiny->new->get($url);

my $stamp = timestamp();

if ($response->{success}) {

    my $content = $response->{content};
    my $json = decode_json($content);
    my $OK = $json->[0]->{dbId};

    #say $content;

    my $stamp = timestamp();

    if ($OK) {
	say "OK $stamp";
    }
    else {
	say STDERR "Error, restarting $stamp";
	say $content;
	#system "/etc/init.d/tomcat7 restart"; # NO NO NO!!!
	system "touch /usr/local/reactomes/Reactome/production/apache-tomcat/webapps/ReactomeRESTfulAPI.war";
    }
}
else {
    say STDERR "Error, restarting $stamp";
    system "/etc/init.d/tomcat7 restart";
}

sub timestamp {
    my ($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdst)=localtime(time);
    return sprintf ( "%04d-%02d-%02d:%02d:%02d:%02d",
                                   $year+1900,$mon+1,$mday,$hour,$min,$sec);
}
