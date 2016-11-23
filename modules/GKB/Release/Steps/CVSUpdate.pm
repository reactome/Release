package GKB::Release::Steps::CVSUpdate;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => "gkbdev" );
has '+passwords' => ( default => sub { [] } );
has '+user_input' => ( default => sub {{'key_pairs_exist' => {'query' => _get_key_pair_query() }}});
has '+directory' => ( default => "$release/cvs_update" );
has '+mail' => ( default => sub { 
					my $self = shift;
					return {
						'to' => '',
						'subject' => $self->name,
						'body' => '',
						'attachment' => ''
					};
				}
);
						
override 'run_commands' => sub {
	my ($self, $gkbdir) = @_;
    
    my $key_pairs_exist = $self->user_input->{'key_pairs_exist'}->{'response'} =~ /^y/;
    
    die "Key pairs must be created before the CVS updates can be run automatically" unless $key_pairs_exist;

    $self->cmd("Updating images directory",[
       ["perl update_images_directory.pl > $self->{name}.out 2> $self->{name}.err"],
       ["grep -v '^cvs update: move away .* it is in the way\$' $self->{name}.err | cat > $self->{name}.err"]
    ]);
};

sub _get_key_pair_query {
<<END;
Remote CVS update requires SSH authentication.  Are SSH key pairs for $user in place for the following client/server pairs?
    localhost and reactomerelease (unnecessary if localhost is reactomerelease)
    localhost and reactomeprd1
    localhost and reactomecurator
    reactomerelease and reactomecurator
    reactomeprd1 and reactomecurator
    reactomecurator and reactomecurator (required for passwordless CVS access)
(y/n)?
END
}

1;
