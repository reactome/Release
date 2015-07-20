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
    
    my $args = "-repo /usr/local/gkb";
    $args .= " -host $live_server" if $gkbdir eq "gkb";
   
    $self->cmd("Updating source code from git",[["perl git_update.pl $args"]]);
};

1;
