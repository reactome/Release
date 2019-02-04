package GKB::Release::Steps::UpdateConfig;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+directory' => ( default => "$release/update_config_file" );
has '+mail' => ( default => sub { 
    my $self = shift;
    return {
        'to' => '',
        'subject' => $self->name,
        'body' => "",
        'attachment' => ""
    };
});
has '+user_input' => ( default => sub {{
    'last_release_date' => { 'query' => 'Enter the previous release date as yyyymmdd:' }
}});

override 'run_commands' => sub {
    my ($self, $gkbdir) = @_;

    my $host = $hosts{$gkbdir};

    my $last_release_date = $self->user_input->{'last_release_date'}->{'response'};
    my $config_path = "$gkbmodules/GKB";
    $self->cmd("Updating configuration file",[
        ["perl updateconfig.pl -database $db -last_release $last_release_date -host $host -config_path $config_path"]
    ]);
    
    if ($gkbdir eq "gkbdev") {	    
    	$self->mail->{'to'} = 'curation';
    	$self->mail->{'body'} = "The stats file can now be created"; 
    }
};

1;
