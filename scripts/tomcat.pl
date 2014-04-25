#!/usr/bin/perl -w
use strict;
use Getopt::Long;

# A script to monitor/restart reactome tomcat servers
# Sheldon McKay <sheldon.mckay@gmail.com>

# Threshold Paramaters
use constant MAXM => 8*1_073_741_824; # memory
use constant MAXP => 4;               # processes
use constant MAXC => 150;             # % CPU

# Other configuration
use constant PRODUCTION     => '/usr/local/reactomes/Reactome/website_3_0';
use constant PRODUCTION2    => '/data/reactomes/Reactome/website_3_0';
use constant PRODUCTION3    => '/usr/local/gkb/gkb_test';
use constant DEVELOPMENT    => '/usr/local/reactomes/Reactome/development';
use constant DEVELOPMENT2   => '/usr/local/gkbdev/tomcat';
use constant CURATOR        => '/usr/local/reactomes/Reactome/curator';
use constant FALLBACK       => '/usr/local/reactomes/Reactome/fallback';

# You need to be root to run this script!
die "Sorry, root permission required.\n" unless $> == 0;

my ($force,$help,$norestart);
GetOptions (
            "monitor"  => \$norestart, 
	    "force"    => \$force,
            "help"     => \$help)
    or die("Error in command line arguments\n");


chomp(my $timestamp = `date`);
report(usage()) && exit 0 if $help;


# This is a python script that digs deep to find memory usage.
# Install it if we do not have it already.
use constant MEM   => 'https://raw.github.com/pixelb/ps_mem/master/ps_mem.py';
unless (-e '/usr/local/bin/ps_mem.py') {
    report("No ps_mem.py detected.  I will install it now");
    sleep 1;
    my $murl = 'https://raw.github.com/pixelb/ps_mem/master/ps_mem.py';
    chdir '/usr/local/bin' or die $!;
    system "wget --no-check-certificate $murl";
    die "I can't seem to find ps_mem.py at $murl!\n" unless -e 'ps_mem.py';
    system "chmod 755 ps_mem.py";
}

# Are we on a dev or production server?
my $reactome_path;
my $fallback_path;
if (-d PRODUCTION || -d PRODUCTION2 || -d PRODUCTION3) {
    $reactome_path = PRODUCTION  if -d PRODUCTION;
    $reactome_path = PRODUCTION2 if -d PRODUCTION2;
    $reactome_path = PRODUCTION3 if -d PRODUCTION3;
    $fallback_path = FALLBACK    if -d FALLBACK;
}
elsif (-d DEVELOPMENT || -d DEVELOPMENT2) {
    $reactome_path = DEVELOPMENT if -d DEVELOPMENT;
    $reactome_path = DEVELOPMENT2 if -d DEVELOPMENT2;
}
elsif (-d CURATOR) {
    $reactome_path = CURATOR;
}
else {
    die "I'm not sure what kind of server environment this is. ",
    "I can't find reactome in the usual places\n";
}


handle_tomcat();

if ($fallback_path) {
    $reactome_path = $fallback_path;
    handle_tomcat()
}

# Checks to see how many tomcat processes there are, how much memory is used,
# and how much CPU is used.  If too much/many, restart
sub handle_tomcat {
    my $fallback = $fallback_path && $fallback_path eq $reactome_path;
    
    # Get the tomcat  process ids
    my @pids = $fallback ? get_fallback_pids() : get_tomcat_pids();

    # Get the tomcat resource usage
    my $mem  = 0;
    my $proc = 0;
    my $cpu_usage = 0;
    my ($k,$m,$g) = (1024,1_048_576,1_073_741_824);

    my $report_process = @pids > 1;
    for my $pid (@pids) {
	print STDERR `ps -o args -p $pid | tail -1` if $report_process;
	open IN, "ps_mem.py -p $pid |";
	my $localmem;
	while (<IN>) {
	    chomp;
	    next if /Private|\=|\-|^$/;
	    s/^\s+|\s+$//g;
	    my ($num,$scale) = split;
	    my $factor = $scale eq 'KiB' ? $k : $scale eq 'MiB' ? $m : $g;
	    $num *= $factor;
	    $proc++;
	    $localmem += $num;
	}
	close IN;
	
	my $cpu = get_cpu_usage($pid);
	my $bytes = scale_num($localmem);
	$mem += $localmem;
	$cpu_usage += $cpu;
    }

    my $bytes = scale_num($mem);

    my $type = $fallback ? 'Fallback' : 'Main';
    my ($is,$s) = $proc > 1 ? ('are','es') : ('is','');
    report("$type Server: There $is $proc tomcat process$s using $bytes of memory and $cpu_usage% CPU.");
    
    my @reasons = ("Forced restart") if $force;
    push @reasons, "Too much memory usage ($bytes)" if $mem > MAXM;
    push @reasons, "Too much CPU usage" if $cpu_usage > MAXC;
    push @reasons, "Too many processes" if $proc > MAXP;
    push @reasons, "I could not find a running server" if !$proc;

    if (!$norestart && @reasons > 0) {
	report("I will now restart the $type tomcat server because:\n" . join("\n",@reasons));
	my $msg = "Tomcat $type restart MEM=$bytes PROCESSES=$proc CPU=$cpu_usage\%";
	system "echo '$msg' |mail -s 'TOMCAT RESTART ALERT' sheldon.mckay\@gmail.com";
	restart_tomcat($fallback);
    }
    #report("---\n");
}

sub report {
    my $message = shift || '';
    print "$timestamp $message\n";
}

sub usage {
q(Usage: ./server.pl [options]
 Options:
   m|monitor report resource usage only, no restart
   f|force   force server restart
   h|help    this message

  Examples:
     # Check to see if tomcat is behaving itself
     sudo ./tomcat.pl -m
     
     # Force restart of tomcat
     sudo ./tomcat.pl -f

     # Check resource usage, restart if required
     sudo ./tomcat.pl 
);
}

sub restart_tomcat {
    my $fallback = shift;
    stop_tomcat($fallback);
    start_tomcat($fallback);
}

sub start_tomcat {
    my $fallback = shift;
    my $tomcat_start = $fallback ? FALLBACK . '/apache-tomcat/bin/startup.sh'
                                 : $reactome_path . '/apache-tomcat/bin/startup.sh'; 
    die "I could not find $tomcat_start\n" unless -e $tomcat_start;

    system $tomcat_start;
}

sub stop_tomcat {
    my $fallback = shift;
    my @pids = $fallback ? get_fallback_pids() : get_tomcat_pids();
    my $type = $fallback ? 'Fallback' : 'Main';
    for my $pid (@pids) {
	#report("Killing $type tomcat PID $pid");
        system "kill -9 $pid";
    }
    sleep 3;
}

sub get_tomcat_pids {
    my @paths = (PRODUCTION,PRODUCTION2,PRODUCTION3,DEVELOPMENT,CURATOR);
    my @pids;
    for (@paths) {
	push @pids, grep {chomp && !/grep|ps aux|tomcat.pl/ && s/^\S+\s+(\d+).+$/$1/} `ps aux |grep tomcat |grep $_`;
    }
    return @pids;
}

sub get_fallback_pids {
    my $fallback = FALLBACK;
    my @pids = grep {chomp && !/grep|ps aux/ && s/^\S+\s+(\d+).+$/$1/} `ps aux |grep tomcat |grep $fallback`;
}

sub get_cpu_usage {
    my $pid = shift;
    my ($cpu) =  map {(split)[2]} grep {!/USER/} `ps u -p $pid`;
    return $cpu;
}

sub scale_num {
    my $num = shift;
    my ($k,$m,$g) = (1023,1_048_575,1_073_741_823);
    my ($unit,$denom)  = $num > $g ? ('G',++$g) : $num > $m ? ('M',++$m) : ('K',++$k);
    my $bytes = sprintf('%.1f',$num/$denom) . " ${unit}Byte"; 
}
