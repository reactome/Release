package GKB::Release::Steps::MSigDB_GSEA;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

my $gsea_output_file = "Reactome_GeneSet_$version.txt";

has '+gkb' => ( default => 'gkbdev' );
has '+passwords' => ( default => sub { ['mysql'] } );
has '+directory' => ( default => "$release/msigdb-gsea" );
has '+mail' => ( default => sub {
	my $self = shift;
		return {
			'to' => '',
			'subject' => $self->name,
			'body' => '',
			'attachment' => $gsea_output_file
		};
	}
);

override 'run_commands' => sub {
    my ($self, $gkbdir) = @_;

    $self->cmd('Build GSEA jar and dependencies', [['perl build_gsea.pl > build_gsea.out 2> build_gsea.err']]);

    my $port = 3306;
    my $human_species = 48887;
    my @gsea_args = (
        $self->{host}, # database host
        $db, # database name
        $user, # database user
        $pass, # database pass
        $port, # database port
        $gsea_output_file, # output file
        $human_species, # species (human = 48887)
        'true' # isForMsigDB? (when false, GMT format used)
    );

    $self->cmd('Running GSEA output script', [["./runGSEAOutput.sh @gsea_args > msigdb-gsea.out 2> msigdb-gsea.err"]]);
};


override 'archive_files' => sub {
    my ($self, $version) = @_;

    # arguments passed to this method are implicitly passed to the superclass method by Moose
    # https://metacpan.org/pod/release/DOY/Moose-2.0604/lib/Moose/Manual/MethodModifiers.pod#OVERRIDE-AND-SUPER
    my $archive_directory = super();
    system "mv $gsea_output_file $archive_directory";
};

1;
