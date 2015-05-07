#!/usr/local/bin/perl -w
use strict;

use lib "/usr/local/gkbdev/modules";

use GKB::Config;
use GKB::DBAdaptor;
use GKB::Utils;

use autodie;
use Term::ReadKey;

my $recent_version = prompt('Enter recent test slice version: (e.g. 39, 39a, etc):');
my $previous_version = prompt('Enter previous test slice version: (e.g. 38):');

my $recent_db = "test_slice_$recent_version";
print 'recent db is '.$recent_db."\n";
my $dba_recent = get_dba($recent_db);
my $ewas_recent = get_human_ewas_instances($dba_recent);

my $previous_db = "test_slice_$previous_version";
print 'previous db is '.$previous_db."\n";
my $dba_old = get_dba($previous_db);
my $ewas_old = get_human_ewas_instances($dba_old);

my %recent_ewas_names;
my %old_ewas_names;

foreach my $ewas_instance (@{$ewas_recent}){
	push @{$recent_ewas_names{$ewas_instance->displayName}},$ewas_instance;
}
print 'new slice total = ' . (scalar keys %recent_ewas_names) ."\n";

foreach my $ewas_instance (@{$ewas_old}){
	push @{$old_ewas_names{$ewas_instance->displayName}},$ewas_instance;
}
print 'old slice total = ' . (scalar keys %old_ewas_names) . "\n";
 
my %seen = ();
my @new_ewas;
foreach my $recent_ewas_name (keys %recent_ewas_names) {
	unless ($old_ewas_names{$recent_ewas_name}) {
		foreach my $ewas_instance (@{$recent_ewas_names{$recent_ewas_name}}) {
			push (@new_ewas, $ewas_instance) unless $seen{$ewas_instance->db_id}++;
		}
	}
}

print "Second step for curators to EWASs mapping starts now\n\n";

open my $curator_new_ewas_fh, '>', "curator_newEWAS$recent_version.txt";
print $curator_new_ewas_fh "Curator\tnew_ewas_count\n\n";
print "Curator\tnew_ewas_count\n\n";

my %curator2new_ewas;
foreach my $ewas (@new_ewas) {
	my $ewas_description = $ewas->db_id . ":" . $ewas->displayName;
	my $curator;
	if ($ewas->created->[0] && $ewas->created->[0]->author->[0]) {
		if ($ewas->created->[0]->author->[0]->_displayName->[0]) {
			$curator = $ewas->created->[0]->author->[0]->_displayName->[0];
 		}
	}
	$curator ||= 'Unknown';
	
	push @{$curator2new_ewas{$curator}}, $ewas_description;
}

foreach my $curator (keys %curator2new_ewas){
	my $output = $curator . "\t" . scalar @{$curator2new_ewas{$curator}} . "\t" . join("\t",@{$curator2new_ewas{$curator}});
	print $curator_new_ewas_fh "$output\n";
	print "$output\n";
}

print $curator_new_ewas_fh "Total new EWASs: ". @new_ewas."\n";
print "Total new EWASs: ".@new_ewas."\n";

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

sub get_human_ewas_instances {
	my $dba = shift; 
	
	return get_instances($dba, 'EntityWithAccessionedSequence', '48887');
}

sub get_instances {
	my $dba = shift;
	my $class = shift;
	my $species = shift;
	
	return $dba->fetch_instance(
		-CLASS => $class,
		-QUERY => 
    	[
    		{
    			-ATTRIBUTE => 'species',
				-VALUE => [$species]
			}
		]
	);
}