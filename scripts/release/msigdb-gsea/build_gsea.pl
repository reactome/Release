#!/usr/bin/perl
use strict;
use warnings;

our $VERSION = 1.0;

use autodie qw/:all/;
use Cwd;

my %repositories = (
    'Pathway-Exchange' => {
        url => 'https://github.com/reactome/Pathway-Exchange.git',
        ant_xml_file => 'GSEAdeployAsApplication.xml',
        output => 'GSEAExport.jar'
    },
    'CuratorTool' => {
        url => 'https://github.com/reactome/CuratorTool.git',
        ant_xml_file => 'WebELVDiagram.xml',
        output => 'WebELVTool/*.jar'
    }
);

foreach my $repository_name (keys %repositories) {
    my $current_dir = getcwd;
    if (-d "$repository_name/.git") {
        chdir $repository_name;
        system 'git pull';
        chdir $current_dir;
    } else {
        my $repository_url = $repositories{$repository_name}{'url'};
        system "git clone $repository_url";
    }

    chdir "$repository_name/ant";
    my $ant_xml_file = $repositories{$repository_name}{'ant_xml_file'};
    system "ant -buildfile $ant_xml_file";
    chdir $current_dir;

    my $output = $repositories{$repository_name}{'output'};
    system "mv $repository_name/$output .";
}
