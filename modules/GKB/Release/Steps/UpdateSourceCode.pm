package GKB::Release::Steps::UpdateSourceCode;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => "gkbdev" );
has '+passwords' => ( default => sub { [] });
has '+directory' => ( default => "$release/update_source_code" );
has '+mail' => ( default => sub { 
					my $self = shift;
					return {
						'to' => 'curation',
						'subject' => $self->name,
						'body' => "",
						'attachment' => ""
					};
				}
);

override 'run_commands' => sub {
    my ($self, $gkbdir) = @_;
    
        
    # The command is run on the live server when $gkbdir is gkb
    my $ssh_server = ($gkbdir eq "gkb") ? $live_server : '';
    my $git_repo = '/usr/local/gkb';
   
    cmd("Updating source code from git",[["perl git_update.pl $git_repo $ssh_server"]]);
};

1;
