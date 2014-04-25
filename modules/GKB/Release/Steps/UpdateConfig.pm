package GKB::Release::Steps::UpdateConfig;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

#has '+gkb' => ();
#has '+passwords' => ();
has '+directory' => ( default => "$release/update_config_file" );
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

my $prd = prompt('Enter the previous release date as yyyymmdd:');

override 'run_commands' => sub {
    my ($self, $gkbdir) = @_;

    my $host = $hosts{$gkbdir};
    my $path = "/usr/local/$gkbdir/modules/GKB";

    cmd("Updating configuration file",[["perl updateconfig.pl -version $version -lastrelease $prd -host $host -configpath $path"]]);
    
    if ($gkbdir eq "gkbdev") {	    
    	$self->mail->{'to'} = 'curation';
    	$self->mail->{'body'} = "The stats file can now be created"; 
    }
};

1;
