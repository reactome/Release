package GKB::Release::Steps::RestartTomcat;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

#has '+gkb' => ();
has '+passwords' => ( default => sub { ['sudo'] } );
has '+directory' => ( default => "$release/restart_tomcat");
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
    
    $self->cmd("Restarting tomcat", [["ssh $live_server 'echo $sudo | sudo -S /etc/init.d/tomcat7 restart'"]]);
};
    
1;
