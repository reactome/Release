#!/usr/bin/perl -w
use common::sense;
use Getopt::Long;
use Data::Dumper;
use List::Util 'min';
use Try::Tiny;

# A script to monitor/restart the mysql server
# Sheldon McKay <sheldon.mckay@gmail.com>

use constant MAXCPU => 200;
use constant MAXCON => 150;
use constant PID => '/var/run/mysqld/mysqld.pid';

# You need to be root to run this script!
die "Sorry, root permission required.\n" unless $> == 0;

my $instances = get_running_instances();
warn ("$instances running instances\n")  if $instances && $instances > 1;

my ($force,$help,$norestart,$passwd);
GetOptions (
    "monitor"  => \$norestart, 
    "force"    => \$force,
    "help"     => \$help,
    "pass:s"   => \$passwd)
    or die("Error in command line arguments\n", usage());

chomp(my $timestamp = `date`);
report(usage()) && exit 0 if $help;
$passwd || die "You need to provide the mysql password\n", usage();


my @reasons;

my $mpid = get_mpid();
my $cpu_usage = get_cpu_usage($mpid);
my $connections;

try {
    $connections = get_mysql_connections();
} catch {
    push @reasons, "Unable to get number of connections";
    $connections = 0;
};

report("MySQL Server: $cpu_usage\% CPU; $connections active connections.");

if ($connections > MAXCON) {
    push @reasons, "Because $connections connections is too many";
}


if ($cpu_usage > MAXCPU) {
    push @reasons, "Too much CPU usage";
}

push @reasons, "Forced restart" if $force;



if (!$norestart && @reasons > 0) {
    restart_mysql(@reasons);
}

sub restart_mysql {
    my @reasons = @_;
    my $reasons = join("\n",@reasons);
    report("I will now try to restart the mysql server because:\n$reasons\n");
    my $log = "/tmp/mysql_log$$.txt";
    system "echo '$timestamp' > $log";

    my $mpid = get_mpid();
    print "I need to kill process $mpid\n";
    kill('KILL',$mpid);
    sleep 2;
#    start_mysql($log);
    $mpid = get_mpid();
    print "PMID is now $mpid\n";
}

sub get_mpid {
    open MPID, PID or die "Could not open "+PID+" $!\n";
    while (<MPID>) {
	chomp;
	return $_;
    }
}

sub start_mysql {
    my $log = shift;
    system "/etc/init.d/mysql start >> $log 2>&1";
    system "cat $log";
    system "cat $log | mail -s 'MYSQL RESTART ALERT' reactome-developer\@reactome.org";
    unlink $log;
}

sub usage {
q(Usage: ./mysql.pl [options]
 Options:
   m|monitor report resource usage only, no restart
   f|force   force server restart
   h|help    this message

  Examples:
     # Check to see if mysql is behaving itself
     sudo ./mysql.pl -m
     
     # Force restart of mysql
     sudo ./mysql.pl -f

     # Check resource usage, restart if required
     sudo ./mysql.pl 
);
}

sub get_mysql_connections {
    #print STDERR "mysqladmin -uroot -p$passwd processlist";
    my $connections = `mysqladmin -uroot -p$passwd processlist | wc -l`;
    chomp $connections;
    die unless (defined $connections) && $connections =~ /^\d+$/;
    
    return $connections;
}

sub get_cpu_usage {
    my $pid = shift;
    my ($cpu) =  map {(split)[2]} grep {!/USER/} `ps u -p $pid`;
    return $cpu;
}

sub get_running_instances {
    my $script = $0;
    chomp(my $instance = `basename $script`);
    open PID, "ps aux |grep  $instance | grep -v grep |" or die $!;
    my @pids;
    while (<PID>) {
	print "PROCESS: $_";
	my ($pid) = (split)[1];
	push @pids, $pid;
    }
    return @pids ? @pids - 1 : @pids;
}

sub get_mysql_pids {
    open MPID, "ps aux |grep mysqld | grep 'port=3306' |";
    my @pids;
    while (<PID>) {
	/mysqld/ || next;
        my ($pid) = (split)[1];
        push @pids, $pid;
    }
    return @pids;
}

sub report {
    my $message = shift || '';
    print "$timestamp $message\n";
}
