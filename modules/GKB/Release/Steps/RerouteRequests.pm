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
has '+user_input' => (default => sub {{'fallback' => {'query' => 'Redirect requests to fallback (y/n):'}}});

override 'run_commands' => sub {
    my ($self, $gkbdir) = @_;
    
    my $config_file = "/usr/local/gkb/website/conf/httpd.conf";
    my $fallback = $self->user_input->{'fallback'}->{'response'} =~ /^y/ ? 1 : 0;
    
    cmd("Rerouting requests", [["perl rerouterequests.pl $config_file $live_server $fallback"]]);    
    cmd("Restarting apache", [["echo $sudo | sudo -S /etc/init.d/apache2 restart"]], {"ssh" => $live_server});
};
  
1;
