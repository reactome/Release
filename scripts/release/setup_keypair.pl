#!/usr/bin/perl
use strict;
use warnings;

use lib '/usr/local/gkbdev/modules';

use autodie;
use File::HomeDir;
use Getopt::Long;

my ($user, $remote_host, $help);

GetOptions('user=s' => \$user, 'host=s' => \$remote_host, 'help' => \$help);

if ($help) {
    print usage_instructions();
    exit;
}

chomp($user ||= `whoami`);
$remote_host ||= 'reactomeprd1.oicr.on.ca';

my $homedir = go_to_home_dir($user);
generate_key_pair($homedir);
copy_public_key_to_server($user, $homedir, $remote_host);

sub go_to_home_dir {
    my $user = shift;
    my $homedir = File::HomeDir->users_home($user) || File::HomeDir->my_home;
    chdir $homedir;
    
    return $homedir;
}

sub generate_key_pair {
    my $homedir = shift;
    `ssh-keygen -t rsa` unless (-e "$homedir/.ssh/id_rsa.pub");
}

sub copy_public_key_to_server {
    my $user = shift;
    my $homedir = shift;
    my $host = shift;
    
    `ssh $user\@$host 'mkdir -p $homedir/.ssh; chmod 700 $homedir/.ssh'`;
    
    unless (key_is_already_authorized($user, $homedir, $host)) {
	add_key($user, $homedir, $host);
    } else {
	print "Public key for $user is already in place on $host\n";
    }
}

sub key_is_already_authorized {
    my $user = shift;
    my $homedir = shift;
    my $host = shift;
    
    return `ssh $user\@$host 'cat $homedir/.ssh/authorized_keys 2> /dev/null' | grep -f $homedir/.ssh/id_rsa.pub`;
}

sub add_key {
    my $user = shift;
    my $homedir = shift;
    my $host = shift;
    
    `cat $homedir/.ssh/id_rsa.pub | ssh -v $user\@$host 'cat >> $homedir/.ssh/authorized_keys; chmod 600 $homedir/.ssh/authorized_keys'`;
}

sub usage_instructions {
    return <<END;
    perl $0 [options]
    
    Options:
    
    -user, -u	User account for which to create key pair (default is curent user)
    -host	Remote host to receive public key (default is reactomeprd1.oicr.on.ca)
    -help	Display these instructions
END
}