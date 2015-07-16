package GKB::Release::Steps::UncuratedProteins;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => "gkbdev" );
has '+passwords' => ( default => sub { [] } );
has '+directory' => ( default => "$release/website_files_update" );
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
    
    my $host = $self->host;
	
    $self->cmd("Generating uncurated proteins file",[["perl uncurated_proteins.pl"]]);
};

1;
