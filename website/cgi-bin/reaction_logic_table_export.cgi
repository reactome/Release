#!/usr/bin/perl
use strict;
use warnings;

use autodie qw/:all/;
use CGI;
use Cwd;
use File::Path qw/make_path remove_tree/;
use Time::HiRes qw/gettimeofday/;

my $cgi = CGI->new;

my @db_ids = split(' ', $cgi->param('db_ids'));
my $join_pathways = $cgi->param('join_pathways') eq 'checked' ? 1 : 0;

my $timestamp = int(gettimeofday);
my $gkb = "/usr/local/reactomes/Reactome/production/GKB";
my $scripts = "$gkb/scripts";
my $output_dir = "$gkb/website/html/img-tmp/reaction_logic_table_output.$timestamp";
make_path($output_dir);

my @ids = $join_pathways ? (join ",", @db_ids) : @db_ids;
foreach my $id (@ids) {
    system("perl $scripts/reaction_logic_table.pl -pathways $id -output_dir $output_dir");
}

download_single_file($output_dir, $cgi);
remove_tree($output_dir);

sub download_single_file {
    my $dir = shift;
    my $cgi = shift;
    
    my @files = get_files($dir);
    my $file_to_download;
    if (scalar @files > 1) {
        $file_to_download = zip_directory($dir);
    } else {
        $file_to_download = $files[0];
    }
    
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
    `tar -czvf $logic_table_zipped_file *`;
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
        -content-disposition => 'attachment',
        -attachment => $file
    );
    binmode $fh;
    print $_ while (<$fh>);
    close $fh;
}