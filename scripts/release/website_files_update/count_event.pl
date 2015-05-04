#!/usr/local/bin/perl -w
use strict;

use lib "/usr/local/gkb/modules";

use GKB::Config;
use GKB::DBAdaptor;

use autodie;
use Term::ReadKey;
 

my $recent_version = prompt('Enter recent test slice version: (e.g. 39, 39a, etc):');
my $previous_version = prompt('Enter previous test slice version: (e.g. 38):');

my $recent_test_slice = "test_slice_$recent_version";
my $previous_test_slice = "test_slice_$previous_version\_myisam";

open (my $output, ">", "newevents$recent_version.txt"); 
report("Current release is version $recent_version\n", $output);

my @recent_slice_events = get_slice_events($recent_test_slice);
my @previous_slice_events = get_slice_events($previous_test_slice);

report("total events from $recent_test_slice = " . get_unique_db_ids(@recent_slice_events) . "\n", $output);
report("total events from $previous_test_slice = " . get_unique_db_ids(@previous_slice_events) . "\n", $output);
 
my %new_events;
my $total_new_events;
foreach my $event (@recent_slice_events){
    my $event_db_id = $event->db_id;
    if (! grep(/^$event_db_id$/, map($_->db_id, @previous_slice_events))) {
        my $curator = get_author_name($event) ? get_author_name($event) : 'Created Mysteriously';
        push(@{$new_events{$curator}}, $event->_class->[0] . " " . $event->db_id . ":" . $event->name->[0]);
	
	$total_new_events++;
    }
}

print $output "\n\nCurator\tNew Event Count\tNew Events\n";
foreach my $curator (keys %new_events){
    print $output $curator . "\t" .
		  scalar @{$new_events{$curator}} . "\t" .
		  join ("\t", @{$new_events{$curator}})."\n";
}
 
report('new events total = ' . $total_new_events . "\n", $output);

close($output);

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

sub get_slice_events {
    my $slice = shift;
    
    my $dba = get_dba($slice);
    my $slice_events = $dba->fetch_instance(-CLASS => 'Event');
    
    return @{$slice_events};
}

sub get_unique_db_ids {
    my @instances = @_;
    
    my %unique_ids = ();
    foreach my $instance (@instances){ 
        $unique_ids{$instance->db_id} = 1; 
    }
    
    return keys %unique_ids;
}

sub get_dba {
    my $db = shift;

    return GKB::DBAdaptor->new(
	-dbname => $db,
	-user   => $GKB::Config::GK_DB_USER,
	-host   => $GKB::Config::GK_DB_HOST, # host where mysqld is running                                                                                                                              
	-pass   => $GKB::Config::GK_DB_PASS,
	-port   => '3306'
    );	
}

sub report {
    my $message = shift;
    my @file_handles = @_;
    
    push @file_handles, *STDOUT;
    
    foreach my $file_handle (@file_handles) {
	print $file_handle $message;
    }
}

sub get_author_name {
    my $event = shift;
    
    return unless ($event->created->[0] && $event->created->[0]->author->[0]);
    
    return $event->created->[0]->author->[0]->_displayName->[0];
}

