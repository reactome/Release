#!/usr/bin/perl -w
use strict;
use Getopt::Long;
use Data::Dumper;
use List::Util 'min';

# A script to monitor/restart the mysql server
# Sheldon McKay <sheldon.mckay@gmail.com>

# Threshold Paramaters
use constant MAXCPU => 200;
use constant MAXCON => 50;

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

my @connections;
my @cpu_usage;
my $no_mysql;
for my $it (1..5) {
    my $cpu_usage = 0;
    my $proc = my @pids = get_mysql_pids();
    unless ($proc) {
	warn "No MYSQL!";
	$no_mysql++;
	last;
    }
    #my $report_process = @pids > 1;
    for my $pid (@pids) {
	#print STDERR `ps -o args -p $pid | tail -1` if $report_process;
	my $cpu = get_cpu_usage($pid);
	$cpu_usage += $cpu;
    }
    
    my $connections = get_mysql_connections();

    my ($is,$s) = @pids > 1 ? ('are','es') : ('is','');

    report("MySQL Server: $is $proc process$s; $cpu_usage\% CPU; $connections active connections.");
    
    if ($connections > MAXCON || $cpu_usage > MAXCPU) {
	push @connections, $connections;
	push @cpu_usage, $cpu_usage;
	sleep 60 unless $it == 2;
	chomp($timestamp = `date`);
    } 
    else {
	@connections = ();
	@cpu_usage   = ();
	last;
    }
}


my @reasons = ("Forced restart") if $force;

unless ($no_mysql) {
    if (@connections == 2) {
	my $mincon = min(@connections);
	my $mincpu = min(@cpu_usage);
	my $maxcon = MAXCON;
	my $maxcpu = MAXCPU;
	
	if ($mincon > MAXCON) {
	    push @reasons, "sustained number of connection > $maxcon over 5 minutes";
	}
	
	if ($mincpu > MAXCPU) {
	    push @reasons, "sustained CPU usage > $maxcpu over 5 minutes";
	}
    }
}
else {
    push @reasons, "No mysql server running";
}


if (!$norestart && @reasons > 0) {
    restart_mysql(@reasons);
}

my $proc = get_mysql_pids();
unless ($proc) {
    restart_mysql("No mysql server restart");
}


sub restart_mysql {
    my @reasons = @_;
    my $reasons = join("\n",@reasons);
    report("I will now try to restart the mysql server because:\n$reasons\n");
    my $log = "/tmp/mysql_log$$.txt";
    system "echo '$timestamp' > $log";
    system "/etc/init.d/mysql restart >>$log 2>&1";
    system "cat $log";
    system "cat $log | mail -s 'MYSQL RESTART ALERT' sheldon.mckay\@gmail.com";
    #unlink $log;
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
	my ($pid) = (split)[1];
	push @pids, $pid;
    }
    return @pids ? @pids - 1 : @pids;
}

sub get_mysql_pids {
    open PID, "ps aux |grep mysqld |grep 3306 | grep -v grep |" or die $!;
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
