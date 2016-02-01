package GKB::Release::Steps::TOC;

use DateTime;
use File::stat;
use Net::SFTP::Foreign;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+passwords' => ( default => sub { ['sudo'] } );
has '+directory' => ( default => "$release/toc" );
has '+mail' => ( default => sub { 
					my $self = shift;
					return {
						'to' => '',
						'subject' => $self->name,
						'body' => "",
						'attachment' => ""
					};
				}
);

override 'run_commands' => sub {
    my ($self, $gkbdir) = @_;

    my $ssh = $self->get_server();
    my $website_dir = replace_gkb_alias_in_dir($website, $gkbdir);
    my $livedb = 'gk_current';
  
    if ($gkbdir eq "gkbdev") {
    	$self->cmd("Removing cached frontpage files",
    	    [
                ["echo $sudo | sudo -S rm -rf $website_dir/html/img-fp/$db"],
                ["echo $sudo | sudo -S rm $website_dir/html/img-fp/$livedb"]
    	   ]
    	);
    
    	$self->cmd("Creating table of contents",
    	    [
                ["mkdir -p $website_dir/html/img-fp"],
                ["$website_dir/cgi-bin/toc DB=$db"],
            	["$website_dir/cgi-bin/doi_toc DB=$db"],
                ["ln -s $website_dir/html/img-fp/$db $website_dir/html/img-fp/$livedb"],
            	["echo $sudo | sudo -S chown -R \${USER}:www-data $website_dir/html/img-fp/$db/toc"],
            	["echo $sudo | sudo -S chown -R \${USER}:www-data $website_dir/html/img-fp/$db/doi_toc"]
    	    ]
    	);
    } elsif ($gkbdir eq "gkb") {
        $self->cmd("Creating table of contents",
            [
                ["echo $sudo | sudo -S rm -rf $website_dir/html/img-fp/$livedb"],
            	["$website_dir/cgi-bin/toc DB=$livedb"],
                ["$website_dir/cgi-bin/doi_toc DB=$livedb"],
                ["echo $sudo | sudo -S chown -R \${USER}:www-data $website_dir/html/img-fp/$livedb/*toc"]
            ],
            {"ssh" => $ssh}
        );
    }
};

override 'post_step_tests' => sub {
    my $self = shift;
    
    my @errors = super();
    push @errors, $self->_check_file_modification_times();
    return @errors;
};

sub get_server {
    my $self = shift;
    
    return $self->gkb eq 'gkb' ? $live_server : undef;
}

sub _check_file_modification_times {
    my $self = shift;
    
    my $TIME_ZONE = 'UTC';
    my $MAX_SINCE_MODIFIED = 2; # Minutes
    
    my @errors;
    my $website_dir = replace_gkb_alias_in_dir($website, $self->gkb);
    foreach my $file (map {"$website_dir/html/img-fp/$_"} ("$db/doi_toc", "$db/toc", 'gk_current')) {
        my $modification_time = $self->_get_modification_time($file, \@errors);
        next unless $modification_time;
        my $seconds_since_modification = time - $modification_time;
        my $minutes_since_modification = $seconds_since_modification / 60;
        if ($minutes_since_modification > $MAX_SINCE_MODIFIED) {
            my $datetime = DateTime->from_epoch( epoch => $modification_time );
            $datetime->set_time_zone($TIME_ZONE);
            push @errors, "$file hasn't been modified since " .
            $datetime->ymd('/') . ' ' . $datetime->hms . " ($TIME_ZONE)";
        }
    }
    return @errors;
}

sub _get_modification_time {
    my $self = shift;
    
    my $file = shift;
    my $errors = shift;
    
    my $remote_server = $self->get_server();
    if ($remote_server) {
        my $sftp = Net::SFTP::Foreign->new($remote_server);
        my $attributes = $sftp->stat($file);
        unless ($attributes) {
            push @{$errors}, "Unable to get modification time of $file from $remote_server";
            return;
        }
        return $attributes->mtime;
    }
    
    my $stat = stat($file);
    unless ($stat) {
        push @{$errors}, "Unable to get modification time of $file";
        return;
    }
    return $stat->mtime;
}
1;
