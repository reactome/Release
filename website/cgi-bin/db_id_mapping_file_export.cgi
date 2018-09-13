#!/usr/bin/perl -T
use strict;
use warnings;

use autodie qw/:all/;
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

run(@ARGV) unless caller();

sub run {
    Readonly my $sender_address => 'joel.weiser@oicr.on.ca';
    #Readonly my $minutes_to_cache_mapping_file => 5;
    
    STDOUT->autoflush;
    $ENV{PATH} = '/bin/sbin:/usr/bin:/usr/sbin:/usr/local/bin:/usr/local/sbin';
    
    my $cgi = CGI->new;
    if ($cgi->request_method eq 'GET') {
        die "This CGI script cannot be accessed directly\n";
    }
    
    my $recipient_address = $cgi->param('emailaddress');
    if (!(Email::Valid->address($recipient_address))) {
        confess "$recipient_address is not a valid e-mail address: $Email::Valid::Details check failed";
    }
    
    if (-s get_path_to_mapping_file()) { #&& mapping_file_is_recent($minutes_to_cache_mapping_file)) {
        open(my $mapping_file_fh, '<', get_path_to_mapping_file());
        flock($mapping_file_fh, LOCK_SH);
        try {
            send_email_with_mapping_file({
                'sender' => $sender_address,
                'recipient' => $recipient_address
            });
        } catch {
            display_error_page($cgi, $_);
            exit;
        };
        display_submitted_page($cgi, { 'file_must_be_generated' => 0 });
        close($mapping_file_fh);
        exit 0;
    }
    
    my $pid = fork() // die "Could not fork process\n";
    if ($pid) {
        display_submitted_page($cgi, { 'file_must_be_generated' => 1 });
    } else {
        try {
            close STDOUT;
            close STDERR;
            
            my $output_dir_path = get_output_dir_path();
            my $output_file_name = get_name_of_mapping_file();
            my $mapping_file_script = catfile(get_scripts_dir_path(), 'db_id_to_name_mapping.pl');

            unlink($output_file_name) if (-e $output_file_name);
            make_path($output_dir_path);
            system("perl $mapping_file_script -output_dir $output_dir_path -output_file $output_file_name 2> $output_file_name.err");
            send_email_with_mapping_file({
                'sender' => $sender_address,
                'recipient' => $recipient_address
            });
        } catch {
            error_clean_up($_);
        };
        
        exit 0;
    }
}

sub mapping_file_is_recent {
    my $minutes_to_cache_mapping_file = shift;
    my $minutes_since_modification = (time - stat(get_path_to_mapping_file())->mtime) / 60;
    
    return ($minutes_since_modification <= $minutes_to_cache_mapping_file);
}
    
sub send_email_with_mapping_file {
    my $parameters = shift || {};
    
    my $sender_address = $parameters->{'sender'} || confess "No e-mail address provided for sender\n";
    my $recipient_address = $parameters->{'recipient'} || confess "No e-mail address provided for recipient\n";
    
    my $msg = MIME::Lite->new(
        From => $sender_address,
        To => $recipient_address,
        Subject => 'Db id to mapping file export',
        Type => 'multipart/mixed'
    ) or confess "Could not create e-mail message: $!";
    
    $msg = add_email_body($msg);
    $msg = add_email_file_attachment($msg);

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

    $msg->attach(
       	Type => 'text/plain',
        Path => get_path_to_mapping_file(),
        Filename => get_name_of_mapping_file()
    );
    
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
            $cgi->p('Generation of the db id to name mapping file takes about 5 minutes.  You should receive an e-mail after this completes.') :
            $cgi->p('File has been sent!');
    
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
          get_home_page_link($cgi),
          $cgi->end_html;
}

sub get_home_page_link {
    my $cgi = shift;
    return $cgi->a({href=>'/logic_table.html'}, 'Return to logic table form'),
}

sub get_gkb_dir_path {
    return catfile('', 'usr', 'local', 'reactomes', 'Reactome', 'production', 'GKB');
}

sub get_scripts_dir_path {
    return catfile(get_gkb_dir_path(), 'scripts');
}

sub get_output_dir_path {
    return catfile(get_gkb_dir_path(), 'website', 'html', 'img-tmp', "db_id_mapping_file_export", get_reactome_version());
}

sub get_path_to_mapping_file {
    return catfile(get_output_dir_path(), 'db_id_to_name_mapping.txt');
}

sub get_name_of_mapping_file {
    return (fileparse(get_path_to_mapping_file()))[0];
}

sub get_reactome_version {
    my $version = get("https://reactome.org/ContentService/data/database/version");
    return $version if $version =~ /^\d+$/;
}

sub error_clean_up {
    my $error = shift;
    
    unlink(get_name_of_mapping_file()) if -e (get_name_of_mapping_file());
    die "$0: Process interrupted - " . ($error || $!) . "\n";
}

