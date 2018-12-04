package GKB::Release::Utils;

=head1 NAME

GKB::Release::Utils

=head1 DESCRIPTION

Provides general purpose sub-routines for the Reactome
release pipeline.

=head2 Methods

=over 12

=item C<set_environment>

Sets the HOME, PATH, and PERL5LIB
environment variables for using the
release pipeline.

Parameter:
	Reactome server host (String -- required)
	
=item C<prompt>

Queries the user for information and optionally
hides key strokes.

Parameter:
	Query (String -- required)
	Hide key strokes (Boolean -- optional with default false)

Return:
	Response (String)
	
=item C<releaselog>

Takes input for logging and writes
to the log file specified in the
configuration module.

Parameter:
	Lines (Array -- required)

Return:
	Lines (Array)
	
=item C<replace_gkb_alias_in_dir>

Returns a directory path with the gkb alias in
the directory path provided with the new gkb
alias provided.

Parameter:
	Directory path to be changed (String -- required)
	GKB alias (String -- required)

Return:
	Directory path with new alias (String)

=back
	
=head1 SEE ALSO

GKB::Release::Config
GKB::Release::Step

=head1 AUTHOR
Joel Weiser E<lt>joel.weiser@oicr.on.caE<gt>

Copyright (c) 2015 Ontario Institute for Cancer Research

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

use strict;
use warnings;

use v5.10;

use lib '/usr/local/gkb/modules';

use GKB::Config;
use GKB::DBAdaptor;
use GKB::Release::Config;


use Term::ReadKey;
use autodie;

use base 'Exporter';


# Created by: Joel Weiser (joel.weiser@oicr.on.ca for questions/comments)
# Purpose: A module to provide utility functions for the Reactome release pipeline.

#Exports all subroutines
our @EXPORT = qw/get_dba set_environment prompt releaselog replace_gkb_alias_in_dir/;

sub get_dba {
	my $db = shift;
	my $host = shift // $GKB::Config::GK_DB_HOST;
	
	return GKB::DBAdaptor->new(
		-dbname => $db,
		-user => $GKB::Config::GK_DB_USER,
		-pass => $GKB::Config::GK_DB_PASS,
		-host => $host,
		-port => 3306
	);
}

# C shell environment
sub set_environment {
    my $host = shift;
    my $gkb = $hosts{$host} // 'gkb';

    $ENV{'HOME'} = "/home/$user";
    $ENV{'PATH'} = "/usr/local/bin:$ENV{'PATH'}";
    $ENV{'PERL5LIB'} .= ":/usr/local/$gkb/modules:/usr/local/bioperl-1.0";
    umask 0002;
}

# Ask user for information
sub prompt {
    my $question = shift;
    print $question;
    my $pass = shift;
    ReadMode 'noecho' if $pass; # Don't show keystrokes if it is a password
    my $return = ReadLine 0;
    chomp $return;
    
    ReadMode 'normal';
    print "\n" if $pass;
    return $return;
}

sub replace_gkb_alias_in_dir {
	my $dir = shift;
	my $gkb = shift;
	
	$dir =~ s/gkb.*?\//$gkb\//;
	
	return $dir;
}

sub releaselog {
	my @lines = @_;
	
	_make_log_directory($logdir);
	_write_to_log_file($logfile, @lines);
	
	return @lines;
}

sub _make_log_directory {
	my $log_directory = shift;
	return 1 if (-e $log_directory);
	return (system("mkdir -p $log_directory") == 0);
}

sub _write_to_log_file {
	my $log_file = shift;
	my @lines = @_;
	
	open(my $log, '>>', $log_file);
	say $log @lines;
	close $log;	
}

1;
