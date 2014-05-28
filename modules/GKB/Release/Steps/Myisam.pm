package GKB::Release::Steps::Myisam;

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

    cmd("Converting database to myisam",[["perl innodb2myisam.pl -pass $pass -dbfrom test_slice_$version -dbto test_slice_$version\_myisam"]]);
};

1;
