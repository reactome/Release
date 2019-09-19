package GKB::Release::Steps::DataExporter;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => 'gkbdev' );
has '+passwords' => ( default => sub { ['mysql'] } );
has '+user_input' => (default => sub {
    {
        'props' => {'query' => 'Have you updated data-exporter/config.properties? '},
    }
});
has '+directory' => ( default => "$release/data-exporter" );
has '+mail' => ( default => sub {
    my $self = shift;
    return {
        'to' => '',
        'subject' => $self->name,
        'body' => '',
        'attachment' => ''
    };
});

override 'run_commands' => sub {
    my ($self, $gkbdir) = @_;

    if ($self->user_input->{'props'}->{'response'} !~ /^y/i) {
        confess 'Please update the graph db credential, reactomeVersion, and outputDir fields ' .
                "in data-exporter/config.properties before running.\n";
    }

    # Run NCBI, UCSC, and EuropePMC scripts
    # OMIM no longer processed by NCBI (October 8, 2015 -- Joel Weiser)
    $self->cmd('Running data-exporter for NCBI, UCSC, and EuropePMC', [
        ['bash runDataExporter.sh --build_jar > data-exporter.out 2> data-exporter.err']
    ]);

    # Connect to NCBI FTP server and upload files
    $self->cmd('Uploading NCBI files',
        [
            ["perl upload_ncbi.pl -version $version > upload_ncbi.out 2> upload_ncbi.err"]
        ]
    );

    # Connect to EuropePMC FTP server and upload files
    $self->cmd('Uploading EuropePMC files',
        [
            ["perl upload_europepmc.pl -version $version > upload_europepmc.out 2> upload_europepmc.err"]
        ]
    );
};

1;
