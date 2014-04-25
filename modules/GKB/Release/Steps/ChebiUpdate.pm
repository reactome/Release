package GKB::Release::Steps::ChebiUpdate;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => "gkbdev" );
has '+passwords' => ( default => sub { ['mysql'] } );
has '+directory' => ( default => "$release/chebi_update" );
has '+mail' => ( default => sub { 
					my $self = shift;
					return {
						'to' => 'curation',
						'subject' => $self->name,
						'body' => "",
						'attachment' => $self->directory . "/chebi.wiki"
					};
				}
);

override 'run_commands' => sub {
	my ($self, $gkbdir) = @_;

    cmd("Running Chebi script",
    	[
		    #["cvs up improve_chebi_ids.pl"],
 			["perl improve_chebi_ids.pl -db $gkcentral -host $gkcentral_host -user $user -pass $pass > improve_chebi_ids.out"]
    	]
 	);
};

1;
