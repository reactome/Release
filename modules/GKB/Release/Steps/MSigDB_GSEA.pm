package GKB::Release::Steps::MSigDB_GSEA;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => 'gkbdev' );
has '+passwords' => ( default => sub { ['mysql'] } );
has '+directory' => ( default => "$release/msigdb-gsea" );
has '+mail' => ( default => sub {
					my $self = shift;
					return {
						'to' => '',
						'subject' => $self->name,
						'body' => '',
						'attachment' => "Reactome_GeneSet_$version.txt"
					};
				}
);

override 'run_commands' => sub {
    my ($self, $gkbdir) = @_;

    $self->cmd('Build GSEA jar and dependencies', [['perl build_gsea.pl']]);

    my $port = 3306;
    my $human_species = 48887;
    my @gsea_args = (
        $self->{host}, # database host
        $db, # database name
        $user, # database user
        $pass, # database pass
        $port, # database port
        "Reactome_GeneSet_$version.txt", # output file
        $human_species, # species (human = 48887)
        'true' # isForMsigDB? (when false, GMT format used)
    );

    $self->cmd('Running GSEA output script', [["./runGSEAOutput.sh @gsea_args > msigdb-gsea.out 2> msigdb-gsea.err"]]);
};

1;
