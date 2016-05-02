#!/usr/bin/perl
use strict;
use warnings;

use lib '/usr/local/gkb/modules';

use Data::Dumper;
use Getopt::Long;
use Readonly;
use Net::OpenSSH;
use Sys::Hostname::Long;
use Tie::IxHash;

use GKB::Config;
use Log::Log4perl qw/get_logger/;
Log::Log4perl->init(\$LOG_CONF);
my $logger = get_logger(__PACKAGE__);

my ($user, $images_dir);
&GetOptions(
    'user=s' => \$user,
    'dir=s' => \$images_dir
);

$user ||= getpwuid($<);
$images_dir ||= '/usr/local/gkb/website/images';

Readonly my $localhost => hostname_long;
Readonly my $live_server => 'reactomeprd1.oicr.on.ca';
Readonly my $release_server => 'reactomerelease.oicr.on.ca';
Readonly my $curator_server => 'reactomecurator.oicr.on.ca';

my %host_key_pairs;
my $t = tie(%host_key_pairs, 'Tie::IxHash',
    'localhost' => [$live_server, $release_server, $curator_server],
    $release_server => [$curator_server],
    $live_server => [$curator_server],
    $curator_server => [$curator_server]
);

my %unauthorized_key_pairs = get_unauthorized_key_pairs(\%host_key_pairs, $user);
if (scalar keys %unauthorized_key_pairs) {
    report_unauthorized_key_pairs(%unauthorized_key_pairs);
    exit 1;
}

foreach my $server (@{$host_key_pairs{'localhost'}}) {
    my $ssh;
    unless ($server eq $localhost) {
        $ssh = Net::OpenSSH->new("$user\@$server");
        if ($ssh->error) {        
            $logger->error("Unable to establish connection to $server: $ssh->{error}\n");
            next;
        }
    }
    my @cvs_update_results = update_cvs($ssh, $images_dir);
    my @cvs_conflicts = get_cvs_conflicts(@cvs_update_results);
    if (@cvs_conflicts) {
        my @remaining_cvs_conflicts = fix_cvs_conflicts($ssh, $images_dir, \@cvs_conflicts);
        if (@remaining_cvs_conflicts) {
            $logger->error("Unresolvable conflicts during CVS update of $images_dir on $server\n");
            $logger->error($_) foreach @cvs_conflicts;
            exit 1;
        }
    }
    $logger->info("CVS update of $images_dir on $server successful\n");
}

sub get_unauthorized_key_pairs {
    my $key_pairs = shift;
    
    my %unauthorized_key_pairs;
    my %tested_pairs;
        
    foreach my $client (keys %{$key_pairs}) {
        foreach my $server (@{$key_pairs->{$client}}) {
            next if $tested_pairs{$client}{$server}++;
            if ($client eq 'localhost' || $client eq $localhost) {
                next if ($localhost eq $server) && ($localhost ne $curator_server);
                my $ssh = Net::OpenSSH->new("$user\@$server");
                if ($ssh->error) {
                    push @{$unauthorized_key_pairs{$localhost}}, $server;
                }
            } else {
                my $ssh = Net::OpenSSH->new("$user\@$client");
                unless ($ssh->error) {
                    my $connection_success = $ssh->system("ssh -q $user\@$server exit");
                    unless ($connection_success) {
                        push @{$unauthorized_key_pairs{$client}}, $server;
                    }
                }
            }
        }
    }
    
    return %unauthorized_key_pairs;
}

sub report_unauthorized_key_pairs {
    my %unauthorized_key_pairs = @_;
    
    my $logger = get_logger(__PACKAGE__);
    
    $logger->error("The following servers require an SSH key pair before proceeding:\n");
    foreach my $client (keys %unauthorized_key_pairs) {
        foreach my $server (@{$unauthorized_key_pairs{$client}}) {
            $logger->error("$client as the client and $server as the server\n");
        }
    }
}

sub update_cvs {
    my $ssh = shift;
    my $directory = shift;
    
    my $logger = get_logger(__PACKAGE__);
    
    $logger->info("Doing a CVS update for $directory on " . ($ssh ? $ssh->get_host : $localhost) . "\n");
    my $update_command = "cd $directory; cvs -q up";
    if ($ssh) {
        return $ssh->capture($update_command);
    } else {
        return `$update_command`;
    }
}

sub get_cvs_conflicts {
    my @cvs_update_results = @_;
    
    my @cvs_conflicts = grep /^C /, @cvs_update_results;
    foreach (@cvs_conflicts) {
        chomp;
        s/^C //;
    }
    
    return @cvs_conflicts;
}

sub fix_cvs_conflicts {
    my $ssh = shift;
    my $directory = shift;
    my $cvs_conflicts = shift;
    
    my $logger = get_logger(__PACKAGE__);
    
    foreach my $cvs_conflict (@{$cvs_conflicts}) {
        $logger->info("Backing up $images_dir/$cvs_conflict\n");
        my $add_bak_extension_command = "mv $images_dir/$cvs_conflict $images_dir/$cvs_conflict.bak";
        if ($ssh) {
            $ssh->system($add_bak_extension_command);
        } else {
            system($add_bak_extension_command);
        }        
    }
    
    my @cvs_update_results = update_cvs($ssh, $directory);
    my @cvs_conflicts = get_cvs_conflicts(@cvs_update_results);
    
    return @cvs_conflicts;
}