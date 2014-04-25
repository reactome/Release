#!/usr/bin/perl
use strict;
use warnings;
use v5.10;

use DateTime;

print 'WARNING - Running this script will overwrite existing cron jobs for user ' . `whoami`;
say 'If you wish to view existing cron jobs for this user, type "crontab -l" at the command prompt';

my $response = '';
$response = prompt('Do you wish to continue running this script - y/n:') until $response eq 'n' || $response eq 'y';
exit if $response eq 'n';

my $out = 'cron.txt';
my @flags = qw/-rr -df -fs -rl -i/;
my %events = (
    '-df' => {
        name => "Datafreeze",
        date => undef        
    },
    
    '-rr' => {
        name => "Review Request",
        date => undef
    },
    
    '-fs' => {
        name => "Final Slice",
        date => undef
    },
    
    '-rl' => {
        name => "Review Letter",
        date => undef
    },
    
    '-i' => {
        name => "Illustrator",
        date => undef
    }
);

my $append = '';
if (-e $out) {
    $append = prompt('Append to existing calendar - y/n:') until $append eq 'n' || $append eq 'y';
}

my $cron;
if ($append eq 'y') {
    open ($cron, ">>", $out);
} else {
    open ($cron, ">", $out);
}

my $version = prompt("Enter Reactome version number where this calendar should begin adding reminder dates: ");
my $cycles = prompt('How many release cycles are you adding to the calendar? ');

my $scripts = '/usr/local/gkbdev/scripts';

for (my $i = 0; $i < $cycles; $i++) {
    foreach my $flag (@flags) {
        my $datestring = prompt("Enter date of " . $events{$flag}->{'name'} . " reminder as 'yyyymmdd' for release $version: ");
        $datestring =~ /(\d{4})(\d{2})(\d{2})/;
        my $date = DateTime->new(
            year => $1,
            month => $2,
            day => $3
        );
        $events{$flag}->{'date'} = $date;
    }
    
    my $df_date = $events{'-df'}->{'date'};
    my $df = get_date_string($df_date);
    
    my $fs_date = $events{'-fs'}->{'date'};
    my $fs = get_date_string($fs_date);
    
    foreach my $flag (@flags) {
        my $date = $events{$flag}->{'date'};
        my $day = $date->day();
        my $month = $date->month();
        
        my $line = "0 0 $day $month '*' perl $scripts/releasemail.pl $flag $version";
        if ($flag eq '-df') {
            $line .= " \'$df\' \'$fs\'";
        }
        
        say $cron $line;
    }
    
    $version++;
}    
close $cron;

`crontab $out`;

sub prompt {
    my $query = shift;
    print $query;
    
    my $answer = <>;
    chomp $answer;
    return $answer;
}

sub get_date_string {
    my $date = shift;
    
    return $date->day_name() . ', ' . $date->month_name() . ' ' . $date->day();
}