#!/usr/local/bin/perl
use strict;
use warnings;

my $git_repo = shift || die usage_instructions();
my $ssh_server = shift;

my $update_cmd = "cd $git_repo;git stash;git pull;git stash pop";

$ssh_server ? `ssh $ssh_server $update_cmd` : `$update_cmd`;

sub usage_instructions {
    return <<END;

Please provide a git repository directory and, optionally, a host name

Usage: perl $0 git_repo [server host]
Examples: perl $0 /usr/local/gkb
	  perl $0 /usr/local/gkb reactomeprd1.oicr.on.ca
END

}