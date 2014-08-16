package GKB::Release::Steps::OrthoInference;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => "gkbdev" );
has '+passwords' => ( default => sub { ['mysql'] } );
has '+directory' => ( default => "$release/orthoinference" );
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
    
    cmd("Creating orthopredictions and backing up database",
    	[
            ["mkdir -p $version"],
            ["perl wrapper_ortho_inference.pl -r $version -user $user -pass $pass > $version/wrapper_ortho_inference.out"],
            ["mysqldump --opt -u$user -p$pass $db > $db\_after_ortho.dump"]
	]
    );
};

1;
