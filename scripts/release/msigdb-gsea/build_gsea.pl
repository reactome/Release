#!/usr/bin/perl
use strict;
use warnings;

our $VERSION = 1.0;

use autodie qw/:all/;
use Capture::Tiny ':all';
use Cwd;
use Getopt::Long;
use Readonly;

Readonly my $DEFAULT_REPO_VERSION => 'master';
my ($pathway_exchange_repo_version, $curator_tool_repo_verison);
GetOptions(
    'pathway-exchange-version:s' => \$pathway_exchange_repo_version,
    'curator-tool-version:s' => \$curator_tool_repo_verison
);
$pathway_exchange_repo_version ||= $DEFAULT_REPO_VERSION;
$curator_tool_repo_verison ||= $DEFAULT_REPO_VERSION;

my %repositories = (
    'Pathway-Exchange' => {
        url => 'https://github.com/reactome/Pathway-Exchange.git',
        version => $pathway_exchange_repo_version,
        ant_xml_file => 'GSEAdeployAsApplication.xml',
        output => 'GSEAExport.jar',
    },
    'CuratorTool' => {
        url => 'https://github.com/reactome/CuratorTool.git',
        version => $curator_tool_repo_verison,
        ant_xml_file => 'WebELVDiagram.xml',
        output => 'WebELVTool/*.jar',
    }
);

# Build required output from each repository
foreach my $repository_name (keys %repositories) {
    my $current_dir = getcwd;
    my $repo_version = $repositories{$repository_name}{'version'};
    # Pull latest changes if local repository exists
    if (-d "$repository_name/.git") {
        chdir $repository_name;
        my $stderr = run_command('git pull');
        if (trim($stderr)) {
            print STDERR "Problem pulling $repository_name:\n$stderr";
        }

        chdir $current_dir;
    # Clone repository if it doesn't exist locally
    } else {
        my $repository_url = $repositories{$repository_name}{'url'};
        my $stderr = run_command("git clone $repository_url", {
            ignore_error => qr/^Cloning into .*/
        });

        if (trim($stderr)) {
            print STDERR "Problem cloning $repository_url:\n$stderr";
        }
    }

    # Find the appropriate ant file and build the required output
    chdir $repository_name;
    my $stderr = run_command("git checkout $repo_version", {
        ignore_error => qr/^Already on .*/
    });
    if (trim($stderr)) {
        print STDERR "Problem checking out $repo_version:\n$stderr";
    }


    my $ant_xml_file = $repositories{$repository_name}{'ant_xml_file'};
    system "ant -buildfile ant/$ant_xml_file";
    chdir $current_dir;

    # Move the output to the directory containing this script
    my $output = $repositories{$repository_name}{'output'};
    system "mv $repository_name/$output .";
}

sub trim {
    my $string = shift;

    $string =~ s/^\s+//; # Remove leading white space
    $string =~ s/\s+$//; # Remove trailing white space

    return $string;
}

sub run_command {
    my $command = shift;
    my $options = shift;

    my $stderr = capture_stderr {
        system $command;
    };
    # Remove unneeded message of cloning repository from STDERR
    # Then output any error messages remaining back to STDERR
    my $error_to_ignore = $options->{'ignore_error'};
    if ($error_to_ignore) {
        $stderr =~ s/$error_to_ignore//m;
    }

    return $stderr;
}
