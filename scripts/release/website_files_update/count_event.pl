#!/usr/local/bin/perl  -w                                                                                                                                                          

# This is a better version of the script. the main script is 7check.pl and this 
#should always be a copy of 7check.pl

use lib "$ENV{HOME}/bioperl-1.0";
use lib "$ENV{HOME}/GKB/modules";

use GKB::ClipsAdaptor;
use GKB::DBAdaptor;
use GKB::Release::Utils;
use GKB::Release::Config;

use Data::Dumper;
use Getopt::Long;
 
use GKB::Utils_esther;
use strict;

print 'Enter recent test slice version: (e.g. 39, 39a, etc) ';
my $ver = <STDIN>;
chomp $ver;

print 'Enter previous test slice version: (e.g. 38) ';
my $prevver = <STDIN>;
chomp $prevver;

my $out = '/home/matthews/newevents'.$ver.'.html';

open (OUTPUT,">$out");
 
my $recent = 'test_slice_'.$ver;

print OUTPUT 'Current release is version '.$ver.'<br><br>';

print 'recent db is '.$recent."\n";

print OUTPUT 'recent db is '.$recent.'<br>';


my $db = $recent;

my $host = 'localhost';

my $pass = prompt("Enter your mysql password: ", 1);


my $dba_new = GKB::DBAdaptor->new(
     -dbname => $db,
     -user   => $user,
     -host   => $host, # host where mysqld is running                                                                                                                              
     -pass   => $pass,
     -port   => '3306'
);

my $human = '48887';

my (%hashnew, %hashold, $count, $count2, @ina);

my $proteinnew = $dba_new->fetch_instance(-CLASS => 'Event');

my %seen = ();

foreach (@{$proteinnew}){ 
    $hashnew{$_->db_id} = $_->db_id unless $seen{$_->db_id}++; 
}                                

print 'total events from ' . $recent . ' = ';

print OUTPUT 'total events from ' . $recent . ' = ';


my $total = keys %hashnew;
print $total . "\n";
print OUTPUT $total . '<br><br>';
 




my $previous= 'test_slice_' . $prevver . '_myisam';
chomp $previous;

print 'previous db is ' . $previous . "\n";

print OUTPUT 'previous db is '.$previous.'<br>';

$db = $previous;
 
my $dba_old = GKB::DBAdaptor->new (
     -dbname => $db,
     -user   => $user,
     -host   => $host, # host where mysqld is running                                                                                                                              
     -pass   => $pass,
     -port   =>  '3306'
);
 
my $proteinold = $dba_old->fetch_instance(-CLASS => 'Event');

%seen = ();

foreach (@{$proteinold}){
    $hashold{$_->db_id} =$_->db_id unless $seen{$_->db_id}++;
}

print 'total events from ' . $previous . ' = ';
    
$total = keys %hashold;
print $total . "\n\n";

print OUTPUT 'total events from ' . $previous. ' = ';
    
print OUTPUT $total . '<br><br>';
 
 
 
my %newevents;

my @id;

my $curator;
foreach (@{$proteinnew}){
    if (! $hashold{$_->db_id}){
        $count++;
	
        push (@id, $_->db_id);
	
        my $eve = $dba_new->fetch_instance_by_db_id($_->db_id);
	
        if($eve->[0]->created->[0] && $eve->[0]->created->[0]->author->[0]){
            $curator = $eve->[0]->created->[0]->author->[0]->_displayName->[0];
        }else{
            $curator = 'Created Mysteriously';
        }
        push(@{$newevents{$curator}},$_->db_id."\t".$_->_class->[0]."\t".$_->name->[0]);
        #print $_->db_id."\t".$_->_class->[0]."\t".$_->name->[0]."\n";
    }
}

foreach my $key (keys %newevents){
    #print  $key."\n\n";
    print OUTPUT $key.'<br><br>'."\n";
    #print  join ("\n", @{$newevents{$key}});
    print OUTPUT join ('<br>', @{$newevents{$key}});
    #print  "\n\n";
    print OUTPUT  '<br><br>'."\n";
}
 
print 'new events total = ' . $count . "\n";
 
print OUTPUT 'new events total = ' . $count . '<br><br>'."\n";
