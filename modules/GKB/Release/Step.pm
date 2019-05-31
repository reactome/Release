package GKB::Release::Step;

=head1 NAME

GKB::Release::Step

=head1 DESCRIPTION

Provides an abstract class for defining what
a step in the release pipeline will be and
what it will do.

=head2 ATTRIBUTES

=item Passwords (read-only)

The passwords the step requires to run (none by default)

=item User Input (read-write)

Input the step requires from the user (generic queries as
opposed to passwords - none by default)

=item Gkb (read-only)

Alias the step will use for the gkb directory (i.e. gkb or gkbdev)

=item Directory (read-write)

Starting working directory for the step (release directory
by default)

=item Name (read-only)

Name of the step (extracted from the starting directory
by default)

=item Host (read-only)

Host server on which the step will run (server housing
this module by default)

=item Mail (read-write)

Mail attributes as a hash reference (i.e. 'to', 'from', 'subject',
'body', etc.)


=head2 Methods

=over 12

=item C<set_user_inputs_and_passwords>

Prompt the user for passwords (unless
obtained by a previously invoked step)
and input specific to the step instance
which invoked this method.

Parameter:
    $self (implicit in method call)

=item C<run>

The main method of the Step (and its children) module.
Defines the order of what is done in running a release
step.

Parameter:
    $self (implicit in method call)

=item C<source_code_passes_tests>

Checks for source code tests in 't' directory
of 'directory' attribute and runs them.

Return:
    Outcome (Boolean - true by default if no 't'
    directory or 'prove' binary not available)

=item C<run_commands> Abstract

Method to run commands -- C<cmd> method
provided to aid implementation in child
classes.

Parameter:
    $self (implicit in method call)

=item C<post_step_tests>

Checks output files for problems in
size and content.

Parameter:
    $self (implicit in method call)

Return:
    Errors in file size/content (Array)

=item C<cmd>

Runs a group of commmand line statements,
logging results in the log file specified
by the configuration module.

Parameters:
    $self (implicit in method call)
    message - displayed to user when invoking method (String -- required)
    cmdref - reference to array of arrays composed of commands
             and arguments.  Arguments can be embedded with or
             separated from the command.

             e.g. [
                ["perl test.pl", "arg1", "arg2", ]
                ["ls -l"]
             ]
    parameters - parameters for the command (Hashref).
                Valid parameters:
                    'ssh' => 'server on which to execute commands'

Return:
    List of results for each command in cmdref.
    Each element is a hashref with the following
    structure (Array of hashref).

    {
        'command' => $cmd,
        'args' => \@args,
        'stdout' => $stdout,
        'stderr' => $stderr,
        'exit_code' => $exit_code
    }

=item C<archive_files>

Send files ending in .dump, .err, .log,
.out to an archive directory derived
from the name of the step and the archive
base directory specified in the configuration
module.

Parameters:
    $self (implicit in method call)
    version - reactome release version (String -- required)

Return:
    Directory where files were archived.  Undefined
    if errors (String)

=item C<mail_now>

Send mail using parameters defined in
the 'mail' attribute.

Parameters:
    $self (implicit in method call)

Return:
    Return value -- 1 for success, 0 for failure

=back

=head1 SEE ALSO

GKB::Release::Config
GKB::Release::Utils
GKB::Release::Steps::*

=head1 AUTHOR
Joel Weiser E<lt>joel.weiser@oicr.on.caE<gt>

Copyright (c) 2015 Ontario Institute for Cancer Research

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

use feature qw/say/;

use autodie;
use Carp;
use Capture::Tiny ':all';
use Email::Address;
use Email::Valid;
use File::Basename;
use File::Spec;
use File::stat;
use List::MoreUtils qw/uniq all none/;
use Mail::Sendmail;
use MIME::Lite;
use Net::OpenSSH;
use Readonly;
use Try::Tiny;

use lib '/usr/local/gkb/modules';

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
    lazy => 1,
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
        my $host = 'localhost';
        chomp $host;
        return $host;
    }
);

has 'mail' => (
    is => 'rw',
    isa => 'HashRef[Str]'
);

sub set_user_input_and_passwords {
    my $self = shift;

    $self->_set_passwords();
    $self->_set_user_input();

    return;
}

sub run {
    my $self = shift;

    chdir $self->directory;
    set_environment($self->host);
    if (!source_code_passes_tests()) {
        return;
    }

    say "Running $self->{name} pre-step tests...";
    my @pre_step_test_errors = $self->pre_step_tests();
    if (@pre_step_test_errors) {
        my $pre_step_test_log = File::Spec->catfile($self->directory, 'pre_step_test_errors.log');
        open my $pre_step_test_fh, '>', $pre_step_test_log;
        print {$pre_step_test_fh} (join "\n", @pre_step_test_errors);
        close $pre_step_test_fh;
        say releaselog("ERRORS from $self->{name} pre-step tests reported -- see $pre_step_test_log");

        return; # Prevent step from being run by stopping the run method prematurely
    } else {
        say releaselog("No errors from $self->{name} pre-step tests");
    }

    $self->run_commands($self->gkb);

    say "Running $self->{name} post-step tests...";
    my @post_step_test_errors = $self->post_step_tests();
    if (@post_step_test_errors) {
        # Let's write the errors to a file. That way, someone OTHER than the mail recipient can see them.
        my $post_step_test_log = File::Spec->catfile($self->directory, 'post_step_test_errors.log');
        open my $post_step_test_fh, '>', $post_step_test_log;
        binmode $post_step_test_fh, ':encoding(UTF-8)';
        print {$post_step_test_fh} join "\n", @post_step_test_errors;
        close $post_step_test_fh;
        say releaselog("ERRORS from $self->{name} post-step tests - see e-mail report or $post_step_test_log");
        $self->mail->{'body'} = "Errors Reported\n\n" . join("\n", @post_step_test_errors);
        $self->mail->{'to'} = 'automation';
    } else {
        say releaselog("No errors from $self->{name} post-step tests");
        if ($self->mail->{'body'}) {
            $self->mail->{'body'} .= "\n\n";
        }
        $self->mail->{'body'} .= "$self->{name} step has completed successfully";
    }
    #$self->mail_now();

    say releaselog('Archiving output, logs, dump files...');
    $self->archive_files($version);

    return 1; # Successful run
}

sub source_code_passes_tests {
    # If a test directory and the prove binary exist, run tests
    # Otherwise, sources tests are ignored because they either don't exist or can't be run
    return ((-d 't') && `which prove`) ? ((system 'prove') == 0) : 1;
}

sub run_commands {
    confess 'The run_commands method must be overridden!';
}

# May be overriden by implementation of specific steps
sub pre_step_tests {
    my $self = shift;

    say releaselog('No pre-step tests to be run for ' . $self->name);
    return;
}

sub post_step_tests {
    my $self = shift;

    return grep { defined } (
        _get_file_size_errors('post_requisite_file_listing'),
         _get_output_errors()
    );
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
                    if (!$ssh->error) {
                        $ssh->system("$cmd @args");
                    }
                } else {
                    system $cmd, @args;
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

sub cmd_successful {
    my $self = shift;
    my $results = shift;

    return all { $_->{'exit_code'} == 0} @{$results};
}

sub archive_files {
    my $self = shift;
    my $version = shift;

    my $step_archive = "$archive/" . $self->name;
    my $step_version_archive = "$step_archive/$version";

    system "mkdir -p $step_version_archive";
    if (-d $step_version_archive) {
        foreach my $sql_dump_file (glob '*.dump') {
            if (-e "$sql_dump_file.gz") {
                system "mv --backup=numbered $sql_dump_file.gz $step_version_archive";
            }
            system "gzip -qf $sql_dump_file 2> /dev/null";
        }

        foreach my $file (map { glob } qw/*.err *.log *.out *.dump.gz/) {
            system "mv --backup=numbered $file $step_version_archive";
        }

        if (!(-e 'archive')) {
            symlink $step_archive, 'archive';
        }
    }

    return $step_version_archive;
}

sub mail_now {
    my $self = shift;
    my $params = $self->mail;

    Readonly my $MAIL_SUCCEEDED => 1;
    Readonly my $MAIL_FAILED => 0;

    my $from = _get_sender_address($params);
    if (scalar Email::Address->parse($from) != 1 || !Email::Valid->address($from)) {
        say releaselog("Unable to send mail - sender address $from is not a single or valid e-mail address");
        return $MAIL_FAILED;
    }

    my $to = $TEST_MODE ? $maillist{'automation'} : _get_recipient_addresses($params);
    if (none { Email::Valid->address($_)} Email::Address->parse($to)) {
        say releaselog('Unable to send mail - the recipient e-mail address(es) is/are not valid');
        return $MAIL_FAILED;
    }

    my $mail = {
        From => $from,
        To => $to,
        Subject => $params->{'subject'}
    };

    my $mail_sent;
    my $mail_error = '';
    try {
        if (!$params->{'attachment'}) {
            $mail->{'Message'} = $params->{'body'};
            # Handled by Mail::Sendmail
            $mail_sent = sendmail(%{$mail});
        } else {
            # Handled by MIME::Lite
            $mail_sent =_add_body_and_attachment(
                $mail,
                $params->{'body'},
                $params->{'attachment'}
            )->send();
        }
    } catch {
        $mail_error = $_;
    } finally {
        # This needs to be handled in the finally block since Mail::Sendmail may
        # fail, but not throw an exception
        if ($Mail::Sendmail::error) {
            $mail_error .= "\nMail::Sendmail error: " . $Mail::Sendmail::error;
        }
    };

    if (!$mail_sent) {
        say releaselog("Problem sending mail: $mail_error");
        return $MAIL_FAILED;
    }

    return $MAIL_SUCCEEDED;
}

sub _get_output_errors {
    my $error_log_dir = shift // '.';

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

sub _get_file_size_errors {
    my $type = shift;

    my @file_listings = _get_listing_of_required_files($type);
    return unless @file_listings;

    my @errors;
    foreach my $file_listing (@file_listings) {
        chomp $file_listing;
        my ($file_name_pattern, $old_file_size, $verify_file_size) = split "\t", $file_listing;
        my $file_name_pattern_with_version = $file_name_pattern;
        $file_name_pattern_with_version =~ s/{version}/$version/g;
        $verify_file_size //= 0;

        my @file_names = _get_files_matching_pattern($file_name_pattern_with_version);
        push @errors, "there are no files matching $file_name_pattern_with_version" unless @file_names;
        foreach my $file_name (@file_names) {
            my $new_file_size = stat($file_name)->size;
            my $file_size_ok = (!$verify_file_size || _file_size_ok($new_file_size, $old_file_size));

            if ($file_size_ok) {
                $file_listing = join("\t", $file_name_pattern, $new_file_size, $verify_file_size);
            } else {
                push @errors, "$file_name is too small: " . abs(_file_size_percent_change($new_file_size, $old_file_size)) .
                                "% decrease (old file size:$old_file_size; new file size:$new_file_size)";
            }
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


sub _get_files_matching_pattern {
    my $pattern = shift;

    my($file_pattern, $base_dir) = fileparse($pattern);

    opendir(my $dir, $base_dir);
    my @matching_files;
    while (my $file = readdir($dir)) {
        chomp $file;
        push @matching_files, "$base_dir$file" if $file =~ qr/^$file_pattern$/;
    }
    closedir($dir);

    return @matching_files;
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
    my $percent_decrease_limit = shift // -10;

    return 0 unless $new_file_size && $old_file_size;
    return _file_size_percent_change($new_file_size, $old_file_size) > $percent_decrease_limit ? 1 : 0;
}

sub _file_size_percent_change {
    my $new_file_size = shift;
    my $old_file_size = shift;
    my $percent_diff = $old_file_size > 0 ? (($new_file_size - $old_file_size) / $old_file_size) * 100 : $new_file_size;
    return sprintf("%.2f", $percent_diff  );
}

sub _get_sender_address {
    my $params = shift;

    my $from = $params->{'from'};

    return $maillist{$from} // $from if $from;


    return $maillist{'default_sender'};
}

sub _get_recipient_addresses {
    my $params = shift;

    my @recipients = map({$maillist{$_}} split ",", $params->{'to'});
    push @recipients, _get_sender_address($params);

    return join(',', uniq @recipients);
}

sub _add_body_and_attachment {
    my $mail = shift;
    my $body = shift;
    my $attachment_path = shift;

    my $filename = fileparse($attachment_path);
    $mail->{'Type'} = 'multipart/mixed';

    # Construct e-mail message
    my $msg = MIME::Lite->new(%{$mail});

    $msg->attach(
        Type => "TEXT",
        Data => $body
    );

    $msg->attach(
        Type => "text/plain",
        Path => $attachment_path,
        Filename => $filename
    ) if (-e $attachment_path);

    return $msg;
}

sub _set_passwords {
    my $self = shift;

    foreach my $passtype (@{$self->passwords}) {
        my $passref = $passwords{$passtype};
        if (!$$passref) {
            my $attempts = 0;
            my $MAX_ATTEMPTS = 3;
            my $retry;
            do {
                $attempts += 1;
                $retry = 0;
                $$passref = prompt("Enter your " . $passtype . " password: ", 1);
                my $confirmed_password = prompt("Confirm your $passtype password: ", 1);
                if ($confirmed_password ne $$passref) {
                    $retry = 1;
                    my $message = "$passtype passwords do not match";
                    if ($attempts < $MAX_ATTEMPTS) {
                        print("$message -- please try again\n");
                    } else {
                        die("$message -- aborting\n");
                    }
                }
            } while ($retry && $attempts < $MAX_ATTEMPTS);
        }
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
    my $passwords = shift // {%passwords};

    foreach my $passtype (keys %{$passwords}) {
        my $pass = ${$passwords{$passtype}};
        next unless $pass;

        my $hidden = '*' x (length $pass);
        $nopasscmd =~ s/$pass(\s+)/$hidden$1/;
    }

    return $nopasscmd;
}

1;
