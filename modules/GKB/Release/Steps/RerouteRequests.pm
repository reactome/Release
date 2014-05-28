package GKB::Release::Steps::RerouteRequests;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

#has '+gkb' => ();
has '+passwords' => ( default => sub { ['sudo'] } );
has '+directory' => ( default => "$release/reroute_requests" );
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
    
    my $port = ($gkbdir eq "gkb_prod" ) ? 8000 : 80;
    my $config_file = "/usr/local/gkb_test/website/conf/httpd.conf";
    $config_file = "$website/conf/httpd.conf"; # For testing only
    
    cmd("Rerouting requests to port $port", [["perl rerouterequests.pl $port $config_file $live_server"]]);    
    cmd("Restarting apache", [["echo $sudo | sudo -S /etc/init.d/apache2 restart"]], {"ssh" => $live_server});
};
  
1;
