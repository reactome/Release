package GKB::Release::Utils;

use strict;
use warnings;

use v5.10;

use GKB::Release::Config;

use Term::ReadKey;
use autodie;

use base 'Exporter';


# Created by: Joel Weiser (joel.weiser@oicr.on.ca for questions/comments)
# Last Modified: April 27th, 2012
# Purpose: A module to automate release reactome data -- each step is a subroutine.

#Exports all subroutines
our @EXPORT = qw/cvs set_environment prompt releaselog replace_gkb_alias_in_dir/;

sub cvs {
	my $usr = shift || $user;
	my $pss = shift || getpass('sudo');
	my $hst = shift;
	my $cvsroot = shift || $cvs;
	$ENV{CVSROOT} = ":pserver:$usr:$pss\@$hst:$cvsroot";
	`cvs login`;
}
  
# C shell environment and GKB symbolic link for user
sub set_environment {
    my $host = shift;
    my $gkb = $hosts{$host};

    $ENV{'HOME'} = "/home/$user";
    $ENV{'PATH'} = "/usr/local/bin:$ENV{'PATH'}";
    $ENV{'PERL5LIB'} .= ":/usr/local/$gkb/modules:/usr/local/bioperl-1.0";
    umask 0002;
    `mv ~/GKB ~/GKB_$user` if (-e "~/GKB");
    #`ln -sf /usr/local/$gkb ~/GKB` if (-e "/home/$user"); 
}

# Ask user for information
sub prompt {
    my $question = shift;
    print $question;
    my $pass = shift;
    ReadMode 'noecho' if $pass; # Don't show keystrokes if it is a password
    my $return = ReadLine 0;
    chomp $return;
    
    ReadMode 'normal';
    print "\n" if $pass;
    return $return;
}

sub replace_gkb_alias_in_dir {
	my $dir = shift;
	my $gkb = shift;
	
	$dir =~ s/gkb.*?\//$gkb\//;
	
	return $dir;
}

sub releaselog {
	my @lines = @_;
	
	mkdir $logdir unless (-e $logdir);
	  
	open my $log, ">>", "$logfile";  
	say $log @lines; 
	close $log;
	
	return @lines;
}

1;
