package GKB::Release::Step;

use feature qw/say/;

use autodie;
use Capture::Tiny ':all';
use File::stat;
use Net::OpenSSH;

use GKB::Release::Utils;
use GKB::Release::Config;

use Moose;

has 'passwords' => (
	is => "ro",
	isa => 'ArrayRef[Str]',
	default => sub { []; }
);

has 'user_input' => (
	is => 'rw',
	isa => 'HashRef[HashRef]',
	default => sub { {} ; }
);

has 'gkb' => (
	is => 'ro',
	isa => 'Str',
	required => 1
);

has 'directory' => (
	is => 'rw',
	isa => 'Str',
	default => $release
);

has 'name' => (
	is => 'ro',
	isa => 'Str',
	lazy => 1,
	default => sub {
		my $self = shift;
		my ($name) = $self->directory =~ /.*\/(.*)/;
		return $name;
	}
);

has 'host' => (
	is => 'ro',
	isa => 'Str',
	lazy => 1,
	default => sub {
		my $host = `hostname -f`;
		chomp $host;
		return $host;
	}
);

has 'mail' => (
	is => 'rw',
	isa => 'HashRef[Str]'
);

sub run {
	my $self = shift;
	
	chdir $self->directory;
	set_environment($self->host);
	return unless source_code_passes_tests();
	$self->run_commands($self->gkb);
	my @file_size_errors = get_file_size_errors('post_requisite_file_listing');
	my $archive_dir = archive_files($self->name, $version);
	my @output_errors = get_output_errors($archive_dir);
	my @errors = (@file_size_errors, @output_errors);
	if (@errors) {
		$self->mail->{'body'} .= "Errors Reported\n\n";
		$self->mail->{'body'} .= join("\n", @errors);
		mail_now($self->mail);
	}
}

sub source_code_passes_tests {
	return 1 unless (-e "t") && `which prove`;
	return (system("prove") == 0);
}

sub cmd {
	my $self = shift;
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

sub archive_files {
	my $step = shift;
	my $version = shift;
	
	my $step_archive = "$archive/$step";
	my $step_version_archive = "$step_archive/$version";
	
	`mkdir -p $step_version_archive`;
	`mv --backup=numbered $_ $step_version_archive 2>/dev/null` foreach qw/*.dump *.err *.log *.out/;
	symlink $step_archive, 'archive' unless (-e 'archive');
	
	return $step_version_archive;
}

sub get_output_errors {
	my $error_log_dir = shift;
	
	my @all_errors;
	opendir (my $dir, $error_log_dir);
	while (my $file = readdir $dir) {
		next unless ($file =~ /\.err$/);
		push @all_errors, _get_output_errors_from_file("$error_log_dir/$file");
	}
	closedir $dir;
	
	return @all_errors;
}

sub _get_output_errors_from_file {
	my $file_path = shift;
	
	my @errors;
	open(my $file, "<", $file_path);
	while (my $line = <$file>) {
		next if $line =~ /^WARN/;
		push @errors, $line;
	}
	close $file;
	
	return @errors;
}

sub get_file_size_errors {
	my $type = shift;
	
	my @file_listings = _get_listing_of_required_files($type);
	return unless @file_listings;
	
	my @errors;
	foreach my $file_listing (@file_listings) {
		chomp $file_listing;
		my ($file_name, $old_file_size, $verify_file_size) = split "\t", $file_listing;
		my $file_exists = (-e $file_name);
		my $new_file_size = stat($file_name)->size if $file_exists;
		my $file_size_ok = (!$verify_file_size || _file_size_ok($new_file_size, $old_file_size));
		
		if ($file_exists) {
			if ($file_size_ok) {
				$file_listing = join("\t", $file_name, $new_file_size, $verify_file_size);
			} else {
				push @errors, "$file_name is too small";
			}
		} else {
			push @errors, "$file_name doesn't exist";
		}
	}
	
	_update_listing_of_required_files($type, @file_listings);
	
	return @errors;
}

sub _get_listing_of_required_files {
	my $type = shift;
	
	return unless (-e $type);
	
	open(my $file_listing_fh, '<', $type);
	my @file_listings = <$file_listing_fh>;
	close $file_listing_fh;
	
	return @file_listings;
}

sub _update_listing_of_required_files {
	my $file = shift;
	my @file_listings = @_;
	
	if (system("mv --backup=numbered $file $file.bak") == 0) {
		open(my $fh, '>', $file);
		print $fh $_ . "\n" foreach @file_listings;
		close $fh;
	}
}

sub _file_size_ok {
	my $new_file_size = shift;
	my $old_file_size = shift;
	my $percent_change_tolerance = shift // 10;
	
	return 0 unless $new_file_size && $old_file_size;
	
	my $percent_reduction = (($old_file_size - $new_file_size) / $old_file_size) * 100;
	
	return $percent_reduction < $percent_change_tolerance ? 1 : 0;
}

# Mail sent when some steps completed
sub mail_now {
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

sub set_user_input_and_passwords {
	my $self = shift;
	
	$self->_set_passwords();
	$self->_set_user_input();
}

sub _set_passwords {
	my $self = shift;
	
	foreach my $passtype (@{$self->passwords}) {
		my $passref = $passwords{$passtype};
		$$passref = prompt("Enter your " . $passtype . " password: ", 1) unless $$passref;
	}
}

sub _set_user_input {
	my $self = shift;
	
	foreach my $input_name (keys %{$self->user_input}) {
		my $query = $self->user_input->{$input_name}->{'query'};
		my $hide_keystrokes = $self->user_input->{$input_name}->{'hide_keystrokes'};
		
		$self->user_input->{$input_name}->{'response'} = prompt($query, $hide_keystrokes);
	}
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

sub run_commands {
	die "The run_commands method must be overridden!";
}

1;
