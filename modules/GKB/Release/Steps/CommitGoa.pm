package GKB::Release::Steps::CommitGoa;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => "gkbdev" );
has '+passwords' => ( default => sub { ['mysql'] } );
has '+directory' => ( default => "$release/commit_goa" );
has '+mail' => ( default => sub { 
					my $self = shift;
					return {
						'to' => 'automation',
						'subject' => $self->name,
						'body' => "",
						'attachment' => "Reactome2GoV$version"
					};
				}
);

override 'run_commands' => sub {
    my ($self, $gkbdir) = @_;
    
    my $host = $self->host;
    
    my $go_submission = "$release/goa_prepare/GO_submission/go/gene-associations/submission/gene_association.reactome";
    $self->cmd("Committing gene association file to GO SVN",
    	[
			["rm -f $go_submission"],
    		["mv $release/goa_prepare/gene_association.reactome $go_submission"],
    	    ["gzip -f $go_submission"],
    	    ["svn commit -m \"Reactome release $version\" $go_submission.gz"]
    	]
    );
	
    my @args = ("-user", $user, "-pass", $pass, "-host", $host, "-db", $db, "-date", $date, "-debug");
	
    $self->cmd("Creating Reactome2GO file",[["perl reactome2go.pl @args"]]);
};

1;
