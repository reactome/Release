package GKB::Release::Steps::Myisam;

use Capture::Tiny qw/:all/;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => "gkbdev" );
has '+passwords' => ( default => sub { ['mysql'] } );
has '+directory' => ( default => "$release/myisam" );
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

    $self->cmd("Converting database to myisam",[["perl innodb2myisam.pl -user $user -pass $pass -dbfrom $slicedb -dbto $slicedb\_myisam > myisam.out 2> myisam.err"]]);
};

override 'post_step_tests' => sub {
    my ($self) = shift;
    
    my @errors = super();
    my $myisam_db_error = _check_myisam_db_exists();
    push @errors, $myisam_db_error if $myisam_db_error;

    return @errors;
};

sub _check_myisam_db_exists {
    return capture_stderr {
        system("mysql -u$user -p$pass -e 'use $slicedb\_myisam'");
    };
}

1;
