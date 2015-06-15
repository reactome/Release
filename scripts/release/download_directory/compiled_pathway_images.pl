#!/usr/bin/perl
use strict;
use warnings;

use lib '/usr/local/gkb/modules';

use autodie qw/:all/;
use Net::OpenSSH;

use GKB::Config;
use GKB::DBAdaptor;

use Log::Log4perl qw/get_logger/;
Log::Log4perl->init(\$LOG_CONF);
my $logger = get_logger(__PACKAGE__);

my $cvs_host = 'reactomecurator.oicr.on.ca';
my $cvs_images_dir = '/usr/local/cvs_repository/GKB/website/images/Pathway_Illustrations';
my $images_dir = '/usr/local/reactomes/Reactome/production/GKB/website/images';

my $ssh = Net::OpenSSH->new($cvs_host);
die $ssh->error if $ssh->error;

my @cvs_pathway_illustration_files = $ssh->capture("find $cvs_images_dir -maxdepth 1 -type f -printf '%f\n'");

my @figure_instances = @{get_dba()->fetch_instance(-CLASS => 'Figure')};

(my $outfile = $0) =~ s/\.pl/\.tar/;
if (-e "$outfile.gz") {
    system("mv --backup=numbered $outfile.gz $outfile.bak.gz");
}
system("touch $outfile");
my %seen;
foreach my $figure_instance (@figure_instances) {
    my $url = $figure_instance->url->[0];
    
    unless ($url) {
	$logger->info($figure_instance->db_id . " has no url\n");
	next;
    }
    
    next unless $url =~ /Pathway_Illustrations/;
    
    (my $file_path = $url) =~ s/figures/$images_dir/;
    my ($file) = $url =~ /.*\/(.*)$/;
    
    unless (grep /$file/, @cvs_pathway_illustration_files) {
	$logger->info("$file not in cvs\n");
	next;
    }
    
    unless (-e $file_path) {
	$logger->warn("$file_path doesn't exist\n");
	next;
    }
    
    next if $seen{$file_path}++;
    
    system("tar --append --file=$outfile -C $images_dir/Pathway_Illustrations $file");
}
system("gzip $outfile");
$logger->info("$0 has finished\n");

sub get_dba {
    return GKB::DBAdaptor->new (
	-user => $GKB::Config::GK_DB_USER,
	-pass => $GKB::Config::GK_DB_PASS,
	-dbname => $GKB::Config::GK_DB_NAME
    );
}