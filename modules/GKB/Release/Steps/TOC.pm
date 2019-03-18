package GKB::Release::Steps::TOC;

use DateTime;
use File::stat;

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
});

override 'run_commands' => sub {
    my ($self, $gkbdir) = @_;

    my $img_fp_dir = "$website_static/cgi-tmp/img-fp";

    $self->cmd("Removing cached frontpage files",
        [
            ["echo $sudo | sudo -S rm -rf $img_fp_dir/$db"],
            ["mv --backup=numbered $img_fp_dir/$live_db $img_fp_dir/$live_db.old"]
        ]
    );

    $self->cmd("Creating table of contents",
        [
            ["mkdir -p $img_fp_dir"],
            ["$website_static/cgi-bin/toc DB=$live_db"],
            ["$website_static/cgi-bin/doi_toc DB=$live_db"],
            ["echo $sudo | sudo -S chown -R www-data:$reactome_unix_group $img_fp_dir/$live_db/*toc"],
            ["cp -R $img_fp_dir/$live_db $img_fp_dir/$db"]
        ]
    );
};

override 'post_step_tests' => sub {
    my $self = shift;

    my @errors = super();
    push @errors, $self->_check_file_modification_times();
    return @errors;
};

sub _check_file_modification_times {
    my $self = shift;

    my $live_db = 'gk_current';

    my $TIME_ZONE = 'UTC';
    my $MAX_MINUTES_SINCE_MODIFIED = 2;

    my @errors;
    foreach my $file (map {"$website_static/cgi-tmp/img-fp/$_"} ("$live_db/doi_toc", "$live_db/toc")) {
        my $modification_time = $self->_get_modification_time($file, \@errors);
        next unless $modification_time;
        my $seconds_since_modification = time - $modification_time;
        my $minutes_since_modification = $seconds_since_modification / 60;
        if ($minutes_since_modification > $MAX_MINUTES_SINCE_MODIFIED) {
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

    my $stat = stat($file);
    if (!$stat) {
        push @{$errors}, "Unable to get modification time of $file";
        return;
    }
    return $stat->mtime;
}
1;
