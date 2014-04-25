package GKB::Release::Steps::SetEmailReminders;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => "gkbdev" );
has '+passwords' => ( default => sub { ['sudo'] } );
has '+directory' => ( default => "$release/set_email_reminders" );
has '+mail' => ( default => sub { 
					my $self = shift;
					return {
						'to' => 'curation',
						'subject' => $self->name,
						'body' => "Hi Lisa,\n\nCould you please check the attached file to ensure the days are correct for the e-mails (the first four fields are minute hour day month)",
						'attachment' => "cron.txt"
					}
				}
);

override 'run_commands' => sub {
    my ($self, $gkbdir) = @_;
    
    my $release = prompt("Please enter next release date from the internal calendar as yyyymmdd (e.g. 20100430):");
    $release =~ /(\d{4})(\d{2})(\d{2})/;
    $release = DateTime->new(
        year => $1,
        month => $2,
        day => $3
    );
    
    #Final slice date calculation
    my $finalslice = $release;
    $finalslice->subtract(days => 14);
    my $answer = "";
    $answer = prompt("Does the week of the release have a bank holiday in it? (y,n)") while $answer ne "n" && $answer ne "y";
    if ($answer eq "y") {
        $finalslice->add(days => 1);
    }
    my $fsday = $finalslice->day();
    my $fsmonth = $finalslice->month();
    my $fs = $finalslice->month_name(). " " . $finalslice->day();
    
    # Datafreeze date calculation
    my $update = $finalslice;
    $update->subtract(days => 8);
    my $datafreeze = $update;
    # $datafreeze->subtract(days => 1);
    my $dfday = $datafreeze->day();
    my $dfmonth = $datafreeze->month();
    my $df = $datafreeze->day_name() . ", " . $datafreeze->month_name() . " " . $datafreeze->day();
    
    # Review letter date calculation
    my $reviewletter = $datafreeze;
    $reviewletter->subtract(days => 21);
    my $rlday = $reviewletter->day();
    my $rlmonth = $reviewletter->month();
    
    # Review request date calculation
    my $reviewrequest = $reviewletter;
    $reviewrequest->subtract(days => 9 + 8 + 7);
    my $rrday = $reviewrequest->day();
    my $rrmonth = $reviewrequest->month();
    
    # Illustrator reminder date calculation
    my $illustrator = $reviewrequest;
    $illustrator->subtract(days => 7);
    my $iday = $illustrator->day();
    my $imonth = $illustrator->month();
    
    # Prepare cron job
    `echo 0 0 $dfday $dfmonth '*' perl /usr/local/gkbdev/scripts/releasemail.pl -df $version \'$df\' \'$fs\' > cron.txt`;
    `echo 0 0 $rrday $rrmonth '*' perl /usr/local/gkbdev/scripts/releasemail.pl -rr >> cron.txt`;
    `echo 0 17 $fsday $fsmonth '*' perl /usr/local/gkbdev/scripts/releasemail.pl -fs >> cron.txt`;
    `echo 0 0 $rlday $rlmonth '*' perl /usr/local/gkbdev/scripts/releasemail.pl -rl >> cron.txt`;
    `echo 0 0 $iday $imonth '*' perl /usr/local/gkbdev/scripts/releasemail.pl -i >> cron.txt`;
    `echo $sudo | sudo -S crontab -u release cron.txt`;
};

1;
