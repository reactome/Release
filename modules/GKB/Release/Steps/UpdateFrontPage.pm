package GKB::Release::Steps::UpdateFrontPage;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ();
has '+passwords' => ();
has '+user_input' => ( default => sub { {'release_date' => {'query' => 'Please enter the date of the current release as yyyy-mm-dd:'}} });
has '+directory' => ( default => "$release/update_front_page" );
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

    my $release_date = $self->user_input->{'release_date'}->{'response'};
    $self->cmd("Updating Reactome version and release date front page across all servers",
        [["perl update_front_page.pl -new_version $version -new_release_date $release_date > update_front_page.out 2> update_front_page.err"]]
    );
};

1;
