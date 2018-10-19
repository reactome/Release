#!/usr/bin/perl
use strict;
use warnings;

#use autodie qw/:all/;
use Capture::Tiny qw/:all/;
use CGI;
use CGI::Carp qw/fatalsToBrowser/;
use Cwd;
use File::Path qw/make_path remove_tree/;
use Time::HiRes qw/gettimeofday/;

use lib '/usr/local/gkb/modules';
use GKB::CommonUtils;

my $cgi = CGI->new;

my @db_ids = split(' ', $cgi->param('db_ids'));
my $join_pathways = $cgi->param('join_pathways') && $cgi->param('join_pathways') eq 'checked' ? 1 : 0;
my $data_host = $cgi->param('data_host');
my $database = $data_host eq 'reactomecurator.oicr.on.ca' ? 'gk_central' : 'gk_current';

my $dba = get_dba($database, $data_host);
my @ids = $join_pathways ? (join ",", @db_ids) : @db_ids;
my @problem_ids = grep {
    my $instance = $dba->fetch_instance_by_db_id($_)->[0];
    !$instance || !$instance->is_a('Pathway');
} @ids;
    
if (@problem_ids) {
    display_error_message($cgi, "No pathway(s) available for ids " . join(',', @problem_ids));
    exit;
}

my $timestamp = int(gettimeofday);
my $gkb = "/usr/local/reactomes/Reactome/production/GKB";
my $scripts = "$gkb/scripts";
my $output_dir = "$gkb/website/html/img-tmp/reaction_logic_table_output.$timestamp";
make_path($output_dir);

foreach my $id (@ids) {
    my $error = capture_stderr {
        system("perl $scripts/reaction_logic_table.pl -host $data_host -db $database -pathways $id -output_dir $output_dir");
    };
    if ($error) {
        write_error_file("$output_dir/$id.err", $error);
    }
}

if (no_non_error_files($output_dir)) {
    display_error_message($cgi, "Unable to generate file(s) for ids " . $cgi->param('db_ids'));
    remove_tree($output_dir);
    exit;
}

download_single_file($output_dir, $cgi);
remove_tree($output_dir);

sub display_error_message {
    my $cgi = shift;
    my $error_message = shift;
    
    print $cgi->header,
          $cgi->start_html($error_message),
          $cgi->h1($error_message);
    
    print $cgi->p('Navigate to previous page to try modifying your requested pathways'),
          $cgi->end_html;
}

sub write_error_file {
    my $error_file_path = shift;
    my $error = shift;
    
    open(my $error_fh, '>', $error_file_path);
    print $error_fh "$error\n";
    close($error_fh);
}

sub no_non_error_files {
    my $dir = shift;
    return (scalar(grep { $_ !~ /.err$/ } get_files($dir)) == 0);
}

sub download_single_file {
    my $dir = shift;
    my $cgi = shift;
    
    my @files = get_files($dir);
    my $file_to_download = (scalar @files > 1) ? zip_directory($dir) : $files[0];
    
    download_file($dir, $file_to_download, $cgi);
}

sub get_files {
    my $dir = shift;
    
    opendir(my $dh, $dir);
    my @files = grep { -f "$dir/$_" } readdir($dh);
    
    return @files;
}

sub zip_directory {
    my $dir = shift;
    my $logic_table_zipped_file = 'logic_tables.tar.gz';
    
    my $cwd = getcwd;
    chdir $dir;
    system("tar -czvf $logic_table_zipped_file *");
    chdir $cwd;
    
    return $logic_table_zipped_file;
}

sub download_file {
    my $dir = shift;
    my $file = shift;
    my $cgi = shift;
    
    open(my $fh, '<:raw', "$dir/$file");
    print $cgi->header(
        -type => 'application/octet-stream',
        -attachment => $file
    );
    binmode $fh;
    print $_ while (<$fh>);
    close $fh;
}
