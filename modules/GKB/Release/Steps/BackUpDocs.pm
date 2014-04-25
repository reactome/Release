package GKB::Release::Steps::BackUpDocs;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => "gkbdev" );
has '+passwords' => ( default => sub { [] } );
has '+directory' => ( default => "$release" );
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
    
    cmd("Backing up usersguide and release SOP",
    	[
    		["perl run_download_html_from_wiki.pl -wiki_url \"http://wiki.reactome.org/index.php/Usersguide\" -target_dir ../../website/html/userguide"],
    		["perl run_download_html_from_wiki.pl -wiki_url \"http://devwiki.reactome.org/index.php/Release_SOP\" -target_dir ../../docs/SOP"]
		]
	);
};

1;
