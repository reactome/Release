package GKB::Release::Utils;

use strict;
use warnings;

use v5.10;

use GKB::Release::Config;

use Capture::Tiny ':all';
use Net::OpenSSH;
use Term::ReadKey;
use autodie;

use base 'Exporter';


# Created by: Joel Weiser (joel.weiser@oicr.on.ca for questions/comments)
# Last Modified: April 27th, 2012
# Purpose: A module to automate release reactome data -- each step is a subroutine.

#Exports all subroutines
our @EXPORT = qw/cvs set_environment mailnow prompt getpass cmd releaselog replace_gkb_alias_in_dir/;

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

# Mail sent when some steps completed
sub mailnow {
    my %params = %{$_[0]};
    
	my $from = $params{'from'} || $maillist{'automation'};
    
    my @recipients = split ",", $params{'to'};

    my $to = $from;
    foreach my $recipient (@recipients) {
        $to .= "," . $maillist{$recipient}; 
    }
    
    my $subject = $params{'subject'};
    
    my %mail = (
    	From => $from,
    	To => $to,
    	Subject => $subject
    );
    
    
    my $body = $params{'body'} . "\nThe $subject section has finished";
    
    my $attachment_path = $params{'attachment'};
    
    if ($attachment_path) {
    	use MIME::Lite;
    	
        my ($filename) = $attachment_path =~ /\\(.*)$/;
        $mail{'Type'} = "multipart/mixed";
	
	    # Construct e-mail message
    	my $msg = MIME::Lite->new(%mail);
   
   		$msg->attach(
        	Type => "TEXT",
        	Data => $body
    	);

    	$msg->attach(
       		Type => "text/plain",
        	Path => $attachment_path,
        	Filename => $filename 
    	);
   
    	$msg->send() or die;
	} else {
		use Mail::Sendmail;
		
		$mail{'Message'} = $body;
		sendmail(%mail) or die;
	}	
    print "Mail has been sent\n";
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

sub getpass {
    my $type = shift;
    
    my $passref = $passwords{$type};
    $$passref = prompt("Enter your " . $type . " password: ", 1) unless $$passref;
    return $$passref;
}

sub replace_gkb_alias_in_dir {
	my $dir = shift;
	my $gkb = shift;
	
	$dir =~ s/gkb.*?\//$gkb\//;
	
	return $dir;
}

sub cmd {
	my $message = lcfirst shift; # Message to display to user
	my $cmdref = shift; # Reference to list of commands to be run	 
	my $parameters = shift; # Parameters for running the commands (e.g. specifying a remote server)

	# Display message
	say releaselog("NOW $message...\n"); 
	
	
	my @cmd_results;

	foreach my $cmdarg (@{$cmdref}) { 
		my ($cmd, @args) = @{$cmdarg};
		
		my $nopasscmd = _hide_passwords($cmd);
	
		print releaselog("Executing $nopasscmd -- " . `date`);
		
		# Execute command
		my ($stdout, $stderr, $exit_code);
		
		capture_stdout {
			($stdout, $stderr, $exit_code) = tee {
				if ($parameters && $parameters->{'ssh'}) {
					my $ssh = Net::OpenSSH->new($parameters->{'ssh'});
					unless($ssh->error) {
						$ssh->system("$cmd @args");
					}
				} else {
					system($cmd, @args);
				}			
			};
		};
		
		# Log errors (if any)
		releaselog($stderr);
		
		# Check for errors and log success/failure
		if ($exit_code == 0) {
			say releaselog("Finished $nopasscmd -- " . `date`);
		} else {
			say releaselog("ERROR: Problem $message\n$nopasscmd failed\nLogged in $logfile\n");	
		}
		
		# Store command and its results
		push @cmd_results, {
			'command' => $cmd,
			'args' => \@args,
			'stdout' => $stdout,
			'stderr' => $stderr,
			'exit_code' => $exit_code
		};
	}
	
	say releaselog("FINISHED $message\n\n");
	
	return @cmd_results;
}

sub releaselog {
	my @lines = @_;
	
	mkdir $logdir unless (-e $logdir);
	  
	open my $log, ">>", "$logfile";  
	say $log @lines; 
	close $log;
	
	return @lines;
}

sub _hide_passwords {
	my $nopasscmd = shift;
	
	foreach my $passtype (keys %passwords) {
		my $pass = ${$passwords{$passtype}};
		next unless $pass;
		
		my $hidden = '*' x (length $pass);
		$nopasscmd =~ s/$pass(\s+)/$hidden$1/;
	}
	
	return $nopasscmd;
}

1;
