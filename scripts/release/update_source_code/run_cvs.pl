#!/usr/local/bin/perl -w

# Runs CVS over selected directories at release time.

# I think it's more user-friendly if the user will not have to practise any
# symlink tricks. Hence the BEGIN block. If the location of the script or
# libraries changes, this will have to be changed.

BEGIN {
    my @a = split('/',$0);
    pop @a;
    push @a, ('..','..','modules');
    my $libpath = join('/', @a);
    unshift (@INC, $libpath);
}
# Set the umask so that the files are group writeable
umask(002);

use strict;
use Cwd;
use Getopt::Long;
use GKB::Config;
use GKB::FileUtils;

our($opt_release);
&GetOptions("release:s");

if (!(defined $opt_release) || $opt_release eq '') {
    print STDERR "You must specify a release number, via the -release command line option\n";
    exit(1);
}

my $command;

# Add the directories that should be updated to this array
# If you don't supply an absolute path, they will be assumed to be relative to the GKB directory.
my @directories_to_be_updated = ("/usr/local/caBIG/caBIGR3", "biopaxexporter", "modules/GKB", "scripts", "orthomcl_project", "java", "BioMart/reactome", "ReactomeGWT", "slicingTool", "website");
my @directories_to_be_emptied = ("website/html/userguide");

# Find the root directory of the GKB hierarchy
my $gk_root_dir = $GK_ROOT_DIR;
if (!(defined $gk_root_dir) || $gk_root_dir eq '') {
	# If no root directory is defined for GKB,
	# assume that the current directory is GKB/scripts.
	# A bit nieave, we can but try, sigh.
	my $curr_work_dir = &Cwd::cwd();
	
	$gk_root_dir = "$curr_work_dir/../..";
}

my @update_collection = ();
$update_collection[0][0] = $gk_root_dir;
$update_collection[0][1] = "";
my $update_collection_counter = 1;
for (my $i=0; $i<scalar(@directories_to_be_updated); $i++) {
    if (!($directories_to_be_updated[$i] =~ /^[\/]/)) {
       $update_collection[0][1] .= " " . $directories_to_be_updated[$i];
    } elsif (!(-e $directories_to_be_updated[$i])) {
       print STDERR "Directory does not exist: " . $directories_to_be_updated[$i] . ", ignoring\n";
       next;
    } else {
        $update_collection[$update_collection_counter][0] = $directories_to_be_updated[$i];
        $update_collection[$update_collection_counter][1] = "*";
        $update_collection_counter++;
    }

    foreach my $directory_to_be_emptied (@directories_to_be_emptied) {
        if ($directory_to_be_emptied =~ /^$directories_to_be_updated[$i]/) {
            if (!($directory_to_be_emptied =~ /^[\/]/)) {
                $directory_to_be_emptied = $gk_root_dir . "/" . $directory_to_be_emptied;
            }
            $command = "rm $directory_to_be_emptied/*";
            print STDERR "directory_to_be_emptied=$directory_to_be_emptied\n";
            my $status = system($command);
            
#            if ($status != 0) {
#                    print STDERR "Nonzero exit status for: $command\n";
#                    exit(1);
#            }
#
            last;
        }
    }
}

my $cvs_output = "";
my $target_directory;
my $directories_to_be_updated;
my $file_utils = new GKB::FileUtils();
for (my $i=0; $i<scalar(@update_collection); $i++) {
    $target_directory = $update_collection[$i][0];
    print "Dealing with target_directory=$target_directory\n";
    $directories_to_be_updated = $update_collection[$i][1];
    $command = "cd $target_directory; sudo chgrp -R gkb $directories_to_be_updated; sudo chmod -R g+rw $directories_to_be_updated";
    my $status = system($command);
    
    if ($status != 0) {
            print STDERR "Nonzero exit status for: $command\n";
            exit(1);
    }
    
    $command = "cd $target_directory; cvs up -d $directories_to_be_updated";
    $cvs_output .= $file_utils->run_command($command);
    print "Finished dealing with target_directory=$target_directory\n";
}
if (!($cvs_output eq '')) {
    $file_utils->write_file("$gk_root_dir/tmp/cvs_update_$opt_release.out", $cvs_output);
}
my @cvs_output_lines = split(/\n/, $cvs_output);
my @error_lines = ();
foreach my $cvs_output_line (@cvs_output_lines) {
    if ($cvs_output_line =~ /^C /) {
        push(@error_lines, $cvs_output_line);
    }
}
if (scalar(@error_lines) > 0) {
    print STDERR "The following files contain conflicts:\n";
    foreach my $error_line (@error_lines) {
        print STDERR "$error_line\n";
    }
}

