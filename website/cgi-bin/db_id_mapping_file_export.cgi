#!/usr/bin/perl -T
use strict;
use warnings;

use autodie qw/:all/;
no autodie qw/flock/;
use Carp;
use CGI;
use CGI::Carp qw/fatalsToBrowser/;
use Email::Valid;
use feature qw/state/;
use Fcntl qw/:flock/;
use File::Basename;
use File::Path qw/make_path remove_tree/;
use File::Spec::Functions 'catfile';
use File::stat;
use IO::Handle;
use LWP::Simple;
use MIME::Lite;
#use sigtrap qw/handler error_clean_up untrapped normal-signals/;
use Readonly;
use Try::Tiny;

run() unless caller();

sub run {
    my $cgi = shift // CGI->new;
    my $database = get_database(get_data_host($cgi));
    
    my $lock_file = catfile(get_output_dir_path($database), "$database.lock");
    open(our $source_fh, '>', $lock_file);
    my $another_process_running = !(flock($source_fh, LOCK_EX|LOCK_NB));
    
    STDOUT->autoflush;
    $ENV{PATH} = '/bin:/bin/sbin:/usr/bin:/usr/sbin:/usr/local/bin:/usr/local/sbin';
    
    if ($cgi->request_method eq 'GET') {
        die "This CGI script cannot be accessed directly\n";
    }
    
    if ($database eq 'gk_central') {
        send_file_for_gk_central($cgi, $another_process_running, $source_fh);
    } else {
        send_file_for_gk_current($cgi, $another_process_running, $source_fh);
    }
    close($source_fh);
}

sub send_file_for_gk_central {
    my $cgi = shift || confess "No CGI object provided\n";
    my $another_process_running = shift // confess "Unknown if another process is running\n";
    my $source_fh = shift || confess "No source file handle provided\n";
    
    my $database = get_database(get_data_host($cgi));
    if ($another_process_running || (-e get_path_to_mapping_file($database) && mapping_file_is_recent($database))) {
        retrieve_mapping_file_and_email($cgi, $source_fh);
    } else {
        my $pid = fork() // die "Could not fork process\n";
        if ($pid) {
            display_submitted_page($cgi, { 'file_must_be_generated' => 1 });
        } else {
            create_mapping_file_and_email($cgi);
        }
    }
}
    
sub send_file_for_gk_current {
    my $cgi = shift || confess "No CGI object provided\n";
    my $another_process_running = shift // confess "Unknown if another process is running\n";
    my $source_fh = shift || confess "No source file handle provided\n";
    
    my $database = get_database(get_data_host($cgi));
    if ($another_process_running || (-e get_path_to_mapping_file($database))) {
        #print "Retrieving mapping file for gk_current\n";
        retrieve_mapping_file_and_email($cgi, $source_fh);
    } else {
        my $pid = fork() // die "Could not fork process\n";
        if ($pid) {
            display_submitted_page($cgi, { 'file_must_be_generated' => 1 });
        } else {
            create_mapping_file_and_email($cgi);
        }
    }    
}

sub create_mapping_file_and_email {
    my $cgi = shift || confess "No CGI object provided\n";

    try {
        close STDOUT;
        close STDERR;
        generate_mapping_file(get_data_host($cgi));
    } catch {
        remove_mapping_file(get_database(get_data_host($cgi)));
        confess "Error generating mapping file: $_";
    };

    my $database = get_database(get_data_host($cgi));
    try {
        send_email_with_mapping_file({
            'sender' => get_sender_address(),
            'recipient' => get_recipient_address($cgi),
            'file_path' => get_path_to_mapping_file($database),
            'attachment_name' => get_name_of_mapping_file($database)
        });            
    } catch {
        confess "Unable to send e-mail to " . get_recipient_address($cgi) . "\n";
    };
}

sub retrieve_mapping_file_and_email {
    my $cgi = shift || confess "No CGI object provided\n";
    my $source_fh = shift || confess "No source file handle provided\n";
    
    my $database = get_database(get_data_host($cgi));
    if (!(-e get_path_to_mapping_file($database))) {
        #print "Mapping file does not exist\n";
        flock($source_fh, LOCK_SH) || die "Unable to get shared lock on $0: $!\n";
    }    
    #try {
        # If this fails (presumably because the other cgi process failed to generate the file), re-generate file and try re-opening
        open(my $mapping_file_fh, '<', get_path_to_mapping_file($database)); 
    #} catch {
        #close($source_fh);
        #run($cgi);
        #exit;
    #};
    my $mapping_file_being_generated = !(flock($mapping_file_fh, LOCK_EX|LOCK_NB));
    #print "Mapping file is being generated: $mapping_file_being_generated\n";
    if (!$mapping_file_being_generated) {
        flock($mapping_file_fh, LOCK_UN) || die "Unable to unlock mapping file\n"; # Remove the exclusive lock used to check if mapping file was already locked
    }
    
    my $pid = fork() // die "Could not fork process\n";
    if ($pid) {
        display_submitted_page($cgi, { 'file_must_be_generated' => $mapping_file_being_generated });
    } else {
        close STDOUT;
        close STDERR;
        flock($mapping_file_fh, LOCK_SH) || die "Unable to get shared lock for mapping file: $!\n";

        try {
            send_email_with_mapping_file({
                'sender' => get_sender_address(),
                'recipient' => get_recipient_address($cgi),
                'file_path' => get_path_to_mapping_file($database),
                'attachment_name' => get_name_of_mapping_file($database)
            });
        } catch {
            display_error_page($cgi, $_);
        } finally {
            close($mapping_file_fh);
        };
    }
}

sub remove_mapping_file {
    my $database = shift || confess "No database name provided\n";
    
    unlink(get_path_to_mapping_file($database)) if -e (get_path_to_mapping_file($database));
}

sub mapping_file_is_recent {
    my $database = shift || "No database provided for determining if mapping file is recent\n";
    my $minutes_to_cache_mapping_file = shift // 10;
    my $minutes_since_modification = (time - stat(get_path_to_mapping_file($database))->mtime) / 60;
    
    return ($minutes_since_modification <= $minutes_to_cache_mapping_file);
}

sub generate_mapping_file {
    my $data_host = shift || confess "No data host provided for generating mapping file\n";
    my $database = get_database($data_host);
    
    try {
        #close STDOUT;
        #close STDERR;
        
        my $output_dir_path = get_output_dir_path($database);
        my $output_file_name = get_name_of_mapping_file($database);
        my $mapping_file_script = catfile(get_scripts_dir_path(), 'db_id_to_name_mapping.pl');

        remove_mapping_file($database);
        make_path($output_dir_path, { chmod => 0775 });
    
        system("perl $mapping_file_script -host $data_host -db $database -output_dir $output_dir_path -output_file $output_file_name 2>> " . get_path_to_mapping_file($database) . ".err");
        # Exclusive file lock to mapping file removed when script terminates (even on interruption/failure) and creates
        # a race condition before the mapping file can be removed by the error_clean_up subroutine        
    } catch {
        error_clean_up($_, $database);
    };
}
    
sub send_email_with_mapping_file {
    my $parameters = shift || {};
    
    my $sender_address = $parameters->{'sender'} || confess "No e-mail address provided for sender\n";
    my $recipient_address = $parameters->{'recipient'} || confess "No e-mail address provided for recipient\n";
    my $file_path = $parameters->{'file_path'} || confess "No file path provided for mapping file\n";
    my $attachment_name = $parameters->{'attachment_name'} || confess "No attachment name provided for mapping file\n";
    
    my $msg = MIME::Lite->new(
        From => $sender_address,
        To => $recipient_address,
        Subject => 'Db id to mapping file export',
        Type => 'multipart/mixed'
    ) or confess "Could not create e-mail message: $!";
    
    $msg = add_email_body($msg);
    $msg = add_email_file_attachment($msg, $file_path, $attachment_name);

    $msg->send();
}

sub add_email_body {
    my $msg = shift || confess "No MIME::Lite e-mail object provided\n";
    
    my $sender_address = $msg->get('From', 0);
    my $body = <<"END_BODY";
This is an automated message.

This message contains the Reactome database id to name mapping file.
If you did not request this file, please delete this e-mail.
If there are any questions regarding this e-mail or the attached file, please respond to $sender_address
END_BODY

    $msg->attach(
        Type => 'TEXT',
        Data => $body
    );
    
    return $msg;
}

sub add_email_file_attachment {
    my $msg = shift || confess "No MIME::Lite e-mail object provided\n";
    my $file_path = shift || confess "No file path provided for attachment\n";
    my $attachment_name = shift || confess "No attachment name provided for attachment\n";

    $msg->attach(
       	Type => 'text/plain',
        Path => $file_path,
        Filename => $attachment_name
    ) or confess "Unable to attach mapping file: $_";
    
    return $msg;
}

sub display_submitted_page {
    my $cgi = shift || confess "No CGI object provided\n";
    my $page_information = shift || {};
    
    my $mapping_file_being_generated = $page_information->{'file_must_be_generated'} // 1;
    print $cgi->header,
          $cgi->start_html('Request for mapping file submitted'),
          $cgi->h1('Request for mapping file submitted');
          
    print $mapping_file_being_generated ?  
            $cgi->p('Generation of the db id to name mapping file can take about 10 minutes.  You should receive an e-mail after this completes.') :
            $cgi->p('File has been sent!');
    print $cgi->p('If you do not receive the mapping file within 30 minutes of your request, please contact ' . get_sender_address() . "\n");
    
    print get_home_page_link($cgi),
          $cgi->end_html;
}

sub display_error_page {
    my $cgi = shift || confess "No CGI object provided\n";
    my $error = shift;
    
    print $cgi->header,
          $cgi->start_html('Unable to send mapping file'),
          $cgi->h1('Error: Unable to send mapping file'),
          $cgi->p($error),
          $cgi->p('If this error persists, please contact ' . get_sender_address() . "\n");
          get_home_page_link($cgi),
          $cgi->end_html;
}

sub get_home_page_link {
    my $cgi = shift || confess "No CGI object provided\n";;
    return $cgi->a({href=>'/logic_table.html'}, 'Return to logic table form'),
}

sub get_gkb_dir_path {
    return catfile('', 'usr', 'local', 'reactomes', 'Reactome', 'production', 'GKB');
}

sub get_scripts_dir_path {
    return catfile(get_gkb_dir_path(), 'scripts');
}

sub get_output_dir_path {
    my $database = shift || confess "No database provided to determine output directory path\n";
    
    my $version = $database eq 'gk_central' ? 'gk_central' : get_reactome_version();
    
    return catfile(get_gkb_dir_path(), 'website', 'html', 'img-tmp', "db_id_mapping_file_export", $version);
}

sub get_path_to_mapping_file {
    my $database = shift || confess "No database provided to determine output directory path\n";
    return catfile(get_output_dir_path($database), 'db_id_to_name_mapping.txt');
}

sub get_name_of_mapping_file {
    my $database = shift || confess "No database provided to determine output directory path\n";
    return (fileparse(get_path_to_mapping_file($database)))[0];
}

sub get_reactome_version {
    my $version = get("https://reactome.org/ContentService/data/database/version");
    return $version if $version =~ /^\d+$/;
}

sub get_sender_address {
    return 'joel.weiser@oicr.on.ca';
}

sub get_recipient_address {
    my $cgi = shift || confess "No CGI object provided\n";
    
    my $recipient_address = $cgi->param('emailaddress');
    if (!(Email::Valid->address($recipient_address))) {
        confess "$recipient_address is not a valid e-mail address: $Email::Valid::Details check failed";
    }
    
    return $recipient_address;
}

sub get_data_host {
    my $cgi = shift || confess "No CGI object provided\n";

    my ($data_host) = $cgi->param('data_host') =~ /(reactome.*?(\.org|\.oicr\.on\.ca))$/;
    if (!$data_host) {
        confess $cgi->param('data_host') . " is not an authorized data host\n";
    }
    
    return $data_host;
}

sub get_database {
    my $data_host = shift || confess "No data host provided\n";
    
    return $data_host eq 'reactomecurator.oicr.on.ca' ? 'gk_central' : 'gk_current';
}

sub error_clean_up {
    my $error = shift || $!;
    my $database = shift || confess "No database provided\n";
    
    remove_mapping_file($database);
    die "$0: Process interrupted - $error\n";
}

