#!/usr/bin/perl -T
use strict;
use warnings;

use autodie qw/:all/;
no autodie qw/flock system/;
use Capture::Tiny qw/:all/;
use CGI;
use CGI::Carp qw/fatalsToBrowser/;
use File::Path qw/make_path remove_tree/;
use File::stat;
use Fcntl qw/:flock/;
use Time::HiRes qw/gettimeofday/;
use Try::Tiny;

use lib '/usr/local/gkb/modules';
use GKB::CommonUtils;

$ENV{PATH} = '/bin:/bin/sbin:/usr/bin:/usr/sbin:/usr/local/bin:/usr/local/sbin';

my $cgi = CGI->new;

my @db_ids = get_db_ids($cgi);
my $join_pathways = $cgi->param('join_pathways') && $cgi->param('join_pathways') eq 'checked' ? 1 : 0;
my $database_server = 'reactomecurator.oicr.on.ca';
my $database = get_data_host($cgi) eq 'curator' ? 'gk_central' : 'gk_current';

my $dba = get_dba($database, $database_server);
my @problem_ids = grep {
    my $instance = $dba->fetch_instance_by_db_id($_)->[0];
    !$instance || !$instance->is_a('Pathway');
} @db_ids;
    
if (@problem_ids) {
    display_error_message($cgi, "No pathway(s) available for ids " . join(',', @problem_ids));
    exit;
}

my $timestamp = int(gettimeofday);
my $gkb = "/usr/local/reactomes/Reactome/production/GKB";
my $scripts = "$gkb/scripts";
my $output_dir = "$gkb/website/html/img-tmp/reaction_logic_table_output/" . get_version($database, $dba);
make_path($output_dir);

my (@output_files, %problem_files);
my @ids = $join_pathways ? (join ",", @db_ids) : @db_ids;
foreach my $id (@ids) {
    my $file_name = get_file_name($dba, $id);
    my $file_full_path = "$output_dir/$file_name";
    
    open(my $generate_fh, '>', "$file_full_path.lock");
    my $logic_table_being_generated = !(flock($generate_fh, LOCK_EX|LOCK_NB));
    if (!$logic_table_being_generated) {
        flock($generate_fh, LOCK_UN) || die "Unable to unlock logic table file: $!\n";
    } else {
        flock($generate_fh, LOCK_SH) || die "Unable to get shared lock on logic table file: $!\n";
    }
    
    flock($generate_fh, LOCK_EX);
    if ((!-e $file_full_path) || ($database eq 'gk_central' && (!file_is_recent($file_full_path)))) {
        my $error = capture_stderr {
            system("perl $scripts/reaction_logic_table.pl -host $database_server -db $database -pathways $id -output_dir $output_dir -output_file $file_name");
        };
        
        open(my $error_fh, '>', "$output_dir/$file_name.err");
        if ($error) {
            print $error_fh "$error\n";
    
            $problem_files{$id} = $file_name;
            unlink("$output_dir/$file_name") if (-e "$output_dir/$file_name");
            next;
        }
        close($error_fh) || die;
    }
    close($generate_fh);
    
    push @output_files, $file_name;
}

if (keys %problem_files) {
    display_error_message($cgi, "Unable to generate file(s) for ids " . join('|', map { $_ . ':' . $problem_files{$_} } keys %problem_files));
    exit;
}

download_single_file($output_dir, \@output_files, $cgi);

sub get_db_ids {
    my $cgi = shift || confess "No CGI object provided\n";
    
    my ($db_ids) = $cgi->param('db_ids') =~ /^((\d+)(\s+\d+)*)$/;
    if (!$db_ids) {
        confess $cgi->param('db_ids') . " must be a whitespace separated list of one of more database identifiers\n";
    }
    
    return split(/\s+/, $db_ids);
}

sub get_data_host {
    my $cgi = shift || confess "No CGI object provided\n";

    my ($data_host) = $cgi->param('data_host') =~ /(production|curator)/;
    if (!$data_host) {
        confess $cgi->param('data_host') . " is not an authorized data host\n";
    }

    return $data_host;
}

sub get_version {
    my $database = shift || confess "No database name provided\n";
    my $dba = shift || confess "No DBAdaptor.pm object provided\n";
    my $version = $database eq 'gk_central' ? 'gk_central' : get_database_version($dba);
    
    return $version;
}

sub get_database_version {
    my $dba = shift || confess "No DBAdaptor.pm object provided\n";
    
    try {
        return $dba->fetch_instance(-CLASS => '_Release')->[0]->releaseNumber->[0];
    } catch {
        confess "Unable to get release number: $_\n";
    };
}

sub display_error_message {
    my $cgi = shift || confess "No CGI object provided\n";
    my $error_message = shift || confess "No error message provided\n";
    
    print $cgi->header,
          $cgi->start_html($error_message),
          $cgi->h1($error_message);
    
    print $cgi->p('Navigate to previous page to try modifying your requested pathways'),
          $cgi->end_html;
}

sub file_is_recent {
    my $file = shift || confess "No file name provided\n";
    my $minutes_to_cache_mapping_file = shift // 10;
    my $minutes_since_modification = (time - stat($file)->mtime) / 60;

    return ($minutes_since_modification <= $minutes_to_cache_mapping_file);
}

sub get_file_name {
    my $dba = shift || confess "No DBAdaptor.pm object provided\n";
    my $id_string = shift;
    
    my @ids = split /\s+/, $id_string;
    if (scalar @ids == 0) {    
        confess "$id_string contains no database identifiers\n";
    }
    
    my $file_name;

    if (scalar @ids == 1) {
        my $instance = $dba->fetch_instance_by_db_id($ids[0])->[0];
        $file_name = $instance->displayName . '.' . $instance->db_id;
    } else {
        $file_name = join("_", @ids);
    }
    $file_name =~ s/['\\\/:\(\)&; ]+/_/g;
    return $file_name . '.tsv';
}

sub download_single_file {
    my $dir = shift;
    my $files = shift;
    my $cgi = shift;
    
    my $compress_to_single_file = (scalar @{$files} > 1);
    my $file = $compress_to_single_file ? zip_files($dir, $files) : $files->[0];
    
    open(my $fh, '<:raw', "$dir/$file") || confess "Unable to open $dir/$file: $!\n";
    print $cgi->header(
        -type => 'application/octet-stream',
        -attachment => $file
    );
    binmode $fh;
    print $_ while (<$fh>);
    close $fh;
    
    if ($compress_to_single_file) {
        unlink("$dir/$file") if (-e "$dir/$file");
    }
}

sub zip_files {
    my $dir = shift || confess "No directory provided\n";
    my $files = shift || confess "No files provided\n";
    my $timestamp = int(gettimeofday);
    my $logic_table_zipped_file = "logic_tables.$timestamp.tar.gz";

    my $file_list = join(' ', @{$files});
    
    system("tar -czf $dir/$logic_table_zipped_file -C $dir $file_list");

    return $logic_table_zipped_file;
}