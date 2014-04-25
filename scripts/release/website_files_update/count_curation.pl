 #!/usr/local/bin/perl  -w                                                                                                                                                          
use strict;

use lib "$ENV{HOME}/bioperl-1.0";
use lib "$ENV{HOME}/GKB/modules";
use GKB::ClipsAdaptor;
use GKB::DBAdaptor;
use Data::Dumper;
use Getopt::Long;
 
use GKB::Utils_esther;
use GKB::Utils;

use GKB::Release::Utils;
use GKB::Release::Config;

print 'Enter recent test slice version: (e.g. 39, 39a, etc) ';
my $ver = <STDIN>;
chomp $ver;

print 'Enter previous test slice version: (e.g. 38) ';
my $prevver = <STDIN>;
chomp $prevver;

my $recent = 'test_slice_'.$ver;

my $db = $recent;

# my $user = `whoami`;
# chomp $user;

#my $host = `hostname`;
#chomp $host;
my $host='localhost';

my $pass = prompt("Enter your mysql password: ", 1);

print 'recent db is '.$recent."\n";

my $out = "newproteins$ver.txt";
open(OUTPUT,">$ENV{HOME}/$out") or die;

my $dba_new = GKB::DBAdaptor->new(
     -dbname => $db,
     -user   => $user,
     -host   => $host, # host where mysqld is running                                                                                                                              
     -pass   => $pass,
     -port   =>  '3306'
);
	 

my $human = '48887';

my $protein_class = &GKB::Utils::get_reference_protein_class($dba_new);

my $proteinnew = $dba_new->fetch_instance(
	-CLASS => $protein_class,
    -QUERY => 
    	[
    		{
    			-ATTRIBUTE => 'species',
            	-VALUE => [$human]
			}
		]
);

my $previous = 'test_slice_' . $prevver;

print 'previous db is '.$previous."\n";
$db = $previous;
 
my $dba_old = GKB::DBAdaptor->new(
     -dbname => $db,
     -user   => $user,
     -host   => $host, # host where mysqld is running                                                                                                                              
     -pass   => $pass,
     -port   =>  '3306'
);
 
$protein_class = &GKB::Utils::get_reference_protein_class($dba_old);

my $proteinold = $dba_old->fetch_instance(
	-CLASS => $protein_class,
    -QUERY => 
    	[
    		{
    			-ATTRIBUTE => 'species',
                -VALUE => [$human]

			}
		]
);
 
my (%hashnew, %hashold, $count, $count2, @ina);
 
my %seen = ();
 
foreach (@{$proteinnew}){
 	if($_->identifier->[0]){
		$hashnew{$_->identifier->[0]} = 1;
	  	print OUTPUT $_->identifier->[0]."\n";
 	}
}

print 'Total Refpepseq in $recent is = ';
print @{$proteinnew}."\n";
 
print 'new slice total = ';
 
my $total = keys %hashnew;
print $total."\n";

foreach (@{$proteinold}){
	$hashold{$_->identifier->[0]} = 1;
}
 
print 'old slice total = ';
 
$total = keys %hashold;
print $total."\n";
 
foreach my $key (keys %hashnew) {
	unless ($hashold{$key}) {
		#print OUTPUT $key."\n";
		push (@ina, $key) unless $seen{$key}++;
		$count++;
	}
}

print 'new proteins in the recent release: ';
print @ina."\n";

#print scalar(keys %hashnew)."\n";


print "Second step for curators to proteins mapping starts now\n\n";
my $out2 = qq/curator_newproteins_new$ver/;
open (OUTPUT2,">$ENV{HOME}/$out2") or die;

my $pro = $dba_new->fetch_instance(
	-CLASS => 'ReferenceGeneProduct',
    -QUERY => 
    	[
    		{
    			-ATTRIBUTE => 'identifier',
                -VALUE => \@ina
            }
		]
);

#print scalar(@{$pro})."\n";
print OUTPUT2 "Curator\tnew_protein_count\n\n";
print "Curator\tnew_protein_count\n\n";

my %hash;
  
my @c = qw/Peter Bijay Phani Lisa Marc Bruce Mike Karen Marija Mark Robin Steve Veronica Other Unknown/;

foreach (@c) {
	$hash{$_} = 0;
}

%seen = ();

my @gg;

foreach (@{$pro}) {
	my $ref1 = $dba_new->fetch_referer_by_instance($_);
	
	if (@{$ref1}) {
		my @ewas = ();
		my @ewas_all = grep {$_->is_a('EntityWithAccessionedSequence')} @{$ref1};
		
		foreach (@ewas_all) {
			push (@ewas, $_) unless $seen{$_->referenceEntity->[0]->identifier->[0]}++;
		}
		
		my $curator;
		foreach (@ewas) {
			if ($_->created->[0]  && $_->created->[0]->author->[0]) {
				#my $ewas_ref = $dba_new->fetch_referer_by_instance($ewas[0]);
				if ($_->created->[0]->author->[0]->_displayName->[0]) {
					$curator = $_->created->[0]->author->[0]->_displayName->[0];
 				}
				
				$count2++;
				
				if ($curator =~ /ustachi/) {
					$hash{'Peter'}++;
				} elsif ($curator =~ /Rothfels/) {
					$hash{'Karen'}++;
				} elsif ($curator =~ /Gilles/) {
					$hash{'Marc'}++;
				} elsif ($curator =~ /Matthe/) {
					$hash{'Lisa'}++;
				} elsif ($curator =~ /Jass/) {
					$hash{'Bijay'}++;
				} elsif ($curator =~ /Milacic/) {
					$hash{'Marija'}++;					
				} elsif ($curator =~ /Williams/) {
					$hash{'Mark'}++;
				} elsif ($curator =~ /Haw/) {
					$hash{'Robin'}++;
				} elsif ($curator =~ /May/) {
					$hash{'Bruce'}++;
				} elsif ($curator =~ /Garap/) {
					$hash{'Phani'}++;
				} elsif ($curator =~ /Jupe/) {
					$hash{'Steve'}++;
				} elsif ($curator =~ /Caudy/) {
					$hash{'Mike'}++;
				} elsif ($curator =~ /Shamovs/) {
					$hash{'Veronica'}++;
				} elsif ($curator) {
					$hash{'Other'}++;
				} else {
					$hash{'Unknown'}++;
					#print OUTPUT2 'Missing created attribute value or unknown curator: '.$_->db_id."\t".$_->referenceEntity->[0]->identifier->[0]."\n";
					#print 'Missing created attribute value or unknown: '.$_->db_id."\t".$_->referenceEntity->[0]->identifier->[0]."\n";
					$count2++;
				}
 			} else{
	    		$hash{'Unknown'}++;
				#print OUTPUT2 'Missing created attribute value or unknown curator: '.$_->db_id."\t".$_->referenceEntity->[0]->identifier->[0]."\n";
				#print 'Missing created attribute value or unknown: '.$_->db_id."\t".$_->referenceEntity->[0]->identifier->[0]."\n";
				$count2++;
			}
		}
	}
}

print OUTPUT2 "\n";

foreach my $key (keys %hash){
	print OUTPUT2 $key."\t".$hash{$key}."\n";
	print $key."\t".$hash{$key}."\n";
}

print OUTPUT2 "Total new proteins: ".@ina."\n";
print  "Total new proteins: ".@ina."\n";
