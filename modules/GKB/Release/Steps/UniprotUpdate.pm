package GKB::Release::Steps::UniprotUpdate;

use GKB::Release::Utils;
use GKB::Release::Config;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => "gkbdev" );
has '+passwords' => ( default => sub { ['mysql', 'sudo'] } );
has '+directory' => ( default => "$release/uniprot_update" );
has '+mail' => ( default => sub { 
    my $self = shift;
    return {
        'to' => 'curation',
        'subject' => $self->name,
        'body' => "",
        'attachment' => $self->directory . "/uniprot.wiki"
    };
});

override 'run_commands' => sub {
    my ($self) = @_;

    $self->cmd("Setting all UniProt files to have group permissions",
        [["echo $sudo | sudo -S chgrp $reactome_unix_group *"]]
    );

    $self->cmd("Backing up database and downloading necessary files",
        [
            ["mysqldump -u$user -p$pass -h$gkcentral_host --lock_tables=FALSE $gkcentral > $gkcentral.dump"],
            ["wget -q -N ftp://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/complete/uniprot_sprot.xml.gz"],
            ["wget -q -O uniprot-reviewed:no.list.gz 'http://www.uniprot.org/uniprot/?query=reviewed%3Ano&compress=yes&format=list'"]
        ]
    );

    my @args = ("-db", $gkcentral, "-host", $gkcentral_host, "-user", $user, "-pass", $pass);
    $self->cmd("Running uniprot perl script",[["perl uniprot_xml2sql_isoform.pl @args > uniprot.out 2> uniprot.err"]]);
};

override 'archive_files' => sub {
    my ($self, $version) = @_;

    my @files_to_archive = (
        'duplicated_db_id.txt',
        'reference_DNA_sequence_report.txt',
        'sequence_uniprot_report.txt',
        'trembl_to_update.acc',
        'uniprot.wiki'
    );
    # arguments passed to this method are implicitly passed to the superclass method by Moose
    # https://metacpan.org/pod/release/DOY/Moose-2.0604/lib/Moose/Manual/MethodModifiers.pod#OVERRIDE-AND-SUPER
    my $archive_directory = super();
    foreach my $file_to_archive (@files_to_archive) {
        system "mv $file_to_archive $archive_directory";
    }
};

1;
