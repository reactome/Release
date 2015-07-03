#!/usr/local/bin/perl
use strict;
use warnings;

use Getopt::Long;

my ($git_repo, $ssh_server, $help);
GetOptions(
    'repo=s' => \$git_repo,
    'host=s' => \$ssh_server,
    'help' => \$help
);

if ($help) {
    print usage_instructions();
    exit;
}

$git_repo ||= '/usr/local/gkb';

my $update_cmd = "cd $git_repo &&
		  git stash &&
		  git pull &&
		  git stash pop";

$ssh_server ? `ssh $ssh_server $update_cmd` : `$update_cmd`;

sub usage_instructions {
    return <<END;

This script updates a git repository on a server

Usage: perl $0 [options]

-repo git_repository_repo_dir 	(default: /usr/local/gkb)
-host git_repository_host	(default: localhost)

Examples:
    perl $0
    perl $0 -repo /usr/local/gkb
    perl $0 -repo /usr/local/gkb -host reactomeprd1.oicr.on.ca
END

}