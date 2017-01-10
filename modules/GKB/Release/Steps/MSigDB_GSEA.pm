package GKB::Release::Steps::MSigDB_GSEA;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => "gkbdev" );
has '+passwords' => ( default => sub { ['mysql'] } );
has '+directory' => ( default => "$release/msigdb-gsea" );
has '+mail' => ( default => sub { 
					my $self = shift;
					return {
						'to' => '',
						'subject' => $self->name,
						'body' => "",
						'attachment' => "Reactome_GeneSet_$version"
					};
				}
);

override 'run_commands' => sub {
    my ($self, $gkbdir) = @_;
    
    $self->cmd("Running GSEA output script", [["./runGSEAOutput.sh $self->{host} $db $user $pass 3306 Reactome_GeneSet_$version 48887 true > msigdb-gsea.out 2> msigdb-gsea.err"]]);
};
    
1;
